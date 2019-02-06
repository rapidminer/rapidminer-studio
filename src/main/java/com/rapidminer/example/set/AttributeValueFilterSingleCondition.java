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

import com.rapidminer.example.Attribute;
import com.rapidminer.example.Example;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.tools.Tools;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.regex.PatternSyntaxException;


/**
 * The condition is fulfilled if an attribute has a value equal to, not equal to, less than, ... a
 * given value.
 * 
 * @author Ingo Mierswa, Nils Woehler
 */
public class AttributeValueFilterSingleCondition implements Condition {

	private static final long serialVersionUID = 1537763901048986863L;

	private static final String[] COMPARISON_TYPES = { "<=", ">=", "!=", "<>", "=", "<", ">" };

	private static final String MISSING_ENCODING = "\\?";

	public static final int LEQ = 0;

	public static final int GEQ = 1;

	public static final int NEQ1 = 2;

	public static final int NEQ2 = 3;

	public static final int EQUALS = 4;

	public static final int LESS = 5;

	public static final int GREATER = 6;

	public static String DATE_PATTERN = "yyyy-MM-dd HH:mm:ss Z";

	private int comparisonType = EQUALS;

	private Attribute attribute;

	private double numericalValue;

	private String nominalValue;

	private Date dateValue;

	private HashSet<Integer> allowedNominalValueIndices;
	private boolean isMissingAllowed = false;

	/**
	 * Creates a new AttributeValueFilter. If attribute is not nominal, value must be either a
	 * number or a date string.
	 */
	public AttributeValueFilterSingleCondition(Attribute attribute, int comparisonType, String value) {
		this.attribute = attribute;
		this.comparisonType = comparisonType;
		setValue(value);
	}

	/**
	 * Constructs an AttributeValueFilter for a given {@link ExampleSet} from a parameter string
	 * 
	 * @param parameterString
	 *            Must be of the form attribute R value, where R is one out of =, !=, &lt&, &gt;,
	 *            &lt;=, and &gt;=.
	 */
	public AttributeValueFilterSingleCondition(ExampleSet exampleSet, String parameterString) {
		if ((parameterString == null) || (parameterString.length() == 0)) {
			throw new IllegalArgumentException("Parameter string must not be empty!");
		}

		int compIndex = -1;
		for (comparisonType = 0; comparisonType < COMPARISON_TYPES.length; comparisonType++) {
			compIndex = parameterString.indexOf(COMPARISON_TYPES[comparisonType]);
			if (compIndex != -1) {
				break;
			}
		}
		if (compIndex == -1) {
			throw new IllegalArgumentException("Parameter string must have the form 'attribute {=|<|>|<=|>=|!=} value'");
		}
		String attName = parameterString.substring(0, compIndex).trim();
		String valueStr = parameterString.substring(compIndex + COMPARISON_TYPES[comparisonType].length()).trim();
		if ((attName.length() == 0) || (valueStr.length() == 0)) {
			throw new IllegalArgumentException("Parameter string must have the form 'attribute {=|<|>|<=|>=|!=} value'");
		}

		this.attribute = exampleSet.getAttributes().get(attName);

		if (this.attribute == null) {
			throw new IllegalArgumentException("Unknown attribute: '" + attName + "'");
		}
		setValue(valueStr);
	}

	private void setValue(String value) {
		if (attribute.isNominal()) {
			if ((comparisonType != EQUALS) && (comparisonType != NEQ1 && comparisonType != NEQ2)) {
				throw new IllegalArgumentException("For nominal attributes only '=' and '!=' or '<>' is allowed!");
			}
			this.nominalValue = value;
			// Check if this string is equal to missing
			this.isMissingAllowed = nominalValue.equals(MISSING_ENCODING);

			this.allowedNominalValueIndices = new HashSet<Integer>(attribute.getMapping().size());
			for (String attributeValue : attribute.getMapping().getValues()) {
				try {
					if (attributeValue.equals(nominalValue) || attributeValue.matches(nominalValue)) {
						allowedNominalValueIndices.add(attribute.getMapping().mapString(attributeValue));
					}
				} catch (PatternSyntaxException e) {
				}
			}
		} else if (attribute.isNumerical()) {
			if (value.equals("?")) {
				this.numericalValue = Double.NaN;
			} else {
				try {
					this.numericalValue = Double.parseDouble(value);
				} catch (NumberFormatException e) {
					throw new IllegalArgumentException("Value for attribute '" + attribute.getName()
							+ "' must be numerical, but was '" + value + "'!");
				}
			}
		} else { // date
			SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_PATTERN);
			try {
				if (value.equals("?")) {
					this.dateValue = null;
				} else {
					this.dateValue = dateFormat.parse(value);
				}
			} catch (ParseException e) {
				throw new IllegalArgumentException("Could not parse value '" + value + "' with date pattern " + DATE_PATTERN);
			}
		}
	}

	/**
	 * Since the condition cannot be altered after creation we can just return the condition object
	 * itself.
	 * 
	 * @deprecated Conditions should not be able to be changed dynamically and hence there is no
	 *             need for a copy
	 */
	@Override
	@Deprecated
	public Condition duplicate() {
		return this;
	}

	@Override
	public String toString() {
		return attribute.getName() + " " + COMPARISON_TYPES[comparisonType] + " "
				+ (attribute.isNominal() ? nominalValue : "" + numericalValue);
	}

	/**
	 * Returns true if the condition is fulfilled for the given example. Comparisons with NaN and
	 * <code>null</code> return <code>false</code>.
	 */
	@Override
	public boolean conditionOk(Example e) {
		if (attribute.isNominal()) {
			double doubleValue = e.getValue(attribute);
			if (Double.isNaN(doubleValue)) {
				switch (comparisonType) {
					case NEQ1:
					case NEQ2:
						return !isMissingAllowed;
					case EQUALS:
						return isMissingAllowed;
					default:
						return false;
				}
			} else {
				int value = (int) doubleValue;
				switch (comparisonType) {
					case NEQ1:
					case NEQ2:
						return !allowedNominalValueIndices.contains(value);
					case EQUALS:
						return allowedNominalValueIndices.contains(value);
					default:
						return false;
				}
			}
		} else if (attribute.isNumerical()) {
			switch (comparisonType) {
				case LEQ:
					return Tools.isLessEqual(e.getNumericalValue(attribute), numericalValue);
				case GEQ:
					return Tools.isGreaterEqual(e.getNumericalValue(attribute), numericalValue);
				case NEQ1:
				case NEQ2:
					return Tools.isNotEqual(e.getNumericalValue(attribute), numericalValue);
				case EQUALS:
					return Tools.isEqual(e.getNumericalValue(attribute), numericalValue);
				case LESS:
					return Tools.isLess(e.getNumericalValue(attribute), numericalValue);
				case GREATER:
					return Tools.isGreater(e.getNumericalValue(attribute), numericalValue);
				default:
					return false;
			}
		} else { // date
			Date currentDateValue;
			if (Double.isNaN(e.getValue(attribute))) {
				currentDateValue = null;
			} else {
				currentDateValue = e.getDateValue(attribute);
			}
			switch (comparisonType) {
				case LEQ:
					return Tools.isLessEqual(currentDateValue, dateValue);
				case GEQ:
					return Tools.isGreaterEqual(currentDateValue, dateValue);
				case NEQ1:
				case NEQ2:
					return Tools.isNotEqual(currentDateValue, dateValue);
				case EQUALS:
					return Tools.isEqual(currentDateValue, dateValue);
				case LESS:
					return Tools.isLess(currentDateValue, dateValue);
				case GREATER:
					return Tools.isGreater(currentDateValue, dateValue);
				default:
					return false;

			}
		}
	}
}
