package ch.ethz.rse.integration;

import ch.ethz.rse.VerificationProperty;
import ch.ethz.rse.VerificationResult;
import ch.ethz.rse.main.Runner;
import ch.ethz.rse.testing.VerificationTestCase;
import ch.ethz.rse.testing.VerificationTestCaseCollector;

import com.google.common.base.Strings;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Allows to easily run an integration test for a single example
*/
public class SpecificClassIT {
	
	public static List<VerificationTestCase> getTests() throws IOException {
		String packageName = "ch.ethz.rse.integration.tests.Loop_Countdown";
		List<VerificationTestCase> cases = VerificationTestCaseCollector.getTests();
		List<VerificationTestCase> filtered_cases = new ArrayList<VerificationTestCase>();
		for (VerificationTestCase c: cases) {
			if (c.getTestClass().getPackageName().equals(packageName)) {
				filtered_cases.add(c);
			}
		}
		return filtered_cases;
	}

	@ParameterizedTest(name = "{index}: {0}")
	@MethodSource("getTests")
	void testExampleClass(VerificationTestCase example) {
		SpecificExampleIT.testOnExample(example);
	}

}
