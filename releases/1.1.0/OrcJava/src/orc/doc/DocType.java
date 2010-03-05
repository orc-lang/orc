//
// DocType.java -- Java class DocType
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

package orc.doc;

public class DocType extends DocNode {
	public final int depth;
	public final String type;

	public DocType(final int depth, final String type) {
		this.depth = depth;
		this.type = type;
	}
}
