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

import java.util.function.Consumer;
import java.util.function.Function;


/**
 * Represents an operation that accepts a single input argument and returns no
 * result but could throw a specific type of {@link Throwable}.
 * Unlike most other functional interfaces, {@code ConsumerWithThrowable} is expected
 * to operate via side-effects.
 *
 * @param <T> the type of the input to the operation
 * @param <E> Exception that is thrown by the consumer
 *
 * @author Jonas Willms-Pfau, Jan Czogalla
 * @since 9.2.0
 */
@SuppressWarnings("squid:S1181")
public interface ConsumerWithThrowable<T, E extends Throwable> extends Consumer<T>  {

	/**
	 * Performs this operation on the given argument.
	 *
	 * @param t the input argument
	 * @throws E if something goes wrong
	 */
	void acceptWithException(T t) throws E;

	/**
	 * {@inheritDoc}
	 * Ignores any thrown exceptions.
	 */
	@Override
	default void accept(T t) {
		try {
			acceptWithException(t);
		} catch (Throwable e) {
			// ignore
		}
	}

	/**
	 * Performs this operation on the given argument.
	 * Wraps any non-{@link RuntimeException} in a runtime exception
	 */
	@SuppressWarnings("squid:S00112")
	default void acceptOrThrow(T t) {
		try {
			acceptWithException(t);
		} catch (RuntimeException e) {
			throw e;
		} catch (Throwable e) {
			throw new RuntimeException(e);
		}
	}

	/** Shortcut to wrap a {@link ConsumerWithThrowable} to a {@link Consumer} using {@link #accept(Object)} */
	static <T> Consumer<T> suppress(ConsumerWithThrowable<T, ?> sc) {
		return sc;
	}

	/** Shortcut to wrap a {@link ConsumerWithThrowable} to a {@link Consumer} using the given exception handler */
	static <T> Consumer<T> suppress(ConsumerWithThrowable<T, ?> sc, Consumer<Throwable> handler) {
		return t -> {
			try {
				sc.acceptWithException(t);
			} catch (Throwable e) {
				handler.accept(e);
			}
		};
	}

	/** Shortcut to wrap a {@link ConsumerWithThrowable} to a {@link Consumer} using {@link #acceptOrThrow(Object)} */
	static <T> Consumer<T> wrap(ConsumerWithThrowable<T, ?> sc) {
		return sc::acceptOrThrow;
	}

	/** Shortcut to wrap a {@link ConsumerWithThrowable} to another one using the given exception wrapper */
	static <T, E extends Throwable> ConsumerWithThrowable<T, E> wrap(ConsumerWithThrowable<T, ?> sc, Function<Throwable, E> wrapper) {
		return t -> {
			try {
				sc.acceptWithException(t);
			} catch (Throwable e) {
				throw wrapper.apply(e);
			}
		};
	}

	/**
	 *  Shortcut to wrap a {@link ConsumerWithThrowable} to a {@link Function} that returns a thrown Exception
	 *  using {@link #acceptWithException(Object)}
	 */
	@SuppressWarnings({"unchecked", "squid:S00112"})
	static <T, E extends Throwable> Function<T, Throwable> wrapAndReturn(ConsumerWithThrowable<T, E> sc) {
		return t -> {
			try {
				sc.acceptWithException(t);
				return null;
			} catch (Throwable e) {
				return e;
			}
		};
	}

	/**
	 * Shortcut to wrap a {@link ConsumerWithThrowable} to a {@link Function} that returns a thrown expected Exception
	 * using {@link #acceptWithException(Object)} and throws a {@link RuntimeException} in case of an unexpected exception
	 *
	 * @param sc
	 * 		the throwing consumer to wrap
	 * @param eClass
	 * 		the expected exception class
	 */
	@SuppressWarnings({"unchecked", "squid:S00112"})
	static <T, E extends Throwable> Function<T, E> wrapAndReturn(ConsumerWithThrowable<T, E> sc, Class<E> eClass) {
		return t -> {
			try {
				sc.acceptWithException(t);
				return null;
			} catch (Throwable e) {
				if (eClass.isInstance(e)) {
					return (E) e;
				}
				throw new RuntimeException(e);
			}
		};
	}
}
