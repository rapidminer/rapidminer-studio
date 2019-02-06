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
package com.rapidminer.gui.flow.processrendering.draw;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.AffineTransform;
import java.awt.geom.Arc2D;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;
import javax.swing.ImageIcon;

import com.rapidminer.BreakpointListener;
import com.rapidminer.Process;
import com.rapidminer.gui.MainFrame;
import com.rapidminer.gui.RapidMinerGUI;
import com.rapidminer.gui.RapidMinerGUI.DragHighlightMode;
import com.rapidminer.gui.animation.Animation;
import com.rapidminer.gui.animation.ProcessAnimationManager;
import com.rapidminer.gui.flow.processrendering.model.ProcessRendererModel;
import com.rapidminer.gui.flow.processrendering.view.ProcessRendererView;
import com.rapidminer.gui.flow.processrendering.view.RenderPhase;
import com.rapidminer.gui.look.Colors;
import com.rapidminer.gui.look.RapidLookAndFeel;
import com.rapidminer.gui.tools.SwingTools;
import com.rapidminer.io.process.GUIProcessXMLFilter;
import com.rapidminer.operator.ExecutionUnit;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorChain;
import com.rapidminer.operator.ProcessRootOperator;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.operator.ports.InputPorts;
import com.rapidminer.operator.ports.OutputPort;
import com.rapidminer.operator.ports.OutputPorts;
import com.rapidminer.operator.ports.Port;
import com.rapidminer.operator.ports.Ports;
import com.rapidminer.operator.ports.metadata.CollectionMetaData;
import com.rapidminer.operator.ports.metadata.Precondition;
import com.rapidminer.tools.FontTools;
import com.rapidminer.tools.I18N;
import com.rapidminer.tools.LogService;
import com.rapidminer.tools.OperatorService;
import com.rapidminer.tutorial.Tutorial;


/**
 * This class does the actual Java2D drawing for the {@link ProcessRendererView}.
 *
 * @author Marco Boeck
 * @since 6.4.0
 *
 */
public final class ProcessDrawer {

	public static final Font OPERATOR_FONT = ProcessRendererModel.OPERATOR_FONT;

	public static final int OPERATOR_WIDTH = ProcessRendererModel.OPERATOR_WIDTH;
	public static final int OPERATOR_MIN_HEIGHT = ProcessRendererModel.MIN_OPERATOR_HEIGHT;
	public static final int HEADER_HEIGHT = ProcessRendererModel.HEADER_HEIGHT;
	public static final int PORT_SIZE = ProcessRendererModel.PORT_SIZE;
	public static final int PORT_SIZE_HIGHLIGHT = (int) (PORT_SIZE * 1.4f);
	public static final int PORT_OFFSET = OPERATOR_FONT.getSize() + 6 + PORT_SIZE;
	public static final int WALL_WIDTH = 3;

	public static final int PROCESS_BOX_SHADOW_OFFSET_X = 4;
	public static final int PROCESS_BOX_SHADOW_OFFSET_Y = 4;

	public static final int GRID_WIDTH = OPERATOR_WIDTH * 3 / 4;
	public static final int GRID_HEIGHT = OPERATOR_MIN_HEIGHT * 3 / 4;
	public static final int GRID_X_OFFSET = OPERATOR_WIDTH / 2;
	public static final int GRID_Y_OFFSET = OPERATOR_MIN_HEIGHT / 2;
	public static final int GRID_AUTOARRANGE_WIDTH = OPERATOR_WIDTH * 3 / 2;
	public static final int GRID_AUTOARRANGE_HEIGHT = OPERATOR_MIN_HEIGHT * 3 / 2;

	/** rendering hints which enable general and text anti aliasing */
	public static final RenderingHints HI_QUALITY_HINTS = new RenderingHints(null);

	/** rendering hints which disable general and text anti aliasing */
	public static final RenderingHints LOW_QUALITY_HINTS = new RenderingHints(null);

	static {
		HI_QUALITY_HINTS.put(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		HI_QUALITY_HINTS.put(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

		LOW_QUALITY_HINTS.put(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		LOW_QUALITY_HINTS.put(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
	}

	public static final Color INNER_DRAG_COLOR = new Color(230, 242, 255, 150);
	public static final Color BORDER_DRAG_COLOR = new Color(200, 200, 200, 200);
	public static final Color OPERATOR_BORDER_COLOR_SELECTED = new Color(255, 102, 0);

	private static final Color INNER_COLOR = Color.WHITE;
	private static final Color SHADOW_COLOR = Color.LIGHT_GRAY;

	private static final int OPERATOR_CORNER = (int) (RapidLookAndFeel.CORNER_DEFAULT_RADIUS * 0.67);
	private static final int OPERATOR_BG_CORNER = RapidLookAndFeel.CORNER_DEFAULT_RADIUS;

	private static final Color BORDER_COLOR = Colors.TAB_BORDER;
	private static final Color HINT_COLOR = new Color(230, 230, 230);

	private static final Color LINE_COLOR = Color.DARK_GRAY;
	private static final Stroke LINE_STROKE = new BasicStroke(1f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
	private static final Stroke PORT_STROKE = new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
	private static final Stroke PORT_HIGHLIGHT_STROKE = new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
	private static final Stroke SELECTION_RECT_STROKE = new BasicStroke(1f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND,
			5f, new float[]{2f, 2f}, 0f);
	private static final Paint SELECTION_RECT_PAINT = Color.GRAY;
	private static final Color PROCESS_TITLE_COLOR = SHADOW_COLOR;
	private static final Stroke BORDER_DRAG_STROKE = new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_MITER,
			10.0f, new float[]{10.0f}, 0.0f);
	private static final int DRAG_BORDER_PADDING = 30;
	private static final int DRAG_BORDER_CORNER = 15;

	private static final Font PROCESS_FONT = FontTools.getFont(Font.DIALOG, Font.BOLD, 12);
	private static final Font DRAG_FONT_LARGE = FontTools.getFont(Font.DIALOG, Font.BOLD, 30);
	private static final Font DRAG_FONT_MEDIUM = FontTools.getFont(Font.DIALOG, Font.BOLD, 22);
	private static final Font DRAG_FONT_SMALL = FontTools.getFont(Font.DIALOG, Font.BOLD, 14);
	private static final Font HINT_FONT_LARGE = FontTools.getFont(Font.DIALOG, Font.BOLD, 30);
	private static final Font HINT_FONT_MEDIUM = FontTools.getFont(Font.DIALOG, Font.BOLD, 24);
	private static final Font HINT_FONT_SMALL = FontTools.getFont(Font.DIALOG, Font.BOLD, 18);

	private static final Font PORT_FONT = FontTools.getFont(Font.DIALOG, Font.PLAIN, 9);
	private static final Color PORT_NAME_COLOR = Color.DARK_GRAY;
	private static final Color PORT_NAME_SELECTION_COLOR = Color.GRAY;
	private static final Color ACTIVE_EDGE_COLOR = new Color(255, 102, 0);

	private static final Stroke OPERATOR_STROKE_SELECTED = new BasicStroke(1.5f, BasicStroke.CAP_ROUND,
			BasicStroke.JOIN_ROUND);
	private static final Stroke OPERATOR_STROKE_HIGHLIGHT = new BasicStroke(1.25f, BasicStroke.CAP_ROUND,
			BasicStroke.JOIN_ROUND);
	private static final Stroke OPERATOR_STROKE_NORMAL = new BasicStroke(1f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);

	private static final Color OPERATOR_BORDER_COLOR = new Color(129, 129, 129);
	private static final Color OPERATOR_BORDER_COLOR_HIGHLIGHT = new Color(100, 100, 100);

	private static final Color OPERATOR_NAME_COLOR = new Color(51, 51, 51);
	private static final Color OPERATOR_NAME_COLOR_HIGHLIGHT = new Color(85, 85, 85);
	private static final Color OPERATOR_NAME_COLOR_SELECTED = OPERATOR_BORDER_COLOR_SELECTED;
	private static final Color OPERATOR_NAME_COLOR_DISABLED = new Color(150, 150, 150);

	private static final Stroke CONNECTION_LINE_STROKE = new BasicStroke(1.3f, BasicStroke.CAP_ROUND,
			BasicStroke.JOIN_ROUND);
	private static final Stroke CONNECTION_HIGHLIGHT_STROKE = new BasicStroke(2.2f, BasicStroke.CAP_ROUND,
			BasicStroke.JOIN_ROUND);
	private static final Stroke CONNECTION_LINE_BACKGROUND_STROKE = new BasicStroke(7f, BasicStroke.CAP_ROUND,
			BasicStroke.JOIN_ROUND);
	private static final Stroke CONNECTION_COLLECTION_LINE_STROKE = new BasicStroke(3f, BasicStroke.CAP_ROUND,
			BasicStroke.JOIN_ROUND);
	private static final Stroke CONNECTION_COLLECTION_HIGHLIGHT_STROKE = new BasicStroke(4f, BasicStroke.CAP_ROUND,
			BasicStroke.JOIN_ROUND);
	private static final Stroke CONNECTION_COLLECTION_LINE_BACKGROUND_STROKE = new BasicStroke(12f, BasicStroke.CAP_ROUND,
			BasicStroke.JOIN_ROUND);

	private static final String HINT_EMPTY_PROCESS_1 = I18N.getGUIMessage("gui.label.processRenderer.empty_hint_1.label");
	private static final String HINT_EMPTY_PROCESS_2 = I18N.getGUIMessage("gui.label.processRenderer.empty_hint_2.label");
	private static final String HINT_EMPTY_PROCESS_3 = I18N.getGUIMessage("gui.label.processRenderer.empty_hint_3.label");
	private static final String DRAG_HERE = I18N.getGUIMessage("gui.label.processRenderer.drag_here.label");
	private static final String DROP_HERE = I18N.getGUIMessage("gui.label.processRenderer.drop_here.label");
	private static final Color DRAG_BG_COLOR = BORDER_DRAG_COLOR;
	private static final Color DRAG_FG_COLOR = new Color(254, 254, 254);

	private static final int PROCESS_TITLE_PADDING = 10;

	private static final double PORT_SIZE_BACKGROUND = PORT_SIZE * 2f;
	private static final double MAX_HEADER_RATIO = 1.35;

	private static final ImageIcon IMAGE_WARNING = SwingTools.createIcon("16/sign_warning2.png");
	private static final ImageIcon IMAGE_WARNING_ZOOMED = SwingTools.createIcon("32/sign_warning2.png");
	private static final ImageIcon IMAGE_BREAKPOINT_WITHIN = SwingTools.createIcon("16/breakpoint.png");
	private static final ImageIcon IMAGE_BREAKPOINT_WITHIN_ZOOMED = SwingTools.createIcon("32/breakpoint.png");
	private static final ImageIcon IMAGE_BREAKPOINTS = SwingTools.createIcon("16/breakpoints.png");
	private static final ImageIcon IMAGE_BREAKPOINTS_ZOOMED = SwingTools.createIcon("32/breakpoints.png");
	private static final ImageIcon IMAGE_BREAKPOINT_BEFORE = SwingTools.createIcon("16/breakpoint_left.png");
	private static final ImageIcon IMAGE_BREAKPOINT_BEFORE_ZOOMED = SwingTools.createIcon("32/breakpoint_left.png");
	private static final ImageIcon IMAGE_BREAKPOINT_AFTER = SwingTools.createIcon("16/breakpoint_right.png");
	private static final ImageIcon IMAGE_BREAKPOINT_AFTER_ZOOMED = SwingTools.createIcon("32/breakpoint_right.png");
	private static final ImageIcon IMAGE_BREAKPOINT_WITHIN_LARGE = SwingTools.createIcon("24/breakpoint.png");
	private static final ImageIcon IMAGE_BREAKPOINTS_LARGE = SwingTools.createIcon("24/breakpoints.png");
	private static final ImageIcon IMAGE_BREAKPOINT_BEFORE_LARGE = SwingTools.createIcon("24/breakpoint_left.png");
	private static final ImageIcon IMAGE_BREAKPOINT_AFTER_LARGE = SwingTools.createIcon("24/breakpoint_right.png");
	private static final ImageIcon IMAGE_BLACKLISTED = SwingTools.createIcon("16/lock.png");
	private static final ImageIcon IMAGE_BLACKLISTED_ZOOMED = SwingTools.createIcon("32/lock.png");

	private static final ImageIcon OPERATOR_RUNNING = SwingTools.createIcon("16/media_play2.png");
	private static final ImageIcon OPERATOR_RUNNING_ZOOMED = SwingTools.createIcon("32/media_play2.png");
	private static final ImageIcon OPERATOR_READY = SwingTools.createIcon("16/check.png");
	private static final ImageIcon OPERATOR_READY_ZOOMED = SwingTools.createIcon("32/check.png");

	/** the size of the operator icon */
	private static final int OPERATOR_ICON_SIZE = 24;

	/** the model backing the process renderer view */
	private ProcessRendererModel model;

	/** indicates if drop highlighting should be rendered */
	private boolean drawHighlight;

	/** the list of draw decorators */
	private Map<RenderPhase, CopyOnWriteArrayList<ProcessDrawDecorator>> decorators;

	/** the list of operator decorators */
	private CopyOnWriteArrayList<OperatorDrawDecorator> operatorDecorators;

	/**
	 * Creates a new drawer instance which can be used to draw the process specified in the model.
	 *
	 * @param model
	 * 		the model containing the data needed to draw the process. See
	 * 		{@link ProcessRendererModel} for a minimal configuration
	 * @param drawHighlight
	 * 		if {@code true} will highlight drop area in the process during drag & drop
	 */
	public ProcessDrawer(final ProcessRendererModel model, final boolean drawHighlight) {
		if (model == null) {
			throw new IllegalArgumentException("model must not be null!");
		}

		this.model = model;
		this.drawHighlight = drawHighlight;

		// prepare decorators for each phase
		decorators = new HashMap<>();
		for (RenderPhase phase : RenderPhase.drawOrder()) {
			decorators.put(phase, new CopyOnWriteArrayList<ProcessDrawDecorator>());
		}

		// prepare operator decorators
		operatorDecorators = new CopyOnWriteArrayList<OperatorDrawDecorator>();
	}

	/**
	 * Draws the entire process(es) based on the {@link ProcessRendererModel} data. Registered
	 * decorators are called during their respective {@link RenderPhase}s.
	 *
	 * @param g2
	 * 		the graphics context to draw upon
	 * @param printing
	 * 		if {@code true} we are printing instead of drawing to the screen
	 */
	public void draw(final Graphics2D g2, final boolean printing) {
		Graphics2D g = (Graphics2D) g2.create();
		double zoomFactor = model.getZoomFactor();

		// draw every process
		for (ExecutionUnit process : model.getProcesses()) {
			// draw background in own graphics context to avoid cross-phase manipulations
			Graphics2D gBG = (Graphics2D) g.create();
			drawBackground(process, gBG, printing);
			gBG.dispose();

			// remember non-scaled transform
			AffineTransform at = g.getTransform();
			g.scale(zoomFactor, zoomFactor);

			// let decorators draw
			Graphics2D gBGD = (Graphics2D) g.create();
			drawPhaseDecorators(process, gBGD, RenderPhase.BACKGROUND, printing);
			gBGD.dispose();

			// draw annotations in own graphics context to avoid cross-phase manipulations
			Graphics2D gAN = (Graphics2D) g.create();
			drawAnnotations(process, gAN, printing);
			gAN.dispose();

			// draw operator annotations in own graphics context to avoid cross-phase manipulations
			Graphics2D gON = (Graphics2D) g.create();
			drawOperatorAnnotations(process, gON, printing);
			gON.dispose();

			// draw operator backgrounds in own graphics context to avoid cross-phase manipulations
			Graphics2D gOB = (Graphics2D) g.create();
			drawOperatorBackgrounds(process, gOB, printing);
			gOB.dispose();

			// draw connections in own graphics context to avoid cross-phase manipulations
			Graphics2D gCO = (Graphics2D) g.create();
			drawConnections(process, gCO, printing);
			gCO.dispose();

			// draw operators in own graphics context to avoid cross-phase manipulations
			Graphics2D gOP = (Graphics2D) g.create();
			drawOperators(process, gOP, printing);
			gOP.dispose();

			// draw operator additions in own graphics context to avoid cross-phase manipulations
			Graphics2D gOA = (Graphics2D) g.create();
			drawOperatorAdditions(process, gOA, printing);
			gOA.dispose();

			// draw overlay in own graphics context to avoid cross-phase manipulations
			Graphics2D gOL = (Graphics2D) g.create();
			drawOverlay(process, gOL, printing);
			gOL.dispose();

			// let decorators draw
			Graphics2D gFG = (Graphics2D) g.create();
			drawPhaseDecorators(process, gFG, RenderPhase.FOREGROUND, printing);
			gFG.dispose();

			// restore non-scaled transform
			g.setTransform(at);

			// draw foreground in own graphics context to avoid cross-phase manipulations
			Graphics2D gFGF = (Graphics2D) g.create();
			drawForeground(process, gFGF, printing);
			gFGF.dispose();

			g.translate(model.getProcessWidth(process) + WALL_WIDTH * 2, 0);
		}
		g.dispose();

		// draw wall element over execution engines
		Graphics2D gW = (Graphics2D) g2.create();
		for (int i = 0; i < model.getProcesses().size() - 1; i++) {
			ExecutionUnit process = model.getProcesses().get(i);
			renderProcessWall(process, gW);
			gW.translate(model.getProcessWidth(process) + WALL_WIDTH * 2, 0);
		}

		// draw port connection attached to mouse over everything
		Port portSource = model.getConnectingPortSource();
		if (portSource != null && model.getMousePositionRelativeToProcess() != null) {
			Graphics2D conG = (Graphics2D) g2.create();
			// translate to correct position
			int width = 0;
			int index = model.getProcessIndex(portSource.getPorts().getOwner().getConnectionContext());
			for (int i = 0; i < index; i++) {
				width += model.getProcessWidth(model.getProcess(i)) + WALL_WIDTH * 2;
			}

			conG.translate(width, 0);
			conG.scale(zoomFactor, zoomFactor);

			conG.setColor(ACTIVE_EDGE_COLOR);
			Point2D fromLoc = ProcessDrawUtils.createPortLocation(portSource, model);
			if (fromLoc != null) {
				double x = portSource instanceof OutputPort ? fromLoc.getX() + PORT_SIZE_HIGHLIGHT / 2
						: fromLoc.getX() - PORT_SIZE_HIGHLIGHT / 2;
				conG.draw(new Line2D.Double(x, fromLoc.getY(), model.getMousePositionRelativeToProcess().getX(),
						model.getMousePositionRelativeToProcess().getY()));
			}
			conG.dispose();
		}

		// draw selection rectangle over everything
		if (model.getSelectionRectangle() != null) {
			Graphics2D selG = (Graphics2D) g2.create();
			selG.setPaint(SELECTION_RECT_PAINT);
			selG.setStroke(SELECTION_RECT_STROKE);
			selG.draw(model.getSelectionRectangle());
			selG.dispose();
		}
	}

	/**
	 * Draws a wall (grey bar) right to the process box. Used for operator chains with multiple
	 * units.
	 *
	 * @param process
	 * 		the process to draw the wall for
	 * @param g2
	 * 		the graphics context to draw on
	 */
	private void renderProcessWall(ExecutionUnit process, Graphics2D g2) {
		double width = model.getProcessWidth(process);
		double height = model.getProcessHeight(process);
		Shape wall = new Rectangle2D.Double(width, -10, 2 * WALL_WIDTH - 1, height + 20);

		g2.setColor(Colors.WINDOW_BACKGROUND);
		g2.fill(wall);

		g2.setColor(Colors.TEXTFIELD_BORDER);
		g2.draw(wall);
	}

	/**
	 * Draws the process(es) background based on the {@link ProcessRendererModel} data and then
	 * calls all registered {@link ProcessDrawDecorator}s for the background render phase.
	 *
	 * @param process
	 * 		the process to draw the background for
	 * @param g2
	 * 		the graphics context to draw upon
	 * @param printing
	 * 		if {@code true} we are printing instead of drawing to the screen
	 */
	public void drawBackground(final ExecutionUnit process, final Graphics2D g2, final boolean printing) {
		Graphics2D gBG = (Graphics2D) g2.create();
		renderBackground(process, gBG, printing);
		gBG.dispose();
	}

	/**
	 * Draws process annotations and then calls all registered {@link ProcessDrawDecorator}s for the
	 * annotations render phase.
	 *
	 * @param process
	 * 		the process to draw the annotations for
	 * @param g2
	 * 		the graphics context to draw upon
	 * @param printing
	 * 		if {@code true} we are printing instead of drawing to the screen
	 */
	public void drawAnnotations(final ExecutionUnit process, final Graphics2D g2, final boolean printing) {
		// let decorators draw
		drawPhaseDecorators(process, g2, RenderPhase.ANNOTATIONS, printing);
	}

	/**
	 * Draws operator backgrounds and then calls all registered {@link ProcessDrawDecorator}s for
	 * the annotations render phase.
	 *
	 * @param process
	 * 		the process to draw the operator backgrounds for
	 * @param g2
	 * 		the graphics context to draw upon
	 * @param printing
	 * 		if {@code true} we are printing instead of drawing to the screen
	 */
	public void drawOperatorBackgrounds(final ExecutionUnit process, final Graphics2D g2, final boolean printing) {
		Graphics2D gBG = (Graphics2D) g2.create();
		// draw background of operators
		for (Operator op : process.getOperators()) {
			Rectangle2D frame = model.getOperatorRect(op);
			if (frame == null) {
				continue;
			}

			// only draw background if operator is visisble
			Rectangle2D opBounds = new Rectangle2D.Double(frame.getX() - 10, frame.getY(), frame.getWidth() + 20,
					frame.getHeight());
			if (g2.getClipBounds() != null && !g2.getClipBounds().intersects(opBounds)) {
				continue;
			}

			renderOperatorBackground(op, gBG);
		}

		// draw connections background for all operators
		for (Operator operator : process.getOperators()) {
			renderConnectionsBackground(operator.getInputPorts(), operator.getOutputPorts(), gBG);
		}

		// draw connections background for process
		renderConnectionsBackground(process.getInnerSinks(), process.getInnerSources(), gBG);
		gBG.dispose();

		// let decorators draw
		drawPhaseDecorators(process, g2, RenderPhase.OPERATOR_BACKGROUND, printing);
	}

	/**
	 * Draws the process(es) operator connections based on the {@link ProcessRendererModel} data and
	 * then calls all registered {@link ProcessDrawDecorator}s for the connections render phase.
	 *
	 * @param process
	 * 		the process to draw the connections for
	 * @param g2
	 * 		the graphics context to draw upon
	 * @param printing
	 * 		if {@code true} we are printing instead of drawing to the screen
	 */
	public void drawConnections(final ExecutionUnit process, final Graphics2D g2, final boolean printing) {
		Graphics2D gCo = (Graphics2D) g2.create();
		// draw connections for all operators
		for (Operator operator : process.getOperators()) {
			renderConnections(operator.getOutputPorts(), gCo);
		}

		// draw connections for process
		renderConnections(process.getInnerSources(), gCo);
		gCo.dispose();

		// let decorators draw
		drawPhaseDecorators(process, g2, RenderPhase.CONNECTIONS, printing);
	}

	/**
	 * Draws operator annotations and then calls all registered {@link ProcessDrawDecorator}s for
	 * the annotations render phase.
	 *
	 * @param process
	 * 		the process to draw the operator annotations for
	 * @param g2
	 * 		the graphics context to draw upon
	 * @param printing
	 * 		if {@code true} we are printing instead of drawing to the screen
	 */
	public void drawOperatorAnnotations(final ExecutionUnit process, final Graphics2D g2, final boolean printing) {
		// let decorators draw
		drawPhaseDecorators(process, g2, RenderPhase.OPERATOR_ANNOTATIONS, printing);
	}

	/**
	 * Draws the process operators based on the {@link ProcessRendererModel} data including their
	 * ports and the ports for the surrounding process and then calls all registered
	 * {@link ProcessDrawDecorator}s for the operator render phase.
	 *
	 * @param process
	 * 		the process to draw the operators for
	 * @param g2
	 * 		the graphics context to draw upon
	 * @param printing
	 * 		if {@code true} we are printing instead of drawing to the screen
	 */
	public void drawOperators(final ExecutionUnit process, final Graphics2D g2, final boolean printing) {
		Graphics2D gOp = (Graphics2D) g2.create();
		// draw not selected operators in order
		for (Operator op : process.getOperators()) {
			if (!model.getSelectedOperators().contains(op)) {
				drawOperator(op, true, gOp, printing);
			}
		}
		// draw selected operators in reverse order for correct z-levels
		List<Operator> drawList = new LinkedList<>(model.getSelectedOperators());
		Collections.reverse(drawList);
		for (Operator op : drawList) {
			if (process.getOperators().contains(op)) {
				drawOperator(op, true, gOp, printing);
			}
		}

		// draw ports of the actual process
		renderPorts(process.getInnerSources(), gOp, true);
		renderPorts(process.getInnerSinks(), gOp, true);
		gOp.dispose();

		// let decorators draw
		drawPhaseDecorators(process, g2, RenderPhase.OPERATORS, printing);
	}

	/**
	 * Draws additions to operators and then calls all registered {@link ProcessDrawDecorator}s for
	 * the operator additions render phase.
	 *
	 * @param process
	 * 		the process to draw the operator additions for
	 * @param g2
	 * 		the graphics context to draw upon
	 * @param printing
	 * 		if {@code true} we are printing instead of drawing to the screen
	 */
	public void drawOperatorAdditions(final ExecutionUnit process, final Graphics2D g2, final boolean printing) {
		// let decorators draw
		drawPhaseDecorators(process, g2, RenderPhase.OPERATOR_ADDITIONS, printing);
	}

	/**
	 * Draws the process(es) operator overlay (i.e. execution order) based on the
	 * {@link ProcessRendererModel} data and then calls all registered {@link ProcessDrawDecorator}s
	 * for the overlay render phase.
	 *
	 * @param process
	 * 		the process to draw the overlay for
	 * @param g2
	 * 		the graphics context to draw upon
	 * @param printing
	 * 		if {@code true} we are printing instead of drawing to the screen
	 */
	public void drawOverlay(final ExecutionUnit process, final Graphics2D g2, final boolean printing) {
		// let decorators draw
		drawPhaseDecorators(process, g2, RenderPhase.OVERLAY, printing);
	}

	/**
	 * Draws the process(es) foreground based on the {@link ProcessRendererModel} data and then
	 * calls all registered {@link ProcessDrawDecorator}s for the foreground render phase.
	 *
	 * @param process
	 * 		the process to draw the foreground for
	 * @param g2
	 * 		the graphics context to draw upon
	 * @param printing
	 * 		if {@code true} we are printing instead of drawing to the screen
	 */
	public void drawForeground(final ExecutionUnit process, final Graphics2D g2, final boolean printing) {
		Graphics2D gBG = (Graphics2D) g2.create();
		renderForeground(process, gBG, printing);
		gBG.dispose();
	}

	/**
	 * Draws the given {@link Operator} if inside the graphics clip bounds.
	 *
	 * @param op
	 * 		the operator to draw. Note that it must have a position attached, see
	 * 		{@link GUIProcessXMLFilter}
	 * @param drawPorts
	 * 		if {@true} will also draw operator ports, otherwise will not draw ports
	 * @param g2
	 * 		the graphics context to draw upon
	 * @param printing
	 * 		if {@code true} we are printing instead of drawing to the screen
	 *
	 */
	public void drawOperator(final Operator op, final boolean drawPorts, final Graphics2D g2, final boolean printing) {
		Rectangle2D frame = model.getOperatorRect(op);
		if (frame == null) {
			return;
		}

		// only draw operator if visible
		Rectangle2D opBounds = new Rectangle2D.Double(frame.getX() - 10, frame.getY(), frame.getWidth() + 20,
				frame.getHeight());
		if (g2.getClipBounds() != null && !g2.getClipBounds().intersects(opBounds)) {
			return;
		}

		renderOperator(op, g2);
		renderPorts(op.getInputPorts(), g2, op.isEnabled());
		renderPorts(op.getOutputPorts(), g2, op.isEnabled());

		// let operator decorators draw
		drawOperatorDecorators(op, g2, printing);
	}

	/**
	 * Adds the given draw decorator for the specified render phase.
	 *
	 * @param decorator
	 * 		the decorator instance to add
	 * @param phase
	 * 		the phase during which the decorator should be called to draw. If multiple
	 * 		decorators want to draw during the same phase, they are called in the order they
	 * 		were registered
	 */
	public void addDecorator(final ProcessDrawDecorator decorator, final RenderPhase phase) {
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
	 *
	 * @param decorator
	 * 		the decorator instance to remove
	 * @param phase
	 * 		the phase from which the decorator should be removed
	 */
	public void removeDecorator(final ProcessDrawDecorator decorator, final RenderPhase phase) {
		if (decorator == null) {
			throw new IllegalArgumentException("decorator must not be null!");
		}
		if (phase == null) {
			throw new IllegalArgumentException("phase must not be null!");
		}

		decorators.get(phase).remove(decorator);
	}

	/**
	 * Adds the given operator draw decorator. The decorator is called directly after the operator
	 * was drawn.
	 *
	 * @param decorator
	 * 		the decorator instance to add
	 */
	public void addDecorator(final OperatorDrawDecorator decorator) {
		if (decorator == null) {
			throw new IllegalArgumentException("decorator must not be null!");
		}

		operatorDecorators.add(decorator);
	}

	/**
	 * Removes the given operator decorator. If the decorator has already been removed, does
	 * nothing.
	 *
	 * @param decorator
	 * 		the decorator instance to remove
	 */
	public void removeDecorator(final OperatorDrawDecorator decorator) {
		if (decorator == null) {
			throw new IllegalArgumentException("decorator must not be null!");
		}

		operatorDecorators.remove(decorator);
	}

	/**
	 * Draws the given {@link Operator}.
	 *
	 * @param operator
	 * 		the operator to draw
	 * @param g2
	 * 		the graphics context
	 */
	private void renderOperator(final Operator operator, final Graphics2D g2) {
		Rectangle2D frame = model.getOperatorRect(operator);
		// the first paint can come before any of the operator register listeners fire
		// thus we need to check the rect for null and set it here once
		// all subsequent calls will then have a valid rect
		if (frame == null) {
			return;
		}

		boolean isSelected = model.getSelectedOperators().contains(operator);
		boolean isHovered = operator == model.getHoveringOperator();
		double headerWidth;
		Rectangle2D nameBounds = OPERATOR_FONT.getStringBounds(operator.getName(), g2.getFontRenderContext());
		if (isSelected) {
			headerWidth = nameBounds.getWidth() + 6;
		} else {
			headerWidth = frame.getWidth() * MAX_HEADER_RATIO;
		}

		Shape bodyShape = new RoundRectangle2D.Double(frame.getMinX(), frame.getMinY() + ProcessRendererModel.HEADER_HEIGHT,
				frame.getWidth(), frame.getHeight() - ProcessRendererModel.HEADER_HEIGHT, OPERATOR_CORNER, OPERATOR_CORNER);

		// Frame Body
		Color baseColor = SwingTools.getOperatorColor(operator);
		if (!operator.isEnabled()) {
			baseColor = Color.LIGHT_GRAY;
		}

		if (operator instanceof OperatorChain) {
			Rectangle shadowRect = new Rectangle(bodyShape.getBounds());
			shadowRect.setLocation(shadowRect.x - PROCESS_BOX_SHADOW_OFFSET_X, shadowRect.y + PROCESS_BOX_SHADOW_OFFSET_Y);
			Shape shadowShape = new RoundRectangle2D.Double(shadowRect.getMinX(), shadowRect.getMinY(),
					shadowRect.getWidth(), shadowRect.getHeight(), OPERATOR_CORNER, OPERATOR_CORNER);

			g2.setPaint(baseColor);
			g2.fill(shadowShape);

			g2.setPaint(LINE_COLOR);
			g2.setStroke(LINE_STROKE);
			drawOperatorShape(g2, isSelected, isHovered, shadowShape);
		}

		g2.setPaint(baseColor);
		g2.fill(bodyShape);

		g2.setPaint(LINE_COLOR);
		g2.setStroke(LINE_STROKE);

		drawOperatorShape(g2, isSelected, isHovered, bodyShape);

		// Label: Name
		g2.setFont(OPERATOR_FONT);
		String name = ProcessDrawUtils.fitString(operator.getName(), g2, (int) headerWidth);

		// take smallest width and center name
		double relevantWidth = nameBounds.getWidth() < headerWidth ? nameBounds.getWidth() : headerWidth;
		double offset = (frame.getWidth() - relevantWidth) / 2;
		int x = (int) (frame.getX() + offset);

		// draw white badge behind operator name
		if ((isHovered || isSelected) && nameBounds.getWidth() > frame.getWidth()) {
			g2.setColor(Color.WHITE);
			int padding = 5;
			g2.fillRoundRect((int) Math.min(frame.getX() - padding, x - padding), (int) frame.getY() - 3,
					(int) Math.max(frame.getWidth() + 2 * padding, relevantWidth + 2 * padding),
					ProcessRendererModel.HEADER_HEIGHT + 3, OPERATOR_BG_CORNER, OPERATOR_BG_CORNER);
		}

		if (operator.isEnabled()) {
			if (isSelected) {
				g2.setColor(OPERATOR_NAME_COLOR_SELECTED);
			} else if (isHovered) {
				g2.setColor(OPERATOR_NAME_COLOR_HIGHLIGHT);
			} else {
				g2.setColor(OPERATOR_NAME_COLOR);
			}
		} else {
			g2.setColor(OPERATOR_NAME_COLOR_DISABLED);
		}
		g2.drawString(name, x, (int) (frame.getY() + OPERATOR_FONT.getSize() + 1));

		double yPosition = frame.getY() + ProcessRendererModel.HEADER_HEIGHT + 7;

		if (operator.isAnimating() && ProcessAnimationManager.INSTANCE.getAnimationForOperator(operator) != null) {
			// draw progress animation if operator is running
			AffineTransform transformBefore = g2.getTransform();
			Animation animation = ProcessAnimationManager.INSTANCE.getAnimationForOperator(operator);
			Rectangle bounds = animation.getBounds();
			g2.translate(frame.getX() + frame.getWidth() / 2, yPosition + OPERATOR_ICON_SIZE / 2);
			g2.scale(OPERATOR_ICON_SIZE / bounds.getWidth(), OPERATOR_ICON_SIZE / bounds.getHeight());
			animation.draw(g2);
			g2.setTransform(transformBefore);
		} else {
			// Icon double size of required icon to make zooming in look smooth
			ImageIcon icon = operator.getOperatorDescription().getLargeIcon();
			if (icon != null) {
				if (!operator.isEnabled()) {
					icon = ProcessDrawUtils.getIcon(operator, icon);
				}
				int iconSize = 24;
				RenderingHints originalRenderingHints = g2.getRenderingHints();
				g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
				g2.drawImage(icon.getImage(), (int) (frame.getX() + frame.getWidth() / 2 - iconSize / 2), (int) yPosition, iconSize, iconSize, null);
				g2.setRenderingHints(originalRenderingHints);
			}
		}

		// Small icons
		int iconX = (int) frame.getX() + 3;
		int iconSize = 16;
		// Dirtyness
		ImageIcon opIcon;
		if (operator.isRunning()) {
			opIcon = model.getZoomFactor() <= 1d ? OPERATOR_RUNNING : OPERATOR_RUNNING_ZOOMED;
		} else if (!operator.isDirty()) {
			opIcon = model.getZoomFactor() <= 1d ? OPERATOR_READY : OPERATOR_READY_ZOOMED;
		} else {
			opIcon = null;
		}

		if (opIcon != null) {
			ImageIcon icon = ProcessDrawUtils.getIcon(operator, opIcon);
			RenderingHints originalRenderingHints = g2.getRenderingHints();
			g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
			g2.drawImage(icon.getImage(), iconX, (int) (frame.getY() + frame.getHeight() - iconSize - 1), iconSize, iconSize, null);
			g2.setRenderingHints(originalRenderingHints);
		}
		iconX += iconSize + 1;

		// Errors
		boolean isBlacklisted = OperatorService.isOperatorBlacklisted(operator.getOperatorDescription().getKey());
		ImageIcon errorIcon;
		if (model.getZoomFactor() <= 1d) {
			errorIcon = isBlacklisted ? IMAGE_BLACKLISTED : IMAGE_WARNING;
		} else {
			errorIcon = isBlacklisted ? IMAGE_BLACKLISTED_ZOOMED : IMAGE_WARNING_ZOOMED;
		}
		if ((!operator.getErrorList().isEmpty() && !operator.isRunning()) || isBlacklisted) {
			int iconY = (int) (frame.getY() + frame.getHeight() - iconSize - 2);
			ImageIcon icon = ProcessDrawUtils.getIcon(operator, errorIcon);
			RenderingHints originalRenderingHints = g2.getRenderingHints();
			g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
			g2.drawImage(icon.getImage(), iconX, iconY, iconSize, iconSize, null);
			g2.setRenderingHints(originalRenderingHints);
		}
		iconX += iconSize + 1;

		// Breakpoint
		if (operator.hasBreakpoint()) {
			ImageIcon breakpointIcon;
			if (operator.getNumberOfBreakpoints() == 1) {
				if (operator.hasBreakpoint(BreakpointListener.BREAKPOINT_BEFORE)) {
					breakpointIcon = model.getZoomFactor() <= 1d ? IMAGE_BREAKPOINT_BEFORE : IMAGE_BREAKPOINT_BEFORE_ZOOMED;
				} else if (operator.hasBreakpoint(BreakpointListener.BREAKPOINT_AFTER)) {
					breakpointIcon = model.getZoomFactor() <= 1d ? IMAGE_BREAKPOINT_AFTER : IMAGE_BREAKPOINT_AFTER_ZOOMED;
				} else {
					breakpointIcon = model.getZoomFactor() <= 1d ? IMAGE_BREAKPOINT_WITHIN : IMAGE_BREAKPOINT_WITHIN_ZOOMED;
				}
			} else {
				breakpointIcon = model.getZoomFactor() <= 1d ? IMAGE_BREAKPOINTS : IMAGE_BREAKPOINTS_ZOOMED;
			}
			ImageIcon icon = ProcessDrawUtils.getIcon(operator, breakpointIcon);
			RenderingHints originalRenderingHints = g2.getRenderingHints();
			g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
			g2.drawImage(icon.getImage(), iconX, (int) (frame.getY() + frame.getHeight() - iconSize - 1), iconSize, iconSize, null);
			g2.setRenderingHints(originalRenderingHints);
		}
	}

	/**
	 * Draw the given shape with color settings for an Operator
	 *
	 * @param graphics2D
	 * 		The graphics to use for drawing
	 * @param isSelected
	 * 		if true set colors to show selection
	 * @param isHovered
	 * 		if true set colors to show hovering
	 * @param bodyShape
	 * 		a shape to be drawn
	 */
	private void drawOperatorShape(Graphics2D graphics2D, boolean isSelected, boolean isHovered, Shape bodyShape) {
		if (isSelected) {
			graphics2D.setPaint(OPERATOR_BORDER_COLOR_SELECTED);
			graphics2D.setStroke(OPERATOR_STROKE_SELECTED);
		} else if (isHovered) {
			graphics2D.setPaint(OPERATOR_BORDER_COLOR_HIGHLIGHT);
			graphics2D.setStroke(OPERATOR_STROKE_HIGHLIGHT);
		} else {
			graphics2D.setPaint(OPERATOR_BORDER_COLOR);
			graphics2D.setStroke(OPERATOR_STROKE_NORMAL);
		}
		graphics2D.draw(bodyShape);
	}

	/**
	 * Draws the given {@link Ports}.
	 *
	 * @param ports
	 * @param g
	 * @param enabled
	 */
	private void renderPorts(final Ports<? extends Port> ports, final Graphics2D g, final boolean enabled) {
		boolean input = ports instanceof InputPorts;
		g.setStroke(LINE_STROKE);

		OutputPort hoveredConnectionSource = model.getHoveringConnectionSource();
		OutputPort selectedConnectionSource = model.getSelectedConnectionSource();
		Port connectingPort = model.getConnectingPortSource();
		InputPort hoveredConnectionTarget = hoveredConnectionSource != null ? hoveredConnectionSource.getDestination()
				: null;
		InputPort selectedConnectionTarget = selectedConnectionSource != null ? selectedConnectionSource.getDestination()
				: null;
		Port hoveredPort = model.getHoveringPort();
		Port hoveredPortConnectedTo = null;
		if (hoveredPort instanceof OutputPort) {
			hoveredPortConnectedTo = ((OutputPort) hoveredPort).getDestination();
		} else if (hoveredPort instanceof InputPort) {
			hoveredPortConnectedTo = ((InputPort) hoveredPort).getSource();
		}

		for (Port port : ports.getAllPorts()) {
			boolean hasError = !port.getErrors().isEmpty();

			Point location = ProcessDrawUtils.createPortLocation(port, model);

			// the first paint can come before any of the operator register listeners fire
			// thus we need to check the location for null; subsequent calls will have a location
			if (location == null) {
				return;
			}

			// determine current state variables
			boolean isConHovered = port.equals(hoveredConnectionSource) || port.equals(hoveredConnectionTarget);
			boolean isConSelected = port.equals(selectedConnectionSource) || port.equals(selectedConnectionTarget);
			boolean isPortHovered = port.equals(hoveredPort) || port.equals(hoveredPortConnectedTo);
			boolean isConDirectlyHovered = isConHovered && !isPortHovered && hoveredPort == null
					&& model.getHoveringOperator() == null && model.getConnectingPortSource() == null;
			boolean isConDirectlySelected = isConSelected && model.getConnectingPortSource() == null;
			Operator draggedOp = !model.getDraggedOperators().isEmpty() ? model.getDraggedOperators().get(0) : null;
			boolean isDragTarget = isConHovered && model.getDraggedOperators().size() == 1
					&& ProcessDrawUtils.canOperatorBeInsertedIntoConnection(model, draggedOp);
			boolean isConnectingSource = port.equals(connectingPort);
			boolean isProcessPort = port.getPorts().getOwner().getOperator() == model.getDisplayedChain();

			double x = location.getX();
			double y = location.getY();
			Color line = isProcessPort ? BORDER_COLOR : OPERATOR_BORDER_COLOR;
			Color fill = Color.WHITE;

			Shape ellipseTop, ellipseBottom, ellipseBoth;
			int startAngle;
			int portSize;
			if (isConDirectlyHovered) {
				portSize = PORT_SIZE_HIGHLIGHT;
			} else if (isConDirectlySelected) {
				portSize = PORT_SIZE_HIGHLIGHT;
			} else if (isDragTarget) {
				portSize = PORT_SIZE_HIGHLIGHT;
			} else if (isPortHovered) {
				portSize = PORT_SIZE_HIGHLIGHT;
			} else if (isConnectingSource) {
				portSize = PORT_SIZE_HIGHLIGHT;
			} else {
				portSize = PORT_SIZE;
			}

			if (input) {
				startAngle = 90;
				ellipseTop = new Arc2D.Double(new Rectangle2D.Double(x - portSize / 2, y - portSize / 2, portSize, portSize),
						startAngle, 90, Arc2D.PIE);
				ellipseBottom = new Arc2D.Double(
						new Rectangle2D.Double(x - portSize / 2, y - portSize / 2, portSize, portSize), startAngle + 90, 90,
						Arc2D.PIE);
				ellipseBoth = new Arc2D.Double(
						new Rectangle2D.Double(x - portSize / 2, y - portSize / 2, portSize, portSize), startAngle, 180,
						Arc2D.PIE);
			} else {
				startAngle = 270;
				ellipseBottom = new Arc2D.Double(
						new Rectangle2D.Double(x - portSize / 2, y - portSize / 2, portSize, portSize), startAngle, 90,
						Arc2D.PIE);
				ellipseTop = new Arc2D.Double(new Rectangle2D.Double(x - portSize / 2, y - portSize / 2, portSize, portSize),
						startAngle + 90, 90, Arc2D.PIE);
				ellipseBoth = new Arc2D.Double(
						new Rectangle2D.Double(x - portSize / 2, y - portSize / 2, portSize, portSize), startAngle, 180,
						Arc2D.PIE);
			}

			if (enabled) {
				// What we have
				if (!hasError) {
					fill = ProcessDrawUtils.getColorFor(port, Color.WHITE, true);
				} else {
					fill = Color.RED;
				}
				g.setColor(fill);

				if (port instanceof OutputPort) {
					g.fill(ellipseBoth);
				}

				// What we want
				if (port instanceof InputPort) {
					g.fill(ellipseTop);
					InputPort inPort = (InputPort) port;
					for (Precondition precondition : inPort.getAllPreconditions()) {
						g.setColor(ProcessDrawUtils.getColorFor(precondition.getExpectedMetaData()));
						break;
					}
					g.fill(ellipseBottom);
				}

				g.setColor(line);
				if (model.getHoveringPort() == port) {
					g.setStroke(PORT_HIGHLIGHT_STROKE);
				} else {
					g.setStroke(PORT_STROKE);
				}
				g.draw(ellipseBoth);
			} else {
				g.setColor(fill);
				g.fill(ellipseBoth);

				g.setColor(line);
				if (model.getHoveringPort() == port) {
					g.setStroke(PORT_HIGHLIGHT_STROKE);
				} else {
					g.setStroke(PORT_STROKE);
				}
				g.draw(ellipseBoth);
			}

			g.setFont(PORT_FONT);
			int xt;
			int yt = 0;
			Rectangle2D strBounds = PORT_FONT.getStringBounds(port.getShortName(), g.getFontRenderContext());
			if (isProcessPort) {
				if (input) {
					xt = -(int) (portSize / 2 + strBounds.getWidth() + 3);
				} else {
					xt = portSize - 3;
				}

				// connected process ports have their label above the connection
				if (port.isConnected() || isConnectingSource) {
					yt = 5;
				}
			} else {
				if (input) {
					xt = PORT_SIZE / 2;
				} else {
					xt = -(int) strBounds.getWidth() - 3;
				}
			}

			if (hasError) {
				g.setColor(Color.RED);
				g.setFont(PORT_FONT.deriveFont(Font.BOLD));
			} else {
				if (port == model.getHoveringPort()) {
					g.setColor(PORT_NAME_SELECTION_COLOR);
				} else {
					g.setColor(PORT_NAME_COLOR);
				}
			}

			g.drawString(port.getShortName(), (int) x + xt, (int) (y - yt + strBounds.getHeight() / 2 - 2));
		}
	}

	/**
	 * Draws the connections for the given ports.
	 *
	 * @param ports
	 * 		the output ports for which to draw connections
	 * @param g2
	 * 		the graphics context to draw upon
	 */
	@SuppressWarnings("deprecation")
	private void renderConnections(final OutputPorts ports, final Graphics2D g2) {
		for (int i = 0; i < ports.getNumberOfPorts(); i++) {
			OutputPort from = ports.getPortByIndex(i);
			Port to = from.getDestination();
			g2.setColor(ProcessDrawUtils.getColorFor(from, Color.LIGHT_GRAY, true));
			if (to != null) {
				Shape connector = ProcessDrawUtils.createConnector(from, to, model);

				// the first paint can come before any of the operator register listeners fire
				// thus we need to check the shape for null; subsequent calls will have a valid
				// shape
				if (connector == null) {
					return;
				}

				// determine current state variables
				boolean isConHovered = from == model.getHoveringConnectionSource();
				boolean isConSelected = from == model.getSelectedConnectionSource();
				boolean isConDirectlyHovered = isConHovered && model.getHoveringPort() == null
						&& model.getHoveringOperator() == null && model.getConnectingPortSource() == null;
				boolean isConDirectlySelected = isConSelected && model.getConnectingPortSource() == null;
				Operator draggedOp = !model.getDraggedOperators().isEmpty() ? model.getDraggedOperators().get(0) : null;
				boolean isDragTarget = isConHovered && model.getDraggedOperators().size() == 1
						&& ProcessDrawUtils.canOperatorBeInsertedIntoConnection(model, draggedOp);
				boolean portHovered = from == model.getHoveringPort() || to == model.getHoveringPort();

				if (from.getMetaData() instanceof CollectionMetaData) {
					if (isDragTarget) {
						g2.setStroke(CONNECTION_COLLECTION_HIGHLIGHT_STROKE);
					} else if (isConDirectlyHovered) {
						g2.setStroke(CONNECTION_COLLECTION_HIGHLIGHT_STROKE);
					} else if (isConDirectlySelected) {
						g2.setStroke(CONNECTION_COLLECTION_HIGHLIGHT_STROKE);
					} else if (portHovered) {
						g2.setStroke(CONNECTION_COLLECTION_HIGHLIGHT_STROKE);
					} else {
						g2.setStroke(CONNECTION_COLLECTION_LINE_STROKE);
					}
					g2.draw(connector);
					g2.setColor(Color.white);
					g2.setStroke(LINE_STROKE);
					g2.draw(connector);
				} else {
					if (isDragTarget) {
						g2.setStroke(CONNECTION_HIGHLIGHT_STROKE);
					} else if (isConDirectlyHovered) {
						g2.setStroke(CONNECTION_HIGHLIGHT_STROKE);
					} else if (isConDirectlySelected) {
						g2.setStroke(CONNECTION_HIGHLIGHT_STROKE);
					} else if (portHovered) {
						g2.setStroke(CONNECTION_HIGHLIGHT_STROKE);
					} else {
						g2.setStroke(CONNECTION_LINE_STROKE);
					}
					g2.draw(connector);
				}
			}
		}
	}

	/**
	 * Draws the operator background (white round rectangle).
	 *
	 * @param operator
	 * 		the operator to draw the background for
	 * @param g2
	 * 		the graphics context to draw upon
	 */
	private void renderOperatorBackground(final Operator operator, final Graphics2D g2) {
		Rectangle2D frame = model.getOperatorRect(operator);
		// the first paint can come before any of the operator register listeners fire
		// thus we need to check the rect for null and set it here once
		// all subsequent calls will then have a valid rect
		if (frame == null) {
			return;
		}

		int expandLeft = 0;
		int expandHeight = 0;
		if (operator instanceof OperatorChain) {
			expandLeft = PROCESS_BOX_SHADOW_OFFSET_X;
			expandHeight = PROCESS_BOX_SHADOW_OFFSET_Y;
		}
		RoundRectangle2D background = new RoundRectangle2D.Double(frame.getX() - 7 - expandLeft, frame.getY() - 3,
				frame.getWidth() + 14 + expandLeft, frame.getHeight() + 11 + expandHeight,
				OPERATOR_BG_CORNER, OPERATOR_BG_CORNER);
		g2.setColor(Color.WHITE);
		g2.fill(background);

		// if name is wider than operator, extend white background for header
		Rectangle2D nameBounds = OPERATOR_FONT.getStringBounds(operator.getName(), g2.getFontRenderContext());
		if (nameBounds.getWidth() > frame.getWidth()) {
			double relevantWidth = Math.min(nameBounds.getWidth(), frame.getWidth() * MAX_HEADER_RATIO);
			double offset = (frame.getWidth() - relevantWidth) / 2;
			int x = (int) (frame.getX() + offset);

			int padding = 5;
			RoundRectangle2D nameBackground = new RoundRectangle2D.Double(
					(int) Math.min(frame.getX() - padding, x - padding), frame.getY() - 3, relevantWidth + 2 * padding,
					ProcessRendererModel.HEADER_HEIGHT + 3, OPERATOR_BG_CORNER, OPERATOR_BG_CORNER);
			g2.fill(nameBackground);
		}

		// render ports
		renderPortsBackground(operator.getInputPorts(), g2);
		renderPortsBackground(operator.getOutputPorts(), g2);
	}

	/**
	 * Draws the connections background (round pipe) for the given ports.
	 *
	 * @param inputPorts
	 * 		the input ports for which to draw connection backgrounds
	 * @param ports
	 * 		the output ports for which to draw connection backgrounds
	 * @param g2
	 * 		the graphics context to draw upon
	 */
	@SuppressWarnings("deprecation")
	private void renderConnectionsBackground(final InputPorts inputPorts, final OutputPorts ports, final Graphics2D g2) {
		for (int i = 0; i < ports.getNumberOfPorts(); i++) {
			OutputPort from = ports.getPortByIndex(i);
			Port to = from.getDestination();
			if (to != null) {
				Shape connector = ProcessDrawUtils.createConnector(from, to, model);

				// the first paint can come before any of the operator register listeners fire
				// thus we need to check the shape for null; subsequent calls will have a valid
				// shape
				if (connector == null) {
					return;
				}

				g2.setColor(Color.WHITE);
				if (from.getMetaData() instanceof CollectionMetaData) {
					g2.setStroke(CONNECTION_COLLECTION_LINE_BACKGROUND_STROKE);
				} else {
					g2.setStroke(CONNECTION_LINE_BACKGROUND_STROKE);
				}
				g2.draw(connector);
			}
		}
	}

	/**
	 * Draws the given {@link Ports}.
	 *
	 * @param ports
	 * @param g2
	 */
	private void renderPortsBackground(final Ports<? extends Port> ports, final Graphics2D g2) {
		boolean input = ports instanceof InputPorts;
		g2.setStroke(LINE_STROKE);

		for (Port port : ports.getAllPorts()) {
			Point location = ProcessDrawUtils.createPortLocation(port, model);

			// the first paint can come before any of the operator register listeners fire
			// thus we need to check the location for null; subsequent calls will have a location
			if (location == null) {
				return;
			}

			double x = location.getX();
			double y = location.getY();

			Shape ellipseBoth;
			int startAngle;
			if (input) {
				startAngle = 90;
				ellipseBoth = new Arc2D.Double(new Rectangle2D.Double(x - PORT_SIZE_BACKGROUND / 2,
						y - PORT_SIZE_BACKGROUND / 2, PORT_SIZE_BACKGROUND, PORT_SIZE_BACKGROUND), startAngle, 180,
						Arc2D.PIE);
			} else {
				startAngle = 270;
				ellipseBoth = new Arc2D.Double(new Rectangle2D.Double(x - PORT_SIZE_BACKGROUND / 2,
						y - PORT_SIZE_BACKGROUND / 2, PORT_SIZE_BACKGROUND, PORT_SIZE_BACKGROUND), startAngle, 180,
						Arc2D.PIE);
			}

			g2.setColor(Color.WHITE);
			g2.fill(ellipseBoth);

		}
	}

	/**
	 * Draws the background for the given process.
	 *
	 * @param process
	 * 		the process for which to render the background
	 * @param g2
	 * 		the graphics context to draw upon
	 */
	private void renderBackground(final ExecutionUnit process, final Graphics2D g2, boolean printing) {
		double width = model.getProcessWidth(process);
		double height = model.getProcessHeight(process);
		Shape frame = new Rectangle2D.Double(0, 0, width, height);

		Color currentInnerColor = INNER_COLOR;

		// background color
		g2.setColor(currentInnerColor);
		g2.fill(frame);

		// remember pre-scaling transform
		AffineTransform at = g2.getTransform();
		g2.scale(model.getZoomFactor(), model.getZoomFactor());

		// process title
		g2.setColor(PROCESS_TITLE_COLOR);
		g2.setFont(PROCESS_FONT);
		Operator displayedChain = process.getEnclosingOperator();

		if (model.getProcesses().size() == 1) {
			g2.drawString(displayedChain.getName(), PROCESS_TITLE_PADDING + 2,
					PROCESS_FONT.getSize() + PROCESS_TITLE_PADDING);
		} else {
			// multiple subprocesses have special names so show them instead of operator name
			g2.drawString(process.getName(), PROCESS_TITLE_PADDING + 2, PROCESS_FONT.getSize() + PROCESS_TITLE_PADDING);
		}

		// restore transform
		g2.setTransform(at);

		// breakpoint
		if (displayedChain.hasBreakpoint()) {
			ImageIcon breakpointIcon;
			if (displayedChain.getNumberOfBreakpoints() == 1) {
				if (displayedChain.hasBreakpoint(BreakpointListener.BREAKPOINT_BEFORE)) {
					breakpointIcon = IMAGE_BREAKPOINT_BEFORE_LARGE;
				} else if (displayedChain.hasBreakpoint(BreakpointListener.BREAKPOINT_AFTER)) {
					breakpointIcon = IMAGE_BREAKPOINT_AFTER_LARGE;
				} else {
					breakpointIcon = IMAGE_BREAKPOINT_WITHIN_LARGE;
				}
			} else {
				breakpointIcon = IMAGE_BREAKPOINTS_LARGE;
			}
			ProcessDrawUtils.getIcon(displayedChain, breakpointIcon).paintIcon(null, g2,
					(int) width - PROCESS_TITLE_PADDING - IMAGE_BREAKPOINTS_LARGE.getIconWidth(), PROCESS_TITLE_PADDING);
		}

		boolean dragIndicate = false;
		if (drawHighlight && (model.isDragStarted() || model.isDropTargetSet() && model.isImportDragged())
				|| model.isOperatorSourceHovered()) {
			switch (RapidMinerGUI.getDragHighlighteMode()) {
				case FULL:
				case BORDER:
					dragIndicate = true;
					break;
				case NONE:
				default:
					break;
			}
		}

		// drag border
		if (dragIndicate && !printing) {

			if (RapidMinerGUI.getDragHighlighteMode() == DragHighlightMode.FULL) {

				// do nothing if we are in a tutorial process
				MainFrame mainFrame = RapidMinerGUI.getMainFrame();
				if (mainFrame != null) {
					Process currentProcess = mainFrame.getProcess();
					if (currentProcess != null) {
						ProcessRootOperator rootOperator = currentProcess.getRootOperator();
						if (rootOperator != null && rootOperator.getUserData(Tutorial.KEY_USER_DATA_FLAG) != null) {
							return;
						}
					}
				}

				Font dragFont;
				if (width >= 600) {
					dragFont = DRAG_FONT_LARGE;
				} else if (width >= 400) {
					dragFont = DRAG_FONT_MEDIUM;
				} else {
					dragFont = DRAG_FONT_SMALL;
				}
				int padding = dragFont.getSize() / 3;
				if (drawHighlight && (model.isDragStarted() || model.isDropTargetSet() && model.isImportDragged())) {
					// drag here text
					g2.setFont(dragFont);
					Rectangle2D bounds = g2.getFontMetrics().getStringBounds(DROP_HERE, g2);

					int x = (int) (width / 2 - bounds.getWidth() / 2);
					int y = (int) (height / 2 + bounds.getHeight() / 2);
					g2.setColor(DRAG_BG_COLOR);

					int rX = x - padding;
					int rY = (int) (y - bounds.getHeight() / 2 - padding);
					g2.fillRoundRect(rX, rY, (int) (bounds.getWidth() + padding * 2), (int) bounds.getHeight(),
							RapidLookAndFeel.CORNER_DEFAULT_RADIUS, RapidLookAndFeel.CORNER_DEFAULT_RADIUS);

					drawCenteredText(process, g2, dragFont, DROP_HERE, DRAG_FG_COLOR, 0);
				} else if (model.isOperatorSourceHovered()) {
					// drop here text
					g2.setFont(dragFont);
					Rectangle2D bounds = g2.getFontMetrics().getStringBounds(DRAG_HERE, g2);

					int x = (int) (width / 2 - bounds.getWidth() / 2);
					int y = (int) (height / 2 + bounds.getHeight() / 2);
					g2.setColor(DRAG_BG_COLOR);

					int rX = x - padding;
					int rY = (int) (y - bounds.getHeight() / 2 - padding);
					g2.fillRoundRect(rX, rY, (int) (bounds.getWidth() + padding * 2), (int) bounds.getHeight(),
							RapidLookAndFeel.CORNER_DEFAULT_RADIUS, RapidLookAndFeel.CORNER_DEFAULT_RADIUS);

					drawCenteredText(process, g2, dragFont, DRAG_HERE, DRAG_FG_COLOR, 0);
				}
			}
		} else if (process.getEnclosingOperator() instanceof ProcessRootOperator && process.getAllInnerOperators().isEmpty()
				&& ((ProcessRootOperator) process.getEnclosingOperator()).getUserData(Tutorial.KEY_USER_DATA_FLAG) == null) {
			// empty process hint text
			// but only if we are not in a tutorial process as indicated by the flag

			g2.setColor(HINT_COLOR);
			Font hintFont;
			if (width >= 700) {
				hintFont = HINT_FONT_LARGE;
			} else if (width >= 500) {
				hintFont = HINT_FONT_MEDIUM;
			} else {
				hintFont = HINT_FONT_SMALL;
			}

			double offset = hintFont.getSize() * 1.5;
			drawCenteredText(process, g2, hintFont, HINT_EMPTY_PROCESS_1, HINT_COLOR, -offset);
			drawCenteredText(process, g2, hintFont, HINT_EMPTY_PROCESS_2, HINT_COLOR, 0);
			drawCenteredText(process, g2, hintFont, HINT_EMPTY_PROCESS_3, HINT_COLOR, offset);
		}
	}

	/**
	 * Renders the drag border if needed.
	 *
	 * @param process
	 * 		the process for which to render the background
	 * @param g2
	 * 		the graphics context to draw upon
	 */
	private void renderForeground(final ExecutionUnit process, final Graphics2D g2, boolean printing) {
		if (drawHighlight && !printing && (model.isDragStarted() || model.isDropTargetSet() && model.isImportDragged())
				|| model.isOperatorSourceHovered()) {
			switch (RapidMinerGUI.getDragHighlighteMode()) {
				case FULL:
				case BORDER:
					drawDragBorder(process, g2);
					break;
				case NONE:
				default:
					break;
			}
		}

	}

	/**
	 * Draws the drag border.
	 *
	 * @param process
	 * 		the process for which to render the background
	 * @param g2
	 * 		the graphics context to draw upon
	 */
	private void drawDragBorder(final ExecutionUnit process, final Graphics2D g2) {
		double width = model.getProcessWidth(process);
		double height = model.getProcessHeight(process);
		Shape dragFrame = new RoundRectangle2D.Double(DRAG_BORDER_PADDING, DRAG_BORDER_PADDING,
				width - 2 * DRAG_BORDER_PADDING, height - 2 * DRAG_BORDER_PADDING, DRAG_BORDER_CORNER, DRAG_BORDER_CORNER);
		g2.setColor(BORDER_DRAG_COLOR);
		g2.setStroke(BORDER_DRAG_STROKE);
		g2.draw(dragFrame);
	}

	/**
	 * Lets the decorators draw for the specified {@link RenderPhase}.
	 *
	 * @param process
	 * 		the process which should be decorated
	 * @param g2
	 * 		the graphics context. Each decorator gets a new context which is disposed
	 * 		afterwards
	 * @param phase
	 * 		the render phase which determines the decorators that are called
	 * @param printing
	 * 		if {@code true} we are printing instead of drawing to the screen
	 */
	private void drawPhaseDecorators(final ExecutionUnit process, final Graphics2D g2, final RenderPhase phase,
									 final boolean printing) {
		double width = model.getProcessWidth(process) * (1 / model.getZoomFactor());
		double height = model.getProcessHeight(process) * (1 / model.getZoomFactor());
		int borderWidth = 2;
		Shape frame = new Rectangle2D.Double(0 + borderWidth, 0 + borderWidth, width - 2 * borderWidth,
				height - 2 * borderWidth);

		for (ProcessDrawDecorator decorater : decorators.get(phase)) {
			Graphics2D g2Deco = null;
			try {
				g2Deco = (Graphics2D) g2.create();
				g2Deco.clip(frame);
				if (printing) {
					decorater.print(process, g2Deco, model);
				} else {
					decorater.draw(process, g2Deco, model);
				}
			} catch (RuntimeException e) {
				// catch everything here
				LogService.getRoot().log(Level.WARNING,
						"com.rapidminer.gui.flow.processrendering.draw.ProcessDrawer.decorator_error", e);
			} finally {
				g2Deco.dispose();
			}
		}
	}

	/**
	 * Lets the decorators draw for the specified {@link RenderPhase}.
	 *
	 * @param operator
	 * 		the operator which should be decorated
	 * @param g2
	 * 		the graphics context. Each decorator gets a new context which is disposed
	 * 		afterwards
	 * @param printing
	 * 		if {@code true} we are printing instead of drawing to the screen
	 */
	private void drawOperatorDecorators(final Operator operator, final Graphics2D g2, final boolean printing) {
		double width = model.getProcessWidth(operator.getExecutionUnit()) * (1 / model.getZoomFactor());
		double height = model.getProcessHeight(operator.getExecutionUnit()) * (1 / model.getZoomFactor());
		int borderWidth = 2;
		Shape frame = new Rectangle2D.Double(0 + borderWidth, 0 + borderWidth, width - 2 * borderWidth,
				height - 2 * borderWidth);

		for (OperatorDrawDecorator decorater : operatorDecorators) {
			Graphics2D g2Deco = null;
			try {
				g2Deco = (Graphics2D) g2.create();
				g2Deco.clip(frame);

				if (printing) {
					decorater.print(operator, g2Deco, model);
				} else {
					decorater.draw(operator, g2Deco, model);
				}

			} catch (RuntimeException e) {
				// catch everything here
				LogService.getRoot().log(Level.WARNING,
						"com.rapidminer.gui.flow.processrendering.draw.ProcessDrawer.operator_decorator_error", e);
			} finally {
				g2Deco.dispose();
			}
		}
	}

	/**
	 * Draws text centered in the process.
	 *
	 * @param process
	 * 		the process in question
	 * @param g2
	 * 		the graphics context
	 * @param font
	 * @param text
	 * @param color
	 * @param yOffset
	 */
	private void drawCenteredText(ExecutionUnit process, Graphics2D g2, Font font, String text, Color color,
								  double yOffset) {
		double width = model.getProcessWidth(process);
		double height = model.getProcessHeight(process);

		Graphics2D g2d = (Graphics2D) g2.create();
		g2d.setFont(font);
		Rectangle2D bounds = g2d.getFontMetrics().getStringBounds(text, g2d);

		int x = (int) (width / 2 - bounds.getWidth() / 2);
		int y = (int) (height / 2 + bounds.getHeight() / 2 + yOffset);
		g2d.setColor(color);
		g2d.drawString(text, x, y);

		g2d.dispose();
	}

}
