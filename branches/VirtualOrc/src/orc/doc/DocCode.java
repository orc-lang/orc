//
// DocCode.java -- Java class DocCode
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

public class DocCode extends DocNode {
	public final String text;

	public DocCode(final String text) {
		this.text = text;
	}
}
