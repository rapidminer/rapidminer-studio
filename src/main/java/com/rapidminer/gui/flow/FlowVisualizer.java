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
package com.rapidminer.gui.flow;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Stroke;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javax.swing.Action;
import javax.swing.JPopupMenu;
import javax.swing.JToggleButton;

import com.rapidminer.gui.RapidMinerGUI;
import com.rapidminer.gui.actions.ToggleAction;
import com.rapidminer.gui.flow.processrendering.draw.ProcessDrawDecorator;
import com.rapidminer.gui.flow.processrendering.draw.ProcessDrawer;
import com.rapidminer.gui.flow.processrendering.model.ProcessRendererModel;
import com.rapidminer.gui.flow.processrendering.view.ProcessEventDecorator;
import com.rapidminer.gui.flow.processrendering.view.ProcessRendererView;
import com.rapidminer.gui.flow.processrendering.view.RenderPhase;
import com.rapidminer.gui.tools.ResourceAction;
import com.rapidminer.gui.tools.SwingTools;
import com.rapidminer.operator.ExecutionUnit;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.tools.FontTools;


/**
 * This class lets the user view and edit the execution order of a process.
 *
 * @author Simon Fischer
 *
 */
public class FlowVisualizer {

	private static final Stroke FLOW_STROKE = new BasicStroke(10f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
	private static final Font FLOW_FONT = FontTools.getFont(Font.DIALOG, Font.BOLD, 18);
	private static final Stroke LINE_STROKE = new BasicStroke(1f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
	private static final Stroke HIGHLIGHT_STROKE = new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
	private static final Color PASSIVE_COLOR = new Color(0, 0, 0, 50);
	private static final Color FLOW_COLOR = new Color(SwingTools.RAPIDMINER_LIGHT_ORANGE.getRed(),
			SwingTools.RAPIDMINER_LIGHT_ORANGE.getGreen(), SwingTools.RAPIDMINER_LIGHT_ORANGE.getBlue(), 125);
	private static final Color GRAY_OUT = new Color(255, 255, 255, 100);

	public final ToggleAction ALTER_EXECUTION_ORDER = new ToggleAction(true, "render_execution_order") {

		private static final long serialVersionUID = -8333670355512143502L;

		@Override
		public void actionToggled(ActionEvent e) {
			if (isSelected()) {
				// update execution order before visualizing it
				RapidMinerGUI.getMainFrame().getProcess().getRootOperator().updateExecutionOrder();
			}
			setActive(isSelected());
			view.requestFocusInWindow();
		}

	};

	protected JToggleButton SHOW_ORDER_TOGGLEBUTTON = ALTER_EXECUTION_ORDER.createToggleButton();

	private final Action BRING_TO_FRONT = new ResourceAction("bring_operator_to_front") {

		private static final long serialVersionUID = 1L;

		@Override
		public void loggedActionPerformed(ActionEvent e) {
			if (hoveringOperator != null) {
				hoveringOperator.getExecutionUnit().moveToIndex(hoveringOperator, 0);
			}
		}
	};

	private boolean active = false;
	private final ProcessRendererView view;

	private Operator startOperator;
	private Operator endOperator;
	private Operator hoveringOperator;
	private Collection<Operator> dependentOps;

	/** the decorator which draws the flow visualization */
	private final ProcessEventDecorator eventDecorator = new ProcessEventDecorator() {

		@Override
		public void processMouseEvent(ExecutionUnit process, MouseEventType type, MouseEvent e) {
			// ignore if not active
			if (!isActive()) {
				return;
			}

			switch (type) {
				case MOUSE_CLICKED:
					showPopupMenu(e);
					break;
				case MOUSE_MOVED:
					hoveringOperator = findOperator(e.getPoint());
					if (startOperator != null) {
						if (hoveringOperator != startOperator) {
							endOperator = hoveringOperator;
							recomputeDependentOperators();
							view.repaint();
						}
					}
					break;
				case MOUSE_PRESSED:
					if (showPopupMenu(e)) {
						return;
					}

					Operator op = findOperator(e.getPoint());
					switch (e.getButton()) {
						case MouseEvent.BUTTON1:
							if (startOperator == null) {
								if (op != startOperator) {
									startOperator = op;
									dependentOps = null;
									recomputeDependentOperators();
									view.repaint();
								}
							} else if (dependentOps != null) {
								startOperator.getExecutionUnit().bringToFront(dependentOps, startOperator);
								startOperator = endOperator = null;
								dependentOps = null;
								view.repaint();
							}
							break;
						case MouseEvent.BUTTON3:
							startOperator = endOperator = null;
							dependentOps = null;
							view.repaint();
							break;
					}
					break;
				case MOUSE_RELEASED:
					showPopupMenu(e);
					break;
				// $CASES-OMITTED$
				default:
					break;

			}

			// no matter what, while flow visualizer is active we consume all events
			e.consume();
		}

		@Override
		public void processKeyEvent(ExecutionUnit process, KeyEventType type, KeyEvent e) {
			// ignore if not active
			if (!isActive()) {
				return;
			}

			if (type == KeyEventType.KEY_PRESSED) {
				if (KeyEvent.VK_ESCAPE == e.getKeyCode()) {
					SHOW_ORDER_TOGGLEBUTTON.doClick();
				}
			}

			// no matter what, while flow visualizer is active we consume all events
			e.consume();
		}
	};

	/** the decorator which draws the flow visualization */
	private final ProcessDrawDecorator drawDecorator = new ProcessDrawDecorator() {

		@Override
		public void draw(ExecutionUnit process, Graphics2D g2, ProcessRendererModel model) {
			if (active) {
				// Re-Arrange operators
				List<Operator> operators = new LinkedList<Operator>(process.getOperators());
				if (dependentOps != null) {
					operators.removeAll(dependentOps);
					int insertionIndex = operators.indexOf(startOperator) + 1;
					for (Operator depOp : dependentOps) {
						operators.add(insertionIndex++, depOp);
					}
				}

				// they should be sorted already.
				Point2D lastPoint = null;
				g2.setStroke(FLOW_STROKE);
				for (Operator op : operators) {
					if (!op.isEnabled()) {
						continue;
					}
					Rectangle2D r = view.getModel().getOperatorRect(op);

					if (startOperator == null || dependentOps != null && dependentOps.contains(op)) {
						g2.setColor(FLOW_COLOR);
					} else {
						g2.setColor(PASSIVE_COLOR);
					}

					if (lastPoint != null) {
						g2.draw(new Line2D.Double(lastPoint.getX(), lastPoint.getY(), r.getCenterX(),
								r.getCenterY() + ProcessDrawer.HEADER_HEIGHT / 2 - 2));
					}
					lastPoint = new Point2D.Double(r.getCenterX(), r.getCenterY() + ProcessDrawer.HEADER_HEIGHT / 2 - 2);
				}

				int i = 0;
				g2.setStroke(LINE_STROKE);
				g2.setFont(FLOW_FONT);
				boolean illegalStart = operators.indexOf(endOperator) < operators.indexOf(startOperator);
				for (Operator op : operators) {
					if (!op.isEnabled()) {
						continue;
					}
					i++;
					Rectangle2D r = view.getModel().getOperatorRect(op);
					int size = 30;
					double y = r.getCenterY() + ProcessDrawer.HEADER_HEIGHT / 2 - 2;

					// gray out operator rect
					Color oldC = g2.getColor();
					g2.setColor(GRAY_OUT);
					g2.fill(r);
					g2.setColor(oldC);

					Ellipse2D circle = new Ellipse2D.Double(r.getCenterX() - size / 2, y - size / 2, size, size);
					// Fill circle
					if (illegalStart && op == endOperator) {
						g2.setColor(Color.red);
					} else if (op == startOperator || op == endOperator) {
						g2.setColor(SwingTools.LIGHT_BLUE);
					} else if (dependentOps != null && dependentOps.contains(op)) {
						g2.setColor(SwingTools.LIGHT_BLUE);
					} else {
						g2.setColor(Color.WHITE);
					}
					g2.fill(circle);

					// Draw circle
					if (op == hoveringOperator || startOperator == null || startOperator == op
							|| dependentOps != null && dependentOps.contains(op)) {
						g2.setColor(Color.BLACK);
					} else {
						g2.setColor(Color.LIGHT_GRAY);
					}
					if (op == hoveringOperator) {
						g2.setStroke(HIGHLIGHT_STROKE);
					} else {
						g2.setStroke(LINE_STROKE);
					}
					g2.draw(circle);

					String label = "" + i;
					Rectangle2D bounds = FLOW_FONT.getStringBounds(label, g2.getFontRenderContext());
					g2.drawString(label, (float) (r.getCenterX() - bounds.getWidth() / 2),
							(float) (y - bounds.getHeight() / 2 - bounds.getY()));
				}
			}
		}

		@Override
		public void print(ExecutionUnit process, Graphics2D g2, ProcessRendererModel model) {
			draw(process, g2, model);
		}
	};

	public FlowVisualizer(ProcessRendererView processRenderer) {
		this.view = processRenderer;

		processRenderer.addEventDecorator(eventDecorator, RenderPhase.OVERLAY);
		processRenderer.addDrawDecorator(drawDecorator, RenderPhase.OVERLAY);

		SHOW_ORDER_TOGGLEBUTTON.setText(null);
	}

	/**
	 * Activates/Deactivates the flow visualizer.
	 *
	 * @param active
	 */
	private void setActive(boolean active) {
		if (this.active != active) {
			this.active = active;
			if (!active) {
				startOperator = null;
				startOperator = endOperator = null;
				dependentOps = null;
			}
			view.repaint();
		}
	}

	/**
	 * Returns whether the flow visualizer is active.
	 *
	 * @return
	 */
	public boolean isActive() {
		return active;
	}

	/**
	 * Return the operators the specified one depends on.
	 *
	 * @param enclosingOperator
	 * @param startIndex
	 * @param endIndex
	 * @param topologicallySortedCandidates
	 * @return
	 */
	private Collection<Operator> getDependingOperators(Operator enclosingOperator, int startIndex, int endIndex,
			List<Operator> topologicallySortedCandidates) {

		if (endIndex <= startIndex) {
			return Collections.emptyList();
		}

		Set<Operator> foundDependingOperators = new HashSet<Operator>();
		Set<Operator> completedOperators = new HashSet<Operator>();

		Operator stopWhenReaching = topologicallySortedCandidates.get(startIndex);

		foundDependingOperators.add(topologicallySortedCandidates.get(endIndex));

		for (int opIndex = endIndex; opIndex > startIndex; opIndex--) {
			Operator op = topologicallySortedCandidates.get(opIndex);

			// remember that we are already working on this one
			completedOperators.add(op);

			// Do we depend on that one? Otherwise, we can continue with the next.
			// (The startIndex-th operator is always in this set, so we actually start doing
			// something.)
			if (!foundDependingOperators.contains(op)) {
				continue;
			}
			for (InputPort in : op.getInputPorts().getAllPorts()) {
				if (in.isConnected()) {
					Operator predecessor = in.getSource().getPorts().getOwner().getOperator();
					// Skip if connected to inner sink
					if (predecessor == enclosingOperator) {
						continue;
					} else {
						// Skip if working on it already
						if (completedOperators.contains(predecessor)) {
							continue;
							// Skip when reaching end of the range
						} else if (predecessor == stopWhenReaching) { // did we reach the end?
							continue;
						} else {
							// Skip when beyond bounds
							int predecessorIndex = topologicallySortedCandidates.indexOf(predecessor);
							if (predecessorIndex <= startIndex) {
								continue;
							} else {
								// Otherwise, add to set of depending operators
								foundDependingOperators.add(predecessor);
							}
						}
					}
				}
			}
		}

		List<Operator> orderedResult = new LinkedList<Operator>();
		for (Operator op : topologicallySortedCandidates) {
			if (foundDependingOperators.contains(op)) {
				orderedResult.add(op);
			}
		}
		return orderedResult;
	}

	/**
	 * Finds the operator under the given point.
	 *
	 * @param point
	 *            the point in question
	 * @return the operator or {@code null}
	 */
	private Operator findOperator(Point point) {
		int processIndex = view.getProcessIndexUnder(point);
		if (processIndex != -1) {
			Point mousePositionRelativeToProcess = view.toProcessSpace(point, processIndex);
			if (mousePositionRelativeToProcess == null) {
				return null;
			}

			for (Operator op : view.getModel().getDisplayedChain().getSubprocess(processIndex).getOperators()) {
				Rectangle2D rect = view.getModel().getOperatorRect(op);
				if (rect.contains(new Point2D.Double(mousePositionRelativeToProcess.x, mousePositionRelativeToProcess.y))) {
					return op;
				}
			}
		}
		return null;
	}

	/**
	 * Calculate operator dependencies.
	 */
	private void recomputeDependentOperators() {
		if (startOperator == null || endOperator == null) {
			dependentOps = null;
		} else {
			ExecutionUnit unit = startOperator.getExecutionUnit();
			if (endOperator.getExecutionUnit() != unit) {
				dependentOps = null;
				return;
			} else {
				List<Operator> operators = unit.getOperators();
				dependentOps = getDependingOperators(view.getModel().getDisplayedChain(), operators.indexOf(startOperator),
						operators.indexOf(endOperator), operators);
			}
		}
	}

	/**
	 * Determine whether to show the popup menu.
	 *
	 * @param e
	 * @return
	 */
	private boolean showPopupMenu(MouseEvent e) {
		if (e.isPopupTrigger()) {
			JPopupMenu menu = new JPopupMenu();
			if (hoveringOperator != null) {
				menu.add(BRING_TO_FRONT);
			}

			// Add action to leave FlowVisualiser and add seperator if it is not the only action.
			if (menu.getSubElements().length > 0) {
				menu.addSeparator();
			}
			menu.add(new ResourceAction("render_execution_order_apply") {

				private static final long serialVersionUID = 1L;

				@Override
				public void loggedActionPerformed(ActionEvent e) {
					ALTER_EXECUTION_ORDER.actionPerformed(e);
				}
			});

			menu.show(view, e.getX(), e.getY());
			e.consume();
			return true;
		} else {
			return false;
		}
	}
}
