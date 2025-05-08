package ssot_parser.ast.nodes;

import ssot_parser.ast.AstNode;
import ssot_parser.NodeWithId;
import ssot_parser.ast.NodeVisitor;
import java.util.Map;
import java.util.Optional;
import java.util.Collections;
import java.util.Objects;
import java.util.HashMap;

/**
 * Represents an import statement.
 */
public class ImportNode implements AstNode, NodeWithId {
    private final String path;
    private final Optional<Long> id; // Although imports usually don't have @id
    private final Map<String, Object> annotations;

    public ImportNode(String path, Optional<Long> id, Map<String, Object> annotations) {
        this.path = Objects.requireNonNull(path, "Import path cannot be null");
        this.id = id != null ? id : Optional.empty();
        this.annotations = annotations != null ? Collections.unmodifiableMap(new HashMap<>(annotations)) : Collections.emptyMap();
    }

    public String getPath() {
        return path;
    }

    @Override
    public Optional<Long> getId() {
        return id;
    }

    @Override
    public Map<String, Object> getAnnotations() {
        return annotations;
    }

    @Override
    public <T> T accept(NodeVisitor<T> visitor) {
        return visitor.visitImportNode(this);
    }

    @Override
    public String toString() {
        return "ImportNode{" +
               "path='" + path + "'" +
               ", id=" + id.map(String::valueOf).orElse("none") +
               ", annotations=" + annotations +
               "}";
    }

    // equals and hashCode based on path?
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ImportNode that = (ImportNode) o;
        return Objects.equals(path, that.path);
    }

    @Override
    public int hashCode() {
        return Objects.hash(path);
    }
} 