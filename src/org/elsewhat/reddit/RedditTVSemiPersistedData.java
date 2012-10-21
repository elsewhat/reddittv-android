package org.elsewhat.reddit;

public class RedditTVSemiPersistedData {
	private RedditPostAdapter postsListAdapter;
	private RedditAPI redditAPI;
	private String currentSubreddit = null;
	private String currentCategory = null;
	private String currentPeriod = null;
	
	/**
	 * @return the postsListAdapter
	 */
	public RedditPostAdapter getPostsListAdapter() {
		return postsListAdapter;
	}

	/**
	 * @return the redditAPI
	 */
	public RedditAPI getRedditAPI() {
		return redditAPI;
	}

	public RedditTVSemiPersistedData(RedditPostAdapter postsListAdapter,RedditAPI redditAPI,String currentSubreddit, String currentCategory,String currentPeriod){
		this.redditAPI=redditAPI;
		this.postsListAdapter=postsListAdapter;
		this.currentSubreddit=currentSubreddit;
		this.currentCategory=currentCategory;
		this.currentPeriod=currentPeriod;
	}

	/**
	 * @return the currentSubreddit
	 */
	public String getCurrentSubreddit() {
		return currentSubreddit;
	}

	/**
	 * @return the currentCategory
	 */
	public String getCurrentCategory() {
		return currentCategory;
	}

	/**
	 * @return the currentPeriod
	 */
	public String getCurrentPeriod() {
		return currentPeriod;
	}
	
	
}
