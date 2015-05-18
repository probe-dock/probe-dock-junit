package io.probedock.client.junit;

import io.probedock.client.annotations.ProbeTest;
import io.probedock.client.annotations.ProbeTestClass;
import io.probedock.client.common.config.ProbeRuntimeException;
import io.probedock.client.common.model.v1.ModelFactory;
import io.probedock.client.common.model.v1.TestResult;
import io.probedock.client.common.model.v1.TestRun;
import io.probedock.client.core.connector.Connector;
import io.probedock.client.core.storage.FileStore;
import java.io.IOException;
import java.util.*;

import org.junit.runner.Description;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The test unit listener is used to send the result to the Probe Dock server
 *
 * @author Laurent Prevost <laurent.prevost@probe-dock.io>
 */
public class ProbeListener extends AbstractProbeListener {
	private static final Logger LOGGER = LoggerFactory.getLogger(ProbeListener.class);

	/**
	 * Store the list of the tests executed
	 */
	private List<TestResult> results = new ArrayList<>();

	/**
	 * Store the test that fail to handle correctly the difference between test failures and test
	 * success in the testFinished method.
	 */
	private Set<String> testFailures = new HashSet<>();

	public ProbeListener() {}

	public ProbeListener(String category) {
		super(category);
	}

	@Override
	public void testRunFinished(Result result) throws Exception {

		// Ensure there is nothing to do when ROX is disabled
		if (configuration.isDisabled()) {
			return;
		}

		if (!results.isEmpty()) {

			try {
				publishTestRun();
			} catch (ProbeRuntimeException pre) {
				LOGGER.warn("Could not publish or save test run", pre);
			}
		}
	}

	private void publishTestRun() throws IOException {
		if (configuration.isPublish() || configuration.isSave()) {
			long runEndedDate = System.currentTimeMillis();

			String uid = configuration.getCurrentUid();

			TestRun testRun = ModelFactory.createTestRun(
				configuration.getProjectApiId(),
				configuration.getProjectVersion(),
				runEndedDate - runStartedDate,
				results,
				uid != null ? Arrays.asList(ModelFactory.createTestReport(uid)) : null
			);

			if (configuration.isSave()) {
				new FileStore(configuration).save(testRun);
			}

			if (configuration.isPublish()) {
				new Connector(configuration).send(testRun);
			}
		}
	}

	@Override
	public void testStarted(Description description) throws Exception {
		super.testStarted(description);

		// Ensure there is nothing to do when ROX is disabled
		if (configuration.isDisabled()) {
			return;
		}

		testStartDates.put(getTechnicalName(description), System.currentTimeMillis());
	}

	@Override
	public void testFinished(Description description) throws Exception {
		super.testFinished(description);

		// Ensure there is nothing to do when ROX is disabled
		if (configuration.isDisabled()) {
			return;
		}

		ProbeTest mAnnotation = getMethodAnnotation(description);
		ProbeTestClass cAnnotation = getClassAnnotation(description);

		if (!testFailures.contains(getTechnicalName(description))) {
			// Create a test result
			TestResult testResult = createTestResult(description, mAnnotation, cAnnotation, true, null);

			results.add(testResult);
		}
	}

	@Override
	public void testFailure(Failure failure) throws Exception {
		super.testFailure(failure);

		// Ensure there is nothing to do when ROX is disabled
		if (configuration.isDisabled()) {
			return;
		}

		ProbeTest mAnnotation = getMethodAnnotation(failure.getDescription());
		ProbeTestClass cAnnotation = getClassAnnotation(failure.getDescription());

		testFailures.add(getTechnicalName(failure.getDescription()));

		// Create the test result
		TestResult testResult = createTestResult(failure.getDescription(), mAnnotation, cAnnotation, false, createAndlogStackTrace(failure));

		results.add(testResult);
	}
}