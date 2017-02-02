//
// ClassForms.scala -- Scala object ClassForms for building OrcO objects
// Project OrcScala
//
// Created by amp on Jan 26, 2015.
//
// Copyright (c) 2017 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//
package orc.compile.translate

import scala.collection.mutable
import scala.collection.immutable.ListMap
import scala.language.reflectiveCalls

import orc.ast.ext
import orc.ast.ext.ExtendedASTTransform
import orc.ast.oil.named._
import orc.error.OrcExceptionExtension._
import orc.error.compiletime._
import orc.values.Field
import orc.ast.hasAutomaticVariableName
import orc.ast.AST
import orc.ast.Positioned
import orc.compile.Logger

/** This class contains all the information needed to generate an instance of a class.
  *
  * It does not have complete information to generate a partial constructor. Some of
  * that information needs to be lifted directly from the ClassLiteral associated
  * with the class.
  *
  * This does not contain the linearization for a class. However, the linearization
  * can be computed from a set of ClassInfos for all the classes involved.
  */
trait ClassBasicInfo extends Positioned {
  /** The name of the class.
    *
    * This will be a generated name for synthetic classes.
    */
  def name: String

  /** The type name for nominal references to the class
    */
  val typeName: BoundTypevar = new BoundTypevar(Some(name))

  /** The name of the partial constructor def.
    *
    * This will take two arguments: self and super.
    */
  val partialConstructorName: BoundVar = new BoundVar(Some(s"$$partialConstructor$$$name"))

  /** The name of the overall constructor def.
    *
    * This will take no arguments.
    */
  val constructorName: BoundVar = new BoundVar(Some(s"$$constructor$$$name"))

  /** The sequence of parents of this class.
    *
    * This is only direct superclasses.
    */
  val superclasses: List[String]

  /** The names of all abstract members in this class mapped to their types.
    *
    * This does not include members defined in superclasses.
    */
  val abstractMembers: List[Field]

  /** The names of all concrete members in this class mapped to their bodies.
    *
    * The body must have an explicit type. This does not include members defined in superclasses.
    */
  val concreteMembers: List[Field]

  /** The class literal containing all the listed abstract and concrete members.
    *
    * If this is None then there may not be any members defined in this class.
    */
  val classLiteral: Option[ext.ClassLiteral]

  require(if (classLiteral.isEmpty) abstractMembers.isEmpty && concreteMembers.isEmpty else true,
    "If classLiteral is not provided then there may not be any members")

  def copy(superclasses: List[String] = superclasses, abstractMembers: List[Field] = abstractMembers, concreteMembers: List[Field] = concreteMembers, classLiteral: Option[ext.ClassLiteral] = classLiteral): ClassBasicInfo
}

object ClassBasicInfo {
  def apply(name: Option[String], superclasses: List[String], abstractMembers: List[Field], concreteMembers: List[Field], classLiteral: Option[ext.ClassLiteral]) = {
    name match {
      case Some(n) =>
        ClassInfo(n, superclasses, abstractMembers, concreteMembers, classLiteral)
      case None =>
        AnonymousClassInfo(superclasses, abstractMembers, concreteMembers, classLiteral)
    }
  }
}

case class AnonymousClassInfo(superclasses: List[String], abstractMembers: List[Field], concreteMembers: List[Field], classLiteral: Option[ext.ClassLiteral]) extends ClassBasicInfo {
  lazy val name = hasAutomaticVariableName.getNextVariableName("syncls")

  def copy(superclasses: List[String] = superclasses, abstractMembers: List[Field] = abstractMembers, concreteMembers: List[Field] = concreteMembers, classLiteral: Option[ext.ClassLiteral] = classLiteral) = {
    AnonymousClassInfo(superclasses, abstractMembers, concreteMembers, classLiteral)
  }

  fillSourceTextRange(classLiteral.flatMap(_.sourceTextRange))
}
case class ClassInfo(name: String, superclasses: List[String], abstractMembers: List[Field], concreteMembers: List[Field], classLiteral: Option[ext.ClassLiteral]) extends ClassBasicInfo {
  def copy(superclasses: List[String] = superclasses, abstractMembers: List[Field] = abstractMembers, concreteMembers: List[Field] = concreteMembers, classLiteral: Option[ext.ClassLiteral] = classLiteral) = {
    ClassInfo(name, superclasses, abstractMembers, concreteMembers, classLiteral)
  }

  fillSourceTextRange(classLiteral.flatMap(_.sourceTextRange))
}

/** Helper functions for class conversion
  * @author amp
  */
case class ClassForms(val translator: Translator) {
  import translator._

  def uniqueField(kind: String) = {
    Field(hasAutomaticVariableName.getNextVariableName(kind))
  }

  /** Generate a set of ClassInfos from a set of class declarations.
    *
    * The returned set of infos may contain classes from the context if they are referenced from the passed
    * classes. The returned set must include all classes needed to generate the linearization.
    *
    */
  def makeClassInfos(clss: Iterable[ext.ClassDeclaration])(implicit ctx: TranslatorContext): Set[ClassBasicInfo] = {
    clss.flatMap(cls => {
      val (c, ecs) = classForms.makeClassInfo(cls.classExpression, Some(cls.name))
      // Force this class to have the location of the declaration instead of the location of the class literal.
      c.sourceTextRange = cls.sourceTextRange
      ecs + c
    }).toSet
  }

  /** Generate the class info for a ClassExpression along with any required synthetic classes.
    *
    * This returns a set of synthetic classes required by the requested class.
    *
    * name is set on the root class info if name is provided. Otherwise the returned class info is synthetic.
    */
  def makeClassInfo(e: ext.ClassExpression, name: Option[String] = None)(implicit ctx: TranslatorContext): (ClassBasicInfo, Set[ClassBasicInfo]) = {
    e match {
      case ext.ClassVariable(n) =>
        def getAllClasses(n: String): Set[ClassBasicInfo] = {
          ctx.classContext(n).superclasses.flatMap(getAllClasses).toSet
        }
        val cls = ctx.classContext(n)
        (cls, cls.superclasses.flatMap(getAllClasses).toSet -- ctx.classContext.values)
      case cl @ ext.ClassLiteral(thisname, decls) => {
        var abstractMembers = List[Field]()
        var concreteMembers = List[Field]()
        def processDecls(ds: Seq[ext.Declaration]): Unit = ds match {
          // NOTE: The first two cases are optimizations. The third case also covers them, but also introduces extra fields.
          case ext.Val(ext.VariablePattern(x), f) +: rest => {
            concreteMembers :+= Field(x)
            processDecls(rest)
          }
          case ext.Val(ext.TypedPattern(ext.VariablePattern(x), t), f) +: rest => {
            concreteMembers :+= Field(x)
            processDecls(rest)
          }

          case ext.Val(p, f) +: rest => {
            def processPattern(p: AST): Unit = p match {
              case ext.VariablePattern(v) =>
                concreteMembers :+= Field(v)
              case p =>
                p.subtrees.foreach(processPattern)
            }
            processPattern(p)
            processDecls(rest)
          }
          case ext.ValSig(v, t) +: rest => {
            abstractMembers :+= Field(v)
            processDecls(rest)
          }
          case ext.CallableSingle(defs, rest) => {
            assert(defs.forall(_.name == defs.head.name))
            val field = Field(defs.head.name)
            if (defs.find((x: ext.CallableDeclaration) => x.isInstanceOf[ext.Callable]).isDefined) {
              // Concrete
              concreteMembers :+= field
            } else {
              // Abstract
              abstractMembers :+= field
            }
            processDecls(rest)
          }
          case decl +: rest => {
            throw (MalformedExpression("Invalid declaration form in class") at decl)
          }
          case List() => ()
        }

        processDecls(decls)

        (ClassBasicInfo(name, List(), abstractMembers, concreteMembers, Some(cl)), Set())
      }
      case ext.ClassSubclassLiteral(ext.ClassMixins(ss @ _*), b) => {
        val (sns, sec) = ss.map(getClassName(_)).unzip
        val (bc, bec) = makeClassInfo(b, name)
        assert(bec.isEmpty, "Class literals should never produce extra classes")
        (bc.copy(superclasses = List(sns: _*)), sec.flatten.toSet)
      }
      case ext.ClassSubclassLiteral(s, b) => {
        val (sn, sec) = getClassName(s)
        val (bc, bec) = makeClassInfo(b, name)
        assert(bec.isEmpty, "Class literals should never produce extra classes")
        (bc.copy(superclasses = List(sn)), sec)
      }
      case ext.ClassMixins(cs @ _*) => {
        val (rn, rec) = cs.map(getClassName(_)).unzip
        (ClassBasicInfo(name, List(rn: _*), List(), List(), None), rec.flatten.toSet)
      }
    }
  }

  def getClassName(e: ext.ClassExpression, name: Option[String] = None)(implicit ctx: TranslatorContext): (String, Set[ClassBasicInfo]) = {
    e match {
      case ext.ClassVariable(n) if ctx.classContext contains n =>
        (ctx.classContext(n).name, Set())
      case ext.ClassVariable(n) =>
        (n, Set())
      case e =>
        val (c, ex) = makeClassInfo(e, name)
        (c.name, ex + c)
    }
  }

  def getMembers(cls: ClassBasicInfo)(implicit ctx: TranslatorContext): (Set[Field], Set[Field]) = {
    val lin = generateLinearization(cls)
    getMembers(lin)
  }

  def getMembers(lin: List[ClassBasicInfo])(implicit ctx: TranslatorContext): (Set[Field], Set[Field]) = {
    val allConcreteMembers = lin.flatMap(_.concreteMembers).toSet
    val allAbstractMembers = lin.flatMap(_.abstractMembers).toSet -- allConcreteMembers
    (allConcreteMembers, allAbstractMembers)
  }

  def isAbstract(cls: ClassBasicInfo)(implicit ctx: TranslatorContext): Boolean = {
    val (_, allAbstractMembers) = getMembers(cls)
    allAbstractMembers.nonEmpty
  }

  /** Build the constructor for the given class.
    *
    * All it's super classes must be in ctx.
    *
    * For each concrete class declaration C with linearization L = A1, ..., An, C generate a function:
    * def newC(): C = new self: C {
    * partialObject = buildObject(self)
    * partialA1 = buildA1(self, partialObject)
    * partialA2 = buildA2(self, partialA1)
    * ...
    * partialAn = buildAn(self, partialAn-1)
    * partialC = buildC(self, partialAn)
    * x = partialC.x for each field in partialC
    * }
    * This is a truly recursive object which ties together all the partials into a chain in linearization order.
    */
  def generateConstructor(cls: ClassBasicInfo)(implicit ctx: TranslatorContext): Option[Def] = {
    try {
      val lin = generateLinearization(cls)

      val (allConcreteMembers, allAbstractMembers) = getMembers(lin)

      if (allAbstractMembers.isEmpty) {
        val body = {
          def partialField(n: String) = Field(s"$$partial$$$n")
          val self = new BoundVar(Some(hasAutomaticVariableName.getNextVariableName("self")))
          val partialObject = (partialField("Object"), makePartialObject(self): Expression)
          val partials = lin.foldRight((List(partialObject))) { (cls, acc) =>
            val (prev, _) +: _ = acc
            val x = new BoundVar()
            val partialConstructorCall =
              Graft(x, FieldAccess(self, prev),
                Call(cls.partialConstructorName, List(self, x), Some(List())))
            (partialField(cls.name), partialConstructorCall) +: acc
          }
          val (finalPartial, _) +: _ = partials
          val fields = allConcreteMembers.map(makeForwardingField(self, finalPartial, _))

          New(self, Some(cls.typeName), ListMap() ++ partials.reverse ++ fields, Some(cls.typeName))
        }

        val formals = List()
        val typeformals = List()
        val argtypes = Some(List())
        val returntype = Some(cls.typeName)

        val d = Def(cls.constructorName, formals, body, typeformals, argtypes, returntype)
        Some(d)
      } else {
        // There are abstract members so there is no constructor.
        None
      }
    } catch {
      case e: CompilationException with ContinuableSeverity =>
        translator.reportProblem(e)
        None
    }
  }

  def makeForwardingField(self: Argument, partial: Field, f: Field) = {
    val x = new BoundVar()
    val extraction: Expression = FieldAccess(self, partial) > x > FieldAccess(x, f)
    (f, extraction)
  }

  /** A cache of linearizations which serves to break cycles.
    *
    * The values are None if the linearization is currently being computed
    * and Some(L) (where L is the linearization) if the computation is complete.
    *
    * This may also improve performance, but that's not the goals.
    */
  val linearizationCache = new mutable.HashMap[ClassBasicInfo, Option[List[ClassBasicInfo]]]()
  // TODO: Move this inside generateLinearization. In the current position it could theoretically confuse classes with the same name in different contexts.

  /** Generate the C3 linearization of cls based on ctx.
    *
    */
  @throws[ConflictingOrderException]
  @throws[CyclicInheritanceException]
  def generateLinearization(cls: ClassBasicInfo)(implicit ctx: TranslatorContext): List[ClassBasicInfo] = {
    def merge(orders: List[List[ClassBasicInfo]]): List[ClassBasicInfo] = {
      Logger.finest(s"Linearizing: (${cls.name}) Merging ${orders.map(_.map(_.name))}")
      orders match {
        case List() => List()
        case l if l.find(_.isEmpty).isDefined => merge(orders.filterNot(_.isEmpty))
        case orders => {
          val heads = orders.map(_.head)
          val tails = orders.flatMap(_.tail).toSet
          def goodCandidate(c: ClassBasicInfo): Boolean = !tails.contains(c)
          heads.find(goodCandidate) match {
            case None =>
              throw (ConflictingOrderException(orders.map(_.map(_.name))) at cls)
            case Some(candidate) =>
              Logger.finest(s"Linearizing: (${cls.name}) good candidate ${candidate.name}")
              val tailsWOCandidate = orders.map(_.dropWhile(_ == candidate))
              candidate +: merge(tailsWOCandidate)
          }
        }
      }
    }

    linearizationCache.get(cls) match {
      case None =>
        // Mark this linearization and currently being computed.
        linearizationCache += cls -> None
        val supers = cls.superclasses.flatMap { n =>
          ctx.classContext.get(n) orElse {
            translator.reportProblem(UnboundClassVariableException(n) at cls)
            None
          }
        }
        val superLins = supers.toList.map(generateLinearization(_))
        val res = merge(superLins :+ (cls +: supers))
        linearizationCache += cls -> Some(res)
        res
      case Some(None) =>
        // This linearization is currently being computed. This means
        // that the inheritance is cyclic which is not allowed.
        throw (CyclicInheritanceException(List(cls.name)) at cls)
      case Some(Some(l)) => l
    }
  }

  def makePartialObject(self: BoundVar): New = {
    // TODO: TYPECHECKER: Add empty structural type to this.
    New(new BoundVar, None, Map(), None)
  }

  /** Generate the partial constructor for a class using the context.
    *
    * For a "class C extends B { decls }" definition generate a function:
    *
    * def buildC(self: C, super: B): C = new _: C { decls ... }
    * (decls will include forwarding declarations for any non-overridden values from B.)
    *
    * The decls will use the self argument as their self instead of the actual object being
    * built. In fact this record construction is not recursive which could be advantageous
    * for optimizations. super is used for any super references in the declarations.
    */
  def generatePartialConstructor(cls: ClassBasicInfo)(implicit ctx: TranslatorContext): Def = {
    val (allConcreteMembers, allAbstractMembers) = getMembers(cls)
    val literal = cls.classLiteral.getOrElse(ext.ClassLiteral(None, List()))
    val (selfRef, superRef, fields) = convertClassLiteral(literal, (allConcreteMembers ++ allAbstractMembers).map(_.field))
    val forwardedFields = allConcreteMembers -- fields.keys
    val forwards = forwardedFields.map(f => (f, FieldAccess(superRef, f)))

    // TODO: TYPECHECKER: Replace cls.typeName with a structural type which only includes concrete fields (and protected fields).
    // see https://docs.google.com/document/d/1_-JO-eSr1CMR7KES6MMwZRM8qzpQMnOz23N7T2PZe_s
    val selfType = cls.typeName

    val body = {
      // TODO: TYPECHECKER: Introduce privateSelf and give it a type to support private members. This will require changes to convertClassLiteral.
      New(new BoundVar, None, fields ++ forwards, Some(selfType))
    }

    // TODO: TYPECHECKER: Replace superType with Union supertype concrete member sets.
    val superType = cls.superclasses.flatMap(c => ctx.classContext.get(c).map(_.typeName)).reduceOption(IntersectionType).getOrElse(Top())

    val formals = List(selfRef, superRef)
    val typeformals = List()
    val argtypes = Some(List(selfType, superType))
    val returntype = Some(selfType)

    val d = Def(cls.partialConstructorName, formals, body, typeformals, argtypes, returntype)
    d
  }

  /** Given a ClassLiteral and a set of fields on this, build all field expressions for the class.
    *
    * Return the BoundVar for this and the mapping of fields to expressions.
    */
  def convertClassLiteral(lit: ext.ClassLiteral, fieldNames: Set[String])(implicit ctx: TranslatorContext): (BoundVar, BoundVar, Map[Field, Expression]) = {
    import ctx._

    val thisName = lit.thisname.getOrElse("this")
    val thisVar = new BoundVar(Some(thisName))
    val superVar = new BoundVar(Some("super"))

    val thisContext = List((thisName, thisVar), ("this", thisVar), ("super", superVar))

    // Remove field names from the context this makes fields shadow outside definitions
    val memberContext = (context -- fieldNames) ++ thisContext
    val newCtx = ctx.copy(context = memberContext)

    def convertFields(d: Seq[ext.Declaration]): Seq[(Field, Option[Expression])] = d match {
      // NOTE: The first two cases are optimizations. The third case also covers them, but also introduces extra fields.
      case ext.Val(ext.VariablePattern(x), f) +: rest =>
        (Field(x), Some(convert(f)(newCtx))) +: convertFields(rest)
      case ext.Val(ext.TypedPattern(ext.VariablePattern(x), t), f) +: rest =>
        (Field(x), Some(HasType(convert(f)(newCtx), convertType(t)))) +: convertFields(rest)
      case ext.Val(p, f) +: rest =>
        val tmpField = uniqueField("pattmp")
        // FIXME: Is it a problem if the same variable is bound in multiple subtrees? We can just call convertPattern with different bridges or modify
        // convertPattern to take an additional argument to the target closure it returns.

        // TODO: If we modify convertPattern to save information from TypedPatterns we could type the below expressions.
        val x = new BoundVar()
        val (source, dcontext, target) = convertPattern(p, x)(newCtx)
        val newf = convert(f)(newCtx)
        val generatedFields = for ((name, code) <- dcontext.toSeq) yield (Field(name), Some(FieldAccess(thisVar, tmpField) > x > target(code)))
        (tmpField, Some(source(newf))) +: (generatedFields ++ convertFields(rest))
      case ext.ValSig(v, t) +: rest =>
        val field = Field(v)
        // TODO: Store type for later use in type checking.
        (field, None) +: convertFields(rest)
      case ext.CallableSingle(defs, rest) =>
        assert(defs.forall(_.name == defs.head.name))
        val field = Field(defs.head.name)
        val name = new BoundVar(Some("$" + defs.head.name))
        val agg = defs.foldLeft(AggregateDef.empty(translator))(_ + _)
        if (agg.clauses.isEmpty) {
          // Abstract
          // TODO: Store type for later use in type checking.
          (field, None) +: convertFields(rest)
        } else {
          // Concrete
          val newdef = agg ->> agg.convert(name, newCtx)
          (field, Some(DeclareCallables(List(newdef), name))) +: convertFields(rest)
        }
      case Seq() =>
        Nil
      case decl :: _ =>
        throw (MalformedExpression("Invalid declaration form in class") at decl)
    }

    // Fields will be unbound variables. Search for them and add look ups as tightly around them as possible.
    val fieldUnboundVars = fieldNames map UnboundVar
    val desugarMembers = new NamedASTTransform {
      override def onExpression(context: List[BoundVar], typecontext: List[BoundTypevar]) = {
        case e if unboundVarArguments(e).exists(fieldUnboundVars contains _) =>
          val toBind = unboundVarArguments(e) & fieldUnboundVars
          toBind.foldRight(e) { (unboundVar, e) =>
            val localVar = new BoundVar(unboundVar.optionalVariableName)
            Graft(localVar, FieldAccess(thisVar, Field(unboundVar.name)), e.subst(localVar, unboundVar))
          }
      }
    }

    val newbindings = convertFields(lit.decls).collect({ case (f, Some(e)) => (f, e) }).toMap.mapValues(desugarMembers.apply)
    (thisVar, superVar, Map() ++ newbindings)
  }

  private def arguments(e: Expression): Set[Argument] = e match {
    case Call(target, args, typeargs) => args.toSet + target
    case FieldAccess(o, f) => Set(o)
    case a: Argument => Set(a)
    case _ => Set()
  }

  private def unboundVarArguments(e: Expression): Set[UnboundVar] = arguments(e).collect {
    case v: UnboundVar => v
  }

  def makeClassDeclarations(clss: Set[ClassBasicInfo])(bodyFunc: TranslatorContext => Expression)(implicit ctx: TranslatorContext): Expression = {
    val tmpCtx = clss.foldLeft(ctx)((c, cls) => c.copy(classContext = c.classContext + (cls.name -> cls)))
    val newCtx = clss.foldLeft(ctx)((newCtx, cls) => {
      if (isAbstract(cls)(tmpCtx)) {
        newCtx.copy(boundDefs = newCtx.boundDefs + cls.partialConstructorName, classContext = newCtx.classContext + (cls.name -> cls))
      } else {
        newCtx.copy(boundDefs = newCtx.boundDefs + cls.constructorName + cls.partialConstructorName, classContext = newCtx.classContext + (cls.name -> cls))
      }
    })

    {
      implicit val ctx = newCtx
      val defs = clss.toList.flatMap(generateConstructor(_)) ++ clss.toList.map(generatePartialConstructor(_))
      DeclareCallables(defs, bodyFunc(ctx))
    }
  }

  /** Build a New expression from the extended equivalents.
    *
    * This will generate any needed synthetic classes as a DeclareClasses node wrapped around the New.
    */
  def makeNew(e: ext.ClassExpression)(implicit ctx: TranslatorContext): Expression = {
    val (cls, clss) = makeClassInfo(e)
    makeClassDeclarations(clss + cls) { ctx =>
      if (ctx.boundDefs.contains(cls.constructorName)) {
        Call(cls.constructorName, List(), Some(List()))
      } else {
        val (_, allAbstractMembers) = getMembers(cls)(ctx)
        translator.reportProblem(InstantiatingAbstractClassException(allAbstractMembers.toList.map(_.field)) at e)
        Call(Constant(cls.name), List(), Some(List()))
      }
    }
  }

  /*
  /** Build a class constructor.
    *
    * toBind is a sequence of fields that should be bound from constructor arguments.
    * toRecursiveBind is a set of fields that should be bound to specific variables
    * (usually recursive functions). This can contain constrName and often will.
    */
  private def makeClassConstructor(isSite: Boolean, clsName: String, constrName: BoundVar, toBind: Seq[Field], toRecursiveBind: Map[Field, BoundVar], argTypes: Option[Seq[Type]], returnType: Option[Type])(ctx: TranslatorContext): Callable = {
    val formals = toBind.map(f => new BoundVar(Some(f.field))).toList
    val body = if (toBind.isEmpty && toRecursiveBind.isEmpty) {
      New(ctx.classcontext(clsName).linearization)
    } else {
      val synClsName = new BoundVar(Some(hasAutomaticVariableName.getNextVariableName("synCls")))
      val thisVar = new BoundVar(Some("this"))
      val superVar = new BoundVar(Some("super"))
      val bindings = for ((f, n) <- (toBind zip formals) ++ toRecursiveBind) yield f -> n
      val cls = Class(synClsName, thisVar, superVar, bindings.toMap, List(Classvar(synClsName)))

      DeclareClasses(List(cls), {
        New(Classvar(synClsName) +: ctx.classcontext(clsName).linearization)
      })
    }
    val typeformals = Nil
    val argtypes = argTypes.map(_.toList)

    if (isSite)
      Site(constrName, formals, body, typeformals, argtypes, returnType)
    else
      Def(constrName, formals, body, typeformals, argtypes, returnType)
  }

  /** Desugar class constructors.
    * The returned classes can a None constructor but have additional fields (both concrete and abstract)
    * that handle the constructor arguments. This also returns a sequence of functions that build the
    * constructors themselves within a specific type context. This cannot return the constructors themselves
    * since they need to be built in a context with the class types that will only exist later in the process.
    *
    * The appoach is to first collect type and name information about all the constructors and then update
    * classes and build constructor maker closures using that information.
    */
  private def desugarClasses(clss: Seq[ext.ClassDeclaration])(implicit ctx: TranslatorContext): (Seq[ext.ClassDeclaration], Map[String, (Map[String, Type], Map[String, ClassInfo]) => Callable]) = {
    // Generate constructor names and ext types
    val constructorInfo = (for (ext.ClassDeclaration(constructor: ext.ClassCallableConstructor, _, _) <- clss) yield {
      val argOptTypes = constructor.formals map { p =>
        p match {
          case ext.TypedPattern(_, t) => Some(t)
          case _ => None
        }
      }
      val argTypesOpt = if (argOptTypes.exists(_.isEmpty)) None else Some(argOptTypes.map(_.get))
      val constrType = for (returnType <- constructor.returntype; argTypes <- argTypesOpt) yield ext.LambdaType(Nil, argTypes, returnType)
      constructor.name -> (new BoundVar(Some(constructor.name)), argTypesOpt, constrType)
    }).toMap

    val toRecursiveBind = constructorInfo map { case (n, (v, _, _)) => Field(n) -> v }
    val newConstructorDecls = constructorInfo.toList.map { case (n, (_, _, t)) => ext.ValSig(n, t) }

    // Generate the desugared classes and constuctor maker functions
    val results = for (cls @ ext.ClassDeclaration(constructor, superclass, body) <- clss) yield {
      val (newCls, constrMakerMapping) = constructor match {
        case _: ext.ClassConstructor.None => (cls, None)
        case constructor: ext.ClassCallableConstructor => {
          val newFields = constructor.formals.map(_ => uniqueField("arg"))

          val (_, argTypesOpt, _) = constructorInfo(constructor.name)
          val argOptTypes = argTypesOpt.map(_.map(Some(_))).getOrElse(constructor.formals.map(_ => None))

          val newDecls = (newFields zip constructor.formals zip argOptTypes) flatMap { both =>
            val ((tmpField, p), argType) = both
            List(
              ext.ValSig(tmpField.field, argType),
              ext.Val(p, ext.Call(ext.Variable("this"), List(ext.FieldAccess(tmpField.field)))))
          }

          val newCls = ext.ClassDeclaration(ext.ClassConstructor.None(constructor.name, constructor.typeformals), superclass,
            body.copy(decls = body.decls ++ newDecls))

          def constrMaker(tc: Map[String, Type], cc: Map[String, ClassInfo]) = {
            val newCtx = ctx.copy(typecontext = tc, classcontext = cc)
            makeClassConstructor(constructor.isInstanceOf[ext.ClassConstructor.Site],
              constructor.name, constructorInfo(constructor.name)._1, newFields, Map(),
              argTypesOpt.map(_.map(convertType(_)(newCtx))), constructor.returntype map { convertType(_)(newCtx) })(newCtx)
          }

          (newCls, Some(constructor.name -> constrMaker _))
        }
      }
      (newCls.copy(body = newCls.body.copy(decls = newCls.body.decls ++ newConstructorDecls)), constrMakerMapping)
    }
    val (newClss, constrs) = results.unzip
    (newClss, constrs.flatten.toMap)
  }
  */
}
