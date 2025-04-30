package ssot_parser;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/** Represents an action definition or reference */
public class ActionNode implements NodeWithId {
    private final Optional<Long> id; // ID if it's an action definition
    public final String name; // Name of the defined action or the action reference string
    // TODO: Add fields for parameters if representing a definition
    private final Map<String, Object> annotations;

    public ActionNode(Optional<Long> id, String name, Map<String, Object> annotations) {
        this.id = id;
        this.name = name;
        this.annotations = Collections.unmodifiableMap(annotations != null ? new HashMap<>(annotations) : Collections.emptyMap());
    }

    @Override
    public Optional<Long> getId() {
        return id;
    }

    public String getName() { return name; }

    @Override
    public Map<String, Object> getAnnotations() {
        return annotations;
    }
} 