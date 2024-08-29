#!/bin/bash

# Define the project path and output directories
PROJECT_PATH="../CSharpLibrary/UrlLoaderNativeLibrary/UrlLoaderNativeLibrary.csproj"
OUTPUT_DIR="../CSharpLibrary/UrlLoaderNativeLibrary/bin/Release/net9.0"
ARM64_DIR="$OUTPUT_DIR/ios-arm64/native"

# Specify the signing identity
SIGNING_IDENTITY="Developer ID Application: SURFTANK LTDA (QZPLNQ7VA7)"

# Build for ARM64
echo "Building for ios-arm64..."
dotnet publish -c Release -r ios-arm64 -p:NativeLib=Shared -p:PublishAotUsingRuntimePack=true "$PROJECT_PATH"
if [ $? -ne 0 ]; then
  echo "Failed to build for ios-arm64"
  exit 1
fi

# Create a universal binary using lipo
ARM64_LIB="$ARM64_DIR/UrlLoaderNativeLibrary.dylib"

# Sign the universal binary
echo "Signing universal binary..."
codesign --force --sign "$SIGNING_IDENTITY" "$ARM64_LIB"
if [ $? -ne 0 ]; then
  echo "Failed to sign the universal binary"
  exit 1
fi

# Verify the signature
echo "Verifying signature..."
codesign --verify --deep --strict --verbose=2 "$ARM64_LIB"
if [ $? -ne 0 ]; then
  echo "Signature verification failed"
  exit 1
fi

echo "IOS binary created and signed successfully: $ARM64_LIB"
