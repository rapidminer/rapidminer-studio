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

import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.stream.Collectors;
import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

import com.rapidminer.BreakpointListener;
import com.rapidminer.Process;
import com.rapidminer.ProcessLocation;
import com.rapidminer.core.license.ProductConstraintManager;
import com.rapidminer.gui.MainFrame;
import com.rapidminer.gui.RapidMinerGUI;
import com.rapidminer.gui.actions.ConnectPortToRepositoryAction;
import com.rapidminer.gui.actions.StoreInRepositoryAction;
import com.rapidminer.gui.actions.export.PrintableComponent;
import com.rapidminer.gui.animation.OperatorAnimationProcessListener;
import com.rapidminer.gui.dnd.AbstractPatchedTransferHandler;
import com.rapidminer.gui.dnd.DragListener;
import com.rapidminer.gui.dnd.OperatorTransferHandler;
import com.rapidminer.gui.flow.ExtensionButton;
import com.rapidminer.gui.flow.OverviewPanel;
import com.rapidminer.gui.flow.PanningManager;
import com.rapidminer.gui.flow.ProcessInteractionListener;
import com.rapidminer.gui.flow.ProcessPanel;
import com.rapidminer.gui.flow.processrendering.annotations.model.OperatorAnnotation;
import com.rapidminer.gui.flow.processrendering.annotations.model.WorkflowAnnotation;
import com.rapidminer.gui.flow.processrendering.annotations.model.WorkflowAnnotations;
import com.rapidminer.gui.flow.processrendering.draw.OperatorDrawDecorator;
import com.rapidminer.gui.flow.processrendering.draw.ProcessDrawDecorator;
import com.rapidminer.gui.flow.processrendering.draw.ProcessDrawUtils;
import com.rapidminer.gui.flow.processrendering.draw.ProcessDrawer;
import com.rapidminer.gui.flow.processrendering.event.ProcessRendererAnnotationEvent;
import com.rapidminer.gui.flow.processrendering.event.ProcessRendererEventListener;
import com.rapidminer.gui.flow.processrendering.event.ProcessRendererModelEvent;
import com.rapidminer.gui.flow.processrendering.event.ProcessRendererOperatorEvent;
import com.rapidminer.gui.flow.processrendering.model.ProcessRendererModel;
import com.rapidminer.gui.flow.processrendering.view.ProcessEventDecorator.KeyEventType;
import com.rapidminer.gui.flow.processrendering.view.ProcessEventDecorator.MouseEventType;
import com.rapidminer.gui.flow.processrendering.view.actions.ArrangeOperatorsAction;
import com.rapidminer.gui.flow.processrendering.view.actions.AutoFitAction;
import com.rapidminer.gui.flow.processrendering.view.actions.DeleteSelectedConnectionAction;
import com.rapidminer.gui.flow.processrendering.view.actions.RenameAction;
import com.rapidminer.gui.flow.processrendering.view.actions.SelectAllAction;
import com.rapidminer.gui.flow.processrendering.view.components.ProcessRendererTooltipProvider;
import com.rapidminer.gui.processeditor.OperatorPortActionRegistry;
import com.rapidminer.gui.properties.celleditors.value.PropertyValueCellEditor;
import com.rapidminer.gui.tools.PrintingTools;
import com.rapidminer.gui.tools.ResourceAction;
import com.rapidminer.gui.tools.ResourceMenu;
import com.rapidminer.gui.tools.SwingTools;
import com.rapidminer.gui.tools.components.ToolTipWindow;
import com.rapidminer.gui.tools.components.ToolTipWindow.TooltipLocation;
import com.rapidminer.license.LicenseEvent;
import com.rapidminer.license.LicenseEvent.LicenseEventType;
import com.rapidminer.license.LicenseManagerListener;
import com.rapidminer.operator.ExecutionUnit;
import com.rapidminer.operator.IOObject;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorChain;
import com.rapidminer.operator.ProcessRootOperator;
import com.rapidminer.operator.ResultObject;
import com.rapidminer.operator.ports.DeliveringPortManager;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.operator.ports.OutputPort;
import com.rapidminer.operator.ports.Port;
import com.rapidminer.operator.ports.Ports;
import com.rapidminer.operator.ports.quickfix.QuickFix;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.repository.RepositoryLocation;
import com.rapidminer.tools.I18N;
import com.rapidminer.tools.LogService;
import com.rapidminer.tools.ParameterService;
import com.rapidminer.tools.SystemInfoUtilities;
import com.rapidminer.tools.SystemInfoUtilities.OperatingSystem;
import com.rapidminer.tools.Tools;
import com.rapidminer.tools.usagestats.ActionStatisticsCollector;


/**
 * This class displays a RapidMiner process and allows user interaction with it.
 * <p>
 * Actual Java2D drawing is delegated to the {@link ProcessDrawer} and its registered
 * {@link ProcessDrawDecorator}s. To decorate the process drawing, call
 * {@link #addDrawDecorator(ProcessDrawDecorator, RenderPhase)} and register custom decorators.
 * </p>
 * <p>
 * To provide hooks into the event handling, i.e. to allow the user to interact with your
 * decorations, event decorators can be registered via
 * {@link #addEventDecorator(ProcessEventDecorator, RenderPhase)}.
 * </p>
 * <p>
 * To simply hook into default popup menus, register a listener via
 * {@link #addProcessInteractionListener(ProcessInteractionListener)}.
 * </p>
 *
 * @author Simon Fischer, Marco Boeck, Jan Czogalla
 * @since 6.4.0
 *
 */
public class ProcessRendererView extends JPanel implements PrintableComponent {

	private static final long serialVersionUID = 1L;

	private static final int RENAME_FIELD_HEIGHT = 21;

	/** the text field used for renaming an operator */
	private JTextField renameField;

	/** responsible for default process renderer view interaction */
	private final transient ProcessRendererMouseHandler interactionMouseHandler;

	/** the mouse handler for the entire process renderer */
	private final transient MouseAdapter mouseHandler = new MouseAdapter() {

		@Override
		public void mouseMoved(final MouseEvent e) {
			// no matter what, update mouse locations first
			model.setCurrentMousePosition(e.getPoint());
			model.setHoveringProcessIndex(getProcessIndexUnder(e.getPoint()));
			int hoveringProcessIndex = model.getHoveringProcessIndex();
			if (model.getHoveringProcessIndex() != -1) {
				model.setMousePositionRelativeToProcess(toProcessSpace(e.getPoint(), hoveringProcessIndex));
			}

			// foreground, overlay and operator addition listeners can be notified before operator
			// hovering
			boolean wasConsumed = false;
			wasConsumed |= processPhaseListenerMouseEvent(MouseEventType.MOUSE_MOVED, e, RenderPhase.FOREGROUND);
			if (wasConsumed) {
				return;
			}
			wasConsumed |= processPhaseListenerMouseEvent(MouseEventType.MOUSE_MOVED, e, RenderPhase.OVERLAY);
			if (wasConsumed) {
				return;
			}
			wasConsumed |= processPhaseListenerMouseEvent(MouseEventType.MOUSE_MOVED, e, RenderPhase.OPERATOR_ADDITIONS);
			if (wasConsumed) {
				return;
			}

			interactionMouseHandler.mouseMoved(e);
			if (e.isConsumed()) {
				return;
			}

			// if core handling did not consume, forward event further down
			wasConsumed |= processPhaseListenerMouseEvent(MouseEventType.MOUSE_MOVED, e, RenderPhase.OPERATORS);
			if (wasConsumed) {
				return;
			}
			wasConsumed |= processPhaseListenerMouseEvent(MouseEventType.MOUSE_MOVED, e, RenderPhase.CONNECTIONS);
			if (wasConsumed) {
				return;
			}
			wasConsumed |= processPhaseListenerMouseEvent(MouseEventType.MOUSE_MOVED, e, RenderPhase.OPERATOR_BACKGROUND);
			if (wasConsumed) {
				return;
			}
			wasConsumed |= processPhaseListenerMouseEvent(MouseEventType.MOUSE_MOVED, e, RenderPhase.OPERATOR_ANNOTATIONS);
			if (wasConsumed) {
				return;
			}
			wasConsumed |= processPhaseListenerMouseEvent(MouseEventType.MOUSE_MOVED, e, RenderPhase.ANNOTATIONS);
			if (wasConsumed) {
				return;
			}
			wasConsumed |= processPhaseListenerMouseEvent(MouseEventType.MOUSE_MOVED, e, RenderPhase.BACKGROUND);
			if (wasConsumed) {
				return;
			}
		}

		@Override
		public void mouseDragged(final MouseEvent e) {
			// no matter what, update mouse locations first
			model.setCurrentMousePosition(e.getPoint());
			model.setHoveringProcessIndex(getProcessIndexUnder(e.getPoint()));
			int hoveringProcessIndex = model.getHoveringProcessIndex();
			if (model.getHoveringProcessIndex() != -1) {
				model.setMousePositionRelativeToProcess(toProcessSpace(e.getPoint(), hoveringProcessIndex));
			}

			// foreground, overlay and operator addition listeners can be notified before operator
			// hovering
			boolean wasConsumed = false;
			wasConsumed |= processPhaseListenerMouseEvent(MouseEventType.MOUSE_DRAGGED, e, RenderPhase.FOREGROUND);
			if (wasConsumed) {
				return;
			}
			wasConsumed |= processPhaseListenerMouseEvent(MouseEventType.MOUSE_DRAGGED, e, RenderPhase.OVERLAY);
			if (wasConsumed) {
				return;
			}
			wasConsumed |= processPhaseListenerMouseEvent(MouseEventType.MOUSE_DRAGGED, e, RenderPhase.OPERATOR_ADDITIONS);
			if (wasConsumed) {
				return;
			}

			interactionMouseHandler.mouseDragged(e);
			if (e.isConsumed()) {
				return;
			}

			// if core handling did not consume, forward event further down
			wasConsumed |= processPhaseListenerMouseEvent(MouseEventType.MOUSE_DRAGGED, e, RenderPhase.OPERATORS);
			if (wasConsumed) {
				return;
			}
			wasConsumed |= processPhaseListenerMouseEvent(MouseEventType.MOUSE_DRAGGED, e, RenderPhase.CONNECTIONS);
			if (wasConsumed) {
				return;
			}
			wasConsumed |= processPhaseListenerMouseEvent(MouseEventType.MOUSE_DRAGGED, e, RenderPhase.OPERATOR_BACKGROUND);
			if (wasConsumed) {
				return;
			}
			wasConsumed |= processPhaseListenerMouseEvent(MouseEventType.MOUSE_DRAGGED, e, RenderPhase.OPERATOR_ANNOTATIONS);
			if (wasConsumed) {
				return;
			}
			wasConsumed |= processPhaseListenerMouseEvent(MouseEventType.MOUSE_DRAGGED, e, RenderPhase.ANNOTATIONS);
			if (wasConsumed) {
				return;
			}
			wasConsumed |= processPhaseListenerMouseEvent(MouseEventType.MOUSE_DRAGGED, e, RenderPhase.BACKGROUND);
			if (wasConsumed) {
				return;
			}
		}

		@Override
		public void mousePressed(final MouseEvent e) {
			// whatever we pressed the mouse on, remove the rename field
			if (renameField != null) {
				remove(renameField);
			}

			requestFocusInWindow();

			// foreground, overlay and operator addition listeners can be notified before operator
			// modifications
			boolean wasConsumed = false;
			wasConsumed |= processPhaseListenerMouseEvent(MouseEventType.MOUSE_PRESSED, e, RenderPhase.FOREGROUND);
			if (wasConsumed) {
				return;
			}
			wasConsumed |= processPhaseListenerMouseEvent(MouseEventType.MOUSE_PRESSED, e, RenderPhase.OVERLAY);
			if (wasConsumed) {
				return;
			}
			wasConsumed |= processPhaseListenerMouseEvent(MouseEventType.MOUSE_PRESSED, e, RenderPhase.OPERATOR_ADDITIONS);
			if (wasConsumed) {
				return;
			}

			interactionMouseHandler.mousePressed(e);
			if (e.isConsumed()) {
				return;
			}

			// if core handling did not consume, forward event further down
			wasConsumed |= processPhaseListenerMouseEvent(MouseEventType.MOUSE_PRESSED, e, RenderPhase.OPERATORS);
			if (wasConsumed) {
				return;
			}
			wasConsumed |= processPhaseListenerMouseEvent(MouseEventType.MOUSE_PRESSED, e, RenderPhase.CONNECTIONS);
			if (wasConsumed) {
				return;
			}
			wasConsumed |= processPhaseListenerMouseEvent(MouseEventType.MOUSE_PRESSED, e, RenderPhase.OPERATOR_BACKGROUND);
			if (wasConsumed) {
				return;
			}
			wasConsumed |= processPhaseListenerMouseEvent(MouseEventType.MOUSE_PRESSED, e, RenderPhase.OPERATOR_ANNOTATIONS);
			if (wasConsumed) {
				return;
			}
			wasConsumed |= processPhaseListenerMouseEvent(MouseEventType.MOUSE_PRESSED, e, RenderPhase.ANNOTATIONS);
			if (wasConsumed) {
				return;
			}

			interactionMouseHandler.mousePressedBackground(e);
			if (e.isConsumed()) {
				return;
			}

			wasConsumed |= processPhaseListenerMouseEvent(MouseEventType.MOUSE_PRESSED, e, RenderPhase.BACKGROUND);
			if (wasConsumed) {
				return;
			}
		}

		@Override
		public void mouseReleased(final MouseEvent e) {
			// foreground, overlay and operator addition listeners can be notified before operator
			// modifications
			boolean wasConsumed = false;
			wasConsumed |= processPhaseListenerMouseEvent(MouseEventType.MOUSE_RELEASED, e, RenderPhase.FOREGROUND);
			if (wasConsumed) {
				return;
			}
			wasConsumed |= processPhaseListenerMouseEvent(MouseEventType.MOUSE_RELEASED, e, RenderPhase.OVERLAY);
			if (wasConsumed) {
				return;
			}
			wasConsumed |= processPhaseListenerMouseEvent(MouseEventType.MOUSE_RELEASED, e, RenderPhase.OPERATOR_ADDITIONS);
			if (wasConsumed) {
				return;
			}

			interactionMouseHandler.mouseReleased(e);
			if (e.isConsumed()) {
				return;
			}

			// if core handling did not consume, forward event further down
			wasConsumed |= processPhaseListenerMouseEvent(MouseEventType.MOUSE_RELEASED, e, RenderPhase.OPERATORS);
			if (wasConsumed) {
				return;
			}
			wasConsumed |= processPhaseListenerMouseEvent(MouseEventType.MOUSE_RELEASED, e, RenderPhase.CONNECTIONS);
			if (wasConsumed) {
				return;
			}
			wasConsumed |= processPhaseListenerMouseEvent(MouseEventType.MOUSE_RELEASED, e, RenderPhase.OPERATOR_BACKGROUND);
			if (wasConsumed) {
				return;
			}
			wasConsumed |= processPhaseListenerMouseEvent(MouseEventType.MOUSE_RELEASED, e,
					RenderPhase.OPERATOR_ANNOTATIONS);
			if (wasConsumed) {
				return;
			}
			wasConsumed |= processPhaseListenerMouseEvent(MouseEventType.MOUSE_RELEASED, e, RenderPhase.ANNOTATIONS);
			if (wasConsumed) {
				return;
			}

			interactionMouseHandler.mouseReleasedBackground(e);
			if (e.isConsumed()) {
				return;
			}

			wasConsumed |= processPhaseListenerMouseEvent(MouseEventType.MOUSE_RELEASED, e, RenderPhase.BACKGROUND);
			if (wasConsumed) {
				return;
			}
		}

		@Override
		public void mouseClicked(final MouseEvent e) {
			// foreground, overlay and operator addition listeners can be notified before operator
			// modifications
			boolean wasConsumed = false;
			wasConsumed |= processPhaseListenerMouseEvent(MouseEventType.MOUSE_CLICKED, e, RenderPhase.FOREGROUND);
			if (wasConsumed) {
				return;
			}
			wasConsumed |= processPhaseListenerMouseEvent(MouseEventType.MOUSE_CLICKED, e, RenderPhase.OVERLAY);
			if (wasConsumed) {
				return;
			}
			wasConsumed |= processPhaseListenerMouseEvent(MouseEventType.MOUSE_CLICKED, e, RenderPhase.OPERATOR_ADDITIONS);
			if (wasConsumed) {
				return;
			}

			interactionMouseHandler.mouseClicked(e);
			if (e.isConsumed()) {
				return;
			}

			// if core handling did not consume, forward event further down
			wasConsumed |= processPhaseListenerMouseEvent(MouseEventType.MOUSE_CLICKED, e, RenderPhase.OPERATORS);
			if (wasConsumed) {
				return;
			}
			wasConsumed |= processPhaseListenerMouseEvent(MouseEventType.MOUSE_CLICKED, e, RenderPhase.CONNECTIONS);
			if (wasConsumed) {
				return;
			}
			wasConsumed |= processPhaseListenerMouseEvent(MouseEventType.MOUSE_CLICKED, e, RenderPhase.OPERATOR_BACKGROUND);
			if (wasConsumed) {
				return;
			}
			wasConsumed |= processPhaseListenerMouseEvent(MouseEventType.MOUSE_CLICKED, e, RenderPhase.OPERATOR_ANNOTATIONS);
			if (wasConsumed) {
				return;
			}
			wasConsumed |= processPhaseListenerMouseEvent(MouseEventType.MOUSE_CLICKED, e, RenderPhase.ANNOTATIONS);
			if (wasConsumed) {
				return;
			}
			wasConsumed |= processPhaseListenerMouseEvent(MouseEventType.MOUSE_CLICKED, e, RenderPhase.BACKGROUND);
			if (wasConsumed) {
				return;
			}
		}

		@Override
		public void mouseEntered(final MouseEvent e) {
			interactionMouseHandler.mouseEntered(e);

			// first come, first served, no limit to specific phases
			boolean wasConsumed = false;
			for (RenderPhase phase : RenderPhase.eventOrder()) {
				wasConsumed |= processPhaseListenerMouseEvent(MouseEventType.MOUSE_ENTERED, e, phase);
				// abort if event was consumed
				if (wasConsumed) {
					return;
				}
			}
		}

		@Override
		public void mouseExited(final MouseEvent e) {
			if (!SwingTools.isMouseEventExitedToChildComponents(ProcessRendererView.this, e)) {
				model.setCurrentMousePosition(null);
			}

			// always reset status text
			interactionMouseHandler.mouseExited(e);

			// first come, first served, no limit to specific phases
			boolean wasConsumed = false;
			for (RenderPhase phase : RenderPhase.eventOrder()) {
				wasConsumed |= processPhaseListenerMouseEvent(MouseEventType.MOUSE_EXITED, e, phase);
				// abort if event was consumed
				if (wasConsumed) {
					return;
				}
			}
		}
	};

	/** the key handler for the entire process renderer */

	private final transient KeyAdapter keyHandler = new KeyAdapter() {

		@Override
		public void keyPressed(final KeyEvent e) {
			boolean wasConsumed = false;
			switch (e.getKeyCode()) {
				case KeyEvent.VK_LEFT:
				case KeyEvent.VK_RIGHT:
				case KeyEvent.VK_UP:
				case KeyEvent.VK_DOWN:
					wasConsumed |= processPhaseListenerKeyEvent(KeyEventType.KEY_PRESSED, e, RenderPhase.FOREGROUND);
					if (wasConsumed) {
						return;
					}
					wasConsumed |= processPhaseListenerKeyEvent(KeyEventType.KEY_PRESSED, e, RenderPhase.OVERLAY);
					if (wasConsumed) {
						return;
					}
					wasConsumed |= processPhaseListenerKeyEvent(KeyEventType.KEY_PRESSED, e, RenderPhase.OPERATOR_ADDITIONS);
					if (wasConsumed) {
						return;
					}

					// operator phase event, no more decorator processing afterwards
					controller.selectInDirection(e);
					e.consume();
					break;
				case KeyEvent.VK_ESCAPE:
					// remove currently dragged connection. Afterwards: first come, first served
					boolean abortedConnection = false;
					if (model.getConnectingPortSource() != null) {
						model.setConnectingPortSource(null);
						model.fireMiscChanged();
						abortedConnection = true;
					}

					// process render phases that come before the OPERATORS phase, abort in case the
					// event was consumed
					for (RenderPhase phase : new RenderPhase[]{RenderPhase.FOREGROUND, RenderPhase.OVERLAY,
							RenderPhase.OPERATOR_ADDITIONS}) {
						wasConsumed |= processPhaseListenerKeyEvent(KeyEventType.KEY_PRESSED, e, phase);
						if (wasConsumed) {
							return;
						}
					}

					// OPERATORS phase event in case we are in a nested operator (consume event and
					// abort)
					if (!abortedConnection && model.getDisplayedChain().getRoot() != model.getDisplayedChain()) {
						OperatorChain parent = model.getDisplayedChain().getParent();
						if (parent != null) {
							model.setDisplayedChainAndFire(parent);
						}
						e.consume();
						return;
					}

					// remaining render phases, abort in case the event was consumed
					for (RenderPhase phase : new RenderPhase[]{RenderPhase.CONNECTIONS, RenderPhase.OPERATOR_BACKGROUND,
							RenderPhase.OPERATOR_ANNOTATIONS, RenderPhase.ANNOTATIONS, RenderPhase.BACKGROUND}) {
						wasConsumed |= processPhaseListenerKeyEvent(KeyEventType.KEY_PRESSED, e, phase);
						if (wasConsumed) {
							return;
						}
					}

					break;
				case KeyEvent.VK_ENTER:
					wasConsumed |= processPhaseListenerKeyEvent(KeyEventType.KEY_PRESSED, e, RenderPhase.FOREGROUND);
					if (wasConsumed) {
						return;
					}
					wasConsumed |= processPhaseListenerKeyEvent(KeyEventType.KEY_PRESSED, e, RenderPhase.OVERLAY);
					if (wasConsumed) {
						return;
					}
					wasConsumed |= processPhaseListenerKeyEvent(KeyEventType.KEY_PRESSED, e, RenderPhase.OPERATOR_ADDITIONS);
					if (wasConsumed) {
						return;
					}

					// operator phase event, no more decorator processing afterwards
					if (!model.getSelectedOperators().isEmpty()) {
						Operator selected = model.getSelectedOperators().get(0);
						if (selected instanceof OperatorChain && e.getModifiersEx() != InputEvent.ALT_DOWN_MASK) {
							// dive into operator chain, unless user has pressed ALT key. ALT + ENTER = activate primary parameter
							ActionStatisticsCollector.getInstance().logOperatorDoubleClick(selected, ActionStatisticsCollector.OPERATOR_ACTION_OPEN);
							model.setDisplayedChainAndFire((OperatorChain) selected);
						} else {
							// look for a primary parameter, and activate it if found
							ParameterType primaryParameter = selected.getPrimaryParameter();
							ActionStatisticsCollector.getInstance().logOperatorDoubleClick(selected, ActionStatisticsCollector.OPERATOR_ACTION_PRIMARY_PARAMETER);
							if (primaryParameter != null) {
								PropertyValueCellEditor editor = RapidMinerGUI.getMainFrame().getPropertyPanel().getEditorForKey(primaryParameter.getKey());
								if (editor != null) {
									editor.activate();
								}
							}
						}
					}
					e.consume();
					break;
				case KeyEvent.VK_BACK_SPACE:
					wasConsumed |= processPhaseListenerKeyEvent(KeyEventType.KEY_PRESSED, e, RenderPhase.FOREGROUND);
					if (wasConsumed) {
						return;
					}
					wasConsumed |= processPhaseListenerKeyEvent(KeyEventType.KEY_PRESSED, e, RenderPhase.OVERLAY);
					if (wasConsumed) {
						return;
					}
					wasConsumed |= processPhaseListenerKeyEvent(KeyEventType.KEY_PRESSED, e, RenderPhase.OPERATOR_ADDITIONS);
					if (wasConsumed) {
						return;
					}

					// OS X: Trigger deletion action.
					// Other: Navigate up (same as ESC).
					if (SystemInfoUtilities.getOperatingSystem() == OperatingSystem.OSX) {
						deleteSelectedAction.actionPerformed(null);
					} else if (model.getDisplayedChain().getRoot() != model.getDisplayedChain()) {
						OperatorChain parent = model.getDisplayedChain().getParent();
						if (parent != null) {
							model.setDisplayedChainAndFire(parent);
						}
					}
					// operator phase event, no more decorator processing afterwards
					e.consume();

					break;
				default:
					// first come, first served, no limit to specific phases
					for (RenderPhase phase : RenderPhase.eventOrder()) {
						wasConsumed |= processPhaseListenerKeyEvent(KeyEventType.KEY_PRESSED, e, phase);
						// abort if event was consumed
						if (wasConsumed) {
							return;
						}
					}
			}
		}

		@Override
		public void keyReleased(KeyEvent e) {
			boolean wasConsumed = false;
			// first come, first served
			for (RenderPhase phase : RenderPhase.eventOrder()) {
				wasConsumed |= processPhaseListenerKeyEvent(KeyEventType.KEY_RELEASED, e, phase);
				// abort if event was consumed
				if (wasConsumed) {
					return;
				}
			}
		}

		@Override
		public void keyTyped(KeyEvent e) {
			boolean wasConsumed = false;
			// first come, first served
			for (RenderPhase phase : RenderPhase.eventOrder()) {
				wasConsumed |= processPhaseListenerKeyEvent(KeyEventType.KEY_TYPED, e, phase);
				// abort if event was consumed
				if (wasConsumed) {
					return;
				}
			}
		}
	};

	/** the drag and drop listener */
	private final transient DragListener dragListener = new DragListener() {

		@Override
		public void dragStarted(Transferable t) {

			// check if transferable can be imported
			if (!controller.canImportTransferable(t)) {
				return;
			}

			model.setDragStarted(true);
			model.fireMiscChanged();
		}

		@Override
		public void dragEnded() {

			model.setDragStarted(false);
			model.fireMiscChanged();
		}
	};

	private final ResourceAction renameAction;
	private final ResourceAction selectAllAction;
	private final ResourceAction deleteSelectedAction;
	private final Action arrangeOperatorsAction;
	private final Action autoFitAction;

	/** the list of extension buttons for subprocesses (add/remove subprocess) */
	private final List<ExtensionButton> subprocessExtensionButtons;

	/** list of interaction listeners */
	private final List<ProcessInteractionListener> processInteractionListeners;

	/** the list of event decorators */
	private final Map<RenderPhase, CopyOnWriteArrayList<ProcessEventDecorator>> decorators;

	/** the backing model */
	private final ProcessRendererModel model;

	/** the controller for this view */
	private final ProcessRendererController controller;

	/** the drawer responsible for 2D drawing the process for this view */
	private final ProcessDrawer drawer;

	/** the drawer responsible for 2D drawing the process for the overview */
	private final ProcessDrawer drawerOverview;

	/** the overview panel */
	private final OverviewPanel overviewPanel;

	/** the mainframe instance */
	private final MainFrame mainFrame;

	/** the repaint filter to ensure a minimal interval between repaints */
	private final RepaintFilter repaintFilter;

	public ProcessRendererView(final ProcessRendererModel model, final ProcessPanel processPanel,
							   final MainFrame mainFrame) {
		this.mainFrame = mainFrame;
		this.model = model;
		this.controller = new ProcessRendererController(this, model);
		this.drawer = new ProcessDrawer(model, true);
		this.drawerOverview = new ProcessDrawer(model, false);
		this.repaintFilter = new RepaintFilter(this);

		// initialize process listener for animations
		new OperatorAnimationProcessListener(model);

		// prepare event decorators for each phase
		decorators = new EnumMap<>(RenderPhase.class);
		for (RenderPhase phase : RenderPhase.eventOrder()) {
			decorators.put(phase, new CopyOnWriteArrayList<>());
		}

		overviewPanel = new OverviewPanel(this);
		interactionMouseHandler = new ProcessRendererMouseHandler(this, model, controller);

		// init list of subprocess extension buttons (add/remove subprocess)
		subprocessExtensionButtons = new LinkedList<>();

		// init listener list
		processInteractionListeners = new LinkedList<>();

		// listen to ProcessRendererModel events
		model.registerEventListener(new ProcessRendererEventListener() {

			@Override
			public void modelChanged(ProcessRendererModelEvent e) {
				switch (e.getEventType()) {
					case DISPLAYED_CHAIN_CHANGED:
						controller.processDisplayedChainChanged();

						// notify registered listeners
						fireDisplayedChainChanged(model.getDisplayedChain());
						break;
					case DISPLAYED_PROCESSES_CHANGED:
						controller.setInitialSizes();
						setupExtensionButtons();
						break;
					case PROCESS_ZOOM_CHANGED:
						controller.autoFit();
						cancelRenaming();
						//$FALL-THROUGH$
					case PROCESS_SIZE_CHANGED:
						SwingUtilities.invokeLater(() -> {
							updateComponentSize();
							repaint();
						});
						break;
					case MISC_CHANGED:
						repaint();
						break;
					case DISPLAYED_CHAIN_WILL_CHANGE:
					default:
						break;
				}

			}

			@Override
			public void operatorsChanged(ProcessRendererOperatorEvent e, Collection<Operator> operators) {
				switch (e.getEventType()) {
					case SELECTED_OPERATORS_CHANGED:
						Operator firstOp = !operators.isEmpty() ? operators.iterator().next() : null;
						if (firstOp != null) {
							// only switch displayed chain if not in selected chain and selected op is not visible
							OperatorChain displayChain = (OperatorChain) (firstOp instanceof ProcessRootOperator ? firstOp
									: model.getDisplayedChain() == firstOp ? firstOp : firstOp.getParent());
							if (displayChain != null && model.getDisplayedChain() != displayChain) {
								model.setDisplayedChainAndFire(displayChain);
								return;
							}
						}
						repaint();
						break;
					case OPERATORS_MOVED:
						Set<Operator> opsToMove = new LinkedHashSet<>(operators);
						boolean wasResized = false;
						boolean shouldMoveOperators = Boolean.parseBoolean(ParameterService.getParameterValue(RapidMinerGUI.PROPERTY_RAPIDMINER_GUI_MOVE_CONNECTED_OPERATORS));
						Set<WorkflowAnnotation> movedAnnotations = new HashSet<>();
						while (!opsToMove.isEmpty()) {
							Iterator<Operator> iterator = opsToMove.iterator();
							Operator op = iterator.next();
							iterator.remove();
							ExecutionUnit executionUnit = op.getExecutionUnit();

							if (shouldMoveOperators && !Tools.isOperatorInCircle(op, -1)) {

								List<Operator> leftConnectedOperators = getDirectlyConnectedPorts(op.getInputPorts(),
										InputPort::getSource, executionUnit, Collections.emptyList());
								List<Operator> rightConnectedOperators = getDirectlyConnectedPorts(op.getOutputPorts(),
										OutputPort::getDestination, executionUnit, opsToMove);

								final Rectangle2D operatorRect = model.getOperatorRect(op);
								final Rectangle2D oldOperatorRect = (Rectangle2D) operatorRect.clone();

								// check it does not collide with other operators and move it to the right if necessary
								leftConnectedOperators.stream().map(model::getOperatorRect)
										.filter(r -> r != null && isOverlapping(r, operatorRect))
										.mapToDouble(r -> r.getX() + ProcessDrawer.GRID_AUTOARRANGE_WIDTH - 1)
										.filter(x -> operatorRect.getX() < x).max()
										.ifPresent(x -> {
											double diff = operatorRect.getX() - x;
											operatorRect.setRect(x, operatorRect.getY(), operatorRect.getWidth(), operatorRect.getHeight());
											model.setOperatorRect(op, operatorRect);
											final WorkflowAnnotations operatorAnnotations = model.getOperatorAnnotations(op);
											if (operatorAnnotations != null && !operatorAnnotations.isEmpty()) {
												List<WorkflowAnnotation> annotationsEventOrder = operatorAnnotations.getAnnotationsEventOrder();
												annotationsEventOrder.stream().filter(anno -> anno instanceof OperatorAnnotation)
														.map(WorkflowAnnotation::getLocation)
														.forEach(r -> r.setRect(r.getX() - diff, r.getY(), r.getWidth(), r.getHeight()));
												movedAnnotations.addAll(annotationsEventOrder);
											}
										});

								// check all connected operators to the right also
								// only if this operator really moved either manually or by this feature
								if (!oldOperatorRect.equals(operatorRect) || operators.contains(op)) {
									opsToMove.addAll(rightConnectedOperators);
								}
							}
							wasResized |= controller.ensureProcessSizeFits(executionUnit, model.getOperatorRect(op));
							// notify registered listeners
							fireOperatorMoved(op);
						}

						if (!movedAnnotations.isEmpty()) {
							model.fireAnnotationsMoved(movedAnnotations);
						}

						// need to repaint if process was not resized
						if (!wasResized) {
							repaint();
						}
						break;
					case PORTS_CHANGED:
						for (Operator op : operators) {
							// trigger calculation of new op height
							controller.processPortsChanged(op);
						}
						repaint();
						break;
					default:
						break;
				}
			}

			/** @since 9.1 */
			private<P extends Port> List<Operator> getDirectlyConnectedPorts(Ports<P> ports, Function<P, Port> opposite, ExecutionUnit executionUnit, Collection<Operator> exclude) {
				// get all connected ports, find their operator, make sure it is an operator on the same process level and is not already moved
				return ports.getAllPorts().stream().filter(Port::isConnected).map(port -> opposite.apply(port).getPorts().getOwner().getOperator())
						.filter(co -> executionUnit == co.getExecutionUnit() && !exclude.contains(co)).distinct().collect(Collectors.toList());
			}

			/** @since 9.2.1 */
			private boolean isOverlapping(Rectangle2D a, Rectangle2D b) {
				return isOverlappingTop(a, b) || isOverlappingTop(b, a);
			}

			/** @since 9.2.1 */
			private boolean isOverlappingTop(Rectangle2D top, Rectangle2D bottom) {
				return top.getY() <= bottom.getY() && bottom.getY() <= top.getMaxY();
			}

			@Override
			public void annotationsChanged(ProcessRendererAnnotationEvent e, Collection<WorkflowAnnotation> annotations) {
				switch (e.getEventType()) {
					case SELECTED_ANNOTATION_CHANGED:
						repaint();
						break;
					case ANNOTATIONS_MOVED:
						for (WorkflowAnnotation anno : annotations) {
							boolean wasResized = controller.ensureProcessSizeFits(anno.getProcess(), anno.getLocation());

							// need to repaint if process was not resized
							if (!wasResized) {
								repaint();
							}
						}
						break;
					case MISC_CHANGED:
						repaint();
						break;
					default:
						break;

				}

			}
		});

		// add GUI actions
		renameAction = new RenameAction(this, controller);
		selectAllAction = new SelectAllAction(this);
		deleteSelectedAction = new DeleteSelectedConnectionAction(this);
		arrangeOperatorsAction = new ArrangeOperatorsAction(this, controller);
		autoFitAction = new AutoFitAction(controller);

		// listen for process panel resizing events to adapt the render size
		processPanel.addComponentListener(new ComponentAdapter() {

			@Override
			public void componentResized(final ComponentEvent e) {
				super.componentResized(e);
				controller.autoFit();
			}
		});
		processPanel.addHierarchyListener(e -> controller.autoFit());

		// register transfer handler and drop target
		setTransferHandler(new ProcessRendererTransferHandler(this, model, controller));
		ProcessRendererDropTarget dropTarget;
		try {
			dropTarget = new ProcessRendererDropTarget(this, AbstractPatchedTransferHandler.getDropTargetListener());
			setDropTarget(dropTarget);
			model.setDropTargetSet(true);
		} catch (Exception e) {
			LogService.getRoot().log(Level.WARNING,
					"com.rapidminer.gui.flow.processrendering.view.ProcessRendererView.drop_target_failed", e.getMessage());
		}

		// we need to know when the license changes because operators may become
		// supported/unsupported
		ProductConstraintManager.INSTANCE.registerLicenseManagerListener(new LicenseManagerListener() {

			@Override
			public <S, C> void handleLicenseEvent(final LicenseEvent<S, C> event) {
				if (event.getType() == LicenseEventType.ACTIVE_LICENSE_CHANGED) {
					ProcessRendererView.this.repaint();
				}
			}
		});

		// add some actions to the action map of this component
		mainFrame.getActions().TOGGLE_BREAKPOINT[BreakpointListener.BREAKPOINT_AFTER].
				addToActionMap(this, WHEN_FOCUSED);
		mainFrame.getActions().TOGGLE_ACTIVATION_ITEM.addToActionMap(this, WHEN_FOCUSED);
		selectAllAction.addToActionMap(this, WHEN_FOCUSED);
		OperatorTransferHandler.addToActionMap(this);
		deleteSelectedAction.addToActionMap(this, "delete", WHEN_FOCUSED);

		// add tooltips
		new ToolTipWindow(new ProcessRendererTooltipProvider(model), this, TooltipLocation.RIGHT);

		// add panning support to allow operators to extend and move the displayed process area when
		// dragged to the side/bottom
		new PanningManager(this);

		init();
	}

	@Override
	public void addNotify() {
		super.addNotify();
		// we do this here to avoid being overridden by main frame
		renameAction.addToActionMap(this, WHEN_FOCUSED);
	}

	@Override
	public void paintComponent(final Graphics graphics) {
		super.paintComponent(graphics);
		if (model.isDragStarted() || model.getConnectingPortSource() != null) {
			((Graphics2D) graphics).setRenderingHints(ProcessDrawer.LOW_QUALITY_HINTS);
		} else {
			((Graphics2D) graphics).setRenderingHints(ProcessDrawer.HI_QUALITY_HINTS);
		}
		Graphics2D g2 = (Graphics2D) graphics.create();
		getProcessDrawer().draw(g2, false);
		g2.dispose();
	}

	@Override
	public void printComponent(final Graphics graphics) {
		((Graphics2D) graphics).setRenderingHints(ProcessDrawer.HI_QUALITY_HINTS);
		getProcessDrawer().draw((Graphics2D) graphics.create(), true);
	}

	/**
	 * Return the index of the process under the given {@link Point2D view point}.
	 *
	 * @param p
	 *            the point
	 * @return the index or -1 if no process is under the point
	 * @see #getProcessIndexOfOperator(Operator)
	 */
	public int getProcessIndexUnder(final Point2D p) {
		if (p == null) {
			return -1;
		}

		if (p.getY() < 0 || p.getY() > controller.getTotalHeight()) {
			return -1;
		}
		int xOffset = 0;
		for (int i = 0; i < model.getProcesses().size(); i++) {
			int relativeX = (int) p.getX() - xOffset;
			if (relativeX >= 0 && relativeX <= model.getProcessWidth(model.getProcess(i))) {
				return i;
			}
			xOffset += ProcessDrawer.WALL_WIDTH * 2 + model.getProcessWidth(model.getProcess(i));
		}
		return -1;
	}

	/**
	 * Converts a {@link Point view point} to a point relative to the specified process.
	 *
	 * @param p
	 *            the original point
	 * @param processIndex
	 *            the index of the process
	 * @return the relative point or {@code null} if no process exists for the specified index
	 * @see ProcessRendererView#fromProcessSpace(Point, int)
	 */
	public Point toProcessSpace(final Point p, final int processIndex) {
		if (processIndex == -1 || processIndex >= model.getProcesses().size()) {
			return null;
		}
		int xOffset = getXOffset(processIndex);
		double zoomFactor = model.getZoomFactor();
		return new Point((int) ((p.getX() - xOffset) * (1 / zoomFactor)), (int) (p.getY() * (1 / zoomFactor)));
	}

	/**
	 * Returns the index of the process of the given {@link Operator}.
	 *
	 * @param op
	 *            the operator
	 * @return the index or -1 if the process is not currently shown
	 * @since 7.5
	 * @see #getProcessIndexUnder(Point2D)
	 */
	public int getProcessIndexOfOperator(Operator op) {
		if (op == null) {
			return -1;
		}
		ExecutionUnit eu = op.getExecutionUnit();
		return model.getProcessIndex(eu);
	}

	/**
	 * Converts a {@link Point} from the process to a point relative to the current view.
	 *
	 * @param p
	 *            the process point
	 * @param processIndex
	 *            the index of the process
	 * @return the view point or {@code null} if no process exists for the specified index
	 * @since 7.5
	 * @see #toProcessSpace(Point, int)
	 */
	public Point fromProcessSpace(final Point p, final int processIndex) {
		if (processIndex == -1 || processIndex >= model.getProcesses().size()) {
			return null;
		}
		int xOffset = getXOffset(processIndex);
		double zoomFactor = model.getZoomFactor();
		return new Point((int) (p.x * zoomFactor) + xOffset, (int) (p.y * zoomFactor));
	}

	/**
	 * Calculates the (absolute/view) x offset for the given process index. The index must be
	 * between 0 (inclusive) and the number of processes currently in the model (exclusive).
	 *
	 * @param processIndex
	 *            the index of the process
	 * @return the x offset before the specified process
	 * @since 7.5
	 * @see #toProcessSpace(Point, int)
	 * @see #fromProcessSpace(Point, int)
	 */
	private int getXOffset(final int processIndex) {
		List<ExecutionUnit> processes = model.getProcesses();
		int xOffset = processIndex * ProcessDrawer.WALL_WIDTH * 2;
		for (int i = 0; i < processIndex; i++) {
			xOffset += model.getProcessWidth(processes.get(i));
		}
		return xOffset;
	}

	/**
	 * Call when the process has been updated, i.e. an operator has been added. Will update operator
	 * locations and then repaint.
	 */
	public void processUpdated() {
		Operator hoveredOp = model.getHoveringOperator();
		boolean hoveredOperatorFound = hoveredOp == null ? true : false;
		List<Operator> movedOperators = new LinkedList<>();
		List<Operator> portChangedOperators = new LinkedList<>();

		// make sure location of every opterator is set and potentially reset hovered op
		for (ExecutionUnit unit : model.getProcesses()) {
			// check if all operators have positions, if not, set them now
			movedOperators = controller.ensureOperatorsHaveLocation(unit);

			for (Operator op : unit.getOperators()) {
				// check if number of ports has changed for any operators
				// otherwise we would not know that we need to check process size and repaint
				Integer formerNumber = model.getNumberOfPorts(op);
				Integer newNumber = op.getInputPorts().getNumberOfPorts() + op.getOutputPorts().getNumberOfPorts();
				if (formerNumber == null || !newNumber.equals(formerNumber)) {
					portChangedOperators.add(op);
					model.setNumberOfPorts(op, newNumber);
				}

				// if hovered operator has not yet been found, see if current one is it
				if (!hoveredOperatorFound && hoveredOp != null && hoveredOp.equals(op)) {
					hoveredOperatorFound = true;
				}
			}
		}

		for (ExecutionUnit unit : model.getProcesses()) {
			// check if number of ports has changed for any processes
			// otherwise we would not know that we need to check process size and repaint
			Operator op = unit.getEnclosingOperator();
			Integer formerNumber = model.getNumberOfPorts(op);
			Integer newNumber = op.getInputPorts().getNumberOfPorts() + op.getOutputPorts().getNumberOfPorts();
			if (formerNumber == null || !newNumber.equals(formerNumber)) {
				portChangedOperators.add(op);
				model.setNumberOfPorts(op, newNumber);
			}
		}

		// reset hovered operator if not in any process anymore
		if (!hoveredOperatorFound) {
			setHoveringOperator(null);
		}

		if (!movedOperators.isEmpty()) {
			model.fireOperatorsMoved(movedOperators);
		} else if (!portChangedOperators.isEmpty()) {
			model.firePortsChanged(portChangedOperators);
		}
	}

	@Override
	public void repaint() {
		if (repaintFilter != null) {
			repaintFilter.requestRepaint();
		} else {
			doRepaint();
		}
	}

	/**
	 * Does the repaint. Only call this method directly when there is a reason not to go through the
	 * {@link RepaintFilter}. Otherwise use {{@link #repaint()}.
	 */
	void doRepaint() {
		super.repaint();
		if (overviewPanel != null && overviewPanel.isShowing()) {
			overviewPanel.repaint();
		}
	}

	/**
	 * Adds a listener that will be informed when the user right-clicks an operator or a port.
	 *
	 * @param l
	 *            the listener
	 */
	public void addProcessInteractionListener(final ProcessInteractionListener l) {
		if (l == null) {
			throw new IllegalArgumentException("l must not be null!");
		}
		processInteractionListeners.add(l);
	}

	/**
	 * @see #addProcessInteractionListener(ProcessInteractionListener)
	 */
	public void removeProcessInteractionListener(final ProcessInteractionListener l) {
		if (l == null) {
			throw new IllegalArgumentException("l must not be null!");
		}
		processInteractionListeners.remove(l);
	}

	/**
	 * Returns the {@link OverviewPanel} for this instance.
	 *
	 * @return the overview panel, never {@code null}
	 */
	public OverviewPanel getOverviewPanel() {
		return overviewPanel;
	}

	@Override
	public Component getExportComponent() {
		return this;
	}

	@Override
	public String getExportIconName() {
		return I18N.getGUIMessage("gui.dockkey.process_panel.icon");
	}

	@Override
	public String getExportName() {
		return I18N.getGUIMessage("gui.dockkey.process_panel.name");
	}

	@Override
	public String getIdentifier() {
		Process process = RapidMinerGUI.getMainFrame().getProcess();
		if (process != null) {
			ProcessLocation processLocation = process.getProcessLocation();
			if (processLocation != null) {
				return processLocation.toString();
			}
		}
		return null;
	}

	/**
	 * Returns the {@link DragListener} for the process renderer.
	 *
	 * @return the listener, never {@code null}
	 */
	public DragListener getDragListener() {
		return dragListener;
	}

	/**
	 * Returns the {@link ProcessDrawer} which is responsible for drawing the process(es) in the
	 * {@link ProcessRendererView}.
	 *
	 * @return the drawer instance, never {@code null}
	 */
	public ProcessDrawer getProcessDrawer() {
		return drawer;
	}

	/**
	 * Returns the {@link ProcessDrawer} which is responsible for drawing the process(es) in the
	 * {@link OverviewPanel}.
	 *
	 * @return the drawer instance, never {@code null}
	 */
	public ProcessDrawer getOverviewPanelDrawer() {
		return drawerOverview;
	}

	/**
	 * Returns the {@link ProcessRendererModel} which is backing the GUI.
	 *
	 * @return the model instance, never {@code null}
	 */
	public ProcessRendererModel getModel() {
		return model;
	}

	/**
	 * Returns the action which automatically fits the process size to the existing operator
	 * locations.
	 *
	 * @return the action, never {@code null}
	 */
	public Action getAutoFitAction() {
		return autoFitAction;
	}

	/**
	 * Returns the action which automatically arranges all operators of the process according to a
	 * graph layout algorithm.
	 *
	 * @return the action, never {@code null}
	 */
	public Action getArrangeOperatorsAction() {
		return arrangeOperatorsAction;
	}

	/**
	 * Adds the given renderer decorator for the specified render phase.
	 * <p>
	 * To add a {@link ProcessDrawDecorator}, call {@link #getProcessDrawer()} and
	 * {@link ProcessDrawer#addDecorator(ProcessDrawDecorator, RenderPhase)} on it.
	 * </p>
	 *
	 * @param decorator
	 *            the decorator instance to add
	 * @param phase
	 *            the phase during which the decorator should be notified of events. If multiple
	 *            decorators want to handle events during the same phase, they are called in the
	 *            order they were registered. If any of the decorators in the chain consume the
	 *            event, the remaining decorators will <strong>not</strong> be notified!
	 */
	public void addEventDecorator(ProcessEventDecorator decorator, RenderPhase phase) {
		if (decorator == null) {
			throw new IllegalArgumentException("decorator must not be null!");
		}
		if (phase == null) {
			throw new IllegalArgumentException("phase must not be null!");
		}

		decorators.get(phase).add(decorator);
	}

	/**
	 * Removes the given decorator for the specified render phase. If the decorator has already been
	 * removed, does nothing.
	 * <p>
	 * To remove a {@link ProcessDrawDecorator}, call {@link #getProcessDrawer()} and
	 * {@link ProcessDrawer#removeDecorator(ProcessDrawDecorator, RenderPhase)} on it.
	 * </p>
	 *
	 * @param decorator
	 *            the decorator instance to remove
	 * @param phase
	 *            the phase from which the decorator should be removed
	 */
	public void removeEventDecorator(ProcessEventDecorator decorator, RenderPhase phase) {
		if (decorator == null) {
			throw new IllegalArgumentException("decorator must not be null!");
		}
		if (phase == null) {
			throw new IllegalArgumentException("phase must not be null!");
		}

		decorators.get(phase).remove(decorator);
	}

	/**
	 * Does the same as {@link ProcessDrawer#addDecorator(ProcessDrawDecorator, RenderPhase)}.
	 *
	 * @param decorator
	 *            the draw decorator
	 * @param phase
	 *            the specified phase in which to draw
	 */
	public void addDrawDecorator(ProcessDrawDecorator decorator, RenderPhase phase) {
		getProcessDrawer().addDecorator(decorator, phase);
	}

	/**
	 * Does the same as {@link ProcessDrawer#removeDecorator(ProcessDrawDecorator, RenderPhase)}.
	 *
	 * @param decorator
	 *            the draw decorator to add
	 * @param phase
	 *            the specified phase for which the decorator was registered
	 */
	public void removeDrawDecorator(ProcessDrawDecorator decorator, RenderPhase phase) {
		getProcessDrawer().removeDecorator(decorator, phase);
	}

	/**
	 * Does the same as {@link ProcessDrawer#addDecorator(OperatorDrawDecorator)}.
	 *
	 * @param decorator
	 *            the operator draw decorator
	 */
	public void addDrawDecorator(OperatorDrawDecorator decorator) {
		getProcessDrawer().addDecorator(decorator);
	}

	/**
	 * Does the same as {@link ProcessDrawer#removeDecorator(OperatorDrawDecorator)}.
	 *
	 * @param decorator
	 *            the operator draw decorator to remove
	 */
	public void removeDrawDecorator(OperatorDrawDecorator decorator) {
		getProcessDrawer().removeDecorator(decorator);
	}

	/**
	 * Opens a rename textfield at the location of the specified operator.
	 *
	 * @param op
	 *            the operator to be renamed
	 */
	public void rename(final Operator op) {
		if (op == null) {
			throw new IllegalArgumentException("op must not be null!");
		}

		int processIndex = controller.getIndex(op.getExecutionUnit());
		if (processIndex == -1) {
			String name = SwingTools.showInputDialog("rename_operator", op.getName());
			if (name != null && name.length() > 0) {
				op.rename(name);
			}
			return;
		}
		renameField = new JTextField(10);
		Rectangle2D rect = model.getOperatorRect(op);
		float fontSize = (float) (ProcessDrawer.OPERATOR_FONT.getSize() * model.getZoomFactor());
		Font nameFont = ProcessDrawer.OPERATOR_FONT.deriveFont(fontSize);
		int width;
		width = (int) nameFont.getStringBounds(op.getName(), ((Graphics2D) getGraphics()).getFontRenderContext()).getWidth();
		width = (int) Math.max(width, ProcessDrawer.OPERATOR_WIDTH * model.getZoomFactor());
		double xOffset = (ProcessDrawer.OPERATOR_WIDTH * model.getZoomFactor() - width) / 2;
		double yOffset = (ProcessDrawer.HEADER_HEIGHT * model.getZoomFactor() - RENAME_FIELD_HEIGHT) / 2;
		renameField.setHorizontalAlignment(SwingConstants.CENTER);
		renameField.setText(op.getName());
		renameField.selectAll();

		int x = (int) (rect.getX() * model.getZoomFactor());
		int y = (int) (rect.getY() * model.getZoomFactor());
		Point p = ProcessDrawUtils.convertToAbsoluteProcessPoint(new Point(x, y), processIndex, model);

		int padding = 7;
		renameField.setBounds((int) (p.getX() + xOffset - padding), (int) (p.getY() + yOffset - 1), width + padding * 2, RENAME_FIELD_HEIGHT);
		renameField.setFont(nameFont);
		renameField.setBorder(null);
		add(renameField);
		renameField.requestFocusInWindow();
		// accepting changes on enter and focus lost
		Runnable renamer = () -> {
			if (renameField == null) {
				return;
			}
			String name = renameField.getText().trim();
			if (name.length() > 0) {
				op.rename(name);
			}
			cancelRenaming();
		};
		renameField.addActionListener(e -> renamer.run());
		renameField.addFocusListener(new FocusAdapter() {

			@Override
			public void focusLost(final FocusEvent e) {
				// right-click menu
				if (e.isTemporary()) {
					return;
				}
				renamer.run();
			}
		});
		// ignore changes on escape
		renameField.addKeyListener(new KeyAdapter() {

			@Override
			public void keyReleased(final KeyEvent e) {
				if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
					cancelRenaming();
				}
			}
		});

		repaint();
	}

	/**
	 * Updates location and size of the extension buttons.
	 */
	void updateExtensionButtons() {
		for (ExtensionButton button : subprocessExtensionButtons) {
			int subprocessIndex = button.getSubprocessIndex();
			int buttonSize = button.getWidth();
			int gap = 2 * ProcessDrawer.WALL_WIDTH;
			if (subprocessIndex >= 0) {
				Point location = ProcessDrawUtils.convertToAbsoluteProcessPoint(new Point(0, 0), subprocessIndex, model);
				int height = (int) model.getProcessHeight(model.getProcess(subprocessIndex));
				int width = (int) model.getProcessWidth(model.getProcess(subprocessIndex));
				button.setBounds(location.x + width - buttonSize - gap - (button.isAdd() ? 0 : buttonSize),
						location.y + height - gap - buttonSize, buttonSize, buttonSize);
			} else {
				Point location = ProcessDrawUtils.convertToAbsoluteProcessPoint(new Point(0, 0), 0, model);
				int height = (int) model.getProcessHeight(model.getProcess(0));
				button.setBounds(location.x + gap, location.y + height - gap - buttonSize, buttonSize, buttonSize);
			}
		}
	}

	/**
	 * Shows a popup menu if preconditions are fulfilled.
	 *
	 * @param e
	 *            the mouse event potentially triggering the popup menu
	 * @return {@code true} if a popup menu was displayed; {@code false} otherwise
	 */
	boolean showPopupMenu(final MouseEvent e) {
		if (model.getConnectingPortSource() != null) {
			return false;
		}
		if (getProcessIndexUnder(e.getPoint()) == -1) {
			return false;
		}
		JPopupMenu menu = new JPopupMenu();

		// port or not port, that is the question
		final Port hoveringPort = model.getHoveringPort();
		OperatorChain displayedChain = model.getDisplayedChain();
		if (hoveringPort != null) {
			// add port actions
			final IOObject data = hoveringPort.getRawData();
			if (data instanceof ResultObject) {
				JMenuItem showResult = new JMenuItem(
						new ResourceAction(true, "show_port_data", ((ResultObject) data).getName()) {

							private static final long serialVersionUID = -6557085878445788274L;

							@Override
							public void loggedActionPerformed(final ActionEvent e) {
								final Operator operator = hoveringPort.getPorts().getOwner().getOperator();
								data.setSource(operator.getName());
								DeliveringPortManager.setLastDeliveringPort(data, hoveringPort);
								mainFrame.getResultDisplay().showResult((ResultObject) data);
							}

						});
				menu.add(showResult);
				try {
					String locationString = mainFrame.getProcess().getRepositoryLocation().getAbsoluteLocation();
					menu.add(new StoreInRepositoryAction(data, new RepositoryLocation(
							locationString.substring(0, locationString.lastIndexOf(RepositoryLocation.SEPARATOR)))));
				} catch (Exception e1) {
					menu.add(new StoreInRepositoryAction(data));
				}
				menu.addSeparator();
			}

			final List<ResourceAction> portActions = OperatorPortActionRegistry.INSTANCE.getPortActions(hoveringPort);
			for (ResourceAction action : portActions) {
				menu.add(action);
			}
			if (!portActions.isEmpty()) {
				menu.addSeparator();
			}

			List<QuickFix> fixes = hoveringPort.collectQuickFixes();
			if (!fixes.isEmpty()) {
				JMenu fixMenu = new ResourceMenu("quick_fixes");
				for (QuickFix fix : fixes) {
					fixMenu.add(fix.getAction());
				}
				menu.add(fixMenu);
			}
			if (hoveringPort.isConnected()) {
				menu.add(new ResourceAction(true, "disconnect") {

					private static final long serialVersionUID = 1L;

					@Override
					public void loggedActionPerformed(final ActionEvent e) {
						if (hoveringPort.isConnected()) {
							if (hoveringPort instanceof OutputPort) {
								((OutputPort) hoveringPort).disconnect();
							} else {
								((InputPort) hoveringPort).getSource().disconnect();
							}
						}
					}
				});
			}

			Ports<? extends Port> ports = hoveringPort.getPorts();
			ExecutionUnit subprocess = displayedChain.getSubprocess(0);
			if (displayedChain instanceof ProcessRootOperator &&
					(ports == subprocess.getInnerSources() || ports == subprocess.getInnerSinks())) {
				menu.add(new ConnectPortToRepositoryAction(hoveringPort));
			}
			firePortMenuWillOpen(menu, hoveringPort);
		} else if (model.getHoveringOperator() == null && model.getHoveringConnectionSource() != null) {
			// right-clicked a connection spline
			menu.add(new ResourceAction(true, "delete_connection") {

				private static final long serialVersionUID = 1L;

				@Override
				public void loggedActionPerformed(final ActionEvent e) {
					disconnectHoveredConnection(model);
				}
			});
		} else {
			// add workflow annotation and background image actions
			int index = model.getHoveringProcessIndex();
			Action[] annotationActions = new Action[4];
			final Operator hoveredOp = model.getHoveringOperator();

			// reset zoom action if clicking on background and zoom is set
			if (hoveredOp == null && model.getZoomFactor() != 1.0) {
				menu.add(new ResourceAction(true, "processrenderer.reset_zoom") {

					private static final long serialVersionUID = 1L;

					@Override
					public void loggedActionPerformed(ActionEvent e) {
						model.resetZoom();
						model.fireProcessZoomChanged();
					}
				});
				menu.addSeparator();
			}

			if (index != -1) {
				ExecutionUnit process = model.getProcess(index);
				Point point = toProcessSpace(e.getPoint(), index);
				if (hoveredOp == null) {
					annotationActions[0] = mainFrame.getProcessPanel().getAnnotationsHandler()
							.makeAddProcessAnnotationAction(process, point);
					annotationActions[1] = mainFrame.getProcessPanel().getAnnotationsHandler().getToggleAnnotationsAction();
					annotationActions[2] = mainFrame.getProcessPanel().getBackgroundImageHandler()
							.makeSetBackgroundImageAction(process);
					if (model.getBackgroundImage(process) != null) {
						annotationActions[3] = mainFrame.getProcessPanel().getBackgroundImageHandler()
								.makeRemoveBackgroundImageAction(process);
					}
				} else {
					WorkflowAnnotations annotations = model.getOperatorAnnotations(hoveredOp);
					if (annotations == null || annotations.isEmpty()) {
						annotationActions[0] = mainFrame.getProcessPanel().getAnnotationsHandler()
								.makeAddOperatorAnnotationAction(hoveredOp);
					} else {
						annotationActions[0] = mainFrame.getProcessPanel().getAnnotationsHandler()
								.makeDetachOperatorAnnotationAction(hoveredOp);
					}
				}
			}

			// add operator actions
			mainFrame.getActions().addToOperatorPopupMenu(menu, renameAction, annotationActions);

			// if not hovering on operator, add process panel actions
			if (hoveredOp == null) {
				menu.addSeparator();
				menu.add(mainFrame.getProcessPanel().getFlowVisualizer().ALTER_EXECUTION_ORDER.createMenuItem());

				JMenu layoutMenu = new ResourceMenu("process_layout");

				layoutMenu.add(new ResourceAction("arrange_operators") {

					private static final long serialVersionUID = 1L;

					@Override
					public void loggedActionPerformed(final ActionEvent ae) {
						int index = getProcessIndexUnder(e.getPoint());
						if (index == -1) {
							for (ExecutionUnit u : model.getProcesses()) {
								controller.autoArrange(u);
							}

						} else {
							controller.autoArrange(model.getProcess(index));
						}
					}
				});
				layoutMenu.add(autoFitAction);
				menu.add(layoutMenu);

				menu.addSeparator();
				String name = "Process";
				if (displayedChain.getProcess().getProcessLocation() != null) {
					name = displayedChain.getProcess().getProcessLocation().getShortName();
				}
				menu.add(PrintingTools.makeExportPrintMenu(this, name));
				fireOperatorMenuWillOpen(menu, displayedChain);
			} else {
				boolean first = true;
				for (OutputPort port : hoveredOp.getOutputPorts().getAllPorts()) {
					final IOObject data = port.getRawData();
					if (data instanceof ResultObject) {
						if (first) {
							menu.addSeparator();
							first = false;
						}
						JMenuItem showResult = new JMenuItem(
								new ResourceAction(true, "show_port_data", ((ResultObject) data).getName()) {

									private static final long serialVersionUID = -6557085878445788274L;

									@Override
									public void loggedActionPerformed(final ActionEvent e) {
										data.setSource(hoveredOp.getName());
										DeliveringPortManager.setLastDeliveringPort(data, port);
										mainFrame.getResultDisplay().showResult((ResultObject) data);
									}
								});
						menu.add(showResult);
					}
				}
				fireOperatorMenuWillOpen(menu, hoveredOp);
			}
		}

		// show popup
		if (menu.getSubElements().length > 0) {
			menu.show(this, e.getX(), e.getY());
		}
		return true;

	}

	/**
	 * Disconnects the hovered connection.
	 *
	 * @param model the {@link ProcessRendererModel}
	 * @since 8.2
	 */
	static void disconnectHoveredConnection(ProcessRendererModel model) {
		OutputPort port = model.getHoveringConnectionSource();
		if (port == null || !port.isConnected()) {
			return;
		}
		port.disconnect();
		model.setHoveringConnectionSource(null);
		if (port.equals(model.getSelectedConnectionSource())) {
			model.setSelectedConnectionSource(null);
		}
		model.fireMiscChanged();
	}

	/**
	 * Updates the currently displayed cursor depending on hover state.
	 */
	void updateCursor() {
		if (model.isHoveringOperatorName()) {
			setCursor(Cursor.getPredefinedCursor(Cursor.TEXT_CURSOR));
		} else if (model.getHoveringOperator() != null || model.getHoveringPort() != null) {
			setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		} else {
			setCursor(Cursor.getDefaultCursor());
		}
	}

	/**
	 * Sets the hovering operator and updates the operator name rollout and the cursor.
	 *
	 * @param hoveringOperator
	 *            the operator or {@code null}
	 */
	void setHoveringOperator(final Operator hoveringOperator) {
		model.setHoveringOperator(hoveringOperator);
		updateCursor();
		model.fireMiscChanged();
	}

	/**
	 * Removes the rename textfield and resets the focus to the view.
	 */
	private void cancelRenaming() {
		if (renameField != null) {
			remove(renameField);
			renameField = null;
			// this makes sure that pressing F2 afterwards works
			// otherwise nothing is focused until the next click
			ProcessRendererView.this.requestFocusInWindow();
			repaint();
		}
	}

	/**
	 * Inits event listeners and GUI dimensions.
	 */
	private void init() {
		addMouseMotionListener(mouseHandler);
		addMouseListener(mouseHandler);
		addMouseWheelListener(mouseHandler);
		addKeyListener(keyHandler);

		// for absolute positioning of tipPane
		setLayout(null);

		setPreferredSize(new Dimension(1000, 440));
		setMinimumSize(new Dimension(100, 100));
		setMaximumSize(new Dimension(2000, 2000));
	}

	/**
	 * Sets up the buttons which can be used to add/remove subprocesses of the currently displayed
	 * operator chain.
	 */
	private void setupExtensionButtons() {
		for (ExtensionButton button : subprocessExtensionButtons) {
			remove(button);
		}
		subprocessExtensionButtons.clear();
		if (model.getDisplayedChain().areSubprocessesExtendable()) {
			for (int index = 0; index < model.getProcesses().size(); index++) {
				double width = model.getProcessWidth(model.getProcess(index)) + 1;
				Point loc = ProcessDrawUtils.convertToAbsoluteProcessPoint(new Point(0, 0), index, model);

				if (index == 0) {
					ExtensionButton addButton2 = new ExtensionButton(model, model.getDisplayedChain(), -1, true);
					addButton2.setBounds((int) (loc.getX() - addButton2.getPreferredSize().getWidth() + 1),
							(int) (loc.getY() - 1), (int) addButton2.getPreferredSize().getWidth(),
							(int) addButton2.getPreferredSize().getHeight());
					subprocessExtensionButtons.add(addButton2);
					add(addButton2);
				}

				ExtensionButton addButton = new ExtensionButton(model, model.getDisplayedChain(), index, true);
				addButton.setBounds((int) (loc.getX() + width), (int) (loc.getY() - 1),
						(int) addButton.getPreferredSize().getWidth(), (int) addButton.getPreferredSize().getHeight());
				subprocessExtensionButtons.add(addButton);
				add(addButton);

				if (model.getProcesses().size() > 1) {
					ExtensionButton deleteButton = new ExtensionButton(model, model.getDisplayedChain(), index, false);
					deleteButton.setBounds((int) (loc.getX() + width), (int) (loc.getY() + addButton.getHeight() - 1),
							(int) deleteButton.getPreferredSize().getWidth(),
							(int) deleteButton.getPreferredSize().getHeight());
					subprocessExtensionButtons.add(deleteButton);
					add(deleteButton);
				}
			}
		}
	}

	/**
	 * Update preferred size of this {@link JComponent} and updates the subprocess extension buttons
	 * as well.
	 */
	private void updateComponentSize() {
		Dimension newSize = new Dimension((int) controller.getTotalWidth(), (int) controller.getTotalHeight());
		updateExtensionButtons();
		if (!newSize.equals(getPreferredSize())) {
			setPreferredSize(newSize);
			revalidate();
		}
	}

	/**
	 * Notifies listeners that the operator context menu will be opened.
	 *
	 * @param m
	 *            the menu instance
	 * @param op
	 *            the operator for which the menu will open
	 */
	private void fireOperatorMenuWillOpen(final JPopupMenu m, final Operator op) {
		List<ProcessInteractionListener> copy = new LinkedList<>(processInteractionListeners);
		for (ProcessInteractionListener l : copy) {
			l.operatorContextMenuWillOpen(m, op);
		}
	}

	/**
	 * Notifies listeners that the port context menu will be opened.
	 *
	 * @param m
	 *            the menu instance
	 * @param port
	 *            the port for which the menu will open
	 */
	private void firePortMenuWillOpen(final JPopupMenu m, final Port port) {
		List<ProcessInteractionListener> copy = new LinkedList<>(processInteractionListeners);
		for (ProcessInteractionListener l : copy) {
			l.portContextMenuWillOpen(m, port);
		}
	}

	/**
	 * Notifies listeners that an operator has moved.
	 *
	 * @param op
	 *            the operator that moved
	 */
	private void fireOperatorMoved(final Operator op) {
		List<ProcessInteractionListener> copy = new LinkedList<>(processInteractionListeners);
		for (ProcessInteractionListener l : copy) {
			l.operatorMoved(op);
		}
	}

	/**
	 * Notifies listeners that the displayed operator chain has changed.
	 *
	 * @param op
	 *            the new displayed chain
	 */
	private void fireDisplayedChainChanged(final OperatorChain op) {
		List<ProcessInteractionListener> copy = new LinkedList<>(processInteractionListeners);
		for (ProcessInteractionListener l : copy) {
			l.displayedChainChanged(op);
		}
	}

	/**
	 * Lets all registered {@link ProcessEventDecorator}s process the mouse event for the given
	 * {@link RenderPhase}. If the event is consumed by any decorator, processing will stop.
	 *
	 * @param type
	 *            the type of the mouse event
	 * @param e
	 *            the event itself
	 * @param phase
	 *            the event phase we are in
	 * @return {@code true} if the event was consumed; {@code false} otherwise
	 */
	private boolean processPhaseListenerMouseEvent(final MouseEventType type, final MouseEvent e, RenderPhase phase) {
		int hoverIndex = model.getHoveringProcessIndex();
		ExecutionUnit hoveredProcess = null;
		if (hoverIndex >= 0 && hoverIndex < model.getProcesses().size()) {
			hoveredProcess = model.getProcess(hoverIndex);
		}

		for (ProcessEventDecorator decorater : decorators.get(phase)) {
			try {
				decorater.processMouseEvent(hoveredProcess, type, e);
			} catch (RuntimeException e1) {
				// catch everything here
				LogService.getRoot().log(Level.WARNING,
						"com.rapidminer.gui.flow.processrendering.view.ProcessRendererView.decorator_error", e1);
			}

			// if the decorator consumed the event, it no longer makes sense to use it.
			if (e.isConsumed()) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Lets all registered {@link ProcessEventDecorator}s process the key event for the given
	 * {@link RenderPhase}. If the event is consumed by any decorator, processing will stop.
	 *
	 * @param type
	 *            the type of the key event
	 * @param e
	 *            the event itself
	 * @param phase
	 *            the event phase we are in
	 * @return {@code true} if the event was consumed; {@code false} otherwise
	 */
	private boolean processPhaseListenerKeyEvent(final KeyEventType type, final KeyEvent e, RenderPhase phase) {
		int hoverIndex = model.getHoveringProcessIndex();
		ExecutionUnit hoveredProcess = null;
		if (hoverIndex >= 0 && hoverIndex < model.getProcesses().size()) {
			hoveredProcess = model.getProcess(hoverIndex);
		}

		for (ProcessEventDecorator decorater : decorators.get(phase)) {
			try {
				decorater.processKeyEvent(hoveredProcess, type, e);
			} catch (RuntimeException e1) {
				// catch everything here
				LogService.getRoot().log(Level.WARNING,
						"com.rapidminer.gui.flow.processrendering.view.ProcessRendererView.decorator_error", e1);
			}
			// if the decorator consumed the event, it no longer makes sense to use it.
			if (e.isConsumed()) {
				return true;
			}
		}
		return false;
	}
}
