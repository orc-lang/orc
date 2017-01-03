//
// OrcClass.scala -- Scala class OrcClass
// Project OrcScala
//
// Created by amp on Jan 15, 2015.
//
// Copyright (c) 2015 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//
package orc.run.core

import orc.OrcRuntime
import orc.ast.oil.nameless.Class

/**
  * @author amp
  */
class OrcClass(
  index: Int,
  classGroup: ClassGroup) extends ResolvableCollectionMember[Class](index, classGroup) {
  override def toString = super.toString + (definition.sourceTextRange, collection, index)
}

class ClassGroup(
  _clss: List[Class],
  _lexicalContext: List[Binding],
  runtime: OrcRuntime)
  extends ResolvableCollection[Class, OrcClass](_clss, _lexicalContext, runtime) {
  def buildMember(i: Int) = new OrcClass(i, this)
}
