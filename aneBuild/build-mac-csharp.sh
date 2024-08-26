#!/bin/bash

# Define the project path and output directories
PROJECT_PATH="../CSharpLibrary/UrlLoaderNativeLibrary/UrlLoaderNativeLibrary.csproj"
OUTPUT_DIR="../CSharpLibrary/UrlLoaderNativeLibrary/bin/Release/net9.0"
ARM64_DIR="$OUTPUT_DIR/osx-arm64/native"
X64_DIR="$OUTPUT_DIR/osx-x64/native"
UNIVERSAL_DIR="$OUTPUT_DIR/universal"

# Specify the signing identity
SIGNING_IDENTITY="Developer ID Application: SURFTANK LTDA (QZPLNQ7VA7)"

# Ensure output directory exists
mkdir -p "$UNIVERSAL_DIR"

# Build for ARM64
echo "Building for osx-arm64..."
dotnet publish -c Release -r osx-arm64 -p:NativeLib=Shared "$PROJECT_PATH"
if [ $? -ne 0 ]; then
  echo "Failed to build for osx-arm64"
  exit 1
fi

# Build for x64
echo "Building for osx-x64..."
dotnet publish -c Release -r osx-x64 -p:NativeLib=Shared "$PROJECT_PATH"
if [ $? -ne 0 ]; then
  echo "Failed to build for osx-x64"
  exit 1
fi

# Create a universal binary using lipo
ARM64_LIB="$ARM64_DIR/UrlLoaderNativeLibrary.dylib"
X64_LIB="$X64_DIR/UrlLoaderNativeLibrary.dylib"
UNIVERSAL_LIB="$UNIVERSAL_DIR/UrlLoaderNativeLibrary.dylib"

echo "Creating universal binary..."
lipo -create -output "$UNIVERSAL_LIB" "$ARM64_LIB" "$X64_LIB"
if [ $? -ne 0 ]; then
  echo "Failed to create universal binary"
  exit 1
fi

# Sign the universal binary
echo "Signing universal binary..."
codesign --force --sign "$SIGNING_IDENTITY" "$UNIVERSAL_LIB"
if [ $? -ne 0 ]; then
  echo "Failed to sign the universal binary"
  exit 1
fi

# Verify the signature
echo "Verifying signature..."
codesign --verify --deep --strict --verbose=2 "$UNIVERSAL_LIB"
if [ $? -ne 0 ]; then
  echo "Signature verification failed"
  exit 1
fi

# Verify the universal binary
echo "Verifying universal binary..."
file "$UNIVERSAL_LIB"

echo "Universal binary created and signed successfully: $UNIVERSAL_LIB"
