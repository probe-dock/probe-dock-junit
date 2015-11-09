package io.probedock.client.junit;

import io.probedock.client.annotations.ProbeTest;
import io.probedock.client.annotations.ProbeTestClass;
import io.probedock.client.common.config.Configuration;
import io.probedock.client.common.model.v1.*;
import io.probedock.client.common.utils.Inflector;
import io.probedock.client.common.utils.TestResultDataUtils;

import java.util.*;
import java.util.logging.Logger;

import org.junit.runner.Description;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunListener;

/**
 * Shared code to create Junit Probe Dock listeners
 *
 * @author Laurent Prevost <laurent.prevost@probedock.io>
 */
public abstract class AbstractProbeListener extends RunListener {
    private static final Logger LOGGER = Logger.getLogger(AbstractProbeListener.class.getCanonicalName());

    /**
     * Probe Dock configuration
     */
    protected static final Configuration configuration = Configuration.getInstance();

    /**
     * Default category when none is specified
     */
    protected static final String DEFAULT_CATEGORY = "JUnit";

    /**
     * Store the date when the run started
     */
    protected Long runStartedDate;

    /**
     * Store the start date of a test to measure the approximative execution time of a each test.
     */
    protected Map<String, Long> testStartDates = new HashMap<>();

    /**
     * Define if all the stack traces must be printed/written
     */
    private boolean fullStackTraces = true;

    /**
     * Define the category of the test that are running. Will be use donly if no other is specified.
     */
    protected String category;

    /**
     * Constructor
     */
    public AbstractProbeListener() {
    }

    /**
     * Constructor
     *
     * @param category The test category
     */
    public AbstractProbeListener(String category) {
        this();
        this.category = category;
    }

    @Override
    public void testStarted(Description description) throws Exception {
        super.testStarted(description);
    }

    @Override
    public void testFinished(Description description) throws Exception {
        super.testFinished(description);
    }

    /**
     * @param fullStackTraces The full stack trace mode
     */
    public void setFullStackTraces(Boolean fullStackTraces) {
        this.fullStackTraces = fullStackTraces;
    }

    @Override
    public void testRunStarted(Description description) throws Exception {
        super.testRunStarted(description);
        runStartedDate = System.currentTimeMillis();
    }

    /**
     * Try to retrieve the {@link ProbeTest} annotation of the test method
     *
     * @param description The representation of the test
     * @return The annotation found, or null if not found
     * @throws NoSuchMethodException
     */
    protected ProbeTest getMethodAnnotation(Description description) throws NoSuchMethodException {
        String methodName = description.getMethodName();

        if (methodName.contains("[")) {
            methodName = methodName.replaceAll("\\[.*\\]", "");
        }

        return description.getTestClass().getMethod(methodName).getAnnotation(ProbeTest.class);
    }

    /**
     * Try to retrieve the {@link ProbeTestClass} annotation of the test class
     *
     * @param description The representation of the test
     * @return The annotation found, or null if not found
     */
    protected ProbeTestClass getClassAnnotation(Description description) {
        return description.getTestClass().getAnnotation(ProbeTestClass.class);
    }

    /**
     * Create a test based on the different information gathered from class, method and description
     *
     * @param fingerprint The fingerprint of the test
     * @param description jUnit test description
     * @param mAnnotation Method annotation
     * @param cAnnotation Class annotation
     * @param passed Test passing or not
     * @param message Message associated to the test result
     * @return The test created from all the data available
     */
    protected TestResult createTestResult(String fingerprint, Description description, ProbeTest mAnnotation, ProbeTestClass cAnnotation, boolean passed, String message) {
        TestResult result = ModelFactory.createTestResult(
            TestResultDataUtils.getKey(mAnnotation),
            fingerprint,
            Inflector.forgeName(description.getTestClass(), description.getMethodName(), mAnnotation),
            TestResultDataUtils.getCategory(getPackage(description.getTestClass()), configuration, cAnnotation, mAnnotation, getCategory()),
            System.currentTimeMillis() - testStartDates.get(fingerprint),
            message,
            passed,
            TestResultDataUtils.isActive(mAnnotation),
            TestResultDataUtils.getContributors(configuration, cAnnotation, mAnnotation),
            TestResultDataUtils.getTags(configuration, cAnnotation, mAnnotation),
            TestResultDataUtils.getTickets(configuration, cAnnotation, mAnnotation),
            null
        );

        ModelFactory.enrichTestResult(result, getPackage(description.getTestClass()), description.getTestClass().getName(), description.getMethodName());

        return result;
    }

    /**
     * Build a stack trace string
     *
     * @param failure The failure to get the exceptions and so on
     * @return The stack trace stringified
     */
    protected String createAndlogStackTrace(Failure failure) {
        StringBuilder sb = new StringBuilder();

        if (failure.getMessage() != null && !failure.getMessage().isEmpty()) {
            sb.append("Failure message: ").append(failure.getMessage());
        }

        if (failure.getException() != null) {
            sb.append("\n\n");
            sb.append(failure.getException().getClass().getCanonicalName()).append(": ").append(failure.getMessage()).append("\n");

            for (StackTraceElement ste : failure.getException().getStackTrace()) {
                sb.append("\tat ").append(ste.getClassName()).append(".").append(ste.getMethodName()).
                    append("(").append(ste.getFileName()).append(":").append(ste.getLineNumber()).append(")\n");

                if (!fullStackTraces && ste.getClassName().equals(failure.getDescription().getClassName())) {
                    sb.append("\t...\n");
                    break;
                }
            }

            if (fullStackTraces && failure.getException().getCause() != null) {
                sb.append("Cause: ").append(failure.getException().getCause().getMessage()).append("\n");

                for (StackTraceElement ste : failure.getException().getCause().getStackTrace()) {
                    sb.append("\tat ").append(ste.getClassName()).append(".").append(ste.getMethodName()).
                        append("(").append(ste.getFileName()).append(":").append(ste.getLineNumber()).append(")\n");
                }
            }

            LOGGER.info("\n" + failure.getTestHeader() + "\n" + sb.toString());
        }

        return sb.toString();
    }

    /**
     * Retrieve the fingerprint of a test based on its description
     *
     * @param description The description
     * @return The fingerprint
     */
    protected final String getFingerprint(Description description) {
        return TestResultDataUtils.getFingerprint(description.getTestClass(), description.getMethodName());
    }

    /**
     * Retrive the category
     *
     * @return The category
     */
    protected final String getCategory() {
        return category != null && !category.isEmpty() ? category : DEFAULT_CATEGORY;
    }

    /**
     * Retrieve the package name if available, if not, default package name is used.
     *
     * @param cls The class to extract the package name
     * @return The package name if found, "defaultPackage" if not found
     */
    protected final String getPackage(Class cls) {
        return cls.getPackage() != null ? cls.getPackage().getName() : "defaultPackage";
    }
}