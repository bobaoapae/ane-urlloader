using System;
using System.Collections.Generic;
using System.IO;
using System.Net;
using System.Net.Http;
using System.Threading.Tasks;
using System.Web;

namespace UrlLoaderNativeLibrary;

public class LoaderManager
{
    private static LoaderManager _instance;

    public static LoaderManager Instance => _instance ??= new LoaderManager();

    public bool Initialized { get; private set; }

    private Action<string, byte[]> _success;
    private Action<string, string> _error;
    private Action<string, string> _progress;
    private Action<string> _writeLog;
    private HttpClient[] _clients;

    public void Initialize(Action<string, byte[]> success, Action<string, string> error, Action<string, string> progress, Action<string> writeLog)
    {
        Initialized = true;
        _success = success;
        _error = error;
        _progress = progress;
        _writeLog = writeLog;
        _clients =
        [
            HappyEyeballsHttp.CreateHttpClient(true, _writeLog),
            HappyEyeballsHttp.CreateHttpClient(true, _writeLog),
            HappyEyeballsHttp.CreateHttpClient(true, _writeLog),
            HappyEyeballsHttp.CreateHttpClient(true, _writeLog),
            HappyEyeballsHttp.CreateHttpClient(true, _writeLog),
            HappyEyeballsHttp.CreateHttpClient(true, _writeLog)
        ];
    }

    public string StartLoad(string url, string method, Dictionary<string, string> variables, Dictionary<string, string> headers)
    {
        var randomId = Guid.NewGuid();

        _ = Task.Run(async () =>
        {
            try
            {
                var request = new HttpRequestMessage(new HttpMethod(method.ToUpper()), url);
                request.Version = HttpVersion.Version20;
                request.VersionPolicy = HttpVersionPolicy.RequestVersionOrLower;
                foreach (var (key, value) in headers)
                {
                    request.Headers.Add(key, value);
                }

                // If method is GET, add variables to the url
                if (request.Method == HttpMethod.Get)
                {
                    var uriBuilder = new UriBuilder(url);
                    var query = HttpUtility.ParseQueryString(uriBuilder.Query);
                    foreach (var (key, value) in variables)
                    {
                        query[key] = value;
                    }

                    uriBuilder.Query = query.ToString()!;
                    request.RequestUri = uriBuilder.Uri;
                }
                else // If method is POST, add variables to the content
                {
                    request.Content = new FormUrlEncodedContent(variables);
                }

                var response = await _clients[Random.Shared.Next(0, _clients.Length - 1)].SendAsync(request, HttpCompletionOption.ResponseHeadersRead);
                if (response.StatusCode >= HttpStatusCode.BadRequest)
                {
                    _ = Task.Run(() =>
                    {
                        try
                        {
                            _error(randomId.ToString(), $"Invalid status code: {response.StatusCode}");
                        }
                        catch (Exception e)
                        {
                            LogAll(e);
                        }
                    });

                    return;
                }

                var contentLength = response.Content.Headers.ContentLength ?? -1;
                var totalBytesRead = 0L;
                var buffer = new byte[8192];

                await using var stream = await response.Content.ReadAsStreamAsync();
                using var memoryStream = new MemoryStream();

                int bytesRead;
                while ((bytesRead = await stream.ReadAsync(buffer, 0, buffer.Length)) > 0)
                {
                    memoryStream.Write(buffer, 0, bytesRead);
                    totalBytesRead += bytesRead;

                    // Report progress if content length is known
                    if (contentLength > 0)
                    {
                        _progress(randomId.ToString(), $"{totalBytesRead};{contentLength}");
                    }
                }

                var result = memoryStream.ToArray();
                _ = Task.Run(() =>
                {
                    try
                    {
                        _success(randomId.ToString(), result);
                    }
                    catch (Exception e)
                    {
                        LogAll(e);

                        try
                        {
                            _error(randomId.ToString(), e.Message);
                        }
                        catch (Exception)
                        {
                            // ignored
                        }
                    }
                });
            }
            catch (Exception e)
            {
                _ = Task.Run(() =>
                {
                    LogAll(e);

                    try
                    {
                        _error(randomId.ToString(), e.Message);
                    }
                    catch (Exception)
                    {
                        // ignored
                    }
                });
            }
        });

        return randomId.ToString();
    }

    private void LogAll(Exception exception)
    {
        if (exception == null)
            return;

        try
        {
            var logBuilder = new System.Text.StringBuilder();

            // Log the main exception
            logBuilder.AppendLine($"Exception: {exception.Message}");
            logBuilder.AppendLine($"Stack Trace: {exception.StackTrace}");

            var inner = exception.InnerException;
            while (inner != null)
            {
                logBuilder.AppendLine($"Inner Exception: {inner.Message}");
                logBuilder.AppendLine($"Inner Stack Trace: {inner.StackTrace}");
                inner = inner.InnerException;
            }

            // Call _writeLog once with the complete log string
            _writeLog(logBuilder.ToString());
        }
        catch (Exception)
        {
            // ignored
        }
    }

    public void AddStaticHost(string host, string ip)
    {
        HappyEyeballsHttp.AddStaticHost(host, ip);
    }

    public void RemoveStaticHost(string host)
    {
        HappyEyeballsHttp.RemoveStaticHost(host);
    }
}