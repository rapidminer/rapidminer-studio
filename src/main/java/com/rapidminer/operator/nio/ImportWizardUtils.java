/**
 * Copyright (C) 2001-2016 by RapidMiner and the contributors
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
package com.rapidminer.operator.nio;

import com.rapidminer.RapidMiner;
import com.rapidminer.gui.tools.SwingTools;
import com.rapidminer.tools.ParameterService;


/**
 * 
 * @author Simon Fischer
 * 
 */
public class ImportWizardUtils {

	public static void showErrorMessage(String resource, String message, Throwable exception) {
		SwingTools.showSimpleErrorMessage("importwizard.io_error", exception, resource, message);
	}

	public static int getPreviewLength() {
		try {
			return Integer
					.parseInt(ParameterService.getParameterValue(RapidMiner.PROPERTY_RAPIDMINER_GENERAL_MAX_TEST_ROWS));
		} catch (NumberFormatException e) {
			return 100;
		}
	}

}
