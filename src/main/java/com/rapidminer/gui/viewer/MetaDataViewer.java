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
package com.rapidminer.gui.viewer;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;

import com.rapidminer.example.ExampleSet;
import com.rapidminer.gui.tools.ExtendedJScrollPane;
import com.rapidminer.gui.tools.ProgressThread;
import com.rapidminer.gui.tools.ResourceAction;
import com.rapidminer.gui.tools.ResourceActionAdapter;
import com.rapidminer.gui.tools.SwingTools;
import com.rapidminer.gui.tools.ViewToolBar;
import com.rapidminer.gui.tools.components.DropDownPopupButton;
import com.rapidminer.gui.tools.components.DropDownPopupButton.PopupMenuProvider;
import com.rapidminer.gui.viewer.metadata.MetaDataStatisticsViewer;
import com.rapidminer.report.Tableable;
import com.rapidminer.tools.ProgressListener;


/**
 * Can be used to display (parts of) the meta data by means of a JTable.
 *
 * @author Ingo Mierswa, Tobias Malbrecht
 * @deprecated use {@link MetaDataStatisticsViewer} instead.
 */
@Deprecated
public class MetaDataViewer extends JPanel implements Tableable {

	private static class ToggleShowColumnItem extends JCheckBoxMenuItem implements ActionListener {

		private static final long serialVersionUID = 570766967933245379L;

		private int index;

		private MetaDataViewerTable metaDataTable;

		ToggleShowColumnItem(String name, int index, boolean state, MetaDataViewerTable metaDataTable) {
			super("Show column '" + name + "'", state);
			setToolTipText("Toggles if the column with name '" + name + "' should be displayed");
			setIcon(SwingTools.createIcon("16/table_selection_column.png"));
			addActionListener(this);
			this.index = index;
			this.metaDataTable = metaDataTable;
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			metaDataTable.getMetaDataModel().setShowColumn(index, isSelected());
		}
	}

	private static final long serialVersionUID = 5466205420267797125L;

	private JLabel generalInfo = new JLabel();

	{
		generalInfo.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 0));
	}

	private MetaDataViewerTable metaDataTable = new MetaDataViewerTable();

	public MetaDataViewer(ExampleSet exampleSet, boolean showOptions) {
		super(new BorderLayout());
		ViewToolBar toolBar = new ViewToolBar();
		toolBar.add(generalInfo, ViewToolBar.LEFT);

		if (showOptions) {
			DropDownPopupButton button = new DropDownPopupButton(new ResourceActionAdapter(true, "select_columns"),
					new PopupMenuProvider() {

						@Override
						public JPopupMenu getPopupMenu() {
							JPopupMenu menu = new JPopupMenu();
							for (int i = 0; i < MetaDataViewerTableModel.COLUMN_NAMES.length; i++) {
								menu.add(new ToggleShowColumnItem(MetaDataViewerTableModel.COLUMN_NAMES[i], i,
										metaDataTable.getMetaDataModel().getShowColumn(i), metaDataTable));
							}
							return menu;
						}

					});
			button.setText("");
			toolBar.add(button, ViewToolBar.RIGHT);

			Action calculateStatisticsAction = new ResourceAction(true, "calculate_statistics") {

				private static final long serialVersionUID = 8763079896628342561L;

				@Override
				public void loggedActionPerformed(ActionEvent e) {
					ProgressThread t = new ProgressThread("calculate_statistics") {

						@Override
						public void run() {
							ProgressListener l = getProgressListener();
							l.setTotal(100);
							l.setCompleted(10);
							metaDataTable.getMetaDataModel().calculateStatistics();
							l.setCompleted(100);
						}
					};
					t.start();

				}
			};
			toolBar.add(calculateStatisticsAction, ViewToolBar.RIGHT);
		}

		add(toolBar, BorderLayout.NORTH);

		JScrollPane tableScrollPane = new ExtendedJScrollPane(metaDataTable);
		tableScrollPane.setBorder(null);
		add(tableScrollPane, BorderLayout.CENTER);

		setExampleSet(exampleSet);
	}

	public void setExampleSet(ExampleSet exampleSet) {
		if (exampleSet != null) {
			StringBuffer infoText = new StringBuffer("ExampleSet (");
			int noExamples = exampleSet.size();
			infoText.append(noExamples);
			infoText.append(noExamples == 1 ? " example, " : " examples, ");
			int noSpecial = exampleSet.getAttributes().specialSize();
			infoText.append(noSpecial);
			infoText.append(noSpecial == 1 ? " special attribute, " : " special attributes, ");
			int noRegular = exampleSet.getAttributes().size();
			infoText.append(noRegular);
			infoText.append(noRegular == 1 ? " regular attribute)" : " regular attributes)");
			generalInfo.setText(infoText.toString());
			metaDataTable.setExampleSet(exampleSet);
		} else {
			generalInfo.setText("no examples");
			metaDataTable.setExampleSet(null);
		}
	}

	/* Reporting methods */

	@Override
	public void prepareReporting() {
		metaDataTable.prepareReporting();
	}

	@Override
	public void finishReporting() {
		metaDataTable.finishReporting();
	}

	@Override
	public String getColumnName(int columnIndex) {
		return metaDataTable.getColumnName(columnIndex);
	}

	@Override
	public String getCell(int row, int column) {
		return metaDataTable.getCell(row, column);
	}

	@Override
	public int getColumnNumber() {
		return metaDataTable.getColumnNumber();
	}

	@Override
	public int getRowNumber() {
		return metaDataTable.getRowNumber();
	}

	@Override
	public boolean isFirstLineHeader() {
		return false;
	}

	@Override
	public boolean isFirstColumnHeader() {
		return false;
	}
}
