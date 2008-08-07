package orc.runtime.values;

import orc.runtime.Token;

public interface ListLike {
	/**
	 * Return the head and tail of a cons-like data structure to a token. The
	 * only place this should be called is the TryCons builtin site.
	 */
	public void uncons(Token caller);
	public void unnil(Token caller);
}
