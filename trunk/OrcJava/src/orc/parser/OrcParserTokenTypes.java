// $ANTLR 2.7.4: "OrcParser.g" -> "OrcLexer.java"$

	package orc.parser;
		
	import java.util.*;
	import java.io.FileInputStream;
	import java.io.FileNotFoundException;
	import orc.ast.*;

public interface OrcParserTokenTypes {
	int EOF = 1;
	int NULL_TREE_LOOKAHEAD = 3;
	int LITERAL_import = 4;
	int STRING = 5;
	int LITERAL_def = 6;
	int NAME = 7;
	int EQ = 8;
	int LITERAL_webservice = 9;
	int LPAREN = 10;
	int COMMA = 11;
	int RPAREN = 12;
	int LITERAL_where = 13;
	int SEMI = 14;
	int LITERAL_in = 15;
	int PAR = 16;
	int SEQ = 17;
	int SEQPUB = 18;
	int LBRACE = 19;
	int RBRACE = 20;
	int INT = 21;
	int SL_COMMENT = 22;
	int BEGIN_COMMENT = 23;
	int END_COMMENT = 24;
	int MULTI_LINE_COMMENT = 25;
	int ESCAPE = 26;
	int ALPHA = 27;
	int DIGIT = 28;
	int SEQ_OR_PUB = 29;
	int WS = 30;
}
