// package ch.ethz.rse.integration;

// import ch.ethz.rse.Frog;
// import ch.ethz.rse.VerificationProperty;
// import ch.ethz.rse.VerificationResult;
// import ch.ethz.rse.main.Runner;
// import ch.ethz.rse.testing.VerificationTestCase;
// import ch.ethz.rse.testing.VerificationTestCaseCollector;
// import ch.ethz.rse.utils.Constants;
// import java.lang.reflect.Method;
// import java.nio.file.Paths;

// import polyglot.ext.jl.ast.Expr_c;

// import com.google.common.base.Charsets;
// import com.google.common.base.Strings;
// import com.google.common.io.Files;

// import java.io.File;
// import java.io.IOException;
// import java.lang.reflect.InvocationTargetException;
// import java.util.Collections;
// import java.util.LinkedList;
// import java.util.List;
// import java.util.regex.Matcher;
// import java.util.regex.Pattern;

// import org.apache.commons.io.FilenameUtils;
// import org.junit.jupiter.api.Assertions;
// import org.junit.jupiter.api.Assumptions;
// import org.junit.jupiter.api.Test;
// import org.slf4j.Logger;
// import org.slf4j.LoggerFactory;

// /**
//  * Runs all classes and gets results automatically
//  * To dry run, copy contents from FrogCompute.txt to Frog.java
// */
// public class DryRunTest {

// 	@Test
// 	void specificTest() throws Exception {

// 		// get directory of tests
// 		String examplesPath = System.getProperty("user.dir") + "/src/test/java/" + "ch.ethz.rse.integration.tests".replace(".", File.separator);
// 		File examples_dir = new File(examplesPath);

// 		// collect tasks
// 		List<VerificationTestCase> tasks = new LinkedList<VerificationTestCase>();
// 		boolean disableOthers = false;

// 		for (File f : examples_dir.listFiles()) {
// 			// skip directories
// 			if (f.isDirectory()) {
// 				continue;
// 			}

// 			String content = Files.asCharSource(f, Charsets.UTF_8).read();
// 			String className = FilenameUtils.removeExtension(f.getName());
// 			String packageName = "ch.ethz.rse.integration.tests" + "." + className;

// 			Frog.item_profit = true;
// 			Frog.non_negative = true;
// 			Frog.total_profit = 0;

// 			Class<?> clazz = Class.forName(packageName);
// 			logger.debug("Class names: " + clazz.getName());

// 			Method[] methods = clazz.getDeclaredMethods();
// 			Method method = methods[0];
// 			logger.debug("Method: " + method);
			
// 			Object instance = clazz.getDeclaredConstructor().newInstance();
			
// 			if (method.getParameterCount() == 0) {
// 				// Invoke the method on the instance
// 				try {
// 					Object result = method.invoke(instance);
// 					String result_string = "// expected results:\n"
// 										 + "// NON_NEGATIVE " + (Frog.non_negative ? "SAFE" : "UNSAFE") + "\n"
// 										 + "// ITEM_PROFIT " + (Frog.item_profit ? "SAFE" : "UNSAFE") + "\n"
// 										 + "// OVERALL_PROFIT " + (Frog.total_profit >= 0 ? "SAFE" : "UNSAFE") + "\n";
// 					logger.debug(result_string);
// 					logger.debug("Overall profit was " + Frog.total_profit);

// 					// Read the entire content of the file
// 					// Define the regex pattern to match the block of lines
// 					String regex = "// expected results:\\s*\\n"
// 								+ "// NON_NEGATIVE (SAFE|UNSAFE)\\s*\\n"
// 								+ "// ITEM_PROFIT (SAFE|UNSAFE)\\s*\\n"
// 								+ "// OVERALL_PROFIT (SAFE|UNSAFE)\\s*\\n";

// 					// Compile the regex pattern
// 					Pattern pattern = Pattern.compile(regex);
// 					Matcher matcher = pattern.matcher(content);

// 					// Replace the matched block with the resultString
// 					String modifiedContent = matcher.replaceAll(result_string);

// 					// Write the modified content back to the file
// 					Files.write(modifiedContent.getBytes(), f);

// 					logger.debug("File updated successfully.");
// 				} catch (IOException e) {
// 					e.printStackTrace();
// 				} catch (IllegalAccessException | InvocationTargetException e) {
// 					logger.debug("Failed to invoke method: " + method.getName());
// 					e.printStackTrace();
// 				}
// 			}
		
// 		}
	
// 	}

// 	private static final Logger logger = LoggerFactory.getLogger(DryRunTest.class);

// }
