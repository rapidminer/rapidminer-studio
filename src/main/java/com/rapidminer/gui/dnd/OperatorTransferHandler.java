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
package com.rapidminer.gui.dnd;

import java.awt.datatransfer.Transferable;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.Icon;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JMenu;
import javax.swing.JPopupMenu;
import javax.swing.KeyStroke;
import javax.swing.TransferHandler;

import com.rapidminer.gui.RapidMinerGUI;
import com.rapidminer.gui.flow.processrendering.annotations.model.WorkflowAnnotation;
import com.rapidminer.gui.operatortree.actions.CutCopyPasteDeleteAction;
import com.rapidminer.gui.operatortree.actions.DeleteOperatorAction;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.ProcessRootOperator;


/**
 * Transfer handler that supports dragging operators and workflow annotations.
 *
 * @author Simon Fischer, Marco Boeck
 *
 */
public abstract class OperatorTransferHandler extends AbstractPatchedTransferHandler {

	private static final long serialVersionUID = 1L;

	/** Returns a list of operators selected for dragging out of this component. */
	protected abstract List<Operator> getDraggedOperators();

	/**
	 * Returns the copied/cut workflow annotation.
	 *
	 * @return the annotation or {@code null}
	 */
	protected WorkflowAnnotation getDraggedAnnotation() {
		return null;
	}

	// General

	@Override
	public Icon getVisualRepresentation(Transferable transferable) {
		return null;
	}

	// Drag Support

	@Override
	public int getSourceActions(JComponent c) {
		return COPY_OR_MOVE;
	}

	@Override
	public Transferable createTransferable(JComponent c) {
		// first, try operators
		List<Operator> operators = getDraggedOperators();
		if (operators != null && !operators.isEmpty()) {
			// remove children
			Iterator<Operator> i = operators.iterator();
			while (i.hasNext()) {
				Operator op = i.next();
				Operator parent = op.getParent();
				while (parent != null) {
					if (operators.contains(parent)) {
						i.remove();
						continue;
					}
					parent = parent.getParent();
				}
			}
			// prevent copying of the process root operator as it only results in error messages
			// when pasting anyway
			if (operators.size() == 1 && operators.get(0) instanceof ProcessRootOperator) {
				return null;
			}
			return new TransferableOperator(operators.toArray(new Operator[operators.size()]));
		}
		// no operators, try workflow annotations
		WorkflowAnnotation anno = getDraggedAnnotation();
		// neither operators nor annotations, give up
		if (anno == null) {
			return null;
		}

		return new TransferableAnnotation(anno);
	}

	@Override
	protected void exportDone(JComponent source, Transferable data, int action) {
		if (data instanceof TransferableOperator) {
			switch (action) {
				case MOVE:
					TransferableOperator top = (TransferableOperator) data;
					Operator parent = null;
					for (Operator operator : top.getOperators()) {
						if (parent == null) {
							parent = operator.getParent();
						}
						operator.removeAndKeepConnections(Arrays.asList(top.getOperators()));
					}
					if (parent != null) {
						RapidMinerGUI.getMainFrame().selectOperator(parent);
					}
					break;
				default:
					// do nothing
			}
		}
		if (data instanceof TransferableAnnotation) {
			switch (action) {
				case MOVE:
					TransferableAnnotation anno = (TransferableAnnotation) data;
					RapidMinerGUI.getMainFrame().getProcessPanel().getAnnotationsHandler().getModel()
							.deleteAnnotation(anno.getAnnotation());
					break;
				default:
					// do nothing
			}
		}
		super.exportDone(source, data, action);
	}

	public static void installMenuItems(JPopupMenu editmenu) {
		installMenuItems(editmenu, false);
	}

	public static void installMenuItems(JPopupMenu editmenu, boolean enableCutCopy) {
		if (enableCutCopy) {
			editmenu.add(CutCopyPasteDeleteAction.CUT_ACTION);
			editmenu.add(CutCopyPasteDeleteAction.COPY_ACTION);
			editmenu.add(CutCopyPasteDeleteAction.PASTE_ACTION);
			editmenu.add(CutCopyPasteDeleteAction.DELETE_ACTION);
		} else {
			editmenu.add(CutCopyPasteDeleteAction.CUT_ACTION).setEnabled(false);
			editmenu.add(CutCopyPasteDeleteAction.COPY_ACTION).setEnabled(false);
			editmenu.add(CutCopyPasteDeleteAction.PASTE_ACTION);
			editmenu.add(CutCopyPasteDeleteAction.DELETE_ACTION).setEnabled(false);
		}
	}

	public static void installMenuItems(JMenu editmenu) {
		editmenu.add(CutCopyPasteDeleteAction.CUT_ACTION);
		editmenu.add(CutCopyPasteDeleteAction.COPY_ACTION);
		editmenu.add(CutCopyPasteDeleteAction.PASTE_ACTION);
		editmenu.add(CutCopyPasteDeleteAction.DELETE_ACTION);
	}

	public static void addToActionMap(JComponent component) {
		ActionMap actionMap = component.getActionMap();
		actionMap.put(TransferHandler.getCutAction().getValue(Action.NAME), TransferHandler.getCutAction());
		actionMap.put(TransferHandler.getCopyAction().getValue(Action.NAME), TransferHandler.getCopyAction());
		actionMap.put(TransferHandler.getPasteAction().getValue(Action.NAME), TransferHandler.getPasteAction());
		actionMap.put(DeleteOperatorAction.getActionName(), new DeleteOperatorAction());

		// only required if you have not set the menu accelerators
		InputMap inputMap = component.getInputMap();
		inputMap.put(KeyStroke.getKeyStroke("ctrl X"), TransferHandler.getCutAction().getValue(Action.NAME));
		inputMap.put(KeyStroke.getKeyStroke("ctrl C"), TransferHandler.getCopyAction().getValue(Action.NAME));
		inputMap.put(KeyStroke.getKeyStroke("ctrl V"), TransferHandler.getPasteAction().getValue(Action.NAME));
		inputMap.put(KeyStroke.getKeyStroke("del"), DeleteOperatorAction.getActionName());
	}
}
