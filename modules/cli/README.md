# Tsurugi SQL console CLI

This module provides a Java program entry for Tsurugi SQL console.

* [Main] - Executes SQL script files.
  * program arguments
    * `-f` , `--file` - path to the script file
    * `-e` , `--endpoint` - Tsurugi OLTP server end-point URI

[Main]:src/main/java/com/tsurugidb/console/cli/Main.java

## Execute

```sh
# cd modules/cli
java -Dorg.slf4j.simpleLogger.defaultLogLevel=debug -jar build/libs/sql-console-*-all.jar -f /path/to/script.sql -e tcp://localhost:12345
```

or, read SQL script from standard input ( `-f` not specified, or specify `-f -` )

```sh
# cd modules/cli
java -Dorg.slf4j.simpleLogger.defaultLogLevel=debug -jar build/libs/sql-console-*-all.jar -e tcp://localhost:12345
```

Please type `\help` to show available commands.
