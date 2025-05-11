# FSM DSL Project (SSoT Parser - Java/ANTLR)

## 概要

このプロジェクトは、状態マシン、サービス、型定義などを含むシステム仕様を記述するための独自 DSL (`.ssot` ファイル) を定義し、そのパーサーと基本的な AST (Abstract Syntax Tree) ビルダーを Java と ANTLR v4 を使用して開発することを目的としています。将来的には、この AST を利用して各種プログラミング言語のコード、スキーマ定義、ドキュメントなどを自動生成することを目指します。

DSL の仕様は [`DSL.md`](./DSL.md) に定義されています。

## Project Status & Recent Developments

**Core Functionality:**
The project is developing a Java/ANTLR-based parser for a custom Domain-Specific Language (`.ssot` files). This DSL is designed to define comprehensive system specifications, including types, services, state machines, actors, communication, deployment configurations, and dependencies. The ANTLR grammar (`SSoT.g4`) and Maven build process (`pom.xml`) are established.

**Abstract Syntax Tree (AST):**
A rich set of Java classes (`src/main/java/ssot_parser/ast/`) represents the DSL elements in the AST. The core `AstBuilderVisitor.java` class is responsible for constructing this AST from the ANTLR parse tree.

**Recent Achievements:**
A significant effort was undertaken to stabilize and correct the `AstBuilderVisitor.java` and its associated AST node interactions. This resolved a comprehensive list of compilation errors and logical issues within the visitor, including corrections for `RefValueNode` resolution, `visitGuardDefinition` logic, and `HistoryStateNode` constructor calls. The `AstBuilderVisitor.java` file now compiles cleanly.

## Current Build Status & Remaining Compilation Errors

While `AstBuilderVisitor.java` now compiles cleanly, the project build (`mvn clean compile`) currently fails due to a new set of compilation errors primarily in the AST node definitions and the `AstValidator`. The main categories of errors are:

1.  **AST Node Interface Implementation Issues:**
    *   **Missing `getId()` Implementation:** Several AST node classes (e.g., `AnnotationNode`, `EventHandlerNode`, `TransitionConfig`) are missing the `getId()` method required by the `AstNode` or `NodeWithId` interface.
    *   **Incorrect `getAnnotations()` Signature/Implementation:** Many AST node classes (e.g., `EventHandlerNode`, `ConditionalTransitionNode`, `OptionalTypeNode`, `MapTypeNode`) implement `getAnnotations()` with a `List<AnnotationNode>` return type instead of the `Map<String, Object>` required by `AstNode`.
2.  **AST Node Constructor Mismatches:**
    *   Errors like `constructor X in class Y cannot be applied to given types` are present, for example, in `ConditionalTransitionNode`'s usage of the `TransitionNode` constructor. These indicate that the arguments passed during object creation do not match any available constructor signatures.
3.  **Missing Visitor Methods in `NodeVisitor`:**
    *   Errors such as `cannot find symbol: method visitXNode(ssot_parser.ast.nodes.XNode)` (e.g., for `ServiceNode`, `GuardNode`, `MachineNode`, `EnumNode`, `RefTypeNode`, `ActionNode`, `PrimitiveTypeNode`) indicate that the `NodeVisitor` interface is missing corresponding `visit...` methods for these AST types.
4.  **Compilation Errors in `AstValidator.java`:**
    *   This class has a large number of `cannot find symbol` errors for various getter methods (e.g., `getTypeDefs`, `getMachineNodes`, `getInvokes`, `getTransitions`) on AST node objects. This suggests that either the getter methods are missing/misnamed in the AST nodes, or the validator is using outdated/incorrect method calls.
    *   There are also `incompatible types` errors, likely stemming from the above issues.


## Next Steps

1.  **(High Priority) Achieve a Clean Build (`mvn clean compile`):** This is the top priority and involves the following sub-steps:
    *   **Fix `AstNode` Interface Implementations:**
        *   Iterate through all AST node classes in `src/main/java/ssot_parser/ast/nodes/` and `src/main/java/ssot_parser/ast/type/`.
        *   For each class implementing `AstNode` (or `NodeWithId`):
            *   Ensure `Optional<Long> getId()` is implemented. If the node type doesn't typically have an ID, return `Optional.empty()`. If it can have an `@id` annotation, extract it from its internal list of `AnnotationNode`s.
            *   Ensure `Map<String, Object> getAnnotations()` is implemented correctly, returning a map derived from its internal `List<AnnotationNode>` (if applicable), rather than the list itself.
    *   **Resolve AST Node Constructor Mismatches:**
        *   Carefully check the constructor signatures in classes like `TransitionNode` and ensure that calling code (e.g., in `ConditionalTransitionNode`) uses the correct arguments.
    *   **Update `NodeVisitor.java`:**
        *   Add any missing `visitXNode(XNode node)` methods to the `NodeVisitor` interface for all AST node types that should be visitable.
    *   **Fix `AstValidator.java`:**
        *   Once the AST node classes are compiling and their interfaces are consistent, systematically review `AstValidator.java`.
        *   Update method calls to use correct getter names and signatures as defined in the AST nodes.
        *   If necessary, add missing getter methods to the AST node classes if the validator requires access to data not currently exposed.
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