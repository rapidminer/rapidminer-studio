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
package com.rapidminer.gui.dnd;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import java.util.Arrays;

import com.rapidminer.gui.flow.processrendering.annotations.model.OperatorAnnotation;
import com.rapidminer.gui.flow.processrendering.annotations.model.ProcessAnnotation;
import com.rapidminer.gui.flow.processrendering.annotations.model.WorkflowAnnotation;


/**
 * Provides a transferable wrapper for {@link WorkflowAnnotation}s in order to cut/copy/paste them
 * in the process renderer.
 *
 * @author Marco Boeck
 * @since 6.4.0
 */
public class TransferableAnnotation implements Transferable {

	public static final DataFlavor LOCAL_OPERATOR_ANNOTATION_FLAVOR = new DataFlavor(DataFlavor.javaJVMLocalObjectMimeType
			+ ";class=" + OperatorAnnotation.class.getName(), "RapidMiner operator annotation");

	public static final DataFlavor LOCAL_PROCESS_ANNOTATION_FLAVOR = new DataFlavor(DataFlavor.javaJVMLocalObjectMimeType
			+ ";class=" + ProcessAnnotation.class.getName(), "RapidMiner process annotation");

	private static final DataFlavor[] DATA_FLAVORS = { LOCAL_OPERATOR_ANNOTATION_FLAVOR, LOCAL_PROCESS_ANNOTATION_FLAVOR };

	private final WorkflowAnnotation originalAnnotation;

	private final WorkflowAnnotation clonedAnnotation;

	public TransferableAnnotation(WorkflowAnnotation annotation) {
		// reference to original annotation (required to delete annotation in case of cut event)
		this.originalAnnotation = annotation;
		// cloning the annotation ensures that further editing does not affect the copied elements
		if (annotation instanceof ProcessAnnotation) {
			this.clonedAnnotation = annotation.createProcessAnnotation(annotation.getProcess());
		} else if (annotation instanceof OperatorAnnotation) {
			this.clonedAnnotation = annotation.createOperatorAnnotation(((OperatorAnnotation) annotation).getAttachedTo());
		} else {
			this.clonedAnnotation = null;
		}
	}

	@Override
	public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException, IOException {
		if (LOCAL_OPERATOR_ANNOTATION_FLAVOR.equals(flavor) || LOCAL_PROCESS_ANNOTATION_FLAVOR.equals(flavor)) {
			return this.clonedAnnotation;
		} else {
			throw new UnsupportedFlavorException(flavor);
		}
	}

	@Override
	public boolean isDataFlavorSupported(DataFlavor flavor) {
		return Arrays.asList(DATA_FLAVORS).contains(flavor);
	}

	@Override
	public DataFlavor[] getTransferDataFlavors() {
		return DATA_FLAVORS;
	}

	/**
	 * @return the original annotation
	 */
	protected WorkflowAnnotation getAnnotation() {
		return this.originalAnnotation;
	}
}
