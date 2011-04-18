//
// ThreadedPartialSite.java -- Java class ThreadedPartialSite
// Project OrcScala
//
// $Id$
//
// Copyright (c) 2009 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.values.sites.compatibility;

/**
 * Abstract class for partial sites whose calls may block (the Java thread). A
 * separate thread is created for every call.
 * 
 * @author quark
 */
public abstract class ThreadedPartialSite extends PartialSite {
  //FIXME: Run evaluate in a separate thread
}