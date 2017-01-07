/**
 * This package contains source code derived from the 1.2
 * release of the JAX-WS JSON binding, found at
 * https://jax-ws-commons.dev.java.net/json/
 * 
 * <p>I have incorporated the source code into our project
 * because it's obviously unmaintained and suffering from
 * bitrot. Bugfixes I have made so far:
 * <ul>
 * <li>Updated to work with org.codehaus.jettison v1.0
 * <li>Fix &lt;script&gt; tag instructions in index.html
 * <li>Fix bug with serializing void returns
 * <li>Use jettison MappedXMLStreamReader
 * <li>Fix bug with serializing JSON strings on Javascript side
 * <li>Allow Javascript code to handle SOAP errors in return handler
 * rather than throwing an uncatchable exception
 * <li>Javascript code should post back to the original request URL.
 * <li>Support to call a function with the service object when it is ready.
 * <li>Fix various bugs with MappedXMLStreamWriter
 * <li>Fix serialization bug with returning String
 * <li>Fix errors with unrecognized namespace when using xsi:type attributes
 * (by hard-coding this namespace into the configuration).
 * <li>Support Javascript error handlers.
 * </ul>
 * 
 * @author quark
 */
package org.jvnet.jax_ws_commons.json;
