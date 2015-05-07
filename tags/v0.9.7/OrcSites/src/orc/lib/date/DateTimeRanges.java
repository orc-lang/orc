package orc.lib.date;

import java.util.Iterator;
import java.util.SortedSet;
import java.util.TreeSet;

import org.joda.time.DateTime;

public class DateTimeRanges {
	private SortedSet<DateTimeRange> ranges = new TreeSet<DateTimeRange>();
	public DateTimeRanges() {}
	
	public DateTimeRanges(DateTimeRange range) {
		ranges.add(range);
	}

	public void union(DateTimeRanges that) {
		SortedSet<DateTimeRange> tailSet = ranges;
		for (DateTimeRange range : that.ranges) {
			// find all of the ranges overlapping this one
			// and replace them with a new unified range
			tailSet = tailSet.tailSet(range);
			for (Iterator<DateTimeRange> i = tailSet.iterator(); i.hasNext();) {
				DateTimeRange r = i.next();
				if (r.compareTo(range) > 0) break;
				i.remove();
				range.union(r);
			}
			tailSet.add(range);
		}
	}
	
	public void intersect(DateTimeRanges that) {
		SortedSet<DateTimeRange> tailSet = ranges;
		ranges = new TreeSet<DateTimeRange>();
		for (DateTimeRange range : that.ranges) {
			// find all of the ranges overlapping this one, intersect
			// each with this one, and add them to the result
			tailSet = tailSet.tailSet(range);
			for (Iterator<DateTimeRange> i = tailSet.iterator(); i.hasNext();) {
				DateTimeRange r = i.next();
				if (r.compareTo(range) > 0) break;
				r.intersect(range);
				if (!r.isEmpty()) ranges.add(r);
			}
		}
	}
	
	public boolean spans(DateTime date) {
		for (DateTimeRange r : ranges.tailSet(new DateTimeRange(date, date))) {
			return (r.start.compareTo(date) <= 0);
		}
		return false;
	}
	
	public SortedSet<DateTimeRange> getRanges() {
		return ranges;
	}
	
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("DateRanges{\n");
		for (DateTimeRange r : ranges) {
			sb.append(r.toString() + "\n");
		}
		sb.append("}");
		return sb.toString();
	}
}
