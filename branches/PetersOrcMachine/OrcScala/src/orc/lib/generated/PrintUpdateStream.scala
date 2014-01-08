//
// PrintUpdateStream.scala -- Scala class/trait/object PrintUpdateStream
// Project OrcScala
//
// $Id$
//
// Created by amp on Jan 7, 2014.
//
// Copyright (c) 2014 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//
package orc.lib.generated

import java.io.PrintStream
import java.io.File
import java.io.ByteArrayOutputStream
import java.io.ByteArrayInputStream
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import scala.io.Source

/**
  *
  * @author amp
  */
class PrintUpdateStream(f: File) extends 
  { val strStream = new ByteArrayOutputStream() } with
  PrintStream(strStream, true, "UTF-8") {
  
  override def close() {
    super.close()
    if( !streamsEqual(new ByteArrayInputStream(strStream.toByteArray()), 
        new FileInputStream(f)) ) {
      val out = new FileOutputStream(f)
      out.write(strStream.toByteArray())
      out.close()
    }
  }
  
  def streamsEqual(a: InputStream, b: InputStream): Boolean = {
    while(true) {
      val ac = a.read()
      val bc = b.read()
      if(ac != bc)
        return false
      else if(ac == -1)
        return true
    }
    return true
  }
}