package org.elsewhat.reddit;

import java.util.ArrayList;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;


public class RedditPostAdapterWithLoadingRow extends ArrayAdapter<RedditPost> {
	private ArrayList<RedditPost> redditPosts;
	
	//asynchronous download of images
	private DrawableManager drawableManager;
	private Context context;
	
	public RedditPostAdapterWithLoadingRow(Context context, int textViewResourceId,
			ArrayList<RedditPost> redditPosts) {
		super(context, textViewResourceId, redditPosts);
		
		this.redditPosts= redditPosts;
		this.context=context;
		drawableManager=new DrawableManager();
	}

	/**
	 * We have at least two types of views in the adapter
	 */
	public int getViewTypeCount() {
		return super.getViewTypeCount()+1;
	}
	
	public int getCount(){
		return super.getCount()+1;
	}

	public int getItemViewType(int position) {
		if (position==super.getCount()) {
			return(IGNORE_ITEM_VIEW_TYPE);
		}

		return(super.getItemViewType(position));
	}

	
	/**
	 * Populate a row in the ListView with data from the RedditPost of that position
	 * 
	 */
    public View getView(int position, View convertView, ViewGroup parent) {
    		//last row will always be a load more row
    	 	if (position==super.getCount()){
    	 		return getLoadMoreView(parent);
    	 		
    	 	}else {
	    		try {
	    	 		View videoListRow = convertView;
		            if (videoListRow == null) {
		            	Context context= parent.getContext();
		                LayoutInflater viewInflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		                videoListRow = viewInflater.inflate(R.layout.video_list_row, null);
		            }
		            RedditPost redditPost = redditPosts.get(position);
		            if (redditPost != null) {
		                    TextView tvTitle = (TextView) videoListRow.findViewById(R.id.video_title);
		                    tvTitle.setText(redditPost.getTitle());
		                    
		                    TextView tvMetrics = (TextView) videoListRow.findViewById(R.id.video_metrics);
		                    tvMetrics.setText( "ups:" + redditPost.getUps() + " downs:" +redditPost.getDowns()+ " comments:"+ redditPost.getNumComments());
		                    
		                    TextView tvRecent = (TextView) videoListRow.findViewById(R.id.video_recent);
		                    tvRecent.setText(redditPost.getSubreddit());
		                    
		                    TextView tvDetails = (TextView) videoListRow.findViewById(R.id.video_detail);
		                    tvDetails.setText(redditPost.getYoutubeId());
		                    
		                    ImageView thumbnailView = (ImageView) videoListRow.findViewById(R.id.video_thumbnail);
		                    drawableManager.fetchDrawableOnThread(redditPost.getThumbnailUrl(), thumbnailView);
		            }
		            return videoListRow;
	    		}catch(RuntimeException e){
	            	e.printStackTrace();
	            	return null;
	            }
	            
    	 	}
    }

	private View getLoadMoreView(ViewGroup parent) {
		LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		return inflater.inflate(R.layout.video_list_row_loadmore, parent, false);
	}	
}
