package orc.runtime.values;

import orc.runtime.Kilim;
import orc.runtime.Token;
import orc.runtime.Kilim.PausableCallable;
import orc.runtime.Kilim.Promise;

public abstract class LazyListValue extends Value implements ListLike {
	public abstract void uncons(Token caller);
	public abstract void unnil(Token caller);
}
