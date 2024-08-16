package br.com.redesurftank {
import flash.events.StatusEvent;
import flash.external.ExtensionContext;
import flash.net.URLVariables;
import flash.system.Capabilities;
import flash.utils.ByteArray;
import flash.utils.Dictionary;

public class AneUrlLoader {

    private static var _instance:AneUrlLoader;

    public static function get instance():AneUrlLoader {
        if (!_instance) {
            _instance = new AneUrlLoader();
        }
        return _instance;
    }

    public static function get isSupported():Boolean {
        var plataform:String = Capabilities.version.substr(0, 3);
        switch (plataform) {
            case "AND":
                return true;
            case "WIN":
                return true;
            default:
                return false;
        }
    }

    private var _extContext:ExtensionContext;
    private var _loaders:Dictionary = new Dictionary();

    function AneUrlLoader() {
        _extContext = ExtensionContext.createExtensionContext("br.com.redesurftank.aneurlloader", "");
        if (!_extContext) {
            throw new Error("ANE not loaded properly. Please check if the extension is added to your AIR project.");
        }
        _extContext.addEventListener("status", onStatusEvent);
    }

    public function initialize():Boolean {
        var result:Boolean = _extContext.call("initialize") as Boolean;
        return result;
    }

    public function loadUrl(url:String, method:String = "GET", variables:Object = null, headers:Object = null, onResult:Function = null, onError:Function = null, onProgress:Function = null):void {
        if (headers is Dictionary) {
            var headersDict:Dictionary = headers as Dictionary;
            var headersObj:Object = {};
            for (var key:Object in headersDict) {
                headersObj[key] = String(headersDict[key]);
            }
            headers = headersObj;
        }
        if (variables is URLVariables) {
            var variablesURL:URLVariables = variables as URLVariables;
            var variablesObj:Object = {};
            for (key in variablesURL) {
                variablesObj[key] = String(variablesURL[key]);
            }
            variables = variablesObj;
        }
        var variablesJson:String = variables ? JSON.stringify(variables) : "";
        var headersJson:String = headers ? JSON.stringify(headers) : "";
        var loaderId:String = _extContext.call("loadUrl", url, method, variablesJson, headersJson) as String;
        if (!loaderId) {
            if (onError) {
                onError(new Error("Error loading URL"));
            }
            return;
        }
        _loaders[loaderId] = {onResult: onResult, onError: onError, onProgress: onProgress};
    }

    private function onStatusEvent(param1:StatusEvent):void {
        var dataSplit:Array = param1.level.split(";");
        var loaderId:String = dataSplit[0];
        var loader:Object = _loaders[loaderId];
        if (!loader) {
            return;
        }
        switch (param1.code) {
            case "progress": {
                if (loader.onProgress) {
                    loader.onProgress(dataSplit[1]);
                }
                break;
            }
            case "error": {
                if (loader.onError) {
                    loader.onError(new Error(dataSplit[1]));
                }
                delete _loaders[loaderId];
                break;
            }
            case "success": {
                if (loader.onResult) {
                    var result:ByteArray = _extContext.call("getResult", loaderId) as ByteArray;
                    if (!result) {
                        if (loader.onError) {
                            loader.onError(new Error("Error getting result"));
                        }
                    } else {
                        result.position = 0;
                        loader.onResult(result);
                    }
                }
                delete _loaders[loaderId];
                break;
            }
        }
    }
}
}
