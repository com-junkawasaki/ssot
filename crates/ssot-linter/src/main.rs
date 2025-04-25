use lsp_server::{
    Connection, ExtractError, Message, Notification, /* Request, RequestId, */ Response,
};
use lsp_types::{
    notification::{DidChangeTextDocument, DidOpenTextDocument, Notification as _},
    Diagnostic, DiagnosticSeverity, /* request::Request as _, */
    InitializeParams, NumberOrString, Position, Range, ServerCapabilities,
    TextDocumentSyncCapability, TextDocumentSyncKind, Url,
};
use once_cell::sync::Lazy;
use pest::Parser;
use pest_derive::Parser;
use serde_json::Value;
use std::{collections::HashMap, error::Error, sync::Mutex}; // For global document cache

#[derive(Parser)]
#[grammar = "ssot.pest"]
struct SsotParser;

// Simple in-memory store for document contents
static DOCUMENT_CACHE: Lazy<Mutex<HashMap<Url, String>>> = Lazy::new(|| Mutex::new(HashMap::new()));

fn main() -> Result<(), Box<dyn Error + Sync + Send>> {
    eprintln!("Starting ssot-linter language server...");

    // Create the transport over stdio.
    let (connection, io_threads) = Connection::stdio();

    // Run the server and wait for the two threads to end (typically by trigger LSP Exit event).
    let server_capabilities = serde_json::to_value(ServerCapabilities {
        text_document_sync: Some(TextDocumentSyncCapability::Kind(
            TextDocumentSyncKind::FULL, // Send full document content on change
        )),
        // Add other capabilities like completionProvider, hoverProvider etc. later
        ..Default::default()
    })?;
    let initialization_params = connection.initialize(server_capabilities)?;
    main_loop(connection, initialization_params)?;

    eprintln!("Shutting down ssot-linter language server.");
    io_threads.join()?;
    Ok(())
}

fn main_loop(
    connection: Connection,
    params: Value, // Actually InitializeParams, but using Value for flexibility
) -> Result<(), Box<dyn Error + Sync + Send>> {
    let _params: InitializeParams = serde_json::from_value(params)?;
    eprintln!("Language server initialized.");

    for msg in &connection.receiver {
        eprintln!("Received message: {:?}", msg); // Log received messages for debugging
        match msg {
            Message::Request(req) => {
                if connection.handle_shutdown(&req)? {
                    return Ok(());
                }
                eprintln!("Received request: {:?}", req);
                // Handle other requests later (e.g., completion, hover)
                // For now, ignore unknown requests
                let resp = Response {
                    id: req.id,
                    result: None,
                    error: Some(lsp_server::ResponseError {
                        code: lsp_server::ErrorCode::MethodNotFound as i32,
                        message: format!("Request method not supported: {}", req.method),
                        data: None,
                    }),
                };
                connection.sender.send(Message::Response(resp))?;
            }
            Message::Response(resp) => {
                eprintln!("Received response: {:?}", resp);
            }
            Message::Notification(not) => {
                eprintln!("Received notification: {:?}", not);
                match not.method.as_str() {
                    DidOpenTextDocument::METHOD => {
                        let params: lsp_types::DidOpenTextDocumentParams =
                            cast_notification::<lsp_types::notification::DidOpenTextDocument>(not)?;
                        let uri = params.text_document.uri;
                        let content = params.text_document.text;
                        eprintln!("Opened document: {}", uri);
                        {
                            let mut cache = DOCUMENT_CACHE.lock().unwrap();
                            cache.insert(uri.clone(), content.clone());
                        }
                        validate_document(&connection.sender, uri, &content)?;
                    }
                    DidChangeTextDocument::METHOD => {
                        let params: lsp_types::DidChangeTextDocumentParams =
                            cast_notification::<lsp_types::notification::DidChangeTextDocument>(
                                not,
                            )?;
                        let uri = params.text_document.uri;
                        // Assuming TextDocumentSyncKind::FULL, the last content is the full new content
                        if let Some(change) = params.content_changes.last() {
                            let content = change.text.clone();
                            eprintln!("Changed document: {}", uri);
                            {
                                let mut cache = DOCUMENT_CACHE.lock().unwrap();
                                cache.insert(uri.clone(), content.clone());
                            }
                            validate_document(&connection.sender, uri, &content)?;
                        }
                    }
                    // Handle other notifications like DidSaveTextDocument, DidCloseTextDocument
                    _ => {}
                }
            }
        }
    }
    Ok(())
}

fn cast_notification<N>(not: Notification) -> Result<N::Params, ExtractError<Notification>>
where
    N: lsp_types::notification::Notification,
    N::Params: serde::de::DeserializeOwned,
{
    not.extract(N::METHOD)
}

fn validate_document(
    sender: &crossbeam_channel::Sender<Message>,
    uri: Url,
    content: &str,
) -> Result<(), Box<dyn Error + Sync + Send>> {
    eprintln!("Validating document: {}", uri);
    let diagnostics = match SsotParser::parse(Rule::ssot_file, content) {
        Ok(_) => {
            eprintln!("Validation OK for {}", uri);
            vec![] // No errors
        }
        Err(e) => {
            eprintln!("Validation error for {}: {}", uri, e);
            // Convert Pest error to LSP Diagnostic
            let mut diagnostics = vec![];
            let (start_line, start_col, end_line, end_col) = match e.line_col {
                pest::error::LineColLocation::Pos((line, col)) => {
                    (line - 1, col - 1, line - 1, col)
                } // Pest is 1-based, LSP is 0-based
                pest::error::LineColLocation::Span(
                    (start_line, start_col),
                    (end_line, end_col),
                ) => (start_line - 1, start_col - 1, end_line - 1, end_col - 1),
            };

            diagnostics.push(Diagnostic {
                range: Range {
                    start: Position {
                        line: start_line as u32,
                        character: start_col as u32,
                    },
                    end: Position {
                        line: end_line as u32,
                        character: end_col as u32,
                    },
                },
                severity: Some(DiagnosticSeverity::ERROR),
                code: Some(NumberOrString::String("syntax-error".to_string())), // Example error code
                source: Some("ssot-linter".to_string()),
                message: e.variant.message().to_string(), // Get the core error message
                ..Default::default()
            });
            diagnostics
        }
    };

    // Publish diagnostics
    let params = lsp_types::PublishDiagnosticsParams {
        uri,
        diagnostics,
        version: None, // Can be used to associate diagnostics with a specific document version
    };
    let notification = Notification::new(
        lsp_types::notification::PublishDiagnostics::METHOD.to_string(),
        params,
    );

    sender.send(Message::Notification(notification))?;
    eprintln!("Published diagnostics.");
    Ok(())
}
