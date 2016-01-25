/**
 * Copyright (C) 2001-2016 by RapidMiner and the contributors
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

import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Shape;
import java.awt.geom.GeneralPath;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.util.logging.Level;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.UIManager;

import com.rapidminer.gui.flow.processrendering.model.ProcessRendererModel;
import com.rapidminer.gui.flow.processrendering.view.ProcessRendererView;
import com.rapidminer.gui.tools.SwingTools;
import com.rapidminer.operator.ExecutionUnit;
import com.rapidminer.operator.IOObject;
import com.rapidminer.operator.IOObjectCollection;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.ports.IncompatibleMDClassException;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.operator.ports.OutputPort;
import com.rapidminer.operator.ports.Port;
import com.rapidminer.operator.ports.metadata.CollectionMetaData;
import com.rapidminer.operator.ports.metadata.CompatibilityLevel;
import com.rapidminer.operator.ports.metadata.MetaData;
import com.rapidminer.tools.ClassColorMap;
import com.rapidminer.tools.LogService;
import com.rapidminer.tools.ParentResolvingMap;
import com.rapidminer.tools.plugin.Plugin;


/**
 * Contains utility methods which are helpful for drawing a process via Java2D.
 *
 * @author Marco Boeck
 * @since 6.4.0
 *
 */
public final class ProcessDrawUtils {

	/** dummy label used to create disabled icons */
	private static final JLabel DUMMY_LABEL = new JLabel();

	/** shadow color */
	private static final Color TRANSPARENT_GRAY = new Color(Color.GRAY.getRed(), Color.GRAY.getGreen(), Color.GRAY.getBlue(),
			0);

	/** map containing IOObject colors */
	private static ParentResolvingMap<Class<?>, Color> IO_CLASS_TO_COLOR_MAP = new ClassColorMap();

	static {
		try {
			IO_CLASS_TO_COLOR_MAP.parseProperties("com/rapidminer/resources/groups.properties", "io.", ".color",
					OperatorDescription.class.getClassLoader(), null);
		} catch (IOException e) {
			LogService.getRoot().log(Level.WARNING,
					"com.rapidminer.gui.flow.ProcessRenderer.loading_operator_group_colors_error");
		}
	}

	/**
	 * Private constructor which throws if called.
	 */
	private ProcessDrawUtils() {
		throw new UnsupportedOperationException("Static utility class");
	}

	/**
	 * This method adds the colors of the given property file to the global group colors.
	 *
	 * @param groupProperties
	 *            the properties group name
	 * @param pluginName
	 *            the name of the plugin
	 * @param classLoader
	 *            the classloader responsible who registered the colors
	 * @param provider
	 *            the extension for the registered group color
	 */
	public static void registerAdditionalGroupColors(final String groupProperties, final String pluginName,
			final ClassLoader classLoader, final Plugin provider) {
		SwingTools.registerAdditionalGroupColors(groupProperties, pluginName, classLoader, provider);
	}

	/**
	 * This method adds the colors of the given property file to the io object colors.
	 *
	 * @param groupProperties
	 *            the properties group name
	 * @param pluginName
	 *            the name of the plugin
	 * @param classLoader
	 *            the classloader responsible who registered the colors
	 * @param provider
	 *            the extension to registered IOObjects for
	 */
	public static void registerAdditionalObjectColors(final String groupProperties, final String pluginName,
			final ClassLoader classLoader, final Plugin provider) {
		try {
			IO_CLASS_TO_COLOR_MAP.parseProperties(groupProperties, "io.", ".color", classLoader, provider);
		} catch (IOException e) {
			LogService.getRoot().log(Level.WARNING, "com.rapidminer.gui.flow.ProcessRenderer.loading_io_object_colors_error",
					pluginName);
		}
	}

	/**
	 * Returns the color specified for the given type of metadata.
	 *
	 * @param md
	 *            the metadata or {@code null}
	 * @return a color, never {@code null}
	 */
	public static Color getColorFor(final MetaData md) {
		if (md == null) {
			return Color.WHITE;
		}
		if (md instanceof CollectionMetaData) {
			MetaData elementMetaDataRecursive = ((CollectionMetaData) md).getElementMetaDataRecursive();
			if (elementMetaDataRecursive != null) {
				return IO_CLASS_TO_COLOR_MAP.get(elementMetaDataRecursive.getObjectClass());
			} else {
				return IO_CLASS_TO_COLOR_MAP.get(IOObject.class);
			}
		} else {
			return IO_CLASS_TO_COLOR_MAP.get(md.getObjectClass());
		}
	}

	/**
	 * Returns a color for the given {@link Port} depending on the metadata at the port.
	 *
	 * @param port
	 *            the port for which a color should be retrieved
	 * @param defaultColor
	 *            the default color if the port has no data
	 * @param enabled
	 *            if the port is enabled
	 * @return a color, never {@code null}
	 */
	public static Color getColorFor(final Port port, final Color defaultColor, final boolean enabled) {
		if (port == null) {
			throw new IllegalArgumentException("port must not be null!");
		}
		if (defaultColor == null) {
			throw new IllegalArgumentException("defaultColor must not be null!");
		}

		if (!enabled) {
			return Color.LIGHT_GRAY;
		}

		IOObject data = port.getAnyDataOrNull();
		MetaData md = null;
		try {
			md = port.getMetaData(MetaData.class);
		} catch (IncompatibleMDClassException e) {
			// should not happen
			return defaultColor;
		}
		if (data != null) {
			if (data instanceof IOObjectCollection) {
				return IO_CLASS_TO_COLOR_MAP.get(((IOObjectCollection<?>) data).getElementClass(true));
			} else {
				return IO_CLASS_TO_COLOR_MAP.get(data.getClass());
			}
		} else if (md != null) {
			return ProcessDrawUtils.getColorFor(md);
		} else {
			return defaultColor;
		}
	}

	/**
	 * Snaps the given point to the nearest snapping point for the {@link ProcessRendererView}. Does
	 * not check if snapping is enabled!
	 *
	 * @param point
	 *            the point which should snap to the nearest possible grid position
	 * @return the nearest position on the grid, never {@code null}
	 */
	public static Point snap(final Point2D point) {
		if (point == null) {
			throw new IllegalArgumentException("point must not be null!");
		}

		int snappedX = (int) point.getX() - ProcessDrawer.GRID_X_OFFSET;
		int factor = (snappedX + ProcessDrawer.GRID_WIDTH / 2) / ProcessDrawer.GRID_WIDTH;
		snappedX /= ProcessDrawer.GRID_WIDTH;
		snappedX = factor * ProcessDrawer.GRID_WIDTH + ProcessDrawer.GRID_X_OFFSET;

		int snappedY = (int) point.getY() - ProcessDrawer.GRID_Y_OFFSET;
		factor = (snappedY + ProcessDrawer.GRID_HEIGHT / 2) / ProcessDrawer.GRID_HEIGHT;
		snappedY /= ProcessDrawer.GRID_HEIGHT;
		snappedY = factor * ProcessDrawer.GRID_HEIGHT + ProcessDrawer.GRID_Y_OFFSET;

		return new Point(snappedX, snappedY);
	}

	/**
	 * Calculates a {@link Rectangle2D} from the specified start and end point.
	 *
	 * @param start
	 *            the starting point
	 * @param end
	 *            the end point
	 * @return the rectangle or {@code null} if the start/end point was {@code null}
	 */
	public static Rectangle2D createRectangle(final Point start, final Point end) {
		if (start == null || end == null) {
			return null;
		}

		return new Rectangle2D.Double(Math.min(start.getX(), end.getX()), Math.min(start.getY(), end.getY()),
				Math.abs(start.getX() - end.getX()), Math.abs(start.getY() - end.getY()));
	}

	/**
	 * Draws a shadow around the given rectangle.
	 *
	 * @param rect
	 *            the rectangle which should get a shadow
	 * @param g2
	 *            the graphics context to draw the shadow on
	 */
	public static void drawShadow(final Rectangle2D rect, final Graphics2D g2) {
		Graphics2D g2S = (Graphics2D) g2.create();

		Rectangle2D shadow = new Rectangle2D.Double(rect.getX() + 5, rect.getY() + ProcessDrawer.HEADER_HEIGHT + 5,
				rect.getWidth(), rect.getHeight() - ProcessDrawer.HEADER_HEIGHT);
		GeneralPath bottom = new GeneralPath();
		bottom.moveTo(shadow.getX(), rect.getMaxY());
		bottom.lineTo(rect.getMaxX(), rect.getMaxY());
		bottom.lineTo(shadow.getMaxX(), shadow.getMaxY());
		bottom.lineTo(shadow.getMinX(), shadow.getMaxY());
		bottom.closePath();
		g2S.setPaint(new GradientPaint((float) rect.getX(), (float) rect.getMaxY(), Color.gray, (float) rect.getX(),
				(float) shadow.getMaxY(), TRANSPARENT_GRAY));
		g2S.fill(bottom);

		GeneralPath right = new GeneralPath();
		right.moveTo(rect.getMaxX(), shadow.getMinY());
		right.lineTo(shadow.getMaxX(), shadow.getMinY());
		right.lineTo(shadow.getMaxX(), shadow.getMaxY());
		right.lineTo(rect.getMaxX(), rect.getMaxY());
		right.closePath();
		g2S.setPaint(new GradientPaint((float) rect.getMaxX(), (float) shadow.getY(), Color.gray, (float) shadow.getMaxX(),
				(float) shadow.getY(), TRANSPARENT_GRAY));
		g2S.fill(right);

		g2S.dispose();
	}

	/**
	 * Creates a spline connector shape from one {@link Port} to another.
	 *
	 * @param fromPort
	 *            the origin port
	 * @param toPort
	 *            the end port
	 * @param model
	 *            the model required to create port locations
	 * @return the shape representing the connection or {@code null} if the ports have no location
	 *         yet
	 */
	public static Shape createConnector(final OutputPort fromPort, final Port toPort, final ProcessRendererModel model) {
		Point2D from = ProcessDrawUtils.createPortLocation(fromPort, model);
		Point2D to = ProcessDrawUtils.createPortLocation(toPort, model);
		if (from == null || to == null) {
			return null;
		}

		from = new Point2D.Double(from.getX() + ProcessDrawer.PORT_SIZE / 2, from.getY());
		to = new Point2D.Double(to.getX() - ProcessDrawer.PORT_SIZE / 2, to.getY());
		return ProcessDrawUtils.createConnectionSpline(from, to);
	}

	/**
	 * Creates a spline connector shape from one {@link Point2D} to another.
	 *
	 * @param from
	 *            the starting point
	 * @param to
	 *            the end point
	 * @return the shape representing the connection, never {@code null}
	 */
	public static Shape createConnectionSpline(final Point2D from, final Point2D to) {
		if (from == null || to == null) {
			throw new IllegalArgumentException("from and to must not be null!");
		}

		int delta = 10;
		GeneralPath connector = new GeneralPath();
		connector.moveTo(from.getX() + 1, from.getY());
		double cx = (from.getX() + to.getX()) / 2;
		double cy = (from.getY() + to.getY()) / 2;
		if (to.getX() >= from.getX() + 2 * delta) {
			connector.curveTo(cx, from.getY(), cx, from.getY(), cx, cy);
			connector.curveTo(cx, to.getY(), cx, to.getY(), to.getX() - 1, to.getY());
		} else {
			connector.curveTo(from.getX() + delta, from.getY(), from.getX() + delta, cy, cx, cy);
			connector.curveTo(to.getX() - delta, cy, to.getX() - delta, to.getY(), to.getX() - 1, to.getY());
		}
		return connector;
	}

	/**
	 * Returns the location of the given {@link Port}.
	 *
	 * @param port
	 *            the location for this port will be returned
	 * @param model
	 *            the model required to create port locations
	 * @return the point or {@code null} if the port has no location yet
	 */
	public static Point createPortLocation(final Port port, final ProcessRendererModel model) {
		if (port.getPorts() == null) {
			return new Point(0, 0);
		}
		Operator op = port.getPorts().getOwner().getOperator();
		int index = port.getPorts().getAllPorts().indexOf(port);
		int addOffset = 0;
		int xOffset = 1;
		for (int i = 0; i <= index; i++) {
			addOffset += model.getPortSpacing(port.getPorts().getPortByIndex(i));
		}

		ExecutionUnit process;
		Point point;
		if (op == model.getDisplayedChain()) {
			// this is an inner port
			process = port.getPorts().getOwner().getConnectionContext();
			if (port instanceof OutputPort) {
				point = new Point(0 + xOffset, ProcessDrawer.OPERATOR_MIN_HEIGHT / 2 + ProcessDrawer.PORT_OFFSET
						+ index * ProcessDrawer.PORT_SIZE * 3 / 2 + addOffset);
			} else {
				point = new Point((int) Math.ceil(model.getProcessWidth(process) - xOffset),
						ProcessDrawer.OPERATOR_MIN_HEIGHT / 2 + ProcessDrawer.PORT_OFFSET
								+ index * ProcessDrawer.PORT_SIZE * 3 / 2 + addOffset);
			}
		} else {
			// this is an outer port of a nested operator
			process = op.getExecutionUnit();
			Rectangle2D opRect = model.getOperatorRect(op);

			// called before notifaction of added operator was received, no location set yet
			if (opRect == null) {
				return null;
			}

			if (port instanceof InputPort) {
				point = new Point((int) Math.ceil(opRect.getX()), (int) Math.ceil(
						opRect.getY() + ProcessDrawer.PORT_OFFSET + index * ProcessDrawer.PORT_SIZE * 3 / 2 + addOffset));
			} else {
				point = new Point((int) Math.ceil(opRect.getMaxX()), (int) Math.ceil(
						opRect.getY() + ProcessDrawer.PORT_OFFSET + index * ProcessDrawer.PORT_SIZE * 3 / 2 + addOffset));
			}
		}
		return point;
	}

	/**
	 * Returns the absolute point for a given point in a process.
	 *
	 * @param p
	 *            the relative point which is relative to the displayed process
	 * @param processIndex
	 *            the index of the process in question
	 * @param model
	 *            the model required to calculate the absolute location
	 * @return the absolute point (relative to the process renderer view) or {@code null} if the
	 *         process index is invalid
	 */
	public static Point convertToAbsoluteProcessPoint(final Point p, final int processIndex,
			final ProcessRendererModel model) {
		double xOffset = 0;
		for (int i = 0; i < model.getProcesses().size(); i++) {
			xOffset += ProcessDrawer.WALL_WIDTH;
			if (i == processIndex) {
				return new Point((int) (p.getX() + xOffset), (int) (p.getY() + ProcessDrawer.PADDING));
			}
			xOffset += ProcessDrawer.WALL_WIDTH + model.getProcessWidth(model.getProcess(i));
		}
		return null;
	}

	/**
	 * Returns the relative point for a given absolute point.
	 *
	 * @param p
	 *            the relative point which is relative to the displayed process
	 * @param processIndex
	 *            the index of the process in question
	 * @param model
	 *            the model required to calculate the relative location
	 * @return the relative point which is relative to the displayed process or {@code null} if the
	 *         process index is invalid
	 */
	public static Point convertToRelativePoint(final Point p, final int processIndex, final ProcessRendererModel model) {
		double xOffset = 0;
		for (int i = 0; i < model.getProcesses().size(); i++) {
			xOffset += ProcessDrawer.WALL_WIDTH;
			if (i == processIndex) {
				return new Point((int) (p.getX() - xOffset), (int) (p.getY() - ProcessDrawer.PADDING));
			}
			xOffset += ProcessDrawer.WALL_WIDTH + model.getProcessWidth(model.getProcess(i));
		}
		return null;
	}

	/**
	 * Abbreviates the string using {@code ...} if necessary.
	 *
	 * @param string
	 *            the string to shorten
	 * @param g2
	 *            the graphics context
	 * @param maxWidth
	 *            the max width in px the string is allowed to use
	 * @return the shorted string, never {@code null}
	 */
	public static String fitString(String string, final Graphics2D g2, final int maxWidth) {
		if (string == null) {
			throw new IllegalArgumentException("string must not be null!");
		}
		if (g2 == null) {
			throw new IllegalArgumentException("g2 must not be null!");
		}

		Rectangle2D bounds = g2.getFont().getStringBounds(string, g2.getFontRenderContext());
		if (bounds.getWidth() <= maxWidth) {
			return string;
		}
		while (g2.getFont().getStringBounds(string + "...", g2.getFontRenderContext()).getWidth() > maxWidth) {
			if (string.length() == 0) {
				return "...";
			}
			string = string.substring(0, string.length() - 1);
		}
		return string + "...";
	}

	/**
	 * Returns whether the specified {@link Operator} has at least one free input and output port.
	 *
	 * @param operator
	 *            the operator in question
	 * @return {@code true} if the operator has one free input and output port ; {@code false}
	 *         otherwise
	 */
	public static boolean hasOperatorFreePorts(final Operator operator) {
		if (operator == null) {
			return false;
		}

		boolean hasFreeInput = false;
		for (InputPort port : operator.getInputPorts().getAllPorts()) {
			if (!port.isConnected()) {
				hasFreeInput = true;
				break;
			}
		}
		if (!hasFreeInput) {
			return false;
		}
		for (OutputPort port : operator.getOutputPorts().getAllPorts()) {
			if (!port.isConnected()) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Returns whether the specified {@link Operator} can be inserted into the currently hovered
	 * connection. If no connection is being hovered, only checks if the operator has at least one
	 * free input and output port.
	 *
	 * @param operator
	 *            the operator in question
	 * @return {@code true} if the operator has one free input and output port ; {@code false}
	 *         otherwise
	 */
	public static boolean canOperatorBeInsertedIntoConnection(final ProcessRendererModel model, final Operator operator) {
		if (operator == null) {
			return false;
		}

		OutputPort hoveringConnectionSource = model.getHoveringConnectionSource();
		MetaData md = null;
		try {
			md = hoveringConnectionSource != null ? hoveringConnectionSource.getMetaData(MetaData.class) : null;
		} catch (IncompatibleMDClassException e) {
			// should not happen
			return false;
		}
		boolean hasFreeInput = false;
		for (InputPort port : operator.getInputPorts().getAllPorts()) {
			if (!port.isConnected()) {
				if (md != null) {
					if (port.isInputCompatible(md, CompatibilityLevel.PRE_VERSION_5)) {
						hasFreeInput = true;
						break;
					}
				} else {
					hasFreeInput = true;
					break;
				}
			}
		}
		if (!hasFreeInput) {
			return false;
		}
		for (OutputPort port : operator.getOutputPorts().getAllPorts()) {
			if (!port.isConnected()) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Returns the height for the specified {@link Operator}.
	 *
	 * @param operator
	 *            the operator in question
	 * @return
	 */
	public static double calcHeighForOperator(Operator operator) {
		double calcHeight = 40 + ProcessRendererModel.PORT_SIZE * 3 / 2
				* Math.max(operator.getInputPorts().getNumberOfPorts(), operator.getOutputPorts().getNumberOfPorts());
		double height = Math.max(ProcessRendererModel.MIN_OPERATOR_HEIGHT, calcHeight);
		return height;
	}

	/**
	 * Returns the given icon in an appropriate enabled/disabled state.
	 *
	 * @param operator
	 *            if the operator is enabled, returns the passed icon directly, otherwise it is
	 *            displayed as disabled
	 * @param icon
	 *            the icon
	 * @return the original icon (if the given operator is enabled) or the icon in a disabled state
	 */
	public static ImageIcon getIcon(final Operator operator, final ImageIcon icon) {
		if (operator.isEnabled()) {
			return icon;
		} else {
			return (ImageIcon) UIManager.getLookAndFeel().getDisabledIcon(DUMMY_LABEL, icon);
		}
	}
}
