# SSOT Language Support

Provides basic language support for the `.ssot` file format used for defining Finite State Machines (FSMs).

The `.ssot` extension signifies the **Single Source of Truth** for the state machine specification. This extension aims to help developers working with `.ssot` files within Visual Studio Code.

The primary goal of the underlying `.ssot` project is to enable developers to define state machine logic declaratively in a `.ssot` file and automatically generate necessary Rust boilerplate code, ensuring type safety and reducing manual implementation effort. (For more details on the `.ssot` project itself, please refer to the main project repository: [https://github.com/jun784/ssot](https://github.com/jun784/ssot))

## Features

*   **Syntax Highlighting:** Provides syntax highlighting for `.ssot` files based on the TextMate grammar defined in `syntaxes/ssot.tmLanguage.json`.
*   **Basic Language Configuration:** Includes basic language features like bracket matching and comment definitions defined in `language-configuration.json`.

*(Note: This extension currently focuses on syntax highlighting. Future enhancements might include snippets, formatting, or deeper language integration.)*

## Requirements

There are no specific requirements or dependencies for installing this extension.

## Extension Settings

This extension does not currently contribute any specific VS Code settings.

## Known Issues

There are no known issues at this time. Please report any bugs or unexpected behavior on the [GitHub repository issues page](https://github.com/jun784/ssot/issues).

## Release Notes

### 0.0.1

*   Initial release.
*   Provides syntax highlighting for `.ssot` files.
*   Includes basic language configuration.

---

**Enjoy!**
