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
package com.rapidminer.connection.legacy;

import java.util.Arrays;

import org.junit.Assert;
import org.junit.Test;

import com.rapidminer.connection.ConnectionInformation;

/**
 * Test for ConversionService
 *
 * @author Jonas Wilms-Pfau
 * @since 9.3.0
 */
public class ConversionServiceTest {

	private static class DoubleService implements ConversionService<Double> {

		@Override
		public boolean canConvert(Object oldConnectionObject) {
			return oldConnectionObject instanceof Double;
		}

		@Override
		public ConnectionInformation convert(Double oldConnectionObject) {
			return null;
		}
	}

	private static class StringService implements ConversionService<String> {

		@Override
		public boolean canConvert(Object oldConnectionObject) {
			return oldConnectionObject instanceof String;
		}

		@Override
		public ConnectionInformation convert(String oldConnectionObject) {
			return null;
		}
	}

	private static class ExceptionService implements ConversionService<Exception> {

		@Override
		public boolean canConvert(Object oldConnectionObject) {
			return oldConnectionObject instanceof Exception;
		}

		@Override
		public ConnectionInformation convert(Exception oldConnectionObject) {
			return null;
		}
	}

	private ConversionService stringService = new StringService();
	private ConversionService doubleService = new DoubleService();
	private ConversionService exceptionServiceService = new ExceptionService();
	private ConversionService[] allServices = new ConversionService[]{stringService, doubleService, exceptionServiceService};
	private Object[] testValues = new Object[]{"", 0D, new Exception()};

	@SuppressWarnings({"unchecked"})
	@Test(expected = ClassCastException.class)
	public void failConversion() throws ConversionException {
		int value = (int) (allServices.length * Math.random());
		allServices[value].convert(testValues[(value + 1) % allServices.length]);
	}

	@Test
	public void findConverter() {
		for (Object test : testValues) {
			Assert.assertEquals(1, Arrays.stream(allServices).filter(s -> s.canConvert(test)).count());
		}
	}

	@SuppressWarnings({"unchecked"})
	@Test
	public void convert() throws ConversionException {
		for (Object test : testValues) {
			for (ConversionService converter : allServices) {
				if (converter.canConvert(test)) {
					converter.convert(test);
				}
			}
		}
	}

}