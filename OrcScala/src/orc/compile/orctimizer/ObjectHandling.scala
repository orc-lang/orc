//
// ObjectHandling.scala -- Scala parametric module trait ObjectHandling
// Project OrcScala
//
// Copyright (c) 2017 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

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
import scala.reflect.ClassTag

/*

  object ObjectHandlingInstance extends ObjectHandling {
    type NodeT = <Node>
    type StoredValueT = <PublicationInfo>
    
    val ObjectValueReified = implicitly[ClassTag[ObjectValue]]
    
    case class ObjectValue(root: NodeT, structures: Map[NodeT, ObjectStructure]) extends ObjectValueBase {
      def derefStoredValue(i: StoredValueT): StoredValueT = {
        <val fields = i.fields map[ObjectInfo] derefObject>
        i.copy(<fields = fields>)
      }
      
      protected def copy(root: NodeT, structs: Map[NodeT, ObjectStructure]): ObjectValue = {
        ObjectValue(root, structs)
      }
    }
    
    object ObjectValue extends ObjectValueCompanion
  }
  
  type ObjectInfo = ObjectHandlingInstance.ObjectInfo
  type ObjectValue = ObjectHandlingInstance.ObjectValue
  val ObjectValue = ObjectHandlingInstance.ObjectValue
  type ObjectRef = ObjectHandlingInstance.ObjectRef
  val ObjectRef = ObjectHandlingInstance.ObjectRef

*/ 

/**
 * A parametric module which implements object handling for analyses.
 * 
 * The client code should instantiate this as documented in the source.
 */
trait ObjectHandling {
  type NodeT >: Node
  type StoredValueT <: LatticeValue[StoredValueT]
  type ResultValueT
  type ObjectRef <: ObjectRefBase
  type ObjectValue <: ObjectValueBase

  val ObjectValueReified: ClassTag[ObjectValue]
  val ObjectRefReified: ClassTag[ObjectRef]
  
  type ObjectStructure = Map[Field, StoredValueT]
  
  abstract trait ObjectInfo {
    val root: NodeT
    def get(f: Field): Option[ResultValueT]
    def ++(o: ObjectInfo): ObjectInfo
    def subsetOf(o: ObjectInfo): Boolean
  }

  protected trait ObjectRefBase extends ObjectInfo {
    val root: NodeT
    
    def get(f: Field): Option[ResultValueT] = throw new AssertionError("Should never be called")
    def ++(o: ObjectInfo): ObjectInfo = throw new AssertionError("Should never be called")
    def subsetOf(o: ObjectInfo): Boolean = root == o.root
  }
  
  protected trait ObjectRefCompanion {
    def apply(root: NodeT): ObjectRef
    //def unapply(ref: ObjectInfo): Option[NodeT]
  }
  
  def ObjectRefCompanion: ObjectRefCompanion

  protected trait ObjectValueBase extends ObjectInfo {
    val root: NodeT
    val structures: Map[NodeT, ObjectStructure]
    
    require(structures contains root, s"Root $root is not available in $structures")

    override def toString() = s"ObjectValue($root, ${structures(root)})"

    def derefStoredValue(i: StoredValueT): ResultValueT
  
    def get(f: Field): Option[ResultValueT] = {
      structures(root).get(f) map derefStoredValue
    }

    protected def copy(root: NodeT, structs: Map[NodeT, ObjectStructure]): ObjectValue

    private def mergeMaps[K, V](m1: Map[K, V], m2: Map[K, V])(f: (V, V) => V): Map[K, V] = {
      m1 ++ m2.map { case (k, v) => k -> m1.get(k).map(f(v, _)).getOrElse(v) }
    }

    def lookupObject(root: NodeT): ObjectValue = {
      copy(root, structures)
    }
    
    protected def derefObject(o: ObjectInfo): ObjectValue = o match {
      case ObjectRefReified(r) =>
        lookupObject(r.root)
      case ObjectValueReified(v) =>
        v
      case _ =>
        throw new NotImplementedError(s"ObjectRef ($ObjectRefReified) and ObjectValue ($ObjectValueReified) must be the only subclasses of ObjectInfo")
    }

    def ++(o: ObjectInfo): ObjectInfo = o match {
      case ObjectValueReified(o) =>
        this ++ o
      case _ =>
        ???
    }
    
    def ++(o: ObjectValue): ObjectValue = {
      require(root == o.root, s"Both objects must have the same root. $root != ${o.root}")
      val newStruct = mergeMaps(structures, o.structures) { (s1, s2) =>
        mergeMaps(s1, s2) { _ combine _ }
      }
      copy(root, newStruct)
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

    def subsetOf(o: ObjectInfo): Boolean = o match {
      case ObjectValueReified(o) =>
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

  protected trait ObjectValueCompanion {
    //def apply(root: NodeT, structs: Map[NodeT, ObjectStructure]): ObjectValue
    
    def buildStructures(node: ExitNode)(processField: (FieldValue, Node, (ObjectInfo) => ObjectRef) => StoredValueT): Map[NodeT, ObjectStructure] = {
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

      def refObject(o: ObjectInfo): ObjectRef = o match {
        case ObjectValueReified(o) =>
          for ((root, struct) <- o.structures) {
            addStructure(root, struct)
          }
          ObjectRefCompanion(o.root)
        case ObjectRefReified(v) => 
          v
        case _ =>
          throw new NotImplementedError(s"ObjectRef ($ObjectRefReified) and ObjectValue ($ObjectValueReified) must be the only subclasses of ObjectInfo")
      }

      val struct: Map[Field, StoredValueT] = (for ((field, content) <- bindings) yield {
        val contentRepr = content match {
          case f @ FieldFuture(expr) =>
            val inNode = ExitNode(SpecificAST(expr, f :: spAst.subtreePath))
            processField(f, inNode, refObject)
          case f @ FieldArgument(a) =>
            val inNode = ValueNode(a, f :: spAst.subtreePath)
            processField(f, inNode, refObject)
        }
        (field -> contentRepr)
      }).toMap

      addStructure(node, struct)

      // Logger.fine(s"Building SFO for: $nw ;;; $field = $fv ;;; Structs = $additionalStructures")
      val structs = additionalStructures.toMap
      structs
    }
  }
}
