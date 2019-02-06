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

import java.util.List;

import com.rapidminer.gui.tools.RegistryEvent;
import com.rapidminer.gui.tools.RegistryListener;
import com.rapidminer.gui.tools.RegistryEvent.RegistryEventType;
import com.rapidminer.tools.AbstractObservable;
import com.rapidminer.tools.Observer;


/**
 * This log selection model contains the currently selected {@link LogModel} in the
 * {@link LogViewer} GUI.
 * <p>
 * To be notified when the selected model changes, add yourself as an {@link Observer} via
 * {@link #addObserver(Observer, boolean)}.
 * </p>
 * 
 * @author Sabrina Kirstein, Marco Boeck
 */
public class LogSelectionModel extends AbstractObservable<LogModel> {

	/** the currently selected log model or null */
	private LogModel currentModel;

	private RegistryListener<LogModel> registerListener;

	public LogSelectionModel() {
		// listener to be notified when a log model is added/removed from the LogModelRegistry
		registerListener = new RegistryListener<LogModel>() {

			@Override
			public void eventTriggered(RegistryEvent<LogModel> event) {
				List<LogModel> registeredLogs = LogModelRegistry.INSTANCE.getRegisteredObjects();
				if (event.getObject() != null) {
					if (event.getType() == RegistryEventType.REGISTERED) {
						// set this as the current model because we want to highlight new logs
						setSelectedLogModel(event.getObject());
					} else if (event.getType() == RegistryEventType.UNREGISTERED) {
						// if the current model has been unregistered, simply use the first
						// model in the keyset or null if none is available anymore
						if (event.getObject().equals(currentModel)) {
							// we always have our RapidMiner Studio log which is not closable, so
							// this is safe
							setSelectedLogModel(registeredLogs.get(0));
						}
					}

				}
			}
		};
		LogModelRegistry.INSTANCE.registerListener(registerListener);
		List<LogModel> registeredModels = LogModelRegistry.INSTANCE.getRegisteredObjects();
		if (!registeredModels.isEmpty()) {
			this.currentModel = registeredModels.get(0);
		}
	}

	/**
	 * Returns the currently selected {@link LogModel}. If none has been set via
	 * {@link #setSelectedLogModel(LogModel)}, returns the first. If no model is available, returns
	 * <code>null</code>.
	 * 
	 * @return
	 */
	public LogModel getCurrentLogModel() {
		return currentModel;
	}

	/**
	 * Sets the currently selected {@link LogModel}.
	 * 
	 * @param model
	 * @throws IllegalArgumentException
	 *             if the given model is not contained in {@link #getLogModels()} or is
	 *             <code>null</code>.
	 */
	public void setSelectedLogModel(LogModel model) throws IllegalArgumentException {
		if (model == null || !LogModelRegistry.INSTANCE.getRegisteredObjects().contains(model)) {
			throw new IllegalArgumentException("model must not be null and must be contained in the available models!");
		}

		this.currentModel = model;
		// notify observers that the current model has changed.
		fireUpdate(model);
	}
}
