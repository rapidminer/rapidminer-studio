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
package com.rapidminer.operator.preprocessing.filter;

import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.AttributeRole;
import com.rapidminer.example.Example;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.table.AttributeFactory;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.OperatorVersion;
import com.rapidminer.operator.UserError;
import com.rapidminer.operator.annotation.ResourceConsumptionEstimator;
import com.rapidminer.operator.error.AttributeNotFoundError;
import com.rapidminer.operator.ports.metadata.AttributeMetaData;
import com.rapidminer.operator.ports.metadata.AttributeParameterPrecondition;
import com.rapidminer.operator.ports.metadata.AttributeSetPrecondition;
import com.rapidminer.operator.ports.metadata.ExampleSetMetaData;
import com.rapidminer.operator.ports.metadata.MetaData;
import com.rapidminer.operator.ports.metadata.SetRelation;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeAttribute;
import com.rapidminer.parameter.ParameterTypeBoolean;
import com.rapidminer.parameter.ParameterTypeCategory;
import com.rapidminer.parameter.UndefinedParameterError;
import com.rapidminer.parameter.conditions.EqualTypeCondition;
import com.rapidminer.tools.Ontology;
import com.rapidminer.tools.OperatorResourceConsumptionHandler;
import com.rapidminer.tools.Tools;


/**
 * This operator changes a date attribute into a numerical one. It allows to specify exactly which
 * entity should be extracted and to which unit or date it should relate. As an example, it is
 * possible to extract seconds within a minute. Analogously, it is also possible to extract the day
 * within a month. But it is also possible to extract the day within a week or within a year. For
 * all time units, it is also possible to extract the number which has passed by since 1970-01-01
 * 00:00.
 *
 * @author Tobias Malbrecht
 */
public class Date2Numerical extends AbstractDateDataProcessing {

	public static final String PARAMETER_ATTRIBUTE_NAME = "attribute_name";

	public static final String PARAMETER_TIME_UNIT = "time_unit";

	public static final String PARAMETER_KEEP_OLD_ATTRIBUTE = "keep_old_attribute";

	/**
	 * Last version to use {@link Ontology#INTEGER}, afterwards {@link Ontology#REAL} is used
	 * @since 9.0.2
	 */
	public static final OperatorVersion VERSION_USING_INTEGER = new OperatorVersion(9, 0, 1);

	Calendar calendar = null;

	public Date2Numerical(OperatorDescription description) {
		super(description);
		getExampleSetInputPort().addPrecondition(
				new AttributeParameterPrecondition(getExampleSetInputPort(), this, PARAMETER_ATTRIBUTE_NAME,
						Ontology.DATE_TIME));
		getExampleSetInputPort().addPrecondition(
				new AttributeSetPrecondition(getExampleSetInputPort(), AttributeSetPrecondition.getAttributesByParameter(
						this, PARAMETER_ATTRIBUTE_NAME)));
	}

	@Override
	protected MetaData modifyMetaData(ExampleSetMetaData metaData) throws UndefinedParameterError {
		AttributeMetaData amd = metaData.getAttributeByName(getParameterAsString(PARAMETER_ATTRIBUTE_NAME));
		if (amd != null) {
			AttributeMetaData newAttribute = amd.clone();
			newAttribute.setType(getTargetType());
			newAttribute.getMean().setUnkown();
			newAttribute.setValueSetRelation(SetRelation.UNKNOWN);
			if (!getParameterAsBoolean(PARAMETER_KEEP_OLD_ATTRIBUTE)) {
				metaData.removeAttribute(amd);
			} else {
				newAttribute.setName(newAttribute.getName() + "_" + TIME_UNITS[getParameterAsInt(PARAMETER_TIME_UNIT)]);
			}
			metaData.addAttribute(newAttribute);
		}
		return metaData;
	}

	@Override
	public ExampleSet apply(ExampleSet exampleSet) throws OperatorException {
		String attributeName = getParameterAsString(PARAMETER_ATTRIBUTE_NAME);
		int timeUnit = getParameterAsInt(PARAMETER_TIME_UNIT);
		int relativeTo = getParameterAsInt(PARAMETERS_RELATIVE_TO[timeUnit]);

		Attribute dateAttribute = exampleSet.getAttributes().get(attributeName);
		if (dateAttribute == null) {
			throw new AttributeNotFoundError(this, PARAMETER_ATTRIBUTE_NAME, getParameterAsString(PARAMETER_ATTRIBUTE_NAME));
		}
		int valueType = dateAttribute.getValueType();
		if (!Ontology.ATTRIBUTE_VALUE_TYPE.isA(valueType, Ontology.DATE_TIME)) {
			throw new UserError(this, 218, dateAttribute.getName(), Ontology.ATTRIBUTE_VALUE_TYPE.mapIndex(valueType));
		}

		Attribute newAttribute = AttributeFactory.createAttribute(getTargetType());
		exampleSet.getExampleTable().addAttribute(newAttribute);
		exampleSet.getAttributes().addRegular(newAttribute);

		calendar = Tools.getPreferredCalendar();

		for (Example example : exampleSet) {
			double value = example.getValue(dateAttribute);
			if (Double.isNaN(value)) {
				example.setValue(newAttribute, value);
			} else {
				example.setValue(newAttribute, extract((long) value, timeUnit, relativeTo));
			}
		}

		if (!getParameterAsBoolean(PARAMETER_KEEP_OLD_ATTRIBUTE)) {
			AttributeRole oldRole = exampleSet.getAttributes().getRole(dateAttribute);
			exampleSet.getAttributes().remove(dateAttribute);
			newAttribute.setName(attributeName);
			exampleSet.getAttributes().setSpecialAttribute(newAttribute, oldRole.getSpecialName());
		} else {
			newAttribute.setName(attributeName + "_" + TIME_UNITS[timeUnit]);
		}

		return exampleSet;
	}

	private double extract(long milliseconds, int timeUnit, int relativeTo) {
		calendar.setTimeInMillis(milliseconds);
		switch (timeUnit) {
			case MILLISECOND:
				switch (relativeTo) {
					case MILLISECOND_RELATIVE_TO_SECOND:
						return milliseconds % 1000;
					case MILLISECOND_RELATIVE_TO_EPOCH:
						return milliseconds;
				}
				break;
			case SECOND:
				switch (relativeTo) {
					case SECOND_RELATIVE_TO_MINUTE:
						return calendar.get(Calendar.SECOND);
					case SECOND_RELATIVE_TO_HOUR:
						return calendar.get(Calendar.MINUTE) * 60 + calendar.get(Calendar.SECOND);
					case SECOND_RELATIVE_TO_DAY:
						return calendar.get(Calendar.HOUR_OF_DAY) * 3600 + calendar.get(Calendar.MINUTE) * 60
								+ calendar.get(Calendar.SECOND);
					case SECOND_RELATIVE_TO_EPOCH:
						return milliseconds / 1000;
				}
				break;
			case MINUTE:
				switch (relativeTo) {
					case MINUTE_RELATIVE_TO_HOUR:
						return calendar.get(Calendar.MINUTE);
					case MINUTE_RELATIVE_TO_DAY:
						return calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE);
					case MINUTE_RELATIVE_TO_EPOCH:
						return milliseconds / 60000;
				}
			case HOUR:
				switch (relativeTo) {
					case HOUR_RELATIVE_TO_DAY:
						return calendar.get(Calendar.HOUR_OF_DAY);
					case HOUR_RELATIVE_TO_EPOCH:
						return milliseconds / 3600000;
				}
				break;
			case DAY:
				switch (relativeTo) {
					case DAY_RELATIVE_TO_WEEK:
						return calendar.get(Calendar.DAY_OF_WEEK);
					case DAY_RELATIVE_TO_MONTH:
						return calendar.get(Calendar.DAY_OF_MONTH);
					case DAY_RELATIVE_TO_YEAR:
						return calendar.get(Calendar.DAY_OF_YEAR);
					case DAY_RELATIVE_TO_EPOCH:
						return milliseconds / 86400000;
				}
				break;
			case WEEK:
				switch (relativeTo) {
					case WEEK_RELATIVE_TO_MONTH:
						return calendar.get(Calendar.WEEK_OF_MONTH);
					case WEEK_RELATIVE_TO_YEAR:
						return calendar.get(Calendar.WEEK_OF_YEAR);
					case WEEK_RELATIVE_TO_EPOCH:
						return milliseconds / 604800000L;
				}
				break;
			case MONTH:
				switch (relativeTo) {
					case MONTH_RELATIVE_TO_QUARTER:
						return calendar.get(Calendar.MONTH) % 3 + 1;
					case MONTH_RELATIVE_TO_YEAR:
						return calendar.get(Calendar.MONTH) + 1;
					case MONTH_RELATIVE_TO_EPOCH:
						return calendar.get(Calendar.MONTH) + 1 + (calendar.get(Calendar.YEAR) - 1970) * 12;
				}
				break;
			case QUARTER:
				switch (relativeTo) {
					case QUARTER_RELATIVE_TO_YEAR:
						return calendar.get(Calendar.MONTH) / 3 + 1;
					case QUARTER_RELATIVE_TO_EPOCH:
						return calendar.get(Calendar.MONTH) / 3 + 1 + (calendar.get(Calendar.YEAR) - 1970) * 4;
				}
				break;
			case HALF_YEAR:
				switch (relativeTo) {
					case HALF_YEAR_RELATIVE_TO_YEAR:
						return calendar.get(Calendar.MONTH) / 6 + 1;
					case HALF_YEAR_RELATIVE_TO_EPOCH:
						return calendar.get(Calendar.MONTH) / 6 + 1 + (calendar.get(Calendar.YEAR) - 1970) * 2;
				}
				break;
			case YEAR:
				switch (relativeTo) {
					case YEAR_RELATIVE_TO_ERA:
						return calendar.get(Calendar.YEAR);
					case YEAR_RELATIVE_TO_EPOCH:
						return calendar.get(Calendar.YEAR) - 1970;
				}
				break;
			default:
				break;
		}
		return Double.NaN;
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();
		types.add(new ParameterTypeAttribute(PARAMETER_ATTRIBUTE_NAME, "The attribute which should be parsed.",
				getExampleSetInputPort(), false, false, Ontology.DATE_TIME));
		types.add(new ParameterTypeCategory(PARAMETER_TIME_UNIT, "The unit in which the time is measured.", TIME_UNITS, 0,
				false));
		ParameterType type = null;
		for (int i = 0; i < TIME_UNITS.length; i++) {
			type = new ParameterTypeCategory(PARAMETERS_RELATIVE_TO[i], "The unit the value is extracted relativ to.",
					RELATIVE_TO_MODES[i], RELATIVE_TO_DEFAULTS[i], false);
			type.registerDependencyCondition(new EqualTypeCondition(this, PARAMETER_TIME_UNIT, TIME_UNITS, true, i));
			types.add(type);
		}
		types.add(new ParameterTypeBoolean(PARAMETER_KEEP_OLD_ATTRIBUTE,
				"Indicates if the original date attribute should be kept.", false));
		return types;
	}

	@Override
	public boolean writesIntoExistingData() {
		return false;
	}

	@Override
	public ResourceConsumptionEstimator getResourceConsumptionEstimator() {
		return OperatorResourceConsumptionHandler
				.getResourceConsumptionEstimator(getInputPort(), Date2Numerical.class, null);
	}

	@Override
	public OperatorVersion[] getIncompatibleVersionChanges() {
		OperatorVersion[] changes = super.getIncompatibleVersionChanges();
		changes = Arrays.copyOf(changes, changes.length + 1);
		changes[changes.length - 1] = VERSION_USING_INTEGER;
		return changes;
	}

	/**
	 * Returns the version dependent target type
	 * @return either {@link Ontology#INTEGER} or {@link Ontology#REAL}
	 */
	private int getTargetType(){
		return getCompatibilityLevel().isAtMost(VERSION_USING_INTEGER) ? Ontology.INTEGER : Ontology.REAL;
	}
}
