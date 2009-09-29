{--
Metalanguage operations.
--}

{--
@site Site[A](A) :: A

This site promotes an Orc closure to a site; when the site is called, the
closure is executed on those arguments. These executions behave
like site calls; in particular, the following four properties hold:

* The site, like any site, is strict in its arguments
* The site returns only the first value published by the executed closure. The 
  closure continues to run, but its subsequent publications are discarded.
* The execution of the closure is protected from termination. If the site
  call is terminated, the closure still runs, and its publications are simply
  ignored.
* If the execution of the closure halts, so does the site call.

The typical usage of Site looks like:
def foo(...) = ...
val Foo = Site(foo)


The typing of Site will enforce the side condition that the type A is an arrow
type.

--}
site Site = orc.lib.reflect.SiteSite
