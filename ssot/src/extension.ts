import * as path from 'path';
import { workspace, ExtensionContext, window } from 'vscode';

import {
    LanguageClient,
    LanguageClientOptions,
    ServerOptions,
    TransportKind
} from 'vscode-languageclient/node'; // Use /node for Node.js environment

let client: LanguageClient;

export function activate(context: ExtensionContext) {
    console.log('Activating ssot extension');

    // --- Server Configuration ---
    // The server is implemented in Rust and expected to be built at ../target/debug/ssot-linter
    // Adjust the path based on your actual workspace structure and build output.
    // Use context.extensionPath which is the absolute path to the extension directory
    const serverCommand = path.join(context.extensionPath, '..', 'target', 'debug', 'ssot-linter');
    console.log(`Server command path: ${serverCommand}`); // Log the path for debugging

    // Check if the server executable exists (optional but helpful for debugging)
    // You might need fs module for this: import * as fs from 'fs';
    // if (!fs.existsSync(serverCommand)) {
    //     window.showErrorMessage(`SSOT Language Server executable not found at: ${serverCommand}`);
    //     console.error(`SSOT Language Server executable not found at: ${serverCommand}`);
    //     return; // Don't try to activate if the server is missing
    // }

    // If the server needs args: const serverArgs = ['--stdio']; else serverArgs = [];
    const serverArgs: string[] = []; // No args needed if server uses stdio directly

    // Server options tell the client how to start the server
    const serverOptions: ServerOptions = {
        run: { command: serverCommand, transport: TransportKind.stdio, args: serverArgs },
        debug: { command: serverCommand, transport: TransportKind.stdio, args: serverArgs } // Can use different command/args for debugging
    };

    // --- Client Configuration ---
    // Options to control the language client
    const clientOptions: LanguageClientOptions = {
        // Register the server for ssot documents
        documentSelector: [{ scheme: 'file', language: 'ssot' }],
        synchronize: {
            // Notify the server about file changes to '.clientrc files contained in the workspace
            // Adjust if your server needs to watch specific files outside of opened documents
            // fileEvents: workspace.createFileSystemWatcher('**/.clientrc')
        }
    };

    // --- Create and Start the Client ---
    client = new LanguageClient(
        'ssotLanguageServer', // ID of the language server client
        'SSOT Language Server', // Name shown to the user
        serverOptions,
        clientOptions
    );

    // Start the client. This will also launch the server
    console.log('Starting ssot language client...');
    client.start()
        .then(() => {
            console.log('SSOT Language Client started successfully.');
        })
        .catch((error) => {
            console.error('Failed to start SSOT Language Client:', error);
             window.showErrorMessage('Failed to start SSOT Language Server. See console (Help > Toggle Developer Tools) for details.');
        });


     // If you have commands to register, do it here
     // context.subscriptions.push(vscode.commands.registerCommand('ssot.myCommand', () => { ... }));
}

export function deactivate(): Thenable<void> | undefined {
    if (!client) {
        return undefined;
    }
    console.log('Deactivating ssot extension');
    return client.stop();
} 