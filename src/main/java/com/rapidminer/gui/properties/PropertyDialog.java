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
package com.rapidminer.gui.properties;

import com.rapidminer.gui.ApplicationFrame;
import com.rapidminer.gui.tools.dialogs.ButtonDialog;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.tools.I18N;


/**
 * @author Simon Fischer, Tobias Malbrecht
 */
public class PropertyDialog extends ButtonDialog {

	private static final long serialVersionUID = -5112534796600557146L;

	private final ParameterType type;

	public PropertyDialog(final ParameterType type, String key) {
		super(ApplicationFrame.getApplicationFrame(), "parameter." + key, ModalityType.APPLICATION_MODAL, new Object[] {});
		this.type = type;
	}

	protected ParameterType getParameterType() {
		return this.type;
	}

	@Override
	protected String getInfoText() {
		return "<html>" + I18N.getMessage(I18N.getGUIBundle(), getKey() + ".title") + ": <b>"
				+ type.getKey().replace("_", " ") + "</b><br/>" + type.getDescription() + "</html>";
	}

	@Override
	protected String getDialogTitle() {
		return super.getDialogTitle() + ": " + type.getKey().replace("_", " ");
	}

	public boolean isOk() {
		return wasConfirmed();
	}
}
