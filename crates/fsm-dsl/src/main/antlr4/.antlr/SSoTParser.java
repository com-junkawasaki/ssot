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
		IMPORT=1, TYPES=2, ACTORS=3, COMMUNICATION=4, SERVICES=5, MACHINES=6, 
		DEPLOYMENT_CONFIG=7, DEPENDENCIES=8, STRUCT=9, ENUM=10, MACHINE=11, CONTEXT=12, 
		ACTIONS=13, GUARDS=14, INVOKES=15, STATES=16, ON=17, AFTER=18, TRANSITION=19, 
		HISTORY=20, SHALLOW=21, DEEP=22, TARGET=23, SERVICE=24, INTERFACE=25, 
		PROTOCOL=26, ACTOR=27, CHANNEL=28, EVENT=29, EXTENDS=30, ENVIRONMENT=31, 
		INFRASTRUCTURE=32, DEPLOYMENT=33, RUST=34, NODEJS=35, VARIABLES=36, PARAMETERS=37, 
		ACTION=38, GUARD=39, ALLOWED_ACTORS=40, NOT=41, INVOKE=42, ON_ENTRY=43, 
		ON_EXIT=44, OUTPUT=45, DESCRIPTION=46, FEATURES=47, T_U8=48, T_U16=49, 
		T_U32=50, T_U64=51, T_I8=52, T_I16=53, T_I32=54, T_I64=55, T_F32=56, T_F64=57, 
		T_BOOL=58, T_STRING=59, T_TIMESTAMP=60, OPTIONAL=61, LIST=62, MAP=63, 
		ID=64, HEX_ID=65, INT=66, FLOAT=67, STRING=68, BOOLEAN=69, DURATION_UNIT=70, 
		AT=71, DOLLAR=72, LPAREN=73, RPAREN=74, LBRACE=75, RBRACE=76, LBRACK=77, 
		RBRACK=78, LT=79, GT=80, SEMI=81, COLON=82, COMMA=83, DOT=84, ARROW=85, 
		COMMENT=86, ML_COMMENT=87, WS=88, VALIDATE=89, DB=90, META=91, ROUTE=92, 
		PUBLISHES=93, INPUT=94, ONDONE=95, ONERROR=96, SRC=97, VERSION=98, TYPE=99, 
		IMPLEMENTS=100, COMMUNICATESWITH=101, TAGS=102, OPERATIONID=103, COMPLEXITY=104, 
		RESPONSIBLETEAM=105, FINAL=106, DEFAULT=107, TS_OUT=108, CAPNP_OUT=109, 
		MERMAID_OUT=110, DOT_OUT=111, ZOD_OUT=112, JSONSCHEMA_OUT=113, PROTO_OUT=114, 
		AVRO_OUT=115, OPENAPI_OUT=116, GRPC_OUT=117, GRAPHQL_OUT=118, ASYNCAPI_OUT=119, 
		SQL_OUT=120, PRISMA_OUT=121, DRIZZLE_OUT=122, IF=123;
	public static final int
		RULE_file = 0, RULE_fileId = 1, RULE_importStatement = 2, RULE_definitionBlock = 3, 
		RULE_annotationName = 4, RULE_annotation = 5, RULE_annotationValue = 6, 
		RULE_value = 7, RULE_attributePairList = 8, RULE_attributePair = 9, RULE_primitiveValue = 10, 
		RULE_referenceValue = 11, RULE_objectValue = 12, RULE_arrayValue = 13, 
		RULE_valueList = 14, RULE_typesBlock = 15, RULE_typeDefinition = 16, RULE_structDefinition = 17, 
		RULE_fieldDefinition = 18, RULE_enumDefinition = 19, RULE_enumVariant = 20, 
		RULE_typeExpr = 21, RULE_primitiveTypeName = 22, RULE_actorsBlock = 23, 
		RULE_actorDefinition = 24, RULE_communicationBlock = 25, RULE_communicationDefinition = 26, 
		RULE_protocolDefinition = 27, RULE_channelDefinition = 28, RULE_channelBodyElement = 29, 
		RULE_channelParameterDefinition = 30, RULE_eventDefinition = 31, RULE_servicesBlock = 32, 
		RULE_serviceElement = 33, RULE_interfaceDefinition = 34, RULE_methodDefinition = 35, 
		RULE_parameterList = 36, RULE_parameter = 37, RULE_serviceDefinition = 38, 
		RULE_machinesBlock = 39, RULE_machineDefinition = 40, RULE_machineBodyElement = 41, 
		RULE_contextDefinition = 42, RULE_contextField = 43, RULE_actionsDefinition = 44, 
		RULE_actionDefinition = 45, RULE_guardsDefinition = 46, RULE_guardDefinition = 47, 
		RULE_invokesDefinition = 48, RULE_invokeDefinition = 49, RULE_invokeDefinitionBody = 50, 
		RULE_invokeAttribute = 51, RULE_invokeSrc = 52, RULE_invokeInputMapping = 53, 
		RULE_invokeOutputMapping = 54, RULE_invokeOnDone = 55, RULE_invokeOnError = 56, 
		RULE_invokeCompletion = 57, RULE_invokeSource = 58, RULE_expressionValue = 59, 
		RULE_keyValuePairList = 60, RULE_keyValuePair = 61, RULE_statesDefinition = 62, 
		RULE_stateDefinitionOrHistoryState = 63, RULE_stateDefinition = 64, RULE_stateBodyElement = 65, 
		RULE_onEntryExit = 66, RULE_actionReference = 67, RULE_invokeState = 68, 
		RULE_invokeStateBody = 69, RULE_onTransition = 70, RULE_afterTransition = 71, 
		RULE_ifTransitionStatement = 72, RULE_transitionSpec = 73, RULE_targetState = 74, 
		RULE_transitionOptions = 75, RULE_transitionOption = 76, RULE_actionReferenceList = 77, 
		RULE_guardReferenceList = 78, RULE_actorReferenceList = 79, RULE_guardReference = 80, 
		RULE_duration = 81, RULE_historyDefinition = 82, RULE_deploymentConfigBlock = 83, 
		RULE_deploymentElement = 84, RULE_environmentDefinition = 85, RULE_variablesBlock = 86, 
		RULE_variableAssignment = 87, RULE_infrastructureDefinition = 88, RULE_deploymentDefinition = 89, 
		RULE_attributeAssignment = 90, RULE_dependenciesBlock = 91, RULE_targetDependencyBlock = 92, 
		RULE_targetType = 93, RULE_dependencyEntry = 94, RULE_dependencyAttribute = 95;
	private static String[] makeRuleNames() {
		return new String[] {
			"file", "fileId", "importStatement", "definitionBlock", "annotationName", 
			"annotation", "annotationValue", "value", "attributePairList", "attributePair", 
			"primitiveValue", "referenceValue", "objectValue", "arrayValue", "valueList", 
			"typesBlock", "typeDefinition", "structDefinition", "fieldDefinition", 
			"enumDefinition", "enumVariant", "typeExpr", "primitiveTypeName", "actorsBlock", 
			"actorDefinition", "communicationBlock", "communicationDefinition", "protocolDefinition", 
			"channelDefinition", "channelBodyElement", "channelParameterDefinition", 
			"eventDefinition", "servicesBlock", "serviceElement", "interfaceDefinition", 
			"methodDefinition", "parameterList", "parameter", "serviceDefinition", 
			"machinesBlock", "machineDefinition", "machineBodyElement", "contextDefinition", 
			"contextField", "actionsDefinition", "actionDefinition", "guardsDefinition", 
			"guardDefinition", "invokesDefinition", "invokeDefinition", "invokeDefinitionBody", 
			"invokeAttribute", "invokeSrc", "invokeInputMapping", "invokeOutputMapping", 
			"invokeOnDone", "invokeOnError", "invokeCompletion", "invokeSource", 
			"expressionValue", "keyValuePairList", "keyValuePair", "statesDefinition", 
			"stateDefinitionOrHistoryState", "stateDefinition", "stateBodyElement", 
			"onEntryExit", "actionReference", "invokeState", "invokeStateBody", "onTransition", 
			"afterTransition", "ifTransitionStatement", "transitionSpec", "targetState", 
			"transitionOptions", "transitionOption", "actionReferenceList", "guardReferenceList", 
			"actorReferenceList", "guardReference", "duration", "historyDefinition", 
			"deploymentConfigBlock", "deploymentElement", "environmentDefinition", 
			"variablesBlock", "variableAssignment", "infrastructureDefinition", "deploymentDefinition", 
			"attributeAssignment", "dependenciesBlock", "targetDependencyBlock", 
			"targetType", "dependencyEntry", "dependencyAttribute"
		};
	}
	public static final String[] ruleNames = makeRuleNames();

	private static String[] makeLiteralNames() {
		return new String[] {
			null, "'import'", "'types'", "'actors'", "'communication'", "'services'", 
			"'machines'", "'deployment_config'", "'dependencies'", "'struct'", "'enum'", 
			"'machine'", "'context'", "'actions'", "'guards'", "'invokes'", "'states'", 
			"'on'", "'after'", "'transition'", "'history'", "'shallow'", "'deep'", 
			"'target'", "'service'", "'interface'", "'protocol'", "'actor'", "'channel'", 
			"'event'", "'extends'", "'environment'", "'infrastructure'", "'deployment'", 
			"'rust'", "'nodejs'", "'variables'", "'parameters'", "'action'", "'guard'", 
			"'allowedActors'", "'not'", "'invoke'", "'onEntry'", "'onExit'", "'output'", 
			"'description'", "'features'", "'u8'", "'u16'", "'u32'", "'u64'", "'i8'", 
			"'i16'", "'i32'", "'i64'", "'f32'", "'f64'", "'bool'", "'string'", "'timestamp'", 
			"'optional'", "'list'", "'map'", null, null, null, null, null, null, 
			null, "'@'", "'$'", "'('", "')'", "'{'", "'}'", "'['", "']'", "'<'", 
			"'>'", "';'", "':'", "','", "'.'", "'->'"
		};
	}
	private static final String[] _LITERAL_NAMES = makeLiteralNames();
	private static String[] makeSymbolicNames() {
		return new String[] {
			null, "IMPORT", "TYPES", "ACTORS", "COMMUNICATION", "SERVICES", "MACHINES", 
			"DEPLOYMENT_CONFIG", "DEPENDENCIES", "STRUCT", "ENUM", "MACHINE", "CONTEXT", 
			"ACTIONS", "GUARDS", "INVOKES", "STATES", "ON", "AFTER", "TRANSITION", 
			"HISTORY", "SHALLOW", "DEEP", "TARGET", "SERVICE", "INTERFACE", "PROTOCOL", 
			"ACTOR", "CHANNEL", "EVENT", "EXTENDS", "ENVIRONMENT", "INFRASTRUCTURE", 
			"DEPLOYMENT", "RUST", "NODEJS", "VARIABLES", "PARAMETERS", "ACTION", 
			"GUARD", "ALLOWED_ACTORS", "NOT", "INVOKE", "ON_ENTRY", "ON_EXIT", "OUTPUT", 
			"DESCRIPTION", "FEATURES", "T_U8", "T_U16", "T_U32", "T_U64", "T_I8", 
			"T_I16", "T_I32", "T_I64", "T_F32", "T_F64", "T_BOOL", "T_STRING", "T_TIMESTAMP", 
			"OPTIONAL", "LIST", "MAP", "ID", "HEX_ID", "INT", "FLOAT", "STRING", 
			"BOOLEAN", "DURATION_UNIT", "AT", "DOLLAR", "LPAREN", "RPAREN", "LBRACE", 
			"RBRACE", "LBRACK", "RBRACK", "LT", "GT", "SEMI", "COLON", "COMMA", "DOT", 
			"ARROW", "COMMENT", "ML_COMMENT", "WS", "VALIDATE", "DB", "META", "ROUTE", 
			"PUBLISHES", "INPUT", "ONDONE", "ONERROR", "SRC", "VERSION", "TYPE", 
			"IMPLEMENTS", "COMMUNICATESWITH", "TAGS", "OPERATIONID", "COMPLEXITY", 
			"RESPONSIBLETEAM", "FINAL", "DEFAULT", "TS_OUT", "CAPNP_OUT", "MERMAID_OUT", 
			"DOT_OUT", "ZOD_OUT", "JSONSCHEMA_OUT", "PROTO_OUT", "AVRO_OUT", "OPENAPI_OUT", 
			"GRPC_OUT", "GRAPHQL_OUT", "ASYNCAPI_OUT", "SQL_OUT", "PRISMA_OUT", "DRIZZLE_OUT", 
			"IF"
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
		public FileIdContext fileId() {
			return getRuleContext(FileIdContext.class,0);
		}
		public List<AnnotationContext> annotation() {
			return getRuleContexts(AnnotationContext.class);
		}
		public AnnotationContext annotation(int i) {
			return getRuleContext(AnnotationContext.class,i);
		}
		public List<ImportStatementContext> importStatement() {
			return getRuleContexts(ImportStatementContext.class);
		}
		public ImportStatementContext importStatement(int i) {
			return getRuleContext(ImportStatementContext.class,i);
		}
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
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(193);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,0,_ctx) ) {
			case 1:
				{
				setState(192);
				fileId();
				}
				break;
			}
			setState(198);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,1,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					{
					{
					setState(195);
					annotation();
					}
					} 
				}
				setState(200);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,1,_ctx);
			}
			setState(204);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==IMPORT) {
				{
				{
				setState(201);
				importStatement();
				}
				}
				setState(206);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(210);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,3,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					{
					{
					setState(207);
					annotation();
					}
					} 
				}
				setState(212);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,3,_ctx);
			}
			setState(216);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while ((((_la) & ~0x3f) == 0 && ((1L << _la) & 508L) != 0)) {
				{
				{
				setState(213);
				definitionBlock();
				}
				}
				setState(218);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(222);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==AT || _la==DOLLAR) {
				{
				{
				setState(219);
				annotation();
				}
				}
				setState(224);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(225);
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
	public static class FileIdContext extends ParserRuleContext {
		public TerminalNode AT() { return getToken(SSoTParser.AT, 0); }
		public TerminalNode HEX_ID() { return getToken(SSoTParser.HEX_ID, 0); }
		public TerminalNode SEMI() { return getToken(SSoTParser.SEMI, 0); }
		public FileIdContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_fileId; }
	}

	public final FileIdContext fileId() throws RecognitionException {
		FileIdContext _localctx = new FileIdContext(_ctx, getState());
		enterRule(_localctx, 2, RULE_fileId);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(227);
			match(AT);
			setState(228);
			match(HEX_ID);
			setState(229);
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
	public static class ImportStatementContext extends ParserRuleContext {
		public TerminalNode IMPORT() { return getToken(SSoTParser.IMPORT, 0); }
		public TerminalNode STRING() { return getToken(SSoTParser.STRING, 0); }
		public TerminalNode SEMI() { return getToken(SSoTParser.SEMI, 0); }
		public ImportStatementContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_importStatement; }
	}

	public final ImportStatementContext importStatement() throws RecognitionException {
		ImportStatementContext _localctx = new ImportStatementContext(_ctx, getState());
		enterRule(_localctx, 4, RULE_importStatement);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(231);
			match(IMPORT);
			setState(232);
			match(STRING);
			setState(233);
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
	public static class DefinitionBlockContext extends ParserRuleContext {
		public TypesBlockContext typesBlock() {
			return getRuleContext(TypesBlockContext.class,0);
		}
		public ServicesBlockContext servicesBlock() {
			return getRuleContext(ServicesBlockContext.class,0);
		}
		public MachinesBlockContext machinesBlock() {
			return getRuleContext(MachinesBlockContext.class,0);
		}
		public ActorsBlockContext actorsBlock() {
			return getRuleContext(ActorsBlockContext.class,0);
		}
		public CommunicationBlockContext communicationBlock() {
			return getRuleContext(CommunicationBlockContext.class,0);
		}
		public DeploymentConfigBlockContext deploymentConfigBlock() {
			return getRuleContext(DeploymentConfigBlockContext.class,0);
		}
		public DependenciesBlockContext dependenciesBlock() {
			return getRuleContext(DependenciesBlockContext.class,0);
		}
		public DefinitionBlockContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_definitionBlock; }
	}

	public final DefinitionBlockContext definitionBlock() throws RecognitionException {
		DefinitionBlockContext _localctx = new DefinitionBlockContext(_ctx, getState());
		enterRule(_localctx, 6, RULE_definitionBlock);
		try {
			setState(242);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case TYPES:
				enterOuterAlt(_localctx, 1);
				{
				setState(235);
				typesBlock();
				}
				break;
			case SERVICES:
				enterOuterAlt(_localctx, 2);
				{
				setState(236);
				servicesBlock();
				}
				break;
			case MACHINES:
				enterOuterAlt(_localctx, 3);
				{
				setState(237);
				machinesBlock();
				}
				break;
			case ACTORS:
				enterOuterAlt(_localctx, 4);
				{
				setState(238);
				actorsBlock();
				}
				break;
			case COMMUNICATION:
				enterOuterAlt(_localctx, 5);
				{
				setState(239);
				communicationBlock();
				}
				break;
			case DEPLOYMENT_CONFIG:
				enterOuterAlt(_localctx, 6);
				{
				setState(240);
				deploymentConfigBlock();
				}
				break;
			case DEPENDENCIES:
				enterOuterAlt(_localctx, 7);
				{
				setState(241);
				dependenciesBlock();
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
		public TerminalNode CHANNEL() { return getToken(SSoTParser.CHANNEL, 0); }
		public TerminalNode PROTOCOL() { return getToken(SSoTParser.PROTOCOL, 0); }
		public TerminalNode DESCRIPTION() { return getToken(SSoTParser.DESCRIPTION, 0); }
		public TerminalNode VALIDATE() { return getToken(SSoTParser.VALIDATE, 0); }
		public TerminalNode DB() { return getToken(SSoTParser.DB, 0); }
		public TerminalNode META() { return getToken(SSoTParser.META, 0); }
		public TerminalNode ROUTE() { return getToken(SSoTParser.ROUTE, 0); }
		public TerminalNode PUBLISHES() { return getToken(SSoTParser.PUBLISHES, 0); }
		public TerminalNode INPUT() { return getToken(SSoTParser.INPUT, 0); }
		public TerminalNode ONDONE() { return getToken(SSoTParser.ONDONE, 0); }
		public TerminalNode ONERROR() { return getToken(SSoTParser.ONERROR, 0); }
		public TerminalNode SRC() { return getToken(SSoTParser.SRC, 0); }
		public TerminalNode VERSION() { return getToken(SSoTParser.VERSION, 0); }
		public TerminalNode TYPE() { return getToken(SSoTParser.TYPE, 0); }
		public TerminalNode PARAMETERS() { return getToken(SSoTParser.PARAMETERS, 0); }
		public TerminalNode IMPLEMENTS() { return getToken(SSoTParser.IMPLEMENTS, 0); }
		public TerminalNode COMMUNICATESWITH() { return getToken(SSoTParser.COMMUNICATESWITH, 0); }
		public TerminalNode TAGS() { return getToken(SSoTParser.TAGS, 0); }
		public TerminalNode OPERATIONID() { return getToken(SSoTParser.OPERATIONID, 0); }
		public TerminalNode COMPLEXITY() { return getToken(SSoTParser.COMPLEXITY, 0); }
		public TerminalNode RESPONSIBLETEAM() { return getToken(SSoTParser.RESPONSIBLETEAM, 0); }
		public TerminalNode FINAL() { return getToken(SSoTParser.FINAL, 0); }
		public TerminalNode DEFAULT() { return getToken(SSoTParser.DEFAULT, 0); }
		public TerminalNode TS_OUT() { return getToken(SSoTParser.TS_OUT, 0); }
		public TerminalNode CAPNP_OUT() { return getToken(SSoTParser.CAPNP_OUT, 0); }
		public TerminalNode MERMAID_OUT() { return getToken(SSoTParser.MERMAID_OUT, 0); }
		public TerminalNode DOT_OUT() { return getToken(SSoTParser.DOT_OUT, 0); }
		public TerminalNode ZOD_OUT() { return getToken(SSoTParser.ZOD_OUT, 0); }
		public TerminalNode JSONSCHEMA_OUT() { return getToken(SSoTParser.JSONSCHEMA_OUT, 0); }
		public TerminalNode PROTO_OUT() { return getToken(SSoTParser.PROTO_OUT, 0); }
		public TerminalNode AVRO_OUT() { return getToken(SSoTParser.AVRO_OUT, 0); }
		public TerminalNode OPENAPI_OUT() { return getToken(SSoTParser.OPENAPI_OUT, 0); }
		public TerminalNode GRPC_OUT() { return getToken(SSoTParser.GRPC_OUT, 0); }
		public TerminalNode GRAPHQL_OUT() { return getToken(SSoTParser.GRAPHQL_OUT, 0); }
		public TerminalNode ASYNCAPI_OUT() { return getToken(SSoTParser.ASYNCAPI_OUT, 0); }
		public TerminalNode SQL_OUT() { return getToken(SSoTParser.SQL_OUT, 0); }
		public TerminalNode PRISMA_OUT() { return getToken(SSoTParser.PRISMA_OUT, 0); }
		public TerminalNode DRIZZLE_OUT() { return getToken(SSoTParser.DRIZZLE_OUT, 0); }
		public AnnotationNameContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_annotationName; }
	}

	public final AnnotationNameContext annotationName() throws RecognitionException {
		AnnotationNameContext _localctx = new AnnotationNameContext(_ctx, getState());
		enterRule(_localctx, 8, RULE_annotationName);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(244);
			_la = _input.LA(1);
			if ( !((((_la) & ~0x3f) == 0 && ((1L << _la) & 70506518675456L) != 0) || ((((_la - 64)) & ~0x3f) == 0 && ((1L << (_la - 64)) & 576460752269869057L) != 0)) ) {
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
	public static class AnnotationContext extends ParserRuleContext {
		public TerminalNode AT() { return getToken(SSoTParser.AT, 0); }
		public TerminalNode ID() { return getToken(SSoTParser.ID, 0); }
		public TerminalNode LPAREN() { return getToken(SSoTParser.LPAREN, 0); }
		public TerminalNode INT() { return getToken(SSoTParser.INT, 0); }
		public TerminalNode RPAREN() { return getToken(SSoTParser.RPAREN, 0); }
		public TerminalNode DOLLAR() { return getToken(SSoTParser.DOLLAR, 0); }
		public AnnotationNameContext annotationName() {
			return getRuleContext(AnnotationNameContext.class,0);
		}
		public AnnotationValueContext annotationValue() {
			return getRuleContext(AnnotationValueContext.class,0);
		}
		public TerminalNode SEMI() { return getToken(SSoTParser.SEMI, 0); }
		public AnnotationContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_annotation; }
	}

	public final AnnotationContext annotation() throws RecognitionException {
		AnnotationContext _localctx = new AnnotationContext(_ctx, getState());
		enterRule(_localctx, 10, RULE_annotation);
		try {
			setState(263);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,8,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(246);
				match(AT);
				setState(247);
				match(ID);
				setState(248);
				match(LPAREN);
				setState(249);
				match(INT);
				setState(250);
				match(RPAREN);
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(251);
				match(DOLLAR);
				setState(252);
				annotationName();
				setState(253);
				match(LPAREN);
				setState(255);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,7,_ctx) ) {
				case 1:
					{
					setState(254);
					annotationValue();
					}
					break;
				}
				setState(257);
				match(RPAREN);
				}
				break;
			case 3:
				enterOuterAlt(_localctx, 3);
				{
				setState(259);
				match(DOLLAR);
				setState(260);
				annotationName();
				setState(261);
				match(SEMI);
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
	public static class AnnotationValueContext extends ParserRuleContext {
		public AttributePairListContext attributePairList() {
			return getRuleContext(AttributePairListContext.class,0);
		}
		public ValueContext value() {
			return getRuleContext(ValueContext.class,0);
		}
		public AnnotationValueContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_annotationValue; }
	}

	public final AnnotationValueContext annotationValue() throws RecognitionException {
		AnnotationValueContext _localctx = new AnnotationValueContext(_ctx, getState());
		enterRule(_localctx, 12, RULE_annotationValue);
		try {
			setState(268);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,9,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(265);
				if (!(_input.LA(2) == COLON)) throw new FailedPredicateException(this, "_input.LA(2) == COLON");
				setState(266);
				attributePairList();
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(267);
				value();
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
	public static class ValueContext extends ParserRuleContext {
		public PrimitiveValueContext primitiveValue() {
			return getRuleContext(PrimitiveValueContext.class,0);
		}
		public ReferenceValueContext referenceValue() {
			return getRuleContext(ReferenceValueContext.class,0);
		}
		public ObjectValueContext objectValue() {
			return getRuleContext(ObjectValueContext.class,0);
		}
		public ArrayValueContext arrayValue() {
			return getRuleContext(ArrayValueContext.class,0);
		}
		public ValueContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_value; }
	}

	public final ValueContext value() throws RecognitionException {
		ValueContext _localctx = new ValueContext(_ctx, getState());
		enterRule(_localctx, 14, RULE_value);
		try {
			setState(274);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case INT:
			case FLOAT:
			case STRING:
			case BOOLEAN:
				enterOuterAlt(_localctx, 1);
				{
				setState(270);
				primitiveValue();
				}
				break;
			case ID:
				enterOuterAlt(_localctx, 2);
				{
				setState(271);
				referenceValue();
				}
				break;
			case LBRACE:
				enterOuterAlt(_localctx, 3);
				{
				setState(272);
				objectValue();
				}
				break;
			case LBRACK:
				enterOuterAlt(_localctx, 4);
				{
				setState(273);
				arrayValue();
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
	public static class AttributePairListContext extends ParserRuleContext {
		public List<AttributePairContext> attributePair() {
			return getRuleContexts(AttributePairContext.class);
		}
		public AttributePairContext attributePair(int i) {
			return getRuleContext(AttributePairContext.class,i);
		}
		public List<TerminalNode> COMMA() { return getTokens(SSoTParser.COMMA); }
		public TerminalNode COMMA(int i) {
			return getToken(SSoTParser.COMMA, i);
		}
		public AttributePairListContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_attributePairList; }
	}

	public final AttributePairListContext attributePairList() throws RecognitionException {
		AttributePairListContext _localctx = new AttributePairListContext(_ctx, getState());
		enterRule(_localctx, 16, RULE_attributePairList);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(276);
			attributePair();
			setState(281);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==COMMA) {
				{
				{
				setState(277);
				match(COMMA);
				setState(278);
				attributePair();
				}
				}
				setState(283);
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
	public static class AttributePairContext extends ParserRuleContext {
		public TerminalNode ID() { return getToken(SSoTParser.ID, 0); }
		public TerminalNode COLON() { return getToken(SSoTParser.COLON, 0); }
		public PrimitiveValueContext primitiveValue() {
			return getRuleContext(PrimitiveValueContext.class,0);
		}
		public ReferenceValueContext referenceValue() {
			return getRuleContext(ReferenceValueContext.class,0);
		}
		public ObjectValueContext objectValue() {
			return getRuleContext(ObjectValueContext.class,0);
		}
		public ArrayValueContext arrayValue() {
			return getRuleContext(ArrayValueContext.class,0);
		}
		public AttributePairContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_attributePair; }
	}

	public final AttributePairContext attributePair() throws RecognitionException {
		AttributePairContext _localctx = new AttributePairContext(_ctx, getState());
		enterRule(_localctx, 18, RULE_attributePair);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(284);
			match(ID);
			setState(285);
			match(COLON);
			setState(290);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case INT:
			case FLOAT:
			case STRING:
			case BOOLEAN:
				{
				setState(286);
				primitiveValue();
				}
				break;
			case ID:
				{
				setState(287);
				referenceValue();
				}
				break;
			case LBRACE:
				{
				setState(288);
				objectValue();
				}
				break;
			case LBRACK:
				{
				setState(289);
				arrayValue();
				}
				break;
			default:
				throw new NoViableAltException(this);
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
	public static class PrimitiveValueContext extends ParserRuleContext {
		public TerminalNode STRING() { return getToken(SSoTParser.STRING, 0); }
		public TerminalNode INT() { return getToken(SSoTParser.INT, 0); }
		public TerminalNode FLOAT() { return getToken(SSoTParser.FLOAT, 0); }
		public TerminalNode BOOLEAN() { return getToken(SSoTParser.BOOLEAN, 0); }
		public PrimitiveValueContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_primitiveValue; }
	}

	public final PrimitiveValueContext primitiveValue() throws RecognitionException {
		PrimitiveValueContext _localctx = new PrimitiveValueContext(_ctx, getState());
		enterRule(_localctx, 20, RULE_primitiveValue);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(292);
			_la = _input.LA(1);
			if ( !(((((_la - 66)) & ~0x3f) == 0 && ((1L << (_la - 66)) & 15L) != 0)) ) {
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
	public static class ReferenceValueContext extends ParserRuleContext {
		public List<TerminalNode> ID() { return getTokens(SSoTParser.ID); }
		public TerminalNode ID(int i) {
			return getToken(SSoTParser.ID, i);
		}
		public List<TerminalNode> DOT() { return getTokens(SSoTParser.DOT); }
		public TerminalNode DOT(int i) {
			return getToken(SSoTParser.DOT, i);
		}
		public ReferenceValueContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_referenceValue; }
	}

	public final ReferenceValueContext referenceValue() throws RecognitionException {
		ReferenceValueContext _localctx = new ReferenceValueContext(_ctx, getState());
		enterRule(_localctx, 22, RULE_referenceValue);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(294);
			match(ID);
			setState(299);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==DOT) {
				{
				{
				setState(295);
				match(DOT);
				setState(296);
				match(ID);
				}
				}
				setState(301);
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
	public static class ObjectValueContext extends ParserRuleContext {
		public TerminalNode LBRACE() { return getToken(SSoTParser.LBRACE, 0); }
		public TerminalNode RBRACE() { return getToken(SSoTParser.RBRACE, 0); }
		public AttributePairListContext attributePairList() {
			return getRuleContext(AttributePairListContext.class,0);
		}
		public ObjectValueContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_objectValue; }
	}

	public final ObjectValueContext objectValue() throws RecognitionException {
		ObjectValueContext _localctx = new ObjectValueContext(_ctx, getState());
		enterRule(_localctx, 24, RULE_objectValue);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(302);
			match(LBRACE);
			setState(304);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==ID) {
				{
				setState(303);
				attributePairList();
				}
			}

			setState(306);
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
	public static class ArrayValueContext extends ParserRuleContext {
		public TerminalNode LBRACK() { return getToken(SSoTParser.LBRACK, 0); }
		public TerminalNode RBRACK() { return getToken(SSoTParser.RBRACK, 0); }
		public ValueListContext valueList() {
			return getRuleContext(ValueListContext.class,0);
		}
		public ArrayValueContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_arrayValue; }
	}

	public final ArrayValueContext arrayValue() throws RecognitionException {
		ArrayValueContext _localctx = new ArrayValueContext(_ctx, getState());
		enterRule(_localctx, 26, RULE_arrayValue);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(308);
			match(LBRACK);
			setState(310);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (((((_la - 64)) & ~0x3f) == 0 && ((1L << (_la - 64)) & 10301L) != 0)) {
				{
				setState(309);
				valueList();
				}
			}

			setState(312);
			match(RBRACK);
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
	public static class ValueListContext extends ParserRuleContext {
		public List<ValueContext> value() {
			return getRuleContexts(ValueContext.class);
		}
		public ValueContext value(int i) {
			return getRuleContext(ValueContext.class,i);
		}
		public List<TerminalNode> COMMA() { return getTokens(SSoTParser.COMMA); }
		public TerminalNode COMMA(int i) {
			return getToken(SSoTParser.COMMA, i);
		}
		public ValueListContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_valueList; }
	}

	public final ValueListContext valueList() throws RecognitionException {
		ValueListContext _localctx = new ValueListContext(_ctx, getState());
		enterRule(_localctx, 28, RULE_valueList);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(314);
			value();
			setState(319);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==COMMA) {
				{
				{
				setState(315);
				match(COMMA);
				setState(316);
				value();
				}
				}
				setState(321);
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
	public static class TypesBlockContext extends ParserRuleContext {
		public TerminalNode TYPES() { return getToken(SSoTParser.TYPES, 0); }
		public TerminalNode LBRACE() { return getToken(SSoTParser.LBRACE, 0); }
		public TerminalNode RBRACE() { return getToken(SSoTParser.RBRACE, 0); }
		public List<AnnotationContext> annotation() {
			return getRuleContexts(AnnotationContext.class);
		}
		public AnnotationContext annotation(int i) {
			return getRuleContext(AnnotationContext.class,i);
		}
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
		enterRule(_localctx, 30, RULE_typesBlock);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(322);
			match(TYPES);
			setState(323);
			match(LBRACE);
			setState(327);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==AT || _la==DOLLAR) {
				{
				{
				setState(324);
				annotation();
				}
				}
				setState(329);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(333);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==STRUCT || _la==ENUM) {
				{
				{
				setState(330);
				typeDefinition();
				}
				}
				setState(335);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(336);
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
		enterRule(_localctx, 32, RULE_typeDefinition);
		try {
			setState(340);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case STRUCT:
				enterOuterAlt(_localctx, 1);
				{
				setState(338);
				structDefinition();
				}
				break;
			case ENUM:
				enterOuterAlt(_localctx, 2);
				{
				setState(339);
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
		public List<AnnotationContext> annotation() {
			return getRuleContexts(AnnotationContext.class);
		}
		public AnnotationContext annotation(int i) {
			return getRuleContext(AnnotationContext.class,i);
		}
		public List<FieldDefinitionContext> fieldDefinition() {
			return getRuleContexts(FieldDefinitionContext.class);
		}
		public FieldDefinitionContext fieldDefinition(int i) {
			return getRuleContext(FieldDefinitionContext.class,i);
		}
		public StructDefinitionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_structDefinition; }
	}

	public final StructDefinitionContext structDefinition() throws RecognitionException {
		StructDefinitionContext _localctx = new StructDefinitionContext(_ctx, getState());
		enterRule(_localctx, 34, RULE_structDefinition);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(342);
			match(STRUCT);
			setState(343);
			match(ID);
			setState(347);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==AT || _la==DOLLAR) {
				{
				{
				setState(344);
				annotation();
				}
				}
				setState(349);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(350);
			match(LBRACE);
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
			setState(360);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==ID) {
				{
				{
				setState(357);
				fieldDefinition();
				}
				}
				setState(362);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(363);
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
	public static class FieldDefinitionContext extends ParserRuleContext {
		public TerminalNode ID() { return getToken(SSoTParser.ID, 0); }
		public TerminalNode COLON() { return getToken(SSoTParser.COLON, 0); }
		public TypeExprContext typeExpr() {
			return getRuleContext(TypeExprContext.class,0);
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
		public FieldDefinitionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_fieldDefinition; }
	}

	public final FieldDefinitionContext fieldDefinition() throws RecognitionException {
		FieldDefinitionContext _localctx = new FieldDefinitionContext(_ctx, getState());
		enterRule(_localctx, 36, RULE_fieldDefinition);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(365);
			match(ID);
			setState(366);
			match(COLON);
			setState(367);
			typeExpr();
			setState(371);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==AT || _la==DOLLAR) {
				{
				{
				setState(368);
				annotation();
				}
				}
				setState(373);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(382);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==LBRACE) {
				{
				setState(374);
				match(LBRACE);
				setState(378);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while (_la==AT || _la==DOLLAR) {
					{
					{
					setState(375);
					annotation();
					}
					}
					setState(380);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				setState(381);
				match(RBRACE);
				}
			}

			setState(384);
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
		public List<AnnotationContext> annotation() {
			return getRuleContexts(AnnotationContext.class);
		}
		public AnnotationContext annotation(int i) {
			return getRuleContext(AnnotationContext.class,i);
		}
		public List<EnumVariantContext> enumVariant() {
			return getRuleContexts(EnumVariantContext.class);
		}
		public EnumVariantContext enumVariant(int i) {
			return getRuleContext(EnumVariantContext.class,i);
		}
		public EnumDefinitionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_enumDefinition; }
	}

	public final EnumDefinitionContext enumDefinition() throws RecognitionException {
		EnumDefinitionContext _localctx = new EnumDefinitionContext(_ctx, getState());
		enterRule(_localctx, 38, RULE_enumDefinition);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(386);
			match(ENUM);
			setState(387);
			match(ID);
			setState(391);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==AT || _la==DOLLAR) {
				{
				{
				setState(388);
				annotation();
				}
				}
				setState(393);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(394);
			match(LBRACE);
			setState(398);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==AT || _la==DOLLAR) {
				{
				{
				setState(395);
				annotation();
				}
				}
				setState(400);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(404);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==ID) {
				{
				{
				setState(401);
				enumVariant();
				}
				}
				setState(406);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(407);
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
	public static class EnumVariantContext extends ParserRuleContext {
		public TerminalNode ID() { return getToken(SSoTParser.ID, 0); }
		public TerminalNode SEMI() { return getToken(SSoTParser.SEMI, 0); }
		public List<AnnotationContext> annotation() {
			return getRuleContexts(AnnotationContext.class);
		}
		public AnnotationContext annotation(int i) {
			return getRuleContext(AnnotationContext.class,i);
		}
		public EnumVariantContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_enumVariant; }
	}

	public final EnumVariantContext enumVariant() throws RecognitionException {
		EnumVariantContext _localctx = new EnumVariantContext(_ctx, getState());
		enterRule(_localctx, 40, RULE_enumVariant);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(409);
			match(ID);
			setState(413);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==AT || _la==DOLLAR) {
				{
				{
				setState(410);
				annotation();
				}
				}
				setState(415);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(416);
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
	public static class TypeExprContext extends ParserRuleContext {
		public PrimitiveTypeNameContext primitiveTypeName() {
			return getRuleContext(PrimitiveTypeNameContext.class,0);
		}
		public ReferenceValueContext referenceValue() {
			return getRuleContext(ReferenceValueContext.class,0);
		}
		public TerminalNode OPTIONAL() { return getToken(SSoTParser.OPTIONAL, 0); }
		public TerminalNode LT() { return getToken(SSoTParser.LT, 0); }
		public List<TypeExprContext> typeExpr() {
			return getRuleContexts(TypeExprContext.class);
		}
		public TypeExprContext typeExpr(int i) {
			return getRuleContext(TypeExprContext.class,i);
		}
		public TerminalNode GT() { return getToken(SSoTParser.GT, 0); }
		public TerminalNode LIST() { return getToken(SSoTParser.LIST, 0); }
		public TerminalNode MAP() { return getToken(SSoTParser.MAP, 0); }
		public TerminalNode COMMA() { return getToken(SSoTParser.COMMA, 0); }
		public TypeExprContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_typeExpr; }
	}

	public final TypeExprContext typeExpr() throws RecognitionException {
		TypeExprContext _localctx = new TypeExprContext(_ctx, getState());
		enterRule(_localctx, 42, RULE_typeExpr);
		try {
			setState(437);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case T_U8:
			case T_U16:
			case T_U32:
			case T_U64:
			case T_I8:
			case T_I16:
			case T_I32:
			case T_I64:
			case T_F32:
			case T_F64:
			case T_BOOL:
			case T_STRING:
			case T_TIMESTAMP:
				enterOuterAlt(_localctx, 1);
				{
				setState(418);
				primitiveTypeName();
				}
				break;
			case ID:
				enterOuterAlt(_localctx, 2);
				{
				setState(419);
				referenceValue();
				}
				break;
			case OPTIONAL:
				enterOuterAlt(_localctx, 3);
				{
				setState(420);
				match(OPTIONAL);
				setState(421);
				match(LT);
				setState(422);
				typeExpr();
				setState(423);
				match(GT);
				}
				break;
			case LIST:
				enterOuterAlt(_localctx, 4);
				{
				setState(425);
				match(LIST);
				setState(426);
				match(LT);
				setState(427);
				typeExpr();
				setState(428);
				match(GT);
				}
				break;
			case MAP:
				enterOuterAlt(_localctx, 5);
				{
				setState(430);
				match(MAP);
				setState(431);
				match(LT);
				setState(432);
				typeExpr();
				setState(433);
				match(COMMA);
				setState(434);
				typeExpr();
				setState(435);
				match(GT);
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
	public static class PrimitiveTypeNameContext extends ParserRuleContext {
		public TerminalNode T_U8() { return getToken(SSoTParser.T_U8, 0); }
		public TerminalNode T_U16() { return getToken(SSoTParser.T_U16, 0); }
		public TerminalNode T_U32() { return getToken(SSoTParser.T_U32, 0); }
		public TerminalNode T_U64() { return getToken(SSoTParser.T_U64, 0); }
		public TerminalNode T_I8() { return getToken(SSoTParser.T_I8, 0); }
		public TerminalNode T_I16() { return getToken(SSoTParser.T_I16, 0); }
		public TerminalNode T_I32() { return getToken(SSoTParser.T_I32, 0); }
		public TerminalNode T_I64() { return getToken(SSoTParser.T_I64, 0); }
		public TerminalNode T_F32() { return getToken(SSoTParser.T_F32, 0); }
		public TerminalNode T_F64() { return getToken(SSoTParser.T_F64, 0); }
		public TerminalNode T_BOOL() { return getToken(SSoTParser.T_BOOL, 0); }
		public TerminalNode T_STRING() { return getToken(SSoTParser.T_STRING, 0); }
		public TerminalNode T_TIMESTAMP() { return getToken(SSoTParser.T_TIMESTAMP, 0); }
		public PrimitiveTypeNameContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_primitiveTypeName; }
	}

	public final PrimitiveTypeNameContext primitiveTypeName() throws RecognitionException {
		PrimitiveTypeNameContext _localctx = new PrimitiveTypeNameContext(_ctx, getState());
		enterRule(_localctx, 44, RULE_primitiveTypeName);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(439);
			_la = _input.LA(1);
			if ( !((((_la) & ~0x3f) == 0 && ((1L << _la) & 2305561534236983296L) != 0)) ) {
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
	public static class ActorsBlockContext extends ParserRuleContext {
		public TerminalNode ACTORS() { return getToken(SSoTParser.ACTORS, 0); }
		public TerminalNode LBRACE() { return getToken(SSoTParser.LBRACE, 0); }
		public TerminalNode RBRACE() { return getToken(SSoTParser.RBRACE, 0); }
		public List<AnnotationContext> annotation() {
			return getRuleContexts(AnnotationContext.class);
		}
		public AnnotationContext annotation(int i) {
			return getRuleContext(AnnotationContext.class,i);
		}
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
		enterRule(_localctx, 46, RULE_actorsBlock);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(441);
			match(ACTORS);
			setState(442);
			match(LBRACE);
			setState(446);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==AT || _la==DOLLAR) {
				{
				{
				setState(443);
				annotation();
				}
				}
				setState(448);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(452);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==ACTOR) {
				{
				{
				setState(449);
				actorDefinition();
				}
				}
				setState(454);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(455);
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
		enterRule(_localctx, 48, RULE_actorDefinition);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(457);
			match(ACTOR);
			setState(458);
			match(ID);
			setState(462);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==AT || _la==DOLLAR) {
				{
				{
				setState(459);
				annotation();
				}
				}
				setState(464);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(465);
			match(LBRACE);
			setState(469);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==AT || _la==DOLLAR) {
				{
				{
				setState(466);
				annotation();
				}
				}
				setState(471);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(472);
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
	public static class CommunicationBlockContext extends ParserRuleContext {
		public TerminalNode COMMUNICATION() { return getToken(SSoTParser.COMMUNICATION, 0); }
		public TerminalNode LBRACE() { return getToken(SSoTParser.LBRACE, 0); }
		public TerminalNode RBRACE() { return getToken(SSoTParser.RBRACE, 0); }
		public List<AnnotationContext> annotation() {
			return getRuleContexts(AnnotationContext.class);
		}
		public AnnotationContext annotation(int i) {
			return getRuleContext(AnnotationContext.class,i);
		}
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
		enterRule(_localctx, 50, RULE_communicationBlock);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(474);
			match(COMMUNICATION);
			setState(475);
			match(LBRACE);
			setState(479);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==AT || _la==DOLLAR) {
				{
				{
				setState(476);
				annotation();
				}
				}
				setState(481);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(485);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while ((((_la) & ~0x3f) == 0 && ((1L << _la) & 872415232L) != 0)) {
				{
				{
				setState(482);
				communicationDefinition();
				}
				}
				setState(487);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(488);
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
		enterRule(_localctx, 52, RULE_communicationDefinition);
		try {
			setState(493);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case PROTOCOL:
				enterOuterAlt(_localctx, 1);
				{
				setState(490);
				protocolDefinition();
				}
				break;
			case CHANNEL:
				enterOuterAlt(_localctx, 2);
				{
				setState(491);
				channelDefinition();
				}
				break;
			case EVENT:
				enterOuterAlt(_localctx, 3);
				{
				setState(492);
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
		enterRule(_localctx, 54, RULE_protocolDefinition);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(495);
			match(PROTOCOL);
			setState(496);
			match(ID);
			setState(500);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==AT || _la==DOLLAR) {
				{
				{
				setState(497);
				annotation();
				}
				}
				setState(502);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(503);
			match(LBRACE);
			setState(507);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==AT || _la==DOLLAR) {
				{
				{
				setState(504);
				annotation();
				}
				}
				setState(509);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(510);
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
		public List<ChannelBodyElementContext> channelBodyElement() {
			return getRuleContexts(ChannelBodyElementContext.class);
		}
		public ChannelBodyElementContext channelBodyElement(int i) {
			return getRuleContext(ChannelBodyElementContext.class,i);
		}
		public ChannelDefinitionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_channelDefinition; }
	}

	public final ChannelDefinitionContext channelDefinition() throws RecognitionException {
		ChannelDefinitionContext _localctx = new ChannelDefinitionContext(_ctx, getState());
		enterRule(_localctx, 56, RULE_channelDefinition);
		int _la;
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(512);
			match(CHANNEL);
			setState(513);
			match(ID);
			setState(517);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==AT || _la==DOLLAR) {
				{
				{
				setState(514);
				annotation();
				}
				}
				setState(519);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(520);
			match(LBRACE);
			setState(524);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,41,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					{
					{
					setState(521);
					annotation();
					}
					} 
				}
				setState(526);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,41,_ctx);
			}
			setState(530);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (((((_la - 37)) & ~0x3f) == 0 && ((1L << (_la - 37)) & 51539607553L) != 0)) {
				{
				{
				setState(527);
				channelBodyElement();
				}
				}
				setState(532);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(533);
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
	public static class ChannelBodyElementContext extends ParserRuleContext {
		public AnnotationContext annotation() {
			return getRuleContext(AnnotationContext.class,0);
		}
		public ChannelParameterDefinitionContext channelParameterDefinition() {
			return getRuleContext(ChannelParameterDefinitionContext.class,0);
		}
		public ChannelBodyElementContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_channelBodyElement; }
	}

	public final ChannelBodyElementContext channelBodyElement() throws RecognitionException {
		ChannelBodyElementContext _localctx = new ChannelBodyElementContext(_ctx, getState());
		enterRule(_localctx, 58, RULE_channelBodyElement);
		try {
			setState(537);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case AT:
			case DOLLAR:
				enterOuterAlt(_localctx, 1);
				{
				setState(535);
				annotation();
				}
				break;
			case PARAMETERS:
				enterOuterAlt(_localctx, 2);
				{
				setState(536);
				channelParameterDefinition();
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
	public static class ChannelParameterDefinitionContext extends ParserRuleContext {
		public TerminalNode PARAMETERS() { return getToken(SSoTParser.PARAMETERS, 0); }
		public TerminalNode LBRACE() { return getToken(SSoTParser.LBRACE, 0); }
		public TerminalNode RBRACE() { return getToken(SSoTParser.RBRACE, 0); }
		public AttributePairListContext attributePairList() {
			return getRuleContext(AttributePairListContext.class,0);
		}
		public ChannelParameterDefinitionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_channelParameterDefinition; }
	}

	public final ChannelParameterDefinitionContext channelParameterDefinition() throws RecognitionException {
		ChannelParameterDefinitionContext _localctx = new ChannelParameterDefinitionContext(_ctx, getState());
		enterRule(_localctx, 60, RULE_channelParameterDefinition);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(539);
			match(PARAMETERS);
			setState(540);
			match(LBRACE);
			setState(542);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==ID) {
				{
				setState(541);
				attributePairList();
				}
			}

			setState(544);
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
		public List<FieldDefinitionContext> fieldDefinition() {
			return getRuleContexts(FieldDefinitionContext.class);
		}
		public FieldDefinitionContext fieldDefinition(int i) {
			return getRuleContext(FieldDefinitionContext.class,i);
		}
		public EventDefinitionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_eventDefinition; }
	}

	public final EventDefinitionContext eventDefinition() throws RecognitionException {
		EventDefinitionContext _localctx = new EventDefinitionContext(_ctx, getState());
		enterRule(_localctx, 62, RULE_eventDefinition);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(546);
			match(EVENT);
			setState(547);
			match(ID);
			setState(551);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==AT || _la==DOLLAR) {
				{
				{
				setState(548);
				annotation();
				}
				}
				setState(553);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(554);
			match(LBRACE);
			setState(558);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==AT || _la==DOLLAR) {
				{
				{
				setState(555);
				annotation();
				}
				}
				setState(560);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(564);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==ID) {
				{
				{
				setState(561);
				fieldDefinition();
				}
				}
				setState(566);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(567);
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
	public static class ServicesBlockContext extends ParserRuleContext {
		public TerminalNode SERVICES() { return getToken(SSoTParser.SERVICES, 0); }
		public TerminalNode LBRACE() { return getToken(SSoTParser.LBRACE, 0); }
		public TerminalNode RBRACE() { return getToken(SSoTParser.RBRACE, 0); }
		public List<AnnotationContext> annotation() {
			return getRuleContexts(AnnotationContext.class);
		}
		public AnnotationContext annotation(int i) {
			return getRuleContext(AnnotationContext.class,i);
		}
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
		enterRule(_localctx, 64, RULE_servicesBlock);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(569);
			match(SERVICES);
			setState(570);
			match(LBRACE);
			setState(574);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==AT || _la==DOLLAR) {
				{
				{
				setState(571);
				annotation();
				}
				}
				setState(576);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(580);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==SERVICE || _la==INTERFACE) {
				{
				{
				setState(577);
				serviceElement();
				}
				}
				setState(582);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(583);
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
		enterRule(_localctx, 66, RULE_serviceElement);
		try {
			setState(587);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case INTERFACE:
				enterOuterAlt(_localctx, 1);
				{
				setState(585);
				interfaceDefinition();
				}
				break;
			case SERVICE:
				enterOuterAlt(_localctx, 2);
				{
				setState(586);
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
		enterRule(_localctx, 68, RULE_interfaceDefinition);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(589);
			match(INTERFACE);
			setState(590);
			match(ID);
			setState(594);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==AT || _la==DOLLAR) {
				{
				{
				setState(591);
				annotation();
				}
				}
				setState(596);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(597);
			match(LBRACE);
			setState(601);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==AT || _la==DOLLAR) {
				{
				{
				setState(598);
				annotation();
				}
				}
				setState(603);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(607);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==ID) {
				{
				{
				setState(604);
				methodDefinition();
				}
				}
				setState(609);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(610);
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
		public ParameterListContext parameterList() {
			return getRuleContext(ParameterListContext.class,0);
		}
		public TerminalNode ARROW() { return getToken(SSoTParser.ARROW, 0); }
		public TypeExprContext typeExpr() {
			return getRuleContext(TypeExprContext.class,0);
		}
		public TerminalNode LBRACE() { return getToken(SSoTParser.LBRACE, 0); }
		public TerminalNode RBRACE() { return getToken(SSoTParser.RBRACE, 0); }
		public MethodDefinitionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_methodDefinition; }
	}

	public final MethodDefinitionContext methodDefinition() throws RecognitionException {
		MethodDefinitionContext _localctx = new MethodDefinitionContext(_ctx, getState());
		enterRule(_localctx, 70, RULE_methodDefinition);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(612);
			match(ID);
			setState(616);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==AT || _la==DOLLAR) {
				{
				{
				setState(613);
				annotation();
				}
				}
				setState(618);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(619);
			match(LPAREN);
			setState(621);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==ID) {
				{
				setState(620);
				parameterList();
				}
			}

			setState(623);
			match(RPAREN);
			setState(626);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==ARROW) {
				{
				setState(624);
				match(ARROW);
				setState(625);
				typeExpr();
				}
			}

			setState(636);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==LBRACE) {
				{
				setState(628);
				match(LBRACE);
				setState(632);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while (_la==AT || _la==DOLLAR) {
					{
					{
					setState(629);
					annotation();
					}
					}
					setState(634);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				setState(635);
				match(RBRACE);
				}
			}

			setState(638);
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
	public static class ParameterListContext extends ParserRuleContext {
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
		public ParameterListContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_parameterList; }
	}

	public final ParameterListContext parameterList() throws RecognitionException {
		ParameterListContext _localctx = new ParameterListContext(_ctx, getState());
		enterRule(_localctx, 72, RULE_parameterList);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(640);
			parameter();
			setState(645);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==COMMA) {
				{
				{
				setState(641);
				match(COMMA);
				setState(642);
				parameter();
				}
				}
				setState(647);
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
		public TypeExprContext typeExpr() {
			return getRuleContext(TypeExprContext.class,0);
		}
		public ParameterContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_parameter; }
	}

	public final ParameterContext parameter() throws RecognitionException {
		ParameterContext _localctx = new ParameterContext(_ctx, getState());
		enterRule(_localctx, 74, RULE_parameter);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(648);
			match(ID);
			setState(649);
			match(COLON);
			setState(650);
			typeExpr();
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
		public TerminalNode EXTENDS() { return getToken(SSoTParser.EXTENDS, 0); }
		public ReferenceValueContext referenceValue() {
			return getRuleContext(ReferenceValueContext.class,0);
		}
		public ServiceDefinitionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_serviceDefinition; }
	}

	public final ServiceDefinitionContext serviceDefinition() throws RecognitionException {
		ServiceDefinitionContext _localctx = new ServiceDefinitionContext(_ctx, getState());
		enterRule(_localctx, 76, RULE_serviceDefinition);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(652);
			match(SERVICE);
			setState(653);
			match(ID);
			setState(657);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==AT || _la==DOLLAR) {
				{
				{
				setState(654);
				annotation();
				}
				}
				setState(659);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(662);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==EXTENDS) {
				{
				setState(660);
				match(EXTENDS);
				setState(661);
				referenceValue();
				}
			}

			setState(664);
			match(LBRACE);
			setState(668);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==AT || _la==DOLLAR) {
				{
				{
				setState(665);
				annotation();
				}
				}
				setState(670);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(671);
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
	public static class MachinesBlockContext extends ParserRuleContext {
		public TerminalNode MACHINES() { return getToken(SSoTParser.MACHINES, 0); }
		public TerminalNode LBRACE() { return getToken(SSoTParser.LBRACE, 0); }
		public TerminalNode RBRACE() { return getToken(SSoTParser.RBRACE, 0); }
		public List<AnnotationContext> annotation() {
			return getRuleContexts(AnnotationContext.class);
		}
		public AnnotationContext annotation(int i) {
			return getRuleContext(AnnotationContext.class,i);
		}
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
		enterRule(_localctx, 78, RULE_machinesBlock);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(673);
			match(MACHINES);
			setState(674);
			match(LBRACE);
			setState(678);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==AT || _la==DOLLAR) {
				{
				{
				setState(675);
				annotation();
				}
				}
				setState(680);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(684);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==MACHINE) {
				{
				{
				setState(681);
				machineDefinition();
				}
				}
				setState(686);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(687);
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
		public List<AnnotationContext> annotation() {
			return getRuleContexts(AnnotationContext.class);
		}
		public AnnotationContext annotation(int i) {
			return getRuleContext(AnnotationContext.class,i);
		}
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
		enterRule(_localctx, 80, RULE_machineDefinition);
		int _la;
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(689);
			match(MACHINE);
			setState(690);
			match(ID);
			setState(694);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==AT || _la==DOLLAR) {
				{
				{
				setState(691);
				annotation();
				}
				}
				setState(696);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(697);
			match(LBRACE);
			setState(701);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,66,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					{
					{
					setState(698);
					annotation();
					}
					} 
				}
				setState(703);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,66,_ctx);
			}
			setState(707);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (((((_la - 12)) & ~0x3f) == 0 && ((1L << (_la - 12)) & 1729382256910270495L) != 0)) {
				{
				{
				setState(704);
				machineBodyElement();
				}
				}
				setState(709);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(710);
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
		public ContextDefinitionContext contextDefinition() {
			return getRuleContext(ContextDefinitionContext.class,0);
		}
		public ActionsDefinitionContext actionsDefinition() {
			return getRuleContext(ActionsDefinitionContext.class,0);
		}
		public GuardsDefinitionContext guardsDefinition() {
			return getRuleContext(GuardsDefinitionContext.class,0);
		}
		public InvokesDefinitionContext invokesDefinition() {
			return getRuleContext(InvokesDefinitionContext.class,0);
		}
		public StatesDefinitionContext statesDefinition() {
			return getRuleContext(StatesDefinitionContext.class,0);
		}
		public AnnotationContext annotation() {
			return getRuleContext(AnnotationContext.class,0);
		}
		public MachineBodyElementContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_machineBodyElement; }
	}

	public final MachineBodyElementContext machineBodyElement() throws RecognitionException {
		MachineBodyElementContext _localctx = new MachineBodyElementContext(_ctx, getState());
		enterRule(_localctx, 82, RULE_machineBodyElement);
		try {
			setState(718);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case CONTEXT:
				enterOuterAlt(_localctx, 1);
				{
				setState(712);
				contextDefinition();
				}
				break;
			case ACTIONS:
				enterOuterAlt(_localctx, 2);
				{
				setState(713);
				actionsDefinition();
				}
				break;
			case GUARDS:
				enterOuterAlt(_localctx, 3);
				{
				setState(714);
				guardsDefinition();
				}
				break;
			case INVOKES:
				enterOuterAlt(_localctx, 4);
				{
				setState(715);
				invokesDefinition();
				}
				break;
			case STATES:
				enterOuterAlt(_localctx, 5);
				{
				setState(716);
				statesDefinition();
				}
				break;
			case AT:
			case DOLLAR:
				enterOuterAlt(_localctx, 6);
				{
				setState(717);
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
	public static class ContextDefinitionContext extends ParserRuleContext {
		public TerminalNode CONTEXT() { return getToken(SSoTParser.CONTEXT, 0); }
		public TerminalNode LBRACE() { return getToken(SSoTParser.LBRACE, 0); }
		public TerminalNode RBRACE() { return getToken(SSoTParser.RBRACE, 0); }
		public List<AnnotationContext> annotation() {
			return getRuleContexts(AnnotationContext.class);
		}
		public AnnotationContext annotation(int i) {
			return getRuleContext(AnnotationContext.class,i);
		}
		public List<ContextFieldContext> contextField() {
			return getRuleContexts(ContextFieldContext.class);
		}
		public ContextFieldContext contextField(int i) {
			return getRuleContext(ContextFieldContext.class,i);
		}
		public ContextDefinitionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_contextDefinition; }
	}

	public final ContextDefinitionContext contextDefinition() throws RecognitionException {
		ContextDefinitionContext _localctx = new ContextDefinitionContext(_ctx, getState());
		enterRule(_localctx, 84, RULE_contextDefinition);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(720);
			match(CONTEXT);
			setState(724);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==AT || _la==DOLLAR) {
				{
				{
				setState(721);
				annotation();
				}
				}
				setState(726);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(727);
			match(LBRACE);
			setState(731);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==AT || _la==DOLLAR) {
				{
				{
				setState(728);
				annotation();
				}
				}
				setState(733);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(737);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==ID) {
				{
				{
				setState(734);
				contextField();
				}
				}
				setState(739);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(740);
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
	public static class ContextFieldContext extends ParserRuleContext {
		public TerminalNode ID() { return getToken(SSoTParser.ID, 0); }
		public TerminalNode COLON() { return getToken(SSoTParser.COLON, 0); }
		public TypeExprContext typeExpr() {
			return getRuleContext(TypeExprContext.class,0);
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
		public ContextFieldContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_contextField; }
	}

	public final ContextFieldContext contextField() throws RecognitionException {
		ContextFieldContext _localctx = new ContextFieldContext(_ctx, getState());
		enterRule(_localctx, 86, RULE_contextField);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(742);
			match(ID);
			setState(743);
			match(COLON);
			setState(744);
			typeExpr();
			setState(748);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==AT || _la==DOLLAR) {
				{
				{
				setState(745);
				annotation();
				}
				}
				setState(750);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(759);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==LBRACE) {
				{
				setState(751);
				match(LBRACE);
				setState(755);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while (_la==AT || _la==DOLLAR) {
					{
					{
					setState(752);
					annotation();
					}
					}
					setState(757);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				setState(758);
				match(RBRACE);
				}
			}

			setState(761);
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
	public static class ActionsDefinitionContext extends ParserRuleContext {
		public TerminalNode ACTIONS() { return getToken(SSoTParser.ACTIONS, 0); }
		public TerminalNode LBRACE() { return getToken(SSoTParser.LBRACE, 0); }
		public TerminalNode RBRACE() { return getToken(SSoTParser.RBRACE, 0); }
		public List<AnnotationContext> annotation() {
			return getRuleContexts(AnnotationContext.class);
		}
		public AnnotationContext annotation(int i) {
			return getRuleContext(AnnotationContext.class,i);
		}
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
		enterRule(_localctx, 88, RULE_actionsDefinition);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(763);
			match(ACTIONS);
			setState(767);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==AT || _la==DOLLAR) {
				{
				{
				setState(764);
				annotation();
				}
				}
				setState(769);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(770);
			match(LBRACE);
			setState(774);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==AT || _la==DOLLAR) {
				{
				{
				setState(771);
				annotation();
				}
				}
				setState(776);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(780);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==ID) {
				{
				{
				setState(777);
				actionDefinition();
				}
				}
				setState(782);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(783);
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
		public List<TerminalNode> ID() { return getTokens(SSoTParser.ID); }
		public TerminalNode ID(int i) {
			return getToken(SSoTParser.ID, i);
		}
		public TerminalNode SEMI() { return getToken(SSoTParser.SEMI, 0); }
		public List<AnnotationContext> annotation() {
			return getRuleContexts(AnnotationContext.class);
		}
		public AnnotationContext annotation(int i) {
			return getRuleContext(AnnotationContext.class,i);
		}
		public TerminalNode LPAREN() { return getToken(SSoTParser.LPAREN, 0); }
		public TerminalNode RPAREN() { return getToken(SSoTParser.RPAREN, 0); }
		public TerminalNode COLON() { return getToken(SSoTParser.COLON, 0); }
		public TypeExprContext typeExpr() {
			return getRuleContext(TypeExprContext.class,0);
		}
		public TerminalNode COMMA() { return getToken(SSoTParser.COMMA, 0); }
		public ActionDefinitionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_actionDefinition; }
	}

	public final ActionDefinitionContext actionDefinition() throws RecognitionException {
		ActionDefinitionContext _localctx = new ActionDefinitionContext(_ctx, getState());
		enterRule(_localctx, 90, RULE_actionDefinition);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(785);
			match(ID);
			setState(789);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==AT || _la==DOLLAR) {
				{
				{
				setState(786);
				annotation();
				}
				}
				setState(791);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(801);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==LPAREN) {
				{
				setState(792);
				match(LPAREN);
				setState(794);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==ID) {
					{
					setState(793);
					match(ID);
					}
				}

				setState(798);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==COMMA) {
					{
					setState(796);
					match(COMMA);
					setState(797);
					match(ID);
					}
				}

				setState(800);
				match(RPAREN);
				}
			}

			setState(805);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==COLON) {
				{
				setState(803);
				match(COLON);
				setState(804);
				typeExpr();
				}
			}

			setState(807);
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
		public List<AnnotationContext> annotation() {
			return getRuleContexts(AnnotationContext.class);
		}
		public AnnotationContext annotation(int i) {
			return getRuleContext(AnnotationContext.class,i);
		}
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
		enterRule(_localctx, 92, RULE_guardsDefinition);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(809);
			match(GUARDS);
			setState(813);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==AT || _la==DOLLAR) {
				{
				{
				setState(810);
				annotation();
				}
				}
				setState(815);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(816);
			match(LBRACE);
			setState(820);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==AT || _la==DOLLAR) {
				{
				{
				setState(817);
				annotation();
				}
				}
				setState(822);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(826);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==ID) {
				{
				{
				setState(823);
				guardDefinition();
				}
				}
				setState(828);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(829);
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
		public List<TerminalNode> ID() { return getTokens(SSoTParser.ID); }
		public TerminalNode ID(int i) {
			return getToken(SSoTParser.ID, i);
		}
		public TerminalNode ARROW() { return getToken(SSoTParser.ARROW, 0); }
		public TerminalNode T_BOOL() { return getToken(SSoTParser.T_BOOL, 0); }
		public TerminalNode SEMI() { return getToken(SSoTParser.SEMI, 0); }
		public List<AnnotationContext> annotation() {
			return getRuleContexts(AnnotationContext.class);
		}
		public AnnotationContext annotation(int i) {
			return getRuleContext(AnnotationContext.class,i);
		}
		public TerminalNode LPAREN() { return getToken(SSoTParser.LPAREN, 0); }
		public TerminalNode RPAREN() { return getToken(SSoTParser.RPAREN, 0); }
		public GuardDefinitionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_guardDefinition; }
	}

	public final GuardDefinitionContext guardDefinition() throws RecognitionException {
		GuardDefinitionContext _localctx = new GuardDefinitionContext(_ctx, getState());
		enterRule(_localctx, 94, RULE_guardDefinition);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(831);
			match(ID);
			setState(835);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==AT || _la==DOLLAR) {
				{
				{
				setState(832);
				annotation();
				}
				}
				setState(837);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(843);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==LPAREN) {
				{
				setState(838);
				match(LPAREN);
				setState(840);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==ID) {
					{
					setState(839);
					match(ID);
					}
				}

				setState(842);
				match(RPAREN);
				}
			}

			setState(845);
			match(ARROW);
			setState(846);
			match(T_BOOL);
			setState(847);
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
	public static class InvokesDefinitionContext extends ParserRuleContext {
		public TerminalNode INVOKES() { return getToken(SSoTParser.INVOKES, 0); }
		public TerminalNode LBRACE() { return getToken(SSoTParser.LBRACE, 0); }
		public TerminalNode RBRACE() { return getToken(SSoTParser.RBRACE, 0); }
		public List<AnnotationContext> annotation() {
			return getRuleContexts(AnnotationContext.class);
		}
		public AnnotationContext annotation(int i) {
			return getRuleContext(AnnotationContext.class,i);
		}
		public List<InvokeDefinitionContext> invokeDefinition() {
			return getRuleContexts(InvokeDefinitionContext.class);
		}
		public InvokeDefinitionContext invokeDefinition(int i) {
			return getRuleContext(InvokeDefinitionContext.class,i);
		}
		public InvokesDefinitionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_invokesDefinition; }
	}

	public final InvokesDefinitionContext invokesDefinition() throws RecognitionException {
		InvokesDefinitionContext _localctx = new InvokesDefinitionContext(_ctx, getState());
		enterRule(_localctx, 96, RULE_invokesDefinition);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(849);
			match(INVOKES);
			setState(853);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==AT || _la==DOLLAR) {
				{
				{
				setState(850);
				annotation();
				}
				}
				setState(855);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(856);
			match(LBRACE);
			setState(860);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==AT || _la==DOLLAR) {
				{
				{
				setState(857);
				annotation();
				}
				}
				setState(862);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(866);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==ID) {
				{
				{
				setState(863);
				invokeDefinition();
				}
				}
				setState(868);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(869);
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
	public static class InvokeDefinitionContext extends ParserRuleContext {
		public TerminalNode ID() { return getToken(SSoTParser.ID, 0); }
		public TerminalNode LBRACE() { return getToken(SSoTParser.LBRACE, 0); }
		public TerminalNode RBRACE() { return getToken(SSoTParser.RBRACE, 0); }
		public List<AnnotationContext> annotation() {
			return getRuleContexts(AnnotationContext.class);
		}
		public AnnotationContext annotation(int i) {
			return getRuleContext(AnnotationContext.class,i);
		}
		public InvokeDefinitionBodyContext invokeDefinitionBody() {
			return getRuleContext(InvokeDefinitionBodyContext.class,0);
		}
		public InvokeDefinitionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_invokeDefinition; }
	}

	public final InvokeDefinitionContext invokeDefinition() throws RecognitionException {
		InvokeDefinitionContext _localctx = new InvokeDefinitionContext(_ctx, getState());
		enterRule(_localctx, 98, RULE_invokeDefinition);
		int _la;
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(871);
			match(ID);
			setState(875);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==AT || _la==DOLLAR) {
				{
				{
				setState(872);
				annotation();
				}
				}
				setState(877);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(878);
			match(LBRACE);
			setState(882);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,93,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					{
					{
					setState(879);
					annotation();
					}
					} 
				}
				setState(884);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,93,_ctx);
			}
			setState(886);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (((((_la - 45)) & ~0x3f) == 0 && ((1L << (_la - 45)) & 8444249502646273L) != 0)) {
				{
				setState(885);
				invokeDefinitionBody();
				}
			}

			setState(888);
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
	public static class InvokeDefinitionBodyContext extends ParserRuleContext {
		public List<InvokeAttributeContext> invokeAttribute() {
			return getRuleContexts(InvokeAttributeContext.class);
		}
		public InvokeAttributeContext invokeAttribute(int i) {
			return getRuleContext(InvokeAttributeContext.class,i);
		}
		public List<TerminalNode> SEMI() { return getTokens(SSoTParser.SEMI); }
		public TerminalNode SEMI(int i) {
			return getToken(SSoTParser.SEMI, i);
		}
		public InvokeDefinitionBodyContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_invokeDefinitionBody; }
	}

	public final InvokeDefinitionBodyContext invokeDefinitionBody() throws RecognitionException {
		InvokeDefinitionBodyContext _localctx = new InvokeDefinitionBodyContext(_ctx, getState());
		enterRule(_localctx, 100, RULE_invokeDefinitionBody);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(890);
			invokeAttribute();
			setState(897);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (((((_la - 45)) & ~0x3f) == 0 && ((1L << (_la - 45)) & 8444318222123009L) != 0)) {
				{
				{
				setState(892);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==SEMI) {
					{
					setState(891);
					match(SEMI);
					}
				}

				setState(894);
				invokeAttribute();
				}
				}
				setState(899);
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
	public static class InvokeAttributeContext extends ParserRuleContext {
		public InvokeSrcContext invokeSrc() {
			return getRuleContext(InvokeSrcContext.class,0);
		}
		public InvokeInputMappingContext invokeInputMapping() {
			return getRuleContext(InvokeInputMappingContext.class,0);
		}
		public InvokeOutputMappingContext invokeOutputMapping() {
			return getRuleContext(InvokeOutputMappingContext.class,0);
		}
		public InvokeOnDoneContext invokeOnDone() {
			return getRuleContext(InvokeOnDoneContext.class,0);
		}
		public InvokeOnErrorContext invokeOnError() {
			return getRuleContext(InvokeOnErrorContext.class,0);
		}
		public AnnotationContext annotation() {
			return getRuleContext(AnnotationContext.class,0);
		}
		public InvokeAttributeContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_invokeAttribute; }
	}

	public final InvokeAttributeContext invokeAttribute() throws RecognitionException {
		InvokeAttributeContext _localctx = new InvokeAttributeContext(_ctx, getState());
		enterRule(_localctx, 102, RULE_invokeAttribute);
		try {
			setState(906);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case SRC:
				enterOuterAlt(_localctx, 1);
				{
				setState(900);
				invokeSrc();
				}
				break;
			case INPUT:
				enterOuterAlt(_localctx, 2);
				{
				setState(901);
				invokeInputMapping();
				}
				break;
			case OUTPUT:
				enterOuterAlt(_localctx, 3);
				{
				setState(902);
				invokeOutputMapping();
				}
				break;
			case ONDONE:
				enterOuterAlt(_localctx, 4);
				{
				setState(903);
				invokeOnDone();
				}
				break;
			case ONERROR:
				enterOuterAlt(_localctx, 5);
				{
				setState(904);
				invokeOnError();
				}
				break;
			case AT:
			case DOLLAR:
				enterOuterAlt(_localctx, 6);
				{
				setState(905);
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
	public static class InvokeSrcContext extends ParserRuleContext {
		public TerminalNode SRC() { return getToken(SSoTParser.SRC, 0); }
		public TerminalNode COLON() { return getToken(SSoTParser.COLON, 0); }
		public InvokeSourceContext invokeSource() {
			return getRuleContext(InvokeSourceContext.class,0);
		}
		public InvokeSrcContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_invokeSrc; }
	}

	public final InvokeSrcContext invokeSrc() throws RecognitionException {
		InvokeSrcContext _localctx = new InvokeSrcContext(_ctx, getState());
		enterRule(_localctx, 104, RULE_invokeSrc);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(908);
			match(SRC);
			setState(909);
			match(COLON);
			setState(910);
			invokeSource();
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
	public static class InvokeInputMappingContext extends ParserRuleContext {
		public TerminalNode INPUT() { return getToken(SSoTParser.INPUT, 0); }
		public TerminalNode COLON() { return getToken(SSoTParser.COLON, 0); }
		public TerminalNode LBRACE() { return getToken(SSoTParser.LBRACE, 0); }
		public TerminalNode RBRACE() { return getToken(SSoTParser.RBRACE, 0); }
		public KeyValuePairListContext keyValuePairList() {
			return getRuleContext(KeyValuePairListContext.class,0);
		}
		public InvokeInputMappingContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_invokeInputMapping; }
	}

	public final InvokeInputMappingContext invokeInputMapping() throws RecognitionException {
		InvokeInputMappingContext _localctx = new InvokeInputMappingContext(_ctx, getState());
		enterRule(_localctx, 106, RULE_invokeInputMapping);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(912);
			match(INPUT);
			setState(913);
			match(COLON);
			setState(914);
			match(LBRACE);
			setState(916);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==STRING) {
				{
				setState(915);
				keyValuePairList();
				}
			}

			setState(918);
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
	public static class InvokeOutputMappingContext extends ParserRuleContext {
		public TerminalNode OUTPUT() { return getToken(SSoTParser.OUTPUT, 0); }
		public TerminalNode COLON() { return getToken(SSoTParser.COLON, 0); }
		public TerminalNode LBRACE() { return getToken(SSoTParser.LBRACE, 0); }
		public TerminalNode RBRACE() { return getToken(SSoTParser.RBRACE, 0); }
		public KeyValuePairListContext keyValuePairList() {
			return getRuleContext(KeyValuePairListContext.class,0);
		}
		public InvokeOutputMappingContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_invokeOutputMapping; }
	}

	public final InvokeOutputMappingContext invokeOutputMapping() throws RecognitionException {
		InvokeOutputMappingContext _localctx = new InvokeOutputMappingContext(_ctx, getState());
		enterRule(_localctx, 108, RULE_invokeOutputMapping);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(920);
			match(OUTPUT);
			setState(921);
			match(COLON);
			setState(922);
			match(LBRACE);
			setState(924);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==STRING) {
				{
				setState(923);
				keyValuePairList();
				}
			}

			setState(926);
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
	public static class InvokeOnDoneContext extends ParserRuleContext {
		public TerminalNode ONDONE() { return getToken(SSoTParser.ONDONE, 0); }
		public TerminalNode COLON() { return getToken(SSoTParser.COLON, 0); }
		public InvokeCompletionContext invokeCompletion() {
			return getRuleContext(InvokeCompletionContext.class,0);
		}
		public InvokeOnDoneContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_invokeOnDone; }
	}

	public final InvokeOnDoneContext invokeOnDone() throws RecognitionException {
		InvokeOnDoneContext _localctx = new InvokeOnDoneContext(_ctx, getState());
		enterRule(_localctx, 110, RULE_invokeOnDone);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(928);
			match(ONDONE);
			setState(929);
			match(COLON);
			setState(930);
			invokeCompletion();
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
	public static class InvokeOnErrorContext extends ParserRuleContext {
		public TerminalNode ONERROR() { return getToken(SSoTParser.ONERROR, 0); }
		public TerminalNode COLON() { return getToken(SSoTParser.COLON, 0); }
		public InvokeCompletionContext invokeCompletion() {
			return getRuleContext(InvokeCompletionContext.class,0);
		}
		public InvokeOnErrorContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_invokeOnError; }
	}

	public final InvokeOnErrorContext invokeOnError() throws RecognitionException {
		InvokeOnErrorContext _localctx = new InvokeOnErrorContext(_ctx, getState());
		enterRule(_localctx, 112, RULE_invokeOnError);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(932);
			match(ONERROR);
			setState(933);
			match(COLON);
			setState(934);
			invokeCompletion();
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
	public static class InvokeCompletionContext extends ParserRuleContext {
		public ActionReferenceListContext actionReferenceList() {
			return getRuleContext(ActionReferenceListContext.class,0);
		}
		public TransitionSpecContext transitionSpec() {
			return getRuleContext(TransitionSpecContext.class,0);
		}
		public List<AnnotationContext> annotation() {
			return getRuleContexts(AnnotationContext.class);
		}
		public AnnotationContext annotation(int i) {
			return getRuleContext(AnnotationContext.class,i);
		}
		public InvokeCompletionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_invokeCompletion; }
	}

	public final InvokeCompletionContext invokeCompletion() throws RecognitionException {
		InvokeCompletionContext _localctx = new InvokeCompletionContext(_ctx, getState());
		enterRule(_localctx, 114, RULE_invokeCompletion);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(939);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==AT || _la==DOLLAR) {
				{
				{
				setState(936);
				annotation();
				}
				}
				setState(941);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(944);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case ID:
			case LBRACK:
				{
				setState(942);
				actionReferenceList();
				}
				break;
			case TRANSITION:
				{
				setState(943);
				transitionSpec();
				}
				break;
			default:
				throw new NoViableAltException(this);
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
	public static class InvokeSourceContext extends ParserRuleContext {
		public TerminalNode STRING() { return getToken(SSoTParser.STRING, 0); }
		public ExpressionValueContext expressionValue() {
			return getRuleContext(ExpressionValueContext.class,0);
		}
		public InvokeSourceContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_invokeSource; }
	}

	public final InvokeSourceContext invokeSource() throws RecognitionException {
		InvokeSourceContext _localctx = new InvokeSourceContext(_ctx, getState());
		enterRule(_localctx, 116, RULE_invokeSource);
		try {
			setState(948);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,102,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(946);
				match(STRING);
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(947);
				expressionValue();
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
	public static class ExpressionValueContext extends ParserRuleContext {
		public ValueContext value() {
			return getRuleContext(ValueContext.class,0);
		}
		public ExpressionValueContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_expressionValue; }
	}

	public final ExpressionValueContext expressionValue() throws RecognitionException {
		ExpressionValueContext _localctx = new ExpressionValueContext(_ctx, getState());
		enterRule(_localctx, 118, RULE_expressionValue);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(950);
			value();
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
	public static class KeyValuePairListContext extends ParserRuleContext {
		public List<KeyValuePairContext> keyValuePair() {
			return getRuleContexts(KeyValuePairContext.class);
		}
		public KeyValuePairContext keyValuePair(int i) {
			return getRuleContext(KeyValuePairContext.class,i);
		}
		public List<TerminalNode> COMMA() { return getTokens(SSoTParser.COMMA); }
		public TerminalNode COMMA(int i) {
			return getToken(SSoTParser.COMMA, i);
		}
		public KeyValuePairListContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_keyValuePairList; }
	}

	public final KeyValuePairListContext keyValuePairList() throws RecognitionException {
		KeyValuePairListContext _localctx = new KeyValuePairListContext(_ctx, getState());
		enterRule(_localctx, 120, RULE_keyValuePairList);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(952);
			keyValuePair();
			setState(957);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==COMMA) {
				{
				{
				setState(953);
				match(COMMA);
				setState(954);
				keyValuePair();
				}
				}
				setState(959);
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
	public static class KeyValuePairContext extends ParserRuleContext {
		public TerminalNode STRING() { return getToken(SSoTParser.STRING, 0); }
		public TerminalNode COLON() { return getToken(SSoTParser.COLON, 0); }
		public ValueContext value() {
			return getRuleContext(ValueContext.class,0);
		}
		public KeyValuePairContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_keyValuePair; }
	}

	public final KeyValuePairContext keyValuePair() throws RecognitionException {
		KeyValuePairContext _localctx = new KeyValuePairContext(_ctx, getState());
		enterRule(_localctx, 122, RULE_keyValuePair);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(960);
			match(STRING);
			setState(961);
			match(COLON);
			setState(962);
			value();
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
		public List<AnnotationContext> annotation() {
			return getRuleContexts(AnnotationContext.class);
		}
		public AnnotationContext annotation(int i) {
			return getRuleContext(AnnotationContext.class,i);
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
		enterRule(_localctx, 124, RULE_statesDefinition);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(964);
			match(STATES);
			setState(968);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==AT || _la==DOLLAR) {
				{
				{
				setState(965);
				annotation();
				}
				}
				setState(970);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(971);
			match(LBRACE);
			setState(975);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==AT || _la==DOLLAR) {
				{
				{
				setState(972);
				annotation();
				}
				}
				setState(977);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(981);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==HISTORY || _la==ID) {
				{
				{
				setState(978);
				stateDefinitionOrHistoryState();
				}
				}
				setState(983);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(984);
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
	public static class StateDefinitionOrHistoryStateContext extends ParserRuleContext {
		public StateDefinitionContext stateDefinition() {
			return getRuleContext(StateDefinitionContext.class,0);
		}
		public HistoryDefinitionContext historyDefinition() {
			return getRuleContext(HistoryDefinitionContext.class,0);
		}
		public StateDefinitionOrHistoryStateContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_stateDefinitionOrHistoryState; }
	}

	public final StateDefinitionOrHistoryStateContext stateDefinitionOrHistoryState() throws RecognitionException {
		StateDefinitionOrHistoryStateContext _localctx = new StateDefinitionOrHistoryStateContext(_ctx, getState());
		enterRule(_localctx, 126, RULE_stateDefinitionOrHistoryState);
		try {
			setState(988);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case ID:
				enterOuterAlt(_localctx, 1);
				{
				setState(986);
				stateDefinition();
				}
				break;
			case HISTORY:
				enterOuterAlt(_localctx, 2);
				{
				setState(987);
				historyDefinition();
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
	public static class StateDefinitionContext extends ParserRuleContext {
		public Token stateName;
		public TerminalNode LBRACE() { return getToken(SSoTParser.LBRACE, 0); }
		public TerminalNode RBRACE() { return getToken(SSoTParser.RBRACE, 0); }
		public TerminalNode ID() { return getToken(SSoTParser.ID, 0); }
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
		public StateDefinitionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_stateDefinition; }
	}

	public final StateDefinitionContext stateDefinition() throws RecognitionException {
		StateDefinitionContext _localctx = new StateDefinitionContext(_ctx, getState());
		enterRule(_localctx, 128, RULE_stateDefinition);
		int _la;
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(990);
			((StateDefinitionContext)_localctx).stateName = match(ID);
			setState(994);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==AT || _la==DOLLAR) {
				{
				{
				setState(991);
				annotation();
				}
				}
				setState(996);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(997);
			match(LBRACE);
			setState(1001);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,109,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					{
					{
					setState(998);
					annotation();
					}
					} 
				}
				setState(1003);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,109,_ctx);
			}
			setState(1007);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while ((((_la) & ~0x3f) == 0 && ((1L << _la) & 30786327085056L) != 0) || ((((_la - 71)) & ~0x3f) == 0 && ((1L << (_la - 71)) & 4503599627370499L) != 0)) {
				{
				{
				setState(1004);
				stateBodyElement();
				}
				}
				setState(1009);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(1010);
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
	public static class StateBodyElementContext extends ParserRuleContext {
		public OnEntryExitContext onEntryExit() {
			return getRuleContext(OnEntryExitContext.class,0);
		}
		public InvokeStateContext invokeState() {
			return getRuleContext(InvokeStateContext.class,0);
		}
		public OnTransitionContext onTransition() {
			return getRuleContext(OnTransitionContext.class,0);
		}
		public AfterTransitionContext afterTransition() {
			return getRuleContext(AfterTransitionContext.class,0);
		}
		public IfTransitionStatementContext ifTransitionStatement() {
			return getRuleContext(IfTransitionStatementContext.class,0);
		}
		public StatesDefinitionContext statesDefinition() {
			return getRuleContext(StatesDefinitionContext.class,0);
		}
		public HistoryDefinitionContext historyDefinition() {
			return getRuleContext(HistoryDefinitionContext.class,0);
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
		enterRule(_localctx, 130, RULE_stateBodyElement);
		try {
			setState(1020);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case ON_ENTRY:
			case ON_EXIT:
				enterOuterAlt(_localctx, 1);
				{
				setState(1012);
				onEntryExit();
				}
				break;
			case INVOKE:
				enterOuterAlt(_localctx, 2);
				{
				setState(1013);
				invokeState();
				}
				break;
			case ON:
				enterOuterAlt(_localctx, 3);
				{
				setState(1014);
				onTransition();
				}
				break;
			case AFTER:
				enterOuterAlt(_localctx, 4);
				{
				setState(1015);
				afterTransition();
				}
				break;
			case IF:
				enterOuterAlt(_localctx, 5);
				{
				setState(1016);
				ifTransitionStatement();
				}
				break;
			case STATES:
				enterOuterAlt(_localctx, 6);
				{
				setState(1017);
				statesDefinition();
				}
				break;
			case HISTORY:
				enterOuterAlt(_localctx, 7);
				{
				setState(1018);
				historyDefinition();
				}
				break;
			case AT:
			case DOLLAR:
				enterOuterAlt(_localctx, 8);
				{
				setState(1019);
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
	public static class OnEntryExitContext extends ParserRuleContext {
		public ActionReferenceContext actionReference() {
			return getRuleContext(ActionReferenceContext.class,0);
		}
		public TerminalNode SEMI() { return getToken(SSoTParser.SEMI, 0); }
		public TerminalNode ON_ENTRY() { return getToken(SSoTParser.ON_ENTRY, 0); }
		public TerminalNode ON_EXIT() { return getToken(SSoTParser.ON_EXIT, 0); }
		public OnEntryExitContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_onEntryExit; }
	}

	public final OnEntryExitContext onEntryExit() throws RecognitionException {
		OnEntryExitContext _localctx = new OnEntryExitContext(_ctx, getState());
		enterRule(_localctx, 132, RULE_onEntryExit);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1022);
			_la = _input.LA(1);
			if ( !(_la==ON_ENTRY || _la==ON_EXIT) ) {
			_errHandler.recoverInline(this);
			}
			else {
				if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
				_errHandler.reportMatch(this);
				consume();
			}
			setState(1023);
			actionReference();
			setState(1024);
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
		public ReferenceValueContext referenceValue() {
			return getRuleContext(ReferenceValueContext.class,0);
		}
		public ActionReferenceContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_actionReference; }
	}

	public final ActionReferenceContext actionReference() throws RecognitionException {
		ActionReferenceContext _localctx = new ActionReferenceContext(_ctx, getState());
		enterRule(_localctx, 134, RULE_actionReference);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1026);
			referenceValue();
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
	public static class InvokeStateContext extends ParserRuleContext {
		public TerminalNode INVOKE() { return getToken(SSoTParser.INVOKE, 0); }
		public TerminalNode ID() { return getToken(SSoTParser.ID, 0); }
		public TerminalNode LBRACE() { return getToken(SSoTParser.LBRACE, 0); }
		public TerminalNode RBRACE() { return getToken(SSoTParser.RBRACE, 0); }
		public TerminalNode SEMI() { return getToken(SSoTParser.SEMI, 0); }
		public List<AnnotationContext> annotation() {
			return getRuleContexts(AnnotationContext.class);
		}
		public AnnotationContext annotation(int i) {
			return getRuleContext(AnnotationContext.class,i);
		}
		public InvokeStateBodyContext invokeStateBody() {
			return getRuleContext(InvokeStateBodyContext.class,0);
		}
		public InvokeStateContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_invokeState; }
	}

	public final InvokeStateContext invokeState() throws RecognitionException {
		InvokeStateContext _localctx = new InvokeStateContext(_ctx, getState());
		enterRule(_localctx, 136, RULE_invokeState);
		int _la;
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(1028);
			match(INVOKE);
			setState(1029);
			match(ID);
			setState(1033);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==AT || _la==DOLLAR) {
				{
				{
				setState(1030);
				annotation();
				}
				}
				setState(1035);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(1036);
			match(LBRACE);
			setState(1040);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,113,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					{
					{
					setState(1037);
					annotation();
					}
					} 
				}
				setState(1042);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,113,_ctx);
			}
			setState(1044);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (((((_la - 45)) & ~0x3f) == 0 && ((1L << (_la - 45)) & 8444249502646273L) != 0)) {
				{
				setState(1043);
				invokeStateBody();
				}
			}

			setState(1046);
			match(RBRACE);
			setState(1047);
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
	public static class InvokeStateBodyContext extends ParserRuleContext {
		public List<InvokeAttributeContext> invokeAttribute() {
			return getRuleContexts(InvokeAttributeContext.class);
		}
		public InvokeAttributeContext invokeAttribute(int i) {
			return getRuleContext(InvokeAttributeContext.class,i);
		}
		public List<TerminalNode> SEMI() { return getTokens(SSoTParser.SEMI); }
		public TerminalNode SEMI(int i) {
			return getToken(SSoTParser.SEMI, i);
		}
		public InvokeStateBodyContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_invokeStateBody; }
	}

	public final InvokeStateBodyContext invokeStateBody() throws RecognitionException {
		InvokeStateBodyContext _localctx = new InvokeStateBodyContext(_ctx, getState());
		enterRule(_localctx, 138, RULE_invokeStateBody);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1049);
			invokeAttribute();
			setState(1056);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (((((_la - 45)) & ~0x3f) == 0 && ((1L << (_la - 45)) & 8444318222123009L) != 0)) {
				{
				{
				setState(1051);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==SEMI) {
					{
					setState(1050);
					match(SEMI);
					}
				}

				setState(1053);
				invokeAttribute();
				}
				}
				setState(1058);
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
	public static class OnTransitionContext extends ParserRuleContext {
		public Token event;
		public TerminalNode ON() { return getToken(SSoTParser.ON, 0); }
		public TransitionSpecContext transitionSpec() {
			return getRuleContext(TransitionSpecContext.class,0);
		}
		public TerminalNode SEMI() { return getToken(SSoTParser.SEMI, 0); }
		public TerminalNode ID() { return getToken(SSoTParser.ID, 0); }
		public List<AnnotationContext> annotation() {
			return getRuleContexts(AnnotationContext.class);
		}
		public AnnotationContext annotation(int i) {
			return getRuleContext(AnnotationContext.class,i);
		}
		public OnTransitionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_onTransition; }
	}

	public final OnTransitionContext onTransition() throws RecognitionException {
		OnTransitionContext _localctx = new OnTransitionContext(_ctx, getState());
		enterRule(_localctx, 140, RULE_onTransition);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1059);
			match(ON);
			setState(1060);
			((OnTransitionContext)_localctx).event = match(ID);
			setState(1064);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==AT || _la==DOLLAR) {
				{
				{
				setState(1061);
				annotation();
				}
				}
				setState(1066);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(1067);
			transitionSpec();
			setState(1068);
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
	public static class AfterTransitionContext extends ParserRuleContext {
		public TerminalNode AFTER() { return getToken(SSoTParser.AFTER, 0); }
		public DurationContext duration() {
			return getRuleContext(DurationContext.class,0);
		}
		public TransitionSpecContext transitionSpec() {
			return getRuleContext(TransitionSpecContext.class,0);
		}
		public TerminalNode SEMI() { return getToken(SSoTParser.SEMI, 0); }
		public List<AnnotationContext> annotation() {
			return getRuleContexts(AnnotationContext.class);
		}
		public AnnotationContext annotation(int i) {
			return getRuleContext(AnnotationContext.class,i);
		}
		public AfterTransitionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_afterTransition; }
	}

	public final AfterTransitionContext afterTransition() throws RecognitionException {
		AfterTransitionContext _localctx = new AfterTransitionContext(_ctx, getState());
		enterRule(_localctx, 142, RULE_afterTransition);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1070);
			match(AFTER);
			setState(1071);
			duration();
			setState(1075);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==AT || _la==DOLLAR) {
				{
				{
				setState(1072);
				annotation();
				}
				}
				setState(1077);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(1078);
			transitionSpec();
			setState(1079);
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
	public static class IfTransitionStatementContext extends ParserRuleContext {
		public GuardReferenceContext condition;
		public TerminalNode IF() { return getToken(SSoTParser.IF, 0); }
		public TransitionSpecContext transitionSpec() {
			return getRuleContext(TransitionSpecContext.class,0);
		}
		public TerminalNode SEMI() { return getToken(SSoTParser.SEMI, 0); }
		public GuardReferenceContext guardReference() {
			return getRuleContext(GuardReferenceContext.class,0);
		}
		public List<AnnotationContext> annotation() {
			return getRuleContexts(AnnotationContext.class);
		}
		public AnnotationContext annotation(int i) {
			return getRuleContext(AnnotationContext.class,i);
		}
		public IfTransitionStatementContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_ifTransitionStatement; }
	}

	public final IfTransitionStatementContext ifTransitionStatement() throws RecognitionException {
		IfTransitionStatementContext _localctx = new IfTransitionStatementContext(_ctx, getState());
		enterRule(_localctx, 144, RULE_ifTransitionStatement);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1081);
			match(IF);
			setState(1082);
			((IfTransitionStatementContext)_localctx).condition = guardReference();
			setState(1086);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==AT || _la==DOLLAR) {
				{
				{
				setState(1083);
				annotation();
				}
				}
				setState(1088);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(1089);
			transitionSpec();
			setState(1090);
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
	public static class TransitionSpecContext extends ParserRuleContext {
		public TerminalNode TRANSITION() { return getToken(SSoTParser.TRANSITION, 0); }
		public TargetStateContext targetState() {
			return getRuleContext(TargetStateContext.class,0);
		}
		public TerminalNode LBRACE() { return getToken(SSoTParser.LBRACE, 0); }
		public TransitionOptionsContext transitionOptions() {
			return getRuleContext(TransitionOptionsContext.class,0);
		}
		public TerminalNode RBRACE() { return getToken(SSoTParser.RBRACE, 0); }
		public TransitionSpecContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_transitionSpec; }
	}

	public final TransitionSpecContext transitionSpec() throws RecognitionException {
		TransitionSpecContext _localctx = new TransitionSpecContext(_ctx, getState());
		enterRule(_localctx, 146, RULE_transitionSpec);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1092);
			match(TRANSITION);
			setState(1093);
			targetState();
			setState(1098);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==LBRACE) {
				{
				setState(1094);
				match(LBRACE);
				setState(1095);
				transitionOptions();
				setState(1096);
				match(RBRACE);
				}
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
	public static class TargetStateContext extends ParserRuleContext {
		public ReferenceValueContext referenceValue() {
			return getRuleContext(ReferenceValueContext.class,0);
		}
		public TerminalNode DOT() { return getToken(SSoTParser.DOT, 0); }
		public TerminalNode HISTORY() { return getToken(SSoTParser.HISTORY, 0); }
		public TargetStateContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_targetState; }
	}

	public final TargetStateContext targetState() throws RecognitionException {
		TargetStateContext _localctx = new TargetStateContext(_ctx, getState());
		enterRule(_localctx, 148, RULE_targetState);
		try {
			setState(1103);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case ID:
				enterOuterAlt(_localctx, 1);
				{
				setState(1100);
				referenceValue();
				}
				break;
			case DOT:
				enterOuterAlt(_localctx, 2);
				{
				setState(1101);
				match(DOT);
				setState(1102);
				match(HISTORY);
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
	public static class TransitionOptionsContext extends ParserRuleContext {
		public List<TransitionOptionContext> transitionOption() {
			return getRuleContexts(TransitionOptionContext.class);
		}
		public TransitionOptionContext transitionOption(int i) {
			return getRuleContext(TransitionOptionContext.class,i);
		}
		public List<TerminalNode> COMMA() { return getTokens(SSoTParser.COMMA); }
		public TerminalNode COMMA(int i) {
			return getToken(SSoTParser.COMMA, i);
		}
		public List<AnnotationContext> annotation() {
			return getRuleContexts(AnnotationContext.class);
		}
		public AnnotationContext annotation(int i) {
			return getRuleContext(AnnotationContext.class,i);
		}
		public TransitionOptionsContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_transitionOptions; }
	}

	public final TransitionOptionsContext transitionOptions() throws RecognitionException {
		TransitionOptionsContext _localctx = new TransitionOptionsContext(_ctx, getState());
		enterRule(_localctx, 150, RULE_transitionOptions);
		int _la;
		try {
			setState(1121);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,125,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(1105);
				transitionOption();
				setState(1112);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while (((((_la - 38)) & ~0x3f) == 0 && ((1L << (_la - 38)) & 35210141892615L) != 0)) {
					{
					{
					setState(1107);
					_errHandler.sync(this);
					_la = _input.LA(1);
					if (_la==COMMA) {
						{
						setState(1106);
						match(COMMA);
						}
					}

					setState(1109);
					transitionOption();
					}
					}
					setState(1114);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(1118);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while (_la==AT || _la==DOLLAR) {
					{
					{
					setState(1115);
					annotation();
					}
					}
					setState(1120);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
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
	public static class TransitionOptionContext extends ParserRuleContext {
		public TerminalNode ACTION() { return getToken(SSoTParser.ACTION, 0); }
		public TerminalNode COLON() { return getToken(SSoTParser.COLON, 0); }
		public ActionReferenceListContext actionReferenceList() {
			return getRuleContext(ActionReferenceListContext.class,0);
		}
		public TerminalNode GUARD() { return getToken(SSoTParser.GUARD, 0); }
		public GuardReferenceListContext guardReferenceList() {
			return getRuleContext(GuardReferenceListContext.class,0);
		}
		public TerminalNode ALLOWED_ACTORS() { return getToken(SSoTParser.ALLOWED_ACTORS, 0); }
		public ActorReferenceListContext actorReferenceList() {
			return getRuleContext(ActorReferenceListContext.class,0);
		}
		public AnnotationContext annotation() {
			return getRuleContext(AnnotationContext.class,0);
		}
		public TransitionOptionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_transitionOption; }
	}

	public final TransitionOptionContext transitionOption() throws RecognitionException {
		TransitionOptionContext _localctx = new TransitionOptionContext(_ctx, getState());
		enterRule(_localctx, 152, RULE_transitionOption);
		try {
			setState(1133);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case ACTION:
				enterOuterAlt(_localctx, 1);
				{
				setState(1123);
				match(ACTION);
				setState(1124);
				match(COLON);
				setState(1125);
				actionReferenceList();
				}
				break;
			case GUARD:
				enterOuterAlt(_localctx, 2);
				{
				setState(1126);
				match(GUARD);
				setState(1127);
				match(COLON);
				setState(1128);
				guardReferenceList();
				}
				break;
			case ALLOWED_ACTORS:
				enterOuterAlt(_localctx, 3);
				{
				setState(1129);
				match(ALLOWED_ACTORS);
				setState(1130);
				match(COLON);
				setState(1131);
				actorReferenceList();
				}
				break;
			case AT:
			case DOLLAR:
				enterOuterAlt(_localctx, 4);
				{
				setState(1132);
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
	public static class ActionReferenceListContext extends ParserRuleContext {
		public TerminalNode LBRACK() { return getToken(SSoTParser.LBRACK, 0); }
		public List<ActionReferenceContext> actionReference() {
			return getRuleContexts(ActionReferenceContext.class);
		}
		public ActionReferenceContext actionReference(int i) {
			return getRuleContext(ActionReferenceContext.class,i);
		}
		public TerminalNode RBRACK() { return getToken(SSoTParser.RBRACK, 0); }
		public List<TerminalNode> COMMA() { return getTokens(SSoTParser.COMMA); }
		public TerminalNode COMMA(int i) {
			return getToken(SSoTParser.COMMA, i);
		}
		public ActionReferenceListContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_actionReferenceList; }
	}

	public final ActionReferenceListContext actionReferenceList() throws RecognitionException {
		ActionReferenceListContext _localctx = new ActionReferenceListContext(_ctx, getState());
		enterRule(_localctx, 154, RULE_actionReferenceList);
		int _la;
		try {
			setState(1147);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case LBRACK:
				enterOuterAlt(_localctx, 1);
				{
				setState(1135);
				match(LBRACK);
				setState(1136);
				actionReference();
				setState(1141);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while (_la==COMMA) {
					{
					{
					setState(1137);
					match(COMMA);
					setState(1138);
					actionReference();
					}
					}
					setState(1143);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				setState(1144);
				match(RBRACK);
				}
				break;
			case ID:
				enterOuterAlt(_localctx, 2);
				{
				setState(1146);
				actionReference();
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
	public static class GuardReferenceListContext extends ParserRuleContext {
		public TerminalNode LBRACK() { return getToken(SSoTParser.LBRACK, 0); }
		public List<GuardReferenceContext> guardReference() {
			return getRuleContexts(GuardReferenceContext.class);
		}
		public GuardReferenceContext guardReference(int i) {
			return getRuleContext(GuardReferenceContext.class,i);
		}
		public TerminalNode RBRACK() { return getToken(SSoTParser.RBRACK, 0); }
		public List<TerminalNode> COMMA() { return getTokens(SSoTParser.COMMA); }
		public TerminalNode COMMA(int i) {
			return getToken(SSoTParser.COMMA, i);
		}
		public GuardReferenceListContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_guardReferenceList; }
	}

	public final GuardReferenceListContext guardReferenceList() throws RecognitionException {
		GuardReferenceListContext _localctx = new GuardReferenceListContext(_ctx, getState());
		enterRule(_localctx, 156, RULE_guardReferenceList);
		int _la;
		try {
			setState(1161);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case LBRACK:
				enterOuterAlt(_localctx, 1);
				{
				setState(1149);
				match(LBRACK);
				setState(1150);
				guardReference();
				setState(1155);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while (_la==COMMA) {
					{
					{
					setState(1151);
					match(COMMA);
					setState(1152);
					guardReference();
					}
					}
					setState(1157);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				setState(1158);
				match(RBRACK);
				}
				break;
			case ID:
				enterOuterAlt(_localctx, 2);
				{
				setState(1160);
				guardReference();
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
	public static class ActorReferenceListContext extends ParserRuleContext {
		public TerminalNode LBRACK() { return getToken(SSoTParser.LBRACK, 0); }
		public List<ReferenceValueContext> referenceValue() {
			return getRuleContexts(ReferenceValueContext.class);
		}
		public ReferenceValueContext referenceValue(int i) {
			return getRuleContext(ReferenceValueContext.class,i);
		}
		public TerminalNode RBRACK() { return getToken(SSoTParser.RBRACK, 0); }
		public List<TerminalNode> COMMA() { return getTokens(SSoTParser.COMMA); }
		public TerminalNode COMMA(int i) {
			return getToken(SSoTParser.COMMA, i);
		}
		public ActorReferenceListContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_actorReferenceList; }
	}

	public final ActorReferenceListContext actorReferenceList() throws RecognitionException {
		ActorReferenceListContext _localctx = new ActorReferenceListContext(_ctx, getState());
		enterRule(_localctx, 158, RULE_actorReferenceList);
		int _la;
		try {
			setState(1175);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case LBRACK:
				enterOuterAlt(_localctx, 1);
				{
				setState(1163);
				match(LBRACK);
				setState(1164);
				referenceValue();
				setState(1169);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while (_la==COMMA) {
					{
					{
					setState(1165);
					match(COMMA);
					setState(1166);
					referenceValue();
					}
					}
					setState(1171);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				setState(1172);
				match(RBRACK);
				}
				break;
			case ID:
				enterOuterAlt(_localctx, 2);
				{
				setState(1174);
				referenceValue();
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
	public static class GuardReferenceContext extends ParserRuleContext {
		public ReferenceValueContext referenceValue() {
			return getRuleContext(ReferenceValueContext.class,0);
		}
		public TerminalNode LPAREN() { return getToken(SSoTParser.LPAREN, 0); }
		public TerminalNode NOT() { return getToken(SSoTParser.NOT, 0); }
		public TerminalNode RPAREN() { return getToken(SSoTParser.RPAREN, 0); }
		public GuardReferenceContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_guardReference; }
	}

	public final GuardReferenceContext guardReference() throws RecognitionException {
		GuardReferenceContext _localctx = new GuardReferenceContext(_ctx, getState());
		enterRule(_localctx, 160, RULE_guardReference);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1177);
			referenceValue();
			setState(1181);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==LPAREN) {
				{
				setState(1178);
				match(LPAREN);
				setState(1179);
				match(NOT);
				setState(1180);
				match(RPAREN);
				}
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
	public static class DurationContext extends ParserRuleContext {
		public TerminalNode INT() { return getToken(SSoTParser.INT, 0); }
		public TerminalNode DURATION_UNIT() { return getToken(SSoTParser.DURATION_UNIT, 0); }
		public DurationContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_duration; }
	}

	public final DurationContext duration() throws RecognitionException {
		DurationContext _localctx = new DurationContext(_ctx, getState());
		enterRule(_localctx, 162, RULE_duration);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1183);
			match(INT);
			setState(1184);
			match(DURATION_UNIT);
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
	public static class HistoryDefinitionContext extends ParserRuleContext {
		public Token historyType;
		public Token targetRef;
		public TerminalNode HISTORY() { return getToken(SSoTParser.HISTORY, 0); }
		public TerminalNode SEMI() { return getToken(SSoTParser.SEMI, 0); }
		public List<AnnotationContext> annotation() {
			return getRuleContexts(AnnotationContext.class);
		}
		public AnnotationContext annotation(int i) {
			return getRuleContext(AnnotationContext.class,i);
		}
		public TerminalNode TARGET() { return getToken(SSoTParser.TARGET, 0); }
		public TransitionSpecContext transitionSpec() {
			return getRuleContext(TransitionSpecContext.class,0);
		}
		public TerminalNode ID() { return getToken(SSoTParser.ID, 0); }
		public TerminalNode SHALLOW() { return getToken(SSoTParser.SHALLOW, 0); }
		public TerminalNode DEEP() { return getToken(SSoTParser.DEEP, 0); }
		public HistoryDefinitionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_historyDefinition; }
	}

	public final HistoryDefinitionContext historyDefinition() throws RecognitionException {
		HistoryDefinitionContext _localctx = new HistoryDefinitionContext(_ctx, getState());
		enterRule(_localctx, 164, RULE_historyDefinition);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1186);
			match(HISTORY);
			setState(1188);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==SHALLOW || _la==DEEP) {
				{
				setState(1187);
				((HistoryDefinitionContext)_localctx).historyType = _input.LT(1);
				_la = _input.LA(1);
				if ( !(_la==SHALLOW || _la==DEEP) ) {
					((HistoryDefinitionContext)_localctx).historyType = (Token)_errHandler.recoverInline(this);
				}
				else {
					if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
					_errHandler.reportMatch(this);
					consume();
				}
				}
			}

			setState(1193);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==AT || _la==DOLLAR) {
				{
				{
				setState(1190);
				annotation();
				}
				}
				setState(1195);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(1198);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==TARGET) {
				{
				setState(1196);
				match(TARGET);
				setState(1197);
				((HistoryDefinitionContext)_localctx).targetRef = match(ID);
				}
			}

			setState(1201);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==TRANSITION) {
				{
				setState(1200);
				transitionSpec();
				}
			}

			setState(1203);
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
	public static class DeploymentConfigBlockContext extends ParserRuleContext {
		public TerminalNode DEPLOYMENT_CONFIG() { return getToken(SSoTParser.DEPLOYMENT_CONFIG, 0); }
		public TerminalNode LBRACE() { return getToken(SSoTParser.LBRACE, 0); }
		public TerminalNode RBRACE() { return getToken(SSoTParser.RBRACE, 0); }
		public List<AnnotationContext> annotation() {
			return getRuleContexts(AnnotationContext.class);
		}
		public AnnotationContext annotation(int i) {
			return getRuleContext(AnnotationContext.class,i);
		}
		public List<DeploymentElementContext> deploymentElement() {
			return getRuleContexts(DeploymentElementContext.class);
		}
		public DeploymentElementContext deploymentElement(int i) {
			return getRuleContext(DeploymentElementContext.class,i);
		}
		public DeploymentConfigBlockContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_deploymentConfigBlock; }
	}

	public final DeploymentConfigBlockContext deploymentConfigBlock() throws RecognitionException {
		DeploymentConfigBlockContext _localctx = new DeploymentConfigBlockContext(_ctx, getState());
		enterRule(_localctx, 166, RULE_deploymentConfigBlock);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1205);
			match(DEPLOYMENT_CONFIG);
			setState(1206);
			match(LBRACE);
			setState(1210);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==AT || _la==DOLLAR) {
				{
				{
				setState(1207);
				annotation();
				}
				}
				setState(1212);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(1216);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while ((((_la) & ~0x3f) == 0 && ((1L << _la) & 15032385536L) != 0)) {
				{
				{
				setState(1213);
				deploymentElement();
				}
				}
				setState(1218);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(1219);
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
	public static class DeploymentElementContext extends ParserRuleContext {
		public EnvironmentDefinitionContext environmentDefinition() {
			return getRuleContext(EnvironmentDefinitionContext.class,0);
		}
		public InfrastructureDefinitionContext infrastructureDefinition() {
			return getRuleContext(InfrastructureDefinitionContext.class,0);
		}
		public DeploymentDefinitionContext deploymentDefinition() {
			return getRuleContext(DeploymentDefinitionContext.class,0);
		}
		public DeploymentElementContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_deploymentElement; }
	}

	public final DeploymentElementContext deploymentElement() throws RecognitionException {
		DeploymentElementContext _localctx = new DeploymentElementContext(_ctx, getState());
		enterRule(_localctx, 168, RULE_deploymentElement);
		try {
			setState(1224);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case ENVIRONMENT:
				enterOuterAlt(_localctx, 1);
				{
				setState(1221);
				environmentDefinition();
				}
				break;
			case INFRASTRUCTURE:
				enterOuterAlt(_localctx, 2);
				{
				setState(1222);
				infrastructureDefinition();
				}
				break;
			case DEPLOYMENT:
				enterOuterAlt(_localctx, 3);
				{
				setState(1223);
				deploymentDefinition();
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
	public static class EnvironmentDefinitionContext extends ParserRuleContext {
		public TerminalNode ENVIRONMENT() { return getToken(SSoTParser.ENVIRONMENT, 0); }
		public TerminalNode ID() { return getToken(SSoTParser.ID, 0); }
		public TerminalNode LBRACE() { return getToken(SSoTParser.LBRACE, 0); }
		public TerminalNode RBRACE() { return getToken(SSoTParser.RBRACE, 0); }
		public List<AnnotationContext> annotation() {
			return getRuleContexts(AnnotationContext.class);
		}
		public AnnotationContext annotation(int i) {
			return getRuleContext(AnnotationContext.class,i);
		}
		public TerminalNode EXTENDS() { return getToken(SSoTParser.EXTENDS, 0); }
		public ReferenceValueContext referenceValue() {
			return getRuleContext(ReferenceValueContext.class,0);
		}
		public VariablesBlockContext variablesBlock() {
			return getRuleContext(VariablesBlockContext.class,0);
		}
		public EnvironmentDefinitionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_environmentDefinition; }
	}

	public final EnvironmentDefinitionContext environmentDefinition() throws RecognitionException {
		EnvironmentDefinitionContext _localctx = new EnvironmentDefinitionContext(_ctx, getState());
		enterRule(_localctx, 170, RULE_environmentDefinition);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1226);
			match(ENVIRONMENT);
			setState(1227);
			match(ID);
			setState(1231);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==AT || _la==DOLLAR) {
				{
				{
				setState(1228);
				annotation();
				}
				}
				setState(1233);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(1236);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==EXTENDS) {
				{
				setState(1234);
				match(EXTENDS);
				setState(1235);
				referenceValue();
				}
			}

			setState(1238);
			match(LBRACE);
			setState(1242);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==AT || _la==DOLLAR) {
				{
				{
				setState(1239);
				annotation();
				}
				}
				setState(1244);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(1246);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==VARIABLES) {
				{
				setState(1245);
				variablesBlock();
				}
			}

			setState(1248);
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
	public static class VariablesBlockContext extends ParserRuleContext {
		public TerminalNode VARIABLES() { return getToken(SSoTParser.VARIABLES, 0); }
		public TerminalNode LBRACE() { return getToken(SSoTParser.LBRACE, 0); }
		public TerminalNode RBRACE() { return getToken(SSoTParser.RBRACE, 0); }
		public List<VariableAssignmentContext> variableAssignment() {
			return getRuleContexts(VariableAssignmentContext.class);
		}
		public VariableAssignmentContext variableAssignment(int i) {
			return getRuleContext(VariableAssignmentContext.class,i);
		}
		public VariablesBlockContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_variablesBlock; }
	}

	public final VariablesBlockContext variablesBlock() throws RecognitionException {
		VariablesBlockContext _localctx = new VariablesBlockContext(_ctx, getState());
		enterRule(_localctx, 172, RULE_variablesBlock);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1250);
			match(VARIABLES);
			setState(1251);
			match(LBRACE);
			setState(1255);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==ID) {
				{
				{
				setState(1252);
				variableAssignment();
				}
				}
				setState(1257);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(1258);
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
	public static class VariableAssignmentContext extends ParserRuleContext {
		public TerminalNode ID() { return getToken(SSoTParser.ID, 0); }
		public TerminalNode COLON() { return getToken(SSoTParser.COLON, 0); }
		public ValueContext value() {
			return getRuleContext(ValueContext.class,0);
		}
		public TerminalNode SEMI() { return getToken(SSoTParser.SEMI, 0); }
		public VariableAssignmentContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_variableAssignment; }
	}

	public final VariableAssignmentContext variableAssignment() throws RecognitionException {
		VariableAssignmentContext _localctx = new VariableAssignmentContext(_ctx, getState());
		enterRule(_localctx, 174, RULE_variableAssignment);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1260);
			match(ID);
			setState(1261);
			match(COLON);
			setState(1262);
			value();
			setState(1263);
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
	public static class InfrastructureDefinitionContext extends ParserRuleContext {
		public TerminalNode INFRASTRUCTURE() { return getToken(SSoTParser.INFRASTRUCTURE, 0); }
		public TerminalNode ID() { return getToken(SSoTParser.ID, 0); }
		public TerminalNode LBRACE() { return getToken(SSoTParser.LBRACE, 0); }
		public TerminalNode RBRACE() { return getToken(SSoTParser.RBRACE, 0); }
		public List<AnnotationContext> annotation() {
			return getRuleContexts(AnnotationContext.class);
		}
		public AnnotationContext annotation(int i) {
			return getRuleContext(AnnotationContext.class,i);
		}
		public List<AttributeAssignmentContext> attributeAssignment() {
			return getRuleContexts(AttributeAssignmentContext.class);
		}
		public AttributeAssignmentContext attributeAssignment(int i) {
			return getRuleContext(AttributeAssignmentContext.class,i);
		}
		public InfrastructureDefinitionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_infrastructureDefinition; }
	}

	public final InfrastructureDefinitionContext infrastructureDefinition() throws RecognitionException {
		InfrastructureDefinitionContext _localctx = new InfrastructureDefinitionContext(_ctx, getState());
		enterRule(_localctx, 176, RULE_infrastructureDefinition);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1265);
			match(INFRASTRUCTURE);
			setState(1266);
			match(ID);
			setState(1270);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==AT || _la==DOLLAR) {
				{
				{
				setState(1267);
				annotation();
				}
				}
				setState(1272);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(1273);
			match(LBRACE);
			setState(1278);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (((((_la - 64)) & ~0x3f) == 0 && ((1L << (_la - 64)) & 385L) != 0)) {
				{
				setState(1276);
				_errHandler.sync(this);
				switch (_input.LA(1)) {
				case AT:
				case DOLLAR:
					{
					setState(1274);
					annotation();
					}
					break;
				case ID:
					{
					setState(1275);
					attributeAssignment();
					}
					break;
				default:
					throw new NoViableAltException(this);
				}
				}
				setState(1280);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(1281);
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
	public static class DeploymentDefinitionContext extends ParserRuleContext {
		public TerminalNode DEPLOYMENT() { return getToken(SSoTParser.DEPLOYMENT, 0); }
		public TerminalNode ID() { return getToken(SSoTParser.ID, 0); }
		public TerminalNode LBRACE() { return getToken(SSoTParser.LBRACE, 0); }
		public TerminalNode RBRACE() { return getToken(SSoTParser.RBRACE, 0); }
		public List<AnnotationContext> annotation() {
			return getRuleContexts(AnnotationContext.class);
		}
		public AnnotationContext annotation(int i) {
			return getRuleContext(AnnotationContext.class,i);
		}
		public List<AttributeAssignmentContext> attributeAssignment() {
			return getRuleContexts(AttributeAssignmentContext.class);
		}
		public AttributeAssignmentContext attributeAssignment(int i) {
			return getRuleContext(AttributeAssignmentContext.class,i);
		}
		public DeploymentDefinitionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_deploymentDefinition; }
	}

	public final DeploymentDefinitionContext deploymentDefinition() throws RecognitionException {
		DeploymentDefinitionContext _localctx = new DeploymentDefinitionContext(_ctx, getState());
		enterRule(_localctx, 178, RULE_deploymentDefinition);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1283);
			match(DEPLOYMENT);
			setState(1284);
			match(ID);
			setState(1288);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==AT || _la==DOLLAR) {
				{
				{
				setState(1285);
				annotation();
				}
				}
				setState(1290);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(1291);
			match(LBRACE);
			setState(1296);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (((((_la - 64)) & ~0x3f) == 0 && ((1L << (_la - 64)) & 385L) != 0)) {
				{
				setState(1294);
				_errHandler.sync(this);
				switch (_input.LA(1)) {
				case AT:
				case DOLLAR:
					{
					setState(1292);
					annotation();
					}
					break;
				case ID:
					{
					setState(1293);
					attributeAssignment();
					}
					break;
				default:
					throw new NoViableAltException(this);
				}
				}
				setState(1298);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(1299);
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
	public static class AttributeAssignmentContext extends ParserRuleContext {
		public TerminalNode ID() { return getToken(SSoTParser.ID, 0); }
		public TerminalNode COLON() { return getToken(SSoTParser.COLON, 0); }
		public ValueContext value() {
			return getRuleContext(ValueContext.class,0);
		}
		public TerminalNode SEMI() { return getToken(SSoTParser.SEMI, 0); }
		public AttributeAssignmentContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_attributeAssignment; }
	}

	public final AttributeAssignmentContext attributeAssignment() throws RecognitionException {
		AttributeAssignmentContext _localctx = new AttributeAssignmentContext(_ctx, getState());
		enterRule(_localctx, 180, RULE_attributeAssignment);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1301);
			match(ID);
			setState(1302);
			match(COLON);
			setState(1303);
			value();
			setState(1304);
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
	public static class DependenciesBlockContext extends ParserRuleContext {
		public TerminalNode DEPENDENCIES() { return getToken(SSoTParser.DEPENDENCIES, 0); }
		public TerminalNode LBRACE() { return getToken(SSoTParser.LBRACE, 0); }
		public TerminalNode RBRACE() { return getToken(SSoTParser.RBRACE, 0); }
		public List<AnnotationContext> annotation() {
			return getRuleContexts(AnnotationContext.class);
		}
		public AnnotationContext annotation(int i) {
			return getRuleContext(AnnotationContext.class,i);
		}
		public List<TargetDependencyBlockContext> targetDependencyBlock() {
			return getRuleContexts(TargetDependencyBlockContext.class);
		}
		public TargetDependencyBlockContext targetDependencyBlock(int i) {
			return getRuleContext(TargetDependencyBlockContext.class,i);
		}
		public DependenciesBlockContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_dependenciesBlock; }
	}

	public final DependenciesBlockContext dependenciesBlock() throws RecognitionException {
		DependenciesBlockContext _localctx = new DependenciesBlockContext(_ctx, getState());
		enterRule(_localctx, 182, RULE_dependenciesBlock);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1306);
			match(DEPENDENCIES);
			setState(1307);
			match(LBRACE);
			setState(1311);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==AT || _la==DOLLAR) {
				{
				{
				setState(1308);
				annotation();
				}
				}
				setState(1313);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(1317);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==RUST || _la==NODEJS) {
				{
				{
				setState(1314);
				targetDependencyBlock();
				}
				}
				setState(1319);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(1320);
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
	public static class TargetDependencyBlockContext extends ParserRuleContext {
		public TargetTypeContext targetType() {
			return getRuleContext(TargetTypeContext.class,0);
		}
		public TerminalNode STRING() { return getToken(SSoTParser.STRING, 0); }
		public TerminalNode LBRACE() { return getToken(SSoTParser.LBRACE, 0); }
		public TerminalNode RBRACE() { return getToken(SSoTParser.RBRACE, 0); }
		public List<AnnotationContext> annotation() {
			return getRuleContexts(AnnotationContext.class);
		}
		public AnnotationContext annotation(int i) {
			return getRuleContext(AnnotationContext.class,i);
		}
		public List<DependencyEntryContext> dependencyEntry() {
			return getRuleContexts(DependencyEntryContext.class);
		}
		public DependencyEntryContext dependencyEntry(int i) {
			return getRuleContext(DependencyEntryContext.class,i);
		}
		public TargetDependencyBlockContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_targetDependencyBlock; }
	}

	public final TargetDependencyBlockContext targetDependencyBlock() throws RecognitionException {
		TargetDependencyBlockContext _localctx = new TargetDependencyBlockContext(_ctx, getState());
		enterRule(_localctx, 184, RULE_targetDependencyBlock);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1322);
			targetType();
			setState(1323);
			match(STRING);
			setState(1327);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==AT || _la==DOLLAR) {
				{
				{
				setState(1324);
				annotation();
				}
				}
				setState(1329);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(1330);
			match(LBRACE);
			setState(1334);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==AT || _la==DOLLAR) {
				{
				{
				setState(1331);
				annotation();
				}
				}
				setState(1336);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(1340);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==ID) {
				{
				{
				setState(1337);
				dependencyEntry();
				}
				}
				setState(1342);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(1343);
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
	public static class TargetTypeContext extends ParserRuleContext {
		public TerminalNode RUST() { return getToken(SSoTParser.RUST, 0); }
		public TerminalNode NODEJS() { return getToken(SSoTParser.NODEJS, 0); }
		public TargetTypeContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_targetType; }
	}

	public final TargetTypeContext targetType() throws RecognitionException {
		TargetTypeContext _localctx = new TargetTypeContext(_ctx, getState());
		enterRule(_localctx, 186, RULE_targetType);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1345);
			_la = _input.LA(1);
			if ( !(_la==RUST || _la==NODEJS) ) {
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
	public static class DependencyEntryContext extends ParserRuleContext {
		public TerminalNode ID() { return getToken(SSoTParser.ID, 0); }
		public TerminalNode LBRACE() { return getToken(SSoTParser.LBRACE, 0); }
		public TerminalNode RBRACE() { return getToken(SSoTParser.RBRACE, 0); }
		public List<AnnotationContext> annotation() {
			return getRuleContexts(AnnotationContext.class);
		}
		public AnnotationContext annotation(int i) {
			return getRuleContext(AnnotationContext.class,i);
		}
		public List<DependencyAttributeContext> dependencyAttribute() {
			return getRuleContexts(DependencyAttributeContext.class);
		}
		public DependencyAttributeContext dependencyAttribute(int i) {
			return getRuleContext(DependencyAttributeContext.class,i);
		}
		public DependencyEntryContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_dependencyEntry; }
	}

	public final DependencyEntryContext dependencyEntry() throws RecognitionException {
		DependencyEntryContext _localctx = new DependencyEntryContext(_ctx, getState());
		enterRule(_localctx, 188, RULE_dependencyEntry);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1347);
			match(ID);
			setState(1351);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==AT || _la==DOLLAR) {
				{
				{
				setState(1348);
				annotation();
				}
				}
				setState(1353);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(1354);
			match(LBRACE);
			setState(1359);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (((((_la - 47)) & ~0x3f) == 0 && ((1L << (_la - 47)) & 50462721L) != 0)) {
				{
				setState(1357);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,158,_ctx) ) {
				case 1:
					{
					setState(1355);
					annotation();
					}
					break;
				case 2:
					{
					setState(1356);
					dependencyAttribute();
					}
					break;
				}
				}
				setState(1361);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(1362);
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
	public static class DependencyAttributeContext extends ParserRuleContext {
		public TerminalNode FEATURES() { return getToken(SSoTParser.FEATURES, 0); }
		public TerminalNode COLON() { return getToken(SSoTParser.COLON, 0); }
		public ArrayValueContext arrayValue() {
			return getRuleContext(ArrayValueContext.class,0);
		}
		public AttributeAssignmentContext attributeAssignment() {
			return getRuleContext(AttributeAssignmentContext.class,0);
		}
		public AnnotationContext annotation() {
			return getRuleContext(AnnotationContext.class,0);
		}
		public DependencyAttributeContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_dependencyAttribute; }
	}

	public final DependencyAttributeContext dependencyAttribute() throws RecognitionException {
		DependencyAttributeContext _localctx = new DependencyAttributeContext(_ctx, getState());
		enterRule(_localctx, 190, RULE_dependencyAttribute);
		try {
			setState(1369);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case FEATURES:
				enterOuterAlt(_localctx, 1);
				{
				{
				setState(1364);
				match(FEATURES);
				setState(1365);
				match(COLON);
				setState(1366);
				arrayValue();
				}
				}
				break;
			case ID:
				enterOuterAlt(_localctx, 2);
				{
				setState(1367);
				attributeAssignment();
				}
				break;
			case AT:
			case DOLLAR:
				enterOuterAlt(_localctx, 3);
				{
				setState(1368);
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

	public boolean sempred(RuleContext _localctx, int ruleIndex, int predIndex) {
		switch (ruleIndex) {
		case 6:
			return annotationValue_sempred((AnnotationValueContext)_localctx, predIndex);
		}
		return true;
	}
	private boolean annotationValue_sempred(AnnotationValueContext _localctx, int predIndex) {
		switch (predIndex) {
		case 0:
			return _input.LA(2) == COLON;
		}
		return true;
	}

	public static final String _serializedATN =
		"\u0004\u0001{\u055c\u0002\u0000\u0007\u0000\u0002\u0001\u0007\u0001\u0002"+
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
		"2\u00072\u00023\u00073\u00024\u00074\u00025\u00075\u00026\u00076\u0002"+
		"7\u00077\u00028\u00078\u00029\u00079\u0002:\u0007:\u0002;\u0007;\u0002"+
		"<\u0007<\u0002=\u0007=\u0002>\u0007>\u0002?\u0007?\u0002@\u0007@\u0002"+
		"A\u0007A\u0002B\u0007B\u0002C\u0007C\u0002D\u0007D\u0002E\u0007E\u0002"+
		"F\u0007F\u0002G\u0007G\u0002H\u0007H\u0002I\u0007I\u0002J\u0007J\u0002"+
		"K\u0007K\u0002L\u0007L\u0002M\u0007M\u0002N\u0007N\u0002O\u0007O\u0002"+
		"P\u0007P\u0002Q\u0007Q\u0002R\u0007R\u0002S\u0007S\u0002T\u0007T\u0002"+
		"U\u0007U\u0002V\u0007V\u0002W\u0007W\u0002X\u0007X\u0002Y\u0007Y\u0002"+
		"Z\u0007Z\u0002[\u0007[\u0002\\\u0007\\\u0002]\u0007]\u0002^\u0007^\u0002"+
		"_\u0007_\u0001\u0000\u0003\u0000\u00c2\b\u0000\u0001\u0000\u0005\u0000"+
		"\u00c5\b\u0000\n\u0000\f\u0000\u00c8\t\u0000\u0001\u0000\u0005\u0000\u00cb"+
		"\b\u0000\n\u0000\f\u0000\u00ce\t\u0000\u0001\u0000\u0005\u0000\u00d1\b"+
		"\u0000\n\u0000\f\u0000\u00d4\t\u0000\u0001\u0000\u0005\u0000\u00d7\b\u0000"+
		"\n\u0000\f\u0000\u00da\t\u0000\u0001\u0000\u0005\u0000\u00dd\b\u0000\n"+
		"\u0000\f\u0000\u00e0\t\u0000\u0001\u0000\u0001\u0000\u0001\u0001\u0001"+
		"\u0001\u0001\u0001\u0001\u0001\u0001\u0002\u0001\u0002\u0001\u0002\u0001"+
		"\u0002\u0001\u0003\u0001\u0003\u0001\u0003\u0001\u0003\u0001\u0003\u0001"+
		"\u0003\u0001\u0003\u0003\u0003\u00f3\b\u0003\u0001\u0004\u0001\u0004\u0001"+
		"\u0005\u0001\u0005\u0001\u0005\u0001\u0005\u0001\u0005\u0001\u0005\u0001"+
		"\u0005\u0001\u0005\u0001\u0005\u0003\u0005\u0100\b\u0005\u0001\u0005\u0001"+
		"\u0005\u0001\u0005\u0001\u0005\u0001\u0005\u0001\u0005\u0003\u0005\u0108"+
		"\b\u0005\u0001\u0006\u0001\u0006\u0001\u0006\u0003\u0006\u010d\b\u0006"+
		"\u0001\u0007\u0001\u0007\u0001\u0007\u0001\u0007\u0003\u0007\u0113\b\u0007"+
		"\u0001\b\u0001\b\u0001\b\u0005\b\u0118\b\b\n\b\f\b\u011b\t\b\u0001\t\u0001"+
		"\t\u0001\t\u0001\t\u0001\t\u0001\t\u0003\t\u0123\b\t\u0001\n\u0001\n\u0001"+
		"\u000b\u0001\u000b\u0001\u000b\u0005\u000b\u012a\b\u000b\n\u000b\f\u000b"+
		"\u012d\t\u000b\u0001\f\u0001\f\u0003\f\u0131\b\f\u0001\f\u0001\f\u0001"+
		"\r\u0001\r\u0003\r\u0137\b\r\u0001\r\u0001\r\u0001\u000e\u0001\u000e\u0001"+
		"\u000e\u0005\u000e\u013e\b\u000e\n\u000e\f\u000e\u0141\t\u000e\u0001\u000f"+
		"\u0001\u000f\u0001\u000f\u0005\u000f\u0146\b\u000f\n\u000f\f\u000f\u0149"+
		"\t\u000f\u0001\u000f\u0005\u000f\u014c\b\u000f\n\u000f\f\u000f\u014f\t"+
		"\u000f\u0001\u000f\u0001\u000f\u0001\u0010\u0001\u0010\u0003\u0010\u0155"+
		"\b\u0010\u0001\u0011\u0001\u0011\u0001\u0011\u0005\u0011\u015a\b\u0011"+
		"\n\u0011\f\u0011\u015d\t\u0011\u0001\u0011\u0001\u0011\u0005\u0011\u0161"+
		"\b\u0011\n\u0011\f\u0011\u0164\t\u0011\u0001\u0011\u0005\u0011\u0167\b"+
		"\u0011\n\u0011\f\u0011\u016a\t\u0011\u0001\u0011\u0001\u0011\u0001\u0012"+
		"\u0001\u0012\u0001\u0012\u0001\u0012\u0005\u0012\u0172\b\u0012\n\u0012"+
		"\f\u0012\u0175\t\u0012\u0001\u0012\u0001\u0012\u0005\u0012\u0179\b\u0012"+
		"\n\u0012\f\u0012\u017c\t\u0012\u0001\u0012\u0003\u0012\u017f\b\u0012\u0001"+
		"\u0012\u0001\u0012\u0001\u0013\u0001\u0013\u0001\u0013\u0005\u0013\u0186"+
		"\b\u0013\n\u0013\f\u0013\u0189\t\u0013\u0001\u0013\u0001\u0013\u0005\u0013"+
		"\u018d\b\u0013\n\u0013\f\u0013\u0190\t\u0013\u0001\u0013\u0005\u0013\u0193"+
		"\b\u0013\n\u0013\f\u0013\u0196\t\u0013\u0001\u0013\u0001\u0013\u0001\u0014"+
		"\u0001\u0014\u0005\u0014\u019c\b\u0014\n\u0014\f\u0014\u019f\t\u0014\u0001"+
		"\u0014\u0001\u0014\u0001\u0015\u0001\u0015\u0001\u0015\u0001\u0015\u0001"+
		"\u0015\u0001\u0015\u0001\u0015\u0001\u0015\u0001\u0015\u0001\u0015\u0001"+
		"\u0015\u0001\u0015\u0001\u0015\u0001\u0015\u0001\u0015\u0001\u0015\u0001"+
		"\u0015\u0001\u0015\u0001\u0015\u0003\u0015\u01b6\b\u0015\u0001\u0016\u0001"+
		"\u0016\u0001\u0017\u0001\u0017\u0001\u0017\u0005\u0017\u01bd\b\u0017\n"+
		"\u0017\f\u0017\u01c0\t\u0017\u0001\u0017\u0005\u0017\u01c3\b\u0017\n\u0017"+
		"\f\u0017\u01c6\t\u0017\u0001\u0017\u0001\u0017\u0001\u0018\u0001\u0018"+
		"\u0001\u0018\u0005\u0018\u01cd\b\u0018\n\u0018\f\u0018\u01d0\t\u0018\u0001"+
		"\u0018\u0001\u0018\u0005\u0018\u01d4\b\u0018\n\u0018\f\u0018\u01d7\t\u0018"+
		"\u0001\u0018\u0001\u0018\u0001\u0019\u0001\u0019\u0001\u0019\u0005\u0019"+
		"\u01de\b\u0019\n\u0019\f\u0019\u01e1\t\u0019\u0001\u0019\u0005\u0019\u01e4"+
		"\b\u0019\n\u0019\f\u0019\u01e7\t\u0019\u0001\u0019\u0001\u0019\u0001\u001a"+
		"\u0001\u001a\u0001\u001a\u0003\u001a\u01ee\b\u001a\u0001\u001b\u0001\u001b"+
		"\u0001\u001b\u0005\u001b\u01f3\b\u001b\n\u001b\f\u001b\u01f6\t\u001b\u0001"+
		"\u001b\u0001\u001b\u0005\u001b\u01fa\b\u001b\n\u001b\f\u001b\u01fd\t\u001b"+
		"\u0001\u001b\u0001\u001b\u0001\u001c\u0001\u001c\u0001\u001c\u0005\u001c"+
		"\u0204\b\u001c\n\u001c\f\u001c\u0207\t\u001c\u0001\u001c\u0001\u001c\u0005"+
		"\u001c\u020b\b\u001c\n\u001c\f\u001c\u020e\t\u001c\u0001\u001c\u0005\u001c"+
		"\u0211\b\u001c\n\u001c\f\u001c\u0214\t\u001c\u0001\u001c\u0001\u001c\u0001"+
		"\u001d\u0001\u001d\u0003\u001d\u021a\b\u001d\u0001\u001e\u0001\u001e\u0001"+
		"\u001e\u0003\u001e\u021f\b\u001e\u0001\u001e\u0001\u001e\u0001\u001f\u0001"+
		"\u001f\u0001\u001f\u0005\u001f\u0226\b\u001f\n\u001f\f\u001f\u0229\t\u001f"+
		"\u0001\u001f\u0001\u001f\u0005\u001f\u022d\b\u001f\n\u001f\f\u001f\u0230"+
		"\t\u001f\u0001\u001f\u0005\u001f\u0233\b\u001f\n\u001f\f\u001f\u0236\t"+
		"\u001f\u0001\u001f\u0001\u001f\u0001 \u0001 \u0001 \u0005 \u023d\b \n"+
		" \f \u0240\t \u0001 \u0005 \u0243\b \n \f \u0246\t \u0001 \u0001 \u0001"+
		"!\u0001!\u0003!\u024c\b!\u0001\"\u0001\"\u0001\"\u0005\"\u0251\b\"\n\""+
		"\f\"\u0254\t\"\u0001\"\u0001\"\u0005\"\u0258\b\"\n\"\f\"\u025b\t\"\u0001"+
		"\"\u0005\"\u025e\b\"\n\"\f\"\u0261\t\"\u0001\"\u0001\"\u0001#\u0001#\u0005"+
		"#\u0267\b#\n#\f#\u026a\t#\u0001#\u0001#\u0003#\u026e\b#\u0001#\u0001#"+
		"\u0001#\u0003#\u0273\b#\u0001#\u0001#\u0005#\u0277\b#\n#\f#\u027a\t#\u0001"+
		"#\u0003#\u027d\b#\u0001#\u0001#\u0001$\u0001$\u0001$\u0005$\u0284\b$\n"+
		"$\f$\u0287\t$\u0001%\u0001%\u0001%\u0001%\u0001&\u0001&\u0001&\u0005&"+
		"\u0290\b&\n&\f&\u0293\t&\u0001&\u0001&\u0003&\u0297\b&\u0001&\u0001&\u0005"+
		"&\u029b\b&\n&\f&\u029e\t&\u0001&\u0001&\u0001\'\u0001\'\u0001\'\u0005"+
		"\'\u02a5\b\'\n\'\f\'\u02a8\t\'\u0001\'\u0005\'\u02ab\b\'\n\'\f\'\u02ae"+
		"\t\'\u0001\'\u0001\'\u0001(\u0001(\u0001(\u0005(\u02b5\b(\n(\f(\u02b8"+
		"\t(\u0001(\u0001(\u0005(\u02bc\b(\n(\f(\u02bf\t(\u0001(\u0005(\u02c2\b"+
		"(\n(\f(\u02c5\t(\u0001(\u0001(\u0001)\u0001)\u0001)\u0001)\u0001)\u0001"+
		")\u0003)\u02cf\b)\u0001*\u0001*\u0005*\u02d3\b*\n*\f*\u02d6\t*\u0001*"+
		"\u0001*\u0005*\u02da\b*\n*\f*\u02dd\t*\u0001*\u0005*\u02e0\b*\n*\f*\u02e3"+
		"\t*\u0001*\u0001*\u0001+\u0001+\u0001+\u0001+\u0005+\u02eb\b+\n+\f+\u02ee"+
		"\t+\u0001+\u0001+\u0005+\u02f2\b+\n+\f+\u02f5\t+\u0001+\u0003+\u02f8\b"+
		"+\u0001+\u0001+\u0001,\u0001,\u0005,\u02fe\b,\n,\f,\u0301\t,\u0001,\u0001"+
		",\u0005,\u0305\b,\n,\f,\u0308\t,\u0001,\u0005,\u030b\b,\n,\f,\u030e\t"+
		",\u0001,\u0001,\u0001-\u0001-\u0005-\u0314\b-\n-\f-\u0317\t-\u0001-\u0001"+
		"-\u0003-\u031b\b-\u0001-\u0001-\u0003-\u031f\b-\u0001-\u0003-\u0322\b"+
		"-\u0001-\u0001-\u0003-\u0326\b-\u0001-\u0001-\u0001.\u0001.\u0005.\u032c"+
		"\b.\n.\f.\u032f\t.\u0001.\u0001.\u0005.\u0333\b.\n.\f.\u0336\t.\u0001"+
		".\u0005.\u0339\b.\n.\f.\u033c\t.\u0001.\u0001.\u0001/\u0001/\u0005/\u0342"+
		"\b/\n/\f/\u0345\t/\u0001/\u0001/\u0003/\u0349\b/\u0001/\u0003/\u034c\b"+
		"/\u0001/\u0001/\u0001/\u0001/\u00010\u00010\u00050\u0354\b0\n0\f0\u0357"+
		"\t0\u00010\u00010\u00050\u035b\b0\n0\f0\u035e\t0\u00010\u00050\u0361\b"+
		"0\n0\f0\u0364\t0\u00010\u00010\u00011\u00011\u00051\u036a\b1\n1\f1\u036d"+
		"\t1\u00011\u00011\u00051\u0371\b1\n1\f1\u0374\t1\u00011\u00031\u0377\b"+
		"1\u00011\u00011\u00012\u00012\u00032\u037d\b2\u00012\u00052\u0380\b2\n"+
		"2\f2\u0383\t2\u00013\u00013\u00013\u00013\u00013\u00013\u00033\u038b\b"+
		"3\u00014\u00014\u00014\u00014\u00015\u00015\u00015\u00015\u00035\u0395"+
		"\b5\u00015\u00015\u00016\u00016\u00016\u00016\u00036\u039d\b6\u00016\u0001"+
		"6\u00017\u00017\u00017\u00017\u00018\u00018\u00018\u00018\u00019\u0005"+
		"9\u03aa\b9\n9\f9\u03ad\t9\u00019\u00019\u00039\u03b1\b9\u0001:\u0001:"+
		"\u0003:\u03b5\b:\u0001;\u0001;\u0001<\u0001<\u0001<\u0005<\u03bc\b<\n"+
		"<\f<\u03bf\t<\u0001=\u0001=\u0001=\u0001=\u0001>\u0001>\u0005>\u03c7\b"+
		">\n>\f>\u03ca\t>\u0001>\u0001>\u0005>\u03ce\b>\n>\f>\u03d1\t>\u0001>\u0005"+
		">\u03d4\b>\n>\f>\u03d7\t>\u0001>\u0001>\u0001?\u0001?\u0003?\u03dd\b?"+
		"\u0001@\u0001@\u0005@\u03e1\b@\n@\f@\u03e4\t@\u0001@\u0001@\u0005@\u03e8"+
		"\b@\n@\f@\u03eb\t@\u0001@\u0005@\u03ee\b@\n@\f@\u03f1\t@\u0001@\u0001"+
		"@\u0001A\u0001A\u0001A\u0001A\u0001A\u0001A\u0001A\u0001A\u0003A\u03fd"+
		"\bA\u0001B\u0001B\u0001B\u0001B\u0001C\u0001C\u0001D\u0001D\u0001D\u0005"+
		"D\u0408\bD\nD\fD\u040b\tD\u0001D\u0001D\u0005D\u040f\bD\nD\fD\u0412\t"+
		"D\u0001D\u0003D\u0415\bD\u0001D\u0001D\u0001D\u0001E\u0001E\u0003E\u041c"+
		"\bE\u0001E\u0005E\u041f\bE\nE\fE\u0422\tE\u0001F\u0001F\u0001F\u0005F"+
		"\u0427\bF\nF\fF\u042a\tF\u0001F\u0001F\u0001F\u0001G\u0001G\u0001G\u0005"+
		"G\u0432\bG\nG\fG\u0435\tG\u0001G\u0001G\u0001G\u0001H\u0001H\u0001H\u0005"+
		"H\u043d\bH\nH\fH\u0440\tH\u0001H\u0001H\u0001H\u0001I\u0001I\u0001I\u0001"+
		"I\u0001I\u0001I\u0003I\u044b\bI\u0001J\u0001J\u0001J\u0003J\u0450\bJ\u0001"+
		"K\u0001K\u0003K\u0454\bK\u0001K\u0005K\u0457\bK\nK\fK\u045a\tK\u0001K"+
		"\u0005K\u045d\bK\nK\fK\u0460\tK\u0003K\u0462\bK\u0001L\u0001L\u0001L\u0001"+
		"L\u0001L\u0001L\u0001L\u0001L\u0001L\u0001L\u0003L\u046e\bL\u0001M\u0001"+
		"M\u0001M\u0001M\u0005M\u0474\bM\nM\fM\u0477\tM\u0001M\u0001M\u0001M\u0003"+
		"M\u047c\bM\u0001N\u0001N\u0001N\u0001N\u0005N\u0482\bN\nN\fN\u0485\tN"+
		"\u0001N\u0001N\u0001N\u0003N\u048a\bN\u0001O\u0001O\u0001O\u0001O\u0005"+
		"O\u0490\bO\nO\fO\u0493\tO\u0001O\u0001O\u0001O\u0003O\u0498\bO\u0001P"+
		"\u0001P\u0001P\u0001P\u0003P\u049e\bP\u0001Q\u0001Q\u0001Q\u0001R\u0001"+
		"R\u0003R\u04a5\bR\u0001R\u0005R\u04a8\bR\nR\fR\u04ab\tR\u0001R\u0001R"+
		"\u0003R\u04af\bR\u0001R\u0003R\u04b2\bR\u0001R\u0001R\u0001S\u0001S\u0001"+
		"S\u0005S\u04b9\bS\nS\fS\u04bc\tS\u0001S\u0005S\u04bf\bS\nS\fS\u04c2\t"+
		"S\u0001S\u0001S\u0001T\u0001T\u0001T\u0003T\u04c9\bT\u0001U\u0001U\u0001"+
		"U\u0005U\u04ce\bU\nU\fU\u04d1\tU\u0001U\u0001U\u0003U\u04d5\bU\u0001U"+
		"\u0001U\u0005U\u04d9\bU\nU\fU\u04dc\tU\u0001U\u0003U\u04df\bU\u0001U\u0001"+
		"U\u0001V\u0001V\u0001V\u0005V\u04e6\bV\nV\fV\u04e9\tV\u0001V\u0001V\u0001"+
		"W\u0001W\u0001W\u0001W\u0001W\u0001X\u0001X\u0001X\u0005X\u04f5\bX\nX"+
		"\fX\u04f8\tX\u0001X\u0001X\u0001X\u0005X\u04fd\bX\nX\fX\u0500\tX\u0001"+
		"X\u0001X\u0001Y\u0001Y\u0001Y\u0005Y\u0507\bY\nY\fY\u050a\tY\u0001Y\u0001"+
		"Y\u0001Y\u0005Y\u050f\bY\nY\fY\u0512\tY\u0001Y\u0001Y\u0001Z\u0001Z\u0001"+
		"Z\u0001Z\u0001Z\u0001[\u0001[\u0001[\u0005[\u051e\b[\n[\f[\u0521\t[\u0001"+
		"[\u0005[\u0524\b[\n[\f[\u0527\t[\u0001[\u0001[\u0001\\\u0001\\\u0001\\"+
		"\u0005\\\u052e\b\\\n\\\f\\\u0531\t\\\u0001\\\u0001\\\u0005\\\u0535\b\\"+
		"\n\\\f\\\u0538\t\\\u0001\\\u0005\\\u053b\b\\\n\\\f\\\u053e\t\\\u0001\\"+
		"\u0001\\\u0001]\u0001]\u0001^\u0001^\u0005^\u0546\b^\n^\f^\u0549\t^\u0001"+
		"^\u0001^\u0001^\u0005^\u054e\b^\n^\f^\u0551\t^\u0001^\u0001^\u0001_\u0001"+
		"_\u0001_\u0001_\u0001_\u0003_\u055a\b_\u0001_\u0000\u0000`\u0000\u0002"+
		"\u0004\u0006\b\n\f\u000e\u0010\u0012\u0014\u0016\u0018\u001a\u001c\u001e"+
		" \"$&(*,.02468:<>@BDFHJLNPRTVXZ\\^`bdfhjlnprtvxz|~\u0080\u0082\u0084\u0086"+
		"\u0088\u008a\u008c\u008e\u0090\u0092\u0094\u0096\u0098\u009a\u009c\u009e"+
		"\u00a0\u00a2\u00a4\u00a6\u00a8\u00aa\u00ac\u00ae\u00b0\u00b2\u00b4\u00b6"+
		"\u00b8\u00ba\u00bc\u00be\u0000\u0006\u0006\u0000\u001a\u001a\u001c\u001c"+
		"%%..@@Yz\u0001\u0000BE\u0001\u00000<\u0001\u0000+,\u0001\u0000\u0015\u0016"+
		"\u0001\u0000\"#\u05bc\u0000\u00c1\u0001\u0000\u0000\u0000\u0002\u00e3"+
		"\u0001\u0000\u0000\u0000\u0004\u00e7\u0001\u0000\u0000\u0000\u0006\u00f2"+
		"\u0001\u0000\u0000\u0000\b\u00f4\u0001\u0000\u0000\u0000\n\u0107\u0001"+
		"\u0000\u0000\u0000\f\u010c\u0001\u0000\u0000\u0000\u000e\u0112\u0001\u0000"+
		"\u0000\u0000\u0010\u0114\u0001\u0000\u0000\u0000\u0012\u011c\u0001\u0000"+
		"\u0000\u0000\u0014\u0124\u0001\u0000\u0000\u0000\u0016\u0126\u0001\u0000"+
		"\u0000\u0000\u0018\u012e\u0001\u0000\u0000\u0000\u001a\u0134\u0001\u0000"+
		"\u0000\u0000\u001c\u013a\u0001\u0000\u0000\u0000\u001e\u0142\u0001\u0000"+
		"\u0000\u0000 \u0154\u0001\u0000\u0000\u0000\"\u0156\u0001\u0000\u0000"+
		"\u0000$\u016d\u0001\u0000\u0000\u0000&\u0182\u0001\u0000\u0000\u0000("+
		"\u0199\u0001\u0000\u0000\u0000*\u01b5\u0001\u0000\u0000\u0000,\u01b7\u0001"+
		"\u0000\u0000\u0000.\u01b9\u0001\u0000\u0000\u00000\u01c9\u0001\u0000\u0000"+
		"\u00002\u01da\u0001\u0000\u0000\u00004\u01ed\u0001\u0000\u0000\u00006"+
		"\u01ef\u0001\u0000\u0000\u00008\u0200\u0001\u0000\u0000\u0000:\u0219\u0001"+
		"\u0000\u0000\u0000<\u021b\u0001\u0000\u0000\u0000>\u0222\u0001\u0000\u0000"+
		"\u0000@\u0239\u0001\u0000\u0000\u0000B\u024b\u0001\u0000\u0000\u0000D"+
		"\u024d\u0001\u0000\u0000\u0000F\u0264\u0001\u0000\u0000\u0000H\u0280\u0001"+
		"\u0000\u0000\u0000J\u0288\u0001\u0000\u0000\u0000L\u028c\u0001\u0000\u0000"+
		"\u0000N\u02a1\u0001\u0000\u0000\u0000P\u02b1\u0001\u0000\u0000\u0000R"+
		"\u02ce\u0001\u0000\u0000\u0000T\u02d0\u0001\u0000\u0000\u0000V\u02e6\u0001"+
		"\u0000\u0000\u0000X\u02fb\u0001\u0000\u0000\u0000Z\u0311\u0001\u0000\u0000"+
		"\u0000\\\u0329\u0001\u0000\u0000\u0000^\u033f\u0001\u0000\u0000\u0000"+
		"`\u0351\u0001\u0000\u0000\u0000b\u0367\u0001\u0000\u0000\u0000d\u037a"+
		"\u0001\u0000\u0000\u0000f\u038a\u0001\u0000\u0000\u0000h\u038c\u0001\u0000"+
		"\u0000\u0000j\u0390\u0001\u0000\u0000\u0000l\u0398\u0001\u0000\u0000\u0000"+
		"n\u03a0\u0001\u0000\u0000\u0000p\u03a4\u0001\u0000\u0000\u0000r\u03ab"+
		"\u0001\u0000\u0000\u0000t\u03b4\u0001\u0000\u0000\u0000v\u03b6\u0001\u0000"+
		"\u0000\u0000x\u03b8\u0001\u0000\u0000\u0000z\u03c0\u0001\u0000\u0000\u0000"+
		"|\u03c4\u0001\u0000\u0000\u0000~\u03dc\u0001\u0000\u0000\u0000\u0080\u03de"+
		"\u0001\u0000\u0000\u0000\u0082\u03fc\u0001\u0000\u0000\u0000\u0084\u03fe"+
		"\u0001\u0000\u0000\u0000\u0086\u0402\u0001\u0000\u0000\u0000\u0088\u0404"+
		"\u0001\u0000\u0000\u0000\u008a\u0419\u0001\u0000\u0000\u0000\u008c\u0423"+
		"\u0001\u0000\u0000\u0000\u008e\u042e\u0001\u0000\u0000\u0000\u0090\u0439"+
		"\u0001\u0000\u0000\u0000\u0092\u0444\u0001\u0000\u0000\u0000\u0094\u044f"+
		"\u0001\u0000\u0000\u0000\u0096\u0461\u0001\u0000\u0000\u0000\u0098\u046d"+
		"\u0001\u0000\u0000\u0000\u009a\u047b\u0001\u0000\u0000\u0000\u009c\u0489"+
		"\u0001\u0000\u0000\u0000\u009e\u0497\u0001\u0000\u0000\u0000\u00a0\u0499"+
		"\u0001\u0000\u0000\u0000\u00a2\u049f\u0001\u0000\u0000\u0000\u00a4\u04a2"+
		"\u0001\u0000\u0000\u0000\u00a6\u04b5\u0001\u0000\u0000\u0000\u00a8\u04c8"+
		"\u0001\u0000\u0000\u0000\u00aa\u04ca\u0001\u0000\u0000\u0000\u00ac\u04e2"+
		"\u0001\u0000\u0000\u0000\u00ae\u04ec\u0001\u0000\u0000\u0000\u00b0\u04f1"+
		"\u0001\u0000\u0000\u0000\u00b2\u0503\u0001\u0000\u0000\u0000\u00b4\u0515"+
		"\u0001\u0000\u0000\u0000\u00b6\u051a\u0001\u0000\u0000\u0000\u00b8\u052a"+
		"\u0001\u0000\u0000\u0000\u00ba\u0541\u0001\u0000\u0000\u0000\u00bc\u0543"+
		"\u0001\u0000\u0000\u0000\u00be\u0559\u0001\u0000\u0000\u0000\u00c0\u00c2"+
		"\u0003\u0002\u0001\u0000\u00c1\u00c0\u0001\u0000\u0000\u0000\u00c1\u00c2"+
		"\u0001\u0000\u0000\u0000\u00c2\u00c6\u0001\u0000\u0000\u0000\u00c3\u00c5"+
		"\u0003\n\u0005\u0000\u00c4\u00c3\u0001\u0000\u0000\u0000\u00c5\u00c8\u0001"+
		"\u0000\u0000\u0000\u00c6\u00c4\u0001\u0000\u0000\u0000\u00c6\u00c7\u0001"+
		"\u0000\u0000\u0000\u00c7\u00cc\u0001\u0000\u0000\u0000\u00c8\u00c6\u0001"+
		"\u0000\u0000\u0000\u00c9\u00cb\u0003\u0004\u0002\u0000\u00ca\u00c9\u0001"+
		"\u0000\u0000\u0000\u00cb\u00ce\u0001\u0000\u0000\u0000\u00cc\u00ca\u0001"+
		"\u0000\u0000\u0000\u00cc\u00cd\u0001\u0000\u0000\u0000\u00cd\u00d2\u0001"+
		"\u0000\u0000\u0000\u00ce\u00cc\u0001\u0000\u0000\u0000\u00cf\u00d1\u0003"+
		"\n\u0005\u0000\u00d0\u00cf\u0001\u0000\u0000\u0000\u00d1\u00d4\u0001\u0000"+
		"\u0000\u0000\u00d2\u00d0\u0001\u0000\u0000\u0000\u00d2\u00d3\u0001\u0000"+
		"\u0000\u0000\u00d3\u00d8\u0001\u0000\u0000\u0000\u00d4\u00d2\u0001\u0000"+
		"\u0000\u0000\u00d5\u00d7\u0003\u0006\u0003\u0000\u00d6\u00d5\u0001\u0000"+
		"\u0000\u0000\u00d7\u00da\u0001\u0000\u0000\u0000\u00d8\u00d6\u0001\u0000"+
		"\u0000\u0000\u00d8\u00d9\u0001\u0000\u0000\u0000\u00d9\u00de\u0001\u0000"+
		"\u0000\u0000\u00da\u00d8\u0001\u0000\u0000\u0000\u00db\u00dd\u0003\n\u0005"+
		"\u0000\u00dc\u00db\u0001\u0000\u0000\u0000\u00dd\u00e0\u0001\u0000\u0000"+
		"\u0000\u00de\u00dc\u0001\u0000\u0000\u0000\u00de\u00df\u0001\u0000\u0000"+
		"\u0000\u00df\u00e1\u0001\u0000\u0000\u0000\u00e0\u00de\u0001\u0000\u0000"+
		"\u0000\u00e1\u00e2\u0005\u0000\u0000\u0001\u00e2\u0001\u0001\u0000\u0000"+
		"\u0000\u00e3\u00e4\u0005G\u0000\u0000\u00e4\u00e5\u0005A\u0000\u0000\u00e5"+
		"\u00e6\u0005Q\u0000\u0000\u00e6\u0003\u0001\u0000\u0000\u0000\u00e7\u00e8"+
		"\u0005\u0001\u0000\u0000\u00e8\u00e9\u0005D\u0000\u0000\u00e9\u00ea\u0005"+
		"Q\u0000\u0000\u00ea\u0005\u0001\u0000\u0000\u0000\u00eb\u00f3\u0003\u001e"+
		"\u000f\u0000\u00ec\u00f3\u0003@ \u0000\u00ed\u00f3\u0003N\'\u0000\u00ee"+
		"\u00f3\u0003.\u0017\u0000\u00ef\u00f3\u00032\u0019\u0000\u00f0\u00f3\u0003"+
		"\u00a6S\u0000\u00f1\u00f3\u0003\u00b6[\u0000\u00f2\u00eb\u0001\u0000\u0000"+
		"\u0000\u00f2\u00ec\u0001\u0000\u0000\u0000\u00f2\u00ed\u0001\u0000\u0000"+
		"\u0000\u00f2\u00ee\u0001\u0000\u0000\u0000\u00f2\u00ef\u0001\u0000\u0000"+
		"\u0000\u00f2\u00f0\u0001\u0000\u0000\u0000\u00f2\u00f1\u0001\u0000\u0000"+
		"\u0000\u00f3\u0007\u0001\u0000\u0000\u0000\u00f4\u00f5\u0007\u0000\u0000"+
		"\u0000\u00f5\t\u0001\u0000\u0000\u0000\u00f6\u00f7\u0005G\u0000\u0000"+
		"\u00f7\u00f8\u0005@\u0000\u0000\u00f8\u00f9\u0005I\u0000\u0000\u00f9\u00fa"+
		"\u0005B\u0000\u0000\u00fa\u0108\u0005J\u0000\u0000\u00fb\u00fc\u0005H"+
		"\u0000\u0000\u00fc\u00fd\u0003\b\u0004\u0000\u00fd\u00ff\u0005I\u0000"+
		"\u0000\u00fe\u0100\u0003\f\u0006\u0000\u00ff\u00fe\u0001\u0000\u0000\u0000"+
		"\u00ff\u0100\u0001\u0000\u0000\u0000\u0100\u0101\u0001\u0000\u0000\u0000"+
		"\u0101\u0102\u0005J\u0000\u0000\u0102\u0108\u0001\u0000\u0000\u0000\u0103"+
		"\u0104\u0005H\u0000\u0000\u0104\u0105\u0003\b\u0004\u0000\u0105\u0106"+
		"\u0005Q\u0000\u0000\u0106\u0108\u0001\u0000\u0000\u0000\u0107\u00f6\u0001"+
		"\u0000\u0000\u0000\u0107\u00fb\u0001\u0000\u0000\u0000\u0107\u0103\u0001"+
		"\u0000\u0000\u0000\u0108\u000b\u0001\u0000\u0000\u0000\u0109\u010a\u0004"+
		"\u0006\u0000\u0000\u010a\u010d\u0003\u0010\b\u0000\u010b\u010d\u0003\u000e"+
		"\u0007\u0000\u010c\u0109\u0001\u0000\u0000\u0000\u010c\u010b\u0001\u0000"+
		"\u0000\u0000\u010d\r\u0001\u0000\u0000\u0000\u010e\u0113\u0003\u0014\n"+
		"\u0000\u010f\u0113\u0003\u0016\u000b\u0000\u0110\u0113\u0003\u0018\f\u0000"+
		"\u0111\u0113\u0003\u001a\r\u0000\u0112\u010e\u0001\u0000\u0000\u0000\u0112"+
		"\u010f\u0001\u0000\u0000\u0000\u0112\u0110\u0001\u0000\u0000\u0000\u0112"+
		"\u0111\u0001\u0000\u0000\u0000\u0113\u000f\u0001\u0000\u0000\u0000\u0114"+
		"\u0119\u0003\u0012\t\u0000\u0115\u0116\u0005S\u0000\u0000\u0116\u0118"+
		"\u0003\u0012\t\u0000\u0117\u0115\u0001\u0000\u0000\u0000\u0118\u011b\u0001"+
		"\u0000\u0000\u0000\u0119\u0117\u0001\u0000\u0000\u0000\u0119\u011a\u0001"+
		"\u0000\u0000\u0000\u011a\u0011\u0001\u0000\u0000\u0000\u011b\u0119\u0001"+
		"\u0000\u0000\u0000\u011c\u011d\u0005@\u0000\u0000\u011d\u0122\u0005R\u0000"+
		"\u0000\u011e\u0123\u0003\u0014\n\u0000\u011f\u0123\u0003\u0016\u000b\u0000"+
		"\u0120\u0123\u0003\u0018\f\u0000\u0121\u0123\u0003\u001a\r\u0000\u0122"+
		"\u011e\u0001\u0000\u0000\u0000\u0122\u011f\u0001\u0000\u0000\u0000\u0122"+
		"\u0120\u0001\u0000\u0000\u0000\u0122\u0121\u0001\u0000\u0000\u0000\u0123"+
		"\u0013\u0001\u0000\u0000\u0000\u0124\u0125\u0007\u0001\u0000\u0000\u0125"+
		"\u0015\u0001\u0000\u0000\u0000\u0126\u012b\u0005@\u0000\u0000\u0127\u0128"+
		"\u0005T\u0000\u0000\u0128\u012a\u0005@\u0000\u0000\u0129\u0127\u0001\u0000"+
		"\u0000\u0000\u012a\u012d\u0001\u0000\u0000\u0000\u012b\u0129\u0001\u0000"+
		"\u0000\u0000\u012b\u012c\u0001\u0000\u0000\u0000\u012c\u0017\u0001\u0000"+
		"\u0000\u0000\u012d\u012b\u0001\u0000\u0000\u0000\u012e\u0130\u0005K\u0000"+
		"\u0000\u012f\u0131\u0003\u0010\b\u0000\u0130\u012f\u0001\u0000\u0000\u0000"+
		"\u0130\u0131\u0001\u0000\u0000\u0000\u0131\u0132\u0001\u0000\u0000\u0000"+
		"\u0132\u0133\u0005L\u0000\u0000\u0133\u0019\u0001\u0000\u0000\u0000\u0134"+
		"\u0136\u0005M\u0000\u0000\u0135\u0137\u0003\u001c\u000e\u0000\u0136\u0135"+
		"\u0001\u0000\u0000\u0000\u0136\u0137\u0001\u0000\u0000\u0000\u0137\u0138"+
		"\u0001\u0000\u0000\u0000\u0138\u0139\u0005N\u0000\u0000\u0139\u001b\u0001"+
		"\u0000\u0000\u0000\u013a\u013f\u0003\u000e\u0007\u0000\u013b\u013c\u0005"+
		"S\u0000\u0000\u013c\u013e\u0003\u000e\u0007\u0000\u013d\u013b\u0001\u0000"+
		"\u0000\u0000\u013e\u0141\u0001\u0000\u0000\u0000\u013f\u013d\u0001\u0000"+
		"\u0000\u0000\u013f\u0140\u0001\u0000\u0000\u0000\u0140\u001d\u0001\u0000"+
		"\u0000\u0000\u0141\u013f\u0001\u0000\u0000\u0000\u0142\u0143\u0005\u0002"+
		"\u0000\u0000\u0143\u0147\u0005K\u0000\u0000\u0144\u0146\u0003\n\u0005"+
		"\u0000\u0145\u0144\u0001\u0000\u0000\u0000\u0146\u0149\u0001\u0000\u0000"+
		"\u0000\u0147\u0145\u0001\u0000\u0000\u0000\u0147\u0148\u0001\u0000\u0000"+
		"\u0000\u0148\u014d\u0001\u0000\u0000\u0000\u0149\u0147\u0001\u0000\u0000"+
		"\u0000\u014a\u014c\u0003 \u0010\u0000\u014b\u014a\u0001\u0000\u0000\u0000"+
		"\u014c\u014f\u0001\u0000\u0000\u0000\u014d\u014b\u0001\u0000\u0000\u0000"+
		"\u014d\u014e\u0001\u0000\u0000\u0000\u014e\u0150\u0001\u0000\u0000\u0000"+
		"\u014f\u014d\u0001\u0000\u0000\u0000\u0150\u0151\u0005L\u0000\u0000\u0151"+
		"\u001f\u0001\u0000\u0000\u0000\u0152\u0155\u0003\"\u0011\u0000\u0153\u0155"+
		"\u0003&\u0013\u0000\u0154\u0152\u0001\u0000\u0000\u0000\u0154\u0153\u0001"+
		"\u0000\u0000\u0000\u0155!\u0001\u0000\u0000\u0000\u0156\u0157\u0005\t"+
		"\u0000\u0000\u0157\u015b\u0005@\u0000\u0000\u0158\u015a\u0003\n\u0005"+
		"\u0000\u0159\u0158\u0001\u0000\u0000\u0000\u015a\u015d\u0001\u0000\u0000"+
		"\u0000\u015b\u0159\u0001\u0000\u0000\u0000\u015b\u015c\u0001\u0000\u0000"+
		"\u0000\u015c\u015e\u0001\u0000\u0000\u0000\u015d\u015b\u0001\u0000\u0000"+
		"\u0000\u015e\u0162\u0005K\u0000\u0000\u015f\u0161\u0003\n\u0005\u0000"+
		"\u0160\u015f\u0001\u0000\u0000\u0000\u0161\u0164\u0001\u0000\u0000\u0000"+
		"\u0162\u0160\u0001\u0000\u0000\u0000\u0162\u0163\u0001\u0000\u0000\u0000"+
		"\u0163\u0168\u0001\u0000\u0000\u0000\u0164\u0162\u0001\u0000\u0000\u0000"+
		"\u0165\u0167\u0003$\u0012\u0000\u0166\u0165\u0001\u0000\u0000\u0000\u0167"+
		"\u016a\u0001\u0000\u0000\u0000\u0168\u0166\u0001\u0000\u0000\u0000\u0168"+
		"\u0169\u0001\u0000\u0000\u0000\u0169\u016b\u0001\u0000\u0000\u0000\u016a"+
		"\u0168\u0001\u0000\u0000\u0000\u016b\u016c\u0005L\u0000\u0000\u016c#\u0001"+
		"\u0000\u0000\u0000\u016d\u016e\u0005@\u0000\u0000\u016e\u016f\u0005R\u0000"+
		"\u0000\u016f\u0173\u0003*\u0015\u0000\u0170\u0172\u0003\n\u0005\u0000"+
		"\u0171\u0170\u0001\u0000\u0000\u0000\u0172\u0175\u0001\u0000\u0000\u0000"+
		"\u0173\u0171\u0001\u0000\u0000\u0000\u0173\u0174\u0001\u0000\u0000\u0000"+
		"\u0174\u017e\u0001\u0000\u0000\u0000\u0175\u0173\u0001\u0000\u0000\u0000"+
		"\u0176\u017a\u0005K\u0000\u0000\u0177\u0179\u0003\n\u0005\u0000\u0178"+
		"\u0177\u0001\u0000\u0000\u0000\u0179\u017c\u0001\u0000\u0000\u0000\u017a"+
		"\u0178\u0001\u0000\u0000\u0000\u017a\u017b\u0001\u0000\u0000\u0000\u017b"+
		"\u017d\u0001\u0000\u0000\u0000\u017c\u017a\u0001\u0000\u0000\u0000\u017d"+
		"\u017f\u0005L\u0000\u0000\u017e\u0176\u0001\u0000\u0000\u0000\u017e\u017f"+
		"\u0001\u0000\u0000\u0000\u017f\u0180\u0001\u0000\u0000\u0000\u0180\u0181"+
		"\u0005Q\u0000\u0000\u0181%\u0001\u0000\u0000\u0000\u0182\u0183\u0005\n"+
		"\u0000\u0000\u0183\u0187\u0005@\u0000\u0000\u0184\u0186\u0003\n\u0005"+
		"\u0000\u0185\u0184\u0001\u0000\u0000\u0000\u0186\u0189\u0001\u0000\u0000"+
		"\u0000\u0187\u0185\u0001\u0000\u0000\u0000\u0187\u0188\u0001\u0000\u0000"+
		"\u0000\u0188\u018a\u0001\u0000\u0000\u0000\u0189\u0187\u0001\u0000\u0000"+
		"\u0000\u018a\u018e\u0005K\u0000\u0000\u018b\u018d\u0003\n\u0005\u0000"+
		"\u018c\u018b\u0001\u0000\u0000\u0000\u018d\u0190\u0001\u0000\u0000\u0000"+
		"\u018e\u018c\u0001\u0000\u0000\u0000\u018e\u018f\u0001\u0000\u0000\u0000"+
		"\u018f\u0194\u0001\u0000\u0000\u0000\u0190\u018e\u0001\u0000\u0000\u0000"+
		"\u0191\u0193\u0003(\u0014\u0000\u0192\u0191\u0001\u0000\u0000\u0000\u0193"+
		"\u0196\u0001\u0000\u0000\u0000\u0194\u0192\u0001\u0000\u0000\u0000\u0194"+
		"\u0195\u0001\u0000\u0000\u0000\u0195\u0197\u0001\u0000\u0000\u0000\u0196"+
		"\u0194\u0001\u0000\u0000\u0000\u0197\u0198\u0005L\u0000\u0000\u0198\'"+
		"\u0001\u0000\u0000\u0000\u0199\u019d\u0005@\u0000\u0000\u019a\u019c\u0003"+
		"\n\u0005\u0000\u019b\u019a\u0001\u0000\u0000\u0000\u019c\u019f\u0001\u0000"+
		"\u0000\u0000\u019d\u019b\u0001\u0000\u0000\u0000\u019d\u019e\u0001\u0000"+
		"\u0000\u0000\u019e\u01a0\u0001\u0000\u0000\u0000\u019f\u019d\u0001\u0000"+
		"\u0000\u0000\u01a0\u01a1\u0005Q\u0000\u0000\u01a1)\u0001\u0000\u0000\u0000"+
		"\u01a2\u01b6\u0003,\u0016\u0000\u01a3\u01b6\u0003\u0016\u000b\u0000\u01a4"+
		"\u01a5\u0005=\u0000\u0000\u01a5\u01a6\u0005O\u0000\u0000\u01a6\u01a7\u0003"+
		"*\u0015\u0000\u01a7\u01a8\u0005P\u0000\u0000\u01a8\u01b6\u0001\u0000\u0000"+
		"\u0000\u01a9\u01aa\u0005>\u0000\u0000\u01aa\u01ab\u0005O\u0000\u0000\u01ab"+
		"\u01ac\u0003*\u0015\u0000\u01ac\u01ad\u0005P\u0000\u0000\u01ad\u01b6\u0001"+
		"\u0000\u0000\u0000\u01ae\u01af\u0005?\u0000\u0000\u01af\u01b0\u0005O\u0000"+
		"\u0000\u01b0\u01b1\u0003*\u0015\u0000\u01b1\u01b2\u0005S\u0000\u0000\u01b2"+
		"\u01b3\u0003*\u0015\u0000\u01b3\u01b4\u0005P\u0000\u0000\u01b4\u01b6\u0001"+
		"\u0000\u0000\u0000\u01b5\u01a2\u0001\u0000\u0000\u0000\u01b5\u01a3\u0001"+
		"\u0000\u0000\u0000\u01b5\u01a4\u0001\u0000\u0000\u0000\u01b5\u01a9\u0001"+
		"\u0000\u0000\u0000\u01b5\u01ae\u0001\u0000\u0000\u0000\u01b6+\u0001\u0000"+
		"\u0000\u0000\u01b7\u01b8\u0007\u0002\u0000\u0000\u01b8-\u0001\u0000\u0000"+
		"\u0000\u01b9\u01ba\u0005\u0003\u0000\u0000\u01ba\u01be\u0005K\u0000\u0000"+
		"\u01bb\u01bd\u0003\n\u0005\u0000\u01bc\u01bb\u0001\u0000\u0000\u0000\u01bd"+
		"\u01c0\u0001\u0000\u0000\u0000\u01be\u01bc\u0001\u0000\u0000\u0000\u01be"+
		"\u01bf\u0001\u0000\u0000\u0000\u01bf\u01c4\u0001\u0000\u0000\u0000\u01c0"+
		"\u01be\u0001\u0000\u0000\u0000\u01c1\u01c3\u00030\u0018\u0000\u01c2\u01c1"+
		"\u0001\u0000\u0000\u0000\u01c3\u01c6\u0001\u0000\u0000\u0000\u01c4\u01c2"+
		"\u0001\u0000\u0000\u0000\u01c4\u01c5\u0001\u0000\u0000\u0000\u01c5\u01c7"+
		"\u0001\u0000\u0000\u0000\u01c6\u01c4\u0001\u0000\u0000\u0000\u01c7\u01c8"+
		"\u0005L\u0000\u0000\u01c8/\u0001\u0000\u0000\u0000\u01c9\u01ca\u0005\u001b"+
		"\u0000\u0000\u01ca\u01ce\u0005@\u0000\u0000\u01cb\u01cd\u0003\n\u0005"+
		"\u0000\u01cc\u01cb\u0001\u0000\u0000\u0000\u01cd\u01d0\u0001\u0000\u0000"+
		"\u0000\u01ce\u01cc\u0001\u0000\u0000\u0000\u01ce\u01cf\u0001\u0000\u0000"+
		"\u0000\u01cf\u01d1\u0001\u0000\u0000\u0000\u01d0\u01ce\u0001\u0000\u0000"+
		"\u0000\u01d1\u01d5\u0005K\u0000\u0000\u01d2\u01d4\u0003\n\u0005\u0000"+
		"\u01d3\u01d2\u0001\u0000\u0000\u0000\u01d4\u01d7\u0001\u0000\u0000\u0000"+
		"\u01d5\u01d3\u0001\u0000\u0000\u0000\u01d5\u01d6\u0001\u0000\u0000\u0000"+
		"\u01d6\u01d8\u0001\u0000\u0000\u0000\u01d7\u01d5\u0001\u0000\u0000\u0000"+
		"\u01d8\u01d9\u0005L\u0000\u0000\u01d91\u0001\u0000\u0000\u0000\u01da\u01db"+
		"\u0005\u0004\u0000\u0000\u01db\u01df\u0005K\u0000\u0000\u01dc\u01de\u0003"+
		"\n\u0005\u0000\u01dd\u01dc\u0001\u0000\u0000\u0000\u01de\u01e1\u0001\u0000"+
		"\u0000\u0000\u01df\u01dd\u0001\u0000\u0000\u0000\u01df\u01e0\u0001\u0000"+
		"\u0000\u0000\u01e0\u01e5\u0001\u0000\u0000\u0000\u01e1\u01df\u0001\u0000"+
		"\u0000\u0000\u01e2\u01e4\u00034\u001a\u0000\u01e3\u01e2\u0001\u0000\u0000"+
		"\u0000\u01e4\u01e7\u0001\u0000\u0000\u0000\u01e5\u01e3\u0001\u0000\u0000"+
		"\u0000\u01e5\u01e6\u0001\u0000\u0000\u0000\u01e6\u01e8\u0001\u0000\u0000"+
		"\u0000\u01e7\u01e5\u0001\u0000\u0000\u0000\u01e8\u01e9\u0005L\u0000\u0000"+
		"\u01e93\u0001\u0000\u0000\u0000\u01ea\u01ee\u00036\u001b\u0000\u01eb\u01ee"+
		"\u00038\u001c\u0000\u01ec\u01ee\u0003>\u001f\u0000\u01ed\u01ea\u0001\u0000"+
		"\u0000\u0000\u01ed\u01eb\u0001\u0000\u0000\u0000\u01ed\u01ec\u0001\u0000"+
		"\u0000\u0000\u01ee5\u0001\u0000\u0000\u0000\u01ef\u01f0\u0005\u001a\u0000"+
		"\u0000\u01f0\u01f4\u0005@\u0000\u0000\u01f1\u01f3\u0003\n\u0005\u0000"+
		"\u01f2\u01f1\u0001\u0000\u0000\u0000\u01f3\u01f6\u0001\u0000\u0000\u0000"+
		"\u01f4\u01f2\u0001\u0000\u0000\u0000\u01f4\u01f5\u0001\u0000\u0000\u0000"+
		"\u01f5\u01f7\u0001\u0000\u0000\u0000\u01f6\u01f4\u0001\u0000\u0000\u0000"+
		"\u01f7\u01fb\u0005K\u0000\u0000\u01f8\u01fa\u0003\n\u0005\u0000\u01f9"+
		"\u01f8\u0001\u0000\u0000\u0000\u01fa\u01fd\u0001\u0000\u0000\u0000\u01fb"+
		"\u01f9\u0001\u0000\u0000\u0000\u01fb\u01fc\u0001\u0000\u0000\u0000\u01fc"+
		"\u01fe\u0001\u0000\u0000\u0000\u01fd\u01fb\u0001\u0000\u0000\u0000\u01fe"+
		"\u01ff\u0005L\u0000\u0000\u01ff7\u0001\u0000\u0000\u0000\u0200\u0201\u0005"+
		"\u001c\u0000\u0000\u0201\u0205\u0005@\u0000\u0000\u0202\u0204\u0003\n"+
		"\u0005\u0000\u0203\u0202\u0001\u0000\u0000\u0000\u0204\u0207\u0001\u0000"+
		"\u0000\u0000\u0205\u0203\u0001\u0000\u0000\u0000\u0205\u0206\u0001\u0000"+
		"\u0000\u0000\u0206\u0208\u0001\u0000\u0000\u0000\u0207\u0205\u0001\u0000"+
		"\u0000\u0000\u0208\u020c\u0005K\u0000\u0000\u0209\u020b\u0003\n\u0005"+
		"\u0000\u020a\u0209\u0001\u0000\u0000\u0000\u020b\u020e\u0001\u0000\u0000"+
		"\u0000\u020c\u020a\u0001\u0000\u0000\u0000\u020c\u020d\u0001\u0000\u0000"+
		"\u0000\u020d\u0212\u0001\u0000\u0000\u0000\u020e\u020c\u0001\u0000\u0000"+
		"\u0000\u020f\u0211\u0003:\u001d\u0000\u0210\u020f\u0001\u0000\u0000\u0000"+
		"\u0211\u0214\u0001\u0000\u0000\u0000\u0212\u0210\u0001\u0000\u0000\u0000"+
		"\u0212\u0213\u0001\u0000\u0000\u0000\u0213\u0215\u0001\u0000\u0000\u0000"+
		"\u0214\u0212\u0001\u0000\u0000\u0000\u0215\u0216\u0005L\u0000\u0000\u0216"+
		"9\u0001\u0000\u0000\u0000\u0217\u021a\u0003\n\u0005\u0000\u0218\u021a"+
		"\u0003<\u001e\u0000\u0219\u0217\u0001\u0000\u0000\u0000\u0219\u0218\u0001"+
		"\u0000\u0000\u0000\u021a;\u0001\u0000\u0000\u0000\u021b\u021c\u0005%\u0000"+
		"\u0000\u021c\u021e\u0005K\u0000\u0000\u021d\u021f\u0003\u0010\b\u0000"+
		"\u021e\u021d\u0001\u0000\u0000\u0000\u021e\u021f\u0001\u0000\u0000\u0000"+
		"\u021f\u0220\u0001\u0000\u0000\u0000\u0220\u0221\u0005L\u0000\u0000\u0221"+
		"=\u0001\u0000\u0000\u0000\u0222\u0223\u0005\u001d\u0000\u0000\u0223\u0227"+
		"\u0005@\u0000\u0000\u0224\u0226\u0003\n\u0005\u0000\u0225\u0224\u0001"+
		"\u0000\u0000\u0000\u0226\u0229\u0001\u0000\u0000\u0000\u0227\u0225\u0001"+
		"\u0000\u0000\u0000\u0227\u0228\u0001\u0000\u0000\u0000\u0228\u022a\u0001"+
		"\u0000\u0000\u0000\u0229\u0227\u0001\u0000\u0000\u0000\u022a\u022e\u0005"+
		"K\u0000\u0000\u022b\u022d\u0003\n\u0005\u0000\u022c\u022b\u0001\u0000"+
		"\u0000\u0000\u022d\u0230\u0001\u0000\u0000\u0000\u022e\u022c\u0001\u0000"+
		"\u0000\u0000\u022e\u022f\u0001\u0000\u0000\u0000\u022f\u0234\u0001\u0000"+
		"\u0000\u0000\u0230\u022e\u0001\u0000\u0000\u0000\u0231\u0233\u0003$\u0012"+
		"\u0000\u0232\u0231\u0001\u0000\u0000\u0000\u0233\u0236\u0001\u0000\u0000"+
		"\u0000\u0234\u0232\u0001\u0000\u0000\u0000\u0234\u0235\u0001\u0000\u0000"+
		"\u0000\u0235\u0237\u0001\u0000\u0000\u0000\u0236\u0234\u0001\u0000\u0000"+
		"\u0000\u0237\u0238\u0005L\u0000\u0000\u0238?\u0001\u0000\u0000\u0000\u0239"+
		"\u023a\u0005\u0005\u0000\u0000\u023a\u023e\u0005K\u0000\u0000\u023b\u023d"+
		"\u0003\n\u0005\u0000\u023c\u023b\u0001\u0000\u0000\u0000\u023d\u0240\u0001"+
		"\u0000\u0000\u0000\u023e\u023c\u0001\u0000\u0000\u0000\u023e\u023f\u0001"+
		"\u0000\u0000\u0000\u023f\u0244\u0001\u0000\u0000\u0000\u0240\u023e\u0001"+
		"\u0000\u0000\u0000\u0241\u0243\u0003B!\u0000\u0242\u0241\u0001\u0000\u0000"+
		"\u0000\u0243\u0246\u0001\u0000\u0000\u0000\u0244\u0242\u0001\u0000\u0000"+
		"\u0000\u0244\u0245\u0001\u0000\u0000\u0000\u0245\u0247\u0001\u0000\u0000"+
		"\u0000\u0246\u0244\u0001\u0000\u0000\u0000\u0247\u0248\u0005L\u0000\u0000"+
		"\u0248A\u0001\u0000\u0000\u0000\u0249\u024c\u0003D\"\u0000\u024a\u024c"+
		"\u0003L&\u0000\u024b\u0249\u0001\u0000\u0000\u0000\u024b\u024a\u0001\u0000"+
		"\u0000\u0000\u024cC\u0001\u0000\u0000\u0000\u024d\u024e\u0005\u0019\u0000"+
		"\u0000\u024e\u0252\u0005@\u0000\u0000\u024f\u0251\u0003\n\u0005\u0000"+
		"\u0250\u024f\u0001\u0000\u0000\u0000\u0251\u0254\u0001\u0000\u0000\u0000"+
		"\u0252\u0250\u0001\u0000\u0000\u0000\u0252\u0253\u0001\u0000\u0000\u0000"+
		"\u0253\u0255\u0001\u0000\u0000\u0000\u0254\u0252\u0001\u0000\u0000\u0000"+
		"\u0255\u0259\u0005K\u0000\u0000\u0256\u0258\u0003\n\u0005\u0000\u0257"+
		"\u0256\u0001\u0000\u0000\u0000\u0258\u025b\u0001\u0000\u0000\u0000\u0259"+
		"\u0257\u0001\u0000\u0000\u0000\u0259\u025a\u0001\u0000\u0000\u0000\u025a"+
		"\u025f\u0001\u0000\u0000\u0000\u025b\u0259\u0001\u0000\u0000\u0000\u025c"+
		"\u025e\u0003F#\u0000\u025d\u025c\u0001\u0000\u0000\u0000\u025e\u0261\u0001"+
		"\u0000\u0000\u0000\u025f\u025d\u0001\u0000\u0000\u0000\u025f\u0260\u0001"+
		"\u0000\u0000\u0000\u0260\u0262\u0001\u0000\u0000\u0000\u0261\u025f\u0001"+
		"\u0000\u0000\u0000\u0262\u0263\u0005L\u0000\u0000\u0263E\u0001\u0000\u0000"+
		"\u0000\u0264\u0268\u0005@\u0000\u0000\u0265\u0267\u0003\n\u0005\u0000"+
		"\u0266\u0265\u0001\u0000\u0000\u0000\u0267\u026a\u0001\u0000\u0000\u0000"+
		"\u0268\u0266\u0001\u0000\u0000\u0000\u0268\u0269\u0001\u0000\u0000\u0000"+
		"\u0269\u026b\u0001\u0000\u0000\u0000\u026a\u0268\u0001\u0000\u0000\u0000"+
		"\u026b\u026d\u0005I\u0000\u0000\u026c\u026e\u0003H$\u0000\u026d\u026c"+
		"\u0001\u0000\u0000\u0000\u026d\u026e\u0001\u0000\u0000\u0000\u026e\u026f"+
		"\u0001\u0000\u0000\u0000\u026f\u0272\u0005J\u0000\u0000\u0270\u0271\u0005"+
		"U\u0000\u0000\u0271\u0273\u0003*\u0015\u0000\u0272\u0270\u0001\u0000\u0000"+
		"\u0000\u0272\u0273\u0001\u0000\u0000\u0000\u0273\u027c\u0001\u0000\u0000"+
		"\u0000\u0274\u0278\u0005K\u0000\u0000\u0275\u0277\u0003\n\u0005\u0000"+
		"\u0276\u0275\u0001\u0000\u0000\u0000\u0277\u027a\u0001\u0000\u0000\u0000"+
		"\u0278\u0276\u0001\u0000\u0000\u0000\u0278\u0279\u0001\u0000\u0000\u0000"+
		"\u0279\u027b\u0001\u0000\u0000\u0000\u027a\u0278\u0001\u0000\u0000\u0000"+
		"\u027b\u027d\u0005L\u0000\u0000\u027c\u0274\u0001\u0000\u0000\u0000\u027c"+
		"\u027d\u0001\u0000\u0000\u0000\u027d\u027e\u0001\u0000\u0000\u0000\u027e"+
		"\u027f\u0005Q\u0000\u0000\u027fG\u0001\u0000\u0000\u0000\u0280\u0285\u0003"+
		"J%\u0000\u0281\u0282\u0005S\u0000\u0000\u0282\u0284\u0003J%\u0000\u0283"+
		"\u0281\u0001\u0000\u0000\u0000\u0284\u0287\u0001\u0000\u0000\u0000\u0285"+
		"\u0283\u0001\u0000\u0000\u0000\u0285\u0286\u0001\u0000\u0000\u0000\u0286"+
		"I\u0001\u0000\u0000\u0000\u0287\u0285\u0001\u0000\u0000\u0000\u0288\u0289"+
		"\u0005@\u0000\u0000\u0289\u028a\u0005R\u0000\u0000\u028a\u028b\u0003*"+
		"\u0015\u0000\u028bK\u0001\u0000\u0000\u0000\u028c\u028d\u0005\u0018\u0000"+
		"\u0000\u028d\u0291\u0005@\u0000\u0000\u028e\u0290\u0003\n\u0005\u0000"+
		"\u028f\u028e\u0001\u0000\u0000\u0000\u0290\u0293\u0001\u0000\u0000\u0000"+
		"\u0291\u028f\u0001\u0000\u0000\u0000\u0291\u0292\u0001\u0000\u0000\u0000"+
		"\u0292\u0296\u0001\u0000\u0000\u0000\u0293\u0291\u0001\u0000\u0000\u0000"+
		"\u0294\u0295\u0005\u001e\u0000\u0000\u0295\u0297\u0003\u0016\u000b\u0000"+
		"\u0296\u0294\u0001\u0000\u0000\u0000\u0296\u0297\u0001\u0000\u0000\u0000"+
		"\u0297\u0298\u0001\u0000\u0000\u0000\u0298\u029c\u0005K\u0000\u0000\u0299"+
		"\u029b\u0003\n\u0005\u0000\u029a\u0299\u0001\u0000\u0000\u0000\u029b\u029e"+
		"\u0001\u0000\u0000\u0000\u029c\u029a\u0001\u0000\u0000\u0000\u029c\u029d"+
		"\u0001\u0000\u0000\u0000\u029d\u029f\u0001\u0000\u0000\u0000\u029e\u029c"+
		"\u0001\u0000\u0000\u0000\u029f\u02a0\u0005L\u0000\u0000\u02a0M\u0001\u0000"+
		"\u0000\u0000\u02a1\u02a2\u0005\u0006\u0000\u0000\u02a2\u02a6\u0005K\u0000"+
		"\u0000\u02a3\u02a5\u0003\n\u0005\u0000\u02a4\u02a3\u0001\u0000\u0000\u0000"+
		"\u02a5\u02a8\u0001\u0000\u0000\u0000\u02a6\u02a4\u0001\u0000\u0000\u0000"+
		"\u02a6\u02a7\u0001\u0000\u0000\u0000\u02a7\u02ac\u0001\u0000\u0000\u0000"+
		"\u02a8\u02a6\u0001\u0000\u0000\u0000\u02a9\u02ab\u0003P(\u0000\u02aa\u02a9"+
		"\u0001\u0000\u0000\u0000\u02ab\u02ae\u0001\u0000\u0000\u0000\u02ac\u02aa"+
		"\u0001\u0000\u0000\u0000\u02ac\u02ad\u0001\u0000\u0000\u0000\u02ad\u02af"+
		"\u0001\u0000\u0000\u0000\u02ae\u02ac\u0001\u0000\u0000\u0000\u02af\u02b0"+
		"\u0005L\u0000\u0000\u02b0O\u0001\u0000\u0000\u0000\u02b1\u02b2\u0005\u000b"+
		"\u0000\u0000\u02b2\u02b6\u0005@\u0000\u0000\u02b3\u02b5\u0003\n\u0005"+
		"\u0000\u02b4\u02b3\u0001\u0000\u0000\u0000\u02b5\u02b8\u0001\u0000\u0000"+
		"\u0000\u02b6\u02b4\u0001\u0000\u0000\u0000\u02b6\u02b7\u0001\u0000\u0000"+
		"\u0000\u02b7\u02b9\u0001\u0000\u0000\u0000\u02b8\u02b6\u0001\u0000\u0000"+
		"\u0000\u02b9\u02bd\u0005K\u0000\u0000\u02ba\u02bc\u0003\n\u0005\u0000"+
		"\u02bb\u02ba\u0001\u0000\u0000\u0000\u02bc\u02bf\u0001\u0000\u0000\u0000"+
		"\u02bd\u02bb\u0001\u0000\u0000\u0000\u02bd\u02be\u0001\u0000\u0000\u0000"+
		"\u02be\u02c3\u0001\u0000\u0000\u0000\u02bf\u02bd\u0001\u0000\u0000\u0000"+
		"\u02c0\u02c2\u0003R)\u0000\u02c1\u02c0\u0001\u0000\u0000\u0000\u02c2\u02c5"+
		"\u0001\u0000\u0000\u0000\u02c3\u02c1\u0001\u0000\u0000\u0000\u02c3\u02c4"+
		"\u0001\u0000\u0000\u0000\u02c4\u02c6\u0001\u0000\u0000\u0000\u02c5\u02c3"+
		"\u0001\u0000\u0000\u0000\u02c6\u02c7\u0005L\u0000\u0000\u02c7Q\u0001\u0000"+
		"\u0000\u0000\u02c8\u02cf\u0003T*\u0000\u02c9\u02cf\u0003X,\u0000\u02ca"+
		"\u02cf\u0003\\.\u0000\u02cb\u02cf\u0003`0\u0000\u02cc\u02cf\u0003|>\u0000"+
		"\u02cd\u02cf\u0003\n\u0005\u0000\u02ce\u02c8\u0001\u0000\u0000\u0000\u02ce"+
		"\u02c9\u0001\u0000\u0000\u0000\u02ce\u02ca\u0001\u0000\u0000\u0000\u02ce"+
		"\u02cb\u0001\u0000\u0000\u0000\u02ce\u02cc\u0001\u0000\u0000\u0000\u02ce"+
		"\u02cd\u0001\u0000\u0000\u0000\u02cfS\u0001\u0000\u0000\u0000\u02d0\u02d4"+
		"\u0005\f\u0000\u0000\u02d1\u02d3\u0003\n\u0005\u0000\u02d2\u02d1\u0001"+
		"\u0000\u0000\u0000\u02d3\u02d6\u0001\u0000\u0000\u0000\u02d4\u02d2\u0001"+
		"\u0000\u0000\u0000\u02d4\u02d5\u0001\u0000\u0000\u0000\u02d5\u02d7\u0001"+
		"\u0000\u0000\u0000\u02d6\u02d4\u0001\u0000\u0000\u0000\u02d7\u02db\u0005"+
		"K\u0000\u0000\u02d8\u02da\u0003\n\u0005\u0000\u02d9\u02d8\u0001\u0000"+
		"\u0000\u0000\u02da\u02dd\u0001\u0000\u0000\u0000\u02db\u02d9\u0001\u0000"+
		"\u0000\u0000\u02db\u02dc\u0001\u0000\u0000\u0000\u02dc\u02e1\u0001\u0000"+
		"\u0000\u0000\u02dd\u02db\u0001\u0000\u0000\u0000\u02de\u02e0\u0003V+\u0000"+
		"\u02df\u02de\u0001\u0000\u0000\u0000\u02e0\u02e3\u0001\u0000\u0000\u0000"+
		"\u02e1\u02df\u0001\u0000\u0000\u0000\u02e1\u02e2\u0001\u0000\u0000\u0000"+
		"\u02e2\u02e4\u0001\u0000\u0000\u0000\u02e3\u02e1\u0001\u0000\u0000\u0000"+
		"\u02e4\u02e5\u0005L\u0000\u0000\u02e5U\u0001\u0000\u0000\u0000\u02e6\u02e7"+
		"\u0005@\u0000\u0000\u02e7\u02e8\u0005R\u0000\u0000\u02e8\u02ec\u0003*"+
		"\u0015\u0000\u02e9\u02eb\u0003\n\u0005\u0000\u02ea\u02e9\u0001\u0000\u0000"+
		"\u0000\u02eb\u02ee\u0001\u0000\u0000\u0000\u02ec\u02ea\u0001\u0000\u0000"+
		"\u0000\u02ec\u02ed\u0001\u0000\u0000\u0000\u02ed\u02f7\u0001\u0000\u0000"+
		"\u0000\u02ee\u02ec\u0001\u0000\u0000\u0000\u02ef\u02f3\u0005K\u0000\u0000"+
		"\u02f0\u02f2\u0003\n\u0005\u0000\u02f1\u02f0\u0001\u0000\u0000\u0000\u02f2"+
		"\u02f5\u0001\u0000\u0000\u0000\u02f3\u02f1\u0001\u0000\u0000\u0000\u02f3"+
		"\u02f4\u0001\u0000\u0000\u0000\u02f4\u02f6\u0001\u0000\u0000\u0000\u02f5"+
		"\u02f3\u0001\u0000\u0000\u0000\u02f6\u02f8\u0005L\u0000\u0000\u02f7\u02ef"+
		"\u0001\u0000\u0000\u0000\u02f7\u02f8\u0001\u0000\u0000\u0000\u02f8\u02f9"+
		"\u0001\u0000\u0000\u0000\u02f9\u02fa\u0005Q\u0000\u0000\u02faW\u0001\u0000"+
		"\u0000\u0000\u02fb\u02ff\u0005\r\u0000\u0000\u02fc\u02fe\u0003\n\u0005"+
		"\u0000\u02fd\u02fc\u0001\u0000\u0000\u0000\u02fe\u0301\u0001\u0000\u0000"+
		"\u0000\u02ff\u02fd\u0001\u0000\u0000\u0000\u02ff\u0300\u0001\u0000\u0000"+
		"\u0000\u0300\u0302\u0001\u0000\u0000\u0000\u0301\u02ff\u0001\u0000\u0000"+
		"\u0000\u0302\u0306\u0005K\u0000\u0000\u0303\u0305\u0003\n\u0005\u0000"+
		"\u0304\u0303\u0001\u0000\u0000\u0000\u0305\u0308\u0001\u0000\u0000\u0000"+
		"\u0306\u0304\u0001\u0000\u0000\u0000\u0306\u0307\u0001\u0000\u0000\u0000"+
		"\u0307\u030c\u0001\u0000\u0000\u0000\u0308\u0306\u0001\u0000\u0000\u0000"+
		"\u0309\u030b\u0003Z-\u0000\u030a\u0309\u0001\u0000\u0000\u0000\u030b\u030e"+
		"\u0001\u0000\u0000\u0000\u030c\u030a\u0001\u0000\u0000\u0000\u030c\u030d"+
		"\u0001\u0000\u0000\u0000\u030d\u030f\u0001\u0000\u0000\u0000\u030e\u030c"+
		"\u0001\u0000\u0000\u0000\u030f\u0310\u0005L\u0000\u0000\u0310Y\u0001\u0000"+
		"\u0000\u0000\u0311\u0315\u0005@\u0000\u0000\u0312\u0314\u0003\n\u0005"+
		"\u0000\u0313\u0312\u0001\u0000\u0000\u0000\u0314\u0317\u0001\u0000\u0000"+
		"\u0000\u0315\u0313\u0001\u0000\u0000\u0000\u0315\u0316\u0001\u0000\u0000"+
		"\u0000\u0316\u0321\u0001\u0000\u0000\u0000\u0317\u0315\u0001\u0000\u0000"+
		"\u0000\u0318\u031a\u0005I\u0000\u0000\u0319\u031b\u0005@\u0000\u0000\u031a"+
		"\u0319\u0001\u0000\u0000\u0000\u031a\u031b\u0001\u0000\u0000\u0000\u031b"+
		"\u031e\u0001\u0000\u0000\u0000\u031c\u031d\u0005S\u0000\u0000\u031d\u031f"+
		"\u0005@\u0000\u0000\u031e\u031c\u0001\u0000\u0000\u0000\u031e\u031f\u0001"+
		"\u0000\u0000\u0000\u031f\u0320\u0001\u0000\u0000\u0000\u0320\u0322\u0005"+
		"J\u0000\u0000\u0321\u0318\u0001\u0000\u0000\u0000\u0321\u0322\u0001\u0000"+
		"\u0000\u0000\u0322\u0325\u0001\u0000\u0000\u0000\u0323\u0324\u0005R\u0000"+
		"\u0000\u0324\u0326\u0003*\u0015\u0000\u0325\u0323\u0001\u0000\u0000\u0000"+
		"\u0325\u0326\u0001\u0000\u0000\u0000\u0326\u0327\u0001\u0000\u0000\u0000"+
		"\u0327\u0328\u0005Q\u0000\u0000\u0328[\u0001\u0000\u0000\u0000\u0329\u032d"+
		"\u0005\u000e\u0000\u0000\u032a\u032c\u0003\n\u0005\u0000\u032b\u032a\u0001"+
		"\u0000\u0000\u0000\u032c\u032f\u0001\u0000\u0000\u0000\u032d\u032b\u0001"+
		"\u0000\u0000\u0000\u032d\u032e\u0001\u0000\u0000\u0000\u032e\u0330\u0001"+
		"\u0000\u0000\u0000\u032f\u032d\u0001\u0000\u0000\u0000\u0330\u0334\u0005"+
		"K\u0000\u0000\u0331\u0333\u0003\n\u0005\u0000\u0332\u0331\u0001\u0000"+
		"\u0000\u0000\u0333\u0336\u0001\u0000\u0000\u0000\u0334\u0332\u0001\u0000"+
		"\u0000\u0000\u0334\u0335\u0001\u0000\u0000\u0000\u0335\u033a\u0001\u0000"+
		"\u0000\u0000\u0336\u0334\u0001\u0000\u0000\u0000\u0337\u0339\u0003^/\u0000"+
		"\u0338\u0337\u0001\u0000\u0000\u0000\u0339\u033c\u0001\u0000\u0000\u0000"+
		"\u033a\u0338\u0001\u0000\u0000\u0000\u033a\u033b\u0001\u0000\u0000\u0000"+
		"\u033b\u033d\u0001\u0000\u0000\u0000\u033c\u033a\u0001\u0000\u0000\u0000"+
		"\u033d\u033e\u0005L\u0000\u0000\u033e]\u0001\u0000\u0000\u0000\u033f\u0343"+
		"\u0005@\u0000\u0000\u0340\u0342\u0003\n\u0005\u0000\u0341\u0340\u0001"+
		"\u0000\u0000\u0000\u0342\u0345\u0001\u0000\u0000\u0000\u0343\u0341\u0001"+
		"\u0000\u0000\u0000\u0343\u0344\u0001\u0000\u0000\u0000\u0344\u034b\u0001"+
		"\u0000\u0000\u0000\u0345\u0343\u0001\u0000\u0000\u0000\u0346\u0348\u0005"+
		"I\u0000\u0000\u0347\u0349\u0005@\u0000\u0000\u0348\u0347\u0001\u0000\u0000"+
		"\u0000\u0348\u0349\u0001\u0000\u0000\u0000\u0349\u034a\u0001\u0000\u0000"+
		"\u0000\u034a\u034c\u0005J\u0000\u0000\u034b\u0346\u0001\u0000\u0000\u0000"+
		"\u034b\u034c\u0001\u0000\u0000\u0000\u034c\u034d\u0001\u0000\u0000\u0000"+
		"\u034d\u034e\u0005U\u0000\u0000\u034e\u034f\u0005:\u0000\u0000\u034f\u0350"+
		"\u0005Q\u0000\u0000\u0350_\u0001\u0000\u0000\u0000\u0351\u0355\u0005\u000f"+
		"\u0000\u0000\u0352\u0354\u0003\n\u0005\u0000\u0353\u0352\u0001\u0000\u0000"+
		"\u0000\u0354\u0357\u0001\u0000\u0000\u0000\u0355\u0353\u0001\u0000\u0000"+
		"\u0000\u0355\u0356\u0001\u0000\u0000\u0000\u0356\u0358\u0001\u0000\u0000"+
		"\u0000\u0357\u0355\u0001\u0000\u0000\u0000\u0358\u035c\u0005K\u0000\u0000"+
		"\u0359\u035b\u0003\n\u0005\u0000\u035a\u0359\u0001\u0000\u0000\u0000\u035b"+
		"\u035e\u0001\u0000\u0000\u0000\u035c\u035a\u0001\u0000\u0000\u0000\u035c"+
		"\u035d\u0001\u0000\u0000\u0000\u035d\u0362\u0001\u0000\u0000\u0000\u035e"+
		"\u035c\u0001\u0000\u0000\u0000\u035f\u0361\u0003b1\u0000\u0360\u035f\u0001"+
		"\u0000\u0000\u0000\u0361\u0364\u0001\u0000\u0000\u0000\u0362\u0360\u0001"+
		"\u0000\u0000\u0000\u0362\u0363\u0001\u0000\u0000\u0000\u0363\u0365\u0001"+
		"\u0000\u0000\u0000\u0364\u0362\u0001\u0000\u0000\u0000\u0365\u0366\u0005"+
		"L\u0000\u0000\u0366a\u0001\u0000\u0000\u0000\u0367\u036b\u0005@\u0000"+
		"\u0000\u0368\u036a\u0003\n\u0005\u0000\u0369\u0368\u0001\u0000\u0000\u0000"+
		"\u036a\u036d\u0001\u0000\u0000\u0000\u036b\u0369\u0001\u0000\u0000\u0000"+
		"\u036b\u036c\u0001\u0000\u0000\u0000\u036c\u036e\u0001\u0000\u0000\u0000"+
		"\u036d\u036b\u0001\u0000\u0000\u0000\u036e\u0372\u0005K\u0000\u0000\u036f"+
		"\u0371\u0003\n\u0005\u0000\u0370\u036f\u0001\u0000\u0000\u0000\u0371\u0374"+
		"\u0001\u0000\u0000\u0000\u0372\u0370\u0001\u0000\u0000\u0000\u0372\u0373"+
		"\u0001\u0000\u0000\u0000\u0373\u0376\u0001\u0000\u0000\u0000\u0374\u0372"+
		"\u0001\u0000\u0000\u0000\u0375\u0377\u0003d2\u0000\u0376\u0375\u0001\u0000"+
		"\u0000\u0000\u0376\u0377\u0001\u0000\u0000\u0000\u0377\u0378\u0001\u0000"+
		"\u0000\u0000\u0378\u0379\u0005L\u0000\u0000\u0379c\u0001\u0000\u0000\u0000"+
		"\u037a\u0381\u0003f3\u0000\u037b\u037d\u0005Q\u0000\u0000\u037c\u037b"+
		"\u0001\u0000\u0000\u0000\u037c\u037d\u0001\u0000\u0000\u0000\u037d\u037e"+
		"\u0001\u0000\u0000\u0000\u037e\u0380\u0003f3\u0000\u037f\u037c\u0001\u0000"+
		"\u0000\u0000\u0380\u0383\u0001\u0000\u0000\u0000\u0381\u037f\u0001\u0000"+
		"\u0000\u0000\u0381\u0382\u0001\u0000\u0000\u0000\u0382e\u0001\u0000\u0000"+
		"\u0000\u0383\u0381\u0001\u0000\u0000\u0000\u0384\u038b\u0003h4\u0000\u0385"+
		"\u038b\u0003j5\u0000\u0386\u038b\u0003l6\u0000\u0387\u038b\u0003n7\u0000"+
		"\u0388\u038b\u0003p8\u0000\u0389\u038b\u0003\n\u0005\u0000\u038a\u0384"+
		"\u0001\u0000\u0000\u0000\u038a\u0385\u0001\u0000\u0000\u0000\u038a\u0386"+
		"\u0001\u0000\u0000\u0000\u038a\u0387\u0001\u0000\u0000\u0000\u038a\u0388"+
		"\u0001\u0000\u0000\u0000\u038a\u0389\u0001\u0000\u0000\u0000\u038bg\u0001"+
		"\u0000\u0000\u0000\u038c\u038d\u0005a\u0000\u0000\u038d\u038e\u0005R\u0000"+
		"\u0000\u038e\u038f\u0003t:\u0000\u038fi\u0001\u0000\u0000\u0000\u0390"+
		"\u0391\u0005^\u0000\u0000\u0391\u0392\u0005R\u0000\u0000\u0392\u0394\u0005"+
		"K\u0000\u0000\u0393\u0395\u0003x<\u0000\u0394\u0393\u0001\u0000\u0000"+
		"\u0000\u0394\u0395\u0001\u0000\u0000\u0000\u0395\u0396\u0001\u0000\u0000"+
		"\u0000\u0396\u0397\u0005L\u0000\u0000\u0397k\u0001\u0000\u0000\u0000\u0398"+
		"\u0399\u0005-\u0000\u0000\u0399\u039a\u0005R\u0000\u0000\u039a\u039c\u0005"+
		"K\u0000\u0000\u039b\u039d\u0003x<\u0000\u039c\u039b\u0001\u0000\u0000"+
		"\u0000\u039c\u039d\u0001\u0000\u0000\u0000\u039d\u039e\u0001\u0000\u0000"+
		"\u0000\u039e\u039f\u0005L\u0000\u0000\u039fm\u0001\u0000\u0000\u0000\u03a0"+
		"\u03a1\u0005_\u0000\u0000\u03a1\u03a2\u0005R\u0000\u0000\u03a2\u03a3\u0003"+
		"r9\u0000\u03a3o\u0001\u0000\u0000\u0000\u03a4\u03a5\u0005`\u0000\u0000"+
		"\u03a5\u03a6\u0005R\u0000\u0000\u03a6\u03a7\u0003r9\u0000\u03a7q\u0001"+
		"\u0000\u0000\u0000\u03a8\u03aa\u0003\n\u0005\u0000\u03a9\u03a8\u0001\u0000"+
		"\u0000\u0000\u03aa\u03ad\u0001\u0000\u0000\u0000\u03ab\u03a9\u0001\u0000"+
		"\u0000\u0000\u03ab\u03ac\u0001\u0000\u0000\u0000\u03ac\u03b0\u0001\u0000"+
		"\u0000\u0000\u03ad\u03ab\u0001\u0000\u0000\u0000\u03ae\u03b1\u0003\u009a"+
		"M\u0000\u03af\u03b1\u0003\u0092I\u0000\u03b0\u03ae\u0001\u0000\u0000\u0000"+
		"\u03b0\u03af\u0001\u0000\u0000\u0000\u03b1s\u0001\u0000\u0000\u0000\u03b2"+
		"\u03b5\u0005D\u0000\u0000\u03b3\u03b5\u0003v;\u0000\u03b4\u03b2\u0001"+
		"\u0000\u0000\u0000\u03b4\u03b3\u0001\u0000\u0000\u0000\u03b5u\u0001\u0000"+
		"\u0000\u0000\u03b6\u03b7\u0003\u000e\u0007\u0000\u03b7w\u0001\u0000\u0000"+
		"\u0000\u03b8\u03bd\u0003z=\u0000\u03b9\u03ba\u0005S\u0000\u0000\u03ba"+
		"\u03bc\u0003z=\u0000\u03bb\u03b9\u0001\u0000\u0000\u0000\u03bc\u03bf\u0001"+
		"\u0000\u0000\u0000\u03bd\u03bb\u0001\u0000\u0000\u0000\u03bd\u03be\u0001"+
		"\u0000\u0000\u0000\u03bey\u0001\u0000\u0000\u0000\u03bf\u03bd\u0001\u0000"+
		"\u0000\u0000\u03c0\u03c1\u0005D\u0000\u0000\u03c1\u03c2\u0005R\u0000\u0000"+
		"\u03c2\u03c3\u0003\u000e\u0007\u0000\u03c3{\u0001\u0000\u0000\u0000\u03c4"+
		"\u03c8\u0005\u0010\u0000\u0000\u03c5\u03c7\u0003\n\u0005\u0000\u03c6\u03c5"+
		"\u0001\u0000\u0000\u0000\u03c7\u03ca\u0001\u0000\u0000\u0000\u03c8\u03c6"+
		"\u0001\u0000\u0000\u0000\u03c8\u03c9\u0001\u0000\u0000\u0000\u03c9\u03cb"+
		"\u0001\u0000\u0000\u0000\u03ca\u03c8\u0001\u0000\u0000\u0000\u03cb\u03cf"+
		"\u0005K\u0000\u0000\u03cc\u03ce\u0003\n\u0005\u0000\u03cd\u03cc\u0001"+
		"\u0000\u0000\u0000\u03ce\u03d1\u0001\u0000\u0000\u0000\u03cf\u03cd\u0001"+
		"\u0000\u0000\u0000\u03cf\u03d0\u0001\u0000\u0000\u0000\u03d0\u03d5\u0001"+
		"\u0000\u0000\u0000\u03d1\u03cf\u0001\u0000\u0000\u0000\u03d2\u03d4\u0003"+
		"~?\u0000\u03d3\u03d2\u0001\u0000\u0000\u0000\u03d4\u03d7\u0001\u0000\u0000"+
		"\u0000\u03d5\u03d3\u0001\u0000\u0000\u0000\u03d5\u03d6\u0001\u0000\u0000"+
		"\u0000\u03d6\u03d8\u0001\u0000\u0000\u0000\u03d7\u03d5\u0001\u0000\u0000"+
		"\u0000\u03d8\u03d9\u0005L\u0000\u0000\u03d9}\u0001\u0000\u0000\u0000\u03da"+
		"\u03dd\u0003\u0080@\u0000\u03db\u03dd\u0003\u00a4R\u0000\u03dc\u03da\u0001"+
		"\u0000\u0000\u0000\u03dc\u03db\u0001\u0000\u0000\u0000\u03dd\u007f\u0001"+
		"\u0000\u0000\u0000\u03de\u03e2\u0005@\u0000\u0000\u03df\u03e1\u0003\n"+
		"\u0005\u0000\u03e0\u03df\u0001\u0000\u0000\u0000\u03e1\u03e4\u0001\u0000"+
		"\u0000\u0000\u03e2\u03e0\u0001\u0000\u0000\u0000\u03e2\u03e3\u0001\u0000"+
		"\u0000\u0000\u03e3\u03e5\u0001\u0000\u0000\u0000\u03e4\u03e2\u0001\u0000"+
		"\u0000\u0000\u03e5\u03e9\u0005K\u0000\u0000\u03e6\u03e8\u0003\n\u0005"+
		"\u0000\u03e7\u03e6\u0001\u0000\u0000\u0000\u03e8\u03eb\u0001\u0000\u0000"+
		"\u0000\u03e9\u03e7\u0001\u0000\u0000\u0000\u03e9\u03ea\u0001\u0000\u0000"+
		"\u0000\u03ea\u03ef\u0001\u0000\u0000\u0000\u03eb\u03e9\u0001\u0000\u0000"+
		"\u0000\u03ec\u03ee\u0003\u0082A\u0000\u03ed\u03ec\u0001\u0000\u0000\u0000"+
		"\u03ee\u03f1\u0001\u0000\u0000\u0000\u03ef\u03ed\u0001\u0000\u0000\u0000"+
		"\u03ef\u03f0\u0001\u0000\u0000\u0000\u03f0\u03f2\u0001\u0000\u0000\u0000"+
		"\u03f1\u03ef\u0001\u0000\u0000\u0000\u03f2\u03f3\u0005L\u0000\u0000\u03f3"+
		"\u0081\u0001\u0000\u0000\u0000\u03f4\u03fd\u0003\u0084B\u0000\u03f5\u03fd"+
		"\u0003\u0088D\u0000\u03f6\u03fd\u0003\u008cF\u0000\u03f7\u03fd\u0003\u008e"+
		"G\u0000\u03f8\u03fd\u0003\u0090H\u0000\u03f9\u03fd\u0003|>\u0000\u03fa"+
		"\u03fd\u0003\u00a4R\u0000\u03fb\u03fd\u0003\n\u0005\u0000\u03fc\u03f4"+
		"\u0001\u0000\u0000\u0000\u03fc\u03f5\u0001\u0000\u0000\u0000\u03fc\u03f6"+
		"\u0001\u0000\u0000\u0000\u03fc\u03f7\u0001\u0000\u0000\u0000\u03fc\u03f8"+
		"\u0001\u0000\u0000\u0000\u03fc\u03f9\u0001\u0000\u0000\u0000\u03fc\u03fa"+
		"\u0001\u0000\u0000\u0000\u03fc\u03fb\u0001\u0000\u0000\u0000\u03fd\u0083"+
		"\u0001\u0000\u0000\u0000\u03fe\u03ff\u0007\u0003\u0000\u0000\u03ff\u0400"+
		"\u0003\u0086C\u0000\u0400\u0401\u0005Q\u0000\u0000\u0401\u0085\u0001\u0000"+
		"\u0000\u0000\u0402\u0403\u0003\u0016\u000b\u0000\u0403\u0087\u0001\u0000"+
		"\u0000\u0000\u0404\u0405\u0005*\u0000\u0000\u0405\u0409\u0005@\u0000\u0000"+
		"\u0406\u0408\u0003\n\u0005\u0000\u0407\u0406\u0001\u0000\u0000\u0000\u0408"+
		"\u040b\u0001\u0000\u0000\u0000\u0409\u0407\u0001\u0000\u0000\u0000\u0409"+
		"\u040a\u0001\u0000\u0000\u0000\u040a\u040c\u0001\u0000\u0000\u0000\u040b"+
		"\u0409\u0001\u0000\u0000\u0000\u040c\u0410\u0005K\u0000\u0000\u040d\u040f"+
		"\u0003\n\u0005\u0000\u040e\u040d\u0001\u0000\u0000\u0000\u040f\u0412\u0001"+
		"\u0000\u0000\u0000\u0410\u040e\u0001\u0000\u0000\u0000\u0410\u0411\u0001"+
		"\u0000\u0000\u0000\u0411\u0414\u0001\u0000\u0000\u0000\u0412\u0410\u0001"+
		"\u0000\u0000\u0000\u0413\u0415\u0003\u008aE\u0000\u0414\u0413\u0001\u0000"+
		"\u0000\u0000\u0414\u0415\u0001\u0000\u0000\u0000\u0415\u0416\u0001\u0000"+
		"\u0000\u0000\u0416\u0417\u0005L\u0000\u0000\u0417\u0418\u0005Q\u0000\u0000"+
		"\u0418\u0089\u0001\u0000\u0000\u0000\u0419\u0420\u0003f3\u0000\u041a\u041c"+
		"\u0005Q\u0000\u0000\u041b\u041a\u0001\u0000\u0000\u0000\u041b\u041c\u0001"+
		"\u0000\u0000\u0000\u041c\u041d\u0001\u0000\u0000\u0000\u041d\u041f\u0003"+
		"f3\u0000\u041e\u041b\u0001\u0000\u0000\u0000\u041f\u0422\u0001\u0000\u0000"+
		"\u0000\u0420\u041e\u0001\u0000\u0000\u0000\u0420\u0421\u0001\u0000\u0000"+
		"\u0000\u0421\u008b\u0001\u0000\u0000\u0000\u0422\u0420\u0001\u0000\u0000"+
		"\u0000\u0423\u0424\u0005\u0011\u0000\u0000\u0424\u0428\u0005@\u0000\u0000"+
		"\u0425\u0427\u0003\n\u0005\u0000\u0426\u0425\u0001\u0000\u0000\u0000\u0427"+
		"\u042a\u0001\u0000\u0000\u0000\u0428\u0426\u0001\u0000\u0000\u0000\u0428"+
		"\u0429\u0001\u0000\u0000\u0000\u0429\u042b\u0001\u0000\u0000\u0000\u042a"+
		"\u0428\u0001\u0000\u0000\u0000\u042b\u042c\u0003\u0092I\u0000\u042c\u042d"+
		"\u0005Q\u0000\u0000\u042d\u008d\u0001\u0000\u0000\u0000\u042e\u042f\u0005"+
		"\u0012\u0000\u0000\u042f\u0433\u0003\u00a2Q\u0000\u0430\u0432\u0003\n"+
		"\u0005\u0000\u0431\u0430\u0001\u0000\u0000\u0000\u0432\u0435\u0001\u0000"+
		"\u0000\u0000\u0433\u0431\u0001\u0000\u0000\u0000\u0433\u0434\u0001\u0000"+
		"\u0000\u0000\u0434\u0436\u0001\u0000\u0000\u0000\u0435\u0433\u0001\u0000"+
		"\u0000\u0000\u0436\u0437\u0003\u0092I\u0000\u0437\u0438\u0005Q\u0000\u0000"+
		"\u0438\u008f\u0001\u0000\u0000\u0000\u0439\u043a\u0005{\u0000\u0000\u043a"+
		"\u043e\u0003\u00a0P\u0000\u043b\u043d\u0003\n\u0005\u0000\u043c\u043b"+
		"\u0001\u0000\u0000\u0000\u043d\u0440\u0001\u0000\u0000\u0000\u043e\u043c"+
		"\u0001\u0000\u0000\u0000\u043e\u043f\u0001\u0000\u0000\u0000\u043f\u0441"+
		"\u0001\u0000\u0000\u0000\u0440\u043e\u0001\u0000\u0000\u0000\u0441\u0442"+
		"\u0003\u0092I\u0000\u0442\u0443\u0005Q\u0000\u0000\u0443\u0091\u0001\u0000"+
		"\u0000\u0000\u0444\u0445\u0005\u0013\u0000\u0000\u0445\u044a\u0003\u0094"+
		"J\u0000\u0446\u0447\u0005K\u0000\u0000\u0447\u0448\u0003\u0096K\u0000"+
		"\u0448\u0449\u0005L\u0000\u0000\u0449\u044b\u0001\u0000\u0000\u0000\u044a"+
		"\u0446\u0001\u0000\u0000\u0000\u044a\u044b\u0001\u0000\u0000\u0000\u044b"+
		"\u0093\u0001\u0000\u0000\u0000\u044c\u0450\u0003\u0016\u000b\u0000\u044d"+
		"\u044e\u0005T\u0000\u0000\u044e\u0450\u0005\u0014\u0000\u0000\u044f\u044c"+
		"\u0001\u0000\u0000\u0000\u044f\u044d\u0001\u0000\u0000\u0000\u0450\u0095"+
		"\u0001\u0000\u0000\u0000\u0451\u0458\u0003\u0098L\u0000\u0452\u0454\u0005"+
		"S\u0000\u0000\u0453\u0452\u0001\u0000\u0000\u0000\u0453\u0454\u0001\u0000"+
		"\u0000\u0000\u0454\u0455\u0001\u0000\u0000\u0000\u0455\u0457\u0003\u0098"+
		"L\u0000\u0456\u0453\u0001\u0000\u0000\u0000\u0457\u045a\u0001\u0000\u0000"+
		"\u0000\u0458\u0456\u0001\u0000\u0000\u0000\u0458\u0459\u0001\u0000\u0000"+
		"\u0000\u0459\u0462\u0001\u0000\u0000\u0000\u045a\u0458\u0001\u0000\u0000"+
		"\u0000\u045b\u045d\u0003\n\u0005\u0000\u045c\u045b\u0001\u0000\u0000\u0000"+
		"\u045d\u0460\u0001\u0000\u0000\u0000\u045e\u045c\u0001\u0000\u0000\u0000"+
		"\u045e\u045f\u0001\u0000\u0000\u0000\u045f\u0462\u0001\u0000\u0000\u0000"+
		"\u0460\u045e\u0001\u0000\u0000\u0000\u0461\u0451\u0001\u0000\u0000\u0000"+
		"\u0461\u045e\u0001\u0000\u0000\u0000\u0462\u0097\u0001\u0000\u0000\u0000"+
		"\u0463\u0464\u0005&\u0000\u0000\u0464\u0465\u0005R\u0000\u0000\u0465\u046e"+
		"\u0003\u009aM\u0000\u0466\u0467\u0005\'\u0000\u0000\u0467\u0468\u0005"+
		"R\u0000\u0000\u0468\u046e\u0003\u009cN\u0000\u0469\u046a\u0005(\u0000"+
		"\u0000\u046a\u046b\u0005R\u0000\u0000\u046b\u046e\u0003\u009eO\u0000\u046c"+
		"\u046e\u0003\n\u0005\u0000\u046d\u0463\u0001\u0000\u0000\u0000\u046d\u0466"+
		"\u0001\u0000\u0000\u0000\u046d\u0469\u0001\u0000\u0000\u0000\u046d\u046c"+
		"\u0001\u0000\u0000\u0000\u046e\u0099\u0001\u0000\u0000\u0000\u046f\u0470"+
		"\u0005M\u0000\u0000\u0470\u0475\u0003\u0086C\u0000\u0471\u0472\u0005S"+
		"\u0000\u0000\u0472\u0474\u0003\u0086C\u0000\u0473\u0471\u0001\u0000\u0000"+
		"\u0000\u0474\u0477\u0001\u0000\u0000\u0000\u0475\u0473\u0001\u0000\u0000"+
		"\u0000\u0475\u0476\u0001\u0000\u0000\u0000\u0476\u0478\u0001\u0000\u0000"+
		"\u0000\u0477\u0475\u0001\u0000\u0000\u0000\u0478\u0479\u0005N\u0000\u0000"+
		"\u0479\u047c\u0001\u0000\u0000\u0000\u047a\u047c\u0003\u0086C\u0000\u047b"+
		"\u046f\u0001\u0000\u0000\u0000\u047b\u047a\u0001\u0000\u0000\u0000\u047c"+
		"\u009b\u0001\u0000\u0000\u0000\u047d\u047e\u0005M\u0000\u0000\u047e\u0483"+
		"\u0003\u00a0P\u0000\u047f\u0480\u0005S\u0000\u0000\u0480\u0482\u0003\u00a0"+
		"P\u0000\u0481\u047f\u0001\u0000\u0000\u0000\u0482\u0485\u0001\u0000\u0000"+
		"\u0000\u0483\u0481\u0001\u0000\u0000\u0000\u0483\u0484\u0001\u0000\u0000"+
		"\u0000\u0484\u0486\u0001\u0000\u0000\u0000\u0485\u0483\u0001\u0000\u0000"+
		"\u0000\u0486\u0487\u0005N\u0000\u0000\u0487\u048a\u0001\u0000\u0000\u0000"+
		"\u0488\u048a\u0003\u00a0P\u0000\u0489\u047d\u0001\u0000\u0000\u0000\u0489"+
		"\u0488\u0001\u0000\u0000\u0000\u048a\u009d\u0001\u0000\u0000\u0000\u048b"+
		"\u048c\u0005M\u0000\u0000\u048c\u0491\u0003\u0016\u000b\u0000\u048d\u048e"+
		"\u0005S\u0000\u0000\u048e\u0490\u0003\u0016\u000b\u0000\u048f\u048d\u0001"+
		"\u0000\u0000\u0000\u0490\u0493\u0001\u0000\u0000\u0000\u0491\u048f\u0001"+
		"\u0000\u0000\u0000\u0491\u0492\u0001\u0000\u0000\u0000\u0492\u0494\u0001"+
		"\u0000\u0000\u0000\u0493\u0491\u0001\u0000\u0000\u0000\u0494\u0495\u0005"+
		"N\u0000\u0000\u0495\u0498\u0001\u0000\u0000\u0000\u0496\u0498\u0003\u0016"+
		"\u000b\u0000\u0497\u048b\u0001\u0000\u0000\u0000\u0497\u0496\u0001\u0000"+
		"\u0000\u0000\u0498\u009f\u0001\u0000\u0000\u0000\u0499\u049d\u0003\u0016"+
		"\u000b\u0000\u049a\u049b\u0005I\u0000\u0000\u049b\u049c\u0005)\u0000\u0000"+
		"\u049c\u049e\u0005J\u0000\u0000\u049d\u049a\u0001\u0000\u0000\u0000\u049d"+
		"\u049e\u0001\u0000\u0000\u0000\u049e\u00a1\u0001\u0000\u0000\u0000\u049f"+
		"\u04a0\u0005B\u0000\u0000\u04a0\u04a1\u0005F\u0000\u0000\u04a1\u00a3\u0001"+
		"\u0000\u0000\u0000\u04a2\u04a4\u0005\u0014\u0000\u0000\u04a3\u04a5\u0007"+
		"\u0004\u0000\u0000\u04a4\u04a3\u0001\u0000\u0000\u0000\u04a4\u04a5\u0001"+
		"\u0000\u0000\u0000\u04a5\u04a9\u0001\u0000\u0000\u0000\u04a6\u04a8\u0003"+
		"\n\u0005\u0000\u04a7\u04a6\u0001\u0000\u0000\u0000\u04a8\u04ab\u0001\u0000"+
		"\u0000\u0000\u04a9\u04a7\u0001\u0000\u0000\u0000\u04a9\u04aa\u0001\u0000"+
		"\u0000\u0000\u04aa\u04ae\u0001\u0000\u0000\u0000\u04ab\u04a9\u0001\u0000"+
		"\u0000\u0000\u04ac\u04ad\u0005\u0017\u0000\u0000\u04ad\u04af\u0005@\u0000"+
		"\u0000\u04ae\u04ac\u0001\u0000\u0000\u0000\u04ae\u04af\u0001\u0000\u0000"+
		"\u0000\u04af\u04b1\u0001\u0000\u0000\u0000\u04b0\u04b2\u0003\u0092I\u0000"+
		"\u04b1\u04b0\u0001\u0000\u0000\u0000\u04b1\u04b2\u0001\u0000\u0000\u0000"+
		"\u04b2\u04b3\u0001\u0000\u0000\u0000\u04b3\u04b4\u0005Q\u0000\u0000\u04b4"+
		"\u00a5\u0001\u0000\u0000\u0000\u04b5\u04b6\u0005\u0007\u0000\u0000\u04b6"+
		"\u04ba\u0005K\u0000\u0000\u04b7\u04b9\u0003\n\u0005\u0000\u04b8\u04b7"+
		"\u0001\u0000\u0000\u0000\u04b9\u04bc\u0001\u0000\u0000\u0000\u04ba\u04b8"+
		"\u0001\u0000\u0000\u0000\u04ba\u04bb\u0001\u0000\u0000\u0000\u04bb\u04c0"+
		"\u0001\u0000\u0000\u0000\u04bc\u04ba\u0001\u0000\u0000\u0000\u04bd\u04bf"+
		"\u0003\u00a8T\u0000\u04be\u04bd\u0001\u0000\u0000\u0000\u04bf\u04c2\u0001"+
		"\u0000\u0000\u0000\u04c0\u04be\u0001\u0000\u0000\u0000\u04c0\u04c1\u0001"+
		"\u0000\u0000\u0000\u04c1\u04c3\u0001\u0000\u0000\u0000\u04c2\u04c0\u0001"+
		"\u0000\u0000\u0000\u04c3\u04c4\u0005L\u0000\u0000\u04c4\u00a7\u0001\u0000"+
		"\u0000\u0000\u04c5\u04c9\u0003\u00aaU\u0000\u04c6\u04c9\u0003\u00b0X\u0000"+
		"\u04c7\u04c9\u0003\u00b2Y\u0000\u04c8\u04c5\u0001\u0000\u0000\u0000\u04c8"+
		"\u04c6\u0001\u0000\u0000\u0000\u04c8\u04c7\u0001\u0000\u0000\u0000\u04c9"+
		"\u00a9\u0001\u0000\u0000\u0000\u04ca\u04cb\u0005\u001f\u0000\u0000\u04cb"+
		"\u04cf\u0005@\u0000\u0000\u04cc\u04ce\u0003\n\u0005\u0000\u04cd\u04cc"+
		"\u0001\u0000\u0000\u0000\u04ce\u04d1\u0001\u0000\u0000\u0000\u04cf\u04cd"+
		"\u0001\u0000\u0000\u0000\u04cf\u04d0\u0001\u0000\u0000\u0000\u04d0\u04d4"+
		"\u0001\u0000\u0000\u0000\u04d1\u04cf\u0001\u0000\u0000\u0000\u04d2\u04d3"+
		"\u0005\u001e\u0000\u0000\u04d3\u04d5\u0003\u0016\u000b\u0000\u04d4\u04d2"+
		"\u0001\u0000\u0000\u0000\u04d4\u04d5\u0001\u0000\u0000\u0000\u04d5\u04d6"+
		"\u0001\u0000\u0000\u0000\u04d6\u04da\u0005K\u0000\u0000\u04d7\u04d9\u0003"+
		"\n\u0005\u0000\u04d8\u04d7\u0001\u0000\u0000\u0000\u04d9\u04dc\u0001\u0000"+
		"\u0000\u0000\u04da\u04d8\u0001\u0000\u0000\u0000\u04da\u04db\u0001\u0000"+
		"\u0000\u0000\u04db\u04de\u0001\u0000\u0000\u0000\u04dc\u04da\u0001\u0000"+
		"\u0000\u0000\u04dd\u04df\u0003\u00acV\u0000\u04de\u04dd\u0001\u0000\u0000"+
		"\u0000\u04de\u04df\u0001\u0000\u0000\u0000\u04df\u04e0\u0001\u0000\u0000"+
		"\u0000\u04e0\u04e1\u0005L\u0000\u0000\u04e1\u00ab\u0001\u0000\u0000\u0000"+
		"\u04e2\u04e3\u0005$\u0000\u0000\u04e3\u04e7\u0005K\u0000\u0000\u04e4\u04e6"+
		"\u0003\u00aeW\u0000\u04e5\u04e4\u0001\u0000\u0000\u0000\u04e6\u04e9\u0001"+
		"\u0000\u0000\u0000\u04e7\u04e5\u0001\u0000\u0000\u0000\u04e7\u04e8\u0001"+
		"\u0000\u0000\u0000\u04e8\u04ea\u0001\u0000\u0000\u0000\u04e9\u04e7\u0001"+
		"\u0000\u0000\u0000\u04ea\u04eb\u0005L\u0000\u0000\u04eb\u00ad\u0001\u0000"+
		"\u0000\u0000\u04ec\u04ed\u0005@\u0000\u0000\u04ed\u04ee\u0005R\u0000\u0000"+
		"\u04ee\u04ef\u0003\u000e\u0007\u0000\u04ef\u04f0\u0005Q\u0000\u0000\u04f0"+
		"\u00af\u0001\u0000\u0000\u0000\u04f1\u04f2\u0005 \u0000\u0000\u04f2\u04f6"+
		"\u0005@\u0000\u0000\u04f3\u04f5\u0003\n\u0005\u0000\u04f4\u04f3\u0001"+
		"\u0000\u0000\u0000\u04f5\u04f8\u0001\u0000\u0000\u0000\u04f6\u04f4\u0001"+
		"\u0000\u0000\u0000\u04f6\u04f7\u0001\u0000\u0000\u0000\u04f7\u04f9\u0001"+
		"\u0000\u0000\u0000\u04f8\u04f6\u0001\u0000\u0000\u0000\u04f9\u04fe\u0005"+
		"K\u0000\u0000\u04fa\u04fd\u0003\n\u0005\u0000\u04fb\u04fd\u0003\u00b4"+
		"Z\u0000\u04fc\u04fa\u0001\u0000\u0000\u0000\u04fc\u04fb\u0001\u0000\u0000"+
		"\u0000\u04fd\u0500\u0001\u0000\u0000\u0000\u04fe\u04fc\u0001\u0000\u0000"+
		"\u0000\u04fe\u04ff\u0001\u0000\u0000\u0000\u04ff\u0501\u0001\u0000\u0000"+
		"\u0000\u0500\u04fe\u0001\u0000\u0000\u0000\u0501\u0502\u0005L\u0000\u0000"+
		"\u0502\u00b1\u0001\u0000\u0000\u0000\u0503\u0504\u0005!\u0000\u0000\u0504"+
		"\u0508\u0005@\u0000\u0000\u0505\u0507\u0003\n\u0005\u0000\u0506\u0505"+
		"\u0001\u0000\u0000\u0000\u0507\u050a\u0001\u0000\u0000\u0000\u0508\u0506"+
		"\u0001\u0000\u0000\u0000\u0508\u0509\u0001\u0000\u0000\u0000\u0509\u050b"+
		"\u0001\u0000\u0000\u0000\u050a\u0508\u0001\u0000\u0000\u0000\u050b\u0510"+
		"\u0005K\u0000\u0000\u050c\u050f\u0003\n\u0005\u0000\u050d\u050f\u0003"+
		"\u00b4Z\u0000\u050e\u050c\u0001\u0000\u0000\u0000\u050e\u050d\u0001\u0000"+
		"\u0000\u0000\u050f\u0512\u0001\u0000\u0000\u0000\u0510\u050e\u0001\u0000"+
		"\u0000\u0000\u0510\u0511\u0001\u0000\u0000\u0000\u0511\u0513\u0001\u0000"+
		"\u0000\u0000\u0512\u0510\u0001\u0000\u0000\u0000\u0513\u0514\u0005L\u0000"+
		"\u0000\u0514\u00b3\u0001\u0000\u0000\u0000\u0515\u0516\u0005@\u0000\u0000"+
		"\u0516\u0517\u0005R\u0000\u0000\u0517\u0518\u0003\u000e\u0007\u0000\u0518"+
		"\u0519\u0005Q\u0000\u0000\u0519\u00b5\u0001\u0000\u0000\u0000\u051a\u051b"+
		"\u0005\b\u0000\u0000\u051b\u051f\u0005K\u0000\u0000\u051c\u051e\u0003"+
		"\n\u0005\u0000\u051d\u051c\u0001\u0000\u0000\u0000\u051e\u0521\u0001\u0000"+
		"\u0000\u0000\u051f\u051d\u0001\u0000\u0000\u0000\u051f\u0520\u0001\u0000"+
		"\u0000\u0000\u0520\u0525\u0001\u0000\u0000\u0000\u0521\u051f\u0001\u0000"+
		"\u0000\u0000\u0522\u0524\u0003\u00b8\\\u0000\u0523\u0522\u0001\u0000\u0000"+
		"\u0000\u0524\u0527\u0001\u0000\u0000\u0000\u0525\u0523\u0001\u0000\u0000"+
		"\u0000\u0525\u0526\u0001\u0000\u0000\u0000\u0526\u0528\u0001\u0000\u0000"+
		"\u0000\u0527\u0525\u0001\u0000\u0000\u0000\u0528\u0529\u0005L\u0000\u0000"+
		"\u0529\u00b7\u0001\u0000\u0000\u0000\u052a\u052b\u0003\u00ba]\u0000\u052b"+
		"\u052f\u0005D\u0000\u0000\u052c\u052e\u0003\n\u0005\u0000\u052d\u052c"+
		"\u0001\u0000\u0000\u0000\u052e\u0531\u0001\u0000\u0000\u0000\u052f\u052d"+
		"\u0001\u0000\u0000\u0000\u052f\u0530\u0001\u0000\u0000\u0000\u0530\u0532"+
		"\u0001\u0000\u0000\u0000\u0531\u052f\u0001\u0000\u0000\u0000\u0532\u0536"+
		"\u0005K\u0000\u0000\u0533\u0535\u0003\n\u0005\u0000\u0534\u0533\u0001"+
		"\u0000\u0000\u0000\u0535\u0538\u0001\u0000\u0000\u0000\u0536\u0534\u0001"+
		"\u0000\u0000\u0000\u0536\u0537\u0001\u0000\u0000\u0000\u0537\u053c\u0001"+
		"\u0000\u0000\u0000\u0538\u0536\u0001\u0000\u0000\u0000\u0539\u053b\u0003"+
		"\u00bc^\u0000\u053a\u0539\u0001\u0000\u0000\u0000\u053b\u053e\u0001\u0000"+
		"\u0000\u0000\u053c\u053a\u0001\u0000\u0000\u0000\u053c\u053d\u0001\u0000"+
		"\u0000\u0000\u053d\u053f\u0001\u0000\u0000\u0000\u053e\u053c\u0001\u0000"+
		"\u0000\u0000\u053f\u0540\u0005L\u0000\u0000\u0540\u00b9\u0001\u0000\u0000"+
		"\u0000\u0541\u0542\u0007\u0005\u0000\u0000\u0542\u00bb\u0001\u0000\u0000"+
		"\u0000\u0543\u0547\u0005@\u0000\u0000\u0544\u0546\u0003\n\u0005\u0000"+
		"\u0545\u0544\u0001\u0000\u0000\u0000\u0546\u0549\u0001\u0000\u0000\u0000"+
		"\u0547\u0545\u0001\u0000\u0000\u0000\u0547\u0548\u0001\u0000\u0000\u0000"+
		"\u0548\u054a\u0001\u0000\u0000\u0000\u0549\u0547\u0001\u0000\u0000\u0000"+
		"\u054a\u054f\u0005K\u0000\u0000\u054b\u054e\u0003\n\u0005\u0000\u054c"+
		"\u054e\u0003\u00be_\u0000\u054d\u054b\u0001\u0000\u0000\u0000\u054d\u054c"+
		"\u0001\u0000\u0000\u0000\u054e\u0551\u0001\u0000\u0000\u0000\u054f\u054d"+
		"\u0001\u0000\u0000\u0000\u054f\u0550\u0001\u0000\u0000\u0000\u0550\u0552"+
		"\u0001\u0000\u0000\u0000\u0551\u054f\u0001\u0000\u0000\u0000\u0552\u0553"+
		"\u0005L\u0000\u0000\u0553\u00bd\u0001\u0000\u0000\u0000\u0554\u0555\u0005"+
		"/\u0000\u0000\u0555\u0556\u0005R\u0000\u0000\u0556\u055a\u0003\u001a\r"+
		"\u0000\u0557\u055a\u0003\u00b4Z\u0000\u0558\u055a\u0003\n\u0005\u0000"+
		"\u0559\u0554\u0001\u0000\u0000\u0000\u0559\u0557\u0001\u0000\u0000\u0000"+
		"\u0559\u0558\u0001\u0000\u0000\u0000\u055a\u00bf\u0001\u0000\u0000\u0000"+
		"\u00a1\u00c1\u00c6\u00cc\u00d2\u00d8\u00de\u00f2\u00ff\u0107\u010c\u0112"+
		"\u0119\u0122\u012b\u0130\u0136\u013f\u0147\u014d\u0154\u015b\u0162\u0168"+
		"\u0173\u017a\u017e\u0187\u018e\u0194\u019d\u01b5\u01be\u01c4\u01ce\u01d5"+
		"\u01df\u01e5\u01ed\u01f4\u01fb\u0205\u020c\u0212\u0219\u021e\u0227\u022e"+
		"\u0234\u023e\u0244\u024b\u0252\u0259\u025f\u0268\u026d\u0272\u0278\u027c"+
		"\u0285\u0291\u0296\u029c\u02a6\u02ac\u02b6\u02bd\u02c3\u02ce\u02d4\u02db"+
		"\u02e1\u02ec\u02f3\u02f7\u02ff\u0306\u030c\u0315\u031a\u031e\u0321\u0325"+
		"\u032d\u0334\u033a\u0343\u0348\u034b\u0355\u035c\u0362\u036b\u0372\u0376"+
		"\u037c\u0381\u038a\u0394\u039c\u03ab\u03b0\u03b4\u03bd\u03c8\u03cf\u03d5"+
		"\u03dc\u03e2\u03e9\u03ef\u03fc\u0409\u0410\u0414\u041b\u0420\u0428\u0433"+
		"\u043e\u044a\u044f\u0453\u0458\u045e\u0461\u046d\u0475\u047b\u0483\u0489"+
		"\u0491\u0497\u049d\u04a4\u04a9\u04ae\u04b1\u04ba\u04c0\u04c8\u04cf\u04d4"+
		"\u04da\u04de\u04e7\u04f6\u04fc\u04fe\u0508\u050e\u0510\u051f\u0525\u052f"+
		"\u0536\u053c\u0547\u054d\u054f\u0559";
	public static final ATN _ATN =
		new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}