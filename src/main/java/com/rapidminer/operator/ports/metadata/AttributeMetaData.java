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

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.AbstractCollection;
import java.util.Date;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;

import com.rapidminer.RapidMiner;
import com.rapidminer.example.Attribute;
import com.rapidminer.example.AttributeRole;
import com.rapidminer.example.Attributes;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.Statistics;
import com.rapidminer.operator.Annotations;
import com.rapidminer.tools.Ontology;
import com.rapidminer.tools.ParameterService;
import com.rapidminer.tools.Tools;
import com.rapidminer.tools.math.container.Range;


/**
 * Meta data about an attribute
 *
 * @author Simon Fischer
 *
 */
public class AttributeMetaData implements Serializable {

	private static final long serialVersionUID = 1L;

	/** Threshold for the number of nominal values shown by {@link #getRangeString()} */
	private static final int MAX_DISPLAYED_NOMINAL_VALUES = 1000;

	private ExampleSetMetaData owner = null;

	private String name;

	private int type = Ontology.ATTRIBUTE_VALUE;
	private String role = null;
	private MDInteger numberOfMissingValues = new MDInteger(0);

	// it has to be ensured that the appropriate value set type is constructed anyway
	private SetRelation valueSetRelation = SetRelation.UNKNOWN;
	private Range valueRange = new Range(Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);
	private Set<String> valueSet = new TreeSet<String>();
	private String mode;

	private MDReal mean = new MDReal();

	private Annotations annotations = new Annotations();

	private boolean shrinkedValueSet = false;

	public AttributeMetaData(String name, int type) {
		this(name, type, null);
	}

	/**
	 * This will generate the complete meta data with all values.
	 */
	public AttributeMetaData(AttributeRole role, ExampleSet exampleSet) {
		this(role, exampleSet, false);
	}

	/**
	 * This will generate the attribute meta data with the data's values shortened. If shortened only
	 * the first 100 characters of each nominal value is returned.
	 */
	public AttributeMetaData(AttributeRole role, ExampleSet exampleSet, boolean shortened) {
		this(role.getAttribute().getName(), role.getAttribute().getValueType(), role.getSpecialName());
		Attribute att = role.getAttribute();
		if (att.isNominal()) {
			// always limit the value set
			int maxValues = getMaximumNumberOfNominalValues();
			valueSet.clear();
			valueSetRelation = SetRelation.EQUAL;
			for (String value : att.getMapping().getValues()) {
				if (value == null) {
					continue;
				}
				if (maxValues == 0) {
					valueSetRelation = SetRelation.SUPERSET;
					shrinkedValueSet = true;
					break;
				}
				if (shortened && value.length() > 100) {
					value = value.substring(0, 100);
				}
				valueSet.add(value);
				maxValues--;
			}
		}
		if (exampleSet != null) {
			numberOfMissingValues = new MDInteger((int) exampleSet.getStatistics(att, Statistics.UNKNOWN));
			if (att.isNumerical() || Ontology.ATTRIBUTE_VALUE_TYPE.isA(att.getValueType(), Ontology.DATE_TIME)) {
				valueSetRelation = SetRelation.EQUAL;
				valueRange = new Range(exampleSet.getStatistics(att, Statistics.MINIMUM), exampleSet.getStatistics(att,
						Statistics.MAXIMUM));
				setMean(new MDReal(exampleSet.getStatistics(att, Statistics.AVERAGE)));
			}
			if (att.isNominal()) {
				double modeIndex = exampleSet.getStatistics(att, Statistics.MODE);
				if (!Double.isNaN(modeIndex) && modeIndex >= 0 && modeIndex < att.getMapping().size()) {
					setMode(att.getMapping().mapIndex((int) modeIndex));
				}
			}
		} else {
			numberOfMissingValues = new MDInteger();
			if (att.isNumerical()) {
				setMean(new MDReal());
			}
			if (att.isNominal()) {
				setMode(null);
			}
		}
		this.annotations.putAll(att.getAnnotations());
	}

	public AttributeMetaData(String name, int type, String role) {
		this.name = name;
		this.type = type;
		this.role = role;
	}

	public AttributeMetaData(String name, String role, int nominalType, String... values) {
		this(name, role, values);
		this.type = nominalType;
	}

	public AttributeMetaData(String name, String role, String... values) {
		this.name = name;
		this.type = Ontology.NOMINAL;
		this.role = role;
		this.valueSetRelation = SetRelation.EQUAL;
		// always shrink the value set
		int maxValues = getMaximumNumberOfNominalValues();
		for (String string : values) {
			if (maxValues == 0) {
				this.valueSetRelation = SetRelation.SUPERSET;
				shrinkedValueSet = true;
				break;
			}
			valueSet.add(string);
			maxValues--;
		}
	}

	public AttributeMetaData(String name, String role, Range range) {
		this.name = name;
		this.role = role;
		this.type = Ontology.REAL;
		this.valueRange = range;
		this.valueSetRelation = SetRelation.EQUAL;
	}

	public AttributeMetaData(String name, String role, int type, Range range) {
		this(name, role, range);
		this.type = type;
	}

	private AttributeMetaData(AttributeMetaData attributeMetaData) {
		// must not keep references on mutable objects!
		this.name = attributeMetaData.name;
		this.role = attributeMetaData.role;
		this.type = attributeMetaData.type;
		this.numberOfMissingValues = new MDInteger(attributeMetaData.numberOfMissingValues);
		this.mean = new MDReal(attributeMetaData.mean);
		this.mode = attributeMetaData.mode;
		this.valueSetRelation = attributeMetaData.getValueSetRelation();
		this.valueRange = new Range(attributeMetaData.getValueRange());
		this.valueSet = new TreeSet<String>();
		this.annotations = new Annotations(attributeMetaData.annotations);
		valueSet.addAll(attributeMetaData.getValueSet());
		this.shrinkedValueSet = attributeMetaData.shrinkedValueSet;
	}

	public AttributeMetaData(Attribute attribute) {
		this.name = attribute.getName();
		this.type = attribute.getValueType();
		this.annotations.putAll(attribute.getAnnotations());
	}

	private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
		in.defaultReadObject();
		if (annotations == null) {
			annotations = new Annotations();
		}
	}

	public String getRole() {
		return role;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		String oldName = this.name;
		this.name = name;
		// informing ExampleSetMEtaData if one registered
		if (owner != null) {
			owner.attributeRenamed(this, oldName);
		}
	}

	public String getTypeName() {
		return Ontology.ATTRIBUTE_VALUE_TYPE.mapIndex(type);
	}

	public int getValueType() {
		return type;
	}

	/**
	 * If you change the type, keep in mind to set the value sets and their relation
	 */
	public void setType(int type) {
		if (Ontology.ATTRIBUTE_VALUE_TYPE.isA(type, Ontology.NUMERICAL)) {
			valueSet.clear();
		} else {
			setValueRange(new Range(Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY), SetRelation.SUBSET);
		}
		this.type = type;
	}

	@Override
	public String toString() {
		return getDescription();
	}

	public String getDescription() {
		StringBuilder buf = new StringBuilder();
		if (role != null && !role.equals(Attributes.ATTRIBUTE_NAME)) {
			buf.append("<em>");
			buf.append(role);
			buf.append("</em>: ");
		}
		buf.append(getName());
		buf.append(" (");
		buf.append(getValueTypeName());
		if (valueSetRelation != SetRelation.UNKNOWN) {
			buf.append(" in ");
			appendValueSetDescription(buf);
		} else {
			if (isNominal()) {
				buf.append(", values unkown");
			} else {
				buf.append(", range unknown");
			}
		}
		switch (containsMissingValues()) {
			case NO:
				buf.append("; no missing values");
				break;
			case YES:
				buf.append("; ");
				buf.append(numberOfMissingValues.toString());
				buf.append(" missing values");
				break;
			case UNKNOWN:
				buf.append("; may contain missing values");
				break;
		}
		buf.append(")");
		return buf.toString();
	}

	public String getValueTypeName() {
		return Ontology.ATTRIBUTE_VALUE_TYPE.mapIndex(getValueType());
	}

	public String getValueSetDescription() {
		StringBuilder buf = new StringBuilder();
		appendValueSetDescription(buf);
		return buf.toString();
	}

	private void appendValueSetDescription(StringBuilder buf) {
		if (isNominal()) {
			buf.append(valueSetRelation + " {");
			boolean first = true;
			String mode = getMode();
			int index = 0;
			for (String value : valueSet) {
				index++;
				if (first) {
					first = false;
				} else {
					buf.append(", ");
				}

				if (index >= 10) {
					buf.append("...");
					break;
				}

				boolean isMode = value.equals(mode);
				if (isMode) {
					buf.append("<span style=\"text-decoration:underline\">");
				}
				buf.append(Tools.escapeHTML(value));
				if (isMode) {
					buf.append("</span>");
				}
			}
			buf.append("}");
		}
		if (isNumerical()) {
			buf.append(valueSetRelation + " [");
			if (getValueRange() != null) {
				buf.append(Tools.formatNumber(getValueRange().getLower(), 3));
				buf.append("...");
				buf.append(Tools.formatNumber(getValueRange().getUpper(), 3));
				buf.append("]");
			}
			if (getMean().isKnown()) {
				buf.append("; mean ");
				buf.append(getMean().toString());
			}
		}
		if (valueRange != null && Ontology.ATTRIBUTE_VALUE_TYPE.isA(getValueType(), Ontology.DATE_TIME)
				&& !Double.isInfinite(getValueRange().getLower()) && !Double.isInfinite(getValueRange().getUpper())) {
			buf.append(valueSetRelation + " [");
			switch (getValueType()) {
				case Ontology.DATE:
					buf.append(Tools.formatDate(new Date((long) getValueRange().getLower())));
					buf.append("...");
					buf.append(Tools.formatDate(new Date((long) getValueRange().getUpper())));
					buf.append("]");
					break;
				case Ontology.TIME:
					buf.append(Tools.formatTime(new Date((long) getValueRange().getLower())));
					buf.append("...");
					buf.append(Tools.formatTime(new Date((long) getValueRange().getUpper())));
					buf.append("]");
					break;
				case Ontology.DATE_TIME:
					buf.append(Tools.formatDateTime(new Date((long) getValueRange().getLower())));
					buf.append("...");
					buf.append(Tools.formatDateTime(new Date((long) getValueRange().getUpper())));
					buf.append("]");
					break;
			}
		}
	}

	protected String getDescriptionAsTableRow() {
		StringBuilder b = new StringBuilder();
		b.append("<tr><td>");
		String role2 = getRole();
		if (role2 == null) {
			role2 = "-";
		}
		b.append(role2).append("</td><td>");
		b.append(Tools.escapeHTML(getName()));
		String unit = getAnnotations().getAnnotation(Annotations.KEY_UNIT);
		if (unit != null) {
			b.append(" <em>[").append(unit).append("]</em>");
		}
		b.append("</td><td>");
		b.append(getValueTypeName()).append("</td><td>");

		if (valueSetRelation != SetRelation.UNKNOWN) {
			appendValueSetDescription(b);
		} else {
			if (isNominal()) {
				b.append("values unkown");
			} else {
				b.append("range unknown");
			}
		}
		b.append("</td><td>");

		switch (containsMissingValues()) {
			case NO:
				b.append("no missing values");
				break;
			case YES:
				b.append(numberOfMissingValues.toString());
				b.append(" missing values");
				break;
			case UNKNOWN:
				b.append("may contain missing values");
				break;
		}

		final String comment = getAnnotations().getAnnotation(Annotations.KEY_COMMENT);
		b.append("</td><td>").append(comment != null ? comment : "-").append("</tr></tr>");
		return b.toString();
	}

	@Override
	public AttributeMetaData clone() {
		return new AttributeMetaData(this);
	}

	public boolean isNominal() {
		return Ontology.ATTRIBUTE_VALUE_TYPE.isA(type, Ontology.NOMINAL);
	}

	public boolean isBinominal() {
		return Ontology.ATTRIBUTE_VALUE_TYPE.isA(type, Ontology.BINOMINAL);
	}

	public boolean isPolynominal() {
		return Ontology.ATTRIBUTE_VALUE_TYPE.isA(type, Ontology.POLYNOMINAL);
	}

	public boolean isNumerical() {
		return Ontology.ATTRIBUTE_VALUE_TYPE.isA(type, Ontology.NUMERICAL);
	}

	/**
	 * @return {@code true} if the type of the attribute is a {@link Ontology#DATE_TIME} (and all
	 *         it's subtypes), {@code false} otherwise
	 * 
	 * @since 6.4.0
	 */
	public boolean isDateTime() {
		return Ontology.ATTRIBUTE_VALUE_TYPE.isA(type, Ontology.DATE_TIME);
	}

	public MetaDataInfo containsMissingValues() {
		return numberOfMissingValues.isAtLeast(1);
	}

	public void setNumberOfMissingValues(MDInteger numberOfMissingValues) {
		this.numberOfMissingValues = numberOfMissingValues;
	}

	public MDInteger getNumberOfMissingValues() {
		return this.numberOfMissingValues;
	}

	public SetRelation getValueSetRelation() {
		return valueSetRelation;
	}

	public Set<String> getValueSet() {
		return valueSet;
	}

	public void setValueSet(Set<String> valueSet, SetRelation relation) {
		this.valueSetRelation = relation;
		this.valueSet = valueSet;
		shrinkValueSet();
	}

	public Range getValueRange() {
		return valueRange;
	}

	public void setValueRange(Range range, SetRelation relation) {
		this.valueSetRelation = relation;
		this.valueRange = range;
	}

	public AttributeMetaData copy() {
		return new AttributeMetaData(this);
	}

	/**
	 * Sets the role of this attribute. The name is equivalent with the names from Attributes. To
	 * reset use null as parameter.
	 */
	public void setRole(String role) {
		this.role = role;
	}

	public void setRegular() {
		this.role = null;
	}

	public boolean isSpecial() {
		return role != null;
	}

	/**
	 * This method returns a AttributeMetaData object for the prediction attribute created on
	 * applying a model on an exampleset with the given label.
	 */
	public static AttributeMetaData createPredictionMetaData(AttributeMetaData labelMetaData) {
		AttributeMetaData result = labelMetaData.clone();
		result.setName("prediction(" + result.getName() + ")");
		result.setRole(Attributes.PREDICTION_NAME);
		return result;
	}

	/**
	 * This method creates the attribute meta data for the confidence attributes in the given
	 * exampleSetMetaData. If the values are not known precisely the attributeSet relation of the
	 * exampleSetMetaData object is set appropriate.
	 *
	 * @return
	 */
	public static ExampleSetMetaData createConfidenceAttributeMetaData(ExampleSetMetaData exampleSetMD) {
		if (exampleSetMD.hasSpecial(Attributes.LABEL_NAME) == MetaDataInfo.YES) {
			AttributeMetaData labelMetaData = exampleSetMD.getLabelMetaData();
			if (labelMetaData.isNominal()) {
				for (String value : labelMetaData.getValueSet()) {
					AttributeMetaData conf = new AttributeMetaData(Attributes.CONFIDENCE_NAME + "_" + value, Ontology.REAL,
							Attributes.CONFIDENCE_NAME);
					conf.setValueRange(new Range(0d, 1d), SetRelation.EQUAL);
					exampleSetMD.addAttribute(conf);
				}
				// setting attribute set relation according to value set relation
				exampleSetMD.mergeSetRelation(labelMetaData.getValueSetRelation());
				return exampleSetMD;
			}
		}
		return exampleSetMD;
	}

	public void setValueSetRelation(SetRelation valueSetRelation) {
		this.valueSetRelation = valueSetRelation;
	}

	public void setMean(MDReal mean) {
		this.mean = mean;
	}

	public MDReal getMean() {
		return mean;
	}

	public void setMode(String mode) {
		this.mode = mode;
	}

	public String getMode() {
		return mode;
	}

	/** Sets types and ranges to the superset of this and the argument. */
	public void merge(AttributeMetaData amd) {
		if (amd.isNominal() != this.isNominal()) {
			this.type = Ontology.ATTRIBUTE_VALUE;
		}
		if (isNominal()) {
			if (amd.valueSet != null && this.valueSet != null) {
				if (!amd.valueSet.equals(this.valueSet)) {
					this.valueSetRelation = this.valueSetRelation.merge(SetRelation.SUBSET);
				}
				this.valueSet.addAll(amd.valueSet);
			}
			this.valueSetRelation = this.valueSetRelation.merge(amd.valueSetRelation);
		}
		if (isNumerical()) {
			if (valueRange != null && amd.valueRange != null) {
				double min = Math.min(amd.valueRange.getLower(), this.valueRange.getLower());
				double max = Math.max(amd.valueRange.getUpper(), this.valueRange.getUpper());
				this.valueRange = new Range(min, max);
			}
			this.valueSetRelation = this.valueSetRelation.merge(amd.valueSetRelation);
		}
	}

	/** Returns either the value range or the value set, depending on the type of attribute. */
	public String getRangeString() {

		if (Ontology.ATTRIBUTE_VALUE_TYPE.isA(getValueType(), Ontology.DATE_TIME)) {
			if (!Double.isInfinite(getValueRange().getLower()) && !Double.isInfinite(getValueRange().getUpper())) {
				StringBuilder buf = new StringBuilder();
				buf.append(valueSetRelation.toString());
				if (valueSetRelation != SetRelation.UNKNOWN) {
					buf.append("[");
					switch (getValueType()) {
						case Ontology.DATE:
							buf.append(Tools.formatDate(new Date((long) getValueRange().getLower())));
							buf.append(" \u2013 ");
							buf.append(Tools.formatDate(new Date((long) getValueRange().getUpper())));
							break;
						case Ontology.TIME:
							buf.append(Tools.formatTime(new Date((long) getValueRange().getLower())));
							buf.append(" \u2013 ");
							buf.append(Tools.formatTime(new Date((long) getValueRange().getUpper())));
							break;
						case Ontology.DATE_TIME:
							buf.append(Tools.formatDateTime(new Date((long) getValueRange().getLower())));
							buf.append(" \u2013 ");
							buf.append(Tools.formatDateTime(new Date((long) getValueRange().getUpper())));
							break;
					}
					buf.append("]");
					return buf.toString();
				} else {
					return "Unknown date range";
				}
			}
			return "Unbounded date range";
		} else if (!isNominal() && valueRange != null) {
			return valueSetRelation.toString() + (valueSetRelation != SetRelation.UNKNOWN ? valueRange.toString() : "");
		} else if (isNominal() && valueSet != null) {
			return valueSetRelation.toString() + (valueSetRelation != SetRelation.UNKNOWN ? setToString(valueSet) : "");
		} else {
			return "unknown";
		}
	}


	/**
	 * Converts String set into String analogously to {@link AbstractCollection#toString()}, but with a maximum number
	 * of entries. This is necessary, because otherwise the UI can freeze when this String is calculated repeatedly.
	 * Note that the maximum number of entries cannot be determined by {@link #getMaximumNumberOfNominalValues()}
	 * because we do not want our UI to freeze if the user sets this too high.
	 *
	 * @param set
	 * 		the set which should be converted to a String
	 * @return a String with at most {@link #MAX_DISPLAYED_NOMINAL_VALUES} values of the set
	 */
	private String setToString(Set<String> set) {
		if (set.isEmpty()) {
			return "[]";
		}

		StringBuilder sb = new StringBuilder();
		sb.append('[');
		int size = set.size();
		int shownValues = Math.min(size, MAX_DISPLAYED_NOMINAL_VALUES);
		Iterator<String> it = set.iterator();
		for (int i = 0; i < shownValues - 1; i++) {
			String e = it.next();
			sb.append(e);
			sb.append(',').append(' ');
		}
		if (shownValues < size) {
			sb.append("...");
		} else {
			sb.append(it.next());
		}
		return sb.append(']').toString();
	}

	/**
	 * Throws away nominal values until the value set size is at most the value specified by
	 * property {@link RapidMiner#PROPERTY_RAPIDMINER_GENERAL_MAX_NOMINAL_VALUES}.
	 */
	public void shrinkValueSet() {
		int maxSize = getMaximumNumberOfNominalValues();
		shrinkValueSet(maxSize);
	}

	/**
	 * Returns the maximum number of values to be used for meta data generation as specified by
	 * {@link RapidMiner#PROPERTY_RAPIDMINER_GENERAL_MAX_NOMINAL_VALUES}.
	 */
	public static int getMaximumNumberOfNominalValues() {
		int maxSize = 100;
		String maxSizeString = ParameterService.getParameterValue(RapidMiner.PROPERTY_RAPIDMINER_GENERAL_MAX_NOMINAL_VALUES);
		if (maxSizeString != null) {
			maxSize = Integer.parseInt(maxSizeString);
			if (maxSize == 0) {
				maxSize = Integer.MAX_VALUE;
			}
		}
		return maxSize;
	}

	/** Throws away nominal values until the value set size is at most the given value. */
	private void shrinkValueSet(int maxSize) {
		if (valueSet != null) {
			if (valueSet.size() > maxSize) {
				Set<String> newSet = new TreeSet<>();
				Iterator<String> i = valueSet.iterator();
				int count = 0;
				while (i.hasNext() && count < maxSize) {
					newSet.add(i.next());
					count++;
				}
				this.valueSet = newSet;
				valueSetRelation = valueSetRelation.merge(SetRelation.SUPERSET);
				shrinkedValueSet = true;

				if (owner != null) {
					owner.setNominalDataWasShrinked(true);
				}
			}

		}
	}

	/**
	 * This method is only to be used by ExampleSetMetaData to register as owner of this
	 * attributeMetaData. Returnes is this object or a clone if this object already has an owner.
	 */
	/* pp */AttributeMetaData registerOwner(ExampleSetMetaData owner) {
		if (this.owner == null) {
			this.owner = owner;
			owner.setNominalDataWasShrinked(shrinkedValueSet);
			return this;
		} else {
			AttributeMetaData clone = this.clone();
			clone.owner = owner;
			owner.setNominalDataWasShrinked(clone.shrinkedValueSet);
			return clone;
		}
	}

	public void setAnnotations(Annotations annotations) {
		if (annotations == null) {
			this.annotations = new Annotations();
		} else {
			this.annotations = annotations;
		}
	}

	public Annotations getAnnotations() {
		return annotations;
	}
}
