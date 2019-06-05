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
package com.rapidminer.connection.gui.actions;

import java.awt.Window;
import java.awt.event.ActionEvent;
import java.util.function.BooleanSupplier;

import com.rapidminer.gui.tools.ResourceAction;
import com.rapidminer.gui.tools.dialogs.ConfirmDialog;


/**
 * Displays a confirm dialog on close if unsaved changed exist
 *
 * @author Jonas Wilms-Pfau
 * @since 9.3
 */
public class CancelEditingAction extends ResourceAction {

	private static final long serialVersionUID = 1L;

	private final Window window;
	private final transient BooleanSupplier hasUnsavedChanges;

	/**
	 * Creates a new CancelEditingAction
	 *
	 * @param dialog
	 * 		the window that should be closed
	 * @param i18nKey
	 * 		the i18n key used for both the resource and the ConfirmDialog
	 * @param hasUnsavedChanges
	 * 		supplier if unsaved changes exists
	 */
	public CancelEditingAction(Window dialog, String i18nKey, BooleanSupplier hasUnsavedChanges) {
		super(i18nKey);
		this.window = dialog;
		this.hasUnsavedChanges = hasUnsavedChanges;
	}

	@Override
	protected void loggedActionPerformed(ActionEvent e) {
		if (!hasUnsavedChanges.getAsBoolean() ||
				ConfirmDialog.YES_OPTION == ConfirmDialog.showConfirmDialogWithOptionalCheckbox(window,
						getKey(), ConfirmDialog.YES_NO_OPTION, null,
						ConfirmDialog.NO_OPTION, false)) {
			window.dispose();
		}
	}
}
