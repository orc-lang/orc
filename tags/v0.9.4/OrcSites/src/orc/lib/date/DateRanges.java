package orc.lib.date;

import java.util.Iterator;
import java.util.SortedSet;
import java.util.TreeSet;

import org.joda.time.LocalDateTime;

public class DateRanges {
	private SortedSet<DateRange> ranges = new TreeSet<DateRange>();
	public DateRanges() {}
	
	public DateRanges(DateRange range) {
		ranges.add(range);
	}

	public void union(DateRanges that) {
		SortedSet<DateRange> tailSet = ranges;
		for (DateRange range : that.ranges) {
			// find all of the ranges overlapping this one
			// and replace them with a new unified range
			tailSet = tailSet.tailSet(range);
			for (Iterator<DateRange> i = tailSet.iterator(); i.hasNext();) {
				DateRange r = i.next();
				if (r.compareTo(range) > 0) break;
				i.remove();
				range.union(r);
			}
			tailSet.add(range);
		}
	}
	
	public void intersect(DateRanges that) {
		SortedSet<DateRange> tailSet = ranges;
		ranges = new TreeSet<DateRange>();
		for (DateRange range : that.ranges) {
			// find all of the ranges overlapping this one, intersect
			// each with this one, and add them to the result
			tailSet = tailSet.tailSet(range);
			for (Iterator<DateRange> i = tailSet.iterator(); i.hasNext();) {
				DateRange r = i.next();
				if (r.compareTo(range) > 0) break;
				r.intersect(range);
				if (!r.isEmpty()) ranges.add(r);
			}
		}
	}
	
	public boolean spans(LocalDateTime date) {
		for (DateRange r : ranges.tailSet(new DateRange(date, date))) {
			return (r.start.compareTo(date) <= 0);
		}
		return false;
	}
	
	public SortedSet<DateRange> getRanges() {
		return ranges;
	}
	
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("DateRanges{\n");
		for (DateRange r : ranges) {
			sb.append(r.toString() + "\n");
		}
		sb.append("}");
		return sb.toString();
	}
}
