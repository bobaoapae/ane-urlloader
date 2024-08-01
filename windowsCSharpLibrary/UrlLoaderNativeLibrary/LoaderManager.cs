using System;
using System.Collections.Generic;
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

    public void Initialize(Action<string, byte[]> success, Action<string, string> error, Action<string, string> progress)
    {
        Initialized = true;
        _success = success;
        _error = error;
        _progress = progress;
    }

    public string StartLoad(string url, string method, Dictionary<string, string> variables, Dictionary<string, string> headers)
    {
        var randomId = Guid.NewGuid();
        var client = HappyEyeballsHttp.CreateHttpClient();

        _ = Task.Run(async () =>
        {
            var request = new HttpRequestMessage(new HttpMethod(method.ToUpper()), url);
            foreach (var (key, value) in headers)
            {
                request.Headers.Add(key, value);
            }
            
            //if method is GET, add variables to the url
            if (method.ToUpper() == "GET")
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
            else
            {
                request.Content = new FormUrlEncodedContent(variables);
            }

            try
            {
                var response = await client.SendAsync(request);
                var result = await response.Content.ReadAsByteArrayAsync();
                _ = Task.Run(() => _success(randomId.ToString(), result));
            }
            catch (Exception e)
            {
                _ = Task.Run(() => _error(randomId.ToString(), e.Message));
            }
        });

        return randomId.ToString();
    }
}