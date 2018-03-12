//
// loadingExceptions.scala -- Scala child classes of LoadingException
// Project OrcScala
//
// Created by jthywiss on Dec 9, 2011.
//
// Copyright (c) 2017 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//
package orc.error.loadtime

/** When reading OIL XML, it was found to be syntactically invalid.
  */
@SerialVersionUID(-2514919839288618140L)
case class OilParsingException(message: String)
  extends LoadingException(message)

/** When reading serialized data, it was found encode the wrong type of object.
  */
case class DeserializationTypeException(message: String)
  extends LoadingException(message)
