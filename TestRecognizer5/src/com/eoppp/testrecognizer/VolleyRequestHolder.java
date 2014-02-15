package com.eoppp.testrecognizer;

import android.content.Context;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;

public final class VolleyRequestHolder {
	private static RequestQueue sRequestQueue;

	private VolleyRequestHolder() {

	}

	public static RequestQueue newRequestQueue(final Context context) {
		if (sRequestQueue == null) {
			sRequestQueue = Volley.newRequestQueue(context);
		}
		return sRequestQueue;
	}
}