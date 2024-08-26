using UrlLoaderNativeLibrary;

var httpClient = HappyEyeballsHttp.CreateHttpClient();

var test = await httpClient.GetStringAsync("https://www.google.com");
Console.WriteLine(test);