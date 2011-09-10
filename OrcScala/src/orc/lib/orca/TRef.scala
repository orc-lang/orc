//
// TRef.scala -- Scala class/trait/object TRef
// Project Orca
//
// $Id$
//
// Created by dkitchin on Jun 29, 2011.
//
// Copyright (c) 2011 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//
package orc.lib.orca

import orc.values.sites.TotalSite
import orc.values.sites.Site0
import orc.values.sites.Site1
import orc.values.OrcRecord
import orc.Handle
import orc.run.orca.Repository
import orc.error.NotYetImplementedException
import orc.error.runtime.ArityMismatchException

/**
 * 
 *
 * @author dkitchin
 */
object TRef extends TotalSite {

  def evaluate(args: List[AnyRef]): AnyRef = {
    val instance = {
      args match {
        case Nil => new TRefInstance(None)
        case List(a) => new TRefInstance(Some(a))
        case _ => throw new ArityMismatchException(1, args.size)
      }
    }
    new OrcRecord(
      "read" -> instance.readSite,
      "write" -> instance.writeSite
    )
  }
  
}


class TRefInstance(initialContents: Option[AnyRef]) {
  
  val repository = new Repository[Option[AnyRef]](initialContents)
  
  val readSite = new Site0 {
    def call(h: Handle) {
      val tx = h.context
      repository.read(tx) match {
        case Some(v) => {
          h.publish(v)
        }
        case None => {
          // TODO: Implement blocking reads
          throw new NotYetImplementedException("Blocking reads on txn refs not yet implemented") 
        }
      }
    }
  }
  
  val writeSite = new Site1 {
    def call(a: AnyRef, h: Handle) {
      val tx = h.context
      repository.write(tx, Some(a), h)
    }
  }
  
}






