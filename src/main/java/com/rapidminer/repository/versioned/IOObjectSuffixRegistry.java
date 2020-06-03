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


import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.lang.StringUtils;

import com.rapidminer.operator.IOObject;
import com.rapidminer.tools.DominatingClassFinder;


/**
 * Registry for suffixes used when storing {@link IOObject}s in a {@link NewFilesystemRepository}.
 *
 * @author Gisa Meier
 * @since 9.7.0
 */
public enum IOObjectSuffixRegistry {
    ; //no instance enum, only static methods

    /** Map of handlers, synchronized */
    private static final Map<Class<? extends IOObject>, String> SUFFIX_MAP = new ConcurrentHashMap<>();
    private static final Map<String, Class<? extends IOObject>> CLASS_MAP = new ConcurrentHashMap<>();

    /**
     * Registers an {@link IOObject} class and the file ending for storing it.
     *
     * @param clazz
     * 		the {@link IOObject} subclass
     * @param suffix
     * 		the file ending to use
     * @return {@code true} when the registering was successful
     */
    public static boolean register(Class<? extends IOObject> clazz, String suffix) {
        if (clazz == null || StringUtils.isBlank(suffix)) {
            return false;
        }
        String mySuffix = FilesystemRepositoryUtils.normalizeSuffix(suffix);

        if (SUFFIX_MAP.containsKey(clazz)) {
            return false;
        }
        SUFFIX_MAP.put(clazz, mySuffix);
        CLASS_MAP.putIfAbsent(mySuffix, clazz);

        return true;
    }

    /**
     * Retrieves the {@link IOObject} class of a provided suffix
     *
     * @param suffix
     * 		the suffix to look up
     * @return the IO object class, might simply return {@link IOObject IOObject.class}
     */
    public static Class<? extends IOObject> getIOObjectClass(String suffix) {
        return CLASS_MAP.getOrDefault(FilesystemRepositoryUtils.normalizeSuffix(suffix), IOObject.class);
    }

    /**
     * Returns the registered suffixes for {@link IOObject} file endings.
     *
     * @return the registered suffixes
     */
    static Collection<String> getRegisteredSuffixes() {
        return Collections.unmodifiableCollection(SUFFIX_MAP.values());
    }

    /**
     * Retrieves the suffix for an {@link IOObject}.
     *
     * @param ioo
     * 		the {@link IOObject}
     * @return the suffix used to store the {@link IOObject}
     */
    public static String getSuffix(IOObject ioo) {
        return getSuffix(ioo.getClass());
    }

    /**
     * Retrieves the suffix for an {@link IOObject}.
     *
     * @param ioObjectClass
     * 		the {@link IOObject} class
     * @return the suffix used to store the {@link IOObject}
     */
    public static String getSuffix(Class<? extends IOObject> ioObjectClass) {
        String suffix = SUFFIX_MAP.get(ioObjectClass);

        if (suffix == null) {

            // look for next registered dominating IOObject class
            ioObjectClass = new DominatingClassFinder<IOObject>().findNextDominatingClass(ioObjectClass,
                    SUFFIX_MAP.keySet());

            // registered dominating IOObject class found
            if (ioObjectClass != null) {
                suffix = SUFFIX_MAP.get(ioObjectClass);
            }
        }

        return suffix;
    }
}
