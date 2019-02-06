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
package com.rapidminer.gui;

import java.util.List;

import com.rapidminer.Process;
import com.rapidminer.ProcessListener;
import com.rapidminer.RapidMiner;
import com.rapidminer.gui.flow.processrendering.model.ProcessRendererModel;
import com.rapidminer.gui.processeditor.ProcessEditor;
import com.rapidminer.operator.Operator;


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

	public GeneralProcessListener(ProcessRendererModel processModel) {
		register(processModel);
	}

	public GeneralProcessListener() {
		this((MainFrame) null);
	}

	private void register(Object processProvider) {
		if (!RapidMiner.getExecutionMode().isHeadless()) {
			final ProcessEditor processEditor = new ProcessEditor() {

				@Override
				public void processChanged(Process process) {
					if (GeneralProcessListener.this.process != null) {
						GeneralProcessListener.this.process.getRootOperator()
								.removeProcessListener(GeneralProcessListener.this);
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
			if (processProvider == null) {
				processProvider = RapidMinerGUI.getMainFrame().getProcessPanel().getProcessRenderer().getModel();
			}
			ProcessRendererModel processModel;
			if (processProvider instanceof MainFrame) {
				processModel = ((MainFrame) processProvider).getProcessPanel().getProcessRenderer().getModel();
			} else {
				processModel = (ProcessRendererModel) processProvider;
			}
			processModel.addProcessEditor(processEditor);
		}
	}

}
