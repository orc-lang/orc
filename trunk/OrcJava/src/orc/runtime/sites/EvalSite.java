/**
 * 
 */
package orc.runtime.sites;

import orc.runtime.Args;
import orc.runtime.OrcRuntimeTypeError;
import orc.runtime.Token;
import orc.runtime.values.Value;

/**
 * Abstract class for sites with a total and immediate semantics: evaluate the arguments and
 * return a value without blocking and without affecting the Orc engine. Essentially, subclasses
 * of this class represent sites without any special concurrent behavior.
 * 
 * Subclasses must implement the method evaluate, which takes an argument list and returns
 * a single value.
 * 
 * @author dkitchin
 *
 */
public abstract class EvalSite extends Site {

	@Override
	public void callSite(Args args, Token caller) {

		try {
			caller.resume(evaluate(args));
		}
		catch (OrcRuntimeTypeError e) {
			System.out.println("Call failed due to a type error; remaining silent. [" + e.getMessage() + "]");
		}
	}
	
	abstract public Value evaluate(Args args) throws OrcRuntimeTypeError;

}
