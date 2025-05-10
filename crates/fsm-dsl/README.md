# FSM DSL Project (SSoT Parser - Java/ANTLR)

## 概要

このプロジェクトは、状態マシン、サービス、型定義などを含むシステム仕様を記述するための独自 DSL (`.ssot` ファイル) を定義し、そのパーサーと基本的な AST (Abstract Syntax Tree) ビルダーを Java と ANTLR v4 を使用して開発することを目的としています。将来的には、この AST を利用して各種プログラミング言語のコード、スキーマ定義、ドキュメントなどを自動生成することを目指します。

DSL の仕様は [`DSL.md`](./DSL.md) に定義されています。

## Project Status & Recent Developments

**Core Functionality:**
The project is developing a Java/ANTLR-based parser for a custom Domain-Specific Language (`.ssot` files). This DSL is designed to define comprehensive system specifications, including:
*   **Types:** Structs and enums.
*   **Services:** Interfaces and methods.
*   **State Machines:** Context, actions, guards, states (atomic, compound, parallel, final), transitions (event-triggered, timed, conditional), invocations, and history states.
*   **Actors:** Definitions for system actors.
*   **Communication:** Protocols, channels, and events.
*   **Deployment Configuration:** Environment and infrastructure details.
*   **Dependencies:** External dependencies (e.g., for Rust, NodeJS targets).

The ANTLR grammar (`SSoT.g4`) and Maven build process (`pom.xml`) are established, with ANTLR parser/lexer code generation functioning.

**Abstract Syntax Tree (AST):**
A rich set of Java classes (`src/main/java/ssot_parser/ast/`) represents the DSL elements in the AST. The core `AstBuilderVisitor.java` class is responsible for constructing this AST from the ANTLR parse tree.

**Recent Achievements (Key Fixes and Enhancements):**

A significant effort was recently undertaken to stabilize and correct the `AstBuilderVisitor.java` and its associated AST node interactions. This resolved a comprehensive list of compilation errors and logical issues:

1.  **Syntax Correction:** Removed an extraneous closing brace in `AstBuilderVisitor.java`.
2.  **Visibility & Type Resolution:**
    *   Corrected `PrimitiveTypeNode.PrimitiveType.BOOLEAN` to `PrimitiveTypeNode.PrimitiveType.BOOL`.
    *   Addressed `HistoryStateNode.HistoryType` resolution by importing `ssot_parser.ast.nodes.state.HistoryStateType` and updating usages.
    *   Changed `StateTargetNode` to `TargetStateNode` in `visitHistoryDefinition`.
3.  **AST Node Constructor Mismatches Corrected in `AstBuilderVisitor.java`:**
    *   **`TransitionNode` (in `visitIfTransitionStatement`):** Updated to the 15-argument signature, ensuring correct argument mapping for source/target states, event, conditions, actions, and annotations.
    *   **`InvokeStateNode`:** Rectified to use the correct 6-argument signature, passing the appropriate annotation list.
    *   **`AnnotationNode`:** Modified calls in `visitAnnotation` to include the required `isIdAnnotation` boolean flag.
    *   **`TransitionSpecNode` (in `visitHistoryDefinition`):** Ensured correct 5-argument constructor usage and explicit generic types.
4.  **Method Implementation for AST Values:**
    *   Added `getActualValue()` to the `ValueNode` interface and implemented it across all concrete value node subclasses (`StringValueNode`, `NumberValueNode`, `RefValueNode`, `ObjectValueNode`, `ArrayValueNode`, `NullValueNode`, `BooleanValueNode`).
    *   Added `getFields()` to `ObjectValueNode`.
    *   Updated `AstBuilderVisitor.java` to use these new methods.

These changes have significantly improved the correctness and robustness of the AST construction logic.

## Current Build Status & Remaining Linter Errors

While the major recent fixes have been applied, `AstBuilderVisitor.java` still exhibits a few Java compilation (linter) errors that prevent a clean build (`mvn clean compile`). Addressing these is the immediate priority. The primary remaining errors are:

1.  **`RefValueNode` Visibility/Resolution:**
    *   Errors: `The type ssot_parser.ast.values.RefValueNode is not visible` (import statement) and `RefValueNode cannot be resolved to a type` (usage in `visitInvokeDefinition`, `visitReferenceValue`).
    *   **Analysis:** This suggests a persistent issue with `RefValueNode.java`'s visibility (despite being public) or its package declaration/import, or a deeper build path/classpath problem.
2.  **`visitGuardDefinition` Issues:**
    *   Error: `The method getText() is undefined for the type List<TerminalNode>` (line ~579, on `ctx.ID().getText()`).
    *   Error: `TerminalNode cannot be resolved to a type` (line ~612).
    *   Error: `The constructor GuardNode(...) is undefined` (line ~620).
    *   **Analysis:** These point to incorrect handling of ANTLR context objects (especially `ID()`, which might be a list) and a mismatch with the `GuardNode` constructor.
3.  **`HistoryStateNode` Constructor Mismatch:**
    *   Error: `The constructor HistoryStateNode(...) is undefined` (line ~1057).
    *   **Analysis:** The arguments passed during `HistoryStateNode` creation in `visitHistoryDefinition` do not match an existing constructor signature in `HistoryStateNode.java`.

## Next Steps

1.  **(High Priority) Resolve All Remaining Linter Errors in `AstBuilderVisitor.java`:**
    *   **`RefValueNode`:**
        *   Verify `src/main/java/ssot_parser/ast/values/RefValueNode.java` has the correct `package ssot_parser.ast.values;` and is `public class RefValueNode ...`.
        *   Ensure the import in `AstBuilderVisitor.java` is `import ssot_parser.ast.values.RefValueNode;`.
        *   Perform a clean Maven build (`mvn clean install` or `mvn clean compile`) to rule out stale build artifacts.
    *   **`visitGuardDefinition` Method:**
        *   For `ctx.ID().getText()`: If `ctx.ID()` returns a list, access the specific `TerminalNode` (e.g., `ctx.ID(0).getText()`) or iterate. Ensure `ID()` is not empty and `getText()` is available.
        *   For `TerminalNode cannot be resolved`: Add `import org.antlr.v4.runtime.tree.TerminalNode;` to `AstBuilderVisitor.java`.
        *   For `GuardNode` constructor: Consult `GuardNode.java` for the exact constructor signature (number, types, and order of arguments) and adjust the call `new GuardNode(id, guardName, parameter, returnType, annotationMap)` accordingly.
    *   **`HistoryStateNode` Constructor:**
        *   Examine the constructor(s) in `src/main/java/ssot_parser/ast/nodes/HistoryStateNode.java`.
        *   Modify the call `new HistoryStateNode(id, historyId, type, defaultTransition, annotationMap)` in `visitHistoryDefinition` within `AstBuilderVisitor.java` to match an available constructor.
    *   **Iterative Compilation:** After each targeted fix, run `mvn clean compile -e` to track progress.

2.  **(Post-Compilation) AST Validation Framework:**
    *   Develop and integrate a robust validation phase that runs after AST construction.
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
    *コンパイルエラーが発生する場合は、上記「Current Build Status & Remaining Linter Errors」および「Next Steps」セクションを参照してください。*

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