/**
 * Copyright (C) 2001-2019 RapidMiner GmbH
 */
package com.rapidminer.doc;

import java.io.PrintWriter;

import com.rapidminer.operator.Operator;
import com.sun.javadoc.RootDoc;


/**
 * Generates the documentation for operators.
 * 
 * @author Simon Fischer
 */
public interface OperatorDocGenerator {

	/** Generates the documentation for this operator and writes it to the given writer. */
	public void generateDoc(Operator operator, RootDoc rootDoc, PrintWriter out);

	/** Generates the header for the group with the given name. The name might be null. */
	public void beginGroup(String groupName, PrintWriter out);

	/** Generates the footer for the group with the given name. The name might be null. */
	public void endGroup(String groupName, PrintWriter out);
}
