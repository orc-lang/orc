//
// OrcXML.scala -- Scala object OrcXML
// Project OrcScala
//
// Created by amshali on Jul 12, 2010.
//
// Copyright (c) 2018 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.ast.oil.xml

import java.net.URI

import scala.collection.immutable.HashMap
import scala.language.implicitConversions
import scala.xml.{ Elem, MinimizeMode, Node }
import scala.xml.{ Text, UnprefixedAttribute, Utility, XML }
import scala.xml.NodeSeq.seqToNodeSeq

import orc.ast.hasOptionalVariableName
import orc.ast.oil.nameless.{ Argument, AssertedType, Bot, Call, Callable, ClassType, Constant, DeclareCallables, DeclareType, Def, Expression, FieldAccess, FunctionType, Graft, HasType, Hole, ImportedType, IntersectionType, NamelessAST, New, NominalType, Otherwise, Parallel, RecordType, Sequence, Site, Stop, StructuralType, Top, Trim, TupleType, Type, TypeAbstraction, TypeApplication, TypeVar, UnionType, Variable, VariantType, VtimeZone }
import orc.compile.parse.{ OrcInputContext, OrcSourcePosition, OrcSourceRange }
import orc.error.loadtime.OilParsingException
import orc.values.{ Field, NumericsConfig }

object OrcXML {

  /** Add a metadata-preserving combinator --> for conversions
    * between XML and Orc's nameless ASTs, analogous to the
    * -> conversion between ASTs.
    */
  class NodeWithArrow(xml: Node) {
    def -->[B <: orc.ast.AST](f: Node => B) = {
      val ast = f(xml)

      //FIXME: Move to orc.util.TetxRange
      //FIXME: Make robust to filenames with - and :
      // Extract position information, if available
      (xml \ "@pos").text.split("-").toList.map({ _.split(":").toList }) match {
        case List(List(file, line, col)) => {
          val l = Integer.parseInt(line)
          val c = Integer.parseInt(col)
          val pos = new PlaceholderSourceRange(file, l, c, file, l, c)
          ast.fillSourceTextRange(Some(pos))
        }
        case List(List(file, line, colStart), List(colEnd)) => {
          val l = Integer.parseInt(line)
          val cs = Integer.parseInt(colStart)
          val ce = Integer.parseInt(colEnd)
          val pos = new PlaceholderSourceRange(file, l, cs, file, l, ce)
          ast.fillSourceTextRange(Some(pos))
        }
        case List(List(file, lineStart, colStart), List(lineEnd, colEnd)) => {
          val ls = Integer.parseInt(lineStart)
          val le = Integer.parseInt(lineEnd)
          val cs = Integer.parseInt(colStart)
          val ce = Integer.parseInt(colEnd)
          val pos = new PlaceholderSourceRange(file, ls, cs, file, le, ce)
          ast.fillSourceTextRange(Some(pos))
        }
        case List(List(fileStart, lineStart, colStart), List(fileEnd, lineEnd, colEnd)) => {
          val ls = Integer.parseInt(lineStart)
          val le = Integer.parseInt(lineEnd)
          val cs = Integer.parseInt(colStart)
          val ce = Integer.parseInt(colEnd)
          val pos = new PlaceholderSourceRange(fileStart, ls, cs, fileEnd, le, ce)
          ast.fillSourceTextRange(Some(pos))
        }
        case _ => { /* Can't parse pos attribute, disregarding */ }
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
        if (ast.sourceTextRange.isEmpty)
          scala.xml.Null
        else
          new UnprefixedAttribute("pos", ast.sourceTextRange.get.toString, scala.xml.Null)

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
        <fieldaccess name={ f.name }>
          <expr>{ toXML(o) }</expr>
        </fieldaccess>
      case New(st, bindings, t) =>
        <new>
          {
            st match {
              case Some(t) => <selftype>{ toXML(t) }</selftype>
              case None => Nil
            }
          }
          {
            t match {
              case Some(t) => <objtype>{ toXML(t) }</objtype>
              case None => Nil
            }
          }
          {
            for ((n, e) <- bindings) yield <binding name={ n.name }><expr>{ toXML(e) }</expr></binding>
          }
        </new>
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
      case IntersectionType(a, b) =>
        <intersectiontype>
          <left>{ toXML(a) }</left>
          <right>{ toXML(b) }</right>
        </intersectiontype>
      case UnionType(a, b) =>
        <uniontype>
          <left>{ toXML(a) }</left>
          <right>{ toXML(b) }</right>
        </uniontype>
      case NominalType(t) =>
        <nominaltype>
          <arg>{ toXML(t) }</arg>
        </nominaltype>
      case StructuralType(members) =>
        <structuraltype>
          {
            for ((n, t) <- members) yield <entry name={ n.name }>{ toXML(t) }</entry>
          }
        </structuraltype>
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
      case null => <nil/>
      case true => <true/>
      case false => <false/>
      case orc.values.Signal => <signal/>
      case orc.values.Field(s) => <field>{ s }</field>
      case x: java.lang.Class[_] => <jclass>{ x.getCanonicalName() }</jclass>
      case x: orc.values.sites.Site =>
        <site>{ strip$(a.asInstanceOf[AnyRef].getClass().getName) }</site>
      case x: orc.values.HasMembers =>
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
      case <integer>{ i @ _* }</integer> => NumericsConfig.toOrcIntegral(i.text.trim)
      case <number>{ n @ _* }</number> => NumericsConfig.toOrcFloatingPoint(n.text.trim)
      case <string>{ s @ _* }</string> => new String(s.text)
      case <true/> => java.lang.Boolean.TRUE
      case <false/> => java.lang.Boolean.FALSE
      case <signal/> => orc.values.Signal
      case <field>{ s @ _* }</field> => orc.values.Field(s.text.trim)
      case <nil/> => null
      case <jclass>{ x @ _* }</jclass> =>
        orc.values.sites.JavaSiteForm.resolve(x.text.trim)
      case <site>{ c @ _* }</site> => {
        orc.values.sites.OrcSiteForm.resolve(c.text.trim)
      }
      case other => throw new OilParsingException("XML fragment " + other + " could not be converted to an Orc value")
    }
  }

  /** An OrcInputContext that can only report its name. */
  class PlaceholderResource(override val descr: String) extends OrcInputContext {
    override def hashCode = descr.hashCode
    override def equals(that: Any) = that match {
      case thatPR: PlaceholderResource => this.descr == thatPR.descr
      case _ => false
    }

    private def stubbed() = throw new UnsupportedOperationException("PlaceholderResource cannot perform this operation")

    override val reader = null
    override def toURI = stubbed()
    override def toURL = stubbed()
    override protected def resolve(baseURI: URI, pathElements: String*) = stubbed()
    override def newInputFromPath(pathElements: String*) = stubbed()
    override def lineColForCharNumber(charNum: CharacterNumber) = (0, 0) //stubbed()
    override def lineText(startLineNum: LineNumber, endLineNum: LineNumber) = stubbed()
    override def lineText(lineNum: LineNumber) = stubbed()
  }

  /** An OrcSourcePosition that only knows its filename, line, and column.
    * PlaceholderPositions are assigned a synthetic (fake) offset for comparison purposes.
    * PlaceholderPositions do not have any line content.
    */
  class PlaceholderPosition(filename: String, override val line: Int, override val column: Int) extends OrcSourcePosition(new PlaceholderResource(filename), (line - 1) * 4096 + (column - 1)) with Serializable {
    override protected def getLineCol() = (line, column)
    override def lineContent: String = ""
    override def lineContentWithCaret = ""

    @throws(classOf[java.io.ObjectStreamException])
    protected def writeReplace(): AnyRef = {
        new PlaceholderPositionMarshalingReplacement(resource.descr, line, column)
    }
  }

  protected case class PlaceholderPositionMarshalingReplacement(val filename: String, val line: Int, val column: Int) {
    @throws(classOf[java.io.ObjectStreamException])
    protected def readResolve(): AnyRef = new PlaceholderPosition(filename, line, column)
  }

  /** An OrcSourceRange made of two PlaceholderPositions. */
  class PlaceholderSourceRange(filenameStart: String, lineStart: Int, columnStart: Int, filenameEnd: String, lineEnd: Int, columnEnd: Int)
      extends OrcSourceRange(
        (new PlaceholderPosition(filenameStart, lineStart, columnStart), new PlaceholderPosition(filenameEnd, lineEnd, columnEnd))
      ) with Serializable {
    override def lineContent: String = ""
    override def lineContentWithCaret = ""

    @throws(classOf[java.io.ObjectStreamException])
    protected def writeReplace(): AnyRef = {
        new PlaceholderSourceRangeMarshalingReplacement(start.resource.descr, start.line, start.column, end.resource.descr, end.line, end.column)
    }
  }

  protected case class PlaceholderSourceRangeMarshalingReplacement(filenameStart: String, lineStart: Int, columnStart: Int, filenameEnd: String, lineEnd: Int, columnEnd: Int) {
    @throws(classOf[java.io.ObjectStreamException])
    protected def readResolve(): AnyRef = new PlaceholderSourceRange(filenameStart, lineStart, columnStart, filenameEnd, lineEnd, columnEnd)
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
        FieldAccess(argumentFromXML(obj), Field((xml \ "@name").text.trim))
      case <new>{ rest @ _* }</new> =>
        val sts = rest.filter(_.label == "selftype")
        val ots = rest.filter(_.label == "objtype")
        val bs = rest.filter(_.label == "binding")
        val selfType = sts.headOption match {
          case Some(<selftype>{ st }</selftype>) => Some(typeFromXML(st))
          case _ => None
        }
        val objType = ots.headOption match {
          case Some(<objtype>{ st }</objtype>) => Some(typeFromXML(st))
          case _ => None
        }
        val bindings = for (b <- bs) yield {
          val <binding><expr>{e}</expr></binding> = b
          val name = (b \ "@name").text.trim
          (Field(name), fromXML(e))
        }
        New(selfType, bindings.toMap, objType)
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
      case <intersectiontype><left>{ e1 }</left><right>{ e2 }</right></intersectiontype> =>
        IntersectionType(typeFromXML(e1), typeFromXML(e2))
      case <uniontype><left>{ e1 }</left><right>{ e2 }</right></uniontype> =>
        UnionType(typeFromXML(e1), typeFromXML(e2))
      case <nominaltype><arg>{ element }</arg></nominaltype> =>
        NominalType(typeFromXML(element))
      case <structuraltype>{ entries @ _* }</structuraltype> => {
        var t1: Map[Field, Type] = Map.empty
        for (v @ <entry>{ t }</entry> <- entries)
          t1 += Field((v \ "@name").text.trim) -> typeFromXML(t)
        StructuralType(t1)
      }
      case other => throw new OilParsingException("XML fragment " + other + " could not be converted to an Orc type")
    }
  }

}
