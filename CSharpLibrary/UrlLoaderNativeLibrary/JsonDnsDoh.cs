using System.Collections.Generic;
using System.Text.Json.Serialization;

namespace UrlLoaderNativeLibrary;

public class DnsResponse
{
    [JsonPropertyName("Answer")]
    public List<DnsAnswer> Answers { get; set; }
}

public class DnsAnswer
{
    [JsonPropertyName("type")]
    public int Type { get; set; } // 1 for A records, 28 for AAAA records
    [JsonPropertyName("data")]
    public string Data { get; set; }
}

[JsonSerializable(typeof(DnsResponse))]
internal partial class DnsResponseContext : JsonSerializerContext
{
}