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

import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JPanel;

import com.rapidminer.RapidMiner;
import com.rapidminer.gui.ApplicationFrame;
import com.rapidminer.gui.properties.SettingsDialog;
import com.rapidminer.gui.tools.ResourceActionAdapter;
import com.rapidminer.gui.tools.SwingTools;
import com.rapidminer.parameter.ParameterTypeCategory;
import com.rapidminer.tools.I18N;
import com.rapidminer.tools.ParameterService;


/**
 * A dialog that asks the user a question which can be answered with yes or no and remembers its
 * decision. The user can decide whether or not their answer is remembered. If it is remembered, the
 * dialog will not be displayed the next time.
 *
 * To use this class, define "gui.dialog.KEY.title" and "gui.dialog.KEY.message" in the GUI
 * properties file. Also, register a property using
 * {@link RapidMiner#registerRapidMinerProperty(com.rapidminer.parameter.ParameterType)} with a
 * {@link ParameterTypeCategory} where the categories are {@link #PROPERTY_VALUES} (yes, no, ask).
 * Preferrably, the default value should be {@link #ASK}. Pass the i18n key and the key of this
 * property to {@link #confirmAction(String, String)}.
 *
 * The behaviour of this dialog depends on the current value of the property.
 * <ul>
 * <li>If its value is either "true" or "false", nothing will happen and the method will simply
 * return true or false, depending on this value.</li>
 * <li>If the value is "ask", a dialog will pop up, and either true (yes) or false (false) will be
 * returned. Furthermore, if the user checks the "Remember my decision" checkbox, the property will
 * be set to the users decision ("true" or "false"), and the property will be saved to the users
 * private property file. Hence, the next call to {@link #confirmAction(String, String)} will return
 * without showing a dialog.</li>
 * </ul>
 *
 * In order to make the dialog shown again, the user must set the value back to "ask" in the
 * {@link SettingsDialog}.
 *
 * @author Simon Fischer, Marco Boeck
 *
 */
public class DecisionRememberingConfirmDialog extends ButtonDialog {

	/**
	 * The type of the dialog.
	 */
	private enum DialogType {
		/** yes/no option */
		CONFIRM,

		/** only OK option, just for user information */
		ACKNOWLEDGE;
	}

	private static final long serialVersionUID = 1L;

	private static final String VALUE_TRUE = "true";
	private static final String VALUE_FALSE = "false";
	private static final String VALUE_ASK = "ask";
	private static final String VALUE_SHOW = "show";
	private static final String VALUE_HIDE = "hide";

	public static final String[] PROPERTY_VALUES = { VALUE_TRUE, VALUE_FALSE, VALUE_ASK };
	public static final String[] PROPERTY_VALUES_ACK = { VALUE_SHOW, VALUE_HIDE };

	public static final int TRUE = 0;
	public static final int FALSE = 1;
	public static final int ASK = 2;
	public static final int SHOW = 0;
	public static final int HIDE = 1;

	private JCheckBox dontAskAgainBox;
	private DialogType type;
	private final String propertyName;
	private boolean confirmed;

	private DecisionRememberingConfirmDialog(String i18nKey, String property, DialogType type, Object... arguments) {
		super(ApplicationFrame.getApplicationFrame(), i18nKey, ModalityType.APPLICATION_MODAL, arguments);
		if (i18nKey == null) {
			throw new IllegalArgumentException("i18nKey must not be null!");
		}
		if (property == null) {
			throw new IllegalArgumentException("property must not be null!");
		}
		if (type == null) {
			throw new IllegalArgumentException("type must not be null!");
		}
		this.propertyName = property;
		this.type = type;

		JButton toFocus = null;
		JPanel buttonPanel = new JPanel(new BorderLayout());
		if (type == DialogType.CONFIRM) {
			dontAskAgainBox = new JCheckBox(new ResourceActionAdapter("remember_decision"));
			dontAskAgainBox.setSelected(false);
			JButton yesButton = makeOkButton("yes");
			JButton noButton = makeCancelButton("no");
			buttonPanel.add(dontAskAgainBox, BorderLayout.WEST);
			buttonPanel.add(makeButtonPanel(yesButton, noButton), BorderLayout.EAST);
			toFocus = noButton;
		} else if (type == DialogType.ACKNOWLEDGE) {
			dontAskAgainBox = new JCheckBox(new ResourceActionAdapter("dont_show_again"));
			dontAskAgainBox.setSelected(false);
			JButton okButton = makeOkButton("ok");
			buttonPanel.add(dontAskAgainBox, BorderLayout.WEST);
			buttonPanel.add(makeButtonPanel(okButton), BorderLayout.EAST);
			toFocus = okButton;
		}

		layoutDefault(null, buttonPanel);
		if (toFocus != null) {
			toFocus.requestFocusInWindow();
		}
	}

	@Override
	public void ok() {
		confirmed = true;
		saveIfDesired();
		dispose();
	}

	@Override
	public void cancel() {
		saveIfDesired();
		confirmed = false;
		dispose();
	}

	@Override
	protected Icon getInfoIcon() {
		// constants not possible because this class is referenced too early
		if (type == DialogType.CONFIRM) {
			return SwingTools.createIcon("48/" + I18N.getGUIMessage("gui.dialog.confirm.icon"));
		} else if (type == DialogType.ACKNOWLEDGE) {
			return SwingTools.createIcon("48/" + I18N.getGUIMessage("gui.dialog.ack.icon"));
		}
		return null;
	}

	private void saveIfDesired() {
		if (dontAskAgainBox.isSelected()) {
			String value = null;
			if (type == DialogType.CONFIRM) {
				value = confirmed ? VALUE_TRUE : VALUE_FALSE;
			} else if (type == DialogType.ACKNOWLEDGE) {
				value = VALUE_HIDE;
			}

			if (value != null) {
				ParameterService.setParameterValue(propertyName, value);
				ParameterService.saveParameters();
			}
		}
	}

	/**
	 * Shows a dialog which can be confirmed or not via yes/no buttons. If the user selects the
	 * "don't ask me again" checkbox, the user choice is remembered and applied automatically in the
	 * future.
	 *
	 * @param i18nKey
	 * @param propertyKey
	 *            the key which is used to remember the user choice
	 * @return <code>true</code> if the dialog was confirmed; <code>false</code> otherwise
	 */
	public static boolean confirmAction(String i18nKey, String propertyKey) {
		return confirmAction(i18nKey, propertyKey, new Object[0]);
	}

	/**
	 * Shows a dialog which can be confirmed or not via yes/no buttons. If the user selects the
	 * "don't ask me again" checkbox, the user choice is remembered and applied automatically in the
	 * future.
	 *
	 * @param i18nKey
	 *            i18n key
	 * @param propertyKey
	 *            the key which is used to remember the user choice
	 * @param arguments
	 *            optional i18n arguments
	 * @return <code>true</code> if the dialog was confirmed; <code>false</code> otherwise
	 */
	public static boolean confirmAction(String i18nKey, String propertyKey, Object... arguments) {
		String propValue = ParameterService.getParameterValue(propertyKey);
		if (propValue != null) {
			if (propValue.equals(VALUE_TRUE)) {
				return true;
			} else if (propValue.equals(VALUE_FALSE)) {
				return false;
			}
		}
		DecisionRememberingConfirmDialog d = new DecisionRememberingConfirmDialog(i18nKey, propertyKey, DialogType.CONFIRM,
				arguments);
		d.setVisible(true);
		return d.confirmed;
	}

	/**
	 * Shows a dialog which can only be acknowledged via OK button. If the user selects the
	 * "don't tell me again" checkbox, this dialog will no longer be shown.
	 *
	 * @param i18nKey
	 *            i18n key
	 * @param propertyKey
	 *            the key which is used to remember the user choice
	 * @param arguments
	 *            optional i18n arguments
	 */
	public static void acknowledgeAction(String i18nKey, String propertyKey, Object... arguments) {
		String propValue = ParameterService.getParameterValue(propertyKey);
		if (VALUE_HIDE.equals(propValue)) {
			return;
		}
		DecisionRememberingConfirmDialog d = new DecisionRememberingConfirmDialog(i18nKey, propertyKey,
				DialogType.ACKNOWLEDGE, arguments);
		d.setVisible(true);
	}
}
