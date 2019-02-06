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
package com.rapidminer.gui.tools;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.net.PasswordAuthentication;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;

import com.rapidminer.RapidMiner;
import com.rapidminer.gui.ApplicationFrame;
import com.rapidminer.gui.RapidMinerGUI;
import com.rapidminer.gui.security.UserCredential;
import com.rapidminer.gui.security.Wallet;
import com.rapidminer.gui.tools.dialogs.ButtonDialog;
import com.rapidminer.tools.I18N;
import com.rapidminer.tools.LogService;
import com.rapidminer.tools.ParameterService;
import com.rapidminer.tools.PasswordInputCanceledException;


/**
 * Dialog asking for username and passwords. Answers may be cached (if chosen by user).
 *
 * @author Simon Fischer
 *
 */
public class PasswordDialog extends ButtonDialog {

	/**
	 * Determines if the PasswordDialog should be suppressed
	 */
	private static AtomicInteger suppressionCount = new AtomicInteger(0);

	/**
	 * Checks if a suppression exists
	 *
	 * @return true if one or more suppressions exist
	 */
	private static boolean isSuppressed() {
		return suppressionCount.get() > 0;
	}

	/**
	 * Prevents the dialog from being shown to the user
	 * <p>
	 * Warning: You must remove the suppression from the PasswordDialog after you are done!
	 * </p>
	 */
	public static void addSuppression() {
		suppressionCount.incrementAndGet();
	}

	/**
	 * Removes the suppression from the PasswordDialog, must be called after addSuppression
	 */
	public static void removeSuppression() {
		suppressionCount.decrementAndGet();
	}

	private static final long serialVersionUID = 1L;

	private final JTextField usernameField = new JTextField(20);
	private final JPasswordField passwordField = new JPasswordField(20);
	private final JCheckBox rememberBox = new JCheckBox(new ResourceActionAdapter("authentication.remember"));
	private final JLabel enforcedIcon = new ResourceLabel("preferences.setting_enforced");

	private PasswordDialog(Window owner, String i18nKey, UserCredential preset, Object... args) {
		super(owner, i18nKey, ModalityType.APPLICATION_MODAL, args);
		setModal(true);
		if (preset != null && preset.getUsername() != null) {
			usernameField.setText(preset.getUsername());
		}
		if (preset != null && preset.getPassword() != null) {
			passwordField.setText(new String(preset.getPassword()));
			rememberBox.setSelected(true);
		}
		String url = preset != null ? preset.getURL() : null;

		JPanel main = new JPanel(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.BOTH;
		c.anchor = GridBagConstraints.FIRST_LINE_START;
		c.insets = new Insets(4, 4, 4, 4);

		JLabel label = new ResourceLabel("authentication.username", url);
		label.setLabelFor(usernameField);
		c.gridwidth = GridBagConstraints.RELATIVE;
		main.add(label, c);
		c.gridwidth = GridBagConstraints.REMAINDER;
		main.add(usernameField, c);

		label = new ResourceLabel("authentication.password", url);
		label.setLabelFor(passwordField);
		c.gridwidth = GridBagConstraints.RELATIVE;
		main.add(label, c);
		c.gridwidth = GridBagConstraints.REMAINDER;
		main.add(passwordField, c);

		JPanel rememberPanel = new JPanel();
		rememberPanel.add(rememberBox);
		if (Boolean.parseBoolean(ParameterService.getParameterValue(RapidMinerGUI.PROPERTY_RAPIDMINER_DISALLOW_REMEMBER_PASSWORD))) {
			rememberPanel.add(enforcedIcon);
			rememberBox.setSelected(false);
			rememberBox.setEnabled(false);
		}

		main.add(rememberPanel, c);

		layoutDefault(main, makeOkButton(), makeCancelButton());

	}

	public PasswordAuthentication makeAuthentication() {
		return new PasswordAuthentication(usernameField.getText(), passwordField.getPassword());
	}

	public static PasswordAuthentication getPasswordAuthentication(String forUrl, boolean forceRefresh)
			throws PasswordInputCanceledException {
		return getPasswordAuthentication(forUrl, forceRefresh, false);
	}

	/**
	 * @param id
	 *            the ID to accompany the given url. Used to differentiate multiple entries for the
	 *            same url.
	 * @param i18nKey
	 *            The i18nKey has to look like every dialog i18n: gui.dialog.KEY.title etc.
	 * @param args
	 *            Will be used when i18nKey is set. If i18nKey is <code>null</code> it will be not
	 *            be used
	 **/
	@SuppressWarnings("deprecation")
	public static PasswordAuthentication getPasswordAuthentication(String id, String forUrl, boolean forceRefresh,
			boolean hideDialogIfPasswordKnown, String i18nKey, Object... args) throws PasswordInputCanceledException {

		// First check whether the credentials are stored within the Wallet
		UserCredential authentication = Wallet.getInstance().getEntry(id, forUrl);

		// return immediately if known and desired
		if (hideDialogIfPasswordKnown && !forceRefresh && authentication != null && authentication.getPassword() != null) {

			if (authentication.getPassword().length == 0) {
				return null;
			}

			LogService.getRoot().log(Level.CONFIG, "com.rapidminer.gui.tools.PassworDialog.reusing_cached_password", forUrl);
			return authentication.makePasswordAuthentication();
		}

		// If password was not stored within the wallet or a refresh is forced, check whether we are
		// in headless mode
		if (RapidMiner.getExecutionMode().isHeadless()) {
			LogService.getRoot().log(Level.WARNING, "com.rapidminer.gui.tools.PassworDialog.no_query_password_in_batch_mode",
					forUrl);
			return null;
		}
		// Throw away all requests before the UI is ready
		if (!RapidMinerGUI.isInitialized() || !ApplicationFrame.getApplicationFrame().isVisible()) {
			return null;
		}

		// If the PasswordDialog is suppressed for reasons don't show it
		if (PasswordDialog.isSuppressed()) {
			return null;
		}

		char[] oldPassword = null;
		// clear cache if refresh forced
		if (forceRefresh && authentication != null) {
			oldPassword = authentication.getPassword();
			authentication.setPassword(null);
			Wallet.getInstance().registerCredentials(id, authentication);
		}
		if (authentication == null) {
			authentication = new UserCredential(forUrl, null, null);
		}
		// only set to default authentication key if provided key not available
		if (i18nKey == null || i18nKeyNotAvailable(i18nKey)) {
			i18nKey = "authentication";
			args = new Object[] { authentication.getURL() };
		}

		final Object[] pdArgs = args;
		final UserCredential passwordDialogCredentials = authentication;
		final String passwordI18N = i18nKey;

		// create PasswordDialog on EDT
		final PasswordDialog pd = SwingTools.invokeAndWaitWithResult(() -> {
			PasswordDialog pd1 = new PasswordDialog(ApplicationFrame.getApplicationFrame(), passwordI18N,
					passwordDialogCredentials, pdArgs);
			// Onboarding is already a APPLICATION_MODAL
			pd1.setModalityType(ModalityType.TOOLKIT_MODAL);
			pd1.setVisible(true);
			return pd1;
		});
		if (!pd.wasConfirmed()) {
			// If the user canceled the Password Dialog on forceRefresh the password is at this
			// point null. Restore old value to prevent NPEs.
			if (forceRefresh) {
				authentication.setPassword(oldPassword);
			}
			throw new PasswordInputCanceledException();
		}
		PasswordAuthentication result = pd.makeAuthentication();
		if (pd.rememberBox.isSelected()) {
			UserCredential userCredential = new UserCredential(forUrl, result.getUserName(), result.getPassword());
			if (id != null) {
				Wallet.getInstance().registerCredentials(id, userCredential);
			} else {
				// compatibility with old API
				Wallet.getInstance().registerCredentials(userCredential);
			}
		} else if (id != null) {
			Wallet.getInstance().removeEntry(id, forUrl);
		} else {
			// compatibility with old API
			Wallet.getInstance().removeEntry(forUrl);
		}
		Wallet.getInstance().saveCache();

		return result;
	}

	/**
	 * Tests the availability of a given i18n key. More specifically tests if the key
	 * <p>{@code gui.dialog.<i18nKey>.title} can be found.
	 *
	 * @param i18nKey the i18n key to test for dialog purposes
	 * @return whether the key is available
	 * @since 9.0.2
	 */
	private static boolean i18nKeyNotAvailable(String i18nKey) {
		String fullKey = "gui.dialog." + i18nKey + ".title";
		return fullKey.equals(I18N.getMessage(I18N.getGUIBundle(), fullKey));
	}

	public static PasswordAuthentication getPasswordAuthentication(String forUrl, boolean forceRefresh,
			boolean hideDialogIfPasswordKnown) throws PasswordInputCanceledException {
		return getPasswordAuthentication(forUrl, forceRefresh, hideDialogIfPasswordKnown, null);
	}

	/**
	 * @param i18nKey
	 *            The i18nKey has to look like every dialog i18n: gui.dialog.KEY.title etc.
	 * @param args
	 *            Will be used when i18nKey is set. If i18nKey is <code>null</code> it will be not
	 *            be used
	 * @deprecated use
	 *             {@link #getPasswordAuthentication(String, String, boolean, boolean, String, Object...)}
	 *             instead.
	 **/
	@Deprecated
	public static PasswordAuthentication getPasswordAuthentication(String forUrl, boolean forceRefresh,
			boolean hideDialogIfPasswordKnown, String i18nKey, Object... args) throws PasswordInputCanceledException {
		return getPasswordAuthentication(null, forUrl, forceRefresh, hideDialogIfPasswordKnown, null);
	}
}
