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
package com.rapidminer.connection.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import javax.swing.event.EventListenerList;

import com.rapidminer.connection.util.RegistrationEvent.RegistrationEventType;
import com.rapidminer.tools.I18N;
import com.rapidminer.tools.ValidationUtil;


/**
 * Generic handler registry for {@link GenericHandler} subinterfaces. Handlers can be registered and unregistered,
 * searched by type and (un)registrations can be observed.
 *
 * @param <H>
 * 		the handler subclass/-interface
 * @author Jan Czogalla
 * @since 9.3
 */
public abstract class GenericHandlerRegistry<H extends GenericHandler> {

	/**
	 * {@link IllegalArgumentException} to indicate a missing handler for a given type.
	 *
	 * @author Jan Czogalla
	 * @since 9.3
	 */
	public static class MissingHandlerException extends IllegalArgumentException {

		private static final String ERROR_PREFIX = "generic_registry.missing_handler.";
		private static final String ERROR_CORE = "core";
		private static final String ERROR_EXTENSION = "extension";
		private static final String TYPE_PREFIX = "generic_registry.handler.type.";

		/** @see #createMessage(String, String) */
		private MissingHandlerException(String type, String handlerType) {
			super(createMessage(type, handlerType));
		}

		/**
		 * Creates a new i18n message for a missing handler.
		 * Resolves error message for
		 * <p>
		 * {@value #ERROR_PREFIX}{@code [}{@value #ERROR_CORE}{@code |}{@value #ERROR_EXTENSION}{@code ]}
		 * <p>
		 * depending on whether the given type indicates its origin and uses the registryType to resolve its i18n name by looking up
		 * <p>
		 * {@value #TYPE_PREFIX}{@code registryType}
		 *
		 * @param type
		 * 		the type that is missing a handler
		 * @param registryType
		 * 		the type of the registry
		 * @return the {@link I18N} message for the missing handler
		 * @see GenericHandler#getType()
		 * @see GenericHandlerRegistry#getRegistryType()
		 */
		private static String createMessage(String type, String registryType) {
			int namespacePos = type.indexOf(':');
			String namespace = null;
			if (namespacePos >= 0) {
				namespace = type.substring(0, namespacePos);
			}
			String errorKey = ERROR_PREFIX;
			if (namespace == null) {
				errorKey += ERROR_CORE;
			} else {
				errorKey += ERROR_EXTENSION;
			}
			String handlerTypeName = Objects.toString(I18N.getErrorMessageOrNull(TYPE_PREFIX + registryType), "");
			return I18N.getErrorMessage(errorKey, handlerTypeName, type, namespace);
		}
	}

	private Map<String, H> registeredHandlers = new HashMap<>();
	private EventListenerList eventListeners = new EventListenerList();

	/** Abstract singleton class */
	protected GenericHandlerRegistry() {
	}

	/**
	 * Register new {@link H handler}. Must not be {@code null}. If a handler with the same type was already
	 * registered, does nothing. If the handler is successfully registered,
	 * triggers a {@link RegistrationEventType#REGISTERED REGISTERED} event.
	 */
	public void registerHandler(H handler) {
		ValidationUtil.requireNonNull(handler, "handler");
		if (registeredHandlers.putIfAbsent(ValidationUtil.requireNonEmptyString(handler.getType(), "handler type"), handler) == null) {
			fireRegistryEvent(handler, RegistrationEventType.REGISTERED);
		}
	}

	/**
	 * Unregister a {@link H handler}. Must not be {@code null}. Only can be removed if exactly this handler
	 * was registered before using {@link #registerHandler(H)}. If the handler is successfully unregistered,
	 * triggers a {@link RegistrationEventType#UNREGISTERED UNREGISTERED} event.
	 */
	public void unregisterHandler(H handler) {
		ValidationUtil.requireNonNull(handler, "handler");
		if (registeredHandlers.remove(ValidationUtil.requireNonEmptyString(handler.getType(), "handler type"), handler)) {
			fireRegistryEvent(handler, RegistrationEventType.UNREGISTERED);
		}
	}

	/** Add an event listener for {@link RegistrationEvent RegistrationEvents} */
	public <L extends GenericRegistrationEventListener<H>> void addEventListener(L listener) {
		ValidationUtil.requireNonNull(listener, "listener");
		eventListeners.add(getListenerClass(listener), listener);
	}

	/** Remove the specified event listener for {@link RegistrationEvent RegistrationEvents} */
	public <L extends GenericRegistrationEventListener<H>> void removeEventListener(L listener) {
		ValidationUtil.requireNonNull(listener, "listener");
		eventListeners.remove(getListenerClass(listener), listener);
	}

	/**
	 * Get the handler for the specified type if one is registered. Otherwise throws a {@link MissingHandlerException}
	 * with more details.
	 *
	 * @param type
	 * 		the type to get the handler for
	 * @return the handler for the given type if it exists, never {@code null}
	 * @throws MissingHandlerException
	 * 		if the type is not known
	 * @see #isTypeKnown(String)
	 */
	public H getHandler(String type) throws MissingHandlerException {
		ValidationUtil.requireNonEmptyString(type, "type");
		H handler = registeredHandlers.get(type);
		if (handler == null) {
			throw new MissingHandlerException(type, getRegistryType());
		}
		return handler;
	}

	/**
	 * Check whether a handler is registered for the given type. If {@code true}, it is safe to call {@link #getHandler(String)}
	 *
	 * @param type
	 * 		the type to check
	 * @return whether the type is known, i.e. has a corresponding handler
	 */
	public boolean isTypeKnown(String type) {
		return registeredHandlers.containsKey(type);
	}

	/** Returns a list of all registered handlers */
	public List<String> getAllTypes() {
		return new ArrayList<>(registeredHandlers.keySet());
	}

	/** Returns either an implementation specific marker interface or the listeners class */
	protected abstract <G extends GenericRegistrationEventListener<H>, L extends G> Class<G> getListenerClass(L listener);

	/**
	 * Returns this registry's type. This is used for some i18n handling and should have an error entry
	 * {@value MissingHandlerException#TYPE_PREFIX}{@code type}
	 */
	protected abstract String getRegistryType();

	/** Fire a {@link RegistrationEvent} of the given {@link RegistrationEventType} regarding the specified handler. */
	private void fireRegistryEvent(H handler, RegistrationEventType eventType) {
		for (GenericRegistrationEventListener<H> listener : eventListeners.getListeners(getListenerClass(null))) {
			listener.registrationChanged(new RegistrationEvent(this, eventType), handler);
		}
	}
}
