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
Significant effort has been dedicated to achieving a compilable and stable AST and validator. Key achievements and the primary focus of the last development cycle were:
*   **Resolving all compilation errors:** This was the main goal. It involved:
    *   Implementing missing methods in various AST nodes (`StateNode`, `TransitionNode`, `InvokeStateNode`, `MachineNode`, `ContextNode`) as required by `AstValidator.java`.
    *   Correcting type mismatches and API usage in `AstValidator.java`. This included:
        *   Standardizing `InvokeCompletionHandlerNode` and updating its usage in `InvokeStateNode`, `AstBuilderVisitor`, and `AstValidator`.
        *   Ensuring correct list types and filtering for methods like `collectStateNamesRecursive`.
        *   Properly handling `TargetStateNode` return types instead of assuming `String`.
        *   Correcting stream collection for `AstNode` lists to `StateNode` specific maps.
        *   Adjusting logic for `ContextVariableNode` and its name retrieval.
        *   Refining how `TypeDefNode` kinds (ENUM vs. STRUCT) are handled in validation logic, particularly for `validateEnum` and `validateStruct`.
    *   Iteratively compiling and fixing new errors as they emerged.

**The project has now achieved a clean `mvn clean compile` state.** This is a major milestone, unblocking further development of semantic validation logic and comprehensive testing.

## Current Build Status & Remaining Compilation Errors

**The project now compiles cleanly with `mvn clean compile`.**

All previously listed compilation errors in `AstValidator.java` and related AST nodes have been addressed. This included:
1.  **Implemented Missing Methods in AST Nodes:**
    *   `StateNode.java`: Added `getTransitions()`, `getHistory()`, `getInvoke()`. Issues related to `getHistoryType()` and `getDefaultHistoryTransition()` were resolved by accessing these through `HistoryStateNode` instances obtained via `StateNode.getHistory()`.
    *   `TransitionNode.java`: Added `getAction()` returning `Optional<ActionReferenceNode>`.
    *   `InvokeStateNode.java`: Added `getSrc()`.
    *   `MachineNode.java`: Added `getInitialState()` returning `Optional<String>`.
    *   `ContextNode.java`: Added `getVariables()` (delegating to `getFields()`).
    *   Calls to `getStateName()` on `AstNode` instances are now handled with appropriate type checking and casting (e.g., in `validateUnreachableStates`).

2.  **Addressed Type Mismatches and Incorrect API Usage in `AstValidator.java`:**
    *   `validateInvokeCompletionHandler` calls: Now correctly uses `InvokeCompletionHandlerNode`.
    *   `collectStateNamesRecursive` call: Now correctly filters and casts `List<AstNode>` to `List<StateNode>`.
    *   `TargetStateNode` to `String` conversion: Now correctly uses `TargetStateNode` methods like `getStateName()` and `getType()`.
    *   `Stream.collect` for `stateMap`: Correctly filters and casts `Stream<AstNode>` to `Stream<StateNode>` before collection.
    *   `validateActionReferences` call: Adapted where necessary (though many calls already expected `List<String>` which `ActionReferenceNode::getActionName` provides).

3.  **Addressed Other Specific Issues in `AstValidator.java`:**
    *   `TypeDefNode` kind checking: Logic in `validateTypes` now uses `typeDef.getKind()` and `validateEnum`'s signature was changed to accept `TypeDefNode`.
    *   History state handling in `validateUnreachableStates`: Logic now correctly uses `StateNode.getHistory()` to get an `Optional<HistoryStateNode>` and then calls appropriate methods on it.

With the codebase compiling, the next phase can focus on enhancing the semantic validation and overall robustness of the parser and AST.

## Next Steps

1.  **(High Priority) AST Validation Framework Enhancement & Testing:**
    *   With `AstValidator.java` compiling, rigorously test and expand its validation logic.
    *   Implement checks for:
        *   **Reference Resolution:** Ensure all identifiers (state names, action names, type names, service names, etc.) are defined before use and are of the correct type for the context.
        *   **Type Checking:** Verify type compatibility in expressions, assignments, and parameters more thoroughly.
        *   **State Machine Semantics:** Expand validation for initial states (including nested ones), transition consistency, event usage, history state usage, and overall state machine integrity.
        *   **Invoke Logic:** Ensure `invoke` sources resolve correctly, and input/output mappings are type-compatible (requires further AST enhancements for invoke definitions).
        *   **Service and Interface Logic:** Validate method implementations against interface definitions, check for duplicate method names, etc.
    *   Develop a comprehensive test suite using diverse `.ssot` example files (both valid and invalid) to verify the parser, AST construction, and validator.

2.  **AST Enhancement & Feature Completion:**
    *   Review and complete the AST node implementations for all DSL constructs (e.g., `InvokeDefinitionNode` to store mappings/handlers, `ActionDefinitionNode` to store parameters/return types, full `ServiceDefinitionNode` details).
    *   Ensure `AstBuilderVisitor.java` fully populates these richer AST nodes for all DSL features, including `services`, `actors`, `communication`, `deployment_config`, and `dependencies` blocks.

3.  **Code Generation / AST Consumers:**
    *   Begin planning and prototyping AST consumers, particularly for targets like `MERMAID_OUT` for visualization, and `TS_OUT` or `PROTO_OUT` for code/schema generation.

4.  **Documentation and DSL Refinement:**
    *   Keep `DSL.md` updated with any grammar or semantic refinements discovered during validation and AST building.
    *   Refine this `README.md` with detailed usage instructions for any new tools or validation outputs.

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