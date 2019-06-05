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
package com.rapidminer.gui.dialog;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.logging.Level;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import com.rapidminer.RapidMiner;
import com.rapidminer.RapidMiner.ExitMode;
import com.rapidminer.core.license.ProductConstraintManager;
import com.rapidminer.gui.license.LicenseTools;
import com.rapidminer.gui.look.Colors;
import com.rapidminer.gui.tools.ExtendedJScrollPane;
import com.rapidminer.gui.tools.ResourceAction;
import com.rapidminer.gui.tools.SwingTools;
import com.rapidminer.gui.tools.components.FixedWidthLabel;
import com.rapidminer.gui.tools.dialogs.ButtonDialog;
import com.rapidminer.repository.RepositoryException;
import com.rapidminer.tools.FileSystemService;
import com.rapidminer.tools.I18N;
import com.rapidminer.tools.LogService;
import com.rapidminer.tools.Tools;


/**
 * Dialog to display the EULA text. User must accept the EULA before RapidMiner can be used.
 *
 * @author Michael Knopf
 */
public class EULADialog extends ButtonDialog implements AdjustmentListener, ChangeListener {

	private static final long serialVersionUID = 1067725913323338148L;

	private static final String DEFAULT_EULA = "EULA_EN.txt";

	/**
	 * Should be adjusted whenever the EULA is updated.
	 */
	private static final String ACCEPT_PROPERTY = "rapidminer.eula.v7.accepted";

	private final JButton acceptButton;
	private final JCheckBox acceptCheckBox;
	private final JTextArea eulaText;
	private final JScrollPane scrollPane;

	/**
	 * Simple confirm dialog to be used instead of the ConfirmDialog extending the ButtonDialog
	 * class. This is necessary, since the ButtonDialog family does not support specifying owner
	 * components.
	 *
	 * @author Michael Knopf
	 */
	private class ConfirmDialog extends JDialog {

		private static final long serialVersionUID = 1L;

		public static final int YES_OPTION = JOptionPane.YES_OPTION;
		public static final int NO_OPTION = JOptionPane.NO_OPTION;

		private int returnCode = NO_OPTION;

		/**
		 * Constructor.
		 */
		public ConfirmDialog() {
			super(EULADialog.this, I18N.getMessage(I18N.getGUIBundle(), "gui.dialog.eula.title"), true);

			// setup info panel (using the same layout as the ButtonDialog class)
			JLabel message = new FixedWidthLabel(420, I18N.getMessage(I18N.getGUIBundle(),
					"gui.dialog.confirm.decline_eula.message",
					LicenseTools.translateProductName(ProductConstraintManager.INSTANCE.getActiveLicense())));
			JLabel icon = new JLabel(SwingTools.createIcon("48/"
					+ I18N.getMessage(I18N.getGUIBundle(), "gui.dialog.eula.icon")));

			JPanel messagePanel = new JPanel(new BorderLayout(20, 0));
			messagePanel.setBorder(BorderFactory.createEmptyBorder(12, 16, 16, 4));
			messagePanel.add(icon, BorderLayout.WEST);
			messagePanel.add(message, BorderLayout.CENTER);

			// setup button panel
			JButton quitRapidMinerButton = new JButton(new ResourceAction("decline_eula_confirm",
					LicenseTools.translateProductName(ProductConstraintManager.INSTANCE.getActiveLicense())) {

				private static final long serialVersionUID = 1L;

				@Override
				public void loggedActionPerformed(ActionEvent e) {
					ConfirmDialog.this.returnCode = YES_OPTION;
					ConfirmDialog.this.setVisible(false);
				}
			});

			JButton goBackButton = new JButton(new ResourceAction("decline_eula_go_back") {

				private static final long serialVersionUID = 1L;

				@Override
				public void loggedActionPerformed(ActionEvent e) {
					ConfirmDialog.this.returnCode = NO_OPTION;
					ConfirmDialog.this.setVisible(false);
				}
			});

			JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, ButtonDialog.GAP, ButtonDialog.GAP));
			buttonPanel.add(goBackButton);
			buttonPanel.add(quitRapidMinerButton);

			// again, mimic ButtonDialog's layout
			this.setLayout(new BorderLayout(ButtonDialog.GAP, ButtonDialog.GAP));
			this.setResizable(false);
			this.add(messagePanel, BorderLayout.CENTER);
			this.add(buttonPanel, BorderLayout.SOUTH);
			this.pack();
			this.setLocationRelativeTo(EULADialog.this);
		}

		/**
		 * Returns the user selection.
		 */
		public int getReturnOption() {
			return this.returnCode;
		}

	}

	/**
	 * Constructor.
	 */
	public EULADialog() {
		super(null, "eula", ModalityType.TOOLKIT_MODAL, new Object[] { LicenseTools
				.translateProductName(ProductConstraintManager.INSTANCE.getActiveLicense()) });

		this.acceptButton = this.makeAcceptButton();
		this.acceptButton.setEnabled(false);

		this.acceptCheckBox = new JCheckBox(I18N.getGUILabel("read_eula"));
		this.acceptCheckBox.setEnabled(false);

		this.eulaText = new JTextArea();
		this.eulaText.setColumns(60);
		this.eulaText.setRows(15);
		this.eulaText.setLineWrap(true);
		this.eulaText.setWrapStyleWord(true);
		this.eulaText.setEditable(false);
		this.eulaText.setText(this.loadEULA());
		eulaText.setBorder(null);

		this.scrollPane = new ExtendedJScrollPane(this.eulaText);
		scrollPane.setBorder(BorderFactory.createLineBorder(Colors.TEXTFIELD_BORDER));

		this.layoutDefault(this.makeContentPanel(), this.acceptButton, this.makeDeclineButton());
		this.setResizable(false);
		this.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);

		// add listeners (must be added after doing the layout)
		this.acceptCheckBox.addChangeListener(this);
		this.scrollPane.getVerticalScrollBar().addAdjustmentListener(this);
	}

	/**
	 * Loads translated EULA text. If the file cannot be found a default text is loaded (the English
	 * version of the EULA).
	 */
	private String loadEULA() {
		// look up location of EULA file
		String pathToEula = I18N.getMessage(I18N.getGUIBundle(), "gui.resource.eula_file");
		String eulaText = null;

		// read EULA text
		try (InputStream inputStream = Tools.getResourceInputStream(pathToEula)) {
			eulaText = Tools.readTextFile(inputStream);
		} catch (IOException | RepositoryException e1) {
			// loading the translated EULA failed, load default text instead
			LogService.getRoot().log(Level.WARNING, "com.rapidminer.gui.dialog.EULADialog.cannot_open_translated_eula");
			try (InputStream inputStream = Tools.getResourceInputStream(DEFAULT_EULA)) {
				eulaText = Tools.readTextFile(inputStream);
			} catch (IOException | RepositoryException e2) {
				// loading the default EULA failed (this should never happen)
				LogService.getRoot().log(Level.SEVERE, "com.rapidminer.gui.dialog.EULADialog.cannot_open_default_eula");
			}
		}

		return eulaText;
	}

	/**
	 * Creates a button to accept the EULA. Invokes storage of the corresponding property.
	 */
	private JButton makeAcceptButton() {
		ResourceAction action = new ResourceAction("accept_eula") {

			private static final long serialVersionUID = 3102243518938674477L;

			@Override
			public void loggedActionPerformed(ActionEvent e) {
				// store decision and close dialog
				EULADialog.setEULAAccepted(true);
				dispose();
			}
		};

		return new JButton(action);
	}

	/**
	 * Creates the content panel consisting of a scrollable text area to display the EULA text and a
	 * check box to accept it.
	 */
	private JComponent makeContentPanel() {
		JPanel panel = new JPanel();
		panel.setLayout(new BorderLayout());

		// add text area with scroll pane
		panel.add(this.scrollPane, BorderLayout.CENTER);
		// scroll to tohe top of the document
		SwingUtilities.invokeLater(new Runnable() {

			@Override
			public void run() {
				scrollPane.getVerticalScrollBar().setValue(0);
			}
		});

		// add check box to enable accept button
		panel.add(this.acceptCheckBox, BorderLayout.SOUTH);

		return panel;
	}

	/**
	 * Creates a button to decline the EULA. A confirmation dialog is shown before RapidMiner is
	 * closed.
	 */
	private JButton makeDeclineButton() {
		ResourceAction action = new ResourceAction("decline_eula") {

			private static final long serialVersionUID = 3102243518938674477L;

			@Override
			public void loggedActionPerformed(ActionEvent e) {
				ConfirmDialog dialog = new ConfirmDialog();
				dialog.setVisible(true);
				dialog.requestFocusInWindow();
				if (dialog.getReturnOption() == ConfirmDialog.YES_OPTION) {
					RapidMiner.quit(ExitMode.NORMAL);
				}
			}
		};

		return new JButton(action);
	}

	/**
	 * Listens to changes of the check box, enables the accept button when the check box is active.
	 */
	@Override
	public void stateChanged(ChangeEvent e) {
		if (e.getSource() == this.acceptCheckBox) {
			this.acceptButton.setEnabled(this.acceptCheckBox.isSelected());
		}
	}

	/**
	 * Listens to changes of the scroll bar of the text are showing the EULA text, enables the check
	 * box once the user scrolled to the end of the document.
	 */
	@Override
	public void adjustmentValueChanged(AdjustmentEvent e) {
		JScrollBar scrollBar = this.scrollPane.getVerticalScrollBar();
		if (e.getSource() == scrollBar) {
			// the maximum value of the scroll bar assumes that the content is
			// not visible anymore, since this is not the case when scrolling
			// to the end of the document (the last part is still visible),
			// we have to include the visible amount in the comparison
			int currentValue = scrollBar.getValue() + scrollBar.getVisibleAmount();
			if (currentValue >= scrollBar.getMaximum()) {
				// the user scrolled to the end of the document
				this.acceptCheckBox.setEnabled(true);
				this.acceptCheckBox.requestFocusInWindow();
			}
		}
	}

	/**
	 * Looks up the users' decision to accept/decline the current EULA in the "eula.properties"
	 * file.
	 *
	 * @return True if the EULA was accepted, false otherwise.
	 */
	public static boolean getEULAAccepted() {

		File eulaPropertiesFile = FileSystemService.getUserConfigFile("eula.properties");
		Properties eulaProperties = new Properties();

		// if the property file already exists, load its contents
		if (eulaPropertiesFile.exists()) {
			try (FileInputStream in = new FileInputStream(eulaPropertiesFile)) {
				eulaProperties.load(in);
			} catch (IOException e) {
				LogService.getRoot().log(Level.WARNING, "com.rapidminer.gui.dialog.EULADialog.loading_properties_failed", e);
			}
			// check and return property
			String accepted = eulaProperties.getProperty(ACCEPT_PROPERTY);
			return Tools.booleanValue(accepted, false);
		} else {
			// property cannot be set
			return false;
		}
	}

	/**
	 * Stores the users' decision to accept/decline the current EULA in the "eula.properties" file.
	 *
	 * @param accepted
	 *            Indicates whether the user accepted the EULA.
	 */
	public static void setEULAAccepted(boolean accepted) {
		File eulaPropertiesFile = FileSystemService.getUserConfigFile("eula.properties");
		Properties eulaProperties = new Properties();

		// if the property file already exists, load its contents
		if (eulaPropertiesFile.exists()) {
			try (FileInputStream in = new FileInputStream(eulaPropertiesFile)) {
				eulaProperties.load(in);
			} catch (IOException e) {
				LogService.getRoot().log(Level.WARNING, "com.rapidminer.gui.dialog.EULADialog.loading_properties_failed", e);
			}
		}

		// set the acceptance property
		eulaProperties.setProperty(ACCEPT_PROPERTY, accepted ? "true" : "false");

		// store properties
		try (FileOutputStream out = new FileOutputStream(eulaPropertiesFile)) {
			eulaProperties.store(out, "RapidMiner EULA Properties");
		} catch (IOException e) {
			LogService.getRoot().log(Level.WARNING, "com.rapidminer.gui.dialog.EULADialog.storing_properties_failed", e);
		}
	}
}
