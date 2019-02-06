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
package com.rapidminer.gui.plotter;

import com.rapidminer.datatable.DataTable;
import com.rapidminer.gui.RapidMinerGUI;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;


/**
 * This dialog can be used to create a plot dialog containing a complete plotter panel (including
 * all options and user interfaces) from a given {@link DataTable}. This might be useful if an
 * operator should display some data or results.
 * 
 * @author Ingo Mierswa
 * @deprecated since 9.2.0
 */
@Deprecated
public class SimplePlotterPanelDialog extends JDialog {

	private static final long serialVersionUID = -3618058787783237559L;

	public SimplePlotterPanelDialog(DataTable dataTable) {
		this(dataTable, true);
	}

	public SimplePlotterPanelDialog(DataTable dataTable, boolean modal) {
		this(RapidMinerGUI.getMainFrame(), dataTable, -1, -1, modal);
	}

	public SimplePlotterPanelDialog(Frame owner, final DataTable dataTable, int width, int height, boolean modal) {
		super(owner, dataTable.getName(), modal);
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		PlotterPanel plotterPanel = new PlotterPanel(dataTable, PlotterConfigurationModel.DATA_SET_PLOTTER_SELECTION);
		getContentPane().add(plotterPanel, BorderLayout.CENTER);

		JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		JButton okButton = new JButton("Ok");
		okButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				ok();
			}
		});
		buttonPanel.add(okButton);

		getContentPane().add(buttonPanel, BorderLayout.SOUTH);

		if ((width < 0) || (height < 0)) {
			setSize(600, 400);
		} else {
			setSize(width, height);
		}
		setLocationRelativeTo(owner);
	}

	private void ok() {
		dispose();
	}
}
