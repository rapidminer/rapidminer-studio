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
package com.rapidminer.gui.flow.processrendering.draw;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.Arc2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RectangularShape;
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
import com.rapidminer.gui.RapidMinerGUI;
import com.rapidminer.gui.flow.processrendering.model.ProcessRendererModel;
import com.rapidminer.gui.flow.processrendering.view.ProcessRendererView;
import com.rapidminer.gui.flow.processrendering.view.RenderPhase;
import com.rapidminer.gui.flow.processrendering.view.components.InterpolationMap;
import com.rapidminer.gui.tools.SwingTools;
import com.rapidminer.io.process.GUIProcessXMLFilter;
import com.rapidminer.operator.ExecutionUnit;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorChain;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.operator.ports.InputPorts;
import com.rapidminer.operator.ports.OutputPort;
import com.rapidminer.operator.ports.OutputPorts;
import com.rapidminer.operator.ports.Port;
import com.rapidminer.operator.ports.Ports;
import com.rapidminer.operator.ports.metadata.CollectionMetaData;
import com.rapidminer.operator.ports.metadata.Precondition;
import com.rapidminer.tools.LogService;


/**
 * This class does the actual Java2D drawing for the {@link ProcessRendererView}.
 *
 * @author Marco Boeck
 * @since 6.4.0
 *
 */
public final class ProcessDrawer {

	public static final Font OPERATOR_FONT = new Font(Font.DIALOG, Font.BOLD, 11);

	public static final int OPERATOR_WIDTH = ProcessRendererModel.OPERATOR_WIDTH;
	public static final int OPERATOR_MIN_HEIGHT = ProcessRendererModel.MIN_OPERATOR_HEIGHT;
	public static final int PORT_SIZE = ProcessRendererModel.PORT_SIZE;
	public static final int PORT_SIZE_HIGHLIGHT = (int) (PORT_SIZE * 1.4f);
	public static final int PORT_OFFSET = OPERATOR_FONT.getSize() + 6 + PORT_SIZE;
	public static final int PADDING = 10;
	public static final int WALL_WIDTH = 25;

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

	public static final Color INNER_DRAG_COLOR = RapidMinerGUI.getBodyHighlightColor();
	public static final Color LINE_DRAG_COLOR = RapidMinerGUI.getBorderHighlightColor();

	public static final int HEADER_HEIGHT = OPERATOR_FONT.getSize() + 7;

	private static final Color INNER_COLOR = Color.WHITE;
	private static final Color SHADOW_COLOR = Color.LIGHT_GRAY;

	private static final Color LINE_COLOR = Color.DARK_GRAY;
	private static final Stroke LINE_STROKE = new BasicStroke(1f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
	private static final Stroke HIGHLIGHT_STROKE = new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
	private static final Stroke SELECTION_RECT_STROKE = new BasicStroke(1f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND,
			5f, new float[] { 2f, 2f }, 0f);
	private static final Paint SELECTION_RECT_PAINT = Color.GRAY;
	private static final Color PROCESS_TITLE_COLOR = SHADOW_COLOR;
	private static final Paint SHADOW_TOP_GRADIENT = new GradientPaint(0, 0, SHADOW_COLOR, PADDING, 0, Color.WHITE);
	private static final Paint SHADOW_LEFT_GRADIENT = new GradientPaint(0, 0, SHADOW_COLOR, 0, PADDING, Color.WHITE);
	private static final Stroke LINE_DRAG_STROKE = new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);

	private static final Font PROCESS_FONT = new Font("Dialog", Font.BOLD, 12);
	private static final Font PORT_FONT = new Font("Dialog", Font.PLAIN, 9);
	private static final Color PORT_NAME_COLOR = Color.DARK_GRAY;
	private static final Color PORT_NAME_SELECTION_COLOR = Color.GRAY;
	private static final Color ACTIVE_EDGE_COLOR = SwingTools.RAPIDMINER_ORANGE;

	private static final Stroke FRAME_STROKE_SELECTED = new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
	private static final Stroke FRAME_STROKE_NORMAL = new BasicStroke(1f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);

	private static final Color FRAME_COLOR_SELECTED = SwingTools.RAPIDMINER_ORANGE;
	private static final Color FRAME_COLOR_NORMAL = LINE_COLOR;

	private static final Paint SHADOW_TOP_DRAG_GRADIENT = new GradientPaint(0, 0, SHADOW_COLOR, PADDING, 0, INNER_DRAG_COLOR);
	private static final Paint SHADOW_LEFT_DRAG_GRADIENT = new GradientPaint(0, 0, SHADOW_COLOR, PADDING, 0,
			INNER_DRAG_COLOR);

	private static final Stroke CONNECTION_LINE_STROKE = new BasicStroke(1.3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
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

	private static final double PORT_SIZE_BACKGROUND = PORT_SIZE * 2f;

	private static final ImageIcon IMAGE_WARNING = SwingTools.createIcon("16/sign_warning.png");
	private static final ImageIcon IMAGE_BREAKPOINT_WITHIN = SwingTools.createIcon("16/breakpoint.png");
	private static final ImageIcon IMAGE_BREAKPOINTS = SwingTools.createIcon("16/breakpoints.png");
	private static final ImageIcon IMAGE_BREAKPOINT_BEFORE = SwingTools.createIcon("16/breakpoint_up.png");
	private static final ImageIcon IMAGE_BREAKPOINT_AFTER = SwingTools.createIcon("16/breakpoint_down.png");
	private static final ImageIcon IMAGE_BREAKPOINT_WITHIN_LARGE = SwingTools.createIcon("24/breakpoint.png");
	private static final ImageIcon IMAGE_BREAKPOINTS_LARGE = SwingTools.createIcon("24/breakpoints.png");
	private static final ImageIcon IMAGE_BREAKPOINT_BEFORE_LARGE = SwingTools.createIcon("24/breakpoint_up.png");
	private static final ImageIcon IMAGE_BREAKPOINT_AFTER_LARGE = SwingTools.createIcon("24/breakpoint_down.png");
	private static final ImageIcon IMAGE_BRANCH = SwingTools.createIcon("16/elements_selection.png");

	private static final ImageIcon OPERATOR_RUNNING = SwingTools.createIcon("16/bullet_triangle_glass_green.png");
	private static final ImageIcon OPERATOR_READY = SwingTools.createIcon("16/bullet_ball_glass_green.png");
	private static final ImageIcon OPERATOR_DIRTY = SwingTools.createIcon("16/bullet_ball_glass_yellow.png");
	private static final ImageIcon OPERATOR_ERROR_ICON = SwingTools.createIcon("16/bullet_ball_glass_red.png");

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
	 *            the model containing the data needed to draw the process. See
	 *            {@link ProcessRendererModel} for a minimal configuration
	 * @param drawHighlight
	 *            if {@code true} will highlight drop area in the process during drag & drop
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
	 *            the graphics context to draw upon
	 * @param printing
	 *            if {@code true} we are printing instead of drawing to the screen
	 */
	public void draw(final Graphics2D g2, final boolean printing) {
		Graphics2D g = (Graphics2D) g2.create();
		g.translate(0, PADDING);

		// draw every process
		for (ExecutionUnit process : model.getProcesses()) {
			g.translate(WALL_WIDTH, 0);

			// draw background in own graphics context to avoid cross-phase manipulations
			Graphics2D gBG = (Graphics2D) g.create();
			drawBackground(process, gBG, printing);
			gBG.dispose();

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

			// draw foreground in own graphics context to avoid cross-phase manipulations
			Graphics2D gFG = (Graphics2D) g.create();
			drawForeground(process, gFG, printing);
			gFG.dispose();

			g.translate(model.getProcessWidth(process) + WALL_WIDTH, 0);
		}
		g.dispose();

		// draw port connection attached to mouse over everything
		Port portSource = model.getConnectingPortSource();
		if (portSource != null && model.getMousePositionRelativeToProcess() != null) {
			Graphics2D conG = (Graphics2D) g2.create();
			// translate to correct position
			int width = WALL_WIDTH;
			int index = model.getProcessIndex(portSource.getPorts().getOwner().getConnectionContext());
			for (int i = 0; i < index; i++) {
				width += model.getProcessWidth(model.getProcess(i)) + WALL_WIDTH * 2;
			}
			conG.translate(width, PADDING);

			conG.setColor(ACTIVE_EDGE_COLOR);
			Point2D fromLoc = ProcessDrawUtils.createPortLocation(portSource, model);
			double x = portSource instanceof OutputPort ? fromLoc.getX() + PORT_SIZE_HIGHLIGHT / 2 : fromLoc.getX()
					- PORT_SIZE_HIGHLIGHT / 2;
			conG.draw(new Line2D.Double(x, fromLoc.getY(), model.getMousePositionRelativeToProcess().getX(), model
					.getMousePositionRelativeToProcess().getY()));
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
	 * Draws the process(es) background based on the {@link ProcessRendererModel} data and then
	 * calls all registered {@link ProcessDrawDecorator}s for the background render phase.
	 *
	 * @param process
	 *            the process to draw the background for
	 * @param g2
	 *            the graphics context to draw upon
	 * @param printing
	 *            if {@code true} we are printing instead of drawing to the screen
	 */
	public void drawBackground(final ExecutionUnit process, final Graphics2D g2, final boolean printing) {
		Graphics2D gBG = (Graphics2D) g2.create();
		renderBackground(process, gBG);
		gBG.dispose();

		// let decorators draw
		drawPhaseDecorators(process, g2, RenderPhase.BACKGROUND, printing);
	}

	/**
	 * Draws process annotations and then calls all registered {@link ProcessDrawDecorator}s for the
	 * annotations render phase.
	 *
	 * @param process
	 *            the process to draw the annotations for
	 * @param g2
	 *            the graphics context to draw upon
	 * @param printing
	 *            if {@code true} we are printing instead of drawing to the screen
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
	 *            the process to draw the operator backgrounds for
	 * @param g2
	 *            the graphics context to draw upon
	 * @param printing
	 *            if {@code true} we are printing instead of drawing to the screen
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
	 *            the process to draw the connections for
	 * @param g2
	 *            the graphics context to draw upon
	 * @param printing
	 *            if {@code true} we are printing instead of drawing to the screen
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
	 *            the process to draw the operator annotations for
	 * @param g2
	 *            the graphics context to draw upon
	 * @param printing
	 *            if {@code true} we are printing instead of drawing to the screen
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
	 *            the process to draw the operators for
	 * @param g2
	 *            the graphics context to draw upon
	 * @param printing
	 *            if {@code true} we are printing instead of drawing to the screen
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
	 *            the process to draw the operator additions for
	 * @param g2
	 *            the graphics context to draw upon
	 * @param printing
	 *            if {@code true} we are printing instead of drawing to the screen
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
	 *            the process to draw the overlay for
	 * @param g2
	 *            the graphics context to draw upon
	 * @param printing
	 *            if {@code true} we are printing instead of drawing to the screen
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
	 *            the process to draw the foreground for
	 * @param g2
	 *            the graphics context to draw upon
	 * @param printing
	 *            if {@code true} we are printing instead of drawing to the screen
	 */
	public void drawForeground(final ExecutionUnit process, final Graphics2D g2, final boolean printing) {
		// let decorators draw
		drawPhaseDecorators(process, g2, RenderPhase.FOREGROUND, printing);
	}

	/**
	 * Draws the given {@link Operator} if inside the graphics clip bounds.
	 *
	 * @param op
	 *            the operator to draw. Note that it must have a position attached, see
	 *            {@link GUIProcessXMLFilter}
	 * @param drawPorts
	 *            if {@true} will also draw operator ports, otherwise will not draw ports
	 * @param g2
	 *            the graphics context to draw upon
	 * @param printing
	 *            if {@code true} we are printing instead of drawing to the screen
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
	 *            the decorator instance to add
	 * @param phase
	 *            the phase during which the decorator should be called to draw. If multiple
	 *            decorators want to draw during the same phase, they are called in the order they
	 *            were registered
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
	 *            the decorator instance to remove
	 * @param phase
	 *            the phase from which the decorator should be removed
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
	 *            the decorator instance to add
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
	 *            the decorator instance to remove
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
	 *            the operator to draw
	 * @param g2
	 *            the graphics context
	 */
	private void renderOperator(final Operator operator, final Graphics2D g2) {
		Rectangle2D frame = model.getOperatorRect(operator);
		// the first paint can come before any of the operator register listeners fire
		// thus we need to check the rect for null and set it here once
		// all subsequent calls will then have a valid rect
		if (frame == null) {
			return;
		}

		final double headerHeight = HEADER_HEIGHT;
		double headerWidth;
		Shape bodyShape;
		InterpolationMap map = model.getNameRolloutInterpolationMap();
		double nameRollout = map != null ? map.getValue(operator) : 0;
		if (nameRollout > 0) {
			Rectangle2D nameBounds = OPERATOR_FONT.getStringBounds(operator.getName(), g2.getFontRenderContext());
			headerWidth = nameBounds.getWidth() + 6;
			if (headerWidth > frame.getWidth()) {
				double dif = headerWidth - frame.getWidth();
				headerWidth = frame.getWidth() + nameRollout * dif;
				GeneralPath path = new GeneralPath();
				path.moveTo(frame.getMinX(), frame.getMinY());
				path.lineTo(frame.getMinX() + headerWidth, frame.getMinY());
				path.lineTo(frame.getMinX() + headerWidth, frame.getMinY() + headerHeight);
				path.lineTo(frame.getMaxX(), frame.getMinY() + headerHeight);
				path.lineTo(frame.getMaxX(), frame.getMaxY());
				path.lineTo(frame.getMinX(), frame.getMaxY());
				path.closePath();
				bodyShape = path;
			} else {
				headerWidth = frame.getWidth();
				bodyShape = frame;
			}
		} else {
			headerWidth = frame.getWidth();
			bodyShape = frame;
		}

		// Shadow
		if (!model.getSelectedOperators().isEmpty() && operator == model.getSelectedOperators().get(0)) {
			ProcessDrawUtils.drawShadow(frame, g2);
		}

		// Frame head
		Color baseColor = SwingTools.getOperatorColor(operator);
		if (!operator.isEnabled()) {
			baseColor = Color.LIGHT_GRAY;
		}

		g2.setPaint(baseColor);
		g2.fill(frame);

		// head gradient
		Rectangle2D bar = new Rectangle2D.Double(frame.getX(), frame.getY(), headerWidth, headerHeight);
		Color c0 = new Color(Math.max(baseColor.getRed() - 25, 0), Math.max(baseColor.getGreen() - 25, 0), Math.max(
				baseColor.getBlue() - 25, 0));
		Color c1 = baseColor;
		Rectangle2D[] regions = createSplitHorizontalBar(bar, 0.0, 0.2, 0.6);

		GradientPaint gp = new GradientPaint(0.0f, (float) regions[0].getMinY(), c0, 0.0f, (float) regions[0].getMaxX(), c1);
		g2.setPaint(gp);
		g2.fill(regions[0]);

		gp = new GradientPaint(0.0f, (float) regions[1].getMinY(), c1, 0.0f, (float) regions[1].getMaxY(), Color.WHITE);
		g2.setPaint(gp);
		g2.fill(regions[1]);

		gp = new GradientPaint(0.0f, (float) regions[2].getMinY(), Color.WHITE, 0.0f, (float) regions[2].getMaxY(), c1);
		g2.setPaint(gp);
		g2.fill(regions[2]);

		gp = new GradientPaint(0.0f, (float) regions[3].getMinY(), c1, 0.0f, (float) regions[3].getMaxY(), c0.darker());
		g2.setPaint(gp);
		g2.fill(regions[3]);

		// Frame Body
		g2.setPaint(LINE_COLOR);
		g2.setStroke(LINE_STROKE);
		if (model.getSelectedOperators().contains(operator)) {
			g2.setPaint(FRAME_COLOR_SELECTED);
			g2.setStroke(FRAME_STROKE_SELECTED);
		} else {
			g2.setPaint(FRAME_COLOR_NORMAL);
			g2.setStroke(FRAME_STROKE_NORMAL);
		}
		g2.draw(bodyShape);

		// Label: Name
		g2.setFont(OPERATOR_FONT);
		if (operator.isEnabled()) {
			if (operator == model.getHoveringOperator()) {
				g2.setPaint(baseColor.darker());
			} else if (model.getSelectedOperators().contains(operator)) {
				g2.setPaint(baseColor.darker());
			} else {
				g2.setPaint(baseColor.darker().darker());
			}
		} else {
			g2.setPaint(baseColor.darker().darker());
		}
		g2.drawString(ProcessDrawUtils.fitString(operator.getName(), g2, (int) headerWidth - 3), (int) frame.getX() + 4,
				(int) (frame.getY() + OPERATOR_FONT.getSize() + 1));

		// Icon
		ImageIcon icon = operator.getOperatorDescription().getIcon();
		if (icon != null) {
			if (!operator.isEnabled()) {
				icon = ProcessDrawUtils.getIcon(operator, icon);
			}
			icon.paintIcon(
					null,
					g2,
					(int) (frame.getX() + frame.getWidth() / 2 - icon.getIconWidth() / 2),
					(int) (frame.getY() + headerHeight + (frame.getHeight() - headerHeight - 10) / 2 - icon.getIconHeight() / 2));
		}

		// Small icons
		int iconX = (int) frame.getX() + 3;
		// Dirtyness
		ImageIcon opIcon;
		if (operator.isRunning()) {
			opIcon = OPERATOR_RUNNING;
		} else if (!operator.isDirty()) {
			opIcon = OPERATOR_READY;
		} else if (!operator.getErrorList().isEmpty()) {
			opIcon = OPERATOR_ERROR_ICON;
		} else {
			opIcon = OPERATOR_DIRTY;
		}
		ProcessDrawUtils.getIcon(operator, opIcon).paintIcon(null, g2, iconX,
				(int) (frame.getY() + frame.getHeight() - opIcon.getIconHeight() - 1));
		iconX += opIcon.getIconWidth() + 1;

		// Errors
		if (!operator.getErrorList().isEmpty()) {
			ProcessDrawUtils.getIcon(operator, IMAGE_WARNING).paintIcon(null, g2, iconX,
					(int) (frame.getY() + frame.getHeight() - IMAGE_WARNING.getIconHeight() - 1));
		}
		iconX += IMAGE_WARNING.getIconWidth() + 1;

		// Breakpoint
		if (operator.hasBreakpoint()) {
			ImageIcon breakpointIcon;
			if (operator.getNumberOfBreakpoints() == 1) {
				if (operator.hasBreakpoint(BreakpointListener.BREAKPOINT_BEFORE)) {
					breakpointIcon = IMAGE_BREAKPOINT_BEFORE;
				} else if (operator.hasBreakpoint(BreakpointListener.BREAKPOINT_AFTER)) {
					breakpointIcon = IMAGE_BREAKPOINT_AFTER;
				} else {
					breakpointIcon = IMAGE_BREAKPOINT_WITHIN;
				}
			} else {
				breakpointIcon = IMAGE_BREAKPOINTS;
			}
			ProcessDrawUtils.getIcon(operator, breakpointIcon).paintIcon(null, g2, iconX,
					(int) (frame.getY() + frame.getHeight() - breakpointIcon.getIconHeight() - 1));
		}
		iconX += IMAGE_BREAKPOINTS.getIconWidth() + 1;

		// placeholder for workflow annotations icon
		iconX += IMAGE_BREAKPOINTS.getIconWidth() + 1;

		if (operator instanceof OperatorChain) {
			ProcessDrawUtils.getIcon(operator, IMAGE_BRANCH).paintIcon(null, g2, iconX,
					(int) (frame.getY() + frame.getHeight() - IMAGE_BRANCH.getIconHeight() - 1));
		}
		iconX += IMAGE_BRANCH.getIconWidth() + 1;
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

			double x = location.getX();
			double y = location.getY();
			Color line = Color.DARK_GRAY;
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
				ellipseTop = new Arc2D.Double(
						new Rectangle2D.Double(x - portSize / 2, y - portSize / 2, portSize, portSize), startAngle, 90,
						Arc2D.PIE);
				ellipseBottom = new Arc2D.Double(new Rectangle2D.Double(x - portSize / 2, y - portSize / 2, portSize,
						portSize), startAngle + 90, 90, Arc2D.PIE);
				ellipseBoth = new Arc2D.Double(
						new Rectangle2D.Double(x - portSize / 2, y - portSize / 2, portSize, portSize), startAngle, 180,
						Arc2D.PIE);
			} else {
				startAngle = 270;
				ellipseBottom = new Arc2D.Double(new Rectangle2D.Double(x - portSize / 2, y - portSize / 2, portSize,
						portSize), startAngle, 90, Arc2D.PIE);
				ellipseTop = new Arc2D.Double(
						new Rectangle2D.Double(x - portSize / 2, y - portSize / 2, portSize, portSize), startAngle + 90, 90,
						Arc2D.PIE);
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
					g.setStroke(HIGHLIGHT_STROKE);
				} else {
					g.setStroke(LINE_STROKE);
				}
				g.draw(ellipseBoth);
			} else {
				g.setColor(fill);
				g.fill(ellipseBoth);

				g.setColor(line);
				if (model.getHoveringPort() == port) {
					g.setStroke(HIGHLIGHT_STROKE);
				} else {
					g.setStroke(LINE_STROKE);
				}
				g.draw(ellipseBoth);
			}

			g.setFont(PORT_FONT);
			int xt;
			Rectangle2D strBounds = PORT_FONT.getStringBounds(port.getShortName(), g.getFontRenderContext());
			if (input) {
				xt = PORT_SIZE / 2;
			} else {
				xt = -(int) strBounds.getWidth() - 3;
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

			g.drawString(port.getShortName(), (int) x + xt, (int) (y + strBounds.getHeight() / 2 - 2));
		}
	}

	/**
	 * Draws the connections for the given ports.
	 *
	 * @param ports
	 *            the output ports for which to draw connections
	 * @param g2
	 *            the graphics context to draw upon
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
	 *            the operator to draw the background for
	 * @param g2
	 *            the graphics context to draw upon
	 */
	private void renderOperatorBackground(final Operator operator, final Graphics2D g2) {
		Rectangle2D frame = model.getOperatorRect(operator);
		// the first paint can come before any of the operator register listeners fire
		// thus we need to check the rect for null and set it here once
		// all subsequent calls will then have a valid rect
		if (frame == null) {
			return;
		}

		RoundRectangle2D background = new RoundRectangle2D.Double(frame.getX() - 7, frame.getY() - 7, frame.getWidth() + 14,
				frame.getHeight() + 14, 10, 10);
		g2.setColor(Color.WHITE);
		g2.fill(background);

		// render ports
		renderPortsBackground(operator.getInputPorts(), g2);
		renderPortsBackground(operator.getOutputPorts(), g2);
	}

	/**
	 * Draws the connections background (round pipe) for the given ports.
	 *
	 * @param inputPorts
	 *            the input ports for which to draw connection backgrounds
	 * @param ports
	 *            the output ports for which to draw connection backgrounds
	 * @param g2
	 *            the graphics context to draw upon
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
				ellipseBoth = new Arc2D.Double(new Rectangle2D.Double(x - PORT_SIZE_BACKGROUND / 2, y - PORT_SIZE_BACKGROUND
						/ 2, PORT_SIZE_BACKGROUND, PORT_SIZE_BACKGROUND), startAngle, 180, Arc2D.PIE);
			} else {
				startAngle = 270;
				ellipseBoth = new Arc2D.Double(new Rectangle2D.Double(x - PORT_SIZE_BACKGROUND / 2, y - PORT_SIZE_BACKGROUND
						/ 2, PORT_SIZE_BACKGROUND, PORT_SIZE_BACKGROUND), startAngle, 180, Arc2D.PIE);
			}

			g2.setColor(Color.WHITE);
			g2.fill(ellipseBoth);

		}
	}

	/**
	 * Draws the background for the given process.
	 *
	 * @param process
	 *            the process for which to render the background
	 * @param g2
	 *            the graphics context to draw upon
	 */
	private void renderBackground(final ExecutionUnit process, final Graphics2D g2) {
		double width = model.getProcessWidth(process);
		double height = model.getProcessHeight(process);
		Shape frame = new Rectangle2D.Double(0, 0, width, height);

		Paint currentInnerColor = INNER_COLOR;
		Paint currentTopGradient = SHADOW_TOP_GRADIENT;
		Paint currentLeftGradient = SHADOW_LEFT_GRADIENT;
		Stroke currentLineStroke = LINE_STROKE;
		Paint currentLineColor = LINE_COLOR;

		if (drawHighlight && (model.isDragStarted() || model.isDropTargetSet() && model.isImportDragged())) {
			switch (RapidMinerGUI.getDragHighlighteMode()) {
				case FULL:
					currentInnerColor = INNER_DRAG_COLOR;
					currentTopGradient = SHADOW_TOP_DRAG_GRADIENT;
					currentLeftGradient = SHADOW_LEFT_DRAG_GRADIENT;
					//$FALL-THROUGH$
				case BORDER:
					currentLineStroke = LINE_DRAG_STROKE;
					currentLineColor = LINE_DRAG_COLOR;
					break;
				case NONE:
				default:
					break;
			}
		}

		// background color
		g2.setPaint(currentInnerColor);
		g2.fill(frame);

		// process title
		g2.setColor(PROCESS_TITLE_COLOR);
		g2.setFont(PROCESS_FONT);
		Operator displayedChain = process.getEnclosingOperator();
		g2.drawString(process.getName(), PADDING + 2, PROCESS_FONT.getSize() + PADDING);

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
					(int) width - PADDING - IMAGE_BREAKPOINTS_LARGE.getIconWidth(), PADDING);
		}

		// padding gradients
		g2.setPaint(currentTopGradient);
		g2.fill(new Rectangle2D.Double(0, 0, PADDING, height));
		GeneralPath top = new GeneralPath();
		int shadowWidth = PADDING;
		top.moveTo(0, 0);
		top.lineTo(width, 0);
		top.lineTo(width, shadowWidth);
		top.lineTo(shadowWidth, shadowWidth);
		top.closePath();
		g2.setPaint(currentLeftGradient);
		g2.fill(top);

		// frame color
		g2.setPaint(currentLineColor);
		g2.setStroke(currentLineStroke);
		g2.draw(frame);
	}

	/**
	 * Creates a split horizontal bar for dividing operator header and body.
	 *
	 * @param bar
	 * @param a
	 * @param b
	 * @param c
	 * @return
	 */
	private Rectangle2D[] createSplitHorizontalBar(final RectangularShape bar, final double a, final double b, final double c) {
		Rectangle2D[] result = new Rectangle2D[4];
		double y0 = bar.getMinY();
		double y1 = Math.rint(y0 + bar.getHeight() * a);
		double y2 = Math.rint(y0 + bar.getHeight() * b);
		double y3 = Math.rint(y0 + bar.getHeight() * c);
		result[0] = new Rectangle2D.Double(bar.getMinX(), bar.getMinY(), bar.getWidth(), y1 - y0);
		result[1] = new Rectangle2D.Double(bar.getMinX(), y1, bar.getWidth(), y2 - y1);
		result[2] = new Rectangle2D.Double(bar.getMinX(), y2, bar.getWidth(), y3 - y2);
		result[3] = new Rectangle2D.Double(bar.getMinX(), y3, bar.getWidth(), bar.getMaxY() - y3);
		return result;
	}

	/**
	 * Lets the decorators draw for the specified {@link RenderPhase}.
	 *
	 * @param process
	 *            the process which should be decorated
	 * @param g2
	 *            the graphics context. Each decorator gets a new context which is disposed
	 *            afterwards
	 * @param phase
	 *            the render phase which determines the decorators that are called
	 * @param printing
	 *            if {@code true} we are printing instead of drawing to the screen
	 */
	private void drawPhaseDecorators(final ExecutionUnit process, final Graphics2D g2, final RenderPhase phase,
			final boolean printing) {
		for (ProcessDrawDecorator decorater : decorators.get(phase)) {
			Graphics2D g2Deco = null;
			try {
				g2Deco = (Graphics2D) g2.create();
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
	 *            the operator which should be decorated
	 * @param g2
	 *            the graphics context. Each decorator gets a new context which is disposed
	 *            afterwards
	 * @param printing
	 *            if {@code true} we are printing instead of drawing to the screen
	 */
	private void drawOperatorDecorators(final Operator operator, final Graphics2D g2, final boolean printing) {
		for (OperatorDrawDecorator decorater : operatorDecorators) {
			Graphics2D g2Deco = null;
			try {
				g2Deco = (Graphics2D) g2.create();
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
}
