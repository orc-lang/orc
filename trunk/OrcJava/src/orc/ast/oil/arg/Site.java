package orc.ast.oil.arg;

import java.net.URI;
import java.net.URISyntaxException;

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
	@Override
	public orc.orchard.oil.Argument marshal() {
		try {
			return new orc.orchard.oil.Site("orc", new URI(site.getClass().getName()));
		} catch (URISyntaxException e) {
			// impossible by construction
			throw new AssertionError(e);
		}
	}
}
