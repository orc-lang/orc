//
// FractionDefs.scala -- Scala object FractionDefs
// Project OrcScala
//
// $Id$
//
// Created by dkitchin on Jul 10, 2010.
//
// Copyright (c) 2011 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//
package orc.compile.optimize

import orc.util.Graph
import orc.util.Node
import orc.util.Direction
import scala.collection.mutable.LinkedList

import orc.ast.oil.named._

/**
 * 
 *
 * @author srosario
 */
object FractionDefs extends NamedASTTransform {
  
    override def onExpression(context: List[BoundVar], typecontext: List[BoundTypevar]) = {
      case DeclareDefs(defs, body) => {
        val newdefs = defs map { transform(_, context, typecontext) }
        val newdefnames = newdefs map { _.name }
        val newdeflists = fraction(newdefs)
        val newbody = transform(body, newdefnames ::: context, typecontext)
        newdeflists.foldRight(newbody) { DeclareDefs }
      }
    }  
  
    /**
     * Divides a list of defs into a list of mutually recursive (sub)lists
     * of defs. The return list is ordered such that no mutually recursive
     * sub-list has references to definitions in the mutually recursive
     * sub-lists that follow it. 
     */
    def fraction(decls: List[Def]): LinkedList[List[Def]] = {
      if(decls.size == 1)
         return new LinkedList(decls,LinkedList.empty)
         
      val nodes = for(d <- decls) yield new Node(d)
      val g = new Graph(nodes)
       
      // Add edges in the graph.
      for{ n1 <- g.nodes 
           n2 <- g.nodes
           if (n1 != n2)
         } {
         val def1 = n1.elem 
         val def2 = n2.elem
         if (def1.freevars contains def2.name) {
           // Add Def (its node) points to other Defs that it refers to
           g.addEdge(n1,n2)
         }
      }

      /* Do a DFS on the graph, ignoring the resulting forest*/
      g.depthSearch(Direction.Forward)
      /* Sort the elements of the graph in decreasing order
       * of their finish times. */
      g.sort 
      /* Do a second DFS, on the complement of the original graph
       * (i.e, do a backward DFS). The result is a topologically
       * sorted collection of mutually recursive definitions */ 
      val forest: LinkedList[List[Node[Def]]] = g.depthSearch(Direction.Backward)
      // Extract the Defs from the Nodes.
      forest map { _ map { _.elem } }
    }
       
    
}
