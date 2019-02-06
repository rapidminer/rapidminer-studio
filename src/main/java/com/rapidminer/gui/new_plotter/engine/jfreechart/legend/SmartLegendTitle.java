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

import java.awt.Color;
import java.awt.Font;
import java.awt.LinearGradientPaint;
import java.awt.Paint;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.Rectangle2D;

import org.jfree.chart.LegendItem;
import org.jfree.chart.LegendItemSource;
import org.jfree.chart.block.Arrangement;
import org.jfree.chart.block.Block;
import org.jfree.chart.block.BlockContainer;
import org.jfree.chart.block.BorderArrangement;
import org.jfree.chart.block.CenterArrangement;
import org.jfree.chart.block.LabelBlock;
import org.jfree.chart.title.LegendGraphic;
import org.jfree.chart.title.LegendItemBlockContainer;
import org.jfree.chart.title.LegendTitle;
import org.jfree.ui.RectangleEdge;


/**
 * This class is the GUI container for all legend items.
 * 
 * @author Marius Helf
 * @deprecated since 9.2.0
 */
@Deprecated
public class SmartLegendTitle extends LegendTitle {

	private static final long serialVersionUID = 1L;

	public SmartLegendTitle(LegendItemSource source, Arrangement hLayout, Arrangement vLayout) {
		super(source, hLayout, vLayout);
	}

	public SmartLegendTitle(LegendItemSource source) {
		super(source);
	}

	@Override
	protected Block createLegendItemBlock(LegendItem item) {
		if (item instanceof FlankedShapeLegendItem) {
			return createFlankedShapeLegendItem((FlankedShapeLegendItem) item);
		} else {
			return createDefaultLegendItem(item);
		}
	}

	private Block createDefaultLegendItem(LegendItem item) {
		BlockContainer result = null;

		Shape shape = item.getShape();
		if (shape == null) {
			shape = new Rectangle();
		}
		LegendGraphic lg = new LegendGraphic(shape, item.getFillPaint());
		lg.setFillPaintTransformer(item.getFillPaintTransformer());
		lg.setShapeFilled(item.isShapeFilled());
		lg.setLine(item.getLine());
		lg.setLineStroke(item.getLineStroke());
		lg.setLinePaint(item.getLinePaint());
		lg.setLineVisible(item.isLineVisible());
		lg.setShapeVisible(item.isShapeVisible());
		lg.setShapeOutlineVisible(item.isShapeOutlineVisible());
		lg.setOutlinePaint(item.getOutlinePaint());
		lg.setOutlineStroke(item.getOutlineStroke());
		lg.setPadding(getLegendItemGraphicPadding());

		LegendItemBlockContainer legendItem = new LegendItemBlockContainer(new BorderArrangement(), item.getDataset(),
				item.getSeriesKey());
		lg.setShapeAnchor(getLegendItemGraphicAnchor());
		lg.setShapeLocation(getLegendItemGraphicLocation());
		legendItem.add(lg, getLegendItemGraphicEdge());

		Font textFont = item.getLabelFont();
		if (textFont == null) {
			textFont = getItemFont();
		}
		Paint textPaint = item.getLabelPaint();
		if (textPaint == null) {
			textPaint = getItemPaint();
		}
		LabelBlock labelBlock = new LabelBlock(item.getLabel(), textFont, textPaint);
		labelBlock.setPadding(getItemLabelPadding());
		legendItem.add(labelBlock);
		legendItem.setToolTipText(item.getToolTipText());
		legendItem.setURLText(item.getURLText());

		result = new BlockContainer(new CenterArrangement());
		result.add(legendItem);

		return result;
	}

	private Block createFlankedShapeLegendItem(FlankedShapeLegendItem item) {
		BlockContainer result = null;
		LegendGraphic lg = new CustomLegendGraphic(item.getShape(), item.getFillPaint());
		lg.setFillPaintTransformer(item.getFillPaintTransformer());
		lg.setShapeFilled(item.isShapeFilled());
		lg.setLine(item.getLine());
		lg.setLineStroke(item.getLineStroke());
		lg.setLinePaint(item.getLinePaint());
		lg.setLineVisible(item.isLineVisible());
		lg.setShapeVisible(item.isShapeVisible());
		lg.setShapeOutlineVisible(item.isShapeOutlineVisible());
		lg.setOutlinePaint(item.getOutlinePaint());
		lg.setOutlineStroke(item.getOutlineStroke());
		lg.setPadding(this.getLegendItemGraphicPadding());

		LegendItemBlockContainer legendItem = new LegendItemBlockContainer(new BorderArrangement(), item.getDataset(),
				item.getSeriesKey());
		Font textFont = item.getLabelFont();
		if (textFont == null) {
			textFont = getItemFont();
		}
		Paint textPaint = item.getLabelPaint();
		if (textPaint == null) {
			textPaint = getItemPaint();
		}

		ColoredBlockContainer graphicsContainer = new ColoredBlockContainer(new Color(0, 0, 0, 0), new BorderArrangement());

		LabelBlock labelBlock;
		Font smallerTextFont = textFont.deriveFont(textFont.getSize() * .8f);
		Font labelTextFont = textFont;

		labelBlock = new LabelBlock(item.getLeftShapeLabel(), smallerTextFont, textPaint);
		graphicsContainer.add(labelBlock, RectangleEdge.LEFT);

		graphicsContainer.add(lg, null);

		labelBlock = new LabelBlock(item.getRightShapeLabel(), smallerTextFont, textPaint);
		graphicsContainer.add(labelBlock, RectangleEdge.RIGHT);

		legendItem.add(graphicsContainer, getLegendItemGraphicEdge());

		labelBlock = new LabelBlock(item.getLabel(), labelTextFont, textPaint);
		labelBlock.setPadding(getItemLabelPadding());
		legendItem.add(labelBlock);
		legendItem.setToolTipText(item.getToolTipText());
		legendItem.setURLText(item.getURLText());

		result = new BlockContainer(new CenterArrangement());
		result.add(legendItem);

		return result;
	}

	public static Paint transformLinearGradient(LinearGradientPaint paint, Shape target) {
		Rectangle2D bounds = target.getBounds2D();
		float left = (float) bounds.getMinX();
		float right = (float) bounds.getMaxX();
		LinearGradientPaint newPaint = new LinearGradientPaint(left, 0, right, 0, paint.getFractions(), paint.getColors());
		return newPaint;
	}
}
