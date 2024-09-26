using System.Collections.Generic;
using System.Text.Json.Serialization;

namespace UrlLoaderNativeLibrary;

public class DnsResponse
{
    public List<DnsAnswer> Answers { get; set; }
}

public class DnsAnswer
{
    public int Type { get; set; } // 1 for A records, 28 for AAAA records
    public string Data { get; set; }
}

[JsonSerializable(typeof(DnsResponse))]
internal partial class DnsResponseContext : JsonSerializerContext
{
}