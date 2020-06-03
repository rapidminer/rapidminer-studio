/**
 * Copyright (C) 2001-2020 by RapidMiner and the contributors
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
package com.rapidminer.repository;

import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.apache.commons.lang.StringUtils;


/**
 * Abstract registry for custom behavior when something happens with file suffixes. All methods using file suffixes are
 * case-insensitive.
 * <p>
 * Note that only one action can be registered per file suffix. If there is already an action registered, a new
 * registration will silently fail (register method will return {@code false}).
 * </p>
 * <p> Suffix is defined as the content after the last '.' in a file name.</p>
 *
 * @author Marco Boeck
 * @since 9.7
 */
public abstract class AbstractFileSuffixRegistry<T> {

	private final Map<String, T> callbackMap = Collections.synchronizedMap(new HashMap<>());


	/**
	 * Registers the given callback.
	 *
	 * @param suffix   the suffix, should not start with a leading '.', must not be {@code null}
	 * @param callback the callback, must not be {@code null}
	 * @return {@code true} if registration was successful; {@code false} if there was already a callback for that
	 * suffix
	 */
	public boolean registerCallback(String suffix, T callback) {
		if (callback == null) {
			throw new IllegalArgumentException("callback must not be null!");
		}

		return callbackMap.putIfAbsent(prepareSuffix(suffix), callback) == null;
	}

	/**
	 * Tries to unregister the given callback for the given suffix. If the callback passed here is not the same as the
	 * one stored in the registry for the provided suffix, it will <strong>not</strong> be unregistered! Calling this
	 * multiple times has no effect. If the callback is not registered has also no effect.
	 *
	 * @param suffix   the suffix, should not start with a leading '.', must not be {@code null}
	 * @param callback the callback, must not be {@code null}
	 */
	public void unregisterCallback(String suffix, T callback) {
		if (callback == null) {
			throw new IllegalArgumentException("callback must not be null!");
		}

		suffix = prepareSuffix(suffix);
		synchronized (callbackMap) {
			T existingCallback = callbackMap.get(suffix);
			if (existingCallback == callback) {
				callbackMap.remove(suffix);
			}
		}
	}

	/**
	 * Returns the callback for the given file suffix. This method is case-insensitive.
	 *
	 * @param suffix the suffix, should not start with a leading '.', must not be {@code null}
	 * @return the callback if one is registered, or {@code null}
	 */
	public T getCallback(String suffix) {
		return callbackMap.get(prepareSuffix(suffix));
	}

	/**
	 * Prepares the suffix. Will cut off a leading '.' and will also strip it to an empty string and to lower case.
	 *
	 * @param suffix the suffix, must not be {@code null}
	 * @return the prepared suffix, never {@code null}
	 */
	protected String prepareSuffix(String suffix) {
		if (suffix == null) {
			throw new IllegalArgumentException("suffix must not be null!");
		}

		if (suffix.startsWith(".")) {
			suffix = suffix.substring(1);
		}
		return StringUtils.stripToEmpty(suffix.toLowerCase(Locale.ENGLISH));
	}
}
