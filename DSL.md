# State Machine DSL Syntax (Inspired by Cap'n Proto)

This document defines the syntax for the Single Source of Truth (`.ssot`) files used to define state machines, services, and related components in this project. The syntax borrows concepts from Cap'n Proto's schema language for clarity and structure, using `@` for IDs and `$` for metadata/tool annotations. The design emphasizes integrating domain-specific concerns into core constructs like `action`, `guard`, and `invoke` rather than proliferating specialized annotations. It supports hierarchical states, parallel states, history states, and timed events.

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

# --- State Machine Definition with Advanced Features ---
machine ComplexMachine @id(2) {
  $description("Demonstrates advanced state machine features.");
  $initial(Loading);

  context @id(0) {
    data: string @id(0);
    historyMarker: string @id(1);
    timerId: optional<u32> @id(2);
  }

  state Loading @id(0) {
    invoke loadData @id(0) {
      src: DataLoader.fetch;
      onDone @id(1) transition Dashboard { action storeData; }
      onError @id(2) transition ErrorState;
    }
  }

  # --- Composite State with History ---
  state Dashboard @id(1) {
    $description("Main dashboard area with multiple sections.");
    $initial(SectionA); # Initial sub-state

    # --- History State Definition ---
    # Remembers the last active sub-state (SectionA or SectionB)
    history shallow @id(0) target SectionA; # Default target if no history

    on GOTO_SETTINGS @id(0) transition Settings;
    on GOTO_DEEP_SETTINGS @id(1) transition DeepSettings;

    # --- Sub-States ---
    states {
      state SectionA @id(0) {
        on GOTO_B @id(0) transition SectionB;
      }
      state SectionB @id(1) {
        $initial(SubB1);
        # Deep history example
        history deep @id(0) target SubB1;

        on GOTO_A @id(0) transition SectionA;
        states {
           state SubB1 @id(0);
           state SubB2 @id(1);
           on GOTO_SUB_B2 @id(0) transition SubB2;
           on GOTO_SUB_B1 @id(1) transition SubB1;
        }
      }
    }
  }

  # --- State with Delayed Event / Timeout ---
  state Settings @id(2) {
    $description("User settings screen.");

    onEntry @id(0) { action startSettingsTimer; }
    onExit @id(1) { action cancelSettingsTimer; }

    # --- Delayed Transition --- After 5 seconds, transition back to Dashboard
    after 5s @id(0) transition Dashboard.history; # Transition to history state

    on SAVE_SETTINGS @id(1) transition Dashboard { action saveSettings; }
    on BACK @id(2) transition Dashboard.history; # Explicit transition to history
  }

  state DeepSettings @id(3) {
     on BACK @id(0) transition Dashboard.history(deep); # Target deep history of Dashboard.SectionB
  }

  # --- Parallel State Example ---
  state Processing @id(4) {
    $description("Handles background processing with status display.");
    $parallel: true; # Mark this state as parallel

    on CANCEL @id(0) transition Loading;

    # --- Parallel Regions --- These run concurrently
    states {
      # Region 1: Background Task Management
      state TaskRunner @id(0) {
        $initial(Running);
        states {
          state Running @id(0) {
            invoke backgroundTask @id(0) {
              src: Worker.run;
              onDone @id(0) transition ../Success; # Transition relative to parent
              onError @id(1) transition ../Failure;
            }
          }
          # Success/Failure are sibling states to TaskRunner within Processing
        }
      }

      # Region 2: Status Display
      state StatusDisplay @id(1) {
        $initial(ShowingProgress);
        states {
          state ShowingProgress @id(0) {
            onEntry @id(0) { action startStatusUpdates; }
            onExit @id(1) { action stopStatusUpdates; }
            on SHOW_DETAILS @id(0) transition ShowingDetails;
          }
          state ShowingDetails @id(1) {
            on HIDE_DETAILS @id(0) transition ShowingProgress;
          }
        }
      }
    } # End of parallel regions

    # These states are siblings to the parallel regions, acting as join points
    state Success @id(2) { $final; } # Processing finishes when TaskRunner reaches Success
    state Failure @id(3) { on RETRY @id(0) transition TaskRunner; } # Option to retry

  } # End of parallel state Processing

  state ErrorState @id(5);

  # --- Actions & Guards (Definitions) ---
  action storeData @id(100) (ctx: ContextType, event: DoneInvokeEventType);
  action saveSettings @id(101) (ctx: ContextType, event: EventType);
  action startSettingsTimer @id(102) (ctx: ContextType);
  action cancelSettingsTimer @id(103) (ctx: ContextType);
  action startStatusUpdates @id(104) (ctx: ContextType);
  action stopStatusUpdates @id(105) (ctx: ContextType);
  # ... other actions/guards ...
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

### 2.2. Annotations (`@` and `$ - Extended Further`)
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
- **New `$` Annotations:**
  - **`$parallel: true;`**: Applied to a composite `state`. Indicates that the regions defined within its `states` block should be treated as parallel (concurrent) state regions.

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

### 2.6. State Definition (Extended for Parallel & History)
- `state StateName @id(state_id) { ... }`
- **Parallel States:** If a state has the `$parallel: true;` annotation:
    - The `states { ... }` block defines the parallel regions.
    - Each direct child `state` within this block represents an independent, concurrently active region.
    - The parallel state is considered exited only when all its regions have reached a final state (a state marked `$final;` *within the scope of the parallel state*).
    - Transitions originating from *within* a parallel region can target states outside the parallel state or sibling states/regions using relative paths (e.g., `../TargetState`).
    - Events sent to the machine while in a parallel state are delivered to *all* active regions that can handle them.
- **Composite States (Hierarchical):** Can contain:
    - `onEntry`, `onExit`, `states { ... }` (as before).
    - **NEW: `history [shallow|deep] @id(history_id) target DefaultTargetState;`**: Defines a history pseudo-state.
        - `shallow` (default if omitted): Remembers only the direct child state of this composite state.
        - `deep`: Remembers the full path to the most nested active state(s) within this composite state.
        - `@id`: Unique ID for the history state definition.
        - `target DefaultTargetState`: The state to transition to if there is no history information to restore (e.g., first entry). Must be a sub-state of the current composite state.
        - Transitions can target this history state using dot notation: `transition ParentState.history` or `transition ParentState.history(deep)` to specify which history type to invoke if multiple are defined.
- Can have standard annotations: `$description`, `$final;`, `$version`, `$deprecated`, `$route`, `$meta`.

### 2.7. Transitions (`on ... transition` and `after ... transition`)
- **Event Transitions:** `on EVENT_NAME @id(event_id) transition TargetState { ... }` (Unchanged structure).
- **NEW: Delayed Transitions:** `after DURATION @id(delay_id) transition TargetState { action ..., guard ... }`
    - Defined within a `state` block.
    - `DURATION`: Specifies the delay before the transition is triggered. Format examples: `5s`, `100ms`, `2.5m` (seconds, milliseconds, minutes). The interpretation depends on the runtime environment.
    - The timer for the delay starts when the state is entered.
    - If the state is exited before the delay completes, the transition is cancelled.
    - Multiple `after` transitions can be defined within a state.
- **Targeting History:** Transitions can target a history state using `TargetState.history` or `TargetState.history(deep|shallow)`.

### 2.8. Actions and Guards
- `action actionName @id(action_id) (...);`
- `guard guardName @id(guard_id) (...) -> bool;`
- **Core Logic:** Actions encapsulate specific pieces of logic, including side effects like logging (`logInfo`), metric updates (`incrementMetric`), database operations, etc. Complex operations might be broken into multiple actions.
- **DB Operations:** Should ideally be encapsulated within specific actions or handled via `invoke` if interacting with a data access layer/service.
- **Permissions:** Guards are the primary mechanism for permission checks.
- Can have `$description`, `$version`, `$deprecated`, `$meta`.

### 2.9. Actors (`invoke`)
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

This further extended DSL now incorporates parallel states, shallow/deep history states, and delayed transitions (`after`), significantly increasing its compatibility with SCXML and XState patterns and enhancing its expressiveness for complex stateful logic. 