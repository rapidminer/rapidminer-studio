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
package com.rapidminer.gui.renderer.cluster;

import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableModel;

import com.rapidminer.gui.renderer.AbstractTableModelTableRenderer;
import com.rapidminer.operator.IOContainer;
import com.rapidminer.operator.clustering.CentroidClusterModel;


/**
 * This is the renderer for the cluster model centroid table renderer.
 *
 * @author Sebastian Land
 */
public class ClusterModelCentroidTableRenderer extends AbstractTableModelTableRenderer {

	private static class CentroidTableModel extends AbstractTableModel {

		private static final long serialVersionUID = 2196512073454635516L;
		private CentroidClusterModel model;

		public CentroidTableModel(CentroidClusterModel model) {
			this.model = model;
		}

		@Override
		public int getColumnCount() {
			return model.getNumberOfClusters() + 1;
		}

		@Override
		public int getRowCount() {
			return model.getAttributeNames().length;
		}

		@Override
		public Object getValueAt(int rowIndex, int columnIndex) {
			if (columnIndex == 0) {
				return model.getAttributeNames()[rowIndex];
			}
			return model.getCentroid(columnIndex - 1).getCentroid()[rowIndex];
		}

		@Override
		public Class<?> getColumnClass(int columnIndex) {
			if (columnIndex == 0) {
				return String.class;
			}
			return Double.class;
		}

		@Override
		public String getColumnName(int columnIndex) {
			if (columnIndex == 0) {
				return "Attribute";
			}
			return "cluster_" + model.getCluster(columnIndex - 1).getClusterId();
		}
	}

	@Override
	public TableModel getTableModel(Object renderable, IOContainer ioContainer, boolean isReporting) {
		CentroidClusterModel clusterModel = (CentroidClusterModel) renderable;
		if (clusterModel != null) {
			return new CentroidTableModel(clusterModel);
		} else {
			return null;
		}
	}

	@Override
	public String getName() {
		return "Centroid Table";
	}

	@Override
	public boolean isAutoresize() {
		return true;
	}
}
