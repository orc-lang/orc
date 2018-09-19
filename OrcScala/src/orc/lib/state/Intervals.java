//
// Intervals.java -- Java class Intervals
// Project OrcScala
//
// Copyright (c) 2016 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.lib.state;

import java.util.Iterator;
import java.util.NoSuchElementException;

@SuppressWarnings({ "hiding", "unchecked", "rawtypes" })
public final class Intervals<T extends Comparable> implements Iterable<Interval<T>> {
    private static class DTRCons<T extends Comparable> {
        protected DTRCons<T> next;
        protected final Interval<T> interval;

        public DTRCons(final DTRCons<T> next, final Interval<T> range) {
            this.next = next;
            this.interval = range;
        }
    }

    /** An ordered (increasing) linked list of disjoint intervals. */
    private final DTRCons<T> head;

    private Intervals(final DTRCons<T> head) {
        this.head = head;
    }

    public Intervals() {
        this.head = null;
    }

    public Intervals(final Interval<T> range) {
        this(new DTRCons(null, range));
    }

    /**
     * This is most efficient when the interval goes at the front of the set.
     */
    public Intervals<T> union(final Interval<T> interval_) {
        Interval<T> interval = interval_;
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

    public Intervals<T> intersect(final Intervals<T> that) {
        // identical ranges
        if (this == that) {
            return this;
        }
        // empty ranges
        if (isEmpty()) {
            return this;
        }
        if (that.isEmpty()) {
            return that;
        }
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

    public boolean spans(final T point) {
        for (final Interval<T> r : this) {
            if (r.spans(point)) {
                return true;
            }
        }
        return false;
    }

    public boolean isEmpty() {
        return head == null;
    }

    @Override
    public Iterator<Interval<T>> iterator() {
        return new MyIterator(head);
    }

    private static class MyIterator<T extends Comparable> implements Iterator<Interval<T>> {
        private DTRCons next;

        public MyIterator(final DTRCons next) {
            this.next = next;
        }

        @Override
        public boolean hasNext() {
            return next != null;
        }

        @Override
        public Interval<T> next() {
            if (next == null) {
                throw new NoSuchElementException();
            }
            final Interval<T> out = next.interval;
            next = next.next;
            return out;
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("{");
        final Iterator<Interval<T>> it = iterator();
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
