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
    *   主要な AST ノードクラス (`SsotRoot`, `TypeDefNode`, `FieldNode`, `EnumVariantNode`, `AnnotationNode` など) が `src/main/java/ssot_parser/ast/nodes/` に定義されています。
    *   状態マシン (`machines` ブロック) に関する AST ノード (`MachineNode`, `ContextNode`, `ActionDefinitionNode`, `GuardDefinitionNode`, `InvokeDefinitionNode`, `StateNode`, `TransitionNode`, `EventHandlerNode`, `ConditionalTransitionNode`, `InvokeStateNode`, `ValueNode`, `InvokeCompletionHandler`) が設計・実装されました。
    *   ANTLR の Visitor パターンを用いた `AstBuilderVisitor.java` が実装されており、Parse Tree から AST を構築します。
    *   `types` ブロック (`struct`, `enum`, `field`, `variant`)、基本的なアノテーション (`@id`, `$name`) の AST ノードが生成されます。
    *   `machines` ブロック内の `context`, `actions`, `guards`, `invokes` 定義部分の基本的な AST 構築ロジックが `AstBuilderVisitor` に実装されました。
    *   `states` ブロック内の `state` 定義の AST 構築 (entry/exit action, on transition, conditional transition, invoke) も部分的に `AstBuilderVisitor` に実装されました。
    *   `communication` ブロックと `actors` ブロックも基本的なコンテナノードは生成されますが、詳細な内部要素の AST 構築は不完全です。
    *   **課題:** 状態マシン (`machines` ブロック) の AST 構築、特に `invoke` の `input` マッピングや `history` 状態などの詳細、および `AstBuilderVisitor` と `InvokeStateNode` における `ValueNode` と `InvokeCompletionHandler` の利用の修正が必要です。サービス (`services` ブロック: service, interface, method など)、`deployment_config`, `dependencies` ブロックも未対応です。
*   **AST 検証 (部分的):**
    *   基本的な `AstValidator.java` が存在し、型名の一意性チェックなど、ごく一部の検証が行われます。
    *   **課題:** 詳細な意味論的検証 (参照解決、型チェック、状態マシンの妥当性検証など) は未実装です。
*   **コード生成:** 未実装です。

## 今後のステップ (ロードマップ案)

1.  **(最優先) State Machine の AST 構築完了と Visitor 修正:**
    *   `AstBuilderVisitor.java` と `InvokeStateNode.java` でトップレベルの `ValueNode` と `InvokeCompletionHandler` を正しく使用するように修正する。
    *   `AstBuilderVisitor` 内の `visitStateDefinition` を完成させ、`invoke` の `input` マッピングや `history` 状態など、全ての状態要素を文法に基づいて正確に処理するようにする。
    *   状態マシン関連の AST 構築に関するユニットテストを追加する。
2.  **(次点) AST 検証の強化 (State Machine 中心):**
    *   状態マシン内の状態遷移の妥当性、参照されている Action/Guard/Context/Invoke/State の存在確認、初期状態の検証など、意味論的な検証を `AstValidator` に追加する。
3.  **アノテーション処理の拡充:** `@id` や各種 `$annotation` をパースし、対応する AST ノードに情報を付加するロジックを確認・拡充する。検証ロジックでも利用する。
4.  **Service/Interface の Visitor/AST 実装:** `services` ブロック内の要素を処理する Visitor と AST ノードを実装する。
5.  **テストの導入と拡充:** 特に Service/Interface の AST 構築と検証を中心に、ユニットテストを追加する。
6.  **エラーハンドリングの強化:** パースエラーや AST 検証エラーをより分かりやすく報告するように改善する。
7.  **その他のブロックの Visitor/AST 実装:** `actors`, `communication`, `deployment_config`, `dependencies` ブロックに対応する Visitor と AST ノードを実装する。
8.  **Imports の処理:** `import` 文を解釈し、別ファイルの定義を解決できるようにする。
9.  **コード生成器の実装:** AST からターゲット言語 (例: Mermaid グラフ定義) を出力する機能を追加する (初期段階)。

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