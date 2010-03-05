//
// OrcSite.java -- Java class OrcSite
// Project OrcJava
//
// $Id$
//
// Copyright (c) 2009 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.ast.sites;

import java.net.URI;

import orc.Config;
import orc.error.compiletime.SiteResolutionException;

public class OrcSite extends Site {

	protected URI location;

	public OrcSite(final URI location) {
		this.location = location;
	}

	@Override
	public URI getLocation() {
		return location;
	}

	@Override
	public String getProtocol() {
		return ORC;
	}

	private Class classify(final Config config) throws SiteResolutionException {
		final String classname = location.getSchemeSpecificPart();
		Class<?> cls;

		try {
			cls = config.loadClass(classname);
		} catch (final ClassNotFoundException e) {
			throw new SiteResolutionException("Failed to load class " + classname + " as a site. Class not found.");
		}

		if (orc.runtime.sites.Site.class.isAssignableFrom(cls)) {
			return cls;
		} else {
			throw new SiteResolutionException("Class " + cls + " cannot be used as a site because it is not a subtype of orc.runtime.sites.Site.");
		}
	}

	@Override
	public orc.runtime.sites.Site instantiate(final Config config) throws SiteResolutionException {

		final Class cls = classify(config);
		try {
			return (orc.runtime.sites.Site) cls.newInstance();
		} catch (final InstantiationException e) {
			throw new SiteResolutionException("Failed to load class " + cls + " as a site. Instantiation error.", e);
		} catch (final IllegalAccessException e) {
			throw new SiteResolutionException("Failed to load class " + cls + " as a site. Constructor is not accessible.");
		}

	}
}
