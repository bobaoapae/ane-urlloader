#!/bin/bash

# Define the project path and output directories
PROJECT_PATH="../CSharpLibrary/UrlLoaderNativeLibrary/UrlLoaderNativeLibrary.csproj"
OUTPUT_DIR="../CSharpLibrary/UrlLoaderNativeLibrary/bin/Release/net9.0"
ARM64_DIR="$OUTPUT_DIR/osx-arm64/native"
X64_DIR="$OUTPUT_DIR/osx-x64/native"
IOS_ARM64_DIR="$OUTPUT_DIR/ios-arm64/native"
MACOS_UNIVERSAL_DIR="$OUTPUT_DIR/macos-universal"
IOS_UNIVERSAL_DIR="$OUTPUT_DIR/ios-universal"

# Specify the signing identity
SIGNING_IDENTITY="Developer ID Application: SURFTANK LTDA (QZPLNQ7VA7)"

# Ensure output directories exist
mkdir -p "$MACOS_UNIVERSAL_DIR"
mkdir -p "$IOS_UNIVERSAL_DIR"

# Build for ARM64 (macOS)
echo "Building for osx-arm64..."
dotnet publish -c Release -r osx-arm64 -p:NativeLib=Shared "$PROJECT_PATH"
if [ $? -ne 0 ]; then
  echo "Failed to build for osx-arm64"
  exit 1
fi

# Build for x64 (macOS)
echo "Building for osx-x64..."
dotnet publish -c Release -r osx-x64 -p:NativeLib=Shared "$PROJECT_PATH"
if [ $? -ne 0 ]; then
  echo "Failed to build for osx-x64"
  exit 1
fi

# Build for ARM64 (iOS)
echo "Building for ios-arm64..."
dotnet publish -c Release -r ios-arm64 -p:NativeLib=Shared -p:PublishAotUsingRuntimePack=true "$PROJECT_PATH"
if [ $? -ne 0 ]; then
  echo "Failed to build for ios-arm64"
  exit 1
fi

# Create a universal binary for macOS using lipo
ARM64_LIB="$ARM64_DIR/UrlLoaderNativeLibrary.dylib"
X64_LIB="$X64_DIR/UrlLoaderNativeLibrary.dylib"
MACOS_UNIVERSAL_LIB="$MACOS_UNIVERSAL_DIR/UrlLoaderNativeLibrary.dylib"

echo "Creating macOS universal binary..."
lipo -create -output "$MACOS_UNIVERSAL_LIB" "$ARM64_LIB" "$X64_LIB"
if [ $? -ne 0 ]; then
  echo "Failed to create macOS universal binary"
  exit 1
fi

# Verify the macOS universal binary
echo "Verifying macOS universal binary..."
file "$MACOS_UNIVERSAL_LIB"

# Fix the install name of the macOS dylib to be relative to the main framework library's location
echo "Fixing install name for macOS dylib..."
install_name_tool -id "@loader_path/Frameworks/UrlLoaderNativeLibrary.dylib" "$MACOS_UNIVERSAL_LIB"
if [ $? -ne 0 ]; then
  echo "Failed to fix the install name for macOS dylib"
  exit 1
fi

echo "Install name fixed successfully for macOS dylib."

# Sign the macOS universal binary
echo "Signing macOS universal binary..."
codesign --force --sign "$SIGNING_IDENTITY" --options=runtime --timestamp "$MACOS_UNIVERSAL_LIB"
if [ $? -ne 0 ]; then
  echo "Failed to sign the macOS universal binary"
  exit 1
fi

# Verify the signature for macOS
echo "Verifying signature for macOS binary..."
codesign --verify --deep --strict --verbose=2 "$MACOS_UNIVERSAL_LIB"
if [ $? -ne 0 ]; then
  echo "Signature verification failed for macOS binary"
  exit 1
fi

echo "macOS universal binary created and signed successfully: $MACOS_UNIVERSAL_LIB"

# Create a separate binary for iOS (ios-arm64)
IOS_ARM64_LIB="$IOS_ARM64_DIR/UrlLoaderNativeLibrary.dylib"
IOS_UNIVERSAL_LIB="$IOS_UNIVERSAL_DIR/UrlLoaderNativeLibrary.dylib"

echo "Copying iOS binary..."
cp "$IOS_ARM64_LIB" "$IOS_UNIVERSAL_LIB"
if [ $? -ne 0 ]; then
  echo "Failed to copy iOS binary"
  exit 1
fi

# Fix the install name of the iOS dylib to be relative to the main framework library's location
echo "Fixing install name for iOS dylib..."
install_name_tool -id "@executable_path/Frameworks/UrlLoaderANE_IOS_Wrapper.framework/Frameworks/UrlLoaderNativeLibrary.dylib" "$IOS_UNIVERSAL_LIB"
if [ $? -ne 0 ]; then
  echo "Failed to fix the install name for iOS dylib"
  exit 1
fi

echo "Install name fixed successfully for iOS dylib."

# Sign the iOS binary
echo "Signing iOS binary..."
codesign --force --sign "$SIGNING_IDENTITY" --options=runtime --timestamp "$IOS_UNIVERSAL_LIB"
if [ $? -ne 0 ]; then
  echo "Failed to sign the iOS binary"
  exit 1
fi

# Verify the signature for iOS
echo "Verifying signature for iOS binary..."
codesign --verify --deep --strict --verbose=2 "$IOS_UNIVERSAL_LIB"
if [ $? -ne 0 ]; then
  echo "Signature verification failed for iOS binary"
  exit 1
fi

echo "iOS binary signed successfully: $IOS_UNIVERSAL_LIB"
