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
    *   Created new AST Node classes: `ActionReferenceNode.java`, `TransitionSpecNode.java`, `GuardReferenceNode.java`, `TargetStateNode.java`.

This refactoring is a crucial step towards a more expressive and capable FSM DSL. However, fully functional integration requires corresponding updates to `StateNode.java` and `MachineNode.java`, as well as the implementation of comprehensive unit tests.

## Current Development Status and Next Steps

**Project Summary:**

The project is developing a Java/ANTLR-based parser for a custom Domain-Specific Language (`.ssot` files) used to define state machines, services, and types. The ANTLR grammar (`SSoT.g4`) and Maven build process (`pom.xml`) are largely in place, with ANTLR parser/lexer code generation functioning correctly.

Significant progress has been made in defining and refactoring Abstract Syntax Tree (AST) node classes (primarily within `src/main/java/ssot_parser/ast/`) and the core `AstBuilderVisitor.java` (in `src/main/java/ssot_parser/`), which is responsible for constructing the AST from the ANTLR parse tree. Key achievements include:

*   **New AST Node Creation:** Introduced several new AST node classes to support richer DSL features, including `ActionReferenceNode`, `TransitionSpecNode`, `GuardReferenceNode`, `TargetStateNode`, `DurationNode`, `RefTypeNode` (for type references), and `RefValueNode` (for value references).
*   **Enum Definitions:** Essential enums within AST nodes, such as `PrimitiveTypeNode.PrimitiveType`, `StateType`, `TransitionNode.TransitionType`, and `HistoryStateNode.HistoryType`, have been defined or corrected to accurately model DSL constructs.
*   **`AstBuilderVisitor.java` Refactoring:** This central class has undergone extensive modifications:
    *   Improved handling of ANTLR parse tree contexts for various DSL blocks (file, types, services, machines, context, actions, guards, invokes).
    *   Systematic alignment of AST node constructor calls with their actual definitions. This was a major focus and resolved many previous errors.
    *   Enhanced processing of state definitions (`visitStateDefinition`), including support for nested states, history states, and various transition types by adapting to a more detailed `StateNode` structure.
    *   Refined parsing of transitions (`visitIfTransitionStatement`, and paving the way for `visitOnTransition`, `visitAfterTransition`) to use the refactored `TransitionNode`.
    *   Improved annotation processing and the addition/refinement of helper methods (e.g., `extractIdFromList`, `extractActionReferenceNodes`, `parseInvokeCompletionHandler`).

**Current Challenge & Next Steps to Achieve Successful Compilation:**

Despite resolving many previous issues, `AstBuilderVisitor.java` still has Java compilation (linter) errors that prevent a clean build (`mvn clean compile`). The immediate goal is to eliminate these remaining errors. The primary categories are:

1.  **Syntax Error:** A simple syntax error (an extraneous closing brace) in `AstBuilderVisitor.java`.
2.  **AST Node Constructor Mismatches:** Calls to constructors for `TransitionNode`, `InvokeStateNode`, and `AnnotationNode` in `AstBuilderVisitor.java` do not perfectly align with their current definitions. This often involves ensuring the correct number, order, and type of arguments.
3.  **Unresolved Types/Visibility:**
    *   `RefValueNode` is reported as not visible, possibly due to a missing `public` modifier or an incorrect package statement (though it appears correct).
    *   `PrimitiveTypeNode.PrimitiveType.BOOLEAN` is not resolving, though the enum and constant are defined. This might be a stale linter issue or a subtle import problem.
    *   `HistoryStateNode.HistoryType` is not resolving.
    *   `StateTargetNode` (an older name) is used instead of the correct `TargetStateNode`.
4.  **Missing Methods on AST Nodes:**
    *   `ValueNode` and its implementations need a consistent way to get the underlying Java value (e.g., an `getActualValue()` method).
    *   `ObjectValueNode` needs a `getFields()` method.
5.  **Potential `ValueNodeType` Issue:** `RefValueNode` attempts to return `ValueNodeType.REFERENCE`, which implies this enum and constant must be correctly defined.

The following refined steps should be undertaken to resolve these outstanding compilation errors:

1.  **Fix Syntax Error in `AstBuilderVisitor.java`:**
    *   Remove the extraneous `}` at the very end of `AstBuilderVisitor.java` (around line 1216).

2.  **Correct `TransitionNode` Constructor Call:**
    *   In `AstBuilderVisitor.java` (around line 865, in `visitIfTransitionStatement`), ensure the `new TransitionNode(...)` call precisely matches the 10-argument constructor defined in `TransitionNode.java`:
        `TransitionNode(Optional<Long> id, String event, TargetStateNode targetState, Optional<GuardReferenceNode> condition, List<ActionReferenceNode> actions, List<GuardReferenceNode> guards, List<String> allowedActors, Optional<DurationNode> delay, TransitionType type, Map<String, Object> annotationsMap)`
    *   Verify argument order and types meticulously. The linter previously indicated a type mismatch for the `condition` argument, suggesting an order or type issue there.
    *   Apply similar corrections to `visitOnTransition` and `visitAfterTransition` to use this new constructor.

3.  **Address `RefValueNode` and `PrimitiveTypeNode.PrimitiveType.BOOLEAN` Issues:**
    *   Confirm `RefValueNode.java` is `public class RefValueNode...` and its package is `ssot_parser.ast.values`. Ensure the import in `AstBuilderVisitor.java` is correct.
    *   Verify `PrimitiveTypeNode.java` correctly defines `public enum PrimitiveType { ..., BOOLEAN, ... }` and is properly imported in `AstBuilderVisitor.java`.

4.  **Correct `InvokeStateNode` and `AnnotationNode` Constructor Calls:**
    *   Consult `InvokeStateNode.java` and `AnnotationNode.java` for their exact constructor signatures.
    *   Modify the calls in `AstBuilderVisitor.java` (lines 966 for `InvokeStateNode`, lines 1078 and 1104 for `AnnotationNode`) to match.

5.  **Implement `ValueNode.getActualValue()` and `ObjectValueNode.getFields()`:**
    *   Define `Object getActualValue();` in the `ValueNode` interface (or abstract class).
    *   Implement this method in all concrete subclasses of `ValueNode` (e.g., `StringValueNode`, `NumberValueNode`, `RefValueNode`, `ObjectValueNode`, `ArrayValueNode`, `BooleanValueNode`, `NullValueNode`) to return their specific underlying Java value.
    *   Add `public Map<String, ValueNode> getFields()` to `ObjectValueNode.java`.
    *   Update `AstBuilderVisitor.java` (lines 1087, 1089) to use these methods.

6.  **Fix `HistoryStateNode.HistoryType` and `StateTargetNode` Usage:**
    *   Ensure `HistoryStateNode.java` defines `public enum HistoryType { SHALLOW, DEEP }` and it's imported/used correctly in `AstBuilderVisitor.java` (line 981).
    *   In `AstBuilderVisitor.java` (line 1026), change `new StateTargetNode(...)` to `new TargetStateNode(...)`.

7.  **Define `ValueNodeType.REFERENCE` (if needed):**
    *   If `RefValueNode` uses `ValueNodeType.REFERENCE`, ensure the `ValueNodeType` enum (likely in `ssot_parser.ast.values`) defines this constant.

8.  **Iterative Compilation:**
    *   After each targeted fix, run `mvn clean compile -e -X`. This will help track progress and catch new errors as they surface.

## 今後のステップ (ロードマップ案)

1.  **(最優先) `AstBuilderVisitor.java` のコンパイルエラー完全解消:**
    *   上記「Next Steps to Achieve Successful Compilation」に記載されているタスクを完了し、`mvn clean compile` がエラーなく成功するようにする。
2.  **(重要) `visitOnTransition` および `visitAfterTransition` の更新:**
    *   `AstBuilderVisitor.java` 内の `visitOnTransition` および `visitAfterTransition` メソッドをリファクタリングし、新しい `TransitionNode` のコンストラクタを使用するようにする。これには、イベント名、遅延 (`DurationNode` のパース)、ターゲット状態、および `TransitionSpecNode` からアクション、ガードなどを適切に抽出する処理が含まれる。
3.  **(継続中・重要) 状態マシン関連の AST 構築に関するユニットテストを拡充する:**
    *   `AstBuilderVisitor.java` のコンパイルが成功した後、特に `StateNode`, `MachineNode`, `TransitionNode` および関連するビジターメソッドのロジックに対して、包括的なユニットテストを作成・実行する。
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