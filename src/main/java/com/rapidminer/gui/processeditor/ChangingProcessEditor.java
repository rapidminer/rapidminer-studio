/**
 * Copyright (C) 2001-2020 by RapidMiner and the contributors
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

import java.util.List;

import com.rapidminer.Process;
import com.rapidminer.operator.Operator;


/**
 * An abstract {@link ExtendedProcessEditor} that reacts only if the current process was changed to a new one.
 * Subclasses must implement {@link #doWork(Process, Process)}.
 *
 * @author Jan Czogalla
 * @since 9.6
 */
public abstract class ChangingProcessEditor implements ExtendedProcessEditor {

	private Process currentProcess;

	@Override
	public void processViewChanged(Process process){
		//noop
	}

	@Override
	public final void processChanged(Process process){
		update(process);
	}

	@Override
	public void setSelection(List<Operator> selection){
		//noop
	}

	@Override
	public final void processUpdated(Process process){
		update(process);
	}

	/** Updates the state and triggers {@link #doWork(Process, Process)} of the given process is not the current process */
	private void update(Process process) {
		if (currentProcess == process) {
			return;
		}
		doWork(currentProcess, process);
		currentProcess = process;
	}

	/**
	 * Handles the transition from the current process to the new process.
	 *
	 * @param currentProcess
	 * 		the current/old process; might be {@code null}
	 * @param newProcess
	 * 		the new process; might be {@code null}
	 */
	protected abstract void doWork(Process currentProcess, Process newProcess);
}
