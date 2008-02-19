/*
 * Copyright 2005, The University of Texas at Austin. All rights reserved.
 */

header {
	package orc.parser;
		
	import java.util.*;
	import java.io.File;
	import java.io.FileInputStream;
	import java.io.FileNotFoundException;
	import orc.ast.extended.*;
	import orc.ast.extended.pattern.*;
	
} 

class OrcParser extends Parser;
options {
    k = 2;
}


startRule returns [Expression e = null]
	: e=expr EOF
    ;

decRule returns [List<Declaration> decList = null]
	: decList=decls EOF
	;

expr returns [Expression e = null]
	{
		List<Declaration> decList=null;
	}
	: decList=decls e=where_expr
	  { for (Declaration d : decList) 
	  		e=new Declare(d,e); }
	;

decls returns [List<Declaration> decList = new ArrayList<Declaration>()]
	{
		Declaration d;
		List<Declaration> dl;	
	}
	: (d=decl { decList.add(d); } | dl=include { decList.addAll(dl); })* 
	;

include returns [List<Declaration> decList = null]
	: "include" s:STRING
		{
			try
			{
				File f = new File(s.getText());	
				OrcLexer flexer = new OrcLexer(new FileInputStream(f));
				OrcParser fparser = new OrcParser(flexer);
				decList = fparser.decRule();
			}
			catch (Exception e) { throw new Error("Error including file: " + s); }
		}
	;	

decl returns [Declaration d=null] 
	: d=decl_site 
	| d=decl_class
	| d=decl_defs
	;
	
decl_site returns [Declaration d=null]
	{
		String cname;
		String v;
	}
	: "site" v=ident_name EQ cname=class_name
		{ d = new SiteDeclaration(v,cname); }
	;
	
decl_class returns [Declaration d=null]
	{
		String cname;
		String v;
	}
	: "class" v=ident_name EQ cname=class_name
		{ d = new ClassDeclaration(v,cname); }
	;

class_name returns [String s=""]
	: a:NAME {s=s+a.getText();} (DOT b:NAME {s=s+"."+b.getText();})*;

ident_name returns [String s=""]
	: a:NAME {s=a.getText();} | b:STRING {s=b.getText();}
	;	

decl_defs returns [Declaration d=null]
	{
		List<Definition> defs;
	}
	: defs=defn_group { d=new DefsDeclaration(defs); }
	;

defn_group returns [List<Definition> defs = new ArrayList<Definition>()]
	{
		Definition def;
	}
	: def=defn {defs.add(def);}
	  (options {greedy = true;} : def=defn {defs.add(def);} )?
	;


defn returns [Definition def=null]
	{
		Expression body;
		List<String> formals;
	}	
	: "def" name:NAME formals=formals_list EQ body=expr
	   {def = new Definition(name.getText(), formals, body); }
	;


formals_list returns [List<String> formals = new ArrayList<String>() ]
	: ( LPAREN n:NAME 
			{ formals.add(n.getText()); }
		( COMMA n2:NAME
			{ formals.add(n2.getText()); }
		)* RPAREN 
	  )? 
	;

where_expr returns [Expression e = null]
	{
		Expression e2;
		Pattern p;	
	}
	: e=par_expr (
	  options {greedy = true;} 
	    : LANGLE p=pattern LANGLE e2=par_expr 
			{ e = new Where(e,e2,p); }
		| LANGLE LANGLE e2=par_expr
			{ e = new Where(e,e2,new WildcardPattern()); }		
		)*
	;

par_expr returns [Expression e = null]
	{
		Expression e2;
	}
	: e=seq_expr (
	  options {greedy = true;} : 
		PAR e2=seq_expr 
			{ e = new Parallel(e, e2); }
		)*
	;

seq_expr returns [Expression e = null]
	{
		Expression e2;
		Pattern p;
	}
	: e = op_expr (
	   options {greedy = true;} 
	   		: RANGLE p=pattern RANGLE e2=seq_expr 
				{ e = new Sequential(e, e2, p); }
			| RANGLE RANGLE e2=seq_expr
				{ e = new Sequential(e, e2, new WildcardPattern()); }
		)?
	;


op_expr returns [Expression n = null]
  : {Expression p = null; List<Expression> args = null;} 
    n=relationalExpression 
    ( options {greedy = true;} : 
      { args = new ArrayList<Expression>();
   	    args.add(n);
        }
     (AND  {n = new Call("op&&", args);}
      |OR  {n = new Call("op||", args);}
      ) 
      p=relationalExpression {args.add(p);})*
  ;
	
relationalExpression returns [Expression n = null]
  : {Expression p = null; List<Expression> args = null;} 
    n=addingExpression 
    ( options {greedy = true;} : 
      {args = new ArrayList<Expression>();
   	   args.add(n);
       }
     (EQ       {n = new Call("op=", args);}
      |NOT_EQ  {n = new Call("op/=", args);}
      |GT      {n = new Call("op>.", args);}
      |GTE     {n = new Call("op>=", args);}
      |LT      {n = new Call("op<.", args);}
      |LTE      {n = new Call("op<=", args);}
      )
      p=addingExpression {args.add(p);})*
  ;
	
addingExpression returns [Expression n = null]
  : {Expression p = null; List<Expression> args = null; } 
    n=multiplyingExpression 
    (options {greedy = true;}:
     {args = new ArrayList<Expression>();
   	  args.add(n);
      }
    (PLUS   {n = new Call("op+", args);}
     | MINUS {n = new Call("op-", args);}
    ) 
      p=multiplyingExpression {args.add(p);})*
  ;
	
multiplyingExpression returns [Expression n = null]
  : {Expression p = null; Token t = null; List<Expression> args = null;} 
   n=signExpression
   (options {greedy = true;} : 
    {args = new ArrayList<Expression>();
   	  args.add(n);
      }
   (TIMES {n = new Call("op*", args);}
    |DIV  {n = new Call("op/", args);}
    |MOD {n = new Call("op%", args);}
    ) p=signExpression {args.add(p);})*
  ;
  
signExpression returns [Expression n = null]
  : {Expression p = null;
  	 List<Expression> args = new ArrayList<Expression>();}
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
  
booleanNegationExpression returns [Expression n = null]
  : {Expression p = null;
  	 List<Expression> args = new ArrayList<Expression>();}
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


call_expr returns [Expression e = null]
	{
		List<Expression> args = null;
	}
	: e=basic_expr
	  (options {greedy = true;} :
	     args = arguments { e = new Call(e, args); }
	   | DOT f:NAME { e = new Dot(e, f.getText()); }
	  )*
	| "let" args = arguments { e = new Let(args); }
	;
	
arguments returns [List<Expression> args = new ArrayList<Expression>();]
	{Expression p;}
	: LPAREN 
		 (p=expr { args.add(p); }
		    ( COMMA p=expr { args.add(p); })*
		 )?
	  RPAREN 
	;

	
basic_expr returns [Expression n = null]
	{
		List<Expression> args = null;
		Expression p;
	}
	: name:NAME {n = new Name(name.getText());}
	| n=literal_expr			
	| LPAREN p=expr n=tail_expr[p]
	;
	
// ANTLR needs to die in a fire. I should not need to do this conversion by hand. Ever. -dkitchin
tail_expr[Expression a] returns [Expression n = null]
	{
		List<Expression> args;
	}	
	: RPAREN { n = a; }
	| {args = new ArrayList<Expression>(); args.add(a); }
	   (COMMA a=expr {args.add(a);} )+ RPAREN
	  {n = new Tuple(args);}
	;	

literal_expr returns [Literal l = null]
	: num:INT
		{ l = new Literal(new Integer(num.getText())); }
	| str:STRING
		{ l = new Literal(str.getText()); }
	| "true"
		{ l = new Literal(new Boolean(true)); }
	| "false"
		{ l = new Literal(new Boolean(false)); }
	;




pattern returns [Pattern p = null]
	: p=tuple_pattern 
		("as" var:NAME
		{ p = new AsPattern(p,var.getText()); }
		)?
	;

tuple_pattern returns [Pattern p = null]
	{
		List<Pattern> ps = null;
		Pattern q;	
	}
	: 
	  p=cons_pattern
		(options {greedy = true;} :
		 COMMA q=cons_pattern
		 	{ if (ps == null) 
		 		{	 
		 		  ps = new LinkedList<Pattern>();
		 		  ps.add(p);
		 		  p = new TuplePattern(ps);
		 		}
		 	  ps.add(q); 
		 	}
		)*
	; 

cons_pattern returns [Pattern p = null]
	{
		Pattern q;
	}
	: p=basic_pattern 
		(COLON q=cons_pattern 
			{ p = new ConsPattern(p, q); }
		)?
	;
	
	
basic_pattern returns [Pattern p = null]
	{
		Literal l;
		List<Pattern> ps;
		Pattern q;
	}
	: UNDERSCORE
		{ p = new WildcardPattern(); }
	| l=literal_expr
		{ p = new LiteralPattern(l); }
	| BANG q=basic_pattern
		{ p = new PublishPattern(q); }
	| site:NAME 
	  ( options { greedy=true; } :
	    LPAREN q=pattern RPAREN
		{ p = new CallPattern(var.getText(),q); }
	  )
	| var:NAME
		{ p = new VariablePattern(var.getText()); }
	| LBRACKET RBRACKET
		{ p = new NilPattern(); }
	| LPAREN p=pattern RPAREN
	;
	
	
// ANTLR needs to die in a fire. I should not need to do this conversion by hand. Ever. -dkitchin
tail_pattern returns [List<Pattern> ps = new LinkedList<Pattern>();]
	{
		Pattern p; 
	}	
	: (COMMA p=basic_pattern {ps.add(p);} )* RPAREN
	;

call_pattern returns [Expression e = null]
	{
		List<Expression> args = null;
	}
	: e=basic_expr
	  (options {greedy = true;} :
	     args = arguments { e = new Call(e, args); }
	   | DOT f:NAME { e = new Dot(e, f.getText()); }
	  )*
	| "let" args = arguments { e = new Let(args); }
	;
	
arguments_pattern returns [List<Expression> args = new ArrayList<Expression>();]
	{Expression p;}
	: LPAREN 
		 (p=expr { args.add(p); }
		    ( COMMA p=expr { args.add(p); })*
		 )?
	  RPAREN 
	;

	
basic_expr_pattern returns [Expression n = null]
	{
		List<Expression> args = null;
		Expression p;
	}
	: name:NAME {n = new Name(name.getText());}
	| n=literal_expr			
	| LPAREN p=expr n=tail_expr[p]
	;
	
// ANTLR needs to die in a fire. I should not need to do this conversion by hand. Ever. -dkitchin
tail_tuple_pattern[Expression a] returns [Expression n = null]
	{
		List<Expression> args;
	}	
	: RPAREN { n = a; }
	| {args = new ArrayList<Expression>(); args.add(a); }
	   (COMMA a=expr {args.add(a);} )+ RPAREN
	  {n = new Tuple(args);}
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
    
UNDERSCORE : '_';
    
protected
ALPHA : ( 'a'..'z' | 'A'..'Z' | UNDERSCORE) ;

protected
DIGIT : '0'..'9';


/*
protected
SEQ : ">"! ( NAME )? ">"!  ;
*/

LBRACE : '{';
RBRACE : '}';

LANGLE : '<';
RANGLE : '>';

/*
SEQ_OR_PUB :
	(SEQ) => SEQ { $setType(SEQ); }
	| (SEQPUB) => SEQPUB { $setType(SEQPUB); }
	;
	*/
/*	
protected	
NILCASE: LBRACKET RBRACKET ARROW ;
	*/
	
ARROW      : "=>";
PAR        : '|';
SEMI       : ';';
COMMA      : ',';
COLON	   : ':';

BANG	   : '!';

EQ         : '=';
LPAREN     : '(';
RPAREN     : ')';
DOT        : '.'   ;
LBRACKET   : '['   ;
RBRACKET   : ']'   ;
NOT_EQ 	   : "/="  ;
NOT        : "~";

// a hack
LT         : "<."  ; 
GT         : ">."  ;

LTE        : "<="  ;
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

