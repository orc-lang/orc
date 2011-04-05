--
-- reflect.inc -- Orc standard prelude include, metalanguage section
-- Project OrcScala
--
-- $Id$
--
-- Copyright (c) 2011 The University of Texas at Austin. All rights reserved.
--
-- Use and redistribution of this file is governed by the license terms in
-- the LICENSE file found in the project's top-level directory and also found at
-- URL: http://orc.csres.utexas.edu/license.shtml .
--

{--
Metalanguage operations.
--}

{--
@site MakeSite[A](A) :: A

This site promotes an Orc closure to a site; when the site is called, the
closure is executed on those arguments. These executions behave
like site calls; in particular, the following four properties hold:

<itemizedlist>
<listitem><para>The site, like all sites, is strict in its arguments.</para></listitem>
<listitem><para>The site returns only the first value published by the executed closure. The closure continues to run, but its subsequent publications are discarded. </para></listitem>
<listitem><para>The execution of the closure is protected from being <link linkend="ref.concepts.states.kill">killed</link>. If the site call is <link linkend="ref.concepts.states.kill">killed</link>, the closure still runs, and its publications are simply ignored. </para></listitem>
<listitem><para>If the execution of the closure <link linkend="ref.concepts.states.halt">halts</link>, so does the site call. </para></listitem>
</itemizedlist>

The typical usage of MakeSite looks like:

<programlisting language="orc"><![CDATA[
def foo(...) = ...
val Foo = MakeSite(foo)]]></programlisting>

The typing of MakeSite enforces the additional condition that the type A is a function type.
--}
site MakeSite = "orc.lib.builtin.MakeSite"
