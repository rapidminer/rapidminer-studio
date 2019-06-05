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
 * A wrapper sub-interface of {@link Function} that can handle method references that can also throw an exception.
 * By default, the {@link #apply(Object)} method will suppress the exception and return {@code null} on error
 * (same as {@link #applyOrNull(Object)}). The opposite is done with {@link #applyOrThrow(Object)}, which will wrap any
 * {@link Throwable} in a {@link RuntimeException} if it is not already one and throws that exception.
 *
 * @param <E> the type of the exception thrown by the function
 *
 * @since 9.3
 * @author Jan Czogalla
 */
public interface FunctionWithThrowable<T, R, E extends Throwable> extends Function<T, R> {

	/** The basic method; Adds a throws declaration to the method signature. */
	R applyWithException(T t) throws E;

	/** Same as {@link #applyOrNull(Object)} */
	@Override

	default R apply(T t) {
		return applyOrNull(t);
	}

	/** Applies the function and may return {@code null} on an error */
	@SuppressWarnings("squid:S1181")
	default R applyOrNull(T t) {
		try {
			return applyWithException(t);
		} catch (Throwable e) {
			return null;
		}
	}

	/** Applies the function and throws a {@link RuntimeException} if any error occurs. */
	@SuppressWarnings({"squid:S1181", "squid:S00112"})
	default R applyOrThrow(T t) {
		try {
			return applyWithException(t);
		} catch (RuntimeException e) {
			throw e;
		} catch (Throwable e) {
			throw new RuntimeException(e);
		}
	}

	/** Shortcut to wrap a {@link FunctionWithThrowable} to a {@link Function} using {@link #applyOrNull(Object)} */
	static <T, R> Function<T, R> suppress(FunctionWithThrowable<T, R, ?> sf) {
		return sf;
	}

	/** Shortcut to wrap a {@link FunctionWithThrowable} to a {@link Function} using the given exception handler */
	@SuppressWarnings("squid:S1181")
	static <T, R> Function<T, R> suppress(FunctionWithThrowable<T, R, ?> sf, Consumer<Throwable> handler) {
		return t -> {
			try {
				return sf.applyWithException(t);
			} catch (Throwable e) {
				handler.accept(e);
				return null;
			}
		};
	}

	/** Shortcut to wrap a {@link FunctionWithThrowable} to a {@link Function} using {@link #applyOrThrow(Object)} */
	static <T, R> Function<T, R> wrap(FunctionWithThrowable<T, R, ?> sf) {
		return sf::applyOrThrow;
	}

	/** Shortcut to wrap a {@link FunctionWithThrowable} to another one using the given exception wrapper */
	@SuppressWarnings("squid:S1181")
	static <T, R, E extends Throwable> FunctionWithThrowable<T, R, E> wrap(FunctionWithThrowable<T, R, ?> sf, Function<Throwable, E> wrapper) {
		return t -> {
			try {
				return sf.applyWithException(t);
			} catch (Throwable e) {
				throw wrapper.apply(e);
			}
		};
	}
}
