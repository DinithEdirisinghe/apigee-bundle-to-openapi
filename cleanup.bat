@echo off
cd /d "C:\Apigee to openAPI Java Library\apigee-bundle-to-openapi"

echo Deleting unwanted files...
echo.

if exist "JSONPlaceholder-Todo-API-openapi.json" (
    del "JSONPlaceholder-Todo-API-openapi.json"
    echo Deleted: JSONPlaceholder-Todo-API-openapi.json
)

if exist "JSONPlaceholder-Todo-API-openapi.yaml" (
    del "JSONPlaceholder-Todo-API-openapi.yaml"
    echo Deleted: JSONPlaceholder-Todo-API-openapi.yaml
)

if exist "test-openapi.json" (
    del "test-openapi.json"
    echo Deleted: test-openapi.json
)

if exist "test-openapi.yaml" (
    del "test-openapi.yaml"
    echo Deleted: test-openapi.yaml
)

if exist "service-account.json" (
    del "service-account.json"
    echo Deleted: service-account.json
)

if exist "pom.xml.asc" (
    del "pom.xml.asc"
    echo Deleted: pom.xml.asc
)

if exist "run-test.bat" (
    del "run-test.bat"
    echo Deleted: run-test.bat
)

if exist "run-apigee-conversion.bat" (
    del "run-apigee-conversion.bat"
    echo Deleted: run-apigee-conversion.bat
)

if exist "TEST_RESULTS.md" (
    del "TEST_RESULTS.md"
    echo Deleted: TEST_RESULTS.md
)

if exist "IMPROVEMENTS_SUMMARY.md" (
    del "IMPROVEMENTS_SUMMARY.md"
    echo Deleted: IMPROVEMENTS_SUMMARY.md
)

echo.
echo === REMAINING FILES IN ROOT DIRECTORY ===
echo.
dir /B

pause
