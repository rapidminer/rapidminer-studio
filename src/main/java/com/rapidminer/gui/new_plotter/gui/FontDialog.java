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
package com.rapidminer.gui.new_plotter.gui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Locale;

import javax.swing.AbstractListModel;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.TitledBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import com.rapidminer.gui.tools.ResourceAction;
import com.rapidminer.gui.tools.ResourceLabel;
import com.rapidminer.gui.tools.dialogs.ButtonDialog;
import com.rapidminer.tools.FontTools;


/**
 * @author Nils Woehler
 * @deprecated since 9.2.0
 */
@Deprecated
public class FontDialog extends ButtonDialog {

	private static final String BOLD_ITALIC = "Bold Italic";
	private static final String ITALIC = "Italic";
	private static final String BOLD = "Bold";
	private static final String PLAIN = "Plain";

	private static final long serialVersionUID = 1L;

	public static final int RET_CANCEL = 0;

	public static final int RET_OK = 1;

	private Font font;

	private int returnStatus;

	private JPanel mainPanel;
	private JPanel fontPanel;

	private JLabel fontLabel;
	private JLabel styleLabel;
	private JLabel sizeLabel;

	private JList<String> fontList;

	private JScrollPane fontScrollPane;

	private JList<String> styleList;

	private JScrollPane styleScrollPane;

	private JList<String> sizeList;

	private JScrollPane sizeScrollPane;

	private JPanel previewPanel;

	private JLabel previewLabel;

	public FontDialog(Component parent, Font font, String i18nKey) {
		super(parent != null ? SwingUtilities.getWindowAncestor(parent) : null, i18nKey, ModalityType.APPLICATION_MODAL,
				new Object[] {});
		this.font = font;

		this.setResizable(false);
		this.addWindowListener(new WindowAdapter() {

			@Override
			public void windowClosing(WindowEvent e) {
				closeDialog(e);
			}
		});

		createComponents();

		this.pack();

		this.setSize(new java.awt.Dimension(443, 429));

		setLocationRelativeTo(parent);

		initComponents(font);

		Action cancelAction = new ResourceAction(i18nKey) {

			private static final long serialVersionUID = 1L;

			@Override
			public void loggedActionPerformed(ActionEvent e) {
				doClose(RET_CANCEL);
			}
		};
		getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
				.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0, false), "CANCEL");
		getRootPane().getActionMap().put("CANCEL", cancelAction);

	}

	private void createComponents() {

		GridBagConstraints itemConstraint = new GridBagConstraints();
		Insets standardInsets = new Insets(1, 1, 1, 1);

		{
			mainPanel = new JPanel(new GridLayout(2, 1));

			{
				// create FONT PANEL
				fontPanel = new JPanel(new GridBagLayout());

				// add font label
				fontLabel = new ResourceLabel("plotter.configuration_dialog.font_dialog.font");

				itemConstraint.fill = GridBagConstraints.HORIZONTAL;
				itemConstraint.insets = standardInsets;
				itemConstraint.weightx = 2.0;

				fontPanel.add(fontLabel, itemConstraint);

				// add style label
				styleLabel = new ResourceLabel("plotter.configuration_dialog.font_dialog.style");

				itemConstraint = new GridBagConstraints();
				itemConstraint.fill = GridBagConstraints.HORIZONTAL;
				itemConstraint.insets = standardInsets;

				fontPanel.add(styleLabel, itemConstraint);

				// add size label
				sizeLabel = new ResourceLabel("plotter.configuration_dialog.font_dialog.size");

				itemConstraint = new GridBagConstraints();
				itemConstraint.weightx = 0.2;
				itemConstraint.fill = GridBagConstraints.HORIZONTAL;
				itemConstraint.insets = standardInsets;

				fontPanel.add(sizeLabel, itemConstraint);

				// create font list
				fontList = new JList<>(
						GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames(Locale.getDefault()));
				fontLabel.setLabelFor(fontList);
				fontList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
				fontList.setSelectedValue(font.getFamily(Locale.getDefault()), true);
				fontList.addListSelectionListener(new ListSelectionListener() {

					@Override
					public void valueChanged(ListSelectionEvent e) {
						fontListValueChanged(e);
					}

				});

				// add font list to scrollPane
				fontScrollPane = new JScrollPane();
				fontScrollPane.setViewportView(fontList);

				itemConstraint = new GridBagConstraints();
				itemConstraint.gridx = 0;
				itemConstraint.gridy = 1;
				itemConstraint.ipadx = 1;
				itemConstraint.weightx = 2.0;
				itemConstraint.fill = GridBagConstraints.BOTH;

				// add font scroll pane to panel
				fontPanel.add(fontScrollPane, itemConstraint);

				// create style list
				styleList = new JList<String>(new javax.swing.AbstractListModel<String>() {

					private static final long serialVersionUID = 1L;
					String[] strings = { PLAIN, BOLD, ITALIC, BOLD_ITALIC };

					@Override
					public int getSize() {
						return strings.length;
					}

					@Override
					public String getElementAt(int i) {
						return strings[i];
					}
				});
				styleLabel.setLabelFor(styleList);
				styleList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
				styleList.setSelectedValue(font.getStyle(), true);
				styleList.addListSelectionListener(new ListSelectionListener() {

					@Override
					public void valueChanged(ListSelectionEvent e) {
						styleListValueChanged(e);

					}

				});

				// add style list to scrollPane
				styleScrollPane = new JScrollPane();
				styleScrollPane.setViewportView(styleList);

				itemConstraint = new GridBagConstraints();
				itemConstraint.gridx = 1;
				itemConstraint.gridy = 1;
				itemConstraint.fill = GridBagConstraints.HORIZONTAL;
				itemConstraint.ipadx = 1;
				itemConstraint.insets = standardInsets;

				// add style scroll pane to panel
				fontPanel.add(styleScrollPane, itemConstraint);

				// create size list
				sizeList = new JList<String>(new AbstractListModel<String>() {

					private static final long serialVersionUID = 1L;
					String[] strings = { "8", "10", "11", "12", "14", "16", "20", "24", "28", "36", "48", "72", "96" };

					@Override
					public int getSize() {
						return strings.length;
					}

					@Override
					public String getElementAt(int i) {
						return strings[i];
					}
				});
				sizeLabel.setLabelFor(sizeList);
				sizeList.setSelectedValue(font.getSize(), true);
				sizeList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
				sizeList.addListSelectionListener(new ListSelectionListener() {

					@Override
					public void valueChanged(ListSelectionEvent e) {
						sizeListValueChanged(e);
					}

				});

				// add size list to scrollPane
				sizeScrollPane = new JScrollPane();
				sizeScrollPane.setViewportView(sizeList);

				itemConstraint = new GridBagConstraints();
				itemConstraint.gridx = 2;
				itemConstraint.gridy = 1;
				itemConstraint.fill = GridBagConstraints.HORIZONTAL;
				itemConstraint.ipadx = 1;
				itemConstraint.insets = standardInsets;
				itemConstraint.weightx = 0.2;

				// add size scroll pane to panel
				fontPanel.add(sizeScrollPane, itemConstraint);

				// add font panel to mainPanel
				mainPanel.add(fontPanel);
			}

			{
				// create preview panel
				previewPanel = new JPanel(new BorderLayout());
				previewPanel.setBorder(new TitledBorder(null, "Preview", TitledBorder.DEFAULT_JUSTIFICATION,
						TitledBorder.DEFAULT_POSITION, FontTools.getFont(Font.DIALOG, 0, 12)));

				// create preview label
				previewLabel = new JLabel("ABCDEFG abcdefg", SwingConstants.CENTER);
				previewLabel.setFont(font);

				// add preview label to preview panel
				previewPanel.add(previewLabel, BorderLayout.CENTER);

				mainPanel.add(previewPanel);
			}

			this.getContentPane().add(mainPanel, BorderLayout.CENTER);
		}

		JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));

		JButton okayButton = new JButton(new ResourceAction(false, "plotter.configuration_dialog.okay") {

			private static final long serialVersionUID = 1L;

			@Override
			public void loggedActionPerformed(ActionEvent e) {
				okayButtonPerformed();
			}
		});
		getRootPane().setDefaultButton(okayButton);

		buttonPanel.add(okayButton);

		JButton cancelButton = new JButton(new ResourceAction(false, "plotter.configuration_dialog.cancel") {

			private static final long serialVersionUID = 1L;

			@Override
			public void loggedActionPerformed(ActionEvent e) {
				cancelButtonPerformed();
			}
		});

		buttonPanel.add(cancelButton);

		this.getContentPane().add(buttonPanel, BorderLayout.SOUTH);

	}

	private void initComponents(Font font) {
		fontList.setSelectedValue(font.getFamily(), true);
		fontList.ensureIndexIsVisible(fontList.getSelectedIndex());
		fontListValueChanged(null);

		int size = font.getSize();
		int sizeListSize = sizeList.getModel().getSize();
		int distance = Integer.MAX_VALUE;
		int nearestIndex = -1;
		for (int i = 0; i < sizeListSize; ++i) {
			int value = Integer.parseInt(sizeList.getModel().getElementAt(i));
			int d = Math.abs(value - size);
			if (d < distance) {
				nearestIndex = i;
				distance = d;
			}
		}
		if (nearestIndex >= 0) {
			sizeList.setSelectedIndex(nearestIndex);
		} else {
			sizeList.setSelectedValue("12", true);
		}
		sizeList.ensureIndexIsVisible(sizeList.getSelectedIndex());
		sizeListValueChanged(null);

		int style = font.getStyle();
		String selectedValue = null;
		switch (style) {
			case Font.PLAIN:
				selectedValue = PLAIN;
				break;
			case Font.BOLD:
				selectedValue = BOLD;
				break;
			case Font.ITALIC:
				selectedValue = ITALIC;
				break;
			default:
				selectedValue = BOLD_ITALIC;
				break;
		}
		styleList.setSelectedValue(selectedValue, true);
		styleList.ensureIndexIsVisible(styleList.getSelectedIndex());
		styleListValueChanged(null);

	}

	private void cancelButtonPerformed() {
		doClose(RET_CANCEL);

	}

	private void okayButtonPerformed() {
		doClose(RET_OK);
	}

	@Override
	public Font getFont() {
		return font;
	}

	private void fontListValueChanged(ListSelectionEvent e) {
		font = FontTools.getFont(fontList.getSelectedValue(), font.getStyle(), font.getSize());

		previewLabel.setFont(font);
	}

	private void styleListValueChanged(ListSelectionEvent e) {

		int style = -1;
		String selectedStyle = styleList.getSelectedValue();
		if (selectedStyle == PLAIN) {
			style = Font.PLAIN;
		}
		if (selectedStyle == BOLD) {
			style = Font.BOLD;
		}
		if (selectedStyle == ITALIC) {
			style = Font.ITALIC;
		}
		if (selectedStyle == BOLD_ITALIC) {
			style = Font.BOLD + Font.ITALIC;
		}

		font = FontTools.getFont(font.getFamily(), style, font.getSize());
		previewLabel.setFont(font);
	}

	private void sizeListValueChanged(ListSelectionEvent e) {
		int size = Integer.parseInt(sizeList.getSelectedValue());

		font = FontTools.getFont(font.getFamily(), font.getStyle(), size);

		previewLabel.setFont(font);

	}

	private void closeDialog(WindowEvent evt) {
		doClose(RET_CANCEL);
	}

	private void doClose(int retStatus) {
		returnStatus = retStatus;
		setVisible(false);
		dispose();
	}

	public int getReturnStatus() {
		return returnStatus;
	}
}
