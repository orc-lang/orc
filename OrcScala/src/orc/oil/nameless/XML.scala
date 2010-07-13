//
// XML.scala -- Scala class/trait/object XML
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
package orc.oil.nameless


import scala.xml._
import orc.oil.nameless._


object OrcXML {
  
  def writeXML(e : NamelessAST) {
    val xmlout = <orc>
    {toXML(e)}
    </orc>    
    println(xmlout)
  }
  
  def toXML(e : NamelessAST) : Elem = {
    e match {
      case Stop() => <stop/>
      case Call(target, args, typeArgs) =>
        <call>
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
            case None =>
          }
        }
        </call>
      case Parallel(left, right) => 
        <parallel>
        <left>{toXML(left)}</left>
        <right>{toXML(right)}</right>
        </parallel>
      case Sequence(left, right) => 
        <sequence>
        <left>{toXML(left)}</left>
        <right>{toXML(right)}</right>
        </sequence>
      case Prune(left, right) => 
        <prune>
        <left>{toXML(left)}</left>
        <right>{toXML(right)}</right>
        </prune>
      case Otherwise(left, right) => 
        <otherwise>
        <left>{toXML(left)}</left>
        <right>{toXML(right)}</right>
        </otherwise> 
      case DeclareDefs(unclosedVars, defs, body: Expression) =>
        <declaredefs>
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
        <declaretype>
        <atype>{toXML(t)}</atype>
        <body>{toXML(body)}</body>
        </declaretype> 
      case HasType(body: Expression, expectedType: Type) => 
        <hastype>
          <body>{toXML(body)}</body>
          <expectedtype>{toXML(expectedType)}</expectedtype>
        </hastype>
      case Constant(v: AnyRef) => <constant>{v}</constant>
      case Variable(i: Int) => <variable>{i}</variable>
      case Top() => <top/>
      case Bot() => <bot/>
      case TypeVar(i) => <typevar>{i}</typevar>
      case TupleType(elements) =>
        <tupletype>
        {for (a <- elements) yield
          <element>{toXML(a)}</element>
        }
        </tupletype>
      case RecordType(entries) =>
        <recordtype>
        {for ((n, t) <- entries) yield
          <entry>
          <name>{n}</name>
          <rtype>{toXML(t)}</rtype>
          </entry>
        }        
        </recordtype>
      case TypeApplication(tycon: Int, typeactuals) =>
        <typeapplication>
          <typeconst>{tycon}</typeconst>
          <typeactuals>
            {for (t <- typeactuals) yield
              <typeactual>{toXML(t)}</typeactual>
            }
          </typeactuals>
        </typeapplication>
      case AssertedType(assertedType: Type) =>
        <assertedtype>{toXML(assertedType)}</assertedtype>
      case FunctionType(typeFormalArity: Int, argTypes, returnType: Type) =>
        <functiontype>
          <typearity>{typeFormalArity}</typearity>
          <argtypes>
            {for (t <- argTypes) yield
              <arg>{toXML(t)}</arg>
            }
          </argtypes>
          <returntype>{returnType}</returntype>
        </functiontype>
      case TypeAbstraction(typeFormalArity: Int, t: Type) =>
        <typeabstraction>
          <typearity>{typeFormalArity}</typearity>
          <atype>{toXML(t)}</atype>
        </typeabstraction>
      case ImportedType(classname: String) =>
        <importedtype>{classname}</importedtype>
      case ClassType(classname: String) =>
        <classtype>{classname}</classtype>
      case VariantType(variants) =>
        <varianttype>
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
      case Def(typeFormalArity: Int, arity: Int, body: Expression, argTypes: Option[List[Type]], returnType: Option[Type]) =>
        <definition>
          <typearity>{typeFormalArity}</typearity>
          <arity>{arity}</arity>
          <body>{toXML(body)}</body>
          {argTypes match {
            case Some(l) => <argtypes>{ l map { x => <arg>{toXML(x)}</arg>}}</argtypes>
            case None => <argtypes></argtypes>
          }}
          {returnType match {
            case Some(t) => <returntype>{toXML(t)}</returntype>
            case None => <returntype></returntype>
          }}
        </definition>
      case _ => throw new Error("Invalid Node for XML conversion!")
    }
  }

  import orc.compile.StandardOrcCompiler
  import orc.run.StandardOrcRuntime
  import orc.compile.parse.OrcReader
  import orc.run._
  import orc.values.Value
  import orc.values.sites.Site
  import orc.values.Format
  import scala.concurrent.SyncVar
  import orc.OrcOptions
  import orc.ExperimentOptions

  def main(args: Array[String]) {
    if (args.length < 1) {
      throw new Exception("Please supply a source file name as the first argument.\n" +
                          "Within Eclipse, use ${resource_loc}")
    }
    ExperimentOptions.filename = args(0)
    val compiler = new StandardOrcCompiler()
    val reader = OrcReader(new java.io.FileReader(ExperimentOptions.filename), ExperimentOptions.filename, compiler.openInclude(_, _, ExperimentOptions))
    val compiledOil = compiler(reader, ExperimentOptions)
    if (compiledOil != null) {
      OrcXML.writeXML(compiledOil)
    }
    else {
      Console.err.println("Compilation failed.")
    }
  }

}
