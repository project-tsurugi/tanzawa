# prompt for tgsql

Tsurugi SQLコンソール（tgsql）のプロンプトはクライアント変数によって変更することが出来る。



## デフォルトのプロンプト

最初に表示されるプロンプトをprompt1、複数行にまたがって入力するときのプロンプトをprompt2と呼ぶ。

promt1, prompt2のデフォルトは以下の通り。

```
tgsql> ...prompt1
     | ...prompt2
```



## プロンプトのクライアント変数

以下のクライアント変数にプロンプトの書式を設定する。

| クライアント変数名      | 説明                                           |
| ----------------------- | ---------------------------------------------- |
| console.prompt1.default | デフォルトのプロンプト                         |
| console.prompt1.tx      | トランザクション実行中のデフォルトのプロンプト |
| console.prompt1.occ     | OCC実行中のプロンプト                          |
| console.prompt1.ltx     | LTX実行中のプロンプト                          |
| console.prompt1.rtx     | RTX実行中のプロンプト                          |

トランザクションが実行されていないときやトランザクション実行中のプロンプトが設定されていない場合は、console.prompt1.defaultの設定が使われる。

トランザクション実行中でconsole.prompt1.occ/ltx/rtxが設定されていない場合は、console.prompt1.txの設定が使われる。

空文字列を設定するとデフォルトに戻る。（設定が削除される）

prompt2もprompt1と同様に設定可能。



## プロンプトの書式

プロンプトのクライアント変数には専用の書式を設定する。

波括弧で囲ったプロパティーは実行中の値に置換される。

トランザクションに関するプロパティーは、トランザクション実行中のプロンプトのみで使用できる。

| プロパティー名                 | 説明                                    | 出力例                |
| ------------------------------ | --------------------------------------- | --------------------- |
| now.日付書式                   | 現在日時（日付書式はDateTimeFormatter） |                       |
| endpoint                       | エンドポイント                          | tcp://localhost:12345 |
| connection.label               | セッションのラベル                      |                       |
| tx.id                          | トランザクションID                      | TID-0000000000000001  |
| tx.option                      | トランザクションオプション              |                       |
| tx.type                        | トランザクション種別                    | OCC                   |
| tx.label                       | トランザクションのラベル                |                       |
| tx.include-ddl                 | DDL実行オプション                       | true                  |
| tx.write-preserve, tx.wp       | write preserve                          | "test1", "test2"      |
| tx.inclusive-read-area, tx.ira | inclusive read area                     | "test1", "test2"      |
| tx.exclusive-read-area, tx.era | exclusive read area                     | "test1", "test2"      |
| tx.priority                    | priority                                |                       |



## 例

```
tgsql> \set console.prompt1.default "example> "
console.prompt1.default=example>
example> \set console.prompt1.default ""
console.prompt1.default=null
tgsql>
```

```
tgsql> \set console.prompt1.tx "{tx.type}({tx.label})> "
console.prompt1.tx={tx.type}({tx.label})>
tgsql> begin as tx1;
transaction started. option=[
  type: OCC
  label: "tx1"
]
Time: 2.731 ms
OCC(tx1)>
```

