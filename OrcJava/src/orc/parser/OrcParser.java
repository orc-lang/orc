// $ANTLR 2.7.4: "OrcParser.g" -> "OrcParser.java"$

	package orc.parser;
		
	import java.util.*;
	import java.io.FileInputStream;
	import java.io.FileNotFoundException;
	import orc.ast.*;

import antlr.TokenBuffer;
import antlr.TokenStreamException;
import antlr.TokenStreamIOException;
import antlr.ANTLRException;
import antlr.LLkParser;
import antlr.Token;
import antlr.TokenStream;
import antlr.RecognitionException;
import antlr.NoViableAltException;
import antlr.MismatchedTokenException;
import antlr.SemanticException;
import antlr.ParserSharedInputState;
import antlr.collections.impl.BitSet;

public class OrcParser extends antlr.LLkParser       implements OrcParserTokenTypes
 {

protected OrcParser(TokenBuffer tokenBuf, int k) {
  super(tokenBuf,k);
  tokenNames = _tokenNames;
}

public OrcParser(TokenBuffer tokenBuf) {
  this(tokenBuf,1);
}

protected OrcParser(TokenStream lexer, int k) {
  super(lexer,k);
  tokenNames = _tokenNames;
}

public OrcParser(TokenStream lexer) {
  this(lexer,1);
}

public OrcParser(ParserSharedInputState state) {
  super(state,1);
  tokenNames = _tokenNames;
}

	public final OrcProcess  startRule() throws RecognitionException, TokenStreamException {
		OrcProcess n = null;
		
		
		try {      // for error handling
			n=expr();
		}
		catch (RecognitionException ex) {
			reportError(ex);
			consume();
			consumeUntil(_tokenSet_0);
		}
		return n;
	}
	
	public final OrcProcess  expr() throws RecognitionException, TokenStreamException {
		OrcProcess n = null;
		
		
		try {      // for error handling
			switch ( LA(1)) {
			case LITERAL_def:
			{
				n=def();
				break;
			}
			case STRING:
			case NAME:
			case LBRACE:
			case INT:
			{
				n=where_expr();
				break;
			}
			case LITERAL_import:
			{
				n=import_expr();
				break;
			}
			case LITERAL_webservice:
			{
				n=webservice();
				break;
			}
			default:
			{
				throw new NoViableAltException(LT(1), getFilename());
			}
			}
		}
		catch (RecognitionException ex) {
			reportError(ex);
			consume();
			consumeUntil(_tokenSet_1);
		}
		return n;
	}
	
	public final OrcProcess  def() throws RecognitionException, TokenStreamException {
		OrcProcess n = null;
		
		Token  name = null;
		
				OrcProcess body, rest;
				List<String> formals;
			
		
		try {      // for error handling
			match(LITERAL_def);
			name = LT(1);
			match(NAME);
			formals=formals_list();
			match(EQ);
			body=expr();
			rest=expr();
			n = new Define(name.getText(), formals, body, rest);
		}
		catch (RecognitionException ex) {
			reportError(ex);
			consume();
			consumeUntil(_tokenSet_1);
		}
		return n;
	}
	
	public final OrcProcess  where_expr() throws RecognitionException, TokenStreamException {
		OrcProcess n = null;
		
		
		try {      // for error handling
			n=par_expr();
			{
			switch ( LA(1)) {
			case LITERAL_where:
			{
				match(LITERAL_where);
				AsymmetricParallelComposition
								an = new AsymmetricParallelComposition(n);
								n = an;
				binding_list(an);
				break;
			}
			case EOF:
			case LITERAL_import:
			case STRING:
			case LITERAL_def:
			case NAME:
			case LITERAL_webservice:
			case LBRACE:
			case RBRACE:
			case INT:
			{
				break;
			}
			default:
			{
				throw new NoViableAltException(LT(1), getFilename());
			}
			}
			}
		}
		catch (RecognitionException ex) {
			reportError(ex);
			consume();
			consumeUntil(_tokenSet_1);
		}
		return n;
	}
	
	public final OrcProcess  import_expr() throws RecognitionException, TokenStreamException {
		OrcProcess n=null;
		
		Token  sl = null;
		
				OrcProcess m;
			
		
		try {      // for error handling
			match(LITERAL_import);
			sl = LT(1);
			match(STRING);
			m=expr();
			
					 OrcLexer lexer=null;
					 try {
									lexer = new OrcLexer(new FileInputStream(sl.getText()));
								} catch (FileNotFoundException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}
					 OrcParser parser = new OrcParser(lexer);
					 n = new Include(parser.startRule(), m);
					
		}
		catch (RecognitionException ex) {
			reportError(ex);
			consume();
			consumeUntil(_tokenSet_1);
		}
		return n;
	}
	
	public final OrcProcess  webservice() throws RecognitionException, TokenStreamException {
		OrcProcess n = null;
		
		Token  name = null;
		Token  pluginClassName = null;
		Token  propertiesFilename = null;
		
			
		
		try {      // for error handling
			match(LITERAL_webservice);
			name = LT(1);
			match(NAME);
			pluginClassName = LT(1);
			match(STRING);
			propertiesFilename = LT(1);
			match(STRING);
			n = new WebService(name.getText(),pluginClassName.getText(),propertiesFilename.getText());
		}
		catch (RecognitionException ex) {
			reportError(ex);
			consume();
			consumeUntil(_tokenSet_1);
		}
		return n;
	}
	
	public final List<String>  formals_list() throws RecognitionException, TokenStreamException {
		List<String> formals = new ArrayList<String>() ;
		
		Token  n = null;
		Token  n2 = null;
		
		try {      // for error handling
			{
			switch ( LA(1)) {
			case LPAREN:
			{
				match(LPAREN);
				n = LT(1);
				match(NAME);
				formals.add(n.getText());
				{
				_loop9:
				do {
					if ((LA(1)==COMMA)) {
						match(COMMA);
						n2 = LT(1);
						match(NAME);
						formals.add(n2.getText());
					}
					else {
						break _loop9;
					}
					
				} while (true);
				}
				match(RPAREN);
				break;
			}
			case EQ:
			{
				break;
			}
			default:
			{
				throw new NoViableAltException(LT(1), getFilename());
			}
			}
			}
		}
		catch (RecognitionException ex) {
			reportError(ex);
			consume();
			consumeUntil(_tokenSet_2);
		}
		return formals;
	}
	
	public final OrcProcess  par_expr() throws RecognitionException, TokenStreamException {
		OrcProcess n = null;
		
		
				OrcProcess n2;
			
		
		try {      // for error handling
			n=seq_expr();
			{
			_loop18:
			do {
				if ((LA(1)==PAR)) {
					match(PAR);
					n2=seq_expr();
					n = new ParallelComposition(n, n2);
				}
				else {
					break _loop18;
				}
				
			} while (true);
			}
		}
		catch (RecognitionException ex) {
			reportError(ex);
			consume();
			consumeUntil(_tokenSet_3);
		}
		return n;
	}
	
	public final void binding_list(
		AsymmetricParallelComposition n
	) throws RecognitionException, TokenStreamException {
		
		
		try {      // for error handling
			binding(n);
			{
			_loop14:
			do {
				if ((LA(1)==SEMI)) {
					match(SEMI);
					binding(n);
				}
				else {
					break _loop14;
				}
				
			} while (true);
			}
		}
		catch (RecognitionException ex) {
			reportError(ex);
			consume();
			consumeUntil(_tokenSet_1);
		}
	}
	
	public final void binding(
		AsymmetricParallelComposition n
	) throws RecognitionException, TokenStreamException {
		
		Token  name = null;
		
				OrcProcess expr;
			
		
		try {      // for error handling
			name = LT(1);
			match(NAME);
			match(LITERAL_in);
			expr=par_expr();
			n.addBinding(name.getText(), expr);
		}
		catch (RecognitionException ex) {
			reportError(ex);
			consume();
			consumeUntil(_tokenSet_4);
		}
	}
	
	public final OrcProcess  seq_expr() throws RecognitionException, TokenStreamException {
		OrcProcess n = null;
		
		Token  var = null;
		Token  var2 = null;
		
				OrcProcess n2;
			
		
		try {      // for error handling
			n=basic_expr(false);
			{
			switch ( LA(1)) {
			case SEQ:
			{
				var = LT(1);
				match(SEQ);
				n2=seq_expr();
				n = new SequentialComposition(n, var.getText(), false, n2);
				break;
			}
			case SEQPUB:
			{
				var2 = LT(1);
				match(SEQPUB);
				n2=seq_expr();
				n = new SequentialComposition(n, var2.getText(), true, n2);
				break;
			}
			case EOF:
			case LITERAL_import:
			case STRING:
			case LITERAL_def:
			case NAME:
			case LITERAL_webservice:
			case LITERAL_where:
			case SEMI:
			case PAR:
			case LBRACE:
			case RBRACE:
			case INT:
			{
				break;
			}
			default:
			{
				throw new NoViableAltException(LT(1), getFilename());
			}
			}
			}
		}
		catch (RecognitionException ex) {
			reportError(ex);
			consume();
			consumeUntil(_tokenSet_5);
		}
		return n;
	}
	
	public final OrcProcess  basic_expr(
		boolean asParam
	) throws RecognitionException, TokenStreamException {
		OrcProcess n = null;
		
		Token  name = null;
		Token  num = null;
		Token  str = null;
		
				List<OrcProcess> args = null;
				OrcProcess p;
			
		
		try {      // for error handling
			switch ( LA(1)) {
			case LBRACE:
			{
				match(LBRACE);
				n=expr();
				match(RBRACE);
				break;
			}
			case NAME:
			{
				name = LT(1);
				match(NAME);
				{
				switch ( LA(1)) {
				case LPAREN:
				{
					match(LPAREN);
					args = new ArrayList<OrcProcess>();
					p=basic_expr(true);
					args.add(p);
					{
					_loop24:
					do {
						if ((LA(1)==COMMA)) {
							match(COMMA);
							p=basic_expr(true);
							args.add(p);
						}
						else {
							break _loop24;
						}
						
					} while (true);
					}
					match(RPAREN);
					break;
				}
				case EOF:
				case LITERAL_import:
				case STRING:
				case LITERAL_def:
				case NAME:
				case LITERAL_webservice:
				case COMMA:
				case RPAREN:
				case LITERAL_where:
				case SEMI:
				case PAR:
				case SEQ:
				case SEQPUB:
				case LBRACE:
				case RBRACE:
				case INT:
				{
					break;
				}
				default:
				{
					throw new NoViableAltException(LT(1), getFilename());
				}
				}
				}
				if (asParam && args == null)
							n = new Variable(name.getText());
						  else
						     n = new Call(name.getText(), args);
				break;
			}
			case INT:
			{
				num = LT(1);
				match(INT);
				n = new Literal(new Integer(num.getText()));
				break;
			}
			case STRING:
			{
				str = LT(1);
				match(STRING);
				n = new Literal(str.getText());
				break;
			}
			default:
			{
				throw new NoViableAltException(LT(1), getFilename());
			}
			}
		}
		catch (RecognitionException ex) {
			reportError(ex);
			consume();
			consumeUntil(_tokenSet_6);
		}
		return n;
	}
	
	
	public static final String[] _tokenNames = {
		"<0>",
		"EOF",
		"<2>",
		"NULL_TREE_LOOKAHEAD",
		"\"import\"",
		"STRING",
		"\"def\"",
		"NAME",
		"EQ",
		"\"webservice\"",
		"LPAREN",
		"COMMA",
		"RPAREN",
		"\"where\"",
		"SEMI",
		"\"in\"",
		"PAR",
		"SEQ",
		"SEQPUB",
		"LBRACE",
		"RBRACE",
		"INT",
		"SL_COMMENT",
		"BEGIN_COMMENT",
		"END_COMMENT",
		"MULTI_LINE_COMMENT",
		"ESCAPE",
		"ALPHA",
		"DIGIT",
		"SEQ_OR_PUB",
		"WS"
	};
	
	private static final long[] mk_tokenSet_0() {
		long[] data = { 2L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_0 = new BitSet(mk_tokenSet_0());
	private static final long[] mk_tokenSet_1() {
		long[] data = { 3670770L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_1 = new BitSet(mk_tokenSet_1());
	private static final long[] mk_tokenSet_2() {
		long[] data = { 256L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_2 = new BitSet(mk_tokenSet_2());
	private static final long[] mk_tokenSet_3() {
		long[] data = { 3695346L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_3 = new BitSet(mk_tokenSet_3());
	private static final long[] mk_tokenSet_4() {
		long[] data = { 3687154L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_4 = new BitSet(mk_tokenSet_4());
	private static final long[] mk_tokenSet_5() {
		long[] data = { 3760882L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_5 = new BitSet(mk_tokenSet_5());
	private static final long[] mk_tokenSet_6() {
		long[] data = { 4160242L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_6 = new BitSet(mk_tokenSet_6());
	
	}
