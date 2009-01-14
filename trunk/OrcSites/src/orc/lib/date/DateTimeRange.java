package orc.lib.date;

import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;

/** The end date is considered exclusive. */
public final class DateTimeRange implements Comparable<DateTimeRange> {
	public final static DateTimeRange NULL;
	static {
		DateTime dummy = new DateTime();
		NULL = new DateTimeRange(dummy, dummy);
	}
	public DateTime start;
	public DateTime end;
	public DateTimeRange(DateTime start, DateTime end) {
		super();
		this.start = start;
		this.end = end;
	}
	public DateTimeRange(LocalDateTime start, LocalDateTime end) {
		this(start.toDateTime(), end.toDateTime());
	}
	public DateTimeRange(LocalDate start, LocalDate end) {
		this(start.toDateMidnight().toDateTime(), end.toDateMidnight().toDateTime());
	}
	public void union(DateTimeRange that) {
		if (that.start.compareTo(this.start) < 0) this.start = that.start;
		if (that.end.compareTo(this.end) > 0) this.end = that.end;
	}
	public void intersect(DateTimeRange that) {
		if (that.start.compareTo(this.start) > 0) this.start = that.start;
		if (that.end.compareTo(this.end) < 0) this.end = that.end;
	}
	public boolean intersects(DateTimeRange that) {
		if (that.start.compareTo(this.end) >= 0) return false;
		if (that.end.compareTo(this.start) <= 0) return false;
		return true;
	}
	public boolean isEmpty() {
		return start.compareTo(end) >= 0;
	}
	public DateTime getStart() {
		return start;
	}
	public DateTime getEnd() {
		return end;
	}
	/**
	 * Returns 0 if the ranges overlap. For non-disjoint ranges, this
	 * violates transitivity, but that's ok because we ensure that only
	 * disjoint ranges are stored in a sorted set.
	 * 
	 * <p>Also note that this ordering is "inconsistent with equals".
	 */
	public int compareTo(DateTimeRange that) {
		if (that.end.compareTo(this.start) < 0) return 1;
		if (that.start.compareTo(this.end) > 0) return -1;
		return 0;
	}
	
	public String toString() {
		return start.toString() + " - " + end.toString();
	}
}