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
package com.rapidminer.repository;

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;


/**
 *
 * A registry which allows to register custom repository implementations.
 *
 * @author Nils Woehler
 * @since 6.5.0
 *
 */
public enum CustomRepositoryRegistry {

	INSTANCE;

	private final List<CustomRepositoryFactory> factories = new LinkedList<>();

	/**
	 * Registers a new {@link CustomRepositoryFactory} instance.
	 *
	 * @param factory
	 *            the new {@link CustomRepositoryFactory} instance
	 * @throws RepositoryException
	 *             thrown if a factory for the {@link Repository} class or XML tag is already
	 *             registered
	 * @since 6.5
	 */
	public void register(CustomRepositoryFactory factory) throws RepositoryException {
		synchronized (factories) {
			if (getClasses().contains(factory.getRepositoryClass())) {
				throw new RepositoryException(String.format("Factory for custom repository class %s already registered!",
						factory.getRepositoryClass().getSimpleName()));
			} else if (getFactory(factory.getXMLTag()) != null) {
				throw new RepositoryException(String.format("Factory for XML tag %s already registered!",
						factory.getXMLTag()));
			}
			factories.add(factory);
		}
	}

	/**
	 * @return all registered {@link CustomRepositoryFactory} as unmodifiable {@link List}
	 * @since 6.5
	 */
	public List<CustomRepositoryFactory> getFactories() {
		synchronized (factories) {
			return Collections.unmodifiableList(factories);
		}
	}

	/**
	 * @return a set with all registered custom repository classes
	 * @since 6.5
	 */
	public Set<Class<? extends Repository>> getClasses() {
		Set<Class<? extends Repository>> repositoryClasses = new HashSet<>();
		for (CustomRepositoryFactory factory : getFactories()) {
			repositoryClasses.add(factory.getRepositoryClass());
		}
		return repositoryClasses;
	}

	/**
	 * @param xmlTag
	 *            the XML tag used to lookup the {@link CustomRepositoryFactory}
	 * @return the {@link CustomRepositoryFactory} for the provided XML tag or {@code null} if no
	 *         factory is available for the provided tag
	 * @since 6.5
	 */
	public CustomRepositoryFactory getFactory(String xmlTag) {
		if (xmlTag == null) {
			throw new IllegalArgumentException("XML tag must not be null");
		}
		for (CustomRepositoryFactory factory : getFactories()) {
			if (xmlTag.equals(factory.getXMLTag())) {
				return factory;
			}
		}
		return null;
	}
}
