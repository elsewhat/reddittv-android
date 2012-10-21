package org.elsewhat.reddit;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.StringTokenizer;

import android.net.Uri;
import android.util.Log;

public class RedditPost {

	private String id;
	private String title;
	private String subreddit;
	private String url;
	private String permalink;
	private String thumbnailUrl;
	private int ups;
	private int downs;
	private int numComments;
	private String modhash;
	// private String postedDate;
	// private String postedBy;


	public RedditPost(String id,String modhash,String title, String subreddit, String url,
			String permalink, String thumbnailUrl, int ups, int downs,
			int numComments) {
		super();
		this.id=id;
		this.modhash=modhash;
		this.title = title;
		this.subreddit = subreddit;
		this.url = url;
		this.permalink = permalink;
		this.thumbnailUrl = thumbnailUrl;
		this.ups = ups;
		this.downs = downs;
		this.numComments = numComments;
	}

	/**
	 * @return the id
	 */
	public String getId() {
		return "t3_"+id;
	}

	/**
	 * @param id the id to set
	 */
	public void setId(String id) {
		this.id = id;
	}

	public boolean isYoutubeVideo() {
		if(url==null){
			return false;
		}
		try {
			URL urlLink = new URL(url);
			if(urlLink.getHost().endsWith("youtube.com") ){
				//we currently only support links with v=
				if (url.contains("v=")){
					return true;
				}else {
					return false;
				}
			}else if(urlLink.getHost().endsWith("youtu.be") ){
				return true;
			}else {
				return false;
			}
			
		}catch (MalformedURLException e) {
			return false;
		}

	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getSubreddit() {
		return subreddit;
	}

	public void setSubreddit(String subreddit) {
		this.subreddit = subreddit;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public String getPermalink() {
		return permalink;
	}

	public void setPermalink(String permalink) {
		this.permalink = permalink;
	}

	public String getCommentsURL (){
		return "http://www.reddit.com"+ permalink;
	}
	public int getUps() {
		return ups;
	}

	public void setUps(int ups) {
		this.ups = ups;
	}

	public int getDowns() {
		return downs;
	}

	public void setDowns(int downs) {
		this.downs = downs;
	}

	public int getNumComments() {
		return numComments;
	}

	public void setNum_comments(int numComments) {
		this.numComments = numComments;
	}

	public String getYoutubeId() {
		if (isYoutubeVideo()) {
			
			try {
				//first convert &amp; to &
				String normalizedUrl  = url.replace("&amp;", "&");
				
				//first option. Try from Uri
				Uri uriLink = Uri.parse(normalizedUrl);
				String youtubeId=null;
				youtubeId=uriLink.getQueryParameter("v");
				
				if(youtubeId!=null){
					return youtubeId;
				}
				
				
				
				URL urlLink = new URL(normalizedUrl);
				if(urlLink.getHost().endsWith("youtube.com") ){
					String query = urlLink.getQuery();
					StringTokenizer queryTokenizer = new StringTokenizer(query, "&", false);
					while (queryTokenizer.hasMoreElements()) 
					{   // First Pass to retrive the "parametername=value" combo

					    String paramValueToken = queryTokenizer.nextToken();
					    if(paramValueToken!=null & paramValueToken.startsWith("v")){
					    	StringTokenizer vParamTokenizer = new StringTokenizer(paramValueToken, "=", false );
					    	if(vParamTokenizer.countTokens()==2){
					    		vParamTokenizer.nextToken();
					    		youtubeId= vParamTokenizer.nextToken();
					    	}
					    }
					}
					
					return youtubeId;
				}else if (urlLink.getHost().endsWith("youtu.be")){
					String path= urlLink.getPath();
					if(path!=null && path.length()>0){
						//remove starting /
						path=path.substring(1);
					}
					return path;					
				}else {
					Log.d("RedditTV", url + " Is not a known youtube host. Cannot find youtubeid");
					return null;
				}
				
			}catch (MalformedURLException e) {
				Log.w("RedditTV", "could not retrieve youtube id from url"+ url,e); 
				return null;
			}catch (RuntimeException re) {
				Log.w("RedditTV", "could not retrieve youtube id from url"+ url,re); 
				return null;
			}
			
		} else {
			return null;
		}
	}
	

	public void setThumbnailUrl(String thumbnailUrl) {
		this.thumbnailUrl = thumbnailUrl;
	}

	public String getThumbnailUrl() {
		return thumbnailUrl;
	}

	public static RedditPost getSingleTestData() {
		return new RedditPost(
				"gyaeo",
				"",
				"You're in Buenos Aires, your flight is delayed and the airport staff are having difficulty calming the passengers. What do you do? Oh yeah...small detail...you're Cyndi Lauper. This happened two days ago.",
				"videos",
				"http://www.youtube.com/watch?v=4PrBnG9E4I4",
				"http://www.reddit.com/r/videos/comments/fys0w/youre_in_buenos_aires_your_flight_is_delayed_and/",
				"http://img.youtube.com/vi/4PrBnG9E4I4/default.jpg", 400, 100,
				50);

	}
	
	/**
	 * @return the modhash
	 */
	public String getModhash() {
		return modhash;
	}

	/**
	 * @param modhash the modhash to set
	 */
	public void setModhash(String modhash) {
		this.modhash = modhash;
	}

	public static ArrayList<RedditPost> getTestData (int nr){
		ArrayList<RedditPost> posts = new ArrayList<RedditPost>(nr);
		for (int i = 0; i < nr; i++) {
			posts.add(RedditPost.getSingleTestData());
		}
		return posts;
	}
	
	public String toString(){
		return title + " - " + url + " - "+ getYoutubeId();
	}

}
