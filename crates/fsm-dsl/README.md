# fsm-dsl Crate

[![crates.io](https://img.shields.io/crates/v/fsm-dsl.svg)](https://crates.io/crates/fsm-dsl) [![docs.rs](https://docs.rs/fsm-dsl/badge.svg)](https://docs.rs/fsm-dsl)

このクレートは、 `.ssot` (Single Source of Truth) ファイル形式で記述されたステートマシン記述言語 (DSL) をパースするためのライブラリです。 `DSL.md` で定義された構文に基づいて、 `.ssot` ファイルを解析し、抽象構文木 (AST) を生成します。

## 目的

-   `.ssot` ファイル形式の定義に基づき、ステートマシン、型、サービスなどの定義を一元管理する。
-   `.ssot` ファイルから Rust のデータ構造 (AST) を生成し、他のツール (コードジェネレーター、バリデーター、ビジュアライザーなど) で利用可能にする。
-   型安全なモデル駆動開発を支援する。

## 現在の機能

-   `.ssot` ファイルの基本的な構文解析:
    -   ファイル ID (`@0x...;`)
    -   インポート文 (`import "..."`)
    -   コメント (`# ...`)
-   トップレベルブロックの認識:
    -   `types {}`
    -   `actors {}` (プレースホルダー)
    -   `communication {}` (プレースホルダー)
    -   `services {}` (プレースホルダー)
    -   `machines {}` (プレースホルダー)
    -   `deployment_config {}` (プレースホルダー)
-   `types {}` ブロックの詳細な解析:
    -   `struct` 定義 (フィールド、ID、アノテーション付き)
    -   `enum` 定義 (バリアント、ID、アノテーション付き)
    -   型指定子: 単純型 (例: `string`), `list<T>`, `optional<T>`
    -   アノテーション:
        -   `$description("...")`
        -   `$validate(...)` (キー: 値形式の引数 - 文字列/整数)
        -   `$db(...)` (キー: 値形式の引数 - 文字列/整数)
        -   `$meta(...)` (キー: 値形式の引数 - 文字列/整数)
        -   `$genericFlag;`
        -   `$genericKeyValue("...")`
-   基本的なテストケースによるパーサーの検証。

## 今後のロードマップ

1.  **ステートマシン構文の実装:**
    -   `machines {}` ブロック内の詳細な構文解析:
        -   `machine` 定義
        -   `context` 定義 (フィールド)
        -   `states {}` ブロックとネストされた状態
        -   `initial`, `final`, `parallel`, `history` 状態
        -   イベントハンドラ (`on EVENT ...`)
        -   遅延遷移 (`after DURATION ...`)
        -   遷移 (`transition TARGET { action ..., guard ... }`)
        -   `actions {}` ブロックとアクション定義
        -   `guards {}` ブロックとガード定義
        -   `invokes {}` ブロックと呼び出し定義 (サービス、プロミス、マシン)
2.  **サービス/通信/アクター構文の実装:**
    -   `services {}` ブロック: `interface`, `service` 定義 (`extends`, `implements`, `$route`, `$protocol` など)
    -   `communication {}` ブロック: `protocol`, `channel`, `event` 定義
    -   `actors {}` ブロック: `actor` 定義
3.  **デプロイメント構文の実装:**
    -   `deployment_config {}` ブロック: `environment`, `infrastructure`, `deployment` 定義
4.  **高度な型/アノテーション値のサポート:**
    -   `map<K, V>` 型指定子
    -   アノテーション値としての `boolean`, `list`, `object` (ネスト構造)
5.  **エラー報告の改善:** より詳細で位置情報に基づいたエラーメッセージ。
6.  **テストカバレッジの向上:** 特に複雑なケースやエッジケースを含むテストの追加。
7.  **(任意) コードジェネレーターの追加:** AST から Rust コードや他の形式 (Mermaid, PlantUML など) を生成する機能。

## 使用例

```rust
use fsm_dsl::parser::{parse_ssot_content, ParseError};
use fsm_dsl::ast::SsotAst;

fn main() -> Result<(), ParseError> {
    let ssot_content = r#"
        @0x1234567890abcdef;
        import "/path/to/base.ssot";

        types {
            $description("Example types");

            struct Point @id(0) {
                x: i32 @id(0);
                y: i32 @id(1);
            }

            enum Color @id(1) {
                RED @id(0);
                GREEN @id(1);
                BLUE @id(2) { $description("Primary blue"); };
            }
        }

        // machines {}, services {} etc. would follow
    "#;

    let ast: SsotAst = parse_ssot_content(ssot_content, None)?;

    println!("Successfully parsed SSOT file!");
    println!("File ID: {:?}", ast.file_id);
    println!("Imports: {:?}", ast.imports);

    for definition in ast.definitions {
        match definition {
            fsm_dsl::ast::TopLevelDefinition::Types(types_block) => {
                println!("Found types block with {} definitions.", types_block.definitions.len());
                // Process types...
            }
            // Handle other block types...
            _ => {}
        }
    }

    Ok(())
}
```

## 貢献

バグ報告、機能リクエスト、プルリクエストを歓迎します。 