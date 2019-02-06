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

	/**
	 * @deprecated use static creation method
	 *             {@link #create(ExampleSet, ExampleSet, boolean, boolean)} instead
	 */
	@Deprecated
	public RemappedExampleSet(ExampleSet parentSet, ExampleSet mappingSet) {
		this(parentSet, mappingSet, true);
	}

	/**
	 * @deprecated use static creation method
	 *             {@link #create(ExampleSet, ExampleSet, boolean, boolean)} instead
	 */
	@Deprecated
	public RemappedExampleSet(ExampleSet parentSet, ExampleSet _mappingSet, boolean keepAdditional) {
		this(parentSet, _mappingSet, keepAdditional, true);
	}

	/**
	 * Sorts the regular attributes of parentSet in the order of mappingSet. If additional
	 * attributes occur and keepAdditional is {@code true}, they are appended on the end of the
	 * example set. If transformMappings is {@code true} then mapping indices returned by
	 * {@link Example#getValue} are remapped to the mappings used in mappingSet and the attribute
	 * mappings are changed to those in mappingSet.
	 * <p>
	 * Note that {@link Example#getValueAsString} might not return the same as before the remapping
	 * but a missing value if not all strings in the old attribute mapping are part of the mapping
	 * from {@link #mappingSet}. Therefore, {@link RemappedExampleSet}s with transformMappings
	 * {@code true} should not be returned at ports unless those affected attributes are removed.
	 *
	 * @param parentSet
	 *            the example set that should be adjusted to the mapping set
	 * @param mappingSet
	 *            the example set to which we want to adjust the parent set
	 * @param keepAdditional
	 *            if {@code true} attributes from the parentSet that are not in the mappingSet are
	 *            kept
	 * @param transformMappings
	 *            if {@code true} an {@link AttributeTransformationRemapping} is added to the
	 *            nominal attributes of the adjusted example set so that {@code example.getValue(a)}
	 *            returns the mapping index according to the mapping of the attribute in mappingSet
	 *            and the nominal mapping of those attributes is adjusted
	 * @deprecated use static creation method
	 *             {@link #create(ExampleSet, ExampleSet, boolean, boolean)} instead
	 */
	@Deprecated
	public RemappedExampleSet(ExampleSet parentSet, ExampleSet mappingSet, boolean keepAdditional,
			boolean transformMappings) {
		this.parent = (ExampleSet) parentSet.clone();
		remap(mappingSet, keepAdditional, transformMappings, parent);
	}

	/**
	 * Creates a new example set with the regular attributes of parentSet sorted in the order of
	 * mappingSet. If additional attributes occur and keepAdditional is {@code true}, they are
	 * appended on the end of the example set. If transformMappings is {@code true} then mapping
	 * indices returned by {@link Example#getValue} are remapped to the mappings used in mappingSet
	 * and the attribute mappings are changed to those in mappingSet.
	 * <p>
	 * Note that {@link Example#getValueAsString} might not return the same as before the remapping
	 * but a missing value if not all strings in the old attribute mapping are part of the mapping
	 * from {@link #mappingSet}. Therefore, {@link RemappedExampleSet}s with transformMappings
	 * {@code true} should not be returned at ports unless those affected attributes are removed.
	 *
	 * @param parentSet
	 *            the example set that should be adjusted to the mapping set
	 * @param mappingSet
	 *            the example set to which we want to adjust the parent set
	 * @param keepAdditional
	 *            if {@code true} attributes from the parentSet that are not in the mappingSet are
	 *            kept
	 * @param transformMappings
	 *            if {@code true} an {@link AttributeTransformationRemapping} is added to the
	 *            nominal attributes of the adjusted example set so that {@code example.getValue(a)}
	 *            returns the mapping index according to the mapping of the attribute in mappingSet
	 *            and the nominal mapping of those attributes is adjusted
	 * @return a new example set based on parentSet remapped to mappingSet
	 * @since 7.5.1
	 */
	public static ExampleSet create(ExampleSet parentSet, ExampleSet mappingSet, boolean keepAdditional,
			boolean transformMappings) {
		ExampleSet newSet = (ExampleSet) parentSet.clone();
		remap(mappingSet, keepAdditional, transformMappings, newSet);
		return newSet;
	}

	/**
	 * Sorts the regular attributes of exampleSet in the order of mappingSet. If additional
	 * attributes occur and keepAdditional is {@code true}, they are appended on the end of the
	 * example set. If transformMappings is {@code true} then mapping indices returned by
	 * {@link Example#getValue} are remapped to the mappings used in mappingSet and the attribute
	 * mappings are changed to those in mappingSet.
	 * <p>
	 * Note that {@link Example#getValueAsString} might not return the same as before the remapping
	 * but a missing value if not all strings in the old attribute mapping are part of the mapping
	 * from {@link #mappingSet}. Therefore, {@link RemappedExampleSet}s with transformMappings
	 * {@code true} should not be returned at ports unless those affected attributes are removed.
	 *
	 * @param mappingSet
	 *            the set to which the example set should be adjusted
	 * @param keepAdditional
	 *            if {@code true} attributes from the parentSet that are not in the mappingSet are
	 *            kept
	 * @param transformMappings
	 *            if {@code true} an {@link AttributeTransformationRemapping} is added to the
	 *            nominal attributes of the adjusted example set so that {@code example.getValue(a)}
	 *            returns the mapping index according to the mapping of the attribute in mappingSet
	 *            and the nominal mapping of those attributes is adjusted
	 * @param exampleSet
	 *            the example set to adjust
	 */
	private static void remap(ExampleSet mappingSet, boolean keepAdditional, boolean transformMappings,
			ExampleSet exampleSet) {
		ExampleSet clonedMappingSet = (ExampleSet) mappingSet.clone();

		// check for a missing mappingSet because of compatibility
		if (clonedMappingSet != null) {
			Attributes attributes = exampleSet.getAttributes();

			// copying attributes into name map
			Map<String, Attribute> attributeMap = new LinkedHashMap<>(exampleSet.size());
			for (Attribute attribute : attributes) {
				attributeMap.put(attribute.getName(), attribute);
			}

			// clearing cloned set
			attributes.clearRegular();

			// adding again in mappingSets's order
			for (Attribute mapAttribute : clonedMappingSet.getAttributes()) {
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

			if (transformMappings) {
				// mapping nominal values
				Iterator<AttributeRole> a = exampleSet.getAttributes().allAttributeRoles();
				while (a.hasNext()) {
					AttributeRole role = a.next();
					Attribute currentAttribute = role.getAttribute();
					if (currentAttribute.isNominal()) {
						Attribute oldMappingAttribute = clonedMappingSet.getAttributes().get(role.getAttribute().getName());
						if (oldMappingAttribute != null && oldMappingAttribute.isNominal()) {
							NominalMapping currentMapping = currentAttribute.getMapping();
							NominalMapping overlayedMapping = oldMappingAttribute.getMapping();
							currentAttribute.setMapping(overlayedMapping);
							currentAttribute.addTransformation(new FullAttributeTransformationRemapping(currentMapping));
						}
					}
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

	@Override
	public void cleanup() {
		parent.cleanup();
	}
}
