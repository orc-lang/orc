/**
 * 
 */
package orc.ast;

import orc.runtime.Environment;

/**
 * @author dkitchin
 *
 * Interface implemented by objects which can add bindings to an environment.
 *
 */
public interface EnvBinder {

	public Environment bind(Environment env);
	
}
