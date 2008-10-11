package orc.lib.data;

import orc.error.runtime.ArgumentTypeMismatchException;
import orc.error.runtime.RuntimeTypeException;
import orc.error.runtime.TokenException;
import orc.runtime.Args;
import orc.runtime.sites.Constructor;
import orc.runtime.values.TupleValue;

public class Left extends Either {
	private static class LeftTag extends Tag {
		public LeftTag(Object value) { super(value); }
	}
	@Override
	public Object construct(Args args) throws TokenException {
		return new LeftTag(args.getArg(0));
	}
	@Override
	public TupleValue deconstruct(Object arg) throws TokenException {
		if (!(arg instanceof Tag)) throw new ArgumentTypeMismatchException(arg.getClass() + " is not an instance of Either.Tag");
		if (!(arg instanceof LeftTag)) return null;
		return new TupleValue(((LeftTag)arg).value);
	}
}
