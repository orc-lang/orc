// $ANTLR : "OrcParser.g" -> "OrcLexer.java"$

	package orc.parser;
		
	import java.util.*;
	import java.io.FileInputStream;
	import java.io.FileNotFoundException;
	import orc.ast.*;

public interface OrcParserTokenTypes {
	int EOF = 1;
	int NULL_TREE_LOOKAHEAD = 3;
	int LITERAL_site = 4;
	int EQ = 5;
	int LITERAL_class = 6;
	int NAME = 7;
	int DOT = 8;
	int STRING = 9;
	int AMPERSAND = 10;
	int LITERAL_def = 11;
	int LPAREN = 12;
	int COMMA = 13;
	int RPAREN = 14;
	int LITERAL_where = 15;
	int SEMI = 16;
	int LITERAL_in = 17;
	int PAR = 18;
	int SEQ = 19;
	int SEQPUB = 20;
	int INT = 21;
	int LITERAL_true = 22;
	int LITERAL_false = 23;
	int NOT = 24;
	int MINUS = 25;
	int TIMES = 26;
	int DIV = 27;
	int MOD = 28;
	int PLUS = 29;
	int NOT_EQ = 30;
	int GT = 31;
	int GTE = 32;
	int LT = 33;
	int LTE = 34;
	int AND = 35;
	int OR = 36;
	int SL_COMMENT = 37;
	int BEGIN_COMMENT = 38;
	int END_COMMENT = 39;
	int ML_COMMENT = 40;
	int ESCAPE = 41;
	int ALPHA = 42;
	int DIGIT = 43;
	int LBRACE = 44;
	int RBRACE = 45;
	int SEQ_OR_PUB = 46;
	int NILCASE = 47;
	int ARROW = 48;
	int LBRACKET = 49;
	int RBRACKET = 50;
	int WS = 51;
}
