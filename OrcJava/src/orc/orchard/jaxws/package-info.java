/**
 * JAX-WS bindings for Orchard services.
 *
 * <p>In order to compile these, you will need to do a manual or clean build.
 * I have added an Ant wsgen.xml file with the necessary tasks to build the
 * webservice stuff.
 * 
 * <p>To run a web service for testing, just run the corresponding Java file in
 * Eclipse, e.g. run ExecutorService. The "main" method will start a mini embedded
 * HTTP server at http://localhost:8080/orchard.
 * 
 * <p>In theory it should be possible to integrate this stuff into Eclipse
 * automatically via the JDT-APT plugin (which is included in 3.2+).
 * Unfortunately there are problems with the JAX-WS annotation processor
 * which make it impossible to use via JDT-APT:
 * <ul>
 * <li>"The JAX-WS annotation processor stores and reads static data that
 * prevents it from being used incrementally", according to
 * <a href="https://bugs.eclipse.org/bugs/show_bug.cgi?id=106541">JDT-APT bug 106541</a>.
 * Fortunately, the JDT-APT folks provide a work around which allows JAX-WS
 * annotations to be processed in "Batch" mode (although this makes it no
 * more automatic than the Ant solution we're currently using).
 * <li>More seriously, if JAX-WS RI is used to process annotations on
 * multiple webservices at once with different endpointInterface annotations,
 * it barfs. This is recorded as a "feature request" at
 * <a href="https://jax-ws.dev.java.net/issues/show_bug.cgi?id=345">JAX-WS RI bug 345</a>,
 * but in any case that makes the annotation processor unusable even
 * with the "Batch mode" hack.
 * </ul>
 * 
 * <p>For testing purposes I would recommend installing the soapUI plugin.
 * Instructions can be found at http://www.soapui.org/eclipse/index.html
 */
package orc.orchard.jaxws;
