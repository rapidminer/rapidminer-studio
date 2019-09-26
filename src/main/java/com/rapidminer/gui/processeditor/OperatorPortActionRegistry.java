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
package com.rapidminer.gui.processeditor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.rapidminer.gui.tools.ResourceAction;
import com.rapidminer.operator.IOObject;
import com.rapidminer.operator.ports.Port;


/**
 * The {@link OperatorPortActionRegistry} provides a hook to add {@link ResourceAction}s  to {@link Port}s from {@link com.rapidminer.operator.Operator}s.
 * Based on the type and the available {@link IOObject} of the {@link Port} the {@link OperatorPortActionProducer} can
 * decide to provide a new {@link ResourceAction} to be added to the context menu of the hovered and right-clicked port.
 *
 * @author Andreas Timm
 * @since 9.1
 */
public enum OperatorPortActionRegistry {
	/**
	 * The instance
	 */
	INSTANCE;

	/**
	 * Map containing registered {@link OperatorPortActionProducer}s for a {@link Port} class
	 */
	private Map<Class<? extends Port>, List<OperatorPortActionProducer>> porttypeActions = new HashMap<>();

	/**
	 * Add an {@link OperatorPortActionProducer} to these {@link Port} class instances and its specializations.
	 *
	 * @param clazz
	 * 		the {@link Port} class the producer is registered for
	 * @param producer
	 * 		the {@link ResourceAction} producing instance
	 */
	public void addPortAction(Class<? extends Port> clazz, OperatorPortActionProducer producer) {
		if (clazz == null) {
			throw new IllegalArgumentException("'clazz' must not be null");
		}
		if (producer == null) {
			throw new IllegalArgumentException("'producer' must not be null");
		}
		porttypeActions.computeIfAbsent(clazz, f -> new ArrayList<>()).add(producer);
	}

	/**
	 * Remove an {@link OperatorPortActionProducer} from the registry.
	 *
	 * @param clazz
	 * 		{@link Port} class the producer was registered for
	 * @param producer
	 * 		the {@link ResourceAction} producing instance
	 * @return true if the removal did remove an entry from the registry
	 */
	public boolean removePortAction(Class<Port> clazz, OperatorPortActionProducer producer) {
		if (porttypeActions.containsKey(clazz)) {
			final List<OperatorPortActionProducer> resourceActions = porttypeActions.get(clazz);
			return resourceActions.remove(producer);
		}
		return false;
	}

	/**
	 * Get the {@link ResourceAction}s for the given {@link Port}. Check if the registered {@link OperatorPortActionProducer}
	 * accept the data type provided from getAnyDataOrNull and add it to the result.
	 *
	 * @param port
	 * 		the port that is being checked for further entries.
	 * @return the additional ResourceActions in alphabetic order
	 */
	public List<ResourceAction> getPortActions(Port port) {
		if (port == null || port.getRawData() == null) {
			return Collections.emptyList();
		}

		final Class<? extends IOObject> ioobjectClass = port.getRawData().getClass();
		List<ResourceAction> result = new ArrayList<>();
		for (Map.Entry<Class<? extends Port>, List<OperatorPortActionProducer>> entry : porttypeActions.entrySet()) {
			if (!entry.getKey().isAssignableFrom(port.getClass())) {
				continue;
			}
			final List<OperatorPortActionProducer> operatorPortActionProducers = entry.getValue();
			if (operatorPortActionProducers == null) {
				continue;
			}
			operatorPortActionProducers.stream().filter(portAction -> portAction.acceptsConvertible(ioobjectClass))
					.map(portAction -> portAction.createAction(port)).filter(Objects::nonNull).forEach(result::add);
		}
		return result;
	}

}
