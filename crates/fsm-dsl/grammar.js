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
      $.annotation, // Allow top-level annotations
      $.types_block,
      $.machines_block,
      $.services_block,
      $.communication_block,
      $.actors_block,
      $.deployment_config_block
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

    // --- Items within Blocks ---
    _type_definition: $ => choice(
      $.struct_definition,
      $.enum_definition,
      // $.annotation // Annotations handled before definitions in blocks
    ),
    _machine_definition_item: $ => choice(
      $.machine_definition,
      $.annotation
    ),
    _service_item: $ => choice(
      $.interface_definition,
      $.service_definition,
      $.annotation
    ),
    _communication_item: $ => choice(
      $.protocol_definition,
      $.channel_definition,
      $.event_definition,
      $.annotation
    ),
    _actor_definition_item: $ => choice(
      $.actor_definition,
      $.annotation
    ),
    _deployment_item: $ => choice(
      $.environment_definition,
      $.infrastructure_definition,
      $.deployment_definition,
      $.annotation
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
      $.invokes_block,
      $.annotation // Allow annotations between elements
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
        field('id', $.numeric_id)
        // TODO: Optional parameters/body
      )
    ),

    guards_block: $ => seq(
      repeat($.annotation),
      'guards',
      field('id', $.numeric_id),
      '{',
      // TODO: guard_definition
      repeat($.guard_definition),
      '}'
    ),

    guard_definition: $ => semi(
      seq(
        repeat($.annotation),
        'guard',
        field('name', $.identifier),
        field('id', $.numeric_id)
        // TODO: Optional expression/body
      )
    ),

    invokes_block: $ => seq(
      repeat($.annotation),
      'invokes',
      field('id', $.numeric_id),
      '{',
      // TODO: invoke_definition
      repeat($.invoke_definition),
      '}'
    ),

    invoke_definition: $ => seq(
      repeat($.annotation),
      'invoke',
      field('name', $.identifier),
      field('id', $.numeric_id),
      '{',
      // TODO: invoke properties (src, input, onDone, onError)
      repeat1($._invoke_property), // Must have at least src
      '}'
    ),

    _invoke_property: $ => choice(
      $.invoke_src,
      $.invoke_input,
      $.invoke_on_done,
      $.invoke_on_error,
      $.annotation
    ),

    invoke_src: $ => semi(
      seq('src', ':', $.invoke_source_value)
    ),

    // Service.Method | "literal" | MachineName
    invoke_source_value: $ => choice(
      seq($.identifier, '.', $.identifier),
      $.string_literal,
      $.identifier
    ),

    invoke_input: $ => semi(
      seq('input', ':', $.object_literal) // Reuse object literal from annotations
    ),

    invoke_on_done: $ => semi(
      seq(
        'onDone',
        field('id', $.numeric_id),
        $.transition_target_specifier // Reuse transition target rule
      )
    ),

    invoke_on_error: $ => semi(
      seq(
        'onError',
        field('id', $.numeric_id),
        $.transition_target_specifier // Reuse transition target rule
      )
    ),

    // --- Placeholder for States Block --- 
    states_block: $ => seq(
      repeat($.annotation),
      'states',
      field('id', $.numeric_id),
      '{',
      // TODO: state_definition
      '}'
    ),

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

    action_list: $ => seq(
      '[', sepBy(',', $.identifier), ']'
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

    string_literal: $ => seq(
      '"',
      repeat(choice(
        token.immediate(/[^"\\]+/), // Match characters that are not double quote or backslash
        $.escape_sequence
      )),
      '"'
    ),
    // Define escape sequence rule separately
    escape_sequence: $ => token.immediate(/\\(?:["\\/bfnrt]|u[0-9a-fA-F]{4})/),

    integer_literal: $ => /-?[0-9]+/,

    boolean_literal: $ => choice('true', 'false'),

    // DURATION_LITERAL from grammar: INTEGER_LITERAL ~ ("ms" | "s" | "m" | "h")
    duration_literal: $ => seq($.integer_literal, choice('ms', 's', 'm', 'h')),

    // --- Annotations ---
    annotation: $ => semi(
      seq(
        '$',
        field('name', $.identifier),
        field('arguments', optional($.annotation_args))
      )
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