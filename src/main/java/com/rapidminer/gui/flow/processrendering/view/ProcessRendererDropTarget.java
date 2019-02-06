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

import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;
import java.awt.dnd.DropTargetListener;
import java.util.TooManyListenersException;

import javax.swing.TransferHandler;
import javax.swing.event.EventListenerList;


/**
 * Drop target helper class for {@link ProcessRendererView}.
 *
 * @author Simon Fischer
 * @since 6.4.0
 *
 */
class ProcessRendererDropTarget extends DropTarget {

	private static final long serialVersionUID = 1L;

	private EventListenerList dropTragetListenerList;
	private ProcessRendererView view;

	public ProcessRendererDropTarget(final ProcessRendererView view, final DropTargetListener dropTargetListener) {
		super(view, TransferHandler.COPY_OR_MOVE | TransferHandler.LINK, null);
		this.view = view;
		try {
			super.addDropTargetListener(dropTargetListener);
		} catch (TooManyListenersException tmle) {
		}
	}

	@Override
	public void addDropTargetListener(final DropTargetListener dtl) throws TooManyListenersException {
		if (dropTragetListenerList == null) {
			dropTragetListenerList = new EventListenerList();
		}
		dropTragetListenerList.add(DropTargetListener.class, dtl);
	}

	@Override
	public void removeDropTargetListener(final DropTargetListener dtl) {
		if (dropTragetListenerList != null) {
			dropTragetListenerList.remove(DropTargetListener.class, dtl);
		}
	}

	@Override
	public void dragEnter(final DropTargetDragEvent e) {
		super.dragEnter(e);
		if (dropTragetListenerList != null) {
			Object[] listeners = dropTragetListenerList.getListenerList();
			for (int i = listeners.length - 2; i >= 0; i -= 2) {
				if (listeners[i] == DropTargetListener.class) {
					((DropTargetListener) listeners[i + 1]).dragEnter(e);
				}
			}
		}
	}

	@Override
	public void dragOver(final DropTargetDragEvent e) {
		super.dragOver(e);
		if (dropTragetListenerList != null) {
			Object[] listeners = dropTragetListenerList.getListenerList();
			for (int i = listeners.length - 2; i >= 0; i -= 2) {
				if (listeners[i] == DropTargetListener.class) {
					((DropTargetListener) listeners[i + 1]).dragOver(e);
				}
			}
		}
	}

	@Override
	public void dragExit(final DropTargetEvent e) {
		super.dragExit(e);
		view.getModel().setImportDragged(false);
		view.getModel().fireMiscChanged();
		if (dropTragetListenerList != null) {
			Object[] listeners = dropTragetListenerList.getListenerList();
			for (int i = listeners.length - 2; i >= 0; i -= 2) {
				if (listeners[i] == DropTargetListener.class) {
					((DropTargetListener) listeners[i + 1]).dragExit(e);
				}
			}
		}
	}

	@Override
	public void drop(final DropTargetDropEvent e) {
		super.drop(e);
		view.getModel().setImportDragged(false);
		view.getModel().fireMiscChanged();
		if (dropTragetListenerList != null) {
			for (DropTargetListener listener : dropTragetListenerList.getListeners(DropTargetListener.class)) {
				listener.drop(e);
			}
		}
	}

	@Override
	public void dropActionChanged(final DropTargetDragEvent e) {
		super.dropActionChanged(e);
		if (dropTragetListenerList != null) {
			for (DropTargetListener listener : dropTragetListenerList.getListeners(DropTargetListener.class)) {
				listener.dropActionChanged(e);
			}
		}
	}
}
