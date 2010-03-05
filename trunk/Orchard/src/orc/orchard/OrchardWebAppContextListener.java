//
// OrchardWebAppContextListener.java -- Java class OrchardWebAppContextListener
// Project Orchard
//
// $Id$
//
// Created by jthywiss on Feb 24, 2010.
//
// Copyright (c) 2010 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.orchard;

import java.util.Collections;
import java.util.Enumeration;

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

	/* (non-Javadoc)
	 * @see javax.servlet.ServletContextListener#contextInitialized(javax.servlet.ServletContextEvent)
	 */
	@Override
	public void contextInitialized(final ServletContextEvent event) {
		final Enumeration<? extends String> entries = event.getServletContext().getInitParameterNames();
		for (final String parmName : Collections.list(entries)) {
			OrchardProperties.setProperty(parmName, event.getServletContext().getInitParameter(parmName));
		}
	}

	/* (non-Javadoc)
	 * @see javax.servlet.ServletContextListener#contextDestroyed(javax.servlet.ServletContextEvent)
	 */
	@Override
	public void contextDestroyed(final ServletContextEvent event) {
	}

}
