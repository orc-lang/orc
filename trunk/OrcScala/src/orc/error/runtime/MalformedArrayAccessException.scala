//
// MalformedArrayAccessException.scala -- Scala class MalformedArrayAccessException
// Project OrcScala
//
// $Id$
//
// Created by jthywiss on Jul 14, 2010.
//
// Copyright (c) 2010 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//
package orc.error.runtime

class MalformedArrayAccessException(val args: List[AnyRef]) extends
  TokenException("Array access requires a single Integer as an argument")
