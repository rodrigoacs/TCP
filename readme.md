compile:
    javac -d bin -cp "lib/lanterna-3.1.1.jar" src/*.java

run:
    server:java -cp bin Server

    client:
        linux: java -cp "bin:lib/lanterna-3.1.1.jar" Client
        windows: javaw -cp "bin;lib/lanterna-3.1.1.jar" Client