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
package com.rapidminer.gui.look.ui;

import javax.swing.AbstractButton;
import javax.swing.JComponent;
import javax.swing.UIManager;
import javax.swing.plaf.ComponentUI;


/**
 * The UI for a check box.
 * 
 * @author Ingo Mierswa
 */
public class CheckBoxUI extends RadioButtonUI {

	private static final String propertyPrefix = "CheckBox.";

	private boolean initialize = false;

	public static ComponentUI createUI(JComponent jcomponent) {
		return new CheckBoxUI();
	}

	@Override
	public String getPropertyPrefix() {
		return propertyPrefix;
	}

	@Override
	public void installDefaults(AbstractButton abstractbutton) {
		super.installDefaults(abstractbutton);
		if (!this.initialize) {
			this.icon = UIManager.getIcon(getPropertyPrefix() + "icon");
			this.initialize = true;
		}
		abstractbutton.setRolloverEnabled(true);
	}

	@Override
	protected void uninstallDefaults(AbstractButton abstractbutton) {
		super.uninstallDefaults(abstractbutton);
		this.initialize = false;
	}
}
