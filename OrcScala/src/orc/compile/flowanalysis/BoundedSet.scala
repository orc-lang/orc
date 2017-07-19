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

  sealed abstract class BoundedSet[T >: TL <: TU] extends LatticeValue[BoundedSet[T]] {
    def union(o: BoundedSet[T]): BoundedSet[T]
    def ++(o: BoundedSet[T]): BoundedSet[T] = 
      union(o)
    def +(o: T): BoundedSet[T] = 
      union(BoundedSetModule.this.apply(o))

    def collect[U >: TL <: TU](pf: PartialFunction[T, U]): BoundedSet[U]

    def map[U >: TL <: TU](f: T => U): BoundedSet[U]
    def flatMap[U >: TL <: TU](f: T => BoundedSet[U]): BoundedSet[U]

    def values: Option[Set[T]]

    def modify[U >: TL <: TU](f: Set[T] => Set[U]): BoundedSet[U]
    def flatModify[U >: TL <: TU](f: Set[T] => BoundedSet[U]): BoundedSet[U]
    
    def subsetOf(o: BoundedSet[T]): Boolean
    def supersetOf(o: BoundedSet[T]): Boolean = o.subsetOf(this)
    
    def exists(f: (T) => Boolean): Boolean
    def forall(f: (T) => Boolean): Boolean

    override def hashCode() = values.hashCode()
    override def equals(o: Any) = o match {
      case o: BoundedSet[T] => values == o.values
      case _ => false
    }

    def lessThan(o: BoundedSet[T]): Boolean = subsetOf(o)
    def combine(o: BoundedSet[T]): BoundedSet[T] = union(o)
    
    def isEmpty: Boolean
    def nonEmpty: Boolean = !isEmpty
  }

  class ConcreteBoundedSet[T >: TL <: TU](s: Set[T]) extends BoundedSet[T] {
    require(s.size <= sizeLimit)

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

    def subsetOf(o: BoundedSet[T]): Boolean = o match {
      case ConcreteBoundedSet(s1) => s subsetOf s1
      case _ => true
    }

    def exists(f: (T) => Boolean): Boolean = s.exists(f)
    def forall(f: (T) => Boolean): Boolean = s.forall(f)
    
    def isEmpty: Boolean = s.isEmpty
  }

  class ConcreteBoundedSetCompanion {
    def apply[T >: TL <: TU](s: Set[T]) = new ConcreteBoundedSet(s)
    def unapply[T >: TL <: TU](o: ConcreteBoundedSet[T]): Option[Set[T]] = o.values
  }
  val ConcreteBoundedSet: ConcreteBoundedSetCompanion
  
  case class MaximumBoundedSet[T >: TL <: TU]() extends BoundedSet[T] {
    def union(o: BoundedSet[T]): BoundedSet[T] = MaximumBoundedSet()
    def collect[U >: TL <: TU](pf: PartialFunction[T, U]): BoundedSet[U] = MaximumBoundedSet()

    def map[U >: TL <: TU](f: T => U): BoundedSet[U] = MaximumBoundedSet()
    def flatMap[U >: TL <: TU](f: T => BoundedSet[U]): BoundedSet[U] = MaximumBoundedSet()
    def modify[U >: TL <: TU](f: Set[T] => Set[U]): BoundedSet[U] = MaximumBoundedSet()
    def flatModify[U >: TL <: TU](f: Set[T] => BoundedSet[U]): BoundedSet[U] = MaximumBoundedSet()

    def values = None

    def subsetOf(o: BoundedSet[T]): Boolean = o match {
      case MaximumBoundedSet() => true
      case _ => false
    }
    
    // TODO: This is not actually strictly correct. f could always be false.
    def exists(f: (T) => Boolean): Boolean = true
    def forall(f: (T) => Boolean): Boolean = false
    
    // TODO: This is not actually strictly correct. The domain could be empty.
    def isEmpty: Boolean = false
  }
}
