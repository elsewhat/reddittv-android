package org.elsewhat.reddit;

import android.content.Context;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.preference.Preference.OnPreferenceChangeListener;
import android.util.Log;


public class RedditTVSubredditsPreferences extends PreferenceActivity {

	public static final String PREFS_NAME = "RedditTVPreferences";

	protected EditTextPreference editNewSubreddit;
	protected DeletablePreference[] deleteableSubreddits;
	protected PreferenceCategory currentSubreddits; 

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setPreferenceScreen(createPreferenceHierarchy());
		//getPreferenceScreen().getSharedPreferences()
		//		.registerOnSharedPreferenceChangeListener(this);

		setContentView(R.layout.activity_preferences);


	}

	private PreferenceScreen createPreferenceHierarchy() {
		
		
		// Root
		PreferenceScreen root = getPreferenceManager().createPreferenceScreen(
				this);
		root.getPreferenceManager().setSharedPreferencesName(PREFS_NAME);
		// User preferences
		
		PreferenceCategory manageCat = new PreferenceCategory(this);
		manageCat.setTitle(R.string.lblCategorySubreddits);
		root.addPreference(manageCat);

		editNewSubreddit = new EditTextPreference(this);
		editNewSubreddit.setPersistent(false);
		editNewSubreddit.setDialogTitle(R.string.lblNewSubredditDialogTitle);
		editNewSubreddit.setTitle(R.string.lblNewSubredditTitle);
		editNewSubreddit.setSummary(R.string.lblNewSubredditSummary);
		final Context preferenceContext = this;
		editNewSubreddit.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
			
			@Override
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				Log.d("RedditTV", "New value " + newValue);
				
				CharSequence[] existingSubreddits = getSubredditsFromUI();
				CharSequence newSubreddit = (CharSequence)newValue;
				CharSequence[] toUpdateSubreddits = addToCharSequenceArray(existingSubreddits, newSubreddit);
				RedditTVPreferences.updateSubredditList(preferenceContext, toUpdateSubreddits);
				updateSubredditUI();
				return true;
			}

		});
		
		manageCat.addPreference(editNewSubreddit);

		currentSubreddits = new PreferenceCategory(this);
		currentSubreddits.setTitle(R.string.lblCategoryCurrentSubreddits);
		root.addPreference(currentSubreddits);
		
		updateSubredditUI();

		
		return root;

	}

	private void updateSubredditUI() {
		currentSubreddits.removeAll();
		CharSequence[] subreddits = RedditTVPreferences.getSubredditList(this);
		deleteableSubreddits=new DeletablePreference[subreddits.length];
		final Context preferenceContext = this;
		
		for (int i = 0; i < deleteableSubreddits.length; i++) {
			CharSequence subreddit = subreddits[i];
			DeletablePreference dSubredditValue = new DeletablePreference(this);
			dSubredditValue.setDialogTitle("Delete " +subreddit+ " subreddit?");
			//title must match the subreddit name
			dSubredditValue.setTitle(subreddit);
			dSubredditValue.setSummary("Click to delete");
			dSubredditValue.setDialogIcon(android.R.drawable.ic_delete);
			dSubredditValue.setPersistent(false);
			dSubredditValue.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
				
				@Override
				public boolean onPreferenceChange(Preference preference, Object newValue) {
					//Log.d("RedditTV", "on onPreferenceChange "+ newValue);
					CharSequence[] existingSubreddits = getSubredditsFromUI();
					CharSequence removeSubreddit = preference.getTitle();
					
					CharSequence[] toUpdateSubreddits =removeFromCharSequenceArray(existingSubreddits,removeSubreddit);
					RedditTVPreferences.updateSubredditList(preferenceContext, toUpdateSubreddits);
					updateSubredditUI();
					return true;
				}
			});
			
		
			deleteableSubreddits[i]=dSubredditValue;
			currentSubreddits.addPreference(dSubredditValue);
		}
	}
	

	
	private CharSequence[] getSubredditsFromUI(){
		CharSequence[] subredditsFromUI = new CharSequence[deleteableSubreddits.length];
		for (int i = 0; i < deleteableSubreddits.length; i++) {
			subredditsFromUI[i] = deleteableSubreddits[i].getTitle();
		}
		return subredditsFromUI;
	}
	
	public static CharSequence[] addToCharSequenceArray(CharSequence [] csArray, CharSequence cs){
		if(csArray==null){
			return new CharSequence[] {cs};
		}
 		CharSequence[] csArray2 = new CharSequence[csArray.length+1];
		for (int i = 0; i < csArray.length; i++) {
			csArray2[i]= csArray[i];
		}
		csArray2[csArray2.length-1]=cs;
		return csArray2;
	}
	
	public static CharSequence[] removeFromCharSequenceArray(CharSequence [] csArray, CharSequence cs){
		if(csArray==null){
			return null;
		}
 		CharSequence[] csArray2 = new CharSequence[csArray.length-1];
 		int j=0;
		for (int i = 0; i < csArray.length; i++) {
			if(!csArray[i].equals(cs) && j<csArray2.length){
				csArray2[j]= csArray[i];
				j++;
			}
		}
		return csArray2;
	}




}