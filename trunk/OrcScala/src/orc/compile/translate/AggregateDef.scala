//
// AggregateDef.scala -- Scala class AggregateDef
// Project OrcScala
//
// $Id$
//
// Created by dkitchin on Jun 3, 2010.
//
// Copyright (c) 2010 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.compile.translate

import orc.PartialMapExtension._
import orc.compile.ext._
import orc.oil.named

class AggregateDef(clauses: List[Clause], 
		           typeformals: Option[List[String]], 
		           argtypes: Option[List[Type]], 
		           returntype: Option[Type]) extends orc.AST {
	
		def unify[A](x: Option[A], y: Option[A], err: => Nothing): Option[A] = 
		  (x,y) match {
			case (None, None) => None
			case (Some(x), None) => Some(x)
			case (None, Some(y)) => Some(y)
			case (Some(x), Some(y)) => err
		  }
	
		def +(defn: DefDeclaration): AggregateDef =
		  defn -> {
            case Def(_, List(formals), body, maybeReturnType) => {
              val (newformals, maybeArgTypes) = AggregateDef.formalsPartition(formals)
              val newclause = defn ->> Clause(newformals, body)
              val newArgTypes = unify(argtypes, maybeArgTypes, defn !! "Redundant argument typing")
              val newReturnType = unify(returntype, maybeReturnType, defn !! "Redundant return typing")
              new AggregateDef(newclause::clauses, typeformals, newArgTypes, newReturnType)
            }
            case DefCapsule(name, List(formals), body, maybeReturnType) => {
              this + Def(name, List(formals), new Capsule(body), maybeReturnType)
            }
            case DefSig(_, typeformals2, argtypes2, maybeReturnType) => {
              val argtypes3 = argtypes2 head // List[List[Type]] has only one entry
              val newTypeFormals = unify(typeformals, Some(typeformals2), defn !! "Redundant type parameters")
              val newArgTypes = unify(argtypes, Some(argtypes3), defn !! "Redundant argument typing")
              val newReturnType = unify(returntype, maybeReturnType, defn !! "Redundant return typing")
              new AggregateDef(clauses, newTypeFormals, newArgTypes, newReturnType)
            }
		  }
		
		def +(lambda: Lambda): AggregateDef = {
			val (newformals, maybeArgTypes) = AggregateDef.formalsPartition(lambda.formals.head)
			val newclause = lambda ->> Clause(newformals, lambda.body)
			val newArgTypes = unify(argtypes, maybeArgTypes, lambda !! "Redundant argument typing")
			val newReturnType = unify(returntype, lambda.returntype, lambda !! "Redundant return typing")
			new AggregateDef(newclause::clauses, typeformals, newArgTypes, newReturnType)
		}
			
		def convert(x : named.TempVar): named.Def = {
			if (clauses.isEmpty) { this !! "Unused function signature" }
			val (newformals, newbody) = Clause.convertClauses(clauses)
			
			val getTypeFormals = typeformals.getOrElse(Nil)
			val getArgTypes = argtypes.getOrElse(this !! "Missing argument types")
			val getReturnType = returntype

			val newTypeFormals = getTypeFormals map { _ => new named.TempTypevar() } 
			// FIXME: the type formals should be substituted into these types,
			//        and into the new def body as well.
			val newArgTypes = getArgTypes map Translator.convertType
			val newReturnType = getReturnType map Translator.convertType
			 
			named.Def(x, newformals, newbody, newTypeFormals, newArgTypes, newReturnType)
		}
			
		
		
		def capsuleCheck {
		  var existsCapsule = false
		  var existsNotCapsule = false
		    for (aclause <- clauses) {
			  aclause match {
			    case Clause(_, Capsule(_)) => 
				  if (existsNotCapsule) { aclause !! "This function is not declared as a capsule." } 
				  else existsCapsule = true
			    case _ => 
				  if (existsCapsule) { aclause !! "This function is already declared as a capsule."}
				  else existsNotCapsule = true
              }
            }
	    }
		
}

object AggregateDef {
	
	def formalsPartition(formals: List[Pattern]): (List[Pattern], Option[List[Type]]) = {
		val maybePartitioned = 
			formals partialMap {
				case TypedPattern(p, t) => Some(p, t)
				case _ => None
			}
		maybePartitioned match {
			case Some(l) => {
				val (ps, ts) = List unzip l
				(ps, Some(ts))
			}
			case None => (formals, None) 
		}
	}
	
	val empty = new AggregateDef(Nil, None, None, None)
	
	def apply(defn : DefDeclaration) = defn -> {empty + _}
	def apply(lambda: Lambda) = lambda -> {empty + _}
	
}



