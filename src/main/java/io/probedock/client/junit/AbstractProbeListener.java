package io.probedock.client.junit;

import io.probedock.client.annotations.ProbeTest;
import io.probedock.client.annotations.ProbeTestClass;
import io.probedock.client.common.config.Configuration;
import io.probedock.client.common.model.v1.ModelFactory;
import io.probedock.client.common.model.v1.TestResult;
import io.probedock.client.common.utils.Inflector;
import io.probedock.client.common.utils.MetaDataBuilder;
import io.probedock.client.utils.CollectionHelper;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.runner.Description;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Shared code to create Junit Probe Dock listeners
 *
 * @author Laurent Prevost <laurent.prevost@probe-dock.io>
 */
public abstract class AbstractProbeListener extends RunListener {
	private static final Logger LOGGER = LoggerFactory.getLogger(AbstractProbeListener.class);
	
	/**
	 * Probe Dock configuration
	 */
	protected static final Configuration configuration = Configuration.getInstance();
	
	/**
	 * Default category when none is specified
	 */
	private static final String DEFAULT_CATEGORY	= "JUnit";
	private static final String DEFAULT_TAG			= "unit";
	
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
	 * List of meta data extractors
	 */
	private final List<TestMetaDataExtratctor> extractors = new ArrayList<>();
	
	/**
	 * Constructor
	 */
	public AbstractProbeListener() {
		extractors.add(new StandardTestMetaDataExtractor());
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
		
		for (TestMetaDataExtratctor extractor : extractors) {
			extractor.before(description);
		}
	}

	@Override
	public void testFinished(Description description) throws Exception {
		super.testFinished(description);

		for (TestMetaDataExtratctor extractor : extractors) {
			extractor.after(description);
		}
	}
	
	/**
	 * @param fullStackTraces The full stack trace mode
	 */
	public void setFullStackTraces(Boolean fullStackTraces) {
		this.fullStackTraces = fullStackTraces;
	}
	
	/**
	 * Add an extractor to the list of extractors
	 * 
	 * @param extractor Extractor to add
	 */
	public void addExctractor(TestMetaDataExtratctor extractor) {
		extractors.add(extractor);
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
	 * @param description jUnit test description
	 * @param mAnnotation Method annotation
	 * @param cAnnotation Class annotation
	 * @param passed Test passing or not
	 * @param message Message associated to the test result
	 * @return The test created from all the data available
	 */
	protected TestResult createTestResult(Description description, ProbeTest mAnnotation, ProbeTestClass cAnnotation, boolean passed, String message) {
		MetaDataBuilder data = new MetaDataBuilder();
		
		for (TestMetaDataExtratctor extractor : extractors) {
			data.add(extractor.extract(description));
		}

		return ModelFactory.createTestResult(
			getKey(mAnnotation),
			getName(description, mAnnotation),
			getCategory(cAnnotation, mAnnotation),
			System.currentTimeMillis() - testStartDates.get(getTechnicalName(description)),
			message,
			passed,
			isActive(mAnnotation),
			getTags(mAnnotation, cAnnotation),
			getTickets(mAnnotation, cAnnotation),
			data.toMetaData()
		);
	}

	/**
	 * Retrieve the key from the annotation
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
	 * Compute the list of tags associated for a test
	 * 
	 * @param methodAnnotation The method annotation to get info
	 * @param classAnnotation The class annotation to get info
	 * @return The tags associated to the test
	 */
	private Set<String> getTags(ProbeTest methodAnnotation, ProbeTestClass classAnnotation) {
		Set<String> tags = CollectionHelper.getTags(configuration.getTags(), methodAnnotation, classAnnotation);
		
		if (!tags.contains(DEFAULT_TAG)) {
			tags.add(DEFAULT_TAG);
		}
		
		return tags;
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
			
			LOGGER.info("\n{}\n{}", failure.getTestHeader(), sb.toString());
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
}