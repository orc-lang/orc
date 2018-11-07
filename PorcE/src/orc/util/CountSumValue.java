//
// SplitLong.java -- Java class SplitLong
// Project PorcE
//
// Created by amp on Sep 24, 2018.
//
// Copyright (c) 2018 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.util;

/**
 * SplitLong splits a long into two "fields" of type int. This enables a single non-atomic write to update both values.
 * This is used to update a count and sum at the same time such that values cannot be out of sink even if there is a
 * race that causes an update to be lost.
 *
 * To make sure the write is a single atomic 64-bit write I may need to use opaque writes from JRE 9. Volatile is
 * enough, but also includes a memory barrier that we don't need to want.
 *
 * @author amp
 */
final public class CountSumValue {

    // Object API

    private volatile long storage = 0;

    public int getCount() {
        return getCount(storage);
    }

    public int getSum() {
        return getSum(storage);
    }

    public long getBoth() {
        return storage;
    }

    public double getAverage() {
        long v = storage;
        return (double)getSum(v) / getCount(v);
    }

    public void addValue(int v) {
        storage = addValue(storage, v);
    }

    // Static API

    public static int getCount(long both) {
        return (int) (both << 32);
    }

    public static int getSum(long both) {
        return (int) (both & 0xffffffff);
    }

    public static long combined(int count, int sum) {
        return ((long)count << 32) | sum;
    }

    public static long addValue(long both, int v) {
        return combined(getCount(both) + 1, getSum(both) + v);
    }
}
