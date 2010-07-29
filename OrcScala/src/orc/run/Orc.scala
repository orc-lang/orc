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

package orc.run

import orc.OrcRuntime
import orc.TokenAPI
import orc.OrcEvent
import orc.PublishedEvent
import orc.HaltedEvent
import orc.CaughtEvent
import orc.OrcOptions
import orc.oil._
import orc.oil.nameless._
import orc.values.sites.Site
import orc.values.OrcValue
import orc.values.Closure
import orc.error.OrcException
import orc.error.runtime.TokenException
import orc.error.runtime.UncallableValueException
import orc.error.runtime.ArityMismatchException

import scala.collection.mutable.Set   

import scala.concurrent._

    
trait Orc extends OrcRuntime {
  
  def run(node: Expression, k: OrcEvent => Unit, options: OrcOptions) {
    val exec = new Execution(k)
    val t = new Token(node, exec)
    schedule(t)
  }
  
  def stop = { /* By default, do nothing on stop. */ }
  
  
  // Groups //
  
  // A Group is a structure associated with dynamic instances of an expression,
  // tracking all of the executions occurring within that expression.
  // Different combinators make use of different Group subclasses.
  
  object GroupMemberState extends Enumeration {
    type GroupMemberState = Value
    val Killed, Alive = Value
  }
  //Don't import GroupMemberState._ because of name conflicts
  
  sealed trait GroupMember {
    var gmstate = GroupMemberState.Alive
    def kill: Unit
    def notify(event: OrcEvent): Unit
  }

  import scala.actors.Actor
  import scala.actors.Actor._
    
  object Killer extends Actor {
    def act() {
      loop {
        react {
          case x:GroupMember => x.kill
        }
      }
    }    
  }

  trait Group extends GroupMember {
  
    def publish(t: Token, v: AnyRef): Unit 
    def onHalt: Unit 
  
    var members: Set[GroupMember] = Set()
  
    def halt(t: Token) = synchronized { remove(t) }
    
    /* Note: this is _not_ lazy termination */
    def kill = synchronized { 
      if (gmstate == GroupMemberState.Alive) {
        gmstate = GroupMemberState.Killed; 
        for (m <- members) Killer ! m
      }
    } 
    
  
    def add(m: GroupMember) = synchronized { members.add(m) }
  
    def remove(m: GroupMember) = synchronized { 
      members.remove(m)
      if (members.isEmpty) { onHalt }
    }
    
    def inhabitants: List[Token] = 
      members.toList flatMap {
        case t: Token => List(t) 
        case g: Group => g.inhabitants
      }
    
    /* Find the root of this group tree. By default, a group is its own root. */
    def root: Group = this
  
  }
  
  abstract class Subgroup(parent: Group) extends Group {
    
    override def kill = synchronized { super.kill ; parent.remove(this) }
    def notify(event: OrcEvent) = parent.notify(event)

    override def root = parent.root
    
    parent.add(this)
    
  }
  
  
  // A Groupcell is the group associated with expression g in (f <x< g)
  
  // Possible states of a Groupcell
  class GroupcellState
  case class Unbound(waitlist: List[Option[AnyRef] => Unit]) extends GroupcellState
  case class Bound(v: AnyRef) extends GroupcellState
  case object Dead extends GroupcellState
  
  class Groupcell(parent: Group) extends Subgroup(parent) {

    var state: GroupcellState = Unbound(Nil) 
  
    def publish(t: Token, v: AnyRef) = synchronized {
      state match {
        case Unbound(waitlist) => {
          state = Bound(v)
          t.halt
          this.kill
          for (k <- waitlist) { k(Some(v)) }
        }
        case _ => t.halt    
      }
    }
    
    def onHalt = synchronized  {
      state match {
        case Unbound(waitlist) => {
          state = Dead
          parent.remove(this)
          for (k <- waitlist) { k(None) }     
        }
        case _ => {  }
      }
    }
    
    // Specific to Groupcells
    def read(k: Option[AnyRef] => Unit) = synchronized {
      state match {
        case Unbound(waitlist) => {
          state = Unbound(k :: waitlist)
        }
        case Bound(v) => k(Some(v))
        case Dead => k(None)
      }
    }
    
  }
  
  
  // A Region is the group associated with expression f in (f ; g)
  class Region(parent: Group, t: Token) extends Subgroup(parent) {
  
    
    /* Some(t): No publications have left this region.
     *          If the group halts silently, t will be scheduled.
     *
     *    None: One or more publications has left this region.
     */
    var pending: Option[Token] = Some(t)
  
    def publish(t: Token, v: AnyRef) = synchronized {
      pending foreach { _.halt }
      t.migrate(parent).publish(v)
    }
    
    def onHalt = synchronized {
      pending foreach { schedule(_) }
      parent.remove(this)
    }
    
  } 
    
  // An execution is a special toplevel group, 
  // associated with the entire program.
  class Execution(k: OrcEvent => Unit) extends Group {
  
    def publish(t: Token, v: AnyRef) = synchronized {
      k(PublishedEvent(v))
      t.halt
    }
  
    def onHalt = synchronized {
      k(HaltedEvent)
    }

    def notify(event: OrcEvent) {
        k(event)
    }

  }
  
  
  
  
  // Bindings //
  trait Binding
  case class BoundValue(v: AnyRef) extends Binding
  case class BoundCell(g: Groupcell) extends Binding
  case class BoundDefs(defs: List[Def], pos: Int, lexicalContext: List[Binding]) extends Binding 
  
  // Frames //
  abstract class Frame {
    def apply(t: Token, v: AnyRef): Unit
  }
  
  case class BindingFrame(n: Int) extends Frame {
    def apply(t: Token, v: AnyRef) {
      t.env = t.env.drop(n)
      t.publish(v)
    }
  }
  
  case class SequenceFrame(node: Expression) extends Frame {
    def apply(t: Token, v: AnyRef) {
      schedule(t.bind(BoundValue(v)).move(node))
    }
  }
  
  case class FunctionFrame(callpoint: Expression, env: List[Binding]) extends Frame {
    def apply(t: Token, v: AnyRef) {      
      t.env = env
      t.move(callpoint).publish(v)
    }
  }
  
  case object GroupFrame extends Frame {
    def apply(t: Token, v: AnyRef) {
      t.group.publish(t,v)
    }
  }
  
  
  
  
  // Tokens //
  
  sealed trait TokenState
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
    
    // Public constructor
    def this(start: Expression, g: Group) = {
      this(node = start, group = g, stack = List(GroupFrame))
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
    
    // A live token is added to its group when it is created
    state match {
      case Live => group.add(this)
      case Halted => {  }
      case Killed => {  }
    }
    
    def notify(event: OrcEvent) { group.notify(event) }
  
    def kill {
      state match {
        case Live => { 
          state = Killed
          group.halt(this) 
        }
        case _ => {  }
      }
    }
    
    def fork = (this, copy())
  
    def move(e: Expression) = { node = e ; this }
  
    def push(f: Frame) = { stack = f::stack ; this }
  
    def migrate(newGroup: Group) = { 
      val oldGroup = group
      newGroup.add(this); oldGroup.remove(this)
      group = newGroup
      this 
    }
    
    def join(newGroup: Group) = { this.push(GroupFrame).migrate(newGroup) }
      
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
        case Constant(v) => BoundValue(v)
        case Variable(n) => env(n)
      }
  
    /* Attempt to resolve a binding to a value.
     * When the binding resolves to v, call k(v).
     * (If it is already resolved, k is called immediately)
     */
    def resolve(b: Binding)(k : AnyRef => Unit) {
      b match {
        case BoundValue(v) => k(v)
        case BoundCell(g) => {
          g read { 
            case Some(v) => k(v)
            case None => halt
          }
        }
        case BoundDefs(defs, pos, lexicalContext) => {
          resolve(lexicalContext) {
            valueContext: List[AnyRef] => k( Closure(defs, pos, valueContext) ) 
          }         
        }
      }
    }
  
    /* Attempt to resolve a list of bindings.
     * When all of the bindings are resolved, call k(vs).
     * (If they are all already resolved, k is called immediately) 
     */
    def resolve(bs: List[Binding])(k : List[AnyRef] => Unit) {
      def resolveLeftToRight(bs : List[Binding], vs : List[AnyRef]): Unit =
        bs match {
          case z::zs => resolve(z) { v : AnyRef => resolveLeftToRight(zs, v::vs) }
          case Nil => k(vs.reverse)
        }
      resolveLeftToRight(bs, Nil)
    }
       
    def functionCall(d: Def, context: List[Binding], params: List[Binding]) {  
      if (params.size != d.arity) {
        this !! new ArityMismatchException(d.arity, params.size) /* Arity mismatch. */
      }
            
      /* 1) If this is not a tail call, push a function frame referring to the current environment.
       * 2) Change the current environment to the closure's saved environment.
       * 3) Add bindings for the arguments to the new current environment.
       * 
       * Caution: The ordering of these operations is very important;
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
          
      /* Jump into the function context */
      this.env = context
                
      /* Bind the args */
      for (p <- params) { bind(p) }
                    
      /* Jump into the function body */
      schedule(this.move(d.body))                                   

    }
  
    def run {
      if (state == Live) {
        node match {
          case Stop() => halt
          case (a: Argument) => resolve(lookup(a)) { publish }
          case Call(target, args, _) => {
            val params = args map lookup
            lookup(target) match {
              case BoundDefs(defs, pos, lexicalContext) => {
                val fs = 
                  for (i <- defs.indices) yield {
                    BoundDefs(defs, i, lexicalContext)
                  }
                val context = fs.toList.reverse ::: lexicalContext
                functionCall(defs(pos), context, params)
              }
              case y => 
                resolve(y) {
                  case Closure(defs, pos, valueContext) => {
                    val fs = 
                      for (i <- defs.indices) yield {
                        BoundValue( Closure(defs, i, valueContext) )
                      }
                    val context = fs.toList.reverse ::: ( valueContext map { BoundValue(_) } )
                    functionCall(defs(pos), context, params)
                  }
                  case v => 
                    resolve(params) { vs =>
                      try {
                        invoke(this, v, vs)
                      }
                      catch {
                        case e: OrcException => this !! e
                        case e => { halt ; notify(CaughtEvent(e)) }
                      }
                    }
                }
            }
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
            val groupcell = new Groupcell(group)
            schedule( l.bind(BoundCell(groupcell)).move(left),
                      r.join(groupcell).move(right) )
          }
    
          case Otherwise(left, right) => {
            val (l,r) = fork
            val region = new Region(group, r.move(right))
            schedule(l.join(region).move(left))
          }
    
          case decldefs@ DeclareDefs(openvars, defs, body) => {
            /* Closure compaction: Bind only the free variables
             * of the defs in this lexical context. 
             */
            val lexicalContext = openvars map { i:Int => lookup(Variable(i)) }
            for (i <- defs.indices) { 
              bind(BoundDefs(defs, i, lexicalContext)) 
            }
            schedule(this.move(body))
          }
          case HasType(expr, _) => this.move(expr).run
          case DeclareType(_, expr) => this.move(expr).run
        }
      }
    }
  
    
    
    
    
       
    // Publicly accessible methods
  
    def publish(v: AnyRef) {
      if (state == Live) {
        stack match {
          case f::fs => { 
            stack = fs
            f(this, v)
          }
          case List() => {
            // TODO: What should we do in this case
          }
        }
      } 
    }
  
    def halt {
      state match {
        case Live => { 
          state = Halted 
          group.halt(this) 
        }
        case _ => {  }
      }
    }
    
    override def !!(e: OrcException) { 
      e.setPosition(node.pos)
      e match {
        case te: TokenException if (te.getBacktrace() == null || te.getBacktrace().length == 0) => {
          val callPoints = stack collect { case f : FunctionFrame => f.callpoint.pos }
          te.setBacktrace(callPoints.toArray)
        }
        case _ => { }
      }
      notify(CaughtEvent(e))
      halt
    }
  
    val runtime = Orc.this
    
  } // end of Token

} // end of Orc
