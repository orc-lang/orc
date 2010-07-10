package orc.run

import orc.OrcRuntime
import orc.run.extensions._

class StandardOrcRuntime extends OrcRuntime
with Orc
with StandardInvocationBehavior
with ActorBasedScheduler
with SupportForCapsules
with SupportForSynchronousExecution
with SupportForRtimer
with SupportForStdout
with ExceptionReportingOnConsole


/* The first behavior in the trait list will be tried last */
trait StandardInvocationBehavior extends InvocationBehavior
with ErrorOnUndefinedInvocation
with SupportForSiteInvocation
with SupportForJavaObjectInvocation






