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

import java.awt.GridLayout;

import javax.swing.JPanel;

import com.rapidminer.Process;
import com.rapidminer.ProcessContext;
import com.rapidminer.operator.ports.InputPorts;
import com.rapidminer.operator.ports.OutputPorts;


/**
 * <p>
 * An editor to editor {@link ProcessContext}s. This is used in the {@link RunRemoteDialog} and in
 * the {@link ProcessContextProcessEditor}.
 * </p>
 *
 * <p>
 * The editor keeps a reference to the edited {@link Process} and a {@link ProcessContext}.
 * Typically, the context will be the one provided by the process itself. Passing null to one of the
 * methods expecting a context will automatically use the one provided by the process. However, if
 * you want to edit a context that you want to superimpose on the processes context later, you can
 * pass in an alternative context.
 * </p>
 *
 * @author Simon Fischer, Tobias Malbrecht
 *
 */
public class ProcessContextEditor extends JPanel {

	private static final long serialVersionUID = 1L;

	private final RepositoryLocationsEditor<OutputPorts> inputEditor;
	private final RepositoryLocationsEditor<InputPorts> outputEditor;
	private final MacroEditor macroEditor;

	/**
	 * Constructs an editor for the given process and edited context. See class comment to find out
	 * why you can pass in a context here.
	 */
	public ProcessContextEditor(Process process, ProcessContext alternativeContext) {
		inputEditor = new RepositoryLocationsEditor<OutputPorts>(true, "context.input", "input");
		outputEditor = new RepositoryLocationsEditor<InputPorts>(false, "context.output", "result");
		macroEditor = new MacroEditor(true);

		setLayout(new GridLayout(3, 1));
		((GridLayout) getLayout()).setHgap(0);
		((GridLayout) getLayout()).setVgap(10);

		add(inputEditor);
		add(outputEditor);
		add(macroEditor);

		setProcess(process, alternativeContext);
	}

	/**
	 * Sets the process and edited context. See class comment to find out why you can pass in a
	 * context here.
	 */
	public void setProcess(Process process, ProcessContext context) {
		if (context == null) {
			context = process != null ? process.getContext() : null;
		}
		macroEditor.setContext(context);
		if (context != null) {
			inputEditor.setData(context, process, process.getRootOperator().getSubprocess(0).getInnerSources());
			outputEditor.setData(context, process, process.getRootOperator().getSubprocess(0).getInnerSinks());
		}
	}
}
