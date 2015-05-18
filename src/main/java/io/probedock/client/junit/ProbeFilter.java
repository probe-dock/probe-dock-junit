package io.probedock.client.junit;

import io.probedock.client.core.filters.FilterUtils;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.runner.Description;
import org.junit.runner.manipulation.Filter;

/**
 * Allow filtering of the tests at runtime.
 *
 * @author Laurent Prevost <laurent.prevost@probe-dock.io>
 */
public class ProbeFilter extends Filter {
	/**
	 * Define the filters to apply
	 */
	private List<String> filters;
	
	/**
	 * Default constructor aims to facilitate the reflection
	 */
	public ProbeFilter() {
		filters = new ArrayList<>();
	};
	
	public ProbeFilter(String[] filters) {
		this.filters = Arrays.asList(filters);
	}
	
	/**
	 * @param filter Add a filter
	 */
	public void addFilter(String filter) {
		if (filters != null && filter != null && !filter.isEmpty() && !filters.contains(filter)) {
			filters.add(filter);
		}
	}
	
	@Override
	public boolean shouldRun(Description description) {
		if (!description.isTest()) {
			return true;
		}
		
		// Delegate the filtering to filter utils
		else {
			return FilterUtils.isRunnable(description.getTestClass(), description.getMethodName(), filters.toArray(new String[filters.size()]));
		}
	}

	@Override
	public String describe() {
		return "";
	}
}