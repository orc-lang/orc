//
// Site.java -- Java class Site
// Project OrcJava
//
// $Id$
//
// Copyright (c) 2010 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.ast.oil.expression.argument;

import orc.Config;
import orc.ast.oil.visitor.Visitor;
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
 */

public class Site extends Argument {

	public orc.ast.sites.Site site;

	public Site(final orc.ast.sites.Site site) {
		this.site = site;
	}

	/**
	 * Sites' hash code is based on the site's toString() representation.
	 * 
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		return site == null ? 0 : site.toString().hashCode();
	}

	/**
	 * Sites' equality is based on the site's toString() representation.
	 * 
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		final Site other = (Site) obj;
		if (site == null) {
			if (other.site != null) {
				return false;
			}
		} else if (!site.toString().equals(other.site.toString())) {
			return false;
		}
		return true;
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
