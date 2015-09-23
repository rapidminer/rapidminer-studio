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
package com.rapidminer.gui.dnd;

import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

import javax.swing.Timer;
import javax.swing.tree.TreePath;

import com.rapidminer.Process;
import com.rapidminer.gui.RapidMinerGUI;
import com.rapidminer.gui.flow.processrendering.annotations.model.WorkflowAnnotation;
import com.rapidminer.gui.operatortree.OperatorTree;
import com.rapidminer.gui.operatortree.ProcessTreeModel;
import com.rapidminer.operator.ExecutionUnit;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorChain;
import com.rapidminer.operator.ports.metadata.CompatibilityLevel;


/**
 * Transfer handler for an OperatorTree.
 *
 * @author Simon Fischer
 *
 */
public class OperatorTreeTransferHandler extends ReceivingOperatorTransferHandler {

	private static final long serialVersionUID = -3039947430247192040L;

	private final OperatorTree operatorTree;

	/** A timer needed for automatic node expansion */
	private final Timer nodeExpandDelay = new Timer(1500, new ActionListener() {

		@Override
		public void actionPerformed(ActionEvent e) {
			if (operatorTree.isRootVisible() && operatorTree.getRowForPath(currentPath) == 0) {
				return;
			} else {
				if (operatorTree.isExpanded(currentPath)) {
					operatorTree.collapsePath(currentPath);
				} else {
					operatorTree.expandPath(currentPath);
				}

			}
		}
	}) {

		private static final long serialVersionUID = 5805944782054617670L;
		{
			setRepeats(false);
		}
	};

	public enum Position {
		ABOVE, BELOW, INTO, UNMARKED
	};

	private static class DnDMarker {

		private final ExecutionUnit markedUnit;
		private final Operator markedOperator;
		private final Position markerPosition;

		public DnDMarker(Operator operator, Position position) {
			this.markedOperator = operator;
			this.markerPosition = position;
			this.markedUnit = null;
		}

		private DnDMarker(ExecutionUnit unit, Position position) {
			this.markedUnit = unit;
			this.markerPosition = position;
			this.markedOperator = null;
		}

		@Override
		public boolean equals(Object o) {
			if (!(o instanceof DnDMarker)) {
				return false;
			}
			if (o == null) {
				return false;
			}

			DnDMarker marker = (DnDMarker) o;
			return marker.markedOperator == this.markedOperator && marker.markerPosition == this.markerPosition
					&& marker.markedUnit == this.markedUnit;
		}

		public boolean canDrop(List<Operator> operators) {
			// avoid adding operator as a child of its own.
			Operator target = markedOperator;
			if (target.getExecutionUnit() == null && this.markerPosition != Position.INTO) {
				return false;
			}
			for (Operator operator : operators) {
				if (operator instanceof OperatorChain) {
					if (target == operator) {
						return false;
					}
					if (markedUnit != null) {
						target = markedUnit.getEnclosingOperator();
					}
					if (target == operator || ((OperatorChain) operator).getAllInnerOperators().contains(target)) {
						return false;
					}
				}
			}
			return true;
		}

		public void drop(List<Operator> operators) {
			if (markedOperator != null) {
				if (markerPosition == Position.INTO) {
					// this is only possible for operator chains with only one execution unit.
					for (Operator operator : operators) {
						((OperatorChain) markedOperator).getSubprocess(0).addOperator(operator);
					}
				} else {
					// add as sibling of operator
					ExecutionUnit executionUnit = markedOperator.getExecutionUnit();
					if (executionUnit == null) {
						return;
					}
					int newIndex = executionUnit.getOperators().indexOf(markedOperator);
					if (markerPosition == Position.BELOW) {
						newIndex++;
					}
					int i = 0;
					for (Operator operator : operators) {
						executionUnit.addOperator(operator, newIndex + i);
						i++;
					}
				}
			} else {
				// add to execution unit
				switch (markerPosition) {
					case ABOVE:
						int i = 0;
						for (Operator operator : operators) {
							markedUnit.addOperator(operator, i);
							i++;
						}
						break;
					case BELOW:
						for (Operator operator : operators) {
							markedUnit.addOperator(operator);
						}
						break;
				}
			}
			for (Operator operator : operators) {
				if (RapidMinerGUI.getMainFrame().VALIDATE_AUTOMATICALLY_ACTION.isSelected()) {
					operator.getExecutionUnit().autoWireSingle(operator, CompatibilityLevel.VERSION_5,
							RapidMinerGUI.getMainFrame().getNewOperatorEditor().shouldAutoConnectNewOperatorsInputs(),
							RapidMinerGUI.getMainFrame().getNewOperatorEditor().shouldAutoConnectNewOperatorsOutputs());
				}
			}
		}
	}

	private DnDMarker currentDnDMarker = null;
	private TreePath currentPath;

	public OperatorTreeTransferHandler(OperatorTree operatorTree) {
		this.operatorTree = operatorTree;
	}

	@Override
	protected boolean dropNow(List<Operator> droppedOperators, Point loc) {
		if (loc != null || currentDnDMarker != null) {
			currentDnDMarker.drop(droppedOperators);
		} else {
			RapidMinerGUI.getMainFrame().getActions().insert(droppedOperators);
		}
		return true;
	}

	@Override
	protected void dropEnds() {
		currentPath = null;
		nodeExpandDelay.stop();
		currentDnDMarker = null;
	}

	@Override
	protected boolean isDropLocationOk(List<Operator> newOperators, Point loc) {
		return currentDnDMarker.canDrop(newOperators);
	}

	@Override
	protected void markDropOver(Point currentCursorLocation) {
		currentPath = operatorTree.getClosestPathForLocation(currentCursorLocation.x, currentCursorLocation.y);
		Rectangle dropActionTriggerArea = operatorTree.getPathBounds(currentPath);
		if (dropActionTriggerArea != null) {
			Object currentDropZone = currentPath.getLastPathComponent();
			if (currentDropZone != null) {
				// start the computation of the dropline position
				int operatorPosition = operatorTree.getPathBounds(currentPath).getLocation().y;
				double operatorHeight = operatorTree.getPathBounds(currentPath).getHeight();
				Position position;
				if (currentPath.getLastPathComponent() instanceof OperatorChain
						&& ((OperatorChain) currentPath.getLastPathComponent()).getNumberOfSubprocesses() == 1) {
					if (currentCursorLocation.y < operatorPosition + operatorHeight / 3) {
						position = Position.ABOVE;
					} else if (currentCursorLocation.y > operatorPosition + operatorHeight * 2 / 3) {
						position = Position.BELOW;
					} else {
						position = Position.INTO;
					}
				} else {
					if (currentCursorLocation.y < operatorPosition + operatorHeight / 2) {
						position = Position.ABOVE;
					} else {
						position = Position.BELOW;
					}
				}
				if (currentDropZone instanceof ExecutionUnit) {
					currentDnDMarker = new DnDMarker((ExecutionUnit) currentDropZone, position);
					nodeExpandDelay.restart();
				} else if (currentDropZone instanceof Operator) {
					// if (((Operator)currentDropZone).getExecutionUnit() == null) {
					// currentDnDMarker = null;
					// } else {
					if (((Operator) currentDropZone).getExecutionUnit() == null) {
						position = Position.INTO;
					}
					currentDnDMarker = new DnDMarker((Operator) currentDropZone, position);
					if (currentDropZone instanceof OperatorChain) {
						nodeExpandDelay.restart();
					}
					// }
				} else {
					throw new IllegalArgumentException("Nodes of tree must be Operators or ExecutionUnits.");
				}
			}
		}

		// autoscroll to possible dropzone
		Insets insets = new Insets(40, 40, 40, 40);
		Rectangle currentlyVisible = operatorTree.getVisibleRect();
		Rectangle validCursorArea = new Rectangle(currentlyVisible.x + insets.left, currentlyVisible.y + insets.top,
				currentlyVisible.width - (insets.left + insets.right), currentlyVisible.height
				- (insets.top + insets.bottom));
		if (!validCursorArea.contains(currentCursorLocation)) {
			Rectangle updatedArea = new Rectangle(currentCursorLocation.x - insets.left, currentCursorLocation.y
					- insets.top, insets.left + insets.right, insets.top + insets.bottom);
			operatorTree.scrollRectToVisible(updatedArea);
		}
	}

	@Override
	protected List<Operator> getDraggedOperators() {
		List<Operator> ops = operatorTree.getSelectedOperators();
		if (ops.isEmpty()) {
			return null;
		} else {
			return ops;
		}
	}

	public Position getMarkerPosition(Operator operator) {
		if (currentDnDMarker == null) {
			return Position.UNMARKED;
		} else if (currentDnDMarker.markedOperator == operator) {
			return currentDnDMarker.markerPosition;
		} else {
			return Position.UNMARKED;
		}
	}

	public Position getMarkerPosition(ExecutionUnit unit) {
		if (currentDnDMarker == null) {
			return Position.UNMARKED;
		} else if (currentDnDMarker.markedUnit == unit) {
			return currentDnDMarker.markerPosition;
		} else {
			return Position.UNMARKED;
		}
	}

	@Override
	protected Process getProcess() {
		return ((Operator) ((ProcessTreeModel) operatorTree.getModel()).getRoot()).getProcess();
	}

	@Override
	protected boolean dropNow(String processXML) {
		// cannot drop process XML here
		return false;
	}

	@Override
	protected boolean dropNow(WorkflowAnnotation anno, Point loc) {
		// cannot drop workflow annotations here
		return false;
	}
}
