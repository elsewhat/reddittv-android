package org.elsewhat.reddit;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.google.android.apps.analytics.GoogleAnalyticsTracker;

public class Analytics {
	public final static String ANALYTICS_ID = "UA-22792715-1";
	private static GoogleAnalyticsTracker tracker = GoogleAnalyticsTracker
			.getInstance();
	private static boolean isStarted = false;

	public static void trackPageView(Context context, String pageName) {
		if (isEnabled(context)) {
			try {
				if (!isStarted) {

					tracker.start(Analytics.ANALYTICS_ID, 15, context);
					isStarted = true;

				}
				tracker.trackPageView(pageName);
			} catch (RuntimeException e) {
				Log.d("RedditTV", "Hmmm exception during analytics", e);
			}
		}
	}

	public static void trackEvent(Context context, String category,
			String name, String value) {
		if (isEnabled(context)) {
			try {
				if (!isStarted) {

					tracker.start(Analytics.ANALYTICS_ID, 15, context);
					isStarted = true;

				}
				// analytics don't like whitespace
				name = name.replaceAll(" ", "");
				if (value == null) {
					value = "null";
				}
				value = value.replaceAll(" ", "");
				tracker.trackEvent(category, name, value, 0);
			} catch (RuntimeException e) {
				Log.d("RedditTV", "Hmmm exception during analytics", e);
			}
		}

	}

	public static boolean isEnabled(Context context) {
		SharedPreferences settings = context.getSharedPreferences(
				RedditTVPreferences.PREFS_NAME, 0);
		return settings.getBoolean(RedditTVPreferences.KEY_DO_ANALYTICS, false);
	}
}