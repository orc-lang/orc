package orc.lib.state;

import java.util.Iterator;
import java.util.NoSuchElementException;

public final class Intervals<T extends Comparable> implements Iterable<Interval<T>> {
	private static class DTRCons<T extends Comparable> {
		private DTRCons<T> next;
		private final Interval<T> interval;
		public DTRCons(DTRCons<T> next, Interval<T> range) {
			this.next = next;
			this.interval = range;
		}
	}
	/** An ordered (increasing) linked list of disjoint intervals. */
	private final DTRCons<T> head;
	private Intervals(DTRCons<T> head) {
		this.head = head;
	}
	public Intervals() {
		this.head = null;
	}
	public Intervals(Interval<T> range) {
		this(new DTRCons(null, range));
	}
	
	/**
	 * This is most efficient when the interval goes at the front of the set.
	 */
	public Intervals<T> union(Interval<T> interval) {
		DTRCons headOut, tailOut;
		headOut = tailOut = new DTRCons(null, null);
		DTRCons it;
		// scan for the place in the list where interval will go
		scanning: for (it = head; it != null; it = it.next) {
			switch (it.interval.compareTo(interval)) {
			case 0:
				// current overlaps interval; grow interval
				interval = interval.union(it.interval);
				break;
			case -1:
				// current < interval; add current
				tailOut = tailOut.next = new DTRCons(null, it.interval);
				break;
			case 1:
				// current > interval; stop
				break scanning;
			}
		}
		// postcondition: it comes after interval
		// add the range
		tailOut = tailOut.next = new DTRCons(null, interval);
		// add any remaining items
		tailOut.next = it;
		return new Intervals(headOut.next);
	}
	
	public Intervals<T> intersect(Intervals<T> that) {
		// identical ranges
		if (this == that) return this;
		// empty ranges
		if (isEmpty()) return this;
		if (that.isEmpty()) return that;
		DTRCons head, tail;
		head = tail = new DTRCons(null, null);
		DTRCons next1 = this.head;
		DTRCons next2 = that.head;
		while (next1 != null && next2 != null) {
			switch (next1.interval.compareTo(next2.interval)) {
			case -1:
				// next1 < next2
				next1 = next1.next;
				break;
			case 1:
				// next2 < next1
				next2 = next2.next;
				break;
			case 0:
				// next1 and next2 may overlap
				if (next1.interval.intersects(next2.interval)) {
					// append their intersection
					tail = tail.next = new DTRCons(null, next1.interval.intersect(next2.interval));
				}
				// move forward whichever ends first
				if (next1.interval.getEnd().compareTo(next2.interval.getEnd()) <= 0) {
					next1 = next1.next;
				} else {
					next2 = next2.next;
				}
			}
		}
		return new Intervals(head.next);
	}
	
	public boolean spans(T point) {
		for (Interval<T> r : this) {
			if (r.spans(point)) return true;
		}
		return false;
	}
	
	public boolean isEmpty() {
		return head == null;
	}
	
	public Iterator<Interval<T>> iterator() {
		return new MyIterator(head);
	}
	
	private static class MyIterator<T extends Comparable> implements Iterator<Interval<T>> {
		private DTRCons next;
		public MyIterator(DTRCons next) {
			this.next = next;
		}
		public boolean hasNext() {
			return next != null;
		}

		public Interval<T> next() {
			if (next == null) throw new NoSuchElementException();
			Interval<T> out = next.interval;
			next = next.next;
			return out;
		}

		public void remove() {
			throw new UnsupportedOperationException();
		}
	}
	
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("{");
		Iterator<Interval<T>> it = iterator();
		if (it.hasNext()) {
			sb.append(it.next());
			while (it.hasNext()) {
				sb.append(", " + it.next());
			}
		}
		sb.append("}");
		return sb.toString();
	}
}
