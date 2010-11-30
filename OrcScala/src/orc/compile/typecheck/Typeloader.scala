//
// Typeloader.scala -- Scala class/trait/object Typeloader
// Project OrcScala
//
// $Id$
//
// Created by dkitchin on Nov 29, 2010.
//
// Copyright (c) 2010 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//
package orc.compile.typecheck

import orc.ast.oil.{named => syntactic}
import orc.types._
import orc.values.sites.SiteClassLoading
import orc.compile.typecheck.Typechecker._
import orc.types.Variance._
import orc.error.compiletime.typing._
import orc.error.NotYetImplementedException


/** 
 * Lift syntactic types to semantic types, or reify
 * semantic types to syntactic types.
 *
 * @author dkitchin
 */
object Typeloader extends SiteClassLoading {


  
  /**
   * Lift a syntactic type to a first-order semantic type.
   */
  def lift(t: syntactic.Type)(implicit typeContext: TypeContext, typeOperatorContext: TypeOperatorContext): Type = {
    t match {
      case u: syntactic.BoundTypevar => typeContext(u)
      case syntactic.Top() => Top
      case syntactic.Bot() => Bot
      case syntactic.TupleType(elements) => TupleType(elements map lift)
      case syntactic.RecordType(entries) => RecordType(entries mapValues lift)
      case syntactic.AssertedType(assertedType) => lift(assertedType) 
      case syntactic.FunctionType(typeFormals, argTypes, returnType) => {
        val newTypeFormals = typeFormals map { u => new TypeVariable(u) }
        val typeBindings = typeFormals zip newTypeFormals
        val newTypeContext = typeContext ++ typeBindings
        val newArgTypes = argTypes map { lift(_)(newTypeContext, typeOperatorContext) }
        val newReturnType = lift(returnType)(newTypeContext, typeOperatorContext)
        FunctionType(newTypeFormals, newArgTypes, newReturnType)
      }
      case syntactic.TypeApplication(t, typeactuals) => {
        val operator = liftToOperator(t)
        operator(typeactuals map lift)
      }
      case syntactic.ImportedType(classname) => loadType(classname)
      case syntactic.ClassType(classname) => throw new NotYetImplementedException()
      case syntactic.VariantType(Nil, variants) => throw new NotYetImplementedException()
      case syntactic.UnboundTypevar(name) => {
        throw new UnboundTypeException(name)
      }
      case _ => throw new FirstOrderTypeExpectedException()
    }
  }
  
  /**
   * Lift a syntactic type to a second-order semantic type.
   */ 
  def liftToOperator(t: syntactic.Type)(implicit typeContext: TypeContext, typeOperatorContext: TypeOperatorContext): TypeOperator = {
    t match {
      case u: syntactic.BoundTypevar => typeOperatorContext(u)
      case syntactic.VariantType(typeformals, variants) if (!typeformals.isEmpty) => {
        val newTypeFormals = typeformals map { u => new TypeVariable(u) }
        val typeBindings = typeformals zip newTypeFormals
        val newTypeContext = typeContext ++ typeBindings
        val newVariants = 
          for ((name, variant) <- variants) yield {
            val newVariant = variant map { lift(_)(newTypeContext, typeOperatorContext) }
            (name, newVariant)
          }
        val tycon = DatatypeConstructor(newTypeFormals, newVariants)
        new TypeOperator({ TypeInstance(tycon, _) })
      }
      case syntactic.TypeAbstraction(typeformals, t) => {
        val newTypeFormals = typeformals map { u => new TypeVariable(u) }
        val typeBindings = typeformals zip newTypeFormals
        val newTypeContext = typeContext ++ typeBindings
        val newT = lift(t)(newTypeContext, typeOperatorContext)
        new TypeOperator({ newT.subst(_, newTypeFormals) })
      }
      case syntactic.ImportedType(classname) => {
        loadTypeOperator(classname)
      }
      case syntactic.ClassType(classname) => throw new NotYetImplementedException()
      case syntactic.UnboundTypevar(name) => {
        throw new UnboundTypeException(name)
      }
      case _ => {
        throw new SecondOrderTypeExpectedException()
      }
    }
  }
    
  /**
   * Lift a syntactic type to either a first or second order type;
   * the kind is not known in advance.
   */
  def liftEither(t: syntactic.Type)(implicit typeContext: TypeContext, typeOperatorContext: TypeOperatorContext): Either[Type, TypeOperator] = {
    try {
      Left(lift(t))
    }
    catch {
      case _ : FirstOrderTypeExpectedException => {
        Right(liftToOperator(t))
      }
    }
  }
  
  
  
  /** 
   * This function converts a semantic type back to a syntactic type.
   * 
   * The result is optional, since some semantic types will have
   * no syntactic representation.
   * 
   * This conversion performs reverse lookups on typing contexts
   * to find the syntactic type variable which originally bound
   * the semantic type. If the result is not unique, reification fails.
   * 
   * @author dkitchin
   */
  def reify(that: Type)(implicit typecontext: TypeContext): Option[syntactic.Type] = {
    None
  }
  
  
  def loadType(name: String): Type = {
    val loadedClass = loadClass(name)
    if (classOf[Type] isAssignableFrom loadedClass) {
      try {
        return loadedClass.asInstanceOf[Class[Type]].newInstance()
      } 
      catch {
        case e => throw new TypeResolutionException(loadedClass.getName(), e)
      }
    } 
    else {
      try { // Maybe it's a Scala object....
        val loadedClassCompanion = loadClass(name+"$")
        return loadedClassCompanion.getField("MODULE$").get(null).asInstanceOf[Type]
      } 
      catch {
        case _ => { } //Ignore -- It's not a Scala object, then.
      }
      throw new TypeResolutionException(loadedClass.getName(),new ClassCastException(loadedClass.getClass().getCanonicalName()+" cannot be cast to "+classOf[Type].getCanonicalName()))
    }
  }
  
  def loadTypeOperator(name: String): TypeOperator = {
    val loadedClass = loadClass(name)
    if (classOf[TypeOperator] isAssignableFrom loadedClass) {
      try {
        return loadedClass.asInstanceOf[Class[TypeOperator]].newInstance()
      } 
      catch {
        case e => throw new TypeOperatorResolutionException(loadedClass.getName(), e)
      }
    } 
    else {
      try { // Maybe it's a Scala object....
        val loadedClassCompanion = loadClass(name+"$")
        return loadedClassCompanion.getField("MODULE$").get(null).asInstanceOf[TypeOperator]
      } 
      catch {
        case _ => { } //Ignore -- It's not a Scala object, then.
      }
      throw new TypeOperatorResolutionException(loadedClass.getName(),new ClassCastException(loadedClass.getClass().getCanonicalName()+" cannot be cast to "+classOf[TypeOperator].getCanonicalName()))
    }
  }
  
  
}