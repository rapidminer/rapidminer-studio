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
package com.rapidminer.gui.actions.export;

import com.rapidminer.gui.actions.export.ImageExporter.ExportStatus;
import com.rapidminer.gui.tools.ResourceAction;
import com.rapidminer.gui.tools.SwingTools;

import java.awt.event.ActionEvent;


/**
 * The action uses {@link ImageExporter} to export a {@link PrintableComponent} as an image.
 * Override {@link #exportFinished()}, {@link #exportAborted()} or {@link #exportFinished()} for
 * individual behaviors in case of finish, abort or failure.
 * 
 * @author Nils Woehler
 * 
 */
public abstract class ExportImageAction extends ResourceAction {

	private static final long serialVersionUID = 5803992520098627274L;

	/**
	 * @param smallIcon
	 */
	public ExportImageAction(boolean smallIcon) {
		super(smallIcon, "export_image");
	}

	@Override
	public void loggedActionPerformed(ActionEvent event) {
		PrintableComponent comp = getPrintableComponent();
		try {
			ExportStatus exportResult = new ImageExporter(comp).exportImage();
			if (exportResult == ExportStatus.EXPORTED) {
				// export done, close dialog
				exportFinished();
			} else {
				exportAborted();
			}
		} catch (ImageExportException e) {
			exportFailed(e);
		}
	}

	/**
	 * @return the {@link PrintableComponent} that should be exported
	 */
	protected abstract PrintableComponent getPrintableComponent();

	/**
	 * Will be called after the export has finished successfully. Default operation: Do nothing.
	 */
	protected void exportFinished() {
		// NOOP
	}

	/**
	 * Will be called if the export has been aborted. Default operation: Do nothing.
	 */
	protected void exportAborted() {
		// NOOP
	}

	/**
	 * Will be called if the export has failed. Default operation: Show error dialog.
	 * 
	 * @param e
	 *            the exception that indicates the error
	 */
	protected void exportFailed(ImageExportException e) {
		SwingTools.showSimpleErrorMessage("export_failed", e);
	}

}
