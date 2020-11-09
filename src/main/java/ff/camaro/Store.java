package ff.camaro;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public abstract class Store {

	public static String capitalize(final String value) {
		if (value.length() == 0) {
			return "";
		}
		if (value.length() == 1) {
			return value.toUpperCase();
		}
		return Character.toUpperCase(value.charAt(0)) + value.substring(1);
	}

	@SuppressWarnings("unchecked")
	protected <T> T cast(final String key) {
		return (T) getValue(key);
	}

	public boolean getBoolean(final String key) {
		final var value = getString(key);
		return "true".equals(value);
	}

	public <T> List<T> getList(final String key) {
		final var result = getValue(key);
		return toList(result);
	}

	public Map<String, Object> getMap(final String key) {
		final Map<String, Object> result = cast(key);
		if (result == null) {
			return new HashMap<>();
		}
		return result;
	}

	public String getString(final String key) {
		return getString(key, null);
	}

	public String getString(final String key, final String defaultValue) {
		final var value = getValue(key);
		return toString(value == null ? defaultValue : value);
	}

	public abstract Object getValue(String key);

	@SuppressWarnings("unchecked")
	public <T> T loadClass(final String pck, final String name) throws Exception {
		return (T) Class.forName(pck + "." + Store.capitalize(name)).getConstructor().newInstance();
	}

	@SuppressWarnings("unchecked")
	public <T> List<T> toList(final Object value) {
		if (value == null) {
			return new LinkedList<>();
		}
		if (value instanceof List) {
			return (List<T>) value;
		}
		return Collections.singletonList((T) value);
	}

	public String toString(final Object value) {
		if (value == null) {
			return null;
		}
		return String.valueOf(value);
	}
}
