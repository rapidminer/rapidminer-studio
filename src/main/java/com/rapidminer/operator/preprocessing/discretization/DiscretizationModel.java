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
package com.rapidminer.operator.preprocessing.discretization;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.AttributeRole;
import com.rapidminer.example.Attributes;
import com.rapidminer.example.Example;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.SimpleAttributes;
import com.rapidminer.example.table.AttributeFactory;
import com.rapidminer.example.table.ExampleTable;
import com.rapidminer.example.table.NominalMapping;
import com.rapidminer.example.table.PolynominalMapping;
import com.rapidminer.example.table.ViewAttribute;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.OperatorProgress;
import com.rapidminer.operator.UserError;
import com.rapidminer.operator.preprocessing.PreprocessingModel;
import com.rapidminer.tools.Ontology;
import com.rapidminer.tools.Tools;
import com.rapidminer.tools.container.Tupel;


/**
 * The generic discretization model.
 *
 * @author Sebastian Land
 */
public class DiscretizationModel extends PreprocessingModel {

	private static final long serialVersionUID = -8732346419946567062L;

	private static final int OPERATOR_PROGRESS_STEPS = 100;

	private Map<String, SortedSet<Tupel<Double, String>>> rangesMap;

	private Set<String> attributeNames;

	private boolean removeUseless = true;

	public static final String[] RANGE_NAME_TYPES = { "long", "short", "interval" };

	public static final int RANGE_NAME_LONG = 0;
	public static final int RANGE_NAME_SHORT = 1;
	public static final int RANGE_NAME_INTERVAL = 2;

	public DiscretizationModel(ExampleSet exampleSet) {
		this(exampleSet, false);
	}

	public DiscretizationModel(ExampleSet exampleSet, boolean removeUseless) {
		super(exampleSet);
		attributeNames = new HashSet<String>();
		for (Attribute attribute : exampleSet.getAttributes()) {
			if (attribute.isNumerical()) {
				attributeNames.add(attribute.getName());
			}
		}
		this.removeUseless = removeUseless;
	}

	@Override
	public ExampleSet applyOnData(ExampleSet exampleSet) throws OperatorException {
		// creating new nominal attributes for numerical ones
		Map<Attribute, Attribute> replacementMap = new LinkedHashMap<Attribute, Attribute>();
		Map<String, AttributeRole> replacementRoleMap = new HashMap<String, AttributeRole>();
		ExampleTable table = exampleSet.getExampleTable();
		Attributes attributes = exampleSet.getAttributes();
		Iterator<Attribute> iterator = attributes.allAttributes();
		while (iterator.hasNext()) {
			Attribute attribute = iterator.next();
			if (attribute.isNumerical() && attributeNames.contains(attribute.getName())) {
				Attribute newAttribute = AttributeFactory.createAttribute(Ontology.NOMINAL);
				replacementMap.put(attribute, newAttribute);
				AttributeRole role = attributes.getRole(attribute);
				if (role != null) {
					replacementRoleMap.put(attribute.getName(), role);
				}
			}
		}
		Set<Entry<Attribute, Attribute>> replacements = replacementMap.entrySet();

		// initialize progress
		OperatorProgress progress = null;
		if (getShowProgress() && getOperator() != null && getOperator().getProgress() != null) {
			progress = getOperator().getProgress();
			progress.setTotal(100);
		}

		// creating mapping and adding to table and exampleSet
		long progressCounter = 0;
		for (Entry<Attribute, Attribute> replacement : replacements) {
			SortedSet<Tupel<Double, String>> ranges = rangesMap.get(replacement.getKey().getName());
			Attribute newAttribute = replacement.getValue();
			table.addAttribute(newAttribute);
			exampleSet.getAttributes().addRegular(newAttribute);
			if (ranges != null) {
				for (Tupel<Double, String> rangePair : ranges) {
					newAttribute.getMapping().mapString(rangePair.getSecond());
				}
			}
			if (progress != null && ++progressCounter % OPERATOR_PROGRESS_STEPS == 0) {
				progress.setCompleted((int) (50.0d * progressCounter / replacements.size()));
			}
		}

		// copying data
		progressCounter = 0;
		long totalProgress = (long) replacements.size() * exampleSet.size();
		for (Entry<Attribute, Attribute> replacement : replacements) {
			for (Example example : exampleSet) {
				Attribute originalAttribute = replacement.getKey();
				Attribute newAttribute = replacement.getValue();
				SortedSet<Tupel<Double, String>> ranges = rangesMap.get(originalAttribute.getName());
				if (ranges != null) {
					double value = example.getValue(originalAttribute);
					if (!Double.isNaN(value)) {
						int b = 0;
						for (Tupel<Double, String> rangePair : ranges) {
							if (value <= rangePair.getFirst().doubleValue()) {
								example.setValue(newAttribute, b);
								break;
							}
							b++;
						}
					} else {
						example.setValue(newAttribute, Double.NaN);
					}
				}
				if (progress != null && ++progressCounter % OPERATOR_PROGRESS_STEPS == 0) {
					progress.setCompleted((int) (50.0d * progressCounter / totalProgress + 50));
				}
			}
		}

		// removing old attributes and assigning final names and role to new
		for (Map.Entry<Attribute, Attribute> entry : replacementMap.entrySet()) {
			Attribute oldAttribute = entry.getKey();
			Attribute newAttribute = entry.getValue();
			String name = oldAttribute.getName();
			exampleSet.getAttributes().remove(oldAttribute);
			if (replacementRoleMap.containsKey(name)) {
				exampleSet.getAttributes().getRole(newAttribute).setSpecial(replacementRoleMap.get(name).getSpecialName());
			}
			newAttribute.setName(name);
		}

		// removing useless nominal attributes
		if (removeUseless) {
			iterator = exampleSet.getAttributes().iterator();
			while (iterator.hasNext()) {
				Attribute attribute = iterator.next();
				if (attribute.isNominal()) {
					if (attribute.getMapping().size() < 2) {
						iterator.remove();
					}
				}
			}
		}
		return exampleSet;
	}

	/**
	 * Creates the ranges. If the range name type is 'Interval' and the number of digits is smaller
	 * than 0, the number of digits is automatically determined in a way such that the range names
	 * do actually differ but are rounded as far as possible.
	 */
	public void setRanges(Map<Attribute, double[]> rangesMap, String rangeName, int rangeNameType, int numberOfDigits)
			throws UserError {
		this.rangesMap = new HashMap<String, SortedSet<Tupel<Double, String>>>();
		Iterator<Map.Entry<Attribute, double[]>> r = rangesMap.entrySet().iterator();
		while (r.hasNext()) {
			Map.Entry<Attribute, double[]> entry = r.next();
			Attribute attribute = entry.getKey();
			double[] limits = entry.getValue();

			TreeSet<Tupel<Double, String>> ranges = null;
			boolean valid = true;
			if (rangeNameType == RANGE_NAME_INTERVAL) {
				int startNumberOfDigits = numberOfDigits;
				if (startNumberOfDigits <= 0) {
					startNumberOfDigits = 1;
				}

				for (int n = startNumberOfDigits; n < 30; n++) {
					valid = true;
					ranges = createRanges(limits, rangeName, rangeNameType, n);
					String lastTupel = null;
					for (Tupel<Double, String> t : ranges) {
						if (lastTupel != null) {
							if (lastTupel.equals(t.getSecond())) {
								valid = false;
								break;
							}
						}
						String first = t.getSecond().substring(1, t.getSecond().indexOf(" - "));
						String second = t.getSecond().substring(t.getSecond().indexOf(" - ") + " - ".length(),
								t.getSecond().length() - 1);
						if (first.equals(second)) {
							valid = false;
							break;
						}
						lastTupel = t.getSecond();
					}
					if (valid) {
						break;
					}
				}

				if (!valid) {
					throw new UserError(null, 938);
				}
			} else {
				if (numberOfDigits > 0) {
					ranges = createRanges(limits, rangeName, rangeNameType, numberOfDigits);
				} else {
					ranges = createRanges(limits, rangeName, rangeNameType, 3);
				}
			}

			this.rangesMap.put(attribute.getName(), ranges);
		}
	}

	private TreeSet<Tupel<Double, String>> createRanges(double[] entry, String rangeBaseName, int rangeNameType,
			int numberOfDigits) {
		TreeSet<Tupel<Double, String>> ranges = new TreeSet<Tupel<Double, String>>();
		int i = 1;
		double lastLimit = Double.NEGATIVE_INFINITY;
		for (double rangeValue : entry) {
			String usedRangeName = null;
			switch (rangeNameType) {
				case RANGE_NAME_LONG:
					usedRangeName = rangeBaseName + i + " [" + Tools.formatIntegerIfPossible(lastLimit) + " - "
							+ Tools.formatIntegerIfPossible(rangeValue) + "]";
					break;
				case RANGE_NAME_SHORT:
					usedRangeName = rangeBaseName + i;
					break;
				case RANGE_NAME_INTERVAL:
					usedRangeName = "[" + Tools.formatNumber(lastLimit, numberOfDigits) + " - "
							+ Tools.formatNumber(rangeValue, numberOfDigits) + "]";
					break;
			}

			ranges.add(new Tupel<Double, String>(rangeValue, usedRangeName));
			i++;
			lastLimit = rangeValue;
		}
		return ranges;
	}

	public void setRanges(Map<String, SortedSet<Tupel<Double, String>>> rangesMap) {
		this.rangesMap = rangesMap;
	}

	public Map<String, SortedSet<Tupel<Double, String>>> getRanges() {
		return this.rangesMap;
	}

	@Override
	public String toString() {
		StringBuffer buffer = new StringBuffer();
		for (String attributeName : rangesMap.keySet()) {
			buffer.append(Tools.getLineSeparator());
			buffer.append(Tools.getLineSeparator());
			buffer.append(attributeName);
			buffer.append(Tools.getLineSeparator());
			SortedSet<Tupel<Double, String>> set = rangesMap.get(attributeName);
			buffer.append(Double.NEGATIVE_INFINITY);
			for (Tupel<Double, String> tupel : set) {
				buffer.append(" < " + tupel.getSecond() + " <= " + tupel.getFirst());
			}
		}
		return buffer.toString();
	}

	@Override
	public Attributes getTargetAttributes(ExampleSet parentSet) {
		SimpleAttributes attributes = new SimpleAttributes();
		// add special attributes to new attributes
		Iterator<AttributeRole> specialRoles = parentSet.getAttributes().specialAttributes();
		while (specialRoles.hasNext()) {
			attributes.add(specialRoles.next());
		}

		// add regular attributes
		for (Attribute attribute : parentSet.getAttributes()) {
			if (!attribute.isNumerical() || !attributeNames.contains(attribute.getName())) {
				attributes.addRegular(attribute);
			} else {
				// create nominal mapping
				SortedSet<Tupel<Double, String>> ranges = rangesMap.get(attribute.getName());
				if (ranges.size() > 1) {
					NominalMapping mapping = new PolynominalMapping();
					for (Tupel<Double, String> rangePair : ranges) {
						mapping.mapString(rangePair.getSecond());
					}
					// giving new attributes old name: connection to rangesMap
					attributes.addRegular(
							new ViewAttribute(this, attribute, attribute.getName(), Ontology.POLYNOMINAL, mapping));
				}
			}
		}
		return attributes;
	}

	@Override
	public double getValue(Attribute targetAttribute, double value) {
		SortedSet<Tupel<Double, String>> ranges = rangesMap.get(targetAttribute.getName());
		if (ranges != null) {
			int b = 0;
			for (Tupel<Double, String> rangePair : ranges) {
				if (value <= rangePair.getFirst().doubleValue()) {
					return b;
				}
				b++;
			}
			return Double.NaN;
		} else {
			return value;
		}
	}

	public Set<String> getAttributeNames() {
		return attributeNames;
	}

	public boolean shouldRemoveUseless() {
		return removeUseless;
	}

	@Override
	protected boolean needsRemapping() {
		return false;
	}
}
