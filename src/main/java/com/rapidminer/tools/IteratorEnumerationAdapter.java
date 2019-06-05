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

import java.util.Enumeration;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * A combination wrapper of {@link Iterator} and {@link Enumeration}.
 *
 * @author Jan Czogalla
 * @since 9.3
 */
@SuppressWarnings({"squid:S1150", "squid:S2974", "squid:S1610"})
public abstract class IteratorEnumerationAdapter<E> implements Iterator<E>, Enumeration<E> {

	/** Prevent instantiation outside this class */
	private IteratorEnumerationAdapter(){}

	public static<E> IteratorEnumerationAdapter<E> from(Enumeration<E> enumeration) {
		return new EnumerationIterator<>(enumeration);
	}

	public static<E> IteratorEnumerationAdapter<E> from(Iterator<E> iterator) {
		return new IteratorEnumeration<>(iterator);
	}

	/**
	 * A combination wrapper of {@link Iterator} and {@link Enumeration} for enumerations.
	 *
	 * @author Jan Czogalla
	 * @since 9.3
	 */
	private static final class EnumerationIterator<E> extends IteratorEnumerationAdapter<E> {

		private final Enumeration<E> enumeration;

		private EnumerationIterator(Enumeration<E> enumeration) {
			this.enumeration = enumeration;
		}

		@Override
		public boolean hasNext() {
			return hasMoreElements();
		}

		@Override
		public E next() {
			if (!hasMoreElements()) {
				throw new NoSuchElementException();
			}
			return nextElement();
		}

		@Override
		public void remove() {
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean hasMoreElements() {
			return enumeration.hasMoreElements();
		}

		@Override
		public E nextElement() {
			return enumeration.nextElement();
		}
	}

	/**
	 * A combination wrapper of {@link Iterator} and {@link Enumeration} for iterators.
	 *
	 * @author Jan Czogalla
	 * @since 9.3
	 */
	private static final class IteratorEnumeration<E> extends IteratorEnumerationAdapter<E> {

		private final Iterator<E> iter;

		private IteratorEnumeration(Iterator<E> iter) {
			this.iter = iter;
		}

		@Override
		public boolean hasNext() {
			return iter.hasNext();
		}

		@Override
		public E next() {
			return iter.next();
		}

		@Override
		public void remove() {
			iter.remove();
		}

		@Override
		public boolean hasMoreElements() {
			return hasNext();
		}

		@Override
		public E nextElement() {
			return next();
		}
	}
}
