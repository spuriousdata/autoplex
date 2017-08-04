package com.spuriouslabs.apps.autoplex;

import android.content.Context;
import android.media.browse.MediaBrowser.MediaItem;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;

import com.spuriouslabs.apps.autoplex.plex.PlexConnector;
import com.spuriouslabs.apps.autoplex.plex.PlexMusicProvider;


public class MetadataCacheActivity extends AppCompatActivity
{
	private PlexMusicProvider provider;

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_metadata_cache);

		provider = PlexMusicProvider.get_instance(PlexConnector.getInstance(this));

		showProgressBar();
		provider.retrieveMediaAsync(getString(R.string.all_artists_menu_key), new PlexMusicProvider.Callback()
		{
			@Override
			public void onMusicCatalogReady(boolean success)
			{
				if (success)
					fill_artist_list();
			}
		});
	}

	private void fill_artist_list()
	{
		Context c = getApplicationContext();
		LinearLayout layout = (LinearLayout)findViewById(R.id.artists_scroll_layout);
		for (MediaItem item : provider.getMenu(getString(R.string.all_artists_menu_key))) {
			CheckBox cb = new CheckBox(c);
			cb.setText(item.getDescription().getTitle());
			layout.addView(cb);
		}
		hideProgressBar();
		layout.invalidate();
	}

	private void enableForm(boolean enable)
	{
		((ScrollView)findViewById(R.id.artists_scroll_view)).setEnabled(enable);
	}

	private void showProgressBar()
	{
		enableForm(false);
		((ProgressBar)findViewById(R.id.menu_cache_progress_bar)).setVisibility(View.VISIBLE);
	}

	private void hideProgressBar() {
		enableForm(true);
		((ProgressBar) findViewById(R.id.menu_cache_progress_bar)).setVisibility(View.INVISIBLE);
	};
}
