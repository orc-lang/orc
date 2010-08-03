//
// ThreadedSite.java -- Java class ThreadedSite
// Project OrcScala
//
// $Id$
//
// Created by jthywiss on Jun 2, 2010.
//
// Copyright (c) 2010 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.values.sites.compatibility;

/**
 * Abstract class for sites whose calls may block (the Java thread).
 * 
 * @author quark
 */
public abstract class ThreadedSite extends EvalSite {
    //FIXME: Run evaluate in a separate thread
}
