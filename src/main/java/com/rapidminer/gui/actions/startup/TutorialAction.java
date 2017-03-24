/**
 * Copyright (C) 2001-2017 by RapidMiner and the contributors
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
package com.rapidminer.gui.actions.startup;

import java.awt.event.ActionEvent;

import com.rapidminer.gui.tools.ResourceAction;
import com.rapidminer.gui.tools.SwingTools;
import com.rapidminer.studio.internal.NoStartupDialogRegistreredException;
import com.rapidminer.studio.internal.StartupDialogRegistry;
import com.rapidminer.studio.internal.StartupDialogProvider.ToolbarButton;


/**
 * Opens the getting started dialog showing new process templates.
 *
 * @author Marcel Michel
 * @since 7.0.0
 */
public class TutorialAction extends ResourceAction {

	private static final long serialVersionUID = 1L;

	public TutorialAction() {
		super("toolbar_resources.tutorials");
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		try {
			StartupDialogRegistry.INSTANCE.showStartupDialog(ToolbarButton.TUTORIAL);
		} catch (NoStartupDialogRegistreredException e1) {
			SwingTools.showVerySimpleErrorMessage("tutorials_not_available");
		}
	}
}
