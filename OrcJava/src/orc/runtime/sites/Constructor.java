package orc.runtime.sites;

import orc.error.runtime.MessageNotUnderstoodException;
import orc.error.runtime.TokenException;
import orc.runtime.Args;
import orc.runtime.values.TupleValue;

/**
 * Implement a data constructor/deconstructor.
 * @author quark
 */
public abstract class Constructor extends EvalSite {
	@Override
	public Object evaluate(Args args) throws TokenException {
		String field = null;
		try {
			field = args.fieldName();
		} catch (TokenException e) {
			// not a deconstructor
			return construct(args);
		}
		if (!field.equals("?")) throw new MessageNotUnderstoodException(field);
		return new PartialSite() {
			@Override
			public Object evaluate(Args args) throws TokenException {
				return deconstruct(args.getArg(0));
			}
		};
	}
	/** Return null if the object does not match. */
	public abstract TupleValue deconstruct(Object arg) throws TokenException;
	public abstract Object construct(Args args) throws TokenException;
}