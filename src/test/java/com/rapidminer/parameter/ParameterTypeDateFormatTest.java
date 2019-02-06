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
import java.util.Locale;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;

import com.rapidminer.operator.Operator;
import com.rapidminer.operator.ProcessSetupError;
import com.rapidminer.operator.UserError;


/**
 * Tests the method {@link ParameterTypeDateFormat#createCheckedDateFormat(Operator, String, Locale, boolean)}.
 * Locale is ignored as a parameter.
 *
 * @author Jan Czogalla
 * @since 8.2.0
 */
public class ParameterTypeDateFormatTest {

	private static Operator withCorrectDateFormatParameter;
	private static Operator withIllegalDateFormatParameter;
	private static Operator withoutDateFormatParameter;
	private static Operator[] operators;

	private static final String correctPattern = ParameterTypeDateFormat.DATE_FORMAT_MM_DD_YYYY;
	private static final String illegalPattern = "T";
	private static final List<ProcessSetupError> setupErrors = new ArrayList<>();

	@BeforeClass
	public static void setup() throws UndefinedParameterError {
		withCorrectDateFormatParameter = Mockito.mock(Operator.class);
		Mockito.when(withCorrectDateFormatParameter.getParameter(ParameterTypeDateFormat.PARAMETER_DATE_FORMAT)).thenReturn(correctPattern);
		withIllegalDateFormatParameter = Mockito.mock(Operator.class);
		Mockito.when(withIllegalDateFormatParameter.getParameter(ParameterTypeDateFormat.PARAMETER_DATE_FORMAT)).thenReturn(illegalPattern);
		withoutDateFormatParameter = Mockito.mock(Operator.class);
		Mockito.when(withoutDateFormatParameter.getParameter(Mockito.anyString())).thenThrow(new UndefinedParameterError(""));
		operators = new Operator[]{null, withoutDateFormatParameter, withIllegalDateFormatParameter, withCorrectDateFormatParameter};
		for (int i = 1; i < operators.length; i++) {
			// store setup errors in field for checking
			Mockito.doAnswer(invoke -> {
				setupErrors.add(invoke.getArgument(0));
				return null;
			}).when(operators[i]).addError(Mockito.any(ProcessSetupError.class));
		}
	}

	@Test
	public void testAllPredefinedDateFormats() throws UserError {
		for (String format : ParameterTypeDateFormat.PREDEFINED_DATE_FORMATS) {
			Assert.assertNotNull(ParameterTypeDateFormat.createCheckedDateFormat(format, null));
		}
	}

	@Test
	public void testCreateCheckedDateFormatWithNullReturn() throws UserError {
		for (int op = 0; op < operators.length; op++) {
			Assert.assertNull("Format parser wrongly created", ParameterTypeDateFormat.createCheckedDateFormat(operators[op], illegalPattern, null, true));
		}
		for (int op = 0; op < 2; op++) {
			for (int inSetup = 0; inSetup < 2; inSetup++) {
				Assert.assertNull("Format parser wrongly created", ParameterTypeDateFormat.createCheckedDateFormat(operators[op], null, null, inSetup > 0));
			}
		}
		Assert.assertNull("Format parser wrongly created", ParameterTypeDateFormat.createCheckedDateFormat(withIllegalDateFormatParameter, null, null, true));
	}

	@Test
	public void testCreateCheckedDateFormatWithNonNullReturn() throws UserError {
		for (int op = 0; op < operators.length; op++) {
			for (int inSetup = 0; inSetup < 2; inSetup++) {
				Assert.assertNotNull("No date format created", ParameterTypeDateFormat.createCheckedDateFormat(operators[op], correctPattern, null, inSetup > 0));
			}
		}
		for (int inSetup = 0; inSetup < 2; inSetup++) {
			Assert.assertNotNull("No date format created", ParameterTypeDateFormat.createCheckedDateFormat(withCorrectDateFormatParameter, null, null, inSetup > 0));
		}
	}

	@Test
	public void testCreateCheckedDateFormatWithSetupError() throws UserError {
		for (int op = 1; op < operators.length; op++) {
			setupErrors.clear();
			ParameterTypeDateFormat.createCheckedDateFormat(operators[op], illegalPattern, null, true);
			Assert.assertEquals("No setup error created.", 1, setupErrors.size());
		}
		setupErrors.clear();
		ParameterTypeDateFormat.createCheckedDateFormat(withIllegalDateFormatParameter, null, null, true);
		Assert.assertEquals("No setup error created.", 1, setupErrors.size());
	}

	@Test
	public void testCreateCheckedDateFormatWithUserError() {
		for (int op = 0; op < operators.length; op++) {
			try {
				ParameterTypeDateFormat.createCheckedDateFormat(operators[op], illegalPattern, null, false);
				Assert.fail("No UserError thrown for invalid date format pattern.");
			} catch (UserError ue) {
				// passed
			}
		}
		try {
			ParameterTypeDateFormat.createCheckedDateFormat(withIllegalDateFormatParameter, null, null, false);
			Assert.fail("No UserError thrown for invalid date format pattern.");
		} catch (UserError ue) {
			// passed
		}
	}
}