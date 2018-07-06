//
// InvokerMethod.scala -- Scala traits InvokerMethod and AccessorValue
// Project OrcScala
//
// Copyright (c) 2017 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.values.sites

import orc.values.Field
import orc.{Invoker, Accessor, OrcRuntime}

/** An external method interface in which the method provides Invokers directly.
  */
trait InvokerMethod {
  /** Get an invoker for this target type and argument types.
    *
    * This method is slow and the results should be cached if possible.
    *
    * @return An Invoker or DirectInvoker for the given values or an
    *         instance of InvokerError if there is no invoker.
    *
    * @see UncallableValueInvoker
    */
  def getInvoker(runtime: OrcRuntime, args: Array[AnyRef]): Invoker
}


/** An external value interface in which the value provides Accessors directly.
  */
trait AccessorValue {
  /** Get an accessor which extracts a given field value from this target.
    *
    * This method is slow and the results should be cached if possible.
    *
    * @return An Accessor for the given classes or an
    *         instance of AccessorError if there is no accessor.
    *
    * @see NoSuchMemberAccessor, DoesNotHaveMembersAccessor
    */
  def getAccessor(runtime: OrcRuntime, field: Field): Accessor
}
