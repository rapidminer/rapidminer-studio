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

import java.awt.Dialog;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.Collection;
import java.util.LinkedList;

import javax.swing.AbstractButton;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.KeyStroke;

import com.rapidminer.gui.ApplicationFrame;
import com.rapidminer.gui.tools.ResourceAction;
import com.rapidminer.gui.tools.ResourceActionAdapter;
import com.rapidminer.gui.tools.SwingTools;
import com.rapidminer.tools.I18N;
import com.rapidminer.tools.ParameterService;


/**
 * @author Tobias Malbrecht, Adrian Wilke
 */
public class ConfirmDialog extends ButtonDialog {

	private static final long serialVersionUID = -5825873580778775409L;

	public static final int OK_OPTION = JOptionPane.OK_OPTION;

	public static final int YES_OPTION = JOptionPane.YES_OPTION;

	public static final int NO_OPTION = JOptionPane.NO_OPTION;

	public static final int CANCEL_OPTION = JOptionPane.CANCEL_OPTION;

	public static final int CLOSED_OPTION = JOptionPane.CLOSED_OPTION;

	public static final int OK_CANCEL_OPTION = JOptionPane.OK_CANCEL_OPTION;

	public static final int YES_NO_OPTION = JOptionPane.YES_NO_OPTION;

	public static final int YES_NO_CANCEL_OPTION = JOptionPane.YES_NO_CANCEL_OPTION;

	protected int returnOption = CANCEL_OPTION;

	private JCheckBox dontAskAgainCheckbox = null;

	/**
	 * Creates a confirm dialog where the user can chose his action.
	 *
	 * @param key
	 *            the i18n key
	 * @param mode
	 *            see the static constants of {@link ConfirmDialog}
	 * @param showAskAgainCheckbox
	 *            the user can chose to never be asked again
	 * @param arguments
	 *            additional i18n arguments
	 * @deprecated use {@link #ConfirmDialog(Window, String, int, boolean, Object...)} instead
	 */
	@Deprecated
	public ConfirmDialog(String key, int mode, boolean showAskAgainCheckbox, Object... arguments) {
		this(ApplicationFrame.getApplicationFrame(), key, mode, showAskAgainCheckbox, arguments);
	}

	/**
	 * Creates a confirm dialog where the user can chose his action.
	 *
	 * @param owner
	 *            the owner of the dialog
	 * @param key
	 *            the i18n key
	 * @param mode
	 *            see the static constants of {@link ConfirmDialog}
	 * @param showAskAgainCheckbox
	 *            the user can chose to never be asked again
	 * @param arguments
	 *            additional i18n arguments
	 */
	public ConfirmDialog(Dialog owner, String key, int mode, boolean showAskAgainCheckbox, Object... arguments) {
		this((Window) owner, key, mode, showAskAgainCheckbox, arguments);
	}

	/**
	 * Creates a confirm dialog where the user can chose his action.
	 *
	 * @param owner
	 *            the owner of the dialog
	 * @param key
	 *            the i18n key
	 * @param mode
	 *            see the static constants of {@link ConfirmDialog}
	 * @param showAskAgainCheckbox
	 *            the user can chose to never be asked again
	 * @param arguments
	 *            additional i18n arguments
	 * @since 6.5.0
	 */
	public ConfirmDialog(Window owner, String key, int mode, boolean showAskAgainCheckbox, Object... arguments) {
		super(owner, "confirm." + key, ModalityType.APPLICATION_MODAL, arguments);
		constructConfirmDialog(mode, showAskAgainCheckbox);
	}

	private void constructConfirmDialog(int mode, boolean showAskAgainCheckbox) {
		Collection<AbstractButton> buttons = new LinkedList<>();
		switch (mode) {
			case OK_CANCEL_OPTION:
				buttons.add(makeOkButton());
				buttons.add(makeCancelButton());
				break;
			case YES_NO_OPTION:
				buttons.add(makeYesButton());
				buttons.add(makeNoButton());
				break;
			case YES_NO_CANCEL_OPTION:
				buttons.add(makeYesButton());
				buttons.add(makeNoButton());
				buttons.add(makeCancelButton());
				break;
			default:
				break;
		}

		if (showAskAgainCheckbox) {
			this.dontAskAgainCheckbox = new JCheckBox(new ResourceActionAdapter("dont_ask_again"));
		}
		layoutDefault(this.dontAskAgainCheckbox, buttons);
	}

	@Override
	protected Icon getInfoIcon() {
		String iconKey = I18N.getMessageOrNull(I18N.getGUIBundle(), getKey() + ".icon");
		if (iconKey == null) {
			return SwingTools.createIcon("48/" + I18N.getMessage(I18N.getGUIBundle(), "gui.dialog.confirm.icon"));
		} else {
			return SwingTools.createIcon("48/" + iconKey);
		}
	}

	@Override
	protected JButton makeOkButton() {
		JButton okButton = new JButton(new ResourceAction("ok") {

			private static final long serialVersionUID = -8887199234055845095L;

			@Override
			public void loggedActionPerformed(ActionEvent e) {
				returnOption = OK_OPTION;
				ok();
			}
		});
		getRootPane().setDefaultButton(okButton);
		return okButton;
	}

	@Override
	protected JButton makeCancelButton() {
		ResourceAction cancelAction = new ResourceAction("cancel") {

			private static final long serialVersionUID = -8887199234055845095L;

			@Override
			public void loggedActionPerformed(ActionEvent e) {
				returnOption = CANCEL_OPTION;
				cancel();
			}
		};
		JButton cancelButton = new JButton(cancelAction);
		getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
				KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0, false), "CANCEL");
		getRootPane().getActionMap().put("CANCEL", cancelAction);

		return cancelButton;
	}

	protected JButton makeYesButton() {
		JButton yesButton = new JButton(new ResourceAction("confirm.yes") {

			private static final long serialVersionUID = -8887199234055845095L;

			@Override
			public void loggedActionPerformed(ActionEvent e) {
				returnOption = YES_OPTION;
				yes();
			}
		});
		getRootPane().setDefaultButton(yesButton);
		return yesButton;
	}

	protected JButton makeNoButton() {
		return makeNoButtonInternal("confirm.no");
	}

	protected JButton makeNoButtonInternal(String i18nKey) {
		ResourceAction noAction = new ResourceAction(i18nKey) {

			private static final long serialVersionUID = -8887199234055845095L;

			@Override
			public void loggedActionPerformed(ActionEvent e) {
				returnOption = NO_OPTION;
				no();
			}
		};
		getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
				KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0, false), "NO");
		getRootPane().getActionMap().put("NO", noAction);
		return new JButton(noAction);
	}

	@Override
	protected void ok() {
		dispose();
	}

	@Override
	protected void cancel() {
		dispose();
	}

	protected void yes() {
		dispose();
	}

	protected void no() {
		dispose();
	}

	public int getReturnOption() {
		return returnOption;
	}

	public boolean isDontAskAgainCheckboxSelected() {
		if (dontAskAgainCheckbox == null) {
			return false;
		} else {
			return dontAskAgainCheckbox.isSelected();
		}
	}

	protected void setReturnOption(int option) {
		this.returnOption = option;
	}

	/**
	 * Shows a dialog and returns the return code of the dialog.
	 *
	 * @param key
	 *            The i18n-key. Will be prepended with gui.dialog.confirm and prefixed with .title,
	 *            .message and .icon .
	 * @param mode
	 *            One of {@link #OK_CANCEL_OPTION}, {@link #YES_NO_OPTION} and
	 *            {@link #YES_NO_CANCEL_OPTION}
	 * @param propertyConfirmExit
	 *            If null, the dialog is shown in any case. If not null, the parameter specified
	 *            will be from fetched from {@link ParameterService}. If it is "false", the dialog
	 *            will not be shown.
	 * @param defaultOption
	 *            Will be returned if the dialog is not shown (see parameter propertyConfirmExit.
	 * @param i18nArgs
	 *            Arguments to the i18n string.
	 * @return The return code of the dialog, one of the constants specified above.
	 * @deprecated use {@link #showConfirmDialog(Dialog, String, int, String, int, Object...)}
	 *             instead
	 *
	 */
	@Deprecated
	public static int showConfirmDialog(String key, int mode, String propertyConfirmExit, int defaultOption,
			Object... i18nArgs) {
		return showConfirmDialog(ApplicationFrame.getApplicationFrame(), key, mode, propertyConfirmExit, defaultOption,
				true, i18nArgs);
	}

	/**
	 * Shows a dialog and returns the return code of the dialog.
	 *
	 * @param owner
	 *            the owner where the dialog will be shown in
	 * @param key
	 *            The i18n-key. Will be prepended with gui.dialog.confirm and prefixed with .title,
	 *            .message and .icon .
	 * @param mode
	 *            One of {@link #OK_CANCEL_OPTION}, {@link #YES_NO_OPTION} and
	 *            {@link #YES_NO_CANCEL_OPTION}
	 * @param propertyConfirmExit
	 *            If null, the dialog is shown in any case. If not null, the parameter specified
	 *            will be from fetched from {@link ParameterService}. If it is "false", the dialog
	 *            will not be shown.
	 * @param defaultOption
	 *            Will be returned if the dialog is not shown (see parameter propertyConfirmExit.
	 * @param i18nArgs
	 *            Arguments to the i18n string.
	 * @return The return code of the dialog, one of the constants specified above.
	 * @since 6.5.0
	 */
	public static int showConfirmDialog(Window owner, String key, int mode, String propertyConfirmExit, int defaultOption,
			Object... i18nArgs) {
		return showConfirmDialogWithOptionalCheckbox(owner, key, mode, propertyConfirmExit, defaultOption, true, i18nArgs);
	}

	/**
	 * Same as {@link #showConfirmDialog(Window, String, int, String, int, Object...)} with the
	 * additional option showAskAgainCheckbox.
	 *
	 * @deprecated use
	 *             {@link #showConfirmDialogWithOptionalCheckbox(Window, String, int, String, int, boolean, Object...)}
	 *             instead
	 */
	@Deprecated
	public static int showConfirmDialogWithOptionalCheckbox(String key, int mode, String propertyConfirmExit,
			int defaultOption, boolean showAskAgainCheckbox, Object... i18nArgs) {
		return showConfirmDialogWithOptionalCheckbox(ApplicationFrame.getApplicationFrame(), key, mode, propertyConfirmExit,
				defaultOption, showAskAgainCheckbox, i18nArgs);
	}

	/**
	 * Same as {@link #showConfirmDialog(Window, String, int, String, int, Object...)} with the
	 * additional option showAskAgainCheckbox.
	 *
	 * @since 6.5.0
	 */
	public static int showConfirmDialogWithOptionalCheckbox(Window owner, String key, int mode, String propertyConfirmExit,
			int defaultOption, boolean showAskAgainCheckbox, Object... i18nArgs) {
		if (propertyConfirmExit == null) {
			ConfirmDialog dialog = new ConfirmDialog(owner, key, mode, showAskAgainCheckbox, i18nArgs);
			dialog.setVisible(true);
			return dialog.getReturnOption();
		} else {
			String askProperty = ParameterService.getParameterValue(propertyConfirmExit);
			if (!"false".equals(askProperty)) {
				ConfirmDialog dialog = new ConfirmDialog(owner, key, mode, showAskAgainCheckbox, i18nArgs);
				dialog.setVisible(true);
				ParameterService.setParameterValue(propertyConfirmExit,
						Boolean.toString(!dialog.dontAskAgainCheckbox.isSelected()));
				ParameterService.saveParameters();
				return dialog.getReturnOption();
			} else {
				return defaultOption;
			}
		}
	}
}
