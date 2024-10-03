using UrlLoaderNativeLibrary;


HappyEyeballsHttp.AddStaticHost("test.static", "127.0.0.1");

var httpClient = HappyEyeballsHttp.CreateHttpClient();

var test = await httpClient.GetStringAsync("https://www.google.com");
Console.WriteLine(test);

var test2 = await httpClient.GetStringAsync("https://test.static/");
Console.WriteLine(test2);