package ff.camaro;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.gradle.api.Project;
import org.gradle.api.tasks.SourceSet;
import org.snakeyaml.engine.v1.api.Dump;
import org.snakeyaml.engine.v1.api.DumpSettingsBuilder;
import org.snakeyaml.engine.v1.api.Load;
import org.snakeyaml.engine.v1.api.LoadSettings;
import org.snakeyaml.engine.v1.api.LoadSettingsBuilder;
import org.snakeyaml.engine.v1.common.FlowStyle;

/**
 * Camaro configuration files are yaml files. there are two types of stores:
 * <br>
 *
 * 1. plugins: they define folder structure, dependencies, configurations,
 * project, the configuration files reside into a package
 * "ff.camaro.plugins.models" <br>
 * 2. files: they define files like metadata configuration files, camaro.json
 * files. The configuration files reside into a package "ff.camaro.files.models"
 * <br>
 *
 * @author fernandojerez
 *
 */
public class ConfigLoader {

	public static final ConfigLoader plugin = new ConfigLoader("/ff/camaro/plugin");
	public static final ConfigLoader files = new ConfigLoader("/ff/camaro/files");

	public static String eclipse_output_path(final String name, final String sourceSet) {
		return "build/classes/" + name + "/" + sourceSet;
	}

	public static String output_main_path(final org.gradle.api.Project project, final String name) {
		return ConfigLoader.output_path(project, name, SourceSet.MAIN_SOURCE_SET_NAME);
	}

	public static String output_path(final org.gradle.api.Project project, final String name, final String sourceSet) {
		return "classes/" + name + "/" + sourceSet;
	}

	public static String output_test_path(final Project project, final String name) {
		return ConfigLoader.output_path(project, name, SourceSet.TEST_SOURCE_SET_NAME);
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
	/**
	 * Load a configuration file and the use section and merge the files
	 *
	 * if the file is a fragment merge the process is: <br/>
	 * 1. load the configuration <br/>
	 * 2. merge the use <br/>
	 * 3. merge the configuration again <br/>
	 *
	 * if the file is a project model: <br/>
	 * 1. create a empty configuration<br/>
	 * 2. merge the use section <br/>
	 * 3. merge the configuration <br/>
	 *
	 * @param prj      the gradle project reference
	 * @param resolver the collection of configurations loaded
	 * @param name     the name of the configuration file
	 * @param fragment if a fragment file or model file
	 * @return A map with properties merged
	 */
	public Map<String, Object> load(final Project prj, final KeyResolver resolver, final String name,
			final boolean fragment) {
		final LoadSettings settings = new LoadSettingsBuilder().setLabel("Camaro").build();
		final Load load = new Load(settings);
		final Map<String, Object> cfg = (Map<String, Object>) load.loadFromInputStream(ConfigLoader.class
				.getResourceAsStream(pck_resolver + "/models/" + (fragment ? "fragment/" : "") + name + ".yml"));
		final List<String> uses = value(cfg, "use", true);
		final Map<String, Object> first = fragment ? new LinkedHashMap<>() : new HashMap<>();
		if (fragment) {
			first.putAll(cfg);
		}
		for (final String use : uses) {
			if (fragment) {
				first.putAll(load(prj, resolver, use, fragment));
			} else {
				merge(prj, resolver, first, load(prj, resolver, use, fragment));
			}
		}
		if (fragment) {
			first.putAll(cfg);
		} else {
			merge(prj, resolver, first, cfg);
		}
		return first;
	}

	/**
	 * Load a model configuration file
	 *
	 * @param prj  the gradle project
	 * @param name the name of the file
	 * @return A map with properties merged
	 */
	public Map<String, Object> load(final Project prj, final String name) {
		final KeyResolver resolver = new KeyResolver();
		return load(prj, resolver, name, false);
	}

	@SuppressWarnings("unchecked")
	private void merge(final Project prj, final KeyResolver resolver, final Map<String, Object> first,
			final Map<String, Object> second) {
		resolver.push(second);
		resolver.push(first);
		for (final Map.Entry<String, Object> entry : second.entrySet()) {
			final Object value = process_object(prj, resolver, entry.getValue());
			final String key = entry.getKey();
			if (value instanceof List) {
				first.put(key, merge_list(prj, resolver, value(first, key, false), (List<Object>) value));
				continue;
			}
			if (value instanceof Map) {
				final Object result = first.get(key);
				if (result instanceof Map) {
					resolver.push((Map<String, Object>) result);
					resolver.push((Map<String, Object>) value);
					merge(prj, resolver, (Map<String, Object>) result, (Map<String, Object>) value);
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
				first.put(key, process_object(prj, resolver, value));
			} else if (fvalue instanceof List) {
				((List<Object>) fvalue).add(process_object(prj, resolver, value));
			}
		}
		resolver.pop();
		resolver.pop();
	}

	private Object merge_list(final Project prj, final KeyResolver resolver, final List<Object> object,
			final List<Object> list) {
		final List<Object> result = new LinkedList<>();
		result.addAll(object);
		for (final Object value : list) {
			if (value.equals("$clear")) {
				result.clear();
				continue;
			}
			result.add(process_object(prj, resolver, value));
		}
		return result;
	}

	@SuppressWarnings("unchecked")
	private Object process_object(final Project prj, final KeyResolver resolver, final Object value) {
		if (value instanceof Map) {
			final Map<String, Object> result = (Map<String, Object>) value;
			final List<String> uses = value(result, "use", true);
			final Map<String, Object> first = new LinkedHashMap<>();
			first.putAll(result);
			for (final String use : uses) {
				first.putAll(load(prj, resolver, use, true));
			}
			first.putAll(result);
			final Map<String, Object> nresult = new LinkedHashMap<>();
			resolver.push(nresult);
			for (final Map.Entry<String, Object> entry : first.entrySet()) {
				nresult.put(entry.getKey(), process_object(prj, resolver, entry.getValue()));
			}
			resolver.pop();
			return nresult;
		}
		if (value instanceof List) {
			final List<Object> result = new LinkedList<>();
			for (final Object val : (List<Object>) value) {
				result.add(process_object(prj, resolver, val));
			}
			return result;
		}
		if (value instanceof String) {
			final String str = ((String) value).trim();
			if (str.startsWith("$output_main_path ")) {
				return ConfigLoader.output_main_path(prj,
						toString(resolver.get(str.substring("$output_main_path ".length()).trim())));
			}
			if (str.startsWith("$output_test_path ")) {
				return ConfigLoader.output_test_path(prj,
						toString(resolver.get(str.substring("$output_main_path ".length()).trim())));
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
