package orc.ast.oil.arg;

import orc.ast.oil.Visitor;
import orc.env.Env;
import orc.error.compiletime.typing.TypeException;
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
	public Object resolve(Env<Object> env) {
		/* TODO: Make this more efficient. 
		 * Even though sites are semantically persistent, it's 
		 * unhelpful to have many copies of the same object.
		 */
		return site.instantiate();
	}
	
	public String toString() {
		return "[" + site.toString() + "]";
	}
	
	@Override
	public <E> E accept(Visitor<E> visitor) {
		return visitor.visit(this);
	}

	@Override
	public Type typesynth(Env<Type> ctx) throws TypeException {
		return site.type();
	}
}
