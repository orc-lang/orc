rem @ECHO OFF
REM windows command file

set BatchPath=%~dp0%

java -cp "%BatchPath%/lib/orc.jar;%BatchPath%/lib/antlr.jar;%BatchPath%/lib/mail.jar;%BatchPath%/lib/activation.jar" orc.Orc %1
