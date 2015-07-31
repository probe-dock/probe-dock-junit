package io.probedock.client.junit;

import io.probedock.client.ProbeRuntimeException;
import io.probedock.client.annotations.ProbeTest;
import io.probedock.client.annotations.ProbeTestClass;
import io.probedock.client.common.config.Configuration;
import io.probedock.client.common.model.v1.*;
import io.probedock.client.common.utils.TestResultDataUtils;
import io.probedock.client.core.connector.Connector;
import io.probedock.client.core.storage.FileStore;

import java.io.IOException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.junit.runner.Description;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;

/**
 * The test unit listener is used to send the result to the Probe Dock server
 *
 * @author Laurent Prevost <laurent.prevost@probedock.io>
 */
public class ProbeListener extends AbstractProbeListener {
    private static final Logger LOGGER = Logger.getLogger(ProbeListener.class.getCanonicalName());

    /**
     * Probe Dock JUnit and version
     */
    private static final String PROBE_DOCK_NAME = "Junit";
    private static final String PROBE_DOCK_VERSION = ResourceBundle.getBundle("version").getString("version");

    /**
     * The execution context
     */
    protected Context context;

    /**
     * The test run to publish
     */
    protected TestRun testRun;

    /**
     * Store the list of the tests executed
     */
    private List<TestResult> results = new ArrayList<>();

    /**
     * Store the test that fail to handle correctly the difference between test failures and test success in the
     * testFinished method.
     */
    private Set<String> testFailures = new HashSet<>();

    /**
     * Constructor
     */
    public ProbeListener() {
    }

    /**
     * Constructor
     *
     * @param category The default category for the listener
     */
    public ProbeListener(String category) {
        super(category);
    }

    @Override
    public void testRunStarted(Description description) throws Exception {
        super.testRunStarted(description);

        // Ensure there is nothing to do when ROX is disabled
        if (configuration.isDisabled()) {
            return;
        }

        context = ModelFactory.createContext();
        Probe probe = ModelFactory.createProbe(PROBE_DOCK_NAME, PROBE_DOCK_VERSION);

        testRun = ModelFactory.createTestRun(
            Configuration.getInstance(),
            context,
            probe,
            configuration.getProjectApiId(),
            configuration.getProjectVersion(),
            configuration.getPipeline(),
            configuration.getStage(),
            null,
            null
        );
    }

    @Override
    public void testRunFinished(Result result) throws Exception {
        // Ensure there is nothing to do when ROX is disabled
        if (configuration.isDisabled()) {
            return;
        }

        if (!results.isEmpty()) {
            ModelFactory.enrichContext(context);
            long runEndedDate = System.currentTimeMillis();
            testRun.setDuration(runEndedDate - runStartedDate);
            testRun.addTestResults(results);

            try {
                publishTestRun();
            } catch (ProbeRuntimeException pre) {
                LOGGER.log(Level.WARNING, "Could not publish or save test run", pre);
            }
        }
    }

    /**
     * Publish the results to Probe Dock
     *
     * @throws IOException Any I/O error
     */
    private void publishTestRun() throws IOException {
        if (configuration.isPublish() || configuration.isSave()) {
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

        testStartDates.put(getFingerprint(description), System.currentTimeMillis());
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

        String fingerprint = getFingerprint(description);

        if (!testFailures.contains(fingerprint)) {
            // Create a test result
            TestResult testResult = createTestResult(fingerprint, description, mAnnotation, cAnnotation, true, null);

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

        String fingerprint = getFingerprint(failure.getDescription());

        testFailures.add(fingerprint);

        // Create the test result
        TestResult testResult = createTestResult(fingerprint, failure.getDescription(), mAnnotation, cAnnotation, false, createAndlogStackTrace(failure));

        results.add(testResult);
    }
}