//
// RecordExtensorType.scala -- Scala class RecordExtensorType
// Project OrcScala
//
// $Id$
//
// Created by dkitchin on Mar 30, 2011.
//
// Copyright (c) 2011 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//
package orc.types

import orc.error.compiletime.typing.ArgumentTypecheckingException

/** The type of the record extension operation (+).
  *
  * @author dkitchin
  */
class RecordExtensorType extends BinaryCallableType with StrictType {

  def call(t: Type, u: Type) = {
    (t, u) match {
      case (rt: RecordType, ru: RecordType) => rt + ru
      case (RecordType(_), a) => throw new ArgumentTypecheckingException(1, EmptyRecordType, a)
      case (a, _) => throw new ArgumentTypecheckingException(0, EmptyRecordType, a)
    }

  }

}
