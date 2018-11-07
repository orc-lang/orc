//
// ABPWSDeque.java -- Java class ABPWSDeque
// Project OrcScala
//
// Created by amp on Feb 28, 2017.
//
// Copyright (c) 2018 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.util;

import java.lang.reflect.Field;
import java.util.Arrays;

import sun.misc.Unsafe;

/**
 * @author amp
 */
@sun.misc.Contended
public final class ABPWSDeque<T> implements SchedulingQueue<T> {
  private static final Object ABORT = null;

  // age contains a 16-bit top field followed by a 48-bit tag.
  private volatile long       age   = 0;
  private volatile int        bot   = 0;
  private final Object[]      deq;

  private static final Unsafe       U;
  private static final long         AGE;

  static {
    // initialize field offsets for CAS
    try {
      Field f = Unsafe.class.getDeclaredField("theUnsafe");
      f.setAccessible(true);
      U = (Unsafe) f.get(null);
      Class<?> k = ABPWSDeque.class;
      AGE = U.objectFieldOffset(k.getDeclaredField("age"));
    } catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e) {
      throw new ExceptionInInitializerError(e);
    }
  }

  private boolean casAge(long oldVal, long newVal) {
    return U.compareAndSwapLong(this, AGE, oldVal, newVal);
  }

  /**
   * Constructs a ABP WS Deque with given bound.
   */
  public ABPWSDeque(int maxSize) {
    assert maxSize < (1 << 16);
    deq = new Object[maxSize];
  }

  private static int getTop(long age) {
    return (int) (age & 0xffffL);
  }

  private static long getTag(long age) {
    return (age >> 16) & 0xffffffffffffL;
  }

  private static long makeAge(int top, long tag) {
    return top | ((tag & 0xffffffffffffL) << 16);
  }

  /**
   * 
   */
  @Override
  public boolean push(T v) {
    int localBot = bot;

    // Check for deque overflow.
    if (localBot >= deq.length)
      return false;

    deq[localBot] = v;
    localBot++;
    bot = localBot;
    return true;
  }

  /**
   * 
   */
  @Override
  @SuppressWarnings("unchecked")
  public T pop() {
    int localBot = bot;
    if (localBot == 0)
      return null;
    localBot--;
    bot = localBot;
    Object v = deq[localBot];

    long oldAge = age;
    int oldTop = getTop(oldAge);
    long oldTag = getTag(oldAge);

    // If we didn't get the last element return without CAS
    if (localBot > oldTop) {
      deq[localBot] = null;
      return (T) v;
    }

    // If we did get the last element reset to the bottom and get confirm that
    // top and the old bot are equal and try to CAS in a new tag.
    bot = 0;
    long newAge = makeAge(0, oldTag + 1);
    if (localBot == oldTop && casAge(oldAge, newAge)) {
      deq[localBot] = null;
      return (T) v;
    }

    // ???
    age = newAge;
    return null;
  }

  /**
   * 
   */
  @Override
  @SuppressWarnings("unchecked")
  public T steal() {
    long oldAge = age;
    int localBot = bot;
    int oldTop = getTop(oldAge);
    long oldTag = getTag(oldAge);
    if (localBot <= oldTop) {
      return null;
    }
    Object v = deq[oldTop];
    long newAge = makeAge(oldTop + 1, oldTag);
    if (casAge(oldAge, newAge)) {
      return (T) v;
    }
    return (T) ABORT;
  }

  /**
   * Return current number of elements in the queue. This should only be called
   * from the owner thread and the number may decrease (but not increase) at any
   * time due to calls to popTop().
   */
  @Override
  public int size() {
    long oldAge = age;
    int localBot = bot;
    int oldTop = getTop(oldAge);
    return localBot - oldTop;
  }

  /**
   * Clear all locations to null in an empty deque. This may only be called on
   * an empty deque and may only be called form the owner thread.
   */
  @Override
  public void clean() {
    if (size() > 0) {
      throw new IllegalStateException("ABPWSDeque must be empty to be cleaned");
    }

    Arrays.fill(deq, null);
  }
}
