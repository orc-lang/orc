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
import orc.compile.Logger
import orc.compile.flowanalysis.LatticeValue

trait ObjectHandling {
  self =>
  type This <: ObjectHandling {
    type This = self.This
    type NodeT = self.NodeT
    type StoredValueT = self.StoredValueT
  }
  type NodeT
  type StoredValueT <: LatticeValue[StoredValueT]
  type ObjectStructure = Map[Field, StoredValueT]

  val root: NodeT
  val structures: Map[NodeT, ObjectStructure]

  require(structures contains root, s"Root $root is not available in $structures")

  //protected def combineStored(a: StoredValueT, b: StoredValueT): StoredValueT
  //protected def subsetOfStored(a: StoredValueT, b: StoredValueT): Boolean
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
      mergeMaps(s1, s2) { _ combine _ }
    }
    copyObject(root, newStruct)
  }
  
  private def structureSubsetOf(a: ObjectStructure, b: ObjectStructure) = {
    b forall {
      case (f, vb) =>
        a.get(f) match {
          case Some(va) =>
            val b = va lessThan vb
            if (!b) {
              //Logger.severe(s"Field failed subset check: $f\n${va}\n${vb}") 
            }
            b
          case None =>
            false
        }
    }
  }

  def subsetOf(o: This): Boolean = o match {
    case o: This if this.getClass().isAssignableFrom(o.getClass()) =>
      val sharedStructs = structures.keySet intersect o.structures.keySet
      this.root == o.root &&
        (sharedStructs forall { r =>
          val b = structureSubsetOf(structures(r), o.structures(r))
          if (!b) {
            //Logger.severe(s"Substructure failed subset check: $r\n${structures.get(r)}\n${o.structures.get(r)}") 
          }
          b
        })
    case _ => false
  }
}

trait ObjectHandlingCompanion {
  self =>
  type NodeT >: Node
  type StoredValueT <: LatticeValue[StoredValueT]
  type ObjectStructure = Map[Field, StoredValueT]

  type Instance <: ObjectHandling {
    type This = Instance
    type NodeT = self.NodeT
    type StoredValueT = self.StoredValueT
  }

  def buildStructures(node: ExitNode)(processField: (FieldValue, Node, (Instance) => Unit) => StoredValueT): Map[NodeT, ObjectStructure] = {
    val ExitNode(spAst @ SpecificAST(New(self, _, bindings, _), _)) = node

    val additionalStructures = mutable.Map[NodeT, Map[Field, StoredValueT]]()
    
    def addStructure(root: NodeT, struct: ObjectStructure) = {
      val existing = additionalStructures.get(root)
      val newStruct = existing match {
        case Some(existing) =>
          for ((field, value) <- struct) yield {
            (field, existing(field) combine value)
          }
        case None =>
          struct
      }
      additionalStructures += root -> newStruct
    }
    
    def addObject(o: Instance) = {
      //additionalStructures ++= o.structures.asInstanceOf[Map[NodeT, Map[Field, StoredValueT]]]
      for ((root, struct) <- o.structures) {
        addStructure(root, struct)
      }
    }

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
    
    addStructure(node, struct)
    
    // Logger.fine(s"Building SFO for: $nw ;;; $field = $fv ;;; Structs = $additionalStructures")
    additionalStructures.toMap
  }
}
