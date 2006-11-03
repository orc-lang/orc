/*
 * Copyright 2005, The University of Texas at Austin. All rights reserved.
 */

header {
	package orc.parser;
		
	import java.util.*;
	import java.io.FileInputStream;
	import java.io.FileNotFoundException;
	import orc.ast.*;
} 

class OrcParser extends Parser;

startRule returns [OrcProcess n = null]
	: n=expr
    ;

expr returns [OrcProcess n = null]
	: n=def
	| n=where_expr
	| n=import_expr
	| n=webservice
	;

import_expr returns [OrcProcess n=null]
	{
 		OrcProcess m;
 	}
	: "import" sl:STRING m=expr
		{
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
	;


def returns [OrcProcess n = null]
	{
		OrcProcess body, rest;
		List<String> formals;
	}
	: "def" name:NAME formals=formals_list EQ body=expr rest=expr
		{ n = new Define(name.getText(), formals, body, rest); }
	;

webservice returns [OrcProcess n = null]
	{
	}
	: "webservice" name:NAME pluginClassName:STRING propertiesFilename:STRING
		{ n = new WebService(name.getText(),pluginClassName.getText(),propertiesFilename.getText());}
	;
	
formals_list returns [List<String> formals = new ArrayList<String>() ]
	: ( LPAREN n:NAME 
			{ formals.add(n.getText()); }
		( COMMA n2:NAME
			{ formals.add(n2.getText()); }
		)* RPAREN 
	  )? 
	;

where_expr returns [OrcProcess n = null]
	: n=par_expr ( 
		"where" 
			{ AsymmetricParallelComposition
				an = new AsymmetricParallelComposition(n);
				n = an; }
			binding_list[an]
		)?
	;

binding_list[AsymmetricParallelComposition n]
	: binding[n] ( SEMI binding[n] )*
	;

binding[AsymmetricParallelComposition n]
	{
		OrcProcess expr;
	}
	: name:NAME "in" expr=par_expr 
		{ n.addBinding(name.getText(), expr); }
	;

par_expr returns [OrcProcess n = null]
	{
		OrcProcess n2;
	}
	: n=seq_expr ( 
		PAR n2=seq_expr 
			{ n = new ParallelComposition(n, n2); }
		)*
	;

seq_expr returns [OrcProcess n = null]
	{
		OrcProcess n2;
	}
	: n=basic_expr[false] ( 
		var:SEQ n2=seq_expr 
			{ n = new SequentialComposition(n, var.getText(), false, n2); }
		| var2:SEQPUB n2=seq_expr 
			{ n = new SequentialComposition(n, var2.getText(), true, n2); }
		)?
	;

basic_expr[boolean asParam] returns [OrcProcess n = null]
	{
		List<OrcProcess> args = null;
		OrcProcess p;
	}
	: LBRACE n=expr RBRACE
	| name:NAME ( LPAREN 
				{ args = new ArrayList<OrcProcess>(); }
			p=basic_expr[true]
				{ args.add(p); }
			( COMMA p=basic_expr[true] { args.add(p); })* 
			RPAREN )?
		{ if (asParam && args == null)
			n = new Variable(name.getText());
		  else
		     n = new Call(name.getText(), args); }
	| num:INT
		{ n = new Literal(new Long(num.getText())); }
	| str:STRING
		{ n = new Literal(str.getText()); }
	;
	

class OrcLexer extends Lexer;

options {
    charVocabulary = '\3'..'\177';
    k = 2;
}

SL_COMMENT: 
	"--" (~'\n')* '\n'
     { newline(); $setType(Token.SKIP); }
     ;


protected
BEGIN_COMMENT: "/*" | "{-" ;
protected
END_COMMENT: "*/" | "-}" ;
     	

MULTI_LINE_COMMENT:
	BEGIN_COMMENT ( options {greedy=false;} :'\n' {newline();} | ~'\n')* END_COMMENT
    {$setType(Token.SKIP);}
;


// one-or-more letters followed by a newline
NAME :   ALPHA ( ALPHA | DIGIT )*
    ;

INT : ( DIGIT )+;

STRING: '"'! ( ESCAPE | ~('"'|'\\') )* '"'!;

protected
ESCAPE
    :    '\\'
         ( 'n' { $setText("\n"); }
         | 'r' { $setText("\r"); }
         | 't' { $setText("\t"); }
         | '"' { $setText("\""); }
         | '\\' { $setText("\\"); }
         )
    ;
    
protected
ALPHA : ( 'a'..'z' | 'A'..'Z' | '_') ;

protected
DIGIT : '0'..'9';

protected
SEQPUB : ">!"! ( NAME )? ">"!  ;

protected
SEQ : ">"! ( NAME )? ">"!  ;

LBRACE : '{';

RBRACE : '}';

SEQ_OR_PUB :
	(SEQ) => SEQ { $setType(SEQ); }
	| (SEQPUB) => SEQPUB { $setType(SEQPUB); }
	;

PAR : '|';
SEMI: ';';
COMMA: ',';
EQ: '=';
LPAREN : '(';
RPAREN : ')';


WS : ( ' ' | '\t' | '\n' { newline(); } | '\r' )+
     { $setType(Token.SKIP); }
   ;

