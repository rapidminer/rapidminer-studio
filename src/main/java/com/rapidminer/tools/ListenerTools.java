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

import static com.rapidminer.tools.ConsumerWithThrowable.wrapAndReturn;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.stream.Collectors;

import com.rapidminer.operator.OperatorException;


/**
 * This utility class offers methods to successfully inform a list of listeners before throwing a collective exception
 * if any listener did throw an exception. This is useful for listeners that may have a throws declaration
 * but should not interfere with the following listeners.
 *
 * @since 9.2.0
 * @author Jonas Wilms-Pfau, Jan Czogalla
 */
public final class ListenerTools {

	private static final Consumer<Throwable> THROWABLE_LOGGER = t -> LogService.getRoot().log(Level.WARNING, "com.rapidminer.tools.ListenerTools.uncaught_exception", t);

	/**
	 * Prevent utility class instantiation
	 */
	private ListenerTools() {
		throw new AssertionError("utility class");
	}

	/**
	 * Informs all listeners, suppresses unexpected Exceptions and throws the first expected Exception after all
	 * listeners are informed. Unexpected Exceptions such as Runtime Exceptions are logged and returned.
	 *
	 * @param listeners
	 * 		the listeners to inform
	 * @param method
	 * 		the method that should be called on the listener
	 * @param eClass
	 * 		the expected exception class
	 * @param <L>
	 * 		the listener type
	 * @param <E>
	 * 		the exception that is expected to occur
	 * @return a list of non-{@link E} exceptions that have been thrown by the listeners
	 * @throws E
	 * 		if a listener throws an exception of type E
	 */
	@SuppressWarnings("unchecked")
	public static <L, E extends Throwable> List<Throwable> informAllAndThrow(Collection<L> listeners, ConsumerWithThrowable<L, E> method, Class<E> eClass) throws E {
		Map<Boolean, List<Throwable>> errorMap = listeners.stream().map(wrapAndReturn(method))
				.filter(Objects::nonNull).collect(Collectors.partitioningBy(eClass::isInstance));
		List<E> exceptions = (List<E>) errorMap.get(true);
		List<Throwable> throwables = errorMap.get(false);
		throwables.forEach(THROWABLE_LOGGER);
		if (exceptions.isEmpty()) {
			return throwables;
		}
		E first = exceptions.remove(0);
		throw exceptions.stream().reduce(first, (t1, t2) -> {
			t1.addSuppressed(t2);
			return t1;
		});
	}

	/**
	 * Informs all listeners, suppresses unexpected {@link Exception Exceptions} and throws the first {@link OperatorException}
	 * after all listeners are informed. Unexpected Exceptions such as {@link RuntimeException RuntimeExceptions} are logged and returned.
	 *
	 * @param listeners
	 * 		the listeners to inform
	 * @param method
	 * 		the method that should be called on the listener
	 * @param <L>
	 * 		the listener type
	 * @return a list of non-{@link OperatorException OperatorExceptions} exceptions that have been thrown by the listeners
	 */
	@SuppressWarnings("unchecked")
	public static <L> List<Throwable> informAllAndThrow(Collection<L> listeners, ConsumerWithThrowable<L, OperatorException> method) throws OperatorException {
		return informAllAndThrow(listeners, method, OperatorException.class);
	}

	/**
	 * Informs all listeners, suppresses unexpected {@link Exception Exceptions} and throws the first {@link RuntimeException} after all
	 * listeners are informed.
	 *
	 * @param listeners
	 * 		the listeners to inform
	 * @param method
	 * 		the method that should be called on the listener
	 * @param <L>
	 * 		the listener type
	 * @return a list of non-{@link RuntimeException} exceptions that have been thrown by the listeners
	 */
	public static <L> List<Throwable> informAllAndThrow(Collection<L> listeners, Consumer<L> method) {
		return informAllAndThrow(listeners, method::accept, RuntimeException.class);
	}

	/**
	 * Calls the initial consumer (usually a call to super), then informs all listeners,
	 * suppresses unexpected {@link Exception Exceptions} and throws the first {@link OperatorException}
	 * after all listeners are informed.
	 * Unexpected Exceptions such as {@link RuntimeException RuntimeExceptions} are logged and returned.
	 *
	 * @param initial
	 * 		first call that might throw an exception
	 * @param listeners
	 * 		the listeners to inform
	 * @param method
	 * 		the method that should be called on the listener
	 * @param <L>
	 * 		the listener type
	 * @return a list of non-{@link OperatorException OperatorExceptions} exceptions that have been thrown by the listeners
	 */
	@SuppressWarnings({"squid:S1181", "unchecked"})
	public static <L> List<Throwable> informAllAndThrow(ConsumerWithThrowable<Void, OperatorException> initial, Collection<L> listeners,
														ConsumerWithThrowable<L, OperatorException> method) throws OperatorException {
		List<Object> newListeners = new ArrayList<>(listeners.size() + 1);
		newListeners.add(initial);
		listeners.forEach(newListeners::add);
		ConsumerWithThrowable<Object, OperatorException> newMethod = o -> {
			if (o == initial) {
				initial.acceptWithException(null);
			} else {
				method.acceptWithException((L) o);
			}
		};
		return informAllAndThrow(newListeners, newMethod);
	}
}
