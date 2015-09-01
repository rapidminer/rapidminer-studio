/**
 * Copyright (C) 2001-2015 by RapidMiner and the contributors
 *
 * Complete list of developers available at our web site:
 *
 *      http://rapidminer.com
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses/.
 */
package com.rapidminer.tools;

import java.io.IOException;
import java.net.URL;
import java.util.AbstractMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Level;


/**
 * @author Tobias Malbrecht
 */
public abstract class ParentResolvingMap<K, V> extends AbstractMap<K, V> implements Map<K, V> {

	private final Map<K, V> delegate = new HashMap<K, V>();

	public ParentResolvingMap() {}

	public void parseProperties(String resourceName, String prefix, String suffix, ClassLoader classLoader)
			throws IOException {
		Properties groupProps = new Properties();
		URL resource = classLoader.getResource(resourceName);
		if (resource == null) {
			// LogService.getRoot().warning("Group properties resource '"+resourceName +
			// "' not found.");
			LogService.getRoot().log(Level.WARNING,
					"com.rapidminer.tools.ParentResolvingMap.group_properties_resource_not_found", resourceName);
		} else {
			groupProps.load(resource.openStream());
			for (String propKey : groupProps.stringPropertyNames()) {
				if (propKey.startsWith(prefix) && (propKey.endsWith(suffix))) {
					String keyString = propKey.substring(prefix.length());
					keyString = keyString.substring(0, keyString.length() - suffix.length());
					K mapKey = parseKey(keyString, classLoader);
					V value = parseValue(groupProps.getProperty(propKey), classLoader);
					delegate.put(mapKey, value);
				}
			}
		}
	}

	protected abstract V parseValue(String value, ClassLoader classLoader);

	protected abstract K parseKey(String key, ClassLoader classLoader);

	protected abstract K getParent(K child);

	protected abstract V getDefault();

	@Override
	@SuppressWarnings("unchecked")
	public V get(Object key) {
		while (key != null) {
			V value = delegate.get(key);
			if (value != null) {
				return value;
			} else {
				key = getParent((K) key);
			}
		}
		return getDefault();
	}

	@Override
	public V put(K key, V value) {
		return delegate.put(key, value);
	}

	@Override
	public Set<Map.Entry<K, V>> entrySet() {
		return delegate.entrySet();
	}
}
