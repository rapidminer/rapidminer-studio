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

import com.rapidminer.Process;
import com.rapidminer.gui.MainFrame;
import com.rapidminer.gui.tools.ResourceDockKey;
import com.rapidminer.gui.tools.ViewToolBar;
import com.rapidminer.operator.Operator;
import com.vlsolutions.swing.docking.DockKey;
import com.vlsolutions.swing.docking.Dockable;

import java.awt.BorderLayout;
import java.awt.Component;
import java.util.List;

import javax.swing.JPanel;


/**
 * 
 * @author Simon Fischer
 * 
 */
public class ProcessContextProcessEditor extends JPanel implements ProcessEditor, Dockable {

	public static final String PROCESS_CONTEXT_DOCKKEY = "process_context";

	private static final long serialVersionUID = 1L;

	private final ProcessContextEditor editor = new ProcessContextEditor(null, null);

	public ProcessContextProcessEditor() {
		super(null);
		setLayout(new BorderLayout());
		ViewToolBar toolBar = new ViewToolBar();
		add(toolBar, BorderLayout.NORTH);
		add(editor, BorderLayout.CENTER);
	}

	@Override
	public void processChanged(Process process) {
		editor.setProcess(process, null);
	}

	@Override
	public void processUpdated(Process process) {}

	@Override
	public void setSelection(List<Operator> selection) {}

	private final DockKey DOCK_KEY = new ResourceDockKey(PROCESS_CONTEXT_DOCKKEY);
	private Component dockComponent;
	{
		DOCK_KEY.setDockGroup(MainFrame.DOCK_GROUP_ROOT);
	}

	@Override
	public Component getComponent() {
		if (dockComponent == null) {
			dockComponent = this;
		}
		return dockComponent;
	}

	@Override
	public DockKey getDockKey() {
		return DOCK_KEY;
	}
}
