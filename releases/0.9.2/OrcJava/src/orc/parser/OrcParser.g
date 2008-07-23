/*
 * Copyright 2005, The University of Texas at Austin. All rights reserved.
 */

header {
	package orc.parser;
		
	import java.util.*;
	import java.io.File;
	import java.io.FileInputStream;
	import java.io.FileNotFoundException;
	import java.math.BigInteger;
	import orc.ast.extended.*;
	import orc.ast.extended.declaration.*;
	import orc.ast.extended.pattern.*;
	import orc.error.*;
	import orc.Orc;

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
	: decList=decls e=semi_expr
	  { Collections.reverse(decList);
	  	for (Declaration d : decList) 
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
			try {
				OrcLexer flexer = new OrcLexer(Orc.openInclude(s.getText()));
				OrcParser fparser = new OrcParser(flexer);
				decList = fparser.decRule();
			} catch (FileNotFoundException e) {
				throw new SemanticException(e.getMessage());
			}
		}
	;	

decl returns [Declaration d=null] 
	: d=decl_site 
	| d=decl_class
	| d=decl_defs
	| d=decl_val
	;
	
decl_val returns [Declaration d=null]
	{
		Expression e;
		Pattern p;
	}
	: "val" p=pattern EQ e=expr
		{ d = new ValDeclaration(p,e); }
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
	  (options {greedy = true;} : def=defn {defs.add(def);} )*
	;


defn returns [Definition def=null]
	{
		Expression body;
		List<Pattern> formals;
	}	
	: "def" name:NAME formals=formals_list EQ body=expr
	   { def = new Definition(name.getText(), formals, body); }
	;


formals_list returns [List<Pattern> formals = new ArrayList<Pattern>()]
	{
		Pattern p;
	}
	: ( LPAREN ( p=pattern 
			{ formals.add(p); }
		( COMMA p=pattern
			{ formals.add(p); }
		)* )? RPAREN 
	  )? 
	;

semi_expr returns [Expression e = null]
	{
		Expression e2;
	}
	: e=where_expr (
	  options {greedy = true;} 
	    : SEMI e2=where_expr
			{ e = new Semi(e,e2); }		
		)*
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
	: e = op_expr 
	  (
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
      |GT      {n = new Call("op>", args);}
      |GTE     {n = new Call("op>=", args);}
      |LT      {n = new Call("op<", args);}
      |LTE     {n = new Call("op<=", args);}
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
    (PLUS   {n = new Call("op+", args); }
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
      p=consExpression
       {if (n == null) 
     	   n = p;
     	else 
     	   args.add(p);}
  ;

consExpression returns [Expression e = null]
	{
		Expression e2;
	}
	: e = basic_expr (
	   options {greedy = true;} 
	   		: COLON e2=consExpression 
				{ e = new ConsExpr(e, e2); }
		)?
	;

basic_expr returns [Expression e = null]
	: e=invoke_expr
	| e=lambda_expr
	| e=tuple_expr 
	| e=list_expr 
	| e=silent_expr
	| e=literal
	;


invoke_expr returns [Expression e = null]
	{
		List<Expression> args = null;
		// SourceLocation l = null;
	}
	: n:NAME
	  { e = new Name(n.getText());}
	  (options {greedy = true;} :
	     args = arguments { e = new Call(e, args); } // ((Call)e).setSourceLocation(l); }
	   | DOT f:NAME { e = new Dot(e, f.getText()); }
	  )*
	;
	
arguments returns [List<Expression> args = new ArrayList<Expression>();]
	{
		Expression e;
	}
	: LPAREN 
		 (e=expr { args.add(e); }
		    ( COMMA e=expr { args.add(e); })*
		 )?
	  RPAREN 
	;	


lambda_expr returns [Expression e = null]
	{
		Expression body;
		List<Pattern> formals;
	}	
	: "lambda" formals=formals_list EQ body=expr
	   { e = new Lambda(formals, body); }
	;
	


tuple_expr returns [Expression e = null]
	{
		List<Expression> es = new LinkedList<Expression>();	
	}
	: LPAREN 
		(options {greedy = true; } :
		  e=expr { es.add(e); }
		    (COMMA e=expr { es.add(e); })*
		)?
	  RPAREN
	    {
	    	if (es.size() == 1)
	    		{ e = es.get(0); }
	    	else
	    		{ e = new Let(es); } 
	    }
	; 


list_expr returns [Expression e = null]
	{
		List<Expression> es = new LinkedList<Expression>();
	}
	: LBRACKET
		(e=expr { es.add(e); }
		  (COMMA e=expr { es.add(e); } )*
		)?
	  RBRACKET
	  	{ e = new ListExpr(es); }
	;


silent_expr returns [Expression e = null]
	: "null"
		{ e = new Silent(); }
	;


literal returns [Literal l = null]
	: num:INT
		{ l = new Literal(new BigInteger(num.getText())); }
	| str:STRING
		{ l = new Literal(str.getText()); }
	| TRUE
		{ l = new Literal(true); }
	| FALSE
		{ l = new Literal(false); }
	;



pattern returns [Pattern p = null]
	: p=cons_pattern 
		("as" var:NAME
		{ p = new AsPattern(p,var.getText()); }
		)?
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
		Pattern q;
	}
	: UNDERSCORE
		{ p = new WildcardPattern(); }
	| l=literal
		{ p = new LiteralPattern(l); }
	| BANG q=basic_pattern
		{ p = new PublishPattern(q); }
	| var:NAME 
		{ p = new VariablePattern(var.getText()); } 
	  ( q=tuple_pattern
		{ p = new CallPattern(var.getText(),q); }
	  )?
	| p=list_pattern
	| p=tuple_pattern
	;
	
tuple_pattern returns [Pattern p = null]
	{
		List<Pattern> ps = new LinkedList<Pattern>(); 
	}
	: LPAREN
	    (p=pattern { ps.add(p); }
	     (options {greedy = true;} : COMMA p=pattern { ps.add(p); } )*
		)?
	  RPAREN
	    { if (ps.size() == 1)
	    	{ p = ps.get(0); }
	      else
	      	{ p = new TuplePattern(ps); }
	    }
	;
	
list_pattern returns [Pattern p = null]
	{
		List<Pattern> ps = new LinkedList<Pattern>(); 
	}	
	: LBRACKET
		(p=pattern { ps.add(p); }
		  (options {greedy = true;} : COMMA p=pattern { ps.add(p); } )*
		)?
	  RBRACKET
	  	{ p = new ListPattern(ps); }
	;
	




class OrcLexer extends Lexer;

options {
    charVocabulary = '\3'..'\177';
    k = 4;
}

SL_COMMENT: 
	"--" (~'\n')* '\n'
     { newline(); $setType(Token.SKIP); }
     ;



protected
BEGIN_COMMENT: "{-";
protected
END_COMMENT: "-}";


ML_COMMENT:
	BEGIN_COMMENT ( options {greedy=false;} : ML_COMMENT | '\n' {newline();} | ~'\n')* END_COMMENT
    {$setType(Token.SKIP);}
;




TRUE : "true" ;
FALSE : "false" ;

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

LBRACE : '{';
RBRACE : '}';

LANGLE : '<';
RANGLE : '>';
	
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
LT         : "<:"  ; 
GT         : ":>"  ;

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
ATSIGN     : '@' ;


WS : ( ' ' | '\t' | '\n' { newline(); } | '\r' )+
     { $setType(Token.SKIP); }
   ;

