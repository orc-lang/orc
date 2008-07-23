package orc.runtime;

import orc.error.JavaException;
import orc.error.TokenException;
import orc.runtime.values.Value;

/**
 * A <i>one-shot continuation</i> which a site can use to resume a computation
 * with a suspended value. Exists mainly to provide Java sites with a restricted
 * interface to tokens to prevent them from doing anything bad.
 * 
 * @author quark
 */
public abstract class Continuation {
	/**
	 * Technically this doesn't need to be thread-local, since the current token
	 * should only be read or written from the main engine thread. However making
	 * it thread-local ensures that anybody who tries to read it outside that
	 * thread will not be able to, which is important to prevent site calls from
	 * interfering with each other. Furthermore this makes it easy to use
	 * multiple engine threads to take advantage of SMP.
	 */
	private static ThreadLocal currentToken = new ThreadLocal();

	/**
	 * Get a continuation which can be used by a site to return a value to the
	 * Orc engine. This may only be called by a site once during a site call,
	 * and it must be called by the thread that initiated the call.
	 * 
	 * <p>Note that this does not block or perform any control flow; it's up to
	 * the caller to return normally after calling this method (though the return
	 * value will be ignored).
	 */
	public synchronized static Continuation suspend() {
		Token token = (Token)currentToken.get();
		if (token == null) {
			throw new AssertionError("No current token available.");
		}
		// can only suspend once!
		currentToken.set(null);
		return new TokenContinuation(token);
	}
	
	public interface Thunk {
		public Value apply() throws TokenException;
	}
	
	public static void withToken(Token caller, Thunk thunk) throws TokenException {
		setCurrentToken(caller);
		Value value = thunk.apply();
		caller = getCurrentToken();
		if (caller != null) {
			// the site never suspended, so we can go ahead
			// and resume with the value it returned
			caller.resume(value);
		}
	}
	
	/** Package access so OrcEngine can use this. */
	static void setCurrentToken(Token token) {
		currentToken.set(token);
	}
	
	/** Package access so OrcEngine can use this. */
	static Token getCurrentToken() {
		return (Token)currentToken.get();
	}
	
	/**
	 * Kill the continuation (indicate it will never return). May only be called
	 * once.
	 */
	public abstract void die();

	/**
	 * Signal an error. Well-behaved sites will call this instead of throwing an
	 * exception, to allow Orc to handle the error correctly.
	 */
	public abstract void error(TokenException e);
	
	public void error(Exception e) {
		error(new JavaException(e));
	}
	
	/**
	 * Return a value from a site call. May only be called once.
	 * 
	 * <p>
	 * Note that this does not perform any control flow; the actual resumption
	 * will occur in a separate thread, so it's up to the caller what to
	 * do after resuming.
	 */
	public abstract void resume(Value value);
	
	/**
	 * Continuations which resume by resuming a token.
	 * @author quark
	 */
	public static class TokenContinuation extends Continuation {
    	private Token token;
    	public TokenContinuation(Token token) {
    		this.token = token;
    	}
    	
    	public synchronized void die() {
    		if (token == null) {
    			throw new AssertionError("Tried to kill an old continuation.");
    		}
    		token.die();
    		token = null;
    	}
    	
    	public synchronized void error(TokenException e) {
    		if (token == null) {
    			throw new AssertionError("Tried to kill an old continuation.");
    		}
    		token.error(e);
    		token = null;
    	}
    	
    	public synchronized void resume(Value value) {
    		if (token == null) {
    			throw new AssertionError("Tried to resume an old continuation.");
    		}
    		token.resume(value);
    		token = null;
    	}
	}
	
	/**
	 * Continuation which resumes by resuming a parent continuation.
	 * @author quark
	 */
	public static abstract class NestedContinuation extends Continuation {
    	protected final Continuation parent;
    	public NestedContinuation(Continuation parent) {
    		this.parent = parent;
    	}
    	
    	public void die() {
    		parent.die();
    	}
    	
    	public void error(TokenException e) {
    		parent.error(e);
    	}
    	
    	public void resume(Value value) {
    		parent.resume(value);
    	}
	}
}
