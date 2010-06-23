//
// Builtin.scala -- Collection of objects implementing Orc fundamental sites
// Project OrcScala
//
// $Id$
//
// Created by dkitchin on May 28, 2010.
//
// Copyright (c) 2010 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

/**
 * @authors dkitchin, jthywiss
 */

package orc.lib.builtin

import orc.oil.nameless.Type
import orc.values._
import orc.values.sites._
import orc.error.runtime.ArgumentTypeMismatchException
import orc.error.runtime.ArityMismatchException
import orc.run.StandardOrcExecution
import scala.actors.Actor
import orc.TokenAPI
import orc.oil.nameless.Call
import orc.oil.nameless.Constant

// Logic

object IfT extends PartialSite with UntypedSite {
  override def name = "IfT"
  def evaluate(args: List[Value]) =
    args match {
      case List(Literal(true)) => Some(Signal)
      case List(Literal(false)) => None
      case List(a) => throw new ArgumentTypeMismatchException(0, "Boolean", a.getClass().toString())
      case _ => throw new ArityMismatchException(1, args.size)
  }
}

object IfF extends PartialSite with UntypedSite {
  override def name = "IfF"
  def evaluate(args: List[Value]) =
    args match {
      case List(Literal(true)) => None
      case List(Literal(false)) => Some(Signal)
      case List(a) => throw new ArgumentTypeMismatchException(0, "Boolean", a.getClass().toString())
      case _ => throw new ArityMismatchException(1, args.size)
  }
}

object Eq extends TotalSite with UntypedSite {
  override def name = "Eq"
  def evaluate(args: List[Value]) =
    args match {
      case List(a,b) => Literal(a equals b)
      case _ => throw new ArityMismatchException(2, args.size)
  }
}



// Constructors

object Let extends TotalSite with UntypedSite {
  override def name = "let"
  def evaluate(args: List[Value]) = args match {
    case Nil => Signal
    case v :: Nil => v
    case vs => OrcTuple(vs)
    OrcTuple(args)
  }
}

object TupleConstructor extends TotalSite with UntypedSite {
  override def name = "Tuple"
  def evaluate(args: List[Value]) = OrcTuple(args)
}


object NoneConstructor extends TotalSite with UntypedSite {
  override def name = "None"
  def evaluate(args: List[Value]) =
    args match {
      case List() => OrcOption(None)
      case _ => throw new ArityMismatchException(0, args.size)
  }
  override def extract = Some(NoneExtractor)
}

object SomeConstructor extends TotalSite with UntypedSite {
  override def name = "Some"
  def evaluate(args: List[Value]) =
    args match {
      case List(v) => OrcOption(Some(v))
      case _ => throw new ArityMismatchException(1, args.size)
  }
  override def extract = Some(SomeExtractor)
}



object NilConstructor extends TotalSite with UntypedSite {
  override def name = "Nil"
  def evaluate(args: List[Value]) =
    args match {
      case List() => OrcList(Nil)
      case _ => throw new ArityMismatchException(0, args.size)
  }
  override def extract = Some(NilExtractor)
}

object ConsConstructor extends TotalSite with UntypedSite {
  override def name = "Cons"
  def evaluate(args: List[Value]) =
    args match {
      case List(v, OrcList(vs)) => OrcList(v :: vs)
      case List(v1, v2) => throw new ArgumentTypeMismatchException(1, "List", v2.getClass().toString())
      case _ => throw new ArityMismatchException(2, args.size)
  }
  override def extract = Some(ConsExtractor)
}

object RecordConstructor extends UnimplementedSite //FIXME:TODO: Implement

object DatatypeBuilder extends TotalSite with UntypedSite {
  
  override def name = "Datatype"
  def evaluate(args: List[Value]) =
    args match {
      case List(OrcTuple(vs)) => {
        val datasites: List[Value] = 
          for (OrcTuple(Literal(name: String) :: List(Literal(arity: Int))) <- vs)
            yield new DataSite(name,arity)
        OrcTuple(datasites)
      }
    }
}

class DataSite(name: String, arity: Int) extends Site with UntypedSite {
  
  override def call(args: List[Value], token: TokenAPI) {
      if(args.size != arity) 
        throw new ArityMismatchException(arity, args.size)
   
      token.publish(new TaggedValues(this,args))
  }
  
  override def extract = Some(new PartialSite  with UntypedSite {
 
    override def evaluate(args: List[Value]) = {
        args match {
          case List(TaggedValues(tag,values)) => {
            if (tag == DataSite.this)
              Some(OrcTuple(values))
            else 
              None
          }
          case _ => None
        }
    }
  })
}

// Extractors

object NoneExtractor extends PartialSite with UntypedSite {
  override def name = "None?"
  def evaluate(args: List[Value]) =
    args match {
      case List(OrcOption(None)) => Some(Signal)
      case List(OrcOption(_)) => None
      case List(a) => throw new ArgumentTypeMismatchException(0, "Option", a.getClass().toString())
      case _ => throw new ArityMismatchException(1, args.size)
  }
}

object SomeExtractor extends PartialSite with UntypedSite {
  override def name = "Some?"
  def evaluate(args: List[Value]) =
    args match {
      case List(OrcOption(Some(v))) => Some(v)
      case List(OrcOption(_)) => None
      case List(a) => throw new ArgumentTypeMismatchException(0, "Option", a.getClass().toString())
      case _ => throw new ArityMismatchException(1, args.size)
  }
}



object NilExtractor extends PartialSite with UntypedSite {
  override def name = "Nil?"
  def evaluate(args: List[Value]) =
    args match {
      case List(OrcList(Nil)) => Some(Signal)
      case List(OrcList(_)) => None
      case List(a) => throw new ArgumentTypeMismatchException(0, "List", a.getClass().toString())
      case _ => throw new ArityMismatchException(1, args.size)
  }
}

object ConsExtractor extends PartialSite with UntypedSite {
  override def name = "Cons?"
  def evaluate(args: List[Value]) =
    args match {
      case List(OrcList(v :: vs)) => Some(OrcTuple(List(v, OrcList(vs))))
      case List(OrcList(_)) => None
      case List(a) => throw new ArgumentTypeMismatchException(0, "List", a.getClass().toString())
      case _ => throw new ArityMismatchException(1, args.size)
  }
}

/* 
 * Checks if a Tuple t has a given number of elements.
 * If the check succeeds, the Some(t) is returned,
 * else None.
 */
object TupleArityChecker extends PartialSite with UntypedSite {
  override def name = "TupleArityChecker?"
  def evaluate(args: List[Value]) =
    args match {
      case List(OrcTuple(elems),Literal(arity:Int)) =>
        if (elems.size == arity) {
          Some(OrcTuple(elems))
        } else {
          None
        }
      case List(OrcTuple(_),_) => None
      case List(a) => throw new ArgumentTypeMismatchException(0, "List", a.getClass().toString())
      case _ => throw new ArityMismatchException(1, args.size)
  }
}

object FindExtractor extends TotalSite with UntypedSite {
  override def name = "FindExtractor"
  def evaluate(args: List[Value]) =
    args match {
      case List(s : Site) => s.extract match {
        case Some(extractor) => extractor
        case None => throw new Exception("Could not find extractor for site"+s)
      }
    }
}


// Site site

object SiteSite extends TotalSite with UntypedSite {
  override def name = "Site"
  def evaluate(args: List[Value]) =
    args match {
      case List(c : Closure) => {
        new Site with UntypedSite {
          override def name = "_capsule_"
          def call(args: List[Value], token: TokenAPI) {
            val capsule = new CapsuleExecution(token, c, args)
            capsule.start
          }
        }
      }
      case List(a) => throw new ArgumentTypeMismatchException(0, "Closure", a.getClass().toString())
      case _ => throw new ArityMismatchException(1, args.size)
  }
}

class CapsuleExecution(caller: TokenAPI, code: Closure, args: List[Value]) extends StandardOrcExecution with Actor {
  
  var listener: Option[TokenAPI] = Some(caller)
  
  def act() {
    this.run(Call(Constant(code), args map Constant, Some(Nil)))
  }
  
  def emit(v: Value) { 
    listener foreach { 
      listener = None
      _.publish(v)
    }
  }
  
  def halted { 
    listener foreach {
      listener = None
      _.halt
    }
  }
}








