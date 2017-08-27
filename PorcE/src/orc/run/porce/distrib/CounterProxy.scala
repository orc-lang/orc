//
// CounterProxy.scala -- Scala trait CounterProxyManager, and classes RemoteCounterProxy and RemoteCounterMembersProxy
// Project PorcE
//
// Created by jthywiss on Aug 15, 2017.
//
// Copyright (c) 2017 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.run.porce.distrib

import orc.Schedulable
import orc.run.porce.runtime.Counter
import orc.util.SparseBinaryFraction

/** A DOrcExecution mix-in to create and communicate among distributed counters.
  *
  * Distributed counters implement the algorithm developed by Friedemann MATTERN in 
  * "Global quiescence detection based on credit distribution and recovery" 
  * (1989) [https://doi.org/10.1016/0020-0190(89)90212-3].
  * 
  * @see CounterProxyManager#RemoteCounterProxy
  * @see CounterProxyManager#RemoteCounterMembersProxy
  * @author jthywiss
  */
trait CounterProxyManager {
  execution: DOrcExecution =>  
    
  type CounterId = CounterProxyManager.CounterId
  type DistributedCounterId = CounterProxyManager.DistributedCounterId
  val DistributedCounterId = CounterProxyManager.DistributedCounterId
  
  /** The type of all distributed counter representations.
    *
    * This includes both remote fragments and the controllers.
    * 
    */
  sealed trait DistributedCounter {
    /** The Id of this distributed counter.
      */
    val id: CounterProxyManager#DistributedCounterId
    
    /** Make a token remote by converting it into credit.
      *
      * The caller must already have a token on `counter`. This call consumes it. 
      *
      * Return the credit for the remove node to use.
      */
    def convertToken(): Int
    
    /** Return the local counter associated with this.
      */
    def counter: Counter
    
    /** Activate this representation with some credits.
     *  
     *  Token: Provides a local token to the caller.
     */
    def activate(credits: Int): Unit
  }

  /** A counter with attached distribution information.
    *
    * @author jthywiss, amp
    */
  sealed abstract class DistributedCounterFragment private[CounterProxyManager] (val id: CounterProxyManager#DistributedCounterId)
    extends Counter(0) with DistributedCounter {
    // TODO: PERFORMANCE: Reduce or eliminate the use of synchonized here. The state would need to be compacted and there would be complications with the local count, but it is possible to do I think.
    
    /** The number of credit is 1/(2**credits).
      *
      * -1 means we have 0 credits.
	    */
    var credits: Int = -1
    
    /** True if this is waiting for credit to be provided by the origin.
      */
    var waitingForCredit = false
    // TODO: waitingForCredit is probably not needed. But is useful to leave for initial debugging.
    
    override def toString: String = f"DistributedCounterFragment(id=$id, credits=$credits, waitingForCredit=$waitingForCredit)"    
    
    override def onHalt(): Unit = synchronized {
      Logger.entering(getClass.getName, "onHalt")
      /*
       * Concurrency: We can reach here with credits == -1 and waitingForCredit = false.
       * This is caused because between the decrement that caused this halt and the execution 
       * of onHalt, an entire activate/halt cycle could happen. This would return the credits
       * we would expect to return here. This is safe.
       */
      if(!waitingForCredit && credits >= 0) {
        // We have credits and we are not waiting on more.
        // Credits: We halted locally so return all our credits.
        returnCredits(credits)
        credits = -1
      } else {
        // We are waiting for credit, so we cannot return our credits yet.
        // Or we have no credits and then we still cannot return our credits.
      }
    }

    override def onResurrect(): Unit = synchronized {
      Logger.entering(getClass.getName, "onResurrect")
      assert(!waitingForCredit)
      val n = incrementAndGet()
      // Token: Create a token that is held by the request credits message. It will be returned with the credits.
      logChange(s"'waiting for credit' to $n")
      requestCredits()
      waitingForCredit = true
    }

    /** Activate this representation the provided credits.
      *  
      * Token: Returns a token on this counter to the caller.
      */
    final def activate(credits: Int): Unit = synchronized {
      Logger.entering(getClass.getName, s"activate $credits")
      val n = incrementAndGet()
      logChange(s"activate to $n")
      if(this.credits < 0) {
        this.credits = credits
      } else if(this.credits == credits) {
        this.credits -= 1
      } else {
        returnCredits(credits)
      }
    }
    
    final def provideCredit(credits: Int): Unit = synchronized {
      Logger.entering(getClass.getName, s"provideCredits $credits")
      assert(waitingForCredit)
      // Credits: If we have credits now (because we were activated in the intrem), just return the ones provided. If we still need them, take the credits.
      if(this.credits < 0) {
        this.credits = credits
      } else {
        returnCredits(credits)
      }
      waitingForCredit = false
      // Token: Halt the special 'waiting for credit' token created in onResurrect()
      haltToken()
    }
    
    /** Make a token remote by converting it into credit.
      *
      * The caller must already have a token on this. This call consumes it. 
      *
      * Return the credit for the remove node to use.
      */
    def convertToken(): Int = synchronized {
      // TODO: PERFORMANCE: This could be specialized for the final decrement case to pass the whole value to avoid the need for a message back to the root.
      credits += 1
      val r = credits
      haltToken()
      r
    }
    
    /** Return the local counter associated with this.
      */
    def counter: Counter = this
    
    private[CounterProxyManager] def returnCredits(credits: Int): Unit = {
      if (isDiscorporated) {
        returnCreditsDiscorporate(credits)
      } else {
        returnCreditsHalt(credits)
      }
    }
    private[CounterProxyManager] def returnCreditsHalt(credits: Int): Unit
    private[CounterProxyManager] def returnCreditsDiscorporate(credits: Int): Unit
    private[CounterProxyManager] def requestCredits(): Unit
  }

  /** TODO
    *
    * @author jthywiss, amp
    */
  final class DistributedCounterController private[CounterProxyManager] (val id: CounterProxyManager#DistributedCounterId, val enclosingCounter: Counter) 
      extends DistributedCounter {
    var credits = SparseBinaryFraction.one
    var remoteTokens = 0
    // TODO: MEMORYLEAK: The lack of any local information on if this members proxy is done will make removing proxies from the maps very hard.

    override def toString: String = f"${getClass.getName}(id=$id, enclosingCounter=$enclosingCounter)"
    
    private def addCreditAndCheck(credits: Int)(doHalt: => Unit): Unit = {
      val n = this.credits.addBit(credits)
      Logger.fine(s"$id: Adding 1/(2^$credits) to ${this.credits} = $n")
      this.credits = n
      if(this.credits == SparseBinaryFraction.one) {
        Logger.fine(s"$id: Remote halt: counter at ${enclosingCounter.get}, have $remoteTokens")
        (0 until remoteTokens).foreach { (_) =>
          doHalt
        }
        remoteTokens = 0
      }
    }
    
    /** A remote node returned credit and halted. */
    def notifyHalt(credits: Int): Unit = synchronized {
      Logger.entering(getClass.getName, s"halt $credits")
      addCreditAndCheck(credits) {
        enclosingCounter.haltToken()
      }
    }

    /** A remote node returned credit and discorporated. */
    def notifyDiscorporate(credits: Int): Unit = synchronized {
      Logger.entering(getClass.getName, s"discorporate $credits")
      addCreditAndCheck(credits) {
        enclosingCounter.discorporateToken()
      }
    }

    /** A remote node requested resurrect. 
      *
      * Return the credit for the remove node to use.
      */
    def resurrect(): Int = synchronized {
      Logger.entering(getClass.getName, "resurrect")
      enclosingCounter.newToken()
      convertToken()
    }
    
    /** Make a token remote by converting it into credit.
      *
      * The caller must already have a token on `enclosingCounter`. This call consumes it. 
      *
      * Return the credit for the remove node to use.
      */
    def convertToken(): Int = synchronized {
      Logger.entering(getClass.getName, "takeToken")
      remoteTokens += 1
      val (b, n) = credits.split()
      Logger.fine(s"$id: Splitting $credits into 1/(2^$b), $n")
      credits = n
      b
    }
    
    /** Return the local counter associated with this.
      */
    def counter: Counter = enclosingCounter
    
    /** Activate this representation with some credits.
     *  
     *  Token: Provides a local token to the caller.
     */
    def activate(credits: Int): Unit = synchronized {
      Logger.entering(getClass.getName, s"halt $credits")
      enclosingCounter.newToken()
      addCreditAndCheck(credits) {
        enclosingCounter.haltToken()
      }
    }
  }

  // FIXME: Both of these maps are uncleaned. Elements need to be removed at some point.
  private val distributedCounters = new java.util.concurrent.ConcurrentHashMap[CounterProxyManager#DistributedCounterId, DistributedCounter]()
  private val counterIds = new java.util.concurrent.ConcurrentHashMap[Counter, CounterProxyManager#DistributedCounterId]()

  /** Get the distributed representation of `enclosingCounter`.
    *
    * Initially, this represents no members and does not have a token on `enclosingCounter`.
    * 
    * The returned DistributedCounter may be either a `DistributedCounterFragment` or a `DistributedCounterController`.
    */
  def getDistributedCounterForCounter(enclosingCounter: Counter): DistributedCounter = {
    enclosingCounter match {
      case cp: DistributedCounterFragment => {
        cp
      }
      case _ =>
        val id = counterIds.computeIfAbsent(enclosingCounter, (_) => DistributedCounterId(freshRemoteRefId(), runtime.runtimeId))
        distributedCounters.computeIfAbsent(id, (id) => {
          val rmtCounterMbrProxy = new DistributedCounterController(id, enclosingCounter)
          Logger.finer(f"Created proxy for $enclosingCounter with $id")
          rmtCounterMbrProxy
        })
    }
  }

  /** Return the local representation of a remote counter.
    *
    * This may only be called with the Ids of counters that are already created with getDistributedCounterForCounter on some node.
    */
  def getDistributedCounterForId(id: CounterProxyManager#DistributedCounterId): DistributedCounter = {
    distributedCounters.computeIfAbsent(id, (id) => {
      Logger.finer(s"Created local proxy for id $id")
      assert(id.controller != runtime.runtimeId)
      new DistributedCounterFragment(id) {
        private[CounterProxyManager] def returnCreditsHalt(n: Int): Unit = sendHalt(id)(n)
        private[CounterProxyManager] def returnCreditsDiscorporate(n: Int): Unit = sendDiscorporate(id)(n)
        private[CounterProxyManager] def requestCredits(): Unit = sendResurrect(id)()
      }
    })
  }

  def sendHalt(id: CounterProxyManager#DistributedCounterId)(n: Int): Unit = {
    require(n > 0)
    val location = runtime.locationForRuntimeId(id.controller)
    Tracer.traceHaltGroupMemberSend(id.id, execution.runtime.here, location)
    // Credit: Send credit with message
    location.sendInContext(execution)(HaltGroupMemberProxyCmd(execution.executionId, id.id, n))
  }

  def sendDiscorporate(id: CounterProxyManager#DistributedCounterId)(n: Int): Unit = {
    require(n > 0)
    val location = runtime.locationForRuntimeId(id.controller)
    Tracer.traceDiscorporateGroupMemberSend(id.id, execution.runtime.here, location)
    // Credit: Send credit with message
    location.sendInContext(execution)(DiscorporateGroupMemberProxyCmd(execution.executionId, id.id, n))
  }

  def sendResurrect(id: CounterProxyManager#DistributedCounterId)(): Unit = {
    val location = runtime.locationForRuntimeId(id.controller)
    Tracer.traceDiscorporateGroupMemberSend(id.id, execution.runtime.here, location)
    location.sendInContext(execution)(ResurrectGroupMemberProxyCmd(execution.executionId, id.id))
  }

  def sendProvideCredit(destination: PeerLocation, id: CounterProxyManager#DistributedCounterId)(n: Int): Unit = {
    require(n > 0)
    Tracer.traceDiscorporateGroupMemberSend(id.id, execution.runtime.here, destination)
    // Credit: Send credit with message
    destination.sendInContext(execution)(ProvideCounterCreditCmd(execution.executionId, id.id, n))
  }
  
  private def getDistributedCounterForIdHere(counterId: CounterId): Option[DistributedCounterController] = {
    val dcId = DistributedCounterId(counterId, runtime.runtimeId)
    Option(distributedCounters.get(dcId)) match {
      case Some(c: DistributedCounterController) => {
        Some(c)
      }
      case Some(v) =>
        throw new AssertionError(f"getDistributedCounterForIdHere should only be called for counters with a controller in this location: $counterId%#x, $v")
      case None => {
        Logger.info(f"Unknown distributed counter $counterId%#x")
        None
      }
    }
  }

  def haltGroupMemberProxy(counterId: CounterId, n: Int): Unit = {
    getDistributedCounterForIdHere(counterId) map { g =>
      Logger.fine(s"Scheduling $g.notifyHalt($n)")
      // Token: Pass tokens in message to code in schedulable.
      execution.runtime.schedule(new Schedulable { def run(): Unit = { g.notifyHalt(n) } })
    }
  }

  def discorporateGroupMemberProxy(counterId: CounterId, n: Int): Unit = {
    getDistributedCounterForIdHere(counterId) map { g =>
      Logger.fine(s"Scheduling $g.notifyDiscorporate($n)")
      // Token: Pass tokens in message to code in schedulable.
      execution.runtime.schedule(new Schedulable { def run(): Unit = { g.notifyDiscorporate(n) } })
    }
  }

  def resurrectGroupMemberProxy(counterId: CounterId, requester: PeerLocation): Unit = {
    getDistributedCounterForIdHere(counterId) map { g =>
      Logger.fine(s"Scheduling $g.resurrect()")
      execution.runtime.schedule(new Schedulable { 
        def run(): Unit = { 
          val credit = g.resurrect()
          // Credit: Getting credit and forwarding it to the requester.
          execution.sendProvideCredit(requester, g.id)(credit)
        } 
      })
    }
  }

  def provideCounterCredit(counterId: CounterId, origin: PeerLocation, n: Int): Unit = {
    val g = getDistributedCounterForId(DistributedCounterId(counterId, origin.runtimeId))
    g match {
      case g: DistributedCounterFragment => {
        Logger.fine(s"Scheduling $g.provideCredit($n)")
        execution.runtime.schedule(new Schedulable {
          def run(): Unit = {
            // Credit: Give credit to local counter representation.
            g.provideCredit(n)
          }
        })
      }
      case _ =>
        throw new AssertionError(f"provideCounterCredit should only be called for remote counters.")
    }
  }

}

object CounterProxyManager {
  type CounterId = RemoteRef#RemoteRefId
  case class DistributedCounterId(id: CounterId, controller: DOrcRuntime#RuntimeId) extends Serializable {
    override def toString() = f"DistributedCounterId($id%#x @ $controller%#x)"
  }
}
