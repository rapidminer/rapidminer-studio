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
package com.rapidminer.gui.new_plotter.gui.dialog;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.Rectangle2D;
import java.util.Vector;

import javax.swing.ButtonGroup;
import javax.swing.DefaultComboBoxModel;
import javax.swing.InputVerifier;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSeparator;
import javax.swing.JTextField;
import javax.swing.WindowConstants;

import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.XYPlot;

import com.rapidminer.gui.ApplicationFrame;
import com.rapidminer.gui.new_plotter.configuration.AxisParallelLineConfiguration;
import com.rapidminer.gui.new_plotter.configuration.LineFormat.LineStyle;
import com.rapidminer.gui.new_plotter.configuration.PlotConfiguration;
import com.rapidminer.gui.new_plotter.configuration.RangeAxisConfig;
import com.rapidminer.gui.new_plotter.engine.jfreechart.JFreeChartPlotEngine;
import com.rapidminer.gui.tools.SwingTools;
import com.rapidminer.tools.I18N;


/**
 * This dialog allows the user to configure the addition of a crosshair line in the new charts.
 * 
 * @author Marco Boeck
 * @deprecated since 9.2.0
 */
@Deprecated
public class AddParallelLineDialog extends JDialog {

	/** the ok {@link JButton} */
	private JButton okButton;

	/** the cancel {@link JButton} */
	private JButton cancelButton;

	/** the {@link JTextField} for the x value */
	private JTextField xField;

	/** the {@link JTextField} for the y value */
	private JTextField yField;

	/** the x value the user specified */
	private double x;

	/** the y value the user specified */
	private double y;

	/** if selected, the line will be horizontal */
	private JRadioButton horizontalLineRadiobutton;

	/** if selected, the line will be vertical */
	private JRadioButton verticalLineRadiobutton;

	/**
	 * the {@link JComboBox} where the {@link RangeAxisConfig} will be selected if horizontal line
	 * is selected
	 */
	private JComboBox<RangeAxisConfig> rangeAxisSelectionCombobox;

	/** this button modifies the line */
	private JButton modifyLineButton;

	/** the current {@link JFreeChartPlotEngine} */
	private JFreeChartPlotEngine engine;

	/** the current {@link PlotConfiguration} */
	private PlotConfiguration plotConfig;

	/** the position of the mouse, determines preselected x/y values */
	private Point mousePosition;

	/** the {@link EditParallelLineDialog} instance */
	private EditParallelLineDialog dialog;

	/** the current line to add */
	private AxisParallelLineConfiguration line;

	private static final long serialVersionUID = 1932257219370926682L;

	/**
	 * Creates a new {@link AddParallelLineDialog}.
	 */
	public AddParallelLineDialog() {
		super(ApplicationFrame.getApplicationFrame());
		x = 1.0;
		y = 1.0;

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
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.weightx = 1;
		gbc.anchor = GridBagConstraints.WEST;
		gbc.insets = new Insets(5, 5, 2, 5);
		horizontalLineRadiobutton = new JRadioButton(I18N.getMessage(I18N.getGUIBundle(),
				"gui.action.add_parallel_line.horizontal.label"));
		horizontalLineRadiobutton.setToolTipText(I18N.getMessage(I18N.getGUIBundle(),
				"gui.action.add_parallel_line.horizontal.tip"));
		horizontalLineRadiobutton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				setHorizontalLineSelected();
			}
		});
		horizontalLineRadiobutton.setSelected(true);
		this.add(horizontalLineRadiobutton, gbc);

		gbc.gridx = 1;
		gbc.gridy = 0;
		gbc.anchor = GridBagConstraints.EAST;
		verticalLineRadiobutton = new JRadioButton(I18N.getMessage(I18N.getGUIBundle(),
				"gui.action.add_parallel_line.vertical.label"));
		verticalLineRadiobutton.setToolTipText(I18N.getMessage(I18N.getGUIBundle(),
				"gui.action.add_parallel_line.vertical.tip"));
		verticalLineRadiobutton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				setVerticalLineSelected();
			}
		});
		this.add(verticalLineRadiobutton, gbc);

		ButtonGroup group = new ButtonGroup();
		group.add(horizontalLineRadiobutton);
		group.add(verticalLineRadiobutton);

		gbc.gridx = 0;
		gbc.gridy = 1;
		gbc.fill = GridBagConstraints.BOTH;
		gbc.weightx = 1;
		gbc.gridwidth = 2;
		gbc.anchor = GridBagConstraints.CENTER;
		rangeAxisSelectionCombobox = new JComboBox<>();
		rangeAxisSelectionCombobox.setToolTipText(
				I18N.getMessage(I18N.getGUIBundle(), "gui.action.add_parallel_line.range_axis_combobox.tip"));
		rangeAxisSelectionCombobox.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				updateYFieldValue();
			}
		});
		this.add(rangeAxisSelectionCombobox, gbc);

		gbc.gridx = 0;
		gbc.gridy = 2;
		gbc.fill = GridBagConstraints.NONE;
		gbc.weightx = 1;
		gbc.anchor = GridBagConstraints.WEST;
		gbc.insets = new Insets(5, 5, 2, 5);
		JLabel xLabel = new JLabel(I18N.getMessage(I18N.getGUIBundle(), "gui.action.add_parallel_line.width.label"));
		this.add(xLabel, gbc);

		gbc.gridx = 1;
		gbc.gridy = 2;
		gbc.insets = new Insets(2, 5, 2, 5);
		gbc.fill = GridBagConstraints.HORIZONTAL;
		xField = new JTextField();
		xField.setText(String.valueOf(x));
		xField.setInputVerifier(new InputVerifier() {

			@Override
			public boolean verify(JComponent input) {
				return verifyYInput(input);
			}
		});
		xField.setToolTipText(I18N.getMessage(I18N.getGUIBundle(), "gui.action.add_parallel_line.width.tip"));
		xField.setEnabled(false);
		this.add(xField, gbc);

		gbc.gridx = 0;
		gbc.gridy = 3;
		gbc.fill = GridBagConstraints.NONE;
		JLabel yLabel = new JLabel(I18N.getMessage(I18N.getGUIBundle(), "gui.action.add_parallel_line.height.label"));
		this.add(yLabel, gbc);

		gbc.gridx = 1;
		gbc.gridy = 3;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		yField = new JTextField();
		yField.setToolTipText(I18N.getMessage(I18N.getGUIBundle(), "gui.action.add_parallel_line.height.tip"));
		yField.setText(String.valueOf(y));
		yField.setInputVerifier(new InputVerifier() {

			@Override
			public boolean verify(JComponent input) {
				return verifyXInput(input);
			}
		});
		this.add(yField, gbc);

		gbc.gridx = 0;
		gbc.gridy = 4;
		gbc.fill = GridBagConstraints.NONE;
		gbc.insets = new Insets(10, 5, 0, 5);
		modifyLineButton = new JButton();
		modifyLineButton
				.setToolTipText(I18N.getMessage(I18N.getGUIBundle(), "gui.action.add_parallel_line.modify_line.tip"));
		modifyLineButton.setIcon(SwingTools.createIcon("16/"
				+ I18N.getMessage(I18N.getGUIBundle(), "gui.action.add_parallel_line.modify_line.icon")));
		modifyLineButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				modifyLine();
			}
		});
		this.add(modifyLineButton, gbc);

		gbc.gridx = 0;
		gbc.gridy = 5;
		gbc.gridwidth = 2;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.anchor = GridBagConstraints.CENTER;
		gbc.insets = new Insets(15, 5, 5, 5);
		this.add(new JSeparator(), gbc);

		gbc.gridx = 0;
		gbc.gridy = 6;
		gbc.gridwidth = 1;
		gbc.fill = GridBagConstraints.NONE;
		gbc.anchor = GridBagConstraints.WEST;
		gbc.insets = new Insets(5, 5, 5, 5);
		okButton = new JButton(I18N.getMessage(I18N.getGUIBundle(), "gui.action.add_parallel_line.ok.label"));
		okButton.setToolTipText(I18N.getMessage(I18N.getGUIBundle(), "gui.action.add_parallel_line.ok.tip"));
		okButton.setIcon(SwingTools.createIcon("24/"
				+ I18N.getMessage(I18N.getGUIBundle(), "gui.action.add_parallel_line.ok.icon")));
		okButton.setMnemonic(I18N.getMessage(I18N.getGUIBundle(), "gui.action.add_parallel_line.ok.mne").toCharArray()[0]);
		okButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				boolean successful = addSpecifiedLine();
				// don't dispose dialog if not successful
				if (!successful) {
					return;
				}

				AddParallelLineDialog.this.dispose();
			}
		});
		okButton.addKeyListener(new KeyAdapter() {

			@Override
			public void keyPressed(KeyEvent e) {
				if (e.getKeyCode() == KeyEvent.VK_ENTER) {
					okButton.doClick();
				}
			}
		});
		this.add(okButton, gbc);

		gbc.gridx = 1;
		gbc.gridy = 6;
		gbc.fill = GridBagConstraints.NONE;
		gbc.anchor = GridBagConstraints.EAST;
		cancelButton = new JButton(I18N.getMessage(I18N.getGUIBundle(), "gui.action.add_parallel_line.cancel.label"));
		cancelButton.setToolTipText(I18N.getMessage(I18N.getGUIBundle(), "gui.action.add_parallel_line.cancel.tip"));
		cancelButton.setIcon(SwingTools.createIcon("24/"
				+ I18N.getMessage(I18N.getGUIBundle(), "gui.action.add_parallel_line.cancel.icon")));
		cancelButton.setMnemonic(I18N.getMessage(I18N.getGUIBundle(), "gui.action.add_parallel_line.cancel.mne")
				.toCharArray()[0]);
		cancelButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				// cancel requested, close dialog
				AddParallelLineDialog.this.dispose();
			}
		});
		this.add(cancelButton, gbc);

		// misc settings
		this.setMinimumSize(new Dimension(300, 250));
		// center dialog
		this.setLocationRelativeTo(getOwner());
		this.setTitle(I18N.getMessage(I18N.getGUIBundle(), "gui.action.add_parallel_line.title.label"));
		this.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		this.setModal(true);

		this.addWindowListener(new WindowAdapter() {

			@Override
			public void windowActivated(WindowEvent e) {
				okButton.requestFocusInWindow();
			}
		});
	}

	/**
	 * Sets the mouse position (defines preselected x/y values).
	 * 
	 * @param mousePosition
	 */
	public void setMousePosition(Point mousePosition) {
		if (mousePosition == null) {
			throw new IllegalArgumentException("mousePosition must not be null!");
		}

		this.mousePosition = mousePosition;
	}

	/**
	 * Sets the current {@link JFreeChartPlotEngine} for this dialog.
	 * 
	 * @param engine
	 */
	public void setChartEngine(JFreeChartPlotEngine engine) {
		if (engine == null) {
			throw new IllegalArgumentException("engine must not be null!");
		}

		this.engine = engine;
		setPlotConfiguration(engine.getPlotInstance().getMasterPlotConfiguration());
	}

	/**
	 * Shows the dialog.
	 */
	public void showDialog() {
		if (line == null) {
			line = createLine(1.0);
		} else {
			line = line.clone();
		}
		if (horizontalLineRadiobutton.isSelected()) {
			line.setValue(Double.parseDouble(yField.getText()));
		} else {
			line.setValue(Double.parseDouble(xField.getText()));
		}
		setVisible(true);
	}

	/**
	 * Sets the current {@link PlotConfiguration} for this dialog.
	 * 
	 * @param plotConfig
	 */
	private void setPlotConfiguration(PlotConfiguration plotConfig) {
		if (plotConfig == null) {
			throw new IllegalArgumentException("plotConfig must not be null!");
		}

		this.plotConfig = plotConfig;
		Vector<RangeAxisConfig> rangeConfigsVector = new Vector<RangeAxisConfig>();
		String selectedItem = String.valueOf(rangeAxisSelectionCombobox.getSelectedItem());
		for (RangeAxisConfig config : this.plotConfig.getRangeAxisConfigs()) {
			rangeConfigsVector.add(config);
		}
		rangeAxisSelectionCombobox.setModel(new DefaultComboBoxModel<>(rangeConfigsVector));

		// reselect the previously selected RangeAxisConfig (if it is still there)
		if (selectedItem != null) {
			for (int i = 0; i < rangeAxisSelectionCombobox.getItemCount(); i++) {
				if (String.valueOf(rangeAxisSelectionCombobox.getItemAt(i)).equals(selectedItem)) {
					rangeAxisSelectionCombobox.setSelectedIndex(i);
					break;
				}
			}
		}

		// calculate preselected x/y values
		if (mousePosition != null) {
			Rectangle2D plotArea = engine.getChartPanel().getScreenDataArea();
			if (engine.getChartPanel().getChart().getPlot() instanceof XYPlot) {
				XYPlot plot = (XYPlot) engine.getChartPanel().getChart().getPlot();

				// calculate x value
				double chartX = plot.getDomainAxis().java2DToValue(mousePosition.getX(), plotArea, plot.getDomainAxisEdge());
				xField.setText(String.valueOf(chartX));

				// calculate y value
				for (int i = 0; i < plot.getRangeAxisCount(); i++) {
					ValueAxis config = plot.getRangeAxis(i);
					if (config != null && config.getLabel() != null) {
						if (config.getLabel().equals(String.valueOf(rangeAxisSelectionCombobox.getSelectedItem()))) {
							double chartY = config.java2DToValue(mousePosition.getY(), plotArea, plot.getRangeAxisEdge());
							yField.setText(String.valueOf(chartY));
						}
					}
				}
			}
		}
	}

	/**
	 * Horizontal line selected.
	 */
	private void setHorizontalLineSelected() {
		// horizontal line, so only y-value and rangeAxis is of interest, disable other fields
		rangeAxisSelectionCombobox.setEnabled(true);
		xField.setEnabled(false);
		yField.setEnabled(true);
		okButton.requestFocusInWindow();
	}

	/**
	 * Vertical line selected.
	 */
	private void setVerticalLineSelected() {
		// vertical line, so only x-value is of interest, disable other fields
		rangeAxisSelectionCombobox.setEnabled(false);
		xField.setEnabled(true);
		yField.setEnabled(false);
		okButton.requestFocusInWindow();
	}

	/**
	 * Updates the preselected y-value.
	 */
	private void updateYFieldValue() {
		// update preselected y value because range axis has been changed
		if (mousePosition != null) {
			Rectangle2D plotArea = engine.getChartPanel().getScreenDataArea();
			if (engine.getChartPanel().getChart().getPlot() instanceof XYPlot) {
				XYPlot plot = (XYPlot) engine.getChartPanel().getChart().getPlot();

				// calculate y value
				for (int i = 0; i < plot.getRangeAxisCount(); i++) {
					ValueAxis config = plot.getRangeAxis(i);
					if (config != null && config.getLabel() != null) {
						if (config.getLabel().equals(String.valueOf(rangeAxisSelectionCombobox.getSelectedItem()))) {
							double chartY = config.java2DToValue(mousePosition.getY(), plotArea, plot.getRangeAxisEdge());
							yField.setText(String.valueOf(chartY));
						}
					}
				}
			}
		}
	}

	/**
	 * Verify that the y-value is correct.
	 * 
	 * @param input
	 * @return true if the value is valid; false otherwise
	 */
	private boolean verifyYInput(JComponent input) {
		JTextField textField = (JTextField) input;
		String inputString = textField.getText();
		try {
			Double.parseDouble(inputString);
		} catch (NumberFormatException e) {
			textField.setForeground(Color.RED);
			return false;
		}

		textField.setForeground(Color.BLACK);
		return true;
	}

	/**
	 * Verify that the x-value is correct.
	 * 
	 * @param input
	 * @return true if the value is valid; false otherwise
	 */
	private boolean verifyXInput(JComponent input) {
		JTextField textField = (JTextField) input;
		String inputString = textField.getText();
		try {
			Double.parseDouble(inputString);
		} catch (NumberFormatException e) {
			textField.setForeground(Color.RED);
			return false;
		}

		textField.setForeground(Color.BLACK);
		return true;
	}

	/**
	 * Creates the specified line.
	 * 
	 * @return true if the line has been created; false otherwise
	 */
	private boolean addSpecifiedLine() {
		if (horizontalLineRadiobutton.isSelected()) {
			Object selectedItem = rangeAxisSelectionCombobox.getSelectedItem();
			if (selectedItem != null && selectedItem instanceof RangeAxisConfig) {
				// make sure y value is valid, otherwise don't do anything!
				if (!yField.getInputVerifier().verify(yField)) {
					yField.requestFocusInWindow();
					return false;
				}
				RangeAxisConfig config = (RangeAxisConfig) selectedItem;
				if (line == null) {
					line = createLine(Double.parseDouble(yField.getText()));
				}
				line.setValue(Double.parseDouble(yField.getText()));
				config.getCrossHairLines().addLine(line);
			}
		} else if (verticalLineRadiobutton.isSelected()) {
			// make sure x value is valid, otherwise don't do anything!
			if (!xField.getInputVerifier().verify(xField)) {
				xField.requestFocusInWindow();
				return false;
			}
			if (line == null) {
				line = createLine(Double.parseDouble(xField.getText()));
			}
			line.setValue(Double.parseDouble(xField.getText()));
			plotConfig.getDomainConfigManager().getCrosshairLines().addLine(line);
		}

		return true;
	}

	/**
	 * Modifes the line.
	 */
	private void modifyLine() {
		if (line == null) {
			line = createLine(1.0);
		}
		if (dialog == null) {
			dialog = new EditParallelLineDialog();
		}

		if (horizontalLineRadiobutton.isSelected()) {
			// make sure y value is valid, otherwise don't do anything!
			if (!yField.getInputVerifier().verify(yField)) {
				yField.requestFocusInWindow();
				return;
			}
			line.setValue(Double.parseDouble(yField.getText()));
		} else {
			// make sure x value is valid, otherwise don't do anything!
			if (!xField.getInputVerifier().verify(xField)) {
				xField.requestFocusInWindow();
				return;
			}
			line.setValue(Double.parseDouble(xField.getText()));
		}
		dialog.setLine(line, false);
		dialog.showDialog();
	}

	/**
	 * Creates a new {@link AxisParallelLineConfiguration} with solid {@link LineStyle}.
	 * 
	 * @param value
	 * @return
	 */
	private AxisParallelLineConfiguration createLine(double value) {
		AxisParallelLineConfiguration line = new AxisParallelLineConfiguration(value, false);
		line.getFormat().setStyle(LineStyle.SOLID);
		return line;
	}
}
