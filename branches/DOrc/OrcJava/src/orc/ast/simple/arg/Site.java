package orc.ast.simple.arg;

import orc.runtime.values.Value;


/**
 * Program constants, which occur in argument position. 
 * 
 * @author dkitchin
 *
 */

public class Site extends Argument {
	private static final long serialVersionUID = 1L;
	public orc.runtime.sites.RemoteSite site;
	
	public Site(orc.runtime.sites.RemoteSite site)
	{
		this.site = site;
	}
	
	public Value asValue()
	{
		return new orc.runtime.values.Site(site);
	}
	public String toString() {
		return super.toString() + "(" + site.getClass().getName() +")";
	}
}