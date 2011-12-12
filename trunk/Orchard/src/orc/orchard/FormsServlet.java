//
// FormsServlet.java -- Java class FormsServlet
// Project Orchard
//
// $Id$
//
// Copyright (c) 2009 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.orchard;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import orc.lib.orchard.forms.FormSenderSite;

@SuppressWarnings("serial")
public class FormsServlet extends HttpServlet {

	@Override
	protected void service(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException {
		FormSenderSite.service(request, response);
	}

	/* (non-Javadoc)
	 * @see javax.servlet.GenericServlet#getServletInfo()
	 */
	@Override
	public String getServletInfo() {
		return "Copyright The University of Texas at Austin";
	}

}
