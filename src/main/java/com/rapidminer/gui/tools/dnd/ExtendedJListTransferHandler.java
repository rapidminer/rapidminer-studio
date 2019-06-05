/**
 * Copyright (C) 2001-2019 by RapidMiner and the contributors
 *
 * Complete list of developers available at our web site:
 *
 * http://rapidminer.com
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General
 * Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any
 * later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License for more
 * details.
 *
 * You should have received a copy of the GNU Affero General Public License along with this program. If not, see
 * http://www.gnu.org/licenses/.
 */
package com.rapidminer.gui.tools.dnd;

import java.awt.Component;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.DragSource;
import java.io.IOException;
import java.util.Objects;
import java.util.logging.Level;
import javax.swing.DefaultListModel;
import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.TransferHandler;

import com.rapidminer.tools.LogService;


/**
 * Typed drag & drop handler including a callback option.
 *
 * <p>
 * Example usage to allow re-ordering elements in a list for XY elements: <br/>
 * <ol>
 * <li>{@code list.setTransferHandler(new ExtendedJListTransferHandler<>(XY.class, DnDConstants.ACTION_MOVE,
 * this::updateStructure));}</li>
 * <li>{@code list.setDropMode(DropMode.INSERT);}</li>
 * <li>{@code list.setDragEnabled(true);}</li>
 * </ol>
 * </p>
 *
 * @param <T>
 * 		the transferable type (i.e. the type of what is dragged around)
 * @author Marco Boeck
 * @since 9.2.0
 */
public class ExtendedJListTransferHandler<T> extends TransferHandler {

	private final int dndConstant;
	private final DataFlavor localObjectFlavor;
	private int[] indices;
	private int addIndex = -1;
	private int addCount;

	private Runnable callback;


	/**
	 * Creates a new transfer handler instance.
	 *
	 * @param dataFlavorClass
	 * 		the accepted data flavor class
	 * @param dndConstant
	 * 		whether copy/move/copyAndMove should be allowed, see {@link java.awt.dnd.DnDConstants}
	 * @param callback
	 * 		an optional callback function which is called when the drop has successfully finished
	 */
	public ExtendedJListTransferHandler(Class<T> dataFlavorClass, int dndConstant, Runnable callback) {
		super();
		this.dndConstant = dndConstant;
		this.localObjectFlavor = new DataFlavor(dataFlavorClass, "Array of items");
		this.callback = callback;
	}

	@Override
	@SuppressWarnings("unchecked")
	protected Transferable createTransferable(JComponent c) {
		JList<? super T> source = (JList<? super T>) c;
		c.getRootPane().getGlassPane().setVisible(true);

		indices = source.getSelectedIndices();
		Object[] transferedObjects = source.getSelectedValuesList().toArray(new Object[0]);
		return new Transferable() {

			@Override
			public DataFlavor[] getTransferDataFlavors() {
				return new DataFlavor[]{
						localObjectFlavor
				};
			}

			@Override
			public boolean isDataFlavorSupported(DataFlavor flavor) {
				return Objects.equals(localObjectFlavor, flavor);
			}

			@Override
			public Object getTransferData(DataFlavor flavor)
					throws UnsupportedFlavorException {
				if (isDataFlavorSupported(flavor)) {
					return transferedObjects;
				} else {
					throw new UnsupportedFlavorException(flavor);
				}
			}
		};
	}

	@Override
	public boolean canImport(TransferSupport info) {
		return info.isDrop() && info.isDataFlavorSupported(localObjectFlavor);
	}

	@Override
	public int getSourceActions(JComponent c) {
		Component glassPane = c.getRootPane().getGlassPane();
		glassPane.setCursor(DragSource.DefaultMoveDrop);
		return dndConstant;
	}

	@SuppressWarnings("unchecked")
	@Override
	public boolean importData(TransferSupport info) {
		TransferHandler.DropLocation dropLocation = info.getDropLocation();
		if (!canImport(info) || !(dropLocation instanceof JList.DropLocation)) {
			return false;
		}

		JList.DropLocation dl = (JList.DropLocation) dropLocation;
		JList target = (JList) info.getComponent();
		DefaultListModel listModel = (DefaultListModel) target.getModel();
		int max = listModel.getSize();
		int index = dl.getIndex();
		index = index < 0 ? max : index;
		// make sure to append at the end if index > size
		index = Math.min(index, max);

		addIndex = index;

		try {
			Object[] values = (Object[]) info.getTransferable().getTransferData(localObjectFlavor);
			for (Object value : values) {
				int idx = index++;
				listModel.add(idx, value);
				target.addSelectionInterval(idx, idx);
			}
			addCount = values.length;
			return true;
		} catch (UnsupportedFlavorException | IOException e) {
			// should never happen, log anyway to be safe
			LogService.getRoot().log(Level.WARNING, "com.rapidminer.gui.tools.dnd.ExtendedJListTransferHandler.unexpected_error", e);
		}

		return false;
	}

	@Override
	protected void exportDone(JComponent c, Transferable data, int action) {
		c.getRootPane().getGlassPane().setVisible(false);
		cleanup(c, action == MOVE);
		if ( callback != null) {
			callback.run();
		}
	}

	/**
	 * Make sure indices are correct after a move in the list.
	 */
	private void cleanup(JComponent c, boolean remove) {
		if (remove && Objects.nonNull(indices)) {
			if (addCount > 0) {
				// https://github.com/aterai/java-swing-tips/blob/master/DragSelectDropReordering/src/java/example/MainPanel.java
				for (int i = 0; i < indices.length; i++) {
					if (indices[i] >= addIndex) {
						indices[i] += addCount;
					}
				}
			}
			JList source = (JList) c;
			DefaultListModel model = (DefaultListModel) source.getModel();
			for (int i = indices.length - 1; i >= 0; i--) {
				model.remove(indices[i]);
			}
		}

		indices = null;
		addCount = 0;
		addIndex = -1;
	}
}
