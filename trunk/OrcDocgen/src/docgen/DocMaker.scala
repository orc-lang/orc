//
// DocMaker.scala -- Scala class/trait/object DocMaker
// Project OrcDocgen
//
// $Id$
//
// Created by dkitchin on Dec 16, 2010.
//
// Copyright (c) 2010 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//
package docgen

import java.io.File
import scala.xml._

/**
 * 
 *
 * @author dkitchin
 */
class DocMaker {
  
	val claimedIdentifiers = new scala.collection.mutable.HashSet[String]()
	
  def nonblank(line: String): Boolean = !line.trim().isEmpty
  
  /* Split a chunk of text into paragraphs */
  def paragraphs(s: String): List[String] = {
    def groupLines(lines: List[String]): List[String] = {
      lines match {
        case first :: _ if nonblank(first) => {
          val (nonblankLines, rest) = lines span nonblank
          val para = (nonblankLines foldLeft "") { (s: String, t: String) => s + t }
          para :: groupLines(rest)
        }
        case _ :: rest => groupLines(rest)
        case _ => Nil
      }
    }
    groupLines(s.linesWithSeparators.toList)
  }
  
	/* Optionally add an xml:id tag to an xml element */
	def addId(xml: Elem, optionalID: Option[String]): Elem = {
		optionalID match {
			case None => xml
			case Some(id) => {
			  val idAttribute = new PrefixedAttribute("xml", "id", id, scala.xml.Null)
        xml.copy(attributes = xml.attributes.append(idAttribute))
			}
		}
	}
	
  
  def makeDoc(files: List[File]): Node = {
    val leadingComment = {
      Comment( 
      "Generated by OrcDocgen from: " +
      ({ (files map { f:File => f.getName() }).mkString("{",",","}") } : String) + 
      " on " + 
      { java.text.DateFormat.getDateTimeInstance().format(new java.util.Date()) }
      )
    }
    <appendix id="ref.stdlib">
      { leadingComment }
      <title>Reference</title>

        <xi:include href="ref.stdlib.intro.xml"/>

        { files map renderSection }
    </appendix>
  } 
    
  def renderSection(f: File) = {
    val docItemList = DocParsers.parseFile(f)
    val optionalHeader = {
      docItemList match {
        case DocText(s) :: _ if nonblank(s) => ": " + s.lines.next()
        case _ => "" 
      }
    }
    val optionalSectionName = """\w+""".r.findPrefixOf(f.getName())
    val content = {
    	<section> 
        <title>{ f.getName() + optionalHeader }</title>
        { renderItems(docItemList)("", optionalSectionName) }
      </section>
    }
    addId(content, optionalSectionName map { "ref.stdlib." + _ })
  }
  
  def renderItems(items: List[DocItem])(implicit nextCode: String, optionalSectionName: Option[String]): List[Node] = {
    items match {
      case DocText(s) :: rest => {
        val paraNodes = paragraphs(s) map { p: String => <para>{ Unparsed(p) }</para> }
        paraNodes ::: renderItems(rest)
      }
      case (_ : DocDecl) :: _ => {
        val (decls, rest) = items span { _.isInstanceOf[DocDecl] }
        val newNextCode = {
          items find { _.isInstanceOf[DocOutside] } match {
            case Some(DocOutside(body)) => body
            case None => ""
          }
        }
        val declChunk = {
          <variablelist>
            <?dbfo list-presentation="list"?>
            <?dbhtml list-presentation="table"?>
            { decls map { d: DocItem => renderDecl(d.asInstanceOf[DocDecl])(newNextCode, optionalSectionName) } }
          </variablelist>  
        }
        declChunk :: renderItems(rest)
      }
      case DocImpl :: rest => {
        renderImplementation(nextCode) :: renderItems(rest)
      }
      // If we encounter uncommented content, ignore it.
      case (_ : DocOutside) :: rest => renderItems(rest)
      
      // No more input
      case Nil => Nil
    }
    
  }
  
    
  def renderDecl(decl: DocDecl)(implicit nextCode: String, optionalSectionName: Option[String]) = {
  	
  	val optionalId = {
  		"""^\w+$""".r.findPrefixOf(decl.name) match {
  			case Some(ident) => {
  				val path = "library" :: optionalSectionName.toList ::: List(ident)
  			  val longId = path.mkString(".")
  			  if (claimedIdentifiers contains longId) {
  			  	None
  			  }
  			  else {
  			  	claimedIdentifiers += longId
  			  	Some(longId)
  			  }
  			}
  			case None => None
  		}
  	}
  	
  	val content = {
  		<varlistentry>
  		  <term><code>{ decl.name }</code></term>
        <listitem>
  		 	  <para><code>{ decl.keyword + decl.typeDeclaration }</code></para>
  			  { renderItems(decl.body) }
  			</listitem>
  		</varlistentry>
  	}
  	
  	addId(content, optionalId)
  }  
  
  
  def renderImplementation(code: String) = {
    <formalpara>
      <title>Implementation</title>
      <programlisting language="orc-demo">
        { PCData(code) }
      </programlisting>
    </formalpara>
  }
   
}