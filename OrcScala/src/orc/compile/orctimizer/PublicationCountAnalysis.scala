package orc.compile.orctimizer

import orc.ast.orctimizer.named._

/** Represent a range of numbers lower bounded by a natural and upper bounded
  * by a natural or infinity.
  *
  * @author amp
  */
class FiniteRange(val mini: Int, val maxi: Option[Int]) {
  assert(mini >= 0)
  assert(maxi map { _ >= mini } getOrElse true)

  /** True iff this contains only values greater than or equal to n.
    */
  def >=(n: Int) = mini >= n
  /** True iff this contains only values greater than n.
    */
  def >(n: Int) = mini > n
  /** True iff this contains only values less than or equal to n.
    */
  def <=(n: Int) = maxi map { _ <= n } getOrElse false
  /** True iff this contains only values less than n.
    */
  def <(n: Int) = maxi map { _ < n } getOrElse false

  /** True iff this contains only n.
    */
  def only(n: Int) = mini == n && maxi == Some(n)

  /** Return the intersection of two ranges or None is the intersection is empty.
    */
  def intersectOption(r: FiniteRange): Option[FiniteRange] = {
    val n = mini max r.mini
    val m = (maxi, r.maxi) match {
      case (Some(x), Some(y)) => Some(x min y)
      case (Some(_), None) => maxi
      case (None, Some(_)) => r.maxi
      case (None, None) => None
    }
    if (m map { _ >= n } getOrElse true)
      Some(FiniteRange(n, m))
    else
      None
  }

  def intersect(r: FiniteRange): FiniteRange = {
    intersectOption(r).getOrElse {
      throw new IllegalArgumentException("Ranges do not overlap and range cannot "
        + "represent the empty range. You may have mixed in an incompatible set of"
        + " site metadata traits.")
    }
  }

  /** Return the union of two ranges.
    */
  def union(r: FiniteRange): FiniteRange = {
    val n = mini min r.mini
    // If either is None m is also None
    val m = for (ma <- maxi; mb <- r.maxi) yield ma max mb
    FiniteRange(n, m)
  }

  /** Return a range containing all results of summing values from this and r.
    */
  def +(r: FiniteRange) = {
    FiniteRange(mini + r.mini, (maxi, r.maxi) match {
      case (Some(n), Some(m)) => Some(n + m)
      case _ => None
    })
  }
  /** Return a range containing all results of multiplying values from this and r.
    */
  def *(r: FiniteRange) = {
    FiniteRange(mini * r.mini, (maxi, r.maxi) match {
      case (Some(0), _) | (_, Some(0)) => Some(0)
      case (Some(n), Some(m)) => Some(n * m)
      case _ => None
    })
  }

  /** Return a range similar to this but that upper bounded by lim. Unlike intersection,
    * if lim is less than the lower bound of this return FiniteRange(lim, lim).
    */
  def limitTo(lim: Int) = {
    val n = mini min lim
    val m = maxi map (_ min lim) getOrElse lim
    FiniteRange(n, m)
  }

  /** Return a range which includes 0 but has the same upper bound as this.
    *
    */
  def mayHalt = {
    FiniteRange(0, maxi)
  }

  override def hashCode() = mini ^ maxi.getOrElse(Int.MaxValue)
  override def toString() = s"FiniteRange($mini, $maxi)"
  override def equals(o: Any): Boolean = o match {
    case o: FiniteRange => maxi == o.maxi && mini == o.mini
    case _ => false
  }
}

object FiniteRange {
  val maximum = 1

  def apply(n: Int, m: Option[Int]): FiniteRange = {
    assert(n >= 0)
    // Map any number of publications greater than 1 to Inf
    val m1 = m match {
      case Some(mm) if mm <= maximum =>
        assert(mm >= n)
        Some(mm)
      case _ => None
    }
    val n1 = if(n <= maximum) n else maximum
    new FiniteRange(n1, m1)
  }

  def apply(n: Int, m: Int): FiniteRange = {
    FiniteRange(n, Some(m))
  }

  def apply(r: (Int, Option[Int])): FiniteRange = {
    FiniteRange(r._1, r._2)
  }
}

class PublicationCountAnalysis(root: Expression) extends ForwardOrctimizerAnalyzer(root) {
  type StateT = FiniteRange

  def initialState: FiniteRange = {
    FiniteRange(0, 0)
  }

  def transfer(node: NodeT, old: StateT, inputStates: collection.Map[NodeT, StateT]): StateT = {
    val computed = ??? /*node match {
      case Stop() => FiniteRange(0, 0)
      case CallSite(target, args, _) => {
        FiniteRange(0, None)
      }
      case CallDef(target, args, _) => {
        FiniteRange(0, None)
      }
      case Trim(f) =>
        inputStates(f).limitTo(1)
      case Force(xs, vs, b, body) => {
        inputStates(body)
      }
      case f Parallel g =>
        inputStates(f) + inputStates(g)
      case f Otherwise g =>
        inputStates(f).intersectOption(FiniteRange(1, None)).getOrElse(inputStates(g)) union inputStates(g)
      case Branch(f, x, g) =>
        inputStates(f) * inputStates(g)
      case Future(f) =>
        FiniteRange(1, 1)
      case New(_, _, _, _) =>
        FiniteRange(1, 1)
      case IfDef(a, f, g) =>
        // TODO: Define an analysis to give Def/Unknown/Site
        inputStates(f) union inputStates(g)
      case DeclareCallables(defs, body) => {
        inputStates(body)
      }
      case DeclareType(_, _, b) =>
        inputStates(b)
      case HasType(b, _) =>
        inputStates(b)
      case Constant(_) => FiniteRange(1, 1)
      case (v: BoundVar) => FiniteRange(1, 1)
      case FieldAccess(target, f) =>
          FiniteRange(0, 1)
    }*/

    computed
  }
}
