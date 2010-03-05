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

package orc.ast.xml.expression.argument;

import java.net.URI;

import javax.xml.bind.annotation.XmlAttribute;

import orc.Config;
import orc.error.compiletime.CompilationException;

public class Site extends Argument {
	@XmlAttribute(required = true)
	public String protocol;
	@XmlAttribute(required = true)
	public URI location;

	public Site() {
	}

	public Site(final String protocol, final URI location) {
		this.protocol = protocol;
		this.location = location;
	}

	@Override
	public String toString() {
		return super.toString() + "(" + protocol + ", " + location + ")";
	}

	@Override
	public orc.ast.oil.expression.argument.Argument unmarshal(final Config config) throws CompilationException {
		return new orc.ast.oil.expression.argument.Site(orc.ast.sites.Site.build(protocol, location));
	}
}
