
package orc.ast.oil.expression.argument;

import java.util.Set;

import orc.Config;
import orc.ast.oil.Visitor;
import orc.env.Env;
import orc.error.compiletime.SiteResolutionException;
import orc.error.compiletime.typing.TypeException;
import orc.type.Type;
import orc.type.TypingContext;

/**
 * A site which has been resolved and instantiated.
 * @author quark
 */
public class ResolvedSite extends Site {
	private final orc.runtime.sites.Site instance;

	public ResolvedSite(final Config config, final orc.ast.sites.Site site) throws SiteResolutionException {
		super(site);
		instance = site.instantiate(config);
	}

	@Override
	public Site resolveSites(final Config config) throws SiteResolutionException {
		// already resolved
		return this;
	}

	@Override
	public Object resolve(final Env<Object> env) {
		return instance;
	}

	@Override
	public String toString() {
		return site.toString();
	}

	@Override
	public <E> E accept(final Visitor<E> visitor) {
		return visitor.visit(this);
	}

	@Override
	public Type typesynth(final TypingContext ctx) throws TypeException {
		return instance.type();
	}

}
