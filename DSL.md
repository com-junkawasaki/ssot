# State Machine DSL Syntax (Inspired by Cap'n Proto)

This document defines the syntax for the Single Source of Truth (`.ssot`) files used to define state machines in this project. The syntax borrows concepts from Cap'n Proto's schema language for clarity and structure.

## 1. Overall Structure

A `.ssot` file defines one or more state machines. The core element is the `machine` block.

```ssot
# Unique ID for this definition file (similar to Cap'n Proto file ID)
@id(0xabcdef1234567890);

# Import other definitions (e.g., shared types, actors)
import "/path/to/shared_types.ssot";
import "/path/to/actors.ssot";

# Define the state machine
machine MyMachine @id(0) {
  # Machine-level annotations (optional)
  @description("A simple state machine example.");

  # Define the context (state data)
  context @id(1) {
    count: u32 @id(0);
    userId: string @id(1);
    # Reference imported types
    sharedData: ImportedDataType @id(2);
  }

  # Define states
  state Idle @id(2) @initial {
    @description("The machine is waiting for input.");

    # Define transitions triggered by events
    on EVENT_START @id(0) transition Active {
      @description("Start processing when EVENT_START occurs.");
      # Optional: Specify an action to execute
      action startProcessing;
      # Optional: Specify a guard condition
      guard canStart;
    }

    on EVENT_CHECK @id(1) transition self {
       action checkStatus;
       guard isReady;
    }
  }

  state Active @id(3) {
    # Invoke an actor (service, promise, etc.)
    invoke myActor @id(0) {
      # Optional: Specify source and parameters for the actor
      src: "actorDefinition"; # Reference an imported actor or define inline
      input: { currentCount: context.count };
      # Handle events from the actor
      onDone @id(0) transition Idle {
        action processCompletion;
      }
      onError @id(1) transition Error {
        action logError;
      }
    }

    on EVENT_PAUSE @id(1) transition Paused;
    on EVENT_STOP @id(2) transition FinalState;
  }

  state Paused @id(4) {
    on EVENT_RESUME @id(0) transition Active;
    on EVENT_STOP @id(1) transition FinalState;
  }

  state Error @id(5) {
    # Error state, potentially final or allowing recovery
    on EVENT_RETRY @id(0) transition Idle {
      action cleanup;
    }
  }

  # Define a final state
  state FinalState @id(6) @final;

  # Define shared actions and guards (optional)
  action startProcessing @id(0) (data: ContextType);
  guard canStart @id(1) (data: ContextType) -> bool;
  # ... other actions/guards
}

# Define shared types (can also be in imported files)
struct SharedDataType @id(0x123...) {
  field1: text @id(0);
  field2: bool @id(1);
}

# Define actors (can also be in imported files)
actor myActor @id(0x234...) {
  # Actor definition details (implementation specific)
  @description("An actor performing some async task.");
}


```

## 2. Syntax Elements

### 2.1. Comments
- Single-line comments start with `#`.

```ssot
# This is a comment
```

### 2.2. Annotations (`@`)
- Annotations provide metadata. They are placed before the element they describe.
- `@id(unique_id)`: Mandatory for top-level elements (machine, state, struct, actor, etc.) and fields/transitions within them. IDs should be unique within their scope (similar to Cap'n Proto IDs, but simplified here to integers for clarity). Top-level file ID uses a 64-bit hex literal.
- `@description("text")`: Optional description.
- `@initial`: Marks the starting state of a machine (only one per machine).
- `@final`: Marks a terminal state (can be multiple).

### 2.3. Imports
- `import "/path/to/file.ssot";`
- Imports allow reusing definitions (types, actors, etc.) from other files. Paths are relative to the project root or a predefined import path.

### 2.4. Machine Definition
- `machine MachineName @id(machine_id) { ... }`
- Defines the main state machine container.

### 2.5. Context Definition
- `context @id(context_id) { ... }`
- Defines the data structure (`context`) the machine holds.
- Fields are defined as `fieldName: type @id(field_id);`.
- Supported primitive types: `u8`, `u16`, `u32`, `u64`, `i8`, `i16`, `i32`, `i64`, `f32`, `f64`, `bool`, `string` (or `text`), `list<Type>`, `map<KeyType, ValueType>`.
- Can reference types defined within the file or imported using `TypeName`.

### 2.6. State Definition
- `state StateName @id(state_id) { ... }`
- Defines a state within the machine.
- Can have annotations like `@initial`, `@final`, `@description`.

### 2.7. Transitions (`on ... transition`)
- `on EVENT_NAME @id(event_id) transition TargetState { ... }`
- Defined within a `state` block.
- `EVENT_NAME`: The event that triggers the transition (typically an enum variant or string constant).
- `TargetState`: The state to transition to. Can be `self` to transition back to the same state (useful for actions without changing state).
- Optional elements within the transition block:
    - `action actionName;`: Specifies an action to execute during the transition.
    - `guard guardName;`: Specifies a condition (guard) that must be true for the transition to occur.

### 2.8. Actions and Guards
- Can be defined globally within the `machine` block or potentially imported.
- `action actionName @id(action_id) (param1: type, ...);` (Optional parameters)
- `guard guardName @id(guard_id) (param1: type, ...) -> bool;` (Must return boolean)
- When referenced in a transition (`action actionName;`), the implementation is expected to be provided elsewhere (e.g., in the code generator or runtime).

### 2.9. Actors (`invoke`)
- `invoke actorName @id(invoke_id) { ... }`
- Defined within a `state` block to represent invoking external services, promises, or other machines.
- `src: "identifier";`: Identifies the actor implementation (e.g., name of an imported actor definition).
- `input: { key: value, ... };`: Optional data to pass to the actor (can reference `context`).
- `onDone @id(done_id) transition TargetState { action actionName; }`: Handles successful completion.
- `onError @id(error_id) transition TargetState { action actionName; }`: Handles errors.
- Other actor-specific events can be handled similarly.

### 2.10. Type Definitions (`struct`, `enum`)
- Similar to Cap'n Proto, allows defining custom data structures or enumerations.
- `struct StructName @id(struct_id) { field: type @id(field_id); ... }`
- `enum EnumName @id(enum_id) { VARIANT1 @id(0); VARIANT2 @id(1); ... }`

## 3. IDs (`@id`)
- `@id` annotations are crucial for schema evolution, similar to Cap'n Proto. They ensure that renaming or reordering elements doesn't break compatibility if the IDs remain consistent.
- File ID (`@id(0x...)` at the top) should be unique per file.
- Element IDs (`@id(integer)`) should be unique within their immediate scope (e.g., fields within a struct, states within a machine, transitions within a state). Start IDs from 0 for each scope.

This DSL provides a structured way to define state machines, their context, states, transitions, and interactions, drawing inspiration from the clarity and structure of Cap'n Proto schemas. 