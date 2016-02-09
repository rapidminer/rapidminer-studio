/**
 * Copyright (C) 2001-2016 by RapidMiner and the contributors
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

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.AttributeRole;
import com.rapidminer.example.Attributes;
import com.rapidminer.example.Example;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.table.ExampleTable;
import com.rapidminer.example.table.NominalMapping;
import com.rapidminer.operator.Annotations;


/**
 * This example set uses the mapping given by another example set and "remaps" on the fly the
 * nominal values according to the given set. It also sorts the regular attributes in the order of
 * the other exampleSet if possible. If additional attributes occur, they are appended on the end of
 * the example set, if keepAdditional is selected.
 * 
 * @author Ingo Mierswa, Sebastian Land
 */
public class RemappedExampleSet extends AbstractExampleSet {

	private static final long serialVersionUID = 3460640319989955936L;

	private ExampleSet parent;

	public RemappedExampleSet(ExampleSet parentSet, ExampleSet mappingSet) {
		this(parentSet, mappingSet, true);
	}

	public RemappedExampleSet(ExampleSet parentSet, ExampleSet _mappingSet, boolean keepAdditional) {
		this.parent = (ExampleSet) parentSet.clone();
		ExampleSet mappingSet = (ExampleSet) _mappingSet.clone();

		// check for a missing mappingSet because of compatibility
		if (mappingSet != null) {
			Attributes attributes = parent.getAttributes();

			// copying attributes into name map
			Map<String, Attribute> attributeMap = new LinkedHashMap<>(parent.size());
			for (Attribute attribute : attributes) {
				attributeMap.put(attribute.getName(), attribute);
			}

			// clearing cloned set
			attributes.clearRegular();

			// adding again in mappingSets's order
			for (Attribute mapAttribute : mappingSet.getAttributes()) {
				String name = mapAttribute.getName();
				Attribute attribute = attributeMap.get(name);
				if (attribute != null) {
					attributes.addRegular(attribute);
					attributeMap.remove(name);
				}
			}

			if (keepAdditional) {
				// adding all additional attributes
				for (Attribute attribute : attributeMap.values()) {
					attributes.addRegular(attribute);
				}
			}

			// mapping nominal values
			Iterator<AttributeRole> a = this.parent.getAttributes().allAttributeRoles();
			while (a.hasNext()) {
				AttributeRole role = a.next();
				Attribute currentAttribute = role.getAttribute();
				if (currentAttribute.isNominal()) {
					NominalMapping mapping = null;
					mapping = currentAttribute.getMapping();
					Attribute oldMappingAttribute = mappingSet.getAttributes().get(role.getAttribute().getName());
					if (oldMappingAttribute != null && oldMappingAttribute.isNominal()) {
						mapping = oldMappingAttribute.getMapping();
					}
					currentAttribute.addTransformation(new AttributeTransformationRemapping(mapping));
				}
			}
		}
	}

	/** Clone constructor. */
	public RemappedExampleSet(RemappedExampleSet other) {
		this.parent = (ExampleSet) other.parent.clone();
	}

	@Override
	public Attributes getAttributes() {
		return this.parent.getAttributes();
	}

	@Override
	public ExampleTable getExampleTable() {
		return parent.getExampleTable();
	}

	@Override
	public int size() {
		return parent.size();
	}

	@Override
	public Iterator<Example> iterator() {
		return new AttributesExampleReader(parent.iterator(), this);
	}

	@Override
	public Example getExample(int index) {
		return this.parent.getExample(index);
	}

	@Override
	public Annotations getAnnotations() {
		return parent.getAnnotations();
	}
}
