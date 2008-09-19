package orc.ast.extended.pattern;

import java.util.List;

import orc.ast.simple.Call;
import orc.ast.simple.Expression;
import orc.ast.simple.Parallel;
import orc.ast.simple.Sequential;
import orc.ast.simple.Where;
import orc.ast.simple.WithLocation;
import orc.ast.simple.arg.Argument;
import orc.ast.simple.arg.Field;
import orc.ast.simple.arg.NamedVar;
import orc.ast.simple.arg.Var;
import orc.error.compiletime.PatternException;

public class CallPattern extends Pattern {

	NamedVar site;
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

	@Override
	public void process(Var fragment, PatternSimplifier visitor)
			throws PatternException {
		
		Var result = new Var();
		visitor.assign(result, new WithLocation(
				Pattern.unapply(site, fragment),
				getSourceLocation()));
		visitor.require(result);
		p.process(result, visitor);
	}
	
	public String toString() {
		return site.key + p.toString();
	}

}
