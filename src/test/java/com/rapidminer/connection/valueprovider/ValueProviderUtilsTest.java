/**
 * Copyright (C) 2001-2019 by RapidMiner and the contributors
 *
 * Complete list of developers available at our web site:
 *
 * http://rapidminer.com
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General
 * Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any
 * later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License for more
 * details.
 *
 * You should have received a copy of the GNU Affero General Public License along with this program. If not, see
 * http://www.gnu.org/licenses/.
 */
package com.rapidminer.connection.valueprovider;

import static com.rapidminer.connection.valueprovider.handler.ValueProviderUtils.PLACEHOLDER_PREFIX;
import static com.rapidminer.connection.valueprovider.handler.ValueProviderUtils.PLACEHOLDER_SUFFIX;
import static com.rapidminer.connection.valueprovider.handler.ValueProviderUtils.wrapIntoPlaceholder;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.junit.Test;

import com.rapidminer.connection.valueprovider.handler.ValueProviderUtils;


/**
 * Tests the {@link ValueProviderUtils}.
 *
 * @author Marco Boeck
 * @since 9.3.0
 */
public class ValueProviderUtilsTest {

	private static final String KEY_TEST = "test";
	private static final String KEY_NESTED = "outer%{inner}";

	@Test
	public void test() {
		assertEquals(PLACEHOLDER_PREFIX + KEY_TEST + PLACEHOLDER_SUFFIX, wrapIntoPlaceholder(KEY_TEST));
		assertEquals(PLACEHOLDER_PREFIX + KEY_NESTED + PLACEHOLDER_SUFFIX, wrapIntoPlaceholder(KEY_NESTED));

		try {
			wrapIntoPlaceholder(null);
			fail("null argument should have thrown IAE");
		} catch (IllegalArgumentException e) {
			// expected
		}
		try {
			wrapIntoPlaceholder(" ");
			fail("whitespace-only argument should have thrown IAE");
		} catch (IllegalArgumentException e) {
			// expected
		}
	}
}
