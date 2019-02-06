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
package com.rapidminer.gui.tools;

import java.awt.Component;
import java.awt.Container;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import javax.print.attribute.HashPrintRequestAttributeSet;
import javax.print.attribute.PrintRequestAttributeSet;
import javax.swing.JMenuItem;

import com.rapidminer.gui.Perspective;
import com.rapidminer.gui.RapidMinerGUI;
import com.rapidminer.gui.actions.export.PrintableComponent;
import com.rapidminer.gui.actions.export.PrintableComponentContainer;
import com.rapidminer.gui.actions.export.ShowPrintAndExportDialogAction;
import com.rapidminer.gui.actions.export.SimplePrintableComponent;
import com.rapidminer.gui.tools.dialogs.ConfirmDialog;
import com.rapidminer.tools.I18N;
import com.vlsolutions.swing.docking.Dockable;
import com.vlsolutions.swing.docking.DockableState;


/**
 * This class has static references to a printer job and page format. It also serves as a factory
 * for printer menus.
 *
 * @author Simon Fischer
 *
 */
public class PrintingTools {

	private static final PrinterJob PRINTER_JOB = PrinterJob.getPrinterJob();
	private static PageFormat pageFormat = getPrinterJob().defaultPage();
	private static PrintRequestAttributeSet printSettings = new HashPrintRequestAttributeSet();

	public static PrinterJob getPrinterJob() {
		return PRINTER_JOB;
	}

	public static PageFormat getPageFormat() {
		return pageFormat;
	}

	public static void editPrintSettings() {
		getPrinterJob().pageDialog(printSettings);
	}

	public static PrintRequestAttributeSet getPrintSettings() {
		return printSettings;
	}

	/**
	 * @return <code>true</code> on success. <code>false</code> if user aborts printing.
	 */
	public static boolean print(Printable printable) throws PrinterException {
		getPrinterJob().setPrintable(printable);
		if (getPrinterJob().printDialog()) {
			PrintingTools.getPrinterJob().print(printSettings);
			return true;
		} else {
			return false;
		}
	}

	/**
	 * @return <code>true</code> on success. <code>false</code> if user aborts printing.
	 */
	public static boolean print(Printable printable, PrintRequestAttributeSet printSettings) throws PrinterException {
		getPrinterJob().setPrintable(printable);
		if (getPrinterJob().printDialog(printSettings)) {
			PrintingTools.getPrinterJob().print(printSettings);
			return true;
		} else {
			return false;
		}
	}

	/**
	 * Creates a export menu item for the provided component.
	 *
	 * @param component
	 *            the component the menu should be created for
	 * @param componentName
	 *            the name of the component
	 */
	public static JMenuItem makeExportPrintMenu(Component component, String componentName) {
		JMenuItem menuItem = new JMenuItem(
				new ShowPrintAndExportDialogAction(new SimplePrintableComponent(component, componentName), true));
		return menuItem;
	}

	/**
	 * Prompt a file chooser dialog where the user selects a file location and returns a
	 * {@link File} at this location.
	 *
	 * @param i18nKey
	 *            the i18n key for the dialog to be shown. The provided i18nKey must be contained in
	 *            the GUI properties file (gui.dialog.i18nKey.[title|message|icon]).
	 * @param fileExtension
	 *            the explicit file extension like "pdf" or "png".
	 * @param extensionDescription
	 *            the description of the given format for this file extension
	 * @return the new File in the an object can be stored
	 * @throws IOException
	 */
	static public File promptForFileLocation(String i18nKey, String fileExtension, String extensionDescription)
			throws IOException {

		// check parameters
		if ("".equals(fileExtension) || "".equals(extensionDescription)) {
			throw new IllegalArgumentException("Empty file extension or exntension description are not allowed!");
		}
		if (fileExtension.startsWith(".")) {
			fileExtension = fileExtension.substring(1);
		}

		return promptForFileLocation(i18nKey, new String[] { fileExtension }, new String[] { extensionDescription });
	}

	/**
	 * Prompt a file chooser dialog where the user selects a file location and returns a
	 * {@link File} at this location.
	 *
	 * @param i18nKey
	 *            the i18n key for the dialog to be shown. The provided i18nKey must be contained in
	 *            the GUI properties file (gui.dialog.i18nKey.[title|message|icon]).
	 * @param fileExtensions
	 *            a list of explicit file extension like "pdf" or "png".
	 * @param extensionDescriptions
	 *            a list of descriptions of the given format for this file extension
	 * @return the new File in the an object can be stored
	 * @throws IOException
	 */
	static public File promptForFileLocation(String i18nKey, String[] fileExtensions, String[] extensionDescriptions)
			throws IOException {

		// check parameters
		for (int i = 0; i < fileExtensions.length; ++i) {
			if ("".equals(fileExtensions[i]) || "".equals(extensionDescriptions[i])) {
				throw new IllegalArgumentException("Empty file extension or exntension description are not allowed!");
			}
			if (fileExtensions[i].startsWith(".")) {
				fileExtensions[i] = fileExtensions[i].substring(1);
			}
		}

		// prompt user for file location
		File file = SwingTools.chooseFile(RapidMinerGUI.getMainFrame(), i18nKey, null, false, false, fileExtensions,
				extensionDescriptions, false);
		if (file == null) {
			return null;
		}

		// do not overwrite directories
		if (file.isDirectory()) {
			throw new IOException(I18N.getMessage(I18N.getErrorBundle(), "error.io.file_is_directory", file.getPath()));
		}

		// prompt for overwrite confirmation
		if (file.exists()) {
			int returnVal = SwingTools.showConfirmDialog("export_image", ConfirmDialog.YES_NO_OPTION, file.getName());
			if (returnVal == ConfirmDialog.NO_OPTION) {
				return null;
			}
		}
		return file;
	}

	/**
	 * Returns a list of printable components below the root component. If the root component is a
	 * {@link PrintableComponent} itself, it will be first in the list. The first component in list
	 * is always showing. Other might not be visible on screen.
	 *
	 * @return the components that can be exported as an image.
	 */
	public static final List<PrintableComponent> findExportComponents(Component root) {
		List<PrintableComponent> components = new LinkedList<>();
		findExportComponents(root, components);
		return components;
	}

	/**
	 * Returns a list of printable components from the currently visible perspective.
	 *
	 * @return the components that can be exported as an image.
	 */
	public static final List<PrintableComponent> findExportComponents() {
		List<PrintableComponent> components = new LinkedList<>();

		// otherwise search for all PrintableComponents in current perspective
		for (DockableState state : RapidMinerGUI.getMainFrame().getDockingDesktop().getDockables()) {
			if (state.isHidden()) {
				continue;
			}
			Dockable dockable = state.getDockable();

			// if dockable is visible, search for PrintableComponents
			Component component = dockable.getComponent();
			if (component.isShowing()) {
				findExportComponents(component, components);
			}
		}

		// at last add MainFrame as printable component
		Perspective currentPerspective = RapidMinerGUI.getMainFrame().getPerspectiveController().getModel()
				.getSelectedPerspective();
		String perspectiveName = I18N.getGUIMessage("gui.action.workspace_" + currentPerspective.getName() + ".label");

		components.add(new SimplePrintableComponent(RapidMinerGUI.getMainFrame(), perspectiveName,
				I18N.getGUIMessage("gui.action.workspace_" + currentPerspective.getName() + ".icon")));

		return components;
	}

	/**
	 * Recursive method: Traverses down the component hierarchy until a instance of
	 * {@link PrintableComponent} is found. After adding this instance to the list of result
	 * components the method returns. If component is not a {@link PrintableComponent} but a
	 * {@link Container} this method is invoked on all child components.
	 */
	private static final void findExportComponents(Component component, List<PrintableComponent> resultComponents) {

		// if printable component found, add export component to list and return
		if (component instanceof PrintableComponent) {
			PrintableComponent printableComp = (PrintableComponent) component;
			if (printableComp.getExportComponent() != null) {
				resultComponents.add(printableComp);
			}

			// do not search any further in tree if printable component was found
			return;
		}

		// if pageable component found, add printable components to list and return
		if (component instanceof PrintableComponentContainer) {
			PrintableComponentContainer pageable = (PrintableComponentContainer) component;
			List<PrintableComponent> printableComponents = pageable.getPrintableComponents();
			for (PrintableComponent printable : printableComponents) {
				if (printable.getExportComponent() != null) {
					resultComponents.add(printable);
				}
			}

			// do not search any further in tree if pageable component was found
			return;
		}

		// if container found, dig deeper
		if (component instanceof Container) {
			Container container = (Container) component;
			for (Component comp : container.getComponents()) {
				findExportComponents(comp, resultComponents);
			}
		}
	}

}
