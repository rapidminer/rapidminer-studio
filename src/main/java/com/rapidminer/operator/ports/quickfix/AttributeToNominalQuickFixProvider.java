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
package com.rapidminer.operator.ports.quickfix;

import java.util.LinkedList;
import java.util.List;

import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorCreationException;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.operator.ports.metadata.AttributeMetaData;
import com.rapidminer.operator.preprocessing.filter.Date2Nominal;
import com.rapidminer.operator.preprocessing.filter.NumericToPolynominal;
import com.rapidminer.operator.preprocessing.filter.attributes.SingleAttributeFilter;
import com.rapidminer.operator.tools.AttributeSubsetSelector;
import com.rapidminer.parameter.ParameterTypeDateFormat;
import com.rapidminer.tools.OperatorService;


/**
 * Provides DateTime and Numerical to Nominal quick fixes for a single attribute
 *
 * @author Jonas Wilms-Pfau
 * @since 8.2.0
 */
public final class AttributeToNominalQuickFixProvider {

	/** Rating used in case multiple quick fixes are available */
	private static final int DEFAULT_RATING = 1;

	/**
	 * Prevent utility class instantiation.
	 */
	private AttributeToNominalQuickFixProvider(){
		throw new AssertionError("Utility class");
	}

	/**
	 * Returns a QuickFix for a Numerical or DateTime label to Nominal label
	 *
	 * @param exampleSetInput the input port where the quickfix should be applied
	 * @param labelMD the label that should be converted to nominal
	 * @return list containing the quickfix if available
	 */
	public static List<QuickFix> labelToNominal(InputPort exampleSetInput, AttributeMetaData labelMD) {
		return attributeToNominal(exampleSetInput, labelMD, DEFAULT_RATING, "insert_to_nominal_label");
	}

	/***
	 * Returns a QuickFix for Numerical and DateTime Attributes to Nominal
	 *
	 * @param exampleSetInput the input port where the quickfix should be applied
	 * @param attributeMD the attribute that should be converted
	 * @param rating defines the position in the quickfix list (default 1)
	 * @param i18nKey the i18n key used for the QuickFix
	 * @param i18nArgs i18n arguments
	 * @return list containing the quickfix if available
	 */
	public static List<QuickFix> attributeToNominal(InputPort exampleSetInput, AttributeMetaData attributeMD, int rating, String i18nKey, Object... i18nArgs) {

		LinkedList<QuickFix> quickFixes = new LinkedList<>();

		if (attributeMD.isNumerical()) {
			QuickFix numericalQuickFix = new OperatorInsertionQuickFix(i18nKey,
					i18nArgs, rating, exampleSetInput) {

				@Override
				public Operator createOperator() throws OperatorCreationException {
					NumericToPolynominal operator = OperatorService.createOperator(NumericToPolynominal.class);
					if (attributeMD.isSpecial()) {
						operator.setParameter(AttributeSubsetSelector.PARAMETER_INCLUDE_SPECIAL_ATTRIBUTES, "true");
					}
					operator.setParameter(AttributeSubsetSelector.PARAMETER_FILTER_TYPE, AttributeSubsetSelector.CONDITION_NAMES[AttributeSubsetSelector.CONDITION_SINGLE]);
					operator.setParameter(SingleAttributeFilter.PARAMETER_ATTRIBUTE, attributeMD.getName());
					return operator;
				}
			};
			quickFixes.add(numericalQuickFix);
		} else if (attributeMD.isDateTime()) {
			QuickFix dateTimeQuickFix = new OperatorInsertionQuickFix(i18nKey,
					i18nArgs, rating, exampleSetInput) {

				@Override
				public Operator createOperator() throws OperatorCreationException {
					Date2Nominal operator = OperatorService.createOperator(Date2Nominal.class);
					operator.setParameter(Date2Nominal.PARAMETER_ATTRIBUTE_NAME, attributeMD.getName());
					operator.setParameter(ParameterTypeDateFormat.PARAMETER_DATE_FORMAT, ParameterTypeDateFormat.DATE_TIME_FORMAT_ISO8601_UTC_MS);
					return operator;
				}
			};
			quickFixes.add(dateTimeQuickFix);
		}
		return quickFixes;
	}

}
