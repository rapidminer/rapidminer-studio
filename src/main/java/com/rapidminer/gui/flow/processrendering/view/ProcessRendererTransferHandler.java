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
package com.rapidminer.gui.flow.processrendering.view;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;

import javax.swing.TransferHandler;

import com.rapidminer.Process;
import com.rapidminer.gui.RapidMinerGUI;
import com.rapidminer.gui.dnd.ReceivingOperatorTransferHandler;
import com.rapidminer.gui.flow.AutoWireThread;
import com.rapidminer.gui.flow.processrendering.annotations.model.AnnotationsModel;
import com.rapidminer.gui.flow.processrendering.annotations.model.ProcessAnnotation;
import com.rapidminer.gui.flow.processrendering.annotations.model.WorkflowAnnotation;
import com.rapidminer.gui.flow.processrendering.draw.ProcessDrawUtils;
import com.rapidminer.gui.flow.processrendering.draw.ProcessDrawer;
import com.rapidminer.gui.flow.processrendering.model.ProcessRendererModel;
import com.rapidminer.gui.processeditor.XMLEditor;
import com.rapidminer.operator.ExecutionUnit;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorChain;
import com.rapidminer.operator.ProcessRootOperator;
import com.rapidminer.operator.io.RepositorySource;
import com.rapidminer.tools.I18N;
import com.rapidminer.tools.LogService;
import com.rapidminer.tools.XMLException;


/**
 * The {@link TransferHandler} for the {@link ProcessRendererView}.
 *
 * @author Simon Fischer, Michael Knopf
 * @since 6.4.0
 *
 */
public class ProcessRendererTransferHandler extends ReceivingOperatorTransferHandler {

	private static final long serialVersionUID = 1L;

	/** the view instance */
	private final ProcessRendererView view;

	/** the model instance */
	private final ProcessRendererModel model;

	/** the controller instance */
	private final ProcessRendererController controller;

	/** the operator after which the dropped operator will be added */
	private Operator dropInsertionPredecessor;

	public ProcessRendererTransferHandler(ProcessRendererView view, ProcessRendererModel model,
			ProcessRendererController controller) {
		this.view = view;
		this.model = model;
		this.controller = controller;
	}

	@Override
	public boolean dropNow(final List<Operator> newOperators, Point loc) {
		model.setImportDragged(false);
		model.fireMiscChanged();
		controller.clearStatus();

		if (newOperators.isEmpty()) {
			return true;
		}
		view.requestFocusInWindow();

		// if we don't have a loc, we can use the mouse cursor
		if (loc == null) {
			if (model.getCurrentMousePosition() != null) {
				loc = new Point(model.getCurrentMousePosition());
			} else {
				Rectangle viewing = RapidMinerGUI.getMainFrame().getProcessPanel().getViewPort().getViewRect();
				loc = new Point((int) viewing.getCenterX(), (int) viewing.getCenterY());
			}
		}

		// determine process to drop to
		int processIndex = view.getProcessIndexUnder(loc.getLocation());
		try {
			if (processIndex != -1) {
				// we have a location for the drop/paste
				Operator firstOperator = newOperators.get(0);
				Point dest = view.toProcessSpace(loc, processIndex);

				// if we drop a single Retrieve operator on an inner source of the root
				// op, we immediately attach the repository location to the port.
				boolean isRoot = model.getDisplayedChain() instanceof ProcessRootOperator;
				boolean dropsSource = firstOperator instanceof RepositorySource;
				if (isRoot && dropsSource && newOperators.size() == 1) {
					if (controller.checkPortUnder(model.getProcess(processIndex).getInnerSources(), (int) dest.getX(),
							(int) dest.getY())) {
						String location = firstOperator.getParameters()
								.getParameterOrNull(RepositorySource.PARAMETER_REPOSITORY_ENTRY);
						int index = model.getHoveringPort().getPorts().getAllPorts().indexOf(model.getHoveringPort());
						model.getDisplayedChain().getProcess().getContext().setInputRepositoryLocation(index, location);
						return true;
					}
				}

				// calculate operator position
				Point anchor;
				// snap to grid
				if (model.isSnapToGrid()) {
					anchor = ProcessDrawUtils.snap(new Point2D.Double(dest.getX() - ProcessDrawer.OPERATOR_WIDTH / 2,
							dest.getY() - ProcessDrawer.OPERATOR_MIN_HEIGHT / 2));
				} else {
					anchor = new Point((int) dest.getX() - ProcessDrawer.OPERATOR_WIDTH / 2,
							(int) dest.getY() - ProcessDrawer.OPERATOR_MIN_HEIGHT / 2);
				}

				int index = view.getProcessIndexUnder(dest);
				if (index == -1) {
					index = 0;
				}
				// check if operator overlaps bottom process corner
				double lowestPosition = model.getProcessHeight(model.getProcess(index))
						- ProcessDrawer.GRID_AUTOARRANGE_HEIGHT * model.getZoomFactor();
				if (anchor.getY() > lowestPosition * 1 / model.getZoomFactor()) {
					anchor.setLocation(anchor.getX(), lowestPosition * 1 / model.getZoomFactor());
				}

				// check if operator overlaps right process corner
				double rightestPositon = model.getProcessWidth(model.getProcess(index))
						- ProcessDrawer.GRID_AUTOARRANGE_WIDTH * model.getZoomFactor();
				if (anchor.getX() > rightestPositon * 1 / model.getZoomFactor()) {
					anchor.setLocation(rightestPositon * 1 / model.getZoomFactor(), anchor.getY());
				}

				// check whether all operators have position data and compute upper
				// left anchor of the bounding box
				boolean completePositionData = true;
				double boundingBoxX = Double.MAX_VALUE;
				double boundingBoxY = Double.MAX_VALUE;
				double boundingBoxMaxX = 0;
				double boundingBoxMaxY = 0;
				for (Operator op : newOperators) {
					// check position data
					Rectangle2D rect = model.getOperatorRect(op);
					if (rect == null) {
						completePositionData = false;
						break;
					}
					// compare positions
					if (rect.getX() < boundingBoxX) {
						boundingBoxX = rect.getX();
					}
					if (rect.getY() < boundingBoxY) {
						boundingBoxY = rect.getY();
					}
					if (rect.getMaxX() > boundingBoxMaxX) {
						boundingBoxMaxX = rect.getMaxX();
					}
					if (rect.getMaxY() > boundingBoxMaxY) {
						boundingBoxMaxY = rect.getMaxY();
					}
				}

				if (completePositionData) {
					// adjust position relative to the computed anchor
					int dx = (int) (anchor.getX() - boundingBoxX);
					int dy = (int) (anchor.getY() - boundingBoxY);
					for (Operator op : newOperators) {
						Rectangle2D rect = model.getOperatorRect(op);
						Rectangle2D newRect = new java.awt.geom.Rectangle2D.Double(rect.getX() + dx, rect.getY() + dy,
								rect.getWidth(), rect.getHeight());
						model.setOperatorRect(op, newRect);
						model.fireOperatorMoved(op);
					}
					// update process size (if necessary)
					controller.ensureWidth(model.getProcess(processIndex), (int) boundingBoxMaxX + dx);
					controller.ensureHeight(model.getProcess(processIndex), (int) boundingBoxMaxY + dy);
				} else {
					// position first operator at the anchor and remove position data of
					// remaining operator (if any) to trigger auto positioning
					Rectangle2D anchorRect = new Rectangle2D.Double(anchor.getX(), anchor.getY(),
							ProcessDrawer.OPERATOR_WIDTH, ProcessDrawer.OPERATOR_MIN_HEIGHT);
					int opIndex = 1;
					for (Operator op : newOperators) {
						if (op == firstOperator) {
							model.setOperatorRect(op, anchorRect);
						} else {
							Rectangle2D newAnchor = new Rectangle2D.Double(anchorRect.getMinX(),
									anchorRect.getMinY()
									+ opIndex * (ProcessDrawer.GRID_Y_OFFSET + ProcessDrawer.OPERATOR_MIN_HEIGHT),
									anchorRect.getWidth(), anchorRect.getHeight());
							model.setOperatorRect(op, newAnchor);
							opIndex++;
						}
						model.fireOperatorMoved(op);
					}
				}

				// index at which the first operator is inserted
				int firstInsertionIndex;
				final boolean firstMustBeWired;
				// insert first operator. Possibly insert into connection
				if (model.getHoveringConnectionSource() != null
						&& ProcessDrawUtils.canOperatorBeInsertedIntoConnection(model, firstOperator)) {
					int predecessorIndex = model.getProcess(processIndex).getOperators()
							.indexOf(model.getHoveringConnectionSource().getPorts().getOwner().getOperator());
					if (predecessorIndex != -1) {
						firstInsertionIndex = predecessorIndex + 1;
					} else {
						// can happen if dropIntersectsOutputPort is an inner source
						firstInsertionIndex = getDropInsertionIndex(processIndex);
					}
					model.getProcess(processIndex).addOperator(firstOperator, firstInsertionIndex);
					controller.insertIntoHoveringConnection(firstOperator);
					firstMustBeWired = false;
				} else {
					firstInsertionIndex = getDropInsertionIndex(processIndex);
					model.getProcess(processIndex).addOperator(firstOperator, firstInsertionIndex);
					firstMustBeWired = true;
				}
				// insert the rest (1..n). First, insert, then wire
				for (int i = 1; i < newOperators.size(); i++) {
					Operator newOp = newOperators.get(i);
					model.getProcess(processIndex).addOperator(newOp, firstInsertionIndex + i);
				}
				AutoWireThread.autoWireInBackground(newOperators, firstMustBeWired);
				boolean first = true;
				for (Operator op : newOperators) {
					controller.selectOperator(op, first);
					first = false;
				}
				dropInsertionPredecessor = null;

				model.fireOperatorsMoved(newOperators);
				return true;
			} else {
				dropInsertionPredecessor = null;
				return false;
			}
		} catch (RuntimeException e) {
			LogService.getRoot().log(Level.WARNING, I18N.getMessage(LogService.getRoot().getResourceBundle(),
							"com.rapidminer.gui.flow.ProcessRenderer.error_during_drop", e), e);
			throw e;
		}
	}

	@Override
	protected boolean dropNow(WorkflowAnnotation anno, Point loc) {
		if (anno == null) {
			return false;
		}
		AnnotationsModel annoModel = RapidMinerGUI.getMainFrame().getProcessPanel().getAnnotationsHandler().getModel();

		// pasting an operator anno will always create a process anno
		if (loc == null) {
			loc = model.getMousePositionRelativeToProcess();
		}

		int index = model.getHoveringProcessIndex();
		ExecutionUnit targetProcess;
		if (index != -1) {
			targetProcess = model.getProcess(index);
		} else {
			targetProcess = anno.getProcess();
		}

		ProcessAnnotation proAnno = anno.createProcessAnnotation(targetProcess);
		// move to drop location
		Rectangle2D frame = proAnno.getLocation();
		Rectangle2D newFrame = new Rectangle2D.Double(loc.getX(), loc.getY(), frame.getWidth(), frame.getHeight());
		proAnno.setLocation(newFrame);
		annoModel.addProcessAnnotation(proAnno);

		return true;
	}

	@Override
	protected boolean isDropLocationOk(final List<Operator> newOperators, final Point loc) {
		if (!view.isEnabled()) {
			return false;
		}
		if (view.getProcessIndexUnder(loc) == -1) {
			return false;
		} else {
			for (Operator newOperator : newOperators) {
				if (newOperator instanceof OperatorChain) {
					if (model.getDisplayedChain() == newOperator
							|| ((OperatorChain) newOperator).getAllInnerOperators().contains(model.getDisplayedChain())) {
						return false;
					}
				}
			}
			return true;
		}
	}

	@Override
	protected void markDropOver(final Point dropPoint) {
		int pid = view.getProcessIndexUnder(dropPoint);
		if (pid != -1) {
			Point processSpace = view.toProcessSpace(dropPoint, pid);
			model.setHoveringConnectionSource(controller.getPortForConnectorNear(processSpace, model.getProcess(pid)));
		}
		model.fireMiscChanged();
	}

	@Override
	protected List<Operator> getDraggedOperators() {
		// if a workflow annotation is selected, it takes precedence (only selected if no actual
		// operator is selected, except for the displayed chain)
		AnnotationsModel annoModel = RapidMinerGUI.getMainFrame().getProcessPanel().getAnnotationsHandler().getModel();
		if (annoModel.getSelected() != null) {
			return Collections.<Operator> emptyList();
		}
		return model.getSelectedOperators();
	}

	@Override
	protected WorkflowAnnotation getDraggedAnnotation() {
		AnnotationsModel annoModel = RapidMinerGUI.getMainFrame().getProcessPanel().getAnnotationsHandler().getModel();
		return annoModel.getSelected();
	}

	@Override
	public boolean canImport(final TransferSupport ts) {
		if (ts.isDrop()) {
			int pid = view.getProcessIndexUnder(ts.getDropLocation().getDropPoint());
			if (pid < 0) {
				return false;
			}
		}

		boolean canImport = controller.canImportTransferable(ts.getTransferable());
		canImport &= super.canImport(ts);
		if (ts.isDrop() && model.isDropTargetSet() && !model.isDragStarted() && canImport && !model.isImportDragged()) {
			model.setImportDragged(true);
			model.fireMiscChanged();
		}
		return canImport;
	}

	@Override
	protected void dropEnds() {
		// this prevents wrong drag target message which can occur if the mouseExited event on the
		// operator tree is not triggered when using Java 7
		model.setOperatorSourceHovered(false);

		model.setImportDragged(false);
		model.fireMiscChanged();
	}

	@Override
	protected Process getProcess() {
		return model.getDisplayedChain().getProcess();
	}

	@Override
	protected boolean dropNow(String processXML) {
		((XMLEditor) RapidMinerGUI.getMainFrame().getXMLEditor()).setText(processXML);
		try {
			((XMLEditor) RapidMinerGUI.getMainFrame().getXMLEditor()).validateProcess();
		} catch (IOException | XMLException e) {
			LogService.getRoot().log(Level.WARNING, "com.rapidminer.gui.processeditor.XMLEditor.failed_to_parse_process");
			return false;
		}
		return true;
	}

	/**
	 * Returns the index at which an operator should be inserted. The operator is inserted after
	 * {@link #dropInsertionPredecessor} or as the last operator if
	 * {@link #dropInsertionPredecessor} is null.
	 */
	private int getDropInsertionIndex(final int processIndex) {
		if (dropInsertionPredecessor == null) {
			return model.getProcess(processIndex).getOperators().size();
		} else {
			return dropInsertionPredecessor.getExecutionUnit().getOperators().indexOf(dropInsertionPredecessor) + 1;
		}
	}
}
