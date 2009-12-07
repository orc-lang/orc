package orc.ast.oil.expression.argument;

import java.util.Set;

import orc.Config;
import orc.ast.oil.Visitor;
import orc.env.Env;
import orc.error.OrcError;
import orc.error.compiletime.SiteResolutionException;
import orc.error.compiletime.typing.TypeException;
import orc.type.Type;
import orc.type.TypingContext;


/**
 * A site which has been resolved and instantiated.
 * @author quark
 */
public class ResolvedSite extends Site {
	private orc.runtime.sites.Site instance;
	public ResolvedSite(Config config, orc.ast.sites.Site site) throws SiteResolutionException {
		super(site);
		instance = site.instantiate(config);
	}
	
	@Override
	public Site resolveSites(Config config) throws SiteResolutionException {
		// already resolved
		return this;
	}
	
	@Override
	public Object resolve(Env<Object> env) {
		return instance;
	}
	
	public String toString() {
		return site.toString();
	}
	
	@Override
	public <E> E accept(Visitor<E> visitor) {
		return visitor.visit((Site)this);
	}

	@Override
	public Type typesynth(TypingContext ctx) throws TypeException {
		return instance.type();
	}
	
	@Override
	public void addIndices(Set<Integer> indices, int depth) {
		// do nothing
	}
}
