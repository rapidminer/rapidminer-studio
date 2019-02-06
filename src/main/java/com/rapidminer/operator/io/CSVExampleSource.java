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

import java.util.Iterator;
import java.util.List;

import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeFile;


/**
 * <p>
 * This operator can read csv files. All values must be separated by &quot;,&quot; or by
 * &quot;;&quot;, followed by an arbitrary amount of white space. This means that also only the
 * separator characters are allowed. The first line is used for the attribute names as default.
 * Empty values and the question mark will be read as missing values. You can quote the values
 * (including the column separators) with a double quote (&quot;). You can escape the quoting
 * character with a backslash, i.e. \&quot;.
 * </p>
 *
 * <p>
 * For other file formats or column separators you can use in almost all cases the operator
 * {@link SimpleExampleSource} or, if this is not sufficient, the operator {@link ExampleSource}.
 * </p>
 *
 * @rapidminer.index csv
 * @author Ingo Mierswa
 *
 * @deprecated Replaced by {@link com.rapidminer.operator.nio.CSVExampleSource}.
 */
@Deprecated
public class CSVExampleSource extends SimpleExampleSource {

	// Removed because of deprecation of this operator
	// static {
	// AbstractReader.registerReaderDescription(new ReaderDescription("csv", CSVExampleSource.class,
	// PARAMETER_FILENAME));
	// }

	public CSVExampleSource(final OperatorDescription description) {
		super(description);
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();
		Iterator<ParameterType> p = types.iterator();
		while (p.hasNext()) {
			ParameterType type = p.next();
			if (type.getKey().equals(PARAMETER_USE_FIRST_ROW_AS_ATTRIBUTE_NAMES)) {
				type.setDefaultValue(true);
			} else if (type.getKey().equals(PARAMETER_FILENAME)) {
				((ParameterTypeFile) type).setExtension("csv");
			} else if (type.getKey().equals(PARAMETER_USE_QUOTES)) {
				type.setDefaultValue(true);
			} else if (type.getKey().equals(PARAMETER_COLUMN_SEPARATORS)) {
				type.setDefaultValue(",\\s*|;\\s*");
			}
		}
		return types;
	}

}
