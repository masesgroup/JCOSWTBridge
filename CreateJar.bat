@echo off

rmdir /Q /S output
mkdir output

".\jdk\bin\javac.exe" -cp "./bin/JCOBridge.jar;./bin/SWT.jar" -Werror -d ./output ./src/org/mases/jcobridge/swt/*.java
IF %ERRORLEVEL% NEQ 0 GOTO ERROR
@echo javac END
".\jdk\bin\javadoc.exe" -cp "./bin/JCOBridge.jar;./bin/SWT.jar" -quiet -author -public -d ./docs -link https://www.jcobridge.com/api-java ./src/org/mases/jcobridge/swt/*.java
IF %ERRORLEVEL% NEQ 0 GOTO ERROR
@echo javadoc END
".\jdk\bin\jar.exe" cvfm ./release/JCOSWTBridge.jar ./src/JCOSWTBridge.txt -C ./output .
IF %ERRORLEVEL% NEQ 0 GOTO ERROR
@echo jar JCOSWTBridge.jar END
"c:\Program Files\Java\%JDKVERSION%\bin\jar.exe" cvf ./release/JCOSWTBridge.docs.jar -C ./docs .
IF %ERRORLEVEL% NEQ 0 GOTO ERROR
@echo jar JCOSWTBridge.docs.jar END
GOTO END

:ERROR
@echo failed
PAUSE
exit /b 1
:END
exit /b 0