//
// Translator.scala -- Scala object Translator
// Project OrcScala
//
// Created by dkitchin on May 27, 2010.
//
// Copyright (c) 2017 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.compile.translate

import scala.collection.immutable.{ HashMap, List, Map, Nil }
import scala.collection.mutable
import scala.language.reflectiveCalls
import orc.ast.ext
import orc.ast.ext.ExtendedASTTransform
import orc.ast.oil.named._
import orc.ast.oil.named.Conversions._
import orc.compile.translate.PrimitiveForms._
import orc.error.OrcException
import orc.error.OrcExceptionExtension._
import orc.error.compiletime._
import orc.values.{ Field, Signal }
import orc.values.sites.{ JavaSiteForm, OrcSiteForm }
import orc.error.compiletime.{ CallPatternWithinAsPattern, CompilationException, ContinuableSeverity, DuplicateKeyException, DuplicateTypeFormalException, MalformedExpression, NonlinearPatternException }
import orc.ast.hasOptionalVariableName._

case class TranslatorContext(context: Map[String, Argument], typecontext: Map[String, Type],
  boundDefs: Set[BoundVar], classContext: Map[String, ClassInfo]) {
  lazy val classesByPlaceholder = classContext.values.flatMap(i => Seq((i.constructorPlaceholderName, i), (i.partialConstructorPlaceholderName, i))).toMap
}

class Translator(val reportProblem: CompilationException with ContinuableSeverity => Unit) {
  /** Translate an extended AST to a named OIL AST.
    */
  def translate(extendedAST: ext.Expression): Expression = {
    val rootContext: Map[String, Argument] = HashMap.empty withDefault { UnboundVar(_) }
    val rootTypeContext: Map[String, Type] = HashMap.empty withDefault { UnboundTypevar(_) }
    val rootClassContext: Map[String, ClassInfo] = HashMap.empty
    // TODO: Proper error handling for references to unknown classes.
    convert(extendedAST)(TranslatorContext(rootContext, rootTypeContext, Set(), rootClassContext))
  }

  val classForms = new ClassForms(this)

  /** Convert an extended AST expression to a named OIL expression.
    */
  def convert(e: ext.Expression)(implicit ctx: TranslatorContext): Expression = {
    import ctx._

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
          expr = unfold(List(expr), { 
            case List(m) => convertArgumentGroup(m, g)
            case _ => throw new AssertionError("Translator internal failure (convert Call arg group match error)") 
          })
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
        val newr = convert(r)(ctx.copy(context = context + ((name, x))))
        newl > x > newr
      }
      case ext.Sequential(l, Some(p), r) => {
        val x = new BoundVar(Some(id"$p"))
        val (source, dcontext, target) = convertPattern(p, x)
        val newl = convert(l)
        val newr = convert(r)(ctx.copy(context = context ++ dcontext))
        source(newl) > x > target(newr)
      }
      case ext.Declare(ext.Val(p, f), body) => {
        // Handle graft
        val x = new BoundVar(Some(id"$p"))
        val (source, dcontext, target) = convertPattern(p, x)
        val newbody = convert(body)(ctx.copy(context = context ++ dcontext))
        val newf = convert(f)
        Graft(x, source(newf), target(newbody))
      }
      case ext.Parallel(l, r) => convert(l) || convert(r)
      case ext.Otherwise(l, r) => convert(l) ow convert(r)
      case ext.Trim(e) => Trim(convert(e))
      case n @ ext.New(e) => {
        classForms.makeNew(e)
      }

      case ext.Section(body) => {
        // TODO: Add support for types?
        val lambdaName = new BoundVar(Some(id"lambda"))
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

        val newdef = AggregateDef(formals, newBody)(this).convert(lambdaName, ctx)
        DeclareCallables(List(newdef), lambdaName)
      }
      case lambda: ext.Lambda => {
        val lambdaName = new BoundVar(Some(id"lambda"))
        // TODO: Reintroduce support for types.
        val newdef = AggregateDef(lambda.formals, lambda.body)(this).convert(lambdaName, ctx)
        DeclareCallables(List(newdef), lambdaName)
      }

      case ext.Conditional(ifE, thenE, elseE) => {
        makeConditional(convert(ifE), convert(thenE), convert(elseE))
      }

      case ext.ClassDefGroup(clss, defs, body) => {
        val infos = classForms.makeClassInfos(clss)
        val clsContext = classForms.makeClassContext(infos)(ctx)
        val (newContext, defsIntermediate, defNames) = makeCallablesContext(defs)(clsContext)
        //Logger.info(s"Processing ${clss.map(_.name)} and ${defs.map(_.name)}:\n$newContext")
        val (newClsDefs, newTypes, newContext2) = classForms.makeClassDeclarations(infos)(newContext)
        val newDefDefs = makeCallables(defsIntermediate, defNames)(newContext2)
        val newDefs = newClsDefs ++ newDefDefs
        //Logger.info(s"Processing ${clss.map(_.name)} and ${defs.map(_.name)}:\n${newDefs.map(_.name)}")
        val newBody = convert(body)(newContext2)
        val core = DeclareCallables(newDefs, newBody)
        newTypes.foldRight(core : Expression) { (p, acc) =>
          val (tv, t) = p
          DeclareType(tv, t, acc)
        }
      }

      // TODO: Add recursion of sites with defs/classes

      case ext.CallableGroup(defs, body) => {
        // Defs should already have been handled above.
        assert(defs.head.isInstanceOf[ext.SiteDeclaration])

        val (newContext, defsIntermediate, defNames) = makeCallablesContext(defs)(ctx)
        val newDefs = makeCallables(defsIntermediate, defNames)(newContext)
        val newbody = convert(body)(newContext)

        DeclareCallables(newDefs, newbody)
      }

      case ext.Declare(si @ ext.SiteImport(name, sitename), body) => {
        try {
          val site = Constant(OrcSiteForm.resolve(sitename))
          site.pushDownPosition(si.sourceTextRange)
          convert(body)(ctx.copy(context = context + { (name, site) }))
        } catch {
          case oe: OrcException => throw oe at e
        }
      }
      case ext.Declare(ci @ ext.ClassImport(name, classname), body) => {
        try {
          val u = new BoundTypevar(Some(name))
          u.pushDownPosition(ci.sourceTextRange)
          val site = Constant(JavaSiteForm.resolve(classname))
          site.pushDownPosition(ci.sourceTextRange)
          val newbody = convert(body)(ctx.copy(context = context + { (name, site) }, typecontext = typecontext + { (name, u) }))
          DeclareType(u, ClassType(classname), newbody)
        } catch {
          case oe: OrcException => throw oe at e
        }
      }

      case ext.Declare(ext.Include(_, decls), body) => convert((decls foldRight body) { ext.Declare })

      case ext.Declare(ext.TypeImport(name, classname), body) => {
        val u = new BoundTypevar(Some(name))
        val newbody = convert(body)(ctx.copy(typecontext = typecontext + { (name, u) }))
        DeclareType(u, ImportedType(classname), newbody)
      }

      case ext.Declare(decl @ ext.TypeAlias(name, typeformals, t), body) => {
        val u = new BoundTypevar(Some(name))
        val newtype =
          typeformals match {
            case Nil => convertType(t)
            case _ => {
              val (newTypeFormals, dtypecontext) = convertTypeFormals(typeformals, decl)
              val enclosedType = convertType(t)(ctx.copy(typecontext = typecontext ++ dtypecontext))
              TypeAbstraction(newTypeFormals, enclosedType)
            }
          }
        val newbody = convert(body)(ctx.copy(typecontext = typecontext + { (name, u) }))
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
                case Some(t) => convertType(t)(ctx.copy(typecontext = newtypecontext))
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
        val p = if (names.size == 1) ext.VariablePattern(names.head) else ext.TuplePattern(names map { ext.VariablePattern(_) })
        val x = new BoundVar(Some(id"$name"))
        val (source, dcontext, target) = convertPattern(p, x)

        val newbody = convert(body)(ctx.copy(context ++ dcontext, typecontext + { (name, d) }))
        val makeSites = makeDatatype(d, typeformals.size, constructors, this)

        DeclareType(d, variantType, Graft(x, source(makeSites), target(newbody)))
      }

      case ext.Declare(decl: ext.ValSig, _) => throw (MalformedExpression("Value signatures are only allowed in classes") at decl)
      case ext.Declare(decl, _) => throw (MalformedExpression("Invalid declaration form") at decl)

      case ext.TypeAscription(body, t) => HasType(convert(body), convertType(t))
      case ext.TypeAssertion(body, t) => HasType(convert(body), AssertedType(convertType(t)))

      case ext.Hole => Hole(context, typecontext)

    }
  }

  def convertArgumentGroup(target: Argument, ag: ext.ArgumentGroup)(implicit ctx: TranslatorContext): Expression = {
    import ctx._

    ag match {
      case ext.Args(typeargs, args) => {
        val newtypeargs = typeargs map { _ map convertType }
        unfold(args map convert, { Call(target, _, newtypeargs) })
        //Call(Constant(builtin.ProjectClosure), List(target), None) > f > Call(f, as, newtypeargs)
      }
      case ext.FieldAccess(field) => {
        FieldAccess(target, Field(field))
      }
      case ext.Dereference => {
        Call(context("?"), List(target), None)
      }
    }
  }

  /** Convert a list of extended AST callable declarations to a new context (for the bodies of the defs) and a map
    *
    */
  def makeCallablesContext(defs: List[ext.CallableDeclaration])(implicit ctx: TranslatorContext): (TranslatorContext, Map[String, AggregateDef], Map[String, BoundVar]) = {
    import ctx._

    var defsMap: Map[String, AggregateDef] = HashMap.empty.withDefaultValue(AggregateDef.empty(this))
    for (d <- defs; n = d.name) {
      defsMap = defsMap + { (n, defsMap(n) + d) }
    }

    // we generate these names beforehand since defs can be bound recursively in their own bodies
    val namesMap: Map[String, BoundVar] = Map.empty ++ (for (name <- defsMap.keys) yield (name, new BoundVar(Some(name))))
    val recursiveContext = context ++ namesMap
    val newCtx = ctx.copy(context = recursiveContext, boundDefs = boundDefs ++ namesMap.values)

    (newCtx, defsMap, namesMap)
  }

  /** Convert a list of extended AST def declarations to a list of named OIL definitions.
    */
  def makeCallables(defsMap: Map[String, AggregateDef], namesMap: Map[String, BoundVar])(implicit ctx: TranslatorContext): List[Callable] = {
    val newdefs = for ((n, d) <- defsMap) yield {
      d ->> d.convert(namesMap(n), ctx)
    }

    newdefs.toList
  }

  /** Convert an extended AST type to a named OIL type.
    */
  def convertType(t: ext.Type)(implicit ctx: TranslatorContext): Type = {
    import ctx._

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
        val newArgTypes = argtypes map { convertType(_)(ctx.copy(typecontext = newtypecontext)) }
        val newReturnType = convertType(returntype)(ctx.copy(typecontext = newtypecontext))
        FunctionType(newTypeFormals, newArgTypes, newReturnType)
      }
    }
  }

  /** Convert a list of type formal names to:
    *
    * A list of bound type formal variables
    * and
    * A context mapping those names to those vars
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
    *    A filtering conversion for the source expression
    * and
    *    A binding conversion for the target expression,
    *    parameterized on the variable carrying the result
    */
  def convertPattern(pat: ext.Pattern, bridge: BoundVar)(implicit ctx: TranslatorContext): (Conversion, Map[String, Argument], Conversion) = {
    import ctx._

    val bindingMap: mutable.Map[String, BoundVar] = new mutable.HashMap()

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
          val b = new BoundVar(Some(id"isEq_$c"))
          
          { callEq(focus, Constant(c)) > b > callIft(b) >> _ }
        }
        case ext.VariablePattern(name) => {
          if (focus.optionalVariableName.isEmpty)
            focus.optionalVariableName = Some(id"$name")
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
          val tupletmp = new BoundVar(focus.optionalVariableName)
          val sizecheck = { callTupleArityChecker(focus, tuplesize) > tupletmp > _ }

          /* Match each element of the tuple against its corresponding pattern */
          var elements = id
          for ((p, i) <- ps.zipWithIndex) {
            val y = new BoundVar(Some(id"${focus}_$i"))
            val bindElement: Conversion = { makeNth(tupletmp, i) > y > _ }
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
          val y = new BoundVar(Some(id"${focus}_isCons"))
          val p = ext.TuplePattern(List(ph, pt))
          
          { callIsCons(focus) > y > _ } compose unravel(p, y)
        }
        case ext.RecordPattern(elements) => {
          val y = new BoundVar(Some(id"${focus}_recordMatch"))
          val (labels, patterns) = elements.unzip
          val p = ext.TuplePattern(patterns)
          
          { callRecordMatcher(focus, labels) > y > _ } compose unravel(p, y)
        }
        case ext.CallPattern(name, args) => {
          val y = new BoundVar(Some(id"${focus}_unapplyResult"))
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

    val sourceVar = new BoundVar(bridge.optionalVariableName)
    val filterInto = unravel(pat, sourceVar)(false)
    
    sourceVar ->> bridge

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
          val y = new BoundVar(None)
          for ((name, `r`) <- bindingMap) {
            y.optionalVariableName = Some(id"$name")
            dcontext = dcontext + { (name, y) }
          }
          targetConversion = targetConversion compose { makeNth(bridge, i) > y > _ }
        }

        (sourceConversion, dcontext, targetConversion)
      }

    }

  }

}
