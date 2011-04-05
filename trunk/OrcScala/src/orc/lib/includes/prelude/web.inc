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
Web browsing, HTTP, and JSON capabilities.
--}

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
site Browse = "orc.lib.web.Browse"

{--
The HTTP site provides a simple mechanism to send GET and POST requests to a URL.
 
@site HTTP(java.net.URL) :: HTTP 

Publishes an HTTP site which accepts HTTP requests on the given URL.

@method http.get() :: String

Performs a GET request on the URL used to create this HTTP site. 
The payload is empty. 
The response is published as a single string.

@method http.post(String) :: String

Performs a POST request on the URL used to create this HTTP site. 
The string argument is used as a UTF-16 encoded payload.
The response is published as a single string.

@method http.url :: String

Publishes the URL used to create this HTTP site. If query parameters
were used, they will be displayed in the query part of the URL,
with the appropriate encoding.

@site HTTP(String) :: HTTP

Converts the given string to a URL U and then behaves as HTTP(U).

@site HTTP(String, {..}) :: HTTP

Takes a string S and a query record Q. Maps Q to a URL query string QS by translating each record binding to a query pair, escaping characters
if necessary, and then behaves as HTTP(S+QS).

--} 
site HTTP = "orc.lib.web.HTTP"

{--
@site ReadJSON(String) :: Top 

Parses a string representation of a JSON value, producing an Orc representation of that JSON value. 

An Orc representation of a JSON value, called an OrcJSON value, 
is either a record of OrcJSON values (representing a JSON object), 
a list of OrcJSON values (representing a JSON array), 
or a literal value (representing a JSON primitive value).

ReadJSON and WriteJSON are mutual inverses. 
If <replaceable>S</replaceable> is a string representation of a JSON value, then <code>WriteJSON(ReadJSON(</code><replaceable>S</replaceable><code>))</code> = <replaceable>S</replaceable>, modulo whitespace and object member ordering.

--}
site ReadJSON = "orc.lib.web.ReadJSON"

{-- 
@site WriteJSON(Top) :: String 

Serializes an Orc representation of a JSON value, producing a string representation of that JSON value. 

An Orc representation of a JSON value, called an OrcJSON value, 
is either a record of OrcJSON values (representing a JSON object), 
a list of OrcJSON values (representing a JSON array), 
or a literal value (representing a JSON primitive value).

WriteJSON and ReadJSON are mutual inverses. 
If <replaceable>V</replaceable> is an Orc representation of a JSON value, then <code>ReadJSON(WriteJSON(</code><replaceable>V</replaceable><code>))</code> = <replaceable>V</replaceable>.

--}
site WriteJSON = "orc.lib.web.WriteJSON"

