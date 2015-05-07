package orc.runtime.values;

import orc.runtime.sites.Site;

public abstract class Visitor<V> {
	public abstract V visit(Object value);
	
	public V visit(Value value) {
		return this.visit((Object)value);
	}
	public V visit(Closure v) {
		return this.visit((Value)v);
	}
	public V visit(ListValue v) {
		return this.visit((Value)v);
	}
	public V visit(TupleValue v) {
		return this.visit((Value)v);
	}
	public V visit(Site v) {
		return this.visit((Value)v);
	}
	
	public final static <V> V visit(Visitor<V> visitor, Object value) {
		if (value instanceof Value) {
			return ((Value)value).accept(visitor);
		} else {
			return visitor.visit(value);
		}
	}
}
