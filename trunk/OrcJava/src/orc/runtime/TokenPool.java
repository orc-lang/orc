package orc.runtime;

import orc.error.runtime.TokenLimitReachedError;
import orc.runtime.nodes.Node;
import orc.runtime.regions.Region;
import orc.trace.TokenTracer;

/**
 * Implement a token pool to avoid allocating
 * and freeing a lot of tokens.
 * @author quark
 */
public final class TokenPool {
	/** How many tokens are available before we hit the pool size limit. */
	private int available;
	/** Head of list of free tokens. */
	private Token free;
	
	/**
	 * Create a new pool with the given bound.
	 * If bound &lt; 0 then the pool is unlimited. 
	 */
	public TokenPool(int bound) {
		this.available = bound;
	}
	
	/** Create and return a new, uninitialized token. */
	public synchronized Token newToken() throws TokenLimitReachedError {
		if (available == 0) {
			throw new TokenLimitReachedError();
		} else {
			--available;
			if (free != null) {
				Token out = free;
				free = out.nextFree;
				return out;
			}
			return new Token();
		}
	}
	
	/** Free a token */
	public synchronized void freeToken(Token token) {
		++available;
		token.free();
		token.nextFree = free;
		free = token;
	}
}
