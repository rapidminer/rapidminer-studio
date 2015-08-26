/**
 * Copyright (C) 2001-2015 by RapidMiner and the contributors
 *
 * Complete list of developers available at our web site:
 *
 *      http://rapidminer.com
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses/.
 */
package com.rapidminer.gui.new_plotter.templates.actions;

import com.rapidminer.gui.actions.export.ExportImageAction;
import com.rapidminer.gui.actions.export.PrintableComponent;
import com.rapidminer.gui.actions.export.SimplePrintableComponent;
import com.rapidminer.gui.new_plotter.templates.PlotterTemplate;
import com.rapidminer.gui.tools.ResourceAction;
import com.rapidminer.tools.I18N;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.AffineTransform;

import javax.swing.InputVerifier;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JTextField;
import javax.swing.WindowConstants;


/**
 * Export the currently shown plot.
 * 
 * @author Marco Boeck
 */
public class ExportAdvancedChartAction extends ResourceAction {

	/**
	 * This is a dialog which can be used input a {@link Dimension} (width x height).
	 */
	public static class DimensionDialog extends JDialog {

		/** the ok {@link JButton} */
		private JButton okButton;

		/** the cancel {@link JButton} */
		private JButton cancelButton;

		/** the width the user specified */
		private int width;

		/** the height the user specified */
		private int height;

		/** the field to input the width */
		private JTextField widthField;

		/** the field to input the height */
		private JTextField heightField;

		/** the value indicating whether the user pressed ok or cancel */
		private int returnVal;

		/** min width the user can input */
		private final int MIN_WIDTH;

		/** max width the user can input */
		private final int MAX_WIDTH;

		/** min height the user can input */
		private final int MIN_HEIGHT;

		/** max height the user can input */
		private final int MAX_HEIGHT;

		private static final long serialVersionUID = 1932257219370926682L;

		/**
		 * Creates a new {@link DimensionDialog}.
		 */
		public DimensionDialog() {
			this(50, 16000, 50, 16000);
		}

		/**
		 * Creates a new {@link DimensionDialog} with the given min and max values for width/height.
		 * 
		 * @param minWidth
		 * @param maxWidth
		 * @param minHeight
		 * @param maxHeight
		 */
		public DimensionDialog(int minWidth, int maxWidth, int minHeight, int maxHeight) {
			MIN_WIDTH = minWidth;
			MAX_WIDTH = maxWidth;
			MIN_HEIGHT = minHeight;
			MAX_HEIGHT = maxHeight;

			width = 800;
			height = 600;
			returnVal = JOptionPane.CANCEL_OPTION;

			setupGUI();
		}

		/**
		 * Setup the GUI.
		 */
		private void setupGUI() {
			JPanel mainPanel = new JPanel();
			this.setContentPane(mainPanel);

			// start layout
			mainPanel.setLayout(new GridBagLayout());
			GridBagConstraints gbc = new GridBagConstraints();

			gbc.gridx = 0;
			gbc.gridy = 0;
			gbc.fill = GridBagConstraints.NONE;
			gbc.weightx = 1;
			gbc.anchor = GridBagConstraints.WEST;
			gbc.insets = new Insets(5, 5, 2, 5);
			JLabel widthLabel = new JLabel(I18N.getMessage(I18N.getGUIBundle(),
					"gui.action.export_newplotter_image.width.label"));
			this.add(widthLabel, gbc);

			gbc.gridx = 1;
			gbc.gridy = 0;
			gbc.insets = new Insets(2, 5, 2, 5);
			gbc.fill = GridBagConstraints.HORIZONTAL;
			widthField = new JTextField();
			widthField.setText(String.valueOf(width));
			widthField.setToolTipText(I18N.getMessage(I18N.getGUIBundle(), "gui.action.export_newplotter_image.width.tip",
					MIN_WIDTH, MAX_WIDTH));
			widthField.setInputVerifier(new InputVerifier() {

				@Override
				public boolean verify(JComponent input) {
					JTextField textField = (JTextField) input;
					String inputString = textField.getText();
					try {
						int number = Integer.parseInt(inputString);
						if (number < MIN_WIDTH || number > MAX_WIDTH) {
							textField.setForeground(Color.RED);
							return false;
						}
					} catch (NumberFormatException e) {
						textField.setForeground(Color.RED);
						return false;
					}

					textField.setForeground(Color.BLACK);
					return true;
				}
			});
			this.add(widthField, gbc);

			gbc.gridx = 0;
			gbc.gridy = 1;
			gbc.fill = GridBagConstraints.NONE;
			JLabel heightLabel = new JLabel(I18N.getMessage(I18N.getGUIBundle(),
					"gui.action.export_newplotter_image.height.label"));
			this.add(heightLabel, gbc);

			gbc.gridx = 1;
			gbc.gridy = 1;
			gbc.fill = GridBagConstraints.HORIZONTAL;
			heightField = new JTextField();
			heightField.setToolTipText(I18N.getMessage(I18N.getGUIBundle(), "gui.action.export_newplotter_image.height.tip",
					MIN_HEIGHT, MAX_HEIGHT));
			heightField.setText(String.valueOf(height));
			heightField.setInputVerifier(new InputVerifier() {

				@Override
				public boolean verify(JComponent input) {
					JTextField textField = (JTextField) input;
					String inputString = textField.getText();
					try {
						int number = Integer.parseInt(inputString);
						if (number < MIN_HEIGHT || number > MAX_HEIGHT) {
							textField.setForeground(Color.RED);
							return false;
						}
					} catch (NumberFormatException e) {
						textField.setForeground(Color.RED);
						return false;
					}

					textField.setForeground(Color.BLACK);
					return true;
				}
			});
			this.add(heightField, gbc);

			gbc.gridx = 0;
			gbc.gridy = 2;
			gbc.gridwidth = 2;
			gbc.anchor = GridBagConstraints.CENTER;
			gbc.insets = new Insets(15, 5, 5, 5);
			this.add(new JSeparator(), gbc);

			gbc.gridx = 0;
			gbc.gridy = 3;
			gbc.gridwidth = 1;
			gbc.fill = GridBagConstraints.NONE;
			gbc.anchor = GridBagConstraints.EAST;
			gbc.insets = new Insets(5, 5, 5, 5);
			okButton = new JButton(I18N.getMessage(I18N.getGUIBundle(), "gui.action.export_newplotter_image.ok.label"));
			okButton.setToolTipText(I18N.getMessage(I18N.getGUIBundle(), "gui.action.export_newplotter_image.ok.tip"));
			okButton.setMnemonic(I18N.getMessage(I18N.getGUIBundle(), "gui.action.export_newplotter_image.ok.mne")
					.toCharArray()[0]);
			okButton.setPreferredSize(new Dimension(75, 25));
			okButton.addActionListener(new ActionListener() {

				@Override
				public void actionPerformed(ActionEvent e) {
					DimensionDialog.this.width = Integer.parseInt(widthField.getText());
					DimensionDialog.this.height = Integer.parseInt(heightField.getText());
					DimensionDialog.this.returnVal = JOptionPane.OK_OPTION;
					DimensionDialog.this.dispose();
				}
			});
			this.add(okButton, gbc);

			gbc.gridx = 1;
			gbc.gridy = 3;
			gbc.fill = GridBagConstraints.NONE;
			gbc.anchor = GridBagConstraints.WEST;
			cancelButton = new JButton(I18N.getMessage(I18N.getGUIBundle(),
					"gui.action.export_newplotter_image.cancel.label"));
			cancelButton
					.setToolTipText(I18N.getMessage(I18N.getGUIBundle(), "gui.action.export_newplotter_image.cancel.tip"));
			cancelButton.setMnemonic(I18N.getMessage(I18N.getGUIBundle(), "gui.action.export_newplotter_image.cancel.mne")
					.toCharArray()[0]);
			cancelButton.setPreferredSize(new Dimension(75, 25));
			cancelButton.addActionListener(new ActionListener() {

				@Override
				public void actionPerformed(ActionEvent e) {
					DimensionDialog.this.returnVal = JOptionPane.CANCEL_OPTION;
					DimensionDialog.this.dispose();
				}
			});
			this.add(cancelButton, gbc);

			// misc settings
			this.setMinimumSize(new Dimension(250, 150));
			// center dialog
			this.setLocationRelativeTo(null);
			this.setTitle(I18N.getMessage(I18N.getGUIBundle(), "gui.action.export_newplotter_image.title.label"));
			this.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
			this.setModal(true);
		}

		/**
		 * Returns the {@link Dimension} the user specified.
		 * 
		 * @return
		 */
		public Dimension getUserDimension() {
			return new Dimension(width, height);
		}

		/**
		 * Returns the width the user specified.
		 * 
		 * @return
		 */
		public int getUserWidth() {
			return width;
		}

		/**
		 * Returns the return value indicating the user choice.
		 * <p>
		 * Possible return values are: {@link JOptionPane#CANCEL_OPTION} and
		 * {@link JOptionPane#OK_OPTION}
		 * 
		 * @return
		 */
		public int getReturnValue() {
			return returnVal;
		}

		/**
		 * Returns the height the user specified.
		 * 
		 * @return
		 */
		public int getUserHeight() {
			return height;
		}

		/**
		 * Shows the dialog.
		 */
		public void showDialog() {
			returnVal = JOptionPane.CANCEL_OPTION;
			setVisible(true);
			okButton.requestFocusInWindow();
		}

		/**
		 * Updates the width and height default values.
		 * 
		 * @param width
		 * @param height
		 */
		public void updateSizeValues(int width, int height) {
			this.width = width;
			this.height = height;
			this.widthField.setText(String.valueOf(width));
			this.heightField.setText(String.valueOf(height));
		}
	}

	/** the {@link PlotterTemplate} for this action */
	private PlotterTemplate template;

	/**
	 * the {@link DimensionDialog} instance (used by all {@link ExportAdvancedChartAction}
	 * instances)
	 */
	private static DimensionDialog dialog;

	private static final long serialVersionUID = -2226200404990114956L;

	public ExportAdvancedChartAction(PlotterTemplate template) {
		super(true, "export_newplotter_image");
		if (template == null) {
			throw new IllegalArgumentException("template must not be null!");
		}
		this.template = template;
		setEnabled(true);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		exportPlot(template);
	}

	/**
	 * Opens the plot export options.
	 */
	public static synchronized void exportPlot(final PlotterTemplate template) {
		if (dialog == null) {
			dialog = new DimensionDialog();
		}
		dialog.updateSizeValues(template.getPlotEngine().getChartPanel().getWidth(), template.getPlotEngine()
				.getChartPanel().getHeight());
		dialog.showDialog();
		if (dialog.getReturnValue() == JOptionPane.CANCEL_OPTION) {
			return;
		}

		final JPanel outerPanel = new JPanel() {

			private static final long serialVersionUID = 7315234075649335574L;

			@Override
			public void paintComponent(Graphics g) {
				Graphics2D g2 = (Graphics2D) g;
				AffineTransform at = new AffineTransform();
				double factorWidth = (double) dialog.getUserWidth() / template.getPlotEngine().getChartPanel().getWidth();
				double factorHeight = (double) dialog.getUserHeight() / template.getPlotEngine().getChartPanel().getHeight();
				at.scale(factorWidth, factorHeight);
				g2.transform(at);
				template.getPlotEngine().getChartPanel().print(g2);
			}
		};
		outerPanel.setSize(dialog.getUserDimension());

		new ExportImageAction(true) {

			private static final long serialVersionUID = 1L;

			@Override
			protected PrintableComponent getPrintableComponent() {
				return new SimplePrintableComponent(outerPanel, "plot");
			}

		}.actionPerformed(null);
	}
}
