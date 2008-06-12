package orc.ast.oil.arg;

import orc.env.Env;
import orc.runtime.values.Future;
import orc.runtime.values.Value;


/**
 * Program constants, which occur in argument position. 
 * 
 * @author dkitchin
 *
 */

public class Site extends Arg {

	public orc.runtime.sites.Site site;
	
	public Site(orc.runtime.sites.Site site)
	{
		this.site = site;
	}
	
	@Override
	public Future resolve(Env env) {
		return site;
	}
	
	public String toString() {
		return "[" + site.getClass().toString() + "]";
	}

}
