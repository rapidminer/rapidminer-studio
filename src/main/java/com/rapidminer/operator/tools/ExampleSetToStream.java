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
package com.rapidminer.operator.tools;

import java.io.DataInput;
import java.io.DataInputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.AttributeRole;
import com.rapidminer.example.Example;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.Tools;
import com.rapidminer.example.table.AttributeFactory;
import com.rapidminer.example.table.DoubleSparseArrayDataRow;
import com.rapidminer.example.table.NominalMapping;
import com.rapidminer.example.table.PolynominalMapping;
import com.rapidminer.example.table.SparseDataRow;
import com.rapidminer.example.utils.ExampleSetBuilder;
import com.rapidminer.example.utils.ExampleSets;
import com.rapidminer.operator.Annotations;
import com.rapidminer.tools.LogService;
import com.rapidminer.tools.Ontology;


/**
 * Writes and reads a example sets to and from from streams. TODO: Implement sparse counterpart.
 *
 * @author Simon Fischer
 *
 */
public class ExampleSetToStream {

	/** Original version, used for RapidMiner beta 5 */
	public static final int VERSION_1 = 1;

	/**
	 * Fixes a problem with long strings in DataOutput.writeUTF() which restricts the length to 65k
	 * bytes. Used since RapidMiner 5.0 final release, revision 7197.
	 */
	public static final int VERSION_2 = 2;

	/**
	 * Adds support for {@link Annotations} Used since revision 7430.
	 */
	public static final int VERSION_3 = 3;

	/**
	 * Current version of the stream protocol. To add a new version: - Add a constant here, and
	 * redirect the constant CURRENT_VERSION to the new constant. - Add SVN revision to the comment
	 * of the new version - In {@link SerializationType} add a new enum constant for the new version
	 * and make it the default
	 * */
	public static final int CURRENT_VERSION = VERSION_3;

	private static final Charset STRING_CHARSET = Charset.forName("UTF-8");

	public enum ColumnType {
		NOMINAL_BYTE, NOMINAL_SHORT, NOMINAL_INTEGER, DOUBLE, INTEGER;
	}

	public static class Header {

		private final Annotations annotations;
		private final List<AttributeRole> allRoles;
		private final boolean sparse;

		protected Header(final Annotations annotations, final List<AttributeRole> allRoles, final boolean sparse) {
			super();
			this.allRoles = allRoles;
			this.sparse = sparse;
			this.annotations = annotations;
		}

		public List<AttributeRole> getAllRoles() {
			return allRoles;
		}

		public boolean isSparse() {
			return sparse;
		}

		public Annotations getAnnotations() {
			return annotations;
		}
	}

	private final int version;

	public ExampleSetToStream(final int version) {
		this.version = version;
		if (version != CURRENT_VERSION) {
			LogService.getRoot().log(Level.FINE,
					"com.rapidminer.operator.tools.ExampleSetToStream.using_deprecated_version", version);
		}
	}

	/** Writes header and data of the example set to the stream. */
	public void write(final ExampleSet exampleSet, final OutputStream outputStream) throws IOException {
		DataOutputStream out = new DataOutputStream(outputStream);
		List<AttributeRole> allRoles = new LinkedList<>();
		Iterator<AttributeRole> i = exampleSet.getAttributes().allAttributeRoles();
		while (i.hasNext()) {
			allRoles.add(i.next());
		}
		boolean sparse = false;
		// TODO: Remove ugly instanceof check
		if ((exampleSet.size() > 0) && (exampleSet.getExample(0).getDataRow() instanceof SparseDataRow)) {
			sparse = true;
		}
		writeHeader(exampleSet.getAnnotations(), allRoles, out, sparse);
		writeData(exampleSet, out, allRoles, sparse);
		out.flush();
	}

	/**
	 * Writes nominals and integers as integer, all others as double. All values are prefixed by a
	 * boolean indicating whether the following value is missing, in which case the latter is not
	 * sent at all.
	 *
	 * Iterates over all examples and all attributes - For non-sparse representation, each attribute
	 * value is sent as the data type corresponding to the respective {@link ColumnType}. For
	 * {@link ColumnType#INTEGER}, missing values are sent as Integer.MIN_VALUE+1 plus a "true"
	 * (boolean). The value Integer.MIN_VALUE+1 itself is sent as Integer.MIN_VALUE+1 plus a "false"
	 * (boolean). Otherwise missings are encoded as -1 since nominal indices are always
	 * non-negative. - For sparse representation, only non-default attribute values are sent,
	 * prefixed by an int specifying the attribute index. An attribute index of -1 signals the end
	 * of an example
	 */
	private void writeData(final ExampleSet exampleSet, final DataOutputStream out, final List<AttributeRole> allRoles,
			final boolean sparse) throws IOException {
		out.writeInt(exampleSet.size());
		ColumnType[] columnTypes = convertToColumnTypes(allRoles);

		for (Example example : exampleSet) {
			int attributeIndex = 0;
			for (AttributeRole role : allRoles) {
				Attribute attribute = role.getAttribute();
				double value = example.getValue(attribute);
				writeDatum(value, attributeIndex, attribute, columnTypes[attributeIndex], out, sparse);
				attributeIndex++;
			}
			if (sparse) {
				// indicates linebreaks
				out.writeInt(-1);
			}
		}
	}

	/**
	 * Writes the annotations, meta data, including nominal mappings, to the stream, in the
	 * following order: - annotations {@link #writeAnnotations(DataOutput, Annotations)} - number of
	 * attributes to come - For each attribute - name - special name (empty string if not special!)
	 * - value type name - block type name - If nominal, the number of nominal values, and for each
	 * nominal value - the index - the string - the annotations of the attribute After that follows
	 * a boolean indicating whether we are using sparse format. If yes, all default values will be
	 * sent as doubles, one per attribute.
	 */
	public void writeHeader(final Annotations annotations, final List<AttributeRole> allAttributes,
			final DataOutputStream out, final boolean sparse) throws IOException {
		writeAnnotations(out, annotations);
		out.writeInt(allAttributes.size());
		for (AttributeRole role : allAttributes) {
			Attribute att = role.getAttribute();
			writeString(out, att.getName());
			String specialName = role.getSpecialName();
			if (specialName != null) {
				writeString(out, specialName);
			} else {
				writeString(out, "");
			}
			writeString(out, Ontology.ATTRIBUTE_VALUE_TYPE.mapIndex(att.getValueType()));
			writeString(out, Ontology.ATTRIBUTE_BLOCK_TYPE.mapIndex(att.getBlockType()));
			if (att.isNominal()) {
				NominalMapping mapping = att.getMapping();
				out.writeInt(mapping.size());
				for (String value : mapping.getValues()) {
					out.writeInt(mapping.mapString(value));
					writeString(out, value);
				}
			}
			writeAnnotations(out, att.getAnnotations());
		}
		out.writeBoolean(sparse);
		if (sparse) {
			for (AttributeRole role : allAttributes) {
				out.writeDouble(role.getAttribute().getDefault());
			}
		}
	}

	/** Reads an example set as written by {@link #write(ExampleSet, OutputStream)}. */
	public ExampleSet read(final InputStream inputStream) throws IOException {
		DataInputStream in = new DataInputStream(inputStream);

		// Extract Header information
		Header header = readHeader(in);
		List<AttributeRole> allAttributeRoles = header.getAllRoles();
		List<Attribute> allAttributes = new ArrayList<>();
		for (AttributeRole role : allAttributeRoles) {
			allAttributes.add(role.getAttribute());
		}
		ColumnType columnTypes[] = convertToColumnTypes(allAttributeRoles);
		boolean sparse = header.isSparse();

		// Create example table
		int size = in.readInt();
		ExampleSetBuilder builder = ExampleSets.from(allAttributes).withExpectedSize(size);

		// Read data
		for (int row = 0; row < size; row++) {
			if (sparse) {
				DoubleSparseArrayDataRow sparseRow = new DoubleSparseArrayDataRow(allAttributeRoles.size());
				while (true) {
					int index = in.readInt();
					if (index == -1) {
						break;
					} else {
						sparseRow.set(allAttributes.get(index), readDatum(in, columnTypes[index]));
					}
				}
				sparseRow.trim();
				builder.addDataRow(sparseRow);
			} else {
				double[] data = new double[allAttributeRoles.size()];
				readRow(in, data, columnTypes, sparse, null);
				builder.addRow(data);
			}
		}

		// Create example set
		ExampleSet exampleSet = builder.build();
		// finally, set special attributes
		for (AttributeRole role : allAttributeRoles) {
			if (role.isSpecial()) {
				Attribute att = exampleSet.getAttributes().get(role.getAttribute().getName());
				exampleSet.getAttributes().getRole(att).setSpecial(role.getSpecialName());
			}
		}
		exampleSet.getAnnotations().putAll(header.getAnnotations());
		return exampleSet;
	}

	/**
	 * Reads meta data information as written by {@link #writeHeader(List, DataOutputStream)}. TODO:
	 * This must return an ExampleSetHeader including the roles and the sparse flag.
	 */
	public Header readHeader(final DataInputStream in) throws IOException {
		Annotations annotations = readAnnotations(in);
		int numAttributes = in.readInt();
		List<AttributeRole> allRoles = new LinkedList<>();
		for (int i = 0; i < numAttributes; i++) {
			String name = readString(in);
			String special = readString(in);
			if (special.length() == 0) {
				special = null;
			}
			String tmp = readString(in);
			int valueType = Ontology.ATTRIBUTE_VALUE_TYPE.mapName(tmp);
			if (valueType == -1) {
				throw new IOException("Unknown value type: '" + tmp + "'");
			}
			tmp = readString(in);
			int blockType = Ontology.ATTRIBUTE_BLOCK_TYPE.mapName(tmp);
			if (blockType == -1) {
				throw new IOException("Unknown value type: '" + tmp + "'");
			}
			Attribute attribute = AttributeFactory.createAttribute(name, valueType, blockType);
			AttributeRole role = new AttributeRole(attribute);
			if (special != null) {
				role.setSpecial(special);
			}
			allRoles.add(role);

			// read mapping
			if (attribute.isNominal()) {
				int numValues = in.readInt();
				if (Ontology.ATTRIBUTE_VALUE_TYPE.isA(attribute.getValueType(), Ontology.BINOMINAL)) {
					// in this case we have a binominal mapping and we can keep it.
					NominalMapping mapping = attribute.getMapping();
					for (int j = 0; j < numValues; j++) {
						int index = in.readInt();
						String value = readString(in);
						mapping.setMapping(value, index);
					}
				} else {
					Map<Integer, String> valueMap = new HashMap<>();
					for (int j = 0; j < numValues; j++) {
						int index = in.readInt();
						String value = readString(in);
						valueMap.put(index, value);
					}
					attribute.setMapping(new PolynominalMapping(valueMap));
				}
			}
			Annotations attAnnotations = readAnnotations(in);
			attribute.getAnnotations().putAll(attAnnotations);
		}

		boolean sparse = in.readBoolean();
		if (sparse) {
			for (AttributeRole role : allRoles) {
				role.getAttribute().setDefault(in.readDouble());
			}
		}
		return new Header(annotations, allRoles, sparse);
	}

	/** Extracts column types such that they have minimal memory consumption. */
	public ColumnType[] convertToColumnTypes(final List<AttributeRole> allRoles) {
		ColumnType columnTypes[] = new ColumnType[allRoles.size()];
		for (int i = 0; i < columnTypes.length; i++) {
			Attribute att = allRoles.get(i).getAttribute();
			if (att.isNominal()) {
				if (att.getMapping().size() < Byte.MAX_VALUE) {
					columnTypes[i] = ColumnType.NOMINAL_BYTE;
				} else if (att.getMapping().size() < Short.MAX_VALUE) {
					columnTypes[i] = ColumnType.NOMINAL_SHORT;
				} else {
					columnTypes[i] = ColumnType.NOMINAL_INTEGER;
				}
			} else if (att.isNumerical()) {
				if (Ontology.ATTRIBUTE_VALUE_TYPE.isA(att.getValueType(), Ontology.INTEGER)) {
					columnTypes[i] = ColumnType.INTEGER;
				} else {
					columnTypes[i] = ColumnType.DOUBLE;
				}
			} else {
				columnTypes[i] = ColumnType.DOUBLE;
			}
		}
		return columnTypes;
	}

	/**
	 * Writes a single datum with the given index. The data type is specified by the parameter
	 * columnType. If sparse is true, the value is prefixed by the given attributeIndex.
	 */
	public final void writeDatum(final double value, final int attributeIndex, final Attribute attribute,
			final ColumnType columnType, final DataOutput out, final boolean sparse) throws IOException {
		if (sparse) {
			if (Tools.isDefault(attribute.getDefault(), value)) {
				return;
			} else {
				out.writeInt(attributeIndex);
			}
		}

		switch (columnType) {
			case DOUBLE:
				out.writeDouble(value);
				break;
			case INTEGER:
				if (Double.isNaN(value)) {
					out.writeInt(Integer.MIN_VALUE + 1);
					out.writeBoolean(true);
				} else {
					out.writeInt((int) value);
					if ((int) value == Integer.MIN_VALUE + 1) {
						out.writeBoolean(false);
					}
				}
				break;
			// For the nominal values, we *can* use -1 to encode missings since all values are
			// guaranteed to be non-negative
			case NOMINAL_BYTE:
				if (Double.isNaN(value)) {
					out.writeByte(-1);
				} else {
					out.writeByte((byte) value);
				}
				break;
			case NOMINAL_INTEGER:
				if (Double.isNaN(value)) {
					out.writeInt(-1);
				} else {
					out.writeInt((int) value);
				}
				break;
			case NOMINAL_SHORT:
				if (Double.isNaN(value)) {
					out.writeShort(-1);
				} else {
					out.writeShort((short) value);
				}
				break;
			default:
				// cannot happen
				throw new RuntimeException("Illegal type: " + columnType);
		}
	}

	/**
	 * Reads a single datum in non-sparse representation of the given type and returns it as a
	 * double.
	 */
	private final double readDatum(final DataInput in, final ColumnType columnType) throws IOException {
		switch (columnType) {
			case DOUBLE:
				return in.readDouble();
			case INTEGER:
				int iValue = in.readInt();
				if (iValue == Integer.MIN_VALUE + 1) {
					boolean isMissing = in.readBoolean();
					if (isMissing) {
						return Double.NaN;
					} else {
						return iValue;
					}
				} else {
					return iValue;
				}
			case NOMINAL_BYTE:
				byte bValue = in.readByte();
				if (bValue == -1) {
					return Double.NaN;
				} else {
					return bValue;
				}
			case NOMINAL_INTEGER:
				iValue = in.readInt();
				if (iValue == -1) {
					return Double.NaN;
				} else {
					return iValue;
				}
			case NOMINAL_SHORT:
				short sValue = in.readShort();
				if (sValue == -1) {
					return Double.NaN;
				} else {
					return sValue;
				}
			default:
				// cannot happen
				throw new RuntimeException("Illegal type: " + columnType);
		}
	}

	/** Reads a single row from the stream. */
	public void readRow(final DataInputStream in, final double[] data, final ColumnType[] columnTypes, final boolean sparse,
			final double[] sparseDefaults) throws IOException {
		if (sparse) {
			System.arraycopy(sparseDefaults, 0, data, 0, sparseDefaults.length);
			while (true) {
				int index = in.readInt();
				if (index == -1) {
					break;
				} else {
					data[index] = readDatum(in, columnTypes[index]);
				}
			}
		} else {
			for (int attIndex = 0; attIndex < columnTypes.length; attIndex++) {
				data[attIndex] = readDatum(in, columnTypes[attIndex]);
			}
		}
	}

	private void writeString(final DataOutput out, String value) throws IOException {
		if (value == null) {
			value = "";
		}
		switch (version) {
			case VERSION_1:
				out.writeUTF(value);
				break;
			case VERSION_2:
			case VERSION_3:
				byte[] bytes = value.getBytes(STRING_CHARSET);
				out.writeInt(bytes.length);
				out.write(bytes);
				break;
			default:
				throw new RuntimeException("Version not set");
		}
	}

	private String readString(final DataInput in) throws IOException {
		switch (version) {
			case VERSION_1:
				return in.readUTF();
			case VERSION_2:
			case VERSION_3:
				int length = in.readInt();
				byte[] bytes = new byte[length];
				in.readFully(bytes);
				return new String(bytes, STRING_CHARSET);
			default:
				throw new RuntimeException("Version not set");
		}
	}

	public int getVersion() {
		return version;
	}

	/**
	 * One integer for size For each annotation - one string (
	 * {@link #writeString(DataOutput, String)} for key - one string (
	 * {@link #writeString(DataOutput, String)} for value
	 * */
	public void writeAnnotations(final DataOutput out, final Annotations annotations) throws IOException {
		if (version < VERSION_3) {
			// LogService.getRoot().warning("Ignoring annotations in example set stream version "+version);
			LogService.getRoot().log(Level.WARNING, "com.rapidminer.operator.tools.ExampleSetToStream.ignoring_annotations",
					version);
		} else {
			if (annotations == null) {
				out.writeInt(0);
			} else {
				out.writeInt(annotations.size());
				for (String key : annotations.getKeys()) {
					writeString(out, key);
					writeString(out, annotations.getAnnotation(key));
				}
			}
		}
	}

	public Annotations readAnnotations(final DataInput in) throws IOException {
		if (version < VERSION_3) {
			return new Annotations();
		} else {
			Annotations result = new Annotations();
			int size = in.readInt();
			for (int i = 0; i < size; i++) {
				result.setAnnotation(readString(in), readString(in));
			}
			return result;
		}
	}

}
