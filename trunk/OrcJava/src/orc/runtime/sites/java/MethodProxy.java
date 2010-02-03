//
// MethodProxy.java -- Java class MethodProxy
// Project OrcJava
//
// $Id$
//
// Copyright (c) 2010 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.runtime.sites.java;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.Callable;

import kilim.Fiber;
import kilim.Pausable;
import kilim.State;
import kilim.Task;
import orc.error.runtime.JavaException;
import orc.error.runtime.SiteException;
import orc.error.runtime.TokenException;
import orc.runtime.Args;
import orc.runtime.Kilim;
import orc.runtime.Token;
import orc.runtime.Kilim.PausableCallable;
import orc.runtime.sites.Site;
import orc.runtime.values.Value;

/**
 * Allow a Java method to be used as an Orc site.
 * 
 * <p>MAGIC: pausible methods are run in a Kilim task.
 * We actually go to some lengths to avoid running
 * non-pausable methods in a Kilim task to ensure that
 * if this site is wrapped by ThreadSite, the method
 * will actually run in its own thread and not in the
 * Kilim thread.
 * 
 * @author quark, dkitchin
 */
public class MethodProxy extends Site {
	MethodHandle handle;
	Object instance;

	public MethodProxy(final Object instance, final MethodHandle delegate) {
		this.handle = delegate;
		this.instance = instance;
	}

	@Override
	public void callSite(final Args args, final Token caller) throws TokenException {
		final Object[] oargs = args.asArray();
		final Method m = handle.resolve(oargs);
		if (isPausable(m)) {
			// pausable methods are invoked within a Kilim task
			invokePausable(caller, m, instance, oargs);
		} else {
			// non-pausable methods should be invoked directly
			invoke(caller, m, instance, oargs);
		}
	}

	private static boolean isPausable(final Method m) {
		for (final Class<?> exception : m.getExceptionTypes()) {
			if (exception == Pausable.class) {
				return true;
			}
		}
		return false;
	}

	private static void invoke(final Token caller, final Method m, final Object that, final Object[] args) {
		// FIXME: too much indirection is necessary to run this in a site thread
		Kilim.runThreaded(caller, new Callable<Object>() {
			public Object call() throws Exception {
				try {
					return wrapResult(m, m.invoke(that, args));
				} catch (final InvocationTargetException e) {
					throw new JavaException(e.getCause());
				}
			}
		});
	}

	private static Object wrapResult(final Method m, final Object o) {
		if (m.getReturnType().getName().equals("void")) {
			return Value.signal();
		} else {
			return o;
		}
	}

	private static void invokePausable(final Token caller, final Method m, final Object that, final Object[] args) {
		// Find the woven method
		final Method pm;
		final Class[] parameters = m.getParameterTypes();
		final Class[] pparameters = new Class[parameters.length + 1];
		for (int i = 0; i < parameters.length; ++i) {
			pparameters[i] = parameters[i];
		}
		pparameters[pparameters.length - 1] = Fiber.class;
		try {
			pm = m.getDeclaringClass().getMethod(m.getName(), pparameters);
		} catch (final NoSuchMethodException e) {
			caller.error(new SiteException("Unwoven method: " + m));
			return;
		}
		final Object[] pargs = new Object[args.length + 1]; // +1 for the Fiber
		for (int i = 0; i < args.length; ++i) {
			pargs[i] = args[i];
		}

		// Invoke the method inside a task
		Kilim.runPausable(caller, new PausableCallable<Object>() {
			@Override
			public Object call() throws Pausable, Exception {
				try {
					return wrapResult(pm, _invokePausable(pm, that, pargs));
				} catch (final InvocationTargetException e) {
					// attempt to unwrap a reflected exception
					final Throwable cause = e.getCause();
					if (cause instanceof Exception) {
						throw (Exception) cause;
					} else if (cause instanceof Error) {
						throw (Error) cause;
					} else {
						// some other Throwable which can't be thrown directly
						throw e;
					}
				}
			}
		});
	}

	/**
	 * Invoke a possibly-pausable method reflectively.
	 * The weaver translates calls to this into calls to
	 * {@link Task#_invokePausable(Method, Object, Object[], Fiber)}
	 */
	private static Object _invokePausable(final Method m, final Object that, final Object[] args) throws Pausable, IllegalArgumentException, IllegalAccessException, InvocationTargetException {
		throw new AssertionError("Unwoven method: private static Object MethodProxy#_invokePausable(Method, Object, Object[])");
	}

	/**
	 * Hand-woven implementation of pausable invocation. This is necessary to
	 * weave the fiber into the reflective invocation.
	 */
	@SuppressWarnings("unused")
	private static Object _invokePausable(Method m, Object that, Object[] args, final Fiber f) throws IllegalArgumentException, IllegalAccessException, InvocationTargetException {
		if (f.pc == 1) {
			// resuming
			final InvokeState state = (InvokeState) f.curState;
			m = state.m;
			that = state.that;
			args = state.args;
		}
		args[args.length - 1] = f.down();
		final Object out = m.invoke(that, args);
		if (f.up() == 2) { // Fiber.PAUSING__NO_STATE
			final InvokeState state = new InvokeState();
			state.pc = 1;
			state.m = m;
			state.that = that;
			state.args = args;
			f.setState(state);
		}
		return out;
	}

	/**
	 * Pause state for invocation.
	 */
	private static class InvokeState extends State {
		public Method m;
		public Object that;
		public Object[] args;
	}

	@Override
	public boolean equals(final Object o) {
		if (o == null) {
			return false;
		}
		if (!(o instanceof MethodProxy)) {
			return false;
		}
		final MethodProxy that = (MethodProxy) o;
		return that.instance == this.instance && that.handle == this.handle;
	}

	@Override
	public int hashCode() {
		return (instance != null ? instance.hashCode() : 0) + (handle != null ? handle.hashCode() * 31 : 0);
	}
}
