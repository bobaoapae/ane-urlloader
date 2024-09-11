#!/bin/bash

# Ensure the IPA file is provided as an argument
if [ -z "$1" ]; then
    echo "Usage: $0 <path_to_ipa>"
    exit 1
fi

IPA_PATH="$1"
TEMP_DIR="/tmp/ipa_extracted"
PAYLOAD_DIR="$TEMP_DIR/Payload"
APP_DIR=""
CERTIFICATE=""

# Step 1: Extract the IPA
echo "Extracting IPA..."
mkdir -p "$TEMP_DIR"
unzip -q "$IPA_PATH" -d "$TEMP_DIR"

# Find the .app directory inside the Payload folder
APP_DIR=$(find "$PAYLOAD_DIR" -type d -name "*.app" | head -n 1)

if [ -z "$APP_DIR" ]; then
    echo "Error: Unable to find the .app directory in the IPA."
    exit 1
fi

# Step 2: Retrieve the signing certificate used
echo "Retrieving signing certificate..."
CERTIFICATE=$(codesign -d --verbose=4 "$APP_DIR" 2>&1 | grep -m 1 "Authority=" | sed 's/Authority=//')

if [ -z "$CERTIFICATE" ]; then
    echo "Error: Unable to retrieve the signing certificate from the app."
    exit 1
fi

echo "Certificate found: $CERTIFICATE"

# Step 3: Sign each framework and associated files in the .app with the same certificate
echo "Signing frameworks and associated files..."
FRAMEWORKS_DIR="$APP_DIR/Frameworks"

if [ -d "$FRAMEWORKS_DIR" ]; then
    for FRAMEWORK in "$FRAMEWORKS_DIR"/*; do
        if [ -d "$FRAMEWORK" ]; then
            echo "Signing $FRAMEWORK..."
            codesign --force --sign "$CERTIFICATE" --deep --preserve-metadata=identifier,entitlements "$FRAMEWORK"
            
            # Sign each .dylib or any other binary files inside the framework
            find "$FRAMEWORK" -type f \( -name "*.dylib" -o -name "*" \) | while read -r BINARY_FILE; do
                if file "$BINARY_FILE" | grep -q "Mach-O"; then
                    echo "Signing binary file $BINARY_FILE..."
                    codesign --force --sign "$CERTIFICATE" --preserve-metadata=identifier,entitlements "$BINARY_FILE"
                fi
            done
        fi
    done
else
    echo "No frameworks found to sign."
fi

# Sign the main .app directory
echo "Signing the main app..."
codesign --force --sign "$CERTIFICATE" --deep --preserve-metadata=identifier,entitlements "$APP_DIR"

# Step 4: Repack the IPA
echo "Repacking the IPA..."
cd "$TEMP_DIR"
zip -qr "repacked.ipa" "Payload"

# Move the repacked IPA to the folder of the original IPA
mv "repacked.ipa" "$(dirname "$IPA_PATH")/repacked.ipa"

# Cleanup
echo "Cleaning up..."
rm -rf "$TEMP_DIR"

echo "Done."