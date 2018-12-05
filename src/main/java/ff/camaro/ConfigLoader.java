package ff.camaro;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.gradle.api.tasks.SourceSet;
import org.snakeyaml.engine.v1.api.Dump;
import org.snakeyaml.engine.v1.api.DumpSettingsBuilder;
import org.snakeyaml.engine.v1.api.Load;
import org.snakeyaml.engine.v1.api.LoadSettings;
import org.snakeyaml.engine.v1.api.LoadSettingsBuilder;
import org.snakeyaml.engine.v1.common.FlowStyle;

public class ConfigLoader {

	public static ConfigLoader plugin = new ConfigLoader("/ff/camaro/plugin");
	public static ConfigLoader facet = new ConfigLoader("/ff/camaro/facets");

	public static void main(final String[] args) {
		System.out.println(ConfigLoader.plugin.toYaml(ConfigLoader.plugin.load("multi")));
	}

	public static String output_main_path(final String name) {
		return ConfigLoader.output_path(name, SourceSet.MAIN_SOURCE_SET_NAME);
	}

	public static String output_path(final String name, final String sourceSet) {
		return "build/classes/" + name + "/" + sourceSet;
	}

	public static String output_test_path(final String name) {
		return ConfigLoader.output_path(name, SourceSet.TEST_SOURCE_SET_NAME);
	}

	public static String src_main_path(final String name) {
		return ConfigLoader.src_path(name, SourceSet.MAIN_SOURCE_SET_NAME);
	}

	public static String src_path(final String name, final String sourceSet) {
		return "src/" + sourceSet + "/" + name;
	}

	public static String src_test_path(final String name) {
		return ConfigLoader.src_path(name, SourceSet.TEST_SOURCE_SET_NAME);
	}

	private final Pattern pattern = Pattern.compile("\\{([a-zA-Z\\?\\:\\_]+)\\}");

	private final String pck_resolver;

	public ConfigLoader(final String pck_resolver) {
		super();
		this.pck_resolver = pck_resolver;
	}

	@SuppressWarnings("unchecked")
	public Map<String, Object> load(final KeyResolver resolver, final String name, final boolean cmd) {
		final LoadSettings settings = new LoadSettingsBuilder().setLabel("Camaro").build();
		final Load load = new Load(settings);
		final Map<String, Object> cfg = (Map<String, Object>) load.loadFromInputStream(ConfigLoader.class
				.getResourceAsStream(pck_resolver + "/models/" + (cmd ? "cmds/" : "") + name + ".yml"));
		final List<String> uses = value(cfg, "use", true);
		final Map<String, Object> first = cmd ? new LinkedHashMap<>() : new HashMap<>();
		if (cmd) {
			first.putAll(cfg);
		}
		for (final String use : uses) {
			if (cmd) {
				first.putAll(load(resolver, use, cmd));
			} else {
				merge(resolver, first, load(resolver, use, cmd));
			}
		}
		if (cmd) {
			first.putAll(cfg);
		} else {
			merge(resolver, first, cfg);
		}
		return first;
	}

	public Map<String, Object> load(final String name) {
		final KeyResolver resolver = new KeyResolver();
		return load(resolver, name, false);
	}

	@SuppressWarnings("unchecked")
	private void merge(final KeyResolver resolver, final Map<String, Object> first, final Map<String, Object> second) {
		resolver.push(second);
		resolver.push(first);
		for (final Map.Entry<String, Object> entry : second.entrySet()) {
			final Object value = process_object(resolver, entry.getValue());
			final String key = entry.getKey();
			if (value instanceof List) {
				first.put(key, merge_list(resolver, value(first, key, false), (List<Object>) value));
				continue;
			}
			if (value instanceof Map) {
				final Object result = first.get(key);
				if (result instanceof Map) {
					resolver.push((Map<String, Object>) result);
					resolver.push((Map<String, Object>) value);
					merge(resolver, (Map<String, Object>) result, (Map<String, Object>) value);
					resolver.pop();
					resolver.pop();
					continue;
				}
				first.put(key, value);
				continue;
			}
			if (value instanceof String) {
				if ("$remove".equals(value)) {
					first.remove(key);
					continue;
				}
			}
			final Object fvalue = first.get(key);
			if (fvalue == null || !(fvalue instanceof List)) {
				first.put(key, process_object(resolver, value));
			} else if (fvalue instanceof List) {
				((List<Object>) fvalue).add(process_object(resolver, value));
			}
		}
		resolver.pop();
		resolver.pop();
	}

	private Object merge_list(final KeyResolver resolver, final List<Object> object, final List<Object> list) {
		final List<Object> result = new LinkedList<>();
		result.addAll(object);
		for (final Object value : list) {
			if (value.equals("$clear")) {
				result.clear();
				continue;
			}
			result.add(process_object(resolver, value));
		}
		return result;
	}

	@SuppressWarnings("unchecked")
	private Object process_object(final KeyResolver resolver, final Object value) {
		if (value instanceof Map) {
			final Map<String, Object> result = (Map<String, Object>) value;
			final List<String> uses = value(result, "use", true);
			final Map<String, Object> first = new LinkedHashMap<>();
			first.putAll(result);
			for (final String use : uses) {
				first.putAll(load(resolver, use, true));
			}
			first.putAll(result);
			final Map<String, Object> nresult = new LinkedHashMap<>();
			resolver.push(nresult);
			for (final Map.Entry<String, Object> entry : first.entrySet()) {
				nresult.put(entry.getKey(), process_object(resolver, entry.getValue()));
			}
			resolver.pop();
			return nresult;
		}
		if (value instanceof List) {
			final List<Object> result = new LinkedList<>();
			for (final Object val : (List<Object>) value) {
				result.add(process_object(resolver, val));
			}
			return result;
		}
		if (value instanceof String) {
			final String str = ((String) value).trim();
			if (str.startsWith("$output_main_path ")) {
				return ConfigLoader
						.output_main_path(toString(resolver.get(str.substring("$output_main_path ".length()).trim())));
			}
			if (str.startsWith("$output_test_path ")) {
				return ConfigLoader
						.output_test_path(toString(resolver.get(str.substring("$output_main_path ".length()).trim())));
			}
			if (str.startsWith("$format ")) {
				final Matcher matcher = pattern.matcher(str.substring("$format".length()).trim());
				final StringBuilder result = new StringBuilder();
				while (true) {
					if (!matcher.find()) {
						matcher.appendTail(result);
						break;
					} else {
						String group = matcher.group(1).trim();
						int tix = group.indexOf('?');
						if (tix != -1) {
							final String test = group.substring(0, tix);
							group = group.substring(tix + 1);
							tix = group.indexOf(':');
							String trueValue = group;
							String falseValue = "";
							if (tix != -1) {
								trueValue = group.substring(0, tix).trim();
								falseValue = group.substring(tix + 1).trim();
							}
							final Object testValue = resolver.get(test);
							if (value != null && "true".equals(String.valueOf(testValue))) {
								if (trueValue.length() > 0) {
									matcher.appendReplacement(result, String.valueOf(resolver.get(trueValue)));
								} else {
									matcher.appendReplacement(result, "");
								}
							} else {
								if (falseValue.length() > 0) {
									matcher.appendReplacement(result, String.valueOf(resolver.get(falseValue)));
								} else {
									matcher.appendReplacement(result, "");
								}
							}
							continue;
						}
						matcher.appendReplacement(result, String.valueOf(resolver.get(group)));
					}
				}
				return result.toString();
			}
			return str;
		}
		return value;
	}

	private String toString(final Object object) {
		if (object == null) {
			return null;
		}
		return String.valueOf(object);
	}

	public String toYaml(final Map<String, Object> config) {
		final Dump dump = new Dump(
				new DumpSettingsBuilder().setCanonical(false).setDefaultFlowStyle(FlowStyle.BLOCK).build());
		return dump.dumpToString(config);
	}

	@SuppressWarnings("unchecked")
	private <T> List<T> value(final Map<String, Object> result, final String key, final boolean remove) {
		final Object val = remove ? result.remove(key) : result.get(key);
		if (val == null) {
			return Collections.emptyList();
		}
		if (val instanceof List) {
			return (List<T>) val;
		}
		return Collections.singletonList((T) val);
	}

}
