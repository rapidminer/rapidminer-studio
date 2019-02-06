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
package com.rapidminer.gui.plotter.charts;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;

import com.rapidminer.gui.ApplicationFrame;
import com.rapidminer.gui.tools.ExtendedJScrollPane;


/**
 * This is a dialog which shows two checkboxes for each possible dimension where the user can select
 * if points and / or lines should be drawn for each dimension.
 * 
 * @author Ingo Mierswa
 * @deprecated since 9.2.0
 */
@Deprecated
public class PointsAndLinesDialog extends JDialog {

	private static final long serialVersionUID = 0L;

	private boolean ok = false;

	private DefaultTableModel model;

	public PointsAndLinesDialog(String[] names, boolean[] points, boolean[] lines) {
		super(ApplicationFrame.getApplicationFrame(), "Points and Lines", true);

		Object[][] data = new Object[names.length][3];
		for (int i = 0; i < names.length; i++) {
			data[i][0] = names[i];
			data[i][1] = points[i];
			data[i][2] = lines[i];
		}

		model = new DefaultTableModel(data, new String[] { "Dimension", "Points", "Lines" }) {

			private static final long serialVersionUID = 8034022478180821552L;

			@Override
			public Class<?> getColumnClass(int index) {
				if (index == 0) {
					return String.class;
				} else {
					return Boolean.class;
				}
			}
		};

		JTable table = new JTable(model);

		setLayout(new BorderLayout());
		ExtendedJScrollPane tablePane = new ExtendedJScrollPane(table);
		add(tablePane, BorderLayout.CENTER);

		JPanel mainButtonPanel = new JPanel();
		GridBagLayout layout = new GridBagLayout();
		mainButtonPanel.setLayout(layout);
		GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.BOTH;
		c.insets = new Insets(4, 4, 4, 4);
		c.weightx = 1.0d;
		c.weighty = 1.0d;

		JPanel selectionButtonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));

		JButton pointsButton = new JButton("All Points");
		pointsButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				int selectedCount = 0;
				int deselectedCount = 0;
				for (int i = 0; i < model.getRowCount(); i++) {
					if ((Boolean) model.getValueAt(i, 1)) {
						selectedCount++;
					} else {
						deselectedCount++;
					}
				}

				if (selectedCount > deselectedCount) {
					for (int i = 0; i < model.getRowCount(); i++) {
						model.setValueAt(Boolean.valueOf(false), i, 1);
					}
				} else {
					for (int i = 0; i < model.getRowCount(); i++) {
						model.setValueAt(Boolean.valueOf(true), i, 1);
					}
				}
			}
		});
		selectionButtonPanel.add(pointsButton);

		JButton linesButton = new JButton("All Lines");
		linesButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				int selectedCount = 0;
				int deselectedCount = 0;
				for (int i = 0; i < model.getRowCount(); i++) {
					if ((Boolean) model.getValueAt(i, 2)) {
						selectedCount++;
					} else {
						deselectedCount++;
					}
				}

				if (selectedCount > deselectedCount) {
					for (int i = 0; i < model.getRowCount(); i++) {
						model.setValueAt(Boolean.valueOf(false), i, 2);
					}
				} else {
					for (int i = 0; i < model.getRowCount(); i++) {
						model.setValueAt(Boolean.valueOf(true), i, 2);
					}
				}
			}
		});
		selectionButtonPanel.add(linesButton);

		JPanel dialogButtonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		JButton okButton = new JButton("Ok");
		okButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				setOk(true);
				dispose();
			}
		});
		dialogButtonPanel.add(okButton);

		JButton cancelButton = new JButton("Cancel");
		cancelButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				setOk(false);
				dispose();
			}
		});
		dialogButtonPanel.add(cancelButton);

		layout.setConstraints(selectionButtonPanel, c);
		mainButtonPanel.add(selectionButtonPanel);

		layout.setConstraints(dialogButtonPanel, c);
		mainButtonPanel.add(dialogButtonPanel);

		add(mainButtonPanel, BorderLayout.SOUTH);

		pack();

		setLocationRelativeTo(getOwner());
	}

	public void setOk(boolean ok) {
		this.ok = ok;
	}

	public boolean isOk() {
		return this.ok;
	}

	public boolean showPoints(int index) {
		return (Boolean) model.getValueAt(index, 1);
	}

	public boolean showLines(int index) {
		return (Boolean) model.getValueAt(index, 2);
	}
}
