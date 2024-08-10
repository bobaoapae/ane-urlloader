package br.com.redesurftank.aneurlloader;

import android.util.JsonReader;
import android.util.Log;

import androidx.annotation.NonNull;

import com.adobe.fre.FREByteArray;
import com.adobe.fre.FREContext;
import com.adobe.fre.FREFunction;
import com.adobe.fre.FREObject;

import org.xbill.DNS.DClass;
import org.xbill.DNS.DohResolver;
import org.xbill.DNS.Message;
import org.xbill.DNS.Name;
import org.xbill.DNS.Record;
import org.xbill.DNS.Resolver;
import org.xbill.DNS.Section;
import org.xbill.DNS.SimpleResolver;
import org.xbill.DNS.Type;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Dns;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class AndroidUrlLoaderExtensionContext extends FREContext {
    private static final String CTX_NAME = "AndroidWebSocketExtensionContext";
    private String tag;
    private OkHttpClient _client;
    private Map<UUID, byte[]> _byteBuffers;

    public AndroidUrlLoaderExtensionContext(String extensionName) {
        this.tag = extensionName + "." + CTX_NAME;
        AndroidWebSocketLogger.i(this.tag, "Creating context");
        _byteBuffers = new HashMap<>();
    }

    @Override
    public Map<String, FREFunction> getFunctions() {
        AndroidWebSocketLogger.i(this.tag, "Creating function Map");
        Map<String, FREFunction> functionMap = new HashMap<>();
        functionMap.put(Initialize.KEY, new Initialize());
        functionMap.put(LoadUrl.KEY, new LoadUrl());
        functionMap.put(GetResponse.KEY, new GetResponse());
        return functionMap;
    }

    @Override
    public void dispose() {

    }

    public static class Initialize implements FREFunction {
        public static final String KEY = "initialize";
        private static final String TAG = "AneUrlLoaderInitialize";

        @Override
        public FREObject call(FREContext freContext, FREObject[] freObjects) {
            try {
                AndroidWebSocketLogger.i(TAG, "Initializing");
                AndroidUrlLoaderExtensionContext context = (AndroidUrlLoaderExtensionContext) freContext;
                context._client = new OkHttpClient.Builder().fastFallback(true).dns(new Dns() {
                    @NonNull
                    @Override
                    public List<InetAddress> lookup(@NonNull String s) throws UnknownHostException {
                        List<InetAddress> addresses = new ArrayList<>();
                        try {
                            Log.d(TAG, "Resolving address using cloudflare DoH");
                            DohResolver dohResolver = new DohResolver("https://1.1.1.1/dns-query");
                            Record queryRecord = Record.newRecord(Name.fromString(s + "."), Type.A, DClass.IN);
                            Message queryMessage = Message.newQuery(queryRecord);
                            Message result = dohResolver.send(queryMessage);
                            List<Record> answers = result.getSection(Section.ANSWER);
                            for (Record record : answers) {
                                if (record.getType() == Type.A || record.getType() == Type.AAAA) {
                                    addresses.add(InetAddress.getByName(record.rdataToString()));
                                }
                            }

                        } catch (Exception e) {
                            AndroidWebSocketLogger.e(TAG, "Failure in resolve() method using doh cloudflare: " + e.getMessage(), e);
                        }

                        if (!addresses.isEmpty()) {
                            return addresses;
                        }

                        try {
                            Log.d(TAG, "Resolving address using cloudflare dns normal udp");
                            Resolver resolver = new SimpleResolver(InetAddress.getByName("1.1.1.1"));
                            Record queryRecord = Record.newRecord(Name.fromString(s + "."), Type.A, DClass.IN);
                            Message queryMessage = Message.newQuery(queryRecord);
                            Message result = resolver.send(queryMessage);
                            List<Record> answers = result.getSection(Section.ANSWER);
                            for (Record record : answers) {
                                if (record.getType() == Type.A || record.getType() == Type.AAAA) {
                                    addresses.add(InetAddress.getByName(record.rdataToString()));
                                }
                            }
                        } catch (Exception e) {
                            AndroidWebSocketLogger.e(TAG, "Failure in resolve() method using udp cloudflare: " + e.getMessage(), e);
                        }

                        if (!addresses.isEmpty()) {
                            return addresses;
                        }

                        addresses.addAll(Dns.SYSTEM.lookup(s));

                        return addresses;
                    }
                }).build();
                return FREObject.newObject(true);
            } catch (Exception e) {
                AndroidWebSocketLogger.e(TAG, "Error initializing", e);
            }
            return null;
        }
    }

    public static class LoadUrl implements FREFunction {
        public static final String KEY = "loadUrl";
        private static final String TAG = "AndroidUrlLoaderLoadUrl";

        @Override
        public FREObject call(FREContext freContext, FREObject[] freObjects) {
            try {
                AndroidWebSocketLogger.i(TAG, "Loading url");
                AndroidUrlLoaderExtensionContext context = (AndroidUrlLoaderExtensionContext) freContext;

                String url = freObjects[0].getAsString();
                String method = freObjects[1].getAsString();
                String variablesJson = freObjects[2].getAsString();
                String headersJson = freObjects[3].getAsString();

                AndroidWebSocketLogger.i(TAG, "URL: " + url);
                AndroidWebSocketLogger.i(TAG, "Method: " + method);
                AndroidWebSocketLogger.i(TAG, "Variables: " + variablesJson);
                AndroidWebSocketLogger.i(TAG, "Headers: " + headersJson);

                Map<String, String> variables = new HashMap<>();
                JsonReader reader = new JsonReader(new java.io.StringReader(variablesJson));
                if (!variablesJson.isEmpty()) {
                    reader.beginObject();
                    while (reader.hasNext()) {
                        String name = reader.nextName();
                        String value = reader.nextString();
                        variables.put(name, value);
                    }
                    reader.endObject();
                }
                reader.close();

                Map<String, String> headers = new HashMap<>();
                reader = new JsonReader(new java.io.StringReader(headersJson));
                if (!headersJson.isEmpty()) {
                    reader.beginObject();
                    while (reader.hasNext()) {
                        String name = reader.nextName();
                        String value = reader.nextString();
                        headers.put(name, value);
                    }
                    reader.endObject();
                }
                reader.close();

                UUID uuid = UUID.randomUUID();

                Request.Builder requestBuilder = new Request.Builder();

                if (method.equals("GET")) {
                    okhttp3.HttpUrl.Builder urlBuilder = Objects.requireNonNull(HttpUrl.parse(url)).newBuilder();
                    for (Map.Entry<String, String> entry : variables.entrySet()) {
                        urlBuilder.addQueryParameter(entry.getKey(), entry.getValue());
                    }
                    requestBuilder.url(urlBuilder.build());
                } else if (method.equals("POST")) {
                    okhttp3.FormBody.Builder formBuilder = new okhttp3.FormBody.Builder();
                    for (Map.Entry<String, String> entry : variables.entrySet()) {
                        formBuilder.add(entry.getKey(), entry.getValue());
                    }
                    requestBuilder.url(url).post(formBuilder.build());
                }

                for (Map.Entry<String, String> entry : headers.entrySet()) {
                    requestBuilder.addHeader(entry.getKey(), entry.getValue());
                }

                Request request = requestBuilder.build();
                context._client.newCall(request).enqueue(new Callback() {
                    @Override
                    public void onFailure(@NonNull Call call, @NonNull IOException e) {
                        AndroidWebSocketLogger.e(TAG, "Error loading url", e);
                        context.dispatchStatusEventAsync("error", uuid + ";" + e.getMessage());
                    }

                    @Override
                    public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                        try {
                            int statusCode = response.code();
                            AndroidWebSocketLogger.i(TAG, "URL: " + url + " Status code: " + statusCode);
                            if (statusCode >= 400) {
                                context.dispatchStatusEventAsync("error", uuid + ";Invalid status code: " + statusCode);
                                return;
                            }
                            byte[] bytes = response.body().bytes();
                            context._byteBuffers.put(uuid, bytes);
                            context.dispatchStatusEventAsync("success", uuid.toString());
                        } catch (Exception e) {
                            AndroidWebSocketLogger.e(TAG, "Error loading url", e);
                            context.dispatchStatusEventAsync("error", uuid + ";" + e.getMessage());
                        }
                    }
                });

                return FREObject.newObject(uuid.toString());

            } catch (Exception e) {
                AndroidWebSocketLogger.e(TAG, "Error loading url", e);
            }

            return null;
        }
    }

    public static class GetResponse implements FREFunction {
        public static final String KEY = "getResult";
        private static final String TAG = "AndroidUrlLoaderGetResult";

        @Override
        public FREObject call(FREContext freContext, FREObject[] freObjects) {
            try {
                AndroidWebSocketLogger.i(TAG, "Getting result");
                AndroidUrlLoaderExtensionContext context = (AndroidUrlLoaderExtensionContext) freContext;

                UUID uuid = UUID.fromString(freObjects[0].getAsString());
                byte[] bytes = context._byteBuffers.get(uuid);
                if (bytes != null) {
                    context._byteBuffers.remove(uuid);
                    FREByteArray byteArray = FREByteArray.newByteArray(bytes.length);
                    byteArray.acquire();
                    byteArray.getBytes().put(bytes);
                    byteArray.release();
                    return byteArray;
                }
            } catch (Exception e) {
                AndroidWebSocketLogger.e(TAG, "Error getting result", e);
            }

            return null;
        }
    }
}
