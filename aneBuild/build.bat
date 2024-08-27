@echo off
REM Set the path to the 7z.exe file
set PATH=%PATH%;"C:\Program Files\7-Zip\"
xcopy ..\androidNative\app\build\outputs\aar\app-debug.aar app-debug.aar /Y /F
7z e app-debug.aar classes.jar -o./android -aoa
xcopy ..\out\production\AneUrlLoader\AneUrlLoader.swc library.swc /Y /F
xcopy ..\windowsNative\cmake-build-release-x32\windowsNative.dll .\windows-32\windowsNative.dll /Y /F
dotnet publish /p:NativeLib=Shared /p:Configuration=Release ..\CSharpLibrary\UrlLoaderNativeLibrary\UrlLoaderNativeLibrary.csproj
xcopy ..\CSharpLibrary\UrlLoaderNativeLibrary\bin\Release\net9.0\win-x86\publish\UrlLoaderNativeLibrary.dll .\windows-32\UrlLoaderNativeLibrary.dll /Y /F
7z e library.swc library.swf -o./default -aoa
7z e library.swc library.swf -o./android -aoa
7z e library.swc library.swf -o./windows-32 -aoa
7z e library.swc library.swf -o./macos -aoa
adt -package -target ane br.com.redesurftank.aneurlloader.ane extension.xml -swc library.swc -platform default -C default . -platform Android-ARM -platformoptions platformAndroid.xml -C android . -platform Android-ARM64 -platformoptions platformAndroid.xml -C android . -platform Android-x86 -platformoptions platformAndroid.xml -C android . -platform Android-x64 -platformoptions platformAndroid.xml -C android . -platform Windows-x86 -C windows-32 . -platform MacOS-x86-64 -C macos .