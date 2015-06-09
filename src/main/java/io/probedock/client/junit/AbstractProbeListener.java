package io.probedock.client.junit;

import io.probedock.client.annotations.ProbeTest;
import io.probedock.client.annotations.ProbeTestClass;
import io.probedock.client.common.config.Configuration;
import io.probedock.client.common.model.v1.*;
import io.probedock.client.common.utils.FingerprintGenerator;
import io.probedock.client.common.utils.Inflector;
import io.probedock.client.utils.CollectionHelper;

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
	private static final String DEFAULT_CATEGORY = "unit";

	/**
	 * Store the date when the run started
	 */
	protected Long runStartedDate;

	/**
	 * Store the start date of a test to measure the approximative execution
	 * time of a each test.
	 */
	protected Map<String, Long> testStartDates = new HashMap<>();

	/**
	 * Define if all the stack traces must be printed/written
	 */
	private boolean fullStackTraces = true;
	
	/**
	 * Define the category of the test that are running. Will
	 * be use donly if no other is specified.
	 */
	private String category;
	
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
			getKey(mAnnotation),
			fingerprint,
			getName(description, mAnnotation),
			getCategory(cAnnotation, mAnnotation),
			System.currentTimeMillis() - testStartDates.get(fingerprint),
			message,
			passed,
			isActive(mAnnotation),
			getContributors(mAnnotation, cAnnotation),
			getTags(mAnnotation, cAnnotation),
			getTickets(mAnnotation, cAnnotation),
			null
		);

		ModelFactory.enrichTestResult(result, description.getTestClass().getPackage().getName(), description.getTestClass().getName(), description.getMethodName());

		return result;
	}

	/**
	 * Retrieve the tags from the annotations
	 *
	 * @param annotation The annotation
	 * @return The key or null if there is no key
	 */
	private String getKey(ProbeTest annotation) {
		return annotation != null && annotation.key() != null && !annotation.key().isEmpty() ? annotation.key() : null;
	}

	private Boolean isActive(ProbeTest annotation) {
		return annotation != null ? annotation.active() : null;
	}
	
	/**
	 * Retrieve a name from a test
	 * 
	 * @param description The description of the test
	 * @param mAnnotation The method annotation
	 * @return The name retrieved
	 */
	private String getName(Description description, ProbeTest mAnnotation) {
		if (mAnnotation == null || mAnnotation.name() == null || mAnnotation.name().isEmpty()) {
			return Inflector.getHumanName(description.getTestClass().getSimpleName() + ": " + description.getMethodName());
		}
		else {
			return mAnnotation.name();
		}
	} 
	
	/**
	 * Retrieve the category to apply to the test
	 * @param classAnnotation The roxable class annotation to get the override category
	 * @param methodAnnotation The roxable annotation to get the override category
	 * @return The category found
	 */
	protected String getCategory(ProbeTestClass classAnnotation, ProbeTest methodAnnotation) {
		if (methodAnnotation != null && methodAnnotation.category() != null && !methodAnnotation.category().isEmpty()) {
			return methodAnnotation.category();
		}
		else if (classAnnotation != null && classAnnotation.category() != null && !classAnnotation.category().isEmpty()) {
			return classAnnotation.category();
		}
		else if (configuration.getCategory() != null && !configuration.getCategory().isEmpty()) {
			return configuration.getCategory();
		}
		else if (category != null) {
			return category;
		}
		else {
			return DEFAULT_CATEGORY;
		}
	}

	/**
	 * Compute the list of contributors associated for a test
	 *
	 * @param methodAnnotation The method annotation to get info
	 * @param classAnnotation The class annotation to get info
	 * @return The contributors associated to the test
	 */
	private Set<String> getContributors(ProbeTest methodAnnotation, ProbeTestClass classAnnotation) {
		return CollectionHelper.getContributors(configuration.getTags(), methodAnnotation, classAnnotation);
	}

	/**
	 * Compute the list of tags associated for a test
	 * 
	 * @param methodAnnotation The method annotation to get info
	 * @param classAnnotation The class annotation to get info
	 * @return The tags associated to the test
	 */
	private Set<String> getTags(ProbeTest methodAnnotation, ProbeTestClass classAnnotation) {
		return CollectionHelper.getTags(configuration.getTags(), methodAnnotation, classAnnotation);
	}

	/**
	 * Compute the list of tickets associated for a test
	 * 
	 * @param methodAnnotation The method annotation to get info
	 * @param classAnnotation The class annotation to get info
	 * @return The tickets associated to the test
	 */
	private Set<String> getTickets(ProbeTest methodAnnotation, ProbeTestClass classAnnotation) {
		return CollectionHelper.getTickets(configuration.getTickets(), methodAnnotation, classAnnotation);
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
	 * Build the technical name
	 * 
	 * @param description The description to retrieve the unique name of a test
	 * @return The technical name
	 */
	protected String getTechnicalName(Description description) {
		return description.getClassName() + "." + description.getMethodName();
	}

	/**
	 * Calculate a fingerpring for a test
	 *
	 * @param description The description to retrieve package, class and method
	 * @return The fingerprint calculated
	 */
	protected String getFingerprint(Description description) {
		return FingerprintGenerator.fingerprint(
			description.getTestClass().getPackage().getName(),
			description.getTestClass().getName(),
			description.getMethodName()
		);
	}
}