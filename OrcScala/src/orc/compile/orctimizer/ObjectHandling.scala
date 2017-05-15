package orc.compile.orctimizer

import orc.values.Field
import orc.compile.orctimizer.FlowGraph.{ ExitNode, Node }
import scala.collection.mutable
import orc.ast.orctimizer.named.SpecificAST
import orc.ast.orctimizer.named.New
import orc.ast.orctimizer.named.FieldFuture
import orc.ast.orctimizer.named.FieldArgument
import orc.ast.orctimizer.named.FieldValue
import orc.compile.orctimizer.FlowGraph.ValueNode

trait ObjectHandling {
  self =>
  type This <: ObjectHandling {
    type This = self.This
    type NodeT = self.NodeT
    type StoredValueT = self.StoredValueT
  }
  type NodeT
  type StoredValueT
  type ObjectStructure = Map[Field, StoredValueT]

  val root: NodeT
  val structures: Map[NodeT, ObjectStructure]

  require(structures contains root, s"Root $root is not available in $structures")

  protected def combineStored(a: StoredValueT, b: StoredValueT): StoredValueT
  protected def subsetOfStored(a: StoredValueT, b: StoredValueT): Boolean
  protected def copyObject(root: NodeT, structs: Map[NodeT, ObjectStructure]): This

  //override def toString() = s"ObjectValue($root, ${structures(root)}, ${structures.keySet})"

  def structure: ObjectStructure = structures(root)

  def get(f: Field): Option[StoredValueT] = {
    structure.get(f)
  }

  private def mergeMaps[K, V](m1: Map[K, V], m2: Map[K, V])(f: (V, V) => V): Map[K, V] = {
    m1 ++ m2.map { case (k, v) => k -> m1.get(k).map(f(v, _)).getOrElse(v) }
  }

  def lookupObject(root: NodeT): This = {
    assert(structures contains root, s"Root $root is not available in $this")
    copyObject(root, structures)
  }

  def ++(o: This): This = {
    require(root == o.root, s"Both objects must have the same root. $root != ${o.root}")
    val newStruct = mergeMaps(structures, o.structures) { (s1, s2) =>
      mergeMaps(s1, s2) { combineStored(_, _) }
    }
    copyObject(root, newStruct)
  }

  def subsetOf(o: This): Boolean = o match {
    case o: This if this.getClass().isAssignableFrom(o.getClass()) =>
      this.root == o.root &&
        (o.structure forall {
          case (f, ov) =>
            this.structure.get(f) match {
              case Some(tv) =>
                subsetOfStored(tv, ov)
              case None =>
                false
            }
        })
    case _ => false
  }
}

trait ObjectHandlingCompanion {
  self =>
  type NodeT >: Node
  type StoredValueT
  type ObjectStructure = Map[Field, StoredValueT]

  type Instance <: ObjectHandling {
    type This = Instance
    type NodeT = self.NodeT
    type StoredValueT = self.StoredValueT
  }

  def buildStructures(node: ExitNode)(processField: (FieldValue, Node, (Instance) => Unit) => StoredValueT): Map[NodeT, ObjectStructure] = {
    val ExitNode(spAst @ SpecificAST(New(self, _, bindings, _), _)) = node

    val additionalStructures = mutable.Map[Node, Map[Field, StoredValueT]]()
    def addObject(o: Instance) =
      additionalStructures ++= o.structures.asInstanceOf[Map[Node, Map[Field, StoredValueT]]]

    val struct: Map[Field, StoredValueT] = (for ((field, content) <- bindings) yield {
      val contentRepr = content match {
        case f @ FieldFuture(expr) =>
          val inNode = ExitNode(SpecificAST(expr, f :: spAst.subtreePath))
          processField(f, inNode, addObject)
        case f @ FieldArgument(a) =>
          val inNode = ValueNode(a, f :: spAst.subtreePath)
          processField(f, inNode, addObject)
      }
      (field -> contentRepr)
    }).toMap

    val structs = additionalStructures + (node -> struct)
    // Logger.fine(s"Building SFO for: $nw ;;; $field = $fv ;;; Structs = $structs")
    structs.toMap
  }
}
