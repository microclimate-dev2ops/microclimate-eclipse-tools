/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.microclimate.core.internal.remote;

import java.util.HashMap;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;

public class SyncthingEvent {
	private static final String ID_KEY = "id";
	private static final String GLOBAL_ID_KEY = "globalID";
	private static final String TYPE_KEY = "type";
	private static final String TIME_KEY = "time";
	private static final String DATA_KEY = "data";
	
	public final int id;
	public final int globalId;
	public final String type;
	public final String time;
	public final Map<String, Object> data = new HashMap<String, Object>();
	
	protected SyncthingEvent(JSONObject event) throws JSONException {
		id = event.getInt(ID_KEY);
		globalId = event.getInt(GLOBAL_ID_KEY);
		type = event.getString(TYPE_KEY);
		time = event.getString(TIME_KEY);
		JSONObject dataObj = event.getJSONObject(DATA_KEY);
		for (String key : JSONObject.getNames(dataObj)) {
			Object value = dataObj.get(key);
			data.put(key, value);
		}
	}
}