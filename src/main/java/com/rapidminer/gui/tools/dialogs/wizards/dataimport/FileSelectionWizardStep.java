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
package com.rapidminer.gui.tools.dialogs.wizards.dataimport;

import com.rapidminer.gui.tools.ExtendedJFileChooser;
import com.rapidminer.gui.tools.SwingTools;
import com.rapidminer.gui.tools.dialogs.wizards.AbstractWizard;
import com.rapidminer.gui.tools.dialogs.wizards.WizardStep;

import java.io.File;

import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileFilter;


/**
 * 
 * @author Tobias Malbrecht
 */
public class FileSelectionWizardStep extends WizardStep {

	protected final JFileChooser fileChooser;

	public FileSelectionWizardStep(AbstractWizard parent, FileFilter... fileFilters) {
		this(parent, (File) null, fileFilters);
		setDefaultFileFilter(fileChooser, fileFilters);
	}

	public FileSelectionWizardStep(AbstractWizard parent, File preselected, FileFilter... fileFilters) {
		super("select_file");
		this.fileChooser = SwingTools.createFileChooser("", null, false, fileFilters);
		if (preselected != null) {
			this.fileChooser.setSelectedFile(preselected);
		}
		this.fileChooser.setControlButtonsAreShown(false);
		setDefaultFileFilter(fileChooser, fileFilters);
		if (this.fileChooser instanceof ExtendedJFileChooser) {
			((ExtendedJFileChooser) fileChooser).addChangeListener(parent);
		}
	}

	private void setDefaultFileFilter(JFileChooser fileChooser, FileFilter... fileFilters) {
		if (fileFilters != null && fileFilters.length == 1) {
			// Select single file filter as default
			fileChooser.setFileFilter(fileFilters[0]);
			fileChooser.setAcceptAllFileFilterUsed(true);
		}
	}

	@Override
	protected boolean canGoBack() {
		return false;
	}

	@Override
	protected boolean canProceed() {
		if (fileChooser instanceof ExtendedJFileChooser) {
			File selectedFile = getSelectedFile();
			return selectedFile != null && selectedFile.exists();
		}
		return true;
	}

	@Override
	protected JComponent getComponent() {
		return fileChooser;
	}

	public File getSelectedFile() {
		return fileChooser.getSelectedFile();
	}
}
