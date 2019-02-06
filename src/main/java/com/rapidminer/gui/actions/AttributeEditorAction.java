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
package com.rapidminer.gui.actions;

import java.awt.event.ActionEvent;

import com.rapidminer.gui.attributeeditor.AttributeEditorDialog;
import com.rapidminer.gui.tools.ResourceAction;
import com.rapidminer.gui.tools.SwingTools;
import com.rapidminer.operator.OperatorCreationException;
import com.rapidminer.operator.io.ExampleSource;
import com.rapidminer.tools.OperatorService;


/**
 * Start the corresponding action.
 *
 * @author Ingo Mierswa
 */
@SuppressWarnings("deprecation")
public class AttributeEditorAction extends ResourceAction {

	private static final long serialVersionUID = -1571544885210247278L;

	public AttributeEditorAction() {
		super("attribute_editor");
	}

	@Override
	public void loggedActionPerformed(ActionEvent e) {
		try {
			ExampleSource exampleSource = OperatorService.createOperator(ExampleSource.class);
			AttributeEditorDialog dialog = new AttributeEditorDialog(exampleSource, null);
			dialog.setVisible(true);
		} catch (OperatorCreationException ex) {
			SwingTools.showVerySimpleErrorMessage("cannot_start_attr_editor");
		}
	}
}
