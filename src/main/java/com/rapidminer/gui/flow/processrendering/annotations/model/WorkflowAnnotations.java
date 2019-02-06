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
package com.rapidminer.gui.flow.processrendering.annotations.model;

import java.util.LinkedList;
import java.util.List;

import com.rapidminer.operator.ExecutionUnit;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.UserData;


/**
 * Container for workflow annotations which can either be {@link OperatorAnnotation}s or freely
 * placed in the process. Can contain any number of annotations (including none at all).
 *
 * @author Marco Boeck
 * @since 6.4.0
 *
 */
public class WorkflowAnnotations implements UserData<Object> {

	/** the separate annotations contained in this container in draw order */
	private final List<WorkflowAnnotation> annotationsDrawOrder;

	/** the separate annotations contained in this container in event order */
	private final List<WorkflowAnnotation> annotationsEventOrder;

	private final Object lock;

	/**
	 * Creates an empty workflow annotations container.
	 */
	public WorkflowAnnotations() {
		// two lists for performance reasons. Otherwise we'd have to reverse the list each event
		this.annotationsDrawOrder = new LinkedList<>();
		this.annotationsEventOrder = new LinkedList<>();
		this.lock = new Object();
	}

	/**
	 * Returns the {@link WorkflowAnnotation}s contained in this container in drawing order. In
	 * other words, the first element will be drawn first, the last element drawn last.
	 *
	 * @return the list of annotations, never {@code null}
	 */
	public List<WorkflowAnnotation> getAnnotationsDrawOrder() {
		return annotationsDrawOrder;
	}

	/**
	 * Returns the {@link WorkflowAnnotation}s contained in this container in event handling order.
	 * That order is exactly the reversed drawning order. In other words, the first element will be
	 * receive events last, the last element will recieve them first.
	 *
	 * @return the list of annotations, never {@code null}
	 */
	public List<WorkflowAnnotation> getAnnotationsEventOrder() {
		return annotationsEventOrder;
	}

	/**
	 * Returns whether this container contains any actual annotations.
	 *
	 * @return {@code true} if there are annotations; {@code false} otherwise
	 */
	public boolean isEmpty() {
		synchronized (lock) {
			return annotationsDrawOrder.isEmpty();
		}
	}

	/**
	 * Adds the given annotation.
	 *
	 * @param newAnnotation
	 *            the new annotation to be added
	 */
	public void addAnnotation(final WorkflowAnnotation newAnnotation) {
		if (newAnnotation == null) {
			throw new IllegalArgumentException("newAnnotation must not be null!");
		}
		synchronized (lock) {
			this.annotationsDrawOrder.add(newAnnotation);
			this.annotationsEventOrder.add(0, newAnnotation);
		}
	}

	/**
	 * Removes the given annotation.
	 *
	 * @param toDelete
	 *            the annotation to be removed
	 */
	public void removeAnnotation(final WorkflowAnnotation toDelete) {
		if (toDelete == null) {
			throw new IllegalArgumentException("toDelete must not be null!");
		}
		synchronized (lock) {
			this.annotationsDrawOrder.remove(toDelete);
			this.annotationsEventOrder.remove(toDelete);
		}
	}

	/**
	 * Bring the given annotation to the front. That annotation will be drawn over all other
	 * annotations as well as recieve events first.
	 *
	 * @param anno
	 *            the annotation to bring to the front
	 */
	public void toFront(final WorkflowAnnotation anno) {
		if (anno == null) {
			throw new IllegalArgumentException("anno must not be null!");
		}
		synchronized (lock) {
			if (annotationsDrawOrder.remove(anno)) {
				annotationsDrawOrder.add(anno);
			}
			if (annotationsEventOrder.remove(anno)) {
				annotationsEventOrder.add(0, anno);
			}
		}
	}

	/**
	 * Brings the given annotation one layer forward.
	 *
	 * @param anno
	 *            the annotation to bring forward
	 */
	public void sendForward(final WorkflowAnnotation anno) {
		if (anno == null) {
			throw new IllegalArgumentException("anno must not be null!");
		}
		synchronized (lock) {
			int newIndexDraw = annotationsDrawOrder.indexOf(anno) + 1;
			int newIndexEvent = annotationsEventOrder.indexOf(anno) - 1;
			if (annotationsDrawOrder.remove(anno)) {
				if (newIndexDraw >= annotationsDrawOrder.size()) {
					annotationsDrawOrder.add(anno);
				} else {
					annotationsDrawOrder.add(newIndexDraw, anno);
				}
			}
			if (annotationsEventOrder.remove(anno)) {
				if (newIndexEvent < 0) {
					annotationsEventOrder.add(0, anno);
				} else {
					annotationsEventOrder.add(newIndexEvent, anno);
				}
			}
		}
	}

	/**
	 * Bring the given annotation to the back. That annotation will be drawn behind all other
	 * annotations as well as recieve events last.
	 *
	 * @param anno
	 *            the annotation to bring to the front
	 */
	public void toBack(final WorkflowAnnotation anno) {
		if (anno == null) {
			throw new IllegalArgumentException("anno must not be null!");
		}
		synchronized (lock) {
			if (annotationsDrawOrder.remove(anno)) {
				annotationsDrawOrder.add(0, anno);
			}
			if (annotationsEventOrder.remove(anno)) {
				annotationsEventOrder.add(annotationsEventOrder.size(), anno);
			}
		}
	}

	/**
	 * Sends the given annotation one layer backward.
	 *
	 * @param anno
	 *            the annotation to send backward
	 */
	public void sendBack(final WorkflowAnnotation anno) {
		if (anno == null) {
			throw new IllegalArgumentException("anno must not be null!");
		}
		synchronized (lock) {
			int newIndexDraw = annotationsDrawOrder.indexOf(anno) - 1;
			int newIndexEvent = annotationsEventOrder.indexOf(anno) + 1;
			if (annotationsDrawOrder.remove(anno)) {
				if (newIndexDraw < 0) {
					annotationsDrawOrder.add(0, anno);
				} else {
					annotationsDrawOrder.add(newIndexDraw, anno);
				}
			}
			if (annotationsEventOrder.remove(anno)) {
				if (newIndexEvent >= annotationsEventOrder.size()) {
					annotationsEventOrder.add(anno);
				} else {
					annotationsEventOrder.add(newIndexEvent, anno);
				}
			}
		}
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @param newParent
	 *            must be either an {@link Operator} or an {@link ExecutionUnit}.
	 */
	@Override
	public UserData<Object> copyUserData(Object newParent) {
		WorkflowAnnotations copy = new WorkflowAnnotations();
		synchronized (lock) {
			for (WorkflowAnnotation annotation : getAnnotationsDrawOrder()) {
				if (annotation instanceof ProcessAnnotation) {
					copy.addAnnotation(annotation.createProcessAnnotation((ExecutionUnit) newParent));
				} else if (annotation instanceof OperatorAnnotation) {
					copy.addAnnotation(annotation.createOperatorAnnotation((Operator) newParent));
				}
			}
		}
		return copy;
	}

}
