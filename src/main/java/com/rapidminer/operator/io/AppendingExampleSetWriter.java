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
package com.rapidminer.operator.io;

import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.UserError;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeCategory;
import com.rapidminer.parameter.UndefinedParameterError;

import java.io.File;
import java.util.LinkedList;
import java.util.List;


/**
 * Provides functionality to determine whether files should be overwritten or appended to.
 * 
 * @author Simon Fischer
 * 
 */
public abstract class AppendingExampleSetWriter extends AbstractExampleSetWriter {

	/** The parameter name for &quot;Indicates if an existing table should be overwritten.&quot; */
	public static final String PARAMETER_OVERWRITE_MODE = "overwrite_mode";

	public static final String[] OVERWRITE_MODES = new String[] { "none", "overwrite first, append then", "overwrite",
			"append" };

	public static final int OVERWRITE_MODE_NONE = 0;
	public static final int OVERWRITE_MODE_OVERWRITE_FIRST = 1;
	public static final int OVERWRITE_MODE_OVERWRITE = 2;
	public static final int OVERWRITE_MODE_APPEND = 3;

	public AppendingExampleSetWriter(OperatorDescription description) {
		super(description);
	}

	protected boolean shouldAppend(File dataFile) throws UndefinedParameterError, UserError {
		int overwriteMode = getParameterAsInt(PARAMETER_OVERWRITE_MODE);
		boolean append = false;
		switch (overwriteMode) {
			case OVERWRITE_MODE_NONE:
				if (dataFile.exists()) {
					throw new UserError(this, 100);
				}
				append = false;
				break;
			case OVERWRITE_MODE_OVERWRITE:
				if (dataFile.exists()) {
					getLogger().info("File " + dataFile + " already exists. Overwriting...");
				}
				append = false;
				break;
			case OVERWRITE_MODE_APPEND:
				if (dataFile.exists()) {
					getLogger().info("File " + dataFile + " already exists. Appending...");
				}
				append = true;
				break;
			case OVERWRITE_MODE_OVERWRITE_FIRST:
			default:
				if (getApplyCount() <= 1) { // first time
					if (dataFile.exists()) {
						getLogger().info("File " + dataFile + " already exists. Overwriting this time...");
					}
					append = false;
				} else {
					if (dataFile.exists()) {
						getLogger().info("File " + dataFile + " already exists. Appending...");
					}
					append = true;
				}
				break;
		}
		return append;
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = new LinkedList<ParameterType>();
		types.add(new ParameterTypeCategory(PARAMETER_OVERWRITE_MODE,
				"Indicates if an existing table should be overwritten or if data should be appended.", OVERWRITE_MODES,
				OVERWRITE_MODE_OVERWRITE_FIRST));
		types.addAll(super.getParameterTypes());
		return types;
	}

}
