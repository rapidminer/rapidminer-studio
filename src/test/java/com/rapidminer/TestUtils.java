/**
 * Copyright (C) 2001-2020 by RapidMiner and the contributors
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
package com.rapidminer;


import com.rapidminer.core.license.ProductConstraintManager;
import com.rapidminer.license.AlreadyRegisteredException;
import com.rapidminer.license.InvalidProductException;
import com.rapidminer.license.location.LicenseLoadingException;
import com.rapidminer.operator.OperatorCreationException;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.ProcessRootOperator;
import com.rapidminer.tools.OperatorService;


/**
 * Some often needed utility methods in testing.
 *
 * @since 9.7
 */
public enum TestUtils {

	INSTANCE;


	/**
	 * Registers the {@link ProcessRootOperator} to the {@link OperatorService} if not yet registered and sets up the
	 * {@link com.rapidminer.core.license.ProductConstraintManager}. Needed to be able to at least create and use empty
	 * processes.
	 */
	public void minimalProcessUsageSetup() throws OperatorCreationException, AlreadyRegisteredException, LicenseLoadingException, InvalidProductException, IllegalAccessException {
		if (!ProductConstraintManager.INSTANCE.isInitialized()) {
			ProductConstraintManager.INSTANCE.initialize(null, null);
		}
		if (OperatorService.getOperatorDescription("process") == null) {
			OperatorService.registerOperator(new OperatorDescription("com.rapidminer.operator.ProcessRootOperator",
					"process", ProcessRootOperator.class, getClass().getClassLoader(), "elements_selection.png", null), null);
		}
	}
}
