"use strict";
var __createBinding = (this && this.__createBinding) || (Object.create ? (function(o, m, k, k2) {
    if (k2 === undefined) k2 = k;
    var desc = Object.getOwnPropertyDescriptor(m, k);
    if (!desc || ("get" in desc ? !m.__esModule : desc.writable || desc.configurable)) {
      desc = { enumerable: true, get: function() { return m[k]; } };
    }
    Object.defineProperty(o, k2, desc);
}) : (function(o, m, k, k2) {
    if (k2 === undefined) k2 = k;
    o[k2] = m[k];
}));
var __setModuleDefault = (this && this.__setModuleDefault) || (Object.create ? (function(o, v) {
    Object.defineProperty(o, "default", { enumerable: true, value: v });
}) : function(o, v) {
    o["default"] = v;
});
var __importStar = (this && this.__importStar) || (function () {
    var ownKeys = function(o) {
        ownKeys = Object.getOwnPropertyNames || function (o) {
            var ar = [];
            for (var k in o) if (Object.prototype.hasOwnProperty.call(o, k)) ar[ar.length] = k;
            return ar;
        };
        return ownKeys(o);
    };
    return function (mod) {
        if (mod && mod.__esModule) return mod;
        var result = {};
        if (mod != null) for (var k = ownKeys(mod), i = 0; i < k.length; i++) if (k[i] !== "default") __createBinding(result, mod, k[i]);
        __setModuleDefault(result, mod);
        return result;
    };
})();
Object.defineProperty(exports, "__esModule", { value: true });
exports.activate = activate;
exports.deactivate = deactivate;
const path = __importStar(require("path"));
const vscode_1 = require("vscode");
const node_1 = require("vscode-languageclient/node"); // Use /node for Node.js environment
let client;
function activate(context) {
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
    const serverArgs = []; // No args needed if server uses stdio directly
    // Server options tell the client how to start the server
    const serverOptions = {
        run: { command: serverCommand, transport: node_1.TransportKind.stdio, args: serverArgs },
        debug: { command: serverCommand, transport: node_1.TransportKind.stdio, args: serverArgs } // Can use different command/args for debugging
    };
    // --- Client Configuration ---
    // Options to control the language client
    const clientOptions = {
        // Register the server for ssot documents
        documentSelector: [{ scheme: 'file', language: 'ssot' }],
        synchronize: {
        // Notify the server about file changes to '.clientrc files contained in the workspace
        // Adjust if your server needs to watch specific files outside of opened documents
        // fileEvents: workspace.createFileSystemWatcher('**/.clientrc')
        }
    };
    // --- Create and Start the Client ---
    client = new node_1.LanguageClient('ssotLanguageServer', // ID of the language server client
    'SSOT Language Server', // Name shown to the user
    serverOptions, clientOptions);
    // Start the client. This will also launch the server
    console.log('Starting ssot language client...');
    client.start()
        .then(() => {
        console.log('SSOT Language Client started successfully.');
    })
        .catch((error) => {
        console.error('Failed to start SSOT Language Client:', error);
        vscode_1.window.showErrorMessage('Failed to start SSOT Language Server. See console (Help > Toggle Developer Tools) for details.');
    });
    // If you have commands to register, do it here
    // context.subscriptions.push(vscode.commands.registerCommand('ssot.myCommand', () => { ... }));
}
function deactivate() {
    if (!client) {
        return undefined;
    }
    console.log('Deactivating ssot extension');
    return client.stop();
}
//# sourceMappingURL=extension.js.map