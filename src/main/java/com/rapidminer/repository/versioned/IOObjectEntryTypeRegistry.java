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
package com.rapidminer.repository.versioned;


import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.rapidminer.operator.IOObject;
import com.rapidminer.repository.IOObjectEntry;
import com.rapidminer.tools.DominatingClassFinder;
import com.rapidminer.tools.ValidationUtil;


/**
 * Registry for {@link com.rapidminer.repository.IOObjectEntry} types used when storing {@link IOObject}s in a {@link NewFilesystemRepository}.
 *
 * @author Marco Boeck
 * @since 9.7
 */
public enum IOObjectEntryTypeRegistry {
    ; //no instance enum, only static methods


    /** Map of handlers, synchronized */
    private static final Map<Class<? extends IOObject>, Class<? extends IOObjectEntry>> ENTRY_MAP = new ConcurrentHashMap<>();
    private static final Map<Class<? extends IOObjectEntry>, Class<? extends IOObject>> CLASS_MAP = new ConcurrentHashMap<>();

    /**
     * Registers an {@link IOObject} class and the specific {@link IOObjectEntry} subtype used when storing it.
     *
     * @param clazz      the {@link IOObject} subclass, must not be {@code null}
     * @param entryClass the entry subclass, must not be {@code null}
     * @return {@code true} when the registering was successful
     */
    public static boolean register(Class<? extends IOObject> clazz, Class<? extends IOObjectEntry> entryClass) {
        ValidationUtil.requireNonNull(clazz, "clazz");
        ValidationUtil.requireNonNull(entryClass, "entryClass");

        if (ENTRY_MAP.putIfAbsent(clazz, entryClass) == null) {
            CLASS_MAP.put(entryClass, clazz);
            return true;
        }

        return false;
    }

    /**
     * Retrieves the {@link IOObjectEntry} (sub-)class of a {@link IOObject} class.
     *
     * @param ioClass the class for which to look up the corresponding entry subclass, must not be {@code null}
     * @return the {@link IOObjectEntry} (sub-)class if one is registered, otherwise returns the one for the next
     * dominating class, ultimately ending up at {@link IOObjectEntry}, never {@code null}
     */
    public static Class<? extends IOObjectEntry> getEntryClassForIOObjectClass(Class<? extends IOObject> ioClass) {
        Class<? extends IOObjectEntry> clazz = ENTRY_MAP.get(ValidationUtil.requireNonNull(ioClass, "ioClass"));

        if (clazz == null) {
            // look for next registered dominating IOObject class
            ioClass = new DominatingClassFinder<IOObject>().findNextDominatingClass(ioClass, ENTRY_MAP.keySet());

            // registered dominating IOObject class found
            if (ioClass != null) {
                clazz = ENTRY_MAP.get(ioClass);
            } else {
                clazz = IOObjectEntry.class;
            }
        }

        return clazz;
    }

    /**
     * Retrieves the {@link IOObject} class of an {@link IOObjectEntry} class.
     *
     * @param entryClass the entry class for which to look up the corresponding IOObject class, must not be {@code
     *                   null}
     * @return the {@link IOObjectEntry} (sub-)class if one is registered, otherwise returns the one for the next
     * dominating class, ultimately ending up at {@link IOObject}, never {@code null}
     */
    public static Class<? extends IOObject> getIOObjectClassForEntryClass(Class<? extends IOObjectEntry> entryClass) {
        Class<? extends IOObject> clazz = CLASS_MAP.get(ValidationUtil.requireNonNull(entryClass, "entryClass"));

        if (clazz == null) {
            // look for next registered dominating IOObject class
            entryClass = new DominatingClassFinder<IOObjectEntry>().findNextDominatingClass(entryClass, CLASS_MAP.keySet());

            // registered dominating IOObject class found
            if (entryClass != null) {
                clazz = CLASS_MAP.get(entryClass);
            } else {
                clazz = IOObject.class;
            }
        }

        return clazz;
    }
}
