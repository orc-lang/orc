package orc.runtime;

import orc.error.runtime.TokenLimitReachedError;
import orc.runtime.nodes.Node;
import orc.runtime.regions.Region;
import orc.trace.TokenTracer;

public class TokenPool {
	private int available;
	
	public TokenPool(int bound) {
		this.available = bound;
	}
	
	public synchronized Token newToken() throws TokenLimitReachedError {
		if (available == 0) {
			throw new TokenLimitReachedError();
		} else {
			--available;
			return new Token();
		}
	}
	
	public synchronized void freeToken(Token token) {
		++available;
	}
}
