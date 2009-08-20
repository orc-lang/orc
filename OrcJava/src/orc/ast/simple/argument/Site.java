package orc.ast.simple.argument;


import orc.ast.oil.expression.argument.Arg;
import orc.env.Env;
import orc.runtime.values.Value;


/**
 * Site values, which occur in argument position. 
 * 
 * @author dkitchin
 *
 */

public class Site extends Argument {

	public orc.ast.sites.Site site;
	
	public Site(orc.ast.sites.Site site)
	{
		this.site = site;
	}
	
	@Override
	public Arg convert(Env<Var> vars) {
		
		return new orc.ast.oil.expression.argument.Site(site);
	}

	
}
