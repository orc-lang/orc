//
// Transformer.scala -- Scala class/trait/object Transformer
// Project OrcScala
//
// $Id$
//
// Created by srosario on May 31, 2010.
//
// Copyright (c) 2010 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.oil.nameless

/**
 * Object that performs transformations on Nameless expressions. 
 *
 * @author srosario
 */
object OilTransformer {
  
  /* Does closure compaction on the definitions of the input OIL expression. */
  def compactClosures(e:Expression): Expression = compactClosures(e,0)
  
  private def compactClosures(e: Expression,depth: Int): Expression = {
    e match {
      case Stop => Stop
      case Constant(c) => Constant(c)
      case Variable(i) => Variable(i)
      case Call(target, args, typeArgs) => Call(target,args,typeArgs)  
      case f || g => compactClosures(f,depth)   || compactClosures(g,depth)
      case f >> g => compactClosures(f,depth)   >> compactClosures(g,depth+1)
      case f << g => compactClosures(f,depth+1) << compactClosures(g,depth)
      case f ow g => compactClosures(f,depth)   ow compactClosures(g,depth)
      case declDefs@ DeclareDefs(defs, body) => {
        val newdepth = depth + defs.size
        
        def f(d: Def) = {
          val Def(t,arity,body,a,r) = d
          val ndepth = newdepth+arity
          val nbody = compactClosures(body,ndepth)
          val nfrees = incrList(declDefs.freeVarList,defs.size+arity)
          d.altBody = substitute(nfrees,nbody,ndepth)
          d
        }
        
        val newdefs = defs.map(f)
        val newbody = compactClosures(body,newdepth)
        DeclareDefs(newdefs,newbody) 
      }
      case HasType(body,typ) => HasType(compactClosures(body,depth),typ)
    }
  }
  
  /* 
   * Replace the free variables {i,j,..} in the body,
   * by {depth+(n-1),depth+(n-2),...,depth}, where n
   * is the size of the set {i,j...}.
   */
  def substitute(freevars:List[Int],body:Expression, depth:Int): Expression = {
    body match {
      case Stop => Stop
      case Constant(c) => Constant(c)
      case Variable(i) => {
        if(freevars.contains(i)) {
          val newIdx = depth+(freevars.size-1)-freevars.indexOf(i)
          Variable(newIdx)
        } else
          Variable(i)
      }
      case Call(target, args, typeArgs) => {
        def substArg(f:List[Int],a:Argument, d:Int): Argument = {
          substitute(f,a,d).asInstanceOf[Argument]
        }
        
        val newtarget = substArg(freevars,target,depth)
        val newargs = args.map(substArg(freevars,_,depth))
        Call(newtarget,newargs,typeArgs)  
      }
      case f || g => substitute(freevars,f,depth) || substitute(freevars,g,depth)
      case f >> g => {
        val newf = substitute(freevars,f,depth) 
        val newg = substitute(incrList(freevars,1),g,depth+1)
        newf >> newg
      }
      case f << g => {
        val newf = substitute(incrList(freevars,1),f,depth+1)
        val newg = substitute(freevars,g,depth) 
        newf << newg
      }
      case f ow g => substitute(freevars,f,depth) ow substitute(freevars,g,depth)
      case DeclareDefs(defs, body) => {
        val ndepth = depth + defs.size
        val nfrees = incrList(freevars,defs.size)
        
        def f(d: Def) = {
          val Def(t,arity,body,a,r) = d
          val sbody = substitute(incrList(nfrees,arity),body,ndepth+arity)
          Def(t,arity,sbody,a,r)
        }
        
        val newdefs = defs.map(f)
        val newbody = substitute(nfrees,body,ndepth)  
        DeclareDefs(newdefs,newbody)
      }
      case HasType(body,typ) => HasType(substitute(freevars,body,depth),typ)
    }
  }
  
  private def shift(indices: List[Int], n: Int): List[Int] = {
    for (i <- indices if i >= n) yield i-n
  }
  
  private def incrList(list:List[Int],incr:Int): List[Int] = {
    list.map(_+incr)
  }
}