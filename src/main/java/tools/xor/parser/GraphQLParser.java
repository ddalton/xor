// Generated from GraphQL.g4 by ANTLR 4.9.2
package tools.xor.parser;

import org.antlr.v4.runtime.atn.*;
import org.antlr.v4.runtime.dfa.DFA;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.misc.*;
import org.antlr.v4.runtime.tree.*;
import java.util.List;
import java.util.Iterator;
import java.util.ArrayList;

@SuppressWarnings({"all", "warnings", "unchecked", "unused", "cast"})
public class GraphQLParser extends Parser {
	static { RuntimeMetaData.checkVersion("4.9.2", RuntimeMetaData.VERSION); }

	protected static final DFA[] _decisionToDFA;
	protected static final PredictionContextCache _sharedContextCache =
		new PredictionContextCache();
	public static final int
		T__0=1, T__1=2, T__2=3, T__3=4, T__4=5, T__5=6, T__6=7, T__7=8, T__8=9, 
		T__9=10, T__10=11, T__11=12, T__12=13, T__13=14, T__14=15, T__15=16, T__16=17, 
		T__17=18, T__18=19, T__19=20, T__20=21, T__21=22, T__22=23, T__23=24, 
		T__24=25, T__25=26, T__26=27, T__27=28, T__28=29, T__29=30, T__30=31, 
		T__31=32, T__32=33, T__33=34, T__34=35, T__35=36, T__36=37, T__37=38, 
		T__38=39, T__39=40, T__40=41, T__41=42, T__42=43, T__43=44, T__44=45, 
		T__45=46, T__46=47, T__47=48, T__48=49, T__49=50, NAME=51, STRING=52, 
		BLOCK_STRING=53, ID=54, FLOAT=55, INT=56, PUNCTUATOR=57, WS=58, COMMA=59, 
		LineComment=60, UNICODE_BOM=61, UTF8_BOM=62, UTF16_BOM=63, UTF32_BOM=64;
	public static final int
		RULE_document = 0, RULE_definition = 1, RULE_executableDefinition = 2, 
		RULE_operationDefinition = 3, RULE_operationType = 4, RULE_selectionSet = 5, 
		RULE_selection = 6, RULE_field = 7, RULE_arguments = 8, RULE_argument = 9, 
		RULE_alias = 10, RULE_fragmentSpread = 11, RULE_fragmentDefinition = 12, 
		RULE_fragmentName = 13, RULE_typeCondition = 14, RULE_inlineFragment = 15, 
		RULE_value = 16, RULE_intValue = 17, RULE_floatValue = 18, RULE_booleanValue = 19, 
		RULE_stringValue = 20, RULE_nullValue = 21, RULE_enumValue = 22, RULE_listValue = 23, 
		RULE_objectValue = 24, RULE_objectField = 25, RULE_variable = 26, RULE_variableDefinitions = 27, 
		RULE_variableDefinition = 28, RULE_defaultValue = 29, RULE_type_ = 30, 
		RULE_namedType = 31, RULE_listType = 32, RULE_directives = 33, RULE_directive = 34, 
		RULE_typeSystemDefinition = 35, RULE_typeSystemExtension = 36, RULE_schemaDefinition = 37, 
		RULE_rootOperationTypeDefinition = 38, RULE_schemaExtension = 39, RULE_operationTypeDefinition = 40, 
		RULE_description = 41, RULE_typeDefinition = 42, RULE_typeExtension = 43, 
		RULE_scalarTypeDefinition = 44, RULE_scalarTypeExtension = 45, RULE_objectTypeDefinition = 46, 
		RULE_implementsInterfaces = 47, RULE_fieldsDefinition = 48, RULE_fieldDefinition = 49, 
		RULE_argumentsDefinition = 50, RULE_inputValueDefinition = 51, RULE_objectTypeExtension = 52, 
		RULE_interfaceTypeDefinition = 53, RULE_interfaceTypeExtension = 54, RULE_unionTypeDefinition = 55, 
		RULE_unionMemberTypes = 56, RULE_unionTypeExtension = 57, RULE_enumTypeDefinition = 58, 
		RULE_enumValuesDefinition = 59, RULE_enumValueDefinition = 60, RULE_enumTypeExtension = 61, 
		RULE_inputObjectTypeDefinition = 62, RULE_inputFieldsDefinition = 63, 
		RULE_inputObjectTypeExtension = 64, RULE_directiveDefinition = 65, RULE_directiveLocations = 66, 
		RULE_directiveLocation = 67, RULE_executableDirectiveLocation = 68, RULE_typeSystemDirectiveLocation = 69, 
		RULE_name = 70;
	private static String[] makeRuleNames() {
		return new String[] {
			"document", "definition", "executableDefinition", "operationDefinition", 
			"operationType", "selectionSet", "selection", "field", "arguments", "argument", 
			"alias", "fragmentSpread", "fragmentDefinition", "fragmentName", "typeCondition", 
			"inlineFragment", "value", "intValue", "floatValue", "booleanValue", 
			"stringValue", "nullValue", "enumValue", "listValue", "objectValue", 
			"objectField", "variable", "variableDefinitions", "variableDefinition", 
			"defaultValue", "type_", "namedType", "listType", "directives", "directive", 
			"typeSystemDefinition", "typeSystemExtension", "schemaDefinition", "rootOperationTypeDefinition", 
			"schemaExtension", "operationTypeDefinition", "description", "typeDefinition", 
			"typeExtension", "scalarTypeDefinition", "scalarTypeExtension", "objectTypeDefinition", 
			"implementsInterfaces", "fieldsDefinition", "fieldDefinition", "argumentsDefinition", 
			"inputValueDefinition", "objectTypeExtension", "interfaceTypeDefinition", 
			"interfaceTypeExtension", "unionTypeDefinition", "unionMemberTypes", 
			"unionTypeExtension", "enumTypeDefinition", "enumValuesDefinition", "enumValueDefinition", 
			"enumTypeExtension", "inputObjectTypeDefinition", "inputFieldsDefinition", 
			"inputObjectTypeExtension", "directiveDefinition", "directiveLocations", 
			"directiveLocation", "executableDirectiveLocation", "typeSystemDirectiveLocation", 
			"name"
		};
	}
	public static final String[] ruleNames = makeRuleNames();

	private static String[] makeLiteralNames() {
		return new String[] {
			null, "'query'", "'mutation'", "'subscription'", "'{'", "'}'", "'type'", 
			"'('", "')'", "':'", "'...'", "'fragment'", "'on'", "'true'", "'false'", 
			"'null'", "'['", "']'", "'$'", "'='", "'!'", "'@'", "'schema'", "'extend'", 
			"'scalar'", "'implements'", "'&'", "'interface'", "'union'", "'|'", "'enum'", 
			"'input'", "'directive'", "'QUERY'", "'MUTATION'", "'SUBSCRIPTION'", 
			"'FIELD'", "'FRAGMENT_DEFINITION'", "'FRAGMENT_SPREAD'", "'INLINE_FRAGMENT'", 
			"'SCHEMA'", "'SCALAR'", "'OBJECT'", "'FIELD_DEFINITION'", "'ARGUMENT_DEFINITION'", 
			"'INTERFACE'", "'UNION'", "'ENUM'", "'ENUM_VALUE'", "'INPUT_OBJECT'", 
			"'INPUT_FIELD_DEFINITION'", null, null, null, null, null, null, null, 
			null, "','", null, null, "'\uEFBBBF'", "'\uFEFF'", "'\u0000FEFF'"
		};
	}
	private static final String[] _LITERAL_NAMES = makeLiteralNames();
	private static String[] makeSymbolicNames() {
		return new String[] {
			null, null, null, null, null, null, null, null, null, null, null, null, 
			null, null, null, null, null, null, null, null, null, null, null, null, 
			null, null, null, null, null, null, null, null, null, null, null, null, 
			null, null, null, null, null, null, null, null, null, null, null, null, 
			null, null, null, "NAME", "STRING", "BLOCK_STRING", "ID", "FLOAT", "INT", 
			"PUNCTUATOR", "WS", "COMMA", "LineComment", "UNICODE_BOM", "UTF8_BOM", 
			"UTF16_BOM", "UTF32_BOM"
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
	public String getGrammarFileName() { return "GraphQL.g4"; }

	@Override
	public String[] getRuleNames() { return ruleNames; }

	@Override
	public String getSerializedATN() { return _serializedATN; }

	@Override
	public ATN getATN() { return _ATN; }

	public GraphQLParser(TokenStream input) {
		super(input);
		_interp = new ParserATNSimulator(this,_ATN,_decisionToDFA,_sharedContextCache);
	}

	public static class DocumentContext extends ParserRuleContext {
		public List<DefinitionContext> definition() {
			return getRuleContexts(DefinitionContext.class);
		}
		public DefinitionContext definition(int i) {
			return getRuleContext(DefinitionContext.class,i);
		}
		public DocumentContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_document; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof GraphQLListener ) ((GraphQLListener)listener).enterDocument(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof GraphQLListener ) ((GraphQLListener)listener).exitDocument(this);
		}
	}

	public final DocumentContext document() throws RecognitionException {
		DocumentContext _localctx = new DocumentContext(_ctx, getState());
		enterRule(_localctx, 0, RULE_document);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(143); 
			_errHandler.sync(this);
			_la = _input.LA(1);
			do {
				{
				{
				setState(142);
				definition();
				}
				}
				setState(145); 
				_errHandler.sync(this);
				_la = _input.LA(1);
			} while ( (((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << T__0) | (1L << T__1) | (1L << T__2) | (1L << T__3) | (1L << T__5) | (1L << T__10) | (1L << T__21) | (1L << T__22) | (1L << T__23) | (1L << T__26) | (1L << T__27) | (1L << T__29) | (1L << T__30) | (1L << T__31) | (1L << STRING) | (1L << BLOCK_STRING))) != 0) );
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

	public static class DefinitionContext extends ParserRuleContext {
		public ExecutableDefinitionContext executableDefinition() {
			return getRuleContext(ExecutableDefinitionContext.class,0);
		}
		public TypeSystemDefinitionContext typeSystemDefinition() {
			return getRuleContext(TypeSystemDefinitionContext.class,0);
		}
		public TypeSystemExtensionContext typeSystemExtension() {
			return getRuleContext(TypeSystemExtensionContext.class,0);
		}
		public DefinitionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_definition; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof GraphQLListener ) ((GraphQLListener)listener).enterDefinition(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof GraphQLListener ) ((GraphQLListener)listener).exitDefinition(this);
		}
	}

	public final DefinitionContext definition() throws RecognitionException {
		DefinitionContext _localctx = new DefinitionContext(_ctx, getState());
		enterRule(_localctx, 2, RULE_definition);
		try {
			setState(150);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case T__0:
			case T__1:
			case T__2:
			case T__3:
			case T__10:
				enterOuterAlt(_localctx, 1);
				{
				setState(147);
				executableDefinition();
				}
				break;
			case T__5:
			case T__21:
			case T__23:
			case T__26:
			case T__27:
			case T__29:
			case T__30:
			case T__31:
			case STRING:
			case BLOCK_STRING:
				enterOuterAlt(_localctx, 2);
				{
				setState(148);
				typeSystemDefinition();
				}
				break;
			case T__22:
				enterOuterAlt(_localctx, 3);
				{
				setState(149);
				typeSystemExtension();
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

	public static class ExecutableDefinitionContext extends ParserRuleContext {
		public OperationDefinitionContext operationDefinition() {
			return getRuleContext(OperationDefinitionContext.class,0);
		}
		public FragmentDefinitionContext fragmentDefinition() {
			return getRuleContext(FragmentDefinitionContext.class,0);
		}
		public ExecutableDefinitionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_executableDefinition; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof GraphQLListener ) ((GraphQLListener)listener).enterExecutableDefinition(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof GraphQLListener ) ((GraphQLListener)listener).exitExecutableDefinition(this);
		}
	}

	public final ExecutableDefinitionContext executableDefinition() throws RecognitionException {
		ExecutableDefinitionContext _localctx = new ExecutableDefinitionContext(_ctx, getState());
		enterRule(_localctx, 4, RULE_executableDefinition);
		try {
			setState(154);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case T__0:
			case T__1:
			case T__2:
			case T__3:
				enterOuterAlt(_localctx, 1);
				{
				setState(152);
				operationDefinition();
				}
				break;
			case T__10:
				enterOuterAlt(_localctx, 2);
				{
				setState(153);
				fragmentDefinition();
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

	public static class OperationDefinitionContext extends ParserRuleContext {
		public OperationTypeContext operationType() {
			return getRuleContext(OperationTypeContext.class,0);
		}
		public SelectionSetContext selectionSet() {
			return getRuleContext(SelectionSetContext.class,0);
		}
		public NameContext name() {
			return getRuleContext(NameContext.class,0);
		}
		public VariableDefinitionsContext variableDefinitions() {
			return getRuleContext(VariableDefinitionsContext.class,0);
		}
		public DirectivesContext directives() {
			return getRuleContext(DirectivesContext.class,0);
		}
		public OperationDefinitionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_operationDefinition; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof GraphQLListener ) ((GraphQLListener)listener).enterOperationDefinition(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof GraphQLListener ) ((GraphQLListener)listener).exitOperationDefinition(this);
		}
	}

	public final OperationDefinitionContext operationDefinition() throws RecognitionException {
		OperationDefinitionContext _localctx = new OperationDefinitionContext(_ctx, getState());
		enterRule(_localctx, 6, RULE_operationDefinition);
		int _la;
		try {
			setState(169);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case T__0:
			case T__1:
			case T__2:
				enterOuterAlt(_localctx, 1);
				{
				setState(156);
				operationType();
				setState(158);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==NAME) {
					{
					setState(157);
					name();
					}
				}

				setState(161);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==T__6) {
					{
					setState(160);
					variableDefinitions();
					}
				}

				setState(164);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==T__20) {
					{
					setState(163);
					directives();
					}
				}

				setState(166);
				selectionSet();
				}
				break;
			case T__3:
				enterOuterAlt(_localctx, 2);
				{
				setState(168);
				selectionSet();
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

	public static class OperationTypeContext extends ParserRuleContext {
		public OperationTypeContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_operationType; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof GraphQLListener ) ((GraphQLListener)listener).enterOperationType(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof GraphQLListener ) ((GraphQLListener)listener).exitOperationType(this);
		}
	}

	public final OperationTypeContext operationType() throws RecognitionException {
		OperationTypeContext _localctx = new OperationTypeContext(_ctx, getState());
		enterRule(_localctx, 8, RULE_operationType);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(171);
			_la = _input.LA(1);
			if ( !((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << T__0) | (1L << T__1) | (1L << T__2))) != 0)) ) {
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

	public static class SelectionSetContext extends ParserRuleContext {
		public List<SelectionContext> selection() {
			return getRuleContexts(SelectionContext.class);
		}
		public SelectionContext selection(int i) {
			return getRuleContext(SelectionContext.class,i);
		}
		public SelectionSetContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_selectionSet; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof GraphQLListener ) ((GraphQLListener)listener).enterSelectionSet(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof GraphQLListener ) ((GraphQLListener)listener).exitSelectionSet(this);
		}
	}

	public final SelectionSetContext selectionSet() throws RecognitionException {
		SelectionSetContext _localctx = new SelectionSetContext(_ctx, getState());
		enterRule(_localctx, 10, RULE_selectionSet);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(173);
			match(T__3);
			setState(175); 
			_errHandler.sync(this);
			_la = _input.LA(1);
			do {
				{
				{
				setState(174);
				selection();
				}
				}
				setState(177); 
				_errHandler.sync(this);
				_la = _input.LA(1);
			} while ( (((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << T__5) | (1L << T__9) | (1L << NAME))) != 0) );
			setState(179);
			match(T__4);
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

	public static class SelectionContext extends ParserRuleContext {
		public FieldContext field() {
			return getRuleContext(FieldContext.class,0);
		}
		public FragmentSpreadContext fragmentSpread() {
			return getRuleContext(FragmentSpreadContext.class,0);
		}
		public InlineFragmentContext inlineFragment() {
			return getRuleContext(InlineFragmentContext.class,0);
		}
		public SelectionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_selection; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof GraphQLListener ) ((GraphQLListener)listener).enterSelection(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof GraphQLListener ) ((GraphQLListener)listener).exitSelection(this);
		}
	}

	public final SelectionContext selection() throws RecognitionException {
		SelectionContext _localctx = new SelectionContext(_ctx, getState());
		enterRule(_localctx, 12, RULE_selection);
		try {
			setState(184);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,8,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(181);
				field();
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(182);
				fragmentSpread();
				}
				break;
			case 3:
				enterOuterAlt(_localctx, 3);
				{
				setState(183);
				inlineFragment();
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

	public static class FieldContext extends ParserRuleContext {
		public NameContext name() {
			return getRuleContext(NameContext.class,0);
		}
		public AliasContext alias() {
			return getRuleContext(AliasContext.class,0);
		}
		public ArgumentsContext arguments() {
			return getRuleContext(ArgumentsContext.class,0);
		}
		public DirectivesContext directives() {
			return getRuleContext(DirectivesContext.class,0);
		}
		public SelectionSetContext selectionSet() {
			return getRuleContext(SelectionSetContext.class,0);
		}
		public FieldContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_field; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof GraphQLListener ) ((GraphQLListener)listener).enterField(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof GraphQLListener ) ((GraphQLListener)listener).exitField(this);
		}
	}

	public final FieldContext field() throws RecognitionException {
		FieldContext _localctx = new FieldContext(_ctx, getState());
		enterRule(_localctx, 14, RULE_field);
		int _la;
		try {
			setState(212);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,17,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(187);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,9,_ctx) ) {
				case 1:
					{
					setState(186);
					alias();
					}
					break;
				}
				setState(189);
				name();
				setState(191);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==T__6) {
					{
					setState(190);
					arguments();
					}
				}

				setState(194);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==T__20) {
					{
					setState(193);
					directives();
					}
				}

				setState(197);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==T__3) {
					{
					setState(196);
					selectionSet();
					}
				}

				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(200);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==NAME) {
					{
					setState(199);
					alias();
					}
				}

				setState(202);
				match(T__5);
				setState(204);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==T__6) {
					{
					setState(203);
					arguments();
					}
				}

				setState(207);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==T__20) {
					{
					setState(206);
					directives();
					}
				}

				setState(210);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==T__3) {
					{
					setState(209);
					selectionSet();
					}
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

	public static class ArgumentsContext extends ParserRuleContext {
		public List<ArgumentContext> argument() {
			return getRuleContexts(ArgumentContext.class);
		}
		public ArgumentContext argument(int i) {
			return getRuleContext(ArgumentContext.class,i);
		}
		public ArgumentsContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_arguments; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof GraphQLListener ) ((GraphQLListener)listener).enterArguments(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof GraphQLListener ) ((GraphQLListener)listener).exitArguments(this);
		}
	}

	public final ArgumentsContext arguments() throws RecognitionException {
		ArgumentsContext _localctx = new ArgumentsContext(_ctx, getState());
		enterRule(_localctx, 16, RULE_arguments);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(214);
			match(T__6);
			setState(216); 
			_errHandler.sync(this);
			_la = _input.LA(1);
			do {
				{
				{
				setState(215);
				argument();
				}
				}
				setState(218); 
				_errHandler.sync(this);
				_la = _input.LA(1);
			} while ( _la==NAME );
			setState(220);
			match(T__7);
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

	public static class ArgumentContext extends ParserRuleContext {
		public NameContext name() {
			return getRuleContext(NameContext.class,0);
		}
		public ValueContext value() {
			return getRuleContext(ValueContext.class,0);
		}
		public ArgumentContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_argument; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof GraphQLListener ) ((GraphQLListener)listener).enterArgument(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof GraphQLListener ) ((GraphQLListener)listener).exitArgument(this);
		}
	}

	public final ArgumentContext argument() throws RecognitionException {
		ArgumentContext _localctx = new ArgumentContext(_ctx, getState());
		enterRule(_localctx, 18, RULE_argument);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(222);
			name();
			setState(223);
			match(T__8);
			setState(224);
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

	public static class AliasContext extends ParserRuleContext {
		public NameContext name() {
			return getRuleContext(NameContext.class,0);
		}
		public AliasContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_alias; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof GraphQLListener ) ((GraphQLListener)listener).enterAlias(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof GraphQLListener ) ((GraphQLListener)listener).exitAlias(this);
		}
	}

	public final AliasContext alias() throws RecognitionException {
		AliasContext _localctx = new AliasContext(_ctx, getState());
		enterRule(_localctx, 20, RULE_alias);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(226);
			name();
			setState(227);
			match(T__8);
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

	public static class FragmentSpreadContext extends ParserRuleContext {
		public FragmentNameContext fragmentName() {
			return getRuleContext(FragmentNameContext.class,0);
		}
		public DirectivesContext directives() {
			return getRuleContext(DirectivesContext.class,0);
		}
		public FragmentSpreadContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_fragmentSpread; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof GraphQLListener ) ((GraphQLListener)listener).enterFragmentSpread(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof GraphQLListener ) ((GraphQLListener)listener).exitFragmentSpread(this);
		}
	}

	public final FragmentSpreadContext fragmentSpread() throws RecognitionException {
		FragmentSpreadContext _localctx = new FragmentSpreadContext(_ctx, getState());
		enterRule(_localctx, 22, RULE_fragmentSpread);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(229);
			match(T__9);
			setState(230);
			fragmentName();
			setState(232);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==T__20) {
				{
				setState(231);
				directives();
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

	public static class FragmentDefinitionContext extends ParserRuleContext {
		public FragmentNameContext fragmentName() {
			return getRuleContext(FragmentNameContext.class,0);
		}
		public TypeConditionContext typeCondition() {
			return getRuleContext(TypeConditionContext.class,0);
		}
		public SelectionSetContext selectionSet() {
			return getRuleContext(SelectionSetContext.class,0);
		}
		public DirectivesContext directives() {
			return getRuleContext(DirectivesContext.class,0);
		}
		public FragmentDefinitionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_fragmentDefinition; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof GraphQLListener ) ((GraphQLListener)listener).enterFragmentDefinition(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof GraphQLListener ) ((GraphQLListener)listener).exitFragmentDefinition(this);
		}
	}

	public final FragmentDefinitionContext fragmentDefinition() throws RecognitionException {
		FragmentDefinitionContext _localctx = new FragmentDefinitionContext(_ctx, getState());
		enterRule(_localctx, 24, RULE_fragmentDefinition);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(234);
			match(T__10);
			setState(235);
			fragmentName();
			setState(236);
			typeCondition();
			setState(238);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==T__20) {
				{
				setState(237);
				directives();
				}
			}

			setState(240);
			selectionSet();
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

	public static class FragmentNameContext extends ParserRuleContext {
		public NameContext name() {
			return getRuleContext(NameContext.class,0);
		}
		public FragmentNameContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_fragmentName; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof GraphQLListener ) ((GraphQLListener)listener).enterFragmentName(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof GraphQLListener ) ((GraphQLListener)listener).exitFragmentName(this);
		}
	}

	public final FragmentNameContext fragmentName() throws RecognitionException {
		FragmentNameContext _localctx = new FragmentNameContext(_ctx, getState());
		enterRule(_localctx, 26, RULE_fragmentName);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(242);
			name();
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

	public static class TypeConditionContext extends ParserRuleContext {
		public NamedTypeContext namedType() {
			return getRuleContext(NamedTypeContext.class,0);
		}
		public TypeConditionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_typeCondition; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof GraphQLListener ) ((GraphQLListener)listener).enterTypeCondition(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof GraphQLListener ) ((GraphQLListener)listener).exitTypeCondition(this);
		}
	}

	public final TypeConditionContext typeCondition() throws RecognitionException {
		TypeConditionContext _localctx = new TypeConditionContext(_ctx, getState());
		enterRule(_localctx, 28, RULE_typeCondition);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(244);
			match(T__11);
			setState(245);
			namedType();
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

	public static class InlineFragmentContext extends ParserRuleContext {
		public SelectionSetContext selectionSet() {
			return getRuleContext(SelectionSetContext.class,0);
		}
		public TypeConditionContext typeCondition() {
			return getRuleContext(TypeConditionContext.class,0);
		}
		public DirectivesContext directives() {
			return getRuleContext(DirectivesContext.class,0);
		}
		public InlineFragmentContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_inlineFragment; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof GraphQLListener ) ((GraphQLListener)listener).enterInlineFragment(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof GraphQLListener ) ((GraphQLListener)listener).exitInlineFragment(this);
		}
	}

	public final InlineFragmentContext inlineFragment() throws RecognitionException {
		InlineFragmentContext _localctx = new InlineFragmentContext(_ctx, getState());
		enterRule(_localctx, 30, RULE_inlineFragment);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(247);
			match(T__9);
			setState(249);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==T__11) {
				{
				setState(248);
				typeCondition();
				}
			}

			setState(252);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==T__20) {
				{
				setState(251);
				directives();
				}
			}

			setState(254);
			selectionSet();
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

	public static class ValueContext extends ParserRuleContext {
		public VariableContext variable() {
			return getRuleContext(VariableContext.class,0);
		}
		public IntValueContext intValue() {
			return getRuleContext(IntValueContext.class,0);
		}
		public FloatValueContext floatValue() {
			return getRuleContext(FloatValueContext.class,0);
		}
		public StringValueContext stringValue() {
			return getRuleContext(StringValueContext.class,0);
		}
		public BooleanValueContext booleanValue() {
			return getRuleContext(BooleanValueContext.class,0);
		}
		public NullValueContext nullValue() {
			return getRuleContext(NullValueContext.class,0);
		}
		public EnumValueContext enumValue() {
			return getRuleContext(EnumValueContext.class,0);
		}
		public ListValueContext listValue() {
			return getRuleContext(ListValueContext.class,0);
		}
		public ObjectValueContext objectValue() {
			return getRuleContext(ObjectValueContext.class,0);
		}
		public ValueContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_value; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof GraphQLListener ) ((GraphQLListener)listener).enterValue(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof GraphQLListener ) ((GraphQLListener)listener).exitValue(this);
		}
	}

	public final ValueContext value() throws RecognitionException {
		ValueContext _localctx = new ValueContext(_ctx, getState());
		enterRule(_localctx, 32, RULE_value);
		try {
			setState(265);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case T__17:
				enterOuterAlt(_localctx, 1);
				{
				setState(256);
				variable();
				}
				break;
			case INT:
				enterOuterAlt(_localctx, 2);
				{
				setState(257);
				intValue();
				}
				break;
			case FLOAT:
				enterOuterAlt(_localctx, 3);
				{
				setState(258);
				floatValue();
				}
				break;
			case STRING:
			case BLOCK_STRING:
				enterOuterAlt(_localctx, 4);
				{
				setState(259);
				stringValue();
				}
				break;
			case T__12:
			case T__13:
				enterOuterAlt(_localctx, 5);
				{
				setState(260);
				booleanValue();
				}
				break;
			case T__14:
				enterOuterAlt(_localctx, 6);
				{
				setState(261);
				nullValue();
				}
				break;
			case NAME:
				enterOuterAlt(_localctx, 7);
				{
				setState(262);
				enumValue();
				}
				break;
			case T__15:
				enterOuterAlt(_localctx, 8);
				{
				setState(263);
				listValue();
				}
				break;
			case T__3:
				enterOuterAlt(_localctx, 9);
				{
				setState(264);
				objectValue();
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

	public static class IntValueContext extends ParserRuleContext {
		public TerminalNode INT() { return getToken(GraphQLParser.INT, 0); }
		public IntValueContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_intValue; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof GraphQLListener ) ((GraphQLListener)listener).enterIntValue(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof GraphQLListener ) ((GraphQLListener)listener).exitIntValue(this);
		}
	}

	public final IntValueContext intValue() throws RecognitionException {
		IntValueContext _localctx = new IntValueContext(_ctx, getState());
		enterRule(_localctx, 34, RULE_intValue);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(267);
			match(INT);
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

	public static class FloatValueContext extends ParserRuleContext {
		public TerminalNode FLOAT() { return getToken(GraphQLParser.FLOAT, 0); }
		public FloatValueContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_floatValue; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof GraphQLListener ) ((GraphQLListener)listener).enterFloatValue(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof GraphQLListener ) ((GraphQLListener)listener).exitFloatValue(this);
		}
	}

	public final FloatValueContext floatValue() throws RecognitionException {
		FloatValueContext _localctx = new FloatValueContext(_ctx, getState());
		enterRule(_localctx, 36, RULE_floatValue);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(269);
			match(FLOAT);
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

	public static class BooleanValueContext extends ParserRuleContext {
		public BooleanValueContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_booleanValue; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof GraphQLListener ) ((GraphQLListener)listener).enterBooleanValue(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof GraphQLListener ) ((GraphQLListener)listener).exitBooleanValue(this);
		}
	}

	public final BooleanValueContext booleanValue() throws RecognitionException {
		BooleanValueContext _localctx = new BooleanValueContext(_ctx, getState());
		enterRule(_localctx, 38, RULE_booleanValue);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(271);
			_la = _input.LA(1);
			if ( !(_la==T__12 || _la==T__13) ) {
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

	public static class StringValueContext extends ParserRuleContext {
		public TerminalNode STRING() { return getToken(GraphQLParser.STRING, 0); }
		public TerminalNode BLOCK_STRING() { return getToken(GraphQLParser.BLOCK_STRING, 0); }
		public StringValueContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_stringValue; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof GraphQLListener ) ((GraphQLListener)listener).enterStringValue(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof GraphQLListener ) ((GraphQLListener)listener).exitStringValue(this);
		}
	}

	public final StringValueContext stringValue() throws RecognitionException {
		StringValueContext _localctx = new StringValueContext(_ctx, getState());
		enterRule(_localctx, 40, RULE_stringValue);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(273);
			_la = _input.LA(1);
			if ( !(_la==STRING || _la==BLOCK_STRING) ) {
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

	public static class NullValueContext extends ParserRuleContext {
		public NullValueContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_nullValue; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof GraphQLListener ) ((GraphQLListener)listener).enterNullValue(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof GraphQLListener ) ((GraphQLListener)listener).exitNullValue(this);
		}
	}

	public final NullValueContext nullValue() throws RecognitionException {
		NullValueContext _localctx = new NullValueContext(_ctx, getState());
		enterRule(_localctx, 42, RULE_nullValue);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(275);
			match(T__14);
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

	public static class EnumValueContext extends ParserRuleContext {
		public NameContext name() {
			return getRuleContext(NameContext.class,0);
		}
		public EnumValueContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_enumValue; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof GraphQLListener ) ((GraphQLListener)listener).enterEnumValue(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof GraphQLListener ) ((GraphQLListener)listener).exitEnumValue(this);
		}
	}

	public final EnumValueContext enumValue() throws RecognitionException {
		EnumValueContext _localctx = new EnumValueContext(_ctx, getState());
		enterRule(_localctx, 44, RULE_enumValue);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(277);
			name();
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

	public static class ListValueContext extends ParserRuleContext {
		public List<ValueContext> value() {
			return getRuleContexts(ValueContext.class);
		}
		public ValueContext value(int i) {
			return getRuleContext(ValueContext.class,i);
		}
		public ListValueContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_listValue; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof GraphQLListener ) ((GraphQLListener)listener).enterListValue(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof GraphQLListener ) ((GraphQLListener)listener).exitListValue(this);
		}
	}

	public final ListValueContext listValue() throws RecognitionException {
		ListValueContext _localctx = new ListValueContext(_ctx, getState());
		enterRule(_localctx, 46, RULE_listValue);
		int _la;
		try {
			setState(289);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,25,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(279);
				match(T__15);
				setState(280);
				match(T__16);
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(281);
				match(T__15);
				setState(283); 
				_errHandler.sync(this);
				_la = _input.LA(1);
				do {
					{
					{
					setState(282);
					value();
					}
					}
					setState(285); 
					_errHandler.sync(this);
					_la = _input.LA(1);
				} while ( (((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << T__3) | (1L << T__12) | (1L << T__13) | (1L << T__14) | (1L << T__15) | (1L << T__17) | (1L << NAME) | (1L << STRING) | (1L << BLOCK_STRING) | (1L << FLOAT) | (1L << INT))) != 0) );
				setState(287);
				match(T__16);
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

	public static class ObjectValueContext extends ParserRuleContext {
		public List<ObjectFieldContext> objectField() {
			return getRuleContexts(ObjectFieldContext.class);
		}
		public ObjectFieldContext objectField(int i) {
			return getRuleContext(ObjectFieldContext.class,i);
		}
		public ObjectValueContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_objectValue; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof GraphQLListener ) ((GraphQLListener)listener).enterObjectValue(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof GraphQLListener ) ((GraphQLListener)listener).exitObjectValue(this);
		}
	}

	public final ObjectValueContext objectValue() throws RecognitionException {
		ObjectValueContext _localctx = new ObjectValueContext(_ctx, getState());
		enterRule(_localctx, 48, RULE_objectValue);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(291);
			match(T__3);
			setState(295);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==NAME) {
				{
				{
				setState(292);
				objectField();
				}
				}
				setState(297);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(298);
			match(T__4);
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

	public static class ObjectFieldContext extends ParserRuleContext {
		public NameContext name() {
			return getRuleContext(NameContext.class,0);
		}
		public ValueContext value() {
			return getRuleContext(ValueContext.class,0);
		}
		public ObjectFieldContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_objectField; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof GraphQLListener ) ((GraphQLListener)listener).enterObjectField(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof GraphQLListener ) ((GraphQLListener)listener).exitObjectField(this);
		}
	}

	public final ObjectFieldContext objectField() throws RecognitionException {
		ObjectFieldContext _localctx = new ObjectFieldContext(_ctx, getState());
		enterRule(_localctx, 50, RULE_objectField);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(300);
			name();
			setState(301);
			match(T__8);
			setState(302);
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

	public static class VariableContext extends ParserRuleContext {
		public NameContext name() {
			return getRuleContext(NameContext.class,0);
		}
		public VariableContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_variable; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof GraphQLListener ) ((GraphQLListener)listener).enterVariable(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof GraphQLListener ) ((GraphQLListener)listener).exitVariable(this);
		}
	}

	public final VariableContext variable() throws RecognitionException {
		VariableContext _localctx = new VariableContext(_ctx, getState());
		enterRule(_localctx, 52, RULE_variable);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(304);
			match(T__17);
			setState(305);
			name();
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

	public static class VariableDefinitionsContext extends ParserRuleContext {
		public List<VariableDefinitionContext> variableDefinition() {
			return getRuleContexts(VariableDefinitionContext.class);
		}
		public VariableDefinitionContext variableDefinition(int i) {
			return getRuleContext(VariableDefinitionContext.class,i);
		}
		public VariableDefinitionsContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_variableDefinitions; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof GraphQLListener ) ((GraphQLListener)listener).enterVariableDefinitions(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof GraphQLListener ) ((GraphQLListener)listener).exitVariableDefinitions(this);
		}
	}

	public final VariableDefinitionsContext variableDefinitions() throws RecognitionException {
		VariableDefinitionsContext _localctx = new VariableDefinitionsContext(_ctx, getState());
		enterRule(_localctx, 54, RULE_variableDefinitions);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(307);
			match(T__6);
			setState(309); 
			_errHandler.sync(this);
			_la = _input.LA(1);
			do {
				{
				{
				setState(308);
				variableDefinition();
				}
				}
				setState(311); 
				_errHandler.sync(this);
				_la = _input.LA(1);
			} while ( _la==T__17 );
			setState(313);
			match(T__7);
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

	public static class VariableDefinitionContext extends ParserRuleContext {
		public VariableContext variable() {
			return getRuleContext(VariableContext.class,0);
		}
		public Type_Context type_() {
			return getRuleContext(Type_Context.class,0);
		}
		public DefaultValueContext defaultValue() {
			return getRuleContext(DefaultValueContext.class,0);
		}
		public VariableDefinitionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_variableDefinition; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof GraphQLListener ) ((GraphQLListener)listener).enterVariableDefinition(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof GraphQLListener ) ((GraphQLListener)listener).exitVariableDefinition(this);
		}
	}

	public final VariableDefinitionContext variableDefinition() throws RecognitionException {
		VariableDefinitionContext _localctx = new VariableDefinitionContext(_ctx, getState());
		enterRule(_localctx, 56, RULE_variableDefinition);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(315);
			variable();
			setState(316);
			match(T__8);
			setState(317);
			type_();
			setState(319);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==T__18) {
				{
				setState(318);
				defaultValue();
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

	public static class DefaultValueContext extends ParserRuleContext {
		public ValueContext value() {
			return getRuleContext(ValueContext.class,0);
		}
		public DefaultValueContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_defaultValue; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof GraphQLListener ) ((GraphQLListener)listener).enterDefaultValue(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof GraphQLListener ) ((GraphQLListener)listener).exitDefaultValue(this);
		}
	}

	public final DefaultValueContext defaultValue() throws RecognitionException {
		DefaultValueContext _localctx = new DefaultValueContext(_ctx, getState());
		enterRule(_localctx, 58, RULE_defaultValue);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(321);
			match(T__18);
			setState(322);
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

	public static class Type_Context extends ParserRuleContext {
		public NamedTypeContext namedType() {
			return getRuleContext(NamedTypeContext.class,0);
		}
		public ListTypeContext listType() {
			return getRuleContext(ListTypeContext.class,0);
		}
		public Type_Context(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_type_; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof GraphQLListener ) ((GraphQLListener)listener).enterType_(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof GraphQLListener ) ((GraphQLListener)listener).exitType_(this);
		}
	}

	public final Type_Context type_() throws RecognitionException {
		Type_Context _localctx = new Type_Context(_ctx, getState());
		enterRule(_localctx, 60, RULE_type_);
		int _la;
		try {
			setState(332);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case NAME:
				enterOuterAlt(_localctx, 1);
				{
				setState(324);
				namedType();
				setState(326);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==T__19) {
					{
					setState(325);
					match(T__19);
					}
				}

				}
				break;
			case T__15:
				enterOuterAlt(_localctx, 2);
				{
				setState(328);
				listType();
				setState(330);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==T__19) {
					{
					setState(329);
					match(T__19);
					}
				}

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

	public static class NamedTypeContext extends ParserRuleContext {
		public NameContext name() {
			return getRuleContext(NameContext.class,0);
		}
		public NamedTypeContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_namedType; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof GraphQLListener ) ((GraphQLListener)listener).enterNamedType(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof GraphQLListener ) ((GraphQLListener)listener).exitNamedType(this);
		}
	}

	public final NamedTypeContext namedType() throws RecognitionException {
		NamedTypeContext _localctx = new NamedTypeContext(_ctx, getState());
		enterRule(_localctx, 62, RULE_namedType);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(334);
			name();
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

	public static class ListTypeContext extends ParserRuleContext {
		public Type_Context type_() {
			return getRuleContext(Type_Context.class,0);
		}
		public ListTypeContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_listType; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof GraphQLListener ) ((GraphQLListener)listener).enterListType(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof GraphQLListener ) ((GraphQLListener)listener).exitListType(this);
		}
	}

	public final ListTypeContext listType() throws RecognitionException {
		ListTypeContext _localctx = new ListTypeContext(_ctx, getState());
		enterRule(_localctx, 64, RULE_listType);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(336);
			match(T__15);
			setState(337);
			type_();
			setState(338);
			match(T__16);
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

	public static class DirectivesContext extends ParserRuleContext {
		public List<DirectiveContext> directive() {
			return getRuleContexts(DirectiveContext.class);
		}
		public DirectiveContext directive(int i) {
			return getRuleContext(DirectiveContext.class,i);
		}
		public DirectivesContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_directives; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof GraphQLListener ) ((GraphQLListener)listener).enterDirectives(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof GraphQLListener ) ((GraphQLListener)listener).exitDirectives(this);
		}
	}

	public final DirectivesContext directives() throws RecognitionException {
		DirectivesContext _localctx = new DirectivesContext(_ctx, getState());
		enterRule(_localctx, 66, RULE_directives);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(341); 
			_errHandler.sync(this);
			_la = _input.LA(1);
			do {
				{
				{
				setState(340);
				directive();
				}
				}
				setState(343); 
				_errHandler.sync(this);
				_la = _input.LA(1);
			} while ( _la==T__20 );
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

	public static class DirectiveContext extends ParserRuleContext {
		public NameContext name() {
			return getRuleContext(NameContext.class,0);
		}
		public ArgumentsContext arguments() {
			return getRuleContext(ArgumentsContext.class,0);
		}
		public DirectiveContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_directive; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof GraphQLListener ) ((GraphQLListener)listener).enterDirective(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof GraphQLListener ) ((GraphQLListener)listener).exitDirective(this);
		}
	}

	public final DirectiveContext directive() throws RecognitionException {
		DirectiveContext _localctx = new DirectiveContext(_ctx, getState());
		enterRule(_localctx, 68, RULE_directive);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(345);
			match(T__20);
			setState(346);
			name();
			setState(348);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==T__6) {
				{
				setState(347);
				arguments();
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

	public static class TypeSystemDefinitionContext extends ParserRuleContext {
		public SchemaDefinitionContext schemaDefinition() {
			return getRuleContext(SchemaDefinitionContext.class,0);
		}
		public TypeDefinitionContext typeDefinition() {
			return getRuleContext(TypeDefinitionContext.class,0);
		}
		public DirectiveDefinitionContext directiveDefinition() {
			return getRuleContext(DirectiveDefinitionContext.class,0);
		}
		public TypeSystemDefinitionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_typeSystemDefinition; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof GraphQLListener ) ((GraphQLListener)listener).enterTypeSystemDefinition(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof GraphQLListener ) ((GraphQLListener)listener).exitTypeSystemDefinition(this);
		}
	}

	public final TypeSystemDefinitionContext typeSystemDefinition() throws RecognitionException {
		TypeSystemDefinitionContext _localctx = new TypeSystemDefinitionContext(_ctx, getState());
		enterRule(_localctx, 70, RULE_typeSystemDefinition);
		try {
			setState(353);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,34,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(350);
				schemaDefinition();
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(351);
				typeDefinition();
				}
				break;
			case 3:
				enterOuterAlt(_localctx, 3);
				{
				setState(352);
				directiveDefinition();
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

	public static class TypeSystemExtensionContext extends ParserRuleContext {
		public SchemaExtensionContext schemaExtension() {
			return getRuleContext(SchemaExtensionContext.class,0);
		}
		public TypeExtensionContext typeExtension() {
			return getRuleContext(TypeExtensionContext.class,0);
		}
		public TypeSystemExtensionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_typeSystemExtension; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof GraphQLListener ) ((GraphQLListener)listener).enterTypeSystemExtension(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof GraphQLListener ) ((GraphQLListener)listener).exitTypeSystemExtension(this);
		}
	}

	public final TypeSystemExtensionContext typeSystemExtension() throws RecognitionException {
		TypeSystemExtensionContext _localctx = new TypeSystemExtensionContext(_ctx, getState());
		enterRule(_localctx, 72, RULE_typeSystemExtension);
		try {
			setState(357);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,35,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(355);
				schemaExtension();
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(356);
				typeExtension();
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

	public static class SchemaDefinitionContext extends ParserRuleContext {
		public DirectivesContext directives() {
			return getRuleContext(DirectivesContext.class,0);
		}
		public List<RootOperationTypeDefinitionContext> rootOperationTypeDefinition() {
			return getRuleContexts(RootOperationTypeDefinitionContext.class);
		}
		public RootOperationTypeDefinitionContext rootOperationTypeDefinition(int i) {
			return getRuleContext(RootOperationTypeDefinitionContext.class,i);
		}
		public SchemaDefinitionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_schemaDefinition; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof GraphQLListener ) ((GraphQLListener)listener).enterSchemaDefinition(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof GraphQLListener ) ((GraphQLListener)listener).exitSchemaDefinition(this);
		}
	}

	public final SchemaDefinitionContext schemaDefinition() throws RecognitionException {
		SchemaDefinitionContext _localctx = new SchemaDefinitionContext(_ctx, getState());
		enterRule(_localctx, 74, RULE_schemaDefinition);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(359);
			match(T__21);
			setState(361);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==T__20) {
				{
				setState(360);
				directives();
				}
			}

			setState(363);
			match(T__3);
			setState(365); 
			_errHandler.sync(this);
			_la = _input.LA(1);
			do {
				{
				{
				setState(364);
				rootOperationTypeDefinition();
				}
				}
				setState(367); 
				_errHandler.sync(this);
				_la = _input.LA(1);
			} while ( (((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << T__0) | (1L << T__1) | (1L << T__2))) != 0) );
			setState(369);
			match(T__4);
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

	public static class RootOperationTypeDefinitionContext extends ParserRuleContext {
		public OperationTypeContext operationType() {
			return getRuleContext(OperationTypeContext.class,0);
		}
		public NamedTypeContext namedType() {
			return getRuleContext(NamedTypeContext.class,0);
		}
		public RootOperationTypeDefinitionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_rootOperationTypeDefinition; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof GraphQLListener ) ((GraphQLListener)listener).enterRootOperationTypeDefinition(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof GraphQLListener ) ((GraphQLListener)listener).exitRootOperationTypeDefinition(this);
		}
	}

	public final RootOperationTypeDefinitionContext rootOperationTypeDefinition() throws RecognitionException {
		RootOperationTypeDefinitionContext _localctx = new RootOperationTypeDefinitionContext(_ctx, getState());
		enterRule(_localctx, 76, RULE_rootOperationTypeDefinition);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(371);
			operationType();
			setState(372);
			match(T__8);
			setState(373);
			namedType();
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

	public static class SchemaExtensionContext extends ParserRuleContext {
		public DirectivesContext directives() {
			return getRuleContext(DirectivesContext.class,0);
		}
		public List<OperationTypeDefinitionContext> operationTypeDefinition() {
			return getRuleContexts(OperationTypeDefinitionContext.class);
		}
		public OperationTypeDefinitionContext operationTypeDefinition(int i) {
			return getRuleContext(OperationTypeDefinitionContext.class,i);
		}
		public SchemaExtensionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_schemaExtension; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof GraphQLListener ) ((GraphQLListener)listener).enterSchemaExtension(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof GraphQLListener ) ((GraphQLListener)listener).exitSchemaExtension(this);
		}
	}

	public final SchemaExtensionContext schemaExtension() throws RecognitionException {
		SchemaExtensionContext _localctx = new SchemaExtensionContext(_ctx, getState());
		enterRule(_localctx, 78, RULE_schemaExtension);
		int _la;
		try {
			setState(391);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,40,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(375);
				match(T__22);
				setState(376);
				match(T__21);
				setState(378);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==T__20) {
					{
					setState(377);
					directives();
					}
				}

				setState(380);
				match(T__3);
				setState(382); 
				_errHandler.sync(this);
				_la = _input.LA(1);
				do {
					{
					{
					setState(381);
					operationTypeDefinition();
					}
					}
					setState(384); 
					_errHandler.sync(this);
					_la = _input.LA(1);
				} while ( (((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << T__0) | (1L << T__1) | (1L << T__2))) != 0) );
				setState(386);
				match(T__4);
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(388);
				match(T__22);
				setState(389);
				match(T__21);
				setState(390);
				directives();
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

	public static class OperationTypeDefinitionContext extends ParserRuleContext {
		public OperationTypeContext operationType() {
			return getRuleContext(OperationTypeContext.class,0);
		}
		public NamedTypeContext namedType() {
			return getRuleContext(NamedTypeContext.class,0);
		}
		public OperationTypeDefinitionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_operationTypeDefinition; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof GraphQLListener ) ((GraphQLListener)listener).enterOperationTypeDefinition(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof GraphQLListener ) ((GraphQLListener)listener).exitOperationTypeDefinition(this);
		}
	}

	public final OperationTypeDefinitionContext operationTypeDefinition() throws RecognitionException {
		OperationTypeDefinitionContext _localctx = new OperationTypeDefinitionContext(_ctx, getState());
		enterRule(_localctx, 80, RULE_operationTypeDefinition);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(393);
			operationType();
			setState(394);
			match(T__8);
			setState(395);
			namedType();
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

	public static class DescriptionContext extends ParserRuleContext {
		public StringValueContext stringValue() {
			return getRuleContext(StringValueContext.class,0);
		}
		public DescriptionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_description; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof GraphQLListener ) ((GraphQLListener)listener).enterDescription(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof GraphQLListener ) ((GraphQLListener)listener).exitDescription(this);
		}
	}

	public final DescriptionContext description() throws RecognitionException {
		DescriptionContext _localctx = new DescriptionContext(_ctx, getState());
		enterRule(_localctx, 82, RULE_description);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(397);
			stringValue();
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

	public static class TypeDefinitionContext extends ParserRuleContext {
		public ScalarTypeDefinitionContext scalarTypeDefinition() {
			return getRuleContext(ScalarTypeDefinitionContext.class,0);
		}
		public ObjectTypeDefinitionContext objectTypeDefinition() {
			return getRuleContext(ObjectTypeDefinitionContext.class,0);
		}
		public InterfaceTypeDefinitionContext interfaceTypeDefinition() {
			return getRuleContext(InterfaceTypeDefinitionContext.class,0);
		}
		public UnionTypeDefinitionContext unionTypeDefinition() {
			return getRuleContext(UnionTypeDefinitionContext.class,0);
		}
		public EnumTypeDefinitionContext enumTypeDefinition() {
			return getRuleContext(EnumTypeDefinitionContext.class,0);
		}
		public InputObjectTypeDefinitionContext inputObjectTypeDefinition() {
			return getRuleContext(InputObjectTypeDefinitionContext.class,0);
		}
		public TypeDefinitionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_typeDefinition; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof GraphQLListener ) ((GraphQLListener)listener).enterTypeDefinition(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof GraphQLListener ) ((GraphQLListener)listener).exitTypeDefinition(this);
		}
	}

	public final TypeDefinitionContext typeDefinition() throws RecognitionException {
		TypeDefinitionContext _localctx = new TypeDefinitionContext(_ctx, getState());
		enterRule(_localctx, 84, RULE_typeDefinition);
		try {
			setState(405);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,41,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(399);
				scalarTypeDefinition();
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(400);
				objectTypeDefinition();
				}
				break;
			case 3:
				enterOuterAlt(_localctx, 3);
				{
				setState(401);
				interfaceTypeDefinition();
				}
				break;
			case 4:
				enterOuterAlt(_localctx, 4);
				{
				setState(402);
				unionTypeDefinition();
				}
				break;
			case 5:
				enterOuterAlt(_localctx, 5);
				{
				setState(403);
				enumTypeDefinition();
				}
				break;
			case 6:
				enterOuterAlt(_localctx, 6);
				{
				setState(404);
				inputObjectTypeDefinition();
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

	public static class TypeExtensionContext extends ParserRuleContext {
		public ScalarTypeExtensionContext scalarTypeExtension() {
			return getRuleContext(ScalarTypeExtensionContext.class,0);
		}
		public ObjectTypeExtensionContext objectTypeExtension() {
			return getRuleContext(ObjectTypeExtensionContext.class,0);
		}
		public InterfaceTypeExtensionContext interfaceTypeExtension() {
			return getRuleContext(InterfaceTypeExtensionContext.class,0);
		}
		public UnionTypeExtensionContext unionTypeExtension() {
			return getRuleContext(UnionTypeExtensionContext.class,0);
		}
		public EnumTypeExtensionContext enumTypeExtension() {
			return getRuleContext(EnumTypeExtensionContext.class,0);
		}
		public InputObjectTypeExtensionContext inputObjectTypeExtension() {
			return getRuleContext(InputObjectTypeExtensionContext.class,0);
		}
		public TypeExtensionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_typeExtension; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof GraphQLListener ) ((GraphQLListener)listener).enterTypeExtension(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof GraphQLListener ) ((GraphQLListener)listener).exitTypeExtension(this);
		}
	}

	public final TypeExtensionContext typeExtension() throws RecognitionException {
		TypeExtensionContext _localctx = new TypeExtensionContext(_ctx, getState());
		enterRule(_localctx, 86, RULE_typeExtension);
		try {
			setState(413);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,42,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(407);
				scalarTypeExtension();
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(408);
				objectTypeExtension();
				}
				break;
			case 3:
				enterOuterAlt(_localctx, 3);
				{
				setState(409);
				interfaceTypeExtension();
				}
				break;
			case 4:
				enterOuterAlt(_localctx, 4);
				{
				setState(410);
				unionTypeExtension();
				}
				break;
			case 5:
				enterOuterAlt(_localctx, 5);
				{
				setState(411);
				enumTypeExtension();
				}
				break;
			case 6:
				enterOuterAlt(_localctx, 6);
				{
				setState(412);
				inputObjectTypeExtension();
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

	public static class ScalarTypeDefinitionContext extends ParserRuleContext {
		public NameContext name() {
			return getRuleContext(NameContext.class,0);
		}
		public DescriptionContext description() {
			return getRuleContext(DescriptionContext.class,0);
		}
		public DirectivesContext directives() {
			return getRuleContext(DirectivesContext.class,0);
		}
		public ScalarTypeDefinitionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_scalarTypeDefinition; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof GraphQLListener ) ((GraphQLListener)listener).enterScalarTypeDefinition(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof GraphQLListener ) ((GraphQLListener)listener).exitScalarTypeDefinition(this);
		}
	}

	public final ScalarTypeDefinitionContext scalarTypeDefinition() throws RecognitionException {
		ScalarTypeDefinitionContext _localctx = new ScalarTypeDefinitionContext(_ctx, getState());
		enterRule(_localctx, 88, RULE_scalarTypeDefinition);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(416);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==STRING || _la==BLOCK_STRING) {
				{
				setState(415);
				description();
				}
			}

			setState(418);
			match(T__23);
			setState(419);
			name();
			setState(421);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==T__20) {
				{
				setState(420);
				directives();
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

	public static class ScalarTypeExtensionContext extends ParserRuleContext {
		public NameContext name() {
			return getRuleContext(NameContext.class,0);
		}
		public DirectivesContext directives() {
			return getRuleContext(DirectivesContext.class,0);
		}
		public ScalarTypeExtensionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_scalarTypeExtension; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof GraphQLListener ) ((GraphQLListener)listener).enterScalarTypeExtension(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof GraphQLListener ) ((GraphQLListener)listener).exitScalarTypeExtension(this);
		}
	}

	public final ScalarTypeExtensionContext scalarTypeExtension() throws RecognitionException {
		ScalarTypeExtensionContext _localctx = new ScalarTypeExtensionContext(_ctx, getState());
		enterRule(_localctx, 90, RULE_scalarTypeExtension);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(423);
			match(T__22);
			setState(424);
			match(T__23);
			setState(425);
			name();
			setState(426);
			directives();
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

	public static class ObjectTypeDefinitionContext extends ParserRuleContext {
		public NameContext name() {
			return getRuleContext(NameContext.class,0);
		}
		public DescriptionContext description() {
			return getRuleContext(DescriptionContext.class,0);
		}
		public ImplementsInterfacesContext implementsInterfaces() {
			return getRuleContext(ImplementsInterfacesContext.class,0);
		}
		public DirectivesContext directives() {
			return getRuleContext(DirectivesContext.class,0);
		}
		public FieldsDefinitionContext fieldsDefinition() {
			return getRuleContext(FieldsDefinitionContext.class,0);
		}
		public ObjectTypeDefinitionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_objectTypeDefinition; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof GraphQLListener ) ((GraphQLListener)listener).enterObjectTypeDefinition(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof GraphQLListener ) ((GraphQLListener)listener).exitObjectTypeDefinition(this);
		}
	}

	public final ObjectTypeDefinitionContext objectTypeDefinition() throws RecognitionException {
		ObjectTypeDefinitionContext _localctx = new ObjectTypeDefinitionContext(_ctx, getState());
		enterRule(_localctx, 92, RULE_objectTypeDefinition);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(429);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==STRING || _la==BLOCK_STRING) {
				{
				setState(428);
				description();
				}
			}

			setState(431);
			match(T__5);
			setState(432);
			name();
			setState(434);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==T__24) {
				{
				setState(433);
				implementsInterfaces(0);
				}
			}

			setState(437);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==T__20) {
				{
				setState(436);
				directives();
				}
			}

			setState(440);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,48,_ctx) ) {
			case 1:
				{
				setState(439);
				fieldsDefinition();
				}
				break;
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

	public static class ImplementsInterfacesContext extends ParserRuleContext {
		public NamedTypeContext namedType() {
			return getRuleContext(NamedTypeContext.class,0);
		}
		public ImplementsInterfacesContext implementsInterfaces() {
			return getRuleContext(ImplementsInterfacesContext.class,0);
		}
		public ImplementsInterfacesContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_implementsInterfaces; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof GraphQLListener ) ((GraphQLListener)listener).enterImplementsInterfaces(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof GraphQLListener ) ((GraphQLListener)listener).exitImplementsInterfaces(this);
		}
	}

	public final ImplementsInterfacesContext implementsInterfaces() throws RecognitionException {
		return implementsInterfaces(0);
	}

	private ImplementsInterfacesContext implementsInterfaces(int _p) throws RecognitionException {
		ParserRuleContext _parentctx = _ctx;
		int _parentState = getState();
		ImplementsInterfacesContext _localctx = new ImplementsInterfacesContext(_ctx, _parentState);
		ImplementsInterfacesContext _prevctx = _localctx;
		int _startState = 94;
		enterRecursionRule(_localctx, 94, RULE_implementsInterfaces, _p);
		int _la;
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			{
			setState(443);
			match(T__24);
			setState(445);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==T__25) {
				{
				setState(444);
				match(T__25);
				}
			}

			setState(447);
			namedType();
			}
			_ctx.stop = _input.LT(-1);
			setState(454);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,50,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					if ( _parseListeners!=null ) triggerExitRuleEvent();
					_prevctx = _localctx;
					{
					{
					_localctx = new ImplementsInterfacesContext(_parentctx, _parentState);
					pushNewRecursionContext(_localctx, _startState, RULE_implementsInterfaces);
					setState(449);
					if (!(precpred(_ctx, 1))) throw new FailedPredicateException(this, "precpred(_ctx, 1)");
					setState(450);
					match(T__25);
					setState(451);
					namedType();
					}
					} 
				}
				setState(456);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,50,_ctx);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			unrollRecursionContexts(_parentctx);
		}
		return _localctx;
	}

	public static class FieldsDefinitionContext extends ParserRuleContext {
		public List<FieldDefinitionContext> fieldDefinition() {
			return getRuleContexts(FieldDefinitionContext.class);
		}
		public FieldDefinitionContext fieldDefinition(int i) {
			return getRuleContext(FieldDefinitionContext.class,i);
		}
		public FieldsDefinitionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_fieldsDefinition; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof GraphQLListener ) ((GraphQLListener)listener).enterFieldsDefinition(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof GraphQLListener ) ((GraphQLListener)listener).exitFieldsDefinition(this);
		}
	}

	public final FieldsDefinitionContext fieldsDefinition() throws RecognitionException {
		FieldsDefinitionContext _localctx = new FieldsDefinitionContext(_ctx, getState());
		enterRule(_localctx, 96, RULE_fieldsDefinition);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(457);
			match(T__3);
			setState(459); 
			_errHandler.sync(this);
			_la = _input.LA(1);
			do {
				{
				{
				setState(458);
				fieldDefinition();
				}
				}
				setState(461); 
				_errHandler.sync(this);
				_la = _input.LA(1);
			} while ( (((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << NAME) | (1L << STRING) | (1L << BLOCK_STRING))) != 0) );
			setState(463);
			match(T__4);
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

	public static class FieldDefinitionContext extends ParserRuleContext {
		public NameContext name() {
			return getRuleContext(NameContext.class,0);
		}
		public Type_Context type_() {
			return getRuleContext(Type_Context.class,0);
		}
		public DescriptionContext description() {
			return getRuleContext(DescriptionContext.class,0);
		}
		public ArgumentsDefinitionContext argumentsDefinition() {
			return getRuleContext(ArgumentsDefinitionContext.class,0);
		}
		public DirectivesContext directives() {
			return getRuleContext(DirectivesContext.class,0);
		}
		public FieldDefinitionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_fieldDefinition; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof GraphQLListener ) ((GraphQLListener)listener).enterFieldDefinition(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof GraphQLListener ) ((GraphQLListener)listener).exitFieldDefinition(this);
		}
	}

	public final FieldDefinitionContext fieldDefinition() throws RecognitionException {
		FieldDefinitionContext _localctx = new FieldDefinitionContext(_ctx, getState());
		enterRule(_localctx, 98, RULE_fieldDefinition);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(466);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==STRING || _la==BLOCK_STRING) {
				{
				setState(465);
				description();
				}
			}

			setState(468);
			name();
			setState(470);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==T__6) {
				{
				setState(469);
				argumentsDefinition();
				}
			}

			setState(472);
			match(T__8);
			setState(473);
			type_();
			setState(475);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==T__20) {
				{
				setState(474);
				directives();
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

	public static class ArgumentsDefinitionContext extends ParserRuleContext {
		public List<InputValueDefinitionContext> inputValueDefinition() {
			return getRuleContexts(InputValueDefinitionContext.class);
		}
		public InputValueDefinitionContext inputValueDefinition(int i) {
			return getRuleContext(InputValueDefinitionContext.class,i);
		}
		public ArgumentsDefinitionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_argumentsDefinition; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof GraphQLListener ) ((GraphQLListener)listener).enterArgumentsDefinition(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof GraphQLListener ) ((GraphQLListener)listener).exitArgumentsDefinition(this);
		}
	}

	public final ArgumentsDefinitionContext argumentsDefinition() throws RecognitionException {
		ArgumentsDefinitionContext _localctx = new ArgumentsDefinitionContext(_ctx, getState());
		enterRule(_localctx, 100, RULE_argumentsDefinition);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(477);
			match(T__6);
			setState(479); 
			_errHandler.sync(this);
			_la = _input.LA(1);
			do {
				{
				{
				setState(478);
				inputValueDefinition();
				}
				}
				setState(481); 
				_errHandler.sync(this);
				_la = _input.LA(1);
			} while ( (((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << NAME) | (1L << STRING) | (1L << BLOCK_STRING))) != 0) );
			setState(483);
			match(T__7);
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

	public static class InputValueDefinitionContext extends ParserRuleContext {
		public NameContext name() {
			return getRuleContext(NameContext.class,0);
		}
		public Type_Context type_() {
			return getRuleContext(Type_Context.class,0);
		}
		public DescriptionContext description() {
			return getRuleContext(DescriptionContext.class,0);
		}
		public DefaultValueContext defaultValue() {
			return getRuleContext(DefaultValueContext.class,0);
		}
		public DirectivesContext directives() {
			return getRuleContext(DirectivesContext.class,0);
		}
		public InputValueDefinitionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_inputValueDefinition; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof GraphQLListener ) ((GraphQLListener)listener).enterInputValueDefinition(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof GraphQLListener ) ((GraphQLListener)listener).exitInputValueDefinition(this);
		}
	}

	public final InputValueDefinitionContext inputValueDefinition() throws RecognitionException {
		InputValueDefinitionContext _localctx = new InputValueDefinitionContext(_ctx, getState());
		enterRule(_localctx, 102, RULE_inputValueDefinition);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(486);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==STRING || _la==BLOCK_STRING) {
				{
				setState(485);
				description();
				}
			}

			setState(488);
			name();
			setState(489);
			match(T__8);
			setState(490);
			type_();
			setState(492);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==T__18) {
				{
				setState(491);
				defaultValue();
				}
			}

			setState(495);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==T__20) {
				{
				setState(494);
				directives();
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

	public static class ObjectTypeExtensionContext extends ParserRuleContext {
		public NameContext name() {
			return getRuleContext(NameContext.class,0);
		}
		public FieldsDefinitionContext fieldsDefinition() {
			return getRuleContext(FieldsDefinitionContext.class,0);
		}
		public ImplementsInterfacesContext implementsInterfaces() {
			return getRuleContext(ImplementsInterfacesContext.class,0);
		}
		public DirectivesContext directives() {
			return getRuleContext(DirectivesContext.class,0);
		}
		public ObjectTypeExtensionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_objectTypeExtension; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof GraphQLListener ) ((GraphQLListener)listener).enterObjectTypeExtension(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof GraphQLListener ) ((GraphQLListener)listener).exitObjectTypeExtension(this);
		}
	}

	public final ObjectTypeExtensionContext objectTypeExtension() throws RecognitionException {
		ObjectTypeExtensionContext _localctx = new ObjectTypeExtensionContext(_ctx, getState());
		enterRule(_localctx, 104, RULE_objectTypeExtension);
		int _la;
		try {
			setState(521);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,62,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(497);
				match(T__22);
				setState(498);
				match(T__5);
				setState(499);
				name();
				setState(501);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==T__24) {
					{
					setState(500);
					implementsInterfaces(0);
					}
				}

				setState(504);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==T__20) {
					{
					setState(503);
					directives();
					}
				}

				setState(506);
				fieldsDefinition();
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(508);
				match(T__22);
				setState(509);
				match(T__5);
				setState(510);
				name();
				setState(512);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==T__24) {
					{
					setState(511);
					implementsInterfaces(0);
					}
				}

				setState(514);
				directives();
				}
				break;
			case 3:
				enterOuterAlt(_localctx, 3);
				{
				setState(516);
				match(T__22);
				setState(517);
				match(T__5);
				setState(518);
				name();
				setState(519);
				implementsInterfaces(0);
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

	public static class InterfaceTypeDefinitionContext extends ParserRuleContext {
		public NameContext name() {
			return getRuleContext(NameContext.class,0);
		}
		public DescriptionContext description() {
			return getRuleContext(DescriptionContext.class,0);
		}
		public DirectivesContext directives() {
			return getRuleContext(DirectivesContext.class,0);
		}
		public FieldsDefinitionContext fieldsDefinition() {
			return getRuleContext(FieldsDefinitionContext.class,0);
		}
		public InterfaceTypeDefinitionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_interfaceTypeDefinition; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof GraphQLListener ) ((GraphQLListener)listener).enterInterfaceTypeDefinition(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof GraphQLListener ) ((GraphQLListener)listener).exitInterfaceTypeDefinition(this);
		}
	}

	public final InterfaceTypeDefinitionContext interfaceTypeDefinition() throws RecognitionException {
		InterfaceTypeDefinitionContext _localctx = new InterfaceTypeDefinitionContext(_ctx, getState());
		enterRule(_localctx, 106, RULE_interfaceTypeDefinition);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(524);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==STRING || _la==BLOCK_STRING) {
				{
				setState(523);
				description();
				}
			}

			setState(526);
			match(T__26);
			setState(527);
			name();
			setState(529);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==T__20) {
				{
				setState(528);
				directives();
				}
			}

			setState(532);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,65,_ctx) ) {
			case 1:
				{
				setState(531);
				fieldsDefinition();
				}
				break;
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

	public static class InterfaceTypeExtensionContext extends ParserRuleContext {
		public NameContext name() {
			return getRuleContext(NameContext.class,0);
		}
		public FieldsDefinitionContext fieldsDefinition() {
			return getRuleContext(FieldsDefinitionContext.class,0);
		}
		public DirectivesContext directives() {
			return getRuleContext(DirectivesContext.class,0);
		}
		public InterfaceTypeExtensionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_interfaceTypeExtension; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof GraphQLListener ) ((GraphQLListener)listener).enterInterfaceTypeExtension(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof GraphQLListener ) ((GraphQLListener)listener).exitInterfaceTypeExtension(this);
		}
	}

	public final InterfaceTypeExtensionContext interfaceTypeExtension() throws RecognitionException {
		InterfaceTypeExtensionContext _localctx = new InterfaceTypeExtensionContext(_ctx, getState());
		enterRule(_localctx, 108, RULE_interfaceTypeExtension);
		int _la;
		try {
			setState(547);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,67,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(534);
				match(T__22);
				setState(535);
				match(T__26);
				setState(536);
				name();
				setState(538);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==T__20) {
					{
					setState(537);
					directives();
					}
				}

				setState(540);
				fieldsDefinition();
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(542);
				match(T__22);
				setState(543);
				match(T__26);
				setState(544);
				name();
				setState(545);
				directives();
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

	public static class UnionTypeDefinitionContext extends ParserRuleContext {
		public NameContext name() {
			return getRuleContext(NameContext.class,0);
		}
		public DescriptionContext description() {
			return getRuleContext(DescriptionContext.class,0);
		}
		public DirectivesContext directives() {
			return getRuleContext(DirectivesContext.class,0);
		}
		public UnionMemberTypesContext unionMemberTypes() {
			return getRuleContext(UnionMemberTypesContext.class,0);
		}
		public UnionTypeDefinitionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_unionTypeDefinition; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof GraphQLListener ) ((GraphQLListener)listener).enterUnionTypeDefinition(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof GraphQLListener ) ((GraphQLListener)listener).exitUnionTypeDefinition(this);
		}
	}

	public final UnionTypeDefinitionContext unionTypeDefinition() throws RecognitionException {
		UnionTypeDefinitionContext _localctx = new UnionTypeDefinitionContext(_ctx, getState());
		enterRule(_localctx, 110, RULE_unionTypeDefinition);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(550);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==STRING || _la==BLOCK_STRING) {
				{
				setState(549);
				description();
				}
			}

			setState(552);
			match(T__27);
			setState(553);
			name();
			setState(555);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==T__20) {
				{
				setState(554);
				directives();
				}
			}

			setState(558);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==T__18) {
				{
				setState(557);
				unionMemberTypes();
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

	public static class UnionMemberTypesContext extends ParserRuleContext {
		public List<NamedTypeContext> namedType() {
			return getRuleContexts(NamedTypeContext.class);
		}
		public NamedTypeContext namedType(int i) {
			return getRuleContext(NamedTypeContext.class,i);
		}
		public UnionMemberTypesContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_unionMemberTypes; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof GraphQLListener ) ((GraphQLListener)listener).enterUnionMemberTypes(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof GraphQLListener ) ((GraphQLListener)listener).exitUnionMemberTypes(this);
		}
	}

	public final UnionMemberTypesContext unionMemberTypes() throws RecognitionException {
		UnionMemberTypesContext _localctx = new UnionMemberTypesContext(_ctx, getState());
		enterRule(_localctx, 112, RULE_unionMemberTypes);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(560);
			match(T__18);
			setState(562);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==T__28) {
				{
				setState(561);
				match(T__28);
				}
			}

			setState(564);
			namedType();
			setState(569);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==T__28) {
				{
				{
				setState(565);
				match(T__28);
				setState(566);
				namedType();
				}
				}
				setState(571);
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

	public static class UnionTypeExtensionContext extends ParserRuleContext {
		public NameContext name() {
			return getRuleContext(NameContext.class,0);
		}
		public UnionMemberTypesContext unionMemberTypes() {
			return getRuleContext(UnionMemberTypesContext.class,0);
		}
		public DirectivesContext directives() {
			return getRuleContext(DirectivesContext.class,0);
		}
		public UnionTypeExtensionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_unionTypeExtension; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof GraphQLListener ) ((GraphQLListener)listener).enterUnionTypeExtension(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof GraphQLListener ) ((GraphQLListener)listener).exitUnionTypeExtension(this);
		}
	}

	public final UnionTypeExtensionContext unionTypeExtension() throws RecognitionException {
		UnionTypeExtensionContext _localctx = new UnionTypeExtensionContext(_ctx, getState());
		enterRule(_localctx, 114, RULE_unionTypeExtension);
		int _la;
		try {
			setState(585);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,74,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(572);
				match(T__22);
				setState(573);
				match(T__27);
				setState(574);
				name();
				setState(576);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==T__20) {
					{
					setState(575);
					directives();
					}
				}

				setState(578);
				unionMemberTypes();
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(580);
				match(T__22);
				setState(581);
				match(T__27);
				setState(582);
				name();
				setState(583);
				directives();
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

	public static class EnumTypeDefinitionContext extends ParserRuleContext {
		public NameContext name() {
			return getRuleContext(NameContext.class,0);
		}
		public DescriptionContext description() {
			return getRuleContext(DescriptionContext.class,0);
		}
		public DirectivesContext directives() {
			return getRuleContext(DirectivesContext.class,0);
		}
		public EnumValuesDefinitionContext enumValuesDefinition() {
			return getRuleContext(EnumValuesDefinitionContext.class,0);
		}
		public EnumTypeDefinitionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_enumTypeDefinition; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof GraphQLListener ) ((GraphQLListener)listener).enterEnumTypeDefinition(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof GraphQLListener ) ((GraphQLListener)listener).exitEnumTypeDefinition(this);
		}
	}

	public final EnumTypeDefinitionContext enumTypeDefinition() throws RecognitionException {
		EnumTypeDefinitionContext _localctx = new EnumTypeDefinitionContext(_ctx, getState());
		enterRule(_localctx, 116, RULE_enumTypeDefinition);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(588);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==STRING || _la==BLOCK_STRING) {
				{
				setState(587);
				description();
				}
			}

			setState(590);
			match(T__29);
			setState(591);
			name();
			setState(593);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==T__20) {
				{
				setState(592);
				directives();
				}
			}

			setState(596);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,77,_ctx) ) {
			case 1:
				{
				setState(595);
				enumValuesDefinition();
				}
				break;
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

	public static class EnumValuesDefinitionContext extends ParserRuleContext {
		public List<EnumValueDefinitionContext> enumValueDefinition() {
			return getRuleContexts(EnumValueDefinitionContext.class);
		}
		public EnumValueDefinitionContext enumValueDefinition(int i) {
			return getRuleContext(EnumValueDefinitionContext.class,i);
		}
		public EnumValuesDefinitionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_enumValuesDefinition; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof GraphQLListener ) ((GraphQLListener)listener).enterEnumValuesDefinition(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof GraphQLListener ) ((GraphQLListener)listener).exitEnumValuesDefinition(this);
		}
	}

	public final EnumValuesDefinitionContext enumValuesDefinition() throws RecognitionException {
		EnumValuesDefinitionContext _localctx = new EnumValuesDefinitionContext(_ctx, getState());
		enterRule(_localctx, 118, RULE_enumValuesDefinition);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(598);
			match(T__3);
			setState(600); 
			_errHandler.sync(this);
			_la = _input.LA(1);
			do {
				{
				{
				setState(599);
				enumValueDefinition();
				}
				}
				setState(602); 
				_errHandler.sync(this);
				_la = _input.LA(1);
			} while ( (((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << NAME) | (1L << STRING) | (1L << BLOCK_STRING))) != 0) );
			setState(604);
			match(T__4);
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

	public static class EnumValueDefinitionContext extends ParserRuleContext {
		public EnumValueContext enumValue() {
			return getRuleContext(EnumValueContext.class,0);
		}
		public DescriptionContext description() {
			return getRuleContext(DescriptionContext.class,0);
		}
		public DirectivesContext directives() {
			return getRuleContext(DirectivesContext.class,0);
		}
		public EnumValueDefinitionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_enumValueDefinition; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof GraphQLListener ) ((GraphQLListener)listener).enterEnumValueDefinition(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof GraphQLListener ) ((GraphQLListener)listener).exitEnumValueDefinition(this);
		}
	}

	public final EnumValueDefinitionContext enumValueDefinition() throws RecognitionException {
		EnumValueDefinitionContext _localctx = new EnumValueDefinitionContext(_ctx, getState());
		enterRule(_localctx, 120, RULE_enumValueDefinition);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(607);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==STRING || _la==BLOCK_STRING) {
				{
				setState(606);
				description();
				}
			}

			setState(609);
			enumValue();
			setState(611);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==T__20) {
				{
				setState(610);
				directives();
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

	public static class EnumTypeExtensionContext extends ParserRuleContext {
		public NameContext name() {
			return getRuleContext(NameContext.class,0);
		}
		public EnumValuesDefinitionContext enumValuesDefinition() {
			return getRuleContext(EnumValuesDefinitionContext.class,0);
		}
		public DirectivesContext directives() {
			return getRuleContext(DirectivesContext.class,0);
		}
		public EnumTypeExtensionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_enumTypeExtension; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof GraphQLListener ) ((GraphQLListener)listener).enterEnumTypeExtension(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof GraphQLListener ) ((GraphQLListener)listener).exitEnumTypeExtension(this);
		}
	}

	public final EnumTypeExtensionContext enumTypeExtension() throws RecognitionException {
		EnumTypeExtensionContext _localctx = new EnumTypeExtensionContext(_ctx, getState());
		enterRule(_localctx, 122, RULE_enumTypeExtension);
		int _la;
		try {
			setState(626);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,82,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(613);
				match(T__22);
				setState(614);
				match(T__29);
				setState(615);
				name();
				setState(617);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==T__20) {
					{
					setState(616);
					directives();
					}
				}

				setState(619);
				enumValuesDefinition();
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(621);
				match(T__22);
				setState(622);
				match(T__29);
				setState(623);
				name();
				setState(624);
				directives();
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

	public static class InputObjectTypeDefinitionContext extends ParserRuleContext {
		public NameContext name() {
			return getRuleContext(NameContext.class,0);
		}
		public DescriptionContext description() {
			return getRuleContext(DescriptionContext.class,0);
		}
		public DirectivesContext directives() {
			return getRuleContext(DirectivesContext.class,0);
		}
		public InputFieldsDefinitionContext inputFieldsDefinition() {
			return getRuleContext(InputFieldsDefinitionContext.class,0);
		}
		public InputObjectTypeDefinitionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_inputObjectTypeDefinition; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof GraphQLListener ) ((GraphQLListener)listener).enterInputObjectTypeDefinition(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof GraphQLListener ) ((GraphQLListener)listener).exitInputObjectTypeDefinition(this);
		}
	}

	public final InputObjectTypeDefinitionContext inputObjectTypeDefinition() throws RecognitionException {
		InputObjectTypeDefinitionContext _localctx = new InputObjectTypeDefinitionContext(_ctx, getState());
		enterRule(_localctx, 124, RULE_inputObjectTypeDefinition);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(629);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==STRING || _la==BLOCK_STRING) {
				{
				setState(628);
				description();
				}
			}

			setState(631);
			match(T__30);
			setState(632);
			name();
			setState(634);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==T__20) {
				{
				setState(633);
				directives();
				}
			}

			setState(637);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,85,_ctx) ) {
			case 1:
				{
				setState(636);
				inputFieldsDefinition();
				}
				break;
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

	public static class InputFieldsDefinitionContext extends ParserRuleContext {
		public List<InputValueDefinitionContext> inputValueDefinition() {
			return getRuleContexts(InputValueDefinitionContext.class);
		}
		public InputValueDefinitionContext inputValueDefinition(int i) {
			return getRuleContext(InputValueDefinitionContext.class,i);
		}
		public InputFieldsDefinitionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_inputFieldsDefinition; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof GraphQLListener ) ((GraphQLListener)listener).enterInputFieldsDefinition(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof GraphQLListener ) ((GraphQLListener)listener).exitInputFieldsDefinition(this);
		}
	}

	public final InputFieldsDefinitionContext inputFieldsDefinition() throws RecognitionException {
		InputFieldsDefinitionContext _localctx = new InputFieldsDefinitionContext(_ctx, getState());
		enterRule(_localctx, 126, RULE_inputFieldsDefinition);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(639);
			match(T__3);
			setState(641); 
			_errHandler.sync(this);
			_la = _input.LA(1);
			do {
				{
				{
				setState(640);
				inputValueDefinition();
				}
				}
				setState(643); 
				_errHandler.sync(this);
				_la = _input.LA(1);
			} while ( (((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << NAME) | (1L << STRING) | (1L << BLOCK_STRING))) != 0) );
			setState(645);
			match(T__4);
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

	public static class InputObjectTypeExtensionContext extends ParserRuleContext {
		public NameContext name() {
			return getRuleContext(NameContext.class,0);
		}
		public InputFieldsDefinitionContext inputFieldsDefinition() {
			return getRuleContext(InputFieldsDefinitionContext.class,0);
		}
		public DirectivesContext directives() {
			return getRuleContext(DirectivesContext.class,0);
		}
		public InputObjectTypeExtensionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_inputObjectTypeExtension; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof GraphQLListener ) ((GraphQLListener)listener).enterInputObjectTypeExtension(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof GraphQLListener ) ((GraphQLListener)listener).exitInputObjectTypeExtension(this);
		}
	}

	public final InputObjectTypeExtensionContext inputObjectTypeExtension() throws RecognitionException {
		InputObjectTypeExtensionContext _localctx = new InputObjectTypeExtensionContext(_ctx, getState());
		enterRule(_localctx, 128, RULE_inputObjectTypeExtension);
		int _la;
		try {
			setState(660);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,88,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(647);
				match(T__22);
				setState(648);
				match(T__30);
				setState(649);
				name();
				setState(651);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==T__20) {
					{
					setState(650);
					directives();
					}
				}

				setState(653);
				inputFieldsDefinition();
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(655);
				match(T__22);
				setState(656);
				match(T__30);
				setState(657);
				name();
				setState(658);
				directives();
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

	public static class DirectiveDefinitionContext extends ParserRuleContext {
		public NameContext name() {
			return getRuleContext(NameContext.class,0);
		}
		public DirectiveLocationsContext directiveLocations() {
			return getRuleContext(DirectiveLocationsContext.class,0);
		}
		public DescriptionContext description() {
			return getRuleContext(DescriptionContext.class,0);
		}
		public ArgumentsDefinitionContext argumentsDefinition() {
			return getRuleContext(ArgumentsDefinitionContext.class,0);
		}
		public DirectiveDefinitionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_directiveDefinition; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof GraphQLListener ) ((GraphQLListener)listener).enterDirectiveDefinition(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof GraphQLListener ) ((GraphQLListener)listener).exitDirectiveDefinition(this);
		}
	}

	public final DirectiveDefinitionContext directiveDefinition() throws RecognitionException {
		DirectiveDefinitionContext _localctx = new DirectiveDefinitionContext(_ctx, getState());
		enterRule(_localctx, 130, RULE_directiveDefinition);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(663);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==STRING || _la==BLOCK_STRING) {
				{
				setState(662);
				description();
				}
			}

			setState(665);
			match(T__31);
			setState(666);
			match(T__20);
			setState(667);
			name();
			setState(669);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==T__6) {
				{
				setState(668);
				argumentsDefinition();
				}
			}

			setState(671);
			match(T__11);
			setState(672);
			directiveLocations();
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

	public static class DirectiveLocationsContext extends ParserRuleContext {
		public List<DirectiveLocationContext> directiveLocation() {
			return getRuleContexts(DirectiveLocationContext.class);
		}
		public DirectiveLocationContext directiveLocation(int i) {
			return getRuleContext(DirectiveLocationContext.class,i);
		}
		public DirectiveLocationsContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_directiveLocations; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof GraphQLListener ) ((GraphQLListener)listener).enterDirectiveLocations(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof GraphQLListener ) ((GraphQLListener)listener).exitDirectiveLocations(this);
		}
	}

	public final DirectiveLocationsContext directiveLocations() throws RecognitionException {
		DirectiveLocationsContext _localctx = new DirectiveLocationsContext(_ctx, getState());
		enterRule(_localctx, 132, RULE_directiveLocations);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(674);
			directiveLocation();
			setState(679);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==T__28) {
				{
				{
				setState(675);
				match(T__28);
				setState(676);
				directiveLocation();
				}
				}
				setState(681);
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

	public static class DirectiveLocationContext extends ParserRuleContext {
		public ExecutableDirectiveLocationContext executableDirectiveLocation() {
			return getRuleContext(ExecutableDirectiveLocationContext.class,0);
		}
		public TypeSystemDirectiveLocationContext typeSystemDirectiveLocation() {
			return getRuleContext(TypeSystemDirectiveLocationContext.class,0);
		}
		public DirectiveLocationContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_directiveLocation; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof GraphQLListener ) ((GraphQLListener)listener).enterDirectiveLocation(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof GraphQLListener ) ((GraphQLListener)listener).exitDirectiveLocation(this);
		}
	}

	public final DirectiveLocationContext directiveLocation() throws RecognitionException {
		DirectiveLocationContext _localctx = new DirectiveLocationContext(_ctx, getState());
		enterRule(_localctx, 134, RULE_directiveLocation);
		try {
			setState(684);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case T__32:
			case T__33:
			case T__34:
			case T__35:
			case T__36:
			case T__37:
			case T__38:
				enterOuterAlt(_localctx, 1);
				{
				setState(682);
				executableDirectiveLocation();
				}
				break;
			case T__39:
			case T__40:
			case T__41:
			case T__42:
			case T__43:
			case T__44:
			case T__45:
			case T__46:
			case T__47:
			case T__48:
			case T__49:
				enterOuterAlt(_localctx, 2);
				{
				setState(683);
				typeSystemDirectiveLocation();
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

	public static class ExecutableDirectiveLocationContext extends ParserRuleContext {
		public ExecutableDirectiveLocationContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_executableDirectiveLocation; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof GraphQLListener ) ((GraphQLListener)listener).enterExecutableDirectiveLocation(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof GraphQLListener ) ((GraphQLListener)listener).exitExecutableDirectiveLocation(this);
		}
	}

	public final ExecutableDirectiveLocationContext executableDirectiveLocation() throws RecognitionException {
		ExecutableDirectiveLocationContext _localctx = new ExecutableDirectiveLocationContext(_ctx, getState());
		enterRule(_localctx, 136, RULE_executableDirectiveLocation);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(686);
			_la = _input.LA(1);
			if ( !((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << T__32) | (1L << T__33) | (1L << T__34) | (1L << T__35) | (1L << T__36) | (1L << T__37) | (1L << T__38))) != 0)) ) {
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

	public static class TypeSystemDirectiveLocationContext extends ParserRuleContext {
		public TypeSystemDirectiveLocationContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_typeSystemDirectiveLocation; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof GraphQLListener ) ((GraphQLListener)listener).enterTypeSystemDirectiveLocation(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof GraphQLListener ) ((GraphQLListener)listener).exitTypeSystemDirectiveLocation(this);
		}
	}

	public final TypeSystemDirectiveLocationContext typeSystemDirectiveLocation() throws RecognitionException {
		TypeSystemDirectiveLocationContext _localctx = new TypeSystemDirectiveLocationContext(_ctx, getState());
		enterRule(_localctx, 138, RULE_typeSystemDirectiveLocation);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(688);
			_la = _input.LA(1);
			if ( !((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << T__39) | (1L << T__40) | (1L << T__41) | (1L << T__42) | (1L << T__43) | (1L << T__44) | (1L << T__45) | (1L << T__46) | (1L << T__47) | (1L << T__48) | (1L << T__49))) != 0)) ) {
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

	public static class NameContext extends ParserRuleContext {
		public TerminalNode NAME() { return getToken(GraphQLParser.NAME, 0); }
		public NameContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_name; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof GraphQLListener ) ((GraphQLListener)listener).enterName(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof GraphQLListener ) ((GraphQLListener)listener).exitName(this);
		}
	}

	public final NameContext name() throws RecognitionException {
		NameContext _localctx = new NameContext(_ctx, getState());
		enterRule(_localctx, 140, RULE_name);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(690);
			match(NAME);
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
		case 47:
			return implementsInterfaces_sempred((ImplementsInterfacesContext)_localctx, predIndex);
		}
		return true;
	}
	private boolean implementsInterfaces_sempred(ImplementsInterfacesContext _localctx, int predIndex) {
		switch (predIndex) {
		case 0:
			return precpred(_ctx, 1);
		}
		return true;
	}

	public static final String _serializedATN =
		"\3\u608b\ua72a\u8133\ub9ed\u417c\u3be7\u7786\u5964\3B\u02b7\4\2\t\2\4"+
		"\3\t\3\4\4\t\4\4\5\t\5\4\6\t\6\4\7\t\7\4\b\t\b\4\t\t\t\4\n\t\n\4\13\t"+
		"\13\4\f\t\f\4\r\t\r\4\16\t\16\4\17\t\17\4\20\t\20\4\21\t\21\4\22\t\22"+
		"\4\23\t\23\4\24\t\24\4\25\t\25\4\26\t\26\4\27\t\27\4\30\t\30\4\31\t\31"+
		"\4\32\t\32\4\33\t\33\4\34\t\34\4\35\t\35\4\36\t\36\4\37\t\37\4 \t \4!"+
		"\t!\4\"\t\"\4#\t#\4$\t$\4%\t%\4&\t&\4\'\t\'\4(\t(\4)\t)\4*\t*\4+\t+\4"+
		",\t,\4-\t-\4.\t.\4/\t/\4\60\t\60\4\61\t\61\4\62\t\62\4\63\t\63\4\64\t"+
		"\64\4\65\t\65\4\66\t\66\4\67\t\67\48\t8\49\t9\4:\t:\4;\t;\4<\t<\4=\t="+
		"\4>\t>\4?\t?\4@\t@\4A\tA\4B\tB\4C\tC\4D\tD\4E\tE\4F\tF\4G\tG\4H\tH\3\2"+
		"\6\2\u0092\n\2\r\2\16\2\u0093\3\3\3\3\3\3\5\3\u0099\n\3\3\4\3\4\5\4\u009d"+
		"\n\4\3\5\3\5\5\5\u00a1\n\5\3\5\5\5\u00a4\n\5\3\5\5\5\u00a7\n\5\3\5\3\5"+
		"\3\5\5\5\u00ac\n\5\3\6\3\6\3\7\3\7\6\7\u00b2\n\7\r\7\16\7\u00b3\3\7\3"+
		"\7\3\b\3\b\3\b\5\b\u00bb\n\b\3\t\5\t\u00be\n\t\3\t\3\t\5\t\u00c2\n\t\3"+
		"\t\5\t\u00c5\n\t\3\t\5\t\u00c8\n\t\3\t\5\t\u00cb\n\t\3\t\3\t\5\t\u00cf"+
		"\n\t\3\t\5\t\u00d2\n\t\3\t\5\t\u00d5\n\t\5\t\u00d7\n\t\3\n\3\n\6\n\u00db"+
		"\n\n\r\n\16\n\u00dc\3\n\3\n\3\13\3\13\3\13\3\13\3\f\3\f\3\f\3\r\3\r\3"+
		"\r\5\r\u00eb\n\r\3\16\3\16\3\16\3\16\5\16\u00f1\n\16\3\16\3\16\3\17\3"+
		"\17\3\20\3\20\3\20\3\21\3\21\5\21\u00fc\n\21\3\21\5\21\u00ff\n\21\3\21"+
		"\3\21\3\22\3\22\3\22\3\22\3\22\3\22\3\22\3\22\3\22\5\22\u010c\n\22\3\23"+
		"\3\23\3\24\3\24\3\25\3\25\3\26\3\26\3\27\3\27\3\30\3\30\3\31\3\31\3\31"+
		"\3\31\6\31\u011e\n\31\r\31\16\31\u011f\3\31\3\31\5\31\u0124\n\31\3\32"+
		"\3\32\7\32\u0128\n\32\f\32\16\32\u012b\13\32\3\32\3\32\3\33\3\33\3\33"+
		"\3\33\3\34\3\34\3\34\3\35\3\35\6\35\u0138\n\35\r\35\16\35\u0139\3\35\3"+
		"\35\3\36\3\36\3\36\3\36\5\36\u0142\n\36\3\37\3\37\3\37\3 \3 \5 \u0149"+
		"\n \3 \3 \5 \u014d\n \5 \u014f\n \3!\3!\3\"\3\"\3\"\3\"\3#\6#\u0158\n"+
		"#\r#\16#\u0159\3$\3$\3$\5$\u015f\n$\3%\3%\3%\5%\u0164\n%\3&\3&\5&\u0168"+
		"\n&\3\'\3\'\5\'\u016c\n\'\3\'\3\'\6\'\u0170\n\'\r\'\16\'\u0171\3\'\3\'"+
		"\3(\3(\3(\3(\3)\3)\3)\5)\u017d\n)\3)\3)\6)\u0181\n)\r)\16)\u0182\3)\3"+
		")\3)\3)\3)\5)\u018a\n)\3*\3*\3*\3*\3+\3+\3,\3,\3,\3,\3,\3,\5,\u0198\n"+
		",\3-\3-\3-\3-\3-\3-\5-\u01a0\n-\3.\5.\u01a3\n.\3.\3.\3.\5.\u01a8\n.\3"+
		"/\3/\3/\3/\3/\3\60\5\60\u01b0\n\60\3\60\3\60\3\60\5\60\u01b5\n\60\3\60"+
		"\5\60\u01b8\n\60\3\60\5\60\u01bb\n\60\3\61\3\61\3\61\5\61\u01c0\n\61\3"+
		"\61\3\61\3\61\3\61\3\61\7\61\u01c7\n\61\f\61\16\61\u01ca\13\61\3\62\3"+
		"\62\6\62\u01ce\n\62\r\62\16\62\u01cf\3\62\3\62\3\63\5\63\u01d5\n\63\3"+
		"\63\3\63\5\63\u01d9\n\63\3\63\3\63\3\63\5\63\u01de\n\63\3\64\3\64\6\64"+
		"\u01e2\n\64\r\64\16\64\u01e3\3\64\3\64\3\65\5\65\u01e9\n\65\3\65\3\65"+
		"\3\65\3\65\5\65\u01ef\n\65\3\65\5\65\u01f2\n\65\3\66\3\66\3\66\3\66\5"+
		"\66\u01f8\n\66\3\66\5\66\u01fb\n\66\3\66\3\66\3\66\3\66\3\66\3\66\5\66"+
		"\u0203\n\66\3\66\3\66\3\66\3\66\3\66\3\66\3\66\5\66\u020c\n\66\3\67\5"+
		"\67\u020f\n\67\3\67\3\67\3\67\5\67\u0214\n\67\3\67\5\67\u0217\n\67\38"+
		"\38\38\38\58\u021d\n8\38\38\38\38\38\38\38\58\u0226\n8\39\59\u0229\n9"+
		"\39\39\39\59\u022e\n9\39\59\u0231\n9\3:\3:\5:\u0235\n:\3:\3:\3:\7:\u023a"+
		"\n:\f:\16:\u023d\13:\3;\3;\3;\3;\5;\u0243\n;\3;\3;\3;\3;\3;\3;\3;\5;\u024c"+
		"\n;\3<\5<\u024f\n<\3<\3<\3<\5<\u0254\n<\3<\5<\u0257\n<\3=\3=\6=\u025b"+
		"\n=\r=\16=\u025c\3=\3=\3>\5>\u0262\n>\3>\3>\5>\u0266\n>\3?\3?\3?\3?\5"+
		"?\u026c\n?\3?\3?\3?\3?\3?\3?\3?\5?\u0275\n?\3@\5@\u0278\n@\3@\3@\3@\5"+
		"@\u027d\n@\3@\5@\u0280\n@\3A\3A\6A\u0284\nA\rA\16A\u0285\3A\3A\3B\3B\3"+
		"B\3B\5B\u028e\nB\3B\3B\3B\3B\3B\3B\3B\5B\u0297\nB\3C\5C\u029a\nC\3C\3"+
		"C\3C\3C\5C\u02a0\nC\3C\3C\3C\3D\3D\3D\7D\u02a8\nD\fD\16D\u02ab\13D\3E"+
		"\3E\5E\u02af\nE\3F\3F\3G\3G\3H\3H\3H\2\3`I\2\4\6\b\n\f\16\20\22\24\26"+
		"\30\32\34\36 \"$&(*,.\60\62\64\668:<>@BDFHJLNPRTVXZ\\^`bdfhjlnprtvxz|"+
		"~\u0080\u0082\u0084\u0086\u0088\u008a\u008c\u008e\2\7\3\2\3\5\3\2\17\20"+
		"\3\2\66\67\3\2#)\3\2*\64\2\u02df\2\u0091\3\2\2\2\4\u0098\3\2\2\2\6\u009c"+
		"\3\2\2\2\b\u00ab\3\2\2\2\n\u00ad\3\2\2\2\f\u00af\3\2\2\2\16\u00ba\3\2"+
		"\2\2\20\u00d6\3\2\2\2\22\u00d8\3\2\2\2\24\u00e0\3\2\2\2\26\u00e4\3\2\2"+
		"\2\30\u00e7\3\2\2\2\32\u00ec\3\2\2\2\34\u00f4\3\2\2\2\36\u00f6\3\2\2\2"+
		" \u00f9\3\2\2\2\"\u010b\3\2\2\2$\u010d\3\2\2\2&\u010f\3\2\2\2(\u0111\3"+
		"\2\2\2*\u0113\3\2\2\2,\u0115\3\2\2\2.\u0117\3\2\2\2\60\u0123\3\2\2\2\62"+
		"\u0125\3\2\2\2\64\u012e\3\2\2\2\66\u0132\3\2\2\28\u0135\3\2\2\2:\u013d"+
		"\3\2\2\2<\u0143\3\2\2\2>\u014e\3\2\2\2@\u0150\3\2\2\2B\u0152\3\2\2\2D"+
		"\u0157\3\2\2\2F\u015b\3\2\2\2H\u0163\3\2\2\2J\u0167\3\2\2\2L\u0169\3\2"+
		"\2\2N\u0175\3\2\2\2P\u0189\3\2\2\2R\u018b\3\2\2\2T\u018f\3\2\2\2V\u0197"+
		"\3\2\2\2X\u019f\3\2\2\2Z\u01a2\3\2\2\2\\\u01a9\3\2\2\2^\u01af\3\2\2\2"+
		"`\u01bc\3\2\2\2b\u01cb\3\2\2\2d\u01d4\3\2\2\2f\u01df\3\2\2\2h\u01e8\3"+
		"\2\2\2j\u020b\3\2\2\2l\u020e\3\2\2\2n\u0225\3\2\2\2p\u0228\3\2\2\2r\u0232"+
		"\3\2\2\2t\u024b\3\2\2\2v\u024e\3\2\2\2x\u0258\3\2\2\2z\u0261\3\2\2\2|"+
		"\u0274\3\2\2\2~\u0277\3\2\2\2\u0080\u0281\3\2\2\2\u0082\u0296\3\2\2\2"+
		"\u0084\u0299\3\2\2\2\u0086\u02a4\3\2\2\2\u0088\u02ae\3\2\2\2\u008a\u02b0"+
		"\3\2\2\2\u008c\u02b2\3\2\2\2\u008e\u02b4\3\2\2\2\u0090\u0092\5\4\3\2\u0091"+
		"\u0090\3\2\2\2\u0092\u0093\3\2\2\2\u0093\u0091\3\2\2\2\u0093\u0094\3\2"+
		"\2\2\u0094\3\3\2\2\2\u0095\u0099\5\6\4\2\u0096\u0099\5H%\2\u0097\u0099"+
		"\5J&\2\u0098\u0095\3\2\2\2\u0098\u0096\3\2\2\2\u0098\u0097\3\2\2\2\u0099"+
		"\5\3\2\2\2\u009a\u009d\5\b\5\2\u009b\u009d\5\32\16\2\u009c\u009a\3\2\2"+
		"\2\u009c\u009b\3\2\2\2\u009d\7\3\2\2\2\u009e\u00a0\5\n\6\2\u009f\u00a1"+
		"\5\u008eH\2\u00a0\u009f\3\2\2\2\u00a0\u00a1\3\2\2\2\u00a1\u00a3\3\2\2"+
		"\2\u00a2\u00a4\58\35\2\u00a3\u00a2\3\2\2\2\u00a3\u00a4\3\2\2\2\u00a4\u00a6"+
		"\3\2\2\2\u00a5\u00a7\5D#\2\u00a6\u00a5\3\2\2\2\u00a6\u00a7\3\2\2\2\u00a7"+
		"\u00a8\3\2\2\2\u00a8\u00a9\5\f\7\2\u00a9\u00ac\3\2\2\2\u00aa\u00ac\5\f"+
		"\7\2\u00ab\u009e\3\2\2\2\u00ab\u00aa\3\2\2\2\u00ac\t\3\2\2\2\u00ad\u00ae"+
		"\t\2\2\2\u00ae\13\3\2\2\2\u00af\u00b1\7\6\2\2\u00b0\u00b2\5\16\b\2\u00b1"+
		"\u00b0\3\2\2\2\u00b2\u00b3\3\2\2\2\u00b3\u00b1\3\2\2\2\u00b3\u00b4\3\2"+
		"\2\2\u00b4\u00b5\3\2\2\2\u00b5\u00b6\7\7\2\2\u00b6\r\3\2\2\2\u00b7\u00bb"+
		"\5\20\t\2\u00b8\u00bb\5\30\r\2\u00b9\u00bb\5 \21\2\u00ba\u00b7\3\2\2\2"+
		"\u00ba\u00b8\3\2\2\2\u00ba\u00b9\3\2\2\2\u00bb\17\3\2\2\2\u00bc\u00be"+
		"\5\26\f\2\u00bd\u00bc\3\2\2\2\u00bd\u00be\3\2\2\2\u00be\u00bf\3\2\2\2"+
		"\u00bf\u00c1\5\u008eH\2\u00c0\u00c2\5\22\n\2\u00c1\u00c0\3\2\2\2\u00c1"+
		"\u00c2\3\2\2\2\u00c2\u00c4\3\2\2\2\u00c3\u00c5\5D#\2\u00c4\u00c3\3\2\2"+
		"\2\u00c4\u00c5\3\2\2\2\u00c5\u00c7\3\2\2\2\u00c6\u00c8\5\f\7\2\u00c7\u00c6"+
		"\3\2\2\2\u00c7\u00c8\3\2\2\2\u00c8\u00d7\3\2\2\2\u00c9\u00cb\5\26\f\2"+
		"\u00ca\u00c9\3\2\2\2\u00ca\u00cb\3\2\2\2\u00cb\u00cc\3\2\2\2\u00cc\u00ce"+
		"\7\b\2\2\u00cd\u00cf\5\22\n\2\u00ce\u00cd\3\2\2\2\u00ce\u00cf\3\2\2\2"+
		"\u00cf\u00d1\3\2\2\2\u00d0\u00d2\5D#\2\u00d1\u00d0\3\2\2\2\u00d1\u00d2"+
		"\3\2\2\2\u00d2\u00d4\3\2\2\2\u00d3\u00d5\5\f\7\2\u00d4\u00d3\3\2\2\2\u00d4"+
		"\u00d5\3\2\2\2\u00d5\u00d7\3\2\2\2\u00d6\u00bd\3\2\2\2\u00d6\u00ca\3\2"+
		"\2\2\u00d7\21\3\2\2\2\u00d8\u00da\7\t\2\2\u00d9\u00db\5\24\13\2\u00da"+
		"\u00d9\3\2\2\2\u00db\u00dc\3\2\2\2\u00dc\u00da\3\2\2\2\u00dc\u00dd\3\2"+
		"\2\2\u00dd\u00de\3\2\2\2\u00de\u00df\7\n\2\2\u00df\23\3\2\2\2\u00e0\u00e1"+
		"\5\u008eH\2\u00e1\u00e2\7\13\2\2\u00e2\u00e3\5\"\22\2\u00e3\25\3\2\2\2"+
		"\u00e4\u00e5\5\u008eH\2\u00e5\u00e6\7\13\2\2\u00e6\27\3\2\2\2\u00e7\u00e8"+
		"\7\f\2\2\u00e8\u00ea\5\34\17\2\u00e9\u00eb\5D#\2\u00ea\u00e9\3\2\2\2\u00ea"+
		"\u00eb\3\2\2\2\u00eb\31\3\2\2\2\u00ec\u00ed\7\r\2\2\u00ed\u00ee\5\34\17"+
		"\2\u00ee\u00f0\5\36\20\2\u00ef\u00f1\5D#\2\u00f0\u00ef\3\2\2\2\u00f0\u00f1"+
		"\3\2\2\2\u00f1\u00f2\3\2\2\2\u00f2\u00f3\5\f\7\2\u00f3\33\3\2\2\2\u00f4"+
		"\u00f5\5\u008eH\2\u00f5\35\3\2\2\2\u00f6\u00f7\7\16\2\2\u00f7\u00f8\5"+
		"@!\2\u00f8\37\3\2\2\2\u00f9\u00fb\7\f\2\2\u00fa\u00fc\5\36\20\2\u00fb"+
		"\u00fa\3\2\2\2\u00fb\u00fc\3\2\2\2\u00fc\u00fe\3\2\2\2\u00fd\u00ff\5D"+
		"#\2\u00fe\u00fd\3\2\2\2\u00fe\u00ff\3\2\2\2\u00ff\u0100\3\2\2\2\u0100"+
		"\u0101\5\f\7\2\u0101!\3\2\2\2\u0102\u010c\5\66\34\2\u0103\u010c\5$\23"+
		"\2\u0104\u010c\5&\24\2\u0105\u010c\5*\26\2\u0106\u010c\5(\25\2\u0107\u010c"+
		"\5,\27\2\u0108\u010c\5.\30\2\u0109\u010c\5\60\31\2\u010a\u010c\5\62\32"+
		"\2\u010b\u0102\3\2\2\2\u010b\u0103\3\2\2\2\u010b\u0104\3\2\2\2\u010b\u0105"+
		"\3\2\2\2\u010b\u0106\3\2\2\2\u010b\u0107\3\2\2\2\u010b\u0108\3\2\2\2\u010b"+
		"\u0109\3\2\2\2\u010b\u010a\3\2\2\2\u010c#\3\2\2\2\u010d\u010e\7:\2\2\u010e"+
		"%\3\2\2\2\u010f\u0110\79\2\2\u0110\'\3\2\2\2\u0111\u0112\t\3\2\2\u0112"+
		")\3\2\2\2\u0113\u0114\t\4\2\2\u0114+\3\2\2\2\u0115\u0116\7\21\2\2\u0116"+
		"-\3\2\2\2\u0117\u0118\5\u008eH\2\u0118/\3\2\2\2\u0119\u011a\7\22\2\2\u011a"+
		"\u0124\7\23\2\2\u011b\u011d\7\22\2\2\u011c\u011e\5\"\22\2\u011d\u011c"+
		"\3\2\2\2\u011e\u011f\3\2\2\2\u011f\u011d\3\2\2\2\u011f\u0120\3\2\2\2\u0120"+
		"\u0121\3\2\2\2\u0121\u0122\7\23\2\2\u0122\u0124\3\2\2\2\u0123\u0119\3"+
		"\2\2\2\u0123\u011b\3\2\2\2\u0124\61\3\2\2\2\u0125\u0129\7\6\2\2\u0126"+
		"\u0128\5\64\33\2\u0127\u0126\3\2\2\2\u0128\u012b\3\2\2\2\u0129\u0127\3"+
		"\2\2\2\u0129\u012a\3\2\2\2\u012a\u012c\3\2\2\2\u012b\u0129\3\2\2\2\u012c"+
		"\u012d\7\7\2\2\u012d\63\3\2\2\2\u012e\u012f\5\u008eH\2\u012f\u0130\7\13"+
		"\2\2\u0130\u0131\5\"\22\2\u0131\65\3\2\2\2\u0132\u0133\7\24\2\2\u0133"+
		"\u0134\5\u008eH\2\u0134\67\3\2\2\2\u0135\u0137\7\t\2\2\u0136\u0138\5:"+
		"\36\2\u0137\u0136\3\2\2\2\u0138\u0139\3\2\2\2\u0139\u0137\3\2\2\2\u0139"+
		"\u013a\3\2\2\2\u013a\u013b\3\2\2\2\u013b\u013c\7\n\2\2\u013c9\3\2\2\2"+
		"\u013d\u013e\5\66\34\2\u013e\u013f\7\13\2\2\u013f\u0141\5> \2\u0140\u0142"+
		"\5<\37\2\u0141\u0140\3\2\2\2\u0141\u0142\3\2\2\2\u0142;\3\2\2\2\u0143"+
		"\u0144\7\25\2\2\u0144\u0145\5\"\22\2\u0145=\3\2\2\2\u0146\u0148\5@!\2"+
		"\u0147\u0149\7\26\2\2\u0148\u0147\3\2\2\2\u0148\u0149\3\2\2\2\u0149\u014f"+
		"\3\2\2\2\u014a\u014c\5B\"\2\u014b\u014d\7\26\2\2\u014c\u014b\3\2\2\2\u014c"+
		"\u014d\3\2\2\2\u014d\u014f\3\2\2\2\u014e\u0146\3\2\2\2\u014e\u014a\3\2"+
		"\2\2\u014f?\3\2\2\2\u0150\u0151\5\u008eH\2\u0151A\3\2\2\2\u0152\u0153"+
		"\7\22\2\2\u0153\u0154\5> \2\u0154\u0155\7\23\2\2\u0155C\3\2\2\2\u0156"+
		"\u0158\5F$\2\u0157\u0156\3\2\2\2\u0158\u0159\3\2\2\2\u0159\u0157\3\2\2"+
		"\2\u0159\u015a\3\2\2\2\u015aE\3\2\2\2\u015b\u015c\7\27\2\2\u015c\u015e"+
		"\5\u008eH\2\u015d\u015f\5\22\n\2\u015e\u015d\3\2\2\2\u015e\u015f\3\2\2"+
		"\2\u015fG\3\2\2\2\u0160\u0164\5L\'\2\u0161\u0164\5V,\2\u0162\u0164\5\u0084"+
		"C\2\u0163\u0160\3\2\2\2\u0163\u0161\3\2\2\2\u0163\u0162\3\2\2\2\u0164"+
		"I\3\2\2\2\u0165\u0168\5P)\2\u0166\u0168\5X-\2\u0167\u0165\3\2\2\2\u0167"+
		"\u0166\3\2\2\2\u0168K\3\2\2\2\u0169\u016b\7\30\2\2\u016a\u016c\5D#\2\u016b"+
		"\u016a\3\2\2\2\u016b\u016c\3\2\2\2\u016c\u016d\3\2\2\2\u016d\u016f\7\6"+
		"\2\2\u016e\u0170\5N(\2\u016f\u016e\3\2\2\2\u0170\u0171\3\2\2\2\u0171\u016f"+
		"\3\2\2\2\u0171\u0172\3\2\2\2\u0172\u0173\3\2\2\2\u0173\u0174\7\7\2\2\u0174"+
		"M\3\2\2\2\u0175\u0176\5\n\6\2\u0176\u0177\7\13\2\2\u0177\u0178\5@!\2\u0178"+
		"O\3\2\2\2\u0179\u017a\7\31\2\2\u017a\u017c\7\30\2\2\u017b\u017d\5D#\2"+
		"\u017c\u017b\3\2\2\2\u017c\u017d\3\2\2\2\u017d\u017e\3\2\2\2\u017e\u0180"+
		"\7\6\2\2\u017f\u0181\5R*\2\u0180\u017f\3\2\2\2\u0181\u0182\3\2\2\2\u0182"+
		"\u0180\3\2\2\2\u0182\u0183\3\2\2\2\u0183\u0184\3\2\2\2\u0184\u0185\7\7"+
		"\2\2\u0185\u018a\3\2\2\2\u0186\u0187\7\31\2\2\u0187\u0188\7\30\2\2\u0188"+
		"\u018a\5D#\2\u0189\u0179\3\2\2\2\u0189\u0186\3\2\2\2\u018aQ\3\2\2\2\u018b"+
		"\u018c\5\n\6\2\u018c\u018d\7\13\2\2\u018d\u018e\5@!\2\u018eS\3\2\2\2\u018f"+
		"\u0190\5*\26\2\u0190U\3\2\2\2\u0191\u0198\5Z.\2\u0192\u0198\5^\60\2\u0193"+
		"\u0198\5l\67\2\u0194\u0198\5p9\2\u0195\u0198\5v<\2\u0196\u0198\5~@\2\u0197"+
		"\u0191\3\2\2\2\u0197\u0192\3\2\2\2\u0197\u0193\3\2\2\2\u0197\u0194\3\2"+
		"\2\2\u0197\u0195\3\2\2\2\u0197\u0196\3\2\2\2\u0198W\3\2\2\2\u0199\u01a0"+
		"\5\\/\2\u019a\u01a0\5j\66\2\u019b\u01a0\5n8\2\u019c\u01a0\5t;\2\u019d"+
		"\u01a0\5|?\2\u019e\u01a0\5\u0082B\2\u019f\u0199\3\2\2\2\u019f\u019a\3"+
		"\2\2\2\u019f\u019b\3\2\2\2\u019f\u019c\3\2\2\2\u019f\u019d\3\2\2\2\u019f"+
		"\u019e\3\2\2\2\u01a0Y\3\2\2\2\u01a1\u01a3\5T+\2\u01a2\u01a1\3\2\2\2\u01a2"+
		"\u01a3\3\2\2\2\u01a3\u01a4\3\2\2\2\u01a4\u01a5\7\32\2\2\u01a5\u01a7\5"+
		"\u008eH\2\u01a6\u01a8\5D#\2\u01a7\u01a6\3\2\2\2\u01a7\u01a8\3\2\2\2\u01a8"+
		"[\3\2\2\2\u01a9\u01aa\7\31\2\2\u01aa\u01ab\7\32\2\2\u01ab\u01ac\5\u008e"+
		"H\2\u01ac\u01ad\5D#\2\u01ad]\3\2\2\2\u01ae\u01b0\5T+\2\u01af\u01ae\3\2"+
		"\2\2\u01af\u01b0\3\2\2\2\u01b0\u01b1\3\2\2\2\u01b1\u01b2\7\b\2\2\u01b2"+
		"\u01b4\5\u008eH\2\u01b3\u01b5\5`\61\2\u01b4\u01b3\3\2\2\2\u01b4\u01b5"+
		"\3\2\2\2\u01b5\u01b7\3\2\2\2\u01b6\u01b8\5D#\2\u01b7\u01b6\3\2\2\2\u01b7"+
		"\u01b8\3\2\2\2\u01b8\u01ba\3\2\2\2\u01b9\u01bb\5b\62\2\u01ba\u01b9\3\2"+
		"\2\2\u01ba\u01bb\3\2\2\2\u01bb_\3\2\2\2\u01bc\u01bd\b\61\1\2\u01bd\u01bf"+
		"\7\33\2\2\u01be\u01c0\7\34\2\2\u01bf\u01be\3\2\2\2\u01bf\u01c0\3\2\2\2"+
		"\u01c0\u01c1\3\2\2\2\u01c1\u01c2\5@!\2\u01c2\u01c8\3\2\2\2\u01c3\u01c4"+
		"\f\3\2\2\u01c4\u01c5\7\34\2\2\u01c5\u01c7\5@!\2\u01c6\u01c3\3\2\2\2\u01c7"+
		"\u01ca\3\2\2\2\u01c8\u01c6\3\2\2\2\u01c8\u01c9\3\2\2\2\u01c9a\3\2\2\2"+
		"\u01ca\u01c8\3\2\2\2\u01cb\u01cd\7\6\2\2\u01cc\u01ce\5d\63\2\u01cd\u01cc"+
		"\3\2\2\2\u01ce\u01cf\3\2\2\2\u01cf\u01cd\3\2\2\2\u01cf\u01d0\3\2\2\2\u01d0"+
		"\u01d1\3\2\2\2\u01d1\u01d2\7\7\2\2\u01d2c\3\2\2\2\u01d3\u01d5\5T+\2\u01d4"+
		"\u01d3\3\2\2\2\u01d4\u01d5\3\2\2\2\u01d5\u01d6\3\2\2\2\u01d6\u01d8\5\u008e"+
		"H\2\u01d7\u01d9\5f\64\2\u01d8\u01d7\3\2\2\2\u01d8\u01d9\3\2\2\2\u01d9"+
		"\u01da\3\2\2\2\u01da\u01db\7\13\2\2\u01db\u01dd\5> \2\u01dc\u01de\5D#"+
		"\2\u01dd\u01dc\3\2\2\2\u01dd\u01de\3\2\2\2\u01dee\3\2\2\2\u01df\u01e1"+
		"\7\t\2\2\u01e0\u01e2\5h\65\2\u01e1\u01e0\3\2\2\2\u01e2\u01e3\3\2\2\2\u01e3"+
		"\u01e1\3\2\2\2\u01e3\u01e4\3\2\2\2\u01e4\u01e5\3\2\2\2\u01e5\u01e6\7\n"+
		"\2\2\u01e6g\3\2\2\2\u01e7\u01e9\5T+\2\u01e8\u01e7\3\2\2\2\u01e8\u01e9"+
		"\3\2\2\2\u01e9\u01ea\3\2\2\2\u01ea\u01eb\5\u008eH\2\u01eb\u01ec\7\13\2"+
		"\2\u01ec\u01ee\5> \2\u01ed\u01ef\5<\37\2\u01ee\u01ed\3\2\2\2\u01ee\u01ef"+
		"\3\2\2\2\u01ef\u01f1\3\2\2\2\u01f0\u01f2\5D#\2\u01f1\u01f0\3\2\2\2\u01f1"+
		"\u01f2\3\2\2\2\u01f2i\3\2\2\2\u01f3\u01f4\7\31\2\2\u01f4\u01f5\7\b\2\2"+
		"\u01f5\u01f7\5\u008eH\2\u01f6\u01f8\5`\61\2\u01f7\u01f6\3\2\2\2\u01f7"+
		"\u01f8\3\2\2\2\u01f8\u01fa\3\2\2\2\u01f9\u01fb\5D#\2\u01fa\u01f9\3\2\2"+
		"\2\u01fa\u01fb\3\2\2\2\u01fb\u01fc\3\2\2\2\u01fc\u01fd\5b\62\2\u01fd\u020c"+
		"\3\2\2\2\u01fe\u01ff\7\31\2\2\u01ff\u0200\7\b\2\2\u0200\u0202\5\u008e"+
		"H\2\u0201\u0203\5`\61\2\u0202\u0201\3\2\2\2\u0202\u0203\3\2\2\2\u0203"+
		"\u0204\3\2\2\2\u0204\u0205\5D#\2\u0205\u020c\3\2\2\2\u0206\u0207\7\31"+
		"\2\2\u0207\u0208\7\b\2\2\u0208\u0209\5\u008eH\2\u0209\u020a\5`\61\2\u020a"+
		"\u020c\3\2\2\2\u020b\u01f3\3\2\2\2\u020b\u01fe\3\2\2\2\u020b\u0206\3\2"+
		"\2\2\u020ck\3\2\2\2\u020d\u020f\5T+\2\u020e\u020d\3\2\2\2\u020e\u020f"+
		"\3\2\2\2\u020f\u0210\3\2\2\2\u0210\u0211\7\35\2\2\u0211\u0213\5\u008e"+
		"H\2\u0212\u0214\5D#\2\u0213\u0212\3\2\2\2\u0213\u0214\3\2\2\2\u0214\u0216"+
		"\3\2\2\2\u0215\u0217\5b\62\2\u0216\u0215\3\2\2\2\u0216\u0217\3\2\2\2\u0217"+
		"m\3\2\2\2\u0218\u0219\7\31\2\2\u0219\u021a\7\35\2\2\u021a\u021c\5\u008e"+
		"H\2\u021b\u021d\5D#\2\u021c\u021b\3\2\2\2\u021c\u021d\3\2\2\2\u021d\u021e"+
		"\3\2\2\2\u021e\u021f\5b\62\2\u021f\u0226\3\2\2\2\u0220\u0221\7\31\2\2"+
		"\u0221\u0222\7\35\2\2\u0222\u0223\5\u008eH\2\u0223\u0224\5D#\2\u0224\u0226"+
		"\3\2\2\2\u0225\u0218\3\2\2\2\u0225\u0220\3\2\2\2\u0226o\3\2\2\2\u0227"+
		"\u0229\5T+\2\u0228\u0227\3\2\2\2\u0228\u0229\3\2\2\2\u0229\u022a\3\2\2"+
		"\2\u022a\u022b\7\36\2\2\u022b\u022d\5\u008eH\2\u022c\u022e\5D#\2\u022d"+
		"\u022c\3\2\2\2\u022d\u022e\3\2\2\2\u022e\u0230\3\2\2\2\u022f\u0231\5r"+
		":\2\u0230\u022f\3\2\2\2\u0230\u0231\3\2\2\2\u0231q\3\2\2\2\u0232\u0234"+
		"\7\25\2\2\u0233\u0235\7\37\2\2\u0234\u0233\3\2\2\2\u0234\u0235\3\2\2\2"+
		"\u0235\u0236\3\2\2\2\u0236\u023b\5@!\2\u0237\u0238\7\37\2\2\u0238\u023a"+
		"\5@!\2\u0239\u0237\3\2\2\2\u023a\u023d\3\2\2\2\u023b\u0239\3\2\2\2\u023b"+
		"\u023c\3\2\2\2\u023cs\3\2\2\2\u023d\u023b\3\2\2\2\u023e\u023f\7\31\2\2"+
		"\u023f\u0240\7\36\2\2\u0240\u0242\5\u008eH\2\u0241\u0243\5D#\2\u0242\u0241"+
		"\3\2\2\2\u0242\u0243\3\2\2\2\u0243\u0244\3\2\2\2\u0244\u0245\5r:\2\u0245"+
		"\u024c\3\2\2\2\u0246\u0247\7\31\2\2\u0247\u0248\7\36\2\2\u0248\u0249\5"+
		"\u008eH\2\u0249\u024a\5D#\2\u024a\u024c\3\2\2\2\u024b\u023e\3\2\2\2\u024b"+
		"\u0246\3\2\2\2\u024cu\3\2\2\2\u024d\u024f\5T+\2\u024e\u024d\3\2\2\2\u024e"+
		"\u024f\3\2\2\2\u024f\u0250\3\2\2\2\u0250\u0251\7 \2\2\u0251\u0253\5\u008e"+
		"H\2\u0252\u0254\5D#\2\u0253\u0252\3\2\2\2\u0253\u0254\3\2\2\2\u0254\u0256"+
		"\3\2\2\2\u0255\u0257\5x=\2\u0256\u0255\3\2\2\2\u0256\u0257\3\2\2\2\u0257"+
		"w\3\2\2\2\u0258\u025a\7\6\2\2\u0259\u025b\5z>\2\u025a\u0259\3\2\2\2\u025b"+
		"\u025c\3\2\2\2\u025c\u025a\3\2\2\2\u025c\u025d\3\2\2\2\u025d\u025e\3\2"+
		"\2\2\u025e\u025f\7\7\2\2\u025fy\3\2\2\2\u0260\u0262\5T+\2\u0261\u0260"+
		"\3\2\2\2\u0261\u0262\3\2\2\2\u0262\u0263\3\2\2\2\u0263\u0265\5.\30\2\u0264"+
		"\u0266\5D#\2\u0265\u0264\3\2\2\2\u0265\u0266\3\2\2\2\u0266{\3\2\2\2\u0267"+
		"\u0268\7\31\2\2\u0268\u0269\7 \2\2\u0269\u026b\5\u008eH\2\u026a\u026c"+
		"\5D#\2\u026b\u026a\3\2\2\2\u026b\u026c\3\2\2\2\u026c\u026d\3\2\2\2\u026d"+
		"\u026e\5x=\2\u026e\u0275\3\2\2\2\u026f\u0270\7\31\2\2\u0270\u0271\7 \2"+
		"\2\u0271\u0272\5\u008eH\2\u0272\u0273\5D#\2\u0273\u0275\3\2\2\2\u0274"+
		"\u0267\3\2\2\2\u0274\u026f\3\2\2\2\u0275}\3\2\2\2\u0276\u0278\5T+\2\u0277"+
		"\u0276\3\2\2\2\u0277\u0278\3\2\2\2\u0278\u0279\3\2\2\2\u0279\u027a\7!"+
		"\2\2\u027a\u027c\5\u008eH\2\u027b\u027d\5D#\2\u027c\u027b\3\2\2\2\u027c"+
		"\u027d\3\2\2\2\u027d\u027f\3\2\2\2\u027e\u0280\5\u0080A\2\u027f\u027e"+
		"\3\2\2\2\u027f\u0280\3\2\2\2\u0280\177\3\2\2\2\u0281\u0283\7\6\2\2\u0282"+
		"\u0284\5h\65\2\u0283\u0282\3\2\2\2\u0284\u0285\3\2\2\2\u0285\u0283\3\2"+
		"\2\2\u0285\u0286\3\2\2\2\u0286\u0287\3\2\2\2\u0287\u0288\7\7\2\2\u0288"+
		"\u0081\3\2\2\2\u0289\u028a\7\31\2\2\u028a\u028b\7!\2\2\u028b\u028d\5\u008e"+
		"H\2\u028c\u028e\5D#\2\u028d\u028c\3\2\2\2\u028d\u028e\3\2\2\2\u028e\u028f"+
		"\3\2\2\2\u028f\u0290\5\u0080A\2\u0290\u0297\3\2\2\2\u0291\u0292\7\31\2"+
		"\2\u0292\u0293\7!\2\2\u0293\u0294\5\u008eH\2\u0294\u0295\5D#\2\u0295\u0297"+
		"\3\2\2\2\u0296\u0289\3\2\2\2\u0296\u0291\3\2\2\2\u0297\u0083\3\2\2\2\u0298"+
		"\u029a\5T+\2\u0299\u0298\3\2\2\2\u0299\u029a\3\2\2\2\u029a\u029b\3\2\2"+
		"\2\u029b\u029c\7\"\2\2\u029c\u029d\7\27\2\2\u029d\u029f\5\u008eH\2\u029e"+
		"\u02a0\5f\64\2\u029f\u029e\3\2\2\2\u029f\u02a0\3\2\2\2\u02a0\u02a1\3\2"+
		"\2\2\u02a1\u02a2\7\16\2\2\u02a2\u02a3\5\u0086D\2\u02a3\u0085\3\2\2\2\u02a4"+
		"\u02a9\5\u0088E\2\u02a5\u02a6\7\37\2\2\u02a6\u02a8\5\u0088E\2\u02a7\u02a5"+
		"\3\2\2\2\u02a8\u02ab\3\2\2\2\u02a9\u02a7\3\2\2\2\u02a9\u02aa\3\2\2\2\u02aa"+
		"\u0087\3\2\2\2\u02ab\u02a9\3\2\2\2\u02ac\u02af\5\u008aF\2\u02ad\u02af"+
		"\5\u008cG\2\u02ae\u02ac\3\2\2\2\u02ae\u02ad\3\2\2\2\u02af\u0089\3\2\2"+
		"\2\u02b0\u02b1\t\5\2\2\u02b1\u008b\3\2\2\2\u02b2\u02b3\t\6\2\2\u02b3\u008d"+
		"\3\2\2\2\u02b4\u02b5\7\65\2\2\u02b5\u008f\3\2\2\2_\u0093\u0098\u009c\u00a0"+
		"\u00a3\u00a6\u00ab\u00b3\u00ba\u00bd\u00c1\u00c4\u00c7\u00ca\u00ce\u00d1"+
		"\u00d4\u00d6\u00dc\u00ea\u00f0\u00fb\u00fe\u010b\u011f\u0123\u0129\u0139"+
		"\u0141\u0148\u014c\u014e\u0159\u015e\u0163\u0167\u016b\u0171\u017c\u0182"+
		"\u0189\u0197\u019f\u01a2\u01a7\u01af\u01b4\u01b7\u01ba\u01bf\u01c8\u01cf"+
		"\u01d4\u01d8\u01dd\u01e3\u01e8\u01ee\u01f1\u01f7\u01fa\u0202\u020b\u020e"+
		"\u0213\u0216\u021c\u0225\u0228\u022d\u0230\u0234\u023b\u0242\u024b\u024e"+
		"\u0253\u0256\u025c\u0261\u0265\u026b\u0274\u0277\u027c\u027f\u0285\u028d"+
		"\u0296\u0299\u029f\u02a9\u02ae";
	public static final ATN _ATN =
		new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}
