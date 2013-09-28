//
// GenerateMakeX.scala -- Generator for part of the prelude
// Project OrcScala
//
// $Id: Inequal.scala 3222 2013-07-30 17:28:27Z arthur.peters $
//
// Created by amp on Aug 3, 2013.
//
// Copyright (c) 2013 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.lib.generated

import java.io.PrintStream

object GenerateMakeX extends App {
  val MaximumArity = 22
  
  val Array(outputFile) = args
  
  println("Generating code in: " + outputFile)
  
  val out = new PrintStream(outputFile)
  
  out.println("""--
-- makex.inc -- Orc standard prelude include, generated MakeResilient, MakeStrict, and MakeSingleValued definitions
-- Project OrcScala
--
-- This file is overwritten at every rebuild. See OrcScala/src/orc/lib/generated/GenerateMakeX.scala.
--
-- Copyright (c) 2013 The University of Texas at Austin. All rights reserved.
--
-- Use and redistribution of this file is governed by the license terms in
-- the LICENSE file found in the project's top-level directory and also found at
-- URL: http://orc.csres.utexas.edu/license.shtml .
--

""")

  // Generate MakeSingleValued
  for(n <- 0 to MaximumArity) {
    out.println(s"""import site MakeResilient$n = "orc.lib.builtin.MakeResilient$n"""")
  }
  
  out.println()
  
  // Generate MakeSingleValued
  out.print(s"""
def MakeSingleValued0[T](f :: lambda() :: T) = 
  lambda() :: T = y <y< f()
""")
  for(n <- 1 to MaximumArity) {
    def vars(pre: String) = 1 to n map {i => pre + i} mkString ", "
    def typedvars(pre: String, tpre: String) = 1 to n map {i => s"$pre$i :: $tpre$i"} mkString ", "
    out.print(s"""
def MakeSingleValued$n[${vars("A")}, T](f :: lambda(${vars("A")}) :: T) = 
  lambda(${typedvars("x", "A")}) :: T = y <y< f(${vars("x")})
""")
  }
  
  // Generate MakeStrict
  out.print(s"""
def MakeStrict0[T](f :: lambda() :: T) = f
""")
  for(n <- 1 to MaximumArity) {
    def vars(pre: String) = 1 to n map {i => pre + i} mkString ", "
    def typedvars(pre: String, tpre: String) = 1 to n map {i => s"$pre$i :: $tpre$i"} mkString ", "
    out.print(s"""
def MakeStrict$n[${vars("A")}, T](f :: lambda(${vars("A")}) :: T) = 
  lambda(${typedvars("x", "A")}) :: T = (${vars("x")}) >(${vars("y")})> f(${vars("y")})
""")
  }
  
  // Generate MakeSite
  for(n <- 0 to MaximumArity) {
    def vars(pre: String) = 1 to n map {i => pre + i} mkString ", "
    def typedvars(pre: String, tpre: String) = 1 to n map {i => s"$pre$i :: $tpre$i"} mkString ", "
    out.print(s"""
def MakeSite$n[${vars("A")}${if(n > 0) "," else ""} T](f :: lambda(${vars("A")}) :: T) = 
  MakeStrict$n(MakeSingleValued$n(MakeResilient$n(f)))
""")
  }
  
  out.close()
}