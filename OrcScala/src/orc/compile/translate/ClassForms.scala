//
// ClassForms.scala -- Scala object ClassForms for building OrcO objects
// Project OrcScala
//
// Created by amp on Jan 26, 2015.
//
// Copyright (c) 2018 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//
package orc.compile.translate

import scala.collection.immutable.ListMap
import scala.collection.mutable
import scala.language.reflectiveCalls

import orc.ast.{ AST, Positioned, ext, hasOptionalVariableName }
import orc.ast.oil.named.{ Argument, BoundTypevar, BoundVar, Call, Callable, Constant, DeclareCallables, DeclareType, Def, Expression, FieldAccess, Graft, HasType, IntersectionType, NamedASTTransform, New, NominalType, StructuralType, Top, Type, UnboundVar }
import orc.compile.Logger
import orc.compile.optimize.FractionDefs
import orc.error.OrcExceptionExtension.extendOrcException
import orc.error.compiletime.{ CompilationException, ConflictingOrderException, ContinuableSeverity, CyclicInheritanceException, InstantiatingAbstractClassException, MalformedExpression, UnboundClassVariableException }
import orc.values.Field

import hasOptionalVariableName._

/** This class contains all the information needed to generate an instance of a class.
  *
  * It does not have complete information to generate a partial constructor. Some of
  * that information needs to be lifted directly from the ClassLiteral associated
  * with the class.
  *
  * This does not contain the linearization for a class. However, the linearization
  * can be computed from a set of ClassInfos for all the classes involved.
  *
  * @param name The name of the class. This will be a generated name for synthetic classes.
  * @param superclasses The sequence of parents of this class. This is only direct superclasses.
  * @param abstractMembers The names of all abstract members in this class mapped to their types. This does not include members defined in superclasses.
  * @param concreteMembers The names of all concrete members in this class mapped to their bodies. The body must have an explicit type. This does not include members defined in superclasses.
  * @param classLiteral The class literal containing all the listed abstract and concrete members. If this is None then there may not be any members defined in this class.
  *
  * @param typeName The type name for nominal references to the class.
  * @param partialConstructorName The name of the partial constructor def. This will take two arguments, self and super, plus any free variables.
  * @param partialConstructorPlaceholderName The name of the partial constructor placeholder. This will be eliminated as the compilation progresses. This will take two arguments, self and super.
  * @param constructorName The name of the overall constructor def. This will take no arguments other than free variables.
  * @param constructorPlaceholderName The name of the overall constructor placeholder. This will be eliminated as the compilation progresses. This will take no arguments other than free variables.
  *
  */
class ClassInfo private (
  val name: String,
  val superclasses: Seq[String],
  val abstractMembers: Seq[Field],
  val concreteMembers: Seq[Field],
  val classLiteral: Option[ext.ClassLiteral],
  val capturedVariables: Option[Seq[BoundVar]] = None)(
    val typeName: BoundTypevar = new BoundTypevar(Some(name)),
    val partialConstructorName: BoundVar = new BoundVar(Some(id"$$partialConstructor$$$name")),
    val partialConstructorPlaceholderName: BoundVar = new BoundVar(Some(id"placeholder$$partialConstructor$$$name")),
    val constructorName: BoundVar = new BoundVar(Some(id"$$constructor$$$name")),
    val constructorPlaceholderName: BoundVar = new BoundVar(Some(id"placeholder$$constructor$$$name")))
  extends Positioned {

  def unplaceholder(v: BoundVar) = {
    if (v == constructorPlaceholderName) {
      constructorName
    } else if (v == partialConstructorPlaceholderName) {
      partialConstructorName
    } else {
      throw new IllegalArgumentException(v.toString())
    }
  }

  require(if (classLiteral.isEmpty) abstractMembers.isEmpty && concreteMembers.isEmpty else true,
    "If classLiteral is not provided then there may not be any members")

  fillSourceTextRange(classLiteral.flatMap(_.sourceTextRange))

  override def toString(): String = s"ClassInfo($name, $superclasses, ..., $capturedVariables)"

  override def hashCode(): Int = {
    (name.##, superclasses.##, abstractMembers.##, concreteMembers.##, classLiteral.##, capturedVariables.##).hashCode()
  }
  override def equals(o: Any) = o match {
    case ClassInfo(`name`, `superclasses`, `abstractMembers`, `concreteMembers`, `classLiteral`, `capturedVariables`) => true
    case _ => false
  }

  def copy(superclasses: Seq[String] = this.superclasses,
    abstractMembers: Seq[Field] = this.abstractMembers,
    concreteMembers: Seq[Field] = this.concreteMembers,
    classLiteral: Option[ext.ClassLiteral] = this.classLiteral,
    capturedVariables: Option[Seq[BoundVar]] = this.capturedVariables) = {
    new ClassInfo(
      name, superclasses, abstractMembers, concreteMembers, classLiteral, capturedVariables)(
      typeName, partialConstructorName, partialConstructorPlaceholderName, constructorName, constructorPlaceholderName)
  }
}

object ClassInfo {
  def apply(
    name: String,
    superclasses: Seq[String],
    abstractMembers: Seq[Field],
    concreteMembers: Seq[Field],
    classLiteral: Option[ext.ClassLiteral],
    capturedVariables: Option[Seq[BoundVar]] = None) = {
    new ClassInfo(name, superclasses, abstractMembers, concreteMembers, classLiteral, capturedVariables)()
  }

  def unapply(info: ClassInfo): Option[(String, Seq[String], Seq[Field], Seq[Field], Option[ext.ClassLiteral], Option[Seq[BoundVar]])] = {
    if (info != null) {
      Some((info.name, info.superclasses, info.abstractMembers, info.concreteMembers, info.classLiteral, info.capturedVariables))
    } else {
      None
    }
  }
}

/** Helper functions for class conversion
  *
  * @author amp
  */
case class ClassForms(val translator: Translator) {
  import translator._

  def syntheticName() = {
    id"syncls"
  }

  private def uniqueField(kind: String) = {
    Field(id"$kind")
  }

  /** Generate a set of ClassInfos from a set of class declarations.
    *
    * The returned set of infos may contain classes from the context if they are referenced from the passed
    * classes. The returned set must include all classes needed to generate the linearization.
    *
    */
  def makeClassInfos(clss: Iterable[ext.ClassDeclaration])(implicit ctx: TranslatorContext): Set[ClassInfo] = {
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
  private def makeClassInfo(e: ext.ClassExpression, name: Option[String] = None)(implicit ctx: TranslatorContext): (ClassInfo, Set[ClassInfo]) = {
    e match {
      case ext.ClassVariable(n) =>
        def getAllClasses(n: String): Set[ClassInfo] = {
          ctx.classContext.get(n).map(_.superclasses.flatMap(getAllClasses).toSet).getOrElse({
            translator.reportProblem(UnboundClassVariableException(n) at e)
            Set()
          })
        }
        val cls = ctx.classContext.getOrElse(n, {
          translator.reportProblem(UnboundClassVariableException(n) at e)
          // Build a dummy ClassInfo to allow compilation to continue.
          ClassInfo(n, Seq(), Seq(), Seq(), None)
        })
        (cls, cls.superclasses.flatMap(getAllClasses).toSet -- ctx.classContext.values)
      case cl @ ext.ClassLiteral(thisname, decls) => {
        var abstractMembers = List[Field]()
        var concreteMembers = List[Field]()
        var freeVars = List[BoundVar]()
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

        (ClassInfo(name.getOrElse(syntheticName()), List(), abstractMembers, concreteMembers, Some(cl)), Set())
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
        (ClassInfo(name.getOrElse(syntheticName()), List(rn: _*), List(), List(), None), rec.flatten.toSet)
      }
    }
  }

  private def getClassName(e: ext.ClassExpression, name: Option[String] = None)(implicit ctx: TranslatorContext): (String, Set[ClassInfo]) = {
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

  private def getMembers(cls: ClassInfo)(implicit ctx: TranslatorContext): (Set[Field], Set[Field]) = {
    val lin = generateLinearization(cls)
    getMembers(lin)
  }

  private def getMembers(lin: Seq[ClassInfo])(implicit ctx: TranslatorContext): (Set[Field], Set[Field]) = {
    val allConcreteMembers = lin.flatMap(_.concreteMembers).toSet
    val allAbstractMembers = lin.flatMap(_.abstractMembers).toSet -- allConcreteMembers
    (allConcreteMembers, allAbstractMembers)
  }

  private def isAbstract(cls: ClassInfo)(implicit ctx: TranslatorContext): Boolean = {
    val (_, allAbstractMembers) = getMembers(cls)
    allAbstractMembers.nonEmpty
  }

  /** Build the constructor for the given class.
    *
    * All its super classes must be in ctx.
    *
    * For each concrete class declaration C with linearization L = A1, ..., An, C generate a function:
    * def constructorC(): C = new self: C {
    * partialObject = partialConstructorObject(self)
    * partialA1 = partialConstructorA1(self, partialObject)
    * partialA2 = partialConstructorA2(self, partialA1)
    * ...
    * partialAn = partialConstructorAn(self, partialAn-1)
    * partialC = partialConstructorC(self, partialAn)
    * x = partialC.x for each field in partialC
    * }
    * This is a truly recursive object which ties together all the partials into a chain in linearization order.
    *
    * The generated function requires patching for closure lifting.
    */
  private def generateConstructor(cls: ClassInfo)(implicit ctx: TranslatorContext): Option[Def] = {
    try {
      val lin = generateLinearization(cls)

      val (allConcreteMembers, allAbstractMembers) = getMembers(lin)

      if (allAbstractMembers.isEmpty) {
        val body = {
          def partialField(n: String) = Field(s"$$partial$$$n")

          val self = new BoundVar(Some(id"self_${cls.name}"))

          def generateForwardingObject(s: BoundVar, st: Option[Type], extraFields: Seq[(Field, Expression)], forwardings: Map[Field, Field], t: Option[Type]): Expression = {
            val fields = forwardings.map {
              case (f, src) =>
                makeForwardingField(self, src, f)
            }
            New(s, st, ListMap() ++ extraFields ++ fields, t)
          }

          val partialObject = (partialField("Object"), makePartialObject(self): Expression)

          case class ChainConstructionState(concreteFields: Map[Field, Field], partials: List[(Field, Expression)], types: Set[Type])

          val finalState = lin.foldRight(ChainConstructionState(Map(), List(partialObject), Set(StructuralType(Map())))) { (cls, state) =>
            import state._

            val (prev, _) +: _ = partials
            val x = new BoundVar(Some(id"super_${cls.name}"))
            // TODO: In cases where the super type of the partial we are calling only needs values from one other type we could
            //       optimize this to just be a field reference instead of a complete new object.
            //       This optimization is tricky to get right. But for the empty super it's trivial.
            val superRef = generateForwardingObject(new BoundVar(Some(unusedVariable)), None, Seq(), concreteFields, Some(IntersectionType(types)))
            val partialConstructorCall =
              Graft(x, superRef,
                Call(cls.partialConstructorPlaceholderName, List(self, x), Some(List())))
            val clsPartialField = partialField(cls.name)
            state.copy(concreteFields = concreteFields ++ cls.concreteMembers.map(_ -> clsPartialField),
              partials = (clsPartialField, partialConstructorCall) +: partials,
              types = types + cls.typeName)
          }
          val ChainConstructionState(_, (finalPartial, _) +: _, _) = finalState

          generateForwardingObject(self, Some(cls.typeName), finalState.partials.reverse, finalState.concreteFields, Some(cls.typeName))
        }

        val formals = List()
        val typeformals = List()
        val argtypes = Some(List())
        val returntype = Some(cls.typeName)

        val d = Def(cls.constructorPlaceholderName, formals, body, typeformals, argtypes, returntype)
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

  private def makeForwardingField(self: Argument, partial: Field, f: Field) = {
    val x = new BoundVar(Some(id"${partial.name}"))
    val extraction: Expression = FieldAccess(self, partial) > x > FieldAccess(x, f)
    (f, extraction)
  }

  /** Generate the C3 linearization of cls based on ctx.
    *
    */
  private def generateLinearization(cls: ClassInfo)(implicit ctx: TranslatorContext): Seq[ClassInfo] = {
    // TODO: This has to totally recompute all linearizations ever time.

    /** A cache of linearizations which serves to break cycles.
      *
      * The values are None if the linearization is currently being computed
      * and Some(L) (where L is the linearization) if the computation is complete.
      *
      * This may also improve performance, but that's not the goals.
      */
    val linearizationCache = new mutable.HashMap[ClassInfo, Option[Seq[ClassInfo]]]()

    /** Generate the C3 linearization of cls based on ctx.
      *
      */
    @throws[ConflictingOrderException]
    @throws[CyclicInheritanceException]
    def genLin(cls: ClassInfo)(implicit ctx: TranslatorContext): Seq[ClassInfo] = {
      def merge(orders: Seq[Seq[ClassInfo]]): Seq[ClassInfo] = {
        Logger.finest(s"Linearizing: (${cls.name}) Merging ${orders.map(_.map(_.name))}")
        orders match {
          case List() => List()
          case l if l.find(_.isEmpty).isDefined => merge(orders.filterNot(_.isEmpty))
          case orders => {
            val heads = orders.map(_.head)
            val tails = orders.flatMap(_.tail).toSet
            def goodCandidate(c: ClassInfo): Boolean = !tails.contains(c)
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
          val superLins = supers.toSeq.map(genLin(_))
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

    genLin(cls)
  }

  private def makePartialObject(self: BoundVar): New = {
    New(new BoundVar(Some(id"partialObject")), Some(StructuralType(Map())), Map(), Some(StructuralType(Map())))
  }

  /** Generate the partial constructor for a class using the context.
    *
    * For a "class C extends B { decls }" definition generate a function:
    *
    * def partialConstructorC(self: C, super: B): C = new _: C { decls ... }
    * (decls will include forwarding declarations for any non-overridden values from B.)
    *
    * The decls will use the self argument as their self instead of the actual object being
    * built. In fact this record construction is not recursive which could be advantageous
    * for optimizations. super is used for any super references in the declarations.
    *
    * The generated function requires patching for closure lifting.
    */
  private def generatePartialConstructor(cls: ClassInfo)(implicit ctx: TranslatorContext): Def = {
    val (allConcreteMembers, allAbstractMembers) = getMembers(cls)
    val literal = cls.classLiteral.getOrElse(ext.ClassLiteral(None, List()))
    val (selfRef, superRef, privateRef, fields) = convertClassLiteral(literal, (allConcreteMembers ++ allAbstractMembers).map(_.name))
    val forwardedFields = allConcreteMembers -- fields.keys
    val forwards = forwardedFields.map(f => (f, FieldAccess(superRef, f)))

    // TODO: TYPECHECKER: Replace cls.typeName with a structural type which only includes concrete fields (and protected fields).
    // see https://docs.google.com/document/d/1_-JO-eSr1CMR7KES6MMwZRM8qzpQMnOz23N7T2PZe_s
    val selfType = cls.typeName

    val body = {
      // TODO: TYPECHECKER: Introduce privateSelf and give it a type to support private members. This will require changes to convertClassLiteral.
      New(privateRef, None, fields ++ forwards, Some(selfType))
    }

    // TODO: TYPECHECKER: Replace superType with Union of supertype concrete member sets.
    val superType = cls.superclasses.flatMap(c => ctx.classContext.get(c).map(_.typeName: Type)).reduceOption(IntersectionType(_, _)).getOrElse(Top())

    val formals = List(selfRef, superRef)
    val typeformals = List()
    val argtypes = Some(List(selfType, superType))
    val returntype = Some(selfType)

    val d = Def(cls.partialConstructorPlaceholderName, formals, body, typeformals, argtypes, returntype)
    d
  }

  /** Given a ClassLiteral and a set of fields on this, build all field expressions for the class.
    *
    * Return the BoundVar for this and the mapping of fields to expressions.
    */
  private def convertClassLiteral(lit: ext.ClassLiteral, fieldNames: Set[String])(implicit ctx: TranslatorContext): (BoundVar, BoundVar, BoundVar, Map[Field, Expression]) = {
    import ctx._

    val thisName = lit.thisname.getOrElse("this")
    val thisVar = new BoundVar(Some(thisName))
    val superVar = new BoundVar(Some(id"super"))
    val privateVar = new BoundVar(Some(id"private"))

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
        val tmpField = uniqueField(s"tmp$p")
        // FIXME: Is it a problem if the same variable is bound in multiple subtrees? We can just call convertPattern with different bridges or modify
        // convertPattern to take an additional argument to the target closure it returns.

        // TODO: TYPECHECKER: If we modify convertPattern to save information from TypedPatterns we could type the below expressions.
        val x = new BoundVar(Some(tmpField.name))
        val (source, dcontext, target) = convertPattern(p, x)(newCtx)
        val newf = convert(f)(newCtx)
        val generatedFields = for ((name, code) <- dcontext.toSeq) yield (Field(name), Some(FieldAccess(privateVar, tmpField) > x > target(code)))
        (tmpField, Some(source(newf))) +: (generatedFields ++ convertFields(rest))
      case ext.ValSig(v, t) +: rest =>
        val field = Field(v)
        // TODO: TYPECHECKER: Store type for later use in type checking.
        (field, None) +: convertFields(rest)
      case ext.CallableSingle(defs, rest) =>
        assert(defs.forall(_.name == defs.head.name))
        val field = Field(defs.head.name)
        val name = new BoundVar(Some(id"${defs.head.name}"))
        val agg = defs.foldLeft(AggregateDef.empty(translator))(_ + _)
        if (agg.clauses.isEmpty) {
          // Abstract
          // TODO: TYPECHECKER: Store type for later use in type checking.
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

    (thisVar, superVar, privateVar, Map() ++ newbindings)
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

  /** Extend the provided context with class information.
    *
    */
  def makeClassContext(clss: Set[ClassInfo])(implicit ctx: TranslatorContext): TranslatorContext = {
    // This intermediate context is needed since isAbstract requires a context with all superclasses of the class we want to check.
    val tmpCtx = clss.foldLeft(ctx)((c, cls) => c.copy(classContext = c.classContext + (cls.name -> cls)))
    val newCtx = clss.foldLeft(ctx)((newCtx, cls) => {
      val newClassContext = newCtx.classContext + (cls.name -> cls)
      val newTypeContext = newCtx.typecontext + (cls.name -> cls.typeName)
      if (isAbstract(cls)(tmpCtx)) {
        newCtx.copy(boundDefs = newCtx.boundDefs + cls.partialConstructorName, classContext = newClassContext, typecontext = newTypeContext)
      } else {
        newCtx.copy(boundDefs = newCtx.boundDefs + cls.constructorName + cls.partialConstructorName, classContext = newClassContext, typecontext = newTypeContext)
      }
    })
    newCtx
  }

  private def patchCalls(implicit ctx: TranslatorContext) = new NamedASTTransform {
    override def onExpression(context: List[BoundVar], typecontext: List[BoundTypevar]): PartialFunction[Expression, Expression] = {
      // Patch calls for which the context contains a placeholder with defined captured variable information.
      case Call(target: BoundVar, args, targs) if ctx.classesByPlaceholder.contains(target) && ctx.classesByPlaceholder(target).capturedVariables.isDefined =>
        val info = ctx.classesByPlaceholder(target)
        Call(info.unplaceholder(target), (info.capturedVariables.get ++ args).toList, targs)
    }
  }

  private def patchDef(implicit ctx: TranslatorContext) = (d: Callable) => d match {
    case Def(name, formals, body, typeformals, argtypes, returntype) =>
      assert(ctx.classesByPlaceholder.contains(name))
      val info = ctx.classesByPlaceholder(name)
      assert(info.capturedVariables.isDefined)
      val captured = info.capturedVariables.get
      val addArgs = captured.map(v => new BoundVar(v.optionalVariableName.map(_ + "_lifted")))
      val newBody = (addArgs zip captured).foldLeft(body) { (e, substPair) =>
        val (a, x) = substPair
        assert(a != x)
        e.subst(a, x)
      }

      // FIXME: TYPECHECKER: We have a problem here since the types of the closed variables are not known.
      d ->> Def(info.unplaceholder(name), (addArgs ++ formals).toList, newBody, typeformals, argtypes, returntype)
    case d => d
  }

  /** Create the class declarations and return them in order.
    *
    * The provided context must already have the classes in clss (See makeClassContext).
    */
  def makeClassDeclarations(clss: Set[ClassInfo])(implicit ctx: TranslatorContext): (List[Callable], List[(BoundTypevar, Type)], TranslatorContext) = {
    if (clss.nonEmpty) {
      val clssOrdered = orderBySubclassing(clss.toList).toList
      val defs = clssOrdered.flatMap(generateConstructor(_)) ++ clssOrdered.map(generatePartialConstructor(_))

      val groups = FractionDefs.fraction(defs)
      val closedVariablesByGroup = groups.foldLeft(Map[List[Callable], Set[BoundVar]]()) { (soFar, g) =>
        // Find all the values closed over by the defs
        val direct = g.flatMap(_.freevars).toSet
        // Find any transitively closed values (within this class def set only)
        val indirectLocal = soFar.filter(p => direct.exists(d => p._1.exists(_.name == d))).values.flatten.toSet
        // Find any transitively closed values (from the context)
        val indirectContext = direct.flatMap(v => ctx.classesByPlaceholder.get(v).flatMap(_.capturedVariables).getOrElse(Seq()))
        // Combine the direct and indirect
        val closed = direct ++ indirectLocal ++ indirectContext

        soFar + (g -> closed)
      }

      val closedVariables = closedVariablesByGroup.flatMap(p => p._1.map(c => (c.name, p._2.toList)))

      val newCtx = ctx.copy(classContext = {
        val classPlaceholders = (ctx.classContext.values ++ clss).flatMap(i => Seq(i.constructorPlaceholderName, i.partialConstructorPlaceholderName))
        ctx.classContext.mapValues({ i =>
          if (clss.contains(i)) {
            val constructorClosed = closedVariables.getOrElse(i.constructorPlaceholderName, Seq())
            val partialClosed = closedVariables(i.partialConstructorPlaceholderName)
            val closed = partialClosed.toSet ++ constructorClosed -- classPlaceholders
            i.copy(capturedVariables = Some(closed.toSeq))
          } else {
            i
          }
        }).view.force
      })

      {
        implicit val ctx = newCtx

        Logger.fine(s"Class context with captures:\n${newCtx.classContext}\n${newCtx.classesByPlaceholder}")

        val patchCallsTrans = patchCalls(newCtx)

        // Patch all calls to provide the closed variables
        val defsPatched1 = defs.map(patchCallsTrans(_))

        // Patch the definitions to include and use the closed variables
        val defsPatched2 = defsPatched1.map(patchDef(newCtx))

        val types = clssOrdered map { (cls) =>
          val lin = generateLinearization(cls)
          val superTypes = lin.tail.map(_.typeName)
          // TODO: TYPECHECKER: This types all the members are not put in place here. They are not available in ClassInfo. They probably need to be.
          val addedMembers = (cls.concreteMembers ++ cls.abstractMembers).map((_, Top())).toMap
          val clsType = NominalType(IntersectionType(StructuralType(addedMembers) +: superTypes))
          (cls.typeName, clsType)
        }

        (defsPatched2, types, newCtx)
      }
    } else {
      (Nil, Nil, ctx)
    }
  }

  private def makeClassDeclarationExpression(clss: Set[ClassInfo])(bodyFunc: TranslatorContext => Expression)(implicit ctx: TranslatorContext): Expression = {
    val newCtx = makeClassContext(clss)(ctx)
    val (newDefs, newTypes, newCtx2) = makeClassDeclarations(clss)(newCtx)

    val core = DeclareCallables(newDefs, bodyFunc(newCtx2))
    newTypes.foldRight(core: Expression) { (p, acc) =>
      val (tv, t) = p
      DeclareType(tv, t, acc)
    }
  }

  /** Build a New expression from the extended equivalents.
    *
    * This will generate any needed synthetic classes as a DeclareClasses node wrapped around the New.
    */
  def makeNew(e: ext.ClassExpression)(implicit ctx: TranslatorContext): Expression = {
    val (cls, clss) = makeClassInfo(e)
    makeClassDeclarationExpression(clss ++ (if (ctx.classContext.contains(cls.name)) List() else List(cls))) { ctx =>
      if (ctx.boundDefs.contains(cls.constructorName)) {
        ctx.classContext(cls.name).capturedVariables match {
          case Some(captured) =>
            Call(cls.constructorName, captured.toList, Some(List()))
          case None =>
            Call(cls.constructorPlaceholderName, List(), Some(List()))
        }
      } else {
        val (_, allAbstractMembers) = getMembers(cls)(ctx)
        translator.reportProblem(InstantiatingAbstractClassException(allAbstractMembers.toList.map(_.name)) at e)
        Call(Constant(cls.name), List(), Some(List()))
      }
    }
  }

  /** Sort a sequence of ClassInfo objects.
    */
  private def orderBySubclassing(clss: Seq[ClassInfo]): Seq[ClassInfo] = {
    import orc.util.{ Direction, Graph, Node }

    if (clss.size == 1)
      return clss

    val nodes = for (cls <- clss) yield new Node(cls)
    val g = new Graph(nodes.toList)

    // Add edges in the graph.
    for {
      n1 <- g.nodes
      n2 <- g.nodes
      if (n1 != n2)
    } {
      val cls1 = n1.elem
      val cls2 = n2.elem
      if (cls1.superclasses contains cls2.name) {
        // Add edge from each class to all it's superclasses.
        g.addEdge(n1, n2)
      }
    }

    /* Do a DFS on the graph, ignoring the resulting forest*/
    g.depthSearch(Direction.Backward)
    /* Sort the elements of the graph in decreasing order
     * of their finish times. */
    g.sort

    g.nodes.map(_.elem)
  }

}
