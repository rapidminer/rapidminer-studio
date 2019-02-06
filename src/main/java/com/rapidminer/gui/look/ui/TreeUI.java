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
package com.rapidminer.gui.look.ui;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.Rectangle;

import javax.swing.JComponent;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicTreeUI;
import javax.swing.tree.TreePath;


/**
 * The UI for trees.
 * 
 * @author Ingo Mierswa
 */
public class TreeUI extends BasicTreeUI {

	public static ComponentUI createUI(JComponent x) {
		return new TreeUI();
	}

	public TreeUI() {
		setHashColor(Color.LIGHT_GRAY);
	}

	@Override
	protected int getHorizontalLegBuffer() {
		return 4;
	}

	@Override
	public void installUI(JComponent c) {
		super.installUI(c);

	}

	@Override
	public void uninstallUI(JComponent c) {
		super.uninstallUI(c);
	}

	protected boolean isLocationInExpandControl(int row, int rowLevel, int mouseX, int mouseY) {
		if ((this.tree != null) && !isLeaf(row)) {
			int boxWidth;

			if (getExpandedIcon() != null) {
				boxWidth = getExpandedIcon().getIconWidth() + 6;
			} else {
				boxWidth = 8;
			}

			Insets i = this.tree.getInsets();
			int boxLeftX = (i != null) ? i.left : 0;

			boxLeftX += (((rowLevel + this.depthOffset - 1) * this.totalChildIndent) + getLeftChildIndent()) - boxWidth / 2;

			int boxRightX = boxLeftX + boxWidth;

			return (mouseX >= boxLeftX) && (mouseX <= boxRightX);
		}
		return false;
	}

	protected void paintHorizontalSeparators(Graphics g, JComponent c) {
		Rectangle clipBounds = g.getClipBounds();

		int beginRow = getRowForPath(this.tree, getClosestPathForLocation(this.tree, 0, clipBounds.y));
		int endRow = getRowForPath(this.tree, getClosestPathForLocation(this.tree, 0, clipBounds.y + clipBounds.height - 1));

		if ((beginRow <= -1) || (endRow <= -1)) {
			return;
		}

		for (int i = beginRow; i <= endRow; ++i) {
			TreePath path = getPathForRow(this.tree, i);

			if ((path != null) && (path.getPathCount() == 2)) {
				Rectangle rowBounds = getPathBounds(this.tree, getPathForRow(this.tree, i));

				// Draw a line at the top
				if (rowBounds != null) {
					g.drawLine(clipBounds.x, rowBounds.y, clipBounds.x + clipBounds.width, rowBounds.y);
				}
			}
		}

	}
}
