// Tree-sitter grammar definition for the .ssot language

// Helper function for sequences with optional trailing semicolon
const semi = (rule) => seq(rule, optional(';'));

module.exports = grammar({
  name: 'ssot',

  // Define word rule for identifier conflicts if needed
  // word: $ => $.identifier,

  // Define rules here
  rules: {
    // Start rule
    source_file: $ => seq(
      optional($.file_id),
      repeat($._top_level_item)
    ),

    _top_level_item: $ => choice(
      $.import_statement,
      $.types_block,
      $.machines_block,
      $.services_block,
      $.communication_block,
      $.actors_block,
      $.deployment_config_block,
      $.dependencies_block
    ),

    file_id: $ => semi(
      seq('@0x', /[a-fA-F0-9]+/)
    ),

    import_statement: $ => semi(
      seq('import', $.string_literal)
    ),

    // --- Top Level Blocks ---
    types_block: $ => seq(
      repeat($.annotation), // Annotations before block
      'types',
      '{',
      repeat($._type_definition),
      '}'
    ),
    machines_block: $ => seq(
      repeat($.annotation),
      'machines',
      '{',
      repeat($._machine_definition_item),
      '}'
    ),
    services_block: $ => seq(
      repeat($.annotation),
      'services',
      '{',
      repeat($._service_item),
      '}'
    ),
    communication_block: $ => seq(
      repeat($.annotation),
      'communication',
      '{',
      repeat($._communication_item),
      '}'
    ),
    actors_block: $ => seq(
      repeat($.annotation),
      'actors',
      '{',
      repeat($._actor_definition_item),
      '}'
    ),
    deployment_config_block: $ => seq(
      repeat($.annotation),
      'deployment_config',
      '{',
      repeat($._deployment_item),
      '}'
    ),

    // --- Dependencies Block (NEW) ---
    dependencies_block: $ => seq(
      repeat($.annotation), // Allow annotations on the block itself
      'dependencies',
      '{',
      repeat($._target_dependency_block),
      '}'
    ),

    _target_dependency_block: $ => choice(
      $.rust_dependency_block,
      $.nodejs_dependency_block
      // Add other target types like python, go etc. if needed
    ),

    rust_dependency_block: $ => seq(
      repeat($.annotation),
      'rust',
      field('target_path', $.string_literal),
      field('id', $.numeric_id),
      '{',
      repeat($.dependency_entry),
      '}'
    ),

    nodejs_dependency_block: $ => seq(
      repeat($.annotation),
      'nodejs',
      field('target_path', $.string_literal),
      field('id', $.numeric_id),
      '{',
      repeat($.dependency_entry),
      '}'
    ),

    dependency_entry: $ => seq(
      field('name', $.identifier),
      field('id', $.numeric_id),
      '{',
      repeat($.dependency_attribute),
      '}'
    ),

    dependency_attribute: $ => semi( // Attributes end with optional semicolon
      seq(
        field('key', $.identifier),
        ':',
        field('value', choice(
          $.string_literal,
          $.boolean_literal,
          $.list_literal // For features list
          // Add other potential value types if needed
        )),
        repeat($.annotation) // Allow annotations on attributes like $meta
      )
    ),

    // --- Items within Blocks ---
    _type_definition: $ => choice(
      $.struct_definition,
      $.enum_definition
    ),
    _machine_definition_item: $ => choice(
      $.machine_definition
    ),
    _service_item: $ => choice(
      $.interface_definition,
      $.service_definition
    ),
    _communication_item: $ => choice(
      $.protocol_definition,
      $.channel_definition,
      $.event_definition
    ),
    _actor_definition_item: $ => choice(
      $.actor_definition
    ),
    _deployment_item: $ => choice(
      $.environment_definition,
      $.infrastructure_definition,
      $.deployment_definition
    ),

    // --- Definitions (Placeholders - to be detailed) ---
    struct_definition: $ => seq(
      repeat($.annotation),
      'struct',
      field('name', $.identifier),
      field('id', $.numeric_id),
      '{',
       // TODO: Define field_definition
      repeat($.field_definition),
      '}'
    ),
    enum_definition: $ => seq(
      repeat($.annotation),
      'enum',
      field('name', $.identifier),
      field('id', $.numeric_id),
      '{',
      // TODO: Define enum_variant
      sepBy(';', $.enum_variant), // Variants separated by semicolon
      '}'
    ),

    field_definition: $ => semi(
      seq(
        field('name', $.identifier),
        ':',
        field('type', $.type_specifier),
        field('id', $.numeric_id),
        repeat($.annotation) // Annotations after ID
      )
    ),

    enum_variant: $ => seq(
      field('name', $.identifier),
      field('id', $.numeric_id),
      // Optional annotation block for variant
      field('annotations', optional(seq('{', repeat($.annotation), '}')))
      // Note: Semicolon is handled by sepBy in enum_definition
    ),

    // --- Type Specifier ---
    type_specifier: $ => choice(
      $.list_specifier,
      $.optional_specifier,
      $.map_specifier,
      $.simple_specifier // Must be last due to identifier precedence
    ),
    simple_specifier: $ => field('name', $.identifier),
    list_specifier: $ => seq(
      'list',
      '<',
      field('element_type', $.type_specifier),
      '>'
    ),
    optional_specifier: $ => seq(
      'optional',
      '<',
      field('wrapped_type', $.type_specifier),
      '>'
    ),
    map_specifier: $ => seq(
      'map',
      '<',
      field('key_type', $.type_specifier),
      ',',
      field('value_type', $.type_specifier),
      '>'
    ),

    // --- Definitions for other blocks (Placeholders) ---
    machine_definition: $ => seq(
      repeat($.annotation),
      'machine',
      field('name', $.identifier),
      field('id', $.numeric_id),
      '{',
      // TODO: Define machine body (context, states, etc.)
      optional($.machine_body),
      '}'
    ),

    machine_body: $ => repeat1($._machine_element), // Machine must contain at least one element?

    _machine_element: $ => choice(
      $.context_definition,
      $.states_block,
      $.actions_block,
      $.guards_block,
      $.invokes_block
    ),

    context_definition: $ => seq(
      repeat($.annotation),
      'context',
      field('id', $.numeric_id),
      '{',
      // TODO: context_field_definition
      repeat($.context_field_definition),
      '}'
    ),

    context_field_definition: $ => semi(
      seq(
        field('name', $.identifier),
        ':',
        field('type', $.type_specifier),
        field('id', $.numeric_id),
        repeat($.annotation),
        // TODO: Optional default value
        optional(seq('{', '$default', optional($.annotation_args), optional(';'), '}')) // Placeholder for default
      )
    ),

    actions_block: $ => seq(
      repeat($.annotation),
      'actions',
      field('id', $.numeric_id),
      '{',
      // TODO: action_definition
      repeat($.action_definition),
      '}'
    ),

    action_definition: $ => semi(
      seq(
        repeat($.annotation),
        'action',
        field('name', $.identifier),
        field('id', $.numeric_id),
        // Optional body for inline implementation or config
        optional(field('body', $.action_body))
      )
    ),

    // Placeholder for action body content (can be expanded later)
    action_body: $ => seq(
      '{',
      // Allow multiple statements within the action body
      repeat($._action_statement),
      '}'
    ),

    // Define possible statements within an action body
    _action_statement: $ => semi( // Each statement ends with an optional semicolon
        choice(
            $.assignment_statement,
            $.action_call_statement
            // Add other statement types here if needed (e.g., send, raise)
        )
    ),

    assignment_statement: $ => seq(
        field('assignee', $.identifier_path), // e.g., context.user.name
        '=',
        field('value', $.expression)
    ),

    // Represents calling a predefined action like a function
    action_call_statement: $ => $.function_call,

    guards_block: $ => seq(
      repeat($.annotation),
      'guards',
      field('id', $.numeric_id),
      '{',
      repeat($.guard_definition),
      '}'
    ),

    guard_definition: $ => semi(
      seq(
        repeat($.annotation),
        'guard',
        field('name', $.identifier),
        field('id', $.numeric_id),
        // Optional expression for the guard condition
        optional(field('condition', $.guard_condition))
      )
    ),

    guard_condition: $ => seq(
        '{',
        field('expression', $.expression),
        '}'
    ),

    invokes_block: $ => seq(
      repeat($.annotation),
      'invokes',
      field('id', $.numeric_id),
      '{',
      // TODO: Define invocation definition at machine level if needed
      repeat($.invocation_definition),
      '}'
    ),

    invocation_definition: $ => semi(
      seq(
        repeat($.annotation),
        'invoke',
        // Optional identifier for this specific invocation instance
        optional(field('instance_id', $.identifier)),
        field('id', $.numeric_id),
        '{',
        $.invoke_body,
        '}'
      )
    ),
    invoke_body: $ => repeat1($._invoke_element),

    _invoke_element: $ => choice(
        $.invoke_src,
        $.invoke_params, // Added params
        $.invoke_on_done,
        $.invoke_on_error
    ),

    // Refined invoke elements
    invoke_src: $ => semi(seq('src', ':', field('source', choice($.string_literal, $.identifier_path)))), // Can be URL or reference
    invoke_params: $ => semi(seq('params', ':', field('parameters', $.expression))), // Example: Pass data via an expression (e.g., a map literal or function call)
    invoke_on_done: $ => semi(seq('onDone', ':', field('target', $.invoke_target_definition))),
    invoke_on_error: $ => semi(seq('onError', ':', field('target', $.invoke_target_definition))),

    // Definition for what onDone/onError can target
    invoke_target_definition: $ => seq(
        '{',
        // Target state name should be an identifier
        optional(field('transition', $.identifier)),
        optional(field('actions', $.action_list)),      // Optional list of actions
        '}'
    ),

    action_list: $ => seq(
        'actions', ':', '[', optional(sepBy(',', $.identifier)), ']' // List of action identifiers
    ),

    // --- State Definitions ---
    states_block: $ => seq(
      repeat($.annotation),
      'states',
      field('id', $.numeric_id), // ID for the states block itself? Optional?
      '{',
      repeat($._state_definition_item),
      '}'
    ),

    _state_definition_item: $ => choice(
      $.state_definition
    ),

    state_definition: $ => seq(
      repeat($.annotation),
      optional(field('initial', kw('initial'))),
      optional(field('final', kw('final'))),
      kw('state'),
      field('name', $.identifier),
      field('id', $.numeric_id),
      optional(
        seq(
          '{',
           repeat($._state_body_element),
          '}'
        )
      )
    ),

    _state_body_element: $ => choice(
      $.transition_definition,
      $.state_invoke,
      $.history_definition,
      $.entry_action,
      $.exit_action,
      $.activity_definition,
      $.states_block // Allow nested states
    ),

    entry_action: $ => semi(seq(kw('ENTRY'), repeat1($.identifier))), // Simple identifier list for now
    exit_action: $ => semi(seq(kw('EXIT'), repeat1($.identifier))),  // Simple identifier list for now
    activity_definition: $ => semi(seq(kw('ACTIVITY'), repeat1($.identifier))), // Simple identifier list for now

    transition_definition: $ => semi(choice(
      // ON EVENT transition
      seq(
        repeat($.annotation), // Annotations before ON apply here
        kw('ON'),
        field('event', $.identifier),
        kw('GOTO'),
        field('target', $.identifier),
        optional(seq(kw('IF'), field('guard', $.identifier))),
        optional(seq(kw('DO'), field('action', repeat1($.identifier))))
      ),
      // AFTER DURATION transition
      seq(
        repeat($.annotation), // Annotations before AFTER apply here
        kw('AFTER'),
        field('delay', $.duration_literal),
        kw('GOTO'),
        field('target', $.identifier),
        optional(seq(kw('IF'), field('guard', $.identifier))),
        optional(seq(kw('DO'), field('action', repeat1($.identifier))))
      )
    )),

    state_invoke: $ => semi(seq(
      repeat($.annotation),
      kw('INVOKE'),
      field('source', $.identifier), // What is being invoked (machine, service, function?)
      field('id', $.numeric_id),
      // TODO: Define invocation details (src, data mapping, finalization)
      optional(seq('{', '/* ... invoke details ... */', '}')) // Placeholder
    )),

    history_definition: $ => semi(seq(
      repeat($.annotation),
      kw('HISTORY'),
      field('id', $.numeric_id),
      optional(field('type', choice(kw('shallow'), kw('deep'))))
    )),

    // --- Transition Target (needed by invoke_on_done/error) --- 
    transition_target_specifier: $ => seq(
      'target',
      field('target', $.target_identifier),
      field('details', optional($.transition_details))
    ),

    target_identifier: $ => choice(
      seq($.identifier, '.', 'history'), // Qualified history
      seq('.', 'history'), // Current history
      $.identifier // State name
    ),

    transition_details: $ => seq(
      '{',
      sepBy1(',', $._transition_detail_item), // Must have at least one item?
      '}'
    ),

    _transition_detail_item: $ => choice(
      $.transition_action,
      $.transition_guard,
      $.annotation // Allow annotations within details
    ),

    transition_action: $ => seq(
      'action', ':',
      choice($.identifier, $.action_list)
    ),

    transition_guard: $ => seq(
      'guard', ':',
      choice($.guard_specifier, $.guard_list)
    ),

    guard_list: $ => seq(
      '[', sepBy(',', $.guard_specifier), ']'
    ),

    // Allows guardName(not)
    guard_specifier: $ => seq(
      $.identifier,
      optional(seq('(', 'not', ')'))
    ),

    // --- Interface Definition (Placeholder) ---
    interface_definition: $ => seq(
      repeat($.annotation),
      'interface',
      field('name', $.identifier),
      field('id', $.numeric_id),
      '{',
      // TODO: Define method_definition
      '}'
    ),
    service_definition: $ => seq(
      repeat($.annotation),
      'service',
      field('name', $.identifier),
      field('id', $.numeric_id),
      // TODO: extends BaseService
      '{',
      // TODO: Service body annotations
      '}'
    ),
    protocol_definition: $ => semi(
      seq(
        repeat($.annotation),
        'protocol',
        field('name', $.identifier),
        field('id', $.numeric_id)
      )
    ),
    channel_definition: $ => seq(
      repeat($.annotation),
      'channel',
      field('name', $.identifier),
      field('id', $.numeric_id),
      '{',
      // TODO: channel properties (description, parameters)
      '}'
    ),
    event_definition: $ => seq(
      repeat($.annotation),
      'event',
      field('name', $.identifier),
      field('id', $.numeric_id),
      '{',
      // TODO: field_definition
      '}'
    ),
    actor_definition: $ => semi(
      seq(
        repeat($.annotation),
        'actor',
        field('name', $.identifier),
        field('id', $.numeric_id)
      )
    ),
    environment_definition: $ => seq(
      repeat($.annotation),
      'environment',
      field('name', $.identifier),
      field('id', $.numeric_id),
      // TODO: extends
      '{',
      // TODO: environment properties
      '}'
    ),
    infrastructure_definition: $ => seq(
      repeat($.annotation),
      'infrastructure',
      field('name', $.identifier),
      field('id', $.numeric_id),
      // TODO: extends
      '{',
      // TODO: infra properties (attribute_definition)
      '}'
    ),
    deployment_definition: $ => seq(
      repeat($.annotation),
      'deployment',
      field('name', $.identifier),
      field('id', $.numeric_id),
      '{',
      // TODO: deployment properties
      '}'
    ),

    // --- Basic Tokens & Literals ---
    numeric_id: $ => seq(
      '@id',
      '(',
      field('value', $.integer_literal),
      ')'
    ),

    identifier: $ => /[a-zA-Z_][a-zA-Z0-9_]*/,

    string_literal: $ => /\"([^\\\"]|\\.)*\"/,
    number_literal: $ => /\d+(\.\d+)?/,
    boolean_literal: $ => choice('true', 'false'),
    null_literal: $ => 'null',

    // Duration Literal (e.g., 100ms, 5s, 2m, 1h)
    integer_literal: $ => /-?\d+/,
    duration_literal: $ => seq(
      field('value', $.integer_literal),
      field('unit', choice('ms', 's', 'm', 'h'))
    ),

    _literal: $ => choice(
        $.string_literal,
        $.number_literal,
        $.boolean_literal,
        $.null_literal
    ),

    // --- Expressions (Expanded with operators) ---
    expression: $ => $._logical_or_expression,

    _primary_expression: $ => choice(
      $._literal,
      $.identifier_path,
      $.function_call,
      seq('(', $.expression, ')') // Parenthesized expression
    ),

    unary_expression: $ => prec.left(5, seq(
      field('operator', '!'),
      field('operand', $._primary_expression)
    )),

    // TODO: Add multiplicative/additive if needed (*, /, +, -)

    comparison_expression: $ => prec.left(4, seq(
      field('left', $._primary_expression), // Or higher precedence level if additive/multiplicative added
      field('operator', choice('==', '!=', '<', '>', '<=', '>=')),
      field('right', $._primary_expression)
    )),

    logical_and_expression: $ => prec.left(3, seq(
      field('left', choice($.comparison_expression, $.unary_expression, $._primary_expression)), // Operands can be results of higher precedence ops
      field('operator', '&&'),
      field('right', choice($.comparison_expression, $.unary_expression, $._primary_expression))
    )),

    logical_or_expression: $ => prec.left(2, seq(
      field('left', choice($.logical_and_expression, $.comparison_expression, $.unary_expression, $._primary_expression)),
      field('operator', '||'),
      field('right', choice($.logical_and_expression, $.comparison_expression, $.unary_expression, $._primary_expression))
    )),

    _logical_or_expression: $ => choice(
        $.logical_or_expression,
        $.logical_and_expression,
        $.comparison_expression,
        $.unary_expression,
        $._primary_expression
    ),

    identifier_path: $ => prec.left(1,
      seq(
        $.identifier,
        repeat1(
          seq('.', $.identifier)
        )
      )
    ),

    function_call: $ => seq(
        field('function_name', $.identifier),
        '(',
        // TODO: Define arguments more precisely if needed
        optional(sepBy(',', $.expression)),
        ')'
    ),

    // --- Annotations ---
    annotation: $ =>
      seq(
        '$',
        field('name', $.identifier),
        field('arguments', optional($.annotation_args))
      ),
    annotation_args: $ => seq(
      '(',
      sepBy(',', $.annotation_arg),
      ')'
    ),
    annotation_arg: $ => seq(
      optional(seq(field('key', $.identifier), ':')),
      field('value', $.annotation_value)
    ),
    annotation_value: $ => choice(
      $.literal_value,
      $.list_literal,
      $.object_literal
    ),
    list_literal: $ => seq(
      '[',
      sepBy(',', $.annotation_value),
      ']'
    ),
    object_literal: $ => seq(
      '{',
      sepBy(',', $.argument_pair),
      '}'
    ),
    argument_pair: $ => seq(
      field('key', $.identifier),
      ':',
      field('value', $.annotation_value)
    ),
    literal_value: $ => choice(
      $.string_literal,
      $.integer_literal,
      $.boolean_literal,
      $.duration_literal,
      // $.float_literal, // TODO: Add if needed
      // $.null_literal, // TODO: Add if needed
      $.identifier // Allow identifiers as values (e.g., enum refs)
    ),

    // --- Comments ---
    comment: $ => token(choice(
      seq('#', /.*/), // Single-line comment
      // seq('//', /.*/) // Alternate style
      // seq('/*', /[^*]*\*+([^/*][^*]*\*+)*/, '/') // Multi-line, needs careful regex
    ))
  },

  // Define things like comments, whitespace to be ignored globally
  extras: $ => [
    /\s/, // Whitespace
    $.comment
  ]
});

// Helper function for separated lists (e.g., items separated by commas)
function sepBy1(sep, rule) {
  return seq(rule, repeat(seq(sep, rule)), optional(sep));
}

function sepBy(sep, rule) {
  return optional(sepBy1(sep, rule));
}

// Helper function for keywords to potentially handle case-insensitivity later
function kw(keyword) {
  // return alias(prec(1, new RegExp(keyword, 'i')), keyword); // Case-insensitive example
  return alias(keyword, keyword); // Simple alias for now
}

// Helper rule used in action_body (example)
_expression_or_property: $ => choice(
    $.expression,
    seq($.identifier, ':', $.expression) // Simple key-value property
)

// --- Literals ---
// ... existing code ... 