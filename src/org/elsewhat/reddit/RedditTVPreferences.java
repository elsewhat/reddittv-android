package org.elsewhat.reddit;

import java.util.Date;
import java.util.StringTokenizer;

import org.apache.http.cookie.Cookie;
import org.apache.http.impl.cookie.BasicClientCookie;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.text.method.DigitsKeyListener;
import android.text.method.PasswordTransformationMethod;
import android.widget.EditText;
import android.widget.Toast;

public class RedditTVPreferences extends PreferenceActivity implements
		OnSharedPreferenceChangeListener {

	public static final String PREFS_NAME = "RedditTVPreferences";
	public static final String KEY_REDDIT_USERNAME = "reddituser";
	public static final String KEY_REDDIT_PASSWORD = "redditpwd";
	public static final String KEY_REDDIT_LOGOUT = "redditlogout";

	public static final String KEY_DO_ANALYTICS = "DoAnalytics";
	public static final String KEY_CHANGELOG_VERSION_VIEWED = "ChangelogVersionViewed";
	public static final String KEY_SUBREDDIT_ONLOAD = "subredditOnLoad";
	public static final String KEY_SUBREDDIT_PAGES_ONLOAD = "subredditPagesOnLoad";
	public static final String KEY_CATEGORY_ONLOAD = "categoryOnLoad";
	public static final String KEY_PERIOD_ONLOAD = "periodOnLoad";
	public static final String KEY_SUBREDDITS_LIST = "subredditList";
	public static final String KEY_THEME = "theme";

	public static final String KEY_REDDIT_SESSION_COOKIE = "redditSessionCookie";

	public static final String KEY_RICKROLL = "rickroll";
	public static final String KEY_RICKROLL_PERCENTAGE = "rickrollpercentage";

	public static final String SUBREDDITS_LIST_SEPARATOR = "|";

	public static final String KEY_MEDIACENTER_USERNAME = "mediacenteruser";
	public static final String KEY_MEDIACENTER_PASSWORD = "mediacenterpassword";
	public static final String KEY_MEDIACENTER_HOSTNAME = "mediacenterhost";
	public static final String KEY_MEDIACENTER_PORT = "mediacenterport";
	public static final String KEY_DO_START_XBMC_REMOTE = "dostartxbmcremote";

	public static final String KEY_REDDIT_COOKIE_VALUE = "redditcookievalue";
	public static final String KEY_REDDIT_COOKIE_DOMAIN = "redditcookiedomain";
	public static final String KEY_REDDIT_COOKIE_PATH = "redditcookiepath";
	public static final String KEY_REDDIT_COOKIE_EXPIRE = "redditcookieexpire";
	public static final String KEY_REDDIT_COOKIE_LOGGED_IN_USER = "redditloggedinuser";
	public static final String KEY_NUMBER_OF_VIDEOS_VIEWED = "nrvideos";
	

	public static final boolean DEFAULT_VALUE_DO_ANALYTICS = true;
	public static final String DEFAULT_VALUE_SUBREDDIT_ONLOAD = "videos";
	public static final String DEFAULT_VALUE_SUBREDDIT_PAGES_ONLOAD = "2";
	public static final String DEFAULT_VALUE_CATEGORY_ONLOAD = RedditAPI.CATEGORY_HOT;
	public static final String DEFAULT_VALUE_PERIOD_ONLOAD = RedditAPI.PERIOD_NULL;
	public static final String DEFAULT_VALUE_MEDIACENTER_USERNAME = "xbmc";
	public static final int DEFAULT_VALUE_MEDIACENTER_PORT = 8080;
	public static final boolean DEFAULT_VALUE_DO_START_XBMC_REMOTE = true;
	public static final boolean DEFAULT_VALUE_DO_RICKROLL = false;
	public static final String DEFAULT_VALUE_RICKROLL_PERCENTAGE = "50";
	//Should match the theme_values in strings
	public static final String THEME_NORMAL = "Theme.Normal";
	public static final String THEME_LARGE = "Theme.Large";
	public static final String DEFAULT_VALUE_THEME = THEME_NORMAL;
	public static final String DEFAULT_NUMBER_OF_VIDEOS_WATCHED = "0";
	
	

	// must match the values in the strings
	public static final String DEFAULT_ACTION_WATCH = "watch";
	public static final String DEFAULT_ACTION_GOOGLETV = "googletv";
	public static final String DEFAULT_ACTION_XBMC = "xbmc";
	public static final String DEFAULT_ACTION_LISTENTO = "listento";
	public static final String DEFAULT_VALUE_DEFAULT_ACTION = DEFAULT_ACTION_WATCH;

	public static final String KEY_DEFAULT_CLICK_ACTION = "defaultclickaction";
	

	protected EditTextPreference editTextUser;
	protected EditTextPreference editTextPassword;
	protected DeletablePreference logoutSetting;

	protected EditTextPreference editTextMCHost;
	protected EditTextPreference editTextMCPort;
	protected EditTextPreference editTextMCUser;
	protected EditTextPreference editTextMCPassword;
	protected CheckBoxPreference cbDoStartXBMCRemote;

	protected EditTextPreference editCustomSubreddit;
	protected ListPreference lpDefaultAction;
	protected ListPreference lpTheme;

	protected DeletablePreference resetSettings;

	protected CheckBoxPreference cbAnalytics;
	protected ListPreference lpSearchUsed;
	protected ListPreference lpSubredditStartup;
	protected ListPreference lpSubredditCategoryStartup;
	protected ListPreference lpSubredditPagesStartup;

	protected CheckBoxPreference cbRickroll;
	protected EditTextPreference editTextRickrollPercentage;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setPreferenceScreen(createPreferenceHierarchy());
		getPreferenceScreen().getSharedPreferences()
				.registerOnSharedPreferenceChangeListener(this);

		setContentView(R.layout.activity_preferences);

	}

	private PreferenceScreen createPreferenceHierarchy() {
		// Root
		PreferenceScreen root = getPreferenceManager().createPreferenceScreen(
				this);
		root.getPreferenceManager().setSharedPreferencesName(PREFS_NAME);

		// User preferences
		PreferenceCategory userCat = new PreferenceCategory(this);
		userCat.setTitle(R.string.lblCategoryUser);
		root.addPreference(userCat);

		editTextUser = new EditTextPreference(this);
		editTextUser.setKey(KEY_REDDIT_USERNAME);
		editTextUser.setDialogTitle(R.string.lblUsernameDialogTitle);
		editTextUser.setTitle(R.string.lblUsernameTitle);
		editTextUser.setSummary(R.string.lblUsernameSummaryBlank);
		userCat.addPreference(editTextUser);

		editTextPassword = new EditTextPreference(this);
		editTextPassword.getEditText().setTransformationMethod(
				new PasswordTransformationMethod());
		editTextPassword.setKey(KEY_REDDIT_PASSWORD);
		editTextPassword.setDialogTitle(R.string.lblPasswordDialogTitle);
		editTextPassword.setTitle(R.string.lblPasswordTitle);
		editTextPassword.setSummary(R.string.lblPasswordSummaryBlank);
		userCat.addPreference(editTextPassword);

		logoutSetting = new DeletablePreference(this);
		logoutSetting.setDialogTitle(R.string.lblLogoutDialogTitle);
		logoutSetting.setTitle(R.string.lblLogoutTitle);
		logoutSetting.setSummary(R.string.lblLogoutSummary);
		logoutSetting.setDialogIcon(android.R.drawable.ic_delete);
		logoutSetting.setKey(KEY_REDDIT_LOGOUT);
		logoutSetting.setPersistent(true);
		userCat.addPreference(logoutSetting);

		// manage subreddits
		PreferenceCategory mcGeneral = new PreferenceCategory(this);
		mcGeneral.setTitle(R.string.lblCategoryGeneral);
		root.addPreference(mcGeneral);

		lpTheme = new ListPreference(this);
		lpTheme.setEntries(getResources().getStringArray(
				R.array.themes));
		lpTheme.setEntryValues(getResources().getStringArray(
				R.array.themes_values));
		lpTheme.setKey(KEY_THEME);
		lpTheme.setDialogTitle(R.string.lblThemeDialogTitle);
		lpTheme.setTitle(R.string.lblThemeTitle);
		lpTheme.setDefaultValue(DEFAULT_VALUE_THEME);
		mcGeneral.addPreference(lpTheme);		
		
		lpDefaultAction = new ListPreference(this);
		lpDefaultAction.setEntries(getResources().getStringArray(
				R.array.default_click_action));
		lpDefaultAction.setEntryValues(getResources().getStringArray(
				R.array.default_click_action_values));
		lpDefaultAction.setKey(KEY_DEFAULT_CLICK_ACTION);
		lpDefaultAction.setDialogTitle(R.string.lblDefaultActionDialogTitle);
		lpDefaultAction.setTitle(R.string.lblDefaultActionTitle);
		lpDefaultAction.setDefaultValue(DEFAULT_VALUE_DEFAULT_ACTION);
		mcGeneral.addPreference(lpDefaultAction);
		


		PreferenceScreen subredditScreen = getPreferenceManager()
				.createPreferenceScreen(this);
		subredditScreen.setTitle(R.string.lblCustomSubredditsTitle);
		subredditScreen.setSummary(R.string.lblCustomSubredditsSummary);
		mcGeneral.addPreference(subredditScreen);
		subredditScreen
				.setOnPreferenceClickListener(new OnPreferenceClickListener() {

					@Override
					public boolean onPreferenceClick(Preference preference) {
						Intent iPreferences = new Intent(getBaseContext(),
								RedditTVSubredditsPreferences.class);
						startActivity(iPreferences);
						return true;
					}
				});

		// On startup preference
		PreferenceCategory onStartupCat = new PreferenceCategory(this);
		onStartupCat.setTitle(R.string.lblCategoryOnStartup);
		root.addPreference(onStartupCat);

		lpSubredditStartup = new ListPreference(this);
		CharSequence[] subreddits = getSubredditList(this);
		lpSubredditStartup.setEntries(subreddits);
		lpSubredditStartup.setEntryValues(subreddits);
		lpSubredditStartup.setKey(KEY_SUBREDDIT_ONLOAD);
		lpSubredditStartup.setDefaultValue(DEFAULT_VALUE_SUBREDDIT_ONLOAD);
		lpSubredditStartup
				.setDialogTitle(R.string.lblSubredditOnStartupDialogTitle);
		lpSubredditStartup.setTitle(R.string.lblSubredditOnStartupTitle);
		onStartupCat.addPreference(lpSubredditStartup);

		lpSubredditCategoryStartup = new ListPreference(this);
		CharSequence[] categories = new CharSequence[] {
				RedditAPI.CATEGORY_HOT, RedditAPI.CATEGORY_TOP,
				RedditAPI.CATEGORY_NEW, RedditAPI.CATEGORY_CONTROVERSIAL };
		lpSubredditCategoryStartup.setEntries(categories);
		lpSubredditCategoryStartup.setEntryValues(categories);
		lpSubredditCategoryStartup.setKey(KEY_CATEGORY_ONLOAD);
		lpSubredditCategoryStartup.setDefaultValue(RedditAPI.CATEGORY_HOT);
		lpSubredditCategoryStartup
				.setDialogTitle(R.string.lblCategoryOnStartupDialogTitle);
		lpSubredditCategoryStartup.setTitle(R.string.lblCategoryOnStartupTitle);

		lpSubredditCategoryStartup
				.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {

					@Override
					public boolean onPreferenceChange(Preference preference,
							Object newValue) {
						if (RedditAPI.CATEGORY_TOP.equalsIgnoreCase(newValue
								+ "")
								|| RedditAPI.CATEGORY_CONTROVERSIAL
										.equalsIgnoreCase(newValue + "")) {
							displayPeriodDialog();
						}

						return true;
					}
				});

		onStartupCat.addPreference(lpSubredditCategoryStartup);

		lpSubredditPagesStartup = new ListPreference(this);
		lpSubredditPagesStartup.setEntries(getResources().getStringArray(
				R.array.multiple_pages));
		lpSubredditPagesStartup.setEntryValues(getResources().getStringArray(
				R.array.multiple_pages_values));
		lpSubredditPagesStartup.setKey(KEY_SUBREDDIT_PAGES_ONLOAD);
		lpSubredditPagesStartup
				.setDialogTitle(R.string.lblSubredditPagesOnStartupDialogTitle);
		lpSubredditPagesStartup
				.setTitle(R.string.lblSubredditPagesOnStartupTitle);
		lpSubredditPagesStartup
				.setDefaultValue(DEFAULT_VALUE_SUBREDDIT_PAGES_ONLOAD);
		onStartupCat.addPreference(lpSubredditPagesStartup);

		// Other preferences
		PreferenceCategory mcCat = new PreferenceCategory(this);
		mcCat.setTitle(R.string.lblCategoryMediaCenter);
		root.addPreference(mcCat);

		editTextMCHost = new EditTextPreference(this);
		editTextMCHost.setKey(KEY_MEDIACENTER_HOSTNAME);
		editTextMCHost.setDialogTitle(R.string.lblMCHostDialogTitle);
		editTextMCHost.setTitle(R.string.lblMCHostTitle);
		mcCat.addPreference(editTextMCHost);

		editTextMCPort = new EditTextPreference(this);
		editTextMCPort.setKey(KEY_MEDIACENTER_PORT);
		editTextMCPort.setDialogTitle(R.string.lblMCPortDialogTitle);
		editTextMCPort.setTitle(R.string.lblMCPortTitle);
		editTextMCPort.setDefaultValue(DEFAULT_VALUE_MEDIACENTER_PORT + "");
		mcCat.addPreference(editTextMCPort);

		editTextMCUser = new EditTextPreference(this);
		editTextMCUser.setKey(KEY_MEDIACENTER_USERNAME);
		editTextMCUser.setDialogTitle(R.string.lblMCUserDialogTitle);
		editTextMCUser.setTitle(R.string.lblMCUserTitle);
		editTextMCUser.setDefaultValue(DEFAULT_VALUE_MEDIACENTER_USERNAME);
		editTextMCUser.setSummary(editTextMCUser.getText());
		mcCat.addPreference(editTextMCUser);

		editTextMCPassword = new EditTextPreference(this);
		editTextMCPassword.getEditText().setTransformationMethod(
				new PasswordTransformationMethod());
		editTextMCPassword.setKey(KEY_MEDIACENTER_PASSWORD);
		editTextMCPassword.setDialogTitle(R.string.lblMCPasswordDialogTitle);
		editTextMCPassword.setTitle(R.string.lblMCPasswordTitle);
		mcCat.addPreference(editTextMCPassword);

		cbDoStartXBMCRemote = new CheckBoxPreference(this);
		cbDoStartXBMCRemote.setDefaultValue(new Boolean(
				DEFAULT_VALUE_DO_START_XBMC_REMOTE));
		cbDoStartXBMCRemote.setKey(KEY_DO_START_XBMC_REMOTE);
		cbDoStartXBMCRemote.setTitle(R.string.lblDoStartXBMCRemoteTitle);
		cbDoStartXBMCRemote.setSummary(R.string.lblDoStartXBMCRemoteSummary);
		mcCat.addPreference(cbDoStartXBMCRemote);

		// Other preferences
		PreferenceCategory otherCat = new PreferenceCategory(this);
		otherCat.setTitle(R.string.lblCategoryOther);
		root.addPreference(otherCat);

		cbAnalytics = new CheckBoxPreference(this);
		cbAnalytics.setDefaultValue(new Boolean(DEFAULT_VALUE_DO_ANALYTICS));
		cbAnalytics.setKey(KEY_DO_ANALYTICS);
		cbAnalytics.setTitle(R.string.lblAnalyticsTitle);
		cbAnalytics.setSummary(R.string.lblAnalyticsSummary);
		otherCat.addPreference(cbAnalytics);

		resetSettings = new DeletablePreference(this);
		resetSettings.setDialogTitle(R.string.lblResetSettingDialogTitle);
		resetSettings.setTitle(R.string.lblResetSettingTitle);
		resetSettings.setSummary(R.string.lblResetSettingSummary);
		resetSettings.setDialogIcon(android.R.drawable.ic_delete);
		resetSettings.setPersistent(false);
		resetSettings
				.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {

					@Override
					public boolean onPreferenceChange(Preference preference,
							Object newValue) {
						// reset all preferences
						SharedPreferences preferences = preference.getContext()
								.getSharedPreferences(
										RedditTVPreferences.PREFS_NAME, 0);
						Editor editor = preferences.edit();
						editor.clear();
						editor.commit();
						// restart the preference
						finish();

						return true;
					}
				});

		otherCat.addPreference(resetSettings);

		// Other-WTF preferences
		PreferenceCategory otherWTFCat = new PreferenceCategory(this);
		otherWTFCat.setTitle(R.string.lblCategoryOtherWTF);
		root.addPreference(otherWTFCat);

		cbRickroll = new CheckBoxPreference(this);
		cbRickroll.setDefaultValue(new Boolean(DEFAULT_VALUE_DO_RICKROLL));
		cbRickroll.setKey(KEY_RICKROLL);
		cbRickroll.setTitle(R.string.lblRickrollTitle);
		cbRickroll.setSummary(R.string.lblRickrollSummary);
		otherWTFCat.addPreference(cbRickroll);

		editTextRickrollPercentage = new EditTextPreference(this);
		editTextRickrollPercentage.setKey(KEY_RICKROLL_PERCENTAGE);
		editTextRickrollPercentage
				.setDialogTitle(R.string.lblRickrollProbabilityDialogTitle);
		editTextRickrollPercentage
				.setTitle(R.string.lblRickrollProbabilityTitle);
		editTextRickrollPercentage
				.setDefaultValue(DEFAULT_VALUE_RICKROLL_PERCENTAGE);

		EditText innerEditText = (EditText) editTextRickrollPercentage
				.getEditText();
		innerEditText.setKeyListener(DigitsKeyListener
				.getInstance(false, false));

		editTextRickrollPercentage
				.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
					@Override
					public boolean onPreferenceChange(Preference preference,
							Object newValue) {
						try {
							int number = Integer.parseInt(newValue.toString());
							if (number < 1 || number > 100) {
								notifyUser("Please enter a valid number from 1-100");
								return false;
							}
							return true;
						} catch (NumberFormatException e) {
							notifyUser("Please enter a valid number from 1-100");
							return false;
						}
					}
				});

		// editTextRickrollPercentage.setSummary(editTextMCUser.getText());
		otherWTFCat.addPreference(editTextRickrollPercentage);

		/*
		 * lpSearchUsed = new ListPreference(this);
		 * lpSearchUsed.setDefaultValue(DEFAULT_VALUE_SEARCH);
		 * lpSearchUsed.setKey(KEY_SEARCH_METHOD);
		 * lpSearchUsed.setTitle(R.string.lblSearchTitle);
		 * lpSearchUsed.setEntries(R.array.lblsSearch);
		 * lpSearchUsed.setEntryValues(new
		 * String[]{VALUE_XSEARCH,VALUE_SEARCH});
		 * otherCat.addPreference(lpSearchUsed);
		 */
		updateSummaryBasedOnValue();

		return root;

	}

	private void updateSummaryBasedOnValue() {
		if (editTextUser.getText() != null) {
			editTextUser.setSummary(editTextUser.getText());
		} else {
			editTextUser.setSummary(R.string.lblUsernameSummaryBlank);
		}
		if (editTextPassword.getText() != null) {
			editTextPassword.setSummary(R.string.lblPasswordSummary);
		} else {
			editTextPassword.setSummary(R.string.lblPasswordSummaryBlank);
		}

		if (editTextMCUser.getText() != null) {
			editTextMCUser.setSummary(editTextMCUser.getText());
		} else {
			editTextMCUser.setSummary(R.string.lblMCUserSummaryBlank);
		}

		if (editTextMCPassword.getText() != null) {
			editTextMCPassword.setSummary(R.string.lblMCPasswordSummary);
		} else {
			editTextMCPassword.setSummary(R.string.lblMCPasswordSummaryBlank);
		}

		if (editTextMCHost.getText() != null) {
			editTextMCHost.setSummary(editTextMCHost.getText());
		} else {
			editTextMCHost.setSummary(R.string.lblMCHostSummaryBlank);
		}

		if (editTextMCPort.getText() != null) {
			editTextMCPort.setSummary(editTextMCPort.getText());
		} else {
			editTextMCPort.setSummary(R.string.lblMCPortSummaryBlank);
		}

		editTextRickrollPercentage.setSummary(getResources().getString(
				R.string.lblRickrollProbabilitySummary)
				+ " " + editTextRickrollPercentage.getText() + "%");

		lpSubredditStartup.setSummary(lpSubredditStartup.getValue()
				+ " "
				+ getResources().getString(
						R.string.lblSubredditOnStartupSummary));

		SharedPreferences settings = getSharedPreferences(
				RedditTVPreferences.PREFS_NAME, MODE_WORLD_READABLE);
		String period = settings.getString(KEY_PERIOD_ONLOAD,
				RedditAPI.PERIOD_NULL);

		if (period == null) {
			lpSubredditCategoryStartup.setSummary("Category "
					+ lpSubredditCategoryStartup.getEntry()
					+ " "
					+ getResources().getString(
							R.string.lblCategoryOnStartupSummary));
		} else {
			lpSubredditCategoryStartup.setSummary("Category "
					+ lpSubredditCategoryStartup.getEntry()
					+ " and period "
					+ period + " "
					+ getResources().getString(
							R.string.lblCategoryOnStartupSummary));
		}

		lpSubredditPagesStartup.setSummary(lpSubredditPagesStartup.getEntry()
				+ " "
				+ getResources().getString(
						R.string.lblSubredditPagesOnStartupSummary));

		lpDefaultAction.setSummary(lpDefaultAction.getValue() + " "
				+ getResources().getString(R.string.lblDefaultActionSummary));

		lpTheme.setSummary(getResources().getString(R.string.lblThemeSummary) + " " + lpTheme.getValue());
		
		CharSequence[] subreddits = getSubredditList(this);
		lpSubredditStartup.setEntries(subreddits);
		lpSubredditStartup.setEntryValues(subreddits);

	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
			String key) {
		// SharedPreferences settings =
		// getSharedPreferences(RedditTVPreferences.PREFS_NAME, 0);
		// Need to handle conversion due to lack of api support for int in
		// ListPreferences
		// String updatedValue= settings.getString(key,null);
		// Log.d("RedditTV", key + " key was updated in sharedPreferences");
		updateSummaryBasedOnValue();

	}

	/**
	 * Display a dialog for selecting period. Store result in currentPeriod
	 */
	public void displayPeriodDialog() {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle("Period");
		final CharSequence[] periods = new CharSequence[] {
				RedditAPI.PERIOD_HOUR, RedditAPI.PERIOD_DAY,
				RedditAPI.PERIOD_WEEK, RedditAPI.PERIOD_MONTH,
				RedditAPI.PERIOD_YEAR, RedditAPI.PERIOD_ALLTIME };

		builder.setSingleChoiceItems(periods, -1,
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						if (which < periods.length && which >= 0) {
							String period = (String) periods[which];
							SharedPreferences preferences = getSharedPreferences(
									RedditTVPreferences.PREFS_NAME, 0);
							Editor editor = preferences.edit();
							editor.putString(KEY_PERIOD_ONLOAD, period);
							editor.commit();

							dialog.dismiss();
						} else {
							notifyUser("Error during period selection");
						}
					}
				});
		Dialog dialog = builder.create();
		dialog.show();

	}

	/**
	 * Retrieve a stored reddit session cookie from preferences
	 * 
	 * Returns null if no session exist
	 * 
	 * @param context
	 * @return
	 */
	public static Cookie getStoredRedditSessionCookie(Context context) {
		SharedPreferences settings = context.getSharedPreferences(
				RedditTVPreferences.PREFS_NAME, MODE_WORLD_READABLE);

		String cookieValue = settings.getString(KEY_REDDIT_COOKIE_VALUE, null);
		String cookieDomain = settings
				.getString(KEY_REDDIT_COOKIE_DOMAIN, null);
		String cookiePath = settings.getString(KEY_REDDIT_COOKIE_PATH, null);
		long cookieExpiryDate = settings.getLong(KEY_REDDIT_COOKIE_EXPIRE, -1);

		if (cookieValue != null) {
			BasicClientCookie redditSessionCookie = new BasicClientCookie(
					"reddit_session", cookieValue);
			redditSessionCookie.setDomain(cookieDomain);
			redditSessionCookie.setPath(cookiePath);
			if (cookieExpiryDate != -1) {
				redditSessionCookie.setExpiryDate(new Date(cookieExpiryDate));
			} else {
				redditSessionCookie.setExpiryDate(null);
			}
			return redditSessionCookie;
		}
		return null;
	}

	/**
	 * Gets the username which was associated with the stored reddit session
	 * cookie
	 * 
	 * @param context
	 * @return
	 */
	public static String getStoredRedditSessionUsername(Context context) {
		SharedPreferences settings = context.getSharedPreferences(
				RedditTVPreferences.PREFS_NAME, MODE_WORLD_READABLE);
		return settings.getString(KEY_REDDIT_COOKIE_LOGGED_IN_USER, null);
	}

	/**
	 * Store the reddit session cookie in the preferences
	 * 
	 * @param context
	 * @param redditSessionCookie
	 */
	public static boolean setStoredRedditSessionCookie(Context context,
			Cookie redditSessionCookie, String redditUserLoggedIn) {

		SharedPreferences preferences = context.getSharedPreferences(
				RedditTVPreferences.PREFS_NAME, 0);
		Editor editor = preferences.edit();

		if (redditSessionCookie != null) {
			editor.putString(KEY_REDDIT_COOKIE_VALUE, redditSessionCookie
					.getValue());
			editor.putString(KEY_REDDIT_COOKIE_DOMAIN, redditSessionCookie
					.getDomain());
			editor.putString(KEY_REDDIT_COOKIE_PATH, redditSessionCookie
					.getPath());
			if (redditSessionCookie.getExpiryDate() != null) {
				editor.putLong(KEY_REDDIT_COOKIE_EXPIRE, redditSessionCookie
						.getExpiryDate().getTime());
			}

			editor.putString(KEY_REDDIT_COOKIE_LOGGED_IN_USER,
					redditUserLoggedIn);
			editor.commit();
			return true;
		}
		return false;
	}

	/**
	 * Store the reddit session cookie in the preferences
	 * 
	 * @param context
	 * @param redditSessionCookie
	 */
	public static void deleteStoredRedditSessionCookie(Context context) {

		SharedPreferences preferences = context.getSharedPreferences(
				RedditTVPreferences.PREFS_NAME, 0);
		Editor editor = preferences.edit();

		editor.remove(KEY_REDDIT_COOKIE_VALUE);
		editor.remove(KEY_REDDIT_COOKIE_DOMAIN);
		editor.remove(KEY_REDDIT_COOKIE_PATH);

		editor.remove(KEY_REDDIT_COOKIE_EXPIRE);
		editor.remove(KEY_REDDIT_COOKIE_LOGGED_IN_USER);
		editor.commit();
	}

	/**
	 * Call to get the subreddit list. Fetches the default list through
	 * getDefaultSubredditList if no preference has been stored for the list.
	 * 
	 * The preference is stored as a string and therefore needs to be unpacked
	 * 
	 * @param context
	 * @return
	 */
	public static CharSequence[] getSubredditList(Context context) {
		SharedPreferences preferences = context.getSharedPreferences(
				RedditTVPreferences.PREFS_NAME, MODE_WORLD_READABLE);
		String strSubredditList = preferences.getString(KEY_SUBREDDITS_LIST,
				getDefaultSubredditList(context));

		StringTokenizer tokenizer = new StringTokenizer(strSubredditList,
				SUBREDDITS_LIST_SEPARATOR, false);
		int count = tokenizer.countTokens();
		if (count == 0) {
			return null;
		}
		CharSequence[] subreddits = new CharSequence[count];
		int i = 0;
		while (tokenizer.hasMoreTokens()) {
			String subreddit = tokenizer.nextToken();
			subreddits[i] = subreddit;
			i++;
		}
		return subreddits;
	}

	/**
	 * Update the subreddit list
	 * 
	 * @param context
	 * @return
	 */
	public static void updateSubredditList(Context context,
			CharSequence[] subreddits) {

		if (subreddits == null || subreddits.length == 0) {
			Toast.makeText(context, "Cannot delete all subreddits",
					Toast.LENGTH_SHORT);
			return;
		}
		SharedPreferences preferences = context.getSharedPreferences(
				RedditTVPreferences.PREFS_NAME, 0);

		StringBuffer sbSubreddits = new StringBuffer(200);
		for (int i = 0; i < subreddits.length; i++) {
			sbSubreddits.append(subreddits[i] + SUBREDDITS_LIST_SEPARATOR);
		}
		// delete last seperator
		if (sbSubreddits.length() > 0) {
			sbSubreddits.deleteCharAt(sbSubreddits.length() - 1);
		}

		String strSubredditList = sbSubreddits.toString();
		Editor preferenceEditor = preferences.edit();
		preferenceEditor.putString(KEY_SUBREDDITS_LIST, strSubredditList);
		preferenceEditor.commit();
	}

	public static String getDefaultSubredditList(Context context) {
		CharSequence[] arDefaultSubreddits = context.getResources()
				.getStringArray(R.array.subreddit_select_values);

		StringBuffer sbSubreddits = new StringBuffer(200);
		for (int i = 0; i < arDefaultSubreddits.length; i++) {
			sbSubreddits.append(arDefaultSubreddits[i]
					+ SUBREDDITS_LIST_SEPARATOR);
		}
		// delete last seperator
		if (sbSubreddits.length() > 0) {
			sbSubreddits.deleteCharAt(sbSubreddits.length() - 1);
		}
		return sbSubreddits.toString();
	}

	private void notifyUser(CharSequence msg) {
		Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
	}

}