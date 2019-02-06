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
package com.rapidminer.gui.new_plotter.configuration;

import com.rapidminer.gui.new_plotter.configuration.LineFormat.LineStyle;
import com.rapidminer.gui.new_plotter.listener.SeriesFormatListener;
import com.rapidminer.gui.new_plotter.listener.events.SeriesFormatChangeEvent;
import com.rapidminer.gui.new_plotter.listener.events.SeriesFormatChangeEvent.SeriesFormatChangeType;
import com.rapidminer.gui.new_plotter.utility.DataStructureUtils;
import com.rapidminer.tools.I18N;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Polygon;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.TexturePaint;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.lang.ref.WeakReference;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;


/**
 * Defines dot and line format and other format properties for a data series.
 * 
 * @author Marius Helf, Nils Woehler
 * @deprecated since 9.2.0
 */
@Deprecated
public class SeriesFormat implements Cloneable {

	private static class ItemShapeFactory {

		private static final float SHAPE_SIZE = 7.5f;

		public static Shape createSquare() {
			final float s = SHAPE_SIZE;
			float s_d2 = Math.round(s / 2.0);
			float s_dm2 = s_d2 * 2;
			return new Rectangle2D.Float(-s_d2, -s_d2, s_dm2, s_dm2);
		}

		public static Shape createCircle() {
			final float s = SHAPE_SIZE;
			float s_d2 = Math.round(s / 2.0);
			float s_dm2 = s_d2 * 2;
			return new Ellipse2D.Float(-s_d2, -s_d2, s_dm2, s_dm2);
		}

		public static Shape createDiamond() {
			final float s = SHAPE_SIZE;
			double d = Math.round(Math.sqrt(2 * s * s) / 2.0);
			final GeneralPath p = new GeneralPath();
			p.moveTo(0, d);
			p.lineTo(d, 0);
			p.lineTo(0, -d);
			p.lineTo(-d, 0);
			p.closePath();
			return p;

			// final float s = shapeSize;
			// final float l = s / 4.0f;
			// final float t = s / 4.0f;
			//
			// final float SQRT2 = (float) Math.pow(2.0, 0.5);
			// final GeneralPath p0 = new GeneralPath();
			// p0.moveTo(-l - t, -l + t);
			// p0.lineTo(-l + t, -l - t);
			// p0.lineTo(0.0f, -t * SQRT2);
			// p0.lineTo(l - t, -l - t);
			// p0.lineTo(l + t, -l + t);
			// p0.lineTo(t * SQRT2, 0.0f);
			// p0.lineTo(l + t, l - t);
			// p0.lineTo(l - t, l + t);
			// p0.lineTo(0.0f, t * SQRT2);
			// p0.lineTo(-l + t, l + t);
			// p0.lineTo(-l - t, l - t);
			// p0.lineTo(-t * SQRT2, 0.0f);
			// p0.closePath();
			// return p0;
		}

		public static Shape createTriangular() {
			double s = SHAPE_SIZE;
			int s_2 = (int) Math.round(s / 2.0);
			int[] xPoints = new int[] { -s_2, 0, s_2 };
			int[] yPoints = new int[] { s_2, -s_2, s_2 };
			return new Polygon(xPoints, yPoints, xPoints.length);
		}

		public static Shape createTurnedTriangular() {
			double s = SHAPE_SIZE;
			int s_2 = (int) Math.round(s / 2.0);
			int[] xPoints = new int[] { -s_2, 0, s_2 };
			int[] yPoints = new int[] { -s_2, s_2, -s_2 };
			return new Polygon(xPoints, yPoints, xPoints.length);
		}

		public static Shape createCrosshairs() {
			double x = 0;
			double y = 0;
			double pointSize = SHAPE_SIZE - 1.0d;
			int[] xPoints = new int[] { (int) Math.ceil(x - pointSize / 6.0d), (int) Math.ceil(x + pointSize / 6.0d),
					(int) Math.ceil(x + pointSize / 6.0d), (int) Math.ceil(x + pointSize / 2.0d),
					(int) Math.ceil(x + pointSize / 2.0d), (int) Math.ceil(x + pointSize / 6.0d),
					(int) Math.ceil(x + pointSize / 6.0d), (int) Math.ceil(x - pointSize / 6.0d),
					(int) Math.ceil(x - pointSize / 6.0d), (int) Math.ceil(x - pointSize / 2.0d),
					(int) Math.ceil(x - pointSize / 2.0d), (int) Math.ceil(x - pointSize / 6.0d) };
			int[] yPoints = new int[] { (int) Math.ceil(y - pointSize / 2.0d), (int) Math.ceil(y - pointSize / 2.0d),
					(int) Math.ceil(y - pointSize / 6.0d), (int) Math.ceil(y - pointSize / 6.0d),
					(int) Math.ceil(y + pointSize / 6.0d), (int) Math.ceil(y + pointSize / 6.0d),
					(int) Math.ceil(y + pointSize / 2.0d), (int) Math.ceil(y + pointSize / 2.0d),
					(int) Math.ceil(y + pointSize / 6.0d), (int) Math.ceil(y + pointSize / 6.0d),
					(int) Math.ceil(y - pointSize / 6.0d), (int) Math.ceil(y - pointSize / 6.0d) };
			return new Polygon(xPoints, yPoints, xPoints.length);
		}

		public static Shape createHourglass() {
			Area area = new Area();
			area.add(new Area(createTriangular()));
			area.add(new Area(createTurnedTriangular()));
			return area;
		}

		public static Shape createStar() {
			int s_2 = (int) Math.round(0.4 * SHAPE_SIZE);
			AffineTransform t = new AffineTransform();
			t.translate(0, s_2);

			Area area = new Area();
			area.add(new Area(createTriangular()));
			area.add(new Area(t.createTransformedShape(createTurnedTriangular())));
			return area;
		}

	}

	/**
	 * Defines styles for dots in the plot.
	 * 
	 * @author Marius Helf, Nils Woehler
	 * 
	 */
	public enum ItemShape {
		NONE(null, I18N.getGUILabel("plotter.dotstyle.NONE.label")), CIRCLE(ItemShapeFactory.createCircle(), I18N
				.getGUILabel("plotter.dotstyle.CIRCLE.label")), SQUARE(ItemShapeFactory.createSquare(), I18N
				.getGUILabel("plotter.dotstyle.SQUARE.label")), DIAMOND(ItemShapeFactory.createDiamond(), I18N
				.getGUILabel("plotter.dotstyle.DIAMOND.label")), TRIANGLE(ItemShapeFactory.createTriangular(), I18N
				.getGUILabel("plotter.dotstyle.TRIANGLE.label")), TRIANGLE_UPSIDE_DOWN(ItemShapeFactory
				.createTurnedTriangular(), I18N.getGUILabel("plotter.dotstyle.TRIANGLE_UPSIDE_DOWN.label")), STAR(
				ItemShapeFactory.createStar(), I18N.getGUILabel("plotter.dotstyle.STAR.label")), HOURGLASS(ItemShapeFactory
				.createHourglass(), I18N.getGUILabel("plotter.dotstyle.HOURGLASS.label")), CROSSHAIRS(ItemShapeFactory
				.createCrosshairs(), I18N.getGUILabel("plotter.dotstyle.CROSSHAIRS.label")), ;

		private final Shape shape;
		private final String name;

		private ItemShape(Shape shape, String name) {
			this.shape = shape;
			this.name = name;
		}

		public Shape getShape() {
			return shape;
		}

		/**
		 * @return The display name of the enum value.
		 */
		public String getName() {
			return name;
		}
	}

	/**
	 * Defines indicator type of the plot.
	 * 
	 * @author Marius Helf
	 * 
	 */
	public enum IndicatorType {
		NONE(I18N.getGUILabel("plotter.error_indicator.NONE.label")), BARS(I18N
				.getGUILabel("plotter.error_indicator.BARS.label")), BAND(I18N
				.getGUILabel("plotter.error_indicator.BAND.label")), DIFFERENCE(I18N
				.getGUILabel("plotter.error_indicator.DIFFERENCE.label")), ;

		private final String name;

		private IndicatorType(String name) {
			this.name = name;
		}

		/**
		 * @return The display name of the enum value.
		 */
		public String getName() {
			return name;
		}
	}

	public enum FillStyle {
		NONE(I18N.getGUILabel("plotter.fillstyle.NONE.label")), SOLID(I18N.getGUILabel("plotter.fillstyle.SOLID.label")), CROSS_HATCHED(
				I18N.getGUILabel("plotter.fillstyle.CROSS_HATCHED.label")), RISING_HATCHED(I18N
				.getGUILabel("plotter.fillstyle.RISING_HATCHED.label")), FALLING_HATCHED(I18N
				.getGUILabel("plotter.fillstyle.FALLING_HATCHED.label")), VERTICALLY_HATCHED(I18N
				.getGUILabel("plotter.fillstyle.VERTICALLY_HATCHED.label")), HORIZONTALLY_HATCHED(I18N
				.getGUILabel("plotter.fillstyle.HORIZONTALLY_HATCHED.label"));

		private final String name;

		private FillStyle(String name) {
			this.name = name;
		}

		public String getName() {
			return name;
		}
	}

	public enum VisualizationType {
		LINES_AND_SHAPES(I18N.getGUILabel("plotter.series_type.lines_and_shapes")), BARS(I18N
				.getGUILabel("plotter.series_type.bars")), AREA(I18N.getGUILabel("plotter.series_type.area"));

		private final String name;

		private VisualizationType(String name) {
			this.name = name;
		}

		/**
		 * @return The display name of the enum value.
		 */
		public String getName() {
			return name;
		}
	}

	public enum StackingMode {
		NONE(I18N.getGUILabel("plotter.stacking_mode.none")), ABSOLUTE(I18N.getGUILabel("plotter.stacking_mode.absolute")), RELATIVE(
				I18N.getGUILabel("plotter.stacking_mode.percentage"));

		private final String name;

		private StackingMode(String name) {
			this.name = name;
		}

		/**
		 * @return The display name of the enum value.
		 */
		public String getName() {
			return name;
		}
	}

	private ItemShape itemShape = ItemShape.CIRCLE;	// x, square, circle...
	private double itemSize = 1.0;
	private Color itemColor = Color.GRAY;
	private FillStyle areaFillStyle = FillStyle.SOLID;	// hatched, solid, ...
	private int opacity = 255;
	private StackingMode stackingMode = StackingMode.NONE;
	private VisualizationType seriesType = VisualizationType.LINES_AND_SHAPES;
	private IndicatorType utilityUsage = IndicatorType.NONE;
	private LineFormat lineFormat = new LineFormat();

	/**
	 * The other boundary of the filled area e.g. fills only the space between error bars and values
	 */
	private List<WeakReference<SeriesFormatListener>> changedListenerList = new LinkedList<WeakReference<SeriesFormatListener>>();

	public SeriesFormat() {}

	public Paint getAreaFillPaint() {
		return getAreaFillPaint(getItemColor());
	}

	public Paint getAreaFillPaint(Color color) {
		// TODO caching
		return areaFillPaintFactory(areaFillStyle, color);
	}

	public LineStyle getLineStyle() {
		return lineFormat.getStyle();
	}

	public void setLineStyle(LineStyle lineStyle) {
		if (lineStyle != getLineStyle()) {
			lineFormat.setStyle(lineStyle);
			fireChanged(new SeriesFormatChangeEvent(this, lineStyle));
		}
	}

	public ItemShape getItemShape() {
		return itemShape;
	}

	public void setItemShape(ItemShape itemShape) {
		if (itemShape != this.itemShape) {
			this.itemShape = itemShape;
			fireChanged(new SeriesFormatChangeEvent(this, itemShape));
		}
	}

	public double getItemSize() {
		return itemSize;
	}

	public void setItemSize(double itemSize) {
		if (itemSize != this.itemSize) {
			this.itemSize = itemSize;
			fireChanged(new SeriesFormatChangeEvent(this, itemSize));
		}
	}

	public float getLineWidth() {
		return lineFormat.getWidth();
	}

	public void setLineWidth(float lineWidth) {
		if (lineWidth != lineFormat.getWidth()) {
			lineFormat.setWidth(lineWidth);
			fireChanged(new SeriesFormatChangeEvent(this, lineWidth));
		}
	}

	/**
	 * The ready to use item color (including correct opacity).
	 */
	public Color getItemColor() {
		return itemColor;
	}

	/**
	 * Sets the item color to itemColor. The alpha value of itemColor is ignored, instead the
	 * opacity property of this SeriesFormat is applied.
	 */
	public void setItemColor(Color itemColor) {
		if (itemColor != null
				&& (this.itemColor == null || !itemColor.equals(new Color(this.itemColor.getRed(),
						this.itemColor.getGreen(), this.itemColor.getBlue(), itemColor.getAlpha())))) {
			Color opacityItemColor = new Color(itemColor.getRed(), itemColor.getGreen(), itemColor.getBlue(), opacity);
			this.itemColor = opacityItemColor;
			fireChanged(new SeriesFormatChangeEvent(this, SeriesFormatChangeType.ITEM_COLOR, this.itemColor));
		}
	}

	public Color getLineColor() {
		return lineFormat.getColor();
	}

	public void setLineColor(Color lineColor) {
		Color currentColor = lineFormat.getColor();
		if (lineColor != null
				&& (currentColor == null || !lineColor.equals(new Color(currentColor.getRed(), currentColor.getGreen(),
						currentColor.getBlue(), lineColor.getAlpha())))) {
			Color opacityLineColor = new Color(lineColor.getRed(), lineColor.getGreen(), lineColor.getBlue(), opacity);
			lineFormat.setColor(opacityLineColor);
			fireChanged(new SeriesFormatChangeEvent(this, SeriesFormatChangeType.LINE_COLOR, currentColor));
		}
	}

	public FillStyle getAreaFillStyle() {
		return areaFillStyle;
	}

	public void setAreaFillStyle(FillStyle areaFillStyle) {
		if (areaFillStyle != this.areaFillStyle) {
			this.areaFillStyle = areaFillStyle;
			fireChanged(new SeriesFormatChangeEvent(this, areaFillStyle));
		}
	}

	public int getOpacity() {
		return opacity;
	}

	/**
	 * Creates a Paint with the given FillStyle, opacity and the Color returned by getItemColor().
	 */
	private Paint areaFillPaintFactory(FillStyle style, Color color) {
		if (areaFillStyle == FillStyle.NONE) {
			return new Color(0, 0, 0, 0);
		} else if (areaFillStyle == FillStyle.SOLID) {
			return color;
		} else {
			final int s = 10;

			BufferedImage bufferedImage = new BufferedImage(s, s, BufferedImage.TYPE_INT_ARGB);

			Graphics2D g2 = bufferedImage.createGraphics();
			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, // Anti-alias!
					RenderingHints.VALUE_ANTIALIAS_ON);

			// g2.setColor(new Color(0,0,0,0));
			// g2.fillRect(0, 0, 10, 10);
			g2.setColor(color);
			g2.setStroke(new BasicStroke(s / 10.0f, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_MITER));
			if (areaFillStyle == FillStyle.FALLING_HATCHED) {
				g2.drawLine(0, 0, s, s);
				g2.drawLine(0, -s, 2 * s, s);
				g2.drawLine(-s, 0, s, 2 * s);
			} else if (areaFillStyle == FillStyle.RISING_HATCHED) {
				g2.drawLine(0, s, s, 0);
				g2.drawLine(-s, s, s, -s);
				g2.drawLine(0, 2 * s, 2 * s, 0);
			} else if (areaFillStyle == FillStyle.CROSS_HATCHED) {
				g2.drawLine(0, s, s, 0); // /
				g2.drawLine(0, 0, s, s); // \
			} else if (areaFillStyle == FillStyle.HORIZONTALLY_HATCHED) {
				g2.drawLine(0, s / 4, s, s / 4); // -
				g2.drawLine(0, 3 * s / 4, s, 3 * s / 4); // -
			} else if (areaFillStyle == FillStyle.VERTICALLY_HATCHED) {
				g2.drawLine(s / 4, 0, s / 4, s); // |
				g2.drawLine(3 * s / 4, 0, 3 * s / 4, s); // |
			} else {
				throw new RuntimeException("Illegal area fill style: " + areaFillStyle + ". This cannot happen");
			}

			// paint with the texturing brush
			Rectangle2D rect = new Rectangle2D.Double(0, 0, s, s);
			TexturePaint texturePaint = new TexturePaint(bufferedImage, rect);
			return texturePaint;
		}
	}

	public void setOpacity(int opacity) {
		if (opacity != this.opacity) {
			this.itemColor = DataStructureUtils.setColorAlpha(this.itemColor, opacity);
			lineFormat.setColor(DataStructureUtils.setColorAlpha(lineFormat.getColor(), opacity));
			this.opacity = opacity;
			fireChanged(new SeriesFormatChangeEvent(this, opacity));
		}
	}

	public void addChangeListener(SeriesFormatListener listener) {
		changedListenerList.add(new WeakReference<SeriesFormatListener>(listener));
	}

	public void removeChangeListener(SeriesFormatListener listener) {
		Iterator<WeakReference<SeriesFormatListener>> it = changedListenerList.iterator();
		while (it.hasNext()) {
			SeriesFormatListener l = it.next().get();
			if (l == null || listener == l) {
				it.remove();
			}
		}
	}

	private void fireChanged(SeriesFormatChangeEvent e) {
		Iterator<WeakReference<SeriesFormatListener>> it = changedListenerList.iterator();
		while (it.hasNext()) {
			SeriesFormatListener listener = it.next().get();
			if (listener == null) {
				it.remove();
			} else {
				listener.seriesFormatChanged(e);
			}
		}
	}

	public float getStrokeLength() {
		float[] dashArray = lineFormat.getScaledDashArray();
		if (dashArray != null) {
			float sum = 0;
			for (float value : dashArray) {
				sum += value;
			}
			return sum;
		} else {
			return 0;
		}
	}

	public BasicStroke getStroke() {
		return lineFormat.getStroke();
	}

	public VisualizationType getSeriesType() {
		return seriesType;
	}

	public void setSeriesType(VisualizationType seriesType) {
		if (seriesType != this.seriesType) {
			this.seriesType = seriesType;
			fireChanged(new SeriesFormatChangeEvent(this, seriesType));
		}
	}

	public StackingMode getStackingMode() {
		return stackingMode;
	}

	public void setStackingMode(StackingMode stackingMode) {
		if (stackingMode != this.stackingMode) {
			this.stackingMode = stackingMode;
			fireChanged(new SeriesFormatChangeEvent(this, stackingMode));
		}
	}

	public IndicatorType getUtilityUsage() {
		return utilityUsage;
	}

	public void setUtilityUsage(IndicatorType errorIndicator) {
		if (errorIndicator != this.utilityUsage) {
			this.utilityUsage = errorIndicator;
			fireChanged(new SeriesFormatChangeEvent(this, errorIndicator));
		}
	}

	/**
	 * Returns true iff the attribute XYZ to which the dimension refers (e.g. color for Color
	 * dimension, shape for Shape dimension) is to be calculated individually for each item using
	 * the XYZProvider of dimensionConfig. If this function returns false, the respective attribute
	 * is set globally for the whole series.
	 */
	public static boolean calculateIndividualFormatForEachItem(DimensionConfig domainConfig, DimensionConfig dimensionConfig) {
		if (dimensionConfig == null) {
			return false;
		}

		if (dimensionConfig.isGrouping()) {
			return false;
		} else {
			if (domainConfig.isGrouping()) {
				return false;
			} else {
				return true;
			}
		}
	}

	/**
	 * Returns true iff the attribute XYZ to which the dimension refers (e.g. color for Color
	 * dimension, shape for Shape dimension) is to be calculated individually for each series of a
	 * value source using the XYZProvider of dimensionConfig. If this function returns false, the
	 * respective attribute is set globally for the whole value source.
	 */
	public static boolean useSeriesFormatFromDimensionConfig(DimensionConfig domainConfig, DimensionConfig dimensionConfig) {
		if (calculateIndividualFormatForEachItem(domainConfig, dimensionConfig)) {
			return false;
		}

		if (dimensionConfig == null) {
			return false;
		}

		if (dimensionConfig.isGrouping()) {
			return true;
		} else {
			return false;
		}
	}

	@Override
	public SeriesFormat clone() {
		SeriesFormat clone = new SeriesFormat();
		clone.areaFillStyle = areaFillStyle;
		clone.utilityUsage = utilityUsage;
		clone.itemColor = itemColor;
		clone.itemShape = itemShape;
		clone.itemSize = itemSize;
		clone.lineFormat = lineFormat.clone();
		clone.opacity = opacity;
		clone.seriesType = seriesType;
		clone.stackingMode = stackingMode;

		return clone;
	}
}
