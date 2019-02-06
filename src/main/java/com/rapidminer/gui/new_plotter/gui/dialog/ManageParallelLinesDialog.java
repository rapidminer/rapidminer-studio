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

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.List;
import java.util.Vector;

import javax.swing.ButtonGroup;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.ListSelectionModel;
import javax.swing.WindowConstants;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import com.rapidminer.gui.ApplicationFrame;
import com.rapidminer.gui.new_plotter.configuration.AxisParallelLineConfiguration;
import com.rapidminer.gui.new_plotter.configuration.PlotConfiguration;
import com.rapidminer.gui.new_plotter.configuration.RangeAxisConfig;
import com.rapidminer.gui.new_plotter.engine.jfreechart.JFreeChartPlotEngine;
import com.rapidminer.gui.tools.ExtendedJScrollPane;
import com.rapidminer.gui.tools.SwingTools;
import com.rapidminer.tools.I18N;


/**
 * This dialog allows the user to manage existing crosshair lines in an advanced chart.
 * 
 * @author Marco Boeck
 * @deprecated since 9.2.0
 */
@Deprecated
public class ManageParallelLinesDialog extends JDialog {

	/** the ok {@link JButton} */
	private JButton okButton;

	/** the cancel {@link JButton} */
	private JButton cancelButton;

	/** if selected, the line will be horizontal */
	private JRadioButton horizontalLineRadiobutton;

	/** if selected, the line will be vertical */
	private JRadioButton verticalLineRadiobutton;

	/**
	 * the {@link JComboBox} where the {@link RangeAxisConfig} will be selected if horizontal line
	 * is selected
	 */
	private JComboBox<RangeAxisConfig> rangeAxisSelectionCombobox;

	/**
	 * the {@link JList} which displays the appropriate crosshair lines according to the user
	 * selections
	 */
	private JList<AxisParallelLineConfiguration> linesList;

	/** this button deletes the selected line(s) */
	private JButton deleteSelectedLinesButton;

	/** this button modifies the selected line */
	private JButton modifySelectedLineButton;

	/** this button adds a new line */
	private JButton addNewLineButton;

	/** the current {@link JFreeChartPlotEngine} */
	private JFreeChartPlotEngine engine;

	/** the current {@link PlotConfiguration} */
	private PlotConfiguration plotConfig;

	/** the {@link EditParallelLineDialog} instance */
	private EditParallelLineDialog modifyLineDialog;

	/** the {@link AddParallelLineDialog} instance */
	private AddParallelLineDialog addLineDialog;

	private static final long serialVersionUID = 1932257219370926682L;

	/**
	 * Creates a new {@link ManageParallelLinesDialog}.
	 */
	public ManageParallelLinesDialog() {
		super(ApplicationFrame.getApplicationFrame());
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
				"gui.action.manage_parallel_lines.horizontal.label"));
		horizontalLineRadiobutton.setToolTipText(I18N.getMessage(I18N.getGUIBundle(),
				"gui.action.manage_parallel_lines.horizontal.tip"));
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
				"gui.action.manage_parallel_lines.vertical.label"));
		verticalLineRadiobutton.setToolTipText(I18N.getMessage(I18N.getGUIBundle(),
				"gui.action.manage_parallel_lines.vertical.tip"));
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
				I18N.getMessage(I18N.getGUIBundle(), "gui.action.manage_parallel_lines.range_axis_combobox.tip"));
		rangeAxisSelectionCombobox.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				setHorizontalLineSelected();
			}
		});
		this.add(rangeAxisSelectionCombobox, gbc);

		gbc.gridx = 0;
		gbc.gridy = 2;
		gbc.fill = GridBagConstraints.BOTH;
		gbc.weightx = 1;
		gbc.weighty = 1;
		gbc.gridwidth = 2;
		gbc.anchor = GridBagConstraints.CENTER;
		linesList = new JList<AxisParallelLineConfiguration>();
		linesList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		linesList.addListSelectionListener(new ListSelectionListener() {

			@Override
			public void valueChanged(ListSelectionEvent e) {
				updateButtonStates();
			}
		});
		linesList.addKeyListener(new KeyAdapter() {

			@Override
			public void keyPressed(KeyEvent e) {
				if (e.getKeyCode() == KeyEvent.VK_DELETE) {
					deleteSelectedLines();
				}
			}
		});
		linesList.addMouseListener(new MouseAdapter() {

			@Override
			public void mouseClicked(MouseEvent e) {
				if (e.getClickCount() == 2) {
					modifySelectedLine();
				}
			}
		});
		JScrollPane listScrollPane = new ExtendedJScrollPane(linesList);
		this.add(listScrollPane, gbc);

		JPanel listActionPanel = new JPanel();

		addNewLineButton = new JButton();
		addNewLineButton
				.setToolTipText(I18N.getMessage(I18N.getGUIBundle(), "gui.action.manage_parallel_lines.add_line.tip"));
		addNewLineButton.setIcon(SwingTools.createIcon("16/"
				+ I18N.getMessage(I18N.getGUIBundle(), "gui.action.manage_parallel_lines.add_line.icon")));
		addNewLineButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				addNewLine();
			}
		});
		listActionPanel.add(addNewLineButton);

		modifySelectedLineButton = new JButton();
		modifySelectedLineButton.setToolTipText(I18N.getMessage(I18N.getGUIBundle(),
				"gui.action.manage_parallel_lines.modify_line.tip"));
		modifySelectedLineButton.setIcon(SwingTools.createIcon("16/"
				+ I18N.getMessage(I18N.getGUIBundle(), "gui.action.manage_parallel_lines.modify_line.icon")));
		modifySelectedLineButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				modifySelectedLine();
			}
		});
		listActionPanel.add(modifySelectedLineButton);

		deleteSelectedLinesButton = new JButton();
		deleteSelectedLinesButton.setToolTipText(I18N.getMessage(I18N.getGUIBundle(),
				"gui.action.manage_parallel_lines.delete_lines.tip"));
		deleteSelectedLinesButton.setIcon(SwingTools.createIcon("16/"
				+ I18N.getMessage(I18N.getGUIBundle(), "gui.action.manage_parallel_lines.delete_lines.icon")));
		deleteSelectedLinesButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				deleteSelectedLines();
			}
		});
		listActionPanel.add(deleteSelectedLinesButton);

		gbc.gridx = 0;
		gbc.gridy = 3;
		gbc.fill = GridBagConstraints.NONE;
		gbc.weightx = 0;
		gbc.weighty = 0;
		gbc.gridwidth = 1;
		gbc.anchor = GridBagConstraints.WEST;
		this.add(listActionPanel, gbc);

		gbc.gridx = 0;
		gbc.gridy = 4;
		gbc.gridwidth = 2;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.weightx = 1;
		gbc.weighty = 0;
		gbc.anchor = GridBagConstraints.CENTER;
		gbc.insets = new Insets(15, 5, 5, 5);
		this.add(new JSeparator(), gbc);

		gbc.gridx = 0;
		gbc.gridy = 5;
		gbc.gridwidth = 1;
		gbc.fill = GridBagConstraints.NONE;
		gbc.anchor = GridBagConstraints.WEST;
		gbc.insets = new Insets(5, 5, 5, 5);
		okButton = new JButton(I18N.getMessage(I18N.getGUIBundle(), "gui.action.manage_parallel_lines.ok.label"));
		// okButton.setToolTipText(I18N.getMessage(I18N.getGUIBundle(),
		// "gui.action.manage_parallel_lines.ok.tip"));
		okButton.setIcon(SwingTools.createIcon("24/"
				+ I18N.getMessage(I18N.getGUIBundle(), "gui.action.manage_parallel_lines.ok.icon")));
		okButton.setMnemonic(I18N.getMessage(I18N.getGUIBundle(), "gui.action.manage_parallel_lines.ok.mne").toCharArray()[0]);
		okButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				ManageParallelLinesDialog.this.dispose();
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
		gbc.gridy = 5;
		gbc.fill = GridBagConstraints.NONE;
		gbc.anchor = GridBagConstraints.EAST;
		cancelButton = new JButton(I18N.getMessage(I18N.getGUIBundle(), "gui.action.manage_parallel_lines.cancel.label"));
		// cancelButton.setToolTipText(I18N.getMessage(I18N.getGUIBundle(),
		// "gui.action.manage_parallel_lines.cancel.tip"));
		cancelButton.setIcon(SwingTools.createIcon("24/"
				+ I18N.getMessage(I18N.getGUIBundle(), "gui.action.manage_parallel_lines.cancel.icon")));
		cancelButton.setMnemonic(I18N.getMessage(I18N.getGUIBundle(), "gui.action.manage_parallel_lines.cancel.mne")
				.toCharArray()[0]);
		cancelButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				// cancel requested, close dialog
				ManageParallelLinesDialog.this.dispose();
			}
		});
		this.add(cancelButton, gbc);

		// misc settings
		this.setMinimumSize(new Dimension(400, 300));
		// center dialog
		this.setLocationRelativeTo(getOwner());
		this.setTitle(I18N.getMessage(I18N.getGUIBundle(), "gui.action.manage_parallel_lines.title.label"));
		this.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		this.setModal(true);
		this.addWindowListener(new WindowAdapter() {

			@Override
			public void windowActivated(WindowEvent e) {
				linesList.requestFocusInWindow();
			}
		});
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
		updateLineList();
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
		for (RangeAxisConfig config : this.plotConfig.getRangeAxisConfigs()) {
			rangeConfigsVector.add(config);
		}
		rangeAxisSelectionCombobox.setModel(new DefaultComboBoxModel<>(rangeConfigsVector));
	}

	/**
	 * Horizontal lines selected.
	 */
	private void setHorizontalLineSelected() {
		// show horizontal lines
		rangeAxisSelectionCombobox.setEnabled(true);
		linesList.clearSelection();

		// get all vertical lines of the selected RangeAxisConfig and display them
		RangeAxisConfig selectedConfig = (RangeAxisConfig) rangeAxisSelectionCombobox.getSelectedItem();
		DefaultListModel<AxisParallelLineConfiguration> model = new DefaultListModel<AxisParallelLineConfiguration>();
		if (selectedConfig != null) {
			List<AxisParallelLineConfiguration> rangeAxisLines = selectedConfig.getCrossHairLines().getLines();
			for (int i = 0; i < rangeAxisLines.size(); i++) {
				AxisParallelLineConfiguration line = rangeAxisLines.get(i);
				model.addElement(line);
			}
		}

		linesList.setModel(model);
	}

	/**
	 * Vertical lines selected.
	 */
	private void setVerticalLineSelected() {
		// show horizontal lines
		rangeAxisSelectionCombobox.setEnabled(false);
		linesList.clearSelection();

		// get all horizontal lines and display them
		DefaultListModel<AxisParallelLineConfiguration> model = new DefaultListModel<AxisParallelLineConfiguration>();
		List<AxisParallelLineConfiguration> domainLines = engine.getPlotInstance().getMasterPlotConfiguration()
				.getDomainConfigManager().getCrosshairLines().getLines();
		for (int i = 0; i < domainLines.size(); i++) {
			AxisParallelLineConfiguration line = domainLines.get(i);
			model.addElement(line);
		}

		linesList.setModel(model);
	}

	/**
	 * Deletes the selected lines.
	 */
	private void deleteSelectedLines() {
		// no selection?
		if (linesList.getSelectedIndex() == -1) {
			return;
		}

		for (AxisParallelLineConfiguration line : linesList.getSelectedValuesList()) {
			if (horizontalLineRadiobutton.isSelected()) {
				// horizontal, remove from selected RangeAxisConfig
				RangeAxisConfig config = (RangeAxisConfig) rangeAxisSelectionCombobox.getSelectedItem();
				config.getCrossHairLines().removeLine(line);
			} else {
				// vertical, remove from DomainConfigManager
				engine.getPlotInstance().getMasterPlotConfiguration().getDomainConfigManager().getCrosshairLines()
						.removeLine(line);
			}
		}

		updateLineList();
	}

	/**
	 * Modifes the selected line.
	 */
	private void modifySelectedLine() {
		// cannot modify if more than one is selected
		if (linesList.getSelectedIndices().length > 1) {
			return;
		}
		AxisParallelLineConfiguration line = linesList.getSelectedValue();
		if (line != null) {
			if (modifyLineDialog == null) {
				modifyLineDialog = new EditParallelLineDialog();
			}

			modifyLineDialog.setLine(line, true);
			modifyLineDialog.showDialog();
		}
	}

	/**
	 * Adds a new line.
	 */
	private void addNewLine() {
		if (addLineDialog == null) {
			addLineDialog = new AddParallelLineDialog();
		}

		addLineDialog.setChartEngine(engine);
		addLineDialog.showDialog();

		// after added new line, update list
		if (horizontalLineRadiobutton.isSelected()) {
			setHorizontalLineSelected();
		} else if (verticalLineRadiobutton.isSelected()) {
			setVerticalLineSelected();
		}
	}

	/**
	 * Updates the displayed list of lines.
	 */
	private void updateLineList() {
		// update displayed list of lines
		if (horizontalLineRadiobutton.isSelected()) {
			setHorizontalLineSelected();
		} else {
			setVerticalLineSelected();
		}
		updateButtonStates();
	}

	/**
	 * Updates the delete/edit buttons according to the selection.
	 */
	private void updateButtonStates() {
		// enable buttons according to selection
		int size = linesList.getSelectedValuesList().size();
		deleteSelectedLinesButton.setEnabled(size != 0);
		modifySelectedLineButton.setEnabled(size == 1);
	}
}
