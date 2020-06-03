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

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Level;

import com.rapidminer.example.ExampleSet;
import com.rapidminer.operator.IOObject;
import com.rapidminer.operator.tools.IOObjectSerializer;
import com.rapidminer.operator.tools.SerializationType;
import com.rapidminer.tools.LogService;
import com.rapidminer.tools.ValidationUtil;
import com.rapidminer.tools.io.ClassFromSerializationReader;


/**
 * Utility class to find the IOObject associated with a given {@link Path}, {@link InputStream} or {@link BasicEntry}.
 * For paths and input streams, assumes that the object was written/can be read by {@link IOObjectSerializer} and for
 * repository entries, checks the {@link IOObjectSuffixRegistry} before going to that conclusion.
 *
 * @author Jan Czogalla
 * @since 9.7
 */
public enum IOObjectClassDetector {
	;

	/** Same as default buffer size of {@link java.io.BufferedInputStream BufferedInputStream} */
	private static final int RESET_LIMIT = 8192;

	/**
	 * Find the {@link IOObject} class from a given {@link Path}. Will return {@code null} if any problems occur.
	 *
	 * @param path
	 * 		the path of the serialized IO object; should not be {@code null}
	 * @return the class of the serialized object; might be {@code null}
	 */
	public static Class<? extends IOObject> findClass(Path path) {
		ValidationUtil.requireNonNull(path, "path");
		try (InputStream stream = Files.newInputStream(path)) {
			return findClass(stream);
		} catch (IOException e) {
			LogService.getRoot().log(Level.WARNING, "Could not read from path " + path, e);
			return null;
		}
	}

	/**
	 * Find the {@link IOObject} class from a given {@link InputStream}, using the {@link IOObjectSerializer}.
	 * Does check {@link InputStream#markSupported()}, and if {@code true}, calls {@link #findClassWithReset(InputStream)}.
	 * Will return {@code null} if any problems occur.
	 *
	 * @param in
	 * 		the input stream of the serialized IO object; should not be {@code null}
	 * @return the class of the serialized object; might be {@code null}
	 * @see IOObjectSerializer#deserializeHeader(InputStream)
	 */
	public static Class<? extends IOObject> findClass(InputStream in) {
		ValidationUtil.requireNonNull(in, "input stream");
		if (in.markSupported()) {
			return findClassWithReset(in);
		}
		return findClassInternal(in);
	}

	/**
	 * Same as {@link #findClass(InputStream)}, but resets the input stream after reading if possible.
	 *
	 * @see InputStream#mark(int)
	 * @see InputStream#reset()
	 */
	public static Class<? extends IOObject> findClassWithReset(InputStream in) {
		ValidationUtil.requireNonNull(in, "input stream");
		if (!in.markSupported()) {
			return findClass(in);
		}
		in.mark(RESET_LIMIT);
		Class<? extends IOObject> iooClass = findClassInternal(in);
		try {
			in.reset();
		} catch (IOException e) {
			LogService.getRoot().log(Level.WARNING, "Could not reset input stream", e);
			// ignore
		}
		return iooClass;
	}

	/**
	 * Find the {@link IOObject} class from a given {@link BasicEntry}, using the {@link IOObjectSuffixRegistry}
	 * and {@link #findClass(Path)}. If the suffix registry does not hold a class for the suffix or the class is
	 * {@link IOObject}, a look at the serialized data is necessary.
	 *
	 * @param repoFile the entry to check against; should not be null
	 * @return the class of the serialized object; might be {@code null}
	 * @see IOObjectSuffixRegistry#getIOObjectClass(String)
	 */
	public static Class<? extends IOObject> findClass(BasicEntry<?> repoFile) {
		ValidationUtil.requireNonNull(repoFile, "repository file");
		Class<? extends IOObject> iooClass = IOObjectSuffixRegistry.getIOObjectClass(repoFile.getSuffix());
		if (iooClass != null && iooClass != IOObject.class) {
			return iooClass;
		}
		return findClass(repoFile.getRepositoryAdapter().getRealPath(repoFile));
	}

	/**
	 * Find the {@link IOObject} class from a given {@link InputStream}, using the {@link IOObjectSerializer}.
	 * Does not check if the {@link InputStream#markSupported()}.
	 */
	private static Class<? extends IOObject> findClassInternal(InputStream in) {
		try {
			SerializationType serType = IOObjectSerializer.getInstance().deserializeHeader(in);
			switch (serType) {
				case STREAMED_EXAMPLE_SET_DENSE:
				case STREAMED_EXAMPLE_SET_DENSE_2:
				case STREAMED_EXAMPLE_SET_DENSE_3:
					return ExampleSet.class;
				case JAVA_BINARY:
					return ClassFromSerializationReader.readClass(in, IOObject.class);
				default:
					return null;
			}
		} catch (IOException e) {
			LogService.getRoot().log(Level.WARNING, "Could not read from input stream", e);
			return null;
		}
	}
}
