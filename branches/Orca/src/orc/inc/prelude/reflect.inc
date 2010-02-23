--
-- reflect.inc -- Orc standard prelude include, metalanguage section
-- Project OrcJava
--
-- $Id$
--
-- Copyright (c) 2009 The University of Texas at Austin. All rights reserved.
--
-- Use and redistribution of this file is governed by the license terms in
-- the LICENSE file found in the project's top-level directory and also found at
-- URL: http://orc.csres.utexas.edu/license.shtml .
--

{--
Metalanguage operations.
--}

{--
@site Site[A](A) :: A

This site promotes an Orc closure to a site; when the site is called, the
closure is executed on those arguments. These executions behave
like site calls; in particular, the following four properties hold:

<itemizedlist>
<listitem> The site, like all sites, is strict in its arguments. </listitem>
<listitem> The site returns only the first value published by the executed closure. The closure continues to run, but its subsequent publications are discarded. </listitem>
<listitem> The execution of the closure is protected from termination. If the site call is terminated, the closure still runs, and its publications are simply ignored. </listitem>
<listitem> If the execution of the closure halts, so does the site call. </listitem>
</itemizedlist>

The typical usage of Site looks like:

<programlisting language="orc-demo"><![CDATA[
def foo(...) = ...
val Foo = Site(foo)]]></programlisting>

The typing of Site will enforce the side condition that the type A is an arrow
type.
--}
site Site = orc.runtime.sites.core.SiteSite
