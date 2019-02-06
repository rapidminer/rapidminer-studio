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

import java.util.Calendar;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.Example;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.table.AttributeFactory;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.UserError;
import com.rapidminer.operator.annotation.ResourceConsumptionEstimator;
import com.rapidminer.operator.error.AttributeNotFoundError;
import com.rapidminer.operator.ports.metadata.AttributeMetaData;
import com.rapidminer.operator.ports.metadata.AttributeSetPrecondition;
import com.rapidminer.operator.ports.metadata.ExampleSetMetaData;
import com.rapidminer.operator.ports.metadata.MetaData;
import com.rapidminer.operator.ports.metadata.SetRelation;
import com.rapidminer.operator.preprocessing.AbstractDataProcessing;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeAttribute;
import com.rapidminer.parameter.ParameterTypeBoolean;
import com.rapidminer.parameter.ParameterTypeCategory;
import com.rapidminer.parameter.ParameterTypeInt;
import com.rapidminer.parameter.ParameterTypeList;
import com.rapidminer.parameter.UndefinedParameterError;
import com.rapidminer.tools.Ontology;
import com.rapidminer.tools.OperatorResourceConsumptionHandler;
import com.rapidminer.tools.Tools;


/**
 * This operator allows to adjust a date attribute by adding a constant value in arbitrary units as
 * days, hours or seconds to the attributes value.
 *
 * @author Ingo Mierswa
 */
public class DateAdjust extends AbstractDataProcessing {

	private static final String ATTRIBUTE_NAME_POSTFIX = "_adjusted";

	public static final String PARAMETER_ATTRIBUTE_NAME = "attribute_name";

	public static final String PARAMETER_KEEP_OLD_ATTRIBUTE = "keep_old_attribute";

	public static final String PARAMETER_ADJUSTMENTS = "adjustments";

	public static final String PARAMETER_DATE_UNIT = "date_unit";

	public static final String[] CALENDAR_FIELDS = new String[] { "Year", "Month", "Day", "Hour", "Minute", "Second",
	"Millisecond" };

	public static final int CALENDAR_FIELD_YEAR = 0;
	public static final int CALENDAR_FIELD_MONTH = 1;
	public static final int CALENDAR_FIELD_DAY = 2;
	public static final int CALENDAR_FIELD_HOUR = 3;
	public static final int CALENDAR_FIELD_MINUTE = 4;
	public static final int CALENDAR_FIELD_SECOND = 5;
	public static final int CALENDAR_FIELD_MILLISECOND = 6;

	private static class Adjustment {

		private int originalField;
		private int calendarField;
		private int amount;

		public Adjustment(int field, int amount) {
			this.originalField = field;

			switch (field) {
				case CALENDAR_FIELD_YEAR:
					this.calendarField = Calendar.YEAR;
					break;
				case CALENDAR_FIELD_MONTH:
					this.calendarField = Calendar.MONTH;
					break;
				case CALENDAR_FIELD_DAY:
					this.calendarField = Calendar.DAY_OF_YEAR;
					break;
				case CALENDAR_FIELD_HOUR:
					this.calendarField = Calendar.HOUR_OF_DAY;
					break;
				case CALENDAR_FIELD_MINUTE:
					this.calendarField = Calendar.MINUTE;
					break;
				case CALENDAR_FIELD_SECOND:
					this.calendarField = Calendar.SECOND;
					break;
				case CALENDAR_FIELD_MILLISECOND:
					this.calendarField = Calendar.MILLISECOND;
					break;
			}

			this.amount = amount;
		}

		public int getField() {
			return this.calendarField;
		}

		public int getAmount() {
			return this.amount;
		}

		@Override
		public String toString() {
			return "Adjust " + CALENDAR_FIELDS[originalField] + " (calendar: " + calendarField + ") by " + amount;
		}
	}

	public DateAdjust(OperatorDescription description) {
		super(description);

		getExampleSetInputPort().addPrecondition(
				new AttributeSetPrecondition(getExampleSetInputPort(), AttributeSetPrecondition.getAttributesByParameter(
						this, PARAMETER_ATTRIBUTE_NAME), Ontology.DATE_TIME));
	}

	@Override
	protected MetaData modifyMetaData(ExampleSetMetaData metaData) throws UndefinedParameterError {
		AttributeMetaData targetAttribute = metaData.getAttributeByName(getParameterAsString(PARAMETER_ATTRIBUTE_NAME));
		if (targetAttribute != null) {
			AttributeMetaData newAttribute = targetAttribute.clone();
			newAttribute.setValueSetRelation(SetRelation.UNKNOWN);

			// handling the keeping of old attribute
			if (getParameterAsBoolean(PARAMETER_KEEP_OLD_ATTRIBUTE)) {
				newAttribute.setName(newAttribute.getName() + ATTRIBUTE_NAME_POSTFIX);
			} else {
				metaData.removeAttribute(targetAttribute);
			}
			metaData.addAttribute(newAttribute);

		}
		return metaData;
	}

	@Override
	public ExampleSet apply(ExampleSet exampleSet) throws OperatorException {
		String attributeName = getParameterAsString(PARAMETER_ATTRIBUTE_NAME);
		Attribute dateAttribute = exampleSet.getAttributes().get(attributeName);
		if (dateAttribute == null) {
			throw new AttributeNotFoundError(this, PARAMETER_ATTRIBUTE_NAME, getParameterAsString(PARAMETER_ATTRIBUTE_NAME));
		}
		if (!Ontology.ATTRIBUTE_VALUE_TYPE.isA(dateAttribute.getValueType(), Ontology.DATE_TIME)) {
			throw new UserError(this, 120, attributeName, new Object[] {
					Ontology.VALUE_TYPE_NAMES[dateAttribute.getValueType()], Ontology.VALUE_TYPE_NAMES[Ontology.DATE_TIME] });
		}

		Attribute newAttribute = AttributeFactory.createAttribute(dateAttribute.getValueType());
		exampleSet.getExampleTable().addAttribute(newAttribute);
		exampleSet.getAttributes().addRegular(newAttribute);

		// store adjustments
		List<Adjustment> adjustments = new LinkedList<>();
		List<String[]> adjustmentList = getParameterList(PARAMETER_ADJUSTMENTS);
		for (String[] keyValuePair : adjustmentList) {
			int amount = 0;
			try {
				String amountString = keyValuePair[0].trim();
				if (amountString.startsWith("+")) {
					amountString = amountString.substring(1).trim();
				}
				amount = Integer.parseInt(amountString);
			} catch (NumberFormatException e) {
				throw new UserError(this, 116, PARAMETER_ADJUSTMENTS, "please use only integer numbers for the adjustments");
			}

			int field = -1;
			try {
				field = Integer.parseInt(keyValuePair[1]);
			} catch (NumberFormatException e) {
				// try strings
				int index = 0;
				boolean found = false;
				for (String fieldName : CALENDAR_FIELDS) {
					if (fieldName.equals(keyValuePair[1])) {
						found = true;
						break;
					}
					index++;
				}

				if (!found) {
					throw new UserError(this, 116, PARAMETER_ADJUSTMENTS,
							"please use only known calendar units like Year or Hour for the adjustments");
				} else {
					field = index;
				}
			}

			adjustments.add(new Adjustment(field, amount));
		}

		log("Adjustments: " + adjustments);

		// set values
		Calendar calendar = Tools.getPreferredCalendar();
		for (Example example : exampleSet) {
			Date date = example.getDateValue(dateAttribute);
			calendar.setTime(date);
			for (Adjustment adjustment : adjustments) {
				calendar.add(adjustment.getField(), adjustment.getAmount());
			}
			example.setValue(newAttribute, calendar.getTime().getTime());
		}

		if (!getParameterAsBoolean(PARAMETER_KEEP_OLD_ATTRIBUTE)) {
			exampleSet.getAttributes().remove(dateAttribute);
			newAttribute.setName(attributeName);
		} else {
			newAttribute.setName(attributeName + ATTRIBUTE_NAME_POSTFIX);
		}

		return exampleSet;
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();
		types.add(new ParameterTypeAttribute(PARAMETER_ATTRIBUTE_NAME, "The attribute which should be parsed.",
				getExampleSetInputPort(), false, Ontology.DATE_TIME));

		ParameterType type = new ParameterTypeList(PARAMETER_ADJUSTMENTS, "This list defines all date adjustments.",
				new ParameterTypeInt("adjustment", "The number of units to add to the dates.", -Integer.MAX_VALUE,
						Integer.MAX_VALUE), new ParameterTypeCategory(PARAMETER_DATE_UNIT,
				"The unit which should be adjusted.", CALENDAR_FIELDS, CALENDAR_FIELD_HOUR));
		type.setPrimary(true);
		types.add(type);

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
		return OperatorResourceConsumptionHandler.getResourceConsumptionEstimator(getInputPort(), DateAdjust.class, null);
	}
}
