/**
 * Copyright (C) 2001-2015 by RapidMiner and the contributors
 *
 * Complete list of developers available at our web site:
 *
 *      http://rapidminer.com
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses/.
 */
package com.rapidminer.template.gui;

import com.rapidminer.example.ExampleSet;
import com.rapidminer.gui.RapidMinerGUI;
import com.rapidminer.gui.tools.ResourceAction;
import com.rapidminer.gui.tools.SwingTools;
import com.rapidminer.operator.OperatorCreationException;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.io.ExcelExampleSetWriter;
import com.rapidminer.template.TemplateState;
import com.rapidminer.tools.OperatorService;

import java.awt.event.ActionEvent;
import java.io.File;


/**
 * Exports a table displayed in the {@link ResultsDashboard} as an Excel file using a
 * {@link ExcelExampleSetWriter}.
 * 
 * @author Simon Fischer
 * 
 */
public class ExportExcelAction extends ResourceAction {

	private static final long serialVersionUID = 1L;
	private int index;
	private TemplateState model;

	public ExportExcelAction(TemplateState model, int index) {
		super("template.export_excel");
		this.model = model;
		this.index = index;
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		final File file = SwingTools.chooseFile(RapidMinerGUI.getMainFrame(), "import_process", null, true, false,
				new String[] { "xls" }, new String[] { "Excel spreadsheet" });
		if (file == null) {
			return;
		}
		try {
			ExcelExampleSetWriter writer = OperatorService.createOperator(ExcelExampleSetWriter.class);
			writer.setParameter(ExcelExampleSetWriter.PARAMETER_EXCEL_FILE, file.getAbsolutePath());
			writer.write((ExampleSet) model.getResults()[index - 1]);
		} catch (OperatorCreationException | OperatorException e1) {
			SwingTools.showSimpleErrorMessage("template.error_exportring_xls", e1, file.getAbsolutePath(), e1.getMessage());
		}
	}
}
