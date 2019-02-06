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
package com.rapidminer.operator.ports.quickfix;

import com.rapidminer.Process;
import com.rapidminer.gui.actions.SaveAsAction;


/**
 * Quick fix to save a process.
 *
 * @author Marco Boeck
 * @since 8.2
 */
public class SaveProcessQuickFix extends AbstractQuickFix {

	private Process process;

	/**
	 * This constructor will build a quickfix that let's the user save the given process.
	 */
	public SaveProcessQuickFix(Process process) {
		this(process, "save_process", (Object[]) null);
	}

	/**
	 * This constructor will build a quickfix that will automatically set the parameter to the given
	 * value without further user interaction. Use this constructor if you can comprehend the
	 * correct value.
	 */
	private SaveProcessQuickFix(Process process, String i18nKey, Object... i18nArgs) {
		super(1, true, i18nKey, i18nArgs);
		this.process = process;
	}

	@Override
	public void apply() {
		SaveAsAction.saveAs(process, true);
	}
}
