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
package com.rapidminer.gui.flow.processrendering.annotations;

import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;

import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.TransferHandler;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;

import com.rapidminer.gui.flow.processrendering.annotations.model.WorkflowAnnotation;


/**
 * This transfer handler only returns plain text strings as transferable data, regardless of the
 * transferable type. It converts for example an HTML transferable to a String transferable for
 * {@link WorkflowAnnotation}s.
 *
 * @author Marco Boeck
 * @since 6.4.0
 *
 */
final class TransferHandlerAnnotationPlaintext extends TransferHandler {

	/**
	 * Simple transferable which only contains a string flavor.
	 */
	private static class StringTransferable implements Transferable {

		private String text;

		private StringTransferable(final String text) {
			this.text = text;
		}

		@Override
		public boolean isDataFlavorSupported(DataFlavor flavor) {
			return DataFlavor.stringFlavor.equals(flavor);
		}

		@Override
		public DataFlavor[] getTransferDataFlavors() {
			return new DataFlavor[] { DataFlavor.stringFlavor };
		}

		@Override
		public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException, IOException {
			return text;
		}
	}

	private static final long serialVersionUID = 1L;

	/** original transfer handler to which some calls are delegated */
	private TransferHandler original;

	/** the editor for which the handler is */
	private JEditorPane editor;

	/**
	 * Creates a new plain text transfer handler for the annotation editor.
	 *
	 * @param editor
	 *            the editor instance
	 */
	public TransferHandlerAnnotationPlaintext(final JEditorPane editor) {
		if (editor == null) {
			throw new IllegalArgumentException("editor must not be null!");
		}
		this.editor = editor;
		this.original = editor.getTransferHandler();
	}

	@Override
	public boolean canImport(JComponent comp, DataFlavor[] transferFlavors) {
		return original.canImport(comp, transferFlavors);
	}

	@Override
	public boolean canImport(TransferSupport ts) {
		return original.canImport(ts);
	}

	@Override
	public void exportToClipboard(JComponent comp, Clipboard clip, int action) throws IllegalStateException {
		if (action == COPY || action == MOVE) {
			try {
				String text = AnnotationDrawUtils.getPlaintextFromEditor(editor, true);
				Transferable t = new StringTransferable(text);
				try {
					clip.setContents(t, null);
					exportDone(comp, t, action);
					return;
				} catch (IllegalStateException ise) {
					exportDone(comp, t, NONE);
					throw ise;
				}
			} catch (IndexOutOfBoundsException | IOException | BadLocationException e1) {
				// should not happen
				exportDone(comp, null, NONE);
			}
		}
		exportDone(comp, null, NONE);
	}

	@Override
	protected void exportDone(JComponent source, Transferable data, int action) {
		if (action == MOVE) {
			try {
				Document doc = editor.getDocument();
				doc.remove(editor.getSelectionStart(), editor.getSelectionEnd() - editor.getSelectionStart());
			} catch (BadLocationException e) {
				// ignore
			}
		}
	}

	@Override
	public boolean importData(JComponent comp, Transferable t) {
		return original.importData(comp, t);
	}

	@Override
	public boolean importData(TransferSupport support) {
		try {
			String text = String.valueOf(support.getTransferable().getTransferData(DataFlavor.stringFlavor)).replaceFirst(
					"\\s*", "");
			return original.importData(new TransferSupport(support.getComponent(), new StringTransferable(text)));
		} catch (UnsupportedFlavorException | IOException e) {
			return false;
		}
	}
}
