@echo off
REM SSH copy for macOS and iOS libraries (before packaging)
echo Copying macOS and iOS libraries via SSH

REM Delete the local macOS framework folder before copying
if exist .\macos\UrlLoaderANE.framework (
    rmdir /S /Q .\macos\UrlLoaderANE.framework
)

REM Copying macOS framework
scp -r joaovitorborges@192.168.80.102:/Users/joaovitorborges/IdeaProjects/ane-urlloader/macNative/DerivedData/UrlLoaderANE/Build/Products/Debug/UrlLoaderANE.framework/Versions/Current .\macos\UrlLoaderANE.framework

REM Delete the local iOS framework folder before copying
if exist .\ios\UrlLoaderANE_IOS_Wrapper.framework (
    rmdir /S /Q .\ios\UrlLoaderANE_IOS_Wrapper.framework
)
if exist .\ios\Frameworks (
    rmdir /S /Q .\ios\Frameworks
)

mkdir .\ios\Frameworks
REM Copying iOS framework
scp -r joaovitorborges@192.168.80.102:/Users/joaovitorborges/IdeaProjects/ane-urlloader/macNative/DerivedData/UrlLoaderANE/Build/Products/Debug-iphoneos/UrlLoaderANE_IOS_Wrapper.framework .\ios\UrlLoaderANE_IOS_Wrapper.framework
scp -r joaovitorborges@192.168.80.102:/Users/joaovitorborges/IdeaProjects/ane-urlloader/macNative/DerivedData/UrlLoaderANE/Build/Products/Debug-iphoneos/UrlLoaderANE_IOS_Wrapper.framework .\ios\Frameworks\UrlLoaderANE_IOS_Wrapper.framework
scp joaovitorborges@192.168.80.102:/Users/joaovitorborges/IdeaProjects/ane-urlloader/macNative/DerivedData/UrlLoaderANE/Build/Products/Debug-iphoneos/libUrlLoaderANE-IOS.a .\ios\libUrlLoaderANE-IOS.a
