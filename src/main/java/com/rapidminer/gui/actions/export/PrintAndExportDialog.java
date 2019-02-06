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

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.print.PageFormat;
import java.util.LinkedList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;

import com.rapidminer.gui.ApplicationFrame;
import com.rapidminer.gui.actions.PrintAction;
import com.rapidminer.gui.tools.PrintingTools;
import com.rapidminer.gui.tools.ResourceActionAdapter;
import com.rapidminer.gui.tools.components.ButtonBarCardPanel;
import com.rapidminer.gui.tools.components.PrintableComponentCard;
import com.rapidminer.gui.tools.dialogs.ButtonDialog;


/**
 * A dialog which is shown if the {@link ShowPrintAndExportDialogAction} is executed.
 *
 * @author Nils Woehler
 *
 */
public class PrintAndExportDialog extends ButtonDialog {

	private static final long serialVersionUID = 1L;

	private ButtonBarCardPanel buttonBarCardPanel;

	private final List<PrintableComponent> components;

	private final List<PrintPreviewPanel> previewPanels;

	private PageFormat pageFormat = PrintingTools.getPageFormat();

	public PrintAndExportDialog(List<PrintableComponent> components) {
		super(ApplicationFrame.getApplicationFrame(), "export_and_print", ModalityType.APPLICATION_MODAL, new Object[] {});
		this.components = components;
		this.previewPanels = new LinkedList<>();
		layoutDefault(createPreviewContent(), createButtonPanel(), ButtonDialog.LARGE);
		setResizable(false);
	}

	@Override
	protected void ok() {
		PrintableComponent comp = getSelectedPrintableComponent();
		PrintAction printAction = new PrintAction(comp.getExportComponent(), comp.getExportName());
		printAction.actionPerformed(null);
		if (!printAction.wasCanceled()) {
			dispose();
		}
	}

	/**
	 * @return the currently selected printable component which is extracted from the currently
	 *         selected card.
	 */
	private PrintableComponent getSelectedPrintableComponent() {
		PrintableComponentCard selectedCard = (PrintableComponentCard) buttonBarCardPanel.getSelectedCard();
		return selectedCard.getPrintableComponent();

	}

	private JPanel createButtonPanel() {
		JPanel buttonPanel = new JPanel(new BorderLayout());

		JPanel leftSidePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, ButtonDialog.GAP, ButtonDialog.GAP));

		leftSidePanel.add(new JButton(new ExportImageAction(false) {

			private static final long serialVersionUID = -7451010324095048462L;

			@Override
			protected PrintableComponent getPrintableComponent() {
				return getSelectedPrintableComponent();
			}

			@Override
			protected void exportFinished() {
				cancel();
			}
		}));
		buttonPanel.add(leftSidePanel, BorderLayout.WEST);

		JPanel rightSidePanel = new JPanel(new FlowLayout(FlowLayout.CENTER, ButtonDialog.GAP, ButtonDialog.GAP));

		rightSidePanel.add(new JButton(new ResourceActionAdapter("page_setup") {

			private static final long serialVersionUID = 1L;

			@Override
			public void loggedActionPerformed(ActionEvent e) {
				PageFormat newPageFormat = PrintingTools.getPrinterJob().pageDialog(pageFormat);
				for (PrintPreviewPanel p : previewPanels) {
					p.setPageFormat(newPageFormat);
				}
				pageFormat = newPageFormat;
			}

		}));
		rightSidePanel.add(makeOkButton("print_focused"));
		rightSidePanel.add(makeCancelButton("close"));

		buttonPanel.add(rightSidePanel, BorderLayout.EAST);

		return buttonPanel;
	}

	/**
	 * Creates the middle button bar card panel.
	 */
	private JComponent createPreviewContent() {
		buttonBarCardPanel = new ButtonBarCardPanel();
		buttonBarCardPanel.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY, 1));

		int showingIndex = -1;
		int index = 0;

		// add printable components as cards to button bar card panel
		for (PrintableComponent comp : components) {
			if (comp.getExportComponent() != null) {
				PrintPreviewPanel previewPanel = new PrintPreviewPanel(comp, pageFormat);
				buttonBarCardPanel.addCard(new PrintableComponentCard(comp), previewPanel);
				previewPanels.add(previewPanel);
				if (showingIndex == -1 && comp.isShowing()) {
					showingIndex = index;
				}
				++index;
			}
		}
		if (showingIndex != -1 && showingIndex <= index) {
			buttonBarCardPanel.setSelectedCard(showingIndex);
		}
		return buttonBarCardPanel;
	}
}
