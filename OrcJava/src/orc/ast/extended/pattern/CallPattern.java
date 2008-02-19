package orc.ast.extended.pattern;

import java.util.List;

import orc.ast.simple.Call;
import orc.ast.simple.Expression;
import orc.ast.simple.Where;
import orc.ast.simple.arg.Argument;
import orc.ast.simple.arg.Field;
import orc.ast.simple.arg.NamedVar;
import orc.ast.simple.arg.Var;

public class CallPattern implements Pattern {

	Argument site;
	Pattern pat;
	
	// Create a call based on a string name
	public CallPattern(String site, List<Pattern> args) {
		this.site = new NamedVar(site);
		this.pat = new TuplePattern(args);
	}
	
	// Create a call based on a site constant
	public CallPattern(orc.runtime.sites.Site site, List<Pattern> args) {
		this.site = new orc.ast.simple.arg.Site(site);
		this.pat = new TuplePattern(args);
	}
	
	// Create a call based on a direct pattern
	public CallPattern(String site, Pattern argpat) {
		this.site = new NamedVar(site);
		this.pat = argpat;
	}

	public Expression bind(Expression g, Var t) {
		
		return pat.bind(g,t);
	}

	public Expression match(Expression f) {
		
		// t
		Var fResult = new Var();
		
		// M.match
		Expression siteExpr = new Call(site, new Field("match"));
		
		// m(t) <m< M.match
		Var siteResult = new Var();
		Expression filterExpr = new Where(new Call(siteResult, fResult), siteExpr, siteResult);
		
		// match p ( m(t) <m< M.match )
		Expression matchExpr = pat.match(filterExpr);
		
		// f >t> ( match p ( m(t) <m< M.match ) ) 
		return new orc.ast.simple.Sequential(f, matchExpr, fResult);
	}

	public boolean strict() {
		return true;
	}

}
