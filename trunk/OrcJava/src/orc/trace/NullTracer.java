package orc.trace;

import java.io.IOException;

import orc.error.runtime.TokenException;
import orc.trace.events.Event;

/**
 * Do-nothing tracer, used when tracing is not enabled.
 * @author quark
 */
public final class NullTracer implements Tracer {

	public void call(Object site, Object[] arguments) {}

	public void die() {}

	public void resume(Object value) {}
	
	public void block() {}
	
	public void unblock() {}

	public Tracer fork() {
		return this;
	}

	public void start() {}
	public void flush() {}
	public void print(String value, boolean newline) {}
	public void publish(Object value) {}
	public void error(TokenException error) {}
}
