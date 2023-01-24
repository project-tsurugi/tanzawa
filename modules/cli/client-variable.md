# Tsurugi SQL console CLI client variable

クライアント変数（client variable）は、SQLコンソール内で使用する値。
値はユーザーが指定可能。



## クライアント変数の指定方法

### プログラム引数で指定する方法

プログラム引数に `-D<key>=<value>` 形式で指定する。

```bash
java -jar build/libs/sql-console-*-all.jar console -Dkey1=value1 -D key2=value2
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
  - SQLコンソールでselect文を実行した結果を表示する件数。
    - 負の値の場合、無制限。
  - デフォルトは、consoleモードの場合は1000件。それ以外の場合は無制限。
- `dot.executable`
  - explain文で使用する、Graphvizのdotコマンドの場所（絶対パス）。



## その他

クライアント変数は [ScriptCvKey] で定義されている。

[ScriptCvKey]: ../core/src/main/java/com/tsurugidb/console/core/config/ScriptCvKey.java