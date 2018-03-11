//
// PorcTags.scala -- Truffle tag classes and predicate objects for PorcE
// Project PorcE
//
// Copyright (c) 2018 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.run.porce.instruments

import orc.ast.porc.PorcAST
import orc.run.porce.NodeBase

class TailTag private () {
}

class ProfiledPorcNodeTag private () {
}

object ProfiledPorcNodeTag {
  def isProfiledPorcNode(n: PorcAST): Boolean = {
    import orc.ast.porc._
    n match {
      // Closure Construction 
      case _: Continuation | _: MethodDeclaration => true
      // Calls 
      case _: MethodDirectCall | _: MethodCPSCall | _: CallContinuation => true
      // Future Binds
      case _: Bind | _: BindStop => true
      // Future "Forces"
      case _: Force | _: Resolve => true
      // Internal Object Creation
      case _: NewFuture | _: NewTerminator | _: NewCounter => true
      // Counter Operations
      case _: NewToken | _: HaltToken | _: SetDiscorporate => true
      // Terminator Operations
      case _: Kill | _: CheckKilled => true
      // Spawn
      case _: Spawn => true
      // Objects
      case _: New => true
      
      case _ => false
    }
  }
}

class ProfiledPorcENodeTag private () {
}

object ProfiledPorcENodeTag {
  def isProfiledPorcENode(n: NodeBase): Boolean = {
    import orc.run.porce._
    // This is defined by excluding some nodes and having a default of true
    n match {
      case _: Read.Argument | _: Read.Closure | _: Read.Constant | _: Read.Local => false
      case _: Write.Local => false
      case _: Sequence => false
      case _: IfLenientMethod => false
      case _: TryFinally | _: TryOnException => false
      case _: call.InternalCPSDispatch | _: call.ExternalCPSDispatch => false
      case _ => true
    }
  }
}
