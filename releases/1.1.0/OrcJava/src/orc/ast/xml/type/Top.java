//
// Top.java -- Java class Top
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

package orc.ast.xml.type;

public class Top extends Type {

	public Top() {
	}

	@Override
	public orc.ast.oil.type.Type unmarshal() {
		return orc.ast.oil.type.Type.TOP;
	}
}
