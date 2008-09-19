package orc.lib.date;

import org.joda.time.LocalDateTime;

/** The end date is considered exclusive. */
public class DateRange implements Comparable<DateRange> {
	public final static DateRange NULL;
	static {
		LocalDateTime dummy = new LocalDateTime();
		NULL = new DateRange(dummy, dummy);
	}
	public LocalDateTime start;
	public LocalDateTime end;
	public DateRange(LocalDateTime start, LocalDateTime end) {
		super();
		this.start = start;
		this.end = end;
	}
	public void union(DateRange that) {
		if (that.start.compareTo(this.start) < 0) this.start = that.start;
		if (that.end.compareTo(this.end) > 0) this.end = that.end;
	}
	public void intersect(DateRange that) {
		if (that.start.compareTo(this.start) > 0) this.start = that.start;
		if (that.end.compareTo(this.end) < 0) this.end = that.end;
	}
	public boolean isEmpty() {
		return start.compareTo(end) >= 0;
	}
	public LocalDateTime getStart() {
		return start;
	}
	public LocalDateTime getEnd() {
		return end;
	}
	/**
	 * Returns 0 if the ranges overlap. For non-disjoint ranges, this
	 * violates transitivity, but that's ok because we ensure that only
	 * disjoint ranges are stored in a sorted set.
	 * 
	 * <p>Also note that this ordering is "inconsistent with equals".
	 */
	public int compareTo(DateRange that) {
		if (that.end.compareTo(this.start) < 0) return 1;
		if (that.start.compareTo(this.end) > 0) return -1;
		return 0;
	}
	
	public String toString() {
		return start.toString() + " - " + end.toString();
	}
}