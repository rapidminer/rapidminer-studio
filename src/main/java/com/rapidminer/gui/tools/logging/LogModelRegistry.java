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
package com.rapidminer.gui.tools.logging;

import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

import com.rapidminer.gui.tools.Registry;
import com.rapidminer.gui.tools.RegistryEvent;
import com.rapidminer.gui.tools.RegistryEvent.RegistryEventType;
import com.rapidminer.gui.tools.RegistryListener;


/**
 * Registry for {@link LogModel}s which informs {@link RegistryListener} that {@link LogModel}s
 * (un)registered. The {@link LogModelRegistry} creates the default model.
 *
 * @author Sabrina Kirstein, Marco Boeck
 *
 */
public enum LogModelRegistry implements Registry<LogModel> {

	/** The singleton instance which is created by the JVM */
	INSTANCE;

	/** all registered {@link LogModel}s */
	private List<LogModel> registeredLogModels;

	/** all registered listeners which are notified when a model is added/removed */
	private List<RegistryListener<LogModel>> registeredListener;

	private static final Object LOCK_MODELS = new Object();
	private static final Object LOCK_LISTENER = new Object();

	private LogModelRegistry() {
		registeredLogModels = new LinkedList<>();
		registeredListener = new LinkedList<>();
	}

	/**
	 * Registers the specified {@link LogModel}. If a model with the same name (case insensitive)
	 * has already been registered, throws an {@link IllegalArgumentException}.
	 *
	 * @param logModel
	 *            the model to be registered
	 * @throws IllegalArgumentException
	 *             if a model with the same name (ignores case) is already registered
	 */
	@Override
	public void register(LogModel logModel) throws IllegalArgumentException {
		if (logModel == null) {
			throw new IllegalArgumentException("logModel must not be null!");
		}

		synchronized (LOCK_MODELS) {
			for (LogModel model : registeredLogModels) {
				if (model.getName().toLowerCase(Locale.ENGLISH).equals(logModel.getName().toLowerCase(Locale.ENGLISH))) {
					throw new IllegalArgumentException("logModel with name '" + logModel.getName() + "' already registered!");
				}
			}
			registeredLogModels.add(logModel);
		}

		// notify listeners of new model
		notifyListener(logModel, RegistryEventType.REGISTERED);
	}

	/**
	 * See {@link Registry#unregister(Object)}. Note that a {@link LogModel} which is not closable
	 * will not be unregistered!
	 *
	 * @throws IllegalArgumentException
	 *             if {@link LogModel#isClosable()} returns <code>false</code>
	 */
	@Override
	public void unregister(LogModel logModel) throws IllegalArgumentException {
		if (logModel == null) {
			throw new IllegalArgumentException("logModel must not be null!");
		}

		boolean removed;
		synchronized (LOCK_MODELS) {
			removed = registeredLogModels.remove(logModel);
		}

		// notify listeners of removed model
		if (removed) {
			notifyListener(logModel, RegistryEventType.UNREGISTERED);
		}
	}

	@Override
	public void registerListener(RegistryListener<LogModel> listener) {
		if (listener == null) {
			throw new IllegalArgumentException("listener must not be null!");
		}

		registeredListener.add(listener);
	}

	@Override
	public void unregisterListener(RegistryListener<LogModel> listener) {
		if (listener == null) {
			throw new IllegalArgumentException("listener must not be null!");
		}

		if (registeredListener.contains(listener)) {
			registeredListener.remove(listener);
		}
	}

	/**
	 * Returns all registered {@link LogModel}s.
	 *
	 * @return
	 */
	@Override
	public List<LogModel> getRegisteredObjects() {
		synchronized (LOCK_MODELS) {
			return new LinkedList<>(registeredLogModels);
		}
	}

	/**
	 * Notifies all listeners of a registered/unregistered event.
	 *
	 * @param logModel
	 *            the log model which was registered/unregistered
	 * @param type
	 *            the event type
	 */
	private void notifyListener(LogModel logModel, RegistryEventType type) {
		if (logModel == null) {
			throw new IllegalArgumentException("logModel must not be null!");
		}
		if (type == null) {
			throw new IllegalArgumentException("type must not be null!");
		}

		synchronized (LOCK_LISTENER) {
			for (RegistryListener<LogModel> listener : registeredListener) {
				listener.eventTriggered(new RegistryEvent<>(this, type, logModel));
			}
		}
	}
}
