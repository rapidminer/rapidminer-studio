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
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.table.TableColumn;

import com.rapidminer.gui.look.Colors;
import com.rapidminer.gui.look.RapidLookTools;
import com.rapidminer.gui.processeditor.results.ResultDisplayTools;
import com.rapidminer.gui.properties.PropertyPanel;
import com.rapidminer.gui.tools.ExtendedJScrollPane;
import com.rapidminer.gui.tools.ExtendedJTable;
import com.rapidminer.operator.learner.associations.AssociationRules;


/**
 * The viewer for association rule models.
 *
 * @author Ingo Mierswa
 */
public class AssociationRuleTableViewer extends JPanel implements AssociationRuleFilterListener {

	private static final long serialVersionUID = 4589558372186371570L;

	private ExtendedJTable table = new ExtendedJTable();

	private AssociationRuleTableModel model = null;

	public AssociationRuleTableViewer(AssociationRules rules) {
		if (rules != null && rules.getNumberOfRules() > 0) {
			this.model = new AssociationRuleTableModel(rules);
			setLayout(new BorderLayout());
			JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
			splitPane.setBorder(null);

			// conclusion list
			AssociationRuleFilter filter = new AssociationRuleFilter(rules);
			filter.addAssociationRuleFilterListener(this);
			filter.setBorder(BorderFactory.createEmptyBorder(10, 10, 0, 5));
			splitPane.add(filter, 0);

			// main panel
			{
				JPanel mainPanel = new JPanel();
				mainPanel.setOpaque(true);
				mainPanel.setBackground(Colors.WHITE);
				GridBagLayout layout = new GridBagLayout();
				mainPanel.setLayout(layout);
				GridBagConstraints c = new GridBagConstraints();
				c.fill = GridBagConstraints.BOTH;
				c.weightx = 1;
				c.weighty = 1;
				c.gridwidth = GridBagConstraints.REMAINDER;
				c.insets = new Insets(15, 10, 10, 10);

				table.setModel(model);
				table.setRowHeight(PropertyPanel.VALUE_CELL_EDITOR_HEIGHT);
				table.setRowHighlighting(true);
				table.setAutoResizeMode(ExtendedJTable.AUTO_RESIZE_OFF);
				JScrollPane tablePane = new ExtendedJScrollPane(table);
				tablePane.setBorder(null);
				tablePane.setBackground(Colors.WHITE);
				tablePane.getViewport().setBackground(Colors.WHITE);
				layout.setConstraints(tablePane, c);
				mainPanel.add(tablePane);

				setColumnSizes();

				splitPane.add(mainPanel, 1);
				table.getTableHeader().setBackground(Colors.WHITE);
				table.getTableHeader().putClientProperty(RapidLookTools.PROPERTY_TABLE_HEADER_BACKGROUND, Colors.WHITE);
			}
			filter.triggerFilter();

			add(splitPane, BorderLayout.CENTER);
		} else {
			add(ResultDisplayTools.createErrorComponent("No rules found"), BorderLayout.CENTER);
		}
	}

	private void setColumnSizes() {
		TableColumn col = table.getColumnModel().getColumn(0);
		col.setPreferredWidth(50);
		col = table.getColumnModel().getColumn(1);
		col.setPreferredWidth(300);
		col = table.getColumnModel().getColumn(2);
		col.setPreferredWidth(300);
		col = table.getColumnModel().getColumn(3);
		col.setPreferredWidth(100);
		col = table.getColumnModel().getColumn(4);
		col.setPreferredWidth(100);
	}

	@Override
	public void setFilter(boolean[] filter) {
		this.model.setFilter(filter);
		setColumnSizes();
	}
}
