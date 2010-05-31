//
// Orc.scala -- Scala class Orc
// Project OrcScala
//
// $Id$
//
// Created by dkitchin on May 10, 2010.
//
// Copyright (c) 2010 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc

import orc.oil._
import orc.oil.nameless._
import PartialMapExtension._
import orc.sites.Site
import scala.collection.mutable.Set   


abstract class Orc extends OrcAPI {

  var exec: Option[Execution] = Some(new Execution())

  def run(node: Expression) {
    val exec = new Execution()
    val t = new Token(node, exec)
    t.run
  }
  
  
  // Groups
  
  // A Group is a structure associated with dynamic instances of an expression,
  // tracking all of the executions occurring within that expression.
  // Different combinators make use of different Group subclasses.
  
  trait GroupMember {
    def kill: Unit
  }


  abstract class Group extends GroupMember {
  
    def publish(t: Token, v: Value): Unit
    def onHalt: Unit
  
    var members: Set[GroupMember] = Set()
  
    def halt(t: Token) { remove(t) }
    def kill { for (m <- members) m.kill } 
    /* Note: this is _not_ lazy termination */
  
    def add(m: GroupMember) { members.add(m) }
  
    def remove(m: GroupMember) { 
      members.remove(m)
      if (members.isEmpty) { onHalt }
    }
  
  }
  
  // A Groupcell is the group associated with expression g in (f <x< g)
  
  // Possible states of a Groupcell
  class GroupcellState
  case class Unbound(waitlist: List[Token]) extends GroupcellState
  case class Bound(v: Value) extends GroupcellState
  case object Dead extends GroupcellState
  
  class Groupcell(parent: Group) extends Group {
  
    var state: GroupcellState = Unbound(Nil) 
  
    def publish(t: Token, v: Value) {
      state match {
        case Unbound(waitlist) => {
          state = Bound(v)
          schedule(waitlist)
          t.halt
          this.kill
        }
        case _ => t.halt	
      }
    }
    
    def onHalt {
      state match {
        case Unbound(waitlist) => {
          for (t <- waitlist) t.halt
          state = Dead
          parent.remove(this)
        }
        case _ => {  }
      }
    }
    
    // Specific to Groupcells
    def read(reader: Token): Option[Value] = 
      state match {
        case Bound(v) => Some(v)
        case Unbound(waitlist) => {
          state = Unbound(reader :: waitlist)
          None
        }
        case Dead => {
          reader.halt
          None
        }
    }
  }
  
  object Groupcell {
    def apply(parent: Group): Groupcell = {
        val g = new Groupcell(parent)
        parent.add(g)
        g
    }
  }
  
  
  
  
  // A Region is the group associated with expression f in (f ; g)
  class Region(parent: Group, r: Token) extends Group {
  
    // Some(r): No publications have left this region;
    //			if the group halts silently, pending
    //			will be scheduled.
    // None:	A publication has left this region.
  
    var pending: Option[Token] = Some(r)
  
    def publish(t: Token, v: Value) {
      pending.foreach(_.halt)
      t.publish(v)
    }
    
    def onHalt {
      pending.foreach(schedule(_))
      parent.remove(this)
    }
  
  }	
  
  object Region {
  
    def apply(parent: Group, r: Token): Region = {
      val g = new Region(parent, r)
      parent.add(g)
      g
    }
  }
  
  // An execution is a special toplevel group, 
  // associated with the entire program.
  class Execution extends Group {
  
    def publish(t: Token, v: Value) {
      emit(v)
      t.halt
    }
  
    def onHalt {
      halted
    }
  }
  
  
  
  // Tokens and their auxilliary structures //
  
  
  // Context entries //
  trait Binding
  case class BoundValue(v: Value) extends Binding
  case class BoundCell(g: Groupcell) extends Binding
  implicit def ValuesAreBindings(v: Value): Binding = BoundValue(v)
  implicit def GroupcellsAreBindings(g: Groupcell): Binding = BoundCell(g)
  
  
  // Closures //
  class Closure(d: Def) extends Value {
    val arity: Int = d.arity
    val body: Expression = d.body
    var context: List[Binding] = Nil
  }
  object Closure {
    def unapply(c: Closure) = Some((c.arity, c.body, c.context))
  }
  
  
  
  // Control Frames //
  abstract class Frame extends ((Token, Value) => Unit)
  
  case class BindingFrame(n: Int) extends Frame {
    def apply(t: Token, v: Value) {
      t.env = t.env.drop(n)
      t.publish(v)
    }
  }
  
  case class SequenceFrame(node: Expression) extends Frame {
    def apply(t: Token, v: Value) {
      schedule(t.bind(v).move(node))
    }
  }
  
  case class FunctionFrame(callpoint: Expression, env: List[Binding]) extends Frame {
    def apply(t: Token, v: Value) {
      t.env = env
      t.move(callpoint).publish(v)
    }
  }
  
  case object GroupFrame extends Frame {
    def apply(t: Token, v: Value) {
      t.group.publish(t,v)
    }
  }
  
  
  
  
  // Token //
  
  class TokenState
  case object Live extends TokenState
  case object Halted extends TokenState
  case object Killed extends TokenState
  
  class Token private (
      var node: Expression,
      var stack: List[Frame] = Nil,
      var env: List[Binding] = Nil,
      var group: Group, 
      var state: TokenState = Live
  ) extends TokenAPI with GroupMember {	
  
    def this(start: Expression, exec: Execution) = {
      this(node = start, group = exec)
    }
  
    // Copy constructor with defaults
    private def copy(
        node: Expression = node,
        stack: List[Frame] = stack,
        env: List[Binding] = env,
        group: Group = group,
        state: TokenState = state): Token = 
        {
      new Token(node, stack, env, group, state)
        }
  
  
  
    def fork = (this, copy())
  
    def move(e: Expression) = { node = e ; this }
  
    def push(f: Frame) = { stack = f::stack ; this }
  
  
    // reslice to improve contracts
    def join(child: Group) = { 
      val parent = group
      child.add(this); parent.remove(this)
      group = child
      push(GroupFrame)
      this 
    }			
  
    // Manipulating context frames
  
    def bind(b: Binding): Token = {
      env = b::env
      stack match {
        case BindingFrame(n)::fs => { stack = (new BindingFrame(n+1))::fs }
    
        /* Tail call optimization (part 1 of 2) */
        case FunctionFrame(_,_)::fs => { /* Do not push a binding frame over a tail call.*/ }
    
        case fs => { stack = BindingFrame(1)::fs }
      }
      this
    }
  
  
    def lookup(a: Argument): Binding = 
      a match {
        case Constant(v) => v
        case Variable(n) => env(n)
    }
  
    // Caution: has a side effect! :-P
    def resolve(a: Argument): Option[Value] =
      lookup(a) match {
        case BoundValue(v) => Some(v)
        case BoundCell(g) => g.read(this)
    }
  
  
  
    // Publicly accessible methods
  
    def publish(v: Value) {
      stack match {
        case f::fs => { 
          stack = fs
          f(this, v)
        }
        case Nil => { emit(v) } // !!!
      }
    }
  
    def halt {
      state match {
        case Live => { state = Halted }
        case _ => {  }
      }
    }
  
    def kill {
      state match {
        case Live => { state = Killed }
        case _ => {  }
      }
    }
  
    def run {
      if (state == Live) {
        node match {
          case Stop() => halt
          case (a: Argument) => resolve(a).foreach(publish(_))
          case (Call(target, args, typeArgs)) => {
            resolve(target).foreach({
              case Closure(arity, body, newcontext) => {
                if (arity != args.size) halt /* Arity mismatch. */
      
      
                /* 
                 *          
                 * 1) Push a function frame (if this is not a tail call),
                 *    referring to the current environment
                 * 2) Change the current environment to the closure's
                 *    saved environment.
                 * 3) Add bindings for the arguments to the new current
                 *    environment
                 * 
                 * Caution: The ordering of these statements is very important;
                 *          do not permute them.    
                 */
      
                /* Tail call optimization (part 2 of 2) */
                stack match {
                  /*
                   * Push a new FunctionFrame 
                   * only if the call is not a tail call.
                   */
                  case FunctionFrame(_,_)::fs => {  }
                  case _ => push(new FunctionFrame(node, env))
                }
      
                this.env = newcontext
      
                for (a <- args) { bind(lookup(a)) }
      
                schedule(this.move(body))				  					
              }
              case (s: Site) => {
                val vs = args.partialMap(resolve)
                vs.foreach(invoke(this,s,_))
              }
              case _ => {
                println("You can't call a "+target)
                //TODO:FIXME: throw UncallableValueException
                halt
              }
            })
          }
    
          case Parallel(left, right) => {
            val (l,r) = fork
            schedule(l.move(left), r.move(right))		
          }
    
          case Sequence(left, right) => {
            val frame = new SequenceFrame(right)		  	  
            schedule(this.push(frame).move(left))
          }
    
          case Prune(left, right) => {
            val (l,r) = fork
            val groupcell = Groupcell(group)
            schedule( l.bind(groupcell).move(left),
                r.join(groupcell).move(right) )
          }
    
          case Otherwise(left, right) => {
            val (l,r) = fork
            val region = Region(group, r)
            schedule(l.join(region).move(left))
          }
    
          case DeclareDefs(defs, body) => {
            val cs = defs map ( (d: Def) => new Closure(d) )
            for (c <- cs) { bind(c) }
            for (c <- cs) { c.context = this.env }
            this.move(body).run
          }
        }
      }
    }
  
  }

}
