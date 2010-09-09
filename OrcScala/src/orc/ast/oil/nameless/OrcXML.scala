//
// OrcXML.scala -- Scala object OrcXML
// Project OrcScala
//
// $Id$
//
// Created by amshali on Jul 12, 2010.
//
// Copyright (c) 2010 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//
package orc.ast.oil.nameless

import orc.compile.parse.OrcPosition
import orc.compile.parse.PositionWithFilename
import scala.util.parsing.input.NoPosition
import scala.util.parsing.input.Position
import scala.xml._
import orc.ast.oil.nameless._
import scala.collection.immutable.HashMap


object OrcXML {

  def writeXML(e: NamelessAST): Elem = {
    val xmlout = <orc>
    {toXML(e)}
    </orc>
    trimElem(xmlout) 
  }
  
  def readOilFromStream(source: java.io.InputStream) : Expression = {
    fromXML(XML.load(source))
  }
  
  def writeOilToStream(oil : Expression, dest: java.io.OutputStream) : Unit = {
    XML.write(new java.io.OutputStreamWriter(dest), writeXML(oil), "UTF-8", true, null)
  }
  
  def trimElem(x: Elem): Elem =  {
      val nc = x.child filter {a => 
          if (a.isInstanceOf[Text] && a.text.trim == "") false
          else true
      } map {a => if (a.isInstanceOf[Elem]) trimElem(a.asInstanceOf[Elem]) else a}
      x.copy(child=nc)
  }
  
  def posToString(p : Position) : String = {
    if (p == NoPosition) ""
    else p.toString
  }
  
  def toXML(e: NamelessAST): Elem = {
    e match {
      case Stop() => <stop pos={posToString(e.pos)}/>
      case Call(target, args, typeArgs) =>
        <call pos={posToString(e.pos)}>
        <target>{toXML(target)}</target>
        <args>
            {for (a <- args) yield
              <arg>
                {toXML(a)}
              </arg>
            }
        </args>
        {
          typeArgs match {
            case Some(l) => 
              <typeargs>
                {for (a <- l) yield
                  <arg>
                    {toXML(a)}
                  </arg>
                }
              </typeargs>
            case None => <typeargs/>
          }
        }
        </call>
      case Parallel(left, right) => 
        <parallel pos={posToString(e.pos)}>
        <left>{toXML(left)}</left>
        <right>{toXML(right)}</right>
        </parallel>
      case Sequence(left, right) => 
        <sequence pos={posToString(e.pos)}>
        <left>{toXML(left)}</left>
        <right>{toXML(right)}</right>
        </sequence>
      case Prune(left, right) => 
        <prune pos={posToString(e.pos)}>
        <left>{toXML(left)}</left>
        <right>{toXML(right)}</right>
        </prune>
      case Otherwise(left, right) => 
        <otherwise pos={posToString(e.pos)}>
        <left>{toXML(left)}</left>
        <right>{toXML(right)}</right>
        </otherwise> 
      case DeclareDefs(unclosedVars, defs, body: Expression) =>
        <declaredefs pos={posToString(e.pos)}>
          <unclosedvars>
          {for (a <- unclosedVars) yield
            <uv>{a}</uv>
          }
          </unclosedvars>
          <defs>
          {for (a <- defs) yield
            <adef>
              {toXML(a)}
            </adef>
          }
          </defs>
          <body>
            {toXML(body)}
          </body>
        </declaredefs>
      case DeclareType(t: Type, body: Expression) =>
        <declaretype pos={posToString(e.pos)}>
        <atype>{toXML(t)}</atype>
        <body>{toXML(body)}</body>
        </declaretype> 
      case HasType(body: Expression, expectedType: Type) => 
        <hastype pos={posToString(e.pos)}>
          <body>{toXML(body)}</body>
          <expectedtype>{toXML(expectedType)}</expectedtype>
        </hastype>
      case Constant(v: Any) => <constant pos={posToString(e.pos)}>{anyToXML(v)}</constant>
      case Constant(null) => <constant pos={posToString(e.pos)}><nil/></constant>
      case Variable(i: Int) => <variable pos={posToString(e.pos)}>{i}</variable>
      case Hole(context, typecontext) =>
        <hole pos={posToString(e.pos)}>
        <context>
          {for ((n, a) <- context) yield 
            <binding>
              <name>{n}</name>
              <mapsto>{toXML(a)}</mapsto>
            </binding>
          }
        </context>
        <typecontext>
          {for ((n, t) <- context) yield 
            <binding>
              <name>{n}</name>
              <mapsto>{toXML(t)}</mapsto>
            </binding>
          }
        </typecontext>
        </hole>
      case Top() => <top pos={posToString(e.pos)}/>
      case Bot() => <bot pos={posToString(e.pos)}/>
      case TypeVar(i) => <typevar pos={posToString(e.pos)}>{i}</typevar>
      case TupleType(elements) =>
        <tupletype pos={posToString(e.pos)}>
        {for (a <- elements) yield
          <element>{toXML(a)}</element>
        }
        </tupletype>
      case RecordType(entries) =>
        <recordtype pos={posToString(e.pos)}>
        {for ((n, t) <- entries) yield
          <entry>
          <name>{n}</name>
          <rtype>{toXML(t)}</rtype>
          </entry>
        }        
        </recordtype>
      case TypeApplication(tycon: Int, typeactuals) =>
        <typeapplication pos={posToString(e.pos)}>
          <typeconst>{tycon}</typeconst>
          <typeactuals>
            {for (t <- typeactuals) yield
              <typeactual>{toXML(t)}</typeactual>
            }
          </typeactuals>
        </typeapplication>
      case AssertedType(assertedType: Type) =>
        <assertedtype pos={posToString(e.pos)}>{toXML(assertedType)}</assertedtype>
      case FunctionType(typeFormalArity: Int, argTypes, returnType: Type) =>
        <functiontype pos={posToString(e.pos)}>
          <typearity>{typeFormalArity}</typearity>
          <argtypes>
            {for (t <- argTypes) yield
              <arg>{toXML(t)}</arg>
            }
          </argtypes>
          <returntype>{toXML(returnType)}</returntype>
        </functiontype>
      case TypeAbstraction(typeFormalArity: Int, t: Type) =>
        <typeabstraction pos={posToString(e.pos)}>
          <typearity>{typeFormalArity}</typearity>
          <atype>{toXML(t)}</atype>
        </typeabstraction>
      case ImportedType(classname: String) =>
        <importedtype pos={posToString(e.pos)}>{classname}</importedtype>
      case ClassType(classname: String) =>
        <classtype pos={posToString(e.pos)}>{classname}</classtype>
      case VariantType(variants) =>
        <varianttype pos={posToString(e.pos)}>
          {for ((n, l) <- variants) yield
            <variant>
              <name>n</name>
              <params>
                {l map {
                  case Some(t) => <param>{toXML(t)}</param>
                  case None => <param/>
                }}
              </params>
            </variant>
          }
        </varianttype> 
      case Def(typeFormalArity: Int, arity: Int, body: Expression, argTypes, returnType) =>
        <definition pos={posToString(e.pos)}>
          <typearity>{typeFormalArity}</typearity>
          <arity>{arity}</arity>
          <body>{toXML(body)}</body>
          {argTypes match {
            case Some(l) => <argtypes>{ l map { x => <arg>{toXML(x)}</arg>}}</argtypes>
            case None => <argtypes/>
          }}
          {returnType match {
            case Some(t) => <returntype>{toXML(t)}</returntype>
            case None => <returntype/>
          }}
        </definition>
      case _ => throw new Error("Invalid Node for XML conversion!")
    }
  }
  
  def anyToXML(a: Any): Elem = {
    a match { 
      case i:Int => <int>{a}</int> 
      case f:Float => <float>{a}</float> 
      case d:Double => <double>{a}</double> 
      case l:Long => <long>{a}</long> 
      case c:Char => <char>{a}</char> 
      case b:Boolean => <boolean>{b}</boolean> 
      case b:Byte => <byte>{b}</byte> 
      case b:Short => <short>{b}</short> 
      case s:String => <string>{a}</string>
      case i:scala.math.BigInt => <bigint>{a}</bigint>
      case i:scala.math.BigDecimal => <bigdecimal>{a}</bigdecimal>
      case x:orc.values.sites.JavaClassProxy => <jclassproxy>{x.name}</jclassproxy>
      case x:orc.values.sites.Site => 
        <site>{strip$(a.asInstanceOf[AnyRef].getClass().getName)}</site>
      case orc.values.Signal => <signal/>
      case orc.values.Field(s) => <field>{s}</field>
      case x:AnyRef => <any>{a}</any>
    }
  }
  
  def strip$(s: String): String = {
    if (s.charAt(s.length-1) == '$') s.substring(0, s.length-1) else s
  }

  def anyRefFromXML(inxml: scala.xml.Node): AnyRef = {
    inxml match { 
      case <nil/> => null
      case <boolean>{b@ _*}</boolean> => b.text.trim.toBoolean.asInstanceOf[AnyRef]
      case <string>{s@ _*}</string> => new String(s.text)
      case <bigint>{i@ _*}</bigint> => BigInt(i.text.trim)
      case <bigdecimal>{f@ _*}</bigdecimal> => BigDecimal(f.text.trim)
      case <jclassproxy>{x@ _*}</jclassproxy> => 
        orc.values.sites.JavaSiteForm.resolve(x.text.trim)
      case <site>{c@ _*}</site> => {
        orc.values.sites.OrcSiteForm.resolve(c.text.trim)
      }
      case <signal/> => orc.values.Signal
      case <field>{s@ _*}</field> => orc.values.Field(s.text.trim)
      case _ =>  throw new Error("Invalid XML Node!")
    }
  }
  
  class PositionFilenameLineCol(fn:String, l:Int, c:Int) extends PositionWithFilename {
    override val filename = fn
    override def line = l
    override def column = c
    override def lineContents = ""
    override def toString = filename+":"+line+":"+column
  }

  def posStringToOrcPosition(ps: Option[Seq[scala.xml.Node]]) : Position =  {
    ps match {
      case None => NoPosition
      case Some(s) => {
        val posParts = s(0).toString.split(":")
        if (posParts.length == 3) {
          new PositionFilenameLineCol(posParts(0),
              Integer.parseInt(posParts(1)), 
              Integer.parseInt(posParts(2)))
        }
        else NoPosition
      }
    }
  }
  
  def fromXML(inxml: scala.xml.Node): Expression = {
    val pos = posStringToOrcPosition(inxml.attribute("pos"))
    val exp = inxml match {
      case <orc>{prog}</orc> => fromXML(prog)
      case <stop/> => Stop()
      case <parallel><left>{left}</left><right>{right}</right></parallel> => 
        Parallel(fromXML(left), fromXML(right))
      case <sequence><left>{left}</left><right>{right}</right></sequence> => 
        Sequence(fromXML(left), fromXML(right))
      case <prune><left>{left}</left><right>{right}</right></prune> => 
        Prune(fromXML(left), fromXML(right))
      case <otherwise><left>{left}</left><right>{right}</right></otherwise> => 
        Otherwise(fromXML(left), fromXML(right))
      case <declaredefs><unclosedvars>{uvars@ _*}</unclosedvars><defs>{defs@ _*}</defs><body>{body}</body></declaredefs> => {
          val t1 = for (<uv>{i @ _*}</uv> <- uvars) yield i.text.toInt
          val t2 = for (<adef>{d}</adef> <- defs) yield defFromXML(d)
          val t3 = fromXML(body)
          DeclareDefs(t1.toList, t2.toList, t3)
        }
      case <call><target>{target}</target><args>{args@ _*}</args><typeargs>{typeargs@ _*}</typeargs></call> => {
          val t1 = argumentFromXML(target)
          val t2 = for (<arg>{a}</arg> <- args) yield argumentFromXML(a)
          val t3 = for (<arg>{a}</arg> <- typeargs) yield typeFromXML(a)
          Call(t1, t2.toList, if (t3.size==0) None else Some(t3.toList))
        }
      case <declaretype><atype>{atype}</atype><body>{body}</body></declaretype> => {
          DeclareType(typeFromXML(atype), fromXML(body))
        }
      case <hastype><body>{body}</body><expectedtype>{expectedType}</expectedtype></hastype> => {
          HasType(fromXML(body), typeFromXML(expectedType))
        }
      case <hole><context>{ctx@ _*}</context><typecontext>{typectx@ _*}</typecontext></hole> => { 
        val context = HashMap.empty ++ { 
          for (<binding><name>{n}</name><mapsto>{a}</mapsto></binding> <- ctx) yield 
            (n.text.trim, argumentFromXML(a))
        }
        val typecontext = HashMap.empty ++ {
          for (<binding><name>{n}</name><mapsto>{t}</mapsto></binding> <- typectx) yield 
            (n.text.trim, typeFromXML(t))
        }
        Hole(context, typecontext)
      }
      case <constant>{v}</constant> => Constant(anyRefFromXML(v))
      case <variable>{i@ _*}</variable> => Variable(i.text.trim.toInt)
    }  
    exp.pos = pos
    exp
  }
  
  def argumentFromXML(inxml: scala.xml.Node): Argument = {
    val pos = posStringToOrcPosition(inxml.attribute("pos"))
    val exp = inxml match {
      case <constant>{c}</constant> => Constant(anyRefFromXML(c))
      case <variable>{v@ _*}</variable> => Variable(v.text.toInt)
    }
    exp.pos = pos
    exp
  }
  
  def defFromXML(inxml: scala.xml.Node): Def = {
    val pos = posStringToOrcPosition(inxml.attribute("pos"))
    val exp = inxml match {
      case <definition><typearity>{typeFormalArity@ _*}</typearity><arity>{arity@ _*}</arity><body>{body@ _*}</body><argtypes>{argTypes@ _*}</argtypes><returntype>{returnType@ _*}</returntype></definition> => {
        val t1 = if (argTypes.text.trim == "") None 
            else Some((for (<arg>{a}</arg> <- argTypes) yield typeFromXML(a)).toList)
        
        val t2 = if (returnType.text.trim == "") None
          else Some(typeFromXML(returnType.head))
        Def(typeFormalArity.text.toInt, arity.text.toInt, fromXML(body.head), t1, t2)
      }   
    }
    exp.pos = pos
    exp
  }
  
  def typeFromXML(inxml: scala.xml.Node): Type = {
    val pos = posStringToOrcPosition(inxml.attribute("pos"))
    val exp = inxml match {
      case <top/> => Top()
      case <bot/> => Bot()
      case <typevar>{i@ _*}</typevar> => TypeVar(i.text.trim.toInt)
      case <tupletype>{elements@ _*}</tupletype> => {
        val t1 = for (<element>{e}</element> <- elements) yield typeFromXML(e)
        TupleType(t1.toList)
      }
      case <recordtype>{entries@ _*}</recordtype> => {
        var t1: Map[String, Type] = Map.empty
        for(<entry><name>{n@ _*}</name><rtype>{t}</rtype></entry> <- entries) 
          t1 +=  n.text.trim -> typeFromXML(t)
        RecordType(t1)
      }
      case <typeapplication><typeconst>{tycon@ _*}</typeconst><typeactuals>{tactuals@ _*}</typeactuals></typeapplication> => {
            val t1 = for (<typeactual>{t}</typeactual> <- tactuals) yield typeFromXML(t)
            TypeApplication(tycon.text.trim.toInt, t1.toList)
          }
      case <assertedtype>{assertedType}</assertedtype> => 
        AssertedType(typeFromXML(assertedType))
      case <functiontype><typearity>{typeFormalArity@ _*}</typearity><argtypes>{argtypes@ _*}</argtypes><returntype>{returnType}</returntype></functiontype> => {
          val t1 = for (<arg>{t}</arg> <- argtypes) yield typeFromXML(t)
          FunctionType(typeFormalArity.text.trim.toInt, t1.toList,
              typeFromXML(returnType))
        }
      case <typeabstraction><typearity>{typeFormalArity@ _*}</typearity><atype>{t}</atype></typeabstraction> => 
        TypeAbstraction(typeFormalArity.text.trim.toInt, typeFromXML(t))
      case <importedtype>{classname@ _*}</importedtype> =>
        ImportedType(classname.text.trim)
      case <classtype>{classname@ _*}</classtype> => ClassType(classname.text.trim)
      case <varianttype>{variants@ _*}</varianttype> => {
        val t1 = 
           for (<variant><name>{n@ _*}</name><params>{params@ _*}</params></variant> <- variants) yield {
             val t2 = for (<param>{p}</param> <- params) yield {
               if (p.text.trim == "") None
               else Some(typeFromXML(p))
             }
             (n.text.trim, t2.toList)
           }
        VariantType(t1.toList)
      }
    }
    exp.pos = pos
    exp
  }

}
