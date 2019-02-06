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

import com.rapidminer.example.AttributeRole;
import com.rapidminer.example.NominalStatistics;
import com.rapidminer.example.NumericalStatistics;
import com.rapidminer.example.SimpleAttributes;
import com.rapidminer.example.UnknownStatistics;
import com.rapidminer.example.WeightedNumericalStatistics;
import com.rapidminer.example.table.BinominalAttribute;
import com.rapidminer.example.table.BinominalMapping;
import com.rapidminer.example.table.NumericalAttribute;
import com.rapidminer.example.table.PolynominalAttribute;
import com.rapidminer.example.table.PolynominalMapping;
import com.rapidminer.operator.IOContainer;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorCreationException;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.performance.AbstractPerformanceEvaluator;
import com.rapidminer.operator.performance.PerformanceCriterion;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.lang.reflect.Constructor;
import java.util.List;
import java.util.logging.Level;


/**
 * This class handles all kinds in- and output write processes for all kinds of objects into and
 * from XML. This class must use object streams since memory consumption is too big otherwise.
 * Hence, string based methods are no longer supported.
 * 
 * @author Ingo Mierswa
 */
public class XMLSerialization {

	private static ClassLoader classLoader;

	private com.thoughtworks.xstream.XStream xStream;

	private XMLSerialization(ClassLoader classLoader) {
		try {
			Class<?> xStreamClass = Class.forName("com.thoughtworks.xstream.XStream");
			Class<?> generalDriverClass = Class.forName("com.thoughtworks.xstream.io.HierarchicalStreamDriver");
			Constructor<?> constructor = xStreamClass.getConstructor(generalDriverClass);
			Class<?> driverClass = Class.forName("com.thoughtworks.xstream.io.xml.XppDriver");
			xStream = (com.thoughtworks.xstream.XStream) constructor.newInstance(driverClass.newInstance());
			xStream.setMode(com.thoughtworks.xstream.XStream.ID_REFERENCES);

			// define default aliases here
			addAlias("IOContainer", IOContainer.class);
			addAlias("PolynominalAttribute", PolynominalAttribute.class);
			addAlias("BinominalAttribute", BinominalAttribute.class);
			addAlias("NumericalAttribute", NumericalAttribute.class);

			addAlias("PolynominalMapping", PolynominalMapping.class);
			addAlias("BinominalMapping", BinominalMapping.class);

			addAlias("NumericalStatistics", NumericalStatistics.class);
			addAlias("WeightedNumericalStatistics", WeightedNumericalStatistics.class);
			addAlias("NominalStatistics", NominalStatistics.class);
			addAlias("UnknownStatistics", UnknownStatistics.class);

			addAlias("SimpleAttributes", SimpleAttributes.class);
			addAlias("AttributeRole", AttributeRole.class);

			xStream.setClassLoader(classLoader);

			defineXMLAliasPairs();
		} catch (Throwable e) {
			// TODO: Why are we catching Throwables?
			LogService.getRoot().log(Level.WARNING, I18N.getMessage(LogService.getRoot().getResourceBundle(),
							"com.rapidminer.tools.XMLSerialization.writing_initializing_xml_serialization_error", e), e);
		}
	}

	public static void init(ClassLoader classLoader) {
		XMLSerialization.classLoader = classLoader;
	}

	public void addAlias(String name, Class<?> clazz) {
		if (xStream != null) {
			String alias = name.replaceAll("[^a-zA-Z_0-9-]", "_").replaceAll("_+", "-");
			if (alias.endsWith("-")) {
				alias = alias.substring(0, alias.length() - 1);
			}
			xStream.alias(alias, clazz);
		}
	}

	public void writeXML(Object object, OutputStream out) throws IOException {
		if (xStream != null) {
			try (OutputStreamWriter osw = new OutputStreamWriter(out);
					ObjectOutputStream xOut = xStream.createObjectOutputStream(osw)) {
				xOut.writeObject(object);
				// xstream requires us to close() stream. see java doc of createObjectOutputStream
			}
		} else {
			LogService.getRoot().log(Level.WARNING, "com.rapidminer.tools.XMLSerialization.writing_xml_serialization_error");
			throw new IOException("Cannot write object with XML serialization.");
		}
	}

	public Object fromXML(InputStream in) throws IOException {
		if (xStream != null) {
			try (InputStreamReader xmlReader = new InputStreamReader(in);
					ObjectInputStream xIn = xStream.createObjectInputStream(xmlReader)) {
				return xIn.readObject();
			} catch (ClassNotFoundException e) {
				throw new IOException("Class not found: " + e.getMessage(), e);
			} catch (Exception e) {
				throw new IOException("Cannot read from XML stream, wrong format: " + e.getMessage(), e);
			}
		} else {
			LogService.getRoot().log(Level.WARNING,
					"com.rapidminer.tools.XMLSerialization.reading_object_from_XML_serialization_error");
			throw new IOException("Cannot read object from XML serialization.");
		}
	}

	/**
	 * Returns the singleton instance. We have to return a new instance, since the xStream will
	 * remember several mappings and causing a huge memory leak.
	 **/
	public static XMLSerialization getXMLSerialization() {
		return new XMLSerialization(classLoader);
	}

	/**
	 * Defines the alias pairs for the {@link XMLSerialization} for all IOObject pairs.
	 */
	private void defineXMLAliasPairs() {
		// pairs for IOObjects
		for (String ioObjectName : OperatorService.getIOObjectsNames()) {
			addAlias(ioObjectName, OperatorService.getIOObjectClass(ioObjectName));
		}

		// pairs for performance criteria

		for (String key : OperatorService.getOperatorKeys()) {
			OperatorDescription description = OperatorService.getOperatorDescription(key);
			// test if operator delivers performance criteria
			if (AbstractPerformanceEvaluator.class.isAssignableFrom(description.getOperatorClass())) {
				Operator operator = null;
				try {
					operator = OperatorService.createOperator(key);
				} catch (OperatorCreationException e) {
					// does nothing
				}
				if (operator != null) {
					AbstractPerformanceEvaluator evaluator = (AbstractPerformanceEvaluator) operator;
					List<PerformanceCriterion> criteria = evaluator.getCriteria();
					for (PerformanceCriterion criterion : criteria) {
						addAlias(criterion.getName(), criterion.getClass());
					}
				}
			}
		}
	}

}
