//
// PrettyPrint.scala -- Scala class/trait/object PrettyPrint
// Project OrcScala
//
// $Id$
//
// Created by dkitchin on Jun 7, 2010.
//
// Copyright (c) 2010 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//
package orc.oil.named

/**
 * 
 * Nicer printing for named OIL syntax trees.
 *
 * @author dkitchin
 */


class PrettyPrint {
  
  import orc.oil.named._
  import scala.collection.mutable._
  
  val vars: Map[TempVar, String] = new HashMap()
  var varCounter : Int = 0
  def newVarName() : String = { varCounter += 1 ; "`t" + varCounter }
  def lookup(temp : TempVar) = vars.getOrElseUpdate(temp, newVarName())
  
  val typevars: Map[TempTypevar, String] = new HashMap()
  var typevarCounter : Int = 0
  def newTypevarName() : String = { typevarCounter += 1 ; "`T" + typevarCounter }
  def lookup(temp : TempTypevar) = typevars.getOrElseUpdate(temp, newVarName())  
  
  


  
  
  def commasep(l : List[NamedAST]): String = l match {
    case Nil => ""
    case x::Nil => reduce(x)
    case x::y => y.foldLeft(reduce(x))({ _ + ", " + reduce(_) }) 
  }
  
  def brack(l : List[NamedAST]): String = "[" + commasep(l) + "]"
  def paren(l : List[NamedAST]): String = "(" + commasep(l) + ")"
    
  def reduce(ast : NamedAST): String = 
    ast match {
      case Stop() => "stop"      
      case Call(target, args, typeargs) => {
        reduce(target) + 
        (typeargs match {
          case Some(ts) => brack(ts)
          case None => ""
        }) +
        paren(args)
      }
      case left || right => "(" + reduce(left) + " | " + reduce(right) + ")"
      case Sequence(left, x, right) => "(" + reduce(left) + " >"+reduce(x)+"> " + reduce(right) + ")"
      case Prune(left, x, right) => "(" + reduce(left) + " <"+reduce(x)+"< " + reduce(right) + ")"
      case left ow right => "(" + reduce(left) + " ; " + reduce(right) + ")"
      case DeclareDefs(defs, body) => (defs map reduce).foldLeft("")({_ + _}) + reduce(body)
      case Def(f, formals, body, typeformals, argtypes, returntype) => {  
        val name = f.optionalName.getOrElse(lookup(f))
        "def " + name + brack(typeformals) + paren(argtypes) + 
        (returntype match {
          case Some(t) => " :: " + reduce(t)
          case None => ""
        }) +
        "\n" +
        "def " + name + paren(formals) + " = " + reduce(body) +
        "\n"
      }
      case HasType(body, expectedType) => "(" + reduce(body) + " :: " + reduce(expectedType) + ")"
      case Constant(v) => v.toString()
      case (x: TempVar) => x.optionalName.getOrElse(lookup(x)) 
      case NamedVar(s) => "{unbound: " + s + "}" 
      case u: TempTypevar => u.optionalName.getOrElse(lookup(u))
      case Top() => "Top"
      case Bot() => "Bot"
      case FunctionType(typeformals, argtypes, returntype) => {
        "lambda" + brack(typeformals) + paren(argtypes) + " :: " + reduce(returntype)
      }
      case TupleType(elements) => paren(elements)
      case TypeApplication(tycon, typeactuals) => reduce(tycon) + brack(typeactuals)
      case AssertedType(assertedType) => reduce(assertedType) + "!"
      case NamedTypevar(s) => "{unbound: " + s + "}"
    }

}
  
