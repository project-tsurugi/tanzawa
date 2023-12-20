# `tgdump` - Tsurugi Table Dump Tool

Tsurugi Table Dump Tool extracts the contents of table data and saves them on files.

## command specification

```sh
tgdump <table-name> [<table-name> [...]] --to </path/to/destination-dir> --connection <endpoint-uri> ...
```

parameters:

* `<table-name>` - the dump target table names
* `--to` - the destination directory
* `--connection` - the target Tsurugi endpoint URI (must be `ipc:` protocol)

optional parameters:

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
    * The number of simultaneous processing tables is limited to the number of this threads.
  * default: `1`
* `-v,--verbose`
  * print verbose messages during executions.

special parameters:

* `-h,--help`
  * print the help messages and then exit.
* `--version`
  * print the version information and then exit.
