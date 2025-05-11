# FSM DSL Project (SSoT Parser - Java/ANTLR)

## 概要

このプロジェクトは、状態マシン、サービス、型定義などを含むシステム仕様を記述するための独自 DSL (`.ssot` ファイル) を定義し、そのパーサーと基本的な AST (Abstract Syntax Tree) ビルダーを Java と ANTLR v4 を使用して開発することを目的としています。将来的には、この AST を利用して各種プログラミング言語のコード、スキーマ定義、ドキュメントなどを自動生成することを目指します。

DSL の仕様は [`DSL.md`](./DSL.md) に定義されています。

## Project Status & Recent Developments

**Core Functionality:**
The project is developing a Java/ANTLR-based parser for a custom Domain-Specific Language (`.ssot` files). This DSL is designed to define comprehensive system specifications. The ANTLR grammar (`SSoT.g4`) and Maven build process (`pom.xml`) are established, forming the foundational parsing layer.

**Abstract Syntax Tree (AST):**
A rich set of Java classes (`src/main/java/ssot_parser/ast/`) represents the DSL elements in an Abstract Syntax Tree. The core `AstBuilderVisitor.java` class is responsible for constructing this AST from the ANTLR parse tree.

**Recent Progress & Current Focus:**
Significant effort has been dedicated to achieving a compilable and stable AST and validator. Key achievements include:
*   Stabilization of `AstBuilderVisitor.java`'s interaction with `SsotRoot.java` regarding node collection and method calls.
*   Resolution of common `Optional.orElse` misuse in `mapAnnotations` methods within `ContextVariableNode.java` and `GuardDefinitionNode.java`.
*   Initial corrections in `AstValidator.java`, such as:
    *   Standardizing on `InterfaceNode` instead of `InterfaceDefinitionNode`.
    *   Correcting method references (e.g., `ActionDefinitionNode::getActionName`).
    *   Addressing `Optional` handling for `ContextNode`.
    *   Updating calls from `getInvokes` to `getInvokeInvocations` on `StateNode`.

**The current and immediate priority is to resolve all remaining compilation errors, primarily within `AstValidator.java` and the AST node methods it consumes, to achieve a clean `mvn clean compile`.** This will unblock further development of semantic validation logic and comprehensive testing. While the foundational components are largely in place, this stabilization phase is critical for the project's health and future progress towards full DSL feature implementation and code generation capabilities.

## Current Build Status & Remaining Compilation Errors

While `AstBuilderVisitor.java` compiles cleanly, and specific errors in `ContextVariableNode.java` and `GuardDefinitionNode.java` have been resolved, the project build (`mvn clean compile`) still fails. The errors are almost entirely concentrated in `AstValidator.java` due to interactions with various AST node classes.

The main categories of *current* errors are:

1.  **Missing Methods in AST Nodes (Primarily surfaced by `AstValidator.java`):**
    *   `StateNode.java`: Missing `getTransitions()`, `getHistory()`, `getInvoke()`, `getHistoryType()`, `getDefaultHistoryTransition()`.
    *   `TransitionNode.java`: Missing `getAction()`.
    *   `InvokeStateNode.java`: Missing `getSrc()`.
    *   `MachineNode.java`: Missing `getInitialState()`.
    *   `ContextNode.java`: Missing `getVariables()`.
    *   Generic `AstNode`: Calls to `getStateName()` on `AstNode` instances (may require casting or type checking before call).

2.  **Type Mismatches and Incorrect API Usage in `AstValidator.java`:**
    *   `validateInvokeCompletionHandler` calls: Mismatch between `Optional<ssot_parser.ast.nodes.InvokeCompletionHandler>` and `Optional<ssot_parser.ast.handlers.InvokeCompletionHandler>`.
    *   `collectStateNamesRecursive` call: Argument mismatch (`List<AstNode>` passed, `List<StateNode>` expected).
    *   `TargetStateNode` to `String` conversion: `transition.getTargetState()` returns `TargetStateNode`, but is used as `String`.
    *   `Stream.collect` for `stateMap`: Attempting `StateNode::getStateName` on a `Stream<AstNode>`.
    *   `validateActionReferences` call: Argument mismatch (`List<ActionReferenceNode>` passed, `List<String>` expected).

3.  **Other Specific Issues in `AstValidator.java`:**
    *   `instanceof EnumNode` check: `typeDef` is `TypeDefNode`, so `instanceof EnumNode` might be problematic if `EnumNode` doesn't extend `TypeDefNode` directly (use `typeDef.getKind() == TypeDefNode.TypeKind.ENUM`).
    *   `StateNode.HistoryType` resolution: `HistoryType` enum not correctly resolved in `AstValidator`.

## Next Steps

1.  **(High Priority) Achieve a Clean Build (`mvn clean compile`):** This remains the top priority.
    *   **Fix `AstValidator.java` and related AST Node issues (CONTINUED):**
        *   Systematically address the errors listed in "Current Build Status & Remaining Compilation Errors" above.
        *   For missing methods: Add them to the respective AST node classes in `src/main/java/ssot_parser/ast/nodes/` with correct signatures and return types.
        *   For type mismatches/API usage: Correct the calls in `AstValidator.java` or adapt the method signatures/return types in AST nodes as appropriate.
        *   This will be an iterative process. After fixing a group of errors, re-run `mvn clean compile -e` to check progress.

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