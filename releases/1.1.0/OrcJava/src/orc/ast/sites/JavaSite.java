//
// JavaSite.java -- Java class JavaSite
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
import orc.runtime.sites.java.ClassProxy;

public class JavaSite extends Site {

	protected URI location;

	public JavaSite(final URI location) {
		this.location = location;
	}

	@Override
	public URI getLocation() {
		return location;
	}

	@Override
	public String getProtocol() {
		return JAVA;
	}

	private Class classify(final Config config) throws SiteResolutionException {
		final String classname = location.getSchemeSpecificPart();
		try {
			final Class<?> cls = config.loadClass(classname);
			if (orc.runtime.sites.Site.class.isAssignableFrom(cls)) {
				throw new SiteResolutionException("Tried to load a subclass of orc.runtime.sites.Site as a Java class -- that's not allowed!");
			}
			return cls;
		} catch (final ClassNotFoundException e) {
			throw new SiteResolutionException("Failed to load class " + classname, e);
		}
	}

	@Override
	public orc.runtime.sites.Site instantiate(final Config config) throws SiteResolutionException {
		return ClassProxy.forClass(classify(config));
	}
}
