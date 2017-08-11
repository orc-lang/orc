//
// package.scala -- orc.run.porce.distrib, but really just for developer documentation.
// Project PorcE
//
// Created by amp on Aug 11, 2017.
//
// Copyright (c) 2017 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.run.porce

/*
 * The following comments describe where DOrc can hook into the PorcE framework for specific features.
 */

/* Migration at call sites:
 * 
 * DOrc should override OrcRuntime.getInvoker to return a special remote invoker which handles
 * migration. The remote invoker should return false from canInvoke in all cases where migration
 * is not needed (all values are local for instance). Both canInvoke(...), and invoke(...) 
 * methods take the target and all values as arguments. The canInvoke method does not have
 * access to the runtime, but that shouldn't be needed to check that all arguments are available.
 * The invoke method has access to the runtime, the continuation to run on publication, and the 
 * counter and terminator via the call handle.
 * 
 */

/* Migration at publications in trim:
 * 
 * DOrc should implement a subclass of Terminator which overrides the `kill(k)` method. This method
 * can return false to claim that the continuation will be called as needed by the runtime and 
 * should not be called by the interpreter. Then the DOrc runtime can handle the kill as needed
 * and execute `k` on a different node if needed.
 * 
 */

/* Call PorcE code:
 * 
 * Convert the RemoteRefId for the call target to a RootCallTarget if needed. Then construct
 * a closure using `new PorcEClosure(environment, callTarget, isRoutine)`. Finally, create and
 * schedule an instance of CallClosureSchedulable. See `object CallClosureSchedulable` for 
 * factory methods.
 * 
 */

/* Interacting with Terminators and Counters:
 * 
 * The all public methods on Terminator and Counter are thread-safe and can be directly called
 * at any time. However several of the methods can directly call into PorcE code, and should
 * be scheduled (using any Schedulable you like) to avoid doing work on the remote communication
 * thread.
 * 
 * Terminator#addChild(c): May call c.kill() triggering arbitrary non-blocking execution. 
 * Terminator#removeChild(c): Only performs a remove on a ConcurrentHashMap.
 * Terminator#kill() and Terminator#kill(k): May triggering arbitrary non-blocking execution. 
 * Terminator#isLive(): Only performs an atomic read.
 * Terminator#checkLive(): Only performs an atomic read.
 * 
 * Counter#setDiscorporate(): Sets a volatile flag.
 * Counter#haltToken() and Counter#discorporateToken(): May triggering arbitrary non-blocking execution.
 * Counter#newToken(): May Need to climb the group tree to the root to create newTokens if `this` was discorporated. No PorcE code is executed, but the tree could be large.
 * 
 */

/* Interacting with Futures:
 * 
 * PorcE Futures implement the orc.Future interface and PorcE supports orc.Future objects
 * as futures anywhere a PorcE Future is allowed. So simply use the orc.Future if you 
 * need to pass a future into PorcE.
 * 
 * If you need to resolve a PorcE Future. Call the methods on orc.run.porce.runtime.Future.
 * Like the methods on Terminator and Counter these methods are thread safe, but may cause 
 * arbitrary execution.
 * 
 * Future#bind(v) and Future#stop(): May triggering arbitrary non-blocking execution.
 * 
 * The methods described in orc.Future will never trigger arbitrary execution, other than
 * calling back into the FutureReader.
 */

package object distrib {
  
}