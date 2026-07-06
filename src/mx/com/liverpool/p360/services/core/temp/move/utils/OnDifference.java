package mx.com.liverpool.p360.services.core.temp.move.utils;

import mx.com.liverpool.p360.services.core.RESTWorkshop;

public interface OnDifference {

	void doResolve(org.json.JSONArray columns, org.json.JSONArray values, RESTWorkshop rw);
}
