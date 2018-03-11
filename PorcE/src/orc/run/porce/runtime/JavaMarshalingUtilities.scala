//
// JavaMarshalingUtilities.scala -- Scala object JavaMarshalingUtilities
// Project PorcE
//
// Copyright (c) 2017 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.run.porce.runtime

object JavaMarshalingUtilities {
  def existsMarshalValueWouldReplace(values: Array[AnyRef], untypedMarshalValueWouldReplace: AnyRef => AnyRef): Boolean = {
    val marshalValueWouldReplace = untypedMarshalValueWouldReplace.asInstanceOf[AnyRef => Boolean]
    values exists marshalValueWouldReplace
  }
  def mapMarshaler(values: Array[AnyRef], marshaler: AnyRef => AnyRef): Array[AnyRef] = {
    values map marshaler
  }
}
