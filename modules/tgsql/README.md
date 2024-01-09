# Tsurugi SQL console

The Tsurugi SQL console (`tgsql`) is an application designed for executing SQL commands directly on the Tsurugi server. It is available as a command-line tool, providing access to Tsurugi from various operating systems.

This tool encompasses three main modes:

* [SQL console mode](#launch-sql-console)
  * Execute SQL commands in a REPL environment
* [SQL statement mode](#execute-a-sql-statement)
  * Execute a single SQL statement
* [SQL script file mode](#execute-sql-script-file)
  * Execute a file containing one or more SQL statements

## Build and Install

Execute the below command in this directory (`/modules/tgsql`):

```sh
../../gradlew assemble
```

This will create the following distribution archives:

* `cli/build/distributions/tgsql-<version>.zip`
* `cli/build/distributions/tgsql-<version>.tar.gz`
* `cli/build/distributions/tgsql-<version>-shadow.zip`
* `cli/build/distributions/tgsql-<version>-shadow.tar.gz`

Each of the archives above contains the following contents:

* `tgsql-<version>(-shadow)/bin/tgsql`
  * A script for executing the command
  * Additionally, `tgsql.bat` is included for Windows users
* `tgsql-<version>(-shadow)/lib/*.jar`
  * Java libraries used by the command
  * For the -shadow archives, the above is packaged into a single "Uber JAR" file

Deploy the files mentioned above, and the `tgsql` command will be available for use.

## Execute

### Launch SQL console

```sh
tgsql -c tcp://localhost:12345
```

Please type `\help` to show available commands.

### Execute a SQL statement

```sh
tgsql --exec -c tcp://localhost:12345 "select * from test"
```

### Execute SQL script file

```sh
tgsql --script -c tcp://localhost:12345 -e UTF-8 /path/to/script.sql
```

## Program arguments

### Arguments common to all modes

* `--help, -h` - show help
* `--connection,-c` - connection URL (`tcp://...`, `ipc://...`, etc. compliant with end-point URI of [SessionBuilder.connect](https://github.com/project-tsurugi/tsubakuro/blob/98fa342082af04cf927b875b9d898dd7961f575e/modules/session/src/main/java/com/nautilus_technologies/tsubakuro/low/common/SessionBuilder.java#L35-L45) )
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
* credentials
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

### Arguments for SQL console mode

```sh
tgsql --console <common options> [--auto-commit|--no-auto-commit]
```

* `--auto-commit` - commit each statement
* `--no-auto-commit` - perform a commit only if you explicitly specify a `COMMIT`  (default)

If a transaction is started implicitly, commit for each statement.

### Arguments for SQL statement mode

```sh
tgsql --exec <common options> [--commit|--no-commit] <statement>
```

* `<statement>` - statement to execute
* `--commit` - commit if the statement executes successfully, rollback if it fails (default)
* `--no-commit` - always rollback regardless of success or failure

### Arguments for SQL script file mode

```sh
tgsql --script <common options> [[--encoding|-e] <charset-encoding>] [--auto-commit|--no-auto-commit|--commit|--no-commit] </path/to/script.sql>
```

* `</path/to/script.sql>` - script file to execute
* `--encoding,-e` - character encoding of script files. if not specified, conforms to the execution environment
* `--auto-commit` - commit each statement
* `--no-auto-commit` - perform a commit only if you explicitly specify a `COMMIT` 
* `--commit` - commit if the statement executes successfully, rollback if it fails (default)
* `--no-commit` - always rollback regardless of success or failure

## Grammar rules

see [docs/grammar-rule.md](../../docs/grammar-rule.md).
