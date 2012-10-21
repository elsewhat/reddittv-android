package org.elsewhat.reddit;

import java.util.ArrayList;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

public class RedditPostAdapter extends ArrayAdapter<RedditPost> {
	private ArrayList<RedditPost> redditPosts;

	// asynchronous download of images
	private DrawableManager drawableManager;

	public RedditPostAdapter(Context context, int textViewResourceId,
			ArrayList<RedditPost> redditPosts) {
		super(context, textViewResourceId, redditPosts);

		this.redditPosts = redditPosts;
		drawableManager = new DrawableManager();
	}

	/**
	 * Populate a row in the ListView with data from the RedditPost of that
	 * position
	 * 
	 */
	public View getView(int position, View convertView, ViewGroup parent) {

		View videoListRow = convertView;
		if (videoListRow == null) {
			Context context = parent.getContext();
			LayoutInflater viewInflater = (LayoutInflater) context
					.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			videoListRow = viewInflater.inflate(R.layout.video_list_row, null);
		}
		RedditPost redditPost = redditPosts.get(position);
		if (redditPost != null) {
			TextView tvTitle = (TextView) videoListRow
					.findViewById(R.id.video_title);
			tvTitle.setText(redditPost.getTitle());

			TextView tvMetrics = (TextView) videoListRow
					.findViewById(R.id.video_metrics);
			tvMetrics.setText( redditPost.getSubreddit());

			TextView tvRecent = (TextView) videoListRow
					.findViewById(R.id.video_recent);
			tvRecent.setText(redditPost.getNumComments()+ " comments");

			TextView tvDetails = (TextView) videoListRow
					.findViewById(R.id.video_detail);
			
			tvDetails.setText( redditPost.getUps() + " up "
					+ redditPost.getDowns() + " down");

			ImageView thumbnailView = (ImageView) videoListRow
					.findViewById(R.id.video_thumbnail);
			drawableManager.fetchDrawableOnThread(redditPost.getThumbnailUrl(),
					thumbnailView);
		}
		return videoListRow;

	}

}
