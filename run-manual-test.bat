@echo off
REM This batch file runs the ManualTest to convert Apigee proxy to OpenAPI specifications
REM Make sure service-account.json exists in the project root directory

echo Running ManualTest - Apigee to OpenAPI Conversion...
echo.

cd /d "%~dp0"

REM Run the Maven command
mvn test-compile exec:java -Dexec.mainClass="com.apigee.openapi.converter.ManualTest" -Dexec.classpathScope=test

echo.
echo Test execution completed.
pause
