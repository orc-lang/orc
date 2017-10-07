//
// DistribTestConfig.scala -- Scala object DistribTestConfig
// Project OrcTests
//
// Created by jthywiss on Sep 12, 2017.
//
// Copyright (c) 2017 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.test.proc

import scala.collection.mutable.SortedMap

import orc.util.Config

/** Configuration settings for distributed Orc testing.
  *
  * @author jthywiss
  */
object DistribTestConfig extends Config("DistribTestConfig") {
  unexpanded =>

  object expanded extends scala.collection.immutable.AbstractMap[String, String] with scala.collection.immutable.DefaultMap[String, String]() {

    val addedVariables: scala.collection.mutable.Map[String, String] = scala.collection.mutable.Map[String, String]()

    override def toString: String = iterator.map({ case (k, v) => k + "=" + v }).mkString(s"${getClass.getName}(#filename=${unexpanded.filename}; ", ", ", ")")

    def addVariable(key: String, value: String): Option[String] = addedVariables.put(key, value)

    private val ExpansionIterationLimit = 50

    def expandMacros(str: String): String = {
      var result = str
      var remainIter = ExpansionIterationLimit
      var previous: String = null
      do {
        previous = result
        for ((k, v) <- addedVariables) {
          result = result.replaceAllLiterally("${" + k + "}", v)
        }
        for ((k, v) <- unexpanded) {
          result = result.replaceAllLiterally("${" + k + "}", v)
        }
        remainIter -= 1
      } while (previous != result && remainIter > 0)
      result
    }

    override def get(key: String): Option[String] = {
      addedVariables.get(key) match {
        case Some(v) => Some(expandMacros(v))
        case None => unexpanded.get(key) match {
          case Some(v) => Some(expandMacros(v))
          case None => None
        }
      }
    }

    def getIndexed(key: String): Option[SortedMap[Int, String]] = {
      val elements = SortedMap[Int, String]()
      for ((k, v) <- this) {
        if (k.startsWith(key + "[")) {
          val index = try {
            k.stripPrefix(key + "[").stripSuffix("]").toInt
          } catch {
            case nfe: java.lang.NumberFormatException => throw new IllegalArgumentException(s"Malformed property in $filename: Index must be nonnegative integer: ${k}", nfe)
          }
          if (index < 0) throw throw new IllegalArgumentException(s"Malformed property in $filename: Index must be nonnegative integer: ${k}", new ArrayIndexOutOfBoundsException(index))
          elements += ((index, v))
        }
      }
      if (elements.nonEmpty) {
        Some(elements)
      } else {
        None
      }
    }

    def getIterableFor(key: String): Option[Iterable[String]] = getIndexed(key).map(_.values)

    override def iterator: Iterator[(String, String)] = new scala.collection.AbstractIterator[(String, String)]() {
      val ui = unexpanded.iterator
      def hasNext = ui.hasNext
      def next() = { val (k, v) = ui.next(); (k, expandMacros(v)) }
    }

  }
}
