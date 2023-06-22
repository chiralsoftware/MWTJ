# One hour file sharing

This simple application allows short snips of text and files to be uploaded
and shared for one hour. This is practical for moving a document, sharing an SSH
key, or sharing an image. No login or account is needed. It can be used from both
the web interface and `curl`.

# Native build

This project is also designed to show how native build works.
Make sure GraalVM is installed. Set the `JAVA_HOME` environment variable
to point to the GraalVM directory:

    export JAVA_HOME=/opt/graalvm-jdk-20.0.1+9.1

Use maven to build:

    mvn native:compile -Pnative 

It now runs as a native application. One key part of this demonstration
shows how to compile in classpath resources. See the file
`MyImportRuntimeHints.java` for this example.