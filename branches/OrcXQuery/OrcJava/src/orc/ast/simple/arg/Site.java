package orc.ast.simple.arg;

import orc.runtime.values.Value;


/**
 * Site values, which occur in argument position. 
 * 
 * @author dkitchin
 *
 */

public class Site extends Argument {

	public orc.runtime.sites.Site site;
	
	public Site(orc.runtime.sites.Site site)
	{
		this.site = site;
	}
	
	public Value asValue()
	{
		return site;
	}
	
}
