--
-- web.inc -- Orc standard prelude include, web section
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
@site Browse(String) :: Signal

Attempts to open a browser window in whatever user context
is available, pointing to the URL given by the string argument.
The URL must be absolute and well-formed. If the URL is malformed
or unreachable, the call halts silently. 

Example:
<programlisting language="orc-demo"><![CDATA[
-- Open a browser window to the Google home page
Browse("http://www.google.com")
]]></programlisting>
--}
site Browse = orc.lib.web.Browse

{- HTTP -} 
site HTTP = orc.lib.web.HTTP