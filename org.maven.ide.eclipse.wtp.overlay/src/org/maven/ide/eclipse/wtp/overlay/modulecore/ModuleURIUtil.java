package org.maven.ide.eclipse.wtp.overlay.modulecore;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class ModuleURIUtil {

	public static final String URI_SEPARATOR = "&";
	
	public static Map<String, String> parseUri(String uri) {
		if (uri == null || uri.length() == 0) {
			return Collections.emptyMap();
		}
		Map<String, String> parameters = new HashMap<String, String>();
		int start = uri.indexOf(URI_SEPARATOR); 
		if (start > -1) {
			uri = uri.substring(start);
			String[] entries = uri.split(URI_SEPARATOR);
			for (String entry : entries) {
				if ("".equals(entry)) {
					continue;
				}
				String[] keyValue = entry.split("=");
				if (keyValue.length == 2) {
					parameters.put(keyValue[0], keyValue[1]);
				}
			}
		}
		return parameters;
	}
	
	public static String appendToUri(String uri, Map<String, String> parameters) {
		if (parameters == null || parameters.isEmpty()) {
			return uri;
		}
		StringBuilder sb = new StringBuilder(uri); 
		for (Map.Entry<String, String> entry : parameters.entrySet()) {
			sb.append(URI_SEPARATOR)
			   .append(entry.getKey())
			   .append("=")
			   .append(entry.getValue());
		}
		return sb.toString();
	}
	
	public static String extractModuleName(String uri) {
		if (uri != null && uri.indexOf(URI_SEPARATOR) > 0) {
			return uri.substring(0,uri.indexOf(URI_SEPARATOR));
		}
		return uri;
	}
}
