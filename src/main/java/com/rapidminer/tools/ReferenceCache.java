/**
 * Copyright (C) 2001-2019 by RapidMiner and the contributors
 * 
 * Complete list of developers available at our web site:
 * 
 * http://rapidminer.com
 * 
 * This program is free software: you can redistribute it and/or modify it under the terms of the
 * GNU Affero General Public License as published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Affero General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License along with this program.
 * If not, see http://www.gnu.org/licenses/.
*/
package com.rapidminer.tools;

import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;
import java.util.LinkedHashMap;
import java.util.Map;


/**
 * Heap utilization aware reference cache of the given size. The cache uses the LRU strategy to
 * displace entries.
 * <p>
 * New cache entries can be created via {@link newReference()}. A newly created reference behaves
 * like a {@link SoftReference} as long as the reference is cached. Once displaced from the cache,
 * the references behaves like a {@link WeakReference}.
 * <p>
 * The cache is heap utilization aware in the sense that it displaces entries (if any) if the load
 * factor of the heap is higher than {@value #MAX_HEAP_LOAD_FACTOR}.
 *
 * @author Michael Knopf, Gisa Schaefer
 * @since 7.1.0
 */
public class ReferenceCache<T> {

	/** Cache entries are displaced if the heap load factor is higher than {@value}. */
	private static final double MAX_HEAP_LOAD_FACTOR = 0.5;

	/** LRU cache for the (stronger) {@link SoftReference}s. */
	private final Map<TransparentWeakReference, SoftReference<T>> cache;

	/** The size of this cache. */
	private final int size;

	/**
	 * A heap utilization aware reference object similar in its interface to {@link WeakReference}s
	 * and {@link SoftReference}s. In most scenarios, it is stronger than a {@link WeakReference}
	 * but weaker than a {@link SoftReference}.
	 *
	 * @see ReferenceCache
	 */
	public class Reference {

		/** Weak reference that lives is never cleared manually (fall back for the cache). */
		private final TransparentWeakReference weak;

		/**
		 * Creates a new cached reference to the given object.
		 *
		 * @param the
		 *            object to be cached
		 */
		private Reference(T value) {
			weak = new TransparentWeakReference(value);

			if (value != null) {
				synchronized (cache) {
					checkHeapUtilization();
					cache.put(weak, new SoftReference<>(value));
				}
			}
		}

		/**
		 * Returns this reference object's referent. If this reference object has been cleared,
		 * either by the program or by the garbage collector, then this method returns {@code null}.
		 * <p>
		 * If the reference has not been cleared yet, the corresponding cache entry will be updated.
		 * This makes it more likely that subsequent look ups will be successful.
		 *
		 * @return the object to which this reference refers, or {@code null} if this reference
		 *         object has been cleared
		 */
		public T get() {
			SoftReference<T> soft;

			synchronized (cache) {
				checkHeapUtilization();
				soft = cache.get(weak);
			}

			if (soft == null) {
				return weak.get();
			} else {
				T value = soft.get();
				if (value != null) {
					return value;
				} else {
					return weak.get();
				}
			}
		}

		/**
		 * Returns this reference object's referent. If this reference object has been cleared,
		 * either by the program or by the garbage collector, then this method returns {@code null}.
		 * <p>
		 * Unlike {@link #get()}, invoking this method does not update the cache and thus does not
		 * increase the likelihood of successful subsequent look ups.
		 *
		 * @return the object to which this reference refers, or {@code null} if this reference
		 *         object has been cleared
		 */
		public T weakGet() {
			return weak.get();
		}
	}

	/**
	 * A {@link WeakReference} that is transparent with respect to {@link #hashCode()} and
	 * {@link #equals(Object)}. It inherits its referent's initial hash code and determines equality
	 * by using the referents' implementations.
	 */
	private class TransparentWeakReference extends WeakReference<T> {

		/** The referent's initial hash code. */
		private final int hashCode;

		private TransparentWeakReference(T referent) {
			super(referent);
			// remember the referents initial hash code
			hashCode = referent == null ? 0 : referent.hashCode();
		}

		@Override
		public int hashCode() {
			return hashCode;
		}

		@Override
		public boolean equals(Object obj) {
			if (obj instanceof ReferenceCache.TransparentWeakReference) {
				WeakReference<?> other = (WeakReference<?>) obj;
				Object thisReferrent = get();
				Object otherReferrent = other.get();
				return thisReferrent == null ? otherReferrent == null : thisReferrent.equals(otherReferrent);
			} else {
				return false;
			}
		}
	}

	/**
	 * A simple LRU cache that is implemented by overriding {@link LinkedHashMap#removeEldestEntry}.
	 */
	private class LRUCache extends LinkedHashMap<TransparentWeakReference, SoftReference<T>> {

		private static final long serialVersionUID = 1L;

		private LRUCache() {
			// Use low load factor to prevent collisions and an access order map to enforce LRU
			// properties.
			super(size, 0.5f, true);
		}

		@Override
		public void clear() {
			for (SoftReference<T> value : values()) {
				value.clear();
			}
			super.clear();
		}

		@Override
		protected boolean removeEldestEntry(Map.Entry<TransparentWeakReference, SoftReference<T>> eldest) {
			if (size() > size) {
				// clear eldest entry and remove it from the cache
				eldest.getValue().clear();
				return true;
			}
			return false;
		}
	}

	/**
	 * Creates a new heap utilization aware reference cache of the given size.
	 *
	 * @param size
	 *            the size of the new cache
	 */
	public ReferenceCache(final int size) {
		this.size = size;
		cache = new LRUCache();
	}

	public Reference newReference(T value) {
		return new Reference(value);
	}

	/**
	 * Checks the current heap utilization and clears the cache if necessary.
	 */
	private void checkHeapUtilization() {
		long heapMaxSize = Runtime.getRuntime().maxMemory();
		long heapSize = Runtime.getRuntime().totalMemory();
		long freeSize = Runtime.getRuntime().freeMemory();
		double loadFactor = (double) (heapSize - freeSize) / heapMaxSize;
		if (loadFactor > MAX_HEAP_LOAD_FACTOR) {
			cache.clear();
		}
	}

}
