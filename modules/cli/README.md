# Tsurugi SQL console CLI

This module provides a Java program entry for Tsurugi SQL console.

* [Main] - Executes SQL script files.

[Main]:src/main/java/com/tsurugidb/console/cli/Main.java

## Execute

### SQL console

```sh
# cd modules/cli
java -Dorg.slf4j.simpleLogger.defaultLogLevel=debug -jar build/libs/sql-console-*-all.jar -c tcp://localhost:12345
```

Please type `\help` to show available commands.

### execute a SQL statement

```sh
# cd modules/cli
java -Dorg.slf4j.simpleLogger.defaultLogLevel=debug -jar build/libs/sql-console-*-all.jar --exec -c tcp://localhost:12345 "select * from test"
```

### execute SQL script file

```sh
# cd modules/cli
java -Dorg.slf4j.simpleLogger.defaultLogLevel=debug -jar build/libs/sql-console-*-all.jar --script -c tcp://localhost:12345 -e UTF-8 /path/to/script.sql
```

## Program arguments

### common

* `--help, -h` - show help
* `--connection,-c` - connection url (`tcp://...`, `ipc://...`, etc. compliant with end-point URI of [SessionBuilder.connect](https://github.com/project-tsurugi/tsubakuro/blob/98fa342082af04cf927b875b9d898dd7961f575e/modules/session/src/main/java/com/nautilus_technologies/tsubakuro/low/common/SessionBuilder.java#L35-L45) )
* `--property,-P` - SQL properties (corresponds to `SET <key> TO <value>` , multiple specifications allowed)
* client variable
  * `-D` - client variable (corresponds to `\set <key> <value>`, multiple specifications allowed)
  * `--client-variable` - property file for client variable
* transaction
  * `--transaction,-t` - トtransaction type
    * `short, OCC` - OCC (default)
    * `long, LTX` - LTX ( `--write-preserve` is required for writing)
    * `read, readonly, RO` - read only
    * `manual` - do not start transactions automatically. execute `BEGIN TRANSACTION` explicitly.
  * `--write-preserve,-w` - table to write preserve (multiple specifications allowed)
  * `--read-area-include` - table to inclusive read area (multiple specifications allowed)
  * `--read-area-exclude` - table to exclusive read area (multiple specifications allowed)
  * `--execute` - (`PRIOR`|`EXCLUDING`) (`DEFERRABLE`|`IMMEDIATE`)?
  * `--label` - label
  * `--with` - transaction settings (multiple specifications allowed)
* credential

  * `--user` - user name
    * enter the password via the password prompt after booting.
  * `--auth-token` - authentication token
  * `--credentials` - credentials file path
  * `--no-auth` - do not use authentication mechanism
  * if none of `--user`, `--auth-token`, `--credentials`, `--no-auth`  is specified, solve in the following order
    1. if `TSURUGI_AUTH_TOKEN` is not empty, use that string as the authentication token.
    2. If there is a default credentials file, use it.
    3. do not use authentication mechanism. (if an authentication error occurs, proceed to step 4）
    4. display a prompt to enter the username, and use the entered string as the username if it is not empty. (then a password prompt will also be displayed)

### SQL console

```txt
java Main --console <common options> [--auto-commit|--no-auto-commit]
```

* `--auto-commit` - commit each statement
* `--no-auto-commit` - perform a commit only if you explicitly specify a `COMMIT`  (default)

### execute a SQL statement

```txt
java Main --exec <common options> [--commit|--no-commit] <statement>
```

* `<statement>` - statement to execute
* `--commit` - commit if the statement executes successfully, rollback if it fails (default)
* `--no-commit` - always rollback regardless of success or failure

### execute SQL script file

```txt
java Main --script <common options> [[--encoding|-e] <charset-encoding>] [--auto-commit|--no-auto-commit|--commit|--no-commit] </path/to/script.sql>
```

* `</path/to/script.sql>` - script file to execute
* `--encoding,-e` - character encoding of script files. if not specified, conforms to the execution environment
* `--auto-commit` - commit each statement
* `--no-auto-commit` - perform a commit only if you explicitly specify a `COMMIT` 
* `--commit` - commit if the statement executes successfully, rollback if it fails (default)
* `--no-commit` - always rollback regardless of success or failure



## How to build

```bash
cd /path/to/tanzawa/modules/cli/
../../gradlew shadowJar
ls build/libs/sql-console-*-all.jar
```

