//
// BoundedSet.scala -- Scala abstract module BoundedSetModule
// Project OrcScala
//
// Created by amp on Mar 17, 2017.
//
// Copyright (c) 2017 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.compile.flowanalysis

import scala.collection.immutable.Set

abstract class BoundedSetModule {
  type TU
  type TL <: TU
  val sizeLimit: Int

  def apply[T >: TL <: TU](s: Set[T]): BoundedSet[T] = {
    if (s.size > sizeLimit) {
      MaximumBoundedSet()
    } else {
      ConcreteBoundedSet(s)
    }
  }

  def apply[T >: TL <: TU](ss: T*): BoundedSet[T] = {
    apply(ss.toSet)
  }

  sealed abstract class BoundedSet[T >: TL <: TU] {
    def union(o: BoundedSet[T]): BoundedSet[T]
    def ++(o: BoundedSet[T]): BoundedSet[T] = union(o)
    def +(o: T): BoundedSet[T] = union(BoundedSetModule.this.apply(o))

    def collect[U >: TL <: TU](pf: PartialFunction[T, U]): BoundedSet[U]

    def map[U >: TL <: TU](f: T => U): BoundedSet[U]
    def flatMap[U >: TL <: TU](f: T => BoundedSet[U]): BoundedSet[U]

    def values: Option[Set[T]]

    def modify[U >: TL <: TU](f: Set[T] => Set[U]): BoundedSet[U]
    def flatModify[U >: TL <: TU](f: Set[T] => BoundedSet[U]): BoundedSet[U]

    override def hashCode() = values.hashCode()
    override def equals(o: Any) = o match {
      case o: BoundedSet[T] => values == o.values
      case _ => false
    }
  }

  class ConcreteBoundedSet[T >: TL <: TU](s: Set[T]) extends BoundedSet[T] {
    assert(s.size <= sizeLimit)

    def union(o: BoundedSet[T]): BoundedSet[T] = o match {
      case ConcreteBoundedSet(s1) => BoundedSetModule.this(s ++ s1)
      case MaximumBoundedSet() => MaximumBoundedSet()
    }

    def map[U >: TL <: TU](f: T => U): BoundedSet[U] = {
      ConcreteBoundedSet(s map f)
    }

    def flatMap[U >: TL <: TU](f: T => BoundedSet[U]): BoundedSet[U] = {
      s.foldLeft(BoundedSetModule.this.apply[U]())((acc, x) => acc union f(x))
    }

    def collect[U >: TL <: TU](pf: PartialFunction[T, U]): BoundedSet[U] = {
      ConcreteBoundedSet(s.collect(pf))
    }

    def values = Some(s)

    def modify[U >: TL <: TU](f: Set[T] => Set[U]): BoundedSet[U] = BoundedSetModule.this(f(s))
    def flatModify[U >: TL <: TU](f: Set[T] => BoundedSet[U]): BoundedSet[U] = f(s)

    override def toString() = s"ConcreteBoundedSet(${s.mkString(", ")})"
  }

  class ConcreteBoundedSetObject {
    def apply[T >: TL <: TU](s: Set[T]) = new ConcreteBoundedSet(s)
    def unapply[T >: TL <: TU](o: ConcreteBoundedSet[T]): Option[Set[T]] = o.values
  }
  val ConcreteBoundedSet = new ConcreteBoundedSetObject()

  case class MaximumBoundedSet[T >: TL <: TU]() extends BoundedSet[T] {
    def union(o: BoundedSet[T]): BoundedSet[T] = MaximumBoundedSet()
    def collect[U >: TL <: TU](pf: PartialFunction[T, U]): BoundedSet[U] = MaximumBoundedSet()

    def map[U >: TL <: TU](f: T => U): BoundedSet[U] = MaximumBoundedSet()
    def flatMap[U >: TL <: TU](f: T => BoundedSet[U]): BoundedSet[U] = MaximumBoundedSet()
    def modify[U >: TL <: TU](f: Set[T] => Set[U]): BoundedSet[U] = MaximumBoundedSet()
    def flatModify[U >: TL <: TU](f: Set[T] => BoundedSet[U]): BoundedSet[U] = MaximumBoundedSet()

    def values = None
  }
}
