package orc.ast.extended.pattern;

import java.util.List;

import orc.ast.simple.Call;
import orc.ast.simple.Expression;
import orc.ast.simple.Parallel;
import orc.ast.simple.Sequential;
import orc.ast.simple.Where;
import orc.ast.simple.arg.Argument;
import orc.ast.simple.arg.Field;
import orc.ast.simple.arg.NamedVar;
import orc.ast.simple.arg.Var;

public class CallPattern extends Pattern {

	Argument site;
	Pattern p;
	
	// Create a call based on a string name
	public CallPattern(String site, List<Pattern> args) {
		this.site = new NamedVar(site);
		this.p = new TuplePattern(args);
	}
	
	// Create a call based on a direct pattern
	public CallPattern(String site, Pattern argpat) {
		this.site = new NamedVar(site);
		this.p = argpat;
	}

	public Expression bind(Var u, Expression g) {
		
		return p.bind(u,g);
	}

	public Expression match(Var u) {
		
		// m(u) <m< M.?
		Var m = new Var();
		Expression invertExpr = new Where(new Call(m, u), new Call(site, new Field("?")), m);
		
		
		// isSome(r) >s> p.match s
		Var r = new Var();
		Var s = new Var();
		Expression sbranch = new Sequential(new Call(Pattern.ISSOME, r), p.match(s), s);
		
		// isNone(r) >> none
		Expression nbranch = new Sequential(new Call(Pattern.ISNONE, r), new Call(Pattern.NONE), new Var());
		
		// isSome... | isNone...
		Expression body = new Parallel(sbranch, nbranch);
		
		// ... <r< m(u) <m< M.?
		body = new Where(body, invertExpr, r);
		
		return body;
	}

}
