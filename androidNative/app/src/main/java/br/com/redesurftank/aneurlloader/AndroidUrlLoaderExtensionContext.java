package br.com.redesurftank.aneurlloader;

import android.os.Build;
import android.util.JsonReader;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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
    private Map<String, List<String>> _staticHosts;

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
        functionMap.put(AddStaticHost.KEY, new AddStaticHost());
        functionMap.put(RemoveStaticHost.KEY, new RemoveStaticHost());
        return functionMap;
    }

    @Override
    public void dispose() {

    }

    public static class Initialize implements FREFunction {
        public static final String KEY = "initialize";
        private static final String TAG = "AneUrlLoaderInitialize";
        private static final ExecutorService executor = Executors.newCachedThreadPool();


        @Override
        public FREObject call(FREContext freContext, FREObject[] freObjects) {
            try {
                AndroidWebSocketLogger.i(TAG, "Initializing okhttp client and setting dns resolver");
                AndroidUrlLoaderExtensionContext context = (AndroidUrlLoaderExtensionContext) freContext;
                context._staticHosts = new HashMap<>();
                context._client = new OkHttpClient.Builder().fastFallback(true).dns(new Dns() {
                    @NonNull
                    @Override
                    public List<InetAddress> lookup(@NonNull String s) throws UnknownHostException {
                        List<InetAddress> addresses = new ArrayList<>();

                        try {
                            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
                                addresses.addAll(resolveDnsUsingThreadForLowApi(s));
                            } else {
                                List<InetAddress> fromResolversResult = resolveWithDns(s).join();
                                addresses.addAll(fromResolversResult);
                            }
                        } catch (Exception e) {
                            AndroidWebSocketLogger.e(TAG, "Error in lookup() : " + e.getMessage(), e);
                        }

                        if (!addresses.isEmpty()) {
                            return addresses;
                        }

                        synchronized (context._staticHosts) {
                            try {
                                if (context._staticHosts.containsKey(s)) {
                                    List<String> ips = context._staticHosts.get(s);
                                    for (String ip : ips) {
                                        addresses.add(InetAddress.getByName(ip));
                                    }
                                    if (!addresses.isEmpty()) {
                                        return addresses;
                                    }
                                }
                            } catch (UnknownHostException e) {
                                AndroidWebSocketLogger.e(TAG, "Error in lookup() : " + e.getMessage(), e);
                            }
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

        private InetAddress getByIpWithoutException(String ip) {
            try {
                return InetAddress.getByName(ip);
            } catch (UnknownHostException e) {
                AndroidWebSocketLogger.e(TAG, "Failure in getByIpWithoutException() : " + e.getMessage(), e);
                return null;
            }
        }

        private List<InetAddress> resolveDnsUsingThreadForLowApi(String domain) {
            AndroidWebSocketLogger.i(TAG, "resolveDnsUsingThreadForLowApi() called with: domain = [" + domain + "]");
            List<InetAddress> addresses = new ArrayList<>();
            Thread cloudflareThread = new Thread(() -> {
                try {
                    List<InetAddress> cloudflareAddresses = resolveDns(new DohResolver("https://1.1.1.1/dns-query"), domain);
                    synchronized (addresses) {
                        if (!addresses.isEmpty())
                            return;
                        AndroidWebSocketLogger.d(TAG, "resolveDnsUsingThreadForLowApi() resolved with cloudflare");
                        addresses.addAll(cloudflareAddresses);
                    }
                } catch (Exception e) {
                    AndroidWebSocketLogger.e(TAG, "Failure in resolveDnsUsingThreadForLowApi() : " + e.getMessage(), e);
                }
            });
            Thread googleThread = new Thread(() -> {
                try {
                    List<InetAddress> googleAddresses = resolveDns(new DohResolver("https://dns.google/dns-query"), domain);
                    synchronized (addresses) {
                        if (!addresses.isEmpty())
                            return;
                        AndroidWebSocketLogger.d(TAG, "resolveDnsUsingThreadForLowApi() resolved with google");
                        addresses.addAll(googleAddresses);
                    }
                } catch (Exception e) {
                    AndroidWebSocketLogger.e(TAG, "Failure in resolveDnsUsingThreadForLowApi() : " + e.getMessage(), e);
                }
            });
            Thread adguardThread = new Thread(() -> {
                try {
                    List<InetAddress> adguardAddresses = resolveDns(new DohResolver("https://unfiltered.adguard-dns.com/dns-query"), domain);
                    synchronized (addresses) {
                        if (!addresses.isEmpty())
                            return;
                        AndroidWebSocketLogger.d(TAG, "resolveDnsUsingThreadForLowApi() resolved with adguard");
                        addresses.addAll(adguardAddresses);
                    }
                } catch (Exception e) {
                    AndroidWebSocketLogger.e(TAG, "Failure in resolveDnsUsingThreadForLowApi() : " + e.getMessage(), e);
                }
            });
            Thread cloudflareNormalThread = new Thread(() -> {
                try {
                    List<InetAddress> adguardAddresses = resolveDns(new SimpleResolver(Objects.requireNonNull(getByIpWithoutException("1.1.1.1"))), domain);
                    synchronized (addresses) {
                        if (!addresses.isEmpty())
                            return;
                        AndroidWebSocketLogger.d(TAG, "resolveDnsUsingThreadForLowApi() resolved with cloudflare normal");
                        addresses.addAll(adguardAddresses);
                    }
                } catch (Exception e) {
                    AndroidWebSocketLogger.e(TAG, "Failure in resolveDnsUsingThreadForLowApi() : " + e.getMessage(), e);
                }
            });
            Thread googleNormalThread = new Thread(() -> {
                try {
                    List<InetAddress> adguardAddresses = resolveDns(new SimpleResolver(Objects.requireNonNull(getByIpWithoutException("8.8.8.8"))), domain);
                    synchronized (addresses) {
                        if (!addresses.isEmpty())
                            return;
                        AndroidWebSocketLogger.d(TAG, "resolveDnsUsingThreadForLowApi() resolved with google normal");
                        addresses.addAll(adguardAddresses);
                    }
                } catch (Exception e) {
                    AndroidWebSocketLogger.e(TAG, "Failure in resolveDnsUsingThreadForLowApi() : " + e.getMessage(), e);
                }
            });
            Thread adGuardNormalThread = new Thread(() -> {
                try {
                    List<InetAddress> adguardAddresses = resolveDns(new SimpleResolver(Objects.requireNonNull(getByIpWithoutException("94.140.14.140"))), domain);
                    synchronized (addresses) {
                        if (!addresses.isEmpty())
                            return;
                        AndroidWebSocketLogger.d(TAG, "resolveDnsUsingThreadForLowApi() resolved with adguard normal");
                        addresses.addAll(adguardAddresses);
                    }
                } catch (Exception e) {
                    AndroidWebSocketLogger.e(TAG, "Failure in resolveDnsUsingThreadForLowApi() : " + e.getMessage(), e);
                }
            });

            cloudflareThread.start();
            googleThread.start();
            adguardThread.start();
            cloudflareNormalThread.start();
            googleNormalThread.start();
            adGuardNormalThread.start();

            while (true) {
                synchronized (addresses) {
                    if (!addresses.isEmpty()) {
                        break;
                    }
                }
                //check if all threads are done
                if (!cloudflareThread.isAlive() && !googleThread.isAlive() && !adguardThread.isAlive() && !cloudflareNormalThread.isAlive() && !googleNormalThread.isAlive() && !adGuardNormalThread.isAlive()) {
                    break;
                }
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }

            return addresses;
        }

        @RequiresApi(api = Build.VERSION_CODES.N)
        private CompletableFuture<List<InetAddress>> resolveWithDns(String domain) {
            AndroidWebSocketLogger.i(TAG, "resolveWithDns() called with: domain = [" + domain + "]");
            CompletableFuture<List<InetAddress>> cloudflareFuture = CompletableFuture.supplyAsync(() -> resolveDns(new DohResolver("https://1.1.1.1/dns-query"), domain), executor);
            CompletableFuture<List<InetAddress>> googleFuture = CompletableFuture.supplyAsync(() -> resolveDns(new DohResolver("https://dns.google/dns-query"), domain), executor);
            CompletableFuture<List<InetAddress>> adguardFuture = CompletableFuture.supplyAsync(() -> resolveDns(new DohResolver("https://unfiltered.adguard-dns.com/dns-query"), domain), executor);
            CompletableFuture<List<InetAddress>> cloudflareNormalFuture = CompletableFuture.supplyAsync(() -> resolveDns(new SimpleResolver(Objects.requireNonNull(getByIpWithoutException("1.1.1.1"))), domain), executor);
            CompletableFuture<List<InetAddress>> googleNormalFuture = CompletableFuture.supplyAsync(() -> resolveDns(new SimpleResolver(Objects.requireNonNull(getByIpWithoutException("8.8.8.8"))), domain), executor);
            CompletableFuture<List<InetAddress>> adguardNormalFuture = CompletableFuture.supplyAsync(() -> resolveDns(new SimpleResolver(Objects.requireNonNull(getByIpWithoutException("94.140.14.140"))), domain), executor);

            return CompletableFuture.anyOf(cloudflareFuture, googleFuture, adguardFuture, cloudflareNormalFuture, googleNormalFuture, adguardNormalFuture)
                    .thenApply(o -> {
                        if (cloudflareFuture.isDone() && !cloudflareFuture.isCompletedExceptionally()) {
                            AndroidWebSocketLogger.d(TAG, "resolveWithDns() resolved with cloudflare");
                            return cloudflareFuture.join();
                        } else if (googleFuture.isDone() && !googleFuture.isCompletedExceptionally()) {
                            AndroidWebSocketLogger.d(TAG, "resolveWithDns() resolved with google");
                            return googleFuture.join();
                        } else if (adguardFuture.isDone() && !adguardFuture.isCompletedExceptionally()) {
                            AndroidWebSocketLogger.d(TAG, "resolveWithDns() resolved with adguard");
                            return adguardFuture.join();
                        } else if (cloudflareNormalFuture.isDone() && !cloudflareNormalFuture.isCompletedExceptionally()) {
                            AndroidWebSocketLogger.d(TAG, "resolveWithDns() resolved with cloudflare normal");
                            return cloudflareNormalFuture.join();
                        } else if (googleNormalFuture.isDone() && !googleNormalFuture.isCompletedExceptionally()) {
                            AndroidWebSocketLogger.d(TAG, "resolveWithDns() resolved with google normal");
                            return googleNormalFuture.join();
                        } else if (adguardNormalFuture.isDone() && !adguardNormalFuture.isCompletedExceptionally()) {
                            AndroidWebSocketLogger.d(TAG, "resolveWithDns() resolved with adguard normal");
                            return adguardNormalFuture.join();
                        }

                        return new ArrayList<>();
                    });
        }

        private List<InetAddress> resolveDns(Resolver resolver, String domain) {
            try {
                Record queryRecord = Record.newRecord(Name.fromString(domain + "."), Type.A, DClass.IN);
                Message queryMessage = Message.newQuery(queryRecord);
                Message result = resolver.send(queryMessage);
                List<Record> answers = result.getSection(Section.ANSWER);
                List<InetAddress> addresses = new ArrayList<>();
                for (Record record : answers) {
                    if (record.getType() == Type.A || record.getType() == Type.AAAA) {
                        addresses.add(InetAddress.getByName(record.rdataToString()));
                    }
                }
                return addresses;
            } catch (Exception e) {
                AndroidWebSocketLogger.d(TAG, "Failure in resolveDns() : " + e.getMessage(), e);
                throw new RuntimeException(e);
            }
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

    public static class AddStaticHost implements FREFunction {
        public static final String KEY = "addStaticHost";
        private static final String TAG = "AndroidUrlLoaderAddStaticHost";

        @Override
        public FREObject call(FREContext freContext, FREObject[] freObjects) {
            try {
                AndroidWebSocketLogger.i(TAG, "Adding static host");
                AndroidUrlLoaderExtensionContext context = (AndroidUrlLoaderExtensionContext) freContext;

                String host = freObjects[0].getAsString();
                String ip = freObjects[1].getAsString();

                synchronized (context._staticHosts) {
                    if (!context._staticHosts.containsKey(host)) {
                        context._staticHosts.put(host, new ArrayList<>());
                    }
                    context._staticHosts.get(host).add(ip);
                }

                return FREObject.newObject(true);
            } catch (Exception e) {
                AndroidWebSocketLogger.e(TAG, "Error adding static host", e);
            }

            return null;
        }
    }

    public static class RemoveStaticHost implements FREFunction {
        public static final String KEY = "removeStaticHost";
        private static final String TAG = "AndroidUrlLoaderRemoveStaticHost";

        @Override
        public FREObject call(FREContext freContext, FREObject[] freObjects) {
            try {
                AndroidWebSocketLogger.i(TAG, "Removing static host");
                AndroidUrlLoaderExtensionContext context = (AndroidUrlLoaderExtensionContext) freContext;

                String host = freObjects[0].getAsString();

                synchronized (context._staticHosts) {
                    context._staticHosts.remove(host);
                }

                return FREObject.newObject(true);
            } catch (Exception e) {
                AndroidWebSocketLogger.e(TAG, "Error removing static host", e);
            }

            return null;
        }
    }
}
