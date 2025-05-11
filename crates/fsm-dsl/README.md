# FSM DSL Project (SSoT Parser - Java/ANTLR)

## 概要

このプロジェクトは、状態マシン、サービス、型定義などを含むシステム仕様を記述するための独自 DSL (`.ssot` ファイル) を定義し、そのパーサーと基本的な AST (Abstract Syntax Tree) ビルダーを Java と ANTLR v4 を使用して開発することを目的としています。将来的には、この AST を利用して各種プログラミング言語のコード、スキーマ定義、ドキュメントなどを自動生成することを目指します。

DSL の仕様は [`DSL.md`](./DSL.md) に定義されています。

## Project Status & Recent Developments

**Core Parsing Functionality:**
The project features a robust Java/ANTLR-based parser for a custom Domain-Specific Language (`.ssot` files). This DSL is designed to define comprehensive system specifications, including types, services, actors, state machines, and communication protocols. The ANTLR grammar (`SSoT.g4`) is well-established, and the Maven build process (`pom.xml`) is stable, recently verified by successful compilation runs.

**Abstract Syntax Tree (AST) Construction:**
A comprehensive set of Java classes in `src/main/java/ssot_parser/ast/nodes/` represents the DSL elements within an Abstract Syntax Tree. The `AstBuilderVisitor.java` class is responsible for constructing this AST from the ANTLR parse tree. It currently handles:
*   Top-level file structure, imports, and annotation processing (including `@id`, `$name`).
*   Definition blocks for `types` (structs, enums), `services` (interfaces, service definitions with methods), `machines` (context, actions, guards, invokes, states), `actors`, and `communication` (protocols, channels, events).
*   Detailed state machine constructs including nested states, history states, entry/exit actions, event/conditional/after transitions, and invoke state elements.
*   Ongoing work includes ensuring all parsed DSL details (e.g., for invoke definition handlers and action/guard parameters) are fully represented in their respective AST nodes.

**AST Validation:**
The `AstValidator.java` component performs comprehensive semantic checks on the constructed AST. Its capabilities have been significantly expanded and now include:
*   **ID Uniqueness:**
    *   Global uniqueness checks for `@id` annotations across all major definable elements (types, services, interfaces, machines, actors, communication elements like events, protocols, channels).
    *   Scoped uniqueness checks for IDs within specific contexts (e.g., within a machine definition for its states, transitions, etc.).
*   **Name Uniqueness:**
    *   Ensures uniqueness of definition names for all major DSL constructs: `types` (structs, enums), `services`, `interfaces`, `machines`, `events`, `protocols`, `channels`, and `actors`.
*   **Reference Resolution and Validation:**
    *   Validates references to `types` in field definitions, parameters, and return types, checking against defined types and a known set of primitive types.
    *   Validates that `invoke` targets in state machines refer to defined `services` or `machines`.
    *   Ensures that `action` and `guard` names used in state machine transitions, entry/exit actions, and invoke handlers correspond to definitions within the machine's scope.
    *   Checks that `event` names referenced in transitions are defined.
*   **State Machine Logic Validation:**
    *   **Initial State:** Validates the correct definition and existence of initial states for machines and compound/parallel nested states.
    *   **Final State Semantics:** Ensures `final` states do not have outgoing transitions, invoke definitions, nested states, or initial state declarations.
    *   **State Reachability:** Performs an analysis to identify and report any unreachable states within a machine.
    *   **Transition Consistency:** Detects and reports ambiguous transitions, such as multiple transitions for the same event from a state without distinct guard conditions.
    *   Validates target states of transitions.
*   **Basic Type Checking:** Verifies that types referenced in struct fields, event fields, context variables, and method parameters/return types are either defined user types or known primitives.
*   **Structural Integrity:** Includes checks for duplicated fields/variants within structs/enums/events, and validates service method implementations against their interfaces (name, parameters, return type).

**Future Goals:**
The primary long-term goal remains the development of code generators that leverage the AST to produce various artifacts (code, schemas, documentation). The "ドクトリン" (doctrine) concept noted in the IDEA section is also a potential future DSL enhancement.

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

## IDEA

ドクトリンという宣言形式概念を導入する