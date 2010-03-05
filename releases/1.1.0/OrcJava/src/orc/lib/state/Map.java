//
// Map.java -- Java class Map
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

package orc.lib.state;

import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

import orc.runtime.sites.core.Equal;
import orc.runtime.values.Reference;

/**
 * Map which uses Orc's rules for equality for keys. They are not used for
 * values because you really shouldn't be calling {@link #containsValue(Object)}
 * on a map anyways.
 * 
 * @author quark
 */
public final class Map<K, V> extends AbstractMap<K, V> {
	private final HashMap<Wrapper<K>, V> map = new HashMap<Wrapper<K>, V>();

	private static class Wrapper<K> {
		public K value;

		public Wrapper(final K value) {
			this.value = value;
		}

		@Override
		public int hashCode() {
			return value.hashCode();
		}

		@Override
		public boolean equals(final Object that) {
			return Equal.eq(value, ((Wrapper) that).value);
		}
	}

	private static class MyEntry<K, V> implements Entry<K, V> {
		public Entry<Wrapper<K>, V> entry;

		public MyEntry(final Entry<Wrapper<K>, V> entry) {
			this.entry = entry;
		}

		public K getKey() {
			return entry.getKey().value;
		}

		public V getValue() {
			return entry.getValue();
		}

		public V setValue(final V value) {
			return entry.setValue(value);
		}
	}

	public class MapReference implements Reference<V> {
		private final K key;

		public MapReference(final K key) {
			this.key = key;
		}

		public V read() {
			return get(key);
		}

		public void write(final V value) {
			put(key, value);
		}
	}

	public Reference<V> apply(final K key) {
		return new MapReference(key);
	}

	@Override
	public void clear() {
		map.clear();
	}

	@Override
	public boolean containsKey(final Object key) {
		return map.containsKey(new Wrapper(key));
	}

	@Override
	public boolean containsValue(final Object value) {
		return map.containsValue(value);
	}

	@Override
	public Set<Entry<K, V>> entrySet() {
		final Set<Entry<Wrapper<K>, V>> set = map.entrySet();
		return new AbstractSet<Entry<K, V>>() {
			@Override
			public Iterator<Entry<K, V>> iterator() {
				final Iterator<Entry<Wrapper<K>, V>> iterator = set.iterator();
				return new Iterator<Entry<K, V>>() {

					public boolean hasNext() {
						return iterator.hasNext();
					}

					public java.util.Map.Entry<K, V> next() {
						return new MyEntry(iterator.next());
					}

					public void remove() {
						iterator.remove();
					}
				};
			}

			@Override
			public void clear() {
				set.clear();
			}

			@Override
			public boolean contains(final Object o) {
				if (!(o instanceof MyEntry)) {
					return false;
				}
				return set.contains(((MyEntry) o).entry);
			}

			@Override
			public boolean isEmpty() {
				return set.isEmpty();
			}

			@Override
			public boolean remove(final Object o) {
				if (!(o instanceof MyEntry)) {
					return false;
				}
				return set.remove(((MyEntry) o).entry);
			}

			@Override
			public int size() {
				return set.size();
			}
		};
	}

	@Override
	public V get(final Object key) {
		return map.get(new Wrapper(key));
	}

	@Override
	public V put(final K key, final V value) {
		return map.put(new Wrapper(key), value);
	}

	@Override
	public V remove(final Object key) {
		return map.remove(new Wrapper(key));
	}

	@Override
	public int size() {
		return map.size();
	}

	@Override
	public Collection<V> values() {
		return map.values();
	}
}
