package orc.run.porce.instruments

import orc.ast.porc.PorcAST

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