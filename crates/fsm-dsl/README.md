# FSM DSL Project (SSoT Parser - Java/ANTLR)

## 概要

このプロジェクトは、状態マシン、サービス、型定義などを含むシステム仕様を記述するための独自 DSL (`.ssot` ファイル) を定義し、そのパーサーと基本的な AST (Abstract Syntax Tree) ビルダーを Java と ANTLR v4 を使用して開発することを目的としています。将来的には、この AST を利用して各種プログラミング言語のコード、スキーマ定義、ドキュメントなどを自動生成することを目指します。

DSL の仕様は [`DSL.md`](./DSL.md) に定義されています。

## 現状ステータス (部分的実装)

*   **DSL 仕様:** [`DSL.md`](./DSL.md) にて詳細な仕様が定義済みです。
*   **ANTLR 文法:** `SSoT.g4` ファイルに DSL 仕様の大部分が定義されています。
*   **パーサー基盤 (Java/ANTLR):**
    *   Maven (`pom.xml`) を使用して ANTLR コード生成と Java コードのコンパイルを行うビルドプロセスが確立されています。
    *   ANTLR で生成されたレキサー (`SSoTLexer`) とパーサー (`SSoTParser`) を使用して、入力 `.ssot` ファイルの構文解析を行います (`src/main/java/ssot_parser/Main.java`)。
*   **AST 構築 (部分的):**
    *   主要な AST ノードクラス (`SsotRoot`, `TypeDefNode`, `FieldNode`, `StateNode`, `TransitionNode`, `ActionNode` など) が `src/main/java/ssot_parser/` に定義されています。
    *   ANTLR の Visitor パターンを用いた `AstBuilderVisitor.java` が実装されており、Parse Tree から AST を構築します。
    *   現在、`types` ブロック内の `struct` と `field` 定義については、基本的な AST ノードが生成されます。
    *   `communication` ブロック内の要素 (`protocol`, `channel`, `event`) も認識されますが、対応する AST ノードの構築はプレースホルダー段階です。
    *   **課題:** 状態マシン (`machines` ブロック)、サービス (`services` ブロック)、アクター (`actors` ブロック)、デプロイメント (`deployment_config` ブロック)、依存関係 (`dependencies` ブロック)、およびアノテーション (`@id`, `$name(...)`) の AST 構築は未実装または非常に不完全です。
*   **AST 検証 (部分的):**
    *   基本的な `AstValidator.java` が存在し、型名の一意性チェックなど、ごく一部の検証が行われます。
    *   **課題:** 詳細な意味論的検証 (参照解決、型チェック、アノテーション内容の検証など) は未実装です。
*   **コード生成:** 未実装です。

## 今後のステップ (主な課題)

1.  **アノテーション処理の実装:** `@id` や各種 `$annotation` をパースし、対応する AST ノードに情報を付加する。
2.  **State Machine の Visitor/AST 実装:** `machines` ブロック内の `state`, `context`, `action`, `guard`, `invoke`, `transition` 等に対応する Visitor ロジックと AST ノード (必要に応じて再設計) を実装する。
3.  **Service/Interface の Visitor/AST 実装:** `services` ブロック内の要素を処理する Visitor と AST ノードを実装する。
4.  **その他のブロックの Visitor/AST 実装:** `actors`, `deployment_config`, `dependencies` ブロックに対応する Visitor と AST ノードを実装する。
5.  **AST ノード設計の見直し:** 特に `StateNode`, `TransitionNode` など、文法の詳細を表現できるように設計を改善する。
6.  **AST 検証の強化:** 参照解決、型チェック、アノテーションに基づいたルールなど、より詳細な検証ロジックを `AstValidator` に追加する。
7.  **Imports の処理:** `import` 文を解釈し、別ファイルの定義を解決できるようにする。
8.  **エラーハンドリングの強化:** パースエラーや AST 構築・検証時のエラーをより分かりやすく報告する。
9.  **テストの拡充:** 各 Visitor メソッドや Validator のルールに対するユニットテストを追加する。
10. **コード生成器の実装:** AST からターゲット言語 (Rust, TypeScript, Mermaid など) を出力する機能を追加する。

## ビルドと実行

Maven が導入されたため、以下のコマンドでビルドと実行が可能です。

1.  **ビルド (コンパイルとANTLRコード生成):**
    ```bash
    # プロジェクトルートで実行
    mvn clean compile
    ```
    これにより、`src/main/antlr4` 内の `.g4` ファイルからパーサーコードが `target/generated-sources/antlr4` に生成され、
    `src/main/java` 内の Java コードと共に `target/classes` にコンパイルされます。

2.  **実行 (Mainクラス):**
    ```bash
    # プロジェクトルートで実行
    # Main.java で処理する入力ファイル (inputFile 変数) を変更可能
    mvn exec:java -Dexec.mainClass="ssot_parser.Main"
    ```
    実行すると、指定された `.ssot` ファイルのパース、AST構築、基本的な検証が行われ、`AstBuilderVisitor` 内のログが出力されます。

3.  **実行可能 JAR の作成:**
    ```bash
    # プロジェクトルートで実行
    mvn package
    ```
    これにより、依存ライブラリを含む実行可能な JAR ファイルが `target/` ディレクトリに生成されます (例: `fsm-dsl-parser-0.1.0-SNAPSHOT.jar`)。

4.  **JAR ファイルの実行:**
    ```bash
    # target ディレクトリ内の JAR ファイルを指定
    java -jar target/fsm-dsl-parser-*.jar
    ```
    (上記コマンドは `Main.java` 内でハードコードされた入力ファイルを使用します。)

## 貢献

(貢献ガイドラインは未定です) 