# State Machine DSL Syntax (Inspired by Cap'n Proto)

This document defines the syntax for the Single Source of Truth (`.ssot`) files used to define state machines, services, and related components in this project. The syntax borrows concepts from Cap'n Proto's schema language for clarity and structure, using `@` for IDs and `$` for metadata/tool annotations. The design emphasizes integrating domain-specific concerns into core constructs like `action`, `guard`, and `invoke` rather than proliferating specialized annotations.

## 1. Overall Structure

A `.ssot` file can define one or more state machines, services, interfaces, protocols, and types.

```ssot
# Unique ID for this definition file (Cap'n Proto compatible)
@0xabcdef1234567890;

# Import other definitions
import "/path/to/shared_types.ssot";
import "/path/to/base_service.ssot";
import "/path/to/common_actions.ssot"; # e.g., for logInfo, incrementMetric

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
  $meta(dbTable: "auth_tokens"); # Link to primary DB table via generic meta
  $meta(responsibleTeam: "auth-team"); # Example custom metadata
}

service UserProfileService @id(1) {
  # ... definition ...
  getProfile @id(0) (userId: string) -> UserProfile;
}

# --- State Machine Definition ---
machine UserSession @id(1) {
  # Machine-level annotations
  $description("Manages user login session state.");
  $version("2.1");
  $initial(LoggedOut); # Required: Specify the initial state
  $route("/session"); # Optional: Base route associated with this machine
  $meta(uiComponent: "SessionManager"); # Hint for related UI component
  $meta(testScenario: "./tests/user_session.feature"); # Link to test file

  # Define the context (state data)
  context @id(1) {
    userId: optional<string> @id(0); $description("Logged-in user ID, if any.") $meta(dbColumn: "user_id");
    authToken: optional<AuthToken> @id(1) $meta(dbColumn: "auth_token_id");
    lastActivity: timestamp @id(2); $deprecated("Use sessionExpiry instead.");
    sessionExpiry: timestamp @id(3) $meta(dbColumn: "expires_at");
  }

  # --- Define States (including hierarchical) ---
  state LoggedOut @id(2) {
    $description("User is not logged in.");
    $route("/login"); # Route specific to this state
    $meta(uiComponent: "LoginForm");

    onEntry @id(0) {
      action clearSessionData; $description("Clear any residual session info.");
      action logInfo(message: "User logged out or session cleared"); # Use common action
    }

    on LOGIN_REQUEST @id(1) transition Authenticating {
      action initiateLogin;
      # Use a guard for permission checks
      guard isValidLoginRequest && userHasPermission("public");
    }
  }

  state Authenticating @id(3) {
    $description("Attempting to authenticate the user.");
    $meta(uiComponent: "LoginSpinner");

    # Invoke an external service using its interface
    invoke authServiceLogin @id(0) {
      src: AuthService.login; # Reference service method directly
      input: { credentials: event.credentials };
      onDone @id(1) transition LoggedIn {
        # Action can encapsulate DB logic, metrics, logging
        action handleSuccessfulLogin;
      }
      onError @id(2) transition LoginFailed {
        action handleAuthError;
        action incrementMetric(name: "login_failure_count"); # Use common action
      }
    }
  }

  state LoggedIn @id(4) {
    $description("User is successfully logged in.");
    $initial(Active); # Initial sub-state for this composite state

    onEntry @id(0) {
       action startSessionTimer;
       action logInfo(message: "User logged in, session active.");
    }
    onExit @id(1) {
       action stopSessionTimer;
       action logDebug(message: "Exiting logged in state."); # Different log level via different action
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
          guard userHasPermission("view_profile"); # Permission check via guard
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
        $meta(uiComponent: "UserProfileDisplay");

        # Invoke a different service
        invoke fetchProfile @id(0) {
           src: UserProfileService.getProfile;
           input: { userId: context.userId };
           onDone @id(1) transition self { action displayProfile; }
           onError @id(2) transition Active { action showProfileError; }
        }

        on BACK_TO_ACTIVE @id(1) transition Active;
      }
    } # End of sub-states for LoggedIn
  }

  state LoginFailed @id(5) {
    $description("Authentication failed.");
    $meta(uiComponent: "LoginError");
    on RETRY_LOGIN @id(0) transition LoggedOut;
  }

  # Define shared actions and guards (implementation provided elsewhere)
  action clearSessionData @id(0) (ctx: ContextType);
  guard isValidLoginRequest @id(1) (ctx: ContextType, event: EventType) -> bool;
  action initiateLogin @id(2) (ctx: ContextType, event: EventType);
  # This action now handles storing token, DB updates, logging, metrics etc.
  action handleSuccessfulLogin @id(3) (ctx: ContextType, event: DoneInvokeEventType<LoginResult>);
  action handleAuthError @id(4) (ctx: ContextType, event: ErrorInvokeEventType);
  guard userHasPermission @id(5) (permission: string) -> bool;
  action startSessionTimer @id(6) (ctx: ContextType);
  action stopSessionTimer @id(7) (ctx: ContextType);
  action updateLastActivity @id(8) (ctx: ContextType, event: EventType);
  action initiateLogout @id(9) (ctx: ContextType, event: EventType);
  action handleTimeout @id(10) (ctx: ContextType, event: EventType);
  action displayProfile @id(11) (ctx: ContextType, event: DoneInvokeEventType<UserProfile>);
  action showProfileError @id(12) (ctx: ContextType, event: ErrorInvokeEventType);

  # Common actions (potentially imported)
  action logInfo @id(100) (message: string);
  action logDebug @id(101) (message: string);
  action incrementMetric @id(102) (name: string);
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

struct UserProfile @id(0x100...) {
  # ... profile fields ...
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

### 2.2. Annotations (`@` and `$ - Refined`)
- Annotations provide metadata or tool directives.
- **`@id(unique_id)`**: **Mandatory numerical ID** for schema evolution (unchanged).
- **`$name("value")` or `$flag;`**: **Metadata and Tool Directives** (unchanged prefix).
  - `$description("text")`: Optional human-readable description (unchanged).
  - `$initial(StateName)`: **Required** on `machine` and composite `state` blocks (unchanged).
  - `$final;`: Marks a state as terminal (unchanged).
  - `$rust_out`, `$capnp_out`, `$ts_out`, `$mermaid_out`, `$zod_out`, etc.: Output directory specifications (unchanged concept).
  - `$version("version_string")`: Specifies element version (unchanged).
  - `$deprecated("reason_string")`: Marks element as deprecated (unchanged).
  - `$route("path_template")`: Optional URL routing pattern association (unchanged).
  - `$implements(InterfaceName)`: Used on `service` (unchanged).
  - `$communicatesWith(ServiceName using ProtocolName)`: Used on `service` (unchanged).
  - `$protocol(ProtocolName)`: Used on `service` or `interface` (unchanged).
  - **NEW: `$meta(key: string, value: string)`**: **Generic metadata annotation**. Used to attach arbitrary key-value pairs to elements (`machine`, `state`, `context` field, `service`, etc.). Replaces specialized annotations like `$uiComponent`, `$dbTable`, `$testScenario`. Keys and values are interpreted by code generators or other tools.
    *   Example: `$meta(uiComponent: "MyForm")`, `$meta(dbColumn: "user_email")`, `$meta(testId: "scenario-5")`
  - **REMOVED Annotations:** `$uiComponent`, `$dbTable`, `$dbQuery`, `$testScenario`, `$logLevel`, `$metric`, `$requiresPermission`, `$apiCall`. These concerns are now handled by `$meta`, `action`, `guard`, or `invoke`.

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

### 2.6. State Definition (Refined)
- `state StateName @id(state_id) { ... }`
- Contains `onEntry`, `onExit`, `states` (for hierarchy) as before.
- Can have `$description`, `$final;`, `$version`, `$deprecated`, `$route`, `$meta`.
- **Logging/Metrics:** Performed by invoking specific `action`s within `onEntry`, `onExit`, or transition actions.

### 2.7. Transitions (`on ... transition` - Refined)
- `on EVENT_NAME @id(event_id) transition TargetState { action ..., guard ... }`
- **Permission Checks:** Handled by `guard` conditions (e.g., `guard userHasPermission("admin")`).
- Actions within the transition block handle the core logic, potentially including logging or metric updates by calling other defined actions.
- Can have `$description`, `$version`, `$deprecated`, `$meta`.

### 2.8. Actions and Guards (Refined)
- `action actionName @id(action_id) (...);`
- `guard guardName @id(guard_id) (...) -> bool;`
- **Core Logic:** Actions encapsulate specific pieces of logic, including side effects like logging (`logInfo`), metric updates (`incrementMetric`), database operations, etc. Complex operations might be broken into multiple actions.
- **DB Operations:** Should ideally be encapsulated within specific actions or handled via `invoke` if interacting with a data access layer/service.
- **Permissions:** Guards are the primary mechanism for permission checks.
- Can have `$description`, `$version`, `$deprecated`, `$meta`.

### 2.9. Actors (`invoke` - Refined)
- `invoke invocationName @id(invoke_id) { ... }`
- **Purpose:** Used for calling external services, other state machines, promises, **and interacting with data layers (DB operations)**.
- **`src`:** Identifies the target callable. This could be a service method (`ServiceName.methodName`), a reference to another actor definition, a promise factory, or a special identifier for a data source (e.g., `database.users.update`). The exact format depends on the generator/runtime.
- `input`, `onDone`, `onError` remain the same.
- `$apiCall` annotation is removed; the `src` field serves this purpose.
- Can have `$description`, `$version`, `$deprecated`, `$meta`.

### 2.10. Type Definitions (`struct`, `enum`)
- `struct StructName @id(struct_id) { field: type @id(field_id); ... }`
- `enum EnumName @id(enum_id) { VARIANT1 @id(0); VARIANT2 @id(1); ... }`
- Can have `$description`, `$version`, `$deprecated`, `$meta` annotations.

### **NEW: 2.11. Service Definition**
- `service ServiceName @id(service_id) [extends BaseServiceName] { ... }`
- Defines a system component or service.
- Can optionally `extends` another service definition.
- **Annotations:** `$description`, `$version`, `$deprecated`, `$implements(InterfaceName)`, `$communicatesWith(...)`, `$protocol(...)`, `$meta(...)`.

### **NEW: 2.12. Interface Definition**
- `interface InterfaceName @id(interface_id) { ... }`
- Defines a contract for services. Contains method signatures.
- `methodName @id(method_id) (param1: Type, ...) -> ReturnType;`
- Can have annotations: `$description`, `$version`, `$deprecated`, `$protocol(...)`, `$meta(...)`.

### **NEW: 2.13. Protocol Definition**
- `protocol ProtocolName @id(protocol_id) { ... }`
- Defines a communication protocol type (e.g., CapnpRPC, GRPC, REST).
- Mainly used for documentation and potentially influencing generator output.
- Can have annotations: `$description`, `$meta(...)`.

## 3. IDs (`@id`)
- `@id` annotations (using `@`) are crucial for schema evolution, assigning stable numerical IDs to elements, similar to Cap'n Proto field numbers.
- File ID (`@0x...`) should be unique per file.
- Element IDs (`@integer`) should be unique within their immediate scope (e.g., fields within a struct, states within a machine). Start IDs from 0 for each scope.

This revised DSL promotes a cleaner separation of concerns by integrating cross-cutting aspects like UI hints, DB mapping, and test links into the generic `$meta` annotation, while leveraging core constructs like `action`, `guard`, and `invoke` for dynamic behaviors like logging, permissions, and external interactions. 