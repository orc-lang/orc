package orc.trace.values;

/**
 * Mark a runtime object which may be traced (serialized in a trace file). If
 * the object is mutable it must also override {@link Object#equals(Object)} and
 * {@link Object#hashCode()} to allow traced representations to be cached
 * correctly.
 * 
 * @see Marshaller#marshal(Object)
 * @author quark
 */
public interface TraceableValue {
	public Value marshal(Marshaller tracer);
}
