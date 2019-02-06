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
package com.rapidminer.example.set;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.lang.reflect.Constructor;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPOutputStream;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.rapidminer.datatable.DataTable;
import com.rapidminer.datatable.DataTableExampleSetAdapter;
import com.rapidminer.example.Attribute;
import com.rapidminer.example.AttributeRole;
import com.rapidminer.example.AttributeWeights;
import com.rapidminer.example.Attributes;
import com.rapidminer.example.Example;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.Statistics;
import com.rapidminer.example.table.SparseFormatDataRowReader;
import com.rapidminer.io.process.XMLTools;
import com.rapidminer.operator.IOContainer;
import com.rapidminer.operator.IOObject;
import com.rapidminer.operator.MissingIOObjectException;
import com.rapidminer.operator.ResultObjectAdapter;
import com.rapidminer.tools.Ontology;
import com.rapidminer.tools.Tools;
import com.rapidminer.tools.XMLException;


/**
 * Implements wrapper methods of abstract example set. Implements all ResultObject methods.<br>
 *
 * Apart from the interface methods the implementing classes must have a public single argument
 * clone constructor. This constructor is invoked by reflection from the clone method. Do not forget
 * to call the superclass method.
 *
 * @author Ingo Mierswa, Simon Fischer
 */
public abstract class AbstractExampleSet extends ResultObjectAdapter implements ExampleSet {

	private static final long serialVersionUID = 8596141056047402798L;

	/** Maps attribute names to list of statistics objects. */
	private final Map<String, List<Statistics>> statisticsMap = new HashMap<String, List<Statistics>>();

	/** Maps the id values on the line index in the example table. */
	private Map<Double, int[]> idMap = new HashMap<Double, int[]>();

	/** This method overrides the implementation of ResultObjectAdapter and returns "ExampleSet". */
	@Override
	public String getName() {
		return "ExampleSet";
	}

	@Override
	public Example getExampleFromId(double id) {
		int[] indices = idMap.get(id);
		if (indices != null && indices.length > 0) {
			return getExample(indices[0]);
		} else {
			return null;
		}
	}

	@Override
	public int[] getExampleIndicesFromId(double id) {
		return idMap.get(id);
	}

	// --- Visualisation and toString() methods ---

	@Override
	public String toString() {
		StringBuffer str = new StringBuffer(this.getClass().getSimpleName() + ":" + Tools.getLineSeparator());
		str.append(size() + " examples," + Tools.getLineSeparator());
		str.append(getAttributes().size() + " regular attributes," + Tools.getLineSeparator());

		boolean first = true;
		Iterator<AttributeRole> s = getAttributes().specialAttributes();
		while (s.hasNext()) {
			if (first) {
				str.append("special attributes = {" + Tools.getLineSeparator());
				first = false;
			}
			AttributeRole special = s.next();
			str.append("    " + special.getSpecialName() + " = " + special.getAttribute() + Tools.getLineSeparator());
		}

		if (!first) {
			str.append("}");
		} else {
			str.append("no special attributes" + Tools.getLineSeparator());
		}

		return str.toString();
	}

	/**
	 * This method is used to create a {@link DataTable} from this example set. The default
	 * implementation returns an instance of {@link DataTableExampleSetAdapter}. The given
	 * IOContainer is used to check if there are compatible attribute weights which would used as
	 * column weights of the returned table. Subclasses might want to override this method in order
	 * to allow for other data tables.
	 */
	public DataTable createDataTable(IOContainer container) {
		AttributeWeights weights = null;
		if (container != null) {
			try {
				weights = container.get(AttributeWeights.class);
				for (Attribute attribute : getAttributes()) {
					double weight = weights.getWeight(attribute.getName());
					if (Double.isNaN(weight)) { // not compatible
						weights = null;
						break;
					}
				}
			} catch (MissingIOObjectException e) {
			}
		}
		return new DataTableExampleSetAdapter(this, weights);
	}

	// -------------------- File Writing --------------------

	@Override
	public void writeDataFile(File dataFile, int fractionDigits, boolean quoteNominal, boolean zipped, boolean append,
			Charset encoding) throws IOException {
		try (OutputStream outStream = new FileOutputStream(dataFile, append);
				OutputStream zippedStream = zipped ? new GZIPOutputStream(outStream) : null;
				OutputStreamWriter osw = new OutputStreamWriter(zipped ? zippedStream : outStream, encoding);
				PrintWriter out = new PrintWriter(osw)) {
			Iterator<Example> reader = iterator();
			while (reader.hasNext()) {
				out.println(reader.next().toDenseString(fractionDigits, quoteNominal));
			}
		}
	}

	/** Writes the data into a sparse file format. */
	@Override
	public void writeSparseDataFile(File dataFile, int format, int fractionDigits, boolean quoteNominal, boolean zipped,
			boolean append, Charset encoding) throws IOException {
		try (OutputStream outStream = new FileOutputStream(dataFile, append);
				OutputStream zippedStream = zipped ? new GZIPOutputStream(outStream) : null;
				OutputStreamWriter osw = new OutputStreamWriter(zipped ? zippedStream : outStream, encoding);
				PrintWriter out = new PrintWriter(osw)) {
			Iterator<Example> reader = iterator();
			while (reader.hasNext()) {
				out.println(reader.next().toSparseString(format, fractionDigits, quoteNominal));
			}
		}
	}

	/**
	 * Writes the attribute descriptions for all examples. Writes first all regular attributes and
	 * then the special attributes (just like the data write format of {@link Example#toString()}.
	 * Please note that the given data file will only be used to determine the relative position.
	 */
	@SuppressWarnings("deprecation")
	@Override
	public void writeAttributeFile(File attFile, File dataFile, Charset encoding) throws IOException {
		// determine relative path
		if (dataFile == null) {
			throw new IOException("ExampleSet writing: cannot determine path to data file: data file was not given!");
		}
		String relativePath = Tools.getRelativePath(dataFile, attFile);

		try {
			// building DOM
			Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();

			Element root = document.createElement("attributeset");
			root.setAttribute("default_source", relativePath);
			root.setAttribute("encoding", encoding.name());
			document.appendChild(root);

			int sourcecol = 1;
			Iterator<AttributeRole> i = getAttributes().allAttributeRoles();
			while (i.hasNext()) {
				root.appendChild(writeAttributeMetaData(i.next(), sourcecol, document, false));
				sourcecol++;
			}

			// writing XML from DOM
			try (FileOutputStream fos = new FileOutputStream(attFile);
					OutputStreamWriter osw = new OutputStreamWriter(fos, encoding);
					PrintWriter writer = new PrintWriter(osw)) {
				writer.print(XMLTools.toString(document, encoding));
			}
		} catch (ParserConfigurationException e) {
			throw new IOException("Cannot create XML document builder: " + e, e);
		} catch (XMLException e) {
			throw new IOException("Could not format XML document:" + e, e);
		}
	}

	/**
	 * Writes the attribute descriptions for all examples. Writes only the special attributes which
	 * are supported by the sparse format of the method
	 * {@link Example#toSparseString(int, int, boolean)}. Please note that the given data file is
	 * only be used to determine the relative position.
	 */
	@SuppressWarnings("deprecation")
	@Override
	public void writeSparseAttributeFile(File attFile, File dataFile, int format, Charset encoding) throws IOException {
		if (dataFile == null) {
			throw new IOException("ExampleSet sparse writing: cannot determine path to data file: data file was not given!");
		}

		String relativePath = Tools.getRelativePath(dataFile, attFile);

		try {
			// building DOM
			Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();

			Element root = document.createElement("attributeset");
			root.setAttribute("default_source", relativePath);
			root.setAttribute("encoding", encoding.name());
			document.appendChild(root);

			// special attributes
			AttributeRole labelRole = getAttributes().getRole(Attributes.LABEL_NAME);
			if (labelRole != null && format != SparseFormatDataRowReader.FORMAT_NO_LABEL) {
				root.appendChild(writeAttributeMetaData(labelRole, 0, document, true));
			}
			AttributeRole idRole = getAttributes().getRole(Attributes.ID_NAME);
			if (idRole != null) {
				root.appendChild(writeAttributeMetaData(idRole, 0, document, true));
			}
			AttributeRole weightRole = getAttributes().getRole(Attributes.WEIGHT_NAME);
			if (weightRole != null) {
				root.appendChild(writeAttributeMetaData(weightRole, 0, document, true));
			}

			// regular attributes
			int sourcecol = 1;
			for (Attribute attribute : getAttributes()) {
				root.appendChild(writeAttributeMetaData("attribute", attribute, sourcecol, document, false));
				sourcecol++;
			}

			// writing XML from DOM
			try (FileOutputStream fos = new FileOutputStream(attFile);
					OutputStreamWriter osw = new OutputStreamWriter(fos, encoding);
					PrintWriter writer = new PrintWriter(osw)) {
				writer.print(XMLTools.toString(document, encoding));
			}
		} catch (ParserConfigurationException e) {
			throw new IOException("Cannot create XML document builder: " + e, e);
		} catch (XMLException e) {
			throw new IOException("Could not format XML document:" + e, e);
		}
	}

	/** Writes the data of this attribute in the given stream. */
	private Element writeAttributeMetaData(AttributeRole attributeRole, int sourcecol, Document document, boolean sparse) {
		String tag = "attribute";
		if (attributeRole.isSpecial()) {
			tag = attributeRole.getSpecialName();
		}
		Attribute attribute = attributeRole.getAttribute();
		return writeAttributeMetaData(tag, attribute, sourcecol, document, sparse);
	}

	/** Writes the data of this attribute in the given stream. */
	private Element writeAttributeMetaData(String tag, Attribute attribute, int sourcecol, Document document,
			boolean sparse) {
		Element attributeElement = document.createElement(tag);
		attributeElement.setAttribute("name", attribute.getName());
		if (!sparse || tag.equals("attribute")) {
			attributeElement.setAttribute("sourcecol", sourcecol + "");
		}
		attributeElement.setAttribute("valuetype", Ontology.ATTRIBUTE_VALUE_TYPE.mapIndex(attribute.getValueType()));
		if (!Ontology.ATTRIBUTE_BLOCK_TYPE.isA(attribute.getBlockType(), Ontology.SINGLE_VALUE)) {
			attributeElement.setAttribute("blocktype", Ontology.ATTRIBUTE_BLOCK_TYPE.mapIndex(attribute.getBlockType()));
		}

		// nominal values
		if (Ontology.ATTRIBUTE_VALUE_TYPE.isA(attribute.getValueType(), Ontology.NOMINAL)
				&& !tag.equals(Attributes.KNOWN_ATTRIBUTE_TYPES[Attributes.TYPE_ID])) {
			for (String nominalValue : attribute.getMapping().getValues()) {
				Element valueElement = document.createElement("value");
				valueElement.setTextContent(nominalValue);
				attributeElement.appendChild(valueElement);
			}
		}
		return attributeElement;
	}

	public String getExtension() {
		return "aml";
	}

	public String getFileDescription() {
		return "attribute description file";
	}

	/**
	 * Returns true, if all attributes including labels and other special attributes are equal.
	 */
	@Override
	public boolean equals(Object o) {
		if (!(o instanceof ExampleSet)) {
			return false;
		}
		ExampleSet es = (ExampleSet) o;
		return getAttributes().equals(es.getAttributes());
	}

	/** Returns the hash code of all attributes. */
	@Override
	public int hashCode() {
		return getAttributes().hashCode();
	}

	@Override
	public IOObject copy() {
		return clone();
	}

	/**
	 * Clones the example set by invoking a single argument clone constructor. Please note that a
	 * cloned example set has no information about the attribute statistics. That means, that
	 * attribute statistics must be (re-)calculated after the clone was created.
	 */
	@Override
	public ExampleSet clone() {
		try {
			Class<? extends AbstractExampleSet> clazz = getClass();
			Constructor<? extends AbstractExampleSet> cloneConstructor = clazz.getConstructor(new Class[] { clazz });
			AbstractExampleSet result = cloneConstructor.newInstance(new Object[] { this });
			result.idMap = this.idMap;
			return result;
		} catch (IllegalAccessException e) {
			throw new RuntimeException("Cannot clone ExampleSet: " + e.getMessage());
		} catch (NoSuchMethodException e) {
			throw new RuntimeException("'" + getClass().getName() + "' does not implement clone constructor!");
		} catch (java.lang.reflect.InvocationTargetException e) {
			throw new RuntimeException("Cannot clone " + getClass().getName() + ": " + e + ". Target: "
					+ e.getTargetException() + ". Cause: " + e.getCause() + ".");
		} catch (InstantiationException e) {
			throw new RuntimeException("Cannot clone " + getClass().getName() + ": " + e);
		}
	}

	// =============================================================================

	@Override
	public void remapIds() {
		idMap = new HashMap<Double, int[]>(size());
		Attribute idAttribute = getAttributes().getSpecial(Attributes.ID_NAME);
		if (idAttribute != null) {
			int index = 0;
			for (Example example : this) {
				double value = example.getValue(idAttribute);
				if (!Double.isNaN(value)) {
					if (idMap.containsKey(value)) {
						int[] indices = idMap.get(value);
						int[] newIndices = new int[indices.length + 1];
						for (int i = 0; i < indices.length; i++) {
							newIndices[i] = indices[i];
						}
						newIndices[newIndices.length - 1] = index;
						idMap.put(value, newIndices);
					} else {
						idMap.put(value, new int[] { index });
					}
				}
				index++;
			}
		}
	}

	// =============================================================================

	/**
	 * Recalculates the attribute statistics for all attributes. They are average value, variance,
	 * minimum, and maximum. For nominal attributes the occurences for all values are counted. This
	 * method collects all attributes (regular and special) in a list and invokes
	 * <code>recalculateAttributeStatistics(List attributes)</code> and performs only one data scan.
	 * <p>
	 * The statistics calculation is stopped by {@link Thread#interrupt()}.
	 */
	@Override
	public void recalculateAllAttributeStatistics() {
		List<Attribute> allAttributes = new ArrayList<Attribute>();
		Iterator<Attribute> a = getAttributes().allAttributes();
		while (a.hasNext()) {
			allAttributes.add(a.next());
		}
		recalculateAttributeStatistics(allAttributes);
	}

	/**
	 * Recalculate the attribute statistics of the given attribute.
	 * <p>
	 * The statistics calculation is stopped by {@link Thread#interrupt()}.
	 */
	@Override
	public void recalculateAttributeStatistics(Attribute attribute) {
		List<Attribute> allAttributes = new ArrayList<Attribute>();
		allAttributes.add(attribute);
		recalculateAttributeStatistics(allAttributes);
	}

	/**
	 * Here the Example Set is parsed only once, all the information is retained for each example
	 * set.
	 * <p>
	 * The statistics calculation is stopped by {@link Thread#interrupt()}.
	 */
	private synchronized void recalculateAttributeStatistics(List<Attribute> attributeList) {
		// do nothing if not desired
		if (attributeList.size() == 0) {
			return;
		} else {
			// init statistics
			resetAttributeStatistics(attributeList);

			// calculate statistics
			Attribute weightAttribute = getAttributes().getWeight();
			if (weightAttribute != null && !weightAttribute.isNumerical()) {
				weightAttribute = null;
			}

			for (Attribute attribute : attributeList) {
				if (weightAttribute == null) {
					for (Example example : this) {
						double value = example.getValue(attribute);
						attribute.getAllStatistics().forEachRemaining(s -> s.count(value, 1.0d));
					}
				} else {
					for (Example example : this) {
						double value = example.getValue(attribute);
						double weight = example.getValue(weightAttribute);
						attribute.getAllStatistics().forEachRemaining(s -> s.count(value, weight));
					}
				}
				if (Thread.currentThread().isInterrupted()) {
					// statistics is only partly calculated
					resetAttributeStatistics(attributeList);
					return;
				}
			}

			// store cloned statistics
			for (Attribute attribute : attributeList) {
				// do not directly work on the existing List because that might force a
				// ConcurrentModification and the well known Exception
				List<Statistics> tmpStatisticsList = new LinkedList<>();

				Iterator<Statistics> stats = attribute.getAllStatistics();
				while (stats.hasNext()) {
					Statistics statistics = (Statistics) stats.next().clone();
					tmpStatisticsList.add(statistics);
				}
				statisticsMap.put(attribute.getName(), tmpStatisticsList);
				if (Thread.currentThread().isInterrupted()) {
					return;
				}
			}
		}
	}

	/**
	 * Resets the statistics for all attributes from attributeList.
	 *
	 * @param attributeList
	 *            the attributes for which to reset the statistics
	 */
	private void resetAttributeStatistics(List<Attribute> attributeList) {
		for (Attribute attribute : attributeList) {
			for (Iterator<Statistics> stats = attribute.getAllStatistics(); stats.hasNext();) {
				Statistics statistics = stats.next();
				statistics.startCounting(attribute);
			}
		}
	}

	/**
	 * Returns the desired statistic for the given attribute. This method should be preferred over
	 * the deprecated method Attribute#getStatistics(String) since it correctly calculates and keep
	 * the statistics for the current example set and does not overwrite the statistics in the
	 * attribute. Invokes the method {@link #getStatistics(Attribute, String, String)} with a null
	 * statistics parameter.
	 */
	@Override
	public double getStatistics(Attribute attribute, String statisticsName) {
		return getStatistics(attribute, statisticsName, null);
	}

	/**
	 * Returns the desired statistic for the given attribute. This method should be preferred over
	 * the deprecated method Attribute#getStatistics(String) since it correctly calculates and keep
	 * the statistics for the current example set and does not overwrite the statistics in the
	 * attribute. If the statistics were not calculated before (via one of the recalculate methods)
	 * this method will return NaN. If no statistics is available for the given name, also NaN is
	 * returned.
	 */
	@Override
	public double getStatistics(Attribute attribute, String statisticsName, String statisticsParameter) {
		List<Statistics> statisticsList = statisticsMap.get(attribute.getName());
		if (statisticsList == null) {
			return Double.NaN;
		}

		for (Statistics statistics : statisticsList) {
			if (statistics.handleStatistics(statisticsName)) {
				return statistics.getStatistics(attribute, statisticsName, statisticsParameter);
			}
		}

		return Double.NaN;
	}

	/**
	 * Returns {@code true} if and only if the view implemented by this {@link ExampleSet} is thread-safe with respect
	 * to read operations. This does not guarantee the thread-safety of the entire data set: both the underlying {@link
	 * com.rapidminer.example.table.ExampleTable} and the set's attributes might be unsafe to be read from concurrently
	 * and thus need to be checked separately.
	 *
	 * <p>A complete check is implemented by
	 * {@link com.rapidminer.example.utils.ExampleSets#createThreadSafeCopy(ExampleSet)} which only creates a deep copy
	 * if the thread-safety of the input example set is not guaranteed.
	 *
	 * @return {@code true} iff the view implemented by this example set is thread-safe w.r.t. to read operations
	 * @see com.rapidminer.example.utils.ExampleSets#createThreadSafeCopy(ExampleSet)
	 */
	public boolean isThreadSafeView() {
		return false;
	}
}
