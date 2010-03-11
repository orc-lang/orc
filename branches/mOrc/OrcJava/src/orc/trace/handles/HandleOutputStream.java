//
// HandleOutputStream.java -- Java class HandleOutputStream
// Project OrcJava
//
// $Id$
//
// Copyright (c) 2008 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.trace.handles;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

/**
 * Used in conjunction with {@link Handle} to explicitly manage the lifetime of
 * serialized references. Use it like this:
 * <ol>
 * <li>Instead of using pointers in your object graph, use instances of
 * {@link Handle}.</li>
 * <li>You should call {@link #reset()} regularly to keep the output
 * stream from wasting memory.</li>
 * <li>{@link Handle}s are not affected by {@link #reset()}; instead, the
 * order in which {@link Handle}s are constructed and serialized determines
 * when the output stream forgets about them.</li>
 * </ol>
 * 
 * <p>
 * Some background on the problem this solves: the standard
 * {@link ObjectOutputStream} uses a cache of every object it has seen in order
 * to serialize pointers. This prevents the objects from being GCed. You can
 * clear the cache with a call to {@link ObjectOutputStream#reset()}, but then
 * pointers will no longer be correct.
 * 
 * <p>
 * Why doesn't {@link ObjectOutputStream} just use a weak hash map? That would
 * be fine for writing, but it wouldn't work for reading. Reading still needs to
 * cache pointer values to reconstruct the object graph, but it can't tell when
 * it has seen the last reference to an object in the input file.
 * 
 * <p>
 * The solution implemented here is to allow pointers to be reset on an
 * individual basis, so that both the output and input streams can tell when
 * they have seen the last pointer to an object and clear the object from their
 * caches.
 * 
 * @see HandleOutputStream
 * @see Handle
 * @author quark
 */
public final class HandleOutputStream extends ObjectOutputStream {
	private int resetInterval = 0;
	private int resetCount = 0;

	public HandleOutputStream(final OutputStream out) throws IOException {
		super(out);
	}

	/**
	 * The reset interval determines how often {@link #maybeReset()} actually resets.
	 */
	public HandleOutputStream(final OutputStream out, final int resetInterval) throws IOException {
		super(out);
		this.resetInterval = resetInterval;
	}

	/**
	 * Map values to their corresponding handle ids. This does not need to be a
	 * (relatively expensive) weak hash map because handles are cleared
	 * explicitly using {@link LastHandle}.
	 */
	private final Map<Object, Integer> handles = new HashMap<Object, Integer>();
	/**
	 * Largest handle yet used.
	 */
	private int maxHandle = 0;
	/**
	 * List of freed handles which can be reused.
	 * This should ensure that we don't run out of
	 * handles before running out of memory.
	 */
	private final LinkedList<Integer> freeHandles = new LinkedList<Integer>();

	/**
	 * Change the reset interval. Also resets the current counter so the next
	 * reset occurs after resetInterval calls to {@link #maybeReset()}.
	 */
	public void setResetInterval(final int resetInterval) {
		this.resetInterval = resetInterval;
	}

	/**
	 * This uses the reset interval set by {@link #setResetInterval(int)} or
	 * {@link #HandleOutputStream(OutputStream, int)} to decide whether to
	 * reset the output stream. The default (safe) behavior is to always reset.
	 * @throws IOException 
	 */
	public void maybeReset() throws IOException {
		if (resetInterval == resetCount++) {
			reset();
		}
		if (resetCount > resetInterval) {
			resetCount = 0;
		}
	}

	// I wish I could override writeObject to avoid sharing Handles,
	// but it's declared final.
	// public void writeObject(Object o) { ... }

	/**
	 * Return the handle ID associated with a handle value.
	 */
	public int getHandle(final Object value) {
		assert value != null;
		final int out = handles.get(value);
		assert out > 0;
		return out;
	}

	/**
	 * Create and return a new handle ID for the given value.
	 */
	public int newHandle(final Object value) throws IOException {
		assert value != null;
		int id;
		if (freeHandles.isEmpty()) {
			id = ++maxHandle;
			if (id == 0) {
				throw new IOException("Out of handles");
			}
		} else {
			id = freeHandles.poll();
		}
		handles.put(value, id);
		return id;
	}

	/**
	 * Free the handle ID associated with the given value.
	 * Only call this if you know the handle exists.
	 */
	public void freeHandle(final Object value) {
		assert value != null;
		final int id = handles.remove(value);
		assert id != 0;
		freeHandles.add(id);
	}
}
