// $ANTLR 3.5.2 XPath.g 2014-07-31 11:35:01
package tools.xor.util.xpath;
import org.antlr.runtime.BitSet;
import org.antlr.runtime.MismatchedSetException;
import org.antlr.runtime.NoViableAltException;
import org.antlr.runtime.Parser;
import org.antlr.runtime.ParserRuleReturnScope;
import org.antlr.runtime.RecognitionException;
import org.antlr.runtime.RecognizerSharedState;
import org.antlr.runtime.Token;
import org.antlr.runtime.TokenStream;
import org.antlr.runtime.tree.CommonTreeAdaptor;
import org.antlr.runtime.tree.RewriteRuleSubtreeStream;
import org.antlr.runtime.tree.TreeAdaptor;


@SuppressWarnings("all")
public class XPathParser extends Parser {
	public static final String[] tokenNames = new String[] {
		"<invalid>", "<EOR>", "<DOWN>", "<UP>", "ABBRSTEP", "ABRPATH", "ABSOLUTE", 
		"APOS", "AT", "AXIS", "AxisName", "CC", "COLON", "COMMA", "DOT", "DOTDOT", 
		"Digits", "EXPR", "GE", "LBRAC", "LE", "LESS", "LOCATION", "LPAR", "Literal", 
		"MINUS", "MORE", "MUL", "NCName", "NCNameChar", "NCNameStartChar", "NODE", 
		"NodeType", "Number", "PATHSEP", "PIPE", "PLUS", "PREDICATE", "QUOT", 
		"RBRAC", "ROOT", "RPAR", "Whitespace", "'!='", "'$'", "'='", "'and'", 
		"'div'", "'mod'", "'or'", "'processing-instruction'"
	};
	public static final int EOF=-1;
	public static final int T__43=43;
	public static final int T__44=44;
	public static final int T__45=45;
	public static final int T__46=46;
	public static final int T__47=47;
	public static final int T__48=48;
	public static final int T__49=49;
	public static final int T__50=50;
	public static final int ABBRSTEP=4;
	public static final int ABRPATH=5;
	public static final int ABSOLUTE=6;
	public static final int APOS=7;
	public static final int AT=8;
	public static final int AXIS=9;
	public static final int AxisName=10;
	public static final int CC=11;
	public static final int COLON=12;
	public static final int COMMA=13;
	public static final int DOT=14;
	public static final int DOTDOT=15;
	public static final int Digits=16;
	public static final int EXPR=17;
	public static final int GE=18;
	public static final int LBRAC=19;
	public static final int LE=20;
	public static final int LESS=21;
	public static final int LOCATION=22;
	public static final int LPAR=23;
	public static final int Literal=24;
	public static final int MINUS=25;
	public static final int MORE=26;
	public static final int MUL=27;
	public static final int NCName=28;
	public static final int NCNameChar=29;
	public static final int NCNameStartChar=30;
	public static final int NODE=31;
	public static final int NodeType=32;
	public static final int Number=33;
	public static final int PATHSEP=34;
	public static final int PIPE=35;
	public static final int PLUS=36;
	public static final int PREDICATE=37;
	public static final int QUOT=38;
	public static final int RBRAC=39;
	public static final int ROOT=40;
	public static final int RPAR=41;
	public static final int Whitespace=42;

	// delegates
	public Parser[] getDelegates() {
		return new Parser[] {};
	}

	// delegators


	public XPathParser(TokenStream input) {
		this(input, new RecognizerSharedState());
	}
	public XPathParser(TokenStream input, RecognizerSharedState state) {
		super(input, state);
	}

	protected TreeAdaptor adaptor = new CommonTreeAdaptor();

	public void setTreeAdaptor(TreeAdaptor adaptor) {
		this.adaptor = adaptor;
	}
	public TreeAdaptor getTreeAdaptor() {
		return adaptor;
	}
	@Override public String[] getTokenNames() { return XPathParser.tokenNames; }
	@Override public String getGrammarFileName() { return "XPath.g"; }


	public static class main_return extends ParserRuleReturnScope {
		Object tree;
		@Override
		public Object getTree() { return tree; }
	};


	// $ANTLR start "main"
	// XPath.g:57:1: main : expr -> ^( ROOT expr ) ;
	public final XPathParser.main_return main() throws RecognitionException {
		XPathParser.main_return retval = new XPathParser.main_return();
		retval.start = input.LT(1);

		Object root_0 = null;

		ParserRuleReturnScope expr1 =null;

		RewriteRuleSubtreeStream stream_expr=new RewriteRuleSubtreeStream(adaptor,"rule expr");

		try {
			// XPath.g:57:7: ( expr -> ^( ROOT expr ) )
			// XPath.g:57:10: expr
			{
			pushFollow(FOLLOW_expr_in_main316);
			expr1=expr();
			state._fsp--;

			stream_expr.add(expr1.getTree());
			// AST REWRITE
			// elements: expr
			// token labels: 
			// rule labels: retval
			// token list labels: 
			// rule list labels: 
			// wildcard labels: 
			retval.tree = root_0;
			RewriteRuleSubtreeStream stream_retval=new RewriteRuleSubtreeStream(adaptor,"rule retval",retval!=null?retval.getTree():null);

			root_0 = (Object)adaptor.nil();
			// 57:15: -> ^( ROOT expr )
			{
				// XPath.g:57:18: ^( ROOT expr )
				{
				Object root_1 = (Object)adaptor.nil();
				root_1 = (Object)adaptor.becomeRoot((Object)adaptor.create(ROOT, "ROOT"), root_1);
				adaptor.addChild(root_1, stream_expr.nextTree());
				adaptor.addChild(root_0, root_1);
				}

			}


			retval.tree = root_0;

			}

			retval.stop = input.LT(-1);

			retval.tree = (Object)adaptor.rulePostProcessing(root_0);
			adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);

		}
		catch (RecognitionException re) {
			reportError(re);
			recover(input,re);
			retval.tree = (Object)adaptor.errorNode(input, retval.start, input.LT(-1), re);
		}
		finally {
			// do for sure before leaving
		}
		return retval;
	}
	// $ANTLR end "main"


	public static class locationPathI_return extends ParserRuleReturnScope {
		Object tree;
		@Override
		public Object getTree() { return tree; }
	};


	// $ANTLR start "locationPathI"
	// XPath.g:60:1: locationPathI : locationPath -> ^( LOCATION locationPath ) ;
	public final XPathParser.locationPathI_return locationPathI() throws RecognitionException {
		XPathParser.locationPathI_return retval = new XPathParser.locationPathI_return();
		retval.start = input.LT(1);

		Object root_0 = null;

		ParserRuleReturnScope locationPath2 =null;

		RewriteRuleSubtreeStream stream_locationPath=new RewriteRuleSubtreeStream(adaptor,"rule locationPath");

		try {
			// XPath.g:61:3: ( locationPath -> ^( LOCATION locationPath ) )
			// XPath.g:61:6: locationPath
			{
			pushFollow(FOLLOW_locationPath_in_locationPathI338);
			locationPath2=locationPath();
			state._fsp--;

			stream_locationPath.add(locationPath2.getTree());
			// AST REWRITE
			// elements: locationPath
			// token labels: 
			// rule labels: retval
			// token list labels: 
			// rule list labels: 
			// wildcard labels: 
			retval.tree = root_0;
			RewriteRuleSubtreeStream stream_retval=new RewriteRuleSubtreeStream(adaptor,"rule retval",retval!=null?retval.getTree():null);

			root_0 = (Object)adaptor.nil();
			// 61:19: -> ^( LOCATION locationPath )
			{
				// XPath.g:61:22: ^( LOCATION locationPath )
				{
				Object root_1 = (Object)adaptor.nil();
				root_1 = (Object)adaptor.becomeRoot((Object)adaptor.create(LOCATION, "LOCATION"), root_1);
				adaptor.addChild(root_1, stream_locationPath.nextTree());
				adaptor.addChild(root_0, root_1);
				}

			}


			retval.tree = root_0;

			}

			retval.stop = input.LT(-1);

			retval.tree = (Object)adaptor.rulePostProcessing(root_0);
			adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);

		}
		catch (RecognitionException re) {
			reportError(re);
			recover(input,re);
			retval.tree = (Object)adaptor.errorNode(input, retval.start, input.LT(-1), re);
		}
		finally {
			// do for sure before leaving
		}
		return retval;
	}
	// $ANTLR end "locationPathI"


	public static class locationPath_return extends ParserRuleReturnScope {
		Object tree;
		@Override
		public Object getTree() { return tree; }
	};


	// $ANTLR start "locationPath"
	// XPath.g:64:1: locationPath : ( relativeLocationPath | absolutePathI relativeLocationPath );
	public final XPathParser.locationPath_return locationPath() throws RecognitionException {
		XPathParser.locationPath_return retval = new XPathParser.locationPath_return();
		retval.start = input.LT(1);

		Object root_0 = null;

		ParserRuleReturnScope relativeLocationPath3 =null;
		ParserRuleReturnScope absolutePathI4 =null;
		ParserRuleReturnScope relativeLocationPath5 =null;


		try {
			// XPath.g:65:3: ( relativeLocationPath | absolutePathI relativeLocationPath )
			int alt1=2;
			int LA1_0 = input.LA(1);
			if ( (LA1_0==AT||LA1_0==AxisName||(LA1_0 >= DOT && LA1_0 <= DOTDOT)||(LA1_0 >= MUL && LA1_0 <= NCName)||LA1_0==NodeType||LA1_0==50) ) {
				alt1=1;
			}
			else if ( (LA1_0==ABRPATH||LA1_0==PATHSEP) ) {
				alt1=2;
			}

			else {
				NoViableAltException nvae =
					new NoViableAltException("", 1, 0, input);
				throw nvae;
			}

			switch (alt1) {
				case 1 :
					// XPath.g:65:6: relativeLocationPath
					{
					root_0 = (Object)adaptor.nil();


					pushFollow(FOLLOW_relativeLocationPath_in_locationPath361);
					relativeLocationPath3=relativeLocationPath();
					state._fsp--;

					adaptor.addChild(root_0, relativeLocationPath3.getTree());

					}
					break;
				case 2 :
					// XPath.g:66:6: absolutePathI relativeLocationPath
					{
					root_0 = (Object)adaptor.nil();


					pushFollow(FOLLOW_absolutePathI_in_locationPath368);
					absolutePathI4=absolutePathI();
					state._fsp--;

					adaptor.addChild(root_0, absolutePathI4.getTree());

					pushFollow(FOLLOW_relativeLocationPath_in_locationPath370);
					relativeLocationPath5=relativeLocationPath();
					state._fsp--;

					adaptor.addChild(root_0, relativeLocationPath5.getTree());

					}
					break;

			}
			retval.stop = input.LT(-1);

			retval.tree = (Object)adaptor.rulePostProcessing(root_0);
			adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);

		}
		catch (RecognitionException re) {
			reportError(re);
			recover(input,re);
			retval.tree = (Object)adaptor.errorNode(input, retval.start, input.LT(-1), re);
		}
		finally {
			// do for sure before leaving
		}
		return retval;
	}
	// $ANTLR end "locationPath"


	public static class absolutePathI_return extends ParserRuleReturnScope {
		Object tree;
		@Override
		public Object getTree() { return tree; }
	};


	// $ANTLR start "absolutePathI"
	// XPath.g:69:1: absolutePathI : absolutePath -> ^( ABSOLUTE absolutePath ) ;
	public final XPathParser.absolutePathI_return absolutePathI() throws RecognitionException {
		XPathParser.absolutePathI_return retval = new XPathParser.absolutePathI_return();
		retval.start = input.LT(1);

		Object root_0 = null;

		ParserRuleReturnScope absolutePath6 =null;

		RewriteRuleSubtreeStream stream_absolutePath=new RewriteRuleSubtreeStream(adaptor,"rule absolutePath");

		try {
			// XPath.g:70:3: ( absolutePath -> ^( ABSOLUTE absolutePath ) )
			// XPath.g:70:6: absolutePath
			{
			pushFollow(FOLLOW_absolutePath_in_absolutePathI384);
			absolutePath6=absolutePath();
			state._fsp--;

			stream_absolutePath.add(absolutePath6.getTree());
			// AST REWRITE
			// elements: absolutePath
			// token labels: 
			// rule labels: retval
			// token list labels: 
			// rule list labels: 
			// wildcard labels: 
			retval.tree = root_0;
			RewriteRuleSubtreeStream stream_retval=new RewriteRuleSubtreeStream(adaptor,"rule retval",retval!=null?retval.getTree():null);

			root_0 = (Object)adaptor.nil();
			// 70:19: -> ^( ABSOLUTE absolutePath )
			{
				// XPath.g:70:22: ^( ABSOLUTE absolutePath )
				{
				Object root_1 = (Object)adaptor.nil();
				root_1 = (Object)adaptor.becomeRoot((Object)adaptor.create(ABSOLUTE, "ABSOLUTE"), root_1);
				adaptor.addChild(root_1, stream_absolutePath.nextTree());
				adaptor.addChild(root_0, root_1);
				}

			}


			retval.tree = root_0;

			}

			retval.stop = input.LT(-1);

			retval.tree = (Object)adaptor.rulePostProcessing(root_0);
			adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);

		}
		catch (RecognitionException re) {
			reportError(re);
			recover(input,re);
			retval.tree = (Object)adaptor.errorNode(input, retval.start, input.LT(-1), re);
		}
		finally {
			// do for sure before leaving
		}
		return retval;
	}
	// $ANTLR end "absolutePathI"


	public static class absolutePath_return extends ParserRuleReturnScope {
		Object tree;
		@Override
		public Object getTree() { return tree; }
	};


	// $ANTLR start "absolutePath"
	// XPath.g:73:1: absolutePath : ( '/' | '//' );
	public final XPathParser.absolutePath_return absolutePath() throws RecognitionException {
		XPathParser.absolutePath_return retval = new XPathParser.absolutePath_return();
		retval.start = input.LT(1);

		Object root_0 = null;

		Token set7=null;

		Object set7_tree=null;

		try {
			// XPath.g:74:3: ( '/' | '//' )
			// XPath.g:
			{
			root_0 = (Object)adaptor.nil();


			set7=input.LT(1);
			if ( input.LA(1)==ABRPATH||input.LA(1)==PATHSEP ) {
				input.consume();
				adaptor.addChild(root_0, (Object)adaptor.create(set7));
				state.errorRecovery=false;
			}
			else {
				MismatchedSetException mse = new MismatchedSetException(null,input);
				throw mse;
			}
			}

			retval.stop = input.LT(-1);

			retval.tree = (Object)adaptor.rulePostProcessing(root_0);
			adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);

		}
		catch (RecognitionException re) {
			reportError(re);
			recover(input,re);
			retval.tree = (Object)adaptor.errorNode(input, retval.start, input.LT(-1), re);
		}
		finally {
			// do for sure before leaving
		}
		return retval;
	}
	// $ANTLR end "absolutePath"


	public static class relativeLocationPath_return extends ParserRuleReturnScope {
		Object tree;
		@Override
		public Object getTree() { return tree; }
	};


	// $ANTLR start "relativeLocationPath"
	// XPath.g:77:1: relativeLocationPath : step ( ( '/' | '//' ) step )* ;
	public final XPathParser.relativeLocationPath_return relativeLocationPath() throws RecognitionException {
		XPathParser.relativeLocationPath_return retval = new XPathParser.relativeLocationPath_return();
		retval.start = input.LT(1);

		Object root_0 = null;

		Token set9=null;
		ParserRuleReturnScope step8 =null;
		ParserRuleReturnScope step10 =null;

		Object set9_tree=null;

		try {
			// XPath.g:78:3: ( step ( ( '/' | '//' ) step )* )
			// XPath.g:78:6: step ( ( '/' | '//' ) step )*
			{
			root_0 = (Object)adaptor.nil();


			pushFollow(FOLLOW_step_in_relativeLocationPath424);
			step8=step();
			state._fsp--;

			adaptor.addChild(root_0, step8.getTree());

			// XPath.g:78:11: ( ( '/' | '//' ) step )*
			loop2:
			while (true) {
				int alt2=2;
				int LA2_0 = input.LA(1);
				if ( (LA2_0==ABRPATH||LA2_0==PATHSEP) ) {
					alt2=1;
				}

				switch (alt2) {
				case 1 :
					// XPath.g:78:12: ( '/' | '//' ) step
					{
					set9=input.LT(1);
					if ( input.LA(1)==ABRPATH||input.LA(1)==PATHSEP ) {
						input.consume();
						adaptor.addChild(root_0, (Object)adaptor.create(set9));
						state.errorRecovery=false;
					}
					else {
						MismatchedSetException mse = new MismatchedSetException(null,input);
						throw mse;
					}
					pushFollow(FOLLOW_step_in_relativeLocationPath433);
					step10=step();
					state._fsp--;

					adaptor.addChild(root_0, step10.getTree());

					}
					break;

				default :
					break loop2;
				}
			}

			}

			retval.stop = input.LT(-1);

			retval.tree = (Object)adaptor.rulePostProcessing(root_0);
			adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);

		}
		catch (RecognitionException re) {
			reportError(re);
			recover(input,re);
			retval.tree = (Object)adaptor.errorNode(input, retval.start, input.LT(-1), re);
		}
		finally {
			// do for sure before leaving
		}
		return retval;
	}
	// $ANTLR end "relativeLocationPath"


	public static class step_return extends ParserRuleReturnScope {
		Object tree;
		@Override
		public Object getTree() { return tree; }
	};


	// $ANTLR start "step"
	// XPath.g:81:1: step : ( axisSpecifierI nodeTestI ( predicateI )* | abbreviatedStepI );
	public final XPathParser.step_return step() throws RecognitionException {
		XPathParser.step_return retval = new XPathParser.step_return();
		retval.start = input.LT(1);

		Object root_0 = null;

		ParserRuleReturnScope axisSpecifierI11 =null;
		ParserRuleReturnScope nodeTestI12 =null;
		ParserRuleReturnScope predicateI13 =null;
		ParserRuleReturnScope abbreviatedStepI14 =null;


		try {
			// XPath.g:81:7: ( axisSpecifierI nodeTestI ( predicateI )* | abbreviatedStepI )
			int alt4=2;
			int LA4_0 = input.LA(1);
			if ( (LA4_0==AT||LA4_0==AxisName||(LA4_0 >= MUL && LA4_0 <= NCName)||LA4_0==NodeType||LA4_0==50) ) {
				alt4=1;
			}
			else if ( ((LA4_0 >= DOT && LA4_0 <= DOTDOT)) ) {
				alt4=2;
			}

			else {
				NoViableAltException nvae =
					new NoViableAltException("", 4, 0, input);
				throw nvae;
			}

			switch (alt4) {
				case 1 :
					// XPath.g:81:10: axisSpecifierI nodeTestI ( predicateI )*
					{
					root_0 = (Object)adaptor.nil();


					pushFollow(FOLLOW_axisSpecifierI_in_step448);
					axisSpecifierI11=axisSpecifierI();
					state._fsp--;

					adaptor.addChild(root_0, axisSpecifierI11.getTree());

					pushFollow(FOLLOW_nodeTestI_in_step450);
					nodeTestI12=nodeTestI();
					state._fsp--;

					adaptor.addChild(root_0, nodeTestI12.getTree());

					// XPath.g:81:35: ( predicateI )*
					loop3:
					while (true) {
						int alt3=2;
						int LA3_0 = input.LA(1);
						if ( (LA3_0==LBRAC) ) {
							alt3=1;
						}

						switch (alt3) {
						case 1 :
							// XPath.g:81:35: predicateI
							{
							pushFollow(FOLLOW_predicateI_in_step452);
							predicateI13=predicateI();
							state._fsp--;

							adaptor.addChild(root_0, predicateI13.getTree());

							}
							break;

						default :
							break loop3;
						}
					}

					}
					break;
				case 2 :
					// XPath.g:82:6: abbreviatedStepI
					{
					root_0 = (Object)adaptor.nil();


					pushFollow(FOLLOW_abbreviatedStepI_in_step460);
					abbreviatedStepI14=abbreviatedStepI();
					state._fsp--;

					adaptor.addChild(root_0, abbreviatedStepI14.getTree());

					}
					break;

			}
			retval.stop = input.LT(-1);

			retval.tree = (Object)adaptor.rulePostProcessing(root_0);
			adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);

		}
		catch (RecognitionException re) {
			reportError(re);
			recover(input,re);
			retval.tree = (Object)adaptor.errorNode(input, retval.start, input.LT(-1), re);
		}
		finally {
			// do for sure before leaving
		}
		return retval;
	}
	// $ANTLR end "step"


	public static class axisSpecifierI_return extends ParserRuleReturnScope {
		Object tree;
		@Override
		public Object getTree() { return tree; }
	};


	// $ANTLR start "axisSpecifierI"
	// XPath.g:85:1: axisSpecifierI : axisSpecifier -> ^( AXIS ( axisSpecifier )? ) ;
	public final XPathParser.axisSpecifierI_return axisSpecifierI() throws RecognitionException {
		XPathParser.axisSpecifierI_return retval = new XPathParser.axisSpecifierI_return();
		retval.start = input.LT(1);

		Object root_0 = null;

		ParserRuleReturnScope axisSpecifier15 =null;

		RewriteRuleSubtreeStream stream_axisSpecifier=new RewriteRuleSubtreeStream(adaptor,"rule axisSpecifier");

		try {
			// XPath.g:86:3: ( axisSpecifier -> ^( AXIS ( axisSpecifier )? ) )
			// XPath.g:86:6: axisSpecifier
			{
			pushFollow(FOLLOW_axisSpecifier_in_axisSpecifierI474);
			axisSpecifier15=axisSpecifier();
			state._fsp--;

			stream_axisSpecifier.add(axisSpecifier15.getTree());
			// AST REWRITE
			// elements: axisSpecifier
			// token labels: 
			// rule labels: retval
			// token list labels: 
			// rule list labels: 
			// wildcard labels: 
			retval.tree = root_0;
			RewriteRuleSubtreeStream stream_retval=new RewriteRuleSubtreeStream(adaptor,"rule retval",retval!=null?retval.getTree():null);

			root_0 = (Object)adaptor.nil();
			// 86:20: -> ^( AXIS ( axisSpecifier )? )
			{
				// XPath.g:86:23: ^( AXIS ( axisSpecifier )? )
				{
				Object root_1 = (Object)adaptor.nil();
				root_1 = (Object)adaptor.becomeRoot((Object)adaptor.create(AXIS, "AXIS"), root_1);
				// XPath.g:86:30: ( axisSpecifier )?
				if ( stream_axisSpecifier.hasNext() ) {
					adaptor.addChild(root_1, stream_axisSpecifier.nextTree());
				}
				stream_axisSpecifier.reset();

				adaptor.addChild(root_0, root_1);
				}

			}


			retval.tree = root_0;

			}

			retval.stop = input.LT(-1);

			retval.tree = (Object)adaptor.rulePostProcessing(root_0);
			adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);

		}
		catch (RecognitionException re) {
			reportError(re);
			recover(input,re);
			retval.tree = (Object)adaptor.errorNode(input, retval.start, input.LT(-1), re);
		}
		finally {
			// do for sure before leaving
		}
		return retval;
	}
	// $ANTLR end "axisSpecifierI"


	public static class axisSpecifier_return extends ParserRuleReturnScope {
		Object tree;
		@Override
		public Object getTree() { return tree; }
	};


	// $ANTLR start "axisSpecifier"
	// XPath.g:89:1: axisSpecifier : ( AxisName '::' | ( '@' )? );
	public final XPathParser.axisSpecifier_return axisSpecifier() throws RecognitionException {
		XPathParser.axisSpecifier_return retval = new XPathParser.axisSpecifier_return();
		retval.start = input.LT(1);

		Object root_0 = null;

		Token AxisName16=null;
		Token string_literal17=null;
		Token char_literal18=null;

		Object AxisName16_tree=null;
		Object string_literal17_tree=null;
		Object char_literal18_tree=null;

		try {
			// XPath.g:90:3: ( AxisName '::' | ( '@' )? )
			int alt6=2;
			int LA6_0 = input.LA(1);
			if ( (LA6_0==AxisName) ) {
				int LA6_1 = input.LA(2);
				if ( (LA6_1==CC) ) {
					alt6=1;
				}
				else if ( (LA6_1==EOF||LA6_1==ABRPATH||(LA6_1 >= COLON && LA6_1 <= COMMA)||(LA6_1 >= GE && LA6_1 <= LESS)||(LA6_1 >= MINUS && LA6_1 <= MUL)||(LA6_1 >= PATHSEP && LA6_1 <= PLUS)||LA6_1==RBRAC||LA6_1==RPAR||LA6_1==43||(LA6_1 >= 45 && LA6_1 <= 49)) ) {
					alt6=2;
				}

				else {
					int nvaeMark = input.mark();
					try {
						input.consume();
						NoViableAltException nvae =
							new NoViableAltException("", 6, 1, input);
						throw nvae;
					} finally {
						input.rewind(nvaeMark);
					}
				}

			}
			else if ( (LA6_0==AT||(LA6_0 >= MUL && LA6_0 <= NCName)||LA6_0==NodeType||LA6_0==50) ) {
				alt6=2;
			}

			else {
				NoViableAltException nvae =
					new NoViableAltException("", 6, 0, input);
				throw nvae;
			}

			switch (alt6) {
				case 1 :
					// XPath.g:90:6: AxisName '::'
					{
					root_0 = (Object)adaptor.nil();


					AxisName16=(Token)match(input,AxisName,FOLLOW_AxisName_in_axisSpecifier497); 
					AxisName16_tree = (Object)adaptor.create(AxisName16);
					adaptor.addChild(root_0, AxisName16_tree);

					string_literal17=(Token)match(input,CC,FOLLOW_CC_in_axisSpecifier499); 
					string_literal17_tree = (Object)adaptor.create(string_literal17);
					adaptor.addChild(root_0, string_literal17_tree);

					}
					break;
				case 2 :
					// XPath.g:91:6: ( '@' )?
					{
					root_0 = (Object)adaptor.nil();


					// XPath.g:91:6: ( '@' )?
					int alt5=2;
					int LA5_0 = input.LA(1);
					if ( (LA5_0==AT) ) {
						alt5=1;
					}
					switch (alt5) {
						case 1 :
							// XPath.g:91:6: '@'
							{
							char_literal18=(Token)match(input,AT,FOLLOW_AT_in_axisSpecifier506); 
							char_literal18_tree = (Object)adaptor.create(char_literal18);
							adaptor.addChild(root_0, char_literal18_tree);

							}
							break;

					}

					}
					break;

			}
			retval.stop = input.LT(-1);

			retval.tree = (Object)adaptor.rulePostProcessing(root_0);
			adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);

		}
		catch (RecognitionException re) {
			reportError(re);
			recover(input,re);
			retval.tree = (Object)adaptor.errorNode(input, retval.start, input.LT(-1), re);
		}
		finally {
			// do for sure before leaving
		}
		return retval;
	}
	// $ANTLR end "axisSpecifier"


	public static class nodeTestI_return extends ParserRuleReturnScope {
		Object tree;
		@Override
		public Object getTree() { return tree; }
	};


	// $ANTLR start "nodeTestI"
	// XPath.g:94:1: nodeTestI : nodeTest -> ^( NODE nodeTest ) ;
	public final XPathParser.nodeTestI_return nodeTestI() throws RecognitionException {
		XPathParser.nodeTestI_return retval = new XPathParser.nodeTestI_return();
		retval.start = input.LT(1);

		Object root_0 = null;

		ParserRuleReturnScope nodeTest19 =null;

		RewriteRuleSubtreeStream stream_nodeTest=new RewriteRuleSubtreeStream(adaptor,"rule nodeTest");

		try {
			// XPath.g:95:3: ( nodeTest -> ^( NODE nodeTest ) )
			// XPath.g:95:6: nodeTest
			{
			pushFollow(FOLLOW_nodeTest_in_nodeTestI521);
			nodeTest19=nodeTest();
			state._fsp--;

			stream_nodeTest.add(nodeTest19.getTree());
			// AST REWRITE
			// elements: nodeTest
			// token labels: 
			// rule labels: retval
			// token list labels: 
			// rule list labels: 
			// wildcard labels: 
			retval.tree = root_0;
			RewriteRuleSubtreeStream stream_retval=new RewriteRuleSubtreeStream(adaptor,"rule retval",retval!=null?retval.getTree():null);

			root_0 = (Object)adaptor.nil();
			// 95:15: -> ^( NODE nodeTest )
			{
				// XPath.g:95:18: ^( NODE nodeTest )
				{
				Object root_1 = (Object)adaptor.nil();
				root_1 = (Object)adaptor.becomeRoot((Object)adaptor.create(NODE, "NODE"), root_1);
				adaptor.addChild(root_1, stream_nodeTest.nextTree());
				adaptor.addChild(root_0, root_1);
				}

			}


			retval.tree = root_0;

			}

			retval.stop = input.LT(-1);

			retval.tree = (Object)adaptor.rulePostProcessing(root_0);
			adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);

		}
		catch (RecognitionException re) {
			reportError(re);
			recover(input,re);
			retval.tree = (Object)adaptor.errorNode(input, retval.start, input.LT(-1), re);
		}
		finally {
			// do for sure before leaving
		}
		return retval;
	}
	// $ANTLR end "nodeTestI"


	public static class nodeTest_return extends ParserRuleReturnScope {
		Object tree;
		@Override
		public Object getTree() { return tree; }
	};


	// $ANTLR start "nodeTest"
	// XPath.g:98:1: nodeTest : ( nameTest | NodeType '(' ')' | 'processing-instruction' '(' Literal ')' );
	public final XPathParser.nodeTest_return nodeTest() throws RecognitionException {
		XPathParser.nodeTest_return retval = new XPathParser.nodeTest_return();
		retval.start = input.LT(1);

		Object root_0 = null;

		Token NodeType21=null;
		Token char_literal22=null;
		Token char_literal23=null;
		Token string_literal24=null;
		Token char_literal25=null;
		Token Literal26=null;
		Token char_literal27=null;
		ParserRuleReturnScope nameTest20 =null;

		Object NodeType21_tree=null;
		Object char_literal22_tree=null;
		Object char_literal23_tree=null;
		Object string_literal24_tree=null;
		Object char_literal25_tree=null;
		Object Literal26_tree=null;
		Object char_literal27_tree=null;

		try {
			// XPath.g:98:9: ( nameTest | NodeType '(' ')' | 'processing-instruction' '(' Literal ')' )
			int alt7=3;
			switch ( input.LA(1) ) {
			case AxisName:
			case MUL:
			case NCName:
				{
				alt7=1;
				}
				break;
			case NodeType:
				{
				alt7=2;
				}
				break;
			case 50:
				{
				alt7=3;
				}
				break;
			default:
				NoViableAltException nvae =
					new NoViableAltException("", 7, 0, input);
				throw nvae;
			}
			switch (alt7) {
				case 1 :
					// XPath.g:98:12: nameTest
					{
					root_0 = (Object)adaptor.nil();


					pushFollow(FOLLOW_nameTest_in_nodeTest540);
					nameTest20=nameTest();
					state._fsp--;

					adaptor.addChild(root_0, nameTest20.getTree());

					}
					break;
				case 2 :
					// XPath.g:99:6: NodeType '(' ')'
					{
					root_0 = (Object)adaptor.nil();


					NodeType21=(Token)match(input,NodeType,FOLLOW_NodeType_in_nodeTest547); 
					NodeType21_tree = (Object)adaptor.create(NodeType21);
					adaptor.addChild(root_0, NodeType21_tree);

					char_literal22=(Token)match(input,LPAR,FOLLOW_LPAR_in_nodeTest549); 
					char_literal22_tree = (Object)adaptor.create(char_literal22);
					adaptor.addChild(root_0, char_literal22_tree);

					char_literal23=(Token)match(input,RPAR,FOLLOW_RPAR_in_nodeTest551); 
					char_literal23_tree = (Object)adaptor.create(char_literal23);
					adaptor.addChild(root_0, char_literal23_tree);

					}
					break;
				case 3 :
					// XPath.g:100:6: 'processing-instruction' '(' Literal ')'
					{
					root_0 = (Object)adaptor.nil();


					string_literal24=(Token)match(input,50,FOLLOW_50_in_nodeTest558); 
					string_literal24_tree = (Object)adaptor.create(string_literal24);
					adaptor.addChild(root_0, string_literal24_tree);

					char_literal25=(Token)match(input,LPAR,FOLLOW_LPAR_in_nodeTest560); 
					char_literal25_tree = (Object)adaptor.create(char_literal25);
					adaptor.addChild(root_0, char_literal25_tree);

					Literal26=(Token)match(input,Literal,FOLLOW_Literal_in_nodeTest562); 
					Literal26_tree = (Object)adaptor.create(Literal26);
					adaptor.addChild(root_0, Literal26_tree);

					char_literal27=(Token)match(input,RPAR,FOLLOW_RPAR_in_nodeTest564); 
					char_literal27_tree = (Object)adaptor.create(char_literal27);
					adaptor.addChild(root_0, char_literal27_tree);

					}
					break;

			}
			retval.stop = input.LT(-1);

			retval.tree = (Object)adaptor.rulePostProcessing(root_0);
			adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);

		}
		catch (RecognitionException re) {
			reportError(re);
			recover(input,re);
			retval.tree = (Object)adaptor.errorNode(input, retval.start, input.LT(-1), re);
		}
		finally {
			// do for sure before leaving
		}
		return retval;
	}
	// $ANTLR end "nodeTest"


	public static class predicateI_return extends ParserRuleReturnScope {
		Object tree;
		@Override
		public Object getTree() { return tree; }
	};


	// $ANTLR start "predicateI"
	// XPath.g:103:1: predicateI : predicate -> ^( PREDICATE predicate ) ;
	public final XPathParser.predicateI_return predicateI() throws RecognitionException {
		XPathParser.predicateI_return retval = new XPathParser.predicateI_return();
		retval.start = input.LT(1);

		Object root_0 = null;

		ParserRuleReturnScope predicate28 =null;

		RewriteRuleSubtreeStream stream_predicate=new RewriteRuleSubtreeStream(adaptor,"rule predicate");

		try {
			// XPath.g:104:3: ( predicate -> ^( PREDICATE predicate ) )
			// XPath.g:104:6: predicate
			{
			pushFollow(FOLLOW_predicate_in_predicateI578);
			predicate28=predicate();
			state._fsp--;

			stream_predicate.add(predicate28.getTree());
			// AST REWRITE
			// elements: predicate
			// token labels: 
			// rule labels: retval
			// token list labels: 
			// rule list labels: 
			// wildcard labels: 
			retval.tree = root_0;
			RewriteRuleSubtreeStream stream_retval=new RewriteRuleSubtreeStream(adaptor,"rule retval",retval!=null?retval.getTree():null);

			root_0 = (Object)adaptor.nil();
			// 104:16: -> ^( PREDICATE predicate )
			{
				// XPath.g:104:19: ^( PREDICATE predicate )
				{
				Object root_1 = (Object)adaptor.nil();
				root_1 = (Object)adaptor.becomeRoot((Object)adaptor.create(PREDICATE, "PREDICATE"), root_1);
				adaptor.addChild(root_1, stream_predicate.nextTree());
				adaptor.addChild(root_0, root_1);
				}

			}


			retval.tree = root_0;

			}

			retval.stop = input.LT(-1);

			retval.tree = (Object)adaptor.rulePostProcessing(root_0);
			adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);

		}
		catch (RecognitionException re) {
			reportError(re);
			recover(input,re);
			retval.tree = (Object)adaptor.errorNode(input, retval.start, input.LT(-1), re);
		}
		finally {
			// do for sure before leaving
		}
		return retval;
	}
	// $ANTLR end "predicateI"


	public static class predicate_return extends ParserRuleReturnScope {
		Object tree;
		@Override
		public Object getTree() { return tree; }
	};


	// $ANTLR start "predicate"
	// XPath.g:107:1: predicate : '[' expr ']' ;
	public final XPathParser.predicate_return predicate() throws RecognitionException {
		XPathParser.predicate_return retval = new XPathParser.predicate_return();
		retval.start = input.LT(1);

		Object root_0 = null;

		Token char_literal29=null;
		Token char_literal31=null;
		ParserRuleReturnScope expr30 =null;

		Object char_literal29_tree=null;
		Object char_literal31_tree=null;

		try {
			// XPath.g:108:3: ( '[' expr ']' )
			// XPath.g:108:6: '[' expr ']'
			{
			root_0 = (Object)adaptor.nil();


			char_literal29=(Token)match(input,LBRAC,FOLLOW_LBRAC_in_predicate600); 
			char_literal29_tree = (Object)adaptor.create(char_literal29);
			adaptor.addChild(root_0, char_literal29_tree);

			pushFollow(FOLLOW_expr_in_predicate602);
			expr30=expr();
			state._fsp--;

			adaptor.addChild(root_0, expr30.getTree());

			char_literal31=(Token)match(input,RBRAC,FOLLOW_RBRAC_in_predicate604); 
			char_literal31_tree = (Object)adaptor.create(char_literal31);
			adaptor.addChild(root_0, char_literal31_tree);

			}

			retval.stop = input.LT(-1);

			retval.tree = (Object)adaptor.rulePostProcessing(root_0);
			adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);

		}
		catch (RecognitionException re) {
			reportError(re);
			recover(input,re);
			retval.tree = (Object)adaptor.errorNode(input, retval.start, input.LT(-1), re);
		}
		finally {
			// do for sure before leaving
		}
		return retval;
	}
	// $ANTLR end "predicate"


	public static class abbreviatedStepI_return extends ParserRuleReturnScope {
		Object tree;
		@Override
		public Object getTree() { return tree; }
	};


	// $ANTLR start "abbreviatedStepI"
	// XPath.g:111:1: abbreviatedStepI : abbreviatedStep -> ^( ABBRSTEP abbreviatedStep ) ;
	public final XPathParser.abbreviatedStepI_return abbreviatedStepI() throws RecognitionException {
		XPathParser.abbreviatedStepI_return retval = new XPathParser.abbreviatedStepI_return();
		retval.start = input.LT(1);

		Object root_0 = null;

		ParserRuleReturnScope abbreviatedStep32 =null;

		RewriteRuleSubtreeStream stream_abbreviatedStep=new RewriteRuleSubtreeStream(adaptor,"rule abbreviatedStep");

		try {
			// XPath.g:112:3: ( abbreviatedStep -> ^( ABBRSTEP abbreviatedStep ) )
			// XPath.g:112:6: abbreviatedStep
			{
			pushFollow(FOLLOW_abbreviatedStep_in_abbreviatedStepI618);
			abbreviatedStep32=abbreviatedStep();
			state._fsp--;

			stream_abbreviatedStep.add(abbreviatedStep32.getTree());
			// AST REWRITE
			// elements: abbreviatedStep
			// token labels: 
			// rule labels: retval
			// token list labels: 
			// rule list labels: 
			// wildcard labels: 
			retval.tree = root_0;
			RewriteRuleSubtreeStream stream_retval=new RewriteRuleSubtreeStream(adaptor,"rule retval",retval!=null?retval.getTree():null);

			root_0 = (Object)adaptor.nil();
			// 112:22: -> ^( ABBRSTEP abbreviatedStep )
			{
				// XPath.g:112:25: ^( ABBRSTEP abbreviatedStep )
				{
				Object root_1 = (Object)adaptor.nil();
				root_1 = (Object)adaptor.becomeRoot((Object)adaptor.create(ABBRSTEP, "ABBRSTEP"), root_1);
				adaptor.addChild(root_1, stream_abbreviatedStep.nextTree());
				adaptor.addChild(root_0, root_1);
				}

			}


			retval.tree = root_0;

			}

			retval.stop = input.LT(-1);

			retval.tree = (Object)adaptor.rulePostProcessing(root_0);
			adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);

		}
		catch (RecognitionException re) {
			reportError(re);
			recover(input,re);
			retval.tree = (Object)adaptor.errorNode(input, retval.start, input.LT(-1), re);
		}
		finally {
			// do for sure before leaving
		}
		return retval;
	}
	// $ANTLR end "abbreviatedStepI"


	public static class abbreviatedStep_return extends ParserRuleReturnScope {
		Object tree;
		@Override
		public Object getTree() { return tree; }
	};


	// $ANTLR start "abbreviatedStep"
	// XPath.g:115:1: abbreviatedStep : ( '.' | '..' );
	public final XPathParser.abbreviatedStep_return abbreviatedStep() throws RecognitionException {
		XPathParser.abbreviatedStep_return retval = new XPathParser.abbreviatedStep_return();
		retval.start = input.LT(1);

		Object root_0 = null;

		Token set33=null;

		Object set33_tree=null;

		try {
			// XPath.g:116:3: ( '.' | '..' )
			// XPath.g:
			{
			root_0 = (Object)adaptor.nil();


			set33=input.LT(1);
			if ( (input.LA(1) >= DOT && input.LA(1) <= DOTDOT) ) {
				input.consume();
				adaptor.addChild(root_0, (Object)adaptor.create(set33));
				state.errorRecovery=false;
			}
			else {
				MismatchedSetException mse = new MismatchedSetException(null,input);
				throw mse;
			}
			}

			retval.stop = input.LT(-1);

			retval.tree = (Object)adaptor.rulePostProcessing(root_0);
			adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);

		}
		catch (RecognitionException re) {
			reportError(re);
			recover(input,re);
			retval.tree = (Object)adaptor.errorNode(input, retval.start, input.LT(-1), re);
		}
		finally {
			// do for sure before leaving
		}
		return retval;
	}
	// $ANTLR end "abbreviatedStep"


	public static class expr_return extends ParserRuleReturnScope {
		Object tree;
		@Override
		public Object getTree() { return tree; }
	};


	// $ANTLR start "expr"
	// XPath.g:120:1: expr : orExpr ;
	public final XPathParser.expr_return expr() throws RecognitionException {
		XPathParser.expr_return retval = new XPathParser.expr_return();
		retval.start = input.LT(1);

		Object root_0 = null;

		ParserRuleReturnScope orExpr34 =null;


		try {
			// XPath.g:120:7: ( orExpr )
			// XPath.g:120:10: orExpr
			{
			root_0 = (Object)adaptor.nil();


			pushFollow(FOLLOW_orExpr_in_expr660);
			orExpr34=orExpr();
			state._fsp--;

			adaptor.addChild(root_0, orExpr34.getTree());

			}

			retval.stop = input.LT(-1);

			retval.tree = (Object)adaptor.rulePostProcessing(root_0);
			adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);

		}
		catch (RecognitionException re) {
			reportError(re);
			recover(input,re);
			retval.tree = (Object)adaptor.errorNode(input, retval.start, input.LT(-1), re);
		}
		finally {
			// do for sure before leaving
		}
		return retval;
	}
	// $ANTLR end "expr"


	public static class primaryExpr_return extends ParserRuleReturnScope {
		Object tree;
		@Override
		public Object getTree() { return tree; }
	};


	// $ANTLR start "primaryExpr"
	// XPath.g:123:1: primaryExpr : ( variableReference | '(' expr ')' | Literal | Number | functionCall );
	public final XPathParser.primaryExpr_return primaryExpr() throws RecognitionException {
		XPathParser.primaryExpr_return retval = new XPathParser.primaryExpr_return();
		retval.start = input.LT(1);

		Object root_0 = null;

		Token char_literal36=null;
		Token char_literal38=null;
		Token Literal39=null;
		Token Number40=null;
		ParserRuleReturnScope variableReference35 =null;
		ParserRuleReturnScope expr37 =null;
		ParserRuleReturnScope functionCall41 =null;

		Object char_literal36_tree=null;
		Object char_literal38_tree=null;
		Object Literal39_tree=null;
		Object Number40_tree=null;

		try {
			// XPath.g:124:3: ( variableReference | '(' expr ')' | Literal | Number | functionCall )
			int alt8=5;
			switch ( input.LA(1) ) {
			case 44:
				{
				alt8=1;
				}
				break;
			case LPAR:
				{
				alt8=2;
				}
				break;
			case Literal:
				{
				alt8=3;
				}
				break;
			case Number:
				{
				alt8=4;
				}
				break;
			case AxisName:
			case NCName:
				{
				alt8=5;
				}
				break;
			default:
				NoViableAltException nvae =
					new NoViableAltException("", 8, 0, input);
				throw nvae;
			}
			switch (alt8) {
				case 1 :
					// XPath.g:124:6: variableReference
					{
					root_0 = (Object)adaptor.nil();


					pushFollow(FOLLOW_variableReference_in_primaryExpr674);
					variableReference35=variableReference();
					state._fsp--;

					adaptor.addChild(root_0, variableReference35.getTree());

					}
					break;
				case 2 :
					// XPath.g:125:6: '(' expr ')'
					{
					root_0 = (Object)adaptor.nil();


					char_literal36=(Token)match(input,LPAR,FOLLOW_LPAR_in_primaryExpr681); 
					char_literal36_tree = (Object)adaptor.create(char_literal36);
					adaptor.addChild(root_0, char_literal36_tree);

					pushFollow(FOLLOW_expr_in_primaryExpr683);
					expr37=expr();
					state._fsp--;

					adaptor.addChild(root_0, expr37.getTree());

					char_literal38=(Token)match(input,RPAR,FOLLOW_RPAR_in_primaryExpr685); 
					char_literal38_tree = (Object)adaptor.create(char_literal38);
					adaptor.addChild(root_0, char_literal38_tree);

					}
					break;
				case 3 :
					// XPath.g:126:6: Literal
					{
					root_0 = (Object)adaptor.nil();


					Literal39=(Token)match(input,Literal,FOLLOW_Literal_in_primaryExpr692); 
					Literal39_tree = (Object)adaptor.create(Literal39);
					adaptor.addChild(root_0, Literal39_tree);

					}
					break;
				case 4 :
					// XPath.g:127:6: Number
					{
					root_0 = (Object)adaptor.nil();


					Number40=(Token)match(input,Number,FOLLOW_Number_in_primaryExpr699); 
					Number40_tree = (Object)adaptor.create(Number40);
					adaptor.addChild(root_0, Number40_tree);

					}
					break;
				case 5 :
					// XPath.g:128:6: functionCall
					{
					root_0 = (Object)adaptor.nil();


					pushFollow(FOLLOW_functionCall_in_primaryExpr708);
					functionCall41=functionCall();
					state._fsp--;

					adaptor.addChild(root_0, functionCall41.getTree());

					}
					break;

			}
			retval.stop = input.LT(-1);

			retval.tree = (Object)adaptor.rulePostProcessing(root_0);
			adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);

		}
		catch (RecognitionException re) {
			reportError(re);
			recover(input,re);
			retval.tree = (Object)adaptor.errorNode(input, retval.start, input.LT(-1), re);
		}
		finally {
			// do for sure before leaving
		}
		return retval;
	}
	// $ANTLR end "primaryExpr"


	public static class functionCall_return extends ParserRuleReturnScope {
		Object tree;
		@Override
		public Object getTree() { return tree; }
	};


	// $ANTLR start "functionCall"
	// XPath.g:131:1: functionCall : functionName '(' ( expr ( ',' expr )* )? ')' ;
	public final XPathParser.functionCall_return functionCall() throws RecognitionException {
		XPathParser.functionCall_return retval = new XPathParser.functionCall_return();
		retval.start = input.LT(1);

		Object root_0 = null;

		Token char_literal43=null;
		Token char_literal45=null;
		Token char_literal47=null;
		ParserRuleReturnScope functionName42 =null;
		ParserRuleReturnScope expr44 =null;
		ParserRuleReturnScope expr46 =null;

		Object char_literal43_tree=null;
		Object char_literal45_tree=null;
		Object char_literal47_tree=null;

		try {
			// XPath.g:132:3: ( functionName '(' ( expr ( ',' expr )* )? ')' )
			// XPath.g:132:6: functionName '(' ( expr ( ',' expr )* )? ')'
			{
			root_0 = (Object)adaptor.nil();


			pushFollow(FOLLOW_functionName_in_functionCall722);
			functionName42=functionName();
			state._fsp--;

			adaptor.addChild(root_0, functionName42.getTree());

			char_literal43=(Token)match(input,LPAR,FOLLOW_LPAR_in_functionCall724); 
			char_literal43_tree = (Object)adaptor.create(char_literal43);
			adaptor.addChild(root_0, char_literal43_tree);

			// XPath.g:132:23: ( expr ( ',' expr )* )?
			int alt10=2;
			int LA10_0 = input.LA(1);
			if ( (LA10_0==ABRPATH||LA10_0==AT||LA10_0==AxisName||(LA10_0 >= DOT && LA10_0 <= DOTDOT)||(LA10_0 >= LPAR && LA10_0 <= MINUS)||(LA10_0 >= MUL && LA10_0 <= NCName)||(LA10_0 >= NodeType && LA10_0 <= PATHSEP)||LA10_0==44||LA10_0==50) ) {
				alt10=1;
			}
			switch (alt10) {
				case 1 :
					// XPath.g:132:25: expr ( ',' expr )*
					{
					pushFollow(FOLLOW_expr_in_functionCall728);
					expr44=expr();
					state._fsp--;

					adaptor.addChild(root_0, expr44.getTree());

					// XPath.g:132:30: ( ',' expr )*
					loop9:
					while (true) {
						int alt9=2;
						int LA9_0 = input.LA(1);
						if ( (LA9_0==COMMA) ) {
							alt9=1;
						}

						switch (alt9) {
						case 1 :
							// XPath.g:132:32: ',' expr
							{
							char_literal45=(Token)match(input,COMMA,FOLLOW_COMMA_in_functionCall732); 
							char_literal45_tree = (Object)adaptor.create(char_literal45);
							adaptor.addChild(root_0, char_literal45_tree);

							pushFollow(FOLLOW_expr_in_functionCall734);
							expr46=expr();
							state._fsp--;

							adaptor.addChild(root_0, expr46.getTree());

							}
							break;

						default :
							break loop9;
						}
					}

					}
					break;

			}

			char_literal47=(Token)match(input,RPAR,FOLLOW_RPAR_in_functionCall742); 
			char_literal47_tree = (Object)adaptor.create(char_literal47);
			adaptor.addChild(root_0, char_literal47_tree);

			}

			retval.stop = input.LT(-1);

			retval.tree = (Object)adaptor.rulePostProcessing(root_0);
			adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);

		}
		catch (RecognitionException re) {
			reportError(re);
			recover(input,re);
			retval.tree = (Object)adaptor.errorNode(input, retval.start, input.LT(-1), re);
		}
		finally {
			// do for sure before leaving
		}
		return retval;
	}
	// $ANTLR end "functionCall"


	public static class unionExprNoRoot_return extends ParserRuleReturnScope {
		Object tree;
		@Override
		public Object getTree() { return tree; }
	};


	// $ANTLR start "unionExprNoRoot"
	// XPath.g:135:1: unionExprNoRoot : ( pathExprNoRootI ( '|' unionExprNoRoot )? | '/' '|' unionExprNoRoot );
	public final XPathParser.unionExprNoRoot_return unionExprNoRoot() throws RecognitionException {
		XPathParser.unionExprNoRoot_return retval = new XPathParser.unionExprNoRoot_return();
		retval.start = input.LT(1);

		Object root_0 = null;

		Token char_literal49=null;
		Token char_literal51=null;
		Token char_literal52=null;
		ParserRuleReturnScope pathExprNoRootI48 =null;
		ParserRuleReturnScope unionExprNoRoot50 =null;
		ParserRuleReturnScope unionExprNoRoot53 =null;

		Object char_literal49_tree=null;
		Object char_literal51_tree=null;
		Object char_literal52_tree=null;

		try {
			// XPath.g:136:3: ( pathExprNoRootI ( '|' unionExprNoRoot )? | '/' '|' unionExprNoRoot )
			int alt12=2;
			int LA12_0 = input.LA(1);
			if ( (LA12_0==ABRPATH||LA12_0==AT||LA12_0==AxisName||(LA12_0 >= DOT && LA12_0 <= DOTDOT)||(LA12_0 >= LPAR && LA12_0 <= Literal)||(LA12_0 >= MUL && LA12_0 <= NCName)||(LA12_0 >= NodeType && LA12_0 <= Number)||LA12_0==44||LA12_0==50) ) {
				alt12=1;
			}
			else if ( (LA12_0==PATHSEP) ) {
				int LA12_2 = input.LA(2);
				if ( (LA12_2==PIPE) ) {
					alt12=2;
				}
				else if ( (LA12_2==AT||LA12_2==AxisName||(LA12_2 >= DOT && LA12_2 <= DOTDOT)||(LA12_2 >= MUL && LA12_2 <= NCName)||LA12_2==NodeType||LA12_2==50) ) {
					alt12=1;
				}

				else {
					int nvaeMark = input.mark();
					try {
						input.consume();
						NoViableAltException nvae =
							new NoViableAltException("", 12, 2, input);
						throw nvae;
					} finally {
						input.rewind(nvaeMark);
					}
				}

			}

			else {
				NoViableAltException nvae =
					new NoViableAltException("", 12, 0, input);
				throw nvae;
			}

			switch (alt12) {
				case 1 :
					// XPath.g:136:6: pathExprNoRootI ( '|' unionExprNoRoot )?
					{
					root_0 = (Object)adaptor.nil();


					pushFollow(FOLLOW_pathExprNoRootI_in_unionExprNoRoot756);
					pathExprNoRootI48=pathExprNoRootI();
					state._fsp--;

					adaptor.addChild(root_0, pathExprNoRootI48.getTree());

					// XPath.g:136:22: ( '|' unionExprNoRoot )?
					int alt11=2;
					int LA11_0 = input.LA(1);
					if ( (LA11_0==PIPE) ) {
						alt11=1;
					}
					switch (alt11) {
						case 1 :
							// XPath.g:136:23: '|' unionExprNoRoot
							{
							char_literal49=(Token)match(input,PIPE,FOLLOW_PIPE_in_unionExprNoRoot759); 
							char_literal49_tree = (Object)adaptor.create(char_literal49);
							adaptor.addChild(root_0, char_literal49_tree);

							pushFollow(FOLLOW_unionExprNoRoot_in_unionExprNoRoot761);
							unionExprNoRoot50=unionExprNoRoot();
							state._fsp--;

							adaptor.addChild(root_0, unionExprNoRoot50.getTree());

							}
							break;

					}

					}
					break;
				case 2 :
					// XPath.g:137:6: '/' '|' unionExprNoRoot
					{
					root_0 = (Object)adaptor.nil();


					char_literal51=(Token)match(input,PATHSEP,FOLLOW_PATHSEP_in_unionExprNoRoot771); 
					char_literal51_tree = (Object)adaptor.create(char_literal51);
					adaptor.addChild(root_0, char_literal51_tree);

					char_literal52=(Token)match(input,PIPE,FOLLOW_PIPE_in_unionExprNoRoot773); 
					char_literal52_tree = (Object)adaptor.create(char_literal52);
					adaptor.addChild(root_0, char_literal52_tree);

					pushFollow(FOLLOW_unionExprNoRoot_in_unionExprNoRoot775);
					unionExprNoRoot53=unionExprNoRoot();
					state._fsp--;

					adaptor.addChild(root_0, unionExprNoRoot53.getTree());

					}
					break;

			}
			retval.stop = input.LT(-1);

			retval.tree = (Object)adaptor.rulePostProcessing(root_0);
			adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);

		}
		catch (RecognitionException re) {
			reportError(re);
			recover(input,re);
			retval.tree = (Object)adaptor.errorNode(input, retval.start, input.LT(-1), re);
		}
		finally {
			// do for sure before leaving
		}
		return retval;
	}
	// $ANTLR end "unionExprNoRoot"


	public static class pathExprNoRootI_return extends ParserRuleReturnScope {
		Object tree;
		@Override
		public Object getTree() { return tree; }
	};


	// $ANTLR start "pathExprNoRootI"
	// XPath.g:140:1: pathExprNoRootI : pathExprNoRoot -> ^( EXPR pathExprNoRoot ) ;
	public final XPathParser.pathExprNoRootI_return pathExprNoRootI() throws RecognitionException {
		XPathParser.pathExprNoRootI_return retval = new XPathParser.pathExprNoRootI_return();
		retval.start = input.LT(1);

		Object root_0 = null;

		ParserRuleReturnScope pathExprNoRoot54 =null;

		RewriteRuleSubtreeStream stream_pathExprNoRoot=new RewriteRuleSubtreeStream(adaptor,"rule pathExprNoRoot");

		try {
			// XPath.g:141:3: ( pathExprNoRoot -> ^( EXPR pathExprNoRoot ) )
			// XPath.g:141:6: pathExprNoRoot
			{
			pushFollow(FOLLOW_pathExprNoRoot_in_pathExprNoRootI789);
			pathExprNoRoot54=pathExprNoRoot();
			state._fsp--;

			stream_pathExprNoRoot.add(pathExprNoRoot54.getTree());
			// AST REWRITE
			// elements: pathExprNoRoot
			// token labels: 
			// rule labels: retval
			// token list labels: 
			// rule list labels: 
			// wildcard labels: 
			retval.tree = root_0;
			RewriteRuleSubtreeStream stream_retval=new RewriteRuleSubtreeStream(adaptor,"rule retval",retval!=null?retval.getTree():null);

			root_0 = (Object)adaptor.nil();
			// 141:21: -> ^( EXPR pathExprNoRoot )
			{
				// XPath.g:141:24: ^( EXPR pathExprNoRoot )
				{
				Object root_1 = (Object)adaptor.nil();
				root_1 = (Object)adaptor.becomeRoot((Object)adaptor.create(EXPR, "EXPR"), root_1);
				adaptor.addChild(root_1, stream_pathExprNoRoot.nextTree());
				adaptor.addChild(root_0, root_1);
				}

			}


			retval.tree = root_0;

			}

			retval.stop = input.LT(-1);

			retval.tree = (Object)adaptor.rulePostProcessing(root_0);
			adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);

		}
		catch (RecognitionException re) {
			reportError(re);
			recover(input,re);
			retval.tree = (Object)adaptor.errorNode(input, retval.start, input.LT(-1), re);
		}
		finally {
			// do for sure before leaving
		}
		return retval;
	}
	// $ANTLR end "pathExprNoRootI"


	public static class pathExprNoRoot_return extends ParserRuleReturnScope {
		Object tree;
		@Override
		public Object getTree() { return tree; }
	};


	// $ANTLR start "pathExprNoRoot"
	// XPath.g:144:1: pathExprNoRoot : ( locationPathI | filterExpr ( ( '/' | '//' ) relativeLocationPath )? );
	public final XPathParser.pathExprNoRoot_return pathExprNoRoot() throws RecognitionException {
		XPathParser.pathExprNoRoot_return retval = new XPathParser.pathExprNoRoot_return();
		retval.start = input.LT(1);

		Object root_0 = null;

		Token set57=null;
		ParserRuleReturnScope locationPathI55 =null;
		ParserRuleReturnScope filterExpr56 =null;
		ParserRuleReturnScope relativeLocationPath58 =null;

		Object set57_tree=null;

		try {
			// XPath.g:145:3: ( locationPathI | filterExpr ( ( '/' | '//' ) relativeLocationPath )? )
			int alt14=2;
			switch ( input.LA(1) ) {
			case AxisName:
				{
				switch ( input.LA(2) ) {
				case EOF:
				case ABRPATH:
				case CC:
				case COMMA:
				case GE:
				case LBRAC:
				case LE:
				case LESS:
				case MINUS:
				case MORE:
				case MUL:
				case PATHSEP:
				case PIPE:
				case PLUS:
				case RBRAC:
				case RPAR:
				case 43:
				case 45:
				case 46:
				case 47:
				case 48:
				case 49:
					{
					alt14=1;
					}
					break;
				case COLON:
					{
					int LA14_5 = input.LA(3);
					if ( (LA14_5==MUL) ) {
						alt14=1;
					}
					else if ( (LA14_5==AxisName||LA14_5==NCName) ) {
						int LA14_6 = input.LA(4);
						if ( (LA14_6==EOF||LA14_6==ABRPATH||LA14_6==COMMA||(LA14_6 >= GE && LA14_6 <= LESS)||(LA14_6 >= MINUS && LA14_6 <= MUL)||(LA14_6 >= PATHSEP && LA14_6 <= PLUS)||LA14_6==RBRAC||LA14_6==RPAR||LA14_6==43||(LA14_6 >= 45 && LA14_6 <= 49)) ) {
							alt14=1;
						}
						else if ( (LA14_6==LPAR) ) {
							alt14=2;
						}

						else {
							int nvaeMark = input.mark();
							try {
								for (int nvaeConsume = 0; nvaeConsume < 4 - 1; nvaeConsume++) {
									input.consume();
								}
								NoViableAltException nvae =
									new NoViableAltException("", 14, 6, input);
								throw nvae;
							} finally {
								input.rewind(nvaeMark);
							}
						}

					}

					else {
						int nvaeMark = input.mark();
						try {
							for (int nvaeConsume = 0; nvaeConsume < 3 - 1; nvaeConsume++) {
								input.consume();
							}
							NoViableAltException nvae =
								new NoViableAltException("", 14, 5, input);
							throw nvae;
						} finally {
							input.rewind(nvaeMark);
						}
					}

					}
					break;
				case LPAR:
					{
					alt14=2;
					}
					break;
				default:
					int nvaeMark = input.mark();
					try {
						input.consume();
						NoViableAltException nvae =
							new NoViableAltException("", 14, 1, input);
						throw nvae;
					} finally {
						input.rewind(nvaeMark);
					}
				}
				}
				break;
			case ABRPATH:
			case AT:
			case DOT:
			case DOTDOT:
			case MUL:
			case NodeType:
			case PATHSEP:
			case 50:
				{
				alt14=1;
				}
				break;
			case NCName:
				{
				switch ( input.LA(2) ) {
				case COLON:
					{
					int LA14_5 = input.LA(3);
					if ( (LA14_5==MUL) ) {
						alt14=1;
					}
					else if ( (LA14_5==AxisName||LA14_5==NCName) ) {
						int LA14_6 = input.LA(4);
						if ( (LA14_6==EOF||LA14_6==ABRPATH||LA14_6==COMMA||(LA14_6 >= GE && LA14_6 <= LESS)||(LA14_6 >= MINUS && LA14_6 <= MUL)||(LA14_6 >= PATHSEP && LA14_6 <= PLUS)||LA14_6==RBRAC||LA14_6==RPAR||LA14_6==43||(LA14_6 >= 45 && LA14_6 <= 49)) ) {
							alt14=1;
						}
						else if ( (LA14_6==LPAR) ) {
							alt14=2;
						}

						else {
							int nvaeMark = input.mark();
							try {
								for (int nvaeConsume = 0; nvaeConsume < 4 - 1; nvaeConsume++) {
									input.consume();
								}
								NoViableAltException nvae =
									new NoViableAltException("", 14, 6, input);
								throw nvae;
							} finally {
								input.rewind(nvaeMark);
							}
						}

					}

					else {
						int nvaeMark = input.mark();
						try {
							for (int nvaeConsume = 0; nvaeConsume < 3 - 1; nvaeConsume++) {
								input.consume();
							}
							NoViableAltException nvae =
								new NoViableAltException("", 14, 5, input);
							throw nvae;
						} finally {
							input.rewind(nvaeMark);
						}
					}

					}
					break;
				case EOF:
				case ABRPATH:
				case COMMA:
				case GE:
				case LBRAC:
				case LE:
				case LESS:
				case MINUS:
				case MORE:
				case MUL:
				case PATHSEP:
				case PIPE:
				case PLUS:
				case RBRAC:
				case RPAR:
				case 43:
				case 45:
				case 46:
				case 47:
				case 48:
				case 49:
					{
					alt14=1;
					}
					break;
				case LPAR:
					{
					alt14=2;
					}
					break;
				default:
					int nvaeMark = input.mark();
					try {
						input.consume();
						NoViableAltException nvae =
							new NoViableAltException("", 14, 3, input);
						throw nvae;
					} finally {
						input.rewind(nvaeMark);
					}
				}
				}
				break;
			case LPAR:
			case Literal:
			case Number:
			case 44:
				{
				alt14=2;
				}
				break;
			default:
				NoViableAltException nvae =
					new NoViableAltException("", 14, 0, input);
				throw nvae;
			}
			switch (alt14) {
				case 1 :
					// XPath.g:145:6: locationPathI
					{
					root_0 = (Object)adaptor.nil();


					pushFollow(FOLLOW_locationPathI_in_pathExprNoRoot811);
					locationPathI55=locationPathI();
					state._fsp--;

					adaptor.addChild(root_0, locationPathI55.getTree());

					}
					break;
				case 2 :
					// XPath.g:146:6: filterExpr ( ( '/' | '//' ) relativeLocationPath )?
					{
					root_0 = (Object)adaptor.nil();


					pushFollow(FOLLOW_filterExpr_in_pathExprNoRoot818);
					filterExpr56=filterExpr();
					state._fsp--;

					adaptor.addChild(root_0, filterExpr56.getTree());

					// XPath.g:146:17: ( ( '/' | '//' ) relativeLocationPath )?
					int alt13=2;
					int LA13_0 = input.LA(1);
					if ( (LA13_0==ABRPATH||LA13_0==PATHSEP) ) {
						alt13=1;
					}
					switch (alt13) {
						case 1 :
							// XPath.g:146:18: ( '/' | '//' ) relativeLocationPath
							{
							set57=input.LT(1);
							if ( input.LA(1)==ABRPATH||input.LA(1)==PATHSEP ) {
								input.consume();
								adaptor.addChild(root_0, (Object)adaptor.create(set57));
								state.errorRecovery=false;
							}
							else {
								MismatchedSetException mse = new MismatchedSetException(null,input);
								throw mse;
							}
							pushFollow(FOLLOW_relativeLocationPath_in_pathExprNoRoot827);
							relativeLocationPath58=relativeLocationPath();
							state._fsp--;

							adaptor.addChild(root_0, relativeLocationPath58.getTree());

							}
							break;

					}

					}
					break;

			}
			retval.stop = input.LT(-1);

			retval.tree = (Object)adaptor.rulePostProcessing(root_0);
			adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);

		}
		catch (RecognitionException re) {
			reportError(re);
			recover(input,re);
			retval.tree = (Object)adaptor.errorNode(input, retval.start, input.LT(-1), re);
		}
		finally {
			// do for sure before leaving
		}
		return retval;
	}
	// $ANTLR end "pathExprNoRoot"


	public static class filterExpr_return extends ParserRuleReturnScope {
		Object tree;
		@Override
		public Object getTree() { return tree; }
	};


	// $ANTLR start "filterExpr"
	// XPath.g:149:1: filterExpr : primaryExpr ( predicateI )* ;
	public final XPathParser.filterExpr_return filterExpr() throws RecognitionException {
		XPathParser.filterExpr_return retval = new XPathParser.filterExpr_return();
		retval.start = input.LT(1);

		Object root_0 = null;

		ParserRuleReturnScope primaryExpr59 =null;
		ParserRuleReturnScope predicateI60 =null;


		try {
			// XPath.g:150:3: ( primaryExpr ( predicateI )* )
			// XPath.g:150:6: primaryExpr ( predicateI )*
			{
			root_0 = (Object)adaptor.nil();


			pushFollow(FOLLOW_primaryExpr_in_filterExpr843);
			primaryExpr59=primaryExpr();
			state._fsp--;

			adaptor.addChild(root_0, primaryExpr59.getTree());

			// XPath.g:150:18: ( predicateI )*
			loop15:
			while (true) {
				int alt15=2;
				int LA15_0 = input.LA(1);
				if ( (LA15_0==LBRAC) ) {
					alt15=1;
				}

				switch (alt15) {
				case 1 :
					// XPath.g:150:18: predicateI
					{
					pushFollow(FOLLOW_predicateI_in_filterExpr845);
					predicateI60=predicateI();
					state._fsp--;

					adaptor.addChild(root_0, predicateI60.getTree());

					}
					break;

				default :
					break loop15;
				}
			}

			}

			retval.stop = input.LT(-1);

			retval.tree = (Object)adaptor.rulePostProcessing(root_0);
			adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);

		}
		catch (RecognitionException re) {
			reportError(re);
			recover(input,re);
			retval.tree = (Object)adaptor.errorNode(input, retval.start, input.LT(-1), re);
		}
		finally {
			// do for sure before leaving
		}
		return retval;
	}
	// $ANTLR end "filterExpr"


	public static class orExpr_return extends ParserRuleReturnScope {
		Object tree;
		@Override
		public Object getTree() { return tree; }
	};


	// $ANTLR start "orExpr"
	// XPath.g:153:1: orExpr : andExpr ( 'or' andExpr )* ;
	public final XPathParser.orExpr_return orExpr() throws RecognitionException {
		XPathParser.orExpr_return retval = new XPathParser.orExpr_return();
		retval.start = input.LT(1);

		Object root_0 = null;

		Token string_literal62=null;
		ParserRuleReturnScope andExpr61 =null;
		ParserRuleReturnScope andExpr63 =null;

		Object string_literal62_tree=null;

		try {
			// XPath.g:153:9: ( andExpr ( 'or' andExpr )* )
			// XPath.g:153:12: andExpr ( 'or' andExpr )*
			{
			root_0 = (Object)adaptor.nil();


			pushFollow(FOLLOW_andExpr_in_orExpr859);
			andExpr61=andExpr();
			state._fsp--;

			adaptor.addChild(root_0, andExpr61.getTree());

			// XPath.g:153:20: ( 'or' andExpr )*
			loop16:
			while (true) {
				int alt16=2;
				int LA16_0 = input.LA(1);
				if ( (LA16_0==49) ) {
					alt16=1;
				}

				switch (alt16) {
				case 1 :
					// XPath.g:153:21: 'or' andExpr
					{
					string_literal62=(Token)match(input,49,FOLLOW_49_in_orExpr862); 
					string_literal62_tree = (Object)adaptor.create(string_literal62);
					adaptor.addChild(root_0, string_literal62_tree);

					pushFollow(FOLLOW_andExpr_in_orExpr864);
					andExpr63=andExpr();
					state._fsp--;

					adaptor.addChild(root_0, andExpr63.getTree());

					}
					break;

				default :
					break loop16;
				}
			}

			}

			retval.stop = input.LT(-1);

			retval.tree = (Object)adaptor.rulePostProcessing(root_0);
			adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);

		}
		catch (RecognitionException re) {
			reportError(re);
			recover(input,re);
			retval.tree = (Object)adaptor.errorNode(input, retval.start, input.LT(-1), re);
		}
		finally {
			// do for sure before leaving
		}
		return retval;
	}
	// $ANTLR end "orExpr"


	public static class andExpr_return extends ParserRuleReturnScope {
		Object tree;
		@Override
		public Object getTree() { return tree; }
	};


	// $ANTLR start "andExpr"
	// XPath.g:156:1: andExpr : equalityExpr ( 'and' equalityExpr )* ;
	public final XPathParser.andExpr_return andExpr() throws RecognitionException {
		XPathParser.andExpr_return retval = new XPathParser.andExpr_return();
		retval.start = input.LT(1);

		Object root_0 = null;

		Token string_literal65=null;
		ParserRuleReturnScope equalityExpr64 =null;
		ParserRuleReturnScope equalityExpr66 =null;

		Object string_literal65_tree=null;

		try {
			// XPath.g:156:10: ( equalityExpr ( 'and' equalityExpr )* )
			// XPath.g:156:13: equalityExpr ( 'and' equalityExpr )*
			{
			root_0 = (Object)adaptor.nil();


			pushFollow(FOLLOW_equalityExpr_in_andExpr879);
			equalityExpr64=equalityExpr();
			state._fsp--;

			adaptor.addChild(root_0, equalityExpr64.getTree());

			// XPath.g:156:26: ( 'and' equalityExpr )*
			loop17:
			while (true) {
				int alt17=2;
				int LA17_0 = input.LA(1);
				if ( (LA17_0==46) ) {
					alt17=1;
				}

				switch (alt17) {
				case 1 :
					// XPath.g:156:27: 'and' equalityExpr
					{
					string_literal65=(Token)match(input,46,FOLLOW_46_in_andExpr882); 
					string_literal65_tree = (Object)adaptor.create(string_literal65);
					adaptor.addChild(root_0, string_literal65_tree);

					pushFollow(FOLLOW_equalityExpr_in_andExpr884);
					equalityExpr66=equalityExpr();
					state._fsp--;

					adaptor.addChild(root_0, equalityExpr66.getTree());

					}
					break;

				default :
					break loop17;
				}
			}

			}

			retval.stop = input.LT(-1);

			retval.tree = (Object)adaptor.rulePostProcessing(root_0);
			adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);

		}
		catch (RecognitionException re) {
			reportError(re);
			recover(input,re);
			retval.tree = (Object)adaptor.errorNode(input, retval.start, input.LT(-1), re);
		}
		finally {
			// do for sure before leaving
		}
		return retval;
	}
	// $ANTLR end "andExpr"


	public static class equalityExpr_return extends ParserRuleReturnScope {
		Object tree;
		@Override
		public Object getTree() { return tree; }
	};


	// $ANTLR start "equalityExpr"
	// XPath.g:159:1: equalityExpr : relationalExpr ( ( '=' | '!=' ) relationalExpr )* ;
	public final XPathParser.equalityExpr_return equalityExpr() throws RecognitionException {
		XPathParser.equalityExpr_return retval = new XPathParser.equalityExpr_return();
		retval.start = input.LT(1);

		Object root_0 = null;

		Token set68=null;
		ParserRuleReturnScope relationalExpr67 =null;
		ParserRuleReturnScope relationalExpr69 =null;

		Object set68_tree=null;

		try {
			// XPath.g:160:3: ( relationalExpr ( ( '=' | '!=' ) relationalExpr )* )
			// XPath.g:160:6: relationalExpr ( ( '=' | '!=' ) relationalExpr )*
			{
			root_0 = (Object)adaptor.nil();


			pushFollow(FOLLOW_relationalExpr_in_equalityExpr900);
			relationalExpr67=relationalExpr();
			state._fsp--;

			adaptor.addChild(root_0, relationalExpr67.getTree());

			// XPath.g:160:21: ( ( '=' | '!=' ) relationalExpr )*
			loop18:
			while (true) {
				int alt18=2;
				int LA18_0 = input.LA(1);
				if ( (LA18_0==43||LA18_0==45) ) {
					alt18=1;
				}

				switch (alt18) {
				case 1 :
					// XPath.g:160:22: ( '=' | '!=' ) relationalExpr
					{
					set68=input.LT(1);
					if ( input.LA(1)==43||input.LA(1)==45 ) {
						input.consume();
						adaptor.addChild(root_0, (Object)adaptor.create(set68));
						state.errorRecovery=false;
					}
					else {
						MismatchedSetException mse = new MismatchedSetException(null,input);
						throw mse;
					}
					pushFollow(FOLLOW_relationalExpr_in_equalityExpr909);
					relationalExpr69=relationalExpr();
					state._fsp--;

					adaptor.addChild(root_0, relationalExpr69.getTree());

					}
					break;

				default :
					break loop18;
				}
			}

			}

			retval.stop = input.LT(-1);

			retval.tree = (Object)adaptor.rulePostProcessing(root_0);
			adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);

		}
		catch (RecognitionException re) {
			reportError(re);
			recover(input,re);
			retval.tree = (Object)adaptor.errorNode(input, retval.start, input.LT(-1), re);
		}
		finally {
			// do for sure before leaving
		}
		return retval;
	}
	// $ANTLR end "equalityExpr"


	public static class relationalExpr_return extends ParserRuleReturnScope {
		Object tree;
		@Override
		public Object getTree() { return tree; }
	};


	// $ANTLR start "relationalExpr"
	// XPath.g:163:1: relationalExpr : additiveExpr ( ( '<' | '>' | '<=' | '>=' ) additiveExpr )* ;
	public final XPathParser.relationalExpr_return relationalExpr() throws RecognitionException {
		XPathParser.relationalExpr_return retval = new XPathParser.relationalExpr_return();
		retval.start = input.LT(1);

		Object root_0 = null;

		Token set71=null;
		ParserRuleReturnScope additiveExpr70 =null;
		ParserRuleReturnScope additiveExpr72 =null;

		Object set71_tree=null;

		try {
			// XPath.g:164:3: ( additiveExpr ( ( '<' | '>' | '<=' | '>=' ) additiveExpr )* )
			// XPath.g:164:6: additiveExpr ( ( '<' | '>' | '<=' | '>=' ) additiveExpr )*
			{
			root_0 = (Object)adaptor.nil();


			pushFollow(FOLLOW_additiveExpr_in_relationalExpr925);
			additiveExpr70=additiveExpr();
			state._fsp--;

			adaptor.addChild(root_0, additiveExpr70.getTree());

			// XPath.g:164:19: ( ( '<' | '>' | '<=' | '>=' ) additiveExpr )*
			loop19:
			while (true) {
				int alt19=2;
				int LA19_0 = input.LA(1);
				if ( (LA19_0==GE||(LA19_0 >= LE && LA19_0 <= LESS)||LA19_0==MORE) ) {
					alt19=1;
				}

				switch (alt19) {
				case 1 :
					// XPath.g:164:20: ( '<' | '>' | '<=' | '>=' ) additiveExpr
					{
					set71=input.LT(1);
					if ( input.LA(1)==GE||(input.LA(1) >= LE && input.LA(1) <= LESS)||input.LA(1)==MORE ) {
						input.consume();
						adaptor.addChild(root_0, (Object)adaptor.create(set71));
						state.errorRecovery=false;
					}
					else {
						MismatchedSetException mse = new MismatchedSetException(null,input);
						throw mse;
					}
					pushFollow(FOLLOW_additiveExpr_in_relationalExpr938);
					additiveExpr72=additiveExpr();
					state._fsp--;

					adaptor.addChild(root_0, additiveExpr72.getTree());

					}
					break;

				default :
					break loop19;
				}
			}

			}

			retval.stop = input.LT(-1);

			retval.tree = (Object)adaptor.rulePostProcessing(root_0);
			adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);

		}
		catch (RecognitionException re) {
			reportError(re);
			recover(input,re);
			retval.tree = (Object)adaptor.errorNode(input, retval.start, input.LT(-1), re);
		}
		finally {
			// do for sure before leaving
		}
		return retval;
	}
	// $ANTLR end "relationalExpr"


	public static class additiveExpr_return extends ParserRuleReturnScope {
		Object tree;
		@Override
		public Object getTree() { return tree; }
	};


	// $ANTLR start "additiveExpr"
	// XPath.g:167:1: additiveExpr : multiplicativeExpr ( ( '+' | '-' ) multiplicativeExpr )* ;
	public final XPathParser.additiveExpr_return additiveExpr() throws RecognitionException {
		XPathParser.additiveExpr_return retval = new XPathParser.additiveExpr_return();
		retval.start = input.LT(1);

		Object root_0 = null;

		Token set74=null;
		ParserRuleReturnScope multiplicativeExpr73 =null;
		ParserRuleReturnScope multiplicativeExpr75 =null;

		Object set74_tree=null;

		try {
			// XPath.g:168:3: ( multiplicativeExpr ( ( '+' | '-' ) multiplicativeExpr )* )
			// XPath.g:168:6: multiplicativeExpr ( ( '+' | '-' ) multiplicativeExpr )*
			{
			root_0 = (Object)adaptor.nil();


			pushFollow(FOLLOW_multiplicativeExpr_in_additiveExpr954);
			multiplicativeExpr73=multiplicativeExpr();
			state._fsp--;

			adaptor.addChild(root_0, multiplicativeExpr73.getTree());

			// XPath.g:168:25: ( ( '+' | '-' ) multiplicativeExpr )*
			loop20:
			while (true) {
				int alt20=2;
				int LA20_0 = input.LA(1);
				if ( (LA20_0==MINUS||LA20_0==PLUS) ) {
					alt20=1;
				}

				switch (alt20) {
				case 1 :
					// XPath.g:168:26: ( '+' | '-' ) multiplicativeExpr
					{
					set74=input.LT(1);
					if ( input.LA(1)==MINUS||input.LA(1)==PLUS ) {
						input.consume();
						adaptor.addChild(root_0, (Object)adaptor.create(set74));
						state.errorRecovery=false;
					}
					else {
						MismatchedSetException mse = new MismatchedSetException(null,input);
						throw mse;
					}
					pushFollow(FOLLOW_multiplicativeExpr_in_additiveExpr963);
					multiplicativeExpr75=multiplicativeExpr();
					state._fsp--;

					adaptor.addChild(root_0, multiplicativeExpr75.getTree());

					}
					break;

				default :
					break loop20;
				}
			}

			}

			retval.stop = input.LT(-1);

			retval.tree = (Object)adaptor.rulePostProcessing(root_0);
			adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);

		}
		catch (RecognitionException re) {
			reportError(re);
			recover(input,re);
			retval.tree = (Object)adaptor.errorNode(input, retval.start, input.LT(-1), re);
		}
		finally {
			// do for sure before leaving
		}
		return retval;
	}
	// $ANTLR end "additiveExpr"


	public static class multiplicativeExpr_return extends ParserRuleReturnScope {
		Object tree;
		@Override
		public Object getTree() { return tree; }
	};


	// $ANTLR start "multiplicativeExpr"
	// XPath.g:171:1: multiplicativeExpr : ( unaryExprNoRoot ( ( '*' | 'div' | 'mod' ) multiplicativeExpr )? | '/' ( ( 'div' | 'mod' ) multiplicativeExpr )? );
	public final XPathParser.multiplicativeExpr_return multiplicativeExpr() throws RecognitionException {
		XPathParser.multiplicativeExpr_return retval = new XPathParser.multiplicativeExpr_return();
		retval.start = input.LT(1);

		Object root_0 = null;

		Token set77=null;
		Token char_literal79=null;
		Token set80=null;
		ParserRuleReturnScope unaryExprNoRoot76 =null;
		ParserRuleReturnScope multiplicativeExpr78 =null;
		ParserRuleReturnScope multiplicativeExpr81 =null;

		Object set77_tree=null;
		Object char_literal79_tree=null;
		Object set80_tree=null;

		try {
			// XPath.g:172:3: ( unaryExprNoRoot ( ( '*' | 'div' | 'mod' ) multiplicativeExpr )? | '/' ( ( 'div' | 'mod' ) multiplicativeExpr )? )
			int alt23=2;
			int LA23_0 = input.LA(1);
			if ( (LA23_0==ABRPATH||LA23_0==AT||LA23_0==AxisName||(LA23_0 >= DOT && LA23_0 <= DOTDOT)||(LA23_0 >= LPAR && LA23_0 <= MINUS)||(LA23_0 >= MUL && LA23_0 <= NCName)||(LA23_0 >= NodeType && LA23_0 <= Number)||LA23_0==44||LA23_0==50) ) {
				alt23=1;
			}
			else if ( (LA23_0==PATHSEP) ) {
				int LA23_2 = input.LA(2);
				if ( (LA23_2==AT||LA23_2==AxisName||(LA23_2 >= DOT && LA23_2 <= DOTDOT)||(LA23_2 >= MUL && LA23_2 <= NCName)||LA23_2==NodeType||LA23_2==PIPE||LA23_2==50) ) {
					alt23=1;
				}
				else if ( (LA23_2==EOF||LA23_2==COMMA||LA23_2==GE||(LA23_2 >= LE && LA23_2 <= LESS)||(LA23_2 >= MINUS && LA23_2 <= MORE)||LA23_2==PLUS||LA23_2==RBRAC||LA23_2==RPAR||LA23_2==43||(LA23_2 >= 45 && LA23_2 <= 49)) ) {
					alt23=2;
				}

				else {
					int nvaeMark = input.mark();
					try {
						input.consume();
						NoViableAltException nvae =
							new NoViableAltException("", 23, 2, input);
						throw nvae;
					} finally {
						input.rewind(nvaeMark);
					}
				}

			}

			else {
				NoViableAltException nvae =
					new NoViableAltException("", 23, 0, input);
				throw nvae;
			}

			switch (alt23) {
				case 1 :
					// XPath.g:172:6: unaryExprNoRoot ( ( '*' | 'div' | 'mod' ) multiplicativeExpr )?
					{
					root_0 = (Object)adaptor.nil();


					pushFollow(FOLLOW_unaryExprNoRoot_in_multiplicativeExpr979);
					unaryExprNoRoot76=unaryExprNoRoot();
					state._fsp--;

					adaptor.addChild(root_0, unaryExprNoRoot76.getTree());

					// XPath.g:172:22: ( ( '*' | 'div' | 'mod' ) multiplicativeExpr )?
					int alt21=2;
					int LA21_0 = input.LA(1);
					if ( (LA21_0==MUL||(LA21_0 >= 47 && LA21_0 <= 48)) ) {
						alt21=1;
					}
					switch (alt21) {
						case 1 :
							// XPath.g:172:23: ( '*' | 'div' | 'mod' ) multiplicativeExpr
							{
							set77=input.LT(1);
							if ( input.LA(1)==MUL||(input.LA(1) >= 47 && input.LA(1) <= 48) ) {
								input.consume();
								adaptor.addChild(root_0, (Object)adaptor.create(set77));
								state.errorRecovery=false;
							}
							else {
								MismatchedSetException mse = new MismatchedSetException(null,input);
								throw mse;
							}
							pushFollow(FOLLOW_multiplicativeExpr_in_multiplicativeExpr990);
							multiplicativeExpr78=multiplicativeExpr();
							state._fsp--;

							adaptor.addChild(root_0, multiplicativeExpr78.getTree());

							}
							break;

					}

					}
					break;
				case 2 :
					// XPath.g:173:6: '/' ( ( 'div' | 'mod' ) multiplicativeExpr )?
					{
					root_0 = (Object)adaptor.nil();


					char_literal79=(Token)match(input,PATHSEP,FOLLOW_PATHSEP_in_multiplicativeExpr999); 
					char_literal79_tree = (Object)adaptor.create(char_literal79);
					adaptor.addChild(root_0, char_literal79_tree);

					// XPath.g:173:10: ( ( 'div' | 'mod' ) multiplicativeExpr )?
					int alt22=2;
					int LA22_0 = input.LA(1);
					if ( ((LA22_0 >= 47 && LA22_0 <= 48)) ) {
						alt22=1;
					}
					switch (alt22) {
						case 1 :
							// XPath.g:173:11: ( 'div' | 'mod' ) multiplicativeExpr
							{
							set80=input.LT(1);
							if ( (input.LA(1) >= 47 && input.LA(1) <= 48) ) {
								input.consume();
								adaptor.addChild(root_0, (Object)adaptor.create(set80));
								state.errorRecovery=false;
							}
							else {
								MismatchedSetException mse = new MismatchedSetException(null,input);
								throw mse;
							}
							pushFollow(FOLLOW_multiplicativeExpr_in_multiplicativeExpr1008);
							multiplicativeExpr81=multiplicativeExpr();
							state._fsp--;

							adaptor.addChild(root_0, multiplicativeExpr81.getTree());

							}
							break;

					}

					}
					break;

			}
			retval.stop = input.LT(-1);

			retval.tree = (Object)adaptor.rulePostProcessing(root_0);
			adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);

		}
		catch (RecognitionException re) {
			reportError(re);
			recover(input,re);
			retval.tree = (Object)adaptor.errorNode(input, retval.start, input.LT(-1), re);
		}
		finally {
			// do for sure before leaving
		}
		return retval;
	}
	// $ANTLR end "multiplicativeExpr"


	public static class unaryExprNoRoot_return extends ParserRuleReturnScope {
		Object tree;
		@Override
		public Object getTree() { return tree; }
	};


	// $ANTLR start "unaryExprNoRoot"
	// XPath.g:176:1: unaryExprNoRoot : ( '-' )* unionExprNoRoot ;
	public final XPathParser.unaryExprNoRoot_return unaryExprNoRoot() throws RecognitionException {
		XPathParser.unaryExprNoRoot_return retval = new XPathParser.unaryExprNoRoot_return();
		retval.start = input.LT(1);

		Object root_0 = null;

		Token char_literal82=null;
		ParserRuleReturnScope unionExprNoRoot83 =null;

		Object char_literal82_tree=null;

		try {
			// XPath.g:177:3: ( ( '-' )* unionExprNoRoot )
			// XPath.g:177:6: ( '-' )* unionExprNoRoot
			{
			root_0 = (Object)adaptor.nil();


			// XPath.g:177:6: ( '-' )*
			loop24:
			while (true) {
				int alt24=2;
				int LA24_0 = input.LA(1);
				if ( (LA24_0==MINUS) ) {
					alt24=1;
				}

				switch (alt24) {
				case 1 :
					// XPath.g:177:6: '-'
					{
					char_literal82=(Token)match(input,MINUS,FOLLOW_MINUS_in_unaryExprNoRoot1024); 
					char_literal82_tree = (Object)adaptor.create(char_literal82);
					adaptor.addChild(root_0, char_literal82_tree);

					}
					break;

				default :
					break loop24;
				}
			}

			pushFollow(FOLLOW_unionExprNoRoot_in_unaryExprNoRoot1027);
			unionExprNoRoot83=unionExprNoRoot();
			state._fsp--;

			adaptor.addChild(root_0, unionExprNoRoot83.getTree());

			}

			retval.stop = input.LT(-1);

			retval.tree = (Object)adaptor.rulePostProcessing(root_0);
			adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);

		}
		catch (RecognitionException re) {
			reportError(re);
			recover(input,re);
			retval.tree = (Object)adaptor.errorNode(input, retval.start, input.LT(-1), re);
		}
		finally {
			// do for sure before leaving
		}
		return retval;
	}
	// $ANTLR end "unaryExprNoRoot"


	public static class qName_return extends ParserRuleReturnScope {
		Object tree;
		@Override
		public Object getTree() { return tree; }
	};


	// $ANTLR start "qName"
	// XPath.g:180:1: qName : nCName ( ':' nCName )? ;
	public final XPathParser.qName_return qName() throws RecognitionException {
		XPathParser.qName_return retval = new XPathParser.qName_return();
		retval.start = input.LT(1);

		Object root_0 = null;

		Token char_literal85=null;
		ParserRuleReturnScope nCName84 =null;
		ParserRuleReturnScope nCName86 =null;

		Object char_literal85_tree=null;

		try {
			// XPath.g:180:8: ( nCName ( ':' nCName )? )
			// XPath.g:180:11: nCName ( ':' nCName )?
			{
			root_0 = (Object)adaptor.nil();


			pushFollow(FOLLOW_nCName_in_qName1040);
			nCName84=nCName();
			state._fsp--;

			adaptor.addChild(root_0, nCName84.getTree());

			// XPath.g:180:18: ( ':' nCName )?
			int alt25=2;
			int LA25_0 = input.LA(1);
			if ( (LA25_0==COLON) ) {
				alt25=1;
			}
			switch (alt25) {
				case 1 :
					// XPath.g:180:19: ':' nCName
					{
					char_literal85=(Token)match(input,COLON,FOLLOW_COLON_in_qName1043); 
					char_literal85_tree = (Object)adaptor.create(char_literal85);
					adaptor.addChild(root_0, char_literal85_tree);

					pushFollow(FOLLOW_nCName_in_qName1045);
					nCName86=nCName();
					state._fsp--;

					adaptor.addChild(root_0, nCName86.getTree());

					}
					break;

			}

			}

			retval.stop = input.LT(-1);

			retval.tree = (Object)adaptor.rulePostProcessing(root_0);
			adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);

		}
		catch (RecognitionException re) {
			reportError(re);
			recover(input,re);
			retval.tree = (Object)adaptor.errorNode(input, retval.start, input.LT(-1), re);
		}
		finally {
			// do for sure before leaving
		}
		return retval;
	}
	// $ANTLR end "qName"


	public static class functionName_return extends ParserRuleReturnScope {
		Object tree;
		@Override
		public Object getTree() { return tree; }
	};


	// $ANTLR start "functionName"
	// XPath.g:183:1: functionName : qName ;
	public final XPathParser.functionName_return functionName() throws RecognitionException {
		XPathParser.functionName_return retval = new XPathParser.functionName_return();
		retval.start = input.LT(1);

		Object root_0 = null;

		ParserRuleReturnScope qName87 =null;


		try {
			// XPath.g:184:3: ( qName )
			// XPath.g:184:6: qName
			{
			root_0 = (Object)adaptor.nil();


			pushFollow(FOLLOW_qName_in_functionName1061);
			qName87=qName();
			state._fsp--;

			adaptor.addChild(root_0, qName87.getTree());

			}

			retval.stop = input.LT(-1);

			retval.tree = (Object)adaptor.rulePostProcessing(root_0);
			adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);

		}
		catch (RecognitionException re) {
			reportError(re);
			recover(input,re);
			retval.tree = (Object)adaptor.errorNode(input, retval.start, input.LT(-1), re);
		}
		finally {
			// do for sure before leaving
		}
		return retval;
	}
	// $ANTLR end "functionName"


	public static class variableReference_return extends ParserRuleReturnScope {
		Object tree;
		@Override
		public Object getTree() { return tree; }
	};


	// $ANTLR start "variableReference"
	// XPath.g:187:1: variableReference : '$' qName ;
	public final XPathParser.variableReference_return variableReference() throws RecognitionException {
		XPathParser.variableReference_return retval = new XPathParser.variableReference_return();
		retval.start = input.LT(1);

		Object root_0 = null;

		Token char_literal88=null;
		ParserRuleReturnScope qName89 =null;

		Object char_literal88_tree=null;

		try {
			// XPath.g:188:3: ( '$' qName )
			// XPath.g:188:6: '$' qName
			{
			root_0 = (Object)adaptor.nil();


			char_literal88=(Token)match(input,44,FOLLOW_44_in_variableReference1077); 
			char_literal88_tree = (Object)adaptor.create(char_literal88);
			adaptor.addChild(root_0, char_literal88_tree);

			pushFollow(FOLLOW_qName_in_variableReference1079);
			qName89=qName();
			state._fsp--;

			adaptor.addChild(root_0, qName89.getTree());

			}

			retval.stop = input.LT(-1);

			retval.tree = (Object)adaptor.rulePostProcessing(root_0);
			adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);

		}
		catch (RecognitionException re) {
			reportError(re);
			recover(input,re);
			retval.tree = (Object)adaptor.errorNode(input, retval.start, input.LT(-1), re);
		}
		finally {
			// do for sure before leaving
		}
		return retval;
	}
	// $ANTLR end "variableReference"


	public static class nameTest_return extends ParserRuleReturnScope {
		Object tree;
		@Override
		public Object getTree() { return tree; }
	};


	// $ANTLR start "nameTest"
	// XPath.g:191:1: nameTest : ( '*' | nCName ':' '*' | qName );
	public final XPathParser.nameTest_return nameTest() throws RecognitionException {
		XPathParser.nameTest_return retval = new XPathParser.nameTest_return();
		retval.start = input.LT(1);

		Object root_0 = null;

		Token char_literal90=null;
		Token char_literal92=null;
		Token char_literal93=null;
		ParserRuleReturnScope nCName91 =null;
		ParserRuleReturnScope qName94 =null;

		Object char_literal90_tree=null;
		Object char_literal92_tree=null;
		Object char_literal93_tree=null;

		try {
			// XPath.g:191:9: ( '*' | nCName ':' '*' | qName )
			int alt26=3;
			int LA26_0 = input.LA(1);
			if ( (LA26_0==MUL) ) {
				alt26=1;
			}
			else if ( (LA26_0==AxisName||LA26_0==NCName) ) {
				int LA26_2 = input.LA(2);
				if ( (LA26_2==COLON) ) {
					int LA26_3 = input.LA(3);
					if ( (LA26_3==MUL) ) {
						alt26=2;
					}
					else if ( (LA26_3==AxisName||LA26_3==NCName) ) {
						alt26=3;
					}

					else {
						int nvaeMark = input.mark();
						try {
							for (int nvaeConsume = 0; nvaeConsume < 3 - 1; nvaeConsume++) {
								input.consume();
							}
							NoViableAltException nvae =
								new NoViableAltException("", 26, 3, input);
							throw nvae;
						} finally {
							input.rewind(nvaeMark);
						}
					}

				}
				else if ( (LA26_2==EOF||LA26_2==ABRPATH||LA26_2==COMMA||(LA26_2 >= GE && LA26_2 <= LESS)||(LA26_2 >= MINUS && LA26_2 <= MUL)||(LA26_2 >= PATHSEP && LA26_2 <= PLUS)||LA26_2==RBRAC||LA26_2==RPAR||LA26_2==43||(LA26_2 >= 45 && LA26_2 <= 49)) ) {
					alt26=3;
				}

				else {
					int nvaeMark = input.mark();
					try {
						input.consume();
						NoViableAltException nvae =
							new NoViableAltException("", 26, 2, input);
						throw nvae;
					} finally {
						input.rewind(nvaeMark);
					}
				}

			}

			else {
				NoViableAltException nvae =
					new NoViableAltException("", 26, 0, input);
				throw nvae;
			}

			switch (alt26) {
				case 1 :
					// XPath.g:191:12: '*'
					{
					root_0 = (Object)adaptor.nil();


					char_literal90=(Token)match(input,MUL,FOLLOW_MUL_in_nameTest1090); 
					char_literal90_tree = (Object)adaptor.create(char_literal90);
					adaptor.addChild(root_0, char_literal90_tree);

					}
					break;
				case 2 :
					// XPath.g:192:6: nCName ':' '*'
					{
					root_0 = (Object)adaptor.nil();


					pushFollow(FOLLOW_nCName_in_nameTest1097);
					nCName91=nCName();
					state._fsp--;

					adaptor.addChild(root_0, nCName91.getTree());

					char_literal92=(Token)match(input,COLON,FOLLOW_COLON_in_nameTest1099); 
					char_literal92_tree = (Object)adaptor.create(char_literal92);
					adaptor.addChild(root_0, char_literal92_tree);

					char_literal93=(Token)match(input,MUL,FOLLOW_MUL_in_nameTest1101); 
					char_literal93_tree = (Object)adaptor.create(char_literal93);
					adaptor.addChild(root_0, char_literal93_tree);

					}
					break;
				case 3 :
					// XPath.g:193:6: qName
					{
					root_0 = (Object)adaptor.nil();


					pushFollow(FOLLOW_qName_in_nameTest1108);
					qName94=qName();
					state._fsp--;

					adaptor.addChild(root_0, qName94.getTree());

					}
					break;

			}
			retval.stop = input.LT(-1);

			retval.tree = (Object)adaptor.rulePostProcessing(root_0);
			adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);

		}
		catch (RecognitionException re) {
			reportError(re);
			recover(input,re);
			retval.tree = (Object)adaptor.errorNode(input, retval.start, input.LT(-1), re);
		}
		finally {
			// do for sure before leaving
		}
		return retval;
	}
	// $ANTLR end "nameTest"


	public static class nCName_return extends ParserRuleReturnScope {
		Object tree;
		@Override
		public Object getTree() { return tree; }
	};


	// $ANTLR start "nCName"
	// XPath.g:196:1: nCName : ( NCName | AxisName );
	public final XPathParser.nCName_return nCName() throws RecognitionException {
		XPathParser.nCName_return retval = new XPathParser.nCName_return();
		retval.start = input.LT(1);

		Object root_0 = null;

		Token set95=null;

		Object set95_tree=null;

		try {
			// XPath.g:196:9: ( NCName | AxisName )
			// XPath.g:
			{
			root_0 = (Object)adaptor.nil();


			set95=input.LT(1);
			if ( input.LA(1)==AxisName||input.LA(1)==NCName ) {
				input.consume();
				adaptor.addChild(root_0, (Object)adaptor.create(set95));
				state.errorRecovery=false;
			}
			else {
				MismatchedSetException mse = new MismatchedSetException(null,input);
				throw mse;
			}
			}

			retval.stop = input.LT(-1);

			retval.tree = (Object)adaptor.rulePostProcessing(root_0);
			adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);

		}
		catch (RecognitionException re) {
			reportError(re);
			recover(input,re);
			retval.tree = (Object)adaptor.errorNode(input, retval.start, input.LT(-1), re);
		}
		finally {
			// do for sure before leaving
		}
		return retval;
	}
	// $ANTLR end "nCName"

	// Delegated rules



	public static final BitSet FOLLOW_expr_in_main316 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_locationPath_in_locationPathI338 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_relativeLocationPath_in_locationPath361 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_absolutePathI_in_locationPath368 = new BitSet(new long[]{0x000000000000C500L});
	public static final BitSet FOLLOW_relativeLocationPath_in_locationPath370 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_absolutePath_in_absolutePathI384 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_step_in_relativeLocationPath424 = new BitSet(new long[]{0x0000000400000022L});
	public static final BitSet FOLLOW_set_in_relativeLocationPath427 = new BitSet(new long[]{0x000000000000C500L});
	public static final BitSet FOLLOW_step_in_relativeLocationPath433 = new BitSet(new long[]{0x0000000400000022L});
	public static final BitSet FOLLOW_axisSpecifierI_in_step448 = new BitSet(new long[]{0x0004000118000400L});
	public static final BitSet FOLLOW_nodeTestI_in_step450 = new BitSet(new long[]{0x0000000000080002L});
	public static final BitSet FOLLOW_predicateI_in_step452 = new BitSet(new long[]{0x0000000000080002L});
	public static final BitSet FOLLOW_abbreviatedStepI_in_step460 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_axisSpecifier_in_axisSpecifierI474 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_AxisName_in_axisSpecifier497 = new BitSet(new long[]{0x0000000000000800L});
	public static final BitSet FOLLOW_CC_in_axisSpecifier499 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_AT_in_axisSpecifier506 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_nodeTest_in_nodeTestI521 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_nameTest_in_nodeTest540 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_NodeType_in_nodeTest547 = new BitSet(new long[]{0x0000000000800000L});
	public static final BitSet FOLLOW_LPAR_in_nodeTest549 = new BitSet(new long[]{0x0000020000000000L});
	public static final BitSet FOLLOW_RPAR_in_nodeTest551 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_50_in_nodeTest558 = new BitSet(new long[]{0x0000000000800000L});
	public static final BitSet FOLLOW_LPAR_in_nodeTest560 = new BitSet(new long[]{0x0000000001000000L});
	public static final BitSet FOLLOW_Literal_in_nodeTest562 = new BitSet(new long[]{0x0000020000000000L});
	public static final BitSet FOLLOW_RPAR_in_nodeTest564 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_predicate_in_predicateI578 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_LBRAC_in_predicate600 = new BitSet(new long[]{0x000010061380C520L});
	public static final BitSet FOLLOW_expr_in_predicate602 = new BitSet(new long[]{0x0000008000000000L});
	public static final BitSet FOLLOW_RBRAC_in_predicate604 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_abbreviatedStep_in_abbreviatedStepI618 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_orExpr_in_expr660 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_variableReference_in_primaryExpr674 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_LPAR_in_primaryExpr681 = new BitSet(new long[]{0x000010061380C520L});
	public static final BitSet FOLLOW_expr_in_primaryExpr683 = new BitSet(new long[]{0x0000020000000000L});
	public static final BitSet FOLLOW_RPAR_in_primaryExpr685 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_Literal_in_primaryExpr692 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_Number_in_primaryExpr699 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_functionCall_in_primaryExpr708 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_functionName_in_functionCall722 = new BitSet(new long[]{0x0000000000800000L});
	public static final BitSet FOLLOW_LPAR_in_functionCall724 = new BitSet(new long[]{0x000012061380C520L});
	public static final BitSet FOLLOW_expr_in_functionCall728 = new BitSet(new long[]{0x0000020000002000L});
	public static final BitSet FOLLOW_COMMA_in_functionCall732 = new BitSet(new long[]{0x000010061380C520L});
	public static final BitSet FOLLOW_expr_in_functionCall734 = new BitSet(new long[]{0x0000020000002000L});
	public static final BitSet FOLLOW_RPAR_in_functionCall742 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_pathExprNoRootI_in_unionExprNoRoot756 = new BitSet(new long[]{0x0000000800000002L});
	public static final BitSet FOLLOW_PIPE_in_unionExprNoRoot759 = new BitSet(new long[]{0x000010061180C520L});
	public static final BitSet FOLLOW_unionExprNoRoot_in_unionExprNoRoot761 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_PATHSEP_in_unionExprNoRoot771 = new BitSet(new long[]{0x0000000800000000L});
	public static final BitSet FOLLOW_PIPE_in_unionExprNoRoot773 = new BitSet(new long[]{0x000010061180C520L});
	public static final BitSet FOLLOW_unionExprNoRoot_in_unionExprNoRoot775 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_pathExprNoRoot_in_pathExprNoRootI789 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_locationPathI_in_pathExprNoRoot811 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_filterExpr_in_pathExprNoRoot818 = new BitSet(new long[]{0x0000000400000022L});
	public static final BitSet FOLLOW_set_in_pathExprNoRoot821 = new BitSet(new long[]{0x000000000000C500L});
	public static final BitSet FOLLOW_relativeLocationPath_in_pathExprNoRoot827 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_primaryExpr_in_filterExpr843 = new BitSet(new long[]{0x0000000000080002L});
	public static final BitSet FOLLOW_predicateI_in_filterExpr845 = new BitSet(new long[]{0x0000000000080002L});
	public static final BitSet FOLLOW_andExpr_in_orExpr859 = new BitSet(new long[]{0x0002000000000002L});
	public static final BitSet FOLLOW_49_in_orExpr862 = new BitSet(new long[]{0x000010061380C520L});
	public static final BitSet FOLLOW_andExpr_in_orExpr864 = new BitSet(new long[]{0x0002000000000002L});
	public static final BitSet FOLLOW_equalityExpr_in_andExpr879 = new BitSet(new long[]{0x0000400000000002L});
	public static final BitSet FOLLOW_46_in_andExpr882 = new BitSet(new long[]{0x000010061380C520L});
	public static final BitSet FOLLOW_equalityExpr_in_andExpr884 = new BitSet(new long[]{0x0000400000000002L});
	public static final BitSet FOLLOW_relationalExpr_in_equalityExpr900 = new BitSet(new long[]{0x0000280000000002L});
	public static final BitSet FOLLOW_set_in_equalityExpr903 = new BitSet(new long[]{0x000010061380C520L});
	public static final BitSet FOLLOW_relationalExpr_in_equalityExpr909 = new BitSet(new long[]{0x0000280000000002L});
	public static final BitSet FOLLOW_additiveExpr_in_relationalExpr925 = new BitSet(new long[]{0x0000000004340002L});
	public static final BitSet FOLLOW_set_in_relationalExpr928 = new BitSet(new long[]{0x000010061380C520L});
	public static final BitSet FOLLOW_additiveExpr_in_relationalExpr938 = new BitSet(new long[]{0x0000000004340002L});
	public static final BitSet FOLLOW_multiplicativeExpr_in_additiveExpr954 = new BitSet(new long[]{0x0000001002000002L});
	public static final BitSet FOLLOW_set_in_additiveExpr957 = new BitSet(new long[]{0x000010061380C520L});
	public static final BitSet FOLLOW_multiplicativeExpr_in_additiveExpr963 = new BitSet(new long[]{0x0000001002000002L});
	public static final BitSet FOLLOW_unaryExprNoRoot_in_multiplicativeExpr979 = new BitSet(new long[]{0x0001800008000002L});
	public static final BitSet FOLLOW_set_in_multiplicativeExpr982 = new BitSet(new long[]{0x000010061380C520L});
	public static final BitSet FOLLOW_multiplicativeExpr_in_multiplicativeExpr990 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_PATHSEP_in_multiplicativeExpr999 = new BitSet(new long[]{0x0001800000000002L});
	public static final BitSet FOLLOW_set_in_multiplicativeExpr1002 = new BitSet(new long[]{0x000010061380C520L});
	public static final BitSet FOLLOW_multiplicativeExpr_in_multiplicativeExpr1008 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_MINUS_in_unaryExprNoRoot1024 = new BitSet(new long[]{0x000010061380C520L});
	public static final BitSet FOLLOW_unionExprNoRoot_in_unaryExprNoRoot1027 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_nCName_in_qName1040 = new BitSet(new long[]{0x0000000000001002L});
	public static final BitSet FOLLOW_COLON_in_qName1043 = new BitSet(new long[]{0x0000000010000400L});
	public static final BitSet FOLLOW_nCName_in_qName1045 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_qName_in_functionName1061 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_44_in_variableReference1077 = new BitSet(new long[]{0x0000000010000400L});
	public static final BitSet FOLLOW_qName_in_variableReference1079 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_MUL_in_nameTest1090 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_nCName_in_nameTest1097 = new BitSet(new long[]{0x0000000000001000L});
	public static final BitSet FOLLOW_COLON_in_nameTest1099 = new BitSet(new long[]{0x0000000008000000L});
	public static final BitSet FOLLOW_MUL_in_nameTest1101 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_qName_in_nameTest1108 = new BitSet(new long[]{0x0000000000000002L});
}
