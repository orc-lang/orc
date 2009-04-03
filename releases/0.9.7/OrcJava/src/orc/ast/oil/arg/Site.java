package orc.ast.oil.arg;

import java.util.Set;

import orc.ast.oil.Visitor;
import orc.env.Env;
import orc.error.compiletime.typing.TypeException;
import orc.error.runtime.SiteResolutionException;
import orc.type.Type;


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
	public <T> T resolve(Env<T> env) throws SiteResolutionException {
		/* TODO: Make this more efficient. 
		 * Even though sites are semantically persistent, it's 
		 * unhelpful to have many copies of the same object.
		 */
		return (T)site.instantiate();
	}
	
	public String toString() {
		return site.toString();
	}
	
	@Override
	public <E> E accept(Visitor<E> visitor) {
		return visitor.visit(this);
	}

	@Override
	public Type typesynth(Env<Type> ctx, Env<Type> typectx) throws TypeException {
		return site.type();
	}
	
	@Override
	public void addIndices(Set<Integer> indices, int depth) {
		return;
	}
}
