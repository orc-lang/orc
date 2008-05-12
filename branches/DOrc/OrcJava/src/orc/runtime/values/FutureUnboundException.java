package orc.runtime.values;

/**
 * Thrown if someone tries to read a Future value before it is ready.
 * This is slightly cleaner than using a null value to indicate a
 * not-ready future.
 */
public class FutureUnboundException extends Exception {
	/** Will the future ever be available? */
	public boolean alive;
	public FutureUnboundException(boolean alive) {
		this.alive = alive;
	}
}