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

import java.awt.Dialog.ModalityType;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Level;

import javax.swing.BorderFactory;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import com.rapidminer.RapidMiner;
import com.rapidminer.gui.ApplicationFrame;
import com.rapidminer.gui.look.Colors;
import com.rapidminer.gui.tools.ExtendedJScrollPane;
import com.rapidminer.gui.tools.dialogs.ButtonDialog;
import com.rapidminer.gui.tools.dialogs.ButtonDialog.ButtonDialogBuilder;
import com.rapidminer.gui.tools.dialogs.ButtonDialog.ButtonDialogBuilder.DefaultButtons;
import com.rapidminer.repository.RepositoryException;
import com.rapidminer.tools.LogService;
import com.rapidminer.tools.ParameterService;
import com.rapidminer.tools.Tools;
import com.rapidminer.tools.usagestats.ActionStatisticsCollector;


/**
 * Listener for the {@link SettingsDialog} that checks if beta features are activated. In case of an
 * activation a dialog with the beta eula is shown, but only once per session.
 *
 * @author Gisa Schaefer
 * @since 7.3
 */
class BetaFeaturesListener extends WindowAdapter {

	/** the name of the resource file containing the beta eula */
	private static final String BETA_EULA = "BETA_EULA.txt";

	/** stores if the beta features are activated when the listener is constructed */
	private boolean betaActiveBefore;

	/**
	 * Constructs a listener for beta features activation.
	 */
	BetaFeaturesListener() {
		betaActiveBefore = Boolean
				.parseBoolean(ParameterService.getParameterValue(RapidMiner.PROPERTY_RAPIDMINER_UPDATE_BETA_FEATURES));
	}

	@Override
	public void windowClosed(WindowEvent e) {
		boolean betaActiveNow = Boolean
				.parseBoolean(ParameterService.getParameterValue(RapidMiner.PROPERTY_RAPIDMINER_UPDATE_BETA_FEATURES));
		if (!betaActiveBefore && betaActiveNow) {
			JTextArea textArea = new JTextArea();
			textArea.setColumns(60);
			textArea.setRows(15);
			textArea.setLineWrap(true);
			textArea.setWrapStyleWord(true);
			textArea.setEditable(false);
			textArea.setText(loadBetaEULA());
			textArea.setBorder(null);
			textArea.setCaretPosition(0);

			JScrollPane scrollPane = new ExtendedJScrollPane(textArea);
			scrollPane.setBorder(BorderFactory.createLineBorder(Colors.TEXTFIELD_BORDER));

			ButtonDialog dialog = new ButtonDialogBuilder("beta_features_eula")
					.setOwner(ApplicationFrame.getApplicationFrame())
					.setButtons(DefaultButtons.OK_BUTTON, DefaultButtons.CANCEL_BUTTON)
					.setModalityType(ModalityType.APPLICATION_MODAL).setContent(scrollPane, ButtonDialog.DEFAULT_SIZE)
					.build();
			dialog.setVisible(true);
			if (!dialog.wasConfirmed()) {
				ParameterService.setParameterValue(RapidMiner.PROPERTY_RAPIDMINER_UPDATE_BETA_FEATURES,
						String.valueOf(false));
				ParameterService.saveParameters();
				ActionStatisticsCollector.INSTANCE.log(ActionStatisticsCollector.TYPE_BETA_FEATURES,
						ActionStatisticsCollector.VALUE_BETA_FEATURES_ACTIVATION, "cancelled");
			}
		}
	}

	/**
	 * @return the text for the beta eula
	 */
	private String loadBetaEULA() {
		String eulaText = null;
		// read EULA text
		try (InputStream inputStream = Tools.getResourceInputStream(BETA_EULA)) {
			eulaText = Tools.readTextFile(inputStream);
		} catch (IOException | RepositoryException e2) {
			// loading the EULA failed (this should never happen)
			LogService.getRoot().log(Level.SEVERE,
					"com.rapidminer.gui.properties.BetaFeaturesListener.cannot_open_beta_eula");
		}

		return eulaText;
	}

}
