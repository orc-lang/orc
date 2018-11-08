//
// Ref.java -- Java class Ref
// Project OrcScala
//
// Copyright (c) 2017 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.lib.state

import java.util.{LinkedList, Queue}

import scala.collection.JavaConverters.collectionAsScalaIterableConverter

import orc.{
  DirectInvoker,
  MaterializedCallContext,
  OrcRuntime,
  VirtualCallContext
}
import orc.error.runtime.ArityMismatchException
import orc.lib.state.types.RefType
import orc.run.distrib.{AbstractLocation, ClusterLocations, DOrcPlacementPolicy}
import orc.types.Type
import orc.values.{FastObject, Signal}
import orc.values.sites.{
  FunctionalSite,
  NonBlockingSite,
  PartialSite0Simple,
  Site0Simple,
  Site1Simple,
  SiteMetadata,
  TargetClassAndArgumentClassSpecializedInvoker,
  TotalSiteBase,
  TypedSite
}

/** Rewritable mutable reference. The reference can be initialized with a value,
  * or left initially empty. Read operations block if the reference is empty.
  * Write operations always succeed.
  *
  * @author dkitchin
  */
object Ref
    extends TotalSiteBase
    with TypedSite
    with SiteMetadata
    with FunctionalSite {
  override val inlinable = true
  def getInvoker(runtime: OrcRuntime, args: Array[AnyRef]): DirectInvoker = {
    args.size match {
      case 1 => invoker(this, args: _*)((_, args) => new Ref.Instance(args(0)))
      case 0 => invoker(this, args: _*)((_, args) => new Ref.Instance())
      case _ =>
        new TargetClassAndArgumentClassSpecializedInvoker(this, args)
        with DirectInvoker {
          @throws[Throwable]
          def invokeDirect(target: AnyRef, arguments: Array[AnyRef]): AnyRef = {
            throw new ArityMismatchException(1, args.size)
          }
        }
    }
  }

  override def orcType(): Type = RefType.getBuilder

  private val members = FastObject.members("read", "write", "readD")

  class Instance() extends FastObject(members) with DOrcPlacementPolicy {

    protected var readQueue: Queue[MaterializedCallContext] =
      new LinkedList[MaterializedCallContext]()

    var contents: AnyRef = null

    /*
     * Create the reference with an initial value already assigned. In this
     * case, we don't need a reader queue.
     */
    def this(initial: AnyRef) = {
      this()
      this.contents = initial
      this.readQueue = null
    }

    protected val values = Array(
      new readMethod(),
      new writeMethod(),
      new PartialSite0Simple with NonBlockingSite {
        override val inlinable = true
        // readD
        override def eval() = {
          Instance.this.synchronized {
            if (readQueue != null) {
              None
            } else {
              Some(contents)
            }
          }
        }
      }
    )

    protected class readMethod extends Site0Simple {
      override val inlinable = true

      override def eval(reader: VirtualCallContext) = {
        Instance.this.synchronized {
          /*
           * If the read queue is not null, the cell has not been set.
           * Add this caller to the read queue.
           */

          if (readQueue != null) {
            val mat = reader.materialize()
            mat.setQuiescent()
            readQueue.add(mat)
            reader.empty
          } else
            /* Otherwise, return the contents of the cell */ {
              reader.publish(contents)
            }
        }
      }

    }

    protected class writeMethod extends Site1Simple[AnyRef]() {
      override val inlinable = true

      override def eval(writer: VirtualCallContext, v: AnyRef) = {
        Instance.this.synchronized {
          /* Set the contents of the cell */

          contents = v
          /* A write publishes a signal. */
          val r1 = writer.publish(Signal)
          if (readQueue != null) {
            val rq: Queue[MaterializedCallContext] = readQueue
            /*
             * Null out the read queue. This indicates that the cell
             * has been written. It also allowed the associated
             * memory to be reclaimed.
             */

            readQueue = null
            /*
             * Wake up all queued readers and report the written
             * value to them.
             */

            rq.asScala.foldLeft(r1)(_.publish(_, v))
          } else {
            r1
          }
        }
      }
    }

    override def toString(): String = "Ref(" + contents + ")"

    override def permittedLocations[L <: AbstractLocation](
        locations: ClusterLocations[L]): scala.collection.immutable.Set[L] =
      locations.hereSet
  }
}
