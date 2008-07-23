package orc.ast.oil.arg;

import java.net.URI;
import java.net.URISyntaxException;

import orc.ast.oil.Visitor;
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

	public orc.ast.sites.Site site;
	
	public Site(orc.ast.sites.Site site)
	{
		this.site = site;
	}
	
	@Override
	public Future resolve(Env env) {
		/* TODO: Make this more efficient. 
		 * Even though sites are semantically persistent, it's 
		 * unhelpful to have many copies of the same object.
		 */
		return site.instantiate();
	}
	
	public String toString() {
		return "[" + site.getClass().toString() + "]";
	}
	
	@Override
	public <E> E accept(Visitor<E> visitor) {
		return visitor.visit(this);
	}
}
