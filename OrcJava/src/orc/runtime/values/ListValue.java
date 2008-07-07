package orc.runtime.values;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * Common ancestor for ConsValue and NilValue. Unlike scheme, the Cons
 * constructor does not allow you to create a degenerate cons where
 * the tail is not a list, so we can guarantee that all Conses actually
 * have a list structure. (If you want a degenerate cons, just use a
 * tuples.)
 */
public abstract class ListValue extends Value implements Iterable { 
	
	public abstract List<Value> enlist();
	
	public static ListValue make(List<Value> vs) {
		
		ListValue l = new NilValue();
		
		for(int i = vs.size() - 1; i >= 0; i--) {
			l = new ConsValue(vs.get(i), l);
		}
		
		return l;
	}
	
	public Iterator iterator() {
		return enlist().iterator();
	}
	
	@Override
	public <E> E accept(Visitor<E> visitor) {
		return visitor.visit(this);
	}
}
