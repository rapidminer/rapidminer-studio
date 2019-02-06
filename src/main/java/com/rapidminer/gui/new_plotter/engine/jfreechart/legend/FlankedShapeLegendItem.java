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
package com.rapidminer.gui.new_plotter.engine.jfreechart.legend;

import java.awt.Paint;
import java.awt.Shape;
import java.awt.Stroke;
import java.text.AttributedString;


/**
 * A LegendItem which has a label left and right of the shape in addition to the default label.
 * 
 * @author Marius Helf
 * @deprecated since 9.2.0
 */
@Deprecated
public class FlankedShapeLegendItem extends CustomLegendItem {

	private static final long serialVersionUID = 1L;

	private String leftShapeLabel = null;
	private String rightShapeLabel = null;

	public FlankedShapeLegendItem(AttributedString label, String description, String toolTipText, String urlText,
			boolean shapeVisible, Shape shape, boolean shapeFilled, Paint fillPaint, boolean shapeOutlineVisible,
			Paint outlinePaint, Stroke outlineStroke, boolean lineVisible, Shape line, Stroke lineStroke, Paint linePaint) {
		super(label, description, toolTipText, urlText, shapeVisible, shape, shapeFilled, fillPaint, shapeOutlineVisible,
				outlinePaint, outlineStroke, lineVisible, line, lineStroke, linePaint);
	}

	public FlankedShapeLegendItem(AttributedString label, String description, String toolTipText, String urlText,
			Shape shape, Paint fillPaint, Stroke outlineStroke, Paint outlinePaint) {
		super(label, description, toolTipText, urlText, shape, fillPaint, outlineStroke, outlinePaint);
	}

	public FlankedShapeLegendItem(AttributedString label, String description, String toolTipText, String urlText,
			Shape shape, Paint fillPaint) {
		super(label, description, toolTipText, urlText, shape, fillPaint);
	}

	public FlankedShapeLegendItem(AttributedString label, String description, String toolTipText, String urlText,
			Shape line, Stroke lineStroke, Paint linePaint) {
		super(label, description, toolTipText, urlText, line, lineStroke, linePaint);
	}

	public FlankedShapeLegendItem(String label, Paint paint) {
		super(label, paint);
	}

	public FlankedShapeLegendItem(String label, String description, String toolTipText, String urlText,
			boolean shapeVisible, Shape shape, boolean shapeFilled, Paint fillPaint, boolean shapeOutlineVisible,
			Paint outlinePaint, Stroke outlineStroke, boolean lineVisible, Shape line, Stroke lineStroke, Paint linePaint) {
		super(label, description, toolTipText, urlText, shapeVisible, shape, shapeFilled, fillPaint, shapeOutlineVisible,
				outlinePaint, outlineStroke, lineVisible, line, lineStroke, linePaint);
	}

	public FlankedShapeLegendItem(String label, String description, String toolTipText, String urlText, Shape shape,
			Paint fillPaint, Stroke outlineStroke, Paint outlinePaint) {
		super(label, description, toolTipText, urlText, shape, fillPaint, outlineStroke, outlinePaint);
	}

	public FlankedShapeLegendItem(String label, String description, String toolTipText, String urlText, Shape shape,
			Paint fillPaint) {
		super(label, description, toolTipText, urlText, shape, fillPaint);
	}

	public FlankedShapeLegendItem(String label, String description, String toolTipText, String urlText, Shape line,
			Stroke lineStroke, Paint linePaint) {
		super(label, description, toolTipText, urlText, line, lineStroke, linePaint);
	}

	public FlankedShapeLegendItem(String label) {
		super(label);
	}

	public FlankedShapeLegendItem(String leftShapeLabel, String rightShapeLabel, String label) {
		super(label);
		this.leftShapeLabel = leftShapeLabel;
		this.rightShapeLabel = rightShapeLabel;
	}

	public String getLeftShapeLabel() {
		return leftShapeLabel;
	}

	public void setLeftShapeLabel(String leftShapeLabel) {
		this.leftShapeLabel = leftShapeLabel;
	}

	public String getRightShapeLabel() {
		return rightShapeLabel;
	}

	public void setRightShapeLabel(String rightShapeLabel) {
		this.rightShapeLabel = rightShapeLabel;
	}
}
