package io.probedock.client.junit;

import io.probedock.client.core.filters.FilterDefinition;
import io.probedock.client.core.filters.FilterDefinitionImpl;
import io.probedock.client.core.filters.FilterUtils;
import java.util.ArrayList;
import java.util.List;
import org.junit.runner.Description;
import org.junit.runner.manipulation.Filter;

/**
 * Allow filtering of the tests at runtime.
 *
 * @author Laurent Prevost, laurent.prevost@probedock.io
 */
public class ProbeFilter extends Filter {
	/**
	 * Define the filters to apply
	 */
	private List<FilterDefinition> filters;
	
	/**
	 * Default constructor aims to facilitate the reflection
	 */
	public ProbeFilter() {
		filters = new ArrayList<>();
	};
	
	public ProbeFilter(List<FilterDefinition> filters) {
		this.filters = filters;
	}
	
	/**
	 * @param filter Add a filter
	 */
	public void addFilter(String filter) {
		boolean found = false;

		if (filters != null) {
			for (FilterDefinition filterDefinition : filters) {
				if (filterDefinition.getText().equalsIgnoreCase(filter)) {
					found = true;
					break;
				}
			}

			if (!found) {
				filters.add(new FilterDefinitionImpl("*", filter));
			}
		}
	}
	
	@Override
	public boolean shouldRun(Description description) {
		if (!description.isTest()) {
			return true;
		}

		if (description.isSuite()) {
			return false;
		}
		
		// Delegate the filtering to filter utils
		else {
			return FilterUtils.isRunnable(description.getTestClass(), description.getMethodName(), filters);
		}
	}

	@Override
	public String describe() {
		return "";
	}
}