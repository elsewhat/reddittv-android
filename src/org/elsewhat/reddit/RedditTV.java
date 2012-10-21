package org.elsewhat.reddit;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Random;


import org.apache.http.client.CookieStore;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.xbmc.api.business.INotifiableManager;
import org.xbmc.httpapi.Connection;
import org.xbmc.httpapi.client.ControlClient;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.View.OnClickListener;
import android.view.View.OnCreateContextMenuListener;
import android.view.View.OnLongClickListener;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;

@SuppressLint("WorldReadableFiles")
public class RedditTV extends Activity implements OnCreateContextMenuListener,
		OnItemClickListener, OnClickListener, OnLongClickListener, OnSharedPreferenceChangeListener {
	private static final int ACTION_WATCH = Menu.FIRST;
	private static final int ACTION_COMMENTS = Menu.FIRST + 1;
	private static final int ACTION_SENDTO = Menu.FIRST + 2;
	private static final int ACTION_SENDTO_VIEW = Menu.FIRST + 3;
	private static final int ACTION_FAVORITE = Menu.FIRST + 4;
	private static final int ACTION_UPVOTE = Menu.FIRST + 5;
	private static final int ACTION_DOWNVOTE = Menu.FIRST + 6;
	private static final int ACTION_LISTENTO = Menu.FIRST + 7;

	private RedditPostAdapter postsListAdapter;
	private RedditAPI redditAPI;
	
	

	private String currentSubreddit = "videos";
	private String currentCategory = RedditAPI.CATEGORY_HOT;
	private String currentPeriod = RedditAPI.PERIOD_NULL;
	private int nrPages=1;
	private boolean doSelectFirst=false;
	
	ListView listView;
	
	//static in order to keep playing in backgroun untill gc
	private static MediaPlayer mediaplayer;
	private static final int LISTENTO_NOTIFICATION_ID=1;
	private static final String INTENT_STOP_LISTENTO="STOPLISTENTO";
	
	private boolean themeHasBeenChanged = false;
	
	
	
	

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.d("RedditTV", "onCreate called");
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		
		SharedPreferences settings = getSharedPreferences(RedditTVPreferences.PREFS_NAME, MODE_WORLD_READABLE);
		settings.registerOnSharedPreferenceChangeListener(this);
		setTheme(settings);
		
		setContentView(R.layout.main);
		//check if we have data persisted in the activity 
		//landscape orientation or activity destroyed
		

		RedditTVSemiPersistedData activityPersistedData = getLastNonConfigurationInstance();
		if (activityPersistedData!=null){
			redditAPI=activityPersistedData.getRedditAPI();
			postsListAdapter=activityPersistedData.getPostsListAdapter();
			currentSubreddit=activityPersistedData.getCurrentSubreddit();
			currentCategory= activityPersistedData.getCurrentCategory();
			currentPeriod= activityPersistedData.getCurrentPeriod();
			
		}else {//if we have a new activity, we read some values from preferences
			
			//Need to handle conversion due to lack of api support for int in ListPreferences
			currentSubreddit= settings.getString(RedditTVPreferences.KEY_SUBREDDIT_ONLOAD, "videos");
			String strNrPages= settings.getString(RedditTVPreferences.KEY_SUBREDDIT_PAGES_ONLOAD, "1");
			
			currentCategory=settings.getString(RedditTVPreferences.KEY_CATEGORY_ONLOAD, RedditAPI.CATEGORY_HOT);
			currentPeriod= settings.getString(RedditTVPreferences.KEY_PERIOD_ONLOAD, RedditAPI.PERIOD_NULL);

			try {
				nrPages = Integer.parseInt(strNrPages);
			}catch (NumberFormatException e) {
				nrPages=1;
			}
			//lets check if we have a logged on user we can use
			redditAPI=new RedditAPI();
			String alreadyLoggedInUser = redditAPI.getPersistedLoggedInUser(this);
			if(redditAPI.isLoggedIn(this)&& alreadyLoggedInUser!=null){
				notifyUser("Retrieved persisted login for " +alreadyLoggedInUser);
			}
			
		}
		
		if(redditAPI==null){
			redditAPI = new RedditAPI();
		}

		//check if we should stop the mediaplayer
		Intent incomingIntent =getIntent();
		if(incomingIntent!=null){
			//TODO check for value if several actions exist
			if(incomingIntent.getAction()!=null && incomingIntent.getAction().equalsIgnoreCase(INTENT_STOP_LISTENTO) ){
				try {
					if(mediaplayer!=null && mediaplayer.isPlaying()){
						notifyUser("Stopping audio playback");
						mediaplayer.stop();
						mediaplayer.release();
					}
				}catch (IllegalStateException e) {
					//ignore
				}

			}
		}

		
		listView = (ListView) findViewById(R.id.video_list);

		LayoutInflater inflater = (LayoutInflater) this
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View footerView = inflater.inflate(R.layout.video_list_row_load,
				null);
		footerView.setOnClickListener(this);
		footerView.setOnLongClickListener(this);
		listView.addFooterView(footerView);

		listView.setOnItemClickListener(this);
		
		if(postsListAdapter==null){//Start of an activity
			Analytics.trackPageView(this,"/home");
			
			
			// create empty list and bind to list view
			ArrayList<RedditPost> redditPosts = new ArrayList<RedditPost>(50);
			postsListAdapter = new RedditPostAdapter(this, R.layout.video_list_row,
					redditPosts);
			// load posts asynchronously
			listView.setAdapter(postsListAdapter);
			doSelectFirst=true;
			new AddRedditPostsTask(this,currentSubreddit, currentCategory, currentPeriod,true,nrPages)
			.execute();
		}else {
			listView.setAdapter(postsListAdapter);
		}

		registerForContextMenu(listView);
		updateLoadMoreText();
	}


	@Override
	public RedditTVSemiPersistedData onRetainNonConfigurationInstance() {
		return new RedditTVSemiPersistedData(postsListAdapter, redditAPI,currentSubreddit,currentCategory,currentPeriod);
	}
	
	@Override
	public RedditTVSemiPersistedData getLastNonConfigurationInstance() {
		// TODO Auto-generated method stub
		return (RedditTVSemiPersistedData)super.getLastNonConfigurationInstance();
	}


	@Override
	protected void onDestroy() {
		Log.d("RedditTV", "ondestroy called");
		SharedPreferences settings = getSharedPreferences(RedditTVPreferences.PREFS_NAME, MODE_WORLD_READABLE);
		settings.unregisterOnSharedPreferenceChangeListener(this);
		super.onDestroy();
	}

	@Override
	protected void onPause() {
		Log.d("RedditTV", "onPause called");
		super.onPause();
	}


	@Override
	protected void onResume() {
		Log.d("RedditTV", "onResume called");
		super.onResume();
		
		if(themeHasBeenChanged){
			Intent intent = getIntent();
			notifyUser(getResources().getString(R.string.msgNewThemeReloading));
			finish();
			startActivity(intent);
		}
	}

	@Override
	protected void onStop() {
		Log.d("RedditTV", "onStop called");
		super.onStop();
	}
	
	@Override
	protected void onRestart() {
		Log.d("RedditTV", "onRestart called");
		super.onStop();
	}

	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {
		
		//fix for googletv that triggers this instead of the footer view's listener
		if(menuInfo instanceof AdapterContextMenuInfo){
			int position = ((AdapterContextMenuInfo)menuInfo).position;
			if(position>=postsListAdapter.getCount()){
				onLongClick(v);
				return;
			}
		}
		
		super.onCreateContextMenu(menu, v, menuInfo);
		menu.add(0, ACTION_WATCH, 0, R.string.contextMenuWatch);
		menu.add(0, ACTION_COMMENTS, 0, R.string.contextMenuComments);
		menu.add(0, ACTION_SENDTO, 0, R.string.contextMenuSendTo);
		menu.add(0, ACTION_SENDTO_VIEW, 0, R.string.contextMenuSendToView);
		menu.add(0, ACTION_FAVORITE, 0, R.string.contextMenuFavorite);
		menu.add(0, ACTION_LISTENTO, 0, R.string.contextListenTo);
		
		//menu.add(0, ACTION_UPVOTE, 0, R.string.contextMenuUpvote);
		//menu.add(0, ACTION_DOWNVOTE, 0, R.string.contextMenuDownvote);

	}
	

	public boolean onContextItemSelected(MenuItem item) {
		AdapterContextMenuInfo menuInfo = (AdapterContextMenuInfo) item
				.getMenuInfo();
		RedditPost redditPost = postsListAdapter.getItem(menuInfo.position);

		switch (item.getItemId()) {
		case ACTION_WATCH:
			actionWatch(redditPost);

			return true;
		case ACTION_COMMENTS:
			// URL should trigger Reddit is fun if installed
			startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(redditPost
					.getCommentsURL())));
			return true;

		case ACTION_SENDTO:
			actionSendToGoogleTV(redditPost);

			return true;
			
		case ACTION_SENDTO_VIEW:	
			actionSendToXBMC(redditPost);
	
			return true;			

		case ACTION_FAVORITE:
			actionSave(redditPost);
			return true;

		case ACTION_UPVOTE:
			Toast.makeText(this, "Not implemented yet. View comments and upvote from web interface (or Reddit is fun)", Toast.LENGTH_SHORT)
					.show();
			return true;

		case ACTION_DOWNVOTE:
			Toast.makeText(this, "Not implemented yet. View comments and upvote from web interface (or Reddit is fun)", Toast.LENGTH_SHORT)
					.show();
			return true;
			
		case ACTION_LISTENTO:
			actionListenTo(redditPost);
			return true;

		}
		return super.onContextItemSelected(item);
	}

	private void actionSendToXBMC(RedditPost redditPost) {
		Analytics.trackEvent(this, "RedditTV", "sendto", "XBMC");
		SharedPreferences settings = getSharedPreferences(RedditTVPreferences.PREFS_NAME,MODE_WORLD_READABLE );
		//Need to handle conversion due to lack of api support for int in ListPreferences
		String mcHostname= settings.getString(RedditTVPreferences.KEY_MEDIACENTER_HOSTNAME,null);
		if (mcHostname==null){
			notifyUser(getResources().getText(R.string.mediacenterNotConfigured));
			return;
		}
		String strMCPort = settings.getString(RedditTVPreferences.KEY_MEDIACENTER_PORT,"");
		int mcPort=RedditTVPreferences.DEFAULT_VALUE_MEDIACENTER_PORT;
		try {
			mcPort= Integer.parseInt(strMCPort);
		}catch (NumberFormatException e) {
			Log.w("RedditTV", strMCPort + " is not a valid port. Using default port "+ mcPort);
		}
		String mcUser =settings.getString(RedditTVPreferences.KEY_MEDIACENTER_USERNAME,null);
		String mcPassword =settings.getString(RedditTVPreferences.KEY_MEDIACENTER_PASSWORD,null);
		
		INotifiableManager xbmcManager = new INotifiableManager() {  
            public void onError(Exception e) {
            	Log.e("RedditTV", "Failed to integrate with XBMC",e);
            	notifyUser("Failed to integrate with XBMC. Error " + e.toString());
            }
		};
		//TODO read from preferences		
		Connection xbmcConnection= Connection.getInstance(mcHostname, mcPort);
		xbmcConnection.setAuth(mcUser, mcPassword);
		//force update in case of changed preferences
		xbmcConnection.setHost(mcHostname, mcPort);
		
		ControlClient xbmcClient = new ControlClient(xbmcConnection);
		
		Log.d("RedditTV", "Connecting to XBMC with host"+mcHostname+ " and port"+ mcPort);
		//TODO: Get youtube id
		xbmcClient.playUrl(xbmcManager, "plugin://plugin.video.youtube/?action=play_video&videoid="+redditPost.getYoutubeId());
		
		//see if we should launch the official XBMC remote
		boolean doLaunchXBMCRemote = settings.getBoolean(RedditTVPreferences.KEY_DO_START_XBMC_REMOTE, RedditTVPreferences.DEFAULT_VALUE_DO_START_XBMC_REMOTE);
		if(doLaunchXBMCRemote){
			actionLaunchXBMCRemote();
		}
		
	}
	
	private void actionLaunchXBMCRemote(){
		Intent intent = new Intent(Intent.ACTION_MAIN);
	    intent.setComponent(ComponentName.unflattenFromString("org.xbmc.android.remote/.presentation.activity.HomeActivity"));
	    intent.addCategory(Intent.CATEGORY_LAUNCHER);
	    if(AndroidUtil.isIntentAvailable(this, intent)){
	    	startActivity(intent);
	    }else {
	    	notifyUser("Could not start XBMC Remote as it is not installed.\nPlease install or modify setting for starting the remote");
	    }	
	}
	
	private void actionSendToGoogleTV(RedditPost redditPost){
		Analytics.trackEvent(this, "RedditTV", "sendto", "GoogleTV");
		String shareTxt = redditPost.getUrl();
		Log.i("RedditTV", "Sharing url "+ shareTxt);
		
		Intent shareIntent = new Intent();
		shareIntent.setAction(Intent.ACTION_SEND);
		shareIntent.setType("text/plain");
		shareIntent.putExtra(Intent.EXTRA_TEXT, shareTxt);
		//shareIntent.setClassName("com.google.android.apps.tvremote", "StartupActivity");
		/* Send it off to the Activity-Chooser */
		//startActivity(Intent.createChooser(shareIntent, "Send to ..."));
		startActivity(shareIntent);
	}

	private void actionWatch(RedditPost redditPost) {
		//let us always use the youtube id and generate our own url
		String postUrl = "http://www.youtube.com/watch?v="+redditPost.getYoutubeId();
		
		//check if we're in rickroll mode!
		SharedPreferences settings = getSharedPreferences(RedditTVPreferences.PREFS_NAME, MODE_WORLD_READABLE);
		boolean doRickrollMode= settings.getBoolean(RedditTVPreferences.KEY_RICKROLL, false);
		if(doRickrollMode){
			int percentage=50;
			try {
				String strPercentage= settings.getString(RedditTVPreferences.KEY_RICKROLL_PERCENTAGE,RedditTVPreferences.DEFAULT_VALUE_RICKROLL_PERCENTAGE );
				percentage = Integer.parseInt(strPercentage);
			}catch (NumberFormatException e) {
				Log.w("RedditTV", "Rickroll percentage was not a number", e);
			}
			
			Random random = new Random(System.currentTimeMillis());
			//get int from [1,100]
			int randomInt =random.nextInt(100)+1;
			
			if(percentage>=randomInt){
				Log.w("RedditTV", "Prepare to be rickrolled!");
				Analytics.trackEvent(this, "RedditTV", "sendto", "rickroll");
				postUrl= "http://www.youtube.com/watch?v=oHg5SJYRHA0";
			}
		}
		
		displayBackButtonInfoOnWatch();
		startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(postUrl)));

	}
	
	/**
	 * Action for saving a reddit.
	 * Errors and messages to user is handled as part of this method
	 * @param redditPost
	 */
	private void actionSave(RedditPost redditPost){
		SaveRedditPostTask saveRedditPostTask = new SaveRedditPostTask(this, redditPost);
		saveRedditPostTask.execute();
	}
	
	
	private void actionListenTo(RedditPost redditPost) {
		ListenToTask listenToTask = new ListenToTask(this,redditPost,mediaplayer);
		listenToTask.execute();

	}
	
	private void actionClearList(){
		while (postsListAdapter.getCount() >= 1) {
			postsListAdapter.remove((postsListAdapter.getItem(0)));
		}
		postsListAdapter.notifyDataSetChanged();
		
		redditAPI.doClearAfterHistory();
		doSelectFirst=true;
	}

	public void onItemClick(AdapterView<?> parent, View view, int position,
			long id) {
		
		//fix for googletv that triggered this event for the footer also
		if(position >= postsListAdapter.getCount()){
			onClick(view);
			return;
		}
		
		RedditPost redditPost = postsListAdapter.getItem(position);
		
		//find the default action
		SharedPreferences settings = getSharedPreferences(RedditTVPreferences.PREFS_NAME, MODE_WORLD_READABLE);
		String action=settings.getString(RedditTVPreferences.KEY_DEFAULT_CLICK_ACTION, RedditTVPreferences.DEFAULT_VALUE_CATEGORY_ONLOAD);
		if(RedditTVPreferences.DEFAULT_ACTION_WATCH.equalsIgnoreCase(action)){
			actionWatch(redditPost);
		}else if (RedditTVPreferences.DEFAULT_ACTION_XBMC.equalsIgnoreCase(action)){
			actionSendToXBMC(redditPost);
		}else if (RedditTVPreferences.DEFAULT_ACTION_GOOGLETV.equalsIgnoreCase(action)){
			actionSendToGoogleTV(redditPost);
		}else if (RedditTVPreferences.DEFAULT_ACTION_LISTENTO.equalsIgnoreCase(action)){
			actionListenTo(redditPost);
		}else {
			Log.w("RedditTV", "Unknown default action. Defaulting to watch action");
			actionWatch(redditPost);
		}
		
	}

	public void onClick(View view) {
		nrPages=1;
		new AddRedditPostsTask(this,currentSubreddit, currentCategory, currentPeriod,true,nrPages)
				.execute();
	}


	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.menu, menu);
		return true;
	}

	@Override
	public boolean onMenuItemSelected(int featureId, MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menuPickSubreddit:
			displayPickSubredditDialog();
			return true;
		case R.id.menuSortBy:
			displayCategoryDialog();
			return true;
		case R.id.menuClearList:
			actionClearList();

			return true;
		case R.id.menuAbout:
			startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://redd.it/gsmet")));
			return true;
		case R.id.menuPreferences:
			Intent iPreferences = new Intent(this, RedditTVPreferences.class);
			startActivity(iPreferences);
			return true;
		default:
			return super.onMenuItemSelected(featureId, item);
		}

	}

	public void displayPickSubredditDialog(){
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle("Select subreddit");
		CharSequence [] subredditsFromPreferences = RedditTVPreferences.getSubredditList(this);
		
		
		
		//if logged in we add the saved subreddit
		if (redditAPI.isLoggedIn(this)){
			CharSequence savedSubreddit = "saved";
			subredditsFromPreferences= RedditTVSubredditsPreferences.addToCharSequenceArray(subredditsFromPreferences, savedSubreddit);
		}
		
		//Add manage subreddits link
		CharSequence manageSubreddits = getResources().getString(R.string.menu_manage_subreddits);
		final CharSequence[] subreddits = RedditTVSubredditsPreferences.addToCharSequenceArray(subredditsFromPreferences, manageSubreddits);

		final Context context = this;
		
		builder.setSingleChoiceItems(subreddits,
				-1,
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						if (which < subreddits.length-1&& which>=0){
							currentSubreddit=(String)subreddits[which];
							updateLoadMoreText();
							dialog.dismiss();	
						}else if(which == subreddits.length-1) {
							Intent iPreferences = new Intent(context, RedditTVSubredditsPreferences.class);
							startActivity(iPreferences);
							dialog.dismiss();	
						}
						else {
							notifyUser("Error during period selection");
						}	
					}
				}
				);
		Dialog dialog = builder.create();
		dialog.show();	
	}
	
	/**
	 * Display select category dialog
	 * May trigger select Period dialog.
	 * Result stored in currentCategory
	 * 
	 */
	public void displayCategoryDialog(){
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle("Choose category");
		builder.setSingleChoiceItems(new CharSequence[] {
				RedditAPI.CATEGORY_HOT, RedditAPI.CATEGORY_TOP,
				RedditAPI.CATEGORY_NEW, RedditAPI.CATEGORY_CONTROVERSIAL },
				-1,
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						switch (which){
							case 0: //HOT
								currentCategory=RedditAPI.CATEGORY_HOT;
								currentPeriod = RedditAPI.PERIOD_NULL;
								dialog.dismiss();
								updateLoadMoreText();
								return;
							case 1://Top
								currentCategory=RedditAPI.CATEGORY_TOP;
								dialog.dismiss();
								updateLoadMoreText();
								displayPeriodDialog();
								return;
							case 2://new
								currentCategory=RedditAPI.CATEGORY_NEW;
								currentPeriod = RedditAPI.PERIOD_NULL;
								dialog.dismiss();
								updateLoadMoreText();
								return;
							case 3://controversial
								currentCategory=RedditAPI.CATEGORY_CONTROVERSIAL;
								dialog.dismiss();
								updateLoadMoreText();
								displayPeriodDialog();
								return;
						}		
							
					}
				}
				);
		Dialog dialog = builder.create();
		dialog.show();
	}
	/**
	 * Display a dialog for selecting period.
	 * Store result in currentPeriod
	 */
	public void displayPeriodDialog(){
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle("Period");
		final CharSequence [] periods = new CharSequence[] {
				RedditAPI.PERIOD_HOUR,RedditAPI.PERIOD_DAY, RedditAPI.PERIOD_WEEK,
				RedditAPI.PERIOD_MONTH, RedditAPI.PERIOD_YEAR,RedditAPI.PERIOD_ALLTIME };
		
		
		builder.setSingleChoiceItems(periods,
				-1,
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						if (which < periods.length&& which>=0){
							currentPeriod=(String)periods[which];
							updateLoadMoreText();
							dialog.dismiss();	
						}else {
							notifyUser("Error during period selection");
						}	
					}
				}
				);
		Dialog dialog = builder.create();
		dialog.show();
		
	}

	private void displayBackButtonInfoOnWatch() {
		SharedPreferences settings = getSharedPreferences(RedditTVPreferences.PREFS_NAME, MODE_WORLD_WRITEABLE);
		String strNrOfVideosWatched = 
				settings.getString(RedditTVPreferences.KEY_NUMBER_OF_VIDEOS_VIEWED, RedditTVPreferences.DEFAULT_NUMBER_OF_VIDEOS_WATCHED);
		
		try {
			int nrOfVideosWatched = Integer.valueOf(strNrOfVideosWatched);
			if(nrOfVideosWatched<=5){
				Toast.makeText(this, R.string.backButtonInfoOnWatch, Toast.LENGTH_LONG)
					.show();
			}
			nrOfVideosWatched++;
			Editor editor = settings.edit();
			editor.putString(RedditTVPreferences.KEY_NUMBER_OF_VIDEOS_VIEWED, nrOfVideosWatched+"");
			editor.commit();
		}catch(RuntimeException e){
			Log.w("RedditTV", "Got exception during int conversion. No biggie" , e);
		}
	
		
	}



	/**
	 * Async method of retrieving reddit posts Create new object for each
	 * invocation Uses redditAPI object (must be initialized) Uses
	 * postsListAdapter (must be initialized) by populating with new RedditPosts
	 */
	class AddRedditPostsTask extends AsyncTask<Void, String, Throwable> {
		private String subreddit;
		private String category;
		private String period;
		private int nrPages = 1;
		private boolean attemptLogin=false;
		private Context context;
		ArrayList<RedditPost> redditPosts;

		public AddRedditPostsTask(Context context,String subreddit, String category,
				String period,boolean attemptLogin, int nrPages) {
			this.subreddit = subreddit;
			this.category = category;
			this.period = period;
			this.nrPages=nrPages;
			this.context=context;
			this.attemptLogin=attemptLogin;
			Analytics.trackEvent(context, "Load", "Subreddit", subreddit);
			Analytics.trackEvent(context, "Load", "Category", category);
			Analytics.trackEvent(context, "Load", "Period", period);
			
		}
		
		public AddRedditPostsTask(Context context,String subreddit, String category,
				String period) {
			this(context,subreddit,category,period,true,1);
		}

		@Override
		protected void onPreExecute() {
			// update UI for loading more
			ProgressBar progressBar = (ProgressBar) findViewById(R.id.loading_progress);
			//TextView loadTitle = (TextView) findViewById(R.id.load_title);
			if (progressBar != null) {
				progressBar.setVisibility(View.VISIBLE);
				updateLoadMoreText();
			}

		}

		@Override
		protected Throwable doInBackground(Void... params) {
			try {
				//if we can, we will log in
				if(attemptLogin && !redditAPI.isLoggedIn(context)&&redditAPI.canLogIn(context)){
					publishProgress("Login to reddit ...");
					
					boolean loginResult = redditAPI.doLogin(context);
					if(loginResult==false){
						Analytics.trackEvent(context, "RedditTV", "Logon", "LogonFailed");
						publishProgress("Login to reddit failed!\nSettings may be wrong or reddit is having problems");
					}else {
						Analytics.trackEvent(context, "RedditTV", "Logon", "LogonOK");
						publishProgress("Login to reddit ok");	
					}
				}
				
				redditPosts = redditAPI.getRedditPosts(context, subreddit, category,
						period);
				return null;
			} catch (Throwable t) {
				return t;
			}
		}

		@Override
		protected void onPostExecute(Throwable t) {
			if (t == null && redditPosts!=null) {
				int nrVideos= 0;
				for (Iterator<RedditPost> iterator = redditPosts.iterator(); iterator
						.hasNext();) {
					RedditPost post = iterator.next();
					// only add youtube videos
					if (post.isYoutubeVideo() && post.getYoutubeId()!=null) {
						postsListAdapter.add(post);
						nrVideos++;
						Log.d("RedditTV", "Adding post "+ post);
					}else if (post.isYoutubeVideo() && post.getYoutubeId()==null){
						Log.w("RedditTV", "Could not find youtube id for post "+ post);
					}

				}
				if(nrVideos==0){
					notifyUser("No videos on this page");
				}


				postsListAdapter.notifyDataSetChanged();
				
				if (nrPages>1){
					Log.d("RedditTV", "Page " + nrPages + " loaded" );
					new AddRedditPostsTask(context,subreddit, category, period,false,nrPages-1).execute();
				}else {
					//for googletv force the first item to be selected
					if(listView!=null && doSelectFirst){
						listView.setSelection(0);
						doSelectFirst=false;
					}
				}

			} else {// got exception
				if(t!=null){
					Log.e("RedditTV", "Exception occured",t);
				}
				notifyUser("Could not connect to Reddit\nReddit may be down or you might not be connected to the almighty internet");
			}
			
			// update UI for loading more
			ProgressBar progressBar = (ProgressBar) findViewById(R.id.loading_progress);
			if (progressBar != null) {
				progressBar.setVisibility(View.INVISIBLE);
				updateLoadMoreText();
			}
		}
		
		protected void onProgressUpdate (String ... status){
			notifyUser(status[0]);
		}
	}
	
	/**
	 * Async method of retrieving reddit posts Create new object for each
	 * invocation Uses redditAPI object (must be initialized) Uses
	 * postsListAdapter (must be initialized) by populating with new RedditPosts
	 */
	class SaveRedditPostTask extends AsyncTask<Void, String, Throwable> {
		private Context context;
		RedditPost redditPost;

		public SaveRedditPostTask(Context context,RedditPost redditPost) {
			this.context=context;
			this.redditPost=redditPost;
			Analytics.trackEvent(context, "Save", "Subreddit", redditPost.getSubreddit());
			Analytics.trackEvent(context, "Save", "Thing", redditPost.getId());	
		}


		@Override
		protected void onPreExecute() {
		}

		@Override
		protected Throwable doInBackground(Void... params) {
			try {
				//if we're logged in and the redditPost has a modhash we save
				if(redditAPI.isLoggedIn(context) && redditPost.getModhash()!=null && !"".equals(redditPost.getModhash())){
					publishProgress("Saving reddit post " + redditPost.getId()+" ...");
					try {
						//doSavePost should only return if successful (exception if not)
						redditAPI.doSavePost(context, redditPost);
						publishProgress("Reddit post saved to user " +redditAPI.getLoggedInUser());
					} catch (Throwable e) {
						return e;
					}	
				}else if(!redditAPI.isLoggedIn(context)) {
					return new Exception("Cannot save reddit post since you are not logged in");
				}else if(redditPost.getModhash()==null || "".equals(redditPost.getModhash())) {
					return new Exception("Cannot save reddit post since you were not logged in when post was retrieved.\nClear list and try again");
				}
				return null;
			} catch (Throwable t) {
				return t;
			}
		}

		@Override
		protected void onPostExecute(Throwable t) {
			if (t != null) {
				notifyUser(t.getMessage());
			}
		}
		
		protected void onProgressUpdate (String ... status){
			notifyUser(status[0]);
		}
	}
	
	
	
	/**
	 * Async method of playing the sound of a youtube video in the backgroun
	 */
	class ListenToTask extends AsyncTask<Void, String, Throwable> {
		private Context context;
		RedditPost redditPost;

		public ListenToTask(Context context,RedditPost redditPost, MediaPlayer player) {
			this.context=context;
			this.redditPost=redditPost;
			Analytics.trackEvent(context, "ListenTo", "Subreddit", redditPost.getSubreddit());
			Analytics.trackEvent(context, "ListenTo", "Thing", redditPost.getId());	
		}


		@Override
		protected void onPreExecute() {
		}

		@Override
		protected Throwable doInBackground(Void... params) {
			//publishProgress("Reddit post saved to user " +redditAPI.getLoggedInUser());
			String gDataQueryURL = "http://gdata.youtube.com/feeds/api/videos?&max-results=20&v=2&format=1&q="+redditPost.getYoutubeId();
			String rtspURL=null;
			
			//query gdata
			DefaultHttpClient httpClient = new DefaultHttpClient();
			HttpGet request = new HttpGet(gDataQueryURL);

			String gDataResponse;
			ResponseHandler<String> responseHandler=new BasicResponseHandler();
	        try {
	        	gDataResponse = httpClient.execute(request, responseHandler);
	        }catch (IOException e) {
				Log.w("RedditTV", "Error during getResponseFromUrl ", e);
				return new Throwable("Could not contact gdata at url "+ gDataQueryURL);
			}
			
	        //find rtsp link
	        if(gDataResponse.contains("rtsp:") && gDataResponse.contains(".3gp")){
	        	//TODO: Use proper regexp
	        	try{
	        		rtspURL= gDataResponse.substring(gDataResponse.indexOf("rtsp:"), gDataResponse.indexOf(".3gp")+4);
	        	}catch (RuntimeException e){
	        		Log.w("RedditTV", "Failed to parse RTSP link in GData response", e);
	        		return new Throwable("Failed to parse RTSP link in GData response");
	        	}
	        	
	        }else {
	        	return new Throwable("No streaming RTSP link in GData response");
	        }
			
	        publishProgress("Found rtsp stream.\nPlease wait while buffering");
			//String rtspURL = "rtsp://v3.cache6.c.youtube.com/CiILENy73wIaGQmRyqFJREIcTRMYDSANFEgGUgZ2aWRlb3MM/0/0/0/video.3gp";
			//initialize or reset the mediaplayer
	        if(mediaplayer==null){
				mediaplayer = new MediaPlayer();
			}else {
				try {
					if(mediaplayer.isPlaying()){
						mediaplayer.stop();
					}
					
				}catch (RuntimeException e) {
					Log.w("RedditTV", "Exception during resetting of player",e);
				}
				mediaplayer.reset();

			}
			
	        //play it
			try {
				mediaplayer.setDataSource(rtspURL);
				mediaplayer.prepare();
				mediaplayer.start();
				return null;
			} catch (IllegalArgumentException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return e;
			} catch (IllegalStateException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return e;
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return e;
			}
			
		}

		@Override
		protected void onPostExecute(Throwable t) {
			if (t != null) {
				notifyUser(t.getMessage());
			}
			
			String ns = Context.NOTIFICATION_SERVICE;
			NotificationManager mNotificationManager = (NotificationManager) getSystemService(ns);
			
			int icon = R.drawable.reddittv_logo_small;
			CharSequence tickerText = "Listening to RedditTV";
			long when = System.currentTimeMillis();

			Notification notification = new Notification(icon, tickerText, when);

			CharSequence contentTitle = "Listening to RedditTV audio";
			CharSequence contentText = "Click to stop current song " + redditPost.getTitle();
			Intent notificationIntent = new Intent(context, RedditTV.class);
			
			notificationIntent.putExtra(INTENT_STOP_LISTENTO, "TRUE");
			notificationIntent.setAction(INTENT_STOP_LISTENTO);
			PendingIntent contentIntent = PendingIntent.getActivity(context, 0, notificationIntent, 0);
			

			notification.setLatestEventInfo(context, contentTitle, contentText, contentIntent);

			mNotificationManager.notify(LISTENTO_NOTIFICATION_ID, notification);
			
		}
		
		protected void onProgressUpdate (String ... status){
			notifyUser(status[0]);
		}
	}
	
	private void updateLoadMoreText(){
		if (((ProgressBar) findViewById(R.id.loading_progress)).getVisibility()==View.INVISIBLE){
			((TextView) findViewById(R.id.load_title)).setText(R.string.listLoadMore);
		}else {
			((TextView) findViewById(R.id.load_title)).setText(R.string.listLoadingMore);
		}
		((TextView) findViewById(R.id.load_subreddit)).setText(currentSubreddit);
		((TextView) findViewById(R.id.load_category)).setText(currentCategory);
		if(currentPeriod!=RedditAPI.PERIOD_NULL){
			((TextView) findViewById(R.id.load_period)).setText(currentPeriod);
			((TextView) findViewById(R.id.load_period)).setVisibility(View.VISIBLE);
			((TextView) findViewById(R.id.load_label_period)).setVisibility(View.VISIBLE);
		}else {
			((TextView) findViewById(R.id.load_label_period)).setVisibility(View.INVISIBLE);
			((TextView) findViewById(R.id.load_period)).setVisibility(View.INVISIBLE);
		}
		
		//String loadMoreText = getString(R.string.listLoadMore,currentSubreddit, currentCategory);
		//loadMoreView.setText(loadMoreText);
	}

	private void notifyUser(CharSequence msg) {
		Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
	}


	/**
	 * On long click of list view footer
	 * 
	 */
	@Override
	public boolean onLongClick(View arg0) {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle("Load multiple pages");
		
		final Context c=this;
		builder.setSingleChoiceItems(R.array.multiple_pages,
				-1,
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						int nrPages  = which+1; 
						new AddRedditPostsTask(c,currentSubreddit, currentCategory, currentPeriod,true,nrPages)
						.execute();
						dialog.dismiss();
					}
				}
				);
		Dialog dialog = builder.create();
		dialog.show();
		return true;
	}

	private void setTheme(SharedPreferences sharedPreferences) {
		
		//set the theme
		String theme= sharedPreferences.getString(RedditTVPreferences.KEY_THEME, RedditTVPreferences.DEFAULT_VALUE_THEME);
		if(RedditTVPreferences.THEME_NORMAL.equals(theme)){
			setTheme(R.style.Theme_Normal);
		}else if(RedditTVPreferences.THEME_LARGE.equals(theme)){
			setTheme(R.style.Theme_Large);
		}
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
			String key) {
		
		if(key!=null&& key==RedditTVPreferences.KEY_REDDIT_USERNAME){
			if(redditAPI.isLoggedIn(this)){
				notifyUser("Logging you out as reddit user has changed");
			}
			redditAPI.doDeleteLocalLoginInformation(this);
		}
		
		if(key!=null&& key==RedditTVPreferences.KEY_REDDIT_LOGOUT){
			if(redditAPI.isLoggedIn(this)){
				notifyUser("Forced logout from reddit");
				RedditTVPreferences.deleteStoredRedditSessionCookie(this);
				redditAPI.doDeleteLocalLoginInformation(this);
			}else {
				notifyUser("No forced logout as you are not logged in");
			}
			String value = sharedPreferences.getString(key,null);
		}
		if(key!=null&& key==RedditTVPreferences.KEY_THEME){
			//theme has been changed. We must restart the activity for it to be used
			//(cannot change it dynamically)
			//finish(); 
			themeHasBeenChanged=true;
		}
		
		
	}
}