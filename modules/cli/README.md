# Tsurugi SQL console CLI

This module provides a Java program entry for Tsurugi SQL console.

* [Main] - Executes SQL script files.
  * program arguments
    * `[0]` - path to the script file
    * `[1]` - Tsurugi OLTP server end-point URI

[Main]:src/main/java/com/tsurugidb/console/cli/Main.java

## Execute

```sh
# cd modules/cli
java -Dorg.slf4j.simpleLogger.defaultLogLevel=debug -jar build/libs/tanzawa-cli-*-all.jar /path/to/script.sql tcp://localhost:12345
```

or, read SQL script from standard input (specify `-` to script path)

```sh
# cd modules/cli
java -Dorg.slf4j.simpleLogger.defaultLogLevel=debug -jar build/libs/tanzawa-cli-*-all.jar - tcp://localhost:12345
```

Please type `\help` to show available commands.
