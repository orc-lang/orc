/**
 * RMI bindings for Orchard services.
 *
 * <p>To run these services locally:
 * <ol>
 * <li>Start rmiregistry with the classpath pointing to the Orc classpath.
 * <li>Run {@link orc.orchard.rmi.ExecutorService}. By default this starts the executor at
 * rmi://localhost/orchard
 * <li>Run {@link orc.orchard.rmi.RmiTest} to run a simple test.
 * </ol>
 * 
 * <p>Running across a network is somewhat more complicated.
 * I will document that soon.
 */

package orc.orchard.rmi;

