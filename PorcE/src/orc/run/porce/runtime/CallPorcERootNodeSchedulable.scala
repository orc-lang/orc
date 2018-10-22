//
// CallPorcERootNodeSchedulable.scala -- Scala class and object CallPorcERootNodeSchedulable
// Project PorcE
//
// Created by amp on Oct 2018.
//
// Copyright (c) 2018 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.run.porce.runtime

import orc.Schedulable
import orc.values.Format
import com.oracle.truffle.api.RootCallTarget

final class CallPorcERootNodeSchedulable(private var _target: RootCallTarget, private var _arguments: Array[AnyRef]) extends Schedulable {
  require(_target != null)
  require(_arguments != null)

  override val nonblocking: Boolean = true

  def run(): Unit = {
    val (t, a) = (_target, _arguments)
    _target = null
    _arguments = null
    t.call(a: _*)
  }

  override def toString(): String = {
    val a = if (PorcERuntime.displayClosureValues) {
      if (_arguments == null) "" else {
        _arguments.map(v => Format.formatValue(v).take(48)).mkString(", ")
      }
    } else {
      ""
    }

    s"${_target}($a)"
  }
}
