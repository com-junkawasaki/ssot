package ssot_parser;

import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.ParseTree;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

import ssot_parser.ast.AstNode;
import ssot_parser.ast.nodes.*;
import ssot_parser.ast.type.*;

/**
 * Tests for parsing communication definitions (protocol, channel, event) and building the corresponding AST.
 */
public class CommunicationAstTest {

    // Helper method (Copied from StateMachineAstTest)
    private SsotRoot parseAndBuildAst(String inputString) throws Exception {
        InputStream is = new ByteArrayInputStream(inputString.getBytes(StandardCharsets.UTF_8));
        CharStream input = CharStreams.fromStream(is);
        SSoTLexer lexer = new SSoTLexer(input);
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        SSoTParser parser = new SSoTParser(tokens);

        parser.removeErrorListeners();
        parser.addErrorListener(new BaseErrorListener() {
            @Override
            public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol,
                                    int line, int charPositionInLine, String msg, RecognitionException e) {
                throw new RuntimeException("Syntax error at line " + line + ":" + charPositionInLine + " " + msg, e);
            }
        });

        ParseTree tree = parser.file();
        AstBuilderVisitor visitor = new AstBuilderVisitor();
        Object result = visitor.visit(tree);
        assertTrue(result instanceof SsotRoot, "AST building should return an SsotRoot");
        return (SsotRoot) result;
    }

    @Test
    void testCommunicationBlock() throws Exception {
        String input = """
        communication {
            @id(500)
            protocol HTTP { $version("1.1"); }

            @id(501)
            channel UserEvents { $protocol(HTTP); }

            @id(502)
            event UserLoggedIn {
                userId: string;
                timestamp: timestamp;
            }
             event OrderPlaced { orderId: u64; }
        }
        """;
        SsotRoot root = parseAndBuildAst(input);
        assertEquals(4, root.getCommunicationDefinitions().size());

        // Protocol
        ProtocolNode http = root.getCommunicationDefinitions().stream()
                               .filter(c -> c instanceof ProtocolNode && ((ProtocolNode)c).getName().equals("HTTP"))
                               .map(c -> (ProtocolNode)c).findFirst().orElse(null);
        assertNotNull(http);
        assertEquals(Optional.of(500L), http.getId());
        assertEquals(1, http.getAnnotations().size());
        assertEquals("version", http.getAnnotations().get(0).getName());
        assertEquals("1.1", http.getAnnotations().get(0).getValue());

        // Channel
        ChannelNode userEvents = root.getCommunicationDefinitions().stream()
                                   .filter(c -> c instanceof ChannelNode && ((ChannelNode)c).getName().equals("UserEvents"))
                                   .map(c -> (ChannelNode)c).findFirst().orElse(null);
        assertNotNull(userEvents);
        assertEquals(Optional.of(501L), userEvents.getId());
        assertEquals(1, userEvents.getAnnotations().size());
        assertEquals("protocol", userEvents.getAnnotations().get(0).getName());
        // Note: annotation value parsing currently treats referenceValue as String
        assertEquals("HTTP", userEvents.getAnnotations().get(0).getValue());

        // Event UserLoggedIn
        EventNode loggedIn = root.getCommunicationDefinitions().stream()
                                .filter(c -> c instanceof EventNode && ((EventNode)c).getName().equals("UserLoggedIn"))
                                .map(c -> (EventNode)c).findFirst().orElse(null);
        assertNotNull(loggedIn);
        assertEquals(Optional.of(502L), loggedIn.getId());
        assertTrue(loggedIn.getAnnotations().isEmpty());
        assertEquals(2, loggedIn.getFields().size());
        assertTrue(loggedIn.getFields().get(0).getType() instanceof PrimitiveTypeNode);
        assertEquals("string", ((PrimitiveTypeNode)loggedIn.getFields().get(0).getType()).getTypeName());
        assertEquals("userId", loggedIn.getFields().get(0).getName());
        assertTrue(loggedIn.getFields().get(1).getType() instanceof PrimitiveTypeNode);
        assertEquals("timestamp", ((PrimitiveTypeNode)loggedIn.getFields().get(1).getType()).getTypeName());
        assertEquals("timestamp", loggedIn.getFields().get(1).getName());

         // Event OrderPlaced
         EventNode orderPlaced = root.getCommunicationDefinitions().stream()
                                 .filter(c -> c instanceof EventNode && ((EventNode)c).getName().equals("OrderPlaced"))
                                 .map(c -> (EventNode)c).findFirst().orElse(null);
         assertNotNull(orderPlaced);
         assertTrue(orderPlaced.getId().isEmpty());
         assertTrue(orderPlaced.getAnnotations().isEmpty());
         assertEquals(1, orderPlaced.getFields().size());
         assertEquals("orderId", orderPlaced.getFields().get(0).getName());
         assertTrue(orderPlaced.getFields().get(0).getType() instanceof PrimitiveTypeNode);
         assertEquals("u64", ((PrimitiveTypeNode)orderPlaced.getFields().get(0).getType()).getTypeName());

    }
} 