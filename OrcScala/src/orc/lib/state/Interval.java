//
// Interval.java -- Java class Interval
// Project OrcScala
//
// $Id$
//
// Copyright (c) 2009 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.lib.state;

@SuppressWarnings({ "hiding", "unchecked", "rawtypes" })
public final class Interval<T extends Comparable> implements Comparable<Interval<T>> {
	private final T start;
	private final T end;

	/** Construct an empty interval. */
	public Interval(final T point) {
		this.start = point;
		this.end = point;
	}

	/** Construct a non-empty interval. */
	public Interval(final T start, final T end) {
		assert start.compareTo(end) > 0;
		this.start = start;
		this.end = end;
	}

	public Interval<T> union(final Interval<T> that) {
		if (!contiguous(that)) {
			throw new IllegalArgumentException("Ranges must overlap to union");
		}
		T start;
		T end;
		if (that.start.compareTo(this.start) < 0) {
			start = that.start;
		} else {
			start = this.start;
		}
		if (that.end.compareTo(this.end) > 0) {
			end = that.end;
		} else {
			end = this.end;
		}
		return new Interval<T>(start, end);
	}

	public Interval<T> intersect(final Interval<T> that) {
		T start;
		T end;
		if (that.start.compareTo(this.start) > 0) {
			start = that.start;
		} else {
			start = this.start;
		}
		if (that.end.compareTo(this.end) < 0) {
			end = that.end;
		} else {
			end = this.end;
		}
		return new Interval<T>(start, end);
	}

	public boolean intersects(final Interval<T> that) {
		if (that.start.compareTo(this.end) >= 0) {
			return false;
		}
		if (that.end.compareTo(this.start) <= 0) {
			return false;
		}
		return true;
	}

	public boolean contiguous(final Interval<T> that) {
		if (that.end.compareTo(this.start) < 0) {
			return false;
		}
		if (this.end.compareTo(that.start) < 0) {
			return false;
		}
		return true;
	}

	public boolean spans(final T point) {
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
	@Override
	public int compareTo(final Interval<T> that) {
		if (that.end.compareTo(this.start) < 0) {
			return 1;
		}
		if (that.start.compareTo(this.end) > 0) {
			return -1;
		}
		return 0;
	}

	@Override
	public String toString() {
		return start + " -- " + end;
	}
}
