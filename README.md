# TaskBoardManager

TaskBoardManagerは、ノードやボックスを無限キャンバスへ自由に配置し、接続線で関係を表現するJavaFXデスクトップアプリケーションです。MVP 1.1〜1.8では、意味を持つノードと関係、階層表示、タスク管理、検索までをMVP 1.0へ追加しています。

## 技術構成

- Java 21 Toolchain
- Gradle Kotlin DSL
- JavaFX 21.0.12 LTS
- FXML / JavaFX CSS
- Jackson JSON
- JUnit 5

## モジュール

- `taskboardmanager-model`: JavaFX非依存のボード、型付きノード、関係、変更通知
- `taskboardmanager-core`: 要素操作、DAG検証、階層レイアウト、検索、進捗集計、Undo/Redo
- `taskboardmanager-persistence`: JSONマッピング、検証、安全なファイル置換
- `taskboardmanager-desktop`: JavaFX画面、キャンバス、インスペクター、ファイル操作

依存方向は`desktop → core → model`および`desktop → persistence → model`です。

## 主な機能

- ノードとボックスの追加、移動、タイトル編集、ボックスのリサイズ
- 接続ハンドルによる要素間接続
- パン、カーソル中心ズーム、全体表示、選択要素へのフォーカス
- Ctrlクリックとドラッグ範囲による複数選択、一括移動・削除
- Undo/Redo、コピー、切り取り、貼り付け、全選択
- 新規作成、開く、保存、名前を付けて保存、未保存確認
- ダークテーマ、メニューバー、ツールバー、インスペクター、ステータスバー
- 通常、目標、タスク、疑問、アイデア、メモ、決定事項のノード種類
- 親子、依存、関連、参照、ブロック関係と、親子・依存関係の循環防止
- フリーボード、横向きツリー、階層リストの3表示（同一ドキュメントを共有）
- 階層レイアウトのプレビューと、Undo可能な自由配置への明示的反映
- タスク状態、進捗、優先度、開始日・期限日、直接の子タスク平均
- 疑問の解決状態、アイデアの検討状態、決定内容・理由・決定日
- 文字列検索、種類・状態・優先度フィルター、検索結果からの移動

## 基本操作

- `Ctrl+N` 新規ボード
- `Ctrl+O` 開く
- `Ctrl+S` 保存
- `Ctrl+Shift+S` 名前を付けて保存
- `Ctrl+Z` Undo
- `Ctrl+Y`または`Ctrl+Shift+Z` Redo
- `Ctrl+C / X / V` コピー／切り取り／貼り付け
- `Ctrl+A` 全選択
- `Delete` 選択対象の削除
- `Esc` 操作キャンセル
- 中ボタンドラッグ、またはSpace＋左ドラッグでパン

## 保存形式

標準拡張子は`.tbm.json`です。現在の保存形式は`schemaVersion: 2`で、ノード種類と専用属性、接続関係、ズーム・パン状態を保存します。MVP 1.0の`schemaVersion: 1`は、ノードを「通常」、接続を「未指定」として互換読み込みします。書き込みは同じディレクトリの一時ファイルへ行い、対応環境では原子的に保存先を置換します。

## ビルドと実行

Java 21を利用できる環境で次を実行します。

```powershell
.\gradlew.bat :taskboardmanager-desktop:run
```

テストは次のタスクです。

```powershell
.\gradlew.bat :taskboardmanager-desktop:test
```

この実装作業中には、上記のビルド、テスト、アプリケーション起動は実行していません。

## 変更通知と将来拡張

`BoardDocument`は単調増加するリビジョン番号と最小限の変更イベントを発行します。現時点でネットワーク同期は行いませんが、モデルと描画、永続化を分離したまま差分同期へ拡張できる構成です。
