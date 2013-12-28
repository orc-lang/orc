//
// ReferencesRegisters.scala -- Scala class/trait/object ReferencesRegisters
// Project OrcScala
//
// $Id$
//
// Created by amp on Jun 29, 2013.
//
// Copyright (c) 2013 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//
package orc.ast.porc

/**
  *
  * @author amp
  */
trait ReferencesRegisters {
  this: Expr =>
  lazy val referencesCounter: Boolean = {
    var foundCounter = false
    (new ContextualTransform.Post {
      override def onExpr = {
      case e@(LambdaIn(_, _, _) 
         | SiteCallIn(_, _, _, _) 
         | ForceIn(_, _, _) 
         | ResolveIn(_, _) 
         | SpawnIn(_)
         | NewCounterIn(_) 
         | RestoreCounterIn(_, _) 
         | SetCounterHaltIn(_)
         | CallCounterHalt() in _
         | CallParentCounterHalt() in _
         | MakeCounterTopLevel() in _
         | DecrCounter() in _) => foundCounter = true; e
      }
    })(this)
    foundCounter
  }
  lazy val referencesTerminator: Boolean = {
    var foundTerminator = false
    (new ContextualTransform.Post {
      override def onExpr = {
      case e@(LambdaIn(_, _, _) 
         | SiteCallIn(_, _, _, _) 
         | ExternalCallIn(_, _, _, _) 
         | ForceIn(_, _, _) 
         | ResolveIn(_, _) 
         //| NewTerminatorIn() in _ // TODO: In theory this actually blocks T references. I think
         | GetTerminator() in _ 
         | Kill(_,_) in _ 
         | Killed() in _ 
         | CheckKilled() in _ 
         ) => foundTerminator = true; e
      }
    })(this)
    foundTerminator
  }
}