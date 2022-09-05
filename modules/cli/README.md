# Tsubakuro bootstrap entry

This module provides a Java program entry for Tsurugi SQL console.

* [Main] - Executes SQL script files.
  * program arguments
    * `[0]` - path to the script file
    * `[1]` - Tsurugi OLTP server end-point URI

Please refer about Tsurugi SQL console [console/README.md].

[Main]:src/main/java/com/tsurugidb/tsubakuro/bootstrap/Main.java
[console/README.md]:../console/README.md

## Build

```sh
./gradlew assemble
```

## Execute

```sh
# cd modules/bootstrap
java -Dorg.slf4j.simpleLogger.defaultLogLevel=debug -jar build/libs/tsubakuro-bootstrap-*-all.jar /path/to/script.sql tcp://localhost:12345
```

or, read SQL script from standard input (specify `-` to script path)

```sh
# cd modules/bootstrap
java -Dorg.slf4j.simpleLogger.defaultLogLevel=debug -jar build/libs/tsubakuro-bootstrap-*-all.jar - tcp://localhost:12345
```

Please type `\help` to show available commands.
