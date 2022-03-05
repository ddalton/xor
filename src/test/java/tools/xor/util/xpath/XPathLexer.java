// $ANTLR 3.5.2 XPath.g 2014-07-31 11:35:01
package tools.xor.util.xpath;
import org.antlr.runtime.BaseRecognizer;
import org.antlr.runtime.CharStream;
import org.antlr.runtime.DFA;
import org.antlr.runtime.EarlyExitException;
import org.antlr.runtime.IntStream;
import org.antlr.runtime.Lexer;
import org.antlr.runtime.MismatchedSetException;
import org.antlr.runtime.NoViableAltException;
import org.antlr.runtime.RecognitionException;
import org.antlr.runtime.RecognizerSharedState;

@SuppressWarnings("all")
public class XPathLexer extends Lexer {
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
	// delegators
	public Lexer[] getDelegates() {
		return new Lexer[] {};
	}

	public XPathLexer() {} 
	public XPathLexer(CharStream input) {
		this(input, new RecognizerSharedState());
	}
	public XPathLexer(CharStream input, RecognizerSharedState state) {
		super(input,state);
	}
	@Override public String getGrammarFileName() { return "XPath.g"; }

	// $ANTLR start "ABRPATH"
	public final void mABRPATH() throws RecognitionException {
		try {
			int _type = ABRPATH;
			int _channel = DEFAULT_TOKEN_CHANNEL;
			// XPath.g:2:9: ( '//' )
			// XPath.g:2:11: '//'
			{
			match("//"); 

			}

			state.type = _type;
			state.channel = _channel;
		}
		finally {
			// do for sure before leaving
		}
	}
	// $ANTLR end "ABRPATH"

	// $ANTLR start "APOS"
	public final void mAPOS() throws RecognitionException {
		try {
			int _type = APOS;
			int _channel = DEFAULT_TOKEN_CHANNEL;
			// XPath.g:3:6: ( '\\'' )
			// XPath.g:3:8: '\\''
			{
			match('\''); 
			}

			state.type = _type;
			state.channel = _channel;
		}
		finally {
			// do for sure before leaving
		}
	}
	// $ANTLR end "APOS"

	// $ANTLR start "AT"
	public final void mAT() throws RecognitionException {
		try {
			int _type = AT;
			int _channel = DEFAULT_TOKEN_CHANNEL;
			// XPath.g:4:4: ( '@' )
			// XPath.g:4:6: '@'
			{
			match('@'); 
			}

			state.type = _type;
			state.channel = _channel;
		}
		finally {
			// do for sure before leaving
		}
	}
	// $ANTLR end "AT"

	// $ANTLR start "CC"
	public final void mCC() throws RecognitionException {
		try {
			int _type = CC;
			int _channel = DEFAULT_TOKEN_CHANNEL;
			// XPath.g:5:4: ( '::' )
			// XPath.g:5:6: '::'
			{
			match("::"); 

			}

			state.type = _type;
			state.channel = _channel;
		}
		finally {
			// do for sure before leaving
		}
	}
	// $ANTLR end "CC"

	// $ANTLR start "COLON"
	public final void mCOLON() throws RecognitionException {
		try {
			int _type = COLON;
			int _channel = DEFAULT_TOKEN_CHANNEL;
			// XPath.g:6:7: ( ':' )
			// XPath.g:6:9: ':'
			{
			match(':'); 
			}

			state.type = _type;
			state.channel = _channel;
		}
		finally {
			// do for sure before leaving
		}
	}
	// $ANTLR end "COLON"

	// $ANTLR start "COMMA"
	public final void mCOMMA() throws RecognitionException {
		try {
			int _type = COMMA;
			int _channel = DEFAULT_TOKEN_CHANNEL;
			// XPath.g:7:7: ( ',' )
			// XPath.g:7:9: ','
			{
			match(','); 
			}

			state.type = _type;
			state.channel = _channel;
		}
		finally {
			// do for sure before leaving
		}
	}
	// $ANTLR end "COMMA"

	// $ANTLR start "DOT"
	public final void mDOT() throws RecognitionException {
		try {
			int _type = DOT;
			int _channel = DEFAULT_TOKEN_CHANNEL;
			// XPath.g:8:5: ( '.' )
			// XPath.g:8:7: '.'
			{
			match('.'); 
			}

			state.type = _type;
			state.channel = _channel;
		}
		finally {
			// do for sure before leaving
		}
	}
	// $ANTLR end "DOT"

	// $ANTLR start "DOTDOT"
	public final void mDOTDOT() throws RecognitionException {
		try {
			int _type = DOTDOT;
			int _channel = DEFAULT_TOKEN_CHANNEL;
			// XPath.g:9:8: ( '..' )
			// XPath.g:9:10: '..'
			{
			match(".."); 

			}

			state.type = _type;
			state.channel = _channel;
		}
		finally {
			// do for sure before leaving
		}
	}
	// $ANTLR end "DOTDOT"

	// $ANTLR start "GE"
	public final void mGE() throws RecognitionException {
		try {
			int _type = GE;
			int _channel = DEFAULT_TOKEN_CHANNEL;
			// XPath.g:10:4: ( '>=' )
			// XPath.g:10:6: '>='
			{
			match(">="); 

			}

			state.type = _type;
			state.channel = _channel;
		}
		finally {
			// do for sure before leaving
		}
	}
	// $ANTLR end "GE"

	// $ANTLR start "LBRAC"
	public final void mLBRAC() throws RecognitionException {
		try {
			int _type = LBRAC;
			int _channel = DEFAULT_TOKEN_CHANNEL;
			// XPath.g:11:7: ( '[' )
			// XPath.g:11:9: '['
			{
			match('['); 
			}

			state.type = _type;
			state.channel = _channel;
		}
		finally {
			// do for sure before leaving
		}
	}
	// $ANTLR end "LBRAC"

	// $ANTLR start "LE"
	public final void mLE() throws RecognitionException {
		try {
			int _type = LE;
			int _channel = DEFAULT_TOKEN_CHANNEL;
			// XPath.g:12:4: ( '<=' )
			// XPath.g:12:6: '<='
			{
			match("<="); 

			}

			state.type = _type;
			state.channel = _channel;
		}
		finally {
			// do for sure before leaving
		}
	}
	// $ANTLR end "LE"

	// $ANTLR start "LESS"
	public final void mLESS() throws RecognitionException {
		try {
			int _type = LESS;
			int _channel = DEFAULT_TOKEN_CHANNEL;
			// XPath.g:13:6: ( '<' )
			// XPath.g:13:8: '<'
			{
			match('<'); 
			}

			state.type = _type;
			state.channel = _channel;
		}
		finally {
			// do for sure before leaving
		}
	}
	// $ANTLR end "LESS"

	// $ANTLR start "LPAR"
	public final void mLPAR() throws RecognitionException {
		try {
			int _type = LPAR;
			int _channel = DEFAULT_TOKEN_CHANNEL;
			// XPath.g:14:6: ( '(' )
			// XPath.g:14:8: '('
			{
			match('('); 
			}

			state.type = _type;
			state.channel = _channel;
		}
		finally {
			// do for sure before leaving
		}
	}
	// $ANTLR end "LPAR"

	// $ANTLR start "MINUS"
	public final void mMINUS() throws RecognitionException {
		try {
			int _type = MINUS;
			int _channel = DEFAULT_TOKEN_CHANNEL;
			// XPath.g:15:7: ( '-' )
			// XPath.g:15:9: '-'
			{
			match('-'); 
			}

			state.type = _type;
			state.channel = _channel;
		}
		finally {
			// do for sure before leaving
		}
	}
	// $ANTLR end "MINUS"

	// $ANTLR start "MORE"
	public final void mMORE() throws RecognitionException {
		try {
			int _type = MORE;
			int _channel = DEFAULT_TOKEN_CHANNEL;
			// XPath.g:16:6: ( '>' )
			// XPath.g:16:8: '>'
			{
			match('>'); 
			}

			state.type = _type;
			state.channel = _channel;
		}
		finally {
			// do for sure before leaving
		}
	}
	// $ANTLR end "MORE"

	// $ANTLR start "MUL"
	public final void mMUL() throws RecognitionException {
		try {
			int _type = MUL;
			int _channel = DEFAULT_TOKEN_CHANNEL;
			// XPath.g:17:5: ( '*' )
			// XPath.g:17:7: '*'
			{
			match('*'); 
			}

			state.type = _type;
			state.channel = _channel;
		}
		finally {
			// do for sure before leaving
		}
	}
	// $ANTLR end "MUL"

	// $ANTLR start "PATHSEP"
	public final void mPATHSEP() throws RecognitionException {
		try {
			int _type = PATHSEP;
			int _channel = DEFAULT_TOKEN_CHANNEL;
			// XPath.g:18:9: ( '/' )
			// XPath.g:18:11: '/'
			{
			match('/'); 
			}

			state.type = _type;
			state.channel = _channel;
		}
		finally {
			// do for sure before leaving
		}
	}
	// $ANTLR end "PATHSEP"

	// $ANTLR start "PIPE"
	public final void mPIPE() throws RecognitionException {
		try {
			int _type = PIPE;
			int _channel = DEFAULT_TOKEN_CHANNEL;
			// XPath.g:19:6: ( '|' )
			// XPath.g:19:8: '|'
			{
			match('|'); 
			}

			state.type = _type;
			state.channel = _channel;
		}
		finally {
			// do for sure before leaving
		}
	}
	// $ANTLR end "PIPE"

	// $ANTLR start "PLUS"
	public final void mPLUS() throws RecognitionException {
		try {
			int _type = PLUS;
			int _channel = DEFAULT_TOKEN_CHANNEL;
			// XPath.g:20:6: ( '+' )
			// XPath.g:20:8: '+'
			{
			match('+'); 
			}

			state.type = _type;
			state.channel = _channel;
		}
		finally {
			// do for sure before leaving
		}
	}
	// $ANTLR end "PLUS"

	// $ANTLR start "QUOT"
	public final void mQUOT() throws RecognitionException {
		try {
			int _type = QUOT;
			int _channel = DEFAULT_TOKEN_CHANNEL;
			// XPath.g:21:6: ( '\\\"' )
			// XPath.g:21:8: '\\\"'
			{
			match('\"'); 
			}

			state.type = _type;
			state.channel = _channel;
		}
		finally {
			// do for sure before leaving
		}
	}
	// $ANTLR end "QUOT"

	// $ANTLR start "RBRAC"
	public final void mRBRAC() throws RecognitionException {
		try {
			int _type = RBRAC;
			int _channel = DEFAULT_TOKEN_CHANNEL;
			// XPath.g:22:7: ( ']' )
			// XPath.g:22:9: ']'
			{
			match(']'); 
			}

			state.type = _type;
			state.channel = _channel;
		}
		finally {
			// do for sure before leaving
		}
	}
	// $ANTLR end "RBRAC"

	// $ANTLR start "RPAR"
	public final void mRPAR() throws RecognitionException {
		try {
			int _type = RPAR;
			int _channel = DEFAULT_TOKEN_CHANNEL;
			// XPath.g:23:6: ( ')' )
			// XPath.g:23:8: ')'
			{
			match(')'); 
			}

			state.type = _type;
			state.channel = _channel;
		}
		finally {
			// do for sure before leaving
		}
	}
	// $ANTLR end "RPAR"

	// $ANTLR start "T__43"
	public final void mT__43() throws RecognitionException {
		try {
			int _type = T__43;
			int _channel = DEFAULT_TOKEN_CHANNEL;
			// XPath.g:24:7: ( '!=' )
			// XPath.g:24:9: '!='
			{
			match("!="); 

			}

			state.type = _type;
			state.channel = _channel;
		}
		finally {
			// do for sure before leaving
		}
	}
	// $ANTLR end "T__43"

	// $ANTLR start "T__44"
	public final void mT__44() throws RecognitionException {
		try {
			int _type = T__44;
			int _channel = DEFAULT_TOKEN_CHANNEL;
			// XPath.g:25:7: ( '$' )
			// XPath.g:25:9: '$'
			{
			match('$'); 
			}

			state.type = _type;
			state.channel = _channel;
		}
		finally {
			// do for sure before leaving
		}
	}
	// $ANTLR end "T__44"

	// $ANTLR start "T__45"
	public final void mT__45() throws RecognitionException {
		try {
			int _type = T__45;
			int _channel = DEFAULT_TOKEN_CHANNEL;
			// XPath.g:26:7: ( '=' )
			// XPath.g:26:9: '='
			{
			match('='); 
			}

			state.type = _type;
			state.channel = _channel;
		}
		finally {
			// do for sure before leaving
		}
	}
	// $ANTLR end "T__45"

	// $ANTLR start "T__46"
	public final void mT__46() throws RecognitionException {
		try {
			int _type = T__46;
			int _channel = DEFAULT_TOKEN_CHANNEL;
			// XPath.g:27:7: ( 'and' )
			// XPath.g:27:9: 'and'
			{
			match("and"); 

			}

			state.type = _type;
			state.channel = _channel;
		}
		finally {
			// do for sure before leaving
		}
	}
	// $ANTLR end "T__46"

	// $ANTLR start "T__47"
	public final void mT__47() throws RecognitionException {
		try {
			int _type = T__47;
			int _channel = DEFAULT_TOKEN_CHANNEL;
			// XPath.g:28:7: ( 'div' )
			// XPath.g:28:9: 'div'
			{
			match("div"); 

			}

			state.type = _type;
			state.channel = _channel;
		}
		finally {
			// do for sure before leaving
		}
	}
	// $ANTLR end "T__47"

	// $ANTLR start "T__48"
	public final void mT__48() throws RecognitionException {
		try {
			int _type = T__48;
			int _channel = DEFAULT_TOKEN_CHANNEL;
			// XPath.g:29:7: ( 'mod' )
			// XPath.g:29:9: 'mod'
			{
			match("mod"); 

			}

			state.type = _type;
			state.channel = _channel;
		}
		finally {
			// do for sure before leaving
		}
	}
	// $ANTLR end "T__48"

	// $ANTLR start "T__49"
	public final void mT__49() throws RecognitionException {
		try {
			int _type = T__49;
			int _channel = DEFAULT_TOKEN_CHANNEL;
			// XPath.g:30:7: ( 'or' )
			// XPath.g:30:9: 'or'
			{
			match("or"); 

			}

			state.type = _type;
			state.channel = _channel;
		}
		finally {
			// do for sure before leaving
		}
	}
	// $ANTLR end "T__49"

	// $ANTLR start "T__50"
	public final void mT__50() throws RecognitionException {
		try {
			int _type = T__50;
			int _channel = DEFAULT_TOKEN_CHANNEL;
			// XPath.g:31:7: ( 'processing-instruction' )
			// XPath.g:31:9: 'processing-instruction'
			{
			match("processing-instruction"); 

			}

			state.type = _type;
			state.channel = _channel;
		}
		finally {
			// do for sure before leaving
		}
	}
	// $ANTLR end "T__50"

	// $ANTLR start "NodeType"
	public final void mNodeType() throws RecognitionException {
		try {
			int _type = NodeType;
			int _channel = DEFAULT_TOKEN_CHANNEL;
			// XPath.g:200:9: ( 'comment' | 'text' | 'processing-instruction' | 'node' )
			int alt1=4;
			switch ( input.LA(1) ) {
			case 'c':
				{
				alt1=1;
				}
				break;
			case 't':
				{
				alt1=2;
				}
				break;
			case 'p':
				{
				alt1=3;
				}
				break;
			case 'n':
				{
				alt1=4;
				}
				break;
			default:
				NoViableAltException nvae =
					new NoViableAltException("", 1, 0, input);
				throw nvae;
			}
			switch (alt1) {
				case 1 :
					// XPath.g:200:12: 'comment'
					{
					match("comment"); 

					}
					break;
				case 2 :
					// XPath.g:201:6: 'text'
					{
					match("text"); 

					}
					break;
				case 3 :
					// XPath.g:202:6: 'processing-instruction'
					{
					match("processing-instruction"); 

					}
					break;
				case 4 :
					// XPath.g:203:6: 'node'
					{
					match("node"); 

					}
					break;

			}
			state.type = _type;
			state.channel = _channel;
		}
		finally {
			// do for sure before leaving
		}
	}
	// $ANTLR end "NodeType"

	// $ANTLR start "Number"
	public final void mNumber() throws RecognitionException {
		try {
			int _type = Number;
			int _channel = DEFAULT_TOKEN_CHANNEL;
			// XPath.g:206:9: ( Digits ( '.' ( Digits )? )? | '.' Digits )
			int alt4=2;
			int LA4_0 = input.LA(1);
			if ( ((LA4_0 >= '0' && LA4_0 <= '9')) ) {
				alt4=1;
			}
			else if ( (LA4_0=='.') ) {
				alt4=2;
			}

			else {
				NoViableAltException nvae =
					new NoViableAltException("", 4, 0, input);
				throw nvae;
			}

			switch (alt4) {
				case 1 :
					// XPath.g:206:12: Digits ( '.' ( Digits )? )?
					{
					mDigits(); 

					// XPath.g:206:19: ( '.' ( Digits )? )?
					int alt3=2;
					int LA3_0 = input.LA(1);
					if ( (LA3_0=='.') ) {
						alt3=1;
					}
					switch (alt3) {
						case 1 :
							// XPath.g:206:20: '.' ( Digits )?
							{
							match('.'); 
							// XPath.g:206:24: ( Digits )?
							int alt2=2;
							int LA2_0 = input.LA(1);
							if ( ((LA2_0 >= '0' && LA2_0 <= '9')) ) {
								alt2=1;
							}
							switch (alt2) {
								case 1 :
									// XPath.g:206:24: Digits
									{
									mDigits(); 

									}
									break;

							}

							}
							break;

					}

					}
					break;
				case 2 :
					// XPath.g:207:6: '.' Digits
					{
					match('.'); 
					mDigits(); 

					}
					break;

			}
			state.type = _type;
			state.channel = _channel;
		}
		finally {
			// do for sure before leaving
		}
	}
	// $ANTLR end "Number"

	// $ANTLR start "Digits"
	public final void mDigits() throws RecognitionException {
		try {
			// XPath.g:212:9: ( ( '0' .. '9' )+ )
			// XPath.g:212:12: ( '0' .. '9' )+
			{
			// XPath.g:212:12: ( '0' .. '9' )+
			int cnt5=0;
			loop5:
			while (true) {
				int alt5=2;
				int LA5_0 = input.LA(1);
				if ( ((LA5_0 >= '0' && LA5_0 <= '9')) ) {
					alt5=1;
				}

				switch (alt5) {
				case 1 :
					// XPath.g:
					{
					if ( (input.LA(1) >= '0' && input.LA(1) <= '9') ) {
						input.consume();
					}
					else {
						MismatchedSetException mse = new MismatchedSetException(null,input);
						recover(mse);
						throw mse;
					}
					}
					break;

				default :
					if ( cnt5 >= 1 ) break loop5;
					EarlyExitException eee = new EarlyExitException(5, input);
					throw eee;
				}
				cnt5++;
			}

			}

		}
		finally {
			// do for sure before leaving
		}
	}
	// $ANTLR end "Digits"

	// $ANTLR start "AxisName"
	public final void mAxisName() throws RecognitionException {
		try {
			int _type = AxisName;
			int _channel = DEFAULT_TOKEN_CHANNEL;
			// XPath.g:214:9: ( 'ancestor' | 'ancestor-or-self' | 'attribute' | 'child' | 'descendant' | 'descendant-or-self' | 'following' | 'following-sibling' | 'namespace' | 'parent' | 'preceding' | 'preceding-sibling' | 'self' )
			int alt6=13;
			switch ( input.LA(1) ) {
			case 'a':
				{
				int LA6_1 = input.LA(2);
				if ( (LA6_1=='n') ) {
					int LA6_8 = input.LA(3);
					if ( (LA6_8=='c') ) {
						int LA6_14 = input.LA(4);
						if ( (LA6_14=='e') ) {
							int LA6_18 = input.LA(5);
							if ( (LA6_18=='s') ) {
								int LA6_22 = input.LA(6);
								if ( (LA6_22=='t') ) {
									int LA6_26 = input.LA(7);
									if ( (LA6_26=='o') ) {
										int LA6_30 = input.LA(8);
										if ( (LA6_30=='r') ) {
											int LA6_34 = input.LA(9);
											if ( (LA6_34=='-') ) {
												alt6=2;
											}

											else {
												alt6=1;
											}

										}

										else {
											int nvaeMark = input.mark();
											try {
												for (int nvaeConsume = 0; nvaeConsume < 8 - 1; nvaeConsume++) {
													input.consume();
												}
												NoViableAltException nvae =
													new NoViableAltException("", 6, 30, input);
												throw nvae;
											} finally {
												input.rewind(nvaeMark);
											}
										}

									}

									else {
										int nvaeMark = input.mark();
										try {
											for (int nvaeConsume = 0; nvaeConsume < 7 - 1; nvaeConsume++) {
												input.consume();
											}
											NoViableAltException nvae =
												new NoViableAltException("", 6, 26, input);
											throw nvae;
										} finally {
											input.rewind(nvaeMark);
										}
									}

								}

								else {
									int nvaeMark = input.mark();
									try {
										for (int nvaeConsume = 0; nvaeConsume < 6 - 1; nvaeConsume++) {
											input.consume();
										}
										NoViableAltException nvae =
											new NoViableAltException("", 6, 22, input);
										throw nvae;
									} finally {
										input.rewind(nvaeMark);
									}
								}

							}

							else {
								int nvaeMark = input.mark();
								try {
									for (int nvaeConsume = 0; nvaeConsume < 5 - 1; nvaeConsume++) {
										input.consume();
									}
									NoViableAltException nvae =
										new NoViableAltException("", 6, 18, input);
									throw nvae;
								} finally {
									input.rewind(nvaeMark);
								}
							}

						}

						else {
							int nvaeMark = input.mark();
							try {
								for (int nvaeConsume = 0; nvaeConsume < 4 - 1; nvaeConsume++) {
									input.consume();
								}
								NoViableAltException nvae =
									new NoViableAltException("", 6, 14, input);
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
								new NoViableAltException("", 6, 8, input);
							throw nvae;
						} finally {
							input.rewind(nvaeMark);
						}
					}

				}
				else if ( (LA6_1=='t') ) {
					alt6=3;
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
				break;
			case 'c':
				{
				alt6=4;
				}
				break;
			case 'd':
				{
				int LA6_3 = input.LA(2);
				if ( (LA6_3=='e') ) {
					int LA6_10 = input.LA(3);
					if ( (LA6_10=='s') ) {
						int LA6_15 = input.LA(4);
						if ( (LA6_15=='c') ) {
							int LA6_19 = input.LA(5);
							if ( (LA6_19=='e') ) {
								int LA6_23 = input.LA(6);
								if ( (LA6_23=='n') ) {
									int LA6_27 = input.LA(7);
									if ( (LA6_27=='d') ) {
										int LA6_31 = input.LA(8);
										if ( (LA6_31=='a') ) {
											int LA6_35 = input.LA(9);
											if ( (LA6_35=='n') ) {
												int LA6_40 = input.LA(10);
												if ( (LA6_40=='t') ) {
													int LA6_43 = input.LA(11);
													if ( (LA6_43=='-') ) {
														alt6=6;
													}

													else {
														alt6=5;
													}

												}

												else {
													int nvaeMark = input.mark();
													try {
														for (int nvaeConsume = 0; nvaeConsume < 10 - 1; nvaeConsume++) {
															input.consume();
														}
														NoViableAltException nvae =
															new NoViableAltException("", 6, 40, input);
														throw nvae;
													} finally {
														input.rewind(nvaeMark);
													}
												}

											}

											else {
												int nvaeMark = input.mark();
												try {
													for (int nvaeConsume = 0; nvaeConsume < 9 - 1; nvaeConsume++) {
														input.consume();
													}
													NoViableAltException nvae =
														new NoViableAltException("", 6, 35, input);
													throw nvae;
												} finally {
													input.rewind(nvaeMark);
												}
											}

										}

										else {
											int nvaeMark = input.mark();
											try {
												for (int nvaeConsume = 0; nvaeConsume < 8 - 1; nvaeConsume++) {
													input.consume();
												}
												NoViableAltException nvae =
													new NoViableAltException("", 6, 31, input);
												throw nvae;
											} finally {
												input.rewind(nvaeMark);
											}
										}

									}

									else {
										int nvaeMark = input.mark();
										try {
											for (int nvaeConsume = 0; nvaeConsume < 7 - 1; nvaeConsume++) {
												input.consume();
											}
											NoViableAltException nvae =
												new NoViableAltException("", 6, 27, input);
											throw nvae;
										} finally {
											input.rewind(nvaeMark);
										}
									}

								}

								else {
									int nvaeMark = input.mark();
									try {
										for (int nvaeConsume = 0; nvaeConsume < 6 - 1; nvaeConsume++) {
											input.consume();
										}
										NoViableAltException nvae =
											new NoViableAltException("", 6, 23, input);
										throw nvae;
									} finally {
										input.rewind(nvaeMark);
									}
								}

							}

							else {
								int nvaeMark = input.mark();
								try {
									for (int nvaeConsume = 0; nvaeConsume < 5 - 1; nvaeConsume++) {
										input.consume();
									}
									NoViableAltException nvae =
										new NoViableAltException("", 6, 19, input);
									throw nvae;
								} finally {
									input.rewind(nvaeMark);
								}
							}

						}

						else {
							int nvaeMark = input.mark();
							try {
								for (int nvaeConsume = 0; nvaeConsume < 4 - 1; nvaeConsume++) {
									input.consume();
								}
								NoViableAltException nvae =
									new NoViableAltException("", 6, 15, input);
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
								new NoViableAltException("", 6, 10, input);
							throw nvae;
						} finally {
							input.rewind(nvaeMark);
						}
					}

				}

				else {
					int nvaeMark = input.mark();
					try {
						input.consume();
						NoViableAltException nvae =
							new NoViableAltException("", 6, 3, input);
						throw nvae;
					} finally {
						input.rewind(nvaeMark);
					}
				}

				}
				break;
			case 'f':
				{
				int LA6_4 = input.LA(2);
				if ( (LA6_4=='o') ) {
					int LA6_11 = input.LA(3);
					if ( (LA6_11=='l') ) {
						int LA6_16 = input.LA(4);
						if ( (LA6_16=='l') ) {
							int LA6_20 = input.LA(5);
							if ( (LA6_20=='o') ) {
								int LA6_24 = input.LA(6);
								if ( (LA6_24=='w') ) {
									int LA6_28 = input.LA(7);
									if ( (LA6_28=='i') ) {
										int LA6_32 = input.LA(8);
										if ( (LA6_32=='n') ) {
											int LA6_36 = input.LA(9);
											if ( (LA6_36=='g') ) {
												int LA6_41 = input.LA(10);
												if ( (LA6_41=='-') ) {
													alt6=8;
												}

												else {
													alt6=7;
												}

											}

											else {
												int nvaeMark = input.mark();
												try {
													for (int nvaeConsume = 0; nvaeConsume < 9 - 1; nvaeConsume++) {
														input.consume();
													}
													NoViableAltException nvae =
														new NoViableAltException("", 6, 36, input);
													throw nvae;
												} finally {
													input.rewind(nvaeMark);
												}
											}

										}

										else {
											int nvaeMark = input.mark();
											try {
												for (int nvaeConsume = 0; nvaeConsume < 8 - 1; nvaeConsume++) {
													input.consume();
												}
												NoViableAltException nvae =
													new NoViableAltException("", 6, 32, input);
												throw nvae;
											} finally {
												input.rewind(nvaeMark);
											}
										}

									}

									else {
										int nvaeMark = input.mark();
										try {
											for (int nvaeConsume = 0; nvaeConsume < 7 - 1; nvaeConsume++) {
												input.consume();
											}
											NoViableAltException nvae =
												new NoViableAltException("", 6, 28, input);
											throw nvae;
										} finally {
											input.rewind(nvaeMark);
										}
									}

								}

								else {
									int nvaeMark = input.mark();
									try {
										for (int nvaeConsume = 0; nvaeConsume < 6 - 1; nvaeConsume++) {
											input.consume();
										}
										NoViableAltException nvae =
											new NoViableAltException("", 6, 24, input);
										throw nvae;
									} finally {
										input.rewind(nvaeMark);
									}
								}

							}

							else {
								int nvaeMark = input.mark();
								try {
									for (int nvaeConsume = 0; nvaeConsume < 5 - 1; nvaeConsume++) {
										input.consume();
									}
									NoViableAltException nvae =
										new NoViableAltException("", 6, 20, input);
									throw nvae;
								} finally {
									input.rewind(nvaeMark);
								}
							}

						}

						else {
							int nvaeMark = input.mark();
							try {
								for (int nvaeConsume = 0; nvaeConsume < 4 - 1; nvaeConsume++) {
									input.consume();
								}
								NoViableAltException nvae =
									new NoViableAltException("", 6, 16, input);
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
								new NoViableAltException("", 6, 11, input);
							throw nvae;
						} finally {
							input.rewind(nvaeMark);
						}
					}

				}

				else {
					int nvaeMark = input.mark();
					try {
						input.consume();
						NoViableAltException nvae =
							new NoViableAltException("", 6, 4, input);
						throw nvae;
					} finally {
						input.rewind(nvaeMark);
					}
				}

				}
				break;
			case 'n':
				{
				alt6=9;
				}
				break;
			case 'p':
				{
				int LA6_6 = input.LA(2);
				if ( (LA6_6=='a') ) {
					alt6=10;
				}
				else if ( (LA6_6=='r') ) {
					int LA6_13 = input.LA(3);
					if ( (LA6_13=='e') ) {
						int LA6_17 = input.LA(4);
						if ( (LA6_17=='c') ) {
							int LA6_21 = input.LA(5);
							if ( (LA6_21=='e') ) {
								int LA6_25 = input.LA(6);
								if ( (LA6_25=='d') ) {
									int LA6_29 = input.LA(7);
									if ( (LA6_29=='i') ) {
										int LA6_33 = input.LA(8);
										if ( (LA6_33=='n') ) {
											int LA6_37 = input.LA(9);
											if ( (LA6_37=='g') ) {
												int LA6_42 = input.LA(10);
												if ( (LA6_42=='-') ) {
													alt6=12;
												}

												else {
													alt6=11;
												}

											}

											else {
												int nvaeMark = input.mark();
												try {
													for (int nvaeConsume = 0; nvaeConsume < 9 - 1; nvaeConsume++) {
														input.consume();
													}
													NoViableAltException nvae =
														new NoViableAltException("", 6, 37, input);
													throw nvae;
												} finally {
													input.rewind(nvaeMark);
												}
											}

										}

										else {
											int nvaeMark = input.mark();
											try {
												for (int nvaeConsume = 0; nvaeConsume < 8 - 1; nvaeConsume++) {
													input.consume();
												}
												NoViableAltException nvae =
													new NoViableAltException("", 6, 33, input);
												throw nvae;
											} finally {
												input.rewind(nvaeMark);
											}
										}

									}

									else {
										int nvaeMark = input.mark();
										try {
											for (int nvaeConsume = 0; nvaeConsume < 7 - 1; nvaeConsume++) {
												input.consume();
											}
											NoViableAltException nvae =
												new NoViableAltException("", 6, 29, input);
											throw nvae;
										} finally {
											input.rewind(nvaeMark);
										}
									}

								}

								else {
									int nvaeMark = input.mark();
									try {
										for (int nvaeConsume = 0; nvaeConsume < 6 - 1; nvaeConsume++) {
											input.consume();
										}
										NoViableAltException nvae =
											new NoViableAltException("", 6, 25, input);
										throw nvae;
									} finally {
										input.rewind(nvaeMark);
									}
								}

							}

							else {
								int nvaeMark = input.mark();
								try {
									for (int nvaeConsume = 0; nvaeConsume < 5 - 1; nvaeConsume++) {
										input.consume();
									}
									NoViableAltException nvae =
										new NoViableAltException("", 6, 21, input);
									throw nvae;
								} finally {
									input.rewind(nvaeMark);
								}
							}

						}

						else {
							int nvaeMark = input.mark();
							try {
								for (int nvaeConsume = 0; nvaeConsume < 4 - 1; nvaeConsume++) {
									input.consume();
								}
								NoViableAltException nvae =
									new NoViableAltException("", 6, 17, input);
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
								new NoViableAltException("", 6, 13, input);
							throw nvae;
						} finally {
							input.rewind(nvaeMark);
						}
					}

				}

				else {
					int nvaeMark = input.mark();
					try {
						input.consume();
						NoViableAltException nvae =
							new NoViableAltException("", 6, 6, input);
						throw nvae;
					} finally {
						input.rewind(nvaeMark);
					}
				}

				}
				break;
			case 's':
				{
				alt6=13;
				}
				break;
			default:
				NoViableAltException nvae =
					new NoViableAltException("", 6, 0, input);
				throw nvae;
			}
			switch (alt6) {
				case 1 :
					// XPath.g:214:12: 'ancestor'
					{
					match("ancestor"); 

					}
					break;
				case 2 :
					// XPath.g:215:6: 'ancestor-or-self'
					{
					match("ancestor-or-self"); 

					}
					break;
				case 3 :
					// XPath.g:216:6: 'attribute'
					{
					match("attribute"); 

					}
					break;
				case 4 :
					// XPath.g:217:6: 'child'
					{
					match("child"); 

					}
					break;
				case 5 :
					// XPath.g:218:6: 'descendant'
					{
					match("descendant"); 

					}
					break;
				case 6 :
					// XPath.g:219:6: 'descendant-or-self'
					{
					match("descendant-or-self"); 

					}
					break;
				case 7 :
					// XPath.g:220:6: 'following'
					{
					match("following"); 

					}
					break;
				case 8 :
					// XPath.g:221:6: 'following-sibling'
					{
					match("following-sibling"); 

					}
					break;
				case 9 :
					// XPath.g:222:6: 'namespace'
					{
					match("namespace"); 

					}
					break;
				case 10 :
					// XPath.g:223:6: 'parent'
					{
					match("parent"); 

					}
					break;
				case 11 :
					// XPath.g:224:6: 'preceding'
					{
					match("preceding"); 

					}
					break;
				case 12 :
					// XPath.g:225:6: 'preceding-sibling'
					{
					match("preceding-sibling"); 

					}
					break;
				case 13 :
					// XPath.g:226:6: 'self'
					{
					match("self"); 

					}
					break;

			}
			state.type = _type;
			state.channel = _channel;
		}
		finally {
			// do for sure before leaving
		}
	}
	// $ANTLR end "AxisName"

	// $ANTLR start "Literal"
	public final void mLiteral() throws RecognitionException {
		try {
			int _type = Literal;
			int _channel = DEFAULT_TOKEN_CHANNEL;
			// XPath.g:229:10: ( '\"' (~ '\"' )* '\"' | '\\'' (~ '\\'' )* '\\'' )
			int alt9=2;
			int LA9_0 = input.LA(1);
			if ( (LA9_0=='\"') ) {
				alt9=1;
			}
			else if ( (LA9_0=='\'') ) {
				alt9=2;
			}

			else {
				NoViableAltException nvae =
					new NoViableAltException("", 9, 0, input);
				throw nvae;
			}

			switch (alt9) {
				case 1 :
					// XPath.g:229:13: '\"' (~ '\"' )* '\"'
					{
					match('\"'); 
					// XPath.g:229:17: (~ '\"' )*
					loop7:
					while (true) {
						int alt7=2;
						int LA7_0 = input.LA(1);
						if ( ((LA7_0 >= '\u0000' && LA7_0 <= '!')||(LA7_0 >= '#' && LA7_0 <= '\uFFFF')) ) {
							alt7=1;
						}

						switch (alt7) {
						case 1 :
							// XPath.g:
							{
							if ( (input.LA(1) >= '\u0000' && input.LA(1) <= '!')||(input.LA(1) >= '#' && input.LA(1) <= '\uFFFF') ) {
								input.consume();
							}
							else {
								MismatchedSetException mse = new MismatchedSetException(null,input);
								recover(mse);
								throw mse;
							}
							}
							break;

						default :
							break loop7;
						}
					}

					match('\"'); 
					}
					break;
				case 2 :
					// XPath.g:230:6: '\\'' (~ '\\'' )* '\\''
					{
					match('\''); 
					// XPath.g:230:11: (~ '\\'' )*
					loop8:
					while (true) {
						int alt8=2;
						int LA8_0 = input.LA(1);
						if ( ((LA8_0 >= '\u0000' && LA8_0 <= '&')||(LA8_0 >= '(' && LA8_0 <= '\uFFFF')) ) {
							alt8=1;
						}

						switch (alt8) {
						case 1 :
							// XPath.g:
							{
							if ( (input.LA(1) >= '\u0000' && input.LA(1) <= '&')||(input.LA(1) >= '(' && input.LA(1) <= '\uFFFF') ) {
								input.consume();
							}
							else {
								MismatchedSetException mse = new MismatchedSetException(null,input);
								recover(mse);
								throw mse;
							}
							}
							break;

						default :
							break loop8;
						}
					}

					match('\''); 
					}
					break;

			}
			state.type = _type;
			state.channel = _channel;
		}
		finally {
			// do for sure before leaving
		}
	}
	// $ANTLR end "Literal"

	// $ANTLR start "Whitespace"
	public final void mWhitespace() throws RecognitionException {
		try {
			int _type = Whitespace;
			int _channel = DEFAULT_TOKEN_CHANNEL;
			// XPath.g:234:3: ( ( ' ' | '\\t' | '\\n' | '\\r' )+ )
			// XPath.g:234:6: ( ' ' | '\\t' | '\\n' | '\\r' )+
			{
			// XPath.g:234:6: ( ' ' | '\\t' | '\\n' | '\\r' )+
			int cnt10=0;
			loop10:
			while (true) {
				int alt10=2;
				int LA10_0 = input.LA(1);
				if ( ((LA10_0 >= '\t' && LA10_0 <= '\n')||LA10_0=='\r'||LA10_0==' ') ) {
					alt10=1;
				}

				switch (alt10) {
				case 1 :
					// XPath.g:
					{
					if ( (input.LA(1) >= '\t' && input.LA(1) <= '\n')||input.LA(1)=='\r'||input.LA(1)==' ' ) {
						input.consume();
					}
					else {
						MismatchedSetException mse = new MismatchedSetException(null,input);
						recover(mse);
						throw mse;
					}
					}
					break;

				default :
					if ( cnt10 >= 1 ) break loop10;
					EarlyExitException eee = new EarlyExitException(10, input);
					throw eee;
				}
				cnt10++;
			}

			_channel = HIDDEN;
			}

			state.type = _type;
			state.channel = _channel;
		}
		finally {
			// do for sure before leaving
		}
	}
	// $ANTLR end "Whitespace"

	// $ANTLR start "NCName"
	public final void mNCName() throws RecognitionException {
		try {
			int _type = NCName;
			int _channel = DEFAULT_TOKEN_CHANNEL;
			// XPath.g:237:9: ( NCNameStartChar ( NCNameChar )* )
			// XPath.g:237:12: NCNameStartChar ( NCNameChar )*
			{
			mNCNameStartChar(); 

			// XPath.g:237:28: ( NCNameChar )*
			loop11:
			while (true) {
				int alt11=2;
				int LA11_0 = input.LA(1);
				if ( ((LA11_0 >= '-' && LA11_0 <= '.')||(LA11_0 >= '0' && LA11_0 <= '9')||(LA11_0 >= 'A' && LA11_0 <= 'Z')||LA11_0=='_'||(LA11_0 >= 'a' && LA11_0 <= 'z')||LA11_0=='\u00B7'||(LA11_0 >= '\u00C0' && LA11_0 <= '\u00D6')||(LA11_0 >= '\u00D8' && LA11_0 <= '\u00F6')||(LA11_0 >= '\u00F8' && LA11_0 <= '\u037D')||(LA11_0 >= '\u037F' && LA11_0 <= '\u1FFF')||(LA11_0 >= '\u200C' && LA11_0 <= '\u200D')||(LA11_0 >= '\u203F' && LA11_0 <= '\u2040')||(LA11_0 >= '\u2070' && LA11_0 <= '\u218F')||(LA11_0 >= '\u2C00' && LA11_0 <= '\u2FEF')||(LA11_0 >= '\u3001' && LA11_0 <= '\uD7FF')||(LA11_0 >= '\uF900' && LA11_0 <= '\uFDCF')||(LA11_0 >= '\uFDF0' && LA11_0 <= '\uFFFD')) ) {
					alt11=1;
				}

				switch (alt11) {
				case 1 :
					// XPath.g:
					{
					if ( (input.LA(1) >= '-' && input.LA(1) <= '.')||(input.LA(1) >= '0' && input.LA(1) <= '9')||(input.LA(1) >= 'A' && input.LA(1) <= 'Z')||input.LA(1)=='_'||(input.LA(1) >= 'a' && input.LA(1) <= 'z')||input.LA(1)=='\u00B7'||(input.LA(1) >= '\u00C0' && input.LA(1) <= '\u00D6')||(input.LA(1) >= '\u00D8' && input.LA(1) <= '\u00F6')||(input.LA(1) >= '\u00F8' && input.LA(1) <= '\u037D')||(input.LA(1) >= '\u037F' && input.LA(1) <= '\u1FFF')||(input.LA(1) >= '\u200C' && input.LA(1) <= '\u200D')||(input.LA(1) >= '\u203F' && input.LA(1) <= '\u2040')||(input.LA(1) >= '\u2070' && input.LA(1) <= '\u218F')||(input.LA(1) >= '\u2C00' && input.LA(1) <= '\u2FEF')||(input.LA(1) >= '\u3001' && input.LA(1) <= '\uD7FF')||(input.LA(1) >= '\uF900' && input.LA(1) <= '\uFDCF')||(input.LA(1) >= '\uFDF0' && input.LA(1) <= '\uFFFD') ) {
						input.consume();
					}
					else {
						MismatchedSetException mse = new MismatchedSetException(null,input);
						recover(mse);
						throw mse;
					}
					}
					break;

				default :
					break loop11;
				}
			}

			}

			state.type = _type;
			state.channel = _channel;
		}
		finally {
			// do for sure before leaving
		}
	}
	// $ANTLR end "NCName"

	// $ANTLR start "NCNameStartChar"
	public final void mNCNameStartChar() throws RecognitionException {
		try {
			// XPath.g:243:3: ( 'A' .. 'Z' | '_' | 'a' .. 'z' | '\\u00C0' .. '\\u00D6' | '\\u00D8' .. '\\u00F6' | '\\u00F8' .. '\\u02FF' | '\\u0370' .. '\\u037D' | '\\u037F' .. '\\u1FFF' | '\\u200C' .. '\\u200D' | '\\u2070' .. '\\u218F' | '\\u2C00' .. '\\u2FEF' | '\\u3001' .. '\\uD7FF' | '\\uF900' .. '\\uFDCF' | '\\uFDF0' .. '\\uFFFD' )
			// XPath.g:
			{
			if ( (input.LA(1) >= 'A' && input.LA(1) <= 'Z')||input.LA(1)=='_'||(input.LA(1) >= 'a' && input.LA(1) <= 'z')||(input.LA(1) >= '\u00C0' && input.LA(1) <= '\u00D6')||(input.LA(1) >= '\u00D8' && input.LA(1) <= '\u00F6')||(input.LA(1) >= '\u00F8' && input.LA(1) <= '\u02FF')||(input.LA(1) >= '\u0370' && input.LA(1) <= '\u037D')||(input.LA(1) >= '\u037F' && input.LA(1) <= '\u1FFF')||(input.LA(1) >= '\u200C' && input.LA(1) <= '\u200D')||(input.LA(1) >= '\u2070' && input.LA(1) <= '\u218F')||(input.LA(1) >= '\u2C00' && input.LA(1) <= '\u2FEF')||(input.LA(1) >= '\u3001' && input.LA(1) <= '\uD7FF')||(input.LA(1) >= '\uF900' && input.LA(1) <= '\uFDCF')||(input.LA(1) >= '\uFDF0' && input.LA(1) <= '\uFFFD') ) {
				input.consume();
			}
			else {
				MismatchedSetException mse = new MismatchedSetException(null,input);
				recover(mse);
				throw mse;
			}
			}

		}
		finally {
			// do for sure before leaving
		}
	}
	// $ANTLR end "NCNameStartChar"

	// $ANTLR start "NCNameChar"
	public final void mNCNameChar() throws RecognitionException {
		try {
			// XPath.g:264:3: ( NCNameStartChar | '-' | '.' | '0' .. '9' | '\\u00B7' | '\\u0300' .. '\\u036F' | '\\u203F' .. '\\u2040' )
			// XPath.g:
			{
			if ( (input.LA(1) >= '-' && input.LA(1) <= '.')||(input.LA(1) >= '0' && input.LA(1) <= '9')||(input.LA(1) >= 'A' && input.LA(1) <= 'Z')||input.LA(1)=='_'||(input.LA(1) >= 'a' && input.LA(1) <= 'z')||input.LA(1)=='\u00B7'||(input.LA(1) >= '\u00C0' && input.LA(1) <= '\u00D6')||(input.LA(1) >= '\u00D8' && input.LA(1) <= '\u00F6')||(input.LA(1) >= '\u00F8' && input.LA(1) <= '\u037D')||(input.LA(1) >= '\u037F' && input.LA(1) <= '\u1FFF')||(input.LA(1) >= '\u200C' && input.LA(1) <= '\u200D')||(input.LA(1) >= '\u203F' && input.LA(1) <= '\u2040')||(input.LA(1) >= '\u2070' && input.LA(1) <= '\u218F')||(input.LA(1) >= '\u2C00' && input.LA(1) <= '\u2FEF')||(input.LA(1) >= '\u3001' && input.LA(1) <= '\uD7FF')||(input.LA(1) >= '\uF900' && input.LA(1) <= '\uFDCF')||(input.LA(1) >= '\uFDF0' && input.LA(1) <= '\uFFFD') ) {
				input.consume();
			}
			else {
				MismatchedSetException mse = new MismatchedSetException(null,input);
				recover(mse);
				throw mse;
			}
			}

		}
		finally {
			// do for sure before leaving
		}
	}
	// $ANTLR end "NCNameChar"

	@Override
	public void mTokens() throws RecognitionException {
		// XPath.g:1:8: ( ABRPATH | APOS | AT | CC | COLON | COMMA | DOT | DOTDOT | GE | LBRAC | LE | LESS | LPAR | MINUS | MORE | MUL | PATHSEP | PIPE | PLUS | QUOT | RBRAC | RPAR | T__43 | T__44 | T__45 | T__46 | T__47 | T__48 | T__49 | T__50 | NodeType | Number | AxisName | Literal | Whitespace | NCName )
		int alt12=36;
		alt12 = dfa12.predict(input);
		switch (alt12) {
			case 1 :
				// XPath.g:1:10: ABRPATH
				{
				mABRPATH(); 

				}
				break;
			case 2 :
				// XPath.g:1:18: APOS
				{
				mAPOS(); 

				}
				break;
			case 3 :
				// XPath.g:1:23: AT
				{
				mAT(); 

				}
				break;
			case 4 :
				// XPath.g:1:26: CC
				{
				mCC(); 

				}
				break;
			case 5 :
				// XPath.g:1:29: COLON
				{
				mCOLON(); 

				}
				break;
			case 6 :
				// XPath.g:1:35: COMMA
				{
				mCOMMA(); 

				}
				break;
			case 7 :
				// XPath.g:1:41: DOT
				{
				mDOT(); 

				}
				break;
			case 8 :
				// XPath.g:1:45: DOTDOT
				{
				mDOTDOT(); 

				}
				break;
			case 9 :
				// XPath.g:1:52: GE
				{
				mGE(); 

				}
				break;
			case 10 :
				// XPath.g:1:55: LBRAC
				{
				mLBRAC(); 

				}
				break;
			case 11 :
				// XPath.g:1:61: LE
				{
				mLE(); 

				}
				break;
			case 12 :
				// XPath.g:1:64: LESS
				{
				mLESS(); 

				}
				break;
			case 13 :
				// XPath.g:1:69: LPAR
				{
				mLPAR(); 

				}
				break;
			case 14 :
				// XPath.g:1:74: MINUS
				{
				mMINUS(); 

				}
				break;
			case 15 :
				// XPath.g:1:80: MORE
				{
				mMORE(); 

				}
				break;
			case 16 :
				// XPath.g:1:85: MUL
				{
				mMUL(); 

				}
				break;
			case 17 :
				// XPath.g:1:89: PATHSEP
				{
				mPATHSEP(); 

				}
				break;
			case 18 :
				// XPath.g:1:97: PIPE
				{
				mPIPE(); 

				}
				break;
			case 19 :
				// XPath.g:1:102: PLUS
				{
				mPLUS(); 

				}
				break;
			case 20 :
				// XPath.g:1:107: QUOT
				{
				mQUOT(); 

				}
				break;
			case 21 :
				// XPath.g:1:112: RBRAC
				{
				mRBRAC(); 

				}
				break;
			case 22 :
				// XPath.g:1:118: RPAR
				{
				mRPAR(); 

				}
				break;
			case 23 :
				// XPath.g:1:123: T__43
				{
				mT__43(); 

				}
				break;
			case 24 :
				// XPath.g:1:129: T__44
				{
				mT__44(); 

				}
				break;
			case 25 :
				// XPath.g:1:135: T__45
				{
				mT__45(); 

				}
				break;
			case 26 :
				// XPath.g:1:141: T__46
				{
				mT__46(); 

				}
				break;
			case 27 :
				// XPath.g:1:147: T__47
				{
				mT__47(); 

				}
				break;
			case 28 :
				// XPath.g:1:153: T__48
				{
				mT__48(); 

				}
				break;
			case 29 :
				// XPath.g:1:159: T__49
				{
				mT__49(); 

				}
				break;
			case 30 :
				// XPath.g:1:165: T__50
				{
				mT__50(); 

				}
				break;
			case 31 :
				// XPath.g:1:171: NodeType
				{
				mNodeType(); 

				}
				break;
			case 32 :
				// XPath.g:1:180: Number
				{
				mNumber(); 

				}
				break;
			case 33 :
				// XPath.g:1:187: AxisName
				{
				mAxisName(); 

				}
				break;
			case 34 :
				// XPath.g:1:196: Literal
				{
				mLiteral(); 

				}
				break;
			case 35 :
				// XPath.g:1:204: Whitespace
				{
				mWhitespace(); 

				}
				break;
			case 36 :
				// XPath.g:1:215: NCName
				{
				mNCName(); 

				}
				break;

		}
	}


	protected DFA12 dfa12 = new DFA12(this);
	static final String DFA12_eotS =
		"\1\uffff\1\43\1\44\1\uffff\1\47\1\uffff\1\51\1\53\1\uffff\1\55\5\uffff"+
		"\1\56\5\uffff\10\41\1\uffff\2\41\17\uffff\5\41\1\104\11\41\1\117\2\41"+
		"\1\122\1\41\1\124\1\uffff\12\41\1\uffff\2\41\1\uffff\1\41\1\uffff\5\41"+
		"\2\147\2\41\1\152\7\41\1\152\1\uffff\2\41\1\uffff\5\41\1\152\10\41\1\147"+
		"\2\41\1\152\7\41\1\152\2\41\3\152\1\41\1\152\34\41\1\152\6\41\3\152\4"+
		"\41\1\u00b7\1\uffff";
	static final String DFA12_eofS =
		"\u00b8\uffff";
	static final String DFA12_minS =
		"\1\11\1\57\1\0\1\uffff\1\72\1\uffff\1\56\1\75\1\uffff\1\75\5\uffff\1\0"+
		"\5\uffff\1\156\1\145\1\157\1\162\1\141\1\150\1\145\1\141\1\uffff\1\157"+
		"\1\145\17\uffff\1\143\1\164\1\166\1\163\1\144\1\55\1\145\1\162\1\155\1"+
		"\151\1\170\1\144\1\155\2\154\1\55\1\145\1\162\1\55\1\143\1\55\1\uffff"+
		"\2\143\1\145\1\155\1\154\1\164\2\145\1\154\1\146\1\uffff\1\163\1\151\1"+
		"\uffff\1\145\1\uffff\2\145\1\156\1\145\1\144\2\55\1\163\1\157\1\55\1\164"+
		"\1\142\1\156\1\163\1\144\1\164\1\156\1\55\1\uffff\1\160\1\167\1\uffff"+
		"\1\157\1\165\1\144\1\163\1\151\1\55\1\164\1\141\1\151\1\162\1\164\1\141"+
		"\1\151\1\156\1\55\1\143\1\156\1\55\1\145\2\156\1\147\1\145\1\147\1\157"+
		"\1\55\1\164\1\147\3\55\1\162\2\55\2\163\1\55\1\157\3\151\1\163\1\162\1"+
		"\156\2\142\1\145\1\55\1\163\3\154\1\163\1\164\2\151\1\146\1\145\1\162"+
		"\2\156\1\55\1\154\1\165\2\147\1\146\1\143\3\55\1\164\1\151\1\157\1\156"+
		"\1\55\1\uffff";
	static final String DFA12_maxS =
		"\1\ufffd\1\57\1\uffff\1\uffff\1\72\1\uffff\1\71\1\75\1\uffff\1\75\5\uffff"+
		"\1\uffff\5\uffff\1\164\1\151\1\157\2\162\1\157\1\145\1\157\1\uffff\1\157"+
		"\1\145\17\uffff\1\144\1\164\1\166\1\163\1\144\1\ufffd\1\157\1\162\1\155"+
		"\1\151\1\170\1\144\1\155\2\154\1\ufffd\1\145\1\162\1\ufffd\1\143\1\ufffd"+
		"\1\uffff\2\143\1\145\1\155\1\154\1\164\2\145\1\154\1\146\1\uffff\1\163"+
		"\1\151\1\uffff\1\145\1\uffff\2\145\1\156\1\145\1\144\2\ufffd\1\163\1\157"+
		"\1\ufffd\1\164\1\142\1\156\1\163\1\144\1\164\1\156\1\ufffd\1\uffff\1\160"+
		"\1\167\1\uffff\1\157\1\165\1\144\1\163\1\151\1\ufffd\1\164\1\141\1\151"+
		"\1\162\1\164\1\141\1\151\1\156\1\ufffd\1\143\1\156\1\ufffd\1\145\2\156"+
		"\1\147\1\145\1\147\1\157\1\ufffd\1\164\1\147\3\ufffd\1\162\1\ufffd\1\55"+
		"\2\163\1\55\1\157\3\151\1\163\1\162\1\156\2\142\1\145\1\55\1\163\3\154"+
		"\1\163\1\164\2\151\1\146\1\145\1\162\2\156\1\ufffd\1\154\1\165\2\147\1"+
		"\146\1\143\3\ufffd\1\164\1\151\1\157\1\156\1\ufffd\1\uffff";
	static final String DFA12_acceptS =
		"\3\uffff\1\3\1\uffff\1\6\2\uffff\1\12\1\uffff\1\15\1\16\1\20\1\22\1\23"+
		"\1\uffff\1\25\1\26\1\27\1\30\1\31\10\uffff\1\40\2\uffff\1\43\1\44\1\1"+
		"\1\21\1\2\1\42\1\4\1\5\1\10\1\7\1\11\1\17\1\13\1\14\1\24\25\uffff\1\35"+
		"\12\uffff\1\32\2\uffff\1\33\1\uffff\1\34\22\uffff\1\37\2\uffff\1\41\114"+
		"\uffff\1\36";
	static final String DFA12_specialS =
		"\2\uffff\1\0\14\uffff\1\1\u00a8\uffff}>";
	static final String[] DFA12_transitionS = {
			"\2\40\2\uffff\1\40\22\uffff\1\40\1\22\1\17\1\uffff\1\23\2\uffff\1\2\1"+
			"\12\1\21\1\14\1\16\1\5\1\13\1\6\1\1\12\35\1\4\1\uffff\1\11\1\24\1\7\1"+
			"\uffff\1\3\32\41\1\10\1\uffff\1\20\1\uffff\1\41\1\uffff\1\25\1\41\1\32"+
			"\1\26\1\41\1\36\6\41\1\27\1\34\1\30\1\31\2\41\1\37\1\33\6\41\1\uffff"+
			"\1\15\103\uffff\27\41\1\uffff\37\41\1\uffff\u0208\41\160\uffff\16\41"+
			"\1\uffff\u1c81\41\14\uffff\2\41\142\uffff\u0120\41\u0a70\uffff\u03f0"+
			"\41\21\uffff\ua7ff\41\u2100\uffff\u04d0\41\40\uffff\u020e\41",
			"\1\42",
			"\0\45",
			"",
			"\1\46",
			"",
			"\1\50\1\uffff\12\35",
			"\1\52",
			"",
			"\1\54",
			"",
			"",
			"",
			"",
			"",
			"\0\45",
			"",
			"",
			"",
			"",
			"",
			"\1\57\5\uffff\1\60",
			"\1\62\3\uffff\1\61",
			"\1\63",
			"\1\64",
			"\1\66\20\uffff\1\65",
			"\1\70\6\uffff\1\67",
			"\1\71",
			"\1\73\15\uffff\1\72",
			"",
			"\1\74",
			"\1\75",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"\1\77\1\76",
			"\1\100",
			"\1\101",
			"\1\102",
			"\1\103",
			"\2\41\1\uffff\12\41\7\uffff\32\41\4\uffff\1\41\1\uffff\32\41\74\uffff"+
			"\1\41\10\uffff\27\41\1\uffff\37\41\1\uffff\u0286\41\1\uffff\u1c81\41"+
			"\14\uffff\2\41\61\uffff\2\41\57\uffff\u0120\41\u0a70\uffff\u03f0\41\21"+
			"\uffff\ua7ff\41\u2100\uffff\u04d0\41\40\uffff\u020e\41",
			"\1\106\11\uffff\1\105",
			"\1\107",
			"\1\110",
			"\1\111",
			"\1\112",
			"\1\113",
			"\1\114",
			"\1\115",
			"\1\116",
			"\2\41\1\uffff\12\41\7\uffff\32\41\4\uffff\1\41\1\uffff\32\41\74\uffff"+
			"\1\41\10\uffff\27\41\1\uffff\37\41\1\uffff\u0286\41\1\uffff\u1c81\41"+
			"\14\uffff\2\41\61\uffff\2\41\57\uffff\u0120\41\u0a70\uffff\u03f0\41\21"+
			"\uffff\ua7ff\41\u2100\uffff\u04d0\41\40\uffff\u020e\41",
			"\1\120",
			"\1\121",
			"\2\41\1\uffff\12\41\7\uffff\32\41\4\uffff\1\41\1\uffff\32\41\74\uffff"+
			"\1\41\10\uffff\27\41\1\uffff\37\41\1\uffff\u0286\41\1\uffff\u1c81\41"+
			"\14\uffff\2\41\61\uffff\2\41\57\uffff\u0120\41\u0a70\uffff\u03f0\41\21"+
			"\uffff\ua7ff\41\u2100\uffff\u04d0\41\40\uffff\u020e\41",
			"\1\123",
			"\2\41\1\uffff\12\41\7\uffff\32\41\4\uffff\1\41\1\uffff\32\41\74\uffff"+
			"\1\41\10\uffff\27\41\1\uffff\37\41\1\uffff\u0286\41\1\uffff\u1c81\41"+
			"\14\uffff\2\41\61\uffff\2\41\57\uffff\u0120\41\u0a70\uffff\u03f0\41\21"+
			"\uffff\ua7ff\41\u2100\uffff\u04d0\41\40\uffff\u020e\41",
			"",
			"\1\125",
			"\1\126",
			"\1\127",
			"\1\130",
			"\1\131",
			"\1\132",
			"\1\133",
			"\1\134",
			"\1\135",
			"\1\136",
			"",
			"\1\137",
			"\1\140",
			"",
			"\1\141",
			"",
			"\1\142",
			"\1\143",
			"\1\144",
			"\1\145",
			"\1\146",
			"\2\41\1\uffff\12\41\7\uffff\32\41\4\uffff\1\41\1\uffff\32\41\74\uffff"+
			"\1\41\10\uffff\27\41\1\uffff\37\41\1\uffff\u0286\41\1\uffff\u1c81\41"+
			"\14\uffff\2\41\61\uffff\2\41\57\uffff\u0120\41\u0a70\uffff\u03f0\41\21"+
			"\uffff\ua7ff\41\u2100\uffff\u04d0\41\40\uffff\u020e\41",
			"\2\41\1\uffff\12\41\7\uffff\32\41\4\uffff\1\41\1\uffff\32\41\74\uffff"+
			"\1\41\10\uffff\27\41\1\uffff\37\41\1\uffff\u0286\41\1\uffff\u1c81\41"+
			"\14\uffff\2\41\61\uffff\2\41\57\uffff\u0120\41\u0a70\uffff\u03f0\41\21"+
			"\uffff\ua7ff\41\u2100\uffff\u04d0\41\40\uffff\u020e\41",
			"\1\150",
			"\1\151",
			"\2\41\1\uffff\12\41\7\uffff\32\41\4\uffff\1\41\1\uffff\32\41\74\uffff"+
			"\1\41\10\uffff\27\41\1\uffff\37\41\1\uffff\u0286\41\1\uffff\u1c81\41"+
			"\14\uffff\2\41\61\uffff\2\41\57\uffff\u0120\41\u0a70\uffff\u03f0\41\21"+
			"\uffff\ua7ff\41\u2100\uffff\u04d0\41\40\uffff\u020e\41",
			"\1\153",
			"\1\154",
			"\1\155",
			"\1\156",
			"\1\157",
			"\1\160",
			"\1\161",
			"\2\41\1\uffff\12\41\7\uffff\32\41\4\uffff\1\41\1\uffff\32\41\74\uffff"+
			"\1\41\10\uffff\27\41\1\uffff\37\41\1\uffff\u0286\41\1\uffff\u1c81\41"+
			"\14\uffff\2\41\61\uffff\2\41\57\uffff\u0120\41\u0a70\uffff\u03f0\41\21"+
			"\uffff\ua7ff\41\u2100\uffff\u04d0\41\40\uffff\u020e\41",
			"",
			"\1\162",
			"\1\163",
			"",
			"\1\164",
			"\1\165",
			"\1\166",
			"\1\167",
			"\1\170",
			"\2\41\1\uffff\12\41\7\uffff\32\41\4\uffff\1\41\1\uffff\32\41\74\uffff"+
			"\1\41\10\uffff\27\41\1\uffff\37\41\1\uffff\u0286\41\1\uffff\u1c81\41"+
			"\14\uffff\2\41\61\uffff\2\41\57\uffff\u0120\41\u0a70\uffff\u03f0\41\21"+
			"\uffff\ua7ff\41\u2100\uffff\u04d0\41\40\uffff\u020e\41",
			"\1\171",
			"\1\172",
			"\1\173",
			"\1\174",
			"\1\175",
			"\1\176",
			"\1\177",
			"\1\u0080",
			"\2\41\1\uffff\12\41\7\uffff\32\41\4\uffff\1\41\1\uffff\32\41\74\uffff"+
			"\1\41\10\uffff\27\41\1\uffff\37\41\1\uffff\u0286\41\1\uffff\u1c81\41"+
			"\14\uffff\2\41\61\uffff\2\41\57\uffff\u0120\41\u0a70\uffff\u03f0\41\21"+
			"\uffff\ua7ff\41\u2100\uffff\u04d0\41\40\uffff\u020e\41",
			"\1\u0081",
			"\1\u0082",
			"\1\u0083\1\41\1\uffff\12\41\7\uffff\32\41\4\uffff\1\41\1\uffff\32\41"+
			"\74\uffff\1\41\10\uffff\27\41\1\uffff\37\41\1\uffff\u0286\41\1\uffff"+
			"\u1c81\41\14\uffff\2\41\61\uffff\2\41\57\uffff\u0120\41\u0a70\uffff\u03f0"+
			"\41\21\uffff\ua7ff\41\u2100\uffff\u04d0\41\40\uffff\u020e\41",
			"\1\u0084",
			"\1\u0085",
			"\1\u0086",
			"\1\u0087",
			"\1\u0088",
			"\1\u0089",
			"\1\u008a",
			"\2\41\1\uffff\12\41\7\uffff\32\41\4\uffff\1\41\1\uffff\32\41\74\uffff"+
			"\1\41\10\uffff\27\41\1\uffff\37\41\1\uffff\u0286\41\1\uffff\u1c81\41"+
			"\14\uffff\2\41\61\uffff\2\41\57\uffff\u0120\41\u0a70\uffff\u03f0\41\21"+
			"\uffff\ua7ff\41\u2100\uffff\u04d0\41\40\uffff\u020e\41",
			"\1\u008b",
			"\1\u008c",
			"\1\u008d\1\41\1\uffff\12\41\7\uffff\32\41\4\uffff\1\41\1\uffff\32\41"+
			"\74\uffff\1\41\10\uffff\27\41\1\uffff\37\41\1\uffff\u0286\41\1\uffff"+
			"\u1c81\41\14\uffff\2\41\61\uffff\2\41\57\uffff\u0120\41\u0a70\uffff\u03f0"+
			"\41\21\uffff\ua7ff\41\u2100\uffff\u04d0\41\40\uffff\u020e\41",
			"\2\41\1\uffff\12\41\7\uffff\32\41\4\uffff\1\41\1\uffff\32\41\74\uffff"+
			"\1\41\10\uffff\27\41\1\uffff\37\41\1\uffff\u0286\41\1\uffff\u1c81\41"+
			"\14\uffff\2\41\61\uffff\2\41\57\uffff\u0120\41\u0a70\uffff\u03f0\41\21"+
			"\uffff\ua7ff\41\u2100\uffff\u04d0\41\40\uffff\u020e\41",
			"\1\u008e\1\41\1\uffff\12\41\7\uffff\32\41\4\uffff\1\41\1\uffff\32\41"+
			"\74\uffff\1\41\10\uffff\27\41\1\uffff\37\41\1\uffff\u0286\41\1\uffff"+
			"\u1c81\41\14\uffff\2\41\61\uffff\2\41\57\uffff\u0120\41\u0a70\uffff\u03f0"+
			"\41\21\uffff\ua7ff\41\u2100\uffff\u04d0\41\40\uffff\u020e\41",
			"\1\u008f",
			"\1\u0090\1\41\1\uffff\12\41\7\uffff\32\41\4\uffff\1\41\1\uffff\32\41"+
			"\74\uffff\1\41\10\uffff\27\41\1\uffff\37\41\1\uffff\u0286\41\1\uffff"+
			"\u1c81\41\14\uffff\2\41\61\uffff\2\41\57\uffff\u0120\41\u0a70\uffff\u03f0"+
			"\41\21\uffff\ua7ff\41\u2100\uffff\u04d0\41\40\uffff\u020e\41",
			"\1\u0091",
			"\1\u0092",
			"\1\u0093",
			"\1\u0094",
			"\1\u0095",
			"\1\u0096",
			"\1\u0097",
			"\1\u0098",
			"\1\u0099",
			"\1\u009a",
			"\1\u009b",
			"\1\u009c",
			"\1\u009d",
			"\1\u009e",
			"\1\u009f",
			"\1\u00a0",
			"\1\u00a1",
			"\1\u00a2",
			"\1\u00a3",
			"\1\u00a4",
			"\1\u00a5",
			"\1\u00a6",
			"\1\u00a7",
			"\1\u00a8",
			"\1\u00a9",
			"\1\u00aa",
			"\1\u00ab",
			"\1\u00ac",
			"\2\41\1\uffff\12\41\7\uffff\32\41\4\uffff\1\41\1\uffff\32\41\74\uffff"+
			"\1\41\10\uffff\27\41\1\uffff\37\41\1\uffff\u0286\41\1\uffff\u1c81\41"+
			"\14\uffff\2\41\61\uffff\2\41\57\uffff\u0120\41\u0a70\uffff\u03f0\41\21"+
			"\uffff\ua7ff\41\u2100\uffff\u04d0\41\40\uffff\u020e\41",
			"\1\u00ad",
			"\1\u00ae",
			"\1\u00af",
			"\1\u00b0",
			"\1\u00b1",
			"\1\u00b2",
			"\2\41\1\uffff\12\41\7\uffff\32\41\4\uffff\1\41\1\uffff\32\41\74\uffff"+
			"\1\41\10\uffff\27\41\1\uffff\37\41\1\uffff\u0286\41\1\uffff\u1c81\41"+
			"\14\uffff\2\41\61\uffff\2\41\57\uffff\u0120\41\u0a70\uffff\u03f0\41\21"+
			"\uffff\ua7ff\41\u2100\uffff\u04d0\41\40\uffff\u020e\41",
			"\2\41\1\uffff\12\41\7\uffff\32\41\4\uffff\1\41\1\uffff\32\41\74\uffff"+
			"\1\41\10\uffff\27\41\1\uffff\37\41\1\uffff\u0286\41\1\uffff\u1c81\41"+
			"\14\uffff\2\41\61\uffff\2\41\57\uffff\u0120\41\u0a70\uffff\u03f0\41\21"+
			"\uffff\ua7ff\41\u2100\uffff\u04d0\41\40\uffff\u020e\41",
			"\2\41\1\uffff\12\41\7\uffff\32\41\4\uffff\1\41\1\uffff\32\41\74\uffff"+
			"\1\41\10\uffff\27\41\1\uffff\37\41\1\uffff\u0286\41\1\uffff\u1c81\41"+
			"\14\uffff\2\41\61\uffff\2\41\57\uffff\u0120\41\u0a70\uffff\u03f0\41\21"+
			"\uffff\ua7ff\41\u2100\uffff\u04d0\41\40\uffff\u020e\41",
			"\1\u00b3",
			"\1\u00b4",
			"\1\u00b5",
			"\1\u00b6",
			"\2\41\1\uffff\12\41\7\uffff\32\41\4\uffff\1\41\1\uffff\32\41\74\uffff"+
			"\1\41\10\uffff\27\41\1\uffff\37\41\1\uffff\u0286\41\1\uffff\u1c81\41"+
			"\14\uffff\2\41\61\uffff\2\41\57\uffff\u0120\41\u0a70\uffff\u03f0\41\21"+
			"\uffff\ua7ff\41\u2100\uffff\u04d0\41\40\uffff\u020e\41",
			""
	};

	static final short[] DFA12_eot = DFA.unpackEncodedString(DFA12_eotS);
	static final short[] DFA12_eof = DFA.unpackEncodedString(DFA12_eofS);
	static final char[] DFA12_min = DFA.unpackEncodedStringToUnsignedChars(DFA12_minS);
	static final char[] DFA12_max = DFA.unpackEncodedStringToUnsignedChars(DFA12_maxS);
	static final short[] DFA12_accept = DFA.unpackEncodedString(DFA12_acceptS);
	static final short[] DFA12_special = DFA.unpackEncodedString(DFA12_specialS);
	static final short[][] DFA12_transition;

	static {
		int numStates = DFA12_transitionS.length;
		DFA12_transition = new short[numStates][];
		for (int i=0; i<numStates; i++) {
			DFA12_transition[i] = DFA.unpackEncodedString(DFA12_transitionS[i]);
		}
	}

	protected class DFA12 extends DFA {

		public DFA12(BaseRecognizer recognizer) {
			this.recognizer = recognizer;
			this.decisionNumber = 12;
			this.eot = DFA12_eot;
			this.eof = DFA12_eof;
			this.min = DFA12_min;
			this.max = DFA12_max;
			this.accept = DFA12_accept;
			this.special = DFA12_special;
			this.transition = DFA12_transition;
		}
		@Override
		public String getDescription() {
			return "1:1: Tokens : ( ABRPATH | APOS | AT | CC | COLON | COMMA | DOT | DOTDOT | GE | LBRAC | LE | LESS | LPAR | MINUS | MORE | MUL | PATHSEP | PIPE | PLUS | QUOT | RBRAC | RPAR | T__43 | T__44 | T__45 | T__46 | T__47 | T__48 | T__49 | T__50 | NodeType | Number | AxisName | Literal | Whitespace | NCName );";
		}
		@Override
		public int specialStateTransition(int s, IntStream _input) throws NoViableAltException {
			IntStream input = _input;
			int _s = s;
			switch ( s ) {
					case 0 : 
						int LA12_2 = input.LA(1);
						s = -1;
						if ( ((LA12_2 >= '\u0000' && LA12_2 <= '\uFFFF')) ) {s = 37;}
						else s = 36;
						if ( s>=0 ) return s;
						break;

					case 1 : 
						int LA12_15 = input.LA(1);
						s = -1;
						if ( ((LA12_15 >= '\u0000' && LA12_15 <= '\uFFFF')) ) {s = 37;}
						else s = 46;
						if ( s>=0 ) return s;
						break;
			}
			NoViableAltException nvae =
				new NoViableAltException(getDescription(), 12, _s, input);
			error(nvae);
			throw nvae;
		}
	}

}
