using System;
using System.Collections.Generic;
using System.Runtime.CompilerServices;
using System.Runtime.InteropServices;
using System.Text.Json;

namespace UrlLoaderNativeLibrary;

public static unsafe class ExportFunctions
{
    [UnmanagedFunctionPointer(CallingConvention.Cdecl)]
    private delegate void CallBackSuccessPointer(IntPtr pointerId, IntPtr pointerArray, int length);

    [UnmanagedFunctionPointer(CallingConvention.Cdecl)]
    private delegate void CallBackErrorPointer(IntPtr pointerId, IntPtr pointerMessage);

    [UnmanagedFunctionPointer(CallingConvention.Cdecl)]
    private delegate void CallBackProgressPointer(IntPtr pointerId, IntPtr pointerMessage);

    [UnmanagedFunctionPointer(CallingConvention.Cdecl)]
    private delegate void CallBackLogPointer(IntPtr pointerMessage);

    private static CallBackSuccessPointer _callbackSuccess;
    private static CallBackErrorPointer _callbackError;
    private static CallBackProgressPointer _callbackProgress;
    private static CallBackLogPointer _callbackLog;

    [UnmanagedCallersOnly(EntryPoint = "initializerLoader", CallConvs = [typeof(CallConvCdecl)])]
    public static int InitializerLoader(IntPtr pointerCallBackSuccess, IntPtr pointerCallBackError, IntPtr pointerCallBackProgress, IntPtr pointerCallBackLog)
    {
        var result = -1;
        try
        {
            _callbackSuccess = Marshal.GetDelegateForFunctionPointer<CallBackSuccessPointer>(pointerCallBackSuccess);
            _callbackError = Marshal.GetDelegateForFunctionPointer<CallBackErrorPointer>(pointerCallBackError);
            _callbackProgress = Marshal.GetDelegateForFunctionPointer<CallBackProgressPointer>(pointerCallBackProgress);
            _callbackLog = Marshal.GetDelegateForFunctionPointer<CallBackLogPointer>(pointerCallBackLog);

            void WrapperSuccess(string loaderId, byte[] dataLoaded)
            {
                IntPtr ptr1 = Marshal.StringToCoTaskMemAnsi(loaderId);
                IntPtr ptr2 = Marshal.AllocCoTaskMem(dataLoaded.Length);
                Marshal.Copy(dataLoaded, 0, ptr2, dataLoaded.Length);

                _callbackSuccess(ptr1, ptr2, dataLoaded.Length);

                Marshal.FreeCoTaskMem(ptr1);
                Marshal.FreeCoTaskMem(ptr2);
            }

            void WrapperError(string loaderId, string error)
            {
                IntPtr ptr1 = Marshal.StringToCoTaskMemAnsi(loaderId);
                IntPtr ptr2 = Marshal.StringToCoTaskMemAnsi(error);

                _callbackError(ptr1, ptr2);

                Marshal.FreeCoTaskMem(ptr1);
                Marshal.FreeCoTaskMem(ptr2);
            }

            void WrapperProgress(string loaderId, string progress)
            {
                IntPtr ptr1 = Marshal.StringToCoTaskMemAnsi(loaderId);
                IntPtr ptr2 = Marshal.StringToCoTaskMemAnsi(progress);

                _callbackProgress(ptr1, ptr2);

                Marshal.FreeCoTaskMem(ptr1);
                Marshal.FreeCoTaskMem(ptr2);
            }

            void WrapperLog(string message)
            {
                IntPtr ptr = Marshal.StringToCoTaskMemAnsi(message);
                _callbackLog(ptr);
                Marshal.FreeCoTaskMem(ptr);
            }

            LoaderManager.Instance.Initialize(WrapperSuccess, WrapperError, WrapperProgress, WrapperLog);
            result = 1;
        }
        catch (Exception e)
        {
            try
            {
                _callbackLog = Marshal.GetDelegateForFunctionPointer<CallBackLogPointer>(pointerCallBackLog);

                void WrapperLog(string message)
                {
                    IntPtr ptr = Marshal.StringToCoTaskMemAnsi(message);
                    _callbackLog(ptr);
                    Marshal.FreeCoTaskMem(ptr);
                }

                LogAll(e, WrapperLog);
            }
            catch
            {
                // ignored
            }

            result = -2;
        }

        return result;
    }
    
    [UnmanagedCallersOnly(EntryPoint = "addStaticHost", CallConvs = [typeof(CallConvCdecl)])]
    public static void AddStaticHost(IntPtr hostPtr, IntPtr ipPtr)
    {
        try
        {
            var host = Marshal.PtrToStringAnsi(hostPtr);
            var ip = Marshal.PtrToStringAnsi(ipPtr);

            LoaderManager.Instance.AddStaticHost(host, ip);
        }
        catch
        {
            // ignored
        }
    }
    
    [UnmanagedCallersOnly(EntryPoint = "removeStaticHost", CallConvs = [typeof(CallConvCdecl)])]
    public static void RemoveStaticHost(IntPtr hostPtr)
    {
        try
        {
            var host = Marshal.PtrToStringAnsi(hostPtr);

            LoaderManager.Instance.RemoveStaticHost(host);
        }
        catch
        {
            // ignored
        }
    }

    [UnmanagedCallersOnly(EntryPoint = "startLoad", CallConvs = [typeof(CallConvCdecl)])]
    public static IntPtr StartLoad(IntPtr urlPtr, IntPtr methodPtr, IntPtr variablesPtr, IntPtr headersPtr)
    {
        try
        {
            var url = Marshal.PtrToStringAnsi(urlPtr);
            var method = Marshal.PtrToStringAnsi(methodPtr);
            var variables = Marshal.PtrToStringAnsi(variablesPtr);
            var headers = Marshal.PtrToStringAnsi(headersPtr);

            var variablesDictionary = string.IsNullOrEmpty(variables) ? new Dictionary<string, string>() : JsonSerializer.Deserialize(variables, JsonDictionaryHeaderContext.Default.DictionaryStringString);
            var headersDictionary = string.IsNullOrEmpty(headers) ? new Dictionary<string, string>() : JsonSerializer.Deserialize(headers, JsonDictionaryHeaderContext.Default.DictionaryStringString);

            var randomId = LoaderManager.Instance.StartLoad(url, method, variablesDictionary, headersDictionary);
            return Marshal.StringToCoTaskMemAnsi(randomId);
        }
        catch
        {
            return IntPtr.Zero;
        }
    }

    //free id return from startLoad
    [UnmanagedCallersOnly(EntryPoint = "freeId", CallConvs = [typeof(CallConvCdecl)])]
    public static void FreeId(IntPtr idPtr)
    {
        Marshal.FreeCoTaskMem(idPtr);
    }

    private static void LogAll(Exception exception, Action<string> callback)
    {
        if (exception == null)
            return;

        try
        {
            var logBuilder = new System.Text.StringBuilder();

            // Log the main exception
            logBuilder.AppendLine($"Exception: {exception.Message}");
            logBuilder.AppendLine($"Stack Trace: {exception.StackTrace}");

            var inner = exception.InnerException;
            while (inner != null)
            {
                logBuilder.AppendLine($"Inner Exception: {inner.Message}");
                logBuilder.AppendLine($"Inner Stack Trace: {inner.StackTrace}");
                inner = inner.InnerException;
            }

            // Call _writeLog once with the complete log string
            callback(logBuilder.ToString());
        }
        catch (Exception)
        {
            // ignored
        }
    }
}