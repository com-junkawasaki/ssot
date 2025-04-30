# FSM DSL Project (SSoT Parser - Java/ANTLR)

## 概要

このプロジェクトは、システム仕様を記述するための独自 DSL (`.ssot` ファイル) を定義し、そのパーサーを Java と ANTLR v4 を使用して開発することを目的としています。将来的には、この DSL から各種プログラミング言語のコード、スキーマ定義、ドキュメントなどを自動生成することを目指します。

DSL の仕様は [`DSL.md`](./DSL.md) に定義されています。

## 現状ステータス (初期段階)

*   **DSL 仕様:** [`DSL.md`](./DSL.md) にて詳細な仕様が定義済みです。
*   **ANTLR 文法:** `SSoT.g4` ファイルが存在します (ANTLR v4 形式)。
*   **パーサー実装 (Java):**
    *   ANTLR で生成されたレキサー (`SSoTLexer`) を使用して、入力 `.ssot` ファイルをトークン化する基本的な Java コード (`src/main/java/ssot_parser/Main.java`) が実装されています。
    *   ANTLR パーサー (`SSoTParser`) は生成されているものの、構文解析木 (Parse Tree) を構築・走査して意味のある処理を行う部分は未実装です。
    *   コード生成ロジックは未実装です。

## 今後のロードマップ (案)

1.  **ANTLR パーサーの実装:** `SSoTParser` を使用して構文解析を行い、Parse Tree を構築する。
2.  **Parse Tree Visitor/Listener の実装:** Parse Tree を走査し、DSL で定義された構造 (型、サービス、ステートマシン等) をメモリ上のオブジェクトモデルに変換する。
3.  **コード生成器の実装:** オブジェクトモデルから、指定されたターゲット (例: Rust, TypeScript, Mermaid 図) 向けのコード/ファイルを出力するロジックを実装する。
4.  **テストの拡充:** 各ステップ (レキサー、パーサー、コード生成) に対するユニットテストおよび結合テストを追加する。
5.  **コマンドラインインターフェースの整備:** より使いやすいツールとして利用できるよう、コマンドライン引数の処理などを追加する。
6.  **エラーハンドリングの強化:** パースエラーや不正な定義に対する詳細なエラーメッセージを出力する。
7.  **ビルドツールの導入:** Maven または Gradle を導入し、依存関係管理とビルドプロセスを自動化する。

## ビルドと実行

Maven が導入されたため、以下のコマンドでビルドと実行が可能です。

1.  **ビルド (コンパイルとANTLRコード生成):**
    ```bash
    # プロジェクトルートで実行
    mvn compile
    ```
    これにより、`src/main/antlr4` 内の `.g4` ファイルからパーサーコードが `target/generated-sources/antlr4` に生成され、
    `src/main/java` 内の Java コードと共に `target/classes` にコンパイルされます。

2.  **実行 (Mainクラス):**
    ```bash
    # プロジェクトルートで実行
    # Main.java は現在 "temp_comm.ssot" をハードコードで読み込みます
    mvn exec:java -Dexec.mainClass="ssot_parser.Main"
    ```
    または、実行可能な JAR を作成して実行する場合:

3.  **実行可能 JAR の作成:**
    ```bash
    # プロジェクトルートで実行
    mvn package
    ```
    これにより、依存ライブラリを含む実行可能な JAR ファイルが `target/fsm-dsl-parser-0.1.0-SNAPSHOT.jar` として生成されます。(バージョン名は `pom.xml` に依存します)

4.  **JAR ファイルの実行:**
    ```bash
    java -jar target/fsm-dsl-parser-0.1.0-SNAPSHOT.jar
    ```

## 貢献

(貢献ガイドラインは未定です) 