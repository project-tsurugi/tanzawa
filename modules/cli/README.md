# Tsurugi SQL console CLI

This module provides a Java program entry for Tsurugi SQL console.

* [Main] - Executes SQL script files.

[Main]:src/main/java/com/tsurugidb/console/cli/Main.java

## Execute

### SQL console

```sh
# cd modules/cli
java -Dorg.slf4j.simpleLogger.defaultLogLevel=debug -jar build/libs/sql-console-*-all.jar console -c tcp://localhost:12345
```

Please type `\help` to show available commands.

### execute a SQL statement

```sh
# cd modules/cli
java -Dorg.slf4j.simpleLogger.defaultLogLevel=debug -jar build/libs/sql-console-*-all.jar exec -c tcp://localhost:12345 "select * from test"
```

### execute SQL script file

```sh
# cd modules/cli
java -Dorg.slf4j.simpleLogger.defaultLogLevel=debug -jar build/libs/sql-console-*-all.jar script -c tcp://localhost:12345 -e UTF-8 /path/to/script.sql
```

## Program arguments

### common

* `--connection,-c` - 接続先URL (`tcp://...`, `ipc://...`, など、 [SessionBuilder.connect](https://github.com/project-tsurugi/tsubakuro/blob/98fa342082af04cf927b875b9d898dd7961f575e/modules/session/src/main/java/com/nautilus_technologies/tsubakuro/low/common/SessionBuilder.java#L35-L45) の end-point URI に準拠)
* `--property,-P` - SQL設定 (`SET <key> TO <value>` 相当, 複数指定可)
* `--java-property,-J` - Java Virtual Machine のオプションを指定する (複数指定可)
* transaction
  * `--transaction,-t` - トランザクションモード
    * `short, OCC` - OCC (default)
    * `long, LTX` - LTX (書き込みには `--write-preserve` の指定が必要)
    * `read, readonly, RO` - 読み込み専用
    * `manual` - トランザクションを自動で開始せず、 `BEGIN TRANSACTION` 文を指定する
  * `--write-preserve,-w` - 書き込み予約のテーブル (複数指定可)
* credential

  * `--user` - ユーザー名
    * 対応するパスワードは起動後にパスワードプロンプトを経由して入力する
  * `--auth-token` - 認証トークン
  * `--credentials` - 認証情報ファイルのパス
  * `--no-auth` - 認証機構を利用しない

### SQL console

```txt
java Main console <common options> [--auto-commit|--no-auto-commit]
```

* `--auto-commit` - 一文ごとにコミットを実行する
* `--no-auto-commit` - 明示的に `COMMIT` 文を指定した場合のみコミットを実行する (default)

### execute a SQL statement

```txt
java Main exec <common options> [--commit|--no-commit] <statement>
```

* `<statement>` - 実行する文
* `--commit` - 文の実行に成功したらコミットし、失敗したらロールバックする (default)
* `--no-commit` - 実行の成否にかかわらず、常にロールバックする

### execute SQL script file

```txt
java Main script <common options> [[--encoding|-e] <charset-encoding>] [--auto-commit|--no-auto-commit|--commit|--no-commit] </path/to/script.sql>
```

* `</path/to/script.sql>` - 実行するスクリプトファイル
* `--encoding,-e` - スクリプトファイルの文字エンコーディング, 未指定の場合は実行環境に準拠
* `--auto-commit` - 一文ごとにコミットを実行する
* `--no-auto-commit` - 明示的に `COMMIT` 文を指定した場合のみコミットを実行する
* `--commit` - すべての文の実行に成功したらコミットし、いずれかが失敗したらロールバックする (default)
* `--no-commit` - 実行の成否にかかわらず、常にロールバックする

