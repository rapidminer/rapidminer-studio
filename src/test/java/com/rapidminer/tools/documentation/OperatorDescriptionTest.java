package com.rapidminer.tools.documentation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.net.URL;
import java.util.List;

import org.junit.Test;


/**
 * Tests the parsing of the operator documentation.
 *
 * @author Marcel Michel
 *
 */
public class OperatorDescriptionTest {

	private static final String OPERATOR_SUFFIX = "operator.";

	private static final String OPERATOR_DOCUMENTATION_NO_TAGS = "com/rapidminer/tools/documentation/OperatorTagsTest_None.xml";
	private static final String OPERATOR_KEY_NO_TAGS = "operator_no_tags";

	private static final String OPERATOR_DOCUMENTATION_SINGLE_TAG = "com/rapidminer/tools/documentation/OperatorTagsTest_Single.xml";
	private static final String OPERATOR_KEY_SINGLE_TAG = "operator_single_tag";

	private static final String OPERATOR_DOCUMENTATION_MULTIPLE_TAGS = "com/rapidminer/tools/documentation/OperatorTagsTest_Two.xml";
	private static final String OPERATOR_KEY_MULTIPLE_TAGS = "operator_two_tags";

	private OperatorDocumentation parseOperatorDocumentation(String resource, String operatorKey) throws IOException {
		ClassLoader classLoader = OperatorDescriptionTest.class.getClassLoader();
		URL url = classLoader.getResource(resource);
		assertNotNull("Resource " + resource + " not found", url);
		new XMLOperatorDocBundle(url, resource);

		OperatorDocBundle docBundle = XMLOperatorDocBundle.load(classLoader, resource.replace(".xml", ""));
		assertNotNull("DocBundle could not be found", docBundle);

		Object result = docBundle.handleGetObject(OPERATOR_SUFFIX + operatorKey);
		assertNotNull("Operator Documentation could not be found.", result);
		if (!(result instanceof OperatorDocumentation)) {
			fail("DocBundle did not return an OperatorDocumentation.");
		}
		return (OperatorDocumentation) result;
	}

	@Test
	public void noTagsTest() throws IOException {
		OperatorDocumentation operatorDocumentation = parseOperatorDocumentation(OPERATOR_DOCUMENTATION_NO_TAGS,
				OPERATOR_KEY_NO_TAGS);
		assertEquals(0, operatorDocumentation.getTags().size());
	}

	@Test
	public void singleTagTest() throws IOException {
		OperatorDocumentation operatorDocumentation = parseOperatorDocumentation(OPERATOR_DOCUMENTATION_SINGLE_TAG,
				OPERATOR_KEY_SINGLE_TAG);
		List<String> tagList = operatorDocumentation.getTags();
		assertEquals(1, tagList.size());
		assertEquals("First Tag", tagList.get(0));
	}

	@Test
	public void multipleTagTest() throws IOException {
		OperatorDocumentation operatorDocumentation = parseOperatorDocumentation(OPERATOR_DOCUMENTATION_MULTIPLE_TAGS,
				OPERATOR_KEY_MULTIPLE_TAGS);
		List<String> tagList = operatorDocumentation.getTags();
		assertEquals(2, tagList.size());
		assertEquals("First Tag", tagList.get(0));
		assertEquals("Second Tag", tagList.get(1));
	}
}
