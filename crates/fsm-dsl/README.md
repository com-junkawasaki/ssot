# FSM DSL Project (SSoT Parser - Java/ANTLR)

## 概要

このプロジェクトは、状態マシン、サービス、型定義などを含むシステム仕様を記述するための独自 DSL (`.ssot` ファイル) を定義し、そのパーサーと基本的な AST (Abstract Syntax Tree) ビルダーを Java と ANTLR v4 を使用して開発することを目的としています。将来的には、この AST を利用して各種プログラミング言語のコード、スキーマ定義、ドキュメントなどを自動生成することを目指します。

DSL の仕様は [`DSL.md`](./DSL.md) に定義されています。

## Project Status & Recent Developments

**Core Functionality:**
The project is developing a Java/ANTLR-based parser for a custom Domain-Specific Language (`.ssot` files). This DSL is designed to define comprehensive system specifications. The ANTLR grammar (`SSoT.g4`) and Maven build process (`pom.xml`) are established, forming the foundational parsing layer.

**Abstract Syntax Tree (AST):**
A rich set of Java classes (`src/main/java/ssot_parser/ast/`) represents the DSL elements in an Abstract Syntax Tree. The core `AstBuilderVisitor.java` class is responsible for constructing this AST from the ANTLR parse tree and has recently been stabilized.

**Recent Progress & Current Focus:**
Significant effort has been dedicated to achieving a compilable and stable AST and validator. Key achievements include:
*   Stabilization of `AstBuilderVisitor.java`.
*   Systematic correction of `AstNode` interface implementations (e.g., `getId()`, `getAnnotations()`) across various AST node classes (`TransitionConfig`, `ConditionalTransitionNode`, `OptionalTypeNode`, `MapTypeNode`).
*   Resolution of specific AST node constructor mismatches (e.g., in `ConditionalTransitionNode`).
*   Enhancement of the `NodeVisitor` interface by adding missing `visitXNode` methods.
*   Initial corrections in `SsotRoot.java` to align its getter methods with the requirements of `AstValidator.java`.

**The current and immediate priority is to resolve all remaining compilation errors, primarily within `AstValidator.java`, to achieve a clean `mvn clean compile`.** This will unblock further development of semantic validation logic and comprehensive testing. While the foundational components are largely in place, this stabilization phase is critical for the project's health and future progress towards full DSL feature implementation and code generation capabilities.

## Current Build Status & Remaining Compilation Errors

While `AstBuilderVisitor.java` compiles cleanly, and significant progress has been made on AST node and `SsotRoot` issues, the project build (`mvn clean compile`) likely still fails due to errors primarily in `AstValidator.java` and potentially some remaining nuanced issues in AST nodes not yet surfaced by the validator fixes.

The main categories of *remaining anticipated* errors are:

1.  **Compilation Errors in `AstValidator.java`:**
    *   This class likely still has `cannot find symbol` errors for various getter methods on AST node objects or incompatible type issues. Systematic review and correction are needed.
    *   This is the **next primary focus**.
2.  **AST Node Interface Implementation Issues (Further Review):**
    *   While several examples were fixed, a full audit of all AST nodes against `AstNode`/`NodeWithId` interface requirements (`getId()`, `getAnnotations()`) might be beneficial if `AstValidator` issues reveal more problems.
3.  **AST Node Constructor Mismatches (Further Review):**
    *   One instance was fixed. Others may exist and could be revealed as `AstValidator` is fixed.
4.  **Missing Visitor Methods in `NodeVisitor` (Review if new nodes added/changed):**
    *   Major gaps were addressed. If `AstValidator` work implies new visitable nodes or changes, this might need a revisit.


## Next Steps

1.  **(High Priority) Achieve a Clean Build (`mvn clean compile`):** This remains the top priority.
    *   **Fix `AstValidator.java` (CONTINUED):**
        *   Systematically review `AstValidator.java` line by line.
        *   For each `cannot find symbol` or `incompatible types` error:
            *   Identify the AST node type and the method being called.
            *   Verify if the getter method exists on the AST node with the correct name and signature.
            *   If missing or incorrect, add/correct the getter in the respective AST node class (e.g., in `src/main/java/ssot_parser/ast/nodes/` or `src/main/java/ssot_parser/ast/type/`).
            *   Ensure the return types match what `AstValidator` expects.
        *   This will be an iterative process. After fixing a group of errors, re-run `mvn clean compile -e` to check progress.
    *   **(Post-Validator Fixes) Full AST Node Audit (If Needed):**
        *   If `AstValidator` fixes don't resolve all build issues, perform a comprehensive audit of all AST node classes in `src/main/java/ssot_parser/ast/nodes/` and `src/main/java/ssot_parser/ast/type/` to ensure:
            *   Correct implementation of `AstNode` / `NodeWithId` (`getId()`, `getAnnotations()`).
            *   Correct constructor signatures and usage.
    *   **Iterative Compilation:** After each set of targeted fixes, run `mvn clean compile -e` to track progress and identify new errors.

2.  **(Post-Compilation) AST Validation Framework Enhancement:**
    *   With `AstValidator.java` compiling, rigorously test and expand its validation logic.
    *   Implement checks for:
        *   **Reference Resolution:** Ensure all identifiers (state names, action names, type names, etc.) are defined before use.
        *   **Type Checking:** Verify type compatibility in expressions, assignments, and parameters.
        *   **State Machine Semantics:** Validate initial state definitions, transition consistency, reachability, etc.
        *   Start by fleshing out classes in `src/main/java/ssot_parser/validation/`.

3.  **(Post-Compilation) Comprehensive Testing:**
    *   **Unit Tests:** For complex logic within `AstBuilderVisitor.java` methods and individual AST node functionalities.
    *   **Integration Tests:** Create diverse `.ssot` example files (both valid and invalid) to test the end-to-end parsing and AST construction process. Assert the structure and content of the generated AST.

4.  **Implement Remaining DSL Features in AST Builder:**
    *   Ensure full AST construction for `services`, `actors`, `communication`, `deployment_config`, and `dependencies` blocks as defined in `SSoT.g4`. This includes creating or refining corresponding AST nodes and visitor methods.

5.  **Code Generation / AST Consumers:**
    *   Begin implementation of AST consumers, particularly for the `*_OUT` targets indicated in grammar annotations (e.g., `MERMAID_OUT`, `TS_OUT`, `PROTO_OUT`).

6.  **Documentation Enhancement:**
    *   Keep `DSL.md` updated with any grammar refinements.
    *   Maintain this `README.md` with current project status, build/usage instructions, and a clear roadmap.

## ビルドと実行

Maven が導入されたため、以下のコマンドでビルドと実行が可能です。

1.  **ビルド (コンパイルとANTLRコード生成):**
    ```bash
    # プロジェクトルートで実行
    mvn clean compile
    ```
    これにより、`src/main/antlr4` 内の `.g4` ファイルからパーサーコードが `target/generated-sources/antlr4` に生成され、
    `src/main/java` 内の Java コードと共に `target/classes` にコンパイルされます。
    *コンパイルエラーが発生する場合は、上記「Current Build Status & Remaining Compilation Errors」および「Next Steps」セクションを参照してください。*

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