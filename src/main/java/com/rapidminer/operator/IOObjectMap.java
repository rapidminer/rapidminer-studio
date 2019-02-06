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
package com.rapidminer.operator;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.rapidminer.operator.IOObjectMapEvent.IOObjectMapEventType;
import com.rapidminer.tools.AbstractObservable;
import com.rapidminer.tools.Observable;
import com.rapidminer.tools.Observer;


/**
 * This map of {@link IOObject}s is used to make {@link IOObject}s accessible for a given scope.
 *
 * @author Sabrina Kirstein, Marco Boeck
 *
 */
public class IOObjectMap {

	/**
	 * The IOObject Map owns an {@link Observable}, which informs the observers about map changes.
	 */
	private class PrivateMapObservable extends AbstractObservable<IOObjectMapEvent> {

		@Override
		protected void fireUpdate(IOObjectMapEvent event) {
			super.fireUpdate(event);
		}
	}

	/**
	 * Stores IOObjects according to a specified name.
	 */
	private Map<String, IOObject> cacheMap = new HashMap<>();

	/** observable which informs observers about the map state */
	private PrivateMapObservable mapObservable = new PrivateMapObservable();

	private Object LOCK = new Object();

	/**
	 * Adds an observer, which receives updates when the map changes, i.e. an object is
	 * added/removed/changed or multiple objects are added/removed at the same time.
	 *
	 * @param observer
	 */
	public void addMapObserver(Observer<IOObjectMapEvent> observer) {
		mapObservable.addObserver(observer, false);
	}

	/**
	 * Removes the observer. The observer receives no longer updates if the map changes.
	 *
	 * @param observer
	 */
	public void removeMapObserver(Observer<IOObjectMapEvent> observer) {
		mapObservable.removeObserver(observer);
	}

	/** Cache a given {@link IOObject} with an associated name. */
	public void store(String name, IOObject object) {
		IOObject previous = null;
		synchronized (LOCK) {
			previous = cacheMap.put(name, object);

		}

		if (previous == null) {
			mapObservable.fireUpdate(new IOObjectMapEvent(IOObjectMapEventType.ADDED, name));
		} else {
			mapObservable.fireUpdate(new IOObjectMapEvent(IOObjectMapEventType.CHANGED, name));
		}
	}

	/**
	 * Return an {@link IOObject} with the given name or <code>null</code> is none exists.
	 */
	public IOObject get(String name) {
		return cacheMap.get(name);
	}

	/**
	 * Returns all cached {@link IOObject}s
	 *
	 * @return
	 */
	public Map<String, IOObject> getAll() {
		synchronized (LOCK) {
			return Collections.unmodifiableMap(cacheMap);
		}
	}

	/**
	 * Returns the keys of all cached {@link IOObject}s
	 *
	 * @return
	 */
	public Set<String> getAllKeys() {
		synchronized (LOCK) {
			return Collections.unmodifiableSet(cacheMap.keySet());
		}
	}

	/**
	 * Removes an {@link IOObject} with the given name
	 *
	 * @return the removed {@link IOObject} or <code>null</code> if it does not exist
	 */
	public IOObject remove(String name) {
		IOObject removedObject;
		synchronized (LOCK) {
			removedObject = cacheMap.remove(name);
		}
		mapObservable.fireUpdate(new IOObjectMapEvent(IOObjectMapEventType.REMOVED, name));
		return removedObject;
	}

	/** Clears all stored {@link IOObject}s. */
	public void clearStorage() {
		synchronized (LOCK) {
			cacheMap.clear();
		}
		mapObservable.fireUpdate(new IOObjectMapEvent(IOObjectMapEventType.STRUCTURE_CHANGED, null));
	}
}
