//
// AmbiguousInvocationException.java -- Java class AmbiguousInvocationException
// Project OrcScala
//
// $Id$
//
// Created by jthywiss on Jun 23, 2010.
//
// Copyright (c) 2010 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.error.runtime;

/**
 * per JLS section 15.12.2.5
 *
 * @author jthywiss
 */
@SuppressWarnings("serial") //We don't care about serialization compatibility of Orc Exceptions
public class AmbiguousInvocationException extends RuntimeTypeException {

  /**
   * Constructs an object of class AmbiguousInvocationException.
   *
   * @param methodNames
   */
  public AmbiguousInvocationException(String[] methodNames) {
    super("Ambiguous method invocation: "+mkstring(methodNames, "  -OR-  "));
  }

  private static String mkstring(Object[] array, String sep) {
    StringBuilder s = new StringBuilder();
    for (int i = 0; i < array.length; i++) {
      s.append(array[i].toString());
      if (i < array.length -1)
        s.append(sep);
    }
    return s.toString();
  }
}
