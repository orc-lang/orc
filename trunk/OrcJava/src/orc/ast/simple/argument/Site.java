//
// Site.java -- Java class Site
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

package orc.ast.simple.argument;

import orc.env.Env;

/**
 * Site values, which occur in argument position. 
 * 
 * @author dkitchin
 */
public class Site extends Argument {

	public orc.ast.sites.Site site;

	public Site(final orc.ast.sites.Site site) {
		this.site = site;
	}

	@Override
	public orc.ast.oil.expression.argument.Argument convert(final Env<Variable> vars) {

		return new orc.ast.oil.expression.argument.Site(site);
	}

	@Override
	public String toString() {
		return site.toString();
	}
}
