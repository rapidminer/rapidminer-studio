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
package com.rapidminer.gui.processeditor;

import java.awt.BorderLayout;
import java.awt.Component;

import javax.swing.Icon;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import com.rapidminer.datatable.DataTable;
import com.rapidminer.gui.MainFrame;
import com.rapidminer.gui.tools.ResourceLabel;
import com.rapidminer.gui.tools.SwingTools;
import com.rapidminer.gui.viewer.DataTableViewer;
import com.vlsolutions.swing.docking.DockKey;
import com.vlsolutions.swing.docking.Dockable;


/**
 *
 * @author Simon Fischer
 *
 */
public class ProcessLogTab extends JPanel implements Dockable {

	private static final long serialVersionUID = 1L;
	private static Icon DATA_TABLE_ICON = SwingTools.createIcon("16/table.png");
	public static final String DOCKKEY_PREFIX = "datatable_";

	private Component viewer;
	private final DockKey dockKey;

	public ProcessLogTab(String key) {
		setLayout(new BorderLayout());
		this.dockKey = new DockKey(key);
		dockKey.setIcon(DATA_TABLE_ICON);
		dockKey.setDockGroup(MainFrame.DOCK_GROUP_RESULTS);
	}

	public void setDataTableViewer(DataTableViewer viewer) {
		if (viewer == this.viewer) {
			return;
		}
		if (this.viewer != null) {
			remove(this.viewer);
		}
		if (viewer != null) {
			this.viewer = viewer;
			dockKey.setName(viewer.getDataTable().getName());
		} else {
			this.viewer = makeTableNotRestoredLabel();
		}
		add(this.viewer, BorderLayout.CENTER);
	}

	private static ResourceLabel makeTableNotRestoredLabel() {
		ResourceLabel label = new ResourceLabel("resulttab.table_cannot_be_restored");
		label.setHorizontalAlignment(SwingConstants.CENTER);
		label.setIcon(SwingTools.createIcon("16/sign_warning.png"));
		return label;
	}

	public void freeResources() {
		if (viewer != null) {
			remove(viewer);
		}
		viewer = null;
	}

	@Override
	public Component getComponent() {
		return this;
	}

	@Override
	public DockKey getDockKey() {
		return dockKey;
	}

	public DataTable getDataTable() {
		if (viewer instanceof DataTableViewer) {
			return ((DataTableViewer) this.viewer).getDataTable();
		} else {
			return null;
		}
	}
}
