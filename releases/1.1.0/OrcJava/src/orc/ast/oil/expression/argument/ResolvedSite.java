//
// ResolvedSite.java -- Java class ResolvedSite
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
import orc.error.compiletime.SiteResolutionException;
import orc.error.compiletime.typing.TypeException;
import orc.type.Type;
import orc.type.TypingContext;

/**
 * A site which has been resolved and instantiated.
 * @author quark
 */
public class ResolvedSite extends Site {
	transient private final orc.runtime.sites.Site instance;

	public ResolvedSite(final Config config, final orc.ast.sites.Site site) throws SiteResolutionException {
		super(site);
		instance = site.instantiate(config);
	}

	/**
	 * ResolvedSites' hash codes are the same as the unresolved Site
	 * 
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		return super.hashCode();
	}

	/**
	 * ResolvedSites' equality is based on the site's toString() representation;
	 * the site instance is not compared.  However, Sites and ResolvedSites
	 * are not equal, even for the same site.
	 * 
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (!super.equals(obj)) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		final ResolvedSite other = (ResolvedSite) obj;
		if (instance == null) {
			if (other.instance != null) {
				return false;
			}
		} else if (!site.toString().equals(other.site.toString())) {
			return false;
		}
		return true;
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
