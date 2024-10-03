using UrlLoaderNativeLibrary;


HappyEyeballsHttp.AddStaticHost("test.static", "127.0.0.1");
HappyEyeballsHttp.AddStaticHost("redesurftank.com.br", "104.26.9.41");

var httpClient = HappyEyeballsHttp.CreateHttpClient();

var test = await httpClient.GetStringAsync("https://redesurftank.com.br");
Console.WriteLine(test);

var test2 = await httpClient.GetStringAsync("https://test.static/");
Console.WriteLine(test2);