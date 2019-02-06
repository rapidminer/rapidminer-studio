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
package com.rapidminer.gui.tools.dialogs;

import java.awt.BorderLayout;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Collection;

import javax.swing.AbstractButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JPanel;

import com.rapidminer.gui.ApplicationFrame;


/**
 *
 * @author Tobias Malbrecht
 */
public class MultiConfirmDialog extends ConfirmDialog {

	private static final long serialVersionUID = 1L;

	private boolean applyToAll = false;

	/**
	 * Creates a confirm dialog where the user can select that his selection should apply to all
	 * cases.
	 *
	 * @param i18nKey
	 *            the i18n key
	 * @param mode
	 *            the {@link ConfirmDialog}
	 * @param i18nArgs
	 *            additional i18n arguments
	 * @deprecated use {@link #MultiConfirmDialog(Window, String, int, Object...)} instead
	 */
	@Deprecated
	public MultiConfirmDialog(String i18nKey, int mode, Object... i18nArgs) {
		this(ApplicationFrame.getApplicationFrame(), i18nKey, mode, i18nArgs);
	}

	/**
	 * Creates a confirm dialog where the user can select that his selection should apply to all
	 * cases.
	 *
	 * @param owner
	 *            the owner window where this dialog will be shown in
	 * @param i18nKey
	 *            the i18n key
	 * @param mode
	 *            the {@link ConfirmDialog}
	 * @param i18nArgs
	 *            additional i18n arguments
	 * @since 6.5.0
	 */
	public MultiConfirmDialog(Window owner, String i18nKey, int mode, Object... i18nArgs) {
		super(owner, i18nKey, mode, false, i18nArgs);
	}

	@Override
	protected void layoutDefault(JComponent centerComponent, Collection<AbstractButton> buttons) {
		final JCheckBox applyToAllCheckBox = new JCheckBox("Apply to All", false);
		{
			applyToAllCheckBox.addActionListener(new ActionListener() {

				@Override
				public void actionPerformed(ActionEvent e) {
					applyToAll = applyToAllCheckBox.isSelected();
				}

			});
		}
		JPanel buttonPanel = new JPanel(new BorderLayout());
		buttonPanel.add(applyToAllCheckBox, BorderLayout.WEST);
		buttonPanel.add(makeButtonPanel(buttons), BorderLayout.EAST);
		layoutDefault(null, buttonPanel);
	}

	public boolean applyToAll() {
		return applyToAll;
	}
}
