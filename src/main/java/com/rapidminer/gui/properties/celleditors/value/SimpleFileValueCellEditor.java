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
package com.rapidminer.gui.properties.celleditors.value;

import com.rapidminer.operator.Operator;
import com.rapidminer.parameter.ParameterTypeFile;

import java.awt.GridBagConstraints;


/**
 * A simple file cell editor for generic files. These can be used for all parameter types which are
 * not special.
 * 
 * @see AttributeFileValueCellEditor
 * @author Ingo Mierswa, Simon Fischer
 */
public class SimpleFileValueCellEditor extends FileValueCellEditor {

	private static final long serialVersionUID = 8800712397096177848L;

	public SimpleFileValueCellEditor(ParameterTypeFile type) {
		super(type);
		addButton(createFileChooserButton(), GridBagConstraints.REMAINDER);
	}

	/** Does nothing. */
	@Override
	public void setOperator(Operator operator) {}

	@Override
	public boolean rendersLabel() {
		return false;
	}
}
