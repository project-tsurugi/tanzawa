# `tgdump` - Tsurugi Dump Tool

Tsurugi Dump Tool extracts the contents of table data or query result and saves them on files.

## Build and Install

Execute the below command in this directory (`/modules/tgdump`):

```sh
../../gradlew assemble
```

This will create the following distribution archives:

* `cli/build/distributions/tgdump-<version>.zip`
* `cli/build/distributions/tgdump-<version>.tar.gz`
* `cli/build/distributions/tgdump-<version>-shadow.zip`
* `cli/build/distributions/tgdump-<version>-shadow.tar.gz`

Each of the archives above contains the following contents:

* `tgdump-<version>(-shadow)/bin/tgdump`
  * A script for executing the command
  * Additionally, `tgdump.bat` is included for Windows users
* `tgdump-<version>(-shadow)/lib/*.jar`
  * Java libraries used by the command
  * For the -shadow archives, the above is packaged into a single "Uber JAR" file

Deploy the files mentioned above, and the tgdump command will be available for use.

## Command specification

```sh
tgdump <table-name> [<table-name> [...]] --to </path/to/destination-dir> --connection <endpoint-uri> ...
tgdump --sql [<query-label>:]<query-text> [[<query-label>:]<query-string> [...]] --to </path/to/destination-dir> --connection <endpoint-uri> ...
```

examples:

```sh
# Dump the contents of the tables "t1" and "t2" to /tmp/dump/{t1, t2}
tgdump t1 t2 --to /tmp/dump --connection ipc://tsurugi 

# Dump the query results to /tmp/sql/{a, b}
tgdump --sql "a: SELECT * FROM t1 WHERE k = 0" "b: SELECT * FROM t2 WHERE k = 1" --to /tmp/sql --connection ipc://tsurugi

# Dump the contents of the table "t1" to /tmp/single without sub-directories
tgdump --single t1 --to /tmp/single --connection ipc://tsurugi

# Dump the contents of the table "t1" to /tmp/arrow/t1 as Arrow format
tgdump t1 --profile arrow --to /tmp/arrow --connection ipc://tsurugi 
```

parameters:

* `<table-name>` - the dump target table names
* `<query-text>` - the SQL text for dump operations
* `--to` - the destination directory
* `--connection` - the target Tsurugi endpoint URI (must be `ipc:` protocol)

optional parameters:

* `--sql`
  * use SQL text for dump operations instead of table names
  * default: use table names
* `<query-label>`
  * the label for the accompanying SQL text (`<query-text>`)
  * default: auto-generated label
* `--profile`
  * dump profile name / file path
  * available profile name:
    * `default` - dump without any special settings
  * default: `default`
* `--connection-label` (doesn't work)
  * the session label
  * default: no session labels
* `--connection-timeout`
  * the timeout duration (in milliseconds) until the connection was established
  * default: disable timeout
* `--transaction`
  * transaction type of dump operations
  * available transaction types:
    * `OCC` (or `short`)
    * `LTX` (or `long`)
    * `RTX` (or `read`, `readonly`, `read-only`)
  * default: `RTX`
* `--transaction-label`
  * the transaction label
  * default: no transaction labels
* `--threads`
  * the number of client threads in dump operations
    * The number of simultaneous processing tables/queries is limited to the number of this threads.
  * default: `1`
* `-v,--verbose`
  * print verbose messages during executions.

special parameters:

* `-h,--help`
  * print the help messages and then exit.
* `--version`
  * print the version information and then exit.
