//
// Cell.java -- Java class Cell
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

import orc.{MaterializedCallContext, VirtualCallContext}
import orc.lib.state.types.CellType
import orc.run.distrib.{AbstractLocation, ClusterLocations, DOrcPlacementPolicy}
import orc.types.Type
import orc.values.{FastObject, Signal}
import orc.values.sites.{
  FunctionalSite,
  NonBlockingSite,
  PartialSite0Simple,
  Site0Simple,
  Site1Simple,
  TotalSite0Simple,
  TypedSite
}

/** Write-once cell. Read operations block while the cell is empty. Write
  * operatons fail once the cell is full.
  *
  * @author dkitchin
  */
object Cell extends TotalSite0Simple with TypedSite with FunctionalSite {
  override val inlinable = true
  override def eval(): AnyRef = new Cell.Instance()

  override def orcType(): Type = CellType.getBuilder

  private val members = FastObject.members("read", "write", "readD")

  class Instance() extends FastObject(members) with DOrcPlacementPolicy {

    protected var readQueue: Queue[MaterializedCallContext] =
      new LinkedList[MaterializedCallContext]()

    var contents: AnyRef = null

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

    protected class writeMethod
        extends Site1Simple[AnyRef]
        with NonBlockingSite {
      override val inlinable = true

      override def eval(writer: VirtualCallContext, v: AnyRef) = {
        Instance.this.synchronized {
          /*
           * If the read queue is not null, the cell has not yet been
           * set.
           */

          if (readQueue != null) {
            /* Set the contents of the cell */

            contents = v
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

            rq.asScala.foldLeft(writer.publish(Signal))(_.publish(_, v))
            /* A successful write publishes a signal. */
          } else {
            /* A failed write kills the writer. */

            writer.halt()
          }
        }
      }
    }

    override def permittedLocations[L <: AbstractLocation](
        locations: ClusterLocations[L]): scala.collection.immutable.Set[L] =
      locations.hereSet

  }
}
