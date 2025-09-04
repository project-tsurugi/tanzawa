# `tgdump` - Tsurugi Dump Tool

Tsurugi Dump Tool exports table data or query results to files.

## Build and Install

Execute the following command in this directory (`/modules/tgdump`):

```sh
../../gradlew assemble
```

This will create the following distribution archives:

* `cli/build/distributions/tgdump-<version>.zip`
* `cli/build/distributions/tgdump-<version>.tar.gz`
* `cli/build/distributions/tgdump-<version>-shadow.zip`
* `cli/build/distributions/tgdump-<version>-shadow.tar.gz`

Each archive contains the following contents:

* `tgdump-<version>(-shadow)/bin/tgdump`
  * Script for executing the command
  * Additionally, `tgdump.bat` is included for Windows users
* `tgdump-<version>(-shadow)/lib/*.jar`
  * Java libraries used by the command
  * For the `*-shadow` archives, these are packaged into a single "Uber JAR" file

After deploying the files above, you can use the tgdump command.

## Command Specification

```sh
tgdump <table-name> [<table-name> ...] --to </path/to/destination-dir> --connection <endpoint-uri> ...
tgdump --sql [<query-label>:]<query-text> [[<query-label>:]<query-text> ...] --to </path/to/destination-dir> --connection <endpoint-uri> ...
```

Examples:

```sh
# Export the tables "t1" and "t2" to /tmp/dump/{t1, t2}
tgdump t1 t2 --to /tmp/dump --connection ipc:tsurugi

# Export the query results to /tmp/sql/{a, b}
tgdump --sql "a: SELECT * FROM t1 WHERE k = 0" "b: SELECT * FROM t2 WHERE k = 1" --to /tmp/sql --connection ipc:tsurugi

# Export the table "t1" to /tmp/single without sub-directories
tgdump --single t1 --to /tmp/single --connection ipc:tsurugi

# Export the table "t1" to /tmp/arrow/t1 as Arrow format
tgdump t1 --profile arrow --to /tmp/arrow --connection ipc:tsurugi
```

Parameters:

* `<table-name>` - the name(s) of the table(s) to export
* `<query-text>` - the SQL statement(s) to execute for exporting data
* `--to` - the directory where export files will be saved
* `--connection` - the Tsurugi endpoint URI (only `ipc:` protocol is supported)

Optional Parameters:

* `--sql`
  * Use SQL statements for export operations instead of specifying table names.
  * Default: export by table names.
* `<query-label>`
  * A label for the corresponding SQL statement (`<query-text>`).
  * Only available when using the `--sql` option.
  * Default: automatically generated label.
* `--single`
  * Export a single table or query directly into the specified directory, without creating sub-directories.
  * Default: creates sub-directories for each table or query.
* `--profile`
  * Specifies the export profile name or file path.
  * Available profiles:
    * `default` - standard export (Apache Parquet format)
    * `Arrow` - export in Apache Arrow format
    * `Parquet` - export in Apache Parquet format
    * `PG-Strom` - export in Apache Arrow format for PG-Strom
  * Default: `default`
* `--connection-label`
  * The optional session label.
  * Default: no session labels.
* `--connection-timeout`
  * The timeout duration (in milliseconds) for establishing the connection.
  * Default: disable timeout.
* `--user`
  * Authentication user name.
  * See [Session Authentication](#session-authentication) section.
* `--auth-token`
  * Authentication token.
  * See [Session Authentication](#session-authentication) section.
* `--credentials`
  * Authentication credentials file.
  * See [Session Authentication](#session-authentication) section.
* `--no-auth`
  * Connect to the session without using authentication information.
  * See [Session Authentication](#session-authentication) section.
* `--transaction`
  * Transaction type for export operations.
  * Available transaction types:
    * `OCC` (or `short`)
    * `LTX` (or `long`)
    * `RTX` (or `read`, `readonly`, `read-only`)
  * Default: `RTX`
* `--transaction-label`
  * The transaction label.
  * Default: no transaction labels.
* `--threads`
  * The number of client threads used for export operations.
    * The number of tables or queries processed simultaneously is limited by this value.
  * Default: `1`
* `-v,--verbose`
  * Prints verbose messages during execution.

Special Parameters:

* `-h,--help`
  * Prints help messages and exits.
* `--version`
  * Prints version information and exits.

### Session Authentication

Session authentication verifies user credentials when establishing a session with Tsurugi.

The following CLI options are available for session authentication:

* `--user <username>`
  * Authenticate the session using a username and password.
  * The password is provided interactively via standard input.
* `--auth-token <token>`
  * Authenticate the session using a Harinoki authentication token.
* `--credentials <file>`
  * Authenticate the session using a specified credentials file.
* `--no-auth`
  * Connect to the session without using authentication information.

Only one of the above options can be specified at a time. Specifying multiple options will result in an error.

If none of the above options are specified, authentication will proceed as follows:

* If the environment variable `TSURUGI_AUTH_TOKEN` is set, its value will be used as the authentication token.
* If this variable is not set or session establishment fails, `~/.tsurugidb/credentials.key` will be used as the credentials file.
* If this file does not exist or session establishment fails, the session will be established without authentication information.
* If the session has not yet been successfully established, you will be prompted to enter your username and password interactively via standard input.
* If the username is not specified or session establishment fails, the program will output an error message and exit.
