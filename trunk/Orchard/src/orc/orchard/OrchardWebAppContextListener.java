//
// OrchardWebAppContextListener.java -- Java class OrchardWebAppContextListener
// Project Orchard
//
// $Id$
//
// Created by jthywiss on Feb 24, 2010.
//
// Copyright (c) 2013 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.orchard;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.Enumeration;
import java.util.jar.Manifest;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

/**
 * Listens for initialized and destroyed events on the Orchard Web
 * application context.
 *
 * Currently, this just copies the Web app's initialization parameters
 * into the <code>OrchardProperties</code> map.
 *
 * @author jthywiss
 */
public class OrchardWebAppContextListener implements ServletContextListener {

	protected static Logger logger = Logger.getLogger("orc.orchard");

	/* (non-Javadoc)
	 * @see javax.servlet.ServletContextListener#contextInitialized(javax.servlet.ServletContextEvent)
	 */
	@Override
	public void contextInitialized(final ServletContextEvent event) {
		final Enumeration<? extends String> entries = event.getServletContext().getInitParameterNames();
		for (final String parmName : Collections.list(entries)) {
			OrchardProperties.setProperty(parmName, event.getServletContext().getInitParameter(parmName));
		}
		try {
			final InputStream manifestStream = event.getServletContext().getResourceAsStream("/META-INF/MANIFEST.MF");
			if (manifestStream != null) {
				final Manifest warManifest = new Manifest(manifestStream);
				for (Object key : warManifest.getMainAttributes().keySet()) {
					final String propName = "war.manifest." + key;
					OrchardProperties.setProperty(propName, warManifest.getMainAttributes().get(key).toString());
				}
			} else {
				logger.log(Level.SEVERE, "Orchard WAR manifest attributes read failed: ServletContext.getResourceAsStream(\"/META-INF/MANIFEST.MF\") returned null");
			}
		} catch (IOException e) {
			logger.log(Level.SEVERE, "Orchard WAR manifest attributes read failed", e);
		}
		logger.config(getOrchardFullVersionString());
		logger.config(orc.Main.orcImplName() + " " + orc.Main.orcVersion());
	}

	private String getOrchardFullVersionString() {
		return OrchardProperties.getProperty("war.manifest.Implementation-Title") + " " +
			OrchardProperties.getProperty("war.manifest.Implementation-Version") +
			" rev. " + OrchardProperties.getProperty("war.manifest.SVN-Revision");
	}

	/* (non-Javadoc)
	 * @see javax.servlet.ServletContextListener#contextDestroyed(javax.servlet.ServletContextEvent)
	 */
	@Override
	public void contextDestroyed(final ServletContextEvent event) {
	}

}
