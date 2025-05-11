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
The latest development efforts focused on enabling typed parameters and return types for action definitions. Key changes included:
*   Updates to the `SSoT.g4` grammar for `actionDefinition`.
*   Modification of `ActionDefinitionNode.java` to accommodate these new attributes.
*   Initiation of updates to `AstBuilderVisitor.java` to parse the revised grammar.
*   Enhancements to `AstValidator.java` for event definitions, initial state logic, and transition consistency.

**CRITICAL ISSUES - BUILD BROKEN:**
The project is currently **UNSTABLE and UNBUILDABLE** due to critical issues introduced in the last development cycle:
1.  **`AstBuilderVisitor.java` Corruption:** This file has been severely damaged due to accidental large-scale code deletion during an edit. It is missing critical helper methods (e.g., `extractAnnotations`, `mapAnnotations`) and its main inner class (`BlockContainerNode`). The file must be restored from version control to a state before this damage, and then the intended parsing logic for typed actions needs to be carefully re-implemented.
2.  **`AstValidator.java` Linter Errors:** New linter errors indicate that `StateType` cannot be resolved (e.g., "StateType cannot be resolved to a variable", "The method getType() from the type StateNode refers to the missing type StateType"). This is likely an import issue that needs to be fixed.

Addressing these two issues is the **TOP PRIORITY** to return the project to a buildable state.

## Current Build Status & Remaining Compilation Errors

**The project DOES NOT currently compile cleanly.** The `mvn clean compile` command will fail due to severe errors in `AstBuilderVisitor.java` (missing methods/classes) and `AstValidator.java` (unresolved `StateType`).

**Previous State (Now Invalidated):**
Prior to the recent critical issues, the project had achieved a clean `mvn clean compile` state. All previously listed compilation errors in `AstValidator.java` and related AST nodes had been addressed. This included implementing missing methods in AST nodes and correcting type mismatches. *However, this clean state is no longer current due to the new critical issues listed above.*

## Next Steps

1.  **(CRITICAL - BLOCKER) Restore `AstBuilderVisitor.java` and Fix Action Parsing:**
    *   **Revert/Restore:** Restore `src/main/java/ssot_parser/AstBuilderVisitor.java` to a known good state from version control (HEAD or an earlier commit) before the accidental code deletion. The file `/tmp/AstBuilderVisitor.head.java` may contain a recent, more complete version if restoration from git is problematic.
    *   **Re-implement Typed Action Parsing:** Carefully re-apply the necessary changes to `visitActionDefinition` to correctly parse typed parameters (from `ctx.parameterList()`) and return types (from `ctx.typeExpr()`) using the updated `ActionDefinitionNode` constructor. Ensure `ctx.ID().getText()` is used for the action name.
    *   **Verify `visitGuardDefinition`:** Ensure `ctx.ID(0).getText()` is used for the guard name.
    *   **Ensure all helper methods** (like `extractAnnotations`, `mapAnnotations`) and the **`BlockContainerNode` inner class** are present and correct.

2.  **(CRITICAL - BLOCKER) Fix `AstValidator.java` Linter Errors:**
    *   Add the explicit import `import ssot_parser.ast.type.StateType;` to `src/main/java/ssot_parser/validation/AstValidator.java`.
    *   Ensure all references to `StateType` (e.g., `parentState.getType() == StateType.COMPOUND`) resolve correctly.

3.  **(CRITICAL - BLOCKER) Achieve a Clean Build:**
    *   Run `mvn clean compile` and resolve any remaining compilation errors until the build is successful.

4.  **(High Priority - Post-Fix) AST Validation Framework Enhancement & Testing:**
    *   Once the build is stable, rigorously test and expand `AstValidator.java`'s logic.
    *   Implement checks for:
        *   **Reference Resolution:** Ensure all identifiers (state names, action names, type names, service names, etc.) are defined before use and are of the correct type for the context.
        *   **Type Checking:** Verify type compatibility in expressions, assignments, and parameters more thoroughly.
        *   **State Machine Semantics:** Expand validation for initial states (including nested ones), transition consistency, event usage, history state usage, and overall state machine integrity.
        *   **Invoke Logic:** Ensure `invoke` sources resolve correctly, and input/output mappings are type-compatible (requires further AST enhancements for invoke definitions).
        *   **Service and Interface Logic:** Validate method implementations against interface definitions, check for duplicate method names, etc.
    *   Develop a comprehensive test suite using diverse `.ssot` example files (both valid and invalid) to verify the parser, AST construction, and validator.

5.  **(Medium Priority - Post-Fix) AST Enhancement & Feature Completion:**
    *   Review and complete the AST node implementations for all DSL constructs (e.g., `InvokeDefinitionNode` to store mappings/handlers, full `ServiceDefinitionNode` details).
    *   Ensure `AstBuilderVisitor.java` fully populates these richer AST nodes for all DSL features, including `services`, `actors`, `communication`, `deployment_config`, and `dependencies` blocks.

6.  **(Medium Priority - Post-Fix) Code Generation / AST Consumers:**
    *   Begin planning and prototyping AST consumers, particularly for targets like `MERMAID_OUT` for visualization, and `TS_OUT` or `PROTO_OUT` for code/schema generation.

7.  **(Low Priority - Post-Fix) Documentation and DSL Refinement:**
    *   Keep `DSL.md` updated with any grammar or semantic refinements discovered during validation and AST building.
    *   Refine this `README.md` with detailed usage instructions for any new tools or validation outputs once the critical issues are resolved.

## ビルドと実行

Maven が導入されたため、以下のコマンドでビルドと実行が可能です。

1.  **ビルド (コンパイルとANTLRコード生成):**
    ```bash
    # プロジェクトルートで実行
    mvn clean compile
    ```
    これにより、`src/main/antlr4` 内の `.g4` ファイルからパーサーコードが `target/generated-sources/antlr4` に生成され、
    `src/main/java` 内の Java コードと共に `target/classes` にコンパイルされます。
    ***注意: 現在ビルドは失敗します。上記「Next Steps」のCRITICAL項目を解決する必要があります。***

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