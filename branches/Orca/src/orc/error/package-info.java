/**
 * Orc-specific throwables.
 * <ul>
 * <li>OrcError indicates an assertion failure; something that
 * violates an expected invariant.
 * <li>OrcException and descendants indicate various kinds
 * of exceptional conditions. These are further divided by phase:
 * 	<ul>
 * 	<li>CompilationException
 *  <li>ExecutionException
 *  <li>LoadingException
 *  </ul>
 * </ul> 
 */

package orc.error;