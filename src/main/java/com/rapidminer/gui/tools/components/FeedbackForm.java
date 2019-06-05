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
package com.rapidminer.gui.tools.components;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JToggleButton;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import com.rapidminer.gui.MainFrame;
import com.rapidminer.gui.look.Colors;
import com.rapidminer.gui.look.RapidLookTools;
import com.rapidminer.gui.tools.ExtendedJScrollPane;
import com.rapidminer.gui.tools.NotificationPopup;
import com.rapidminer.gui.tools.SwingTools;
import com.rapidminer.tools.I18N;
import com.rapidminer.tools.usagestats.ActionStatisticsCollector;


/**
 * Displays a "was this helpful?" feedback form with thumbs-up/down buttons and a free text field.
 * <p>
 * Ratings are submitted if the user presses the submit button, and are written to the {@link com.rapidminer.tools.usagestats.UsageStatistics} under the provided key.
 * </p>
 *
 * @author Marco Boeck
 * @since 8.1.2
 */
public class FeedbackForm extends JPanel {


	/**
	 * Indicate if the user chose thumbs-up/down or nothing yet.
	 */
	private enum FeedbackState {
		NONE,

		POSITIVE,

		NEGATIVE

	}

	private static final Dimension MIN_SIZE_FREETEXT = new Dimension(50, 100);
	private static final float FONT_SIZE_QUESTION = 14f;

	/** the duration the submission "thank you" note popup appears before fading out */
	private static final int NOTIFICATION_DURATION = 3000;


	private final String key;
	private final String id;

	private FeedbackState state;

	private JButton submitButton;


	/**
	 * Create a new feedback form with the given category and id.
	 *
	 * @param key
	 * 		the key that is used to submit user feedback into the usage statistics
	 * @param id
	 * 		the id that is used to submit user feedback into the usage statistics
	 */
	public FeedbackForm(final String key, final String id) {
		this.key = key;
		this.id = id;
		this.state = FeedbackState.NONE;

		initGUI();
	}

	/**
	 * Initializes the GUI.
	 */
	private void initGUI() {
		setLayout(new GridBagLayout());
		GridBagConstraints gbc = new GridBagConstraints();

		JLabel questionLabel = new JLabel(I18N.getGUILabel("feedback_form.helpful.label"));
		questionLabel.setFont(questionLabel.getFont().deriveFont(Font.BOLD).deriveFont(FONT_SIZE_QUESTION));
		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.weightx = 1.0f;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.gridwidth = 3;
		gbc.insets = new Insets(20, 15, 5, 0);
		add(questionLabel, gbc);

		JToggleButton yesButton = new JToggleButton();
		yesButton.putClientProperty(RapidLookTools.PROPERTY_BUTTON_CIRCLE, Boolean.TRUE);
		yesButton.setIcon(SwingTools.createIcon("24/" + I18N.getGUILabel("feedback_form.yes.icon")));
		yesButton.setToolTipText(I18N.getGUILabel("feedback_form.yes.tip"));
		yesButton.setOpaque(false);
		yesButton.setFocusPainted(false);
		yesButton.addActionListener(actionEvent -> {

			enableSubmit(true);
			state = FeedbackState.POSITIVE;

			// we also log just thumbs-up/down clicks to also capture users who never click "Submit"
			submitToUsageStats(false, "");
		});
		gbc.gridy += 1;
		gbc.weightx = 0.0f;
		gbc.fill = GridBagConstraints.NONE;
		gbc.gridwidth = 1;
		gbc.insets = new Insets(10, 8, 0, 0);
		add(yesButton, gbc);

		JToggleButton noButton = new JToggleButton();
		noButton.putClientProperty(RapidLookTools.PROPERTY_BUTTON_CIRCLE, Boolean.TRUE);
		noButton.setIcon(SwingTools.createIcon("24/" + I18N.getGUILabel("feedback_form.no.icon")));
		noButton.setToolTipText(I18N.getGUILabel("feedback_form.no.tip"));
		noButton.setOpaque(false);
		noButton.setFocusPainted(false);
		noButton.addActionListener(actionEvent -> {

			enableSubmit(true);
			state = FeedbackState.NEGATIVE;

			// we also log just thumbs-up/down clicks to also capture users who never click "Submit"
			submitToUsageStats(false, "");
		});
		gbc.gridx = 1;
		gbc.weightx = 0.0f;
		gbc.fill = GridBagConstraints.NONE;
		add(noButton, gbc);

		// add horizontal filler
		gbc.gridx = 2;
		gbc.weightx = 1.0f;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		add(new JLabel(), gbc);

		JTextArea freeTextArea = new JTextArea(5, 20);
		freeTextArea.setLineWrap(true);
		freeTextArea.setWrapStyleWord(true);
		freeTextArea.setBorder(BorderFactory.createEmptyBorder());
		freeTextArea.getDocument().addDocumentListener(new DocumentListener() {

			@Override
			public void insertUpdate(DocumentEvent e) {
				updateSubmitStatus();
			}

			@Override
			public void removeUpdate(DocumentEvent e) {
				updateSubmitStatus();
			}

			@Override
			public void changedUpdate(DocumentEvent e) {
				updateSubmitStatus();
			}

			private void updateSubmitStatus() {
				enableSubmit(!freeTextArea.getText().trim().isEmpty() || state != FeedbackState.NONE);
			}
		});
		SwingTools.setPrompt(I18N.getGUILabel("feedback_form.freetext.prompt"), freeTextArea);
		gbc.gridx = 0;
		gbc.gridy += 1;
		gbc.weightx = 1.0f;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.gridwidth = 3;
		gbc.insets = new Insets(12, 10, 0, 10);
		JScrollPane scrollPaneText = new ExtendedJScrollPane(freeTextArea);
		scrollPaneText.setBorder(BorderFactory.createLineBorder(Colors.TEXTFIELD_BORDER, 1, true));
		scrollPaneText.setMinimumSize(MIN_SIZE_FREETEXT);
		add(scrollPaneText, gbc);

		submitButton = new JButton(I18N.getGUILabel("feedback_form.submit.label"));
		submitButton.putClientProperty(RapidLookTools.PROPERTY_BUTTON_HIGHLIGHT_DARK, Boolean.TRUE);
		submitButton.putClientProperty(RapidLookTools.PROPERTY_BUTTON_DARK_BORDER, Boolean.TRUE);
		submitButton.setForeground(Colors.WHITE);
		submitButton.setEnabled(false);
		submitButton.setToolTipText(I18N.getGUILabel("feedback_form.submit_disabled.tip"));
		submitButton.addActionListener(actionEvent -> {

			// prevent multiple submissions and hide feedback form
			submitButton.setEnabled(false);
			FeedbackForm.this.setVisible(false);

			submitToUsageStats(true, freeTextArea.getText().trim());

			NotificationPopup.showFadingPopup(
					SwingTools.createNotificationPanel("gui.dialog.message.feedback_form.success.icon",
							"gui.dialog.message.feedback_form.success.message"),
					MainFrame.getApplicationFrame(), NotificationPopup.PopupLocation.LOWER_RIGHT, NOTIFICATION_DURATION, 30, 40);
		});
		gbc.gridy += 1;
		gbc.gridwidth = 2;
		gbc.anchor = GridBagConstraints.WEST;
		gbc.weightx = 0.0f;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.insets = new Insets(10, 10, 10, 0);
		add(submitButton, gbc);

		// add horizontal filler
		gbc.gridx = 2;
		gbc.weightx = 1.0f;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.insets = new Insets(0, 0, 0, 0);
		add(new JLabel(), gbc);

		// add horizontal filler
		gbc.gridx = 0;
		gbc.gridy += 1;
		gbc.weighty = 1.0f;
		gbc.fill = GridBagConstraints.BOTH;
		gbc.gridwidth = 3;
		add(new JLabel(), gbc);


		ButtonGroup feedbackGroup = new ButtonGroup();
		feedbackGroup.add(yesButton);
		feedbackGroup.add(noButton);
	}

	/**
	 * Enables the "Submit" button.
	 *  @param enable
	 * 		if {@code true}, the submit button will be enabled and scrolled to visible; otherwise it will be disabled
	 *
	 */
	private void enableSubmit(boolean enable) {
		submitButton.setEnabled(enable);
		submitButton.setToolTipText(I18N.getGUILabel(enable ? "feedback_form.submit.tip" : "feedback_form.submit_disabled.tip"));

		if (enable) {
			submitButton.scrollRectToVisible(submitButton.getBounds());
		}
	}

	/**
	 * Submits the given rating to the usage stats.
	 *
	 * @param submitted
	 * 		{@code true} if user clicked "Submit" button; {@code false} if user just clicked thumbs-up/down buttons
	 * @param freeText
	 * 		the free text the user entered. Can be empty
	 */
	private void submitToUsageStats(final boolean submitted, final String freeText) {
		ActionStatisticsCollector.INSTANCE.logFeedback(key, id, submitted, state.name(), freeText);

	}

}
