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
        *   `MachineNode` は、マシン直下の `states` ブロック内に `StateNode` と `HistoryStateNode` の両方を含むことができるように更新されました。(`List<AstNode> states` を保持)
    *   `StateNode` は、条件付き遷移 (`if guard transition ...`) を表現するための `ifTransitions` (List<TransitionNode>) フィールドを持つように更新されました。
    *   `HistoryStateNode.java` がDSL仕様 (`history ID (DEEP)? (transitionSpec)? annotation*;`) と整合するように修正され、ID、名前、型、オプショナルなデフォルト遷移、アノテーションを持つようになりました。
    *   ANTLR の Visitor パターンを用いた `AstBuilderVisitor.java` が実装されており、Parse Tree から AST を構築します。
        *   `visitInvokeState` メソッドにおける `InvokeStateNode` の生成処理が修正され、正しいコンストラクタ引数（`invokeDefinitionRef` の抽出、`inputMapping`、`annotations` リストの直接使用、`onDone`/`onError` ハンドラの `Optional` 化を含む）を使用するようになりました。
        *   `parseInvokeCompletionHandler` メソッドが修正され、`InvokeCompletionHandler` の実際のコンストラクタを直接使用し、DSL文法に従って `actionReferenceList` または `transitionSpec` を正しく処理するようになりました。
        *   `visitHistoryStateDefinition` メソッドが修正され、更新された `HistoryStateNode` を正しく生成するようになりました。
    *   `types` ブロック (`struct`, `enum`, `field`, `variant`)、基本的なアノテーション (`@id`, `$name`) の AST ノードが生成されます。
    *   `machines` ブロック内の `context`, `actions`, `guards`, `invokes` 定義部分の基本的な AST 構築ロジックが `AstBuilderVisitor` に実装されました。
    *   `states` ブロック内の `state` 定義の AST 構築 (entry/exit action, on transition, conditional transition, invoke) も部分的に `AstBuilderVisitor` に実装されました。
    *   `communication` ブロックと `actors` ブロックも基本的なコンテナノードは生成されますが、詳細な内部要素の AST 構築は不完全です。
    *   **課題:**
        *   **(最優先) `AstBuilderVisitor.visitStateDefinition` の大規模リファクタリング:** 現在の `StateNode.java` の定義 (複数の `invokeInvocations` リスト、`historyStates` リスト、`StateType` enum、特定の遷移タイプリストなど、より詳細な構造を持つ) に合わせて、`visitStateDefinition` を全面的に修正し、状態の全要素 (特に合成状態、並列状態の構造、初期状態指定、各種遷移、複数のinvoke) を正確に処理できるようにする必要があります。
        *   `AstBuilderVisitor.visitMachineDefinition` における状態リストの処理を、`StateNode` と `HistoryStateNode` の両方を含むように更新する必要があります。
        *   サービス (`services` ブロック: service, interface, method など)、`deployment_config`, `dependencies` ブロックも未対応です。
*   **AST 検証 (部分的):**
    *   基本的な `AstValidator.java` が存在し、型名の一意性チェックなど、ごく一部の検証が行われます。
    *   **課題:** 詳細な意味論的検証 (参照解決、型チェック、状態マシンの妥当性検証など) は未実装です。
*   **コード生成:** 未実装です。
*   **コンパイル状況:**
    *   **重大な問題:** `mvn clean compile` を実行しても、ANTLR パーサー/レキサーのソースコードが `target/generated-sources/antlr4/ssot_parser/` に生成されていません。これが原因で、`AstBuilderVisitor.java` をはじめとする多くのJavaファイルでコンパイルエラー (型解決エラーなど) が大量に発生しています (現状100エラー)。この問題の解決が最優先事項です。
    *   `src/main/java/ssot_parser/ast/values/ValueNode.java` において、`AstNode` インターフェースの未実装メソッド (`getAnnotations`, `getId`) に関するリンターエラー、および `ValueNodeType` の定数 (例: `INTEGER`) が解決できないエラーが新たに発生しています。

## Project Status & Recent Developments

The AST construction logic within `AstBuilderVisitor.java` has undergone a significant refactoring, primarily focused on the `visitStateDefinition` method. This change enables the creation of more detailed and feature-rich `StateNode` objects, supporting various state types (atomic, compound, parallel, final), nested states, history states, multiple invocations, and comprehensive transition handling.

**Key Changes (Recent Session):**

*   **Grammar (`SSoT.g4`) Enhancements & Linting:**
    *   The `statesDefinition` rule within a `machine` now supports a mixed list of `stateDefinition` and `historyDefinition` elements. This allows top-level history states within a machine.
    *   A new `ifTransitionStatement` rule (`IF condition=guardReference annotation* transitionSpec SEMI;`) has been added to `stateBodyElement`. This allows defining conditional transitions directly within a state's body.
    *   The `invokeDefinition` and `historyDefinition` rules were also refined for more detailed specifications.
    *   **Significant linter error correction:** Resolved multiple ANTLR linter errors including label conflicts, implicit token definitions (by adding necessary keywords to the lexer), and ambiguities in optional block definitions. This makes the grammar more robust and prepares it for reliable parser generation.
*   **`AstBuilderVisitor.java` Updates:**
    *   `visitMachineDefinition`:
        *   Refactored to correctly iterate over `machineBodyElementContext` (using ANTLR's generated contexts for `contextDefinition`, `actionsDefinition`, `guardsDefinition`, `invokesDefinition`, `statesDefinition`).
        *   Correctly populates `MachineNode.states` with a list of `AstNode` (which can be `StateNode` or `HistoryStateNode`) based on the updated grammar.
        *   The `MachineNode` constructor call now uses the correct argument order.
        *   Initial state for a machine is now primarily determined by an `$initial` annotation on the `machine` line.
    *   `visitStateDefinition`:
        *   Now processes `ifTransitionStatement` from `stateBodyElement`.
        *   Calls the new `visitIfTransitionStatement` method to parse these conditional transitions.
        *   Populates the `StateNode.ifTransitions` list with `TransitionNode` objects generated from these statements.
    *   `visitIfTransitionStatement` (New Method): Parses `IfTransitionStatementContext` and creates a `TransitionNode` (using a synthetic event like `@IF:<guardName>` and the specified guard as `conditionRef`).
    *   `visitStatesDefinition`: Updated to process `stateDefinitionOrHistoryStateContext` according to grammar changes.
    *   Renamed several block-handling visitor methods for clarity and consistency (e.g., `visitActionsBlock` to `visitActionsDefinition`).

This refactoring is a crucial step towards a more expressive and capable FSM DSL. However, fully functional integration requires corresponding updates to `StateNode.java` and `MachineNode.java`, as well as the implementation of comprehensive unit tests.

## Current Development Status and Next Steps

**Summary of Progress:**

*   **ANTLR Parser Generation:** Successfully configured `pom.xml` (both `antlr4-maven-plugin` and `build-helper-maven-plugin`) to ensure ANTLR generates parser files (`SSoTParser.java`, `SSoTLexer.java`, etc.) into the correct directory (`target/generated-sources/antlr4/ssot_parser/`) with the proper package structure.
*   **AST Node Refinements:** Addressed numerous linter errors and compilation issues in several AST node classes by:
    *   Adding missing methods (e.g., `getId()` in `AstNode`).
    *   Defining missing enum constants (in `ValueNodeType`).
    *   Implementing inherited methods and correcting field access (in `ValueNode` implementations, `ServiceDefinitionNode`).
    *   Adjusting type names and visitor method signatures (in `NodeVisitor`).
    *   Correcting constructors and internal logic (in `TypeDefNode`, `ContextNode`, `SsotRoot`).
*   **`AstBuilderVisitor` Updates:** Significant effort was made to update `AstBuilderVisitor.java`:
    *   Corrected calls to constructors of several AST nodes (`SsotRoot`, `FieldNode`, `EnumVariantNode`, `ContextNode`).
    *   Replaced usage of a non-existent `TypeNode` with `TypeExprNode` in type-related visitor methods.
    *   Removed logic for `defaultValueSpec` in `visitFieldDefinition` as it was no longer in the grammar.
    *   Modified `ContextNode` and its usage in the visitor to handle block-level annotations and use `List<ContextVariableNode>`.

**Current Status:**

Despite these efforts, `mvn clean compile -e -X` still **fails with numerous compilation errors (over 100 in the last run provided in context)**. The vast majority of these errors are concentrated in `AstBuilderVisitor.java`. The primary categories of remaining errors are:

1.  **Mismatches with ANTLR-Generated Contexts:** `AstBuilderVisitor` attempts to call methods on ANTLR context objects (e.g., `ctx.serviceDefinition()`, `ctx.someToken().getText()`) that do not exist or have different signatures in the generated `SSoTParser.java`. This often occurs when a rule can produce multiple instances (list) or when an ID is a list of terminals.
2.  **Incorrect AST Node Constructor Calls:** For several AST nodes (e.g., `ActionDefinitionNode`, `GuardDefinitionNode`, `InvokeDefinitionNode`, `StateNode`), the constructor calls within `AstBuilderVisitor` do not match the actual signatures in the node classes.
3.  **Unresolved or Incorrectly Used Types:** Many types or enum members are reported as unresolvable (e.g., `StateType.NORMAL`, `TransitionTarget`, `CustomType`, `BaseType`, `ActionReferencesNode`). These might be missing, incompletely defined, or used incorrectly within `AstBuilderVisitor`.
4.  **Visitor Logic Issues:** The `AstBuilderVisitor` contains many `System.err.println` statements indicating areas with placeholder logic or known issues that need to be addressed.

**Next Steps to Achieve Successful Compilation:**

The following steps should be undertaken to resolve the outstanding compilation errors. The primary focus is `AstBuilderVisitor.java` and its interaction with the grammar and AST nodes.

1.  **Resolve ANTLR Context Method Mismatches in `AstBuilderVisitor.java`:**
    *   For each error in `AstBuilderVisitor.java` like "The method `someRule()` is undefined for the type `SomeContext`" or "The method `getText()` is undefined for the type `List<TerminalNode>`":
        *   Open `SSoT.g4` and locate the grammar rule corresponding to `SomeContext` (e.g., `servicesBlock` for `ServicesBlockContext`).
        *   Examine how `someRule` or the token (e.g., `ID`) is defined within that rule (e.g., is it `someRule*` indicating a list? Is it optional `someRule?`?).
        *   **Crucially, inspect the generated `target/generated-sources/antlr4/ssot_parser/SSoTParser.java`** to see the actual methods available on `SomeContext`. For example, if `serviceDefinition` is a list in the grammar, the context method will likely be `serviceDefinition()` returning a `List<ServiceDefinitionContext>`, and you'll need to iterate it (e.g., `for (ServiceDefinitionContext serviceCtx : ctx.serviceDefinition())`).
        *   If a token like `ID` can appear multiple times, `ctx.ID()` might return `List<TerminalNode>`. Access elements by index (e.g., `ctx.ID(0).getText()`), ensuring the list is not null/empty.
        *   Update `AstBuilderVisitor.java` to use the correct methods and access patterns based on the generated parser and the grammar. **This is the highest priority.**

2.  **Fix AST Node Constructor Calls in `AstBuilderVisitor.java`:**
    *   For each "constructor ... is undefined" error (e.g., for `ActionDefinitionNode`, `GuardDefinitionNode`, `StateNode`, `InvokeDefinitionNode`):
        *   Open the AST node's Java file (e.g., `src/main/java/ssot_parser/ast/nodes/ActionDefinitionNode.java`).
        *   Compare the declared constructor signature(s) with the arguments being passed in `AstBuilderVisitor.java`.
        *   Ensure the number of arguments, their types, and their order match precisely.
        *   Modify the constructor call in `AstBuilderVisitor.java`. If the AST node's constructor is incorrect or incomplete for its intended use, update the node class first.

3.  **Define or Correct Missing/Incorrect Types and Enum Usage:**
    *   For errors like "`StateType.NORMAL` cannot be resolved" or "`CustomType` cannot be resolved":
        *   **StateType:** Ensure `src/main/java/ssot_parser/ast/type/StateType.java` exists and defines `NORMAL`, `INITIAL`, `FINAL`, etc., as public enum constants.
        *   **TransitionTarget, StateTarget, HistoryTarget:** These seem to be AST node types for representing where a transition goes. Ensure these classes/interfaces are defined (likely in `ssot_parser.ast.nodes` or `ssot_parser.ast.transitions`) and that `AstBuilderVisitor` correctly creates and uses them.
        *   **BaseType, CustomType (in `visitTypeExpr`):** The `visitTypeExpr` method was attempting to instantiate `BaseType` and `CustomType`. It should instead be returning instances of concrete `TypeExprNode` implementers (like `PrimitiveTypeNode`, `RefTypeNode`, `OptionalTypeNode`, etc., which should exist in `ssot_parser.ast.type`). The casts to `ssot_parser.ast.type.TypeNode` within `visitTypeExpr` should also be `TypeExprNode`.
        *   **ActionReferencesNode:** This class is used in `visitStateDefinition` but might not be defined. Determine its purpose and define it, or refactor its usage.
        *   Ensure all necessary imports are present in `AstBuilderVisitor.java`.

4.  **Address Remaining Errors and Refactor `AstBuilderVisitor.java`:**
    *   **`getText()` on `List<TerminalNode>`:** (e.g., L349, L370) When `ctx.ID()` returns a list because the grammar allows multiple IDs, you must access an element first: `ctx.ID(0).getText()` (after checking for null/empty list).
    *   **Undefined methods on ANTLR contexts:** (e.g., L375 `inlineGuardExpression()`, L411 `visitInvokeSource` argument mismatch, L461 `historyStateDefinition()`, L523 `stateBody()`, etc.) These point to a deep mismatch with the `SSoT.g4` grammar and the generated parser. Systematically check the grammar for these rules and how they are structured, then consult `SSoTParser.java` for the correct context methods.
    *   **`Objects.nonNull`:** (L1089) Ensure `java.util.Objects` is imported.
    *   **`getName()`/`getValue()` on `AnnotationNode`:** (L1035, L1050, etc.) `AnnotationNode` has public fields `name` and `value`. Access them directly (e.g., `annotation.name`, `annotation.value`). The `getValue()` method in the error message refers to `Optional::getValue`, not a method on `AnnotationNode`. `annotation.value` itself is an `Object`, so if you expect an `Optional<String>` from it, further processing is needed. `AnnotationNode` does have an `isIdAnnotation()` method.
    *   Go through all `System.err.println` messages in `AstBuilderVisitor.java` and resolve the underlying issues.

5.  **Iterative Compilation:**
    *   After each set of targeted fixes, run `mvn clean compile -e -X`. This will help track progress and catch new errors as they surface.

## 今後のステップ (ロードマップ案)

1.  **(最優先) ANTLR パーサー生成の修復:**
    *   `mvn clean compile` を実行した際に、ANTLR パーサー (`SSoTParser.java`)、レキサー (`SSoTLexer.java`)、ベースビジター (`SSoTBaseVisitor.java`) 等が `target/generated-sources/antlr4/ssot_parser/` ディレクトリに正しく生成されない問題を診断し、修正する。
        *   Maven の `antlr4-maven-plugin` の設定 (`pom.xml` 内) に誤りがないか確認する。
        *   `SSoT.g4` ファイル自体に、コード生成を妨げるエラー (ANTLRツール側ではエラーとして検知されにくいアクションコード内の構文ミスなど) がないか確認する。
    *   パーサーが正しく生成された後、再度コンパイルし、`AstBuilderVisitor.java` などで発生している型解決エラーが解消されることを確認する。
2.  **(次優先) `ValueNode.java` のリンターエラー修正:**
    *   `src/main/java/ssot_parser/ast/values/ValueNode.java` 内の `IntValueNode`, `FloatValueNode`, `RefValueNode` クラスに、`AstNode` インターフェースで定義されている `getAnnotations()` および `getId()` メソッドを実装する。
    *   `ValueNodeType` enum が適切に定義され、その定数 (`INTEGER`, `FLOAT`, `REFERENCE` など) が正しく参照できるようにする (必要であれば `ValueNodeType.java` の作成または修正)。
3.  **(継続中・重要) 状態マシン関連の AST 構築に関するユニットテストを拡充する:**
    *   ANTLR生成と `ValueNode` の問題が解決し、主要部分がコンパイル可能になった後、特に新しい `StateNode` 構造、`MachineNode` 構造、`visitStateDefinition` および `visitMachineDefinition` のロジック、条件付き遷移、トップレベル履歴状態に対して、包括的なユニットテストを作成・実行する。
4.  **(次点) AST 検証の強化 (State Machine 中心):**
    *   状態マシン内の状態遷移の妥当性、参照されている Action/Guard/Context/Invoke/State の存在確認、初期状態の検証など、意味論的な検証を `AstValidator` に追加する。
5.  **アノテーション処理の拡充:** `@id` や各種 `$annotation` をパースし、対応する AST ノードに情報を付加するロジックを確認・拡充する。検証ロジックでも利用する。
6.  **Service/Interface の Visitor/AST 実装:** `services` ブロック内の要素を処理する Visitor と AST ノードを実装する。
7.  **テストの導入と拡充:** 特に Service/Interface の AST 構築と検証を中心に、ユニットテストを追加する。
8.  **エラーハンドリングの強化:** パースエラーや AST 検証エラーをより分かりやすく報告するように改善する。
9.  **その他のブロックの Visitor/AST 実装:** `actors`, `communication`, `deployment_config`, `dependencies` ブロックに対応する Visitor と AST ノードを実装する。
10. **Imports の処理:** `import` 文を解釈し、別ファイルの定義を解決できるようにする。
11. **コード生成器の実装:** AST からターゲット言語 (例: Mermaid グラフ定義) を出力する機能を追加する (初期段階)。

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