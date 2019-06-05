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
package com.rapidminer.operator.ports.metadata;

import com.rapidminer.adaption.belt.IOTable;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.operator.Annotations;
import com.rapidminer.operator.IOObject;
import com.rapidminer.operator.IOObjectCollection;
import com.rapidminer.operator.Model;
import com.rapidminer.connection.ConnectionInformationContainerIOObject;
import com.rapidminer.tools.DominatingClassFinder;
import com.rapidminer.tools.LogService;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;


/**
 * Factory class for creating {@link MetaData} objects for given {@link IOObject}s.
 *
 * See {@link #registerIOObjectMetaData(Class, Class)} and
 * {@link #createMetaDataforIOObject(IOObject, boolean)} for a description.
 *
 * @author Nils Woehler
 *
 */
public class MetaDataFactory {

	private static final MetaDataFactory INSTANCE = new MetaDataFactory();

	public static MetaDataFactory getInstance() {
		return INSTANCE;
	}

	private MetaDataFactory() {
		// private constructor for singleton
	}

	private static final Map<Class<? extends IOObject>, Class<? extends MetaData>> ioObjectToMetaDataClass = new HashMap<Class<? extends IOObject>, Class<? extends MetaData>>();

	static {
		MetaDataFactory.registerIOObjectMetaData(ExampleSet.class, ExampleSetMetaData.class);
		MetaDataFactory.registerIOObjectMetaData(IOTable.class, ExampleSetMetaData.class);
		MetaDataFactory.registerIOObjectMetaData(IOObjectCollection.class, CollectionMetaData.class);
		MetaDataFactory.registerIOObjectMetaData(Model.class, ModelMetaData.class);
		MetaDataFactory.registerIOObjectMetaData(ConnectionInformationContainerIOObject.class, ConnectionInformationMetaData.class);
	}

	/**
	 * Register meta data classes that will be instantiated when meta data for IO Object should be
	 * created. The meta data class must have an constructor with parameters the registered IO
	 * Object as first parameter and as second a boolean 'shortened' which defines whether the meta
	 * data should be complete or whether less relevant facts can be omitted (in order not to
	 * clutter their visual representation).
	 */
	public static void registerIOObjectMetaData(Class<? extends IOObject> ioObjectClass,
												Class<? extends MetaData> metaDataClass) {
		try {
			metaDataClass.getConstructor(ioObjectClass, boolean.class);
		} catch (Throwable e) {
			throw new IllegalArgumentException("Could not register meta data class " + metaDataClass + " for IO object "
					+ ioObjectClass + ". Check if MetaData has a constructor that "
					+ "accepts as first argument the provided IO object class and as second argument a boolean value.", e);
		}

		ioObjectToMetaDataClass.put(ioObjectClass, metaDataClass);
	}

	public MetaData createMetaDataforIOObject(IOObject ioo, boolean shortened) {
		MetaData result;

		Class<? extends IOObject> ioObjectClass = ioo.getClass();
		Class<? extends MetaData> metaDataClass = ioObjectToMetaDataClass.get(ioObjectClass);

		// No MetaData registered for IOObject class
		if (metaDataClass == null) {

			// look for next registered dominating IOObject class
			ioObjectClass = new DominatingClassFinder<IOObject>().findNextDominatingClass(ioo.getClass(),
					ioObjectToMetaDataClass.keySet());

			// registered dominating IOObject class found
			if (ioObjectClass != null) {
				metaDataClass = ioObjectToMetaDataClass.get(ioObjectClass);
			}
		}

		result = instantiateMetaData(metaDataClass, ioObjectClass, ioo, shortened);
		result.setAnnotations(new Annotations(ioo.getAnnotations()));
		return result;
	}

	private MetaData instantiateMetaData(Class<? extends MetaData> metaDataClass,
										 Class<? extends IOObject> compatibleIOObjectClass, IOObject ioo, boolean shortened) {
		if (metaDataClass != null) {
			try {
				return metaDataClass.getConstructor(compatibleIOObjectClass, boolean.class).newInstance(ioo, shortened);
			} catch (Throwable e) {
				LogService.getRoot().log(Level.WARNING,
						"com.rapidminer.operator.ports.metadata.MetaDataFactory.failed_to_create_meta_data", e);
			}
		}
		return new MetaData(ioo.getClass());
	}

}
