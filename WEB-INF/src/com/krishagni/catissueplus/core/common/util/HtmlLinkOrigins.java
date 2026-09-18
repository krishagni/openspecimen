package com.krishagni.catissueplus.core.common.util;

import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class HtmlLinkOrigins {

	public static final String SETTING = "allowed_html_link_origins";

	public static List<String> parse(String value) {
		Set<String> origins = new LinkedHashSet<>();
		for (String entry : (value == null ? "" : value).split("[,\\r\\n\\t]+")) {
			String origin = entry.trim();
			if (origin.isEmpty()) {
				continue;
			}

			try {
				URI uri = new URI(origin);
				String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(Locale.ROOT);
				String path = uri.getRawPath();
				if ((!scheme.equals("https") && !scheme.equals("http")) || // URL does not start with either http or https
					uri.getHost() == null || // No hostname
					uri.getRawUserInfo() != null || // Embedded username/password
					uri.getRawQuery() != null || // query parameters
					uri.getRawFragment() != null || // fragment identifiers like #section
					(path != null && !path.isEmpty() && !path.equals("/")) || // path is neither empty or /
					uri.getPort() > 65535 || // out of range port
					uri.getRawAuthority().endsWith(":")) { // port separator without a port
					throw new IllegalArgumentException(origin);
				}

				int port = uri.getPort();
				boolean defaultPort = port == -1 || (scheme.equals("https") && port == 443) || (scheme.equals("http") && port == 80);
				origins.add(scheme + "://" + uri.getHost().toLowerCase(Locale.ROOT) + (defaultPort ? "" : ":" + port));
			} catch (Exception e) {
				throw new IllegalArgumentException(origin, e);
			}
		}

		return new ArrayList<>(origins);
	}
}
