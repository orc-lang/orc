//
// CompilationExceptions.scala -- Scala child classes of CompilationException
// Project OrcScala
//
// Created by jthywiss on Aug 11, 2010.
//
// Copyright (c) 2017 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//
package orc.error.compiletime

import orc.compile.parse.OrcSourceRange

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
@SerialVersionUID(1039317592598916147L)
abstract class SyntacticException(message: String) extends CompilationException(message)

/** Problem parsing the text of an Orc program. Mostly this
  * is a wrapper around the exceptions thrown by whatever
  * parsing library we use.
  */
@SerialVersionUID(-705986615440958886L)
class ParsingException(val message: String, val errorPos: OrcSourceRange)
  extends SyntacticException(message)
  with SeverityFatal { this.resetPosition(errorPos) }

/** A record expression maps the same key more than once.
  */
@SerialVersionUID(-9117016650562548269L)
case class DuplicateKeyException(val duplicateKey: String)
  extends SyntacticException("Duplicate mapping for key " + duplicateKey + " in record. The rightmost mapping will be used.")
  with SeverityWarning

/** A pattern, or set of pattern arguments, mentions the same variable more than once.
  */
@SerialVersionUID(292803225394024041L)
case class NonlinearPatternException(val repeatedName: String)
  extends SyntacticException("Nonlinear pattern: variable " + repeatedName + " occurs more than once.")
  with SeverityError

/** A call pattern appears as a subpattern of an as pattern.
  */
@SerialVersionUID(3135155295790873092L)
case class CallPatternWithinAsPattern()
  extends SyntacticException("Call pattern occurs within as pattern. This can produce unexpected matching behavior.")
  with SeverityWarning

/** A list of type formals mentions the same variable more than once.
  */
@SerialVersionUID(7694819027283371220L)
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
@SerialVersionUID(2031122784647692321L)
case class ClauseArityMismatch()
  extends SyntacticException("Not all clauses of this function have the same number of arguments.")
  with SeverityFatal

/** A clause can never be reached because the preceding clause matches all possible arguments.
  */
@SerialVersionUID(-629891427544641940L)
case class RedundantMatch()
  extends SyntacticException("Redundant match; this clause can never be reached.")
  with SeverityWarning

/** A clause has redundant type information.
  */
@SerialVersionUID(6431513589901637313L)
abstract class RedundantTypeInformationException(message: String) extends SyntacticException(message) with SeverityWarning
@SerialVersionUID(-7210254063454988009L)
case class RedundantTypeParameters() extends RedundantTypeInformationException("Redundant type parameters")
@SerialVersionUID(-8892724540647226616L)
case class RedundantArgumentType() extends RedundantTypeInformationException("Redundant argument type")
@SerialVersionUID(-8055167575332259977L)
case class RedundantReturnType() extends RedundantTypeInformationException("Redundant return type")
@SerialVersionUID(1850200423303914708L)
case class UnusedFunctionSignature() extends RedundantTypeInformationException("Unused function signature")

/** Call to the Vclock quasi-site (in a sequential combinator) that is malformed
  */
@SerialVersionUID(-2736987424427777920L)
case class IncorrectVclockCall()
  extends SyntacticException("Vclock calls should be of the form: Vclock(SomeTimeOrder) >> e")
  with SeverityError

/** Use of the Vclock quasi-site in an invalid location
  */
@SerialVersionUID(-3646497129797531832L)
case class InvalidVclockUse()
  extends SyntacticException("Vclock can only be used as a call on the left side of a sequential combinator")
  with SeverityError

/** A variable is unbound.
  */
@SerialVersionUID(7607784286527869926L)
case class UnboundVariableException(val varName: String)
  extends SyntacticException("Variable " + varName + " is unbound")
  with SeverityError
@SerialVersionUID(5097600012660280100L)
case class UnboundTypeVariableException(val typevarName: String)
  extends SyntacticException("Type variable " + typevarName + " is unbound")
  with SeverityError
case class UnboundClassVariableException(val clsName: String)
  extends SyntacticException("The class " + clsName + " is undefined")
  with SeverityError

/** The compilation process has produced a malformed expression;
  * this is a fatal internal error, and not the fault of the user.
  */
@SerialVersionUID(8051933862738751228L)
case class MalformedExpression(complaint: String)
  extends SyntacticException(complaint)
  with SeverityInternal

/** Unguarded recursion.
  */
@SerialVersionUID(-4307748302868264406L)
case class UnguardedRecursionException() extends SyntacticException("Unguarded recursion") with SeverityWarning

/** Indicate a problem with include file open/read operations.
  */
@SerialVersionUID(479336004840472249L)
case class IncludeFileException(val includeFileName: String, cause: Throwable)
  extends CompilationException("Problem including " + includeFileName + (if (cause == null) "" else ": " + cause.toString()), cause)
  with SeverityFatal

/** Indicate a problem with site resolution. Ideally
  * this would be a loadtime error, but currently site
  * resolution is done at compile time.
  */
@SerialVersionUID(5710973571979789522L)
case class SiteResolutionException(val siteName: String, cause: Throwable)
  extends CompilationException("Problem loading site " + siteName + (if (cause == null) "" else ": " + cause.toString()), cause)
  with SeverityFatal

/** This Orc program is not valid in class usage.
  */
abstract class ClassException(message: String) extends CompilationException(message)

/** A new expression is instantiating a class with abstract members.
  */
case class InstantiatingAbstractClassException(missingMembers: Iterable[String])
  extends ClassException(s"Instantiating class with abstract members. You need to provide bindings for: ${missingMembers.mkString(", ")}.")
  with SeverityError

/** A constructor is missing types on one of it's arguments.
  */
case class ConstructorArgumentTypeMissingException(className: String, argument: Int)
  extends ClassException(s"Constructor for class $className is missing an explicit type on argument $argument.")
  with SeverityError

/** A constructor is missing a return type.
  */
case class ConstructorReturnTypeMissingException(className: String)
  extends ClassException(s"Constructor for class $className is missing an explicit return type.")
  with SeverityError

/** A with operation is changing the order of methods.
  */
case class ConflictingOrderException(orders: Iterable[Iterable[String]])
  extends ClassException(s"Classes are in different orders in linearizations of mix-ins. The following orders disagree: ${orders.map(_.mkString("<", ", ", ">")).mkString("; ")}")
  with SeverityError

/** A with operation is changing the order of methods.
  */
case class CyclicInheritanceException(classes: Iterable[String])
  extends ClassException(s"Classes have cyclic inheritance involving classes: ${classes.mkString(", ")}")
  with SeverityError

/** A language feature used in the input program is not supported by the backend.
  */
case class FeatureNotSupportedException(feature: String)
  extends CompilationException(s"$feature is unsupported")
  with SeverityFatal

/** Many errors occured during compilation and we want to report them all.
  *
  * This exception will have all the other errors as suppressed exceptions.
  */
class ManyCompilationExceptions(exceptions: Seq[Throwable])
  extends CompilationException("Multiple compiler errors")
  with SeverityFatal {
  exceptions.foreach(addSuppressed)
}
