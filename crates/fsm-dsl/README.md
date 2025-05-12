# FSM DSL Project (SSoT Parser - Java/ANTLR)

## 概要

このプロジェクトは、状態マシン、サービス、型定義などを含むシステム仕様を記述するための独自 DSL (`.ssot` ファイル) を定義し、そのパーサーと基本的な AST (Abstract Syntax Tree) ビルダーを Java と ANTLR v4 を使用して開発することを目的としています。将来的には、この AST を利用して各種プログラミング言語のコード、スキーマ定義、ドキュメントなどを自動生成することを目指します。

DSL の仕様は [`DSL.md`](./DSL.md) に定義されています。

## Project Status & Recent Developments

**Core Parsing Functionality:**
The project features a robust Java/ANTLR-based parser for a custom Domain-Specific Language (`.ssot` files). This DSL is designed to define comprehensive system specifications, including types, services, actors, state machines, and communication protocols. The ANTLR grammar (`SSoT.g4`) is well-established, and the Maven build process (`pom.xml`) is stable.

**Abstract Syntax Tree (AST) Construction:**
*   A comprehensive set of Java classes in `src/main/java/ssot_parser/ast/nodes/` represents the DSL elements. The core AST structure resides in the `ssot_parser.ast` package, promoting separation of concerns.
*   A central root node, `ssot_parser.ast.SsotRoot.java`, encapsulates the entire parsed file, including imports, top-level annotations/ID, and lists of defined types, services, machines, etc.
*   The `AstBuilderVisitor.java` class is responsible for constructing this AST from the ANTLR parse tree. Its main `visitFile` method correctly collects all definitions and constructs/returns an `SsotRoot` instance.
*   It handles top-level file structure, imports, annotation processing, definition blocks (types, services, machines, actors, communication), detailed state machine constructs (nested/history states, transitions, invokes).
*   **Resolved Issue:** Previous compilation errors related to `SsotRoot` duplication and constructor calls in `AstBuilderVisitor.java` have been resolved by removing the redundant `SsotRoot` class and fixing associated import statements.

**AST Validation:**
*   The `ssot_parser.validation.AstValidator` component performs comprehensive semantic checks on the constructed AST. It resides in the dedicated `ssot_parser.validation` package.
*   Its capabilities include: ID/Name uniqueness checks, reference resolution (types, invokes, actions, guards, events), state machine logic validation (initial/final states, reachability, transitions, history), basic type checking, and structural integrity checks.
*   **Code Cleanup:** A redundant `AstValidator` in the root `ssot_parser` package has been removed, consolidating validation logic into the `ssot_parser.validation` package. `Main.java` has been updated accordingly.

**Testing:**
*   JUnit 5 tests are present for major components like state machines, communication, actors, types, services, and validation logic, located in `src/test/java/ssot_parser/`.
*   **Recent Progress (May 2024):** Significant effort has been made to address compilation errors within the test suite. This involved:
    *   Correcting how annotations are accessed from AST nodes.
    *   Refactoring type definition tests (`TypeAstTest.java`) to correctly use `TypeDefNode` with its `TypeKind` for both structs and enums.
    *   Resolving issues with method calls on `ChannelNode` and `EventNode` by checking for semantic information via annotations.
    *   The test suite now **compiles successfully**.
*   **Current Challenge:** Despite compiling, `mvn test` reveals numerous runtime **ANTLR syntax errors** across most test files. This indicates a significant mismatch between the DSL examples used in the tests and the current ANTLR grammar (`src/main/antlr4/SSoT.g4`). The grammar has evolved, and the test inputs need to be updated accordingly.


**Future Goals:**
The primary long-term goal remains the development of code generators that leverage the AST to produce various artifacts (code, schemas, documentation). The "ドクトリン" (doctrine) concept noted in the IDEA section is also a potential future DSL enhancement. Further improvements could include more detailed validation rules and enhanced error reporting once the current test-related issues are resolved.

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
    実行すると、指定された `.ssot` ファイルのパース、AST構築、検証が行われ、結果が出力されます。

3.  **実行可能 JAR の作成:**
    ```bash
    # プロジェクトルートで実行
    mvn package
    ```
    これにより、依存ライブラリを含む実行可能な JAR ファイルが `target/` ディレクトリに生成されます。

4.  **JAR ファイルの実行:**
    ```bash
    # target ディレクトリ内の JAR ファイルを指定
    java -jar target/fsm-dsl-parser-*.jar
    ```
    (上記コマンドは `Main.java` 内でハードコードされた入力ファイルを使用します)

## 貢献

(貢献ガイドラインは未定です)

## IDEA

ドクトリンという宣言形式概念を導入する