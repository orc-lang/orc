package orc.lib.util;

import java.util.List;

import orc.error.runtime.ArgumentTypeMismatchException;
import orc.error.runtime.RuntimeTypeException;
import orc.error.runtime.TokenException;
import orc.runtime.Args;
import orc.runtime.Token;
import orc.runtime.nodes.Node;
import orc.runtime.sites.Site;
import orc.runtime.values.Callable;
import orc.runtime.values.ListValue;
import orc.runtime.values.Value;

/**
 * Apply a callable to a list of arguments.
 * HACK: this is a subclass of site but has slightly different
 * semantics: the callable argument is forced as a call (i.e.
 * free variables in the callable are not forced).
 * @author quark
 */
public class Apply extends Site {
	@Override
	public void createCall(Token caller, List<Object> args, Node nextNode) throws TokenException {
		Callable callable = Value.forceCall(args.get(0), caller);
		if (callable == Value.futureNotReady) return;
		Object arguments = Value.forceArg(args.get(1), caller);
		if (arguments == Value.futureNotReady) return;
		
		if (!(arguments instanceof ListValue))
			throw new ArgumentTypeMismatchException(1, "ListValue", arguments.getClass().toString());
		
		callable.createCall(caller, ((ListValue)arguments).enlist(), nextNode);
	}

	@Override
	public void callSite(Args args, Token caller) throws TokenException {
		// DO NOTHING
	}
}
