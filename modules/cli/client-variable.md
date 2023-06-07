# Tsurugi SQL console CLI client variable

クライアント変数（client variable）は、SQLコンソール内で使用する値。
値はユーザーが指定可能。



## クライアント変数の指定方法

### デフォルトファイルで指定する方法

SQLコンソール起動時に、ユーザーホームディレクトリー（Linuxの場合は`$HOME`, Windowsの場合は`%USERPROFILE%`）の下に`.tsurugidb/tgsql/client-variable.properties`があれば、それが読まれる。

このプロパティーファイルの中に`key=value`形式でクライアント変数を指定する。

### プログラム引数で指定する方法

プログラム引数に `-D<key>=<value>` 形式で指定する。

```bash
java -jar build/libs/sql-console-*-all.jar -Dkey1=value1 -D key2=value2
```

または、クライアント変数が記述されたプロパティーファイルを指定する。

```bash
java -jar build/libs/sql-console-*-all.jar --client-variable client-variable.properties
```

### 実行中に指定する方法

コマンドで指定する。

- `\show client [<key prefix>]`
  - 設定されているクライアント変数を一覧表示する。
  - `key prefix` を指定した場合は、変数名の先頭がそれに一致したものだけを表示する。
- `\set`
  - 設定されているクライアント変数を一覧表示する。
- `\set <key prefix>`
  - 指定されたクライアント変数の値を表示する。
- `\set <key> <value>`
  - クライアント変数に値を設定する。



## 主なクライアント変数

- `select.maxlines`
  - consoleモードでselect文を実行した結果を表示する件数。
    - 負の値の場合、無制限。
  - デフォルトは1000件。
- `sql.timing`
  - SQL文の実行時間の表示の有無。
  - `\timing` コマンドで切り替え可能。
- consoleモードの文字色。十六進数6桁rrggbbで指定する。
  - `console.info.color` - 情報メッセージの文字色
  - `console.implicit.color` - 暗黙メッセージの文字色
  - `console.succeed.color` - 成功メッセージの文字色
  - `console.warning.color` - 警告メッセージの文字色
  - `console.help.color` - ヘルプメッセージの文字色
- `dot.executable`
  - explain文で使用する、Graphvizのdotコマンドの場所（絶対パス）。



## その他

クライアント変数は [ScriptCvKey] や [ReplCvKey] で定義されている。

[ScriptCvKey]: ../core/src/main/java/com/tsurugidb/console/core/config/ScriptCvKey.java
[ReplCvKey]: ../cli/src/main/java/com/tsurugidb/console/cli/repl/ReplCvKey.java