package orc.runtime.values;

import orc.runtime.sites.Site;

public interface Visitor<V> {
	public V visit(Closure v);
	public V visit(Constant v);
	public V visit(Field v);
	public V visit(ListValue v);
	public V visit(OptionValue v);
	public V visit(TupleValue v);
	public V visit(Site v);
}
