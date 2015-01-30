//
// OrcXML.scala -- Scala object OrcXML
// Project OrcScala
//
// $Id$
//
// Created by amshali on Jul 12, 2010.
//
// Copyright (c) 2015 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//
package orc.ast.oil.xml

import scala.language.implicitConversions
import orc.compile.parse.OrcPosition
import orc.compile.parse.PositionWithFilename
import scala.util.parsing.input.NoPosition
import scala.util.parsing.input.Position
import scala.xml._
import orc.ast.oil.nameless._
import scala.collection.immutable.HashMap
import scala.math.BigInt
import scala.math.BigDecimal
import orc.ast.hasOptionalVariableName
import orc.error.loadtime.OilParsingException
import orc.values.Field

object OrcXML {

  /** Add a metadata-preserving combinator --> for conversions
    * between XML and Orc's nameless ASTs, analogous to the
    * -> conversion between ASTs.
    */
  class NodeWithArrow(xml: Node) {
    def -->[B <: orc.ast.AST](f: Node => B) = {
      val ast = f(xml)

      // Extract position information, if available
      (xml \ "@pos").text.split(":").toList match {
        case List(file, line, col) => {
          val l = Integer.parseInt(line)
          val c = Integer.parseInt(col)
          val pos = new PositionFilenameLineCol(file, l, c)
          ast.setPos(pos)
        }
        case _ => {}
      }

      // Extract optional name information, if applicable and available
      ast match {
        case x: hasOptionalVariableName => {
          val n = xml \ "@varname"
          x.optionalVariableName = {
            if (n.isEmpty) { None } else { Some(n.text) }
          }
        }
        case _ => {}
      }

      ast
    }
  }

  class AstWithArrow(ast: orc.ast.AST) {
    def -->(f: orc.ast.AST => Elem) = {
      val xml = f(ast)

      // Get position information, if available
      val posAttribute =
        ast.pos match {
          case NoPosition => scala.xml.Null
          case p => new UnprefixedAttribute("pos", p.toString, scala.xml.Null)
        }

      // Get optional name information, if applicable and available
      val nameAttribute =
        ast match {
          case x: hasOptionalVariableName => {
            x.optionalVariableName match {
              case None => scala.xml.Null
              case Some(n) => new UnprefixedAttribute("varname", n, scala.xml.Null)
            }
          }
          case _ => scala.xml.Null
        }

      xml.copy(attributes = xml.attributes.append(posAttribute).append(nameAttribute))
    }
  }

  implicit def addArrow(xml: Node): NodeWithArrow = new NodeWithArrow(xml)
  implicit def addArrow(ast: orc.ast.AST): AstWithArrow = new AstWithArrow(ast)

  val oilNamespace = "http://orc.csres.utexas.edu/oil.xsd"
  val oilXSD = "http://orc.csres.utexas.edu/oil.xsd"

  def createdBy = orc.Main.orcImplName + " v" + orc.Main.orcVersion

  def astToXml(ast: Expression): Elem = {
    val xmlout =
      <oil xmlns={ oilNamespace } xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation={ oilXSD } created-by={ createdBy }>
        { toXML(ast) }
      </oil>
    trimElem(xmlout)
  }

  @throws(classOf[OilParsingException])
  def xmlToAst(xml: Node): Expression = {
    val root: Seq[Elem] = xml collect { case e: Elem => trimElem(e) }
    root.toList match {
      case List(<oil>{ body }</oil>) => fromXML(body)
      case _ => throw new OilParsingException("Root element must be <oil>...</oil> and must be unique")
    }
  }

  def readOilFromStream(source: java.io.InputStream): Expression = {
    xmlToAst(XML.load(source))
  }

  def writeOilToStream(ast: Expression, dest: java.io.OutputStream) {
    val writer = new java.io.OutputStreamWriter(dest)
    val node = astToXml(ast)
    val xml = Utility.serialize(node, preserveWhitespace = true, minimizeTags = MinimizeMode.Always).toString
    writer.write("<?xml version='1.0' encoding='UTF-8'?>\n")
    writer.write(xml)
    writer.close()
  }

  def trimElem(x: Elem): Elem = {
    x match {
      /* Strings are protected from trim operations */
      case <string>{ _* }</string> => x
      case _ => {
        val noWhitespace = x.child filter
          {
            case t: Text if t.text.trim == "" => false
            case _ => true
          }
        val newchild = noWhitespace map
          {
            case e: Elem => trimElem(e)
            case y => y
          }
        x.copy(child = newchild)
      }
    }
  }

  def toXML(e: NamelessAST): Elem = {
    e --> {
      case Stop() => <stop/>
      case Call(target, args, typeArgs) =>
        <call>
          <target>{ toXML(target) }</target>
          <args>{ args map toXML }</args>
          {
            typeArgs match {
              case Some(ts) => <typeargs>{ ts map toXML }</typeargs>
              case None => Nil
            }
          }
        </call>
      case Parallel(left, right) =>
        <parallel>
          <left>{ toXML(left) }</left>
          <right>{ toXML(right) }</right>
        </parallel>
      case Sequence(left, right) =>
        <sequence>
          <left>{ toXML(left) }</left>
          <right>{ toXML(right) }</right>
        </sequence>
      case Graft(value, body) =>
        <graft>
          <left>{ toXML(value) }</left>
          <right>{ toXML(body) }</right>
        </graft>
      case Trim(e) =>
        <trim>
          <expr>{ toXML(e) }</expr>
        </trim>
      case Otherwise(left, right) =>
        <otherwise>
          <left>{ toXML(left) }</left>
          <right>{ toXML(right) }</right>
        </otherwise>
      case FieldAccess(o, f) =>
        <fieldaccess name={ f.field }>
          <expr>{ toXML(o) }</expr>
        </fieldaccess>
      case New(os) =>
        <new>
          { os.map(toXML) }
        </new>
      case Classvar(i) => <classvar index={ i.toString }/>
      case Class(bindings) => 
        <classfragment>
          {
            for ((n, e) <- bindings) yield <binding name={ n.field }><expr>{ toXML(e) }</expr></binding>
          }
        </classfragment>
      case DeclareClasses(clss, body: Expression) =>
        <declareclasses>
          <classes>{ clss map toXML }</classes>
          <body>{ toXML(body) }</body>
        </declareclasses>
      case DeclareCallables(unclosedVars, defs, body: Expression) =>
        <declaredefs>
          <unclosedvars>{ unclosedVars mkString " " }</unclosedvars>
          <defs>{ defs map toXML }</defs>
          <body>{ toXML(body) }</body>
        </declaredefs>
      case DeclareType(t: Type, body: Expression) =>
        <declaretype>
          <type>{ toXML(t) }</type>
          <body>{ toXML(body) }</body>
        </declaretype>
      case HasType(body: Expression, expectedType: Type) =>
        <hastype>
          <body>{ toXML(body) }</body>
          <expectedtype>{ toXML(expectedType) }</expectedtype>
        </hastype>
      case Constant(v: Any) => <constant>{ anyToXML(v) }</constant>
      case Constant(null) => <constant><nil/></constant>
      case Variable(i: Int) => <variable index={ i.toString }/>
      case VtimeZone(timeOrder, body) =>
        <vtimezone>
          <timeorder>{ toXML(timeOrder) }</timeorder>
          <body>{ toXML(body) }</body>
        </vtimezone>
      case Hole(context, typecontext) =>
        <hole>
          <context>
            {
              for ((n, a) <- context) yield <binding name={ n }>{ toXML(a) }</binding>
            }
          </context>
          <typecontext>
            {
              for ((n, t) <- context) yield <binding name={ n }>{ toXML(t) }</binding>
            }
          </typecontext>
        </hole>
      case Top() => <top/>
      case Bot() => <bot/>
      case TypeVar(i) => <typevar index={ i.toString }/>
      case TupleType(elements) =>
        <tupletype>
          { elements map toXML }
        </tupletype>
      case RecordType(entries) =>
        <recordtype>
          {
            for ((n, t) <- entries) yield <entry name={ n }>{ toXML(t) }</entry>
          }
        </recordtype>
      case TypeApplication(tycon: Int, typeactuals) =>
        <typeapplication>
          <typeconst index={ tycon.toString }/>
          <typeactuals>{ typeactuals map toXML }</typeactuals>
        </typeapplication>
      case AssertedType(assertedType: Type) =>
        <assertedtype>
          { toXML(assertedType) }
        </assertedtype>
      case FunctionType(typeFormalArity: Int, argTypes, returnType: Type) =>
        <functiontype typearity={ typeFormalArity.toString }>
          <argtypes>{ argTypes map toXML }</argtypes>
          <returntype>{ toXML(returnType) }</returntype>
        </functiontype>
      case TypeAbstraction(typeFormalArity: Int, t: Type) =>
        <typeabstraction typearity={ typeFormalArity.toString }>
          { toXML(t) }
        </typeabstraction>
      case ImportedType(classname: String) =>
        <importedtype>
          { classname }
        </importedtype>
      case ClassType(classname: String) =>
        <classtype>
          { classname }
        </classtype>
      case VariantType(typeFormalArity, variants) =>
        <varianttype typearity={ typeFormalArity.toString }>
          {
            for ((n, ts) <- variants) yield <variant name={ n }>{ ts map toXML }</variant>
          }
        </varianttype>
      case Def(typeFormalArity: Int, arity: Int, body: Expression, argTypes, returnType) =>
        <definition typearity={ typeFormalArity.toString } arity={ arity.toString }>
          <body>{ toXML(body) }</body>
          {
            argTypes match {
              case Some(ts) => <argtypes>{ ts map toXML }</argtypes>
              case None => Nil
            }
          }
          {
            returnType match {
              case Some(t) => <returntype>{ toXML(t) }</returntype>
              case None => Nil
            }
          }
        </definition>
      case Site(typeFormalArity: Int, arity: Int, body: Expression, argTypes, returnType) =>
        <sitedefinition typearity={ typeFormalArity.toString } arity={ arity.toString }>
          <body>{ toXML(body) }</body>
          {
            argTypes match {
              case Some(ts) => <argtypes>{ ts map toXML }</argtypes>
              case None => Nil
            }
          }
          {
            returnType match {
              case Some(t) => <returntype>{ toXML(t) }</returntype>
              case None => Nil
            }
          }
        </sitedefinition>
      case _ => throw new AssertionError("Invalid Node for XML conversion!")
    }
  }

  def anyToXML(a: Any): Elem = {
    a match {
      case i @ (_: Int | _: Short | _: Long | _: Char | _: BigInt) => <integer>{ i.toString() }</integer>
      case n @ (_: Float | _: Double | _: BigDecimal) => <number>{ n.toString() }</number>
      case s: String => <string>{ s }</string>
      case true => <true/>
      case false => <false/>
      case orc.values.Signal => <signal/>
      case orc.values.Field(s) => <field>{ s }</field>
      case x: orc.values.sites.JavaClassProxy => <jclassproxy>{ x.name }</jclassproxy>
      case x: orc.values.sites.Site =>
        <site>{ strip$(a.asInstanceOf[AnyRef].getClass().getName) }</site>
      case _ => throw new AssertionError("Could not serialize value " + a.toString + " to XML.")
    }
  }

  def strip$(s: String): String = {
    if (s.charAt(s.length - 1) == '$') s.substring(0, s.length - 1) else s
  }

  @throws(classOf[OilParsingException])
  def anyRefFromXML(inxml: scala.xml.Node): AnyRef = {
    inxml match {
      case <integer>{ i @ _* }</integer> => BigInt(i.text.trim)
      case <number>{ n @ _* }</number> => BigDecimal(n.text.trim)
      case <string>{ s @ _* }</string> => new String(s.text)
      case <true/> => java.lang.Boolean.TRUE
      case <false/> => java.lang.Boolean.FALSE
      case <signal/> => orc.values.Signal
      case <field>{ s @ _* }</field> => orc.values.Field(s.text.trim)
      case <nil/> => null
      case <jclassproxy>{ x @ _* }</jclassproxy> =>
        orc.values.sites.JavaSiteForm.resolve(x.text.trim)
      case <site>{ c @ _* }</site> => {
        orc.values.sites.OrcSiteForm.resolve(c.text.trim)
      }
      case other => throw new OilParsingException("XML fragment " + other + " could not be converted to an Orc value")
    }
  }

  class PositionFilenameLineCol(fn: String, l: Int, c: Int) extends PositionWithFilename {
    override val filename = fn
    override def line = l
    override def column = c
    override def lineContents = ""
    override def toString = filename + ":" + line + ":" + column
  }

  @throws(classOf[OilParsingException])
  def fromXML(xml: scala.xml.Node): Expression = {
    xml --> {
      case <stop/> => Stop()
      case <parallel><left>{ left }</left><right>{ right }</right></parallel> =>
        Parallel(fromXML(left), fromXML(right))
      case <sequence><left>{ left }</left><right>{ right }</right></sequence> =>
        Sequence(fromXML(left), fromXML(right))
      case <graft><left>{ value }</left><right>{ body }</right></graft> =>
        Graft(fromXML(value), fromXML(body))
      case <trim><expr>{ expr }</expr></trim> =>
        Trim(fromXML(expr))
      case <otherwise><left>{ left }</left><right>{ right }</right></otherwise> =>
        Otherwise(fromXML(left), fromXML(right))
      case <fieldaccess><expr>{ obj }</expr></fieldaccess> =>
        FieldAccess(argumentFromXML(obj), Field((xml \ "@name").text))
      case <new>{ os @ _* }</new> =>
        New(os.map(objectStructureFromXML).toList)
      case <declareclasses><classes>{ clss @ _* }</classes><body>{ body }</body></declareclasses> => {
        val t2 = for (d <- clss) yield classFromXML(d)
        val t3 = fromXML(body)
        DeclareClasses(t2.toList, t3)
      }
      case <declaredefs><unclosedvars>{ uvars @ _* }</unclosedvars><defs>{ defs @ _* }</defs><body>{ body }</body></declaredefs> => {
        val t1 = {
          uvars.text.split(" ").toList match {
            case List("") => Nil
            case xs => xs map { _.toInt }
          }
        }
        val t2 = for (d <- defs) yield defFromXML(d)
        val t3 = fromXML(body)
        DeclareCallables(t1, t2.toList, t3)
      }
      case <call><target>{ target }</target><args>{ args @ _* }</args>{ maybeTypeargs @ _* }</call> => {
        val t1 = argumentFromXML(target)
        val t2 = for (a <- args) yield argumentFromXML(a)
        val t3 = maybeTypeargs match {
          case <typeargs>{ typeargs @ _* }</typeargs> => {
            val ts = for (t <- typeargs) yield typeFromXML(t)
            Some(ts.toList)
          }
          case _ => None
        }
        Call(t1, t2.toList, t3)
      }
      case <declaretype><type>{ atype }</type><body>{ body }</body></declaretype> => {
        DeclareType(typeFromXML(atype), fromXML(body))
      }
      case <hastype><body>{ body }</body><expectedtype>{ expectedType }</expectedtype></hastype> => {
        HasType(fromXML(body), typeFromXML(expectedType))
      }
      case <vtimezone><timeorder>{ timeOrder }</timeorder><body>{ body }</body></vtimezone> =>
        VtimeZone(argumentFromXML(timeOrder), fromXML(body))
      case <hole><context>{ ctx @ _* }</context><typecontext>{ typectx @ _* }</typecontext></hole> => {
        val context = HashMap.empty ++ {
          for (b @ <binding>{ a }</binding> <- ctx) yield ((b \ "@name").text, argumentFromXML(a))
        }
        val typecontext = HashMap.empty ++ {
          for (b @ <binding>{ t }</binding> <- typectx) yield ((b \ "@name").text, typeFromXML(t))
        }
        Hole(context, typecontext)
      }
      case <constant>{ c }</constant> => Constant(anyRefFromXML(c))
      case x @ <variable/> => Variable((x \ "@index").text.toInt)
      case other => throw new OilParsingException("XML fragment " + other + " could not be converted to an Orc expression")
    }
  }

  @throws(classOf[OilParsingException])
  def argumentFromXML(xml: scala.xml.Node): Argument = {
    xml --> {
      case <constant>{ c }</constant> => Constant(anyRefFromXML(c))
      case x @ <variable/> => Variable((x \ "@index").text.toInt)
      case other => throw new OilParsingException("XML fragment " + other + " could not be converted to an Orc argument")
    }
  }

  @throws(classOf[OilParsingException])
  def objectStructureFromXML(xml: scala.xml.Node): Classvar = {
    xml --> {
      case <classvar/> => Classvar((xml \ "@index").text.toInt)
      case other => throw new OilParsingException("XML fragment " + other + " could not be converted to an ObjectStructure")
    }
  }

  @throws(classOf[OilParsingException])
  def classFromXML(xml: scala.xml.Node): Class = {
    xml --> {
      case <classfragment>{ bindings @ _* }</classfragment> => {
        val bs = for (x @ <binding><expr>{ xmle }</expr></binding> <- bindings) yield {
          (Field((x \ "@name").text), fromXML(xmle))
        }
        Class(bs.toMap)
      }
      case other => throw new OilParsingException("XML fragment " + other + " could not be converted to a Class")
    }
  }

  @throws(classOf[OilParsingException])
  def defFromXML(xml: scala.xml.Node): Callable = {
    def buildCallable(d: scala.xml.Node, body: scala.xml.Node, rest: Seq[scala.xml.Node], constructor: (Int, Int, Expression, Option[List[Type]], Option[Type]) => Callable) = {
      val typeFormalArity = (d \ "@typearity").text.toInt
      val arity = (d \ "@arity").text.toInt
      val argTypes = rest \ "argtypes" match {
        case <argtypes>{ argTypes @ _* }</argtypes> => Some(argTypes.toList map typeFromXML)
        case _ => None
      }
      val returnType = rest \ "returntype" match {
        case <returntype>{ returnType }</returntype> => Some(typeFromXML(returnType))
        case _ => None
      }
      constructor(typeFormalArity, arity, fromXML(body), argTypes, returnType)
    }

    xml --> {
      case d @ <definition><body>{ body }</body>{ rest @ _* }</definition> => {
        buildCallable(d, body, rest, Def)
      }
      case d @ <sitedefinition><body>{ body }</body>{ rest @ _* }</sitedefinition> => {
        buildCallable(d, body, rest, Site)
      }
      case other => throw new OilParsingException("XML fragment " + other + " could not be converted to an Orc definition")
    }
  }

  @throws(classOf[OilParsingException])
  def typeFromXML(xml: scala.xml.Node): Type = {
    xml --> {
      case <top/> => Top()
      case <bot/> => Bot()
      case u @ <typevar/> => TypeVar((u \ "@index").text.toInt)
      case <tupletype>{ elements @ _* }</tupletype> => {
        TupleType(elements.toList map typeFromXML)
      }
      case <recordtype>{ entries @ _* }</recordtype> => {
        var t1: Map[String, Type] = Map.empty
        for (<entry><name>{ n @ _* }</name><rtype>{ t }</rtype></entry> <- entries)
          t1 += n.text.trim -> typeFromXML(t)
        RecordType(t1)
      }
      case <typeapplication>{ (tc @ <typeconst/>) }<typeactuals>{ tactuals @ _* }</typeactuals></typeapplication> => {
        val ts = tactuals.toList map typeFromXML
        TypeApplication((tc \ "@index").text.toInt, ts)
      }
      case <assertedtype>{ assertedType }</assertedtype> =>
        AssertedType(typeFromXML(assertedType))
      case ty @ <functiontype><argtypes>{ argtypes @ _* }</argtypes><returntype>{ returnType }</returntype></functiontype> => {
        val typeFormalArity = (ty \ "@typearity").text.toInt
        FunctionType(typeFormalArity,
          argtypes.toList map typeFromXML,
          typeFromXML(returnType))
      }
      case ty @ <typeabstraction>{ t }</typeabstraction> => {
        val typeFormalArity = (ty \ "@typearity").text.toInt
        TypeAbstraction(typeFormalArity, typeFromXML(t))
      }
      case <importedtype>{ classname @ _* }</importedtype> =>
        ImportedType(classname.text.trim)
      case <classtype>{ classname @ _* }</classtype> => ClassType(classname.text.trim)
      case vt @ <varianttype>{ variants @ _* }</varianttype> => {
        val typeFormalArity = (vt \ "@typearity").text.toInt
        val newVariants =
          for (v @ <variant>{ params @ _* }</variant> <- variants) yield {
            ((v \ "@name").text, params.toList map typeFromXML)
          }
        VariantType(typeFormalArity, newVariants.toList)
      }
      case other => throw new OilParsingException("XML fragment " + other + " could not be converted to an Orc type")
    }
  }

}
