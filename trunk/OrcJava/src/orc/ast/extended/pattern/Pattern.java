package orc.ast.extended.pattern;

import java.util.LinkedList;
import java.util.List;

import orc.ast.simple.Call;
import orc.ast.simple.Expression;
import orc.ast.simple.Let;
import orc.ast.simple.Parallel;
import orc.ast.simple.Sequential;
import orc.ast.simple.Silent;
import orc.ast.simple.Where;
import orc.ast.simple.arg.Argument;
import orc.ast.simple.arg.Site;
import orc.ast.simple.arg.Var;


/**
 * 
 * Base interface for the abstract syntax of patterns.
 * 
 * Patterns exist only in the extended abstract syntax. They desugar into a series of operations
 * which terminate in variable bindings.
 * 
 * @author dkitchin
 * 
 */

public abstract class Pattern {

	
	/* Sites often used in pattern matching */
	protected static Argument IF = new Site(orc.ast.sites.Site.IF);
	protected static Argument NOT = new Site(orc.ast.sites.Site.NOT);
	
	protected static Argument SOME = new Site(orc.ast.sites.Site.SOME);
	protected static Argument NONE = new Site(orc.ast.sites.Site.NONE);
	
	public static Argument ISSOME = new Site(orc.ast.sites.Site.ISSOME);
	public static Argument ISNONE = new Site(orc.ast.sites.Site.ISNONE);
	
	protected static Argument CONS = new Site(orc.ast.sites.Site.CONS);
	
	protected static Argument ISCONS = new Site(orc.ast.sites.Site.ISCONS);
	protected static Argument ISNIL = new Site(orc.ast.sites.Site.ISNIL);
	
	protected static Argument HEAD = new Site(orc.ast.sites.Site.HEAD);
	protected static Argument TAIL = new Site(orc.ast.sites.Site.TAIL);

	protected static Argument EQUAL = new Site(orc.ast.sites.Site.EQUAL);
		
	/**
	 * @param f The source expression for values to be matched
	 * @return A new expression publishing, for each publication !v of f,
	 * <pre>
	 * some(v')	if p(v) => v' 
	 * none	  	if p(v) => _|_
	 * </pre>
	 */
	public abstract Expression match(Var u);
	public abstract Expression bind(Var u, Expression g);
	
	public Expression match(Expression f) {
		Var t = new Var();
		return new Sequential(f, match(t), t);
	}
	
	public Expression bind(Expression f, Expression g) {
		Var t = new Var();
		return new Where(bind(t, g), f, t);
	}
	
	/* Patterns are assumed to be strict unless set otherwise */
	public boolean strict() {
		return true;
	}
	
	
	/* Create a normalized if-then-else expression from three normalized expressions */
	public static Expression ifexp(Expression test, Expression tc, Expression fc) {
		
		// b
		Var b = new Var();
		
		// if(b)
		Expression ifb = new Call(IF,b);
			
		// if(~b)
		Var tb = new Var();
		Expression ifnotb = new Where(new Call(IF,tb), new Call(NOT,b), tb);
		
		// if(b) >> tc
		Expression tbranch = new Sequential(ifb, tc, new Var());
		
		// if(~b) >> fc
		Expression fbranch = new Sequential(ifnotb, fc, new Var());
		
		// if(b) >> tc | if(~b) >> fc
		Expression body = new Parallel(tbranch, fbranch);
		
		// ( ... ) <b< test
		return new Where(body, test, b);
	}
	
	
	/** 
	 * Lifted application of a site to a list of optional arguments. If every argument
	 * evaluates to some(vi), then the result is some(C(v1,...,vn)). If any argument
	 * evaluates to none, the result is none.
	 * 
	 * lift(e1...en) = 
	 *   ( some(s1, ... , sn) 
	 *     .. <si< isSome(ti) .. )
	 * | ( z >> none() 
	 *     <z< isNone(t1) | ... | isNone(tn) ) 
	 *     
	 *     .. <ti< ei
	 *     
	 * If the argument passed for C is null, it is considered the identity site, and ignored.
	 * This simply produces a lifted tuple.    
	 *     
	 **/
	public static Expression lift(List<Expression> es) {
		
		List<Argument> sargs = new LinkedList<Argument>();
		List<Argument> targs = new LinkedList<Argument>();
		
				
		for (Expression e : es) {
			sargs.add(new Var());  // populate si
			targs.add(new Var());  // populate ti
		}
		
		// some(s1, ... , sn)
		Expression sbranch = new Call(SOME, sargs);
		
		// ... <si< isSome(ti)
		for(int i = 0; i < sargs.size(); i++) {
			Argument si = sargs.get(i);
			Argument ti = targs.get(i);
			sbranch = new Where(sbranch, new Call(ISSOME, ti), (Var)si);
		}
		
		// isNone(t1) | ... | isNone(tn)
		Expression ntest = new Silent();
		for(Argument t : targs) {
			ntest = new Parallel(new Call(ISNONE, t), ntest);
		}

		// z >> none() <z< ...
		Var z = new Var();
		Expression nbranch = new Where(new Sequential(new Let(z), new Call(NONE), new Var()), ntest, z);
		
		
		// some...   | z >> none...
		Expression body = new Parallel(sbranch, nbranch);
		
		// ... <ti< ei
		for(int i = 0; i < targs.size(); i++) {
			body = new Where(body, es.get(i), (Var)targs.get(i));
		}
		
		return body;
	}
	
	/**
	 * Create an expression computing a monadic bind for options. 
	 * I'd really rather be using Haskell here.
	 * 
	 * <p><code>opbind(f,t,g) = f >s> ( (isSome(s) >t> g) >u> some(u) | isNone(s) >> none )</code>
	 */
	public static Expression opbind(Expression f, Var t, Expression g) {
		
		Var s = new Var();
		Var u = new Var();
		
		Expression sbranch = new Sequential(new Sequential(new Call(ISSOME, s), g, t), new Call(SOME, u), u);
		Expression nbranch = new Sequential(new Call(ISNONE, s), new Call(NONE), new Var());
		Expression body = new Parallel(sbranch, nbranch);
		
		return new Sequential(f, body, s);
	}
	
	/**
	 * Filter an expression by piping its publications through isSome.
	 * Values some(v) will publish v, values none will be ignored.
	 * @param e
	 */
	public static Expression filter(Expression e) {
		
		Var u = new Var();
		return new Sequential(e, new Call(ISSOME, u), u); 
	}
	
}
