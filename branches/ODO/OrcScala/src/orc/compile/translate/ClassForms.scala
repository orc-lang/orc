//
// Project OrcScala
//
// $Id$
//
// Created by dkitchin on Aug 5, 2010.
//
// Copyright (c) 2014 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//
package orc.compile.translate

import scala.language.reflectiveCalls

import orc.ast.ext
import orc.lib.builtin
import orc.error.compiletime._
import orc.error.OrcExceptionExtension._
import orc.ast.oil.named._
import orc.ast.ext.ExtendedASTTransform
import orc.values.Field

case class ClassInfo(name: Classvar, linearization: Class.Linearization)

/** @author dkitchin
  */
object ClassForms {
  /** Used to generate unique names for pattern match temporary fields.
    * 
    */
  var tmpId = 1
  // TODO: tmpId could be local to each class if we supported locals that are not visible from subclasses and don't conflict with them.

  /** Helper functions for class conversion
    */
  
  private def concatUniquePreferRight[T](a: List[T], b: List[T]): List[T] = a match {
    case v +: vs => if (b contains v) concatUniquePreferRight(vs, b) else v +: concatUniquePreferRight(vs, b)
    case Seq() => b
  }

  private def linearizeClasses(clss: Seq[ext.ClassDeclaration])(implicit context: Map[String, Argument], typecontext: Map[String, Type], classcontext: Map[String, ClassInfo], translator: Translator): (Map[String, ClassInfo], Seq[ClassFragment]) = {
    // Pretend these are state monads and it's functional. ;-)
    val ctx = scala.collection.mutable.Map[String, ClassInfo]().withDefault(classcontext)
    val additionalClasses = scala.collection.mutable.Map[ext.ClassExpression, ClassFragment]()

    def linearize(e: ext.ClassExpression): Class.Linearization = e match {
      case ext.ClassVariable(n) => ctx(n).linearization
      case e: ext.ClassLiteral => List(Classvar(additionalClasses.getOrElseUpdate(e, makeSyntheticClass(e)).name))
      case ext.ClassSubclassLiteral(s, b) => {
        val synthCls = additionalClasses.getOrElseUpdate(e, makeSyntheticClass(b))
        concatUniquePreferRight(List(Classvar(synthCls.name)), linearize(s))
      }
      case ext.ClassMixin(a, b) => concatUniquePreferRight(linearize(b), linearize(a))
    }
    for (c <- clss) {
      val name = c.name
      val v = Classvar(new BoundVar(Some("$cls$" + name)))
      val lin = v :: (c.superclass match {
        case Some(s) => linearize(s)
        case None => List()
      })
      ctx += name -> ClassInfo(v, lin)
    }
    (ctx.toMap, additionalClasses.values.toSeq)
  }
  
  def makeClassGroup(clss: Seq[ext.ClassDeclaration])(implicit context: Map[String, Argument], typecontext: Map[String, Type], classcontext: Map[String, ClassInfo], translator: Translator): (Seq[ClassFragment], Map[String, ClassInfo]) = {
    // Check for duplicate names
    for (c :: cs <- clss.tails) {
      cs.find(_.name == c.name) match {
        case Some(c2) => throw (DuplicateClassException(c.name) at c2)
        case None => ()
      }
    }

    // Build class names and linearizations
    val (classInfos, additionalClasses) = linearizeClasses(clss)

    val recursiveClassContext = classcontext ++ classInfos
    val newClss = for (c <- clss; ClassInfo(Classvar(name: BoundVar), _) = recursiveClassContext(c.name)) yield {
      ClassForms.makeClassFragment(name, c)(implicitly, implicitly, recursiveClassContext, implicitly)
    }
    
    (additionalClasses ++ newClss, recursiveClassContext)
  }
  
  def makeClassFragment(name: BoundVar, c: ext.ClassDeclaration)(implicit context: Map[String, Argument], typecontext: Map[String, Type], classcontext: Map[String, ClassInfo], translator: Translator): ClassFragment = {// Handle superclass and generate linearization
    val (self, fields) = convertClassLiteral(c.body)
    val cls = ClassFragment(name, self, fields)
    cls
  }
  
  def convertClassLiteral(lit: ext.ClassLiteral)(implicit context: Map[String, Argument], typecontext: Map[String, Type], classcontext: Map[String, ClassInfo], translator: Translator) = {
    import translator._
    val thisName = lit.thisname.getOrElse("this")
    val thisVar = new BoundVar(Some(thisName))
    
    val thisContext = List((thisName, thisVar), ("this", thisVar))
    
    val bindingVisitor = new ExtendedASTTransform {
      var names = Set[String]()
      
      override def onPattern() = {
        case p @ ext.VariablePattern(n) => names += n; p
        case p @ ext.AsPattern(pat, n) => names += n; this(pat); p
      }
      
      override def onDeclaration() = {
        case d: ext.Val => this(d.p); d
        case d: ext.NamedDeclaration => names += d.name; d
        case d => d
      }
    }
    lit.decls.foreach(bindingVisitor.apply)
    val fieldNames = bindingVisitor.names
    val fieldsContext = fieldNames.map(n => (n, new BoundVar(Some(n))))
    
    val memberContext = context ++ thisContext ++ fieldsContext

    def convertFields(d: Seq[ext.Declaration]): Seq[(Field, Expression)] = d match {
      case ext.Val(ext.VariablePattern(x), f) +: rest =>
        (Field(x), convert(f)(memberContext, implicitly, implicitly)) +: convertFields(rest)
      case ext.Val(p, f) +: rest =>
        val tmpField = Field(s"$$pattmp$$$tmpId")
        tmpId += 1
        // FIXME: Is it a problem if the same variable is bound in multiple subtrees?
        val x = new BoundVar()
        val (source, dcontext, target) = convertPattern(p, x)(memberContext, implicitly)
        val newf = convert(f)(memberContext, implicitly, implicitly)
        val generatedFields = for ((name, code) <- dcontext.toSeq) yield (Field(name), FieldAccess(thisVar, tmpField) > x > target(code))
        (tmpField, source(newf)) +: (generatedFields ++ convertFields(rest))
      case ext.CallableSingle(defs, rest) =>
        assert(defs.forall(_.name == defs.head.name))
        val field = Field(defs.head.name)
        val name = new BoundVar(Some("$" + defs.head.name))
        val agg = defs.foldLeft(AggregateDef.empty(translator))(_ + _)

        val newdef = agg ->> agg.convert(name, memberContext, implicitly, implicitly)
        (field, DeclareCallables(List(newdef), name)) +: convertFields(rest)
      case Seq() =>
        Nil
      case decl :: _ =>
        throw (MalformedExpression("Invalid declaration form in class") at decl)
    }
    
    def bindMembers(e: Expression) = {
      fieldsContext.foldRight(e) { (binding, e) =>
        val (name, x) = binding 
        Graft(x, FieldAccess(thisVar, Field(name)), e)
      }
    }
    
    val newbindings = convertFields(lit.decls).toMap.mapValues(bindMembers)
    (thisVar, Map() ++ newbindings)
  }
  
  def makeNew(newe: ext.Expression, e: ext.ClassExpression)(implicit context: Map[String, Argument], typecontext: Map[String, Type], classcontext: Map[String, ClassInfo], translator: Translator): Expression = {
    e match {
      case ext.ClassVariable(n) => newe ->> New(classcontext(n).linearization)
      case lit: ext.ClassLiteral => {
        val cls = makeSyntheticClass(lit)

        e ->> DeclareClasses(List(cls), newe ->> New(List(Classvar(cls.name))))
      }
      case _: ext.ClassMixin | _: ext.ClassSubclassLiteral =>
        val tmpClsname = "$$"
        val replacement = ext.ClassDeclaration(tmpClsname, Some(e), ext.ClassLiteral(None, Nil))
        
        val (infos, clss) = linearizeClasses(List(replacement))
        
        e ->> DeclareClasses(clss.toList, newe ->> New(infos(tmpClsname).linearization.tail))
   }
  }

  private def makeSyntheticClass(lit: ext.ClassLiteral)(implicit context: Map[String, Argument], typecontext: Map[String, Type], classcontext: Map[String, ClassInfo], translator: Translator): ClassFragment = {
    val (self, fields) = convertClassLiteral(lit)
    val clsname = new BoundVar(Some(Var.getNextVariableName("synCls")))
    val cls = lit ->> ClassFragment(clsname, self, fields)
    cls
  }
}
