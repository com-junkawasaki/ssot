2. Cap’n Proto（.capnp）ライクDSL
	•	豊富な型表現力
	•	structのネスト／unionで“基本・複合・並行・履歴ステート”を直感的にモデル化。
	•	default付きフィールド、複数のメタデータ構造体（ApiMeta／DbMeta／PolicyMetaなど）もIDL内で完結。
	•	ゼロコピー in-place 読み込み
	•	大規模定義でもパースコストほぼゼロ＆バイナリサイズ最小。
	•	Rustコード生成
	•	capnp compile -orust → capnpc-rust で、完全型安全なRustバインディングを自動生成。