/*
 * Copyright (C) 2001-2016 RapidMiner GmbH
 */
package com.rapidminer.gui.dialog;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.KeyboardFocusManager;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.FocusAdapter;

import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JTextField;

import com.rapidminer.gui.tools.ResourceAction;
import com.rapidminer.gui.tools.dialogs.ButtonDialog;
import com.rapidminer.gui.tools.dialogs.ButtonDialog.ButtonDialogBuilder;
import com.rapidminer.gui.tools.dialogs.ButtonDialog.ButtonDialogBuilder.DefaultButtons;


/**
 * Creates a simple BrowserUnavailable Dialog
 *
 * @author Jonas Wilms-Pfau
 *
 */
public class BrowserUnavailableDialogFactory {

	/**
	 * Creates a new BrowserUnavailable ButtonDialog
	 *
	 * @param uri
	 *            The URI to display
	 * @return
	 */
	public static ButtonDialog createNewDialog(String uri) {

		ButtonDialogBuilder builder = new ButtonDialogBuilder("browser_unavailable");

		JPanel mainPanel = new JPanel(new GridBagLayout());

		GridBagConstraints gbc = new GridBagConstraints();
		gbc.insets = new Insets(5, 5, 5, 5);
		gbc.gridy = 0;
		gbc.gridx = 0;
		JTextField urlTextField = makeTextField(uri);
		gbc.weightx = 1.0;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		mainPanel.add(urlTextField, gbc);

		gbc.insets = new Insets(5, 0, 5, 5);
		JButton copyButton = makeCopyButton(uri);
		gbc.gridx += 1;
		gbc.weightx = 0.0;
		gbc.fill = GridBagConstraints.NONE;
		gbc.ipady = -1;
		mainPanel.add(copyButton, gbc);

		ButtonDialog dialog = builder.setContent(mainPanel, ButtonDialog.MESSAGE).setButtons(DefaultButtons.CLOSE_BUTTON)
				.setOwner(KeyboardFocusManager.getCurrentKeyboardFocusManager().getActiveWindow()).build();

		return dialog;
	}

	/**
	 * Creates an uneditable JTextField with the given text
	 *
	 * @param text
	 *            The content of the JTextField
	 * @return
	 */
	private static JTextField makeTextField(String text) {
		JTextField urlField = new JTextField(text);
		urlField.setEditable(false);

		urlField.addFocusListener(new FocusAdapter() {

			@Override
			public void focusGained(java.awt.event.FocusEvent evt) {
				urlField.getCaret().setVisible(true);
				urlField.selectAll();
			}
		});
		return urlField;
	}

	/**
	 * Creates a Copy Button
	 *
	 * @param text
	 *            Text to copy into clipboard
	 * @return
	 */
	private static JButton makeCopyButton(String text) {
		Action copyAction = new ResourceAction(true, "browser_unavailable.copy") {

			private static final long serialVersionUID = 1L;

			@Override
			public void actionPerformed(ActionEvent e) {
				StringSelection stringSelection = new StringSelection(text);
				Clipboard clpbrd = Toolkit.getDefaultToolkit().getSystemClipboard();
				clpbrd.setContents(stringSelection, null);
			}

		};
		JButton copyButton = new JButton(copyAction);
		return copyButton;
	}

}
