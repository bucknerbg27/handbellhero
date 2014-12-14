@echo off
rem Script that helps make this project more accessible for
rem non-technical people, without adding dependencies on Ant or Maven

set COMMONS_IO_JAR=resources\commons-io-2.4.jar
set CLASSES_DIR=classes
set JAVA_OPTS=

if "%1" == "" GOTO Run

if "%1" == "log" GOTO Log

if "%1" == "debug" GOTO Debug

if "%1" == "compile" GOTO Compile

echo Unknown command-line option: %1
goto End

:Run
set JAVA_EXEC=java
goto RunExec

:Log
set JAVA_OPTS=-Ddebug=true
goto Run

:Debug
set JAVA_EXEC=jdb
goto RunExec

:MkdirClasses
mkdir classes
goto CompileExec

:Compile
if not exist classes goto MkdirClasses
del classes\*.class 2> nul
:CompileExec
javac -g -classpath %COMMONS_IO_JAR% -d %CLASSES_DIR% src\*.java
goto End


:RunExec
%JAVA_EXEC% %JAVA_OPTS% -classpath %CLASSES_DIR%;%COMMONS_IO_JAR% MenuScreen


:End
