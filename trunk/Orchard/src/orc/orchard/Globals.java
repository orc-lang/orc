package orc.orchard;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * Holds references to objects identified by unique string keys.
 * Each key is associated with a handle; a handle's keys can be deleted
 * en masse, or they will automatically be deleted when the handle is
 * garbage collected.
 * 
 * <p>MAGIC: uses weak references to monitor when handes are deleted.
 * 
 * @param <H> handle type
 * @param <V> value type
 * @author quark
 */
public class Globals<H,V> {
	private ReferenceQueue deadEntries = new ReferenceQueue();
	private class Entry extends WeakReference<H> {
		public String key;
		public V value;
		public Entry(H handle, String key, V value) {
			super(handle, deadEntries);
			this.value = value;
			this.key = key;
		}
	}
	private Map<String, Entry> globals = new HashMap<String, Entry>();
	private Map<H, Set<String>> handleKeys =
		Collections.synchronizedMap(new WeakHashMap<H, Set<String>>());
	
	public synchronized String add(H handle, V value) {
		purge();
		String key;
		do {
			key = java.util.UUID.randomUUID().toString();
		} while (globals.containsKey(key));
		globals.put(key, new Entry(handle, key, value));
		// no need to synchronize on handleKeys because
		// handles are only remove asynchronously, never
		// added
		Set<String> activeKeys = handleKeys.get(handle);
		if (activeKeys == null) {
			activeKeys = new HashSet<String>();
			handleKeys.put(handle, activeKeys);
		}
		activeKeys.add(key);
		return key;
	}
	
	public synchronized V get(String key) {
		Entry e = globals.get(key);
		if (e == null) return null;
		return e.value;
	}
	
	public synchronized V remove(String key) {
		Entry e = globals.remove(key);
		if (e == null) return null;
		Set<String> activeKeys = handleKeys.get(e.get());
		if (activeKeys != null) activeKeys.remove(key);
		return e.value;
	}
	
	public synchronized void removeAll(H handle) {
		Set<String> activeKeys = handleKeys.get(handle);
		if (activeKeys == null) return;
		for (String key : activeKeys) {
			globals.remove(key);
		}
		handleKeys.remove(handle);
	}
	
	private void purge() {
		Entry e;
		while ((e = (Entry)deadEntries.poll()) != null) {
			globals.remove(e.key);
		}
	}
}
