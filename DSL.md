# State Machine DSL Syntax (Inspired by Cap'n Proto)

This document defines the syntax for the Single Source of Truth (`.ssot`) files used to define state machines, services, interfaces, protocols, types, and their associated configurations for code generation (including visualizations, data schemas, API specifications, and database schemas with RLS). The syntax borrows concepts from Cap'n Proto's schema language for clarity and structure, using `@id` for stable numerical IDs and `$` for metadata and tool directives (annotations).

**This DSL recommends organizing definitions within logical blocks (e.g., `types {}`, `services {}`, `machines {}`). While top-level definitions might be supported for backward compatibility by some tools, using blocks is the standard and preferred approach for clarity and organization.**

## 1. Overall Structure Example (Using Blocks)

```ssot
# Unique ID for this definition file (Cap'n Proto compatible)
@0xabcdef1234567890; # Must be globally unique across all .ssot files in the project.

# --- Imports ---
import "/path/to/shared_types.ssot"; # Import definitions from other files.
import "/path/to/base_service.ssot";
import "/path/to/common_actions.ssot";

# --- Output Configuration (Top Level or Per-Block/Element) ---
$rust_out("src/generated");         # Required for Rust code
$capnp_out("schema/capnp");       # Optional: Cap'n Proto schemas
$ts_out("schema/ts");             # Optional: TypeScript types
$mermaid_out("docs/diagrams");   # Optional: Mermaid diagrams
$dot_out("docs/graphs");          # Optional: Graphviz DOT graphs
$zod_out("schema/zod");          # Optional: Zod schemas (TypeScript)
$jsonschema_out("schema/json"); # Optional: JSON Schema files
$proto_out("schema/proto");       # Optional: Protocol Buffer definitions
$avro_out("schema/avro");         # Optional: Avro schemas
$openapi_out("schema/openapi");   # Optional: OpenAPI specifications
$grpc_out("schema/grpc");          # Optional: gRPC service definitions (might use $proto_out)
$graphql_out("schema/graphql");   # Optional: GraphQL schemas
$asyncapi_out("schema/asyncapi"); # Optional: AsyncAPI specifications
$sql_out("schema/sql");            # Optional: SQL DDL files
$prisma_out("schema/prisma");     # Optional: Prisma schema file content
$drizzle_out("schema/drizzle");   # Optional: Drizzle TS schema files

# ==========================================
#  Naming Conventions (Section Added)
# ==========================================
# - Types (struct, enum, machine, service, interface, protocol, actor, channel, environment, infrastructure, deployment): PascalCase
# - Fields, Actions, Guards, Methods, Variables, Parameters: camelCase
# - Enum Variants: UPPER_SNAKE_CASE or PascalCase (Consistency within the project is key)
# - File Names: snake_case.ssot or kebab-case.ssot

# ==========================================
#  Type Definitions Block
# ==========================================
types {
  $description("Contains shared data structures and enumerations.");
  $ts_out("src/generated/types"); # Example: Block-level output override

  struct UserCredentials @id(0) { # IDs within a block scope start from 0 and must be unique within that scope.
    username: string @id(0) {
      $description("User's login name.");
      $validate(minLength: 3, maxLength: 50, pattern: "^[a-zA-Z0-9_]+$");
    }
    password: string @id(1) {
      $description("User's password (validation often done server-side).");
      $validate(minLength: 8);
      $meta(sensitive: true); # Example generic metadata
    }
  }

  struct AuthToken @id(1) {
    $db(table: "auth_tokens", primaryKey: "token"); # DB mapping example
    token: string @id(0) { $db(column: "auth_token", type: "VARCHAR(255)"); } # See DB Mapping section for 'type' details.
    userId: string @id(1) { $db(column: "user_id", type: "UUID", index: true); }
    expiresAt: timestamp @id(2) { $db(column: "expires_at"); }
  }

  enum LoginStatus @id(2) {
    SUCCESS @id(0);
    INVALID_CREDENTIALS @id(1);
    ACCOUNT_LOCKED @id(2);
  }

  struct LoginResult @id(3) {
     status: LoginStatus @id(0);
     token: optional<AuthToken> @id(1);
  }

  struct UserProfile @id(4) {
    $description("Stores user profile information.");
    # Example RLS policies (SQL expressions are DB-specific, e.g., PostgreSQL)
    $db(
      table: "user_profiles",
      primaryKey: "profileId",
      policies: [
        { name: "user_select_own", command: "SELECT", using: "userId = current_setting('request.jwt.claims.sub')::uuid", role: "authenticated" },
        { name: "user_update_own", command: "UPDATE", using: "userId = current_setting('request.jwt.claims.sub')::uuid", withCheck: "userId = current_setting('request.jwt.claims.sub')::uuid", role: "authenticated" },
        { name: "admin_all_access", command: "ALL", using: "true", role: "service_role" }
      ]
    );

    profileId: u64 @id(0) { $db(column: "profile_id", type: "BIGINT", autoIncrement: true); } # Changed type for broader compatibility
    userId: string @id(1) { $db(column: "user_id", type: "UUID", unique: true, nullable: false, foreignKey: { references: "auth.users(id)", onDelete: "CASCADE" }); } # FK syntax slightly refined
    email: string @id(2) {
      $db(column: "email_address", type: "VARCHAR(255)", unique: "idx_email_unique");
      $validate(format: "email", maxLength: 255); # Validation example
    }
    bio: optional<string> @id(3) { $db(type: "TEXT", nullable: true); }
    createdAt: timestamp @id(4) { $db(default: "CURRENT_TIMESTAMP", index: true); }
  }
} # end types block

# ==========================================
#  Actor Definitions Block
# ==========================================
actors {
  $description("Defines actors (roles, systems) interacting with the system.");

  actor AdminUser @id(0) {
    $description("Administrator role.");
    $type("role");
  }

  actor PaymentGateway @id(1) {
    $description("External payment processing system.");
    $type("system");
  }
} # end actors block

# ==========================================
#  Communication Definitions Block
# ==========================================
communication {
  $description("Defines protocols, channels, and events for communication.");

  protocol CapnpRPC @id(0) {
    $description("Uses Cap'n Proto RPC framework.");
  }

  channel UserNotifications @id(1) {
    description: "Channel for user-specific real-time notifications.";
    # Example: Parameter used in topic name like 'users/{userId}/notifications'
    parameters: { userId: string };
  }

  event UserLoggedIn @id(2) {
    $description("Published when a user successfully logs in.");
    $channel(UserNotifications); # Refers to the channel defined above by name.
    userId: string @id(0);
    timestamp: timestamp @id(1);
  }
} # end communication block

# ==========================================
#  Service Definitions Block
# ==========================================
services {
  $description("Defines service interfaces and implementations.");

  interface UserAuthentication @id(0) {
    $description("Defines user authentication operations.");
    $protocol(CapnpRPC); # Default protocol for methods in this interface.

    # Method IDs are unique within the interface scope.
    login @id(0) (credentials: UserCredentials) -> LoginResult {
      $description("Logs a user in.");
      $route(method: "POST", path: "/auth/login"); # OpenAPI hint
      $meta(tags: ["Authentication"], operationId: "userLogin", complexity: 5); # Example adding complexity metadata
    }
    logout @id(1) (token: AuthToken);
    $version("1.1");
  }

  # Assume BaseService defined elsewhere or imported.
  # It's recommended to define BaseService in its own block or file.
  # service BaseService @id(...) { ... }

  service AuthService @id(1) extends BaseService {
    $description("Handles user authentication logic.");
    $implements(UserAuthentication); # References the interface by name.
    # References another service (must be defined). Assumes CapnpRPC from interface.
    $communicatesWith(UserProfileService using CapnpRPC);
    $route(basePath: "/api/v1"); # Base path for routes in implemented interfaces.
    $publishes(UserNotifications); # Declares async publishing capability.
    $meta(responsibleTeam: "auth-team");
  }

  # Assume UserProfileService defined elsewhere or imported.
  # service UserProfileService @id(...) { ... }

} # end services block

# ==========================================
#  State Machine Definitions Block
# ==========================================
machines {
  $description("Defines state machines governing application logic.");

  machine ComplexMachine @id(0) {
    $initial(Loading); # Initial state must be one of the direct child states.
    $description("An example complex state machine.");

    # Context defines the machine's extended state. IDs are unique within context.
    context @id(0) {
        currentUser: optional<UserProfile> @id(0);
        errorMessage: optional<string> @id(1);
        # Default values can be specified for primitive types
        retryCount: u8 @id(2) { $default(0); }
    }

    # Actions define side effects. IDs unique within the machine's actions scope.
    actions @id(1) {
        logError @id(0) (ctx, event); # Parameters are context and event object.
        incrementRetry @id(1) (ctx);
        assignUser @id(2) (ctx, event); # Assigns event data to context.
        clearError @id(3) (ctx);
    }

    # Guards define conditions for transitions. IDs unique within guards scope.
    guards @id(2) {
        hasCurrentUser @id(0) (ctx) -> bool; # Must return boolean.
        maxRetriesReached @id(1) (ctx) -> bool;
    }

    # Invocations define calls to external logic/actors. IDs unique within invokes scope.
    invokes @id(3) {
        # Invokes a method on a defined service. Result handled by onDone/onError.
        fetchUserData @id(0) {
          src: UserProfileService.fetchProfile; # References Service.method
          # input mapping can use context/event data
          # onDone/onError specify transitions or actions
        }
        # Invokes a promise-based function or another machine
        someBackgroundProcess @id(1) { src: "backgroundTaskName"; }
    }

    # States define the possible modes of the machine. IDs unique within the current states block scope.
    states @id(4) {
      Loading @id(0) {
        $description("Initial loading state.");
        # Invokes 'fetchUserData' upon entering this state.
        invoke Loading.fetchUser @id(0) { # Invocation IDs unique within the state.
            src: invokes.fetchUserData; # Reference defined invocation by name.
            input: { userId: "some_static_id" }; # Example input mapping
            # Transition to Dashboard on success, executing actions.
            onDone: transition Dashboard { action: [actions.assignUser, actions.clearError] };
            # Transition to Error state on failure, executing actions.
            onError: transition Error { action: actions.logError };
        }
      }

      Dashboard @id(1) {
        $description("Main dashboard area.");
        $meta(uiComponent: "DashboardView"); # Example UI hint
        $initial(Idle); # Required for composite states.

        # Nested states block. IDs unique within this block.
        states @id(0) {
            Idle @id(0) {
                # On REFRESH event, transition to Refreshing state.
                on REFRESH @id(0) transition Refreshing;
                # On LOGOUT event, transition if triggered by AdminUser.
                on LOGOUT @id(1) transition LoggingOut { $requiresActor(AdminUser); };
            }
            Refreshing @id(1) {
                # Define invocation specific to this state if needed, or reuse machine-level invokes.
                invoke Refreshing.fetchData @id(0) { src: invokes.fetchUserData; onDone: transition Idle; onError: transition Idle; };
            }
        }
        # History state definition (optional)
        history deep @id(1) target Idle; # Deep history, defaults to Idle if no history.
      }

      Error @id(2) {
        $description("Error state.");
        # Conditional transition based on guard.
        on RETRY @id(0) transition Loading {
            guard: [guards.maxRetriesReached(not)]; # Example of negating a guard.
            action: [actions.incrementRetry, actions.clearError]
        }
        # Alternative transition if the first guard fails. Order matters if guards overlap.
        on RETRY @id(1) transition Failure {
            guard: guards.maxRetriesReached;
        };
        # Delayed transition example: after 5 seconds, transition to Failure.
        # See DURATION Format section.
        after 5s @id(2) transition Failure { action: actions.logError };
      }

      LoggingOut @id(3) { /* ... Define behavior ... */ }
      Failure @id(4) { $final; } # Marks this as a terminal state for the machine.

    } # end states block for ComplexMachine

  } # end machine ComplexMachine

} # end machines block

# ==========================================
#  Deployment Configuration Block
# ==========================================
deployment_config {
  $description("Defines environments, infrastructure, and deployment strategies.");

  environment Production @id(0) {
    $provider("aws"); $region("us-east-1");
    # Variables specific to this environment
    variables: { logLevel: "info", apiEndpoint: "https://api.example.com" };
  }

  environment Staging @id(1) extends Production { # Environments can inherit
    $region("us-west-2");
    variables: { logLevel: "debug", apiEndpoint: "https://staging.api.example.com" };
  }

  infrastructure ComputeCluster @id(0) {
    $description("Blueprint for a K8s cluster.");
    type: "kubernetes"; instanceType: "t3.medium"; minSize: 2; maxSize: 10;
  }

  infrastructure Database @id(1) {
     $description("Blueprint for a Postgres DB.");
     type: "rds"; engine: "postgres"; version: "15"; storage: 100; # Example attributes
  }

  deployment DeployAuthServiceProd @id(0) {
    targetEnvironment: Production; # Reference environment by name
    targetInfrastructure: { # Can define specific infrastructure instances
        cluster: ComputeCluster @id(0); # Reference blueprint
        database: Database @id(1) { storage: 200 }; # Override blueprint attributes
    }
    deployable: AuthService; # Reference the service to deploy
    replicas: 3; strategy: "blue_green";
    # Environment variable overrides or secrets specific to this deployment
    config: { $db_connection_string: "$secret(PROD_DB_CONN_STRING)" }; # Example secret reference
  }

} # end deployment_config block

## 2. Syntax Elements

### 2.1. Comments
- Single-line comments start with `#`.

### 2.2. Annotations (`@id` and `$`)
- Provide metadata, configuration, and tool directives.
- **`@id(integer)`**: **Mandatory stable numerical ID**.
    - **Scope Rules (Clarified):**
        - **File ID (`@0x...`):** The very first element in the file *must* be a unique `@0xHEX_ID;`. This ID should be unique across all `.ssot` files within a project or organizational scope used for schema evolution tracking (e.g., with Cap'n Proto).
        - **Block-Level Definitions:** Within each top-level block (`types {}`, `machines {}`, `services {}`, etc.), direct child definitions (e.g., `struct`, `machine`, `service`) must have an `@id(integer)` starting from 0 and unique within that block.
        - **Element-Internal Definitions:** Within elements like `struct`, `enum`, `machine context`, `machine states`, `machine actions`, `machine guards`, `machine invokes`, `interface`, nested `states`, fields/variants/states/actions/guards/invokes/methods must have an `@id(integer)` starting from 0 and unique within that specific scope (e.g., unique among fields of a struct, unique among states directly within a `states` block).
        - **Consistency is key.** Tools rely on these stable IDs for code generation and schema evolution. Avoid renumbering IDs once they are assigned.
- **`$name("value")` or `$flag;`**: General format for metadata annotations.

#### 2.2.1. Core Annotations
  - `$description("text")`: Optional human-readable description. Highly recommended for clarity.
  - `$initial(StateName)`: **Required** on `machine` and composite `state` blocks. Specifies the entry state name.
  - `$final;`: Marks a state as a terminal state within its region.
  - `$version("version_string")`: Optional semantic version for the element.
  - `$deprecated("reason_string" | true)`: Marks an element as deprecated. Generators may issue warnings or omit the element.
  - `$meta(key: string, value: any)`: **Generic metadata annotation**. Attach arbitrary key-value pairs. Value can be string, number, boolean, or potentially a nested object/array depending on tool support.
    - Example: `$meta(uiHint: "Use <DatePicker/>", teamOwner: "checkout-team", complexityScore: 5)`

#### 2.2.2. Output Configuration Annotations
  - Define output directories. Can be top-level or per-block/element (overriding higher levels).
  - `$rust_out("path")`, `$capnp_out("path")`, `$ts_out("path")`, `$mermaid_out("path")`, `$dot_out("path")`, `$zod_out("path")`, `$jsonschema_out("path")`, `$proto_out("path")`, `$avro_out("path")`, `$openapi_out("path")`, `$grpc_out("path")`, `$graphql_out("path")`, `$asyncapi_out("path")`, `$sql_out("path")`, `$prisma_out("path")`, `$drizzle_out("path")`
  - Paths are typically relative to the project root or a configured base path.

#### 2.2.3. State Machine Specific Annotations
  - `$parallel: true;` (on `state`): Marks a composite state whose nested `states` are concurrent regions.
  - See Section 2.6 for `history` syntax details.

#### 2.2.4. Service & Communication Annotations
  - `$implements(InterfaceName)` (on `service`): Specifies implemented interface(s). Names must resolve to defined interfaces.
  - `$communicatesWith(ServiceName using ProtocolName)` (on `service`): Defines interaction dependencies. Names must resolve.
  - `$protocol(ProtocolName)` (on `service` or `interface`): Specifies the default communication protocol. Names must resolve.
  - `$route(...)` (on `interface method` or `service`/`interface`): Hints for REST/HTTP API generation (OpenAPI).
    - On method: `$route(method: "GET" | "POST" | ..., path: "/path/{param}")`. Path parameters `{param}` should correspond to method parameters.
    - On service/interface: `$route(basePath: "/api/v1")`. Prepended to method paths.
  - `$channel(ChannelName)` (on `event`): Associates an event with a defined `channel`. Names must resolve.
  - `$publishes(ChannelName | [ChannelName, ...])` (on `machine` or `service`): Declares publishing capability.
  - `$subscribes(ChannelName | [ChannelName, ...])` (on `machine` or `service`): Declares subscription capability.

#### 2.2.5. Data & Validation Annotations
  - `$validate(...)` (on `struct field` or `event field`): Defines validation rules. Generators translate these.
    - Format: `$validate(rule1: value1, rule2: value2, ...)`
    - Common Rules: `minLength: int`, `maxLength: int`, `min: number`, `max: number`, `pattern: "regex"`, `format: "email" | "uuid" | "url" | ...`, `minItems: int`, `maxItems: int`, `uniqueItems: true`.
    - Example: `email: string @id(0) { $validate(format: "email", maxLength: 254); }`
    - Example: `tags: list<string> @id(1) { $validate(minItems: 1, maxItems: 10, uniqueItems: true); }`

#### 2.2.6. Database Schema Annotations (`$db`) (Clarified)
  - Provides structured mapping information for database schema generation (SQL, Prisma, Drizzle, etc.).
  - **On `struct`:**
    - `table: "table_name"`: Database table name.
    - `primaryKey: "column_name" | ["col1", "col2"]`: Primary key column(s) (referencing field names, not column names).
    - `engine: "InnoDB"`: Optional storage engine hint (DB-specific).
    - `policies: [...]`: Defines Row-Level Security (RLS) policies.
      - Policy Object Structure:
        ```js
        {
          name: "policy_name", // Required: SQL identifier
          command: "SELECT" | "INSERT" | "UPDATE" | "DELETE" | "ALL", // Required
          using: "SQL expression string", // Required for SELECT, UPDATE, DELETE. DB-specific SQL.
          check: "SQL expression string", // Optional: WITH CHECK. DB-specific SQL.
          role: "database_role_name" // Optional: Target role.
        }
        ```
      - **Note:** SQL expressions within `using` and `check` are database-specific (e.g., PostgreSQL, MySQL syntax differs). The DSL defines the structure, not the SQL validity itself. Tools may offer basic validation or linting.
  - **On `struct field`:**
    - `column: "column_name"`: Database column name. If omitted, often derived from field name (e.g., snake_case).
    - `type: "DB_SPECIFIC_TYPE"`: Overrides default type mapping.
        - **Type Mapping (Conceptual - Generator Specific):**
          - `string` -> `VARCHAR(255)`, `TEXT` (depending on usage/length validation)
          - `u32`, `i32` -> `INTEGER`
          - `u64`, `i64` -> `BIGINT`
          - `f32` -> `REAL`, `FLOAT`
          - `f64` -> `DOUBLE PRECISION`
          - `bool` -> `BOOLEAN`
          - `timestamp` -> `TIMESTAMP`, `TIMESTAMPTZ`
          - `list<u8>` -> `BYTEA`, `BLOB`
          - `uuid` (if primitive added) -> `UUID`
        - Use `type` to specify exact types like `VARCHAR(100)`, `DECIMAL(10, 2)`, `JSONB`.
    - `nullable: boolean`: Explicitly set nullability. Defaults usually based on `optional<T>` (nullable if optional).
    - `unique: boolean | "index_name"`: Marks the column as unique.
    - `index: boolean | "index_name"`: Creates an index.
    - `default: "DEFAULT_VALUE" | number | boolean`: Specifies a default value using DB-specific syntax (e.g., `"CURRENT_TIMESTAMP"`, `0`, `true`).
    - `autoIncrement: true`: Marks as auto-incrementing (usually for integer primary keys).
    - `foreignKey: { references: "other_table(other_column)", onDelete?: "CASCADE" | "SET NULL" | "RESTRICT" | ..., onUpdate?: ... }`: Defines a foreign key. `references` uses DB table/column names.

### 2.3. Imports and Name Resolution (Clarified)
- `import "/path/to/file.ssot";`
- Allows reusing definitions from other files.
- **Name Resolution:**
    - Definitions within the current file can be referenced directly by their name (e.g., `UserCredentials`, `ComplexMachine`).
    - Definitions imported from other files *should* ideally be referenced using a qualified name, although simple name resolution might be attempted by tools. The recommended practice is to use `filename.DefinitionName` if ambiguity arises or for clarity, especially in larger projects. For example, if `shared_types.ssot` defines `Address`, it might be referenced as `shared_types.Address`. Tooling determines the exact resolution mechanism (e.g., based on file paths or explicit namespaces if introduced later).
    - Circular imports should be avoided or handled carefully by tooling.
- Paths are typically relative to the project root or configured import paths.

### 2.4. Machine Definition (`machine`)
- `machine MachineName @id(...) { ... }`
- Contains `context`, `states`, `transitions` (implicitly within `states` or explicitly), `action`s, `guard`s, `invoke`s.
- Requires `$initial` annotation pointing to a direct child state name.

### 2.5. Context Definition (`context`)
- `context @id(...) { field: type @id(...) { $default(...), ... }; ... }`
- Defines the machine's extended state variables.
- Supports primitive types (`u8..u64`, `i8..i64`, `f32`, `f64`, `bool`, `string`, `timestamp`), `list<T>`, `optional<T>`, `map<K, V>`, and references to defined `struct`/`enum` types.
- Fields can have `$default` annotations for initial values.

### 2.6. State Definition (`state`)
- `state StateName @id(...) { ... }`
- Defines a mode of the machine.
- Can contain:
    - `onEntry`/`onExit`: Actions executed upon entering/exiting.
    - `invoke`: Actors (services, promises, machines) invoked while in this state.
    - Event handlers: `on EVENT @id(...) transition Target { ... }`
    - Delayed transitions: `after DURATION @id(...) transition Target { ... }`
    - Nested `states` block (making it a composite state).
- **Composite (Hierarchical):** Contains a nested `states` block. Requires `$initial`.
- **Parallel:** Composite state with `$parallel: true;`. Nested `states` are concurrent regions.
- **History:** Defined within a composite state.
    - `history [shallow|deep] @id(...) target DefaultTargetState;`
    - `shallow`: Remembers the immediate child state.
    - `deep`: Remembers the leaf state in the active path.
    - `target`: State to enter if no history is recorded yet.
    - Transitions can target history: `transition .history` (targets history of the current composite state) or `transition ParentState.history`.

### 2.7. Transitions (`on`, `after`)
- Defined within a `state` block.
- `on EVENT_NAME @id(...) transition TargetState { action ..., guard ..., $requiresActor(...) }`
- `after DURATION @id(...) transition TargetState { action ..., guard ..., $requiresActor(...) }`
    - **DURATION Format (Clarified):** A number followed by a unit: `ms` (milliseconds), `s` (seconds), `m` (minutes), `h` (hours). Examples: `100ms`, `5s`, `2m`, `1h`. No spaces between number and unit.
- `TargetState`: Can be a state name (relative to current scope or absolute from machine root), `.siblingState`, `..parentSiblingState`, `.history`, `StateName.history`.
- `action`: Optional list of action names to execute.
- `guard`: Optional list of guard names (all must pass). Use `guardName(not)` for negation.
- `$requiresActor(ActorName | [ActorName1, ActorName2])`: Optional access control.

### 2.8. Actions and Guards (`action`, `guard`)
- Defined within a `machine` block (typically in dedicated `actions {}` / `guards {}` sub-blocks).
- `action actionName @id(...) (ctx: ContextType, event: EventType) [: ReturnType];` # ReturnType is optional.
- `guard guardName @id(...) (ctx: ContextType, event: EventType) -> bool;` # Must return boolean.
- Define reusable logic snippets. Can have `$description`, `$meta`.

### 2.9. Invocations (`invoke`)
- Defined within a `machine` block (typically in `invokes {}` sub-block) or directly within a `state`.
- `invoke invocationName @id(...) { src: Source, input?: {...}, onDone?: TransitionOrAction, onError?: TransitionOrAction }`
- `src`: Identifies the callable:
    - Service method: `ServiceName.methodName`
    - Promise/function identifier: `"promiseOrFunctionName"` (string literal, resolved by implementation)
    - Another machine: `MachineName` (spawns a child machine)
    - Database operation (conceptual): `"db.users.find"` (tooling specific)
- `input`: Maps context/event data to the invoked source's input.
- `onDone`/`onError`: Specify transitions (e.g., `transition TargetState { action ... }`) or actions to execute upon completion/failure.

### 2.10. Type Definitions (`struct`, `enum`)
- Defined within the `types {}` block.
- `struct StructName @id(...) { field: type @id(...) { ...annotations... }; ... }`
- `enum EnumName @id(...) { VARIANT @id(...) { ...annotations... }; ... }`
- Can have `$description`, `$validate`, `$db`, `$meta`.

### 2.11. Service Definition (`service`)
- Defined within the `services {}` block.
- `service ServiceName @id(...) [extends BaseServiceName] { ... }`
- Annotations: `$description`, `$implements`, `$communicatesWith`, `$protocol`, `$route`, `$publishes`, `$subscribes`, `$meta`.

### 2.12. Interface Definition (`interface`)
- Defined within the `services {}` block.
- `interface InterfaceName @id(...) { methodName @id(...) (param: Type, ...) -> ReturnType { ...annotations... }; ... }`
- Annotations: `$description`, `$protocol`, `$route`, `$meta`.

### 2.13. Protocol Definition (`protocol`)
- Defined within the `communication {}` block.
- `protocol ProtocolName @id(...) { $description(...), $meta(...) }`
- Defines a communication protocol type.

### 2.14. Actor Definition (`actor`)
- Defined within the `actors {}` block.
- `actor ActorName @id(...) { $description(...), $type("role" | "system" | ...), $meta(...) }`
- Defines an interacting entity for documentation and access control (`$requiresActor`).

### 2.15. Channel Definition (`channel`)
- Defined within the `communication {}` block.
- `channel ChannelName @id(...) { description?: string; parameters?: { key: type }; $meta(...) }`
- Defines a logical message channel for AsyncAPI.

### 2.16. Environment Definition (`environment`)
- Defined within the `deployment_config {}` block.
- `environment EnvName @id(...) [extends BaseEnvName] { variables?: {key: value}, $provider(...), $region(...), $meta(...) }`
- Defines a deployment environment and its variables.

### 2.17. Infrastructure Definition (`infrastructure`)
- Defined within the `deployment_config {}` block.
- `infrastructure InfraName @id(...) [extends BaseInfraName] { type: "...", /* type-specific attributes */, $description(...), $meta(...) }`
- Defines reusable infrastructure blueprints.

### 2.18. Deployment Definition (`deployment`)
- Defined within the `deployment_config {}` block.
- `deployment DeploymentName @id(...) { targetEnvironment: EnvName, targetInfrastructure: { name: InfraName, ... }, deployable: ServiceOrMachineName, replicas?: int, strategy?: "...", config?: {key: value}, $description(...), $meta(...) }`
- Links deployables to environments and infrastructure.

## 3. IDs (`@id`) (Summary)
- Use `@0x...` for the unique file ID.
- Use `@id(integer)` starting from 0 and unique within their immediate scope (block, struct fields, machine states, etc.).
- Do not reuse or reorder IDs once assigned to maintain stability.

## 4. Key Concepts Added/Enhanced
- **Block Structure:** Recommended organization using `types {}`, `services {}`, etc.
- **Naming Conventions:** Explicitly defined.
- **ID Scopes:** Clarified rules for `@id` uniqueness.
- **DB Mapping:** Details on `type` mapping and SQL expression caveats added. `$db` examples enhanced.
- **DURATION Format:** Specified (`5s`, `100ms`).
- **Name Resolution:** Basic rules and recommendations provided.
- **Validation:** `$validate` examples provided.
- **Metadata:** `$meta` examples shown for various elements.
- **Actor Modeling:** `actor` element and `$requiresActor` for access control.
- **Infrastructure & Deployment:** `environment`, `infrastructure`, `deployment` for IaC.

## 5. Potential Future Extensions (Considerations)
This DSL provides a strong foundation. Future versions or tooling could explore:
- **Standardized Error Handling:** Defining explicit `error` types and mechanisms for propagation within machines or across services.
- **Test Directives:** Annotations or blocks for defining test cases, scenarios, or mocking behavior directly within the SSOT file (e.g., `$test(...)`, `tests {}` block).
- **Advanced Generator Configuration:** More granular control over code generation via specialized annotations or configuration blocks (e.g., customizing serialization, specific framework integrations).
- **Explicit Namespaces:** A more robust mechanism for managing imports and avoiding name collisions in large projects beyond simple filename qualification.
- **Security Policy Definitions:** Expanding beyond RLS to include network policies, API authentication methods, etc.
- **Lifecycle Hooks:** Defining hooks for different stages of the DSL processing or code generation.

This revised DSL aims for greater clarity, consistency, and completeness based on the requested improvements.
