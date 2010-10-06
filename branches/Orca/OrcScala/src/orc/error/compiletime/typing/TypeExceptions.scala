//
// TypeExceptions.scala -- Scala class/trait/object TypeExceptions
// Project OrcScala
//
// $Id$
//
// Created by jthywiss on Aug 11, 2010.
//
// Copyright (c) 2010 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//
package orc.error.compiletime.typing

import orc.types.Type
import orc.error.compiletime._

class ArgumentArityException(val arityExpected: Int, val arityReceived: Int) extends
  TypeException("Expected " + arityExpected + " arguments to call, got " + arityReceived + " arguments instead.") with SeverityError

class DefinitionArityException(val arityFromType: Int, val arityFromSyntax: Int) extends
  TypeException("Definition should have " + arityFromType + " arguments according to its type, observed " + arityFromSyntax + " arguments instead.") with SeverityError

class MissingTypeException() extends
  TypeException("Type checker failed: couldn't obtain sufficient type information from a service or value.") with SeverityError

class MultiTypeException(val specificMessages: String) extends
  TypeException("All alternatives for multitype failed to typecheck.\n" + specificMessages) with SeverityError {
  def addAlternative(t: Type, e: TypeException) =
    new MultiTypeException(specificMessages + (t + " failed due to error: \n" + e + "\n"));
}

class SubtypeFailureException(val expectedType: Type, val receivedType: Type) extends
  TypeException("Expected type " + expectedType + " or some subtype, found type " + receivedType + " instead.") with SeverityError

class TypeArityException(val arityExpected: Int, val arityReceived: Int) extends
  TypeException("Expected " + arityExpected + " arguments to type instantiation, got " + arityReceived + " arguments instead.") with SeverityError

class UnboundTypeException(val typeName: String) extends
  TypeException("Type " + typeName + " is undefined") with SeverityError

class UncallableTypeException(val t: Type) extends
  TypeException("Type " + t + " cannot be called as a service or function.") with SeverityError

class UnrepresentableTypeException(val t: Type) extends
  TypeException(t.toString() + " has no concrete syntax.") with SeverityError

class UnspecifiedArgTypesException() extends
  TypeException("Could not perform type check due to missing argument types; please add argument type annotations") with SeverityError

class UnspecifiedReturnTypeException() extends
  TypeException("Could not perform type check due to missing return type; please add a return type annotation") with SeverityError
