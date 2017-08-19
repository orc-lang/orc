package orc.values.sites

import orc.values.Field
import orc.{Invoker, Accessor}

/** An external method interface in which the method provides Invokers directly.
  */
trait InvokerMethod {
  /** Get an invoker for this target type and argment types.
    *
    * This method is slow and the results should be cached if possible.
    *
    * @return An Invoker or DirectInvoker for the given values or an 
    * 			  instance of InvokerError if there is no invoker.
    * 
    * @see UncallableValueInvoker
    */
  def getInvoker(args: Array[AnyRef]): Invoker
}

  
/** An external value interface in which the value provides Accessors directly.
  */
trait AccessorValue {
  /** Get an accessor which extracts a given field value from this target.
    *
    * This method is slow and the results should be cached if possible.
    *
    * @return An Accessor for the given classes or an 
    * 			  instance of AccessorError if there is no accessor.
    * 
    * @see NoSuchMemberAccessor, DoesNotHaveMembersAccessor
    */
  def getAccessor(field: Field): Accessor
}