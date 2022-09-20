# SQLコンソール（Tanzawa）TODO事項

## 共通

- 認証情報ファイル（--credentials）の処理を追加
  - tsubakuroのFileCredential待ち
- トランザクションオプション（--read-area-include等）の処理を追加
  - tsubakuroのTransactionOption対応待ち
- 暗黙にコミットする際（--commitや--auto-commit）のCommiStatusを指定する
  - 新しいパラメーターを増やす？

## SQL console（REPL）

### コマンド

- テーブル一覧を表示するコマンドの新設
    - そもそもtsubakuroでどうやってテーブル一覧を取得する？
- テーブル定義（TableMetaData）を表示するコマンドの新設
    - `\describe tableName` とか？
- `\halt` が入力されたとき、トランザクションが生きていたら、ロールバックを実行するべき？
- `\exit` を `\e` などと省略できるようにする？
- `\` や `\?` がSQLコマンドとしてDBサーバーで実行されてしまうので、実行せずにエラーにしたい
- （2022-09-15荒川さん済）`\help SELECT` のようなコマンドで、文法の詳細（利用可能なオプション等）を表示してほしい（最低限、BEGINとCOMMIT）（by川口さん）
- ヘルプの日本語化（by荒川さん2022-09-20）
- コマンド名の変更・追加（by荒川さん2022-09-15）
  - `\show tables` - そんなAPIがない
  - `\show table TBL` - 作るの面倒だった
  - `\show transaction` - `\status` がいまいちなので置き換え
  - `\exit force` - `\halt` 相当
  - `\set client setting` - 設定するものがあれば？
- EXPLAINの表示（by荒川さん2022-09-20）
  - グラフを（ツリー化して？）ASCIIアートで表示する
- データベース一覧・スキーマ一覧を表示するコマンドを追加（by川口さん2022-09-15）
  - 現在、データベース・スキーマ自体が未対応

### 操作

- （2022-09-15済）現在入力中のステートメントをCtrl+Cでキャンセルしたい
- SQL実行中、DBサーバーが落ちたときにCtrl+Cで停止させたい
  - SQL実行中にDBサーバーが落ちるとtsubakuroが無限待ちになる。このとき、一切キー入力を受け付けなくなる
    - 現状、カラム名を指定しないinsert文（例: `insert into test values(123)`）で発生する
- タブキーで入力補完したい
  - パーサーの補助が必要
- Ctrl+D（EOF）が入力されたとき、トランザクションが生きていたら、ロールバックを実行するべき？
- （2022-09-15済）Ctrl+Dを2回押さないと終了しないことが多い（1回で終了させたい）（by川口さん）
- コマンドを他所からコピーしてペーストすると、選択状態になってしまう（MacのiTerm2）（by川口さん）
  - Windowsのコマンドプロンプトでは起きない

### 表示

- select結果を整形して表示する
  - 現在はList.toString()をそのまま表示している
- （2022-09-14済）selectされた件数を表示する（by川口さん）
- 更新系SQLの実行結果（処理件数）を表示する
  - 現在のtsubakuroは処理件数を返さない
- 文法エラー（EngineException）のメッセージに含まれる行番号を無くす？（1行入力する度に行番号が増えていく）
  - 行番号は、本来はスクリプトファイル向け
  - 行番号表示を無くすか、ステートメント毎に行番号を1に戻すか？
    - ステートメント毎に行番号を1に戻す方法→ステートメント毎にReader作り直し？

### その他

- （2022-09-20済）コマンドの履歴を次の起動時にも引き継いでほしい（by川口さん）