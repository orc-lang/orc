//
// NotYetImplementedException.java -- Java class NotYetImplementedException
// Project OrcScala
//
// $Id$
//
// Created by jthywiss on Jun 11, 2010.
//
// Copyright (c) 2010 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.error;

/**
 * An exception for an unimplemented point in code.
 *
 * @author jthywiss
 */
@SuppressWarnings("serial")
public class NotYetImplementedException extends RuntimeException {
  /**
   * Constructs an object of class NotYetImplementedException.
   */
  public NotYetImplementedException() {
    super(Thread.currentThread().getStackTrace()[2].toString()+" not yet implemented");
  }

  /**
   * Constructs an object of class NotYetImplementedException.
   *
   * @param message
   */
  public NotYetImplementedException(String message) {
    super(Thread.currentThread().getStackTrace()[2].toString()+": "+message);
  }
}
