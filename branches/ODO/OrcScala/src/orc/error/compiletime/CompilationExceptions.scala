//
// CompilationExceptions.scala -- Scala child classes of CompilationException
// Project OrcScala
//
// $Id$
//
// Created by jthywiss on Aug 11, 2010.
//
// Copyright (c) 2015 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//
package orc.error.compiletime

import scala.util.parsing.input.Position

import orc.error.compiletime.CompileLogger.Severity

// Severity marker traits
trait CompilationExceptionSeverity
trait ContinuableSeverity extends CompilationExceptionSeverity
trait HaltingSeverity extends CompilationExceptionSeverity

/** Severity of this exception is internal, for tool debugging -- users don't care. */
trait SeverityDebug extends ContinuableSeverity

/** Severity of this exception is completely routine.  For example, counts of output size. */
trait SeverityInfo extends ContinuableSeverity

/** Severity of this exception is not routine, but not a problem. */
trait SeverityNotice extends ContinuableSeverity

/** Severity of this exception is a potential problem, but not bad enough to cause output to be disregarded -- it may still be usable. */
trait SeverityWarning extends ContinuableSeverity

/** Severity of this exception is a problem that is severe enough that output was discarded or should be discarded -- it is not usable. */
trait SeverityError extends ContinuableSeverity

/** Severity of this exception is a problem that has caused input processing to be stopped. */
trait SeverityFatal extends HaltingSeverity

/** Severity of this exception is an internal failure of the tool (not the user's fault). */
trait SeverityInternal extends HaltingSeverity

/** This Orc program is not syntactically valid.
  */
abstract class SyntacticException(message: String) extends CompilationException(message)

/** Problem parsing the text of an Orc program. Mostly this
  * is a wrapper around the exceptions thrown by whatever
  * parsing library we use.
  */
class ParsingException(val message: String, val errorPos: Position)
  extends SyntacticException(message)
  with SeverityFatal { this.resetPosition(errorPos) }

/** A record expression maps the same key more than once.
  */
case class DuplicateKeyException(val duplicateKey: String)
  extends SyntacticException("Duplicate mapping for key " + duplicateKey + " in record. The rightmost mapping will be used.")
  with SeverityWarning

/** A pattern, or set of pattern arguments, mentions the same variable more than once.
  */
case class NonlinearPatternException(val repeatedName: String)
  extends SyntacticException("Nonlinear pattern: variable " + repeatedName + " occurs more than once.")
  with SeverityError

/** A call pattern appears as a subpattern of an as pattern.
  */
case class CallPatternWithinAsPattern()
  extends SyntacticException("Call pattern occurs within as pattern. This can produce unexpected matching behavior.")
  with SeverityWarning

/** A list of type formals mentions the same variable more than once.
  */
case class DuplicateTypeFormalException(val repeatedName: String)
  extends SyntacticException("Duplicate type formal: " + repeatedName + " occurs more than once.")
  with SeverityError

/** A recursive group of classes contains the same class more than once.
  */
case class DuplicateClassException(val repeatedName: String)
  extends SyntacticException("Duplicate class: " + repeatedName + " occurs more than once in recursive group.")
  with SeverityError

/** A clause of a function has a different number of parameters than
  * earlier clauses.
  *
  * @author dkitchin
  */
case class ClauseArityMismatch()
  extends SyntacticException("Not all clauses of this function have the same number of arguments.")
  with SeverityFatal

/** A clause can never be reached because the preceding clause matches all possible arguments.
  */
case class RedundantMatch()
  extends SyntacticException("Redundant match; this clause can never be reached.")
  with SeverityWarning

/** A clause has redundant type information.
  */
abstract class RedundantTypeInformationException(message: String) extends SyntacticException(message) with SeverityWarning
case class RedundantTypeParameters() extends RedundantTypeInformationException("Redundant type parameters")
case class RedundantArgumentType() extends RedundantTypeInformationException("Redundant argument type")
case class RedundantReturnType() extends RedundantTypeInformationException("Redundant return type")
case class UnusedFunctionSignature() extends RedundantTypeInformationException("Unused function signature")

/** Call to the Vclock quasi-site (in a sequential combinator) that is malformed
  */
case class IncorrectVclockCall()
  extends SyntacticException("Vclock calls should be of the form: Vclock(SomeTimeOrder) >> e")
  with SeverityError

/** Use of the Vclock quasi-site in an invalid location
  */
case class InvalidVclockUse()
  extends SyntacticException("Vclock can only be used as a call on the left side of a sequential combinator")
  with SeverityError

/** A variable is unbound.
  */
case class UnboundVariableException(val varName: String)
  extends SyntacticException("Variable " + varName + " is unbound")
  with SeverityError
case class UnboundTypeVariableException(val typevarName: String)
  extends SyntacticException("Type variable " + typevarName + " is unbound")
  with SeverityError
case class UnboundClassVariableException(val clsName: String)
  extends SyntacticException("The class " + clsName + " is undefined")
  with SeverityError

/** The compilation process has produced a malformed expression;
  * this is a fatal internal error, and not the fault of the user.
  */
case class MalformedExpression(complaint: String)
  extends SyntacticException(complaint)
  with SeverityInternal

/** Unguarded recursion.
  */
case class UnguardedRecursionException() extends SyntacticException("Unguarded recursion") with SeverityWarning

/** Indicate a problem with include file open/read operations.
  */
case class IncludeFileException(val includeFileName: String, cause: Throwable)
  extends CompilationException("Problem including " + includeFileName + (if (cause == null) "" else ": " + cause.toString()), cause)
  with SeverityFatal

/** Indicate a problem with site resolution. Ideally
  * this would be a loadtime error, but currently site
  * resolution is done at compile time.
  */
case class SiteResolutionException(val siteName: String, cause: Throwable)
  extends CompilationException("Problem loading site " + siteName + (if (cause == null) "" else ": " + cause.toString()), cause)
  with SeverityFatal
  
/** This Orc program is not valid in class usage.
  */
abstract class ClassException(message: String) extends CompilationException(message)

/** A new expression is instantiating a class with abstract members.
  * 
  */
case class InstantiatingAbstractClassException(superclasses: Iterable[String], missingMembers: Iterable[String])
  extends ClassException(s"Instantiating class with abstract members. You need to provide bindings for: ${missingMembers.mkString(", ")}. The superclasses are: ${superclasses.mkString(", ")}")
  with SeverityError
  
/** A with operation is changing the order of methods.
  */
case class ConflictingOrderWarning(leftOrder: Iterable[String], rightOrder: Iterable[String])
  extends ClassException(s"Classes are in different orders in linearizations of mix-ins. ${leftOrder.mkString(", ")} is different from ${rightOrder.mkString(", ")}")
  with SeverityWarning
  
