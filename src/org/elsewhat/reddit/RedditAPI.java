package org.elsewhat.reddit;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Hashtable;
import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.HttpException;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.CookieStore;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.CoreProtocolPNames;
import org.apache.http.protocol.HTTP;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.SharedPreferences;
import android.provider.Settings.Secure;
import android.util.Log;


public class RedditAPI {
	private Hashtable<String, String> afterBySubReddit;
	public static final String CATEGORY_HOT = "hot";
	public static final String CATEGORY_NEW = "new";
	public static final String CATEGORY_TOP = "top";
	public static final String CATEGORY_CONTROVERSIAL = "controversial";

	public static final String PERIOD_HOUR = "hour";
	public static final String PERIOD_DAY = "day";
	public static final String PERIOD_WEEK = "week";
	public static final String PERIOD_MONTH = "month";
	public static final String PERIOD_YEAR = "year";
	public static final String PERIOD_ALLTIME = "all";
	public static final String PERIOD_NULL = null;
	
	private String loggedInUser = null;
	
	private Cookie redditSessionCookie=null;

	public RedditAPI() {
		afterBySubReddit = new Hashtable<String, String>(10);

	}
	
	public boolean isLoggedIn(Context context){
		Cookie cookie= getRedditSessionCookie(context);
		if(cookie!=null){
			return true;
		}else {
			return false;
		}
	}
	
	public boolean canLogIn(Context context){
		SharedPreferences settings =context.getSharedPreferences(RedditTVPreferences.PREFS_NAME, Context.MODE_WORLD_READABLE);
		//reddit usrename
		String redditUser= settings.getString(RedditTVPreferences.KEY_REDDIT_USERNAME, null);
		String redditPassword= settings.getString(RedditTVPreferences.KEY_REDDIT_PASSWORD, null);
		if(redditUser !=null && !"".equals(redditUser) && redditPassword!=null){
			return true;
		}else {
			return false;
		}
	}
	
	public String getLoggedInUser(){
		return loggedInUser;
	}
	
	public String getPersistedLoggedInUser(Context context){
		String persistedLoggedInUser= RedditTVPreferences.getStoredRedditSessionUsername(context);	
		return persistedLoggedInUser;
	}
	
	public void doClearAfterHistory(){
		afterBySubReddit= new Hashtable<String, String>(10);
	}
	
	public void doDeleteLocalLoginInformation(Context c){
		Log.i("RedditTV", "Deleting session cookie:" + redditSessionCookie);
		redditSessionCookie=null;	
		loggedInUser=null;
	}
	
	public void doDeleteAllLoginInformation(Context c){
		Log.i("RedditTV", "Deleting session cookie:" + redditSessionCookie);
		redditSessionCookie=null;	
		loggedInUser=null;
		RedditTVPreferences.deleteStoredRedditSessionCookie(c);
	}
	
	
	
	private Cookie getRedditSessionCookie(Context context){
		if(redditSessionCookie == null){
			//get session cookie from preferences
			redditSessionCookie= RedditTVPreferences.getStoredRedditSessionCookie(context);	
			loggedInUser=getPersistedLoggedInUser(context);
		}
		
		if(redditSessionCookie!=null){
			if (redditSessionCookie.getExpiryDate()!= null && redditSessionCookie.getExpiryDate().after(new Date()) ){
				Log.i("RedditTV", "Session cookie has expired. Deleting it");
				redditSessionCookie=null;
			}	
		}
		return redditSessionCookie;
	}

	public ArrayList<RedditPost> getRedditPosts(Context context,String subreddit,String category, String period){
		
		//after is a reddit paging mechanism
		String afterId= generateKeyForAfter(subreddit,category,period);
		String afterValue=null;
		if(afterBySubReddit.containsKey(afterId)){
			afterValue = afterBySubReddit.get(afterId);
		}
		
		String redditUrl = generateRedditUrl(subreddit,category,period, afterValue);
		InputStream inputStream=null;
		try {
            String strJSON= getResponseFromUrl(context,redditUrl);
            
            // A Simple JSONObject Creation
            JSONObject jsonRoot=new JSONObject(strJSON);
            //Log.d("RedditTV","jsonobject\n"+jsonRoot.toString());

            //get outer data element 
            JSONObject jsonListingData = jsonRoot.getJSONObject("data");
            String after = jsonListingData.optString("after");
            afterBySubReddit.put(afterId, after);
            
            //modhash only exist if logged in
            String modhash =jsonListingData.optString("modhash");
            //get the children array from the data element
            JSONArray jsonListingDataChildren = jsonListingData.getJSONArray("children");
            
            
            ArrayList<RedditPost> posts = new ArrayList<RedditPost>(25);
            
            //loop through the children array (each one is a reddit post)
            for (int i = 0; i < jsonListingDataChildren.length(); i++) {
				JSONObject jsonRedditPost=jsonListingDataChildren.getJSONObject(i);
				//each reddit post has a data element
				JSONObject jsonRedditPostData = jsonRedditPost.getJSONObject("data");
				
				RedditPost post = new RedditPost(
						jsonRedditPostData.optString("id"),
						modhash,
						jsonRedditPostData.optString("title"),
						jsonRedditPostData.optString("subreddit"),
						jsonRedditPostData.optString("url"),
						jsonRedditPostData.optString("permalink"),
						jsonRedditPostData.optString("thumbnail"),
						jsonRedditPostData.optInt("ups"),
						jsonRedditPostData.optInt("downs"),
						jsonRedditPostData.optInt("num_comments"));
				
				//If reddit has no thumbnail it gives it the value /static/noimage.png
				//we replace it with the youtube thumbnail if it is a youtube movei
				if(post.isYoutubeVideo() && post.getThumbnailUrl()!=null && "/static/noimage.png".equalsIgnoreCase(post.getThumbnailUrl()) ){
					String youtubeId= post.getYoutubeId();
					post.setThumbnailUrl("http://img.youtube.com/vi/"+youtubeId+"/default.jpg");
					
				}
				
				posts.add(post);
				
				
				//Log.d("RedditTV", "New post " +post);
			}

				
            return posts;
		
		}catch (MalformedURLException e) {
			//TODO: Return userfriendly error
			Log.e("RedditTV", "URL not valid" + redditUrl,e);
			return null;
		}catch (IOException ioE){
			Log.e("RedditTV", "IOException for url " + redditUrl,ioE);
			return null;
		} catch (JSONException jsonE) {
			Log.e("RedditTV", "JSONException for url " + redditUrl,jsonE);
			jsonE.printStackTrace();
			return null;
		} finally {
           try {
        	   if(inputStream!=null)
        		   inputStream.close();
           }catch (IOException ioE2){}
		}
	}

	private String generateKeyForAfter(String subreddit, String category,
			String period) {
		return subreddit + category + period;
	}

	private String generateRedditUrl(String subreddit, String category,
			String period, String after) {
		if (category == null)
			category = CATEGORY_HOT;

		if (subreddit == null)
			subreddit = "videos";

		String redditCategoryParam = "/" + category;

		String redditPeriodParam = "";

		if (period != PERIOD_NULL) {
			redditPeriodParam = "&t=" + period;
		}

		String afterParam = "";
		if (after != null) {
			afterParam = "&after="+after;
		}

		String redditUrl = "";
		// special handling for frontpage and new
		if (subreddit.equals("frontpage")) {
			redditUrl = "http://www.reddit.com" + redditCategoryParam
					+ ".json?count=50" + redditPeriodParam + afterParam;
		} else if (subreddit.equals("new")) {
			redditUrl = "http://www.reddit.com/new.json?count=50" + afterParam;
		} else if (subreddit.equals("saved")) {
			redditUrl = "http://www.reddit.com/saved.json?count=50" + afterParam;
		}else {
			redditUrl = "http://www.reddit.com/r/" + subreddit
					+ redditCategoryParam + ".json?count=50"
					+ redditPeriodParam + afterParam;

		}
		return redditUrl;
	}

	private String getResponseFromUrl(Context c,String urlString)
			throws MalformedURLException, IOException {
		DefaultHttpClient httpClient = new DefaultHttpClient();
		HttpGet request = new HttpGet(urlString);
		
		
		if(isLoggedIn(c)){
			CookieStore cookieStore = httpClient.getCookieStore();
			cookieStore.addCookie(redditSessionCookie);
			httpClient.getParams().setParameter(CoreProtocolPNames.USER_AGENT, "org.elsewhat.reddit narwhal TV/1.4 "+loggedInUser);
		}else {
			loggedInUser=null;
			String androidId = Secure.getString(c.getContentResolver(),
                    Secure.ANDROID_ID); 
			httpClient.getParams().setParameter(CoreProtocolPNames.USER_AGENT, "org.elsewhat.reddit narwhal TV/1.4 "+androidId);
		}
		
		ResponseHandler<String> responseHandler=new BasicResponseHandler();
        try {
        	String responseBody = httpClient.execute(request, responseHandler);
        	return responseBody;
        }catch (IOException e) {
			Log.w("RedditTV", "Error during getResponseFromUrl ", e);
			return null;
		}

	}
	
    /**
     * 
     */
    public boolean doLogin(Context context)throws HttpException {
		String status = "";
    	//String userError = context.getResources().getString(R.string.msgLoginFailed);
    	HttpEntity entity = null;
    	
    	
		SharedPreferences settings =context.getSharedPreferences(RedditTVPreferences.PREFS_NAME, Context.MODE_WORLD_READABLE);
		//reddit usrename
		String username= settings.getString(RedditTVPreferences.KEY_REDDIT_USERNAME, null);
		String password= settings.getString(RedditTVPreferences.KEY_REDDIT_PASSWORD, null);
		//assume we're not logged in now
		loggedInUser=null;
    	
    	try {
    		//parmeters to reddit
    		List<NameValuePair> postParameters = new ArrayList<NameValuePair>();
    		postParameters.add(new BasicNameValuePair("user", username.toString()));
    		postParameters.add(new BasicNameValuePair("passwd", password.toString()));
    		postParameters.add(new BasicNameValuePair("api_type", "json"));
    		
            HttpPost httppost = new HttpPost("http://www.reddit.com/api/login/");
            httppost.setEntity(new UrlEncodedFormEntity(postParameters, HTTP.UTF_8));
                 
            DefaultHttpClient httpClient = new DefaultHttpClient();
        	HttpResponse response = httpClient.execute(httppost);
        	status = response.getStatusLine().toString();
        	if (!status.contains("200")) {
        		throw new HttpException(status);
        	}
        	
        	entity = response.getEntity();
        	
        	BufferedReader in = new BufferedReader(new InputStreamReader(entity.getContent()));
        	//we get just one line in return
        	String strResponse = in.readLine();
        	Log.d("RedditTV", "Login returned " + strResponse);
        	in.close();
        	entity.consumeContent();
        	
        	if (strResponse == null || "".equals(strResponse)) {
        		throw new HttpException("No content returned from login POST");
        	}
        	
        	
        	if (httpClient.getCookieStore().getCookies().isEmpty())
        		throw new HttpException("Failed to login: No cookies");
        	
        	//final JsonFactory jsonFactory = new JsonFactory();
        	//final JsonParser jp = jsonFactory.createJsonParser(strResponse);
        	
        	//JSONObject jsonRoot=new JSONObject(strResponse);
        	//JSONObject jsonJson = jsonRoot.getJSONObject("json");
        	//JSONObject jsonData = jsonJson.getJSONObject("data");
        		
        	//String userModhash=jsonData.optString("modhash");
        	/*
        	// Go to the errors
        	while (jp.nextToken() != JsonToken.FIELD_NAME || !Constants.JSON_ERRORS.equals(jp.getCurrentName()))
        		;
        	if (jp.nextToken() != JsonToken.START_ARRAY)
        		throw new IllegalStateException("Login: expecting errors START_ARRAY");
        	if (jp.nextToken() != JsonToken.END_ARRAY) {
	        	if (line.contains("WRONG_PASSWORD")) {
	        		userError = "Bad password.";
	        		throw new Exception("Wrong password");
	        	} else {
	        		// Could parse for error code and error description but using whole line is easier.
	        		throw new Exception(line);
	        	}
        	}
        	*/

        	// Getting here means you successfully logged in.
        	// Congratulations!
        	// You are a true reddit master!
        	
        	// Get modhash
        	//while (jp.nextToken() != JsonToken.FIELD_NAME || !Constants.JSON_MODHASH.equals(jp.getCurrentName()))
        		;
        	//jp.nextToken();
        	//settings.setModhash(jp.getText());

        	// Could grab cookie from JSON too, but it lacks expiration date and stuff. So grab from HttpClient.
			List<Cookie> cookies = httpClient.getCookieStore().getCookies();
        	for (Cookie c : cookies) {
        		if (c.getName().equals("reddit_session")) {
        			redditSessionCookie = c;
        			RedditTVPreferences.setStoredRedditSessionCookie(context, redditSessionCookie,username);
        			loggedInUser=username;
        			return true;
        		}
        	}
        	
        	return false;

    	} catch (Exception e) {
    		if (entity != null) {
    			try {
    				entity.consumeContent();
    			} catch (Exception e2) {
    			}
    		}
        }
        return false;
    }
    
    public void doSavePost(Context c, RedditPost redditPost) throws Exception{
    	String redditSaveUrl = "http://www.reddit.com/api/save";
		//redditSaveUrl = "http://www.reddit.com/api/unsave";
		List<NameValuePair> postParameters = new ArrayList<NameValuePair>();
		postParameters.add(new BasicNameValuePair("id", redditPost.getId()));
		postParameters.add(new BasicNameValuePair("uh", redditPost.getModhash()));
		Log.i("RedditTV", "Saving reddit post " + redditPost);
		HttpEntity entity=null;
		
			HttpPost request = new HttpPost(redditSaveUrl);
			request.setHeader("Content-Type", "application/x-www-form-urlencoded");
			try {
				request.setEntity(new UrlEncodedFormEntity(postParameters, HTTP.UTF_8));

			//TODO: Gzip
			//request.setHeader("Accept-Encoding", "gzip");
			
			DefaultHttpClient httpClient = new DefaultHttpClient();	
			
			if(isLoggedIn(c)){
				CookieStore cookieStore = httpClient.getCookieStore();
				cookieStore.addCookie(redditSessionCookie);
			}else {
				throw new Exception("Cannot save reddit post as no user is logged in");
			}
			
			
			HttpResponse response;
				response = httpClient.execute(request);
	
	    	String httpStatus = response.getStatusLine().toString();
	    	
	    	if (!httpStatus.contains("200")) {
	    		if(httpStatus.contains("502")){
	    			throw new HttpException("Save to reddit most likely worked.\n(cryptic HTTP 502 response received)");
	    		}else {
	    			throw new HttpException("Save to reddit failed with response http status "+httpStatus );
	    		}
	    		
	    	}
	    	
	    	entity = response.getEntity();
	
	    	BufferedReader in = new BufferedReader(new InputStreamReader(entity.getContent()));
	    	String line = in.readLine();
	    	Log.i("RedditTV", "Save returned " + line);
	    	in.close();

	    	if (line.contains("USER_REQUIRED")) {
	    		doDeleteAllLoginInformation(c);
	    		throw new HttpException("Save to reddit failed as it doesn't believe you are logged in while RedditTV does!\nReload the content to try login again");
	    		
	    	}
	    	if (line.contains("RATELIMIT")) {
	    		throw new HttpException("Save to reddit failed because ratelimit has been reached. Mostly likely my fault for spamming reddit. Wait and try again");
	    	}
	    	
	    	entity.consumeContent();
	    	return;

			} catch (ClientProtocolException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}finally {
				if (entity != null) {
					try {
						entity.consumeContent();
					} catch (Exception e2) {}
				}
			}
	

    }
		
}