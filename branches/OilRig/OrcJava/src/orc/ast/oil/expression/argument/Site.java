
package orc.ast.oil.expression.argument;

import java.util.Set;

import orc.Config;
import orc.ast.oil.Visitor;
import orc.env.Env;
import orc.error.OrcError;
import orc.error.compiletime.CompilationException;
import orc.error.compiletime.SiteResolutionException;
import orc.error.compiletime.typing.TypeException;
import orc.type.Type;
import orc.type.TypingContext;

/**
 * Sites, which occur in argument position. 
 * 
 * @author dkitchin
 *
 */

public class Site extends Argument {

	public orc.ast.sites.Site site;

	public Site(final orc.ast.sites.Site site) {
		this.site = site;
	}

	public Site resolveSites(final Config config) throws SiteResolutionException {
		return new ResolvedSite(config, site);
	}

	@Override
	public Object resolve(final Env<Object> env) {
		throw new OrcError("Unexpected orc.ast.oil.arg.Site");
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
		throw new OrcError("Unexpected orc.ast.oil.arg.Site");
	}

	@Override
	public orc.ast.xml.expression.argument.Argument marshal() throws CompilationException {
		return new orc.ast.xml.expression.argument.Site(site.getProtocol(), site.getLocation());
	}
}
