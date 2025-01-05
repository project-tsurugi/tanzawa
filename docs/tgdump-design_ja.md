# `tgdump` - Tsurugi Table Dump tool

2023-12-11 arakawa (NT)

## この文書について

* この文書では、Tsurugidb のテーブルをダンプするダンプツールのデザインについて記載する

## コンセプト

* プリミティブなテーブルダンプ処理を行うためのツール
* プログラミングレスでテーブルダンプを取得できる
* Tsurugidb がネイティブに対応しているフォーマットのみ利用可能
  * Parquet
  * Apache Arrow
  * (CSV等は非対応)
* 当面はローカルコンピューター上でしか実行できない
  * Tsurugi の dump/load が TCP 接続に非対応のため
* 目的別にプロファイルを提供し、プロファイルに沿った形式のダンプデータを作成する
  * PG-Strom などのターゲット毎のプロファイルを用意し、ターゲットに適した形式を出力する
* ダンプする内容は調整できない
  * 一部の列のみに限定、特定の行を指定してダンプ等は行えない
* 対称となるローダーは別途開発
  * 当面はダンプファイルを生成する
* Java ベース
  * Java 11 以上のランタイムが必要
  * Tsubakuro JNI ライブラリが必要
* CLI のオプション形式は `tgsql` に似せる
  * Java ベースのため

## CLI

`tgdump` - 指定したテーブルの内容をダンプしたダンプファイルを作成する

```sh
tgdump <table-name> [<table-name> [...]] --to </path/to/destination-dir> --connection <endpoint-uri>
tgdump --sql [<query-label>:]<query-text> [[<query-label>:]<query-text> [...]] --to </path/to/destination-dir> --connection <endpoint-uri>
```

* mandatory parameters
  * `<table-name>` (`--sql` が指定されていない場合)
    * ダンプ対象のテーブル名 (複数指定可能)
  * `<query-text>` (`--sql` が指定された場合)
    * ダンプ処理の内容を表す SQL 文字列
  * `--to`
    * 出力先のディレクトリ
    * 出力先は存在しないか、または空のディレクトリでなければならない
  * `-c,--connection`
    * 接続先の Tsurugi の endpoint URI (現状は `ipc:` のみ対応)
* optional parameters
  * `--sql`
    * テーブル名を指定する代わりに、ダンプ処理の内容を表す SQL 文を直接指定する
  * `<query-label>` (`--sql` が指定された場合)
    * 直後の `<query-text>` を識別するためのクエリラベル
    * クエリラベルに以下のいずれの文字も含めることはできない
      * 空白文字 (タブや改行を含む)
      * クウォート文字 (`'`, `"`)
      * コロン (`:`)
    * 未指定の場合は、 "`<sql><引数の位置>`" という名前のラベルを自動的に付与する (引数の位置は 1 から始まる整数)
  * `--profile`
    * [ダンププロファイル](#ダンププロファイル)名
    * 未指定の場合は `default` という名前のプロファイルを使用する
  * `--connection-label`
    * 接続したセッションのラベル
    * 未指定の場合はラベルを利用しない
  * `--connection-timeout`
    * 接続を確立する際のタイムアウト時間 (ミリ秒)
    * `0` を指定した場合や、未指定の場合はタイムアウトを行わない
  * `--transaction`
    * トランザクションの種類
    * 以下のいずれか (大文字小文字を区別しない)
      * `OCC` (or `short`)
      * `LTX` (or `long`)
      * `RTX` (or `read`, `readonly`, `read-only`)
    * 未指定の場合は `RTX`
  * `--transaction-label`
    * トランザクションのラベル
    * 未指定の場合はラベルを利用しない
  * `--threads`
    * クライアント側の処理スレッド数
      * サーバ側に同時に処理を要求するテーブル数はこのスレッド数に制限される
    * 未指定の場合は `1`
  * `-v,--verbose`
    * より多くのメッセージを標準出力へ出力する
  * `--monitor` (hidden)
    * 指定のファイルにモニタリング情報を出力する
      * 宛先に既にファイルが損座していた場合はエラー
    * 未指定の場合はモニタリング情報を出力しない
* special parameters
  * `-h,--help`
    * ヘルプメッセージを表示して終了する
  * `--version`
    * バージョン情報を表示して終了する
    * `--help` と同時に指定された場合、 `--help` を優先する
* file output
  * `--to` で指定したディレクトリ配下に `<table-name>` または `<query-label>` で指定した名前のディレクトリを作成し、作成した各ディレクトリ配下に当該テーブルの内容または SQL の処理結果をダンプしたダンプファイル (群) を作成する
    * 指定されたテーブルやクエリの数だけ `--to` で指定した配下にディレクトリ (サブディレクトリ) を作成する
    * 各サブディレクトリには0個以上のファイルが生成される
    * テーブル名やクエリラベルがディレクトリ名として適さない場合、当該箇所を適切な文字に置き換える
    * 出力されるダンプ形式は `--profile` で指定したプロファイルに従う
    * `--profile` の種類によっては、 `</path/to/destination-dir>` 配下にその他のファイルを出力する場合もある
    * ダンプファイルは `tsurugidb` プロセスのユーザー権限で書きこまれる (現在の制限)
  * サブディレクトリ名は以下のように計算する
    * 文字集合定義
      * 置換文字: `_` (アンダースコア)
      * 区切り文字: `-` (ハイフン)
      * 置換対象文字
        * ISO Control
        * Unicode 補助文字 (supplementary characters)
          * UTF-16 系ファイルシステムでの面倒を避ける
        * 空白文字
        * 区切り文字
          * 衝突計算の簡略化
        * `.<>:/\|?*"` のいずれか
          * 一部システムでファイル名として利用できないか、特殊な意味を持つもの
    * 操作
      * テーブル名やクエリラベルに含まれる置換対象文字を置換文字に置き換える
      * テーブル名やクエリラベルに含まれる大文字 (uppercase letter: Lu) を小文字 (lowercase letter: Ll) に置き換える
      * テーブル名やクエリラベルの先頭から50文字を残し、残りを除去する (クエリラベルはより短い文字列のみ指定可能)
      * テーブル名やクエリラベルが衝突した場合、末尾に区切り文字と任意の数値を加え、衝突を避ける
    * 備考
      * NFC/NFD については関与しない
        * テーブル名のほうをルール化する際に考える
  * 同一のテーブルを複数回指定した場合、その回数だけダンプ処理が行われる
  * DDL 情報は出力しない
    * TBD: 将来的に対応を検討。その場合は `--to` で指定したディレクトリ配下に生成
* standard output
  * 以下のタイミングで出力を行う
    * 各テーブルのダンプの開始前 (テーブル名やクエリラベルと出力先ディレクトリ)
    * 各テーブルのダンプの終了後 (テーブル名やクエリラベルと出力先ディレクトリ)
  * `--verbose` ではさらに以下の出力を行う (一例)
    * ダンプ処理本体の開始前
    * 各テーブル情報収集の開始前
    * 各テーブル情報収集の終了後
    * トランザクションの開始前
    * トランザクションの開始後
    * ダンプ処理本体の開始前
    * 各テーブルのダンプコマンド発行前
    * 各テーブルのダンプコマンド発行後
    * 各テーブルのダンプファイルが一つ生成された際
    * 各テーブルのダンプコマンド完了後
    * コミット処理の開始前
    * コミット処理の終了後
    * ダンプ処理本体の終了後
* standard error
  * ロガーのメッセージを出力する
    * 非 `0` の exit status を返す場合、ロガー経由でエラーの内容を表示
  * ロガーには [`slf4j-simple`](https://github.com/qos-ch/slf4j) を組み込み、既定で以下の設定がなされている

    プロパティ名 | 既定値 | 概要
    ------------|--------|------
    `org.slf4j.simpleLogger.defaultLogLevel` | `warn`  | ログレベル
    `org.slf4j.simpleLogger.showLogName`     | `false` | ログ名を表示するかどうか
    `org.slf4j.simpleLogger.showThreadName`  | `false` | スレッド名を表示するかどうか
    `org.slf4j.simpleLogger.showDateTime`    | (未設定)  | 日時を表示するかどうか
    `org.slf4j.simpleLogger.dateTimeFormat`  | (未設定) | 日時の形式

    ※「(未設定)」となっている箇所は、正しく表示できないため一時的に未設定に戻している
  
  * コマンド起動時に環境変数 (`JAVA_OPTS`, `TGDUMP_OPTS`) 経由で上書き可能
* monitoring information
  * ダンプ処理の開始前に、対象テーブルごとに以下の `kind=data` のレコード (`format=dump-info`) を出力する

    フィールド名 | 内容 | 備考
    ------|------|------
    `kind` | `data` |
    `format` | `dump-info` | ダンプ対象のテーブルをデータベース上に検出したことを表す
    `type` | ダンプの種類 | `--sql` の指定がない場合は `table`, ある場合は `query`
    `table` | テーブル名 | `--sql` が未指定の場合はクエリラベル
    `query` | クエリ文字列 | `--sql` が未指定の場合は absent
    `destination` | 出力先ファイルパス |
    `columns` | ダンプ対象列の一覧 | 形式は後述 | `--sql` が指定された場合は absent

    `columns` はオブジェクトの配列型で、テーブルに含まれる列ごとに次のプロパティを含むオブジェクトを出力する。

    フィールド名 | 内容 | 備考
    ------|------|------
    `name` | 列名 |
    `type` | 型名 | 形式は後述

    `columns.type` の表示は以下のようになる。

    型 | 表示名 | 備考
    ---|--------|-----
    `BOOL` | `BOOLEAN` |
    `SMALLINT` | `INT4` |
    `INT` | `INT4` |
    `BIGINT` | `INT8` |
    `FLOAT` | `FLOAT4` |
    `DOUBLE` | `FLOAT8` |
    `DECIMAL` | `DECIMAL` |
    `CHAR` | `CHARACTER` |
    `VARCHAR` | `CHARACTER` |
    `BINARY` | `OCTET` |
    `VARBINARY` | `OCTET` |
    `DATE` | `DATE` |
    `TIME` | `TIME_OF_DAY` |
    `TIMESTAMP` | `TIME_POINT` |
    `TIME WITH TIME ZONE` | `TIME_OF_DAY_WITH_TIME_ZONE` |
    `TIMESTAMP WITH TIME ZONE` | `TIME_POINT_WITH_TIME_ZONE` |
    `BLOB` | `BLOB` |
    `CLOB` | `CLOB` |
    ユーザー定義型 | 型名 |
    その他の型 | - | `null` として表示する

  * 各テーブルのダンプ処理を開始した (データベースに実際に処理を依頼した) 際、以下の `kind=data` のレコード (`format=dump-start`) を出力する

    フィールド名 | 内容 | 備考
    ------|------|------
    `kind` | `data` |
    `format` | `dump-start` | 対象のテーブルのダンプ処理を開始したことを表す
    `table` | 対象のテーブル名 | `--sql` が指定された場合はクエリラベルを出力
    `destination` | 出力先ディレクトリパス |

  * サブディレクトリにダンプファイルが追加された際、以下の `kind=data` のレコード (`format=dump-file`) を出力する

    フィールド名 | 内容 | 備考
    ------|------|------
    `kind` | `data` |
    `format` | `dump-file` | ダンプファイルが作成されたことを表す
    `table` | 対象のテーブル名 | `--sql` が指定された場合はクエリラベルを出力
    `destination` | 出力先ファイルパス |

  * サブディレクトリに当該テーブルのすべてのダンプファイルが追加された終わった際、以下の `kind=data` のレコード (`format=dump-finish`) を出力する

    フィールド名 | 内容 | 備考
    ------|------|------
    `kind` | `data` |
    `format` | `dump-finish` | 対象のテーブルのダンプ処理が完了したことを示す
    `table` | 対象のテーブル名 | `--sql` が指定された場合はクエリラベルを出力
    `destination` | 出力先ディレクトリパス |

* exit status

  reason | exit status | 概要
  -------|-------------|------
  (absent) | `0` | 正常に全てのテーブルダンプファイルを作成できた
  `invalid_parameter` | 非 `0` | 必須オプションが未指定、またはいずれかのオプションが不正な値 (モニタリング情報は出力できない場合がある)
  `monitor_output` | 非 `0` | モニタリング情報の出力中にエラーが発生した (モニタリング情報は出力できない場合がある)
  `destination_exists` | 非 `0` | 出力先のディレクトリが存在し、かつ空でない
  `destination_failure` | 非 `0` | 出力先のディレクトリを作成できなかった
  `profile_not_found` | 非 `0` | プロファイル定義ファイルが見つからない
  `profile_not_registered` | 非 `0` | 組み込みプロファイルが見つからない
  `profile_invalid` | 非 `0` | プロファイルのデータ形式が不正
  `profile_unsupported` | 非 `0` | 未対応のプロファイル形式
  `authentication_failure` | 非 `0` | データベース接続時に認証エラーが発生した
  `connection_timeout` | 非 `0` | 対象のデータベースへの接続要求がタイムアウトした
  `connection_failure` | 非 `0` | 対象のデータベースへの接続に失敗した
  `table_not_found` | 非 `0` | 対象のテーブルのいずれかが存在しない
  `begin_failure` | 非 `0` | トランザクションの開始に失敗した
  `prepare_failure` | 非 `0` | ダンプ命令の解釈に失敗した (`--sql` が指定された場合、SQL 文字列に誤りがある)
  `operation_failure` | 非 `0` | ダンプ処理中にエラーが発生した
  `commit_failure` | 非 `0` | ダンプ処理のコミットに失敗した (ダンプファイルは生成されたが、不正確である可能性がある)
  `io` | 非 `0` | ハンドルできないI/Oエラーが発生した
  `server` | 非 `0` | ハンドルできないサーバーエラーが発生した
  `unknown` | 非 `0` | ハンドルできない任意のエラーが発生した
  `interrupted` | 非 `0` | 割り込みが発生した
  `internal` | 非 `0` | アプリケーション側の問題による内部エラー

  ※ reason はモニタリング情報のコマンド終了時の原因コード、 exit status はコマンド自体の終了ステータス

* 備考
  * TBD: strand 前提で、テーブルごとの最大ダンプ並列数を指定できるようにする
    * `-P,--parallel` あたりを想定
  * TODO: 認証システム導入後に以下のオプションを追加予定
    * `--user`
    * `--auth-token`
    * `--credentials`
    * `--no-auth`

## ダンププロファイル

* ダンププロファイルとは、ダンプファイルの利用目的ごとに適した設定をまとめたもの
  * `--profile PG-Strom` のように、宛先ごとに適した形式でダンプファイルを出力する
* ダンププロファイルには、組み込みとダンププロファイル記述ファイルのいずれかを指定する
  * 組み込みのプロファイルは、ツール自体が提供するダンププロファイル
  * ダンププロファイル記述ファイルは、プロファイルを記載したファイルを指定する
  * 内部的には、組み込みプロファイルは、配布物に同梱したダンププロファイル記述ファイルを利用する
* 組み込みのダンププロファイルは以下を用意
  * `default` - デフォルトダンププロファイル。 `parquet` と同等
  * `parquet` - Apache Parquet 形式のファイルを出力する
  * `arrow` - Apache Arrow 形式のファイルを出力する
  * `PG-Strom` - PG-Strom での利用に適した Apache Arrow 形式のファイルを出力する
  * その他候補
    * `parquet-stream` - Apache Parquet 形式のファイルを、ストリーミング処理に適した形式で出力する
    * `arrow-stream` - Apache Arrow 形式のファイルを、ストリーミング処理に適した形式で出力する
* ダンププロファイル記述ファイルは、以下のようなフィールドを持つJSONオブジェクトからなる

  フィールド名 | 形式 | 概要
  ------------|------|------
  `format_version` | 整数 | ダンププロファイル記述ファイルのフォーマットバージョン
  `title` | 文字列 | プロファイルのタイトル (任意)
  `description` | 文字列 | プロファイルの概要 (任意)
  `description.<lang>` | 文字列 | 言語 `<lang>` によるプロファイルの概要 (任意)
  `file_format` | オブジェクト | 出力ファイルの形式 (後述)

  Apache Parquet 形式の場合、 `file_format` には以下のプロパティを有する JSON オブジェクトを指定する (`ParquetFileFormat` に対応)。

  フィールド名 | 形式 | 概要
  ------------|------|------
  `format_type` | 文字列 | `"parquet"`
  `parquet_version` | 文字列 | Parquet file 形式のバージョン
  `record_batch_size` | 整数 | row group の最大行数, 1以上
  `record_batch_in_bytes` | 整数 | row group の最大行数を推定レコードサイズから算定, 1以上
  `codec` | 文字列 | 圧縮コーデックの名前
  `encoding` | 文字列 | 列のエンコーディングの名前
  `columns` | 配列 | 列ごとの設定 (後述)

  上記の `columns` フィールドには、以下のプロパティを有する JSON オブジェクトを指定する (`ParquetColumnFormat` に対応)。

  フィールド名 | 形式 | 概要
  ------------|------|------
  `name` | 文字列 | 対象の列名
  `codec` | 文字列 | 圧縮コーデックの名前
  `encoding` | 文字列 | 列のエンコーディングの名前

  Apache Arrow 形式の場合、 `file_format` には以下のプロパティを有する JSON オブジェクトを指定する (`ArrowFileFormat` に対応)。

  フィールド名 | 形式 | 概要
  ------------|------|------
  `format_type` | 文字列 | `"arrow"`
  `metadata_version` | 文字列 | メタデータ形式のバージョン
  `alignment` | 整数 | メモリアライメントのバイト数, 1以上
  `record_batch_size` | 整数 | record batch の最大行数, 1以上
  `record_batch_in_bytes` | 整数 | record batch の最大行数を推定レコードサイズから算定, 1以上
  `codec` | 文字列 | 圧縮コーデックの名前
  `min_space_saving` | 数値 | 圧縮済みデータを採用する圧縮率の閾値, 0.0~1.0の範囲
  `character_field_type` | 文字列 | `"STRING"` または `"FIXED_SIZE_BINARY"`

* ダンププロファイル記述ファイルを組み込みのダンププロファイルとする場合、以下の手順を行う
  1. クラスパス上に当該ファイルを配置する
  2. クラスパス上の `META-INF/tsurugidb/tgdump/dump-profile.properties` を作成し、 `<profile-name>=</path/to/file.json>` のエントリを追加する
     * 上記の `<profile-name>` が組み込みのプロファイル名、 `</path/to/file.json>` に当該ファイルのクラスパス上の位置を書く
     * `<profile-name>` は小文字とする (組み込みプロファイルは case insensitive)
     * `<profile-name>` が衝突している場合、最初に見つけたもののみを採用し、残りは読み捨てる (警告ログは出す)
