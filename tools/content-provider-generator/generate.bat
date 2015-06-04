set DIRNAME=%~dp0
if "%DIRNAME%" == "" set DIRNAME=.

java -jar android_contentprovider_generator-1.9.2-bundle.jar -i %DIRNAME%\data -o %DIRNAME%\..\..\app\src\main\java

pause
