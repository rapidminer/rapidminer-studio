/**
 * Copyright (C) 2001-2016 by RapidMiner and the contributors
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
package com.rapidminer.gui;

import com.rapidminer.Process;
import com.rapidminer.ProcessListener;
import com.rapidminer.RapidMiner;
import com.rapidminer.gui.processeditor.ProcessEditor;
import com.rapidminer.operator.Operator;

import java.util.List;


/**
 * This listener must be created after the GUI is set up. It will then register automatically with
 * every newly created or opened process.
 * 
 * @author Simon Fischer
 * 
 */
public abstract class GeneralProcessListener implements ProcessListener {

	private Process process;

	public GeneralProcessListener(MainFrame mainFrame) {
		register(mainFrame);
	}

	public GeneralProcessListener() {
		this(null);
	}

	private void register(MainFrame mainFrame) {
		if (!RapidMiner.getExecutionMode().isHeadless()) {
			final ProcessEditor processEditor = new ProcessEditor() {

				@Override
				public void processChanged(Process process) {
					if (GeneralProcessListener.this.process != null) {
						GeneralProcessListener.this.process.getRootOperator().removeProcessListener(
								GeneralProcessListener.this);
					}
					GeneralProcessListener.this.process = process;
					if (GeneralProcessListener.this.process != null) {
						GeneralProcessListener.this.process.getRootOperator()
								.addProcessListener(GeneralProcessListener.this);
					}
				}

				@Override
				public void processUpdated(Process process) {}

				@Override
				public void setSelection(List<Operator> selection) {}

			};
			if (mainFrame == null) {
				RapidMinerGUI.getMainFrame().addProcessEditor(processEditor);
			} else {
				mainFrame.addProcessEditor(processEditor);
			}
		}
	}

}
