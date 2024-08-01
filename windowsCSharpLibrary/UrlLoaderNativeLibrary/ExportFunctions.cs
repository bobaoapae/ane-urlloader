using System;
using System.Collections.Generic;
using System.Runtime.CompilerServices;
using System.Runtime.InteropServices;
using System.Text.Json;
using System.Text.Json.Serialization;

namespace UrlLoaderNativeLibrary;

public static unsafe class ExportFunctions
{
    [UnmanagedFunctionPointer(CallingConvention.Cdecl)]
    private delegate void FunctionPointer(IntPtr pointer1, IntPtr pointer2);

    [UnmanagedFunctionPointer(CallingConvention.Cdecl)]
    private delegate void FunctionPointer2(IntPtr pointer1, IntPtr pointer2, int length);

    private static FunctionPointer2 _fp1;
    private static FunctionPointer _fp2;
    private static FunctionPointer _fp3;

    [UnmanagedCallersOnly(EntryPoint = "initializerLoader")]
    public static int InitializerLoader(IntPtr fp1Ptr, IntPtr fp2Ptr, IntPtr fp3Ptr)
    {
        var result = -1;
        try
        {
            _fp1 = Marshal.GetDelegateForFunctionPointer<FunctionPointer2>(fp1Ptr);
            _fp2 = Marshal.GetDelegateForFunctionPointer<FunctionPointer>(fp2Ptr);
            _fp3 = Marshal.GetDelegateForFunctionPointer<FunctionPointer>(fp3Ptr);

            // Converter FunctionPointer para Action<string, string>
            Action<string, byte[]> action1 = (str1, str2) =>
            {
                IntPtr ptr1 = Marshal.StringToCoTaskMemAnsi(str1);
                IntPtr ptr2 = Marshal.AllocCoTaskMem(str2.Length);
                Marshal.Copy(str2, 0, ptr2, str2.Length);

                _fp1(ptr1, ptr2, str2.Length);

                Marshal.FreeCoTaskMem(ptr1);
                Marshal.FreeCoTaskMem(ptr2);
            };

            Action<string, string> action2 = (str1, str2) =>
            {
                IntPtr ptr1 = Marshal.StringToCoTaskMemAnsi(str1);
                IntPtr ptr2 = Marshal.StringToCoTaskMemAnsi(str2);

                _fp2(ptr1, ptr2);

                Marshal.FreeCoTaskMem(ptr1);
                Marshal.FreeCoTaskMem(ptr2);
            };

            Action<string, string> action3 = (str1, str2) =>
            {
                IntPtr ptr1 = Marshal.StringToCoTaskMemAnsi(str1);
                IntPtr ptr2 = Marshal.StringToCoTaskMemAnsi(str2);

                _fp3(ptr1, ptr2);

                Marshal.FreeCoTaskMem(ptr1);
                Marshal.FreeCoTaskMem(ptr2);
            };

            LoaderManager.Instance.Initialize(action1, action2, action3);
            result = 1;
        }
        catch
        {
            result = -2;
        }

        return result;
    }

    [UnmanagedCallersOnly(EntryPoint = "startLoad")]
    public static IntPtr StartLoad(IntPtr urlPtr, IntPtr methodPtr, IntPtr variablesPtr, IntPtr headersPtr)
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

    [UnmanagedCallersOnly(EntryPoint = "freeResult")]
    public static void FreeResult(IntPtr resultPtr)
    {
        Marshal.FreeCoTaskMem(resultPtr);
    }

    //free id return from startLoad
    [UnmanagedCallersOnly(EntryPoint = "freeId")]
    public static void FreeId(IntPtr idPtr)
    {
        Marshal.FreeCoTaskMem(idPtr);
    }
}