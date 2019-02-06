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
package com.rapidminer.gui.properties.celleditors.value;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.util.LinkedList;

import javax.swing.AbstractButton;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

import com.rapidminer.gui.ApplicationFrame;
import com.rapidminer.gui.look.borders.RoundTitledBorder;
import com.rapidminer.gui.tools.ProgressThread;
import com.rapidminer.gui.tools.ResourceAction;
import com.rapidminer.gui.tools.ResourceActionAdapter;
import com.rapidminer.gui.tools.components.LinkLocalButton;
import com.rapidminer.gui.tools.dialogs.ButtonDialog;
import com.rapidminer.parameter.OAuthMechanism;
import com.rapidminer.tools.I18N;
import com.rapidminer.tools.RMUrlHandler;


/**
 * Dialog which uses an {@link OAuthMechanism} to authenticate RapidMiner via OAuth.
 *
 * @author Marcel Michel
 *
 */
public class OAuthDialog extends ButtonDialog {

	private static final long serialVersionUID = -3332118850917625521L;

	/** displays error and status messages */
	private JLabel statusLabel;

	/** the authorization url textField */
	private JTextField authUrlText;

	/** the code textField */
	private JTextField codeText;

	/** the OAuth mechanism */
	private OAuthMechanism oAuth;

	private JButton confirmButton;

	private JButton cancelButton;

	private JButton urlButton;

	/**
	 * Constructs the dialog and uses an {@link OAuthMechanism} for authentication
	 *
	 * @param oAuthMechanism
	 * @deprecated use {@link #OAuthDialog(Window, OAuthMechanism)} instead
	 */
	@Deprecated
	public OAuthDialog(OAuthMechanism oAuthMechanism) {
		this(ApplicationFrame.getApplicationFrame(), oAuthMechanism);
	}

	/**
	 * Constructs the dialog and uses an {@link OAuthMechanism} for authentication
	 *
	 * @param owner
	 *            the owner window in which this dialog should appear
	 * @param oAuthMechanism
	 *            the mechanism
	 * @since 6.5.0
	 */
	public OAuthDialog(Window owner, OAuthMechanism oAuthMechanism) {
		super(owner, "oauth_dialog", ModalityType.MODELESS, new Object[] {});
		this.oAuth = oAuthMechanism;
		initGUI();
		startOAuth();
	}

	/**
	 * Basic method to initialize the GUI.
	 */
	private void initGUI() {
		JPanel outerPanel = new JPanel();
		outerPanel.setLayout(new GridBagLayout());
		GridBagConstraints gbc = new GridBagConstraints();

		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.weightx = 1;
		gbc.fill = GridBagConstraints.BOTH;
		outerPanel.add(createStep1Panel(), gbc);

		if (oAuth.isOAuth2()) {
			gbc.gridy += 1;
			outerPanel.add(createStep2Panel(), gbc);
		}

		statusLabel = new JLabel();
		statusLabel.setMinimumSize(new Dimension(400, 25));
		statusLabel.setPreferredSize(new Dimension(400, 25));
		statusLabel.setHorizontalAlignment(JLabel.RIGHT);
		gbc.gridy += 1;
		gbc.insets = new Insets(5, 5, 5, 5);
		outerPanel.add(statusLabel, gbc);

		gbc.gridy += 1;
		Component btPanel = createButtons();
		outerPanel.add(btPanel, gbc);

		add(outerPanel);
		layoutDefault(outerPanel, confirmButton, cancelButton);
		if (oAuth.isOAuth2()) {
			setPreferredSize(new Dimension(375, 430));
		} else {
			setPreferredSize(new Dimension(375, 320));
		}
		setResizable(false);
		setModal(true);
		pack();
		setLocationRelativeTo(ApplicationFrame.getApplicationFrame());
	}

	/**
	 * Creates and returns the GUI components for step1.
	 *
	 * @return
	 */
	private Component createStep1Panel() {
		JPanel panel = new JPanel();
		panel.setLayout(new GridBagLayout());
		GridBagConstraints gbc = new GridBagConstraints();

		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.weightx = 1;
		gbc.fill = GridBagConstraints.BOTH;
		gbc.insets = new Insets(0, 51, 0, 0);
		urlButton = new JButton(new ResourceAction(false, "oauth_dialog.wait_for_url") {

			private static final long serialVersionUID = 1154127549553798757L;

			@Override
			public void loggedActionPerformed(ActionEvent e) {
				open(authUrlText.getText());

			}

		});
		gbc.gridx += 1;
		urlButton.setEnabled(false);
		panel.add(urlButton, gbc);

		authUrlText = new JTextField();
		authUrlText.setEditable(false);
		authUrlText.setVisible(false);
		authUrlText.setBackground(Color.WHITE);
		authUrlText.setMinimumSize(new Dimension(80, 33));
		authUrlText.setPreferredSize(new Dimension(80, 33));
		authUrlText.addFocusListener(new FocusListener() {

			@Override
			public void focusLost(FocusEvent e) {
				// nothing
			}

			@Override
			public void focusGained(FocusEvent e) {
				authUrlText.selectAll();

			}
		});
		panel.add(authUrlText, gbc);

		LinkLocalButton showUrlButton = new LinkLocalButton(new ResourceAction(true, "oauth_dialog.show_url") {

			private static final long serialVersionUID = -5000971936611417944L;

			@Override
			public void loggedActionPerformed(ActionEvent e) {
				urlButton.setVisible(false);
				authUrlText.setVisible(true);

			}

		});

		// pretty right alignment method
		SimpleAttributeSet rightAlignment = new SimpleAttributeSet();
		StyledDocument styledDoc = (StyledDocument) showUrlButton.getDocument();
		StyleConstants.setAlignment(rightAlignment, StyleConstants.ALIGN_RIGHT);
		styledDoc.setParagraphAttributes(0, styledDoc.getLength(), rightAlignment, false);

		gbc.gridx = 1;
		gbc.gridy += 1;
		panel.add(showUrlButton, gbc);

		panel.setBorder(new RoundTitledBorder(1, I18N.getMessage(I18N.getGUIBundle(),
				"gui.dialog.oauth_dialog.open_url.label"), false));
		return panel;
	}

	/**
	 * Creates and returns the GUI components for step2.
	 *
	 * @return
	 */
	private Component createStep2Panel() {
		JPanel panel = new JPanel();
		panel.setLayout(new GridBagLayout());
		GridBagConstraints gbc = new GridBagConstraints();

		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.weightx = 1;
		gbc.fill = GridBagConstraints.BOTH;
		gbc.insets = new Insets(0, 51, 0, 0);

		codeText = new JTextField();
		codeText.setMinimumSize(new Dimension(80, 33));
		codeText.setPreferredSize(new Dimension(80, 33));
		panel.add(codeText, gbc);

		panel.setBorder(new RoundTitledBorder(2, I18N.getMessage(I18N.getGUIBundle(),
				"gui.dialog.oauth_dialog.copy_code.label"), false));
		return panel;
	}

	/**
	 * Creates and returns the button component.
	 *
	 * @return
	 */
	private Component createButtons() {
		LinkedList<AbstractButton> buttons = new LinkedList<>();

		confirmButton = new JButton(new ResourceActionAdapter(false, "oauth_dialog.ok") {

			private static final long serialVersionUID = 1L;

			@Override
			public void loggedActionPerformed(ActionEvent e) {
				if (oAuth.isOAuth2() && !validateCode()) {
					return;
				}
				endOAuth();
			}

		});
		buttons.add(confirmButton);

		ResourceAction cancelAction = new ResourceAction(false, "oauth_dialog.cancel") {

			private static final long serialVersionUID = 1L;

			@Override
			public void loggedActionPerformed(ActionEvent e) {
				OAuthDialog.this.dispose();
			}

		};

		cancelButton = new JButton(cancelAction);
		buttons.add(cancelButton);

		getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
				KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0, false), "CANCEL");
		getRootPane().getActionMap().put("CANCEL", cancelAction);

		return makeButtonPanel(buttons);
	}

	/**
	 * Calls the startOAuth in a {@link ProgressThread} and displays the result in the
	 * {@link #statusLabel}
	 */
	private void startOAuth() {
		ProgressThread actionThread = new ProgressThread("oauth_action_start") {

			@Override
			public void run() {
				// show user we are doing something
				SwingUtilities.invokeLater(new Runnable() {

					@Override
					public void run() {
						OAuthDialog.this.displayStatus(I18N.getMessage(I18N.getGUIBundle(),
								"gui.dialog.oauth_dialog.status.start_oauth.message"));
					}
				});

				final String authorizeUrl = oAuth.startOAuth();

				// show user results
				SwingUtilities.invokeLater(new Runnable() {

					@Override
					public void run() {
						if (authorizeUrl != null) {
							OAuthDialog.this.displayStatus("");
							OAuthDialog.this.authUrlText.setText(authorizeUrl);
							OAuthDialog.this.authUrlText.setCaretPosition(0);
							OAuthDialog.this.urlButton.setEnabled(true);
							OAuthDialog.this.urlButton.setText(I18N.getMessage(I18N.getGUIBundle(),
									"gui.action.oauth_dialog.open_url.label"));
							OAuthDialog.this.urlButton.setToolTipText(I18N.getMessage(I18N.getGUIBundle(),
									"gui.action.oauth_dialog.open_url.tip"));
						} else {
							OAuthDialog.this.displayError(I18N.getMessage(I18N.getGUIBundle(),
									"gui.dialog.error.oauth_dialog.start_oauth_fail.message"));
							OAuthDialog.this.confirmButton.setEnabled(false);
						}
					}
				});

			}
		};
		actionThread.start();
	}

	/**
	 * Calls the endOAuth in a {@link ProgressThread} and displays the result in the
	 * {@link #statusLabel}
	 */
	private void endOAuth() {
		ProgressThread actionThread = new ProgressThread("oauth_action_end") {

			@Override
			public void run() {
				// show user we are doing something
				SwingUtilities.invokeLater(new Runnable() {

					@Override
					public void run() {
						OAuthDialog.this.displayStatus(I18N.getMessage(I18N.getGUIBundle(),
								"gui.dialog.oauth_dialog.status.end_oauth.message"));
						OAuthDialog.this.confirmButton.setEnabled(false);
					}
				});
				String code = null;
				if (oAuth.isOAuth2()) {
					code = OAuthDialog.this.codeText.getText().trim();
				}
				final String error = oAuth.endOAuth(code);

				// show user results
				SwingUtilities.invokeLater(new Runnable() {

					@Override
					public void run() {
						if (error != null) {
							OAuthDialog.this.displayError(error);
							OAuthDialog.this.confirmButton.setEnabled(true);
						} else {
							OAuthDialog.this.dispose();
						}
					}
				});

			}
		};
		actionThread.start();
	}

	/**
	 * Validates the {@link #codeText} field.
	 *
	 * @return Returns <code>true</code> if no errors detected otherwise <code>false</code>
	 */
	private boolean validateCode() {
		String code = codeText.getText();
		if (code == null || "".equals(code.trim())) {
			displayError(I18N.getMessage(I18N.getGUIBundle(), "gui.dialog.error.oauth_dialog.empty_code.message"));
			return false;
		}
		statusLabel.setText(null);
		return true;
	}

	/**
	 * Displays a message in the {@link #statusLabel}
	 *
	 * @param message
	 */
	private void displayStatus(String message) {
		statusLabel.setForeground(Color.BLACK);
		statusLabel.setText(message);
		statusLabel.setToolTipText(message);
	}

	/**
	 * Displays an error in the {@link #statusLabel}
	 *
	 * @param errorMessage
	 */
	private void displayError(String errorMessage) {
		statusLabel.setForeground(Color.RED);
		statusLabel.setText(errorMessage);
		statusLabel.setToolTipText(errorMessage);
	}

	/**
	 * Opens an URL with the default web browser.
	 *
	 * @param urlString
	 */
	private void open(String urlString) {
		RMUrlHandler.openInBrowser(urlString);
	}
}
