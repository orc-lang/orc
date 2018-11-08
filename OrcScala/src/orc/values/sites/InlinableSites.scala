//
// InlinableSites.scala -- Scala traits InlinableAccessor and InlinableInvoker
// Project OrcScala
//
// Created by amp on Nov 3, 2018.
//
// Copyright (c) 2018 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.values.sites

import orc.Accessor
import orc.Invoker

/** A marker trait for accessors which can be aggressively recursively inlined by the runtime.
  *
  * The accessor implementation should be general simple and should not perform
  * recursive calls (either directly or indirectly by calling into other code).
  * More specifically, the code should be partially evaluation safe for Truffle.
  *
  * Inlining applies to both `get` and `canGet`.
  */
trait InlinableAccessor extends Accessor

/** A marker trait for invokers which can be aggressively recursively inlined by the runtime.
  *
  * The invoker implementation should be general simple and should not perform
  * recursive calls (either directly or indirectly by calling into other code).
  * More specifically, the code should be partially evaluation safe for Truffle.
  *
  * Inlining applies to `invoke`, `invokeDirect`, and `canInvoke`.
  */
trait InlinableInvoker extends Invoker

/** A marker trait for invokers which can potentially be aggressively recursively inlined by the runtime.
  *
  * The invoker implementation should be general simple and should not perform
  * recursive calls (either directly or indirectly by calling into other code).
  * More specifically, the code should be partially evaluation safe for Truffle.
  *
  * Inlining applies to `invoke`, `invokeDirect`, and `canInvoke`.
  */
trait MaybeInlinableInvoker extends Invoker {
  /** True if this invoker should be inlined.
    */
  val inlinable: Boolean
}
