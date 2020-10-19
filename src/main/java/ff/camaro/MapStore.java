package ff.camaro;

import java.util.Map;

public class MapStore extends Store {

	private final Map<String, Object> data;

	public MapStore(final Map<String, Object> data) {
		this.data = data;
	}

	@Override
	public Object getValue(final String key) {
		return data.get(key);
	}

}
