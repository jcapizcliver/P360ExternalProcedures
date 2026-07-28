package mx.com.liverpool.p360.services.core.temp.exports;

import mx.com.liverpool.p360.services.core.ELog;

public class RealExportProductsUtils {

	private static final java.util.regex.Pattern PURPOSE_REFERENCE_PATTERN = java.util.regex.Pattern.compile( "^(\\d+)\\[\\|]\\[\\|]\\d+" + "\\{#}(\\d+)\\[\\|]\\[\\|]\\d+$");
	
	private final ELog log;
	
	public RealExportProductsUtils(ELog log) {
		this.log = log;
	}
	
	private void log(String message) {
		this.log.log(message);
	}
	
	private void logE(Exception e) {
		this.log.logE(e);
	}
	
	public org.json.JSONArray resolvePurposeCodes( String purposesRaw, java.util.Map<Integer, String> purposeCodes) {

		org.json.JSONArray purposes = new org.json.JSONArray();

		if (purposesRaw == null || purposesRaw.isBlank()) {
			return purposes;
		}

		for (String rawToken : purposesRaw.split(";")) {
			String token = rawToken.trim();

			if (token.isEmpty()) {
				continue;
			}

			java.util.regex.Matcher matcher = PURPOSE_REFERENCE_PATTERN.matcher(token);

			if (!matcher.matches()) {
				log("Purpose con formato inesperado: " + token);
				continue;
			}

			try {
				int lookupID = Integer.parseInt(matcher.group(1));

				int lookupValueID = Integer.parseInt(matcher.group(2));

				if (lookupID != 2) {
					log(
						"Purpose con LookupID inesperado: "
						+ lookupID
						+ ", token: "
						+ token);
					continue;
				}

				String code = purposeCodes.get(lookupValueID);

				if (code == null) {
					log(
						"No se encontró Code para Purpose "
						+ "LookupValueID = "
						+ lookupValueID);

					continue;
				}
				purposes.put(code);
			} catch (NumberFormatException e) {
				log("Purpose con IDs inválidos: " + token);
			}
		}
		return purposes;
	}
	
}
