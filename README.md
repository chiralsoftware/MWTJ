# One hour file sharing

This simple application allows short snips of text and files to be uploaded
and shared for one hour. This is practical for moving a document, sharing an SSH
key, or sharing an image. No login or account is needed. It can be used from both
the web interface and `curl`.

# Native build

This project is also designed to show how native build works.
Make sure GraalVM is installed by downloading the GraalVM for JDK from
https://www.oracle.com/java/technologies/downloads/. Set the 
`JAVA_HOME` environment variable to point to the GraalVM directory:

    export JAVA_HOME=/opt/graalvm-jdk-20.0.1+9.1

Use maven to build:

    mvn native:compile -Pnative 

It now runs as a native application. One key part of this demonstration
shows how to compile in classpath resources. See the file
`MyImportRuntimeHints.java` for this example.

# Notes on GraalVM

The Spring Native Image documentation recommends using SDKMAN to install
the GraalVM. This is not necessary anymore. The easiest way to install
the GraalVM is to simply install it from the Oracle JDK download site
like any other JVM, then set `JAVA_HOME` as above. No further configuration
or installation is necessary.

# Problems with native image

WebJar Locator doesn't work currently with native image so this
application can't use that feature. Other features which do not work
are BouncyCastle and possibly Thymeleaf Layout Dialect, so native image
isn't quite ready for use on larger projects.
