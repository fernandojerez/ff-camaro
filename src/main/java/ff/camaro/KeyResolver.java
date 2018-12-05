package ff.camaro;

import java.util.Map;
import java.util.Stack;

public class KeyResolver {

	private final Stack<Map<String, Object>> maps;

	public KeyResolver() {
		maps = new Stack<>();
	}

	public Object get(final String key) {
		final Map<String, Object> map = maps.pop();
		try {
			final Object value = map.get(key);
			if (value != null) {
				return value;
			}
			if (!maps.isEmpty()) {
				return get(key);
			}
		} finally {
			maps.push(map);
		}
		return null;
	}

	public void pop() {
		maps.pop();
	}

	public void push(final Map<String, Object> map) {
		maps.push(map);
	}

}
