//
// Translator.scala -- Scala object Translator
// Project OrcScala
//
// $Id$
//
// Created by dkitchin on May 27, 2010.
//
// Copyright (c) 2015 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.compile.translate

import scala.language.reflectiveCalls

import scala.collection.immutable.{ HashMap, List, Map, Nil }
import scala.collection.mutable
import orc.ast.ext
import orc.ast.oil._
import orc.ast.oil.named._
import orc.ast.oil.named.Conversions._
import orc.compile.translate.PrimitiveForms.{ callEq, callIft, callIsCons, callIsNil, callRecordMatcher, callTupleArityChecker, makeConditional, makeDatatype, makeLet, makeList, makeNth, makeRecord, makeTuple, makeUnapply }
import orc.error.OrcException
import orc.error.OrcExceptionExtension.extendOrcException
import orc.error.compiletime.{ CallPatternWithinAsPattern, CompilationException, ContinuableSeverity, DuplicateKeyException, DuplicateTypeFormalException, MalformedExpression, NonlinearPatternException, SiteResolutionException }
import orc.lib.builtin
import orc.values.{ Field, Signal }
import orc.values.sites.{ JavaSiteForm, OrcSiteForm }
import orc.ast.ext.ExtendedASTTransform
import orc.error.compiletime.DuplicateClassException

class Translator(val reportProblem: CompilationException with ContinuableSeverity => Unit) {
  /** Translate an extended AST to a named OIL AST.
    */
  def translate(extendedAST: ext.Expression): named.Expression = {
    val rootContext: Map[String, Argument] = HashMap.empty withDefault { UnboundVar(_) }
    val rootTypeContext: Map[String, Type] = HashMap.empty withDefault { UnboundTypevar(_) }
    val rootClassContext: Map[String, ClassInfo] = HashMap.empty
    // TODO: Proper error handling for references to unknown classes.
    convert(extendedAST)(rootContext, rootTypeContext, rootClassContext)
  }
  
  val classForms = new ClassForms(this)

  /** Convert an extended AST expression to a named OIL expression.
    */
  def convert(e: ext.Expression)(implicit context: Map[String, Argument], typecontext: Map[String, Type], classcontext: Map[String, ClassInfo]): Expression = {
    implicit val implicit_self = this
        
    e -> {
      case ext.Stop() => Stop()
      case ext.Constant(c) => Constant(c)
      case ext.Variable(x) => context(x)
      case ext.TupleExpr(es) => unfold(es map convert, makeTuple)
      case ext.ListExpr(es) => unfold(es map convert, makeList)
      case ext.RecordExpr(es) => {
        val seen = new scala.collection.mutable.HashSet[String]()
        val tuples = es map
          {
            case (s, e) => {
              if (seen contains s) { reportProblem(DuplicateKeyException(s) at e) } else { seen += s }
              val f = Constant(Field(s))
              unfold(List(f, convert(e)), makeTuple)
            }
          }
        unfold(tuples, makeRecord)
      }
      case ext.Call(target, gs) => {
        var expr = convert(target)
        for (g <- gs) {
          expr = unfold(List(expr), { case List(m) => convertArgumentGroup(m, g); case _ => throw new AssertionError("Translator internal failure (convert Call arg group match error)") })
        }
        expr
      }
      case ext.PrefixOperator(opName, exp) => {
        val actualOpName = if (opName == "-") "0-" else opName
        val op = context(actualOpName)
        unfold(List(exp) map convert, { Call(op, _, None) })
      }
      case ext.InfixOperator(l, opName, r) => {
        val op = context(opName)
        unfold(List(l, r) map convert, { Call(op, _, None) })
      }

      case ext.Sequential(l, None, r) => convert(l) >> convert(r)
      case ext.Sequential(l, Some(ext.VariablePattern(name)), r) => {
        val x = new BoundVar(Some(name))
        val newl = convert(l)
        val newr = convert(r)(context + ((name, x)), typecontext, implicitly)
        newl > x > newr
      }
      case ext.Sequential(l, Some(p), r) => {
        val x = new BoundVar()
        val (source, dcontext, target) = convertPattern(p, x)
        val newl = convert(l)
        val newr = convert(r)(context ++ dcontext, typecontext, implicitly)
        source(newl) > x > target(newr)
      }
      case ext.Declare(ext.Val(p, f), body) => {
        // Handle graft
        val x = new BoundVar()
        val (source, dcontext, target) = convertPattern(p, x)
        val newbody = convert(body)(context ++ dcontext, typecontext, implicitly)
        val newf = convert(f)
        Graft(x, source(newf), target(newbody))
      }
      case ext.Parallel(l, r) => convert(l) || convert(r)
      case ext.Otherwise(l, r) => convert(l) ow convert(r)
      case ext.Trim(e) => Trim(convert(e))
      case n @ ext.New(e) => {
        classForms.makeNew(n, e)
      }

      case ext.Section(body) => {
        val lambdaName = new BoundVar()
        val removePlaceholders = new ExtendedASTTransform {
          var args = Seq[ext.Pattern]()
          var nextArg = 0

          def handleArgument(p: ext.Placeholder, wrapper: ext.VariablePattern => ext.Pattern) = {
            val argname = s"$$arg$$$nextArg"
            args :+= p ->> wrapper(ext.VariablePattern(argname))
            val r = p ->> ext.Variable(argname)
            nextArg += 1
            r
          }

          override def onExpression() = {
            case p @ ext.Placeholder() => handleArgument(p, x => x)
            case ext.TypeAscription(p @ ext.Placeholder(), t) => handleArgument(p, ext.TypedPattern(_, t))
            case s @ ext.Section(_) => s // Ignore placeholders inside other sections.
          }
        }

        val newBody = removePlaceholders(body)
        val formals = removePlaceholders.args.toList

        val newdef = AggregateDef(formals, newBody)(this).convert(lambdaName, implicitly, implicitly, implicitly)
        DeclareCallables(List(newdef), lambdaName)
      }
      case ext.Conditional(ifE, thenE, elseE) => {
        makeConditional(convert(ifE), convert(thenE), convert(elseE))
      }
      case ext.CallableGroup(defs, body) => {
        val (newdefs, dcontext) = convertDefs(defs)
        val newbody = convert(body)(context ++ dcontext, typecontext, implicitly)
        DeclareCallables(newdefs, newbody)
      }

      case ext.ClassGroup(clss, body) => {
        val (newClss, newClassCtx) = classForms.makeClassGroup(clss)

        DeclareClasses(newClss.toList, convert(body)(implicitly, implicitly, newClassCtx))
      }

      case ext.Declare(si @ ext.SiteImport(name, sitename), body) => {
        try {
          val site = Constant(OrcSiteForm.resolve(sitename))
          site.pushDownPosition(si.pos)
          convert(body)(context + { (name, site) }, typecontext, implicitly)
        } catch {
          case oe: OrcException => throw oe at e
        }
      }
      case ext.Declare(ci @ ext.ClassImport(name, classname), body) => {
        try {
          val u = new BoundTypevar(Some(name))
          u.pushDownPosition(ci.pos)
          val site = Constant(JavaSiteForm.resolve(classname))
          site.pushDownPosition(ci.pos)
          val newbody = convert(body)(context + { (name, site) }, typecontext + { (name, u) }, implicitly)
          DeclareType(u, ClassType(classname), newbody)
        } catch {
          case oe: OrcException => throw oe at e
        }
      }

      case ext.Declare(ext.Include(_, decls), body) => convert((decls foldRight body) { ext.Declare })

      case ext.Declare(ext.TypeImport(name, classname), body) => {
        val u = new BoundTypevar(Some(name))
        val newbody = convert(body)(context, typecontext + { (name, u) }, implicitly)
        DeclareType(u, ImportedType(classname), newbody)
      }

      case ext.Declare(decl @ ext.TypeAlias(name, typeformals, t), body) => {
        val u = new BoundTypevar(Some(name))
        val newtype =
          typeformals match {
            case Nil => convertType(t)
            case _ => {
              val (newTypeFormals, dtypecontext) = convertTypeFormals(typeformals, decl)
              val enclosedType = convertType(t)(typecontext ++ dtypecontext)
              TypeAbstraction(newTypeFormals, enclosedType)
            }
          }
        val newbody = convert(body)(context, typecontext + { (name, u) }, implicitly)
        DeclareType(u, newtype, newbody)
      }

      case ext.Declare(decl @ ext.Datatype(name, typeformals, constructors), body) => {
        val d = new BoundTypevar(Some(name))
        val variantType = {
          val selfVar = new BoundTypevar(Some(name))
          val (newTypeFormals, dtypecontext) = convertTypeFormals(typeformals, decl)
          val newtypecontext = typecontext ++ dtypecontext + { (name, selfVar) }
          val variants =
            for (ext.Constructor(name, types) <- constructors) yield {
              val newtypes = types map {
                case Some(t) => convertType(t)(newtypecontext)
                case None => Top()
              }
              (name, newtypes)
            }
          VariantType(selfVar, newTypeFormals, variants)
        }

        val names = constructors map { _.name }
        /*
         * There is a special case for datatypes with a single constructor. Instead of
         * using a tuple pattern we simply have a variable for the single constructor.
         *
         * This matches the type generated in Datatypes.scala.
         */
        val p = if (names.size == 1) ext.VariablePattern(names.head)
                else ext.TuplePattern(names map { ext.VariablePattern(_) })
        val x = new BoundVar()
        val (source, dcontext, target) = convertPattern(p, x)

        val newbody = convert(body)(context ++ dcontext, typecontext + { (name, d) }, implicitly)
        val makeSites = makeDatatype(d, typeformals.size, constructors, this)

        DeclareType(d, variantType, Graft(x, source(makeSites), target(newbody)))
      }

      case ext.Declare(decl, _) => throw (MalformedExpression("Invalid declaration form") at decl)

      case ext.TypeAscription(body, t) => HasType(convert(body), convertType(t))
      case ext.TypeAssertion(body, t) => HasType(convert(body), AssertedType(convertType(t)))

      case ext.Hole => Hole(context, typecontext)

    }
  }

  def convertArgumentGroup(target: Argument, ag: ext.ArgumentGroup)(implicit context: Map[String, Argument], typecontext: Map[String, Type], classcontext: Map[String, ClassInfo]): Expression = {
    ag match {
      case ext.Args(typeargs, args) => {
        val newtypeargs = typeargs map { _ map convertType }
        unfold(args map convert, { Call(target, _, newtypeargs) })
      }
      case ext.FieldAccess(field) => {
        FieldAccess(target, Field(field))
      }
      case ext.Dereference => {
        Call(context("?"), List(target), None)
      }
    }
  }

  /** Convert a list of extended AST def declarations to:
    *
    *     a list of named OIL definitions
    * and
    *     a mapping from their string names to their new bound names
    */
  def convertDefs(defs: List[ext.CallableDeclaration])(implicit context: Map[String, Argument], typecontext: Map[String, Type], classcontext: Map[String, ClassInfo]): (List[Callable], Map[String, BoundVar]) = {
    var defsMap: Map[String, AggregateDef] = HashMap.empty.withDefaultValue(AggregateDef.empty(this))
    for (d <- defs; n = d.name) {
      defsMap = defsMap + { (n, defsMap(n) + d) }
    }

    // we generate these names beforehand since defs can be bound recursively in their own bodies
    val namesMap: Map[String, BoundVar] = Map.empty ++ (for (name <- defsMap.keys) yield (name, new BoundVar(Some(name))))
    val recursiveContext = context ++ namesMap
    val newdefs = for ((n, d) <- defsMap) yield {
      d ->> d.convert(namesMap(n), recursiveContext, typecontext, classcontext)
    }

    (newdefs.toList, namesMap)
  }

  /** Convert an extended AST type to a named OIL type.
    */
  def convertType(t: ext.Type)(implicit typecontext: Map[String, Type]): named.Type = {
    t -> {
      case ext.TypeVariable(name) => typecontext(name)
      case ext.TupleType(ts) => TupleType(ts map convertType)
      case ext.RecordType(entries) => {
        val newEntries = (HashMap.empty ++ entries) mapValues convertType
        RecordType(newEntries)
      }
      case ext.TypeApplication(name, typeactuals) => {
        TypeApplication(typecontext(name), typeactuals map convertType)
      }
      case ext.LambdaType(typeformals, argtypes, returntype) => {
        val (newTypeFormals, dtypecontext) = convertTypeFormals(typeformals, t)
        val newtypecontext = typecontext ++ dtypecontext
        val newArgTypes = argtypes map { convertType(_)(newtypecontext) }
        val newReturnType = convertType(returntype)(newtypecontext)
        FunctionType(newTypeFormals, newArgTypes, newReturnType)
      }
    }
  }

  /** Convert a list of type formal names to:
    *
    *  A list of bound type formal variables
    * and
    *  A context mapping those names to those vars
    */
  def convertTypeFormals(typeformals: List[String], ast: orc.ast.AST): (List[BoundTypevar], Map[String, BoundTypevar]) = {
    var newTypeFormals: List[BoundTypevar] = Nil
    var formalsMap = new HashMap[String, BoundTypevar]()
    for (name <- typeformals.reverse) {
      if (formalsMap contains name) {
        reportProblem(DuplicateTypeFormalException(name) at ast)
      } else {
        val w = new BoundTypevar(Some(name))
        newTypeFormals = w :: newTypeFormals
        formalsMap = formalsMap + { (name, w) }
      }
    }
    (newTypeFormals, formalsMap)
  }

  type Conversion = Expression => Expression
  val id: Conversion = { e => e }

  /** Convert an extended AST pattern to:
    *
    *     A filtering conversion for the source expression
    * and
    *     A binding conversion for the target expression,
    *     parameterized on the variable carrying the result
    */
  def convertPattern(pat: ext.Pattern, bridge: Argument)(implicit context: Map[String, Argument], typecontext: Map[String, Type]): (Conversion, Map[String, Argument], Conversion) = {

    var bindingMap: mutable.Map[String, BoundVar] = new mutable.HashMap()

    def bind(name: String, x: BoundVar) {
      if (bindingMap contains name) {
        reportProblem(NonlinearPatternException(name) at pat)
      } else {
        bindingMap += { (name, x) }
      }
    }

    def unravel(p: ext.Pattern, focus: BoundVar)(implicit withinAsPattern: Boolean): Conversion = {
      p match {
        case ext.Wildcard() => {
          id
        }
        case ext.ConstantPattern(c) => {
          val b = new BoundVar();
          { callEq(focus, Constant(c)) > b > callIft(b) >> _ }
        }
        case ext.VariablePattern(name) => {
          bind(name, focus)
          id
        }
        case ext.TuplePattern(Nil) => {
          unravel(ext.ConstantPattern(Signal), focus)
        }
        case ext.TuplePattern(List(p)) => {
          unravel(p, focus)
        }
        case ext.TuplePattern(ps) => {
          /* Test that the pattern's size matches the focus tuple's size */
          val tuplesize = Constant(BigInt(ps.size))
          val sizecheck = { callTupleArityChecker(focus, tuplesize) >> _ }

          /* Match each element of the tuple against its corresponding pattern */
          var elements = id
          for ((p, i) <- ps.zipWithIndex) {
            val y = new BoundVar()
            val bindElement: Conversion = { makeNth(focus, i) > y > _ }
            elements = elements compose bindElement compose unravel(p, y)
          }

          sizecheck compose elements
        }
        case ext.ListPattern(Nil) => {
          { callIsNil(focus) >> _ }
        }
        case ext.ListPattern(List(p)) => {
          val consp = ext.ConsPattern(p, ext.ListPattern(Nil))
          unravel(consp, focus)
        }
        case ext.ListPattern(ps) => {
          val seed: ext.Pattern = ext.ListPattern(Nil)
          val folded = (ps foldRight seed)(ext.ConsPattern)
          unravel(folded, focus)
        }
        case ext.ConsPattern(ph, pt) => {
          val y = new BoundVar()
          val p = ext.TuplePattern(List(ph, pt));
          { callIsCons(focus) > y > _ } compose unravel(p, y)
        }
        case ext.RecordPattern(elements) => {
          val y = new BoundVar()
          val (labels, patterns) = elements.unzip
          val p = ext.TuplePattern(patterns);
          { callRecordMatcher(focus, labels) > y > _ } compose unravel(p, y)
        }
        case ext.CallPattern(name, args) => {
          val y = new BoundVar()
          val p = ext.TuplePattern(args)
          val C = context(name)

          if (withinAsPattern) {
            reportProblem(CallPatternWithinAsPattern() at pat)
          }

          { makeUnapply(C, focus) > y > _ } compose unravel(p, y)
        }
        case ext.AsPattern(p, name) => {
          bind(name, focus)
          unravel(p, focus)(true)
        }
        case ext.TypedPattern(p, t) => {
          val T = convertType(t)
          val ascribe: Conversion = { HasType(_, T) }
          ascribe compose unravel(p, focus)
        }
      }
    }

    val sourceVar = new BoundVar()
    val filterInto = unravel(pat, sourceVar)(false)

    bindingMap.values.toList.distinct match {

      case Nil => {
        /* None of the computed results are needed; the pattern had only guards, and no bindings. */

        val sourceConversion: Conversion =
          { _ > sourceVar > filterInto(Constant(Signal)) }

        (sourceConversion, HashMap.empty, id)
      }

      case List(neededResult) => {
        /* Only one result is needed */

        val sourceConversion: Conversion =
          { _ > sourceVar > filterInto(neededResult) }

        val dcontext = HashMap.empty ++ (for ((name, `neededResult`) <- bindingMap) yield { (name, bridge) })

        (sourceConversion, dcontext, id)
      }

      case neededResults => {
        /* More than one result is needed */
        /* Note: This can only occur in a strict pattern.
         * Thus, the source conversion for a non-strict pattern is always the identity function. */
        val sourceConversion: Conversion =
          { _ > sourceVar > filterInto(makeLet(neededResults)) }

        var dcontext: Map[String, Argument] = HashMap.empty
        var targetConversion = id

        for ((r, i) <- neededResults.zipWithIndex) {
          val y = new BoundVar()
          for ((name, `r`) <- bindingMap) {
            dcontext = dcontext + { (name, y) }
          }
          targetConversion = targetConversion compose { makeNth(bridge, i) > y > _ }
        }

        (sourceConversion, dcontext, targetConversion)
      }

    }

  }

}
