//
// DOrcMarshaling.scala -- Scala traits DOrcMarshalingNotifications and DOrcMarshalingReplacement
// Project OrcScala
//
// Created by jthywiss on Sep 24, 2017.
//
// Copyright (c) 2017 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.run.distrib

/** Orc values implementing this trait will be notified of marshaling for
  * serialization to another location.
  *
  * @author jthywiss
  */
trait DOrcMarshalingNotifications {
  def marshaled() {}
  def unmarshaled() {}
}

/** Orc values implementing this trait will be asked for a marshalable
  * replacement for themselves when they are marshaled for serialization
  * to another location.
  *
  * @author jthywiss
  */
trait DOrcMarshalingReplacement {
  def isReplacementNeededForMarshaling(marshalValueWouldReplace: AnyRef => Boolean): Boolean
  def replaceForMarshaling(marshaler: AnyRef => AnyRef): AnyRef
  def isReplacementNeededForUnmarshaling(unmarshalValueWouldReplace: AnyRef => Boolean): Boolean
  def replaceForUnmarshaling(unmarshaler: AnyRef => AnyRef): AnyRef
}
