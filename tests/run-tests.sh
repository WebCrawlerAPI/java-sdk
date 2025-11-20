#!/bin/bash

# WebCrawlerAPI Standalone SDK - Test Runner
# Runs all tests without Maven or Gradle

set -e  # Exit on error

echo "WebCrawlerAPI Standalone SDK - Test Runner"
echo "==========================================="
echo

# Check Java version
if ! command -v java &> /dev/null; then
    echo "Error: Java is not installed"
    echo "Please install Java 17 or newer"
    exit 1
fi

JAVA_VERSION=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}' | awk -F '.' '{print $1}')
if [ "$JAVA_VERSION" -lt 8 ] && [ "$JAVA_VERSION" != "1" ]; then
    echo "Error: Java 8 or newer is required"
    echo "Current version: $(java -version 2>&1 | head -n 1)"
    exit 1
fi

echo "Java version: $(java -version 2>&1 | head -n 1)"
echo

# Create bin directory
mkdir -p bin

# Copy SDK file
echo "Copying WebCrawlerAPI.java..."
cp ../WebCrawlerAPI.java .

# Compile test framework first
echo "Compiling test framework..."
javac -d bin SimpleTestFramework.java

# Compile SDK
echo "Compiling WebCrawlerAPI..."
javac -d bin WebCrawlerAPI.java

# Compile test files
echo "Compiling test files..."
javac -cp bin -d bin WebCrawlerAPITest.java IntegrationTest.java

if [ $? -ne 0 ]; then
    echo "Compilation failed!"
    exit 1
fi

echo "Compilation successful!"
echo

# Run unit tests
echo "=========================================="
echo "Running Unit Tests"
echo "=========================================="
echo
java -cp bin WebCrawlerAPITest
UNIT_EXIT=$?

echo
echo

# Run integration tests if API key is provided
if [ -n "$API_KEY" ]; then
    echo "=========================================="
    echo "Running Integration Tests"
    echo "=========================================="
    echo

    if [ -n "$API_BASE_URL" ]; then
        API_KEY="$API_KEY" API_BASE_URL="$API_BASE_URL" java -cp bin IntegrationTest
    else
        API_KEY="$API_KEY" java -cp bin IntegrationTest
    fi
    INTEGRATION_EXIT=$?

    echo
    echo
else
    echo "=========================================="
    echo "Skipping Integration Tests"
    echo "=========================================="
    echo "Set API_KEY environment variable to run integration tests"
    echo "Example: API_KEY=your-key ./run-tests.sh"
    echo
    INTEGRATION_EXIT=0
fi

# Final summary
echo "=========================================="
echo "Final Test Summary"
echo "=========================================="
echo "Unit Tests:        $([ $UNIT_EXIT -eq 0 ] && echo 'PASSED ✓' || echo 'FAILED ✗')"
echo "Integration Tests: $([ -n "$API_KEY" ] && ([ $INTEGRATION_EXIT -eq 0 ] && echo 'PASSED ✓' || echo 'FAILED ✗') || echo 'SKIPPED')"
echo "=========================================="

# Exit with failure if any tests failed
if [ $UNIT_EXIT -ne 0 ] || [ $INTEGRATION_EXIT -ne 0 ]; then
    exit 1
fi

exit 0
