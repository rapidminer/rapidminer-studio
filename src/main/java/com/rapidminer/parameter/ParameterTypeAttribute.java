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
package com.rapidminer.parameter;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;
import java.util.stream.Collectors;

import org.w3c.dom.Element;

import com.rapidminer.external.alphanum.AlphanumComparator;
import com.rapidminer.io.process.XMLTools;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.operator.ports.MetaDataChangeListener;
import com.rapidminer.operator.ports.metadata.AttributeMetaData;
import com.rapidminer.operator.ports.metadata.ExampleSetMetaData;
import com.rapidminer.operator.ports.metadata.MetaData;
import com.rapidminer.operator.ports.metadata.ModelMetaData;
import com.rapidminer.tools.Ontology;
import com.rapidminer.tools.XMLException;
import com.rapidminer.tools.container.Pair;


/**
 * This attribute type supports the user by let him select an attribute name from a combo box of
 * known attribute names. For long lists, auto completion and filtering of the drop down menu eases
 * the handling. For knowing attribute names before process execution a valid meta data
 * transformation must be performed. Otherwise the user might type in the name, instead of choosing.
 *
 * @author Sebastian Land
 */
public class ParameterTypeAttribute extends ParameterTypeString {

	/**
	 * A {@link MetaDataProvider} which provides metadata by querying the provided input port.
	 * It is used by the Web Client to retrieve input port data for attribute parameters.
	 *
	 */
	public static final class InputPortMetaDataProvider implements MetaDataProvider {

		private final InputPort inPort;

		private InputPortMetaDataProvider(InputPort inPort) {
			this.inPort = inPort;
		}

		@Override
		public MetaData getMetaData() {
			if (inPort != null) {
				return inPort.getMetaData();
			} else {
				return null;
			}
		}

		@Override
		public void addMetaDataChangeListener(MetaDataChangeListener l) {
			inPort.registerMetaDataChangeListener(l);
		}

		@Override
		public void removeMetaDataChangeListener(MetaDataChangeListener l) {
			inPort.removeMetaDataChangeListener(l);

		}

		public InputPort getInputPort() {
			return inPort;
		}
	}

	private static final long serialVersionUID = -4177652183651031337L;

	private static final String ELEMENT_ALLOWED_TYPES = "AllowedTypes";

	private static final String ELEMENT_ALLOWED_TYPE = "Type";

	// private transient InputPort inPort;
	private MetaDataProvider metaDataProvider;

	private int[] allowedValueTypes;

	public ParameterTypeAttribute(Element element) throws XMLException {
		super(element);

		allowedValueTypes = XMLTools.getChildTagsContentAsIntArray(
				XMLTools.getChildElement(element, ELEMENT_ALLOWED_TYPES, true), ELEMENT_ALLOWED_TYPE);
		// operator.getInputPorts().getPortByName(element.getAttribute(ATTRIBUTE_INPUT_PORT));
	}

	public ParameterTypeAttribute(final String key, String description, InputPort inPort) {
		this(key, description, inPort, false);
	}

	public ParameterTypeAttribute(final String key, String description, InputPort inPort, int... valueTypes) {
		this(key, description, inPort, false, valueTypes);
	}

	public ParameterTypeAttribute(final String key, String description, InputPort inPort, boolean optional) {
		this(key, description, inPort, optional, Ontology.ATTRIBUTE_VALUE);
	}

	public ParameterTypeAttribute(final String key, String description, InputPort inPort, boolean optional, boolean expert) {
		this(key, description, inPort, optional, Ontology.ATTRIBUTE_VALUE);
		setExpert(expert);
	}

	public ParameterTypeAttribute(final String key, String description, InputPort inPort, boolean optional, boolean expert,
			int... valueTypes) {
		this(key, description, inPort, optional, Ontology.ATTRIBUTE_VALUE);
		setExpert(expert);
		allowedValueTypes = valueTypes;
	}

	public ParameterTypeAttribute(final String key, String description, final InputPort inPort, boolean optional,
			int... valueTypes) {
		this(key, description, new InputPortMetaDataProvider(inPort), optional, valueTypes);
	}

	public ParameterTypeAttribute(final String key, String description, MetaDataProvider metaDataProvider, boolean optional,
			int... valueTypes) {
		super(key, description, optional);
		this.metaDataProvider = metaDataProvider;
		allowedValueTypes = valueTypes;
	}

	public Vector<String> getAttributeNames() {
		return getAttributeNamesAndTypes(true).stream().map(Pair::getFirst).collect(Collectors.toCollection(Vector::new));
	}

	/**
	 * Returns the attribute names and their value types, can sort if desired.
	 *
	 * @param sortAttributes
	 * 		if {@code true}, will sort alpha-numerically; if {@code false} will not sort at all
	 * @return the vector of pairs between the attribute names and their value type
	 * @since 9.2.0
	 */
	public List<Pair<String, Integer>> getAttributeNamesAndTypes(boolean sortAttributes) {
		List<Pair<String, Integer>> names = new ArrayList<>();
		List<Pair<String, Integer>> regularNames = new ArrayList<>();
		MetaData metaData = getMetaData();
		if (metaData == null) {
			return names;
		}
		ExampleSetMetaData emd = null;
		if (metaData instanceof ExampleSetMetaData) {
			emd = (ExampleSetMetaData) metaData;
		} else if (metaData instanceof ModelMetaData) {
			ModelMetaData mmd = (ModelMetaData) metaData;
			emd = mmd.getTrainingSetMetaData();
		}
		if (emd == null) {
			return names;
		}
		for (AttributeMetaData amd : emd.getAllAttributes()) {
			if (!isFilteredOut(amd) && isOfAllowedType(amd.getValueType())) {
				Pair<String, Integer> nameAndType = new Pair<>(amd.getName(), amd.getValueType());
				if (amd.isSpecial()) {
					names.add(nameAndType);
				} else {
					regularNames.add(nameAndType);
				}
			}
		}

		if (sortAttributes && (!names.isEmpty() || !regularNames.isEmpty())) {
			AlphanumComparator alphanumComparator = new AlphanumComparator(AlphanumComparator.AlphanumCaseSensitivity.INSENSITIVE);
			names.sort((o1, o2) -> alphanumComparator.compare(o1.getFirst(), o2.getFirst()));
			regularNames.sort((o1, o2) -> alphanumComparator.compare(o1.getFirst(), o2.getFirst()));
		}

		names.addAll(regularNames);

		return names;
	}

	/**
	 * Checks if the given value type is allowed for this parameter type, aka the type is identical or a subtype of an
	 * allowed type.
	 *
	 * @param valueType
	 * 		the value type that should be checked
	 * @return {@code true} if the given value type is allowed for this parameter type; {@code false} otherwise
	 * @since 9.2.0
	 */
	public boolean isOfAllowedType(int valueType) {
		boolean isAllowed = false;
		for (int type : allowedValueTypes) {
			isAllowed |= Ontology.ATTRIBUTE_VALUE_TYPE.isA(valueType, type);
		}
		return isAllowed;
	}

	@Override
	public Object getDefaultValue() {
		return "";
	}

	/**
	 * This method might be overridden by subclasses in order to select attributes which are
	 * applicable
	 */
	protected boolean isFilteredOut(AttributeMetaData amd) {
		return false;
	}

	// public InputPort getInputPort() {
	// return inPort;
	// }
	public MetaDataProvider getMetaDataProvider() {
		return metaDataProvider;
	}

	/** Returns the meta data currently available by the {@link #metaDataProvider}. */
	public MetaData getMetaData() {
		MetaData metaData = null;
		if (metaDataProvider != null) {
			metaData = metaDataProvider.getMetaData();
		}
		return metaData;
	}

	@Override
	protected void writeDefinitionToXML(Element typeElement) {
		super.writeDefinitionToXML(typeElement);

		// TODO: What was this for?
		// typeElement.setAttribute(ATTRIBUTE_INPUT_PORT, inPort.getName());
		Element allowedTypesElement = XMLTools.addTag(typeElement, ELEMENT_ALLOWED_TYPES);
		for (int allowedValueType : allowedValueTypes) {
			XMLTools.addTag(allowedTypesElement, ELEMENT_ALLOWED_TYPE, allowedValueType + "");
		}
	}
}
