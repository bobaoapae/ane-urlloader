using System;
using System.Collections.Generic;
using System.Net;
using System.Net.Http;
using System.Threading.Tasks;

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
    private HttpClient _client;

    public void Initialize(Action<string, byte[]> success, Action<string, string> error, Action<string, string> progress, Action<string> writeLog)
    {
        Initialized = true;
        _success = success;
        _error = error;
        _progress = progress;
        _writeLog = writeLog;
        _client = HappyEyeballsHttp.CreateHttpClient();
    }

    public string StartLoad(string url, string method, Dictionary<string, string> variables, Dictionary<string, string> headers)
    {
        var randomId = Guid.NewGuid();

        _ = Task.Run(async () =>
        {
            var request = new HttpRequestMessage(new HttpMethod(method.ToUpper()), url);
            request.Version = HttpVersion.Version20;
            request.VersionPolicy = HttpVersionPolicy.RequestVersionOrLower;
            foreach (var (key, value) in headers)
            {
                request.Headers.Add(key, value);
            }

            //if method is GET, add variables to the url
            if (request.Method == HttpMethod.Get)
            {
                var uriBuilder = new UriBuilder(url);
                var query = System.Web.HttpUtility.ParseQueryString(uriBuilder.Query);
                foreach (var (key, value) in variables)
                {
                    query[key] = value;
                }

                uriBuilder.Query = query.ToString()!;
                request.RequestUri = uriBuilder.Uri;
            }
            else //if method is POST, add variables to the content
            {
                request.Content = new FormUrlEncodedContent(variables);
            }

            try
            {
                var response = await _client.SendAsync(request);
                var result = await response.Content.ReadAsByteArrayAsync();
                _ = Task.Run(() =>
                {
                    try
                    {
                        _success(randomId.ToString(), result);
                    }
                    catch (Exception e)
                    {
                        try
                        {
                            _writeLog(e.Message);
                        }
                        catch (Exception)
                        {
                            // ignored
                        }

                        if (e.InnerException != null)
                        {
                            try
                            {
                                _writeLog(e.InnerException.Message);
                            }
                            catch (Exception)
                            {
                                // ignored
                            }
                        }

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
                    try
                    {
                        _writeLog(e.Message);
                    }
                    catch (Exception)
                    {
                        // ignored
                    }

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
}