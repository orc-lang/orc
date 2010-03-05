//
// Globals.java -- Java class Globals
// Project OrcJava
//
// $Id$
//
// Copyright (c) 2009 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.runtime;

import java.util.HashMap;
import java.util.HashSet;

/**
 * Holds references to objects identified by unique string keys.
 * Each key is associated with a handle; a handle's keys can be deleted
 * en masse.
 * 
 * @param <H> handle type
 * @param <V> value type
 * @author quark
 */
public class Globals<H, V> {
	private class Entry {
		public H handle;
		public V value;

		public Entry(final H handle, final V value) {
			this.handle = handle;
			this.value = value;
		}
	}

	private final HashMap<String, Entry> globals = new HashMap<String, Entry>();
	private final HashMap<H, HashSet<String>> handleKeys = new HashMap<H, HashSet<String>>();

	/**
	 * Store a global, generating a unique name.
	 */
	public String add(final H handle, final V value) {
		String key;
		do {
			key = java.util.UUID.randomUUID().toString();
		} while (!put(handle, key, value));
		return key;
	}

	/**
	 * Store a global, using an existing name. Only use this
	 * if you are confident the name is globally unique, i.e.
	 * it's some form of GUID. Returns false if the key is already
	 * in use.
	 */
	public synchronized boolean put(final H handle, final String key, final V value) {
		if (globals.containsKey(key)) {
			return false;
		}
		globals.put(key, new Entry(handle, value));
		HashSet<String> activeKeys = handleKeys.get(handle);
		if (activeKeys == null) {
			activeKeys = new HashSet<String>();
			handleKeys.put(handle, activeKeys);
		}
		activeKeys.add(key);
		return true;
	}

	public synchronized V get(final String key) {
		final Entry e = globals.get(key);
		if (e == null) {
			return null;
		}
		return e.value;
	}

	public synchronized V remove(final String key) {
		final Entry e = globals.remove(key);
		if (e == null) {
			return null;
		}
		final HashSet<String> activeKeys = handleKeys.get(e.handle);
		if (activeKeys != null) {
			activeKeys.remove(key);
		}
		return e.value;
	}

	public synchronized void removeAll(final H handle) {
		final HashSet<String> activeKeys = handleKeys.get(handle);
		if (activeKeys == null) {
			return;
		}
		for (final String key : activeKeys) {
			globals.remove(key);
		}
		handleKeys.remove(handle);
	}
}
