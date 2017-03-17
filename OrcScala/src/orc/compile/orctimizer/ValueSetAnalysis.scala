package orc.compile.orctimizer

import orc.ast.orctimizer.named._
import orc.values

sealed abstract class BoundedSet[T] {
  def union(o: BoundedSet[T]): BoundedSet[T]

  def values: Set[T]
}
object BoundedSet {
  val sizeLimit = 5
  def apply[T](s: Set[T]): BoundedSet[T] = {
    if (s.size > sizeLimit) {
      MaximumBoundedSet()
    } else {
      ConcreteBoundedSet(s)
    }
  }
  def apply[T](ss: T*): BoundedSet[T] = {
    if (ss.size > sizeLimit) {
      MaximumBoundedSet()
    } else {
      ConcreteBoundedSet(ss.toSet)
    }
  }
}

case class ConcreteBoundedSet[T](s: Set[T]) extends BoundedSet[T] {
  assert(s.size <= BoundedSet.sizeLimit)
  def union(o: BoundedSet[T]): BoundedSet[T] = o match {
    case ConcreteBoundedSet(s1) => BoundedSet(s union s1)
    case MaximumBoundedSet() => MaximumBoundedSet()
  }

  def values = s
}

case class MaximumBoundedSet[T]() extends BoundedSet[T] {
  def union(o: BoundedSet[T]): BoundedSet[T] = MaximumBoundedSet()

  def values = Set()
}

sealed abstract class ValueSet {
  def union(o: ValueSet): ValueSet
}
object ValueSet {
  val maxima = UnionSet(MaximumBoundedSet())
  val minima = EmptySet
}

case object EmptySet extends ValueSet {
  def union(o: ValueSet) = o
}
case class FutureSet(content: ValueSet) extends ValueSet {
  assert(!content.isInstanceOf[FutureSet])

  def union(o: ValueSet) = o match {
    case FutureSet(c) => FutureSet(c union content)
    case o => UnionSet(this, o)
  }
}
case class DefSet(content: BoundedSet[Def]) extends ValueSet {
  def union(o: ValueSet) = o match {
    case DefSet(c) => DefSet(c union content)
    case o => UnionSet(this, o)
  }
}
case class InternalSiteSet(content: BoundedSet[Site]) extends ValueSet {
  def union(o: ValueSet) = o match {
    case InternalSiteSet(c) => InternalSiteSet(c union content)
    case o => UnionSet(this, o)
  }
}
case class ExternalSiteSet(content: BoundedSet[values.sites.Site]) extends ValueSet {
  def union(o: ValueSet) = o match {
    case ExternalSiteSet(c) => ExternalSiteSet(c union content)
    case o => UnionSet(this, o)
  }
}
case class DataValueSet(content: BoundedSet[AnyRef]) extends ValueSet {
  def union(o: ValueSet) = o match {
    case DataValueSet(c) => DataValueSet(c union content)
    case o => UnionSet(this, o)
  }
}
case class UnionSet(content: BoundedSet[ValueSet]) extends ValueSet {
  assert(content.values.forall(x => !x.isInstanceOf[UnionSet]))

  def union(o: ValueSet) = o match {
    case UnionSet(c) => UnionSet(c union content)
    case o => UnionSet(this, o)
  }
}
object UnionSet {
  def apply(a: ValueSet, b: ValueSet): UnionSet = {
    // TODO: If a and b are the same kind they could be merged.
    val sa = a match {
      case UnionSet(s) => s
      case a => BoundedSet(Set(a))
    }
    val sb = b match {
      case UnionSet(s) => s
      case b => BoundedSet(Set(b))
    }
    UnionSet(sa union sb)
  }
}

class ValueSetAnalysis(root: Expression) extends ForwardOrctimizerAnalyzer(root) {
  type StateT = ValueSet

  def initialState: ValueSet = {
    ValueSet.maxima
  }

  def transfer(node: NodeT, old: StateT, inputStates: collection.Map[NodeT, StateT]): StateT = {
    val computed = ??? /*node match {
      case Stop() => EmptySet
      case CallSite(target, args, _) => {
        ValueSet.maxima
      }
      case CallDef(target, args, _) => {
        ValueSet.maxima
      }
      case Trim(f) =>
        inputStates(f)
      case Force(xs, vs, b, body) => {
        inputStates(body)
      }
      case f Parallel g =>
        inputStates(f) union inputStates(g)
      case f Otherwise g =>
        inputStates(f) union inputStates(g)
      case Branch(f, x, g) =>
        inputStates(g)
      case Future(f) =>
        FutureSet(inputStates(f))
      case New(_, _, _, _) =>
        ValueSet.maxima
      case IfDef(a, f, g) =>
        inputStates(a) match {
          case DefSet(_) => inputStates(f)
          case UnionSet(s) if s.values.forall(x => !x.isInstanceOf[DefSet]) => inputStates(g)
          case UnionSet(_) => inputStates(f) union inputStates(g)
          case _ => inputStates(g)
        }
      case DeclareCallables(defs, body) => {
        inputStates(body)
      }
      case DeclareType(_, _, b) =>
        inputStates(b)
      case HasType(b, _) =>
        inputStates(b)
      case Constant(s: values.sites.Site) => 
        ExternalSiteSet(BoundedSet(s))
      case Constant(v) => 
        DataValueSet(BoundedSet(v))
      case (v: BoundVar) => 
        inputStates(v)
      case FieldAccess(target, f) =>
        ValueSet.maxima
    }*/

    computed
  }
}
