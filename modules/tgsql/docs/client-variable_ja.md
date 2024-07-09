# Tsurugi SQL console client variable

クライアント変数は、Tsurugi SQLコンソール（tgsql）内で使用する値。
値はユーザーが指定可能。



## クライアント変数の指定方法

### デフォルトファイルで指定する方法

tgsql起動時に、ユーザーのホームディレクトリー（Linuxの場合は`$HOME`, Windowsの場合は`%USERPROFILE%`）の下に`.tsurugidb/tgsql/client-variable.properties`があれば、それが読まれる。

このプロパティーファイルの中に`key=value`形式でクライアント変数を指定する。

### プログラム引数で指定する方法

プログラム引数に `-D<key>=<value>` あるいは `-D <key>=<value>` 形式で指定する。

```bash
tgsql -Dkey1=value1 -D key2=value2
```

または、クライアント変数が記述されたプロパティーファイルを指定する。

```bash
tgsql --client-variable client-variable.properties
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

### コンソール関連

| 変数名                   | 説明                                       | データ型 | デフォルト |
| ------------------------ | ------------------------------------------ | -------- | ------------ |
| `select.maxlines`        | select文を実行した結果を表示する件数<br />負の値の場合、無制限 | int      | コンソールモードの場合、1000<br />それ以外は-1 |
| `sql.timing`     | SQL文の実行時間の表示の有無<br />`\timing` コマンドで切り替え可能 | boolean | コンソールモードの場合、true<br />それ以外はfalse |
| `console.info.color` | 情報メッセージの文字色 | color | 前景色 |
| `console.implicit.color` | 暗黙メッセージの文字色                     | color    | 黄色         |
| `console.succeed.color`  | 成功メッセージの文字色                     | color    | 緑色         |
| `console.warinig.color`  | 警告メッセージの文字色                     | color    | 赤色         |
| `console.help.color`     | ヘルプメッセージの文字色                   | color    | 灰色         |
| `console.prompt1.～`     | プロンプト。see [prompt.md](prompt_ja.md). | string   | `tgsql>`     |
| `console.prompt2.～`     | プロンプト（2行目以降）                    | string   | `|`          |
| `display.implicit` | 暗黙メッセージの表示の有無 | boolean | true |
| `display.succeed` | 成功メッセージの表示の有無 | boolean | true |
| `implicit-transaction.label.suffix-time` | 暗黙に開始するトランザクションのラベルに付加する日時の書式 | date | `yyyy-MM-dd HH:mm:ss.SSSxxx` |
| `implicit-transaction.auto-commit` | 暗黙に開始したトランザクションを自動的にコミットするかどうか | boolean | true |
| `transaction.label.suffix-time` | 明示的に開始するトランザクションのラベルに付加する日時の書式 | date | なし |

### explain関連

| 変数名              | 説明                                    | データ型 | デフォルト |
| ------------------- | --------------------------------------- | -------- | ---------- |
| `dot.verbose`       | explainのverboseオプション              | boolean  | なし       |
| `dot.output`        | explainの出力先（絶対パス）             | string   | なし       |
| `dot.executable`    | Graphvizのdotコマンドの場所（絶対パス） | string   | なし       |
| `dot.graph.～`      | dotコマンドの `-G` オプションに渡す値   | string   | なし       |
| `dot.graph.rankdir` | グラフの向き                            | string   | `RL`       |
| `dot.node.～`       | dotコマンドの `-N` オプションに渡す値   | string   | なし       |
| `dot.node.shape`    | ノードの形状                            | string   | `rect`     |
| `dot.edge.～`       | dotコマンドの `-E` オプションに渡す値   | string   | なし       |



### データ型

- int
  - 整数値
- boolean
  - true/false、on/off
- string
  - 文字列
- color
  - 十六進数6桁（rrggbb）
- date
  - 日時の書式。JavaのDateTimeFormatterで指定できる形式。



## ソースコード

see [TgsqlCvKey.java](../core/src/main/java/com/tsurugidb/tgsql/core/config/TgsqlCvKey.java),  [ReplCvKey.java](../cli/src/main/java/com/tsurugidb/tgsql/cli/repl/ReplCvKey.java).

