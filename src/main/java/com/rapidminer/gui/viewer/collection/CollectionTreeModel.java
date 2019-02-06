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
package com.rapidminer.gui.viewer.collection;

import javax.swing.event.TreeModelListener;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;

import com.rapidminer.operator.GroupedModel;
import com.rapidminer.operator.IOObject;
import com.rapidminer.operator.IOObjectCollection;
import com.rapidminer.operator.learner.meta.MetaModel;


/**
 * Tree model backed by an {@link IOObjectCollection}.
 *
 * @author Simon Fischer
 */
public class CollectionTreeModel implements TreeModel {

	private final CollectionTreeElement root;

	public CollectionTreeModel(CollectionTreeElement root) {
		this.root = root;
	}

	@Override
	public void addTreeModelListener(TreeModelListener l) {
		// tree is immutable, no listeners
	}

	@Override
	public void removeTreeModelListener(TreeModelListener l) {
		// tree is immutable, no listeners
	}

	@Override
	public void valueForPathChanged(TreePath path, Object newValue) {
		throw new UnsupportedOperationException("Tree is immutable.");
	}

	@SuppressWarnings("unchecked")
	@Override
	public Object getChild(Object parent, int index) {
		IOObject parentIOObject = getIOObject(parent);
		IOObject ioobject = null;
		if (parentIOObject instanceof IOObjectCollection) {
			IOObjectCollection<IOObject> col = (IOObjectCollection<IOObject>) parentIOObject;
			ioobject = col.getElement(index, false);
		} else if (parentIOObject instanceof GroupedModel) {
			ioobject = ((GroupedModel) parentIOObject).getModel(index);
		} else if (parentIOObject instanceof MetaModel) {
			ioobject = ((MetaModel) parentIOObject).getModels().get(index);
		}
		return new CollectionTreeElement(ioobject);
	}

	@SuppressWarnings("unchecked")
	@Override
	public int getChildCount(Object parent) {
		IOObject parentIOObject = getIOObject(parent);
		if (parentIOObject instanceof IOObjectCollection) {
			IOObjectCollection<IOObject> col = (IOObjectCollection<IOObject>) parentIOObject;
			return col.size();
		} else if (parentIOObject instanceof GroupedModel) {
			return ((GroupedModel) parentIOObject).getNumberOfModels();
		} else if (parentIOObject instanceof MetaModel) {
			return ((MetaModel) parentIOObject).getModels().size();
		} else {
			return 0;
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public int getIndexOfChild(Object parent, Object child) {
		IOObject parentIOObject = getIOObject(parent);
		if (parentIOObject instanceof IOObjectCollection) {
			IOObjectCollection<IOObject> col = (IOObjectCollection<IOObject>) parentIOObject;
			return col.getObjects().indexOf(child);
		} else if (parentIOObject instanceof GroupedModel) {
			return ((GroupedModel) parentIOObject).getModels().indexOf(child);
		} else if (parentIOObject instanceof MetaModel) {
			return ((MetaModel) parentIOObject).getModels().indexOf(child);
		} else {
			return -1;
		}
	}

	@Override
	public Object getRoot() {
		return root;
	}

	@Override
	public boolean isLeaf(Object node) {
		IOObject ioObject = getIOObject(node);
		return !(ioObject instanceof IOObjectCollection || ioObject instanceof MetaModel || ioObject instanceof GroupedModel);
	}

	private IOObject getIOObject(Object element) {
		return ((CollectionTreeElement) element).getIOObject();
	}
}
