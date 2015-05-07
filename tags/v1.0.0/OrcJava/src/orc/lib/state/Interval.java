package orc.lib.state;

public final class Interval<T extends Comparable> implements Comparable<Interval<T>> {
	private final T start;
	private final T end;
	/** Construct an empty interval. */
	public Interval(T point) {
		this.start = point;
		this.end = point;
	}
	/** Construct a non-empty interval. */
	public Interval(T start, T end) {
		assert(start.compareTo(end) > 0);
		this.start = start;
		this.end = end;
	}
	public Interval<T> union(Interval<T> that) {
		if (!contiguous(that)) throw new IllegalArgumentException("Ranges must overlap to union");
		T start;
		T end;
		if (that.start.compareTo(this.start) < 0) start = that.start;
		else start = this.start;
		if (that.end.compareTo(this.end) > 0) end = that.end;
		else end = this.end;
		return new Interval(start, end);
	}
	public Interval<T> intersect(Interval<T> that) {
		T start;
		T end;
		if (that.start.compareTo(this.start) > 0) start = that.start;
		else start = this.start;
		if (that.end.compareTo(this.end) < 0) end = that.end;
		else end = this.end;
		return new Interval(start, end);
	}
	public boolean intersects(Interval<T> that) {
		if (that.start.compareTo(this.end) >= 0) return false;
		if (that.end.compareTo(this.start) <= 0) return false;
		return true;
	}
	public boolean contiguous(Interval<T> that) {
		if (that.end.compareTo(this.start) < 0) return false;
		if (this.end.compareTo(that.start) < 0) return false;
		return true;
	}
	public boolean spans(T point) {
		return start.compareTo(point) <= 0 && end.compareTo(point) > 0;
	}
	public boolean isEmpty() {
		return start.compareTo(end) >= 0;
	}
	public T getStart() {
		return start;
	}
	public T getEnd() {
		return end;
	}
	/**
	 * Returns 0 if the intervals overlap. For non-disjoint intervals, this
	 * violates transitivity, but that's ok because we ensure that only
	 * disjoint ranges are stored in a sorted set.
	 * 
	 * <p>Note that even if this method returns 0, the two objects are
	 * not necessarily equal.
	 */
	public int compareTo(Interval<T> that) {
		if (that.end.compareTo(this.start) < 0) return 1;
		if (that.start.compareTo(this.end) > 0) return -1;
		return 0;
	}
	
	public String toString() {
		return start + " -- " + end;
	}
}