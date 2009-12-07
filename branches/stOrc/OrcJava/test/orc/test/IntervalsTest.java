package orc.test;

import orc.lib.state.Interval;
import orc.lib.state.Intervals;

import org.junit.Test;
import static org.junit.Assert.*;

public class IntervalsTest {
	@Test public void union1_good1() {
		Interval<Integer> ival1 = new Interval(1, 3);
		Interval<Integer> ival2 = new Interval(2, 4);
		assertEquals("1 -- 4", ival1.union(ival2).toString());
	}
	@Test public void union1_good2() {
		Interval<Integer> ival1 = new Interval(1, 2);
		Interval<Integer> ival2 = new Interval(2, 4);
		assertEquals("1 -- 4", ival1.union(ival2).toString());
	}
	@Test public void union1_bad1() {
		Interval<Integer> ival1 = new Interval(1, 2);
		Interval<Integer> ival2 = new Interval(3, 4);
		try {
			ival1.union(ival2);
			assert(false);
		} catch (IllegalArgumentException e) {
			// do nothing
		}
	}
	@Test public void union2_good1() {
		Interval<Integer> ival = new Interval(1, 3);
		Intervals<Integer> ivals = new Intervals(new Interval(2, 4));
		assertEquals("{1 -- 4}", ivals.union(ival).toString());
	}
	@Test public void union2_good2() {
		Interval<Integer> ival = new Interval(1, 2);
		Intervals<Integer> ivals = new Intervals(new Interval(2, 4));
		assertEquals("{1 -- 4}", ivals.union(ival).toString());
	}
	@Test public void union2_good3() {
		Interval<Integer> ival = new Interval(1, 2);
		Intervals<Integer> ivals = new Intervals(new Interval(3, 4));
		assertEquals("{1 -- 2, 3 -- 4}", ivals.union(ival).toString());
	}
	@Test public void union2_good4() {
		Interval<Integer> ival = new Interval(3, 4);
		Intervals<Integer> ivals = new Intervals(new Interval(1, 2));
		assertEquals("{1 -- 2, 3 -- 4}", ivals.union(ival).toString());
	}
	@Test public void union2_good5() {
		Interval<Integer> ival = new Interval(1, 2);
		Intervals<Integer> ivals = new Intervals();
		assertEquals("{1 -- 2}", ivals.union(ival).toString());
	}
	@Test public void union2_good6() {
		Interval<Integer> ival = new Interval(1, 2);
		Intervals<Integer> ivals = new Intervals();
		ivals = ivals.union(new Interval(1, 2));
		ivals = ivals.union(new Interval(3, 4));
		ivals = ivals.union(new Interval(5, 6));
		ivals = ivals.union(new Interval(2, 3));
		assertEquals("{1 -- 4, 5 -- 6}", ivals.toString());
	}
	@Test public void union2_good7() {
		Interval<Integer> ival = new Interval(1, 2);
		Intervals<Integer> ivals = new Intervals();
		ivals = ivals.union(new Interval(1, 2));
		ivals = ivals.union(new Interval(3, 4));
		ivals = ivals.union(new Interval(5, 6));
		ivals = ivals.union(new Interval(2, 5));
		assertEquals("{1 -- 6}", ivals.toString());
	}
	@Test public void union2_good8() {
		Interval<Integer> ival = new Interval(1, 2);
		Intervals<Integer> ivals = new Intervals();
		ivals = ivals.union(new Interval(1, 2));
		ivals = ivals.union(new Interval(3, 4));
		ivals = ivals.union(new Interval(5, 6));
		ivals = ivals.union(new Interval(7, 8));
		ivals = ivals.union(new Interval(2, 3));
		ivals = ivals.union(new Interval(6, 7));
		assertEquals("{1 -- 4, 5 -- 8}", ivals.toString());
	}
	@Test public void isect2_good1() {
		Intervals<Integer> ivals1 = new Intervals();
		ivals1 = ivals1.union(new Interval(1, 4));
		Intervals<Integer> ivals2 = new Intervals();
		ivals2 = ivals2.union(new Interval(2, 3));
		assertEquals("{2 -- 3}", ivals1.intersect(ivals2).toString());
	}
	@Test public void isect2_good2() {
		Intervals<Integer> ivals1 = new Intervals();
		ivals1 = ivals1.union(new Interval(1, 4));
		Intervals<Integer> ivals2 = new Intervals();
		ivals2 = ivals2.union(new Interval(1, 2));
		ivals2 = ivals2.union(new Interval(3, 5));
		assertEquals("{1 -- 2, 3 -- 4}", ivals1.intersect(ivals2).toString());
	}
	@Test public void isect2_good3() {
		Intervals<Integer> ivals1 = new Intervals();
		ivals1 = ivals1.union(new Interval(2, 3));
		Intervals<Integer> ivals2 = new Intervals();
		ivals2 = ivals2.union(new Interval(1, 2));
		ivals2 = ivals2.union(new Interval(3, 5));
		assertEquals("{}", ivals1.intersect(ivals2).toString());
	}
	@Test public void isect2_good4() {
		Intervals<Integer> ivals1 = new Intervals();
		ivals1 = ivals1.union(new Interval(1, 3));
		ivals1 = ivals1.union(new Interval(4, 6));
		ivals1 = ivals1.union(new Interval(7, 9));
		Intervals<Integer> ivals2 = new Intervals();
		ivals2 = ivals2.union(new Interval(2, 5));
		ivals2 = ivals2.union(new Interval(6, 8));
		assertEquals("{2 -- 3, 4 -- 5, 7 -- 8}", ivals1.intersect(ivals2).toString());
	}
}
