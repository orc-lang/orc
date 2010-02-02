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

package orc.ast.sites;

import java.net.URI;
import java.net.URISyntaxException;

import orc.Config;
import orc.error.OrcError;
import orc.error.compiletime.SiteResolutionException;

/**
 * 
 * A portable representation of sites. When creating the execution graph, 
 * these are converted to in-memory objects.
 * 
 * @author dkitchin
 *
 */
public abstract class Site {

	/* Protocols */
	public static final String ORC = "orc";
	public static final String JAVA = "java";

	/* Primitive sites */
	public static Site LET = buildCoreSite("Let");
	public static Site ERROR = buildCoreSite("Error");

	public static Site IF = buildCoreSite("If");
	public static Site NOT = buildCoreSite("Not");

	public static Site SOME = buildCoreSite("Some");
	public static Site NONE = buildCoreSite("None");

	public static Site ISSOME = buildCoreSite("IsSome");
	public static Site ISNONE = buildCoreSite("IsNone");

	public static Site CONS = buildCoreSite("Cons");
	public static Site NIL = buildCoreSite("Nil");

	public static Site TRYCONS = buildCoreSite("TryCons");
	public static Site TRYNIL = buildCoreSite("TryNil");

	public static Site EQUAL = buildCoreSite("Equal");

	public static Site DATATYPE = buildCoreSite("Datatype");

	public static Site build(final String protocol, final String location) {
		try {
			return build(protocol, new URI(location));
		} catch (final URISyntaxException e) {
			throw new OrcError(location + " is not a valid URI");
		}
	}

	public static Site build(final String protocol, final URI location) {

		if (protocol.equals(ORC)) {
			return new OrcSite(location);
		} else if (protocol.equals(JAVA)) {
			return new JavaSite(location);
		} else {
			throw new OrcError("'" + protocol + "' is not a supported protocol.");
		}
	}

	/* Specialization of build for primitive sites */
	public static Site buildCoreSite(final String primitive) {
		final String location = "orc.runtime.sites.core." + primitive;
		return build(ORC, location);
	}

	public abstract URI getLocation();

	public abstract String getProtocol();

	public abstract orc.runtime.sites.Site instantiate(Config config) throws SiteResolutionException;

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + (getLocation() == null ? 0 : getLocation().hashCode());
		result = prime * result + (getProtocol() == null ? 0 : getProtocol().hashCode());
		return result;
	}

	/**
	 * Equality on sites.
	 * 
	 * Two sites are equal if their protocols are equal
	 * and their locations are equal.
	 * 
	 * @param obj  The site to which to compare.
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
		final Site that = (Site) obj;
		return this.getLocation().equals(that.getLocation()) && this.getProtocol().equals(that.getProtocol());
	}

	@Override
	public String toString() {
		return "#site(" + getProtocol() + ", " + getLocation() + ")";
	}
}
