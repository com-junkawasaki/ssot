# FSM DSL Project (SSoT Parser - Java/ANTLR)

## 概要

このプロジェクトは、状態マシン、サービス、型定義などを含むシステム仕様を記述するための独自 DSL (`.ssot` ファイル) を定義し、そのパーサーと基本的な AST (Abstract Syntax Tree) ビルダーを Java と ANTLR v4 を使用して開発することを目的としています。将来的には、この AST を利用して各種プログラミング言語のコード、スキーマ定義、ドキュメントなどを自動生成することを目指します。

DSL の仕様は [`DSL.md`](./DSL.md) に定義されています。

## Project Status & Recent Developments

**Core Parsing Functionality:**
The project features a robust Java/ANTLR-based parser for a custom Domain-Specific Language (`.ssot` files). This DSL is designed to define comprehensive system specifications, including types, services, actors, state machines, and communication protocols. The ANTLR grammar (`SSoT.g4`) is well-established, and the Maven build process (`pom.xml`) is generally stable, though currently facing a compilation issue (see below).

**Abstract Syntax Tree (AST) Construction:**
*   A comprehensive set of Java classes in `src/main/java/ssot_parser/ast/nodes/` represents the DSL elements.
*   A central root node, `SsotRoot.java`, has been introduced to encapsulate the entire parsed file, including imports, top-level annotations/ID, and lists of defined types, services, machines, etc.
*   The `AstBuilderVisitor.java` class is responsible for constructing this AST from the ANTLR parse tree. Its main `visitFile` method now correctly collects all definitions and constructs/returns an `SsotRoot` instance.
*   It handles top-level file structure, imports, annotation processing, definition blocks (types, services, machines, actors, communication), detailed state machine constructs (nested/history states, transitions, invokes).
*   **Current Issue:** A persistent compilation error exists within `AstBuilderVisitor.java` related to the `new SsotRoot(...)` constructor call. Despite code appearing correct, the compiler reports an argument type mismatch, preventing successful builds. This needs to be resolved before further testing/development.

**AST Validation:**
The `AstValidator.java` component performs comprehensive semantic checks on the constructed AST. Its capabilities have been significantly expanded and now include:
*   **ID Uniqueness:** Global and scoped checks.
*   **Name Uniqueness:** Checks for all major DSL constructs.
*   **Reference Resolution and Validation:** Type references, invoke targets, action/guard names, event names.
*   **State Machine Logic Validation:**
    *   Initial State (including for compound/parallel states).
    *   Final State Semantics.
    *   State Reachability analysis.
    *   Transition Consistency (ambiguity detection).
    *   Target State validation.
    *   **Nested/History States:** Recently added tests cover validation for initial state requirements in compound states, correct usage of shallow (`$H`) and deep (`$H*`) history states, and checks for transitions targeting non-existent history markers.
    *   **Duplicate State Names:** Checks for duplicate state names within the same scope (e.g., within a parent state or at the machine level).
*   **Basic Type Checking:** Verifying referenced types.
*   **Structural Integrity:** Duplicate fields/variants, service implementation checks against interfaces.
*   **Note:** Some linter errors regarding the `AstValidator(SsotRoot)` constructor being undefined are present in test files, likely stemming from the main compilation issue.

**Future Goals:**
The primary long-term goal remains the development of code generators that leverage the AST to produce various artifacts (code, schemas, documentation). The "ドクトリン" (doctrine) concept noted in the IDEA section is also a potential future DSL enhancement.

## ビルドと実行

Maven が導入されたため、以下のコマンドでビルドと実行が可能です。

**注意:** 現在、`AstBuilderVisitor.java` のコンパイルエラーにより、`mvn compile` および後続のコマンドは失敗します。この問題の解決が最優先事項です。

1.  **ビルド (コンパイルとANTLRコード生成):**
    ```bash
    # プロジェクトルートで実行
    mvn clean compile
    ```
    これにより、`src/main/antlr4` 内の `.g4` ファイルからパーサーコードが `target/generated-sources/antlr4` に生成され、
    `src/main/java` 内の Java コードと共に `target/classes` にコンパイルされます。(現状失敗します)

2.  **実行 (Mainクラス):**
    ```bash
    # プロジェクトルートで実行
    # Main.java で処理する入力ファイル (inputFile 変数) を変更可能
    mvn exec:java -Dexec.mainClass="ssot_parser.Main"
    ```
    実行すると、指定された `.ssot` ファイルのパース、AST構築、基本的な検証が行われ、`AstBuilderVisitor` 内のログが出力されます。(現状ビルドが通らないため実行できません)

3.  **実行可能 JAR の作成:**
    ```bash
    # プロジェクトルートで実行
    mvn package
    ```
    これにより、依存ライブラリを含む実行可能な JAR ファイルが `target/` ディレクトリに生成されます。(現状ビルドが通らないため作成できません)

4.  **JAR ファイルの実行:**
    ```bash
    # target ディレクトリ内の JAR ファイルを指定
    java -jar target/fsm-dsl-parser-*.jar
    ```
    (上記コマンドは `Main.java` 内でハードコードされた入力ファイルを使用します。現状ビルドが通らないため実行できません)

## 貢献

(貢献ガイドラインは未定です)

## IDEA

ドクトリンという宣言形式概念を導入する