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
options {
    k = 2;
}


startRule returns [OrcProcess n = null]
	: n=expr EOF
    ;

expr returns [OrcProcess n = null]
	{
		List<Definition> defs=null;
		OrcProcess p;
	}
	: {defs = new ArrayList<Definition>(); }
	   defs=defn_group p=expr
	   { n=new Define(defs,p); } 
	| n=where_expr
	;

declRule returns [List<EnvBinder> l = new ArrayList<EnvBinder>()]
	{
		EnvBinder b;	
	}
	: (b=decl { l.add(b); })* 
	  EOF
	;

decl returns [EnvBinder b=null] 
	: b=decl_site 
	| b=decl_class
	| b=decl_defs
	;
	
decl_site returns [LoadSite d=null]
	{
		String cname;
		String v;
	}
	: "site" v=ident_name EQ cname=class_name
		{ d = new LoadSite(v,cname); }
	;
	
decl_class returns [LoadClass d=null]
	{
		String cname;
		String v;
	}
	: "class" v=ident_name EQ cname=class_name
		{ d = new LoadClass(v,cname); }
	;

class_name returns [String s=""]
	: a:NAME {s=s+a.getText();} (DOT b:NAME {s=s+"."+b.getText();})*;

ident_name returns [String s=""]
	: a:NAME {s=a.getText();} | b:STRING {s=b.getText();}
	;	

decl_defs returns [LoadDefs d=null]
	{
		List<Definition> defs;
	}
	: defs=defn_group { d=new LoadDefs(defs); }
	;

defn_group returns [List<Definition> defs = new ArrayList<Definition>()]
	{
		Definition d;
	}
	: d=defn {defs.add(d);} (AMPERSAND d=defn {defs.add(d);})*
	;


defn returns [Definition df=null]
	{
		OrcProcess body;
		List<String> formals;
	}	
	: "def" name:NAME formals=formals_list EQ body=expr
	   {df = new Definition(name.getText(), formals, body); }
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
	  options {greedy = true;} : 
		"where" 
			{ AsymmetricParallelComposition
				an = new AsymmetricParallelComposition(n);
				n = an; }
			binding_list[an]
		)?
	;

binding_list[AsymmetricParallelComposition n]
	: binding[n] (options {greedy = true;} : SEMI binding[n] )*
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
	  options {greedy = true;} : 
		PAR n2=seq_expr 
			{ n = new ParallelComposition(n, n2); }
		)*
	;

seq_expr returns [OrcProcess n = null]
	{
		OrcProcess n2;
	}
	: n = expression (
	   options {greedy = true;} : 
		var:SEQ n2=seq_expr 
			{ n = new SequentialComposition(n, var.getText(), false, n2); }
		| var2:SEQPUB n2=seq_expr 
			{ n = new SequentialComposition(n, var2.getText(), true, n2); }
		)?
	;
	

// operators connect from expression to call_expr


call_expr returns [OrcProcess n = null]
	{
		List<OrcProcess> args = null;
		OrcProcess p;
	}
	: n=basic_expr
	  (options {greedy = true;} :
	    args = arguments 
	   { n = new Call(n, args, LT(1)); }
	  )*
	;
	
arguments returns [List<OrcProcess> args = new ArrayList<OrcProcess>();]
	{OrcProcess p;}
	: (DOT method:NAME
	     { args.add(new Literal(method.getText())); }
	     )
	| ( options {greedy = true;} : 
	    LPAREN 
		 (p = expr { args.add(p); }
		    ( COMMA p=expr { args.add(p); })*
		 )?
	    RPAREN 
	  )
	;

	
basic_expr returns [OrcProcess n = null]
	{
		List<OrcProcess> args = null;
		OrcProcess p;
	}
	: name:NAME {n = new Name(name.getText());}
	| num:INT
		{ n = new Literal(new Integer(num.getText())); }
	| str:STRING
		{ n = new Literal(str.getText()); }
	| "true"
		{ n = new Literal(new Boolean(true)); }
	| "false"
		{ n = new Literal(new Boolean(false)); }
				
	| LPAREN p=expr n=tail_expr[p]
	
	// Brace enclosure is needed for closure/thunk syntax
	//| LBRACE n=expr RBRACE
	
	// unit (the empty tuple) is a special case
	| LPAREN RPAREN { n = new Call("let",new ArrayList<OrcProcess>()); }
	;
	
// ANTLR needs to die in a fire. I should not need to do this conversion by hand. Ever. -dkitchin
tail_expr[OrcProcess a] returns [OrcProcess n = null]
	{
		List<OrcProcess> args;
	}	
	: RPAREN { n = a; } 
	| {args = new ArrayList<OrcProcess>(); args.add(a); }
	   (COMMA a=expr {args.add(a);} )+ RPAREN
	  {n = new Call("let",args);}
	;	






// operators

booleanNegationExpression returns [OrcProcess n = null]
  : {OrcProcess p = null;
  	 List<OrcProcess> args = new ArrayList<OrcProcess>();}
     (NOT {if (n == null) 
     	        n = new Call("op~", args);
     	     else 
     	        n = new Call("op~", n);}
      )* 
      p=call_expr
       {if (n == null) 
     	   n = p;
     	else 
     	   args.add(p);}
  ;

signExpression returns [OrcProcess n = null]
  : {OrcProcess p = null;
  	 List<OrcProcess> args = new ArrayList<OrcProcess>();}
     (MINUS {if (n == null) 
     	        n = new Call("opu-", args);
     	     else 
     	        n = new Call("opu-", n);}
     )* 
     p=booleanNegationExpression
     {if (n == null) 
     	   n = p;
     	else 
     	   args.add(p);}
  ;


multiplyingExpression returns [OrcProcess n = null]
  : {OrcProcess p = null; Token t = null; List<OrcProcess> args = null;} 
   n=signExpression
   (options {greedy = true;} : 
    {args = new ArrayList<OrcProcess>();
   	  args.add(n);
      }
   (TIMES {n = new Call("op*", args);}
    |DIV  {n = new Call("op/", args);}
    |MOD {n = new Call("op%", args);}
    ) p=signExpression {args.add(p);})*
  ;

addingExpression returns [OrcProcess n = null]
  : {OrcProcess p = null; List<OrcProcess> args = null; } 
    n=multiplyingExpression 
    (options {greedy = true;}:
     {args = new ArrayList<OrcProcess>();
   	  args.add(n);
      }
    (PLUS   {n = new Call("op+", args);}
     | MINUS {n = new Call("op-", args);}
    ) 
      p=multiplyingExpression {args.add(p);})*
  ;

relationalExpression returns [OrcProcess n = null]
  : {OrcProcess p = null; List<OrcProcess> args = null;} 
    n=addingExpression 
    ( options {greedy = true;} : 
      {args = new ArrayList<OrcProcess>();
   	   args.add(n);
       }
     (EQ       {n = new Call("op=", args);}
      |NOT_EQ  {n = new Call("op/=", args);}
      |GT      {n = new Call("op>", args);}
      |GTE     {n = new Call("op>=", args);}
      |LT      {n = new Call("op<", args);}
      |LTE      {n = new Call("op<=", args);}
      ) 
      p=addingExpression {args.add(p);})*
  ;

expression returns [OrcProcess n = null]
  : {OrcProcess p = null; List<OrcProcess> args = null;} 
    n=relationalExpression 
    ( options {greedy = true;} : 
      { args = new ArrayList<OrcProcess>();
   	    args.add(n);
        }
     (AND  {n = new Call("op&&", args);}
      |OR  {n = new Call("op||", args);}
      ) 
      p=relationalExpression {args.add(p);})*
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
BEGIN_COMMENT: LBRACE '-';
protected
END_COMMENT: '-' RBRACE;




     	
/*
ML_COMMENT:
	LBRACE '-' (options {
        generateAmbigWarnings=false;
      }:  { LA(2)!=RBRACE }? '-'
      | '\n' {newline();}
      | ~('-'|'\n')
    )*
    '-' RBRACE
    {$setType(Token.SKIP);}
;

ML_COMMENT:
	BEGIN_COMMENT ( options {
        generateAmbigWarnings=false;
      }:{LA(2) != '}'}? '-'
	| '\n' {newline();} 
	| ~('-'|'\n') )*
	END_COMMENT
    {$setType(Token.SKIP);}
;
*/


ML_COMMENT:
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
protected	
NILCASE: LBRACKET RBRACKET ARROW ;
	
ARROW : "=>";
PAR : '|';
SEMI: ';';
COMMA: ',';
EQ: '=';
LPAREN : '(';
RPAREN : ')';
DOT        : '.'   ;
LBRACKET   : '['   ;
RBRACKET   : ']'   ;
NOT_EQ : "/="  ;
NOT : "~";
LT         : '<'   ;
LTE        : "<="  ;
GT         : '>'   ;
GTE        : ">="  ;
PLUS       : '+'   ;
MINUS      : '-'   ;
TIMES      : '*'   ;
DIV        : '/'   ;
MOD        : '%'   ;
AND        : "&&"  ;
OR         : "||" ;

AMPERSAND  : '&' ;




WS : ( ' ' | '\t' | '\n' { newline(); } | '\r' )+
     { $setType(Token.SKIP); }
   ;

