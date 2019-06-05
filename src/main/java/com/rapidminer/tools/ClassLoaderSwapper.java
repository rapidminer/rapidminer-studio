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

/**
 * Helper class to swap the Classloader and then put it back at the end of the try regardless of
 * errors.
 *
 * Using it to clean up code...from:
 *
 * if (classLoader != null) { - * currentContextClassLoader =
 * Thread.currentThread().getContextClassLoader(); - *
 * Thread.currentThread().setContextClassLoader(classLoader); - * } try { .... }finally { if
 * (currentContextClassLoader != null) { - *
 * Thread.currentThread().setContextClassLoader(currentContextClassLoader); - * } }
 *
 * to try (ClassLoadSwapper sw = ClassLoadSwapper.swapClassLoaderTo(classLoader)) { ... }
 *
 * @author John Pendzick
 * @since 9.3, copied from Radoop extension
 */
public final class ClassLoaderSwapper implements AutoCloseable {

	private final ClassLoader currentCl;
	private final ClassLoader newClassLoader;

	public static ClassLoaderSwapper withContextClassLoader(ClassLoader newClassLoader) {
		return new ClassLoaderSwapper(newClassLoader);
	}

	private ClassLoaderSwapper(ClassLoader newClassLoader) {

		if (newClassLoader == null) {
			this.currentCl = null;
			this.newClassLoader = null;
		} else {
			ClassLoader contextCL = Thread.currentThread().getContextClassLoader();
			// already this classloader? nothing to do
			if (contextCL == newClassLoader) {
				this.currentCl = this.newClassLoader = null;
				return;
			}
			this.currentCl = contextCL;
			this.newClassLoader = newClassLoader;
			Thread.currentThread().setContextClassLoader(this.newClassLoader);
		}
	}

	/** Get the class loader that was put as the new context class loader. */
	public ClassLoader getNewClassLoader() {
		return this.newClassLoader;
	}

	@Override
	public void close() {
		if (this.currentCl != null) {
			Thread.currentThread().setContextClassLoader(this.currentCl);
		}
	}

}
