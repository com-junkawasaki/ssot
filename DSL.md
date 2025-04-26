# State Machine DSL Syntax (Inspired by Cap'n Proto)

This document defines the syntax for the Single Source of Truth (`.ssot`) files used to define state machines, services, and related components in this project. The syntax borrows concepts from Cap'n Proto's schema language for clarity and structure, using `@` for IDs and `$` for metadata/tool annotations.

## 1. Overall Structure

A `.ssot` file can define one or more state machines, services, interfaces, and types.

```ssot
# Unique ID for this definition file (Cap'n Proto compatible)
@0xabcdef1234567890;

# Import other definitions
import "/path/to/shared_types.ssot";
import "/path/to/base_service.ssot";

# --- Output Configuration ---
# Required: Specify output directories for generated artifacts
$rust_out("src/generated");
$capnp_out("schema/capnp"); # Optional
$ts_out("schema/ts");       # Optional
$mermaid_out("docs/diagrams"); # Optional: For visualization
$zod_out("schema/zod");      # Optional: For Zod schemas

# --- Service & Interface Definitions ---
# Define communication protocols (optional)
protocol CapnpRPC @id(0xaaa...) $description("Uses Cap'n Proto RPC.");
protocol GRPC @id(0xbbb...) $description("Uses gRPC.");

# Define service interfaces
interface UserAuthentication @id(0xccc...) {
  login @id(0) (credentials: UserCredentials) -> LoginResult;
  logout @id(1) (token: AuthToken);
  $version("1.0");
}

# Define services that implement interfaces
service AuthService @id(0) extends BaseService {
  $description("Handles user authentication.");
  $implements(UserAuthentication); # Specifies implemented interface
  $communicatesWith(UserProfileService using CapnpRPC); # Defines interaction
  $protocol(CapnpRPC); # Default protocol for this service
  $dbTable("auth_tokens"); # Link to primary DB table
  $logLevel("info");
}

service UserProfileService @id(1) {
  # ... definition ...
}

# --- State Machine Definition ---
machine UserSession @id(1) {
  # Machine-level annotations
  $description("Manages user login session state.");
  $version("2.1");
  $initial(LoggedOut); # Required: Specify the initial state
  $route("/session"); # Base route for session-related endpoints
  $uiComponent("SessionManager"); # Corresponding UI component hint

  # Define the context (state data)
  context @id(1) {
    userId: optional<string> @id(0); $description("Logged-in user ID, if any.");
    authToken: optional<AuthToken> @id(1);
    lastActivity: timestamp @id(2); $deprecated("Use sessionExpiry instead.");
    sessionExpiry: timestamp @id(3);
  }

  # --- Define States (including hierarchical) ---
  state LoggedOut @id(2) {
    $description("User is not logged in.");
    $route("/login");

    onEntry @id(0) {
      action clearSessionData; $description("Clear any residual session info.");
    }

    on LOGIN_REQUEST @id(1) transition Authenticating {
      action initiateLogin;
      guard isValidLoginRequest;
      $requiresPermission("public"); # Annotation example
    }
  }

  state Authenticating @id(3) {
    $description("Attempting to authenticate the user.");
    $uiComponent("LoginSpinner");

    # Invoke an external service (defined above)
    invoke authServiceLogin @id(0) {
      $apiCall(targetService: "AuthService", method: "login"); # Link to service interface method
      input: { credentials: event.credentials }; # Assuming event carries credentials
      onDone @id(1) transition LoggedIn {
        action storeAuthToken; $dbQuery("UPDATE users SET last_login = NOW() WHERE id = ?");
      }
      onError @id(2) transition LoginFailed {
        action handleAuthError; $metric("login_failure_count");
      }
    }
  }

  state LoggedIn @id(4) {
    $description("User is successfully logged in.");
    $initial(Active); # Initial sub-state for this composite state

    # Entry action for the composite state
    onEntry @id(0) {
       action startSessionTimer;
    }
    # Exit action for the composite state
    onExit @id(1) {
       action stopSessionTimer; $logLevel("debug");
    }

    # --- Define Sub-States (Hierarchical) ---
    states {
      state Active @id(0) {
        $description("User session is active.");
        $route("/"); # Route within the LoggedIn state

        on USER_ACTIVITY @id(0) transition self {
          action updateLastActivity;
        }
        on VIEW_PROFILE @id(1) transition ViewingProfile {
          $requiresPermission("user");
        }
        on LOGOUT_REQUEST @id(2) transition LoggedOut {
          action initiateLogout;
        }
        on SESSION_TIMEOUT @id(3) transition LoggedOut {
          action handleTimeout;
        }
      }

      state ViewingProfile @id(1) {
        $description("User is viewing their profile.");
        $route("/profile");
        $uiComponent("UserProfileDisplay");

        # Example: Invoke a different service
        invoke fetchProfile @id(0) {
           $apiCall(targetService: "UserProfileService", method: "getProfile");
           # ... onDone, onError ...
        }

        on BACK_TO_ACTIVE @id(1) transition Active;
      }
    } # End of sub-states for LoggedIn
  }

  state LoginFailed @id(5) {
    $description("Authentication failed.");
    $uiComponent("LoginError");
    on RETRY_LOGIN @id(0) transition LoggedOut;
  }

  # Define a final state (optional, can implicitly terminate)
  # state Terminated @id(6) $final;

  # Define shared actions and guards
  action clearSessionData @id(0) (ctx: ContextType);
  guard isValidLoginRequest @id(1) (ctx: ContextType, event: EventType) -> bool;
  action initiateLogin @id(2) (ctx: ContextType, event: EventType);
  action storeAuthToken @id(3) (ctx: ContextType, event: DoneInvokeEventType<LoginResult>);
  # ... other actions/guards

  # Test scenario linked to this machine (example)
  $testScenario("file://./tests/user_session.feature");
}

# --- Shared Type Definitions ---
struct UserCredentials @id(0xddd...) {
  username: string @id(0);
  password: string @id(1);
}

struct AuthToken @id(0xeee...) {
  token: string @id(0);
  expiresAt: timestamp @id(1);
}

struct LoginResult @id(0xfff...) {
  success: bool @id(0);
  token: optional<AuthToken> @id(1);
  errorMessage: optional<string> @id(2);
}

# Base service definition (imported or defined here)
service BaseService @id(0x111...) {
  # Common fields or methods
}

# Timestamp type (assuming built-in or imported)
# type timestamp = ...;

## 2. Syntax Elements

### 2.1. Comments
- Single-line comments start with `#`.

```ssot
# This is a comment
```

### 2.2. Annotations (`@` and `$` - Extended)
- **`@id(unique_id)`**: **Mandatory numerical ID**. Used for schema evolution (like Cap'n Proto field numbers). Must be unique within its scope (fields in a struct, states in a machine, etc.). Use `@0x...` format for the top-level file ID.
- **`$name("value")` or `$flag;`**: **Metadata and Tool Directives**. Uses the `$` prefix.
  - `$description("text")`: Optional human-readable description. Generates documentation comments.
  - `$initial(StateName)`: **Required** on the `machine` block. Specifies the entry state.
  - `$final;`: Marks a state as a terminal state. Can be applied to multiple states.
  - `$rust_out("path/to/dir")`: **Required** (top-level or per-machine). Specifies the output directory for generated Rust code.
  - `$capnp_out("path/to/dir")`: Optional. Specifies the output directory for generated Cap'n Proto schemas.
  - `$ts_out("path/to/dir")`: Optional. Specifies the output directory for generated TypeScript types.
  - `$version("version_string")`: Specifies the version of the element (machine, state, service, interface, etc.).
  - `$deprecated("reason_string")`: Marks an element (field, state, event, etc.) as deprecated, optionally providing a reason.
  - `$route("path_template")`: Associates a state or machine with a URL routing pattern.
  - `$uiComponent("ComponentName")`: Hints at the corresponding UI component name.
  - `$dbTable("table_name")`: Links an element (e.g., service, context field) to a database table.
  - `$dbQuery("SQL_or_Query_String")`: Associates an action or transition with a specific database query.
  - `$testScenario("path_or_identifier")`: Links an element (e.g., machine) to a test scenario file or identifier.
  - `$logLevel("level_string")`: Specifies a logging level (e.g., "debug", "info", "error") for actions or state transitions.
  - `$metric("metric_name")`: Associates an action or event with a monitoring metric name.
  - `$requiresPermission("permission_name")`: Specifies an access control requirement for a transition or action.
  - `$apiCall(targetService: "ServiceName", method: "methodName")`: Used within `invoke` blocks to specify details about calling an external service defined in the DSL.
  - **New `$` Annotations:**
    - **Structure & Relationships:**
      - `$implements(InterfaceName)`: Used on a `service` to declare which `interface` it implements.
      - `$communicatesWith(ServiceName using ProtocolName)`: Used on a `service` to declare interaction with another service and the protocol used.
      - `$protocol(ProtocolName)`: Specifies the default communication protocol for a `service` or `interface`.
  - **Generator Specific:**
    - `$mermaid_out("path/to/dir")`: Optional. Specifies output directory for Mermaid diagrams.
    - `$zod_out("path/to/dir")`: Optional. Specifies output directory for Zod schemas.
    - *(Others as needed per generator)*

### 2.3. Imports
- `import "/path/to/file.ssot";`
- Imports allow reusing definitions (types, actors, etc.) from other files. Paths are relative to the project root or a predefined import path.

### 2.4. Machine Definition
- `machine MachineName @id(machine_id) { ... }`
- Defines the main state machine container.
- **Must** contain exactly one `$initial(StateName)` annotation.
- Can contain `$description`, `$rust_out`, `$capnp_out`, `$ts_out` (if not specified at the top level).

### 2.5. Context Definition
- `context @id(context_id) { ... }`
- Defines the data structure (`context`) the machine holds.
- Fields are defined as `fieldName: type @id(field_id);`. Optional `$description` can be added.
- Supported primitive types: `u8`, `u16`, `u32`, `u64`, `i8`, `i16`, `i32`, `i64`, `f32`, `f64`, `bool`, `string` (or `text`), `list<Type>`, `map<KeyType, ValueType>`.
- Can reference types defined within the file or imported using `TypeName`.

### 2.6. State Definition (Extended)
- `state StateName @id(state_id) { ... }`
- **Can now contain:**
  - `onEntry @id(entry_id) { action actionName; ... }`: Defines actions to execute when entering the state. Can have `$description`.
  - `onExit @id(exit_id) { action actionName; ... }`: Defines actions to execute when exiting the state. Can have `$description`.
  - `states { ... }`: Defines nested sub-states, making this a composite state. A composite state must define an `$initial(SubStateName)` annotation.
- Can have annotations like `$description`, `$final;`, `$version`, `$deprecated`, `$route`, `$uiComponent`, `$logLevel`.

### 2.7. Transitions (`on ... transition` - Extended)
- `on EVENT_NAME @id(event_id) transition TargetState { ... }`
- Defined within a `state` block.
- `EVENT_NAME`: The event that triggers the transition (typically an enum variant or string constant).
- `TargetState`: The state to transition to. Can be `self` to transition back to the same state.
- Optional elements within the transition block:
    - `action actionName;`: Specifies an action to execute.
    - `guard guardName;`: Specifies a condition that must be true.
    - `$description("...")`: Describes the transition.
- Can now have annotations like `$requiresPermission`, `$logLevel`, `$metric`, `$dbQuery`.

### 2.8. Actions and Guards
- Can be defined globally within the `machine` block or potentially imported.
- `action actionName @id(action_id) (param1: type, ...);`
- `guard guardName @id(guard_id) (param1: type, ...) -> bool;`
- Can have `$description`, `$deprecated`, `$version`, `$dbQuery`, `$logLevel`, `$metric` annotations.

### 2.9. Actors (`invoke` - Extended)
- `invoke actorName @id(invoke_id) { ... }`
- **Now strongly recommended to include `$apiCall(...)` annotation when invoking external services defined in the DSL.**
  - `$apiCall(targetService: "ServiceName", method: "methodName")`: Provides explicit linkage.
- `input: { key: value, ... };`: Optional data to pass (can reference `context`).
- `onDone @id(done_id) transition TargetState { ... }`: Handles success.
- `onError @id(error_id) transition TargetState { ... }`: Handles errors.
- Can have `$description`, `$version`, `$deprecated` annotations.

### 2.10. Type Definitions (`struct`, `enum`)
- `struct StructName @id(struct_id) { field: type @id(field_id); ... }`
- `enum EnumName @id(enum_id) { VARIANT1 @id(0); VARIANT2 @id(1); ... }`
- Can have `$description`, `$version`, `$deprecated` annotations.

### **NEW: 2.11. Service Definition**
- `service ServiceName @id(service_id) [extends BaseServiceName] { ... }`
- Defines a system component or service.
- Can optionally `extends` another service definition.
- **Annotations:** `$description`, `$version`, `$deprecated`, `$implements(InterfaceName)`, `$communicatesWith(...)`, `$protocol(...)`, `$dbTable(...)`, `$logLevel(...)`.

### **NEW: 2.12. Interface Definition**
- `interface InterfaceName @id(interface_id) { ... }`
- Defines a contract for services. Contains method signatures.
- `methodName @id(method_id) (param1: Type, ...) -> ReturnType;`
- Can have annotations: `$description`, `$version`, `$deprecated`, `$protocol(...)`.

### **NEW: 2.13. Protocol Definition**
- `protocol ProtocolName @id(protocol_id) { ... }`
- Defines a communication protocol type (e.g., CapnpRPC, GRPC, REST).
- Mainly used for documentation and potentially influencing generator output.
- Can have annotations: `$description`.

## 3. IDs (`@id`)
- `@id` annotations (using `@`) are crucial for schema evolution, assigning stable numerical IDs to elements, similar to Cap'n Proto field numbers.
- File ID (`@0x...`) should be unique per file.
- Element IDs (`@integer`) should be unique within their immediate scope (e.g., fields within a struct, states within a machine). Start IDs from 0 for each scope.

This extended DSL now includes constructs for hierarchical states, entry/exit actions, richer metadata, service definitions, and various integration points, bringing it closer to the vision outlined in the `README.md` roadmap. 