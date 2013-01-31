//
// OptionMapExtension.scala -- Scala object OptionMapExtension
// Project OrcScala
//
// $Id$
//
// Created by dkitchin on May 19, 2010.
//
// Copyright (c) 2011 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.util

object OptionMapExtension {

  // Adds an optionMap method to lists
  class ListWithOptionMap[A](xs: List[A]) {
    def optionMap[B](f: A => Option[B]): Option[List[B]] = {
      def helperFunction(xs: List[A], ys: List[B]): Option[List[B]] = {
        xs match {
          case Nil => Some(ys.reverse)
          case x :: xs => f(x) match {
            case Some(y) => helperFunction(xs, y :: ys)
            case None => None
          }
        }
      }
      helperFunction(xs, Nil)
    }
  }
  implicit def addOptionMapToList[A](xs: List[A]) = new ListWithOptionMap(xs)

}
