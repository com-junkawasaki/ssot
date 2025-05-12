// Generated from /Users/junkawasaki/dev/ssot/crates/fsm-dsl/src/main/antlr4/SSoT.g4 by ANTLR 4.13.1

package ssot_parser;

import org.antlr.v4.runtime.atn.*;
import org.antlr.v4.runtime.dfa.DFA;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.misc.*;
import org.antlr.v4.runtime.tree.*;
import java.util.List;
import java.util.Iterator;
import java.util.ArrayList;

@SuppressWarnings({"all", "warnings", "unchecked", "unused", "cast", "CheckReturnValue"})
public class SSoTParser extends Parser {
	static { RuntimeMetaData.checkVersion("4.13.1", RuntimeMetaData.VERSION); }

	protected static final DFA[] _decisionToDFA;
	protected static final PredictionContextCache _sharedContextCache =
		new PredictionContextCache();
	public static final int
		ACTORS=1, ACTOR=2, TYPES=3, STRUCT=4, ENUM=5, COMMUNICATION=6, PROTOCOL=7, 
		CHANNEL=8, EVENT=9, SERVICES=10, INTERFACE=11, SERVICE=12, MACHINES=13, 
		MACHINE=14, INITIAL=15, STATE=16, STATES=17, HISTORY=18, SHALLOW=19, DEEP=20, 
		PARALLEL=21, FINAL=22, TARGET=23, ACTIONS=24, GUARDS=25, ON=26, INVOKE=27, 
		ONDONE=28, ONERROR=29, ON_ENTRY=30, ON_EXIT=31, LIST=32, MAP=33, OPTIONAL=34, 
		PRIMITIVE_TYPE=35, TIMESTAMP_TYPE=36, LBRACE=37, RBRACE=38, LPAREN=39, 
		RPAREN=40, LBRACK=41, RBRACK=42, SEMI=43, COMMA=44, COLON=45, ARROW=46, 
		SLASH=47, AT=48, DOLLAR=49, LT=50, GT=51, DOT=52, STRING=53, INT=54, FLOAT=55, 
		BOOLEAN=56, NULL=57, ID=58, WS=59, COMMENT=60;
	public static final int
		RULE_file = 0, RULE_definitionBlock = 1, RULE_actorsBlock = 2, RULE_actorDefinition = 3, 
		RULE_typesBlock = 4, RULE_typeDefinition = 5, RULE_structDefinition = 6, 
		RULE_structFieldDefinition = 7, RULE_enumDefinition = 8, RULE_enumVariantDefinition = 9, 
		RULE_communicationBlock = 10, RULE_communicationDefinition = 11, RULE_protocolDefinition = 12, 
		RULE_channelDefinition = 13, RULE_eventDefinition = 14, RULE_eventFieldDefinition = 15, 
		RULE_servicesBlock = 16, RULE_serviceElement = 17, RULE_interfaceDefinition = 18, 
		RULE_serviceDefinition = 19, RULE_methodDefinition = 20, RULE_machinesBlock = 21, 
		RULE_machineDefinition = 22, RULE_machineBodyElement = 23, RULE_actionsDefinition = 24, 
		RULE_actionDefinition = 25, RULE_guardsDefinition = 26, RULE_guardDefinition = 27, 
		RULE_statesDefinition = 28, RULE_initialStateDefinition = 29, RULE_stateDefinitionOrHistoryState = 30, 
		RULE_historyStateDefinition = 31, RULE_stateDefinition = 32, RULE_stateType = 33, 
		RULE_stateBody = 34, RULE_stateBodyElement = 35, RULE_entryExitAction = 36, 
		RULE_actionReference = 37, RULE_transitionDefinition = 38, RULE_transitionTarget = 39, 
		RULE_invokeDefinition = 40, RULE_invokeCallback = 41, RULE_annotation = 42, 
		RULE_annotationName = 43, RULE_annotationValue = 44, RULE_literal = 45, 
		RULE_paramList = 46, RULE_parameter = 47, RULE_typeReference = 48, RULE_simpleType = 49, 
		RULE_listType = 50, RULE_mapType = 51, RULE_optionalType = 52;
	private static String[] makeRuleNames() {
		return new String[] {
			"file", "definitionBlock", "actorsBlock", "actorDefinition", "typesBlock", 
			"typeDefinition", "structDefinition", "structFieldDefinition", "enumDefinition", 
			"enumVariantDefinition", "communicationBlock", "communicationDefinition", 
			"protocolDefinition", "channelDefinition", "eventDefinition", "eventFieldDefinition", 
			"servicesBlock", "serviceElement", "interfaceDefinition", "serviceDefinition", 
			"methodDefinition", "machinesBlock", "machineDefinition", "machineBodyElement", 
			"actionsDefinition", "actionDefinition", "guardsDefinition", "guardDefinition", 
			"statesDefinition", "initialStateDefinition", "stateDefinitionOrHistoryState", 
			"historyStateDefinition", "stateDefinition", "stateType", "stateBody", 
			"stateBodyElement", "entryExitAction", "actionReference", "transitionDefinition", 
			"transitionTarget", "invokeDefinition", "invokeCallback", "annotation", 
			"annotationName", "annotationValue", "literal", "paramList", "parameter", 
			"typeReference", "simpleType", "listType", "mapType", "optionalType"
		};
	}
	public static final String[] ruleNames = makeRuleNames();

	private static String[] makeLiteralNames() {
		return new String[] {
			null, "'actors'", "'actor'", "'types'", "'struct'", "'enum'", "'communication'", 
			"'protocol'", "'channel'", "'event'", "'services'", "'interface'", "'service'", 
			"'machines'", "'machine'", "'initial'", "'state'", "'states'", "'history'", 
			"'shallow'", "'deep'", "'parallel'", "'final'", "'target'", "'actions'", 
			"'guards'", "'on'", "'invoke'", "'onDone'", "'onError'", "'onEntry'", 
			"'onExit'", "'list'", "'map'", "'optional'", null, "'timestamp'", "'{'", 
			"'}'", "'('", "')'", "'['", "']'", "';'", "','", "':'", "'->'", "'/'", 
			"'@'", "'$'", "'<'", "'>'", "'.'", null, null, null, null, "'null'"
		};
	}
	private static final String[] _LITERAL_NAMES = makeLiteralNames();
	private static String[] makeSymbolicNames() {
		return new String[] {
			null, "ACTORS", "ACTOR", "TYPES", "STRUCT", "ENUM", "COMMUNICATION", 
			"PROTOCOL", "CHANNEL", "EVENT", "SERVICES", "INTERFACE", "SERVICE", "MACHINES", 
			"MACHINE", "INITIAL", "STATE", "STATES", "HISTORY", "SHALLOW", "DEEP", 
			"PARALLEL", "FINAL", "TARGET", "ACTIONS", "GUARDS", "ON", "INVOKE", "ONDONE", 
			"ONERROR", "ON_ENTRY", "ON_EXIT", "LIST", "MAP", "OPTIONAL", "PRIMITIVE_TYPE", 
			"TIMESTAMP_TYPE", "LBRACE", "RBRACE", "LPAREN", "RPAREN", "LBRACK", "RBRACK", 
			"SEMI", "COMMA", "COLON", "ARROW", "SLASH", "AT", "DOLLAR", "LT", "GT", 
			"DOT", "STRING", "INT", "FLOAT", "BOOLEAN", "NULL", "ID", "WS", "COMMENT"
		};
	}
	private static final String[] _SYMBOLIC_NAMES = makeSymbolicNames();
	public static final Vocabulary VOCABULARY = new VocabularyImpl(_LITERAL_NAMES, _SYMBOLIC_NAMES);

	/**
	 * @deprecated Use {@link #VOCABULARY} instead.
	 */
	@Deprecated
	public static final String[] tokenNames;
	static {
		tokenNames = new String[_SYMBOLIC_NAMES.length];
		for (int i = 0; i < tokenNames.length; i++) {
			tokenNames[i] = VOCABULARY.getLiteralName(i);
			if (tokenNames[i] == null) {
				tokenNames[i] = VOCABULARY.getSymbolicName(i);
			}

			if (tokenNames[i] == null) {
				tokenNames[i] = "<INVALID>";
			}
		}
	}

	@Override
	@Deprecated
	public String[] getTokenNames() {
		return tokenNames;
	}

	@Override

	public Vocabulary getVocabulary() {
		return VOCABULARY;
	}

	@Override
	public String getGrammarFileName() { return "SSoT.g4"; }

	@Override
	public String[] getRuleNames() { return ruleNames; }

	@Override
	public String getSerializedATN() { return _serializedATN; }

	@Override
	public ATN getATN() { return _ATN; }

	public SSoTParser(TokenStream input) {
		super(input);
		_interp = new ParserATNSimulator(this,_ATN,_decisionToDFA,_sharedContextCache);
	}

	@SuppressWarnings("CheckReturnValue")
	public static class FileContext extends ParserRuleContext {
		public TerminalNode EOF() { return getToken(SSoTParser.EOF, 0); }
		public List<DefinitionBlockContext> definitionBlock() {
			return getRuleContexts(DefinitionBlockContext.class);
		}
		public DefinitionBlockContext definitionBlock(int i) {
			return getRuleContext(DefinitionBlockContext.class,i);
		}
		public FileContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_file; }
	}

	public final FileContext file() throws RecognitionException {
		FileContext _localctx = new FileContext(_ctx, getState());
		enterRule(_localctx, 0, RULE_file);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(109);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while ((((_la) & ~0x3f) == 0 && ((1L << _la) & 844424930141258L) != 0)) {
				{
				{
				setState(106);
				definitionBlock();
				}
				}
				setState(111);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(112);
			match(EOF);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class DefinitionBlockContext extends ParserRuleContext {
		public DefinitionBlockContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_definitionBlock; }
	 
		public DefinitionBlockContext() { }
		public void copyFrom(DefinitionBlockContext ctx) {
			super.copyFrom(ctx);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class TypesBlockDefinitionContext extends DefinitionBlockContext {
		public TypesBlockContext typesBlock() {
			return getRuleContext(TypesBlockContext.class,0);
		}
		public List<AnnotationContext> annotation() {
			return getRuleContexts(AnnotationContext.class);
		}
		public AnnotationContext annotation(int i) {
			return getRuleContext(AnnotationContext.class,i);
		}
		public TypesBlockDefinitionContext(DefinitionBlockContext ctx) { copyFrom(ctx); }
	}
	@SuppressWarnings("CheckReturnValue")
	public static class MachinesBlockDefinitionContext extends DefinitionBlockContext {
		public MachinesBlockContext machinesBlock() {
			return getRuleContext(MachinesBlockContext.class,0);
		}
		public List<AnnotationContext> annotation() {
			return getRuleContexts(AnnotationContext.class);
		}
		public AnnotationContext annotation(int i) {
			return getRuleContext(AnnotationContext.class,i);
		}
		public MachinesBlockDefinitionContext(DefinitionBlockContext ctx) { copyFrom(ctx); }
	}
	@SuppressWarnings("CheckReturnValue")
	public static class ActorsBlockDefinitionContext extends DefinitionBlockContext {
		public ActorsBlockContext actorsBlock() {
			return getRuleContext(ActorsBlockContext.class,0);
		}
		public List<AnnotationContext> annotation() {
			return getRuleContexts(AnnotationContext.class);
		}
		public AnnotationContext annotation(int i) {
			return getRuleContext(AnnotationContext.class,i);
		}
		public ActorsBlockDefinitionContext(DefinitionBlockContext ctx) { copyFrom(ctx); }
	}
	@SuppressWarnings("CheckReturnValue")
	public static class CommunicationBlockDefinitionContext extends DefinitionBlockContext {
		public CommunicationBlockContext communicationBlock() {
			return getRuleContext(CommunicationBlockContext.class,0);
		}
		public List<AnnotationContext> annotation() {
			return getRuleContexts(AnnotationContext.class);
		}
		public AnnotationContext annotation(int i) {
			return getRuleContext(AnnotationContext.class,i);
		}
		public CommunicationBlockDefinitionContext(DefinitionBlockContext ctx) { copyFrom(ctx); }
	}
	@SuppressWarnings("CheckReturnValue")
	public static class ServicesBlockDefinitionContext extends DefinitionBlockContext {
		public ServicesBlockContext servicesBlock() {
			return getRuleContext(ServicesBlockContext.class,0);
		}
		public List<AnnotationContext> annotation() {
			return getRuleContexts(AnnotationContext.class);
		}
		public AnnotationContext annotation(int i) {
			return getRuleContext(AnnotationContext.class,i);
		}
		public ServicesBlockDefinitionContext(DefinitionBlockContext ctx) { copyFrom(ctx); }
	}

	public final DefinitionBlockContext definitionBlock() throws RecognitionException {
		DefinitionBlockContext _localctx = new DefinitionBlockContext(_ctx, getState());
		enterRule(_localctx, 2, RULE_definitionBlock);
		int _la;
		try {
			setState(149);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,6,_ctx) ) {
			case 1:
				_localctx = new ActorsBlockDefinitionContext(_localctx);
				enterOuterAlt(_localctx, 1);
				{
				setState(117);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while (_la==AT || _la==DOLLAR) {
					{
					{
					setState(114);
					annotation();
					}
					}
					setState(119);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				setState(120);
				actorsBlock();
				}
				break;
			case 2:
				_localctx = new TypesBlockDefinitionContext(_localctx);
				enterOuterAlt(_localctx, 2);
				{
				setState(124);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while (_la==AT || _la==DOLLAR) {
					{
					{
					setState(121);
					annotation();
					}
					}
					setState(126);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				setState(127);
				typesBlock();
				}
				break;
			case 3:
				_localctx = new ServicesBlockDefinitionContext(_localctx);
				enterOuterAlt(_localctx, 3);
				{
				setState(131);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while (_la==AT || _la==DOLLAR) {
					{
					{
					setState(128);
					annotation();
					}
					}
					setState(133);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				setState(134);
				servicesBlock();
				}
				break;
			case 4:
				_localctx = new CommunicationBlockDefinitionContext(_localctx);
				enterOuterAlt(_localctx, 4);
				{
				setState(138);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while (_la==AT || _la==DOLLAR) {
					{
					{
					setState(135);
					annotation();
					}
					}
					setState(140);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				setState(141);
				communicationBlock();
				}
				break;
			case 5:
				_localctx = new MachinesBlockDefinitionContext(_localctx);
				enterOuterAlt(_localctx, 5);
				{
				setState(145);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while (_la==AT || _la==DOLLAR) {
					{
					{
					setState(142);
					annotation();
					}
					}
					setState(147);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				setState(148);
				machinesBlock();
				}
				break;
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class ActorsBlockContext extends ParserRuleContext {
		public TerminalNode ACTORS() { return getToken(SSoTParser.ACTORS, 0); }
		public TerminalNode LBRACE() { return getToken(SSoTParser.LBRACE, 0); }
		public TerminalNode RBRACE() { return getToken(SSoTParser.RBRACE, 0); }
		public List<ActorDefinitionContext> actorDefinition() {
			return getRuleContexts(ActorDefinitionContext.class);
		}
		public ActorDefinitionContext actorDefinition(int i) {
			return getRuleContext(ActorDefinitionContext.class,i);
		}
		public ActorsBlockContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_actorsBlock; }
	}

	public final ActorsBlockContext actorsBlock() throws RecognitionException {
		ActorsBlockContext _localctx = new ActorsBlockContext(_ctx, getState());
		enterRule(_localctx, 4, RULE_actorsBlock);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(151);
			match(ACTORS);
			setState(152);
			match(LBRACE);
			setState(156);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while ((((_la) & ~0x3f) == 0 && ((1L << _la) & 844424930131972L) != 0)) {
				{
				{
				setState(153);
				actorDefinition();
				}
				}
				setState(158);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(159);
			match(RBRACE);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class ActorDefinitionContext extends ParserRuleContext {
		public TerminalNode ACTOR() { return getToken(SSoTParser.ACTOR, 0); }
		public TerminalNode ID() { return getToken(SSoTParser.ID, 0); }
		public TerminalNode LBRACE() { return getToken(SSoTParser.LBRACE, 0); }
		public TerminalNode RBRACE() { return getToken(SSoTParser.RBRACE, 0); }
		public List<AnnotationContext> annotation() {
			return getRuleContexts(AnnotationContext.class);
		}
		public AnnotationContext annotation(int i) {
			return getRuleContext(AnnotationContext.class,i);
		}
		public ActorDefinitionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_actorDefinition; }
	}

	public final ActorDefinitionContext actorDefinition() throws RecognitionException {
		ActorDefinitionContext _localctx = new ActorDefinitionContext(_ctx, getState());
		enterRule(_localctx, 6, RULE_actorDefinition);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(164);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==AT || _la==DOLLAR) {
				{
				{
				setState(161);
				annotation();
				}
				}
				setState(166);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(167);
			match(ACTOR);
			setState(168);
			match(ID);
			setState(169);
			match(LBRACE);
			setState(173);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==AT || _la==DOLLAR) {
				{
				{
				setState(170);
				annotation();
				}
				}
				setState(175);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(176);
			match(RBRACE);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class TypesBlockContext extends ParserRuleContext {
		public TerminalNode TYPES() { return getToken(SSoTParser.TYPES, 0); }
		public TerminalNode LBRACE() { return getToken(SSoTParser.LBRACE, 0); }
		public TerminalNode RBRACE() { return getToken(SSoTParser.RBRACE, 0); }
		public List<TypeDefinitionContext> typeDefinition() {
			return getRuleContexts(TypeDefinitionContext.class);
		}
		public TypeDefinitionContext typeDefinition(int i) {
			return getRuleContext(TypeDefinitionContext.class,i);
		}
		public TypesBlockContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_typesBlock; }
	}

	public final TypesBlockContext typesBlock() throws RecognitionException {
		TypesBlockContext _localctx = new TypesBlockContext(_ctx, getState());
		enterRule(_localctx, 8, RULE_typesBlock);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(178);
			match(TYPES);
			setState(179);
			match(LBRACE);
			setState(183);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==STRUCT || _la==ENUM) {
				{
				{
				setState(180);
				typeDefinition();
				}
				}
				setState(185);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(186);
			match(RBRACE);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class TypeDefinitionContext extends ParserRuleContext {
		public StructDefinitionContext structDefinition() {
			return getRuleContext(StructDefinitionContext.class,0);
		}
		public EnumDefinitionContext enumDefinition() {
			return getRuleContext(EnumDefinitionContext.class,0);
		}
		public TypeDefinitionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_typeDefinition; }
	}

	public final TypeDefinitionContext typeDefinition() throws RecognitionException {
		TypeDefinitionContext _localctx = new TypeDefinitionContext(_ctx, getState());
		enterRule(_localctx, 10, RULE_typeDefinition);
		try {
			setState(190);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case STRUCT:
				enterOuterAlt(_localctx, 1);
				{
				setState(188);
				structDefinition();
				}
				break;
			case ENUM:
				enterOuterAlt(_localctx, 2);
				{
				setState(189);
				enumDefinition();
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class StructDefinitionContext extends ParserRuleContext {
		public TerminalNode STRUCT() { return getToken(SSoTParser.STRUCT, 0); }
		public TerminalNode ID() { return getToken(SSoTParser.ID, 0); }
		public TerminalNode LBRACE() { return getToken(SSoTParser.LBRACE, 0); }
		public TerminalNode RBRACE() { return getToken(SSoTParser.RBRACE, 0); }
		public List<StructFieldDefinitionContext> structFieldDefinition() {
			return getRuleContexts(StructFieldDefinitionContext.class);
		}
		public StructFieldDefinitionContext structFieldDefinition(int i) {
			return getRuleContext(StructFieldDefinitionContext.class,i);
		}
		public StructDefinitionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_structDefinition; }
	}

	public final StructDefinitionContext structDefinition() throws RecognitionException {
		StructDefinitionContext _localctx = new StructDefinitionContext(_ctx, getState());
		enterRule(_localctx, 12, RULE_structDefinition);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(192);
			match(STRUCT);
			setState(193);
			match(ID);
			setState(194);
			match(LBRACE);
			setState(198);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while ((((_la) & ~0x3f) == 0 && ((1L << _la) & 289074801081843712L) != 0)) {
				{
				{
				setState(195);
				structFieldDefinition();
				}
				}
				setState(200);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(201);
			match(RBRACE);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class StructFieldDefinitionContext extends ParserRuleContext {
		public TerminalNode ID() { return getToken(SSoTParser.ID, 0); }
		public TerminalNode COLON() { return getToken(SSoTParser.COLON, 0); }
		public TypeReferenceContext typeReference() {
			return getRuleContext(TypeReferenceContext.class,0);
		}
		public TerminalNode SEMI() { return getToken(SSoTParser.SEMI, 0); }
		public List<AnnotationContext> annotation() {
			return getRuleContexts(AnnotationContext.class);
		}
		public AnnotationContext annotation(int i) {
			return getRuleContext(AnnotationContext.class,i);
		}
		public TerminalNode LBRACE() { return getToken(SSoTParser.LBRACE, 0); }
		public TerminalNode RBRACE() { return getToken(SSoTParser.RBRACE, 0); }
		public StructFieldDefinitionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_structFieldDefinition; }
	}

	public final StructFieldDefinitionContext structFieldDefinition() throws RecognitionException {
		StructFieldDefinitionContext _localctx = new StructFieldDefinitionContext(_ctx, getState());
		enterRule(_localctx, 14, RULE_structFieldDefinition);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(206);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==AT || _la==DOLLAR) {
				{
				{
				setState(203);
				annotation();
				}
				}
				setState(208);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(209);
			match(ID);
			setState(210);
			match(COLON);
			setState(211);
			typeReference();
			setState(220);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==LBRACE) {
				{
				setState(212);
				match(LBRACE);
				setState(216);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while (_la==AT || _la==DOLLAR) {
					{
					{
					setState(213);
					annotation();
					}
					}
					setState(218);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				setState(219);
				match(RBRACE);
				}
			}

			setState(222);
			match(SEMI);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class EnumDefinitionContext extends ParserRuleContext {
		public TerminalNode ENUM() { return getToken(SSoTParser.ENUM, 0); }
		public TerminalNode ID() { return getToken(SSoTParser.ID, 0); }
		public TerminalNode LBRACE() { return getToken(SSoTParser.LBRACE, 0); }
		public TerminalNode RBRACE() { return getToken(SSoTParser.RBRACE, 0); }
		public List<EnumVariantDefinitionContext> enumVariantDefinition() {
			return getRuleContexts(EnumVariantDefinitionContext.class);
		}
		public EnumVariantDefinitionContext enumVariantDefinition(int i) {
			return getRuleContext(EnumVariantDefinitionContext.class,i);
		}
		public EnumDefinitionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_enumDefinition; }
	}

	public final EnumDefinitionContext enumDefinition() throws RecognitionException {
		EnumDefinitionContext _localctx = new EnumDefinitionContext(_ctx, getState());
		enterRule(_localctx, 16, RULE_enumDefinition);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(224);
			match(ENUM);
			setState(225);
			match(ID);
			setState(226);
			match(LBRACE);
			setState(230);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while ((((_la) & ~0x3f) == 0 && ((1L << _la) & 289074801081843712L) != 0)) {
				{
				{
				setState(227);
				enumVariantDefinition();
				}
				}
				setState(232);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(233);
			match(RBRACE);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class EnumVariantDefinitionContext extends ParserRuleContext {
		public TerminalNode ID() { return getToken(SSoTParser.ID, 0); }
		public TerminalNode SEMI() { return getToken(SSoTParser.SEMI, 0); }
		public List<AnnotationContext> annotation() {
			return getRuleContexts(AnnotationContext.class);
		}
		public AnnotationContext annotation(int i) {
			return getRuleContext(AnnotationContext.class,i);
		}
		public EnumVariantDefinitionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_enumVariantDefinition; }
	}

	public final EnumVariantDefinitionContext enumVariantDefinition() throws RecognitionException {
		EnumVariantDefinitionContext _localctx = new EnumVariantDefinitionContext(_ctx, getState());
		enterRule(_localctx, 18, RULE_enumVariantDefinition);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(238);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==AT || _la==DOLLAR) {
				{
				{
				setState(235);
				annotation();
				}
				}
				setState(240);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(241);
			match(ID);
			setState(242);
			match(SEMI);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class CommunicationBlockContext extends ParserRuleContext {
		public TerminalNode COMMUNICATION() { return getToken(SSoTParser.COMMUNICATION, 0); }
		public TerminalNode LBRACE() { return getToken(SSoTParser.LBRACE, 0); }
		public TerminalNode RBRACE() { return getToken(SSoTParser.RBRACE, 0); }
		public List<CommunicationDefinitionContext> communicationDefinition() {
			return getRuleContexts(CommunicationDefinitionContext.class);
		}
		public CommunicationDefinitionContext communicationDefinition(int i) {
			return getRuleContext(CommunicationDefinitionContext.class,i);
		}
		public CommunicationBlockContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_communicationBlock; }
	}

	public final CommunicationBlockContext communicationBlock() throws RecognitionException {
		CommunicationBlockContext _localctx = new CommunicationBlockContext(_ctx, getState());
		enterRule(_localctx, 20, RULE_communicationBlock);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(244);
			match(COMMUNICATION);
			setState(245);
			match(LBRACE);
			setState(249);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while ((((_la) & ~0x3f) == 0 && ((1L << _la) & 896L) != 0)) {
				{
				{
				setState(246);
				communicationDefinition();
				}
				}
				setState(251);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(252);
			match(RBRACE);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class CommunicationDefinitionContext extends ParserRuleContext {
		public ProtocolDefinitionContext protocolDefinition() {
			return getRuleContext(ProtocolDefinitionContext.class,0);
		}
		public ChannelDefinitionContext channelDefinition() {
			return getRuleContext(ChannelDefinitionContext.class,0);
		}
		public EventDefinitionContext eventDefinition() {
			return getRuleContext(EventDefinitionContext.class,0);
		}
		public CommunicationDefinitionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_communicationDefinition; }
	}

	public final CommunicationDefinitionContext communicationDefinition() throws RecognitionException {
		CommunicationDefinitionContext _localctx = new CommunicationDefinitionContext(_ctx, getState());
		enterRule(_localctx, 22, RULE_communicationDefinition);
		try {
			setState(257);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case PROTOCOL:
				enterOuterAlt(_localctx, 1);
				{
				setState(254);
				protocolDefinition();
				}
				break;
			case CHANNEL:
				enterOuterAlt(_localctx, 2);
				{
				setState(255);
				channelDefinition();
				}
				break;
			case EVENT:
				enterOuterAlt(_localctx, 3);
				{
				setState(256);
				eventDefinition();
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class ProtocolDefinitionContext extends ParserRuleContext {
		public TerminalNode PROTOCOL() { return getToken(SSoTParser.PROTOCOL, 0); }
		public TerminalNode ID() { return getToken(SSoTParser.ID, 0); }
		public TerminalNode LBRACE() { return getToken(SSoTParser.LBRACE, 0); }
		public TerminalNode RBRACE() { return getToken(SSoTParser.RBRACE, 0); }
		public List<AnnotationContext> annotation() {
			return getRuleContexts(AnnotationContext.class);
		}
		public AnnotationContext annotation(int i) {
			return getRuleContext(AnnotationContext.class,i);
		}
		public ProtocolDefinitionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_protocolDefinition; }
	}

	public final ProtocolDefinitionContext protocolDefinition() throws RecognitionException {
		ProtocolDefinitionContext _localctx = new ProtocolDefinitionContext(_ctx, getState());
		enterRule(_localctx, 24, RULE_protocolDefinition);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(259);
			match(PROTOCOL);
			setState(260);
			match(ID);
			setState(261);
			match(LBRACE);
			setState(265);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==AT || _la==DOLLAR) {
				{
				{
				setState(262);
				annotation();
				}
				}
				setState(267);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(268);
			match(RBRACE);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class ChannelDefinitionContext extends ParserRuleContext {
		public TerminalNode CHANNEL() { return getToken(SSoTParser.CHANNEL, 0); }
		public TerminalNode ID() { return getToken(SSoTParser.ID, 0); }
		public TerminalNode LBRACE() { return getToken(SSoTParser.LBRACE, 0); }
		public TerminalNode RBRACE() { return getToken(SSoTParser.RBRACE, 0); }
		public List<AnnotationContext> annotation() {
			return getRuleContexts(AnnotationContext.class);
		}
		public AnnotationContext annotation(int i) {
			return getRuleContext(AnnotationContext.class,i);
		}
		public ChannelDefinitionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_channelDefinition; }
	}

	public final ChannelDefinitionContext channelDefinition() throws RecognitionException {
		ChannelDefinitionContext _localctx = new ChannelDefinitionContext(_ctx, getState());
		enterRule(_localctx, 26, RULE_channelDefinition);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(270);
			match(CHANNEL);
			setState(271);
			match(ID);
			setState(272);
			match(LBRACE);
			setState(276);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==AT || _la==DOLLAR) {
				{
				{
				setState(273);
				annotation();
				}
				}
				setState(278);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(279);
			match(RBRACE);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class EventDefinitionContext extends ParserRuleContext {
		public TerminalNode EVENT() { return getToken(SSoTParser.EVENT, 0); }
		public TerminalNode ID() { return getToken(SSoTParser.ID, 0); }
		public TerminalNode LBRACE() { return getToken(SSoTParser.LBRACE, 0); }
		public TerminalNode RBRACE() { return getToken(SSoTParser.RBRACE, 0); }
		public List<AnnotationContext> annotation() {
			return getRuleContexts(AnnotationContext.class);
		}
		public AnnotationContext annotation(int i) {
			return getRuleContext(AnnotationContext.class,i);
		}
		public List<EventFieldDefinitionContext> eventFieldDefinition() {
			return getRuleContexts(EventFieldDefinitionContext.class);
		}
		public EventFieldDefinitionContext eventFieldDefinition(int i) {
			return getRuleContext(EventFieldDefinitionContext.class,i);
		}
		public EventDefinitionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_eventDefinition; }
	}

	public final EventDefinitionContext eventDefinition() throws RecognitionException {
		EventDefinitionContext _localctx = new EventDefinitionContext(_ctx, getState());
		enterRule(_localctx, 28, RULE_eventDefinition);
		int _la;
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(281);
			match(EVENT);
			setState(282);
			match(ID);
			setState(283);
			match(LBRACE);
			setState(287);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,22,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					{
					{
					setState(284);
					annotation();
					}
					} 
				}
				setState(289);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,22,_ctx);
			}
			setState(293);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while ((((_la) & ~0x3f) == 0 && ((1L << _la) & 289074801081843712L) != 0)) {
				{
				{
				setState(290);
				eventFieldDefinition();
				}
				}
				setState(295);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(296);
			match(RBRACE);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class EventFieldDefinitionContext extends ParserRuleContext {
		public TerminalNode ID() { return getToken(SSoTParser.ID, 0); }
		public TerminalNode COLON() { return getToken(SSoTParser.COLON, 0); }
		public TypeReferenceContext typeReference() {
			return getRuleContext(TypeReferenceContext.class,0);
		}
		public TerminalNode SEMI() { return getToken(SSoTParser.SEMI, 0); }
		public List<AnnotationContext> annotation() {
			return getRuleContexts(AnnotationContext.class);
		}
		public AnnotationContext annotation(int i) {
			return getRuleContext(AnnotationContext.class,i);
		}
		public EventFieldDefinitionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_eventFieldDefinition; }
	}

	public final EventFieldDefinitionContext eventFieldDefinition() throws RecognitionException {
		EventFieldDefinitionContext _localctx = new EventFieldDefinitionContext(_ctx, getState());
		enterRule(_localctx, 30, RULE_eventFieldDefinition);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(301);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==AT || _la==DOLLAR) {
				{
				{
				setState(298);
				annotation();
				}
				}
				setState(303);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(304);
			match(ID);
			setState(305);
			match(COLON);
			setState(306);
			typeReference();
			setState(307);
			match(SEMI);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class ServicesBlockContext extends ParserRuleContext {
		public TerminalNode SERVICES() { return getToken(SSoTParser.SERVICES, 0); }
		public TerminalNode LBRACE() { return getToken(SSoTParser.LBRACE, 0); }
		public TerminalNode RBRACE() { return getToken(SSoTParser.RBRACE, 0); }
		public List<ServiceElementContext> serviceElement() {
			return getRuleContexts(ServiceElementContext.class);
		}
		public ServiceElementContext serviceElement(int i) {
			return getRuleContext(ServiceElementContext.class,i);
		}
		public ServicesBlockContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_servicesBlock; }
	}

	public final ServicesBlockContext servicesBlock() throws RecognitionException {
		ServicesBlockContext _localctx = new ServicesBlockContext(_ctx, getState());
		enterRule(_localctx, 32, RULE_servicesBlock);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(309);
			match(SERVICES);
			setState(310);
			match(LBRACE);
			setState(314);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==INTERFACE || _la==SERVICE) {
				{
				{
				setState(311);
				serviceElement();
				}
				}
				setState(316);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(317);
			match(RBRACE);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class ServiceElementContext extends ParserRuleContext {
		public InterfaceDefinitionContext interfaceDefinition() {
			return getRuleContext(InterfaceDefinitionContext.class,0);
		}
		public ServiceDefinitionContext serviceDefinition() {
			return getRuleContext(ServiceDefinitionContext.class,0);
		}
		public ServiceElementContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_serviceElement; }
	}

	public final ServiceElementContext serviceElement() throws RecognitionException {
		ServiceElementContext _localctx = new ServiceElementContext(_ctx, getState());
		enterRule(_localctx, 34, RULE_serviceElement);
		try {
			setState(321);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case INTERFACE:
				enterOuterAlt(_localctx, 1);
				{
				setState(319);
				interfaceDefinition();
				}
				break;
			case SERVICE:
				enterOuterAlt(_localctx, 2);
				{
				setState(320);
				serviceDefinition();
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class InterfaceDefinitionContext extends ParserRuleContext {
		public TerminalNode INTERFACE() { return getToken(SSoTParser.INTERFACE, 0); }
		public TerminalNode ID() { return getToken(SSoTParser.ID, 0); }
		public TerminalNode LBRACE() { return getToken(SSoTParser.LBRACE, 0); }
		public TerminalNode RBRACE() { return getToken(SSoTParser.RBRACE, 0); }
		public List<AnnotationContext> annotation() {
			return getRuleContexts(AnnotationContext.class);
		}
		public AnnotationContext annotation(int i) {
			return getRuleContext(AnnotationContext.class,i);
		}
		public List<MethodDefinitionContext> methodDefinition() {
			return getRuleContexts(MethodDefinitionContext.class);
		}
		public MethodDefinitionContext methodDefinition(int i) {
			return getRuleContext(MethodDefinitionContext.class,i);
		}
		public InterfaceDefinitionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_interfaceDefinition; }
	}

	public final InterfaceDefinitionContext interfaceDefinition() throws RecognitionException {
		InterfaceDefinitionContext _localctx = new InterfaceDefinitionContext(_ctx, getState());
		enterRule(_localctx, 36, RULE_interfaceDefinition);
		int _la;
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(323);
			match(INTERFACE);
			setState(324);
			match(ID);
			setState(325);
			match(LBRACE);
			setState(329);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,27,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					{
					{
					setState(326);
					annotation();
					}
					} 
				}
				setState(331);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,27,_ctx);
			}
			setState(335);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while ((((_la) & ~0x3f) == 0 && ((1L << _la) & 289074801081843712L) != 0)) {
				{
				{
				setState(332);
				methodDefinition();
				}
				}
				setState(337);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(338);
			match(RBRACE);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class ServiceDefinitionContext extends ParserRuleContext {
		public TerminalNode SERVICE() { return getToken(SSoTParser.SERVICE, 0); }
		public TerminalNode ID() { return getToken(SSoTParser.ID, 0); }
		public TerminalNode LBRACE() { return getToken(SSoTParser.LBRACE, 0); }
		public TerminalNode RBRACE() { return getToken(SSoTParser.RBRACE, 0); }
		public List<AnnotationContext> annotation() {
			return getRuleContexts(AnnotationContext.class);
		}
		public AnnotationContext annotation(int i) {
			return getRuleContext(AnnotationContext.class,i);
		}
		public ServiceDefinitionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_serviceDefinition; }
	}

	public final ServiceDefinitionContext serviceDefinition() throws RecognitionException {
		ServiceDefinitionContext _localctx = new ServiceDefinitionContext(_ctx, getState());
		enterRule(_localctx, 38, RULE_serviceDefinition);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(340);
			match(SERVICE);
			setState(341);
			match(ID);
			setState(342);
			match(LBRACE);
			setState(346);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==AT || _la==DOLLAR) {
				{
				{
				setState(343);
				annotation();
				}
				}
				setState(348);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(349);
			match(RBRACE);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class MethodDefinitionContext extends ParserRuleContext {
		public TerminalNode ID() { return getToken(SSoTParser.ID, 0); }
		public TerminalNode LPAREN() { return getToken(SSoTParser.LPAREN, 0); }
		public TerminalNode RPAREN() { return getToken(SSoTParser.RPAREN, 0); }
		public TerminalNode SEMI() { return getToken(SSoTParser.SEMI, 0); }
		public List<AnnotationContext> annotation() {
			return getRuleContexts(AnnotationContext.class);
		}
		public AnnotationContext annotation(int i) {
			return getRuleContext(AnnotationContext.class,i);
		}
		public ParamListContext paramList() {
			return getRuleContext(ParamListContext.class,0);
		}
		public TerminalNode ARROW() { return getToken(SSoTParser.ARROW, 0); }
		public TypeReferenceContext typeReference() {
			return getRuleContext(TypeReferenceContext.class,0);
		}
		public MethodDefinitionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_methodDefinition; }
	}

	public final MethodDefinitionContext methodDefinition() throws RecognitionException {
		MethodDefinitionContext _localctx = new MethodDefinitionContext(_ctx, getState());
		enterRule(_localctx, 40, RULE_methodDefinition);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(354);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==AT || _la==DOLLAR) {
				{
				{
				setState(351);
				annotation();
				}
				}
				setState(356);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(357);
			match(ID);
			setState(358);
			match(LPAREN);
			setState(360);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==ID) {
				{
				setState(359);
				paramList();
				}
			}

			setState(362);
			match(RPAREN);
			setState(365);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==ARROW) {
				{
				setState(363);
				match(ARROW);
				setState(364);
				typeReference();
				}
			}

			setState(367);
			match(SEMI);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class MachinesBlockContext extends ParserRuleContext {
		public TerminalNode MACHINES() { return getToken(SSoTParser.MACHINES, 0); }
		public TerminalNode LBRACE() { return getToken(SSoTParser.LBRACE, 0); }
		public TerminalNode RBRACE() { return getToken(SSoTParser.RBRACE, 0); }
		public List<MachineDefinitionContext> machineDefinition() {
			return getRuleContexts(MachineDefinitionContext.class);
		}
		public MachineDefinitionContext machineDefinition(int i) {
			return getRuleContext(MachineDefinitionContext.class,i);
		}
		public MachinesBlockContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_machinesBlock; }
	}

	public final MachinesBlockContext machinesBlock() throws RecognitionException {
		MachinesBlockContext _localctx = new MachinesBlockContext(_ctx, getState());
		enterRule(_localctx, 42, RULE_machinesBlock);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(369);
			match(MACHINES);
			setState(370);
			match(LBRACE);
			setState(374);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==MACHINE) {
				{
				{
				setState(371);
				machineDefinition();
				}
				}
				setState(376);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(377);
			match(RBRACE);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class MachineDefinitionContext extends ParserRuleContext {
		public TerminalNode MACHINE() { return getToken(SSoTParser.MACHINE, 0); }
		public TerminalNode ID() { return getToken(SSoTParser.ID, 0); }
		public TerminalNode LBRACE() { return getToken(SSoTParser.LBRACE, 0); }
		public TerminalNode RBRACE() { return getToken(SSoTParser.RBRACE, 0); }
		public List<MachineBodyElementContext> machineBodyElement() {
			return getRuleContexts(MachineBodyElementContext.class);
		}
		public MachineBodyElementContext machineBodyElement(int i) {
			return getRuleContext(MachineBodyElementContext.class,i);
		}
		public MachineDefinitionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_machineDefinition; }
	}

	public final MachineDefinitionContext machineDefinition() throws RecognitionException {
		MachineDefinitionContext _localctx = new MachineDefinitionContext(_ctx, getState());
		enterRule(_localctx, 44, RULE_machineDefinition);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(379);
			match(MACHINE);
			setState(380);
			match(ID);
			setState(381);
			match(LBRACE);
			setState(385);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while ((((_la) & ~0x3f) == 0 && ((1L << _la) & 50462720L) != 0)) {
				{
				{
				setState(382);
				machineBodyElement();
				}
				}
				setState(387);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(388);
			match(RBRACE);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class MachineBodyElementContext extends ParserRuleContext {
		public StatesDefinitionContext statesDefinition() {
			return getRuleContext(StatesDefinitionContext.class,0);
		}
		public ActionsDefinitionContext actionsDefinition() {
			return getRuleContext(ActionsDefinitionContext.class,0);
		}
		public GuardsDefinitionContext guardsDefinition() {
			return getRuleContext(GuardsDefinitionContext.class,0);
		}
		public MachineBodyElementContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_machineBodyElement; }
	}

	public final MachineBodyElementContext machineBodyElement() throws RecognitionException {
		MachineBodyElementContext _localctx = new MachineBodyElementContext(_ctx, getState());
		enterRule(_localctx, 46, RULE_machineBodyElement);
		try {
			setState(393);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case STATES:
				enterOuterAlt(_localctx, 1);
				{
				setState(390);
				statesDefinition();
				}
				break;
			case ACTIONS:
				enterOuterAlt(_localctx, 2);
				{
				setState(391);
				actionsDefinition();
				}
				break;
			case GUARDS:
				enterOuterAlt(_localctx, 3);
				{
				setState(392);
				guardsDefinition();
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class ActionsDefinitionContext extends ParserRuleContext {
		public TerminalNode ACTIONS() { return getToken(SSoTParser.ACTIONS, 0); }
		public TerminalNode LBRACE() { return getToken(SSoTParser.LBRACE, 0); }
		public TerminalNode RBRACE() { return getToken(SSoTParser.RBRACE, 0); }
		public List<ActionDefinitionContext> actionDefinition() {
			return getRuleContexts(ActionDefinitionContext.class);
		}
		public ActionDefinitionContext actionDefinition(int i) {
			return getRuleContext(ActionDefinitionContext.class,i);
		}
		public ActionsDefinitionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_actionsDefinition; }
	}

	public final ActionsDefinitionContext actionsDefinition() throws RecognitionException {
		ActionsDefinitionContext _localctx = new ActionsDefinitionContext(_ctx, getState());
		enterRule(_localctx, 48, RULE_actionsDefinition);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(395);
			match(ACTIONS);
			setState(396);
			match(LBRACE);
			setState(400);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==ID) {
				{
				{
				setState(397);
				actionDefinition();
				}
				}
				setState(402);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(403);
			match(RBRACE);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class ActionDefinitionContext extends ParserRuleContext {
		public TerminalNode ID() { return getToken(SSoTParser.ID, 0); }
		public TerminalNode LPAREN() { return getToken(SSoTParser.LPAREN, 0); }
		public TerminalNode RPAREN() { return getToken(SSoTParser.RPAREN, 0); }
		public TerminalNode SEMI() { return getToken(SSoTParser.SEMI, 0); }
		public ParamListContext paramList() {
			return getRuleContext(ParamListContext.class,0);
		}
		public ActionDefinitionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_actionDefinition; }
	}

	public final ActionDefinitionContext actionDefinition() throws RecognitionException {
		ActionDefinitionContext _localctx = new ActionDefinitionContext(_ctx, getState());
		enterRule(_localctx, 50, RULE_actionDefinition);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(405);
			match(ID);
			setState(406);
			match(LPAREN);
			setState(408);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==ID) {
				{
				setState(407);
				paramList();
				}
			}

			setState(410);
			match(RPAREN);
			setState(411);
			match(SEMI);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class GuardsDefinitionContext extends ParserRuleContext {
		public TerminalNode GUARDS() { return getToken(SSoTParser.GUARDS, 0); }
		public TerminalNode LBRACE() { return getToken(SSoTParser.LBRACE, 0); }
		public TerminalNode RBRACE() { return getToken(SSoTParser.RBRACE, 0); }
		public List<GuardDefinitionContext> guardDefinition() {
			return getRuleContexts(GuardDefinitionContext.class);
		}
		public GuardDefinitionContext guardDefinition(int i) {
			return getRuleContext(GuardDefinitionContext.class,i);
		}
		public GuardsDefinitionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_guardsDefinition; }
	}

	public final GuardsDefinitionContext guardsDefinition() throws RecognitionException {
		GuardsDefinitionContext _localctx = new GuardsDefinitionContext(_ctx, getState());
		enterRule(_localctx, 52, RULE_guardsDefinition);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(413);
			match(GUARDS);
			setState(414);
			match(LBRACE);
			setState(418);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==ID) {
				{
				{
				setState(415);
				guardDefinition();
				}
				}
				setState(420);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(421);
			match(RBRACE);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class GuardDefinitionContext extends ParserRuleContext {
		public TerminalNode ID() { return getToken(SSoTParser.ID, 0); }
		public TerminalNode LPAREN() { return getToken(SSoTParser.LPAREN, 0); }
		public TerminalNode RPAREN() { return getToken(SSoTParser.RPAREN, 0); }
		public TerminalNode SEMI() { return getToken(SSoTParser.SEMI, 0); }
		public ParamListContext paramList() {
			return getRuleContext(ParamListContext.class,0);
		}
		public TerminalNode COLON() { return getToken(SSoTParser.COLON, 0); }
		public TypeReferenceContext typeReference() {
			return getRuleContext(TypeReferenceContext.class,0);
		}
		public GuardDefinitionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_guardDefinition; }
	}

	public final GuardDefinitionContext guardDefinition() throws RecognitionException {
		GuardDefinitionContext _localctx = new GuardDefinitionContext(_ctx, getState());
		enterRule(_localctx, 54, RULE_guardDefinition);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(423);
			match(ID);
			setState(424);
			match(LPAREN);
			setState(426);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==ID) {
				{
				setState(425);
				paramList();
				}
			}

			setState(428);
			match(RPAREN);
			setState(431);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==COLON) {
				{
				setState(429);
				match(COLON);
				setState(430);
				typeReference();
				}
			}

			setState(433);
			match(SEMI);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class StatesDefinitionContext extends ParserRuleContext {
		public TerminalNode STATES() { return getToken(SSoTParser.STATES, 0); }
		public TerminalNode LBRACE() { return getToken(SSoTParser.LBRACE, 0); }
		public TerminalNode RBRACE() { return getToken(SSoTParser.RBRACE, 0); }
		public InitialStateDefinitionContext initialStateDefinition() {
			return getRuleContext(InitialStateDefinitionContext.class,0);
		}
		public List<StateDefinitionOrHistoryStateContext> stateDefinitionOrHistoryState() {
			return getRuleContexts(StateDefinitionOrHistoryStateContext.class);
		}
		public StateDefinitionOrHistoryStateContext stateDefinitionOrHistoryState(int i) {
			return getRuleContext(StateDefinitionOrHistoryStateContext.class,i);
		}
		public StatesDefinitionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_statesDefinition; }
	}

	public final StatesDefinitionContext statesDefinition() throws RecognitionException {
		StatesDefinitionContext _localctx = new StatesDefinitionContext(_ctx, getState());
		enterRule(_localctx, 56, RULE_statesDefinition);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(435);
			match(STATES);
			setState(436);
			match(LBRACE);
			setState(438);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==INITIAL) {
				{
				setState(437);
				initialStateDefinition();
				}
			}

			setState(443);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while ((((_la) & ~0x3f) == 0 && ((1L << _la) & 6619136L) != 0)) {
				{
				{
				setState(440);
				stateDefinitionOrHistoryState();
				}
				}
				setState(445);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(446);
			match(RBRACE);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class InitialStateDefinitionContext extends ParserRuleContext {
		public TerminalNode INITIAL() { return getToken(SSoTParser.INITIAL, 0); }
		public TerminalNode STATE() { return getToken(SSoTParser.STATE, 0); }
		public TerminalNode ID() { return getToken(SSoTParser.ID, 0); }
		public TerminalNode SEMI() { return getToken(SSoTParser.SEMI, 0); }
		public InitialStateDefinitionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_initialStateDefinition; }
	}

	public final InitialStateDefinitionContext initialStateDefinition() throws RecognitionException {
		InitialStateDefinitionContext _localctx = new InitialStateDefinitionContext(_ctx, getState());
		enterRule(_localctx, 58, RULE_initialStateDefinition);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(448);
			match(INITIAL);
			setState(449);
			match(STATE);
			setState(450);
			match(ID);
			setState(451);
			match(SEMI);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class StateDefinitionOrHistoryStateContext extends ParserRuleContext {
		public StateDefinitionContext stateDefinition() {
			return getRuleContext(StateDefinitionContext.class,0);
		}
		public HistoryStateDefinitionContext historyStateDefinition() {
			return getRuleContext(HistoryStateDefinitionContext.class,0);
		}
		public StateDefinitionOrHistoryStateContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_stateDefinitionOrHistoryState; }
	}

	public final StateDefinitionOrHistoryStateContext stateDefinitionOrHistoryState() throws RecognitionException {
		StateDefinitionOrHistoryStateContext _localctx = new StateDefinitionOrHistoryStateContext(_ctx, getState());
		enterRule(_localctx, 60, RULE_stateDefinitionOrHistoryState);
		try {
			setState(455);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case STATE:
			case PARALLEL:
			case FINAL:
				enterOuterAlt(_localctx, 1);
				{
				setState(453);
				stateDefinition();
				}
				break;
			case HISTORY:
				enterOuterAlt(_localctx, 2);
				{
				setState(454);
				historyStateDefinition();
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class HistoryStateDefinitionContext extends ParserRuleContext {
		public Token historyType;
		public Token targetState;
		public TerminalNode HISTORY() { return getToken(SSoTParser.HISTORY, 0); }
		public TerminalNode SEMI() { return getToken(SSoTParser.SEMI, 0); }
		public TerminalNode TARGET() { return getToken(SSoTParser.TARGET, 0); }
		public TerminalNode ID() { return getToken(SSoTParser.ID, 0); }
		public TerminalNode SHALLOW() { return getToken(SSoTParser.SHALLOW, 0); }
		public TerminalNode DEEP() { return getToken(SSoTParser.DEEP, 0); }
		public HistoryStateDefinitionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_historyStateDefinition; }
	}

	public final HistoryStateDefinitionContext historyStateDefinition() throws RecognitionException {
		HistoryStateDefinitionContext _localctx = new HistoryStateDefinitionContext(_ctx, getState());
		enterRule(_localctx, 62, RULE_historyStateDefinition);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(457);
			match(HISTORY);
			setState(459);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==SHALLOW || _la==DEEP) {
				{
				setState(458);
				((HistoryStateDefinitionContext)_localctx).historyType = _input.LT(1);
				_la = _input.LA(1);
				if ( !(_la==SHALLOW || _la==DEEP) ) {
					((HistoryStateDefinitionContext)_localctx).historyType = (Token)_errHandler.recoverInline(this);
				}
				else {
					if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
					_errHandler.reportMatch(this);
					consume();
				}
				}
			}

			setState(463);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==TARGET) {
				{
				setState(461);
				match(TARGET);
				setState(462);
				((HistoryStateDefinitionContext)_localctx).targetState = match(ID);
				}
			}

			setState(465);
			match(SEMI);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class StateDefinitionContext extends ParserRuleContext {
		public TerminalNode STATE() { return getToken(SSoTParser.STATE, 0); }
		public TerminalNode ID() { return getToken(SSoTParser.ID, 0); }
		public StateBodyContext stateBody() {
			return getRuleContext(StateBodyContext.class,0);
		}
		public StateTypeContext stateType() {
			return getRuleContext(StateTypeContext.class,0);
		}
		public StateDefinitionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_stateDefinition; }
	}

	public final StateDefinitionContext stateDefinition() throws RecognitionException {
		StateDefinitionContext _localctx = new StateDefinitionContext(_ctx, getState());
		enterRule(_localctx, 64, RULE_stateDefinition);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(468);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==PARALLEL || _la==FINAL) {
				{
				setState(467);
				stateType();
				}
			}

			setState(470);
			match(STATE);
			setState(471);
			match(ID);
			setState(472);
			stateBody();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class StateTypeContext extends ParserRuleContext {
		public TerminalNode PARALLEL() { return getToken(SSoTParser.PARALLEL, 0); }
		public TerminalNode FINAL() { return getToken(SSoTParser.FINAL, 0); }
		public StateTypeContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_stateType; }
	}

	public final StateTypeContext stateType() throws RecognitionException {
		StateTypeContext _localctx = new StateTypeContext(_ctx, getState());
		enterRule(_localctx, 66, RULE_stateType);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(474);
			_la = _input.LA(1);
			if ( !(_la==PARALLEL || _la==FINAL) ) {
			_errHandler.recoverInline(this);
			}
			else {
				if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
				_errHandler.reportMatch(this);
				consume();
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class StateBodyContext extends ParserRuleContext {
		public TerminalNode LBRACE() { return getToken(SSoTParser.LBRACE, 0); }
		public TerminalNode RBRACE() { return getToken(SSoTParser.RBRACE, 0); }
		public List<AnnotationContext> annotation() {
			return getRuleContexts(AnnotationContext.class);
		}
		public AnnotationContext annotation(int i) {
			return getRuleContext(AnnotationContext.class,i);
		}
		public List<StateBodyElementContext> stateBodyElement() {
			return getRuleContexts(StateBodyElementContext.class);
		}
		public StateBodyElementContext stateBodyElement(int i) {
			return getRuleContext(StateBodyElementContext.class,i);
		}
		public TerminalNode SEMI() { return getToken(SSoTParser.SEMI, 0); }
		public StateBodyContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_stateBody; }
	}

	public final StateBodyContext stateBody() throws RecognitionException {
		StateBodyContext _localctx = new StateBodyContext(_ctx, getState());
		enterRule(_localctx, 68, RULE_stateBody);
		int _la;
		try {
			int _alt;
			setState(491);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case LBRACE:
				enterOuterAlt(_localctx, 1);
				{
				setState(476);
				match(LBRACE);
				setState(480);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,47,_ctx);
				while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
					if ( _alt==1 ) {
						{
						{
						setState(477);
						annotation();
						}
						} 
					}
					setState(482);
					_errHandler.sync(this);
					_alt = getInterpreter().adaptivePredict(_input,47,_ctx);
				}
				setState(486);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while ((((_la) & ~0x3f) == 0 && ((1L << _la) & 844428359303168L) != 0)) {
					{
					{
					setState(483);
					stateBodyElement();
					}
					}
					setState(488);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				setState(489);
				match(RBRACE);
				}
				break;
			case SEMI:
				enterOuterAlt(_localctx, 2);
				{
				setState(490);
				match(SEMI);
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class StateBodyElementContext extends ParserRuleContext {
		public EntryExitActionContext entryExitAction() {
			return getRuleContext(EntryExitActionContext.class,0);
		}
		public TransitionDefinitionContext transitionDefinition() {
			return getRuleContext(TransitionDefinitionContext.class,0);
		}
		public InvokeDefinitionContext invokeDefinition() {
			return getRuleContext(InvokeDefinitionContext.class,0);
		}
		public StateDefinitionOrHistoryStateContext stateDefinitionOrHistoryState() {
			return getRuleContext(StateDefinitionOrHistoryStateContext.class,0);
		}
		public AnnotationContext annotation() {
			return getRuleContext(AnnotationContext.class,0);
		}
		public StateBodyElementContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_stateBodyElement; }
	}

	public final StateBodyElementContext stateBodyElement() throws RecognitionException {
		StateBodyElementContext _localctx = new StateBodyElementContext(_ctx, getState());
		enterRule(_localctx, 70, RULE_stateBodyElement);
		try {
			setState(498);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case ON_ENTRY:
			case ON_EXIT:
				enterOuterAlt(_localctx, 1);
				{
				setState(493);
				entryExitAction();
				}
				break;
			case ON:
				enterOuterAlt(_localctx, 2);
				{
				setState(494);
				transitionDefinition();
				}
				break;
			case INVOKE:
				enterOuterAlt(_localctx, 3);
				{
				setState(495);
				invokeDefinition();
				}
				break;
			case STATE:
			case HISTORY:
			case PARALLEL:
			case FINAL:
				enterOuterAlt(_localctx, 4);
				{
				setState(496);
				stateDefinitionOrHistoryState();
				}
				break;
			case AT:
			case DOLLAR:
				enterOuterAlt(_localctx, 5);
				{
				setState(497);
				annotation();
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class EntryExitActionContext extends ParserRuleContext {
		public ActionReferenceContext actionReference() {
			return getRuleContext(ActionReferenceContext.class,0);
		}
		public TerminalNode SEMI() { return getToken(SSoTParser.SEMI, 0); }
		public TerminalNode ON_ENTRY() { return getToken(SSoTParser.ON_ENTRY, 0); }
		public TerminalNode ON_EXIT() { return getToken(SSoTParser.ON_EXIT, 0); }
		public EntryExitActionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_entryExitAction; }
	}

	public final EntryExitActionContext entryExitAction() throws RecognitionException {
		EntryExitActionContext _localctx = new EntryExitActionContext(_ctx, getState());
		enterRule(_localctx, 72, RULE_entryExitAction);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(500);
			_la = _input.LA(1);
			if ( !(_la==ON_ENTRY || _la==ON_EXIT) ) {
			_errHandler.recoverInline(this);
			}
			else {
				if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
				_errHandler.reportMatch(this);
				consume();
			}
			setState(501);
			actionReference();
			setState(502);
			match(SEMI);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class ActionReferenceContext extends ParserRuleContext {
		public TerminalNode ID() { return getToken(SSoTParser.ID, 0); }
		public ActionReferenceContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_actionReference; }
	}

	public final ActionReferenceContext actionReference() throws RecognitionException {
		ActionReferenceContext _localctx = new ActionReferenceContext(_ctx, getState());
		enterRule(_localctx, 74, RULE_actionReference);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(504);
			match(ID);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class TransitionDefinitionContext extends ParserRuleContext {
		public Token event;
		public Token guard;
		public Token action;
		public TerminalNode ON() { return getToken(SSoTParser.ON, 0); }
		public TransitionTargetContext transitionTarget() {
			return getRuleContext(TransitionTargetContext.class,0);
		}
		public TerminalNode SEMI() { return getToken(SSoTParser.SEMI, 0); }
		public List<TerminalNode> ID() { return getTokens(SSoTParser.ID); }
		public TerminalNode ID(int i) {
			return getToken(SSoTParser.ID, i);
		}
		public TerminalNode LBRACK() { return getToken(SSoTParser.LBRACK, 0); }
		public TerminalNode RBRACK() { return getToken(SSoTParser.RBRACK, 0); }
		public TerminalNode SLASH() { return getToken(SSoTParser.SLASH, 0); }
		public TransitionDefinitionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_transitionDefinition; }
	}

	public final TransitionDefinitionContext transitionDefinition() throws RecognitionException {
		TransitionDefinitionContext _localctx = new TransitionDefinitionContext(_ctx, getState());
		enterRule(_localctx, 76, RULE_transitionDefinition);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(506);
			match(ON);
			setState(507);
			((TransitionDefinitionContext)_localctx).event = match(ID);
			setState(511);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==LBRACK) {
				{
				setState(508);
				match(LBRACK);
				setState(509);
				((TransitionDefinitionContext)_localctx).guard = match(ID);
				setState(510);
				match(RBRACK);
				}
			}

			setState(515);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==SLASH) {
				{
				setState(513);
				match(SLASH);
				setState(514);
				((TransitionDefinitionContext)_localctx).action = match(ID);
				}
			}

			setState(517);
			transitionTarget();
			setState(518);
			match(SEMI);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class TransitionTargetContext extends ParserRuleContext {
		public Token state;
		public TerminalNode TARGET() { return getToken(SSoTParser.TARGET, 0); }
		public TerminalNode ID() { return getToken(SSoTParser.ID, 0); }
		public TransitionTargetContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_transitionTarget; }
	}

	public final TransitionTargetContext transitionTarget() throws RecognitionException {
		TransitionTargetContext _localctx = new TransitionTargetContext(_ctx, getState());
		enterRule(_localctx, 78, RULE_transitionTarget);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(520);
			match(TARGET);
			setState(521);
			((TransitionTargetContext)_localctx).state = match(ID);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class InvokeDefinitionContext extends ParserRuleContext {
		public Token src;
		public TerminalNode INVOKE() { return getToken(SSoTParser.INVOKE, 0); }
		public TerminalNode SEMI() { return getToken(SSoTParser.SEMI, 0); }
		public TerminalNode ID() { return getToken(SSoTParser.ID, 0); }
		public TerminalNode LBRACE() { return getToken(SSoTParser.LBRACE, 0); }
		public TerminalNode RBRACE() { return getToken(SSoTParser.RBRACE, 0); }
		public List<InvokeCallbackContext> invokeCallback() {
			return getRuleContexts(InvokeCallbackContext.class);
		}
		public InvokeCallbackContext invokeCallback(int i) {
			return getRuleContext(InvokeCallbackContext.class,i);
		}
		public InvokeDefinitionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_invokeDefinition; }
	}

	public final InvokeDefinitionContext invokeDefinition() throws RecognitionException {
		InvokeDefinitionContext _localctx = new InvokeDefinitionContext(_ctx, getState());
		enterRule(_localctx, 80, RULE_invokeDefinition);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(523);
			match(INVOKE);
			setState(524);
			((InvokeDefinitionContext)_localctx).src = match(ID);
			setState(533);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==LBRACE) {
				{
				setState(525);
				match(LBRACE);
				setState(529);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while (_la==ONDONE || _la==ONERROR) {
					{
					{
					setState(526);
					invokeCallback();
					}
					}
					setState(531);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				setState(532);
				match(RBRACE);
				}
			}

			setState(535);
			match(SEMI);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class InvokeCallbackContext extends ParserRuleContext {
		public TransitionTargetContext transitionTarget() {
			return getRuleContext(TransitionTargetContext.class,0);
		}
		public TerminalNode SEMI() { return getToken(SSoTParser.SEMI, 0); }
		public TerminalNode ONDONE() { return getToken(SSoTParser.ONDONE, 0); }
		public TerminalNode ONERROR() { return getToken(SSoTParser.ONERROR, 0); }
		public InvokeCallbackContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_invokeCallback; }
	}

	public final InvokeCallbackContext invokeCallback() throws RecognitionException {
		InvokeCallbackContext _localctx = new InvokeCallbackContext(_ctx, getState());
		enterRule(_localctx, 82, RULE_invokeCallback);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(537);
			_la = _input.LA(1);
			if ( !(_la==ONDONE || _la==ONERROR) ) {
			_errHandler.recoverInline(this);
			}
			else {
				if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
				_errHandler.reportMatch(this);
				consume();
			}
			setState(538);
			transitionTarget();
			setState(539);
			match(SEMI);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class AnnotationContext extends ParserRuleContext {
		public AnnotationContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_annotation; }
	 
		public AnnotationContext() { }
		public void copyFrom(AnnotationContext ctx) {
			super.copyFrom(ctx);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class ValueAnnotationContext extends AnnotationContext {
		public TerminalNode DOLLAR() { return getToken(SSoTParser.DOLLAR, 0); }
		public AnnotationNameContext annotationName() {
			return getRuleContext(AnnotationNameContext.class,0);
		}
		public AnnotationValueContext annotationValue() {
			return getRuleContext(AnnotationValueContext.class,0);
		}
		public ValueAnnotationContext(AnnotationContext ctx) { copyFrom(ctx); }
	}
	@SuppressWarnings("CheckReturnValue")
	public static class IdAnnotationContext extends AnnotationContext {
		public TerminalNode AT() { return getToken(SSoTParser.AT, 0); }
		public TerminalNode ID() { return getToken(SSoTParser.ID, 0); }
		public TerminalNode LPAREN() { return getToken(SSoTParser.LPAREN, 0); }
		public TerminalNode INT() { return getToken(SSoTParser.INT, 0); }
		public TerminalNode RPAREN() { return getToken(SSoTParser.RPAREN, 0); }
		public IdAnnotationContext(AnnotationContext ctx) { copyFrom(ctx); }
	}

	public final AnnotationContext annotation() throws RecognitionException {
		AnnotationContext _localctx = new AnnotationContext(_ctx, getState());
		enterRule(_localctx, 84, RULE_annotation);
		try {
			setState(550);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case AT:
				_localctx = new IdAnnotationContext(_localctx);
				enterOuterAlt(_localctx, 1);
				{
				setState(541);
				match(AT);
				setState(542);
				match(ID);
				setState(543);
				match(LPAREN);
				setState(544);
				match(INT);
				setState(545);
				match(RPAREN);
				}
				break;
			case DOLLAR:
				_localctx = new ValueAnnotationContext(_localctx);
				enterOuterAlt(_localctx, 2);
				{
				setState(546);
				match(DOLLAR);
				setState(547);
				annotationName();
				setState(548);
				annotationValue();
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class AnnotationNameContext extends ParserRuleContext {
		public TerminalNode ID() { return getToken(SSoTParser.ID, 0); }
		public AnnotationNameContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_annotationName; }
	}

	public final AnnotationNameContext annotationName() throws RecognitionException {
		AnnotationNameContext _localctx = new AnnotationNameContext(_ctx, getState());
		enterRule(_localctx, 86, RULE_annotationName);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(552);
			match(ID);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class AnnotationValueContext extends ParserRuleContext {
		public TerminalNode LPAREN() { return getToken(SSoTParser.LPAREN, 0); }
		public List<LiteralContext> literal() {
			return getRuleContexts(LiteralContext.class);
		}
		public LiteralContext literal(int i) {
			return getRuleContext(LiteralContext.class,i);
		}
		public TerminalNode RPAREN() { return getToken(SSoTParser.RPAREN, 0); }
		public TerminalNode ID() { return getToken(SSoTParser.ID, 0); }
		public TerminalNode LBRACK() { return getToken(SSoTParser.LBRACK, 0); }
		public TerminalNode RBRACK() { return getToken(SSoTParser.RBRACK, 0); }
		public List<TerminalNode> COMMA() { return getTokens(SSoTParser.COMMA); }
		public TerminalNode COMMA(int i) {
			return getToken(SSoTParser.COMMA, i);
		}
		public AnnotationValueContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_annotationValue; }
	}

	public final AnnotationValueContext annotationValue() throws RecognitionException {
		AnnotationValueContext _localctx = new AnnotationValueContext(_ctx, getState());
		enterRule(_localctx, 88, RULE_annotationValue);
		int _la;
		try {
			setState(575);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,58,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(554);
				match(LPAREN);
				setState(555);
				literal();
				setState(556);
				match(RPAREN);
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(558);
				match(LPAREN);
				setState(559);
				match(ID);
				setState(560);
				match(RPAREN);
				}
				break;
			case 3:
				enterOuterAlt(_localctx, 3);
				{
				setState(561);
				match(LPAREN);
				setState(562);
				match(LBRACK);
				setState(571);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if ((((_la) & ~0x3f) == 0 && ((1L << _la) & 279223176896970752L) != 0)) {
					{
					setState(563);
					literal();
					setState(568);
					_errHandler.sync(this);
					_la = _input.LA(1);
					while (_la==COMMA) {
						{
						{
						setState(564);
						match(COMMA);
						setState(565);
						literal();
						}
						}
						setState(570);
						_errHandler.sync(this);
						_la = _input.LA(1);
					}
					}
				}

				setState(573);
				match(RBRACK);
				setState(574);
				match(RPAREN);
				}
				break;
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class LiteralContext extends ParserRuleContext {
		public TerminalNode STRING() { return getToken(SSoTParser.STRING, 0); }
		public TerminalNode INT() { return getToken(SSoTParser.INT, 0); }
		public TerminalNode FLOAT() { return getToken(SSoTParser.FLOAT, 0); }
		public TerminalNode BOOLEAN() { return getToken(SSoTParser.BOOLEAN, 0); }
		public TerminalNode NULL() { return getToken(SSoTParser.NULL, 0); }
		public LiteralContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_literal; }
	}

	public final LiteralContext literal() throws RecognitionException {
		LiteralContext _localctx = new LiteralContext(_ctx, getState());
		enterRule(_localctx, 90, RULE_literal);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(577);
			_la = _input.LA(1);
			if ( !((((_la) & ~0x3f) == 0 && ((1L << _la) & 279223176896970752L) != 0)) ) {
			_errHandler.recoverInline(this);
			}
			else {
				if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
				_errHandler.reportMatch(this);
				consume();
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class ParamListContext extends ParserRuleContext {
		public List<ParameterContext> parameter() {
			return getRuleContexts(ParameterContext.class);
		}
		public ParameterContext parameter(int i) {
			return getRuleContext(ParameterContext.class,i);
		}
		public List<TerminalNode> COMMA() { return getTokens(SSoTParser.COMMA); }
		public TerminalNode COMMA(int i) {
			return getToken(SSoTParser.COMMA, i);
		}
		public ParamListContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_paramList; }
	}

	public final ParamListContext paramList() throws RecognitionException {
		ParamListContext _localctx = new ParamListContext(_ctx, getState());
		enterRule(_localctx, 92, RULE_paramList);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(579);
			parameter();
			setState(584);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==COMMA) {
				{
				{
				setState(580);
				match(COMMA);
				setState(581);
				parameter();
				}
				}
				setState(586);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class ParameterContext extends ParserRuleContext {
		public TerminalNode ID() { return getToken(SSoTParser.ID, 0); }
		public TerminalNode COLON() { return getToken(SSoTParser.COLON, 0); }
		public TypeReferenceContext typeReference() {
			return getRuleContext(TypeReferenceContext.class,0);
		}
		public ParameterContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_parameter; }
	}

	public final ParameterContext parameter() throws RecognitionException {
		ParameterContext _localctx = new ParameterContext(_ctx, getState());
		enterRule(_localctx, 94, RULE_parameter);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(587);
			match(ID);
			setState(588);
			match(COLON);
			setState(589);
			typeReference();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class TypeReferenceContext extends ParserRuleContext {
		public SimpleTypeContext simpleType() {
			return getRuleContext(SimpleTypeContext.class,0);
		}
		public ListTypeContext listType() {
			return getRuleContext(ListTypeContext.class,0);
		}
		public MapTypeContext mapType() {
			return getRuleContext(MapTypeContext.class,0);
		}
		public OptionalTypeContext optionalType() {
			return getRuleContext(OptionalTypeContext.class,0);
		}
		public TerminalNode ID() { return getToken(SSoTParser.ID, 0); }
		public TypeReferenceContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_typeReference; }
	}

	public final TypeReferenceContext typeReference() throws RecognitionException {
		TypeReferenceContext _localctx = new TypeReferenceContext(_ctx, getState());
		enterRule(_localctx, 96, RULE_typeReference);
		try {
			setState(596);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case PRIMITIVE_TYPE:
			case TIMESTAMP_TYPE:
				enterOuterAlt(_localctx, 1);
				{
				setState(591);
				simpleType();
				}
				break;
			case LIST:
				enterOuterAlt(_localctx, 2);
				{
				setState(592);
				listType();
				}
				break;
			case MAP:
				enterOuterAlt(_localctx, 3);
				{
				setState(593);
				mapType();
				}
				break;
			case OPTIONAL:
				enterOuterAlt(_localctx, 4);
				{
				setState(594);
				optionalType();
				}
				break;
			case ID:
				enterOuterAlt(_localctx, 5);
				{
				setState(595);
				match(ID);
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class SimpleTypeContext extends ParserRuleContext {
		public TerminalNode PRIMITIVE_TYPE() { return getToken(SSoTParser.PRIMITIVE_TYPE, 0); }
		public TerminalNode TIMESTAMP_TYPE() { return getToken(SSoTParser.TIMESTAMP_TYPE, 0); }
		public SimpleTypeContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_simpleType; }
	}

	public final SimpleTypeContext simpleType() throws RecognitionException {
		SimpleTypeContext _localctx = new SimpleTypeContext(_ctx, getState());
		enterRule(_localctx, 98, RULE_simpleType);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(598);
			_la = _input.LA(1);
			if ( !(_la==PRIMITIVE_TYPE || _la==TIMESTAMP_TYPE) ) {
			_errHandler.recoverInline(this);
			}
			else {
				if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
				_errHandler.reportMatch(this);
				consume();
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class ListTypeContext extends ParserRuleContext {
		public TerminalNode LIST() { return getToken(SSoTParser.LIST, 0); }
		public TerminalNode LT() { return getToken(SSoTParser.LT, 0); }
		public TypeReferenceContext typeReference() {
			return getRuleContext(TypeReferenceContext.class,0);
		}
		public TerminalNode GT() { return getToken(SSoTParser.GT, 0); }
		public ListTypeContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_listType; }
	}

	public final ListTypeContext listType() throws RecognitionException {
		ListTypeContext _localctx = new ListTypeContext(_ctx, getState());
		enterRule(_localctx, 100, RULE_listType);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(600);
			match(LIST);
			setState(601);
			match(LT);
			setState(602);
			typeReference();
			setState(603);
			match(GT);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class MapTypeContext extends ParserRuleContext {
		public TerminalNode MAP() { return getToken(SSoTParser.MAP, 0); }
		public TerminalNode LT() { return getToken(SSoTParser.LT, 0); }
		public List<TypeReferenceContext> typeReference() {
			return getRuleContexts(TypeReferenceContext.class);
		}
		public TypeReferenceContext typeReference(int i) {
			return getRuleContext(TypeReferenceContext.class,i);
		}
		public TerminalNode COMMA() { return getToken(SSoTParser.COMMA, 0); }
		public TerminalNode GT() { return getToken(SSoTParser.GT, 0); }
		public MapTypeContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_mapType; }
	}

	public final MapTypeContext mapType() throws RecognitionException {
		MapTypeContext _localctx = new MapTypeContext(_ctx, getState());
		enterRule(_localctx, 102, RULE_mapType);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(605);
			match(MAP);
			setState(606);
			match(LT);
			setState(607);
			typeReference();
			setState(608);
			match(COMMA);
			setState(609);
			typeReference();
			setState(610);
			match(GT);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class OptionalTypeContext extends ParserRuleContext {
		public TerminalNode OPTIONAL() { return getToken(SSoTParser.OPTIONAL, 0); }
		public TerminalNode LT() { return getToken(SSoTParser.LT, 0); }
		public TypeReferenceContext typeReference() {
			return getRuleContext(TypeReferenceContext.class,0);
		}
		public TerminalNode GT() { return getToken(SSoTParser.GT, 0); }
		public OptionalTypeContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_optionalType; }
	}

	public final OptionalTypeContext optionalType() throws RecognitionException {
		OptionalTypeContext _localctx = new OptionalTypeContext(_ctx, getState());
		enterRule(_localctx, 104, RULE_optionalType);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(612);
			match(OPTIONAL);
			setState(613);
			match(LT);
			setState(614);
			typeReference();
			setState(615);
			match(GT);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static final String _serializedATN =
		"\u0004\u0001<\u026a\u0002\u0000\u0007\u0000\u0002\u0001\u0007\u0001\u0002"+
		"\u0002\u0007\u0002\u0002\u0003\u0007\u0003\u0002\u0004\u0007\u0004\u0002"+
		"\u0005\u0007\u0005\u0002\u0006\u0007\u0006\u0002\u0007\u0007\u0007\u0002"+
		"\b\u0007\b\u0002\t\u0007\t\u0002\n\u0007\n\u0002\u000b\u0007\u000b\u0002"+
		"\f\u0007\f\u0002\r\u0007\r\u0002\u000e\u0007\u000e\u0002\u000f\u0007\u000f"+
		"\u0002\u0010\u0007\u0010\u0002\u0011\u0007\u0011\u0002\u0012\u0007\u0012"+
		"\u0002\u0013\u0007\u0013\u0002\u0014\u0007\u0014\u0002\u0015\u0007\u0015"+
		"\u0002\u0016\u0007\u0016\u0002\u0017\u0007\u0017\u0002\u0018\u0007\u0018"+
		"\u0002\u0019\u0007\u0019\u0002\u001a\u0007\u001a\u0002\u001b\u0007\u001b"+
		"\u0002\u001c\u0007\u001c\u0002\u001d\u0007\u001d\u0002\u001e\u0007\u001e"+
		"\u0002\u001f\u0007\u001f\u0002 \u0007 \u0002!\u0007!\u0002\"\u0007\"\u0002"+
		"#\u0007#\u0002$\u0007$\u0002%\u0007%\u0002&\u0007&\u0002\'\u0007\'\u0002"+
		"(\u0007(\u0002)\u0007)\u0002*\u0007*\u0002+\u0007+\u0002,\u0007,\u0002"+
		"-\u0007-\u0002.\u0007.\u0002/\u0007/\u00020\u00070\u00021\u00071\u0002"+
		"2\u00072\u00023\u00073\u00024\u00074\u0001\u0000\u0005\u0000l\b\u0000"+
		"\n\u0000\f\u0000o\t\u0000\u0001\u0000\u0001\u0000\u0001\u0001\u0005\u0001"+
		"t\b\u0001\n\u0001\f\u0001w\t\u0001\u0001\u0001\u0001\u0001\u0005\u0001"+
		"{\b\u0001\n\u0001\f\u0001~\t\u0001\u0001\u0001\u0001\u0001\u0005\u0001"+
		"\u0082\b\u0001\n\u0001\f\u0001\u0085\t\u0001\u0001\u0001\u0001\u0001\u0005"+
		"\u0001\u0089\b\u0001\n\u0001\f\u0001\u008c\t\u0001\u0001\u0001\u0001\u0001"+
		"\u0005\u0001\u0090\b\u0001\n\u0001\f\u0001\u0093\t\u0001\u0001\u0001\u0003"+
		"\u0001\u0096\b\u0001\u0001\u0002\u0001\u0002\u0001\u0002\u0005\u0002\u009b"+
		"\b\u0002\n\u0002\f\u0002\u009e\t\u0002\u0001\u0002\u0001\u0002\u0001\u0003"+
		"\u0005\u0003\u00a3\b\u0003\n\u0003\f\u0003\u00a6\t\u0003\u0001\u0003\u0001"+
		"\u0003\u0001\u0003\u0001\u0003\u0005\u0003\u00ac\b\u0003\n\u0003\f\u0003"+
		"\u00af\t\u0003\u0001\u0003\u0001\u0003\u0001\u0004\u0001\u0004\u0001\u0004"+
		"\u0005\u0004\u00b6\b\u0004\n\u0004\f\u0004\u00b9\t\u0004\u0001\u0004\u0001"+
		"\u0004\u0001\u0005\u0001\u0005\u0003\u0005\u00bf\b\u0005\u0001\u0006\u0001"+
		"\u0006\u0001\u0006\u0001\u0006\u0005\u0006\u00c5\b\u0006\n\u0006\f\u0006"+
		"\u00c8\t\u0006\u0001\u0006\u0001\u0006\u0001\u0007\u0005\u0007\u00cd\b"+
		"\u0007\n\u0007\f\u0007\u00d0\t\u0007\u0001\u0007\u0001\u0007\u0001\u0007"+
		"\u0001\u0007\u0001\u0007\u0005\u0007\u00d7\b\u0007\n\u0007\f\u0007\u00da"+
		"\t\u0007\u0001\u0007\u0003\u0007\u00dd\b\u0007\u0001\u0007\u0001\u0007"+
		"\u0001\b\u0001\b\u0001\b\u0001\b\u0005\b\u00e5\b\b\n\b\f\b\u00e8\t\b\u0001"+
		"\b\u0001\b\u0001\t\u0005\t\u00ed\b\t\n\t\f\t\u00f0\t\t\u0001\t\u0001\t"+
		"\u0001\t\u0001\n\u0001\n\u0001\n\u0005\n\u00f8\b\n\n\n\f\n\u00fb\t\n\u0001"+
		"\n\u0001\n\u0001\u000b\u0001\u000b\u0001\u000b\u0003\u000b\u0102\b\u000b"+
		"\u0001\f\u0001\f\u0001\f\u0001\f\u0005\f\u0108\b\f\n\f\f\f\u010b\t\f\u0001"+
		"\f\u0001\f\u0001\r\u0001\r\u0001\r\u0001\r\u0005\r\u0113\b\r\n\r\f\r\u0116"+
		"\t\r\u0001\r\u0001\r\u0001\u000e\u0001\u000e\u0001\u000e\u0001\u000e\u0005"+
		"\u000e\u011e\b\u000e\n\u000e\f\u000e\u0121\t\u000e\u0001\u000e\u0005\u000e"+
		"\u0124\b\u000e\n\u000e\f\u000e\u0127\t\u000e\u0001\u000e\u0001\u000e\u0001"+
		"\u000f\u0005\u000f\u012c\b\u000f\n\u000f\f\u000f\u012f\t\u000f\u0001\u000f"+
		"\u0001\u000f\u0001\u000f\u0001\u000f\u0001\u000f\u0001\u0010\u0001\u0010"+
		"\u0001\u0010\u0005\u0010\u0139\b\u0010\n\u0010\f\u0010\u013c\t\u0010\u0001"+
		"\u0010\u0001\u0010\u0001\u0011\u0001\u0011\u0003\u0011\u0142\b\u0011\u0001"+
		"\u0012\u0001\u0012\u0001\u0012\u0001\u0012\u0005\u0012\u0148\b\u0012\n"+
		"\u0012\f\u0012\u014b\t\u0012\u0001\u0012\u0005\u0012\u014e\b\u0012\n\u0012"+
		"\f\u0012\u0151\t\u0012\u0001\u0012\u0001\u0012\u0001\u0013\u0001\u0013"+
		"\u0001\u0013\u0001\u0013\u0005\u0013\u0159\b\u0013\n\u0013\f\u0013\u015c"+
		"\t\u0013\u0001\u0013\u0001\u0013\u0001\u0014\u0005\u0014\u0161\b\u0014"+
		"\n\u0014\f\u0014\u0164\t\u0014\u0001\u0014\u0001\u0014\u0001\u0014\u0003"+
		"\u0014\u0169\b\u0014\u0001\u0014\u0001\u0014\u0001\u0014\u0003\u0014\u016e"+
		"\b\u0014\u0001\u0014\u0001\u0014\u0001\u0015\u0001\u0015\u0001\u0015\u0005"+
		"\u0015\u0175\b\u0015\n\u0015\f\u0015\u0178\t\u0015\u0001\u0015\u0001\u0015"+
		"\u0001\u0016\u0001\u0016\u0001\u0016\u0001\u0016\u0005\u0016\u0180\b\u0016"+
		"\n\u0016\f\u0016\u0183\t\u0016\u0001\u0016\u0001\u0016\u0001\u0017\u0001"+
		"\u0017\u0001\u0017\u0003\u0017\u018a\b\u0017\u0001\u0018\u0001\u0018\u0001"+
		"\u0018\u0005\u0018\u018f\b\u0018\n\u0018\f\u0018\u0192\t\u0018\u0001\u0018"+
		"\u0001\u0018\u0001\u0019\u0001\u0019\u0001\u0019\u0003\u0019\u0199\b\u0019"+
		"\u0001\u0019\u0001\u0019\u0001\u0019\u0001\u001a\u0001\u001a\u0001\u001a"+
		"\u0005\u001a\u01a1\b\u001a\n\u001a\f\u001a\u01a4\t\u001a\u0001\u001a\u0001"+
		"\u001a\u0001\u001b\u0001\u001b\u0001\u001b\u0003\u001b\u01ab\b\u001b\u0001"+
		"\u001b\u0001\u001b\u0001\u001b\u0003\u001b\u01b0\b\u001b\u0001\u001b\u0001"+
		"\u001b\u0001\u001c\u0001\u001c\u0001\u001c\u0003\u001c\u01b7\b\u001c\u0001"+
		"\u001c\u0005\u001c\u01ba\b\u001c\n\u001c\f\u001c\u01bd\t\u001c\u0001\u001c"+
		"\u0001\u001c\u0001\u001d\u0001\u001d\u0001\u001d\u0001\u001d\u0001\u001d"+
		"\u0001\u001e\u0001\u001e\u0003\u001e\u01c8\b\u001e\u0001\u001f\u0001\u001f"+
		"\u0003\u001f\u01cc\b\u001f\u0001\u001f\u0001\u001f\u0003\u001f\u01d0\b"+
		"\u001f\u0001\u001f\u0001\u001f\u0001 \u0003 \u01d5\b \u0001 \u0001 \u0001"+
		" \u0001 \u0001!\u0001!\u0001\"\u0001\"\u0005\"\u01df\b\"\n\"\f\"\u01e2"+
		"\t\"\u0001\"\u0005\"\u01e5\b\"\n\"\f\"\u01e8\t\"\u0001\"\u0001\"\u0003"+
		"\"\u01ec\b\"\u0001#\u0001#\u0001#\u0001#\u0001#\u0003#\u01f3\b#\u0001"+
		"$\u0001$\u0001$\u0001$\u0001%\u0001%\u0001&\u0001&\u0001&\u0001&\u0001"+
		"&\u0003&\u0200\b&\u0001&\u0001&\u0003&\u0204\b&\u0001&\u0001&\u0001&\u0001"+
		"\'\u0001\'\u0001\'\u0001(\u0001(\u0001(\u0001(\u0005(\u0210\b(\n(\f(\u0213"+
		"\t(\u0001(\u0003(\u0216\b(\u0001(\u0001(\u0001)\u0001)\u0001)\u0001)\u0001"+
		"*\u0001*\u0001*\u0001*\u0001*\u0001*\u0001*\u0001*\u0001*\u0003*\u0227"+
		"\b*\u0001+\u0001+\u0001,\u0001,\u0001,\u0001,\u0001,\u0001,\u0001,\u0001"+
		",\u0001,\u0001,\u0001,\u0001,\u0005,\u0237\b,\n,\f,\u023a\t,\u0003,\u023c"+
		"\b,\u0001,\u0001,\u0003,\u0240\b,\u0001-\u0001-\u0001.\u0001.\u0001.\u0005"+
		".\u0247\b.\n.\f.\u024a\t.\u0001/\u0001/\u0001/\u0001/\u00010\u00010\u0001"+
		"0\u00010\u00010\u00030\u0255\b0\u00011\u00011\u00012\u00012\u00012\u0001"+
		"2\u00012\u00013\u00013\u00013\u00013\u00013\u00013\u00013\u00014\u0001"+
		"4\u00014\u00014\u00014\u00014\u0000\u00005\u0000\u0002\u0004\u0006\b\n"+
		"\f\u000e\u0010\u0012\u0014\u0016\u0018\u001a\u001c\u001e \"$&(*,.0246"+
		"8:<>@BDFHJLNPRTVXZ\\^`bdfh\u0000\u0006\u0001\u0000\u0013\u0014\u0001\u0000"+
		"\u0015\u0016\u0001\u0000\u001e\u001f\u0001\u0000\u001c\u001d\u0001\u0000"+
		"59\u0001\u0000#$\u027d\u0000m\u0001\u0000\u0000\u0000\u0002\u0095\u0001"+
		"\u0000\u0000\u0000\u0004\u0097\u0001\u0000\u0000\u0000\u0006\u00a4\u0001"+
		"\u0000\u0000\u0000\b\u00b2\u0001\u0000\u0000\u0000\n\u00be\u0001\u0000"+
		"\u0000\u0000\f\u00c0\u0001\u0000\u0000\u0000\u000e\u00ce\u0001\u0000\u0000"+
		"\u0000\u0010\u00e0\u0001\u0000\u0000\u0000\u0012\u00ee\u0001\u0000\u0000"+
		"\u0000\u0014\u00f4\u0001\u0000\u0000\u0000\u0016\u0101\u0001\u0000\u0000"+
		"\u0000\u0018\u0103\u0001\u0000\u0000\u0000\u001a\u010e\u0001\u0000\u0000"+
		"\u0000\u001c\u0119\u0001\u0000\u0000\u0000\u001e\u012d\u0001\u0000\u0000"+
		"\u0000 \u0135\u0001\u0000\u0000\u0000\"\u0141\u0001\u0000\u0000\u0000"+
		"$\u0143\u0001\u0000\u0000\u0000&\u0154\u0001\u0000\u0000\u0000(\u0162"+
		"\u0001\u0000\u0000\u0000*\u0171\u0001\u0000\u0000\u0000,\u017b\u0001\u0000"+
		"\u0000\u0000.\u0189\u0001\u0000\u0000\u00000\u018b\u0001\u0000\u0000\u0000"+
		"2\u0195\u0001\u0000\u0000\u00004\u019d\u0001\u0000\u0000\u00006\u01a7"+
		"\u0001\u0000\u0000\u00008\u01b3\u0001\u0000\u0000\u0000:\u01c0\u0001\u0000"+
		"\u0000\u0000<\u01c7\u0001\u0000\u0000\u0000>\u01c9\u0001\u0000\u0000\u0000"+
		"@\u01d4\u0001\u0000\u0000\u0000B\u01da\u0001\u0000\u0000\u0000D\u01eb"+
		"\u0001\u0000\u0000\u0000F\u01f2\u0001\u0000\u0000\u0000H\u01f4\u0001\u0000"+
		"\u0000\u0000J\u01f8\u0001\u0000\u0000\u0000L\u01fa\u0001\u0000\u0000\u0000"+
		"N\u0208\u0001\u0000\u0000\u0000P\u020b\u0001\u0000\u0000\u0000R\u0219"+
		"\u0001\u0000\u0000\u0000T\u0226\u0001\u0000\u0000\u0000V\u0228\u0001\u0000"+
		"\u0000\u0000X\u023f\u0001\u0000\u0000\u0000Z\u0241\u0001\u0000\u0000\u0000"+
		"\\\u0243\u0001\u0000\u0000\u0000^\u024b\u0001\u0000\u0000\u0000`\u0254"+
		"\u0001\u0000\u0000\u0000b\u0256\u0001\u0000\u0000\u0000d\u0258\u0001\u0000"+
		"\u0000\u0000f\u025d\u0001\u0000\u0000\u0000h\u0264\u0001\u0000\u0000\u0000"+
		"jl\u0003\u0002\u0001\u0000kj\u0001\u0000\u0000\u0000lo\u0001\u0000\u0000"+
		"\u0000mk\u0001\u0000\u0000\u0000mn\u0001\u0000\u0000\u0000np\u0001\u0000"+
		"\u0000\u0000om\u0001\u0000\u0000\u0000pq\u0005\u0000\u0000\u0001q\u0001"+
		"\u0001\u0000\u0000\u0000rt\u0003T*\u0000sr\u0001\u0000\u0000\u0000tw\u0001"+
		"\u0000\u0000\u0000us\u0001\u0000\u0000\u0000uv\u0001\u0000\u0000\u0000"+
		"vx\u0001\u0000\u0000\u0000wu\u0001\u0000\u0000\u0000x\u0096\u0003\u0004"+
		"\u0002\u0000y{\u0003T*\u0000zy\u0001\u0000\u0000\u0000{~\u0001\u0000\u0000"+
		"\u0000|z\u0001\u0000\u0000\u0000|}\u0001\u0000\u0000\u0000}\u007f\u0001"+
		"\u0000\u0000\u0000~|\u0001\u0000\u0000\u0000\u007f\u0096\u0003\b\u0004"+
		"\u0000\u0080\u0082\u0003T*\u0000\u0081\u0080\u0001\u0000\u0000\u0000\u0082"+
		"\u0085\u0001\u0000\u0000\u0000\u0083\u0081\u0001\u0000\u0000\u0000\u0083"+
		"\u0084\u0001\u0000\u0000\u0000\u0084\u0086\u0001\u0000\u0000\u0000\u0085"+
		"\u0083\u0001\u0000\u0000\u0000\u0086\u0096\u0003 \u0010\u0000\u0087\u0089"+
		"\u0003T*\u0000\u0088\u0087\u0001\u0000\u0000\u0000\u0089\u008c\u0001\u0000"+
		"\u0000\u0000\u008a\u0088\u0001\u0000\u0000\u0000\u008a\u008b\u0001\u0000"+
		"\u0000\u0000\u008b\u008d\u0001\u0000\u0000\u0000\u008c\u008a\u0001\u0000"+
		"\u0000\u0000\u008d\u0096\u0003\u0014\n\u0000\u008e\u0090\u0003T*\u0000"+
		"\u008f\u008e\u0001\u0000\u0000\u0000\u0090\u0093\u0001\u0000\u0000\u0000"+
		"\u0091\u008f\u0001\u0000\u0000\u0000\u0091\u0092\u0001\u0000\u0000\u0000"+
		"\u0092\u0094\u0001\u0000\u0000\u0000\u0093\u0091\u0001\u0000\u0000\u0000"+
		"\u0094\u0096\u0003*\u0015\u0000\u0095u\u0001\u0000\u0000\u0000\u0095|"+
		"\u0001\u0000\u0000\u0000\u0095\u0083\u0001\u0000\u0000\u0000\u0095\u008a"+
		"\u0001\u0000\u0000\u0000\u0095\u0091\u0001\u0000\u0000\u0000\u0096\u0003"+
		"\u0001\u0000\u0000\u0000\u0097\u0098\u0005\u0001\u0000\u0000\u0098\u009c"+
		"\u0005%\u0000\u0000\u0099\u009b\u0003\u0006\u0003\u0000\u009a\u0099\u0001"+
		"\u0000\u0000\u0000\u009b\u009e\u0001\u0000\u0000\u0000\u009c\u009a\u0001"+
		"\u0000\u0000\u0000\u009c\u009d\u0001\u0000\u0000\u0000\u009d\u009f\u0001"+
		"\u0000\u0000\u0000\u009e\u009c\u0001\u0000\u0000\u0000\u009f\u00a0\u0005"+
		"&\u0000\u0000\u00a0\u0005\u0001\u0000\u0000\u0000\u00a1\u00a3\u0003T*"+
		"\u0000\u00a2\u00a1\u0001\u0000\u0000\u0000\u00a3\u00a6\u0001\u0000\u0000"+
		"\u0000\u00a4\u00a2\u0001\u0000\u0000\u0000\u00a4\u00a5\u0001\u0000\u0000"+
		"\u0000\u00a5\u00a7\u0001\u0000\u0000\u0000\u00a6\u00a4\u0001\u0000\u0000"+
		"\u0000\u00a7\u00a8\u0005\u0002\u0000\u0000\u00a8\u00a9\u0005:\u0000\u0000"+
		"\u00a9\u00ad\u0005%\u0000\u0000\u00aa\u00ac\u0003T*\u0000\u00ab\u00aa"+
		"\u0001\u0000\u0000\u0000\u00ac\u00af\u0001\u0000\u0000\u0000\u00ad\u00ab"+
		"\u0001\u0000\u0000\u0000\u00ad\u00ae\u0001\u0000\u0000\u0000\u00ae\u00b0"+
		"\u0001\u0000\u0000\u0000\u00af\u00ad\u0001\u0000\u0000\u0000\u00b0\u00b1"+
		"\u0005&\u0000\u0000\u00b1\u0007\u0001\u0000\u0000\u0000\u00b2\u00b3\u0005"+
		"\u0003\u0000\u0000\u00b3\u00b7\u0005%\u0000\u0000\u00b4\u00b6\u0003\n"+
		"\u0005\u0000\u00b5\u00b4\u0001\u0000\u0000\u0000\u00b6\u00b9\u0001\u0000"+
		"\u0000\u0000\u00b7\u00b5\u0001\u0000\u0000\u0000\u00b7\u00b8\u0001\u0000"+
		"\u0000\u0000\u00b8\u00ba\u0001\u0000\u0000\u0000\u00b9\u00b7\u0001\u0000"+
		"\u0000\u0000\u00ba\u00bb\u0005&\u0000\u0000\u00bb\t\u0001\u0000\u0000"+
		"\u0000\u00bc\u00bf\u0003\f\u0006\u0000\u00bd\u00bf\u0003\u0010\b\u0000"+
		"\u00be\u00bc\u0001\u0000\u0000\u0000\u00be\u00bd\u0001\u0000\u0000\u0000"+
		"\u00bf\u000b\u0001\u0000\u0000\u0000\u00c0\u00c1\u0005\u0004\u0000\u0000"+
		"\u00c1\u00c2\u0005:\u0000\u0000\u00c2\u00c6\u0005%\u0000\u0000\u00c3\u00c5"+
		"\u0003\u000e\u0007\u0000\u00c4\u00c3\u0001\u0000\u0000\u0000\u00c5\u00c8"+
		"\u0001\u0000\u0000\u0000\u00c6\u00c4\u0001\u0000\u0000\u0000\u00c6\u00c7"+
		"\u0001\u0000\u0000\u0000\u00c7\u00c9\u0001\u0000\u0000\u0000\u00c8\u00c6"+
		"\u0001\u0000\u0000\u0000\u00c9\u00ca\u0005&\u0000\u0000\u00ca\r\u0001"+
		"\u0000\u0000\u0000\u00cb\u00cd\u0003T*\u0000\u00cc\u00cb\u0001\u0000\u0000"+
		"\u0000\u00cd\u00d0\u0001\u0000\u0000\u0000\u00ce\u00cc\u0001\u0000\u0000"+
		"\u0000\u00ce\u00cf\u0001\u0000\u0000\u0000\u00cf\u00d1\u0001\u0000\u0000"+
		"\u0000\u00d0\u00ce\u0001\u0000\u0000\u0000\u00d1\u00d2\u0005:\u0000\u0000"+
		"\u00d2\u00d3\u0005-\u0000\u0000\u00d3\u00dc\u0003`0\u0000\u00d4\u00d8"+
		"\u0005%\u0000\u0000\u00d5\u00d7\u0003T*\u0000\u00d6\u00d5\u0001\u0000"+
		"\u0000\u0000\u00d7\u00da\u0001\u0000\u0000\u0000\u00d8\u00d6\u0001\u0000"+
		"\u0000\u0000\u00d8\u00d9\u0001\u0000\u0000\u0000\u00d9\u00db\u0001\u0000"+
		"\u0000\u0000\u00da\u00d8\u0001\u0000\u0000\u0000\u00db\u00dd\u0005&\u0000"+
		"\u0000\u00dc\u00d4\u0001\u0000\u0000\u0000\u00dc\u00dd\u0001\u0000\u0000"+
		"\u0000\u00dd\u00de\u0001\u0000\u0000\u0000\u00de\u00df\u0005+\u0000\u0000"+
		"\u00df\u000f\u0001\u0000\u0000\u0000\u00e0\u00e1\u0005\u0005\u0000\u0000"+
		"\u00e1\u00e2\u0005:\u0000\u0000\u00e2\u00e6\u0005%\u0000\u0000\u00e3\u00e5"+
		"\u0003\u0012\t\u0000\u00e4\u00e3\u0001\u0000\u0000\u0000\u00e5\u00e8\u0001"+
		"\u0000\u0000\u0000\u00e6\u00e4\u0001\u0000\u0000\u0000\u00e6\u00e7\u0001"+
		"\u0000\u0000\u0000\u00e7\u00e9\u0001\u0000\u0000\u0000\u00e8\u00e6\u0001"+
		"\u0000\u0000\u0000\u00e9\u00ea\u0005&\u0000\u0000\u00ea\u0011\u0001\u0000"+
		"\u0000\u0000\u00eb\u00ed\u0003T*\u0000\u00ec\u00eb\u0001\u0000\u0000\u0000"+
		"\u00ed\u00f0\u0001\u0000\u0000\u0000\u00ee\u00ec\u0001\u0000\u0000\u0000"+
		"\u00ee\u00ef\u0001\u0000\u0000\u0000\u00ef\u00f1\u0001\u0000\u0000\u0000"+
		"\u00f0\u00ee\u0001\u0000\u0000\u0000\u00f1\u00f2\u0005:\u0000\u0000\u00f2"+
		"\u00f3\u0005+\u0000\u0000\u00f3\u0013\u0001\u0000\u0000\u0000\u00f4\u00f5"+
		"\u0005\u0006\u0000\u0000\u00f5\u00f9\u0005%\u0000\u0000\u00f6\u00f8\u0003"+
		"\u0016\u000b\u0000\u00f7\u00f6\u0001\u0000\u0000\u0000\u00f8\u00fb\u0001"+
		"\u0000\u0000\u0000\u00f9\u00f7\u0001\u0000\u0000\u0000\u00f9\u00fa\u0001"+
		"\u0000\u0000\u0000\u00fa\u00fc\u0001\u0000\u0000\u0000\u00fb\u00f9\u0001"+
		"\u0000\u0000\u0000\u00fc\u00fd\u0005&\u0000\u0000\u00fd\u0015\u0001\u0000"+
		"\u0000\u0000\u00fe\u0102\u0003\u0018\f\u0000\u00ff\u0102\u0003\u001a\r"+
		"\u0000\u0100\u0102\u0003\u001c\u000e\u0000\u0101\u00fe\u0001\u0000\u0000"+
		"\u0000\u0101\u00ff\u0001\u0000\u0000\u0000\u0101\u0100\u0001\u0000\u0000"+
		"\u0000\u0102\u0017\u0001\u0000\u0000\u0000\u0103\u0104\u0005\u0007\u0000"+
		"\u0000\u0104\u0105\u0005:\u0000\u0000\u0105\u0109\u0005%\u0000\u0000\u0106"+
		"\u0108\u0003T*\u0000\u0107\u0106\u0001\u0000\u0000\u0000\u0108\u010b\u0001"+
		"\u0000\u0000\u0000\u0109\u0107\u0001\u0000\u0000\u0000\u0109\u010a\u0001"+
		"\u0000\u0000\u0000\u010a\u010c\u0001\u0000\u0000\u0000\u010b\u0109\u0001"+
		"\u0000\u0000\u0000\u010c\u010d\u0005&\u0000\u0000\u010d\u0019\u0001\u0000"+
		"\u0000\u0000\u010e\u010f\u0005\b\u0000\u0000\u010f\u0110\u0005:\u0000"+
		"\u0000\u0110\u0114\u0005%\u0000\u0000\u0111\u0113\u0003T*\u0000\u0112"+
		"\u0111\u0001\u0000\u0000\u0000\u0113\u0116\u0001\u0000\u0000\u0000\u0114"+
		"\u0112\u0001\u0000\u0000\u0000\u0114\u0115\u0001\u0000\u0000\u0000\u0115"+
		"\u0117\u0001\u0000\u0000\u0000\u0116\u0114\u0001\u0000\u0000\u0000\u0117"+
		"\u0118\u0005&\u0000\u0000\u0118\u001b\u0001\u0000\u0000\u0000\u0119\u011a"+
		"\u0005\t\u0000\u0000\u011a\u011b\u0005:\u0000\u0000\u011b\u011f\u0005"+
		"%\u0000\u0000\u011c\u011e\u0003T*\u0000\u011d\u011c\u0001\u0000\u0000"+
		"\u0000\u011e\u0121\u0001\u0000\u0000\u0000\u011f\u011d\u0001\u0000\u0000"+
		"\u0000\u011f\u0120\u0001\u0000\u0000\u0000\u0120\u0125\u0001\u0000\u0000"+
		"\u0000\u0121\u011f\u0001\u0000\u0000\u0000\u0122\u0124\u0003\u001e\u000f"+
		"\u0000\u0123\u0122\u0001\u0000\u0000\u0000\u0124\u0127\u0001\u0000\u0000"+
		"\u0000\u0125\u0123\u0001\u0000\u0000\u0000\u0125\u0126\u0001\u0000\u0000"+
		"\u0000\u0126\u0128\u0001\u0000\u0000\u0000\u0127\u0125\u0001\u0000\u0000"+
		"\u0000\u0128\u0129\u0005&\u0000\u0000\u0129\u001d\u0001\u0000\u0000\u0000"+
		"\u012a\u012c\u0003T*\u0000\u012b\u012a\u0001\u0000\u0000\u0000\u012c\u012f"+
		"\u0001\u0000\u0000\u0000\u012d\u012b\u0001\u0000\u0000\u0000\u012d\u012e"+
		"\u0001\u0000\u0000\u0000\u012e\u0130\u0001\u0000\u0000\u0000\u012f\u012d"+
		"\u0001\u0000\u0000\u0000\u0130\u0131\u0005:\u0000\u0000\u0131\u0132\u0005"+
		"-\u0000\u0000\u0132\u0133\u0003`0\u0000\u0133\u0134\u0005+\u0000\u0000"+
		"\u0134\u001f\u0001\u0000\u0000\u0000\u0135\u0136\u0005\n\u0000\u0000\u0136"+
		"\u013a\u0005%\u0000\u0000\u0137\u0139\u0003\"\u0011\u0000\u0138\u0137"+
		"\u0001\u0000\u0000\u0000\u0139\u013c\u0001\u0000\u0000\u0000\u013a\u0138"+
		"\u0001\u0000\u0000\u0000\u013a\u013b\u0001\u0000\u0000\u0000\u013b\u013d"+
		"\u0001\u0000\u0000\u0000\u013c\u013a\u0001\u0000\u0000\u0000\u013d\u013e"+
		"\u0005&\u0000\u0000\u013e!\u0001\u0000\u0000\u0000\u013f\u0142\u0003$"+
		"\u0012\u0000\u0140\u0142\u0003&\u0013\u0000\u0141\u013f\u0001\u0000\u0000"+
		"\u0000\u0141\u0140\u0001\u0000\u0000\u0000\u0142#\u0001\u0000\u0000\u0000"+
		"\u0143\u0144\u0005\u000b\u0000\u0000\u0144\u0145\u0005:\u0000\u0000\u0145"+
		"\u0149\u0005%\u0000\u0000\u0146\u0148\u0003T*\u0000\u0147\u0146\u0001"+
		"\u0000\u0000\u0000\u0148\u014b\u0001\u0000\u0000\u0000\u0149\u0147\u0001"+
		"\u0000\u0000\u0000\u0149\u014a\u0001\u0000\u0000\u0000\u014a\u014f\u0001"+
		"\u0000\u0000\u0000\u014b\u0149\u0001\u0000\u0000\u0000\u014c\u014e\u0003"+
		"(\u0014\u0000\u014d\u014c\u0001\u0000\u0000\u0000\u014e\u0151\u0001\u0000"+
		"\u0000\u0000\u014f\u014d\u0001\u0000\u0000\u0000\u014f\u0150\u0001\u0000"+
		"\u0000\u0000\u0150\u0152\u0001\u0000\u0000\u0000\u0151\u014f\u0001\u0000"+
		"\u0000\u0000\u0152\u0153\u0005&\u0000\u0000\u0153%\u0001\u0000\u0000\u0000"+
		"\u0154\u0155\u0005\f\u0000\u0000\u0155\u0156\u0005:\u0000\u0000\u0156"+
		"\u015a\u0005%\u0000\u0000\u0157\u0159\u0003T*\u0000\u0158\u0157\u0001"+
		"\u0000\u0000\u0000\u0159\u015c\u0001\u0000\u0000\u0000\u015a\u0158\u0001"+
		"\u0000\u0000\u0000\u015a\u015b\u0001\u0000\u0000\u0000\u015b\u015d\u0001"+
		"\u0000\u0000\u0000\u015c\u015a\u0001\u0000\u0000\u0000\u015d\u015e\u0005"+
		"&\u0000\u0000\u015e\'\u0001\u0000\u0000\u0000\u015f\u0161\u0003T*\u0000"+
		"\u0160\u015f\u0001\u0000\u0000\u0000\u0161\u0164\u0001\u0000\u0000\u0000"+
		"\u0162\u0160\u0001\u0000\u0000\u0000\u0162\u0163\u0001\u0000\u0000\u0000"+
		"\u0163\u0165\u0001\u0000\u0000\u0000\u0164\u0162\u0001\u0000\u0000\u0000"+
		"\u0165\u0166\u0005:\u0000\u0000\u0166\u0168\u0005\'\u0000\u0000\u0167"+
		"\u0169\u0003\\.\u0000\u0168\u0167\u0001\u0000\u0000\u0000\u0168\u0169"+
		"\u0001\u0000\u0000\u0000\u0169\u016a\u0001\u0000\u0000\u0000\u016a\u016d"+
		"\u0005(\u0000\u0000\u016b\u016c\u0005.\u0000\u0000\u016c\u016e\u0003`"+
		"0\u0000\u016d\u016b\u0001\u0000\u0000\u0000\u016d\u016e\u0001\u0000\u0000"+
		"\u0000\u016e\u016f\u0001\u0000\u0000\u0000\u016f\u0170\u0005+\u0000\u0000"+
		"\u0170)\u0001\u0000\u0000\u0000\u0171\u0172\u0005\r\u0000\u0000\u0172"+
		"\u0176\u0005%\u0000\u0000\u0173\u0175\u0003,\u0016\u0000\u0174\u0173\u0001"+
		"\u0000\u0000\u0000\u0175\u0178\u0001\u0000\u0000\u0000\u0176\u0174\u0001"+
		"\u0000\u0000\u0000\u0176\u0177\u0001\u0000\u0000\u0000\u0177\u0179\u0001"+
		"\u0000\u0000\u0000\u0178\u0176\u0001\u0000\u0000\u0000\u0179\u017a\u0005"+
		"&\u0000\u0000\u017a+\u0001\u0000\u0000\u0000\u017b\u017c\u0005\u000e\u0000"+
		"\u0000\u017c\u017d\u0005:\u0000\u0000\u017d\u0181\u0005%\u0000\u0000\u017e"+
		"\u0180\u0003.\u0017\u0000\u017f\u017e\u0001\u0000\u0000\u0000\u0180\u0183"+
		"\u0001\u0000\u0000\u0000\u0181\u017f\u0001\u0000\u0000\u0000\u0181\u0182"+
		"\u0001\u0000\u0000\u0000\u0182\u0184\u0001\u0000\u0000\u0000\u0183\u0181"+
		"\u0001\u0000\u0000\u0000\u0184\u0185\u0005&\u0000\u0000\u0185-\u0001\u0000"+
		"\u0000\u0000\u0186\u018a\u00038\u001c\u0000\u0187\u018a\u00030\u0018\u0000"+
		"\u0188\u018a\u00034\u001a\u0000\u0189\u0186\u0001\u0000\u0000\u0000\u0189"+
		"\u0187\u0001\u0000\u0000\u0000\u0189\u0188\u0001\u0000\u0000\u0000\u018a"+
		"/\u0001\u0000\u0000\u0000\u018b\u018c\u0005\u0018\u0000\u0000\u018c\u0190"+
		"\u0005%\u0000\u0000\u018d\u018f\u00032\u0019\u0000\u018e\u018d\u0001\u0000"+
		"\u0000\u0000\u018f\u0192\u0001\u0000\u0000\u0000\u0190\u018e\u0001\u0000"+
		"\u0000\u0000\u0190\u0191\u0001\u0000\u0000\u0000\u0191\u0193\u0001\u0000"+
		"\u0000\u0000\u0192\u0190\u0001\u0000\u0000\u0000\u0193\u0194\u0005&\u0000"+
		"\u0000\u01941\u0001\u0000\u0000\u0000\u0195\u0196\u0005:\u0000\u0000\u0196"+
		"\u0198\u0005\'\u0000\u0000\u0197\u0199\u0003\\.\u0000\u0198\u0197\u0001"+
		"\u0000\u0000\u0000\u0198\u0199\u0001\u0000\u0000\u0000\u0199\u019a\u0001"+
		"\u0000\u0000\u0000\u019a\u019b\u0005(\u0000\u0000\u019b\u019c\u0005+\u0000"+
		"\u0000\u019c3\u0001\u0000\u0000\u0000\u019d\u019e\u0005\u0019\u0000\u0000"+
		"\u019e\u01a2\u0005%\u0000\u0000\u019f\u01a1\u00036\u001b\u0000\u01a0\u019f"+
		"\u0001\u0000\u0000\u0000\u01a1\u01a4\u0001\u0000\u0000\u0000\u01a2\u01a0"+
		"\u0001\u0000\u0000\u0000\u01a2\u01a3\u0001\u0000\u0000\u0000\u01a3\u01a5"+
		"\u0001\u0000\u0000\u0000\u01a4\u01a2\u0001\u0000\u0000\u0000\u01a5\u01a6"+
		"\u0005&\u0000\u0000\u01a65\u0001\u0000\u0000\u0000\u01a7\u01a8\u0005:"+
		"\u0000\u0000\u01a8\u01aa\u0005\'\u0000\u0000\u01a9\u01ab\u0003\\.\u0000"+
		"\u01aa\u01a9\u0001\u0000\u0000\u0000\u01aa\u01ab\u0001\u0000\u0000\u0000"+
		"\u01ab\u01ac\u0001\u0000\u0000\u0000\u01ac\u01af\u0005(\u0000\u0000\u01ad"+
		"\u01ae\u0005-\u0000\u0000\u01ae\u01b0\u0003`0\u0000\u01af\u01ad\u0001"+
		"\u0000\u0000\u0000\u01af\u01b0\u0001\u0000\u0000\u0000\u01b0\u01b1\u0001"+
		"\u0000\u0000\u0000\u01b1\u01b2\u0005+\u0000\u0000\u01b27\u0001\u0000\u0000"+
		"\u0000\u01b3\u01b4\u0005\u0011\u0000\u0000\u01b4\u01b6\u0005%\u0000\u0000"+
		"\u01b5\u01b7\u0003:\u001d\u0000\u01b6\u01b5\u0001\u0000\u0000\u0000\u01b6"+
		"\u01b7\u0001\u0000\u0000\u0000\u01b7\u01bb\u0001\u0000\u0000\u0000\u01b8"+
		"\u01ba\u0003<\u001e\u0000\u01b9\u01b8\u0001\u0000\u0000\u0000\u01ba\u01bd"+
		"\u0001\u0000\u0000\u0000\u01bb\u01b9\u0001\u0000\u0000\u0000\u01bb\u01bc"+
		"\u0001\u0000\u0000\u0000\u01bc\u01be\u0001\u0000\u0000\u0000\u01bd\u01bb"+
		"\u0001\u0000\u0000\u0000\u01be\u01bf\u0005&\u0000\u0000\u01bf9\u0001\u0000"+
		"\u0000\u0000\u01c0\u01c1\u0005\u000f\u0000\u0000\u01c1\u01c2\u0005\u0010"+
		"\u0000\u0000\u01c2\u01c3\u0005:\u0000\u0000\u01c3\u01c4\u0005+\u0000\u0000"+
		"\u01c4;\u0001\u0000\u0000\u0000\u01c5\u01c8\u0003@ \u0000\u01c6\u01c8"+
		"\u0003>\u001f\u0000\u01c7\u01c5\u0001\u0000\u0000\u0000\u01c7\u01c6\u0001"+
		"\u0000\u0000\u0000\u01c8=\u0001\u0000\u0000\u0000\u01c9\u01cb\u0005\u0012"+
		"\u0000\u0000\u01ca\u01cc\u0007\u0000\u0000\u0000\u01cb\u01ca\u0001\u0000"+
		"\u0000\u0000\u01cb\u01cc\u0001\u0000\u0000\u0000\u01cc\u01cf\u0001\u0000"+
		"\u0000\u0000\u01cd\u01ce\u0005\u0017\u0000\u0000\u01ce\u01d0\u0005:\u0000"+
		"\u0000\u01cf\u01cd\u0001\u0000\u0000\u0000\u01cf\u01d0\u0001\u0000\u0000"+
		"\u0000\u01d0\u01d1\u0001\u0000\u0000\u0000\u01d1\u01d2\u0005+\u0000\u0000"+
		"\u01d2?\u0001\u0000\u0000\u0000\u01d3\u01d5\u0003B!\u0000\u01d4\u01d3"+
		"\u0001\u0000\u0000\u0000\u01d4\u01d5\u0001\u0000\u0000\u0000\u01d5\u01d6"+
		"\u0001\u0000\u0000\u0000\u01d6\u01d7\u0005\u0010\u0000\u0000\u01d7\u01d8"+
		"\u0005:\u0000\u0000\u01d8\u01d9\u0003D\"\u0000\u01d9A\u0001\u0000\u0000"+
		"\u0000\u01da\u01db\u0007\u0001\u0000\u0000\u01dbC\u0001\u0000\u0000\u0000"+
		"\u01dc\u01e0\u0005%\u0000\u0000\u01dd\u01df\u0003T*\u0000\u01de\u01dd"+
		"\u0001\u0000\u0000\u0000\u01df\u01e2\u0001\u0000\u0000\u0000\u01e0\u01de"+
		"\u0001\u0000\u0000\u0000\u01e0\u01e1\u0001\u0000\u0000\u0000\u01e1\u01e6"+
		"\u0001\u0000\u0000\u0000\u01e2\u01e0\u0001\u0000\u0000\u0000\u01e3\u01e5"+
		"\u0003F#\u0000\u01e4\u01e3\u0001\u0000\u0000\u0000\u01e5\u01e8\u0001\u0000"+
		"\u0000\u0000\u01e6\u01e4\u0001\u0000\u0000\u0000\u01e6\u01e7\u0001\u0000"+
		"\u0000\u0000\u01e7\u01e9\u0001\u0000\u0000\u0000\u01e8\u01e6\u0001\u0000"+
		"\u0000\u0000\u01e9\u01ec\u0005&\u0000\u0000\u01ea\u01ec\u0005+\u0000\u0000"+
		"\u01eb\u01dc\u0001\u0000\u0000\u0000\u01eb\u01ea\u0001\u0000\u0000\u0000"+
		"\u01ecE\u0001\u0000\u0000\u0000\u01ed\u01f3\u0003H$\u0000\u01ee\u01f3"+
		"\u0003L&\u0000\u01ef\u01f3\u0003P(\u0000\u01f0\u01f3\u0003<\u001e\u0000"+
		"\u01f1\u01f3\u0003T*\u0000\u01f2\u01ed\u0001\u0000\u0000\u0000\u01f2\u01ee"+
		"\u0001\u0000\u0000\u0000\u01f2\u01ef\u0001\u0000\u0000\u0000\u01f2\u01f0"+
		"\u0001\u0000\u0000\u0000\u01f2\u01f1\u0001\u0000\u0000\u0000\u01f3G\u0001"+
		"\u0000\u0000\u0000\u01f4\u01f5\u0007\u0002\u0000\u0000\u01f5\u01f6\u0003"+
		"J%\u0000\u01f6\u01f7\u0005+\u0000\u0000\u01f7I\u0001\u0000\u0000\u0000"+
		"\u01f8\u01f9\u0005:\u0000\u0000\u01f9K\u0001\u0000\u0000\u0000\u01fa\u01fb"+
		"\u0005\u001a\u0000\u0000\u01fb\u01ff\u0005:\u0000\u0000\u01fc\u01fd\u0005"+
		")\u0000\u0000\u01fd\u01fe\u0005:\u0000\u0000\u01fe\u0200\u0005*\u0000"+
		"\u0000\u01ff\u01fc\u0001\u0000\u0000\u0000\u01ff\u0200\u0001\u0000\u0000"+
		"\u0000\u0200\u0203\u0001\u0000\u0000\u0000\u0201\u0202\u0005/\u0000\u0000"+
		"\u0202\u0204\u0005:\u0000\u0000\u0203\u0201\u0001\u0000\u0000\u0000\u0203"+
		"\u0204\u0001\u0000\u0000\u0000\u0204\u0205\u0001\u0000\u0000\u0000\u0205"+
		"\u0206\u0003N\'\u0000\u0206\u0207\u0005+\u0000\u0000\u0207M\u0001\u0000"+
		"\u0000\u0000\u0208\u0209\u0005\u0017\u0000\u0000\u0209\u020a\u0005:\u0000"+
		"\u0000\u020aO\u0001\u0000\u0000\u0000\u020b\u020c\u0005\u001b\u0000\u0000"+
		"\u020c\u0215\u0005:\u0000\u0000\u020d\u0211\u0005%\u0000\u0000\u020e\u0210"+
		"\u0003R)\u0000\u020f\u020e\u0001\u0000\u0000\u0000\u0210\u0213\u0001\u0000"+
		"\u0000\u0000\u0211\u020f\u0001\u0000\u0000\u0000\u0211\u0212\u0001\u0000"+
		"\u0000\u0000\u0212\u0214\u0001\u0000\u0000\u0000\u0213\u0211\u0001\u0000"+
		"\u0000\u0000\u0214\u0216\u0005&\u0000\u0000\u0215\u020d\u0001\u0000\u0000"+
		"\u0000\u0215\u0216\u0001\u0000\u0000\u0000\u0216\u0217\u0001\u0000\u0000"+
		"\u0000\u0217\u0218\u0005+\u0000\u0000\u0218Q\u0001\u0000\u0000\u0000\u0219"+
		"\u021a\u0007\u0003\u0000\u0000\u021a\u021b\u0003N\'\u0000\u021b\u021c"+
		"\u0005+\u0000\u0000\u021cS\u0001\u0000\u0000\u0000\u021d\u021e\u00050"+
		"\u0000\u0000\u021e\u021f\u0005:\u0000\u0000\u021f\u0220\u0005\'\u0000"+
		"\u0000\u0220\u0221\u00056\u0000\u0000\u0221\u0227\u0005(\u0000\u0000\u0222"+
		"\u0223\u00051\u0000\u0000\u0223\u0224\u0003V+\u0000\u0224\u0225\u0003"+
		"X,\u0000\u0225\u0227\u0001\u0000\u0000\u0000\u0226\u021d\u0001\u0000\u0000"+
		"\u0000\u0226\u0222\u0001\u0000\u0000\u0000\u0227U\u0001\u0000\u0000\u0000"+
		"\u0228\u0229\u0005:\u0000\u0000\u0229W\u0001\u0000\u0000\u0000\u022a\u022b"+
		"\u0005\'\u0000\u0000\u022b\u022c\u0003Z-\u0000\u022c\u022d\u0005(\u0000"+
		"\u0000\u022d\u0240\u0001\u0000\u0000\u0000\u022e\u022f\u0005\'\u0000\u0000"+
		"\u022f\u0230\u0005:\u0000\u0000\u0230\u0240\u0005(\u0000\u0000\u0231\u0232"+
		"\u0005\'\u0000\u0000\u0232\u023b\u0005)\u0000\u0000\u0233\u0238\u0003"+
		"Z-\u0000\u0234\u0235\u0005,\u0000\u0000\u0235\u0237\u0003Z-\u0000\u0236"+
		"\u0234\u0001\u0000\u0000\u0000\u0237\u023a\u0001\u0000\u0000\u0000\u0238"+
		"\u0236\u0001\u0000\u0000\u0000\u0238\u0239\u0001\u0000\u0000\u0000\u0239"+
		"\u023c\u0001\u0000\u0000\u0000\u023a\u0238\u0001\u0000\u0000\u0000\u023b"+
		"\u0233\u0001\u0000\u0000\u0000\u023b\u023c\u0001\u0000\u0000\u0000\u023c"+
		"\u023d\u0001\u0000\u0000\u0000\u023d\u023e\u0005*\u0000\u0000\u023e\u0240"+
		"\u0005(\u0000\u0000\u023f\u022a\u0001\u0000\u0000\u0000\u023f\u022e\u0001"+
		"\u0000\u0000\u0000\u023f\u0231\u0001\u0000\u0000\u0000\u0240Y\u0001\u0000"+
		"\u0000\u0000\u0241\u0242\u0007\u0004\u0000\u0000\u0242[\u0001\u0000\u0000"+
		"\u0000\u0243\u0248\u0003^/\u0000\u0244\u0245\u0005,\u0000\u0000\u0245"+
		"\u0247\u0003^/\u0000\u0246\u0244\u0001\u0000\u0000\u0000\u0247\u024a\u0001"+
		"\u0000\u0000\u0000\u0248\u0246\u0001\u0000\u0000\u0000\u0248\u0249\u0001"+
		"\u0000\u0000\u0000\u0249]\u0001\u0000\u0000\u0000\u024a\u0248\u0001\u0000"+
		"\u0000\u0000\u024b\u024c\u0005:\u0000\u0000\u024c\u024d\u0005-\u0000\u0000"+
		"\u024d\u024e\u0003`0\u0000\u024e_\u0001\u0000\u0000\u0000\u024f\u0255"+
		"\u0003b1\u0000\u0250\u0255\u0003d2\u0000\u0251\u0255\u0003f3\u0000\u0252"+
		"\u0255\u0003h4\u0000\u0253\u0255\u0005:\u0000\u0000\u0254\u024f\u0001"+
		"\u0000\u0000\u0000\u0254\u0250\u0001\u0000\u0000\u0000\u0254\u0251\u0001"+
		"\u0000\u0000\u0000\u0254\u0252\u0001\u0000\u0000\u0000\u0254\u0253\u0001"+
		"\u0000\u0000\u0000\u0255a\u0001\u0000\u0000\u0000\u0256\u0257\u0007\u0005"+
		"\u0000\u0000\u0257c\u0001\u0000\u0000\u0000\u0258\u0259\u0005 \u0000\u0000"+
		"\u0259\u025a\u00052\u0000\u0000\u025a\u025b\u0003`0\u0000\u025b\u025c"+
		"\u00053\u0000\u0000\u025ce\u0001\u0000\u0000\u0000\u025d\u025e\u0005!"+
		"\u0000\u0000\u025e\u025f\u00052\u0000\u0000\u025f\u0260\u0003`0\u0000"+
		"\u0260\u0261\u0005,\u0000\u0000\u0261\u0262\u0003`0\u0000\u0262\u0263"+
		"\u00053\u0000\u0000\u0263g\u0001\u0000\u0000\u0000\u0264\u0265\u0005\""+
		"\u0000\u0000\u0265\u0266\u00052\u0000\u0000\u0266\u0267\u0003`0\u0000"+
		"\u0267\u0268\u00053\u0000\u0000\u0268i\u0001\u0000\u0000\u0000=mu|\u0083"+
		"\u008a\u0091\u0095\u009c\u00a4\u00ad\u00b7\u00be\u00c6\u00ce\u00d8\u00dc"+
		"\u00e6\u00ee\u00f9\u0101\u0109\u0114\u011f\u0125\u012d\u013a\u0141\u0149"+
		"\u014f\u015a\u0162\u0168\u016d\u0176\u0181\u0189\u0190\u0198\u01a2\u01aa"+
		"\u01af\u01b6\u01bb\u01c7\u01cb\u01cf\u01d4\u01e0\u01e6\u01eb\u01f2\u01ff"+
		"\u0203\u0211\u0215\u0226\u0238\u023b\u023f\u0248\u0254";
	public static final ATN _ATN =
		new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}