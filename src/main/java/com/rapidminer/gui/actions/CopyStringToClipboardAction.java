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
import java.util.function.Supplier;

import com.rapidminer.gui.tools.ResourceAction;
import com.rapidminer.tools.Tools;
import com.rapidminer.tools.ValidationUtil;


/**
 * The action copies the string given by the supplier to the clipboard.
 *
 * @author Marco Boeck
 * @since 9.3.0
 */
public class CopyStringToClipboardAction extends ResourceAction {

	private Supplier<String> stringSupplier;


	/**
	 * Creates a new {@link CopyStringToClipboardAction} instance.
	 *
	 * @param smallIcon
	 *        {@code true} for a 16px icon; {@code false} for a 24px icon
	 * @param i18nKey
	 * 		the i18n key which will be part of the composite key {@code gui.action.{i18nkey}} for the action
	 * @param stringSupplier
	 * 		the supplier which supplies the string when the action is performed, must not be {@code null}
	 */
	public CopyStringToClipboardAction(boolean smallIcon, String i18nKey, Supplier<String> stringSupplier) {
		super(smallIcon, i18nKey);
		this.stringSupplier = ValidationUtil.requireNonNull(stringSupplier, "stringSupplier");
	}

	@Override
	public void loggedActionPerformed(ActionEvent e) {
		Tools.copyStringToClipboard(stringSupplier.get());
	}

}
