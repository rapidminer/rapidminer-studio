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
import javax.swing.JDialog;

import com.rapidminer.gui.RapidMinerGUI;
import com.rapidminer.gui.tools.ResourceAction;
import com.rapidminer.studio.io.gui.internal.DataImportWizardBuilder;
import com.rapidminer.studio.io.gui.internal.DataImportWizardUtils;


/**
 * An action that opens the {@link com.rapidminer.studio.io.gui.internal.DataImportWizard
 * DataImportWizard}.
 *
 * @author Nils Woehler
 * @since 7.0.0
 *
 */
public class ImportDataAction extends ResourceAction {

	private static final long serialVersionUID = 1L;

	/**
	 * Constructs a new {@link ImportDataAction} instance with a big icon.
	 */
	public ImportDataAction() {
		super(false, "add_data");
	}

	/**
	 * Constructs a new {@link ImportDataAction} instance.
	 *
	 * @param smallIcon
	 *            specifies whether the icon should be small (16x16) or big (24x24).
	 */
	public ImportDataAction(boolean smallIcon) {
		super(smallIcon, "add_data");
	}

	@Override
	public void loggedActionPerformed(ActionEvent e) {
		DataImportWizardBuilder builder = new DataImportWizardBuilder();
		builder.setCallback(DataImportWizardUtils.showInResultsCallback());
		JDialog wizard = builder.build(RapidMinerGUI.getMainFrame()).getDialog();
		wizard.setVisible(true);
	}

}
