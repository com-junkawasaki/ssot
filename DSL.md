# State Machine DSL Syntax (Inspired by Cap'n Proto)

This document defines the syntax for the Single Source of Truth (`.ssot`) files used to define state machines, services, interfaces, protocols, types, and their associated configurations for code generation (including visualizations, data schemas, API specifications, and database schemas with RLS). The syntax borrows concepts from Cap'n Proto's schema language for clarity and structure, using `@id` for stable numerical IDs and `$` for metadata and tool directives (annotations).

## 1. Overall Structure

A `.ssot` file defines system components declaratively.

```ssot
# Unique ID for this definition file (Cap'n Proto compatible)
@0xabcdef1234567890;

# Import other definitions
import "/path/to/shared_types.ssot";
import "/path/to/base_service.ssot";
import "/path/to/common_actions.ssot";

# --- Output Configuration ---
# Specify directories for generated artifacts. Apply at top-level or per-element.
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

# --- Shared Type Definitions ---
struct UserCredentials @id(0xddd...) {
  username: string @id(0) {
    $description("User's login name.");
    $validate(minLength: 3, maxLength: 50, pattern: "^[a-zA-Z0-9_]+$");
  }
  password: string @id(1) {
    $description("User's password (validation often done server-side).");
    $validate(minLength: 8);
  }
}

struct AuthToken @id(0xeee...) {
  $db(table: "auth_tokens", primaryKey: "token"); # DB mapping example
  token: string @id(0) { $db(column: "auth_token", type: "VARCHAR(255)"); }
  userId: string @id(1) { $db(column: "user_id", type: "UUID", index: true); }
  expiresAt: timestamp @id(2) { $db(column: "expires_at"); }
}

# --- Communication Protocol Definition ---
protocol CapnpRPC @id(0xaaa...) {
  $description("Uses Cap'n Proto RPC framework.");
}

# --- API Channel Definition (for AsyncAPI) ---
channel UserNotifications @id(0xbbb...) {
  description: "Channel for user-specific real-time notifications.";
  parameters: { userId: string }; # e.g., maps to topic like user/{userId}/notifications
}

# --- Service Interface Definition ---
interface UserAuthentication @id(0xccc...) {
  $description("Defines user authentication operations.");
  $protocol(CapnpRPC); # Default protocol for this interface

  login @id(0) (credentials: UserCredentials) -> LoginResult {
    $description("Logs a user in.");
    $route(method: "POST", path: "/auth/login"); # OpenAPI route hint
    $meta(tags: ["Authentication"], operationId: "userLogin");
  }
  logout @id(1) (token: AuthToken);
  $version("1.1");
}

# --- Service Definition ---
service AuthService @id(0) extends BaseService {
  $description("Handles user authentication logic.");
  $implements(UserAuthentication);
  $communicatesWith(UserProfileService using CapnpRPC);
  $route(basePath: "/api/v1"); # Base path for routes defined in implemented interfaces
  $publishes(UserNotifications); # Declares async publishing capability
  $meta(responsibleTeam: "auth-team");
}

# --- State Machine Definition ---
machine ComplexMachine @id(2) {
  # ... (context, states, transitions, actions, guards, invoke as before) ...

  # Example state with associated UI component metadata
  state Dashboard @id(1) {
    $description("Main dashboard area.");
    $meta(uiComponent: "DashboardView");
    # ... (nested states, transitions, history, etc.) ...
  }
}

# --- Database Mapped Struct with RLS ---
struct UserProfile @id(0x100...) {
  $description("Stores user profile information.");
  $db(
    table: "user_profiles",
    primaryKey: "profileId",
    engine: "InnoDB", # Optional DB engine hint
    policies: [
      { name: "user_select_own", command: "SELECT", using: "userId = current_setting('request.jwt.claims.sub')::uuid", role: "authenticated" },
      { name: "user_update_own", command: "UPDATE", using: "userId = current_setting('request.jwt.claims.sub')::uuid", role: "authenticated" },
      { name: "admin_all_access", command: "ALL", using: "true", role: "service_role" }
    ]
  );

  profileId: u64 @id(0) { $db(column: "profile_id", type: "BIGINT UNSIGNED", autoIncrement: true); }
  userId: string @id(1) { $db(column: "user_id", type: "UUID", unique: true, nullable: false, foreignKey: { references: "auth.users.id" }); } # Example foreign key
  email: string @id(2) {
    $db(column: "email_address", type: "VARCHAR(255)", unique: "idx_email_unique");
    $validate(format: "email", maxLength: 255); # Data validation
  }
  bio: optional<string> @id(3) { $db(type: "TEXT", nullable: true); }
  createdAt: timestamp @id(4) { $db(default: "CURRENT_TIMESTAMP", index: true); }
}

# --- Event for AsyncAPI ---
event UserLoggedIn @id(0x200...) {
  $description("Published when a user successfully logs in.");
  $channel(UserNotifications); # Associates with the defined channel
  userId: string @id(0);
  timestamp: timestamp @id(1);
}

# ... (Other definitions: BaseService, LoginResult, etc.) ...
```

## 2. Syntax Elements

### 2.1. Comments
- Single-line comments start with `#`.

### 2.2. Annotations (`@id` and `$`)
- Provide metadata, configuration, and tool directives.
- **`@id(integer | 0xHex)`**: **Mandatory stable numerical ID** for schema evolution and referencing. Use integers starting from 0 within a scope (e.g., fields in a struct, states in a machine) or a unique hex ID (`@0x...`) for the top-level file ID.
- **`$name("value")` or `$flag;`**: General format for metadata annotations.

#### 2.2.1. Core Annotations
  - `$description("text")`: Optional human-readable description. Used for documentation generation across targets (Rust `///`, Capnp `#`, TS `/** */`, OpenAPI `description`, etc.). Applicable to most elements.
  - `$initial(StateName)`: **Required** on `machine` and composite `state` blocks defining hierarchical states. Specifies the entry state.
  - `$final;`: Marks a state as a terminal state within its region.
  - `$version("version_string")`: Optional version indicator for the element.
  - `$deprecated("reason_string" | true)`: Marks an element as deprecated.
  - `$meta(key: string, value: any)`: **Generic metadata annotation**. Attach arbitrary key-value pairs. Used for tool-specific hints (e.g., `$meta(uiComponent: "MyForm")`, `$meta(responsibleTeam: "auth")`, `$meta(dotRankdir: "LR")`). Replaces many previously proposed specialized annotations.

#### 2.2.2. Output Configuration Annotations
  - Define output directories for specific generators. Can be top-level or per-element (e.g., per `machine`, `struct`).
  - `$rust_out("path")`, `$capnp_out("path")`, `$ts_out("path")`
  - `$mermaid_out("path")`, `$dot_out("path")` (Visualization)
  - `$zod_out("path")`, `$jsonschema_out("path")`, `$proto_out("path")`, `$avro_out("path")` (Data/Schema)
  - `$openapi_out("path")`, `$grpc_out("path")`, `$graphql_out("path")`, `$asyncapi_out("path")` (API/Interface)
  - `$sql_out("path")`, `$prisma_out("path")`, `$drizzle_out("path")` (DB Schema)

#### 2.2.3. State Machine Specific Annotations
  - `$parallel: true;` (on `state`): Marks a composite state whose nested `states` represent concurrent regions.
  - See Section 2.6 for `history` syntax.

#### 2.2.4. Service & Communication Annotations
  - `$implements(InterfaceName)` (on `service`): Specifies implemented interface(s).
  - `$communicatesWith(ServiceName using ProtocolName)` (on `service`): Defines interaction dependencies.
  - `$protocol(ProtocolName)` (on `service` or `interface`): Specifies the default communication protocol.
  - **NEW: `$route(...)`** (on `interface method` or `service`): Provides hints for REST/HTTP API generation (OpenAPI).
    - On method: `$route(method: "GET" | "POST" | ..., path: "/path/{param}")`
    - On service/interface: `$route(basePath: "/api/v1")`
  - **NEW: `$channel(ChannelName)`** (on `event`): Associates an event with a defined `channel` for AsyncAPI generation.
  - **NEW: `$publishes(ChannelName)`** (on `machine` or `service`): Declares that the component publishes messages to the specified channel.
  - **NEW: `$subscribes(ChannelName)`** (on `machine` or `service`): Declares that the component subscribes to messages from the specified channel.

#### 2.2.5. Data & Validation Annotations
  - **NEW: `$validate(...)`** (on `struct field` or `event field`): Defines validation rules independent of the target schema language.
    - Format: `$validate(rule1: value1, rule2: value2, ...)`
    - Example Rules: `minLength: int`, `maxLength: int`, `min: number`, `max: number`, `pattern: "regex"`, `format: "email" | "uuid" | "url" | ...`, `minItems: int`, `maxItems: int`, `uniqueItems: true`, `required: true` (primarily for nested structs/enums within optionals).
    - Generators translate these to Zod, JSON Schema, etc.

#### 2.2.6. Database Schema Annotations
  - **NEW: `$db(...)`** (on `struct` or `struct field`): Provides structured mapping information for database schema generation (SQL, Prisma, Drizzle).
    - **On `struct`:**
      - `table: "table_name"`: Specifies the database table name.
      - `primaryKey: "column_name" | ["col1", "col2"]`: Defines the primary key column(s).
      - `engine: "InnoDB"`: Optional storage engine hint.
      - `policies: [...]`: **Defines Row-Level Security (RLS) policies** (see details below).
    - **On `struct field`:**
      - `column: "column_name"`: Specifies the database column name.
      - `type: "DB_SPECIFIC_TYPE"`: Overrides default type mapping (e.g., "VARCHAR(255)", "BIGINT UNSIGNED", "TEXT").
      - `nullable: boolean`: Explicitly set nullability (defaults based on `optional<T>`).
      - `unique: boolean | "index_name"`: Marks the column as unique (optionally naming the constraint).
      - `index: boolean | "index_name"`: Creates an index on the column.
      - `default: "DEFAULT_VALUE" | number`: Specifies a default value (use DB-specific syntax like "CURRENT_TIMESTAMP").
      - `autoIncrement: true`: Marks the column as auto-incrementing.
      - `foreignKey: { references: "other_table.other_column", onDelete: "CASCADE" | "SET NULL" | ... }`: Defines a foreign key constraint.
    - **RLS Policy Object Structure (within `$db(policies: [...])` on `struct`):**
      ```js
      {
        name: "policy_name",        // Required: SQL identifier for the policy
        command: "SELECT" | "INSERT" | "UPDATE" | "DELETE" | "ALL", // Required: Operation type
        using: "SQL expression string", // Required for SELECT, UPDATE, DELETE: Condition for existing rows
        check: "SQL expression string", // Optional: Condition for new/updated rows (WITH CHECK)
        role: "database_role_name"  // Optional: Apply policy to this specific role
      }
      ```
      *Note: The `using` and `check` expressions are database-specific SQL strings.*

### 2.3. Imports
- `import "/path/to/file.ssot";`
- Allows reusing definitions. Paths are relative to the project root or configured import paths.

### 2.4. Machine Definition
- `machine MachineName @id(...) { ... }`
- Defines a state machine. Contains `context`, `states`, `transitions`, `action`s, `guard`s, `invoke`s.
- Requires `$initial` annotation.

### 2.5. Context Definition
- `context @id(...) { field: type @id(...); ... }`
- Defines the state machine's internal data structure. Fields support primitive types (`u8`..`u64`, `i8`..`i64`, `f32`, `f64`, `bool`, `string`, `timestamp`), `list<T>`, `optional<T>`, `map<K, V>`, and references to other `struct`/`enum` types.

### 2.6. State Definition (Extended)
- `state StateName @id(...) { ... }`
- Can contain `onEntry`, `onExit`, `invoke`, event handlers (`on EVENT ...`), delayed transitions (`after DURATION ...`), and nested `states`.
- **Composite (Hierarchical):** A state containing a nested `states` block. Requires `$initial`.
- **Parallel:** A composite state with `$parallel: true;`. Its nested `states` define concurrently active regions.
- **History:** Defined within a composite state.
    - `history [shallow|deep] @id(...) target DefaultTargetState;`
    - Transitions can target history: `transition ParentState.history` or `transition ParentState.history(deep)`.

### 2.7. Transitions (`on`, `after`)
- `on EVENT_NAME @id(...) transition TargetState { action ..., guard ..., $requiresActor(...) }`
- `after DURATION @id(...) transition TargetState { action ..., guard ..., $requiresActor(...) }` (e.g., `after 5s`, `after 100ms`)
- `TargetState` can be a state name, relative path (`../Sibling`), or history (`State.history`).
- **NEW: `$requiresActor(ActorName | [ActorName1, ActorName2])`**: Optional annotation specifying which `actor` (or actors) are permitted to trigger this transition. Useful for access control modeling.

### 2.8. Actions and Guards
- `action actionName @id(...) (ctx: ContextType, event: EventType) [: ReturnType];`
- `guard guardName @id(...) (ctx: ContextType, event: EventType) -> bool;`
- Define reusable logic snippets. Can have `$description`, `$meta`.

### 2.9. Actors (`invoke`)
- `invoke invocationName @id(...) { src: Source, input?: ..., onDone?: ..., onError?: ... }`
- Used for calling external logic (services, promises, other machines, DB operations).
- `src` identifies the callable (e.g., `MyService.method`, `PromiseFactory`, `database.users.find`).

### 2.10. Type Definitions (`struct`, `enum`)
- `struct StructName @id(...) { field: type @id(...) { ...annotations... }; ... }`
- `enum EnumName @id(...) { VARIANT @id(...) { ...annotations... }; ... }`
- Basic data structures. Can have `$description`, `$validate`, `$db`, `$meta`.

### 2.11. Service Definition
- `service ServiceName @id(...) [extends BaseServiceName] { ... }`
- Defines a logical service component.
- Annotations: `$description`, `$implements`, `$communicatesWith`, `$protocol`, `$route`, `$publishes`, `$subscribes`, `$meta`.

### 2.12. Interface Definition
- `interface InterfaceName @id(...) { methodName @id(...) (params) -> ReturnType { ...annotations... }; ... }`
- Defines a contract for services.
- Annotations: `$description`, `$protocol`, `$route`, `$meta`.

### 2.13. Protocol Definition
- `protocol ProtocolName @id(...) { ... }`
- Defines a communication protocol type (e.g., CapnpRPC, REST). Primarily for documentation and hinting generators.
- Annotations: `$description`, `$meta`.

### **NEW: 2.14. Actor Definition**
- `actor ActorName @id(...) { ... }`
- Defines an entity (human role, external system, etc.) that interacts with the system, particularly state machines.
- Primarily used with the `$requiresActor` annotation on transitions to model access control.
- Annotations:
    - `$description("text")`: Human-readable description of the actor.
    - `$type("role" | "system" | "user_group" | ...)`: Optional categorization of the actor.
    - `$meta(...)`: Generic metadata.
- Example:
  ```ssot
  actor AdminUser @id(0xA001) {
    $description("Administrator role with full access.");
    $type("role");
  }
  actor PaymentGateway @id(0xB001) {
    $description("External payment processing system.");
    $type("system");
  }
  ```

### **NEW: 2.15. Channel Definition**
- `channel ChannelName @id(...) { description?: string; parameters?: { key: type }; ... }`
- Defines a logical message channel for asynchronous communication (used by AsyncAPI).
- `parameters` allow defining dynamic parts of a channel/topic name.

### **NEW: 2.16. Environment Definition**
- `environment EnvName @id(...) [extends BaseEnvName] { ... }`
- Defines a deployment environment (e.g., development, staging, production).
- Can inherit base configurations using `extends`.
- Annotations: `$description`, `$provider("aws" | "gcp" | ...)` `$region("...")`, `$meta`.
- Contains environment-specific `variables`, and references to `deployment` targets or `infrastructure` instances applicable to this environment.
- Example:
  ```ssot
  environment Production @id(0xe00...) {
    $provider("aws"); $region("us-east-1");
    variables: { logLevel: "info" };
    deployment: DeploymentTarget_ProdAppServers;
  }
  ```

### **NEW: 2.17. Infrastructure Definition**
- `infrastructure InfraName @id(...) [extends BaseInfraName] { ... }`
- Defines reusable infrastructure component blueprints (e.g., compute clusters, databases, networks, load balancers).
- Annotations: `$description`, `$meta`.
- Contains type-specific attributes (e.g., `instanceType`, `engine`, `version`, `cidrBlock`).
- Example:
  ```ssot
  infrastructure ComputeCluster @id(0xf00...) {
    type: "kubernetes"; instanceType: "t3.medium"; minSize: 2; maxSize: 10;
  }
  ```

### **NEW: 2.18. Deployment Definition**
- `deployment DeploymentName @id(...) { ... }`
- Links a deployable unit (`service`, `machine`) to a target `environment` and `infrastructure`.
- Specifies deployment parameters (replicas, strategy, configuration overrides).
- Annotations: `$description`, `$meta`.
- Example:
  ```ssot
  deployment DeployAuthServiceProd @id(0xd00...) {
    targetEnvironment: Production;
    targetInfrastructure: ComputeCluster_Prod; // Specific instance
    deployable: AuthService;
    replicas: 3; strategy: "blue_green";
  }
  ```
  *Alternatively, deployment hints can be placed directly on `service`/`machine` using a `$deployment(...)` annotation for simpler cases.*

## 3. IDs (`@id`)
- Crucial for schema evolution and linking definitions. Ensure uniqueness within the appropriate scope. File ID (`@0x...`) must be unique per file.

## 4. Key Concepts Added/Enhanced
- **Expanded Code Generation:** Explicit support for generating various artifacts beyond core Rust/Capnp/TS, **including Infrastructure as Code (IaC)**.
- **Data Validation:** `$validate` annotation for defining rules on data fields.
- **API Specification:** `$route`, `$channel`, `$publishes`, `$subscribes` annotations to support OpenAPI and AsyncAPI generation.
- **Database Schema Mapping:** Structured `$db` annotation for detailed table/column mapping and RLS policy definition.
- **Generic Metadata:** `$meta` annotation for extensibility and tool-specific configuration.
- **NEW: Actor Modeling:** Added `actor` element and `$requiresActor` annotation to model roles, permissions, and system interactions with state machines.
- **NEW: Infrastructure & Deployment:** Added `environment`, `infrastructure`, and `deployment` elements to model infrastructure configuration and deployment strategies, enabling IaC generation.

This significantly extended DSL aims to be a comprehensive Single Source of Truth for defining not just state logic but also related data structures, communication patterns, API contracts, database schemas, **actor interactions/permissions, and the underlying infrastructure and deployment configurations.**
