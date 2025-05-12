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

import ssot_parser.AstBuilderVisitor;
import ssot_parser.ast.AstNode;
import ssot_parser.ast.nodes.*;
import ssot_parser.ast.type.*;
import ssot_parser.ast.SsotRoot;

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
            protocol HTTP { @id(500) $version("1.1") }

            channel UserEvents { @id(501) $protocol(HTTP) }

            event UserLoggedIn { @id(502)
                userId: string;
                timestamp: timestamp;
            }
            event OrderPlaced { @id(503) orderId: u64; $channel(UserEvents) }
        }
        """;
        SsotRoot root = parseAndBuildAst(input);
        assertEquals(4, root.getCommunicationDefinitions().size());

        // Protocol
        assertTrue(root.getCommunicationDefinitions().get(0) instanceof ProtocolNode);
        ProtocolNode proto = (ProtocolNode) root.getCommunicationDefinitions().get(0);
        assertEquals("HTTP", proto.getName());
        assertEquals(Optional.of(500L), proto.getId());
        assertEquals(1, proto.getAnnotations().size());
        assertTrue(proto.getAnnotations().containsKey("version"));
        assertEquals("1.1", proto.getAnnotations().get("version"));

        // Channel
        assertTrue(root.getCommunicationDefinitions().get(1) instanceof ChannelNode);
        ChannelNode chan = (ChannelNode) root.getCommunicationDefinitions().get(1);
        assertEquals("UserEvents", chan.getName());
        // Protocol is via annotation $protocol(HTTP)
        assertTrue(chan.getAnnotations().containsKey("protocol"));
        assertEquals("HTTP", chan.getAnnotations().get("protocol"));
        assertEquals(Optional.of(501L), chan.getId());
        assertEquals(1, chan.getAnnotations().size());

        // Event UserLoggedIn
        assertTrue(root.getCommunicationDefinitions().get(2) instanceof EventNode);
        EventNode loggedIn = (EventNode) root.getCommunicationDefinitions().get(2);
        assertEquals("UserLoggedIn", loggedIn.getName());
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
         assertTrue(root.getCommunicationDefinitions().get(3) instanceof EventNode);
         EventNode orderPlaced = (EventNode) root.getCommunicationDefinitions().get(3);
         assertEquals("OrderPlaced", orderPlaced.getName());
         // Channel for event is via annotation $channel(UserEvents)
         assertTrue(orderPlaced.getAnnotations().containsKey("channel"), "Event should have $channel annotation");
         assertEquals("UserEvents", orderPlaced.getAnnotations().get("channel"));
         assertEquals(Optional.of(503L), orderPlaced.getId());
         assertEquals(1, orderPlaced.getAnnotations().size(), "Event should have only $channel annotation");
         assertEquals(1, orderPlaced.getFields().size());
         assertEquals("orderId", orderPlaced.getFields().get(0).getName());
         assertTrue(orderPlaced.getFields().get(0).getType() instanceof PrimitiveTypeNode);
         assertEquals("u64", ((PrimitiveTypeNode)orderPlaced.getFields().get(0).getType()).getTypeName());
    }
} 