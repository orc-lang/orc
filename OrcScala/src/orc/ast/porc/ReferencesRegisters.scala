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
  this: Command =>
  lazy val referencesCounter: Boolean = {
    var foundCounter = false
    (new ContextualTransform.Post {
      override def onCommand = {
      case e@(LetIn(_, _) 
         | SiteCallIn(_, _, _) 
         | SpawnIn(_, _)
         | NewCounterIn(_) 
         | RestoreCounterIn(_, _) 
         | SetCounterHaltIn(_, _) 
         | GetCounterHaltIn(_, _) 
         | DecrCounterIn(_)) => foundCounter = true; e
      }
    })(this)
    foundCounter
  }
  lazy val referencesTerminator: Boolean = {
    var foundTerminator = false
    (new ContextualTransform.Post {
      override def onCommand = {
      case e@(LetIn(_, _) 
         | SiteCallIn(_, _, _) 
         //| NewTerminatorIn(_) // TODO: In theory this actually blocks T references. I think
         | GetTerminatorIn(_, _) 
         | KillIn(_, _) 
         | IsKilledIn(_, _) 
         | CallKillHandlersIn(_)) => foundTerminator = true; e
      }
    })(this)
    foundTerminator
  }
}