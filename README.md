# AneUrlLoader

**Extension ID:** `br.com.redesurftank.aneurlloader`

AneUrlLoader is an Adobe AIR Native Extension (ANE) compatible with Windows 32-bit, Android, and macOS platforms. It provides a straightforward way to load URLs with various HTTP methods, custom headers, or parameters.

## Key Features

- **Happy Eyeballs (RFC 8305)**: Implements the Happy Eyeballs algorithm to improve connection performance by quickly falling back to the best available IP version (IPv4 or IPv6).
- **HTTP/2 Support**: Utilizes HTTP/2 for faster and more efficient network communication, with a fallback to HTTP/1 if needed.
- **TLS 1.3**: Supports the latest TLS 1.3 protocol for enhanced security and performance.
- **Custom DNS Resolver**: Uses Cloudflare and Google DNS by default, with the ability to configure custom DNS servers for more flexibility.
- **Cross-Platform Compatibility**: Works seamlessly across Windows 32-bit, Android, and macOS platforms.

## Supported Platforms

- Windows 32-bit
- Android
- macOS

## Installation

Ensure that you have included the `AneUrlLoader` ANE in your Adobe AIR project. Update your application descriptor XML to include the extension ID `br.com.redesurftank.aneurlloader` and necessary permissions for each platform.

## Initialization

Before using AneUrlLoader, check if the extension is supported on the current platform and initialize it only once during your application lifecycle.

### Step-by-Step Initialization

1. **Check if the extension is supported:**

   ```actionscript
   if (AneUrlLoader.isSupported) {
       trace("AneUrlLoader is supported on this platform.");
   } else {
       trace("AneUrlLoader is not supported on this platform.");
   }
   ```

2. **Initialize the extension (do this once, typically in your app startup code):**

   ```actionscript
   var initialized:Boolean = false;
   
   if (AneUrlLoader.isSupported) {
       initialized = AneUrlLoader.instance.initialize();
       if (initialized) {
           trace("AneUrlLoader initialized successfully.");
       } else {
           trace("Failed to initialize AneUrlLoader.");
       }
   }
   ```

## Usage

After initialization, you can use `AneUrlLoader` to load URLs with different HTTP methods and configurations.

### Loading a URL

To load a URL, use the `loadUrl` method from the `AneUrlLoader` instance.

#### `loadUrl` Method Signature

```actionscript
native public function loadUrl(
    url:String, 
    method:String = "GET", 
    variables:Object = null, 
    headers:Object = null, 
    onResult:Function = null, 
    onError:Function = null, 
    onProgress:Function = null
):void;
```

#### Parameters

- **url**: The URL to load.
- **method**: The HTTP method to use (default is "GET").
- **variables**: An object containing variables to send with the request (optional).
- **headers**: An object containing custom headers to send with the request (optional).
- **onResult**: A callback function that will be called upon a successful response. The response is passed as a `ByteArray`.
- **onError**: A callback function that will be called if an error occurs. The error is passed as an `Error` object.
- **onProgress**: A callback function that will be called to report the progress of the loading. Progress is passed as a `Number` representing the percentage completed.

### Example Code

Below is an example demonstrating how to use `AneUrlLoader` to load a URL after the extension has been initialized:

```actionscript
// Initialize AneUrlLoader once in your application
var initialized:Boolean = false;

if (AneUrlLoader.isSupported) {
    initialized = AneUrlLoader.instance.initialize();
    if (initialized) {
        trace("AneUrlLoader initialized successfully.");
    } else {
        trace("Failed to initialize AneUrlLoader.");
    }
} else {
    trace("AneUrlLoader is not supported on this platform.");
}

// Usage example after initialization
if (initialized) {
    // Define the URL, HTTP method, and optional headers and variables
    var url:String = "https://example.com/api/data";
    var method:String = "POST";
    var variables:Object = { key1: "value1", key2: "value2" };
    var headers:Object = { "Content-Type": "application/json" };

    // Load the URL with specified parameters and handle responses
    AneUrlLoader.instance.loadUrl(
        url,
        method,
        variables,
        headers,
        function(response:ByteArray):void {
            trace("Success: " + response.toString());
        },
        function(error:Error):void {
            trace("Error: " + error.message);
        },
        function(progress:Number):void {
            trace("Progress: " + progress + "% completed.");
        }
    );
}
```

## Notes

- Always check if the extension is supported on the current platform using `AneUrlLoader.isSupported`.
- Initialize the extension only once before using other methods.
- Ensure your Adobe AIR application descriptor XML file is correctly configured for each platform with the required permissions and extension ID.

## Troubleshooting

If you encounter issues:

1. **Check platform compatibility**: Ensure the ANE is supported on your target platform.
2. **Review initialization**: Make sure `initialize()` is called and returns `true`.
3. **Inspect callback functions**: Ensure your callback functions handle errors and results appropriately.

For further assistance, consult the official documentation or contact support.
