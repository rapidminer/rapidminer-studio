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
package com.rapidminer.tools.io;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectStreamClass;
import java.util.logging.Level;

import com.rapidminer.tools.LogService;
import com.rapidminer.tools.ValidationUtil;
import com.rapidminer.tools.plugin.Plugin;

/**
 * An {@link ObjectInputStream} that only reads the class of the first {@link ObjectStreamClass class descriptor}.
 * This can be used to figure out what class is "hiding" behind an {@link InputStream} without reading the full object.
 * See {@link #readClass(InputStream, Class)} for more details.
 *
 * @author Jan Czogalla
 * @since 9.7
 */
public final class ClassFromSerializationReader extends ObjectInputStream {

	/**
	 * Helper exception to hold the class descriptors class
	 *
	 * @author Jan Czogalla
	 * @since 9.7
	 */
	private static final class ClassFoundException extends IOException {

		private final Class<?> found;

		private ClassFoundException(Class<?> found, String className) {
			if (found == null) {
				try {
					found = Class.forName(className, true, Plugin.getMajorClassLoader());
				} catch (ClassNotFoundException e) {
					// ignore
				}
			}
			this.found = found;
		}

	}

	private ClassFromSerializationReader(InputStream in) throws IOException {
		super(in);
	}

	/**
	 * Read the first class descriptor which indicates the serialized class, then throws a {@link ClassFoundException}
	 * containing that class to avoid further reading
	 */
	@Override
	protected ObjectStreamClass readClassDescriptor() throws IOException, ClassNotFoundException {
		ObjectStreamClass descriptor = super.readClassDescriptor();
		throw new ClassFoundException(descriptor.forClass(), descriptor.getName());
	}

	/**
	 * Reads the class from the input stream that was serialized. Only reads the class, keeping the I/O to a mimimum.
	 * <p>
	 * <strong>Note:</strong> Use a {@link java.io.BufferedInputStream BufferedInputStream} with
	 * {@link java.io.BufferedInputStream#mark(int) mark(int)} and {@link java.io.BufferedInputStream#reset() reset()}
	 * if you want to reuse the given input stream.
	 *
	 * @param in
	 * 		the input stream to read from
	 * @param superClass
	 * 		the expected class or one of its super classes to cap what should be returned
	 * @param <T>
	 * 		the expected return type
	 * @return the class that the input stream represents or {@code null}, if nothing was serialized or the read
	 * class does not match the expected super class.
	 */
	@SuppressWarnings("unchecked")
	public static <T> Class<? extends T> readClass(InputStream in, Class<T> superClass) {
		ValidationUtil.requireNonNull(in, "input stream");
		ValidationUtil.requireNonNull(superClass, "super class");
		try {
			new ClassFromSerializationReader(in).readObject();
		} catch (ClassFoundException e) {
			Class<?> found = e.found;
			if (found != null) {
				if (superClass.isAssignableFrom(found)) {
					return (Class<? extends T>) found;
				} else {
					LogService.log(LogService.getRoot(), Level.WARNING, null,
							"com.rapidminer.tools.io.ClassFromSerializationReader.found_wrong_class",
							superClass.getName(), found.getName());
				}
			}
		} catch (Throwable e) {
			LogService.log(LogService.getRoot(), Level.WARNING, e,
					"com.rapidminer.tools.io.ClassFromSerializationReader.error_deserialize",
					superClass.getName());
			// ignore
		}
		return null;
	}
}
