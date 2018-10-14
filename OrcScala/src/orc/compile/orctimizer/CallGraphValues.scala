//
// CallGraphValues.scala -- Scala object CallGraphValues
// Project OrcScala
//
// Created by amp on Jul 24, 2017.
//
// Copyright (c) 2018 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.compile.orctimizer

import orc.ast.orctimizer.named.{ Constant, FieldArgument, FieldFuture, FieldValue, New }
import orc.compile.orctimizer.FlowGraph.{ ConstantNode, ExitNode, MethodNode, Node, ValueNode }
import orc.util.{ TFalse, TTrue, TUnknown, Ternary }
import orc.values.Field
import orc.values.sites.{ Site => ExtSite }

object CallGraphValues {

  /** An object structure represented as mapping from fields to the values those fields can take on.
    */
  type ObjectStructure = Map[Field, FieldValueSet]
  object ObjectStructure {
    def apply(): ObjectStructure = {
      Map()
    }
  }

  /** A map of construction sites (New nodes) to object structures describing those objects.
    */
  type ObjectStructureMap = Map[Node, ObjectStructure]
  object ObjectStructureMap {
    def apply(pairs: (Node, ObjectStructure)*): ObjectStructureMap = {
      Map(pairs: _*)
    }
  }

  implicit class ObjectStructureMapAdds(self: ObjectStructureMap) {
    private def mergeMaps[K, V](m1: Map[K, V], m2: Map[K, V])(f: (V, V) => V): Map[K, V] = {
      m1 ++ m2.map { case (k, v) => k -> m1.get(k).map(f(v, _)).getOrElse(v) }
    }

    def merge(other: ObjectStructureMap): ObjectStructureMap = {
      mergeMaps(self, other) { (s1, s2) =>
        mergeMaps(s1, s2) { _ union _ }
      }
    }
  }

  /** A constructor (factory) object to build empty sets of objects.
    */
  trait EmptySetConstructor[T <: AnyValueSet[T, T] with ObjectSet[T]] {
    def apply(): T
  }

  object EmptySetConstructor {
    implicit val implObjectValueSet = ObjectValueSet
    implicit val implObjectRefSet = ObjectRefSet
  }

  /** The base of single values.
    *
    * Strictly speaking this still represents more than one dynamic value, but it is the smallest
    * set the analysis can consider.
    */
  sealed abstract class Value[ObjectSetType <: ObjectSet[ObjectSetType]] {
    /** The static source of this value.
      */
    val valueSource: Node

    /** A singleton set of this value.
      */
    def set: ValueSet[ObjectSetType]
  }

  /** A single object value.
    */
  case class ObjectValue(valueSource: Node, structures: Map[Node, ObjectStructure]) extends Value[ObjectValueSet] {
    def structure = structures(valueSource)

    def set = FlowValueSet(FutureValueSet(), ConcreteValueSet(ObjectValueSet(Set(valueSource), structures), NodeValueSet()))

    def get(f: Field): Option[FlowValueSet] = {
      structure.get(f).map(_.toFlow(structures))
    }
  }

  object ObjectValue {
    /** Build an structures map from a New node and a field handling function.
      *
      * @param processField A function which takes a FieldValue, the node that provides the value for this field, and a function to convert objects to references.
      */
    def buildStructures(node: Node)(processField: (FieldValue, Node) => (FieldValueSet, ObjectStructureMap)): Map[Node, ObjectStructure] = {
      val ExitNode(New.Z(self, _, bindings, _)) = node

      var additionalStructures = ObjectStructureMap()

      val struct: ObjectStructure = (for ((field, content) <- bindings) yield {
        val inNode = content match {
          case FieldFuture.Z(expr) => ExitNode(expr)
          case FieldArgument.Z(a) => ValueNode(a)
        }
        val (contentRepr, os) = processField(content.value, inNode)
        additionalStructures = additionalStructures merge os
        (field -> contentRepr)
      }).toMap

      additionalStructures = additionalStructures merge ObjectStructureMap(node -> struct)

      val structs = additionalStructures.toMap
      structs
    }
  }

  /** A single object value as a reference instead of a complete value.
    */
  case class ObjectRef(valueSource: Node) extends Value[ObjectRefSet] {
    def set = FieldValueSet(FutureValueSet(), ConcreteValueSet(ObjectRefSet(Set(valueSource)), NodeValueSet()))
  }

  /** A future value created in a single future expression.
    *
    * This has a set of potential content values.
    */
  case class FutureValue[ObjectSetType <: ObjectSet[ObjectSetType]: EmptySetConstructor](content: ConcreteValueSet[ObjectSetType], valueSource: Node) extends Value[ObjectSetType] {
    def set = {
      val emptyObjectSet = implicitly[EmptySetConstructor[ObjectSetType]]
      ValueSet[ObjectSetType](FutureValueSet(content, Set(valueSource)), ConcreteValueSet(emptyObjectSet(), NodeValueSet()))
    }
  }

  /** A single node value.
    */
  case class NodeValue[ObjectSetType <: ObjectSet[ObjectSetType]: EmptySetConstructor](valueSource: Node) extends Value[ObjectSetType] {
    def set = ValueSet(FutureValueSet(), ConcreteValueSet(ObjectSet(), NodeValueSet(valueSource)))

    def isMethod = valueSource match {
      case n: MethodNode => true
      case ConstantNode(Constant(_: ExtSite), _) => true
      case _ => false
    }

    def isExternalMethod: Ternary = valueSource match {
      case n: MethodNode => TFalse
      case ConstantNode(Constant(_: ExtSite), _) => TTrue
      case _ => TUnknown
    }

    def isInternalMethod: Ternary = valueSource match {
      case n: MethodNode => TTrue
      case ConstantNode(Constant(_: ExtSite), _) => TFalse
      case _ => TUnknown
    }

    def constantValue: Option[AnyRef] = valueSource match {
      case ConstantNode(Constant(v), _) => Some(v)
      case _ => None
    }
  }

  /** The base of all value sets.
    *
    * This provides basic features such as set operations.
    *
    * The subclasses of this are built into a tree with fixed structure.
    *
    * @param T the type of this set of values.
    */
  sealed abstract class AnyValueSet[T <: AnyValueSet[T, ObjectSetT], ObjectSetT <: ObjectSet[ObjectSetT]] extends Product {
    type Member <: Value[ObjectSetT]

    type FlowVariant <: AnyValueSet[_, ObjectValueSet]
    type FieldVariant <: AnyValueSet[_, ObjectRefSet]

    /** @return true if this is a subset of `o`.
      */
    def subsetOf(o: T): Boolean

    /** @return a set which over-approximates the union of this and `o`.
      *
      * The returned value may be exactly the union or it may contain extra values.
      */
    def union(o: T): T

    def ++(o: T): T = union(o)

    /** @return the set of nodes which provide values in this set.
      */
    def valueSources: Set[Node]

    /** Map `f` over all the values in this set.
      *
      * This returns a FlowValueSet since `f` is allowed to return types not allowed in T.
      */
    def map(f: Value[ObjectSetT] => Value[ObjectSetT]): ValueSet[ObjectSetT]

    /** Map `f` over all the values in this set, flattening the result.
      *
      * This returns a FlowValueSet since `f` is allowed to return types not allowed in T.
      */
    def flatMap(f: Value[ObjectSetT] => ValueSet[ObjectSetT]): ValueSet[ObjectSetT]

    /** Filter this set.
      *
      * Unlike map and flatMap, filter cannot add values and hense returns type T.
      */
    def filter(f: Value[ObjectSetT] => Boolean): T

    def withFilter(f: Value[ObjectSetT] => Boolean): T = filter(f)

    def isEmpty = view.isEmpty
    def nonEmpty = view.nonEmpty

    def exists(p: Value[ObjectSetT] => Boolean) = view.exists(p)
    def forall(p: Value[ObjectSetT] => Boolean) = view.forall(p)
    def existsForall(p: Value[ObjectSetT] => Boolean) = exists(p) && forall(p) 

    /** Convert this set to a real scala set for iterating.
      */
    def toSet: Set[Member]

    /** Get a view on the values in this set.
      */
    def view: Iterable[Member]

    /** Get a value with structure for this object set.
      */
    def toFlow(structures: Map[Node, ObjectStructure]): FlowVariant

    /** Get a reference without structure for this object set.
      */
    def toField: (FieldVariant, Map[Node, ObjectStructure])

    override def toString() = s"$productPrefix(${toSet.mkString(", ")})"
  }

  /** A set of values which can contain futures, objects, and any other value.
    *
    * @param ObjectSetType The type of object sets contained in this value set. This allows different object representations.
    *
    * @param futureContent the set of values which this can contain if it is a future. If `futureContent` is empty then this cannot be a future.
    * @param values the set of values which this can be. If `values` is empty then this cannot be a bare value.
    *
    */
  case class ValueSet[ObjectSetType <: ObjectSet[ObjectSetType]](futures: FutureValueSet[ObjectSetType], values: ConcreteValueSet[ObjectSetType])
    extends AnyValueSet[ValueSet[ObjectSetType], ObjectSetType] {
    type Member = Value[ObjectSetType]
    type FlowVariant = FlowValueSet
    type FieldVariant = FieldValueSet

    def subsetOf(o: ValueSet[ObjectSetType]): Boolean = {
      futures.subsetOf(o.futures) &&
        values.subsetOf(o.values)
    }
    def union(o: ValueSet[ObjectSetType]): ValueSet[ObjectSetType] = {
      ValueSet[ObjectSetType](futures.union(o.futures), values.union(o.values))
    }

    def valueSources: Set[Node] = {
      futures.valueSources ++ values.valueSources
    }

    def toFlow(structures: Map[Node, ObjectStructure]): FlowValueSet = {
      FlowValueSet(futures.toFlow(structures), values.toFlow(structures))
    }
    def toField: (FieldValueSet, ObjectStructureMap) = {
      val (futuresField, futuresOS) = futures.toField
      val (valuesField, valuesOS) = values.toField
      (FieldValueSet(futuresField, valuesField), futuresOS merge valuesOS)
    }

    def toSet = futures.toSet ++ values.toSet
    def view = futures.view ++ values.view

    def map(f: Value[ObjectSetType] => Value[ObjectSetType]): ValueSet[ObjectSetType] = {
      futures.map(f) ++ values.map(f)
    }

    def flatMap(f: Value[ObjectSetType] => ValueSet[ObjectSetType]): ValueSet[ObjectSetType] = {
      futures.flatMap(f) ++ values.flatMap(f)
    }

    def filter(f: Value[ObjectSetType] => Boolean): ValueSet[ObjectSetType] = {
      ValueSet[ObjectSetType](futures.filter(f), values.filter(f))
    }
  }

  object ValueSet {
    def apply[T <: ObjectSet[T]: EmptySetConstructor](): ValueSet[T] = {
      ValueSet(FutureValueSet(), ConcreteValueSet())
    }
  }

  abstract class ValueSetCompanion[ObjectSetType <: ObjectSet[ObjectSetType]] {
    implicit val emptySetConstructor: EmptySetConstructor[ObjectSetType]

    def apply() = ValueSet.apply[ObjectSetType]()
    def apply(futuresContent: FutureValueSet[ObjectSetType], values: ConcreteValueSet[ObjectSetType]) = ValueSet.apply(futuresContent, values)
    def unapply(v: ValueSet[ObjectSetType]) = ValueSet.unapply(v)
  }

  /** A set of possible values a future can hold, or can be produced by a force.
    *
    * This cannot hold futures.
    *
    */
  case class ConcreteValueSet[ObjectSetType <: ObjectSet[ObjectSetType]: EmptySetConstructor](objects: ObjectSetType, nodeValues: NodeValueSet[ObjectSetType])
    extends AnyValueSet[ConcreteValueSet[ObjectSetType], ObjectSetType] {
    type Member = Value[ObjectSetType]
    type FlowVariant = ConcreteValueSet[ObjectValueSet]
    type FieldVariant = ConcreteValueSet[ObjectRefSet]

    def subsetOf(o: ConcreteValueSet[ObjectSetType]): Boolean = {
      objects.subsetOf(o.objects) &&
        nodeValues.subsetOf(o.nodeValues)
    }
    def union(o: ConcreteValueSet[ObjectSetType]): ConcreteValueSet[ObjectSetType] = {
      ConcreteValueSet[ObjectSetType](objects.union(o.objects), nodeValues.union(o.nodeValues))
    }

    def valueSources: Set[Node] = {
      objects.valueSources ++ nodeValues.valueSources
    }

    def toFlow(structures: Map[Node, ObjectStructure]): ConcreteValueSet[ObjectValueSet] = {
      ConcreteValueSet[ObjectValueSet](objects.toFlow(structures), nodeValues.toFlow(structures))

    }
    def toField: (ConcreteValueSet[ObjectRefSet], ObjectStructureMap) = {
      val (objectsField, objectsOS) = objects.toField
      val (nodesField, nodesOS) = nodeValues.toField
      (ConcreteValueSet[ObjectRefSet](objectsField, nodesField), objectsOS merge nodesOS)
    }

    def toSet = objects.toSet.toSet[Value[ObjectSetType]] ++ nodeValues.toSet
    def view = objects.view ++ nodeValues.view

    def map(f: Value[ObjectSetType] => Value[ObjectSetType]): ValueSet[ObjectSetType] = {
      objects.map(f) ++ nodeValues.map(f)
    }

    def flatMap(f: Value[ObjectSetType] => ValueSet[ObjectSetType]): ValueSet[ObjectSetType] = {
      objects.flatMap(f) ++ nodeValues.flatMap(f)
    }

    def filter(f: Value[ObjectSetType] => Boolean): ConcreteValueSet[ObjectSetType] = {
      ConcreteValueSet[ObjectSetType](objects.filter(f), nodeValues.filter(f))
    }
  }

  object ConcreteValueSet {
    def apply[T <: ObjectSet[T]: EmptySetConstructor](): ConcreteValueSet[T] = {
      ConcreteValueSet(ObjectSet(), NodeValueSet())
    }
  }

  /** A set of futures represented as a set of potential content values and a set of future expressions that build the futures.
    */
  case class FutureValueSet[ObjectSetType <: ObjectSet[ObjectSetType]: EmptySetConstructor](content: ConcreteValueSet[ObjectSetType], valueSources: Set[Node])
    extends AnyValueSet[FutureValueSet[ObjectSetType], ObjectSetType] {
    type Member = FutureValue[ObjectSetType]
    type FlowVariant = FutureValueSet[ObjectValueSet]
    type FieldVariant = FutureValueSet[ObjectRefSet]
    type ObjectSetT = ObjectSetType

    require(if (valueSources.isEmpty) content.isEmpty else true)

    def subsetOf(o: FutureValueSet[ObjectSetType]): Boolean = {
      content.subsetOf(o.content) &&
        valueSources.subsetOf(o.valueSources)
    }
    def union(o: FutureValueSet[ObjectSetType]): FutureValueSet[ObjectSetType] = {
      FutureValueSet[ObjectSetType](content.union(o.content), valueSources.union(o.valueSources))
    }

    def toFlow(structures: Map[Node, ObjectStructure]): FutureValueSet[ObjectValueSet] = {
      FutureValueSet[ObjectValueSet](content.toFlow(structures), valueSources)
    }
    def toField: (FutureValueSet[ObjectRefSet], ObjectStructureMap) = {
      val (contentField, contentOS) = content.toField
      (FutureValueSet[ObjectRefSet](contentField, valueSources), contentOS)
    }

    def toSet = valueSources.map(FutureValue(content, _))
    def view = valueSources.view.map(FutureValue(content, _))

    def map(f: Value[ObjectSetType] => Value[ObjectSetType]): ValueSet[ObjectSetType] = {
      valueSources.view.map(src => f(FutureValue(content, src)).set).fold(ValueSet())(_ ++ _)
    }

    def flatMap(f: Value[ObjectSetType] => ValueSet[ObjectSetType]): ValueSet[ObjectSetType] = {
      valueSources.view.map(src => f(FutureValue(content, src))).fold(ValueSet())(_ ++ _)
    }

    def filter(f: Value[ObjectSetType] => Boolean): FutureValueSet[ObjectSetType] = {
      val srcs = valueSources.filter(src => f(FutureValue(content, src)))
      FutureValueSet[ObjectSetType](if (srcs.nonEmpty) content else ConcreteValueSet[ObjectSetType](), srcs)
    }
  }

  object FutureValueSet {
    def apply[ObjectSetType <: ObjectSet[ObjectSetType]: EmptySetConstructor](): FutureValueSet[ObjectSetType] = {
      FutureValueSet(ConcreteValueSet(), Set())
    }
  }

  /** A set of values represented as a set of nodes which originally produce those values.
    *
    * @param valueSources The sources which provide values to this set.
    * 	The nodes must all be one of: ConstantNode, MethodNode, ExitNode for a external Call, or VariableNode which is the argument to a method (if the callers are not known).
    */
  case class NodeValueSet[ObjectSetType <: ObjectSet[ObjectSetType]: EmptySetConstructor](values: Set[NodeValue[ObjectSetType]])
    extends AnyValueSet[NodeValueSet[ObjectSetType], ObjectSetType] {
    type Member = NodeValue[ObjectSetType]
    type FlowVariant = NodeValueSet[ObjectValueSet]
    type FieldVariant = NodeValueSet[ObjectRefSet]
    type ObjectSetT = ObjectSetType

    def valueSources = values.map(_.valueSource)

    def subsetOf(o: NodeValueSet[ObjectSetType]): Boolean = {
      valueSources.subsetOf(o.valueSources)
    }
    def union(o: NodeValueSet[ObjectSetType]): NodeValueSet[ObjectSetType] = {
      NodeValueSet(values.union(o.values))
    }

    def toFlow(structures: Map[Node, ObjectStructure]): NodeValueSet[ObjectValueSet] = {
      NodeValueSet[ObjectValueSet](values.map(n => NodeValue[ObjectValueSet](n.valueSource)))
    }
    def toField: (NodeValueSet[ObjectRefSet], ObjectStructureMap) = {
      (NodeValueSet[ObjectRefSet](values.map(n => NodeValue[ObjectRefSet](n.valueSource))), ObjectStructureMap())
    }

    def toSet = values
    def view = values.view

    def map(f: Value[ObjectSetType] => Value[ObjectSetType]): ValueSet[ObjectSetType] = {
      values.view.map(f(_).set).fold(ValueSet())(_ ++ _)
    }

    def flatMap(f: Value[ObjectSetType] => ValueSet[ObjectSetType]): ValueSet[ObjectSetType] = {
      values.view.map(f).fold(ValueSet())(_ ++ _)
    }

    def filter(f: Value[ObjectSetType] => Boolean): NodeValueSet[ObjectSetType] = {
      NodeValueSet[ObjectSetType](values.filter(f))
    }
  }
  object NodeValueSet {
    def apply[ObjectSetType <: ObjectSet[ObjectSetType]: EmptySetConstructor](): NodeValueSet[ObjectSetType] = {
      new NodeValueSet(Set[NodeValue[ObjectSetType]]())
    }
    def apply[ObjectSetType <: ObjectSet[ObjectSetType]: EmptySetConstructor](nodes: Node*): NodeValueSet[ObjectSetType] = {
      NodeValueSet(nodes.map(NodeValue(_)).toSet)
    }
  }

  /** A set of possible values a variable in an Orctimizer program can take on.
    *
    * This is also used for published values.
    */
  type FlowValueSet = ValueSet[ObjectValueSet]
  object FlowValueSet extends ValueSetCompanion[ObjectValueSet] {
    implicit val emptySetConstructor = new EmptySetConstructor[ObjectValueSet] {
      def apply() = ObjectValueSet()
    }
  }

  /** A set of possible values an object field in an Orctimizer program can take on.
    */
  type FieldValueSet = ValueSet[ObjectRefSet]
  object FieldValueSet extends ValueSetCompanion[ObjectRefSet] {
    implicit val emptySetConstructor = new EmptySetConstructor[ObjectRefSet] {
      def apply() = ObjectRefSet()
    }
  }

  /** Base of object representations
    */
  trait ObjectSet[T <: AnyValueSet[T, T] with ObjectSet[T]] extends AnyValueSet[T, T] {
    type FlowVariant = ObjectValueSet
    type FieldVariant = ObjectRefSet
  }

  object ObjectSet {
    def apply[ObjectSetType <: ObjectSet[ObjectSetType]: EmptySetConstructor](): ObjectSetType = {
      val constr: EmptySetConstructor[ObjectSetType] = implicitly
      constr()
    }
  }

  /** A set of objects and their structures.
    *
    * Conceptually this is a set of ObjectValue instances.
    *
    * @param valueSources The set of New-ExitNodes whose results are part of this set.
    * @param structures A mapping from New-ExitNodes to structures for all nodes in `roots` and any
    * 	objects they reference. Structures is allowed to have unrelated objects in it.
    */
  case class ObjectValueSet(valueSources: Set[Node], structures: Map[Node, ObjectStructure]) extends ObjectSet[ObjectValueSet] {
    type Member = ObjectValue
    type ObjectSetT = ObjectValueSet

    private def structureSubsetOf(a: ObjectStructure, b: ObjectStructure) = {
      b forall {
        case (f, vb) =>
          a.get(f) match {
            case Some(va) =>
              va subsetOf vb
            case None =>
              false
          }
      }
    }

    def subsetOf(o: ObjectValueSet): Boolean = {
      valueSources.subsetOf(o.valueSources) &&
        // TODO: This is concerning since we are letting the subset dictate which objects will be compared. 
        (structures.keySet forall { r =>
          o.structures.contains(r) && structureSubsetOf(structures(r), o.structures(r))
        })
    }

    def union(o: ObjectValueSet): ObjectValueSet = {
      ObjectValueSet(valueSources.union(o.valueSources), structures merge o.structures)
    }

    def toField: (ObjectRefSet, ObjectStructureMap) = {
      (ObjectRefSet(valueSources), structures)
    }
    def toFlow(structures: Map[Node, ObjectStructure]) = {
      ObjectValueSet(valueSources, structures merge this.structures)
    }

    def toSet = valueSources.map(ObjectValue(_, structures)).toSet
    def view = valueSources.view.map(ObjectValue(_, structures))

    def map(f: Value[ObjectValueSet] => Value[ObjectValueSet]): FlowValueSet = {
      valueSources.view.map(r => f(ObjectValue(r, structures)).set).fold(FlowValueSet())(_ ++ _)
    }

    def flatMap(f: Value[ObjectValueSet] => ValueSet[ObjectValueSet]): FlowValueSet = {
      valueSources.view.map(r => f(ObjectValue(r, structures))).fold(FlowValueSet())(_ ++ _)
    }

    def filter(f: Value[ObjectValueSet] => Boolean): ObjectValueSet = {
      ObjectValueSet(valueSources.filter(r => f(ObjectValue(r, structures))), structures)
    }
  }

  object ObjectValueSet extends EmptySetConstructor[ObjectValueSet] {
    def apply(): ObjectValueSet = {
      ObjectValueSet(Set(), Map())
    }
  }

  /** A set of objects without their structures.
    *
    * This is used inside ObjectValueSet to represent references to other objects in the `structures` map.
    *
    * @param valueSources The set of New-ExitNodes whose results are part of this set.
    */
  case class ObjectRefSet(valueSources: Set[Node]) extends ObjectSet[ObjectRefSet] {
    type Member = ObjectRef
    type ObjectSetT = ObjectRefSet

    def subsetOf(o: ObjectRefSet): Boolean = {
      valueSources.subsetOf(o.valueSources)
    }

    def union(o: ObjectRefSet): ObjectRefSet = {
      ObjectRefSet(valueSources.union(o.valueSources))
    }

    def toField: (ObjectRefSet, ObjectStructureMap) = {
      (this, ObjectStructureMap())
    }
    def toFlow(structures: Map[Node, ObjectStructure]) = {
      ObjectValueSet(valueSources, structures)
    }

    def toSet = valueSources.map(ObjectRef(_)).toSet
    def view = valueSources.view.map(ObjectRef(_))

    def map(f: Value[ObjectRefSet] => Value[ObjectRefSet]): FieldValueSet = {
      valueSources.view.map(r => f(ObjectRef(r)).set).fold(FieldValueSet())(_ ++ _)
    }

    def flatMap(f: Value[ObjectRefSet] => ValueSet[ObjectRefSet]): FieldValueSet = {
      valueSources.view.map(r => f(ObjectRef(r))).fold(FieldValueSet())(_ ++ _)
    }

    def filter(f: Value[ObjectRefSet] => Boolean): ObjectRefSet = {
      ObjectRefSet(valueSources.filter(r => f(ObjectRef(r))))
    }
  }

  object ObjectRefSet extends EmptySetConstructor[ObjectRefSet] {
    def apply(): ObjectRefSet = {
      ObjectRefSet(Set())
    }
  }
}
