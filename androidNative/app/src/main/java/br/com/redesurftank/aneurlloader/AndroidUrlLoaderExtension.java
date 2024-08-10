package br.com.redesurftank.aneurlloader;

import com.adobe.fre.FREContext;
import com.adobe.fre.FREExtension;

public class AndroidUrlLoaderExtension implements FREExtension {

    private static final String EXT_NAME = "AndroidUrlLoaderExtension";
    private AndroidUrlLoaderExtensionContext context;
    private String tag = "AndroidUrlLoaderExtension";

    @Override
    public FREContext createContext(String arg0) {
        AndroidWebSocketLogger.i(this.tag, "Creating context");
        this.context = new AndroidUrlLoaderExtensionContext(EXT_NAME);
        return this.context;
    }

    @Override
    public void dispose() {
        AndroidWebSocketLogger.i(this.tag, "Disposing extension");
    }

    @Override
    public void initialize() {
        AndroidWebSocketLogger.i(this.tag, "Initialize");
    }
}
