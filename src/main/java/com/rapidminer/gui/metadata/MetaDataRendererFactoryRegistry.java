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
package com.rapidminer.gui.metadata;

import java.awt.Component;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import com.rapidminer.gui.flow.ExampleSetMetaDataTableModel;
import com.rapidminer.operator.ports.metadata.ExampleSetMetaData;
import com.rapidminer.operator.ports.metadata.MetaData;


/**
 * Subclasses of {@link MetaDataRendererFactory} can register themselves here.
 * 
 * @author Simon Fischer, Gabor Makrai
 * 
 */
public class MetaDataRendererFactoryRegistry {

	private Map<Class<? extends MetaData>, MetaDataRendererFactory> factories = new HashMap<Class<? extends MetaData>, MetaDataRendererFactory>();

	private static final MetaDataRendererFactoryRegistry INSTANCE = new MetaDataRendererFactoryRegistry();
	static {
		getInstance().register(new MetaDataRendererFactory() {

			@Override
			public Class<? extends MetaData> getSupportedClass() {
				return ExampleSetMetaData.class;
			}

			@Override
			public Component createRenderer(MetaData metaData) {
				return ExampleSetMetaDataTableModel.makeTableForToolTip((ExampleSetMetaData) metaData);
			}
		});
	}

	/** Gets the singleton instance. */
	public static MetaDataRendererFactoryRegistry getInstance() {
		return INSTANCE;
	}

	/** Registers a new factory. */
	public void register(MetaDataRendererFactory factory) {
		factories.put(factory.getSupportedClass(), factory);
	}

	private int getInheritenceLevelDistanceRecursive(Class<?> currentClass, Class<?> targetClass, int distance) {

		// if the current class is the target class then return with the current distance
		if (currentClass.equals(targetClass)) {
			return distance;
		}

		boolean isInterface = currentClass.isInterface();
		Class<?>[] interfaces = currentClass.getInterfaces();
		// if it is a leaf node of the inheritance tree and it is not the target class then return
		// with -1
		if (isInterface && interfaces.length == 0) {
			return -1;
		}
		if (!isInterface && currentClass.getSuperclass().equals(Object.class)) {
			return -1;
		}

		// determine all super* (included superclass and superinterfaces)
			// if it is interface then there is no superclass
		Class<?>[] superClassAndInterfaces = Arrays.copyOf(interfaces, interfaces.length + (isInterface ? 0 : 1));

		// add superclass if it is not interface
		if (!isInterface) {
			superClassAndInterfaces[superClassAndInterfaces.length - 1] = currentClass.getSuperclass();
		}

		// run recursive search
		int[] returnDistances = new int[superClassAndInterfaces.length];
		for (int i = 0; i < superClassAndInterfaces.length; i++) {
			returnDistances[i] = getInheritenceLevelDistanceRecursive(superClassAndInterfaces[i], targetClass, distance + 1);
		}

		// select the smallest valid value from the return list
		int minReturn = Integer.MAX_VALUE;
		for (int i = 0; i < returnDistances.length; i++) {
			if (returnDistances[i] != -1 && returnDistances[i] < minReturn) {
				minReturn = returnDistances[i];
			}
		}

		// if the minReturn is not changed then return -1
		// this means that target class is nnot found in that branch
		if (minReturn == Integer.MAX_VALUE) {
			return -1;
		} else {
			return minReturn;
		}

	}

	/**
	 * 
	 * Determine the inheritance distance between two classes
	 * 
	 * @param child
	 *            Child class
	 * @param parent
	 *            Parent class
	 * @return Distance between the child and the parent
	 */
	private int getInheritenceLevelDistance(Class<?> child, Class<?> parent) {

		// null input parameters are not acceptable
		if (child == null || parent == null) {
			return -1;
		}

		// if they are not in the same inheritance branch then return -1
		if (!parent.isAssignableFrom(child)) {
			return -1;
		} else {

			// call the recursive inheritance tree travelsar
			return getInheritenceLevelDistanceRecursive(child, parent, 0);
		}
	}

	/**
	 * Creates a renderer for this meta data object or null if there is no suitable renderer or if
	 * the meta data is null.
	 */
	public Component createRenderer(MetaData metaData) {

		// handle the case when we get null metadata
		if (metaData == null) {
			return null;
		}

		// first of all, we need to check that factories contains or doesn't contain render for
		// metadata
		if (factories.containsKey(metaData.getClass())) {

			// if there is a renderer factory element in factories then we need to check that it is
			// null or not
			MetaDataRendererFactory factory = factories.get(metaData.getClass());

			if (factory == null) {
				// if it is null then return with null
				return null;
			} else {
				// it it is not null then call the createRenderer function on the renderer factory
				return factory.createRenderer(metaData);
			}

		} else { // there is no factory in factories for the given metadata, so let's try to find
					// one

			// find the closest (inheritance) renderer from the factories

			int distance = Integer.MAX_VALUE;

			Iterator<Class<? extends MetaData>> iterator = factories.keySet().iterator();

			Class<?> rendererCandidateMetaDataClass = null;

			while (iterator.hasNext()) {

				// get the next key from the factories
				Class<?> metaDataClass = iterator.next();

				// calculate the distance between key MD and parameter MD
				int currentDistance = getInheritenceLevelDistance(metaData.getClass(), metaDataClass);

				// find the closest one
				if (currentDistance != -1 && currentDistance < distance) {
					distance = currentDistance;
					rendererCandidateMetaDataClass = metaDataClass;
				}
			}

			// if an appropriate renderer is exist, then register it for this metadata
			if (rendererCandidateMetaDataClass != null) {
				MetaDataRendererFactory factory = factories.get(rendererCandidateMetaDataClass);
				factories.put(metaData.getClass(), factory);
				return factory.createRenderer(metaData);
			} else { // else add null to this metadata (to avoid further lookups)
				factories.put(metaData.getClass(), null);
				return null;
			}

		}
	}
}
