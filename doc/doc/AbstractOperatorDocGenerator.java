/**
 * Copyright (C) 2001-2019 RapidMiner GmbH
 */
package com.rapidminer.doc;

import java.io.PrintWriter;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
//
//import weka.core.TechnicalInformation;
//import weka.core.TechnicalInformationHandler;

import com.rapidminer.operator.IOObject;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorChain;
import com.rapidminer.operator.OperatorCreationException;
import com.rapidminer.operator.Value;
import com.rapidminer.operator.condition.InnerOperatorCondition;
import com.rapidminer.operator.learner.Learner;
import com.rapidminer.operator.OperatorCapability;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeCategory;
import com.rapidminer.parameter.ParameterTypeStringCategory;
import com.rapidminer.tools.LogService;
import com.rapidminer.tools.OperatorService;
import com.rapidminer.tools.Tools;
import com.sun.javadoc.ClassDoc;
import com.sun.javadoc.RootDoc;
import com.sun.javadoc.SeeTag;
import com.sun.javadoc.Tag;
import com.sun.tools.doclets.Taglet;


/**
 * This generator provides useful methods to generate documentation from the Javadoc comments and abstract methods of an
 * operator. Subclasses can be implemented to deliver different target formats like LaTeX or HTML.
 * 
 * @author Simon Fischer, Ingo Mierswa
 */
public abstract class AbstractOperatorDocGenerator implements OperatorDocGenerator {

	public final static int OPERATOR = 0;

	public final static int OPERATOR_NAME = 1;

	public final static int GROUP_NAME = 2;

	public final static int PARAMETER_LIST = 3;

	public final static int PARAMETER_ITEM = 4;

	public final static int PARAMETER_NAME_REQ = 5;

	public final static int PARAMETER_NAME_OPT = 6;

	public final static int PARAMETER_DESCRIPTION = 7;

	public final static int SHORT_DESCRIPTION = 8;

	public final static int OPERATOR_DESCRIPTION = 9;

	public final static int INPUT_CLASSES_LIST = 10;

	public final static int OUTPUT_CLASSES_LIST = 11;

	public final static int IO_CLASS = 12;

	public final static int INNER_OPERATOR = 13;

	public final static int VALUE_LIST = 14;

	public final static int VALUE_ITEM = 15;

	public final static int VALUE_NAME = 16;

	public final static int VALUE_DESCRIPTION = 17;

	public final static int INDEX_ENTRY = 18;

	public final static int REFERENCE_SECTION = 19;

	public final static int REFERENCE_ENTRY = 20;

	public final static int TECHNICAL_INFORMATION = 21;

	public final static int DEPRECATION_INFO = 22;

	public final static int LEARNER_CAPABILITIES = 23;

	private Map<String, Taglet> tagletMap = new HashMap<String, Taglet>();

	/**
	 * Transform the HTML-comment to the respective output language. The class and operator name are only given for
	 * debugging purposes.
	 */
	 public abstract String transformHTMLJavadocComment(String comment, Class clazz, String operatorName);

	 /** Replace any special characters by an escaped version. */
	 public abstract String escape(String toEscape);

	 public abstract String getOpenTag(int tagNo);

	 public abstract String getCloseTag(int tagNo);

	 public abstract String marginIcon(String iconName);

	 public AbstractOperatorDocGenerator() {
		 CiteTaglet.register(tagletMap);
		 MathTaglet.register(tagletMap);
		 RefTaglet.register(tagletMap);
		 XMLExampleTaglet.register(tagletMap);
	 }

	 public void generateDoc(Operator op, RootDoc rootDoc, PrintWriter out) {
		 ClassDoc opDoc = rootDoc.classNamed(op.getClass().getName());

		 out.println(getOpenTag(OPERATOR));
		 printTags(out, op.getOperatorDescription().getName(), OPERATOR_NAME);
		 out.println();

		 //TODO: Generation does not work anymore because of translation between icon name and image
		 if (op.getOperatorDescription().getIconName() != null)
			 out.println(marginIcon(op.getOperatorDescription().getIconName()));

		 if ((op.getOperatorDescription().getGroup() != null) && (op.getOperatorDescription().getGroup().trim().length() > 0))
			 printTags(out, op.getOperatorDescription().getGroup(), GROUP_NAME);

		 if (opDoc != null) {
			 Tag[] indexTags = opDoc.tags("rapidminer.index");
			 for (int i = 0; i < indexTags.length; i++) {
				 printTags(out, indexTags[i].text(), INDEX_ENTRY);
			 }
		 }

		 if (op.getOperatorDescription().getDeprecationInfo() != null) {
			 printTags(out, op.getOperatorDescription().getDeprecationInfo(), DEPRECATION_INFO);
			 out.println();
		 }

		 Class[] input = op.getInputClasses();
		 if ((input != null) && (input.length > 0)) {
			 out.println(getOpenTag(INPUT_CLASSES_LIST));
			 for (int i = 0; i < input.length; i++) {
				 printTags(out, input[i].getSimpleName(), IO_CLASS);
				 out.println();
			 }
			 out.println(getCloseTag(INPUT_CLASSES_LIST));
		 }

		 Class[] output = op.getOutputClasses();
		 if ((output != null) && (output.length > 0)) {
			 out.println(getOpenTag(OUTPUT_CLASSES_LIST));
			 for (int i = 0; i < output.length; i++) {
				 printTags(out, output[i].getSimpleName(), IO_CLASS);
				 out.println();
			 }
			 out.println(getCloseTag(OUTPUT_CLASSES_LIST));
		 }

		 List parameters = op.getParameterTypes();
		 if (parameters.size() > 0) {
			 out.println(getOpenTag(PARAMETER_LIST));
			 Iterator i = parameters.iterator();
			 while (i.hasNext()) {
				 ParameterType type = (ParameterType) i.next();
				 out.print(getOpenTag(PARAMETER_ITEM));
				 if (type.isOptional()) {
					 printTags(out, type.getKey(), PARAMETER_NAME_OPT);
				 } else {
					 printTags(out, type.getKey(), PARAMETER_NAME_REQ);
				 }
				 if ((type instanceof ParameterTypeCategory) || (type instanceof ParameterTypeStringCategory)) {
					 printTags(out, type.getDescription(), PARAMETER_DESCRIPTION);
				 } else {
					 printTags(out, type.getDescription() + (type.showRange() && type.getRange() != null ? " (" + type.getRange() + ")" : ""), PARAMETER_DESCRIPTION);
				 }
				 out.println(getCloseTag(PARAMETER_ITEM));
			 }
			 out.println(getCloseTag(PARAMETER_LIST));
		 }

		 Collection<Value> values = op.getValues();
		 if (values.size() > 0) {
			 out.println(getOpenTag(VALUE_LIST));
			 Iterator<Value> i = values.iterator();
			 while (i.hasNext()) {
				 Value value = i.next();
				 // if (!value.isDocumented()) continue;
				 out.print(getOpenTag(VALUE_ITEM));
				 printTags(out, value.getKey(), VALUE_NAME);
				 printTags(out, value.getDescription(), VALUE_DESCRIPTION);
				 out.println(getCloseTag(VALUE_ITEM));
			 }
			 out.println(getCloseTag(VALUE_LIST));
		 }

		 if (op instanceof Learner) {
			 Learner learner = (Learner) op;
			 StringBuffer learnerCapabilities = new StringBuffer();
			 boolean first = true;
			 for (OperatorCapability capability : OperatorCapability.values()) {
				 try {
					 if (learner.supportsCapability(capability)) {
						 if (!first)
							 learnerCapabilities.append(", ");
						 learnerCapabilities.append(capability.getDescription());
						 first = false;
					 }
				 } catch (Exception e) {
					 break;
				 }
			 }
			 String result = learnerCapabilities.toString();
			 if (result.length() > 0) {
				 out.print(getOpenTag(LEARNER_CAPABILITIES));
				 out.print(result);
				 out.print(getCloseTag(LEARNER_CAPABILITIES));
			 }
		 }

		 StringBuffer classComment = new StringBuffer();
		 if (opDoc != null) {
			 Tag[] inlineTags = opDoc.inlineTags();
			 for (int i = 0; i < inlineTags.length; i++) {
				 if (inlineTags[i] instanceof SeeTag) {
					 try {
						 Class referencedClass = Class.forName(((SeeTag) inlineTags[i]).referencedClass().qualifiedName());
						 if (Operator.class.isAssignableFrom(referencedClass)) {
							 if (java.lang.reflect.Modifier.isAbstract(referencedClass.getModifiers())) {
								 classComment.append("\\op{" + referencedClass.getSimpleName() + "}");
							 } else {
								 try {
									 Operator refOp = OperatorService.createOperator(referencedClass);
									 classComment.append("\\refop{" + refOp.getOperatorDescription().getName() + "}");
								 } catch (OperatorCreationException e) {
									 classComment.append("\\op{" + referencedClass.getSimpleName() + "}");
								 }
							 }
						 } else if (IOObject.class.isAssignableFrom(referencedClass)) {
							 classComment.append("\\ioobj{" + referencedClass.getSimpleName() + "}");
						 } else {
							 classComment.append("\\java{" + referencedClass.getSimpleName() + "}");
						 }
					 } catch (Throwable e) {
						 LogService.getGlobal().log("In see tag '" + inlineTags[i] + "' of " + op.getClass().getName() + ": " + e, LogService.ERROR);
					 }
				 } else {
					 Taglet taglet = tagletMap.get(inlineTags[i].name().substring(1));
					 if (taglet instanceof TexTaglet) {
						 classComment.append(((TexTaglet) taglet).toTex(inlineTags[i]));
					 } else {
						 classComment.append(escape(inlineTags[i].text()));
					 }
				 }
			 }
		 }

//		 if (op instanceof OperatorChain) {
//			 InnerOperatorCondition condition = ((OperatorChain) op).getInnerOperatorCondition();
//			 if (condition != null) {
//				 out.println(getOpenTag(INNER_OPERATOR));
//				 out.print(transformHTMLJavadocComment(condition.toHTML(), op.getClass(), op.getName()));
//				 out.println(getCloseTag(INNER_OPERATOR));
//			 }
//		 }

		 out.println(getOpenTag(SHORT_DESCRIPTION)
				 + transformHTMLJavadocComment(op.getOperatorDescription().getShortDescription(), op.getClass(), op.getOperatorDescription().getName())
				 + getCloseTag(SHORT_DESCRIPTION));
		 out.println();

		 out.print(getOpenTag(OPERATOR_DESCRIPTION)
				 + transformHTMLJavadocComment(classComment.toString(), op.getClass(), op.getOperatorDescription().getName())
				 + getCloseTag(OPERATOR_DESCRIPTION));
		 out.println();

//		 if (op instanceof TechnicalInformationHandler) {
//			 TechnicalInformation information = ((TechnicalInformationHandler) op).getTechnicalInformation();
//			 if (information != null) {
//				 out.println(getOpenTag(TECHNICAL_INFORMATION)
//						 + transformHTMLJavadocComment(information.toString(), op.getClass(), op.getOperatorDescription().getName())
//						 + getCloseTag(TECHNICAL_INFORMATION));
//				 out.println();
//			 }
//		 }

		 if (opDoc != null) {
			 Tag[] citeTags = opDoc.tags("rapidminer.cite");
			 if (citeTags.length > 0)
				 out.println(getOpenTag(REFERENCE_SECTION));
			 for (int i = 0; i < citeTags.length; i++) {
				 printTags(out, citeTags[i].text(), REFERENCE_ENTRY);
			 }
		 }

		 out.println(getCloseTag(OPERATOR));
	 }

	 private void printTags(PrintWriter out, String text, int tagNo) {
		 out.print(getOpenTag(tagNo) + escape(text) + getCloseTag(tagNo));
	 }

}
