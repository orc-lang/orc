package orc.lib.state;

import java.util.LinkedList;

import orc.error.runtime.TokenException;
import orc.lib.state.types.CounterType;
import orc.runtime.Args;
import orc.runtime.Token;
import orc.runtime.sites.DotSite;
import orc.runtime.sites.EvalSite;
import orc.runtime.sites.PartialSite;
import orc.runtime.sites.Site;
import orc.type.Type;
import orc.type.structured.ArrowType;
import orc.type.structured.MultiType;

/**
 * Factory for counters. 
 * @author quark
 */
public class Counter extends EvalSite {
	@Override
	public Object evaluate(Args args) throws TokenException {
		final int init = (args.size() == 0) ? 0 : args.intArg(0);
		return new DotSite() {
			private int count = init;
			private LinkedList<Token> waiters = new LinkedList<Token>();
			@Override
			protected void addMembers() {
				addMember("inc", new EvalSite() {
					@Override
					public Object evaluate(Args args) throws TokenException {
						++count;
						return signal();
					}
				});
				addMember("dec", new PartialSite() {
					@Override
					public Object evaluate(Args args) throws TokenException {
						if (count > 0) {
							--count;
							if (count == 0) {
								for (Token waiter : waiters) {
									waiter.unsetQuiescent();
									waiter.resume();
								}
								waiters.clear();
							}
							return signal();
						} else return null;
					}
				});
				addMember("onZero", new Site() {
					@Override
					public void callSite(Args args, Token caller) throws TokenException {
						if (count == 0) caller.resume();
						else {
							caller.setQuiescent();
							waiters.add(caller);
						}
					}
				});
				addMember("value", new EvalSite() {
					@Override
					public Object evaluate(Args args) throws TokenException {
						return count;
					}
				});
			}
		};
	}
	
	
	public Type type() {
		return new MultiType(
				new ArrowType(new CounterType()),
				new ArrowType(Type.INTEGER, new CounterType()));
	}
}
