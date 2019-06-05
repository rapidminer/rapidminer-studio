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
package com.rapidminer.connection.util;

import java.util.HashMap;

import org.junit.Assert;
import org.junit.Test;


/**
 * Tests for {@link ValidationResult}, mainly for the {@link ValidationResult#merge(ValidationResult...)} functionality.
 *
 * @author Andreas Timm
 * @since 9.3.0
 */
public class ValidationResultTest {

	@Test
	public void mergeNone() {
		Assert.assertEquals(ValidationResult.nullable(), ValidationResult.merge());
	}

	@Test
	public void mergeFailureSuccess() {
		ValidationResult fail = ValidationResult.failure("fail", new HashMap<>());
		ValidationResult succ = ValidationResult.success("success");

		ValidationResult merge = ValidationResult.merge(succ, fail);
		Assert.assertEquals(fail.getType(), merge.getType());
		Assert.assertEquals(fail.getMessageKey(), merge.getMessageKey());
	}

	@Test
	public void mergeOrderingTest() {
		ValidationResult fail = ValidationResult.failure("fail", new HashMap<>());
		ValidationResult nullable = ValidationResult.nullable();
		ValidationResult succ = ValidationResult.success("success");

		ValidationResult merge = ValidationResult.merge(succ, fail, nullable, nullable, fail, succ);
		Assert.assertEquals(fail.getType(), merge.getType());
		Assert.assertEquals(fail.getMessageKey(), merge.getMessageKey());

		merge = ValidationResult.merge(succ, nullable, nullable, succ);
		Assert.assertEquals(succ.getType(), merge.getType());
		Assert.assertEquals(succ.getMessageKey(), merge.getMessageKey());
	}

	@Test
	public void mergeParamOverwriteTest() {
		HashMap<String, String> parameterErrors = new HashMap<>();
		parameterErrors.put("failed parameter", "failed value");
		String overwritten = "overwritten";
		parameterErrors.put(overwritten, "by fail");
		ValidationResult fail = ValidationResult.failure("firstfail", parameterErrors);
		HashMap<String, String> finalFailedParams = new HashMap<>();
		finalFailedParams.put(overwritten, "by finalfail");
		ValidationResult failLater = ValidationResult.failure("finalfail", finalFailedParams);

		ValidationResult merge = ValidationResult.merge(fail, failLater);

		Assert.assertEquals(failLater.getType(), merge.getType());
		Assert.assertEquals(failLater.getMessageKey(), merge.getMessageKey());
		Assert.assertEquals(2, merge.getParameterErrorMessages().size());
		Assert.assertTrue(merge.getParameterErrorMessages().get(overwritten).equals("by finalfail"));
	}
}
