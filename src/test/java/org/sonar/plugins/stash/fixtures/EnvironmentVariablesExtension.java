package org.sonar.plugins.stash.fixtures;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

// Lifted from here: https://github.com/stefanbirkner/system-rules/blob/063b1c82864c11b9dd3e14846665f796f340b7d5/src/main/java/org/junit/contrib/java/lang/system/EnvironmentVariables.java
// To be migrated to https://github.com/stefanbirkner/system-lambda
public class EnvironmentVariablesExtension implements BeforeEachCallback, AfterEachCallback {

	private final Map<String, String> buffer = new HashMap<String, String>();
	private boolean statementIsExecuting = false;

	/**
	 * Set the value of an environment variable.
	 *
	 * @param name the environment variable's name.
	 * @param value the environment variable's new value. May be {@code null}.
	 * @return the rule itself.
	 */
	public EnvironmentVariablesExtension set(String name, String value) {
		if (statementIsExecuting)
			writeVariableToEnvMap(name, value);
		else
			writeVariableToBuffer(name, value);
		return this;
	}

	/**
	 * Delete multiple environment variables.
	 *
	 * @param names the environment variables' names.
	 * @return the rule itself.
	 */
	public EnvironmentVariablesExtension clear(String... names) {
		for (String name: names)
			set(name, null);
		return this;
	}

	private void writeVariableToEnvMap(String name, String value) {
		set(getEditableMapOfVariables(), name, value);
		set(getTheCaseInsensitiveEnvironment(), name, value);
	}

	private void set(Map<String, String> variables, String name, String value) {
		if (variables != null) //theCaseInsensitiveEnvironment may be null
			if (value == null)
				variables.remove(name);
			else
				variables.put(name, value);
	}

	private void writeVariableToBuffer(String name, String value) {
		buffer.put(name, value);
	}

	private void copyVariablesFromBufferToEnvMap() {
		for (Map.Entry<String, String> nameAndValue: buffer.entrySet()) {
			writeVariableToEnvMap(
					nameAndValue.getKey(), nameAndValue.getValue());
		}
	}

	private static Map<String, String> getEditableMapOfVariables() {
		Class<?> classOfMap = System.getenv().getClass();
		try {
			return getFieldValue(classOfMap, System.getenv(), "m");
		} catch (IllegalAccessException e) {
			throw new RuntimeException("System Rules cannot access the field"
					+ " 'm' of the map System.getenv().", e);
		} catch (NoSuchFieldException e) {
			throw new RuntimeException("System Rules expects System.getenv() to"
					+ " have a field 'm' but it has not.", e);
		}
	}

	/*
	 * The names of environment variables are case-insensitive in Windows.
	 * Therefore it stores the variables in a TreeMap named
	 * theCaseInsensitiveEnvironment.
	 */
	private static Map<String, String> getTheCaseInsensitiveEnvironment() {
		try {
			Class<?> processEnvironment = Class.forName("java.lang.ProcessEnvironment");
			return getFieldValue(
					processEnvironment, null, "theCaseInsensitiveEnvironment");
		} catch (ClassNotFoundException e) {
			throw new RuntimeException("System Rules expects the existence of"
					+ " the class java.lang.ProcessEnvironment but it does not"
					+ " exist.", e);
		} catch (IllegalAccessException e) {
			throw new RuntimeException("System Rules cannot access the static"
					+ " field 'theCaseInsensitiveEnvironment' of the class"
					+ " java.lang.ProcessEnvironment.", e);
		} catch (NoSuchFieldException e) {
			//this field is only available for Windows
			return null;
		}
	}

	private static Map<String, String> getFieldValue(Class<?> klass,
			Object object, String name)
			throws NoSuchFieldException, IllegalAccessException {
		Field field = klass.getDeclaredField(name);
		field.setAccessible(true);
		return (Map<String, String>) field.get(object);
	}

	@Override
	public void beforeEach(ExtensionContext context) throws Exception {
		saveCurrentState();
		statementIsExecuting = true;
		copyVariablesFromBufferToEnvMap();
	}

	@Override
	public void afterEach(ExtensionContext context) throws Exception {
		statementIsExecuting = false;
		restoreOriginalVariables();
	}

	private Map<String, String> originalVariables;

	private void saveCurrentState() {
		originalVariables = new HashMap<String, String>(System.getenv());
	}

	private void restoreOriginalVariables() {
		restoreVariables(getEditableMapOfVariables());
		Map<String, String> theCaseInsensitiveEnvironment
				= getTheCaseInsensitiveEnvironment();
		if (theCaseInsensitiveEnvironment != null)
			restoreVariables(theCaseInsensitiveEnvironment);
	}

	void restoreVariables(Map<String, String> variables) {
		variables.clear();
		variables.putAll(originalVariables);
	}
}
