package com.spuriouslabs.apps.autoplex;

import android.support.design.widget.BaseTransientBottomBar;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.content.SharedPreferences;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ProgressBar;

import com.spuriouslabs.apps.autoplex.plex.utils.MusicLibrary;
import com.spuriouslabs.apps.autoplex.plex.utils.PlexCallback;
import com.spuriouslabs.apps.autoplex.plex.PlexConnector;
import com.spuriouslabs.apps.autoplex.plex.utils.PlexConnectionSet;

import static android.widget.Toast.LENGTH_LONG;

public class MainActivity extends AppCompatActivity
{
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		SharedPreferences settings = getSharedPreferences(this.getString(R.string.settings_name), MODE_PRIVATE);

		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		String token = settings.getString(this.getString(R.string.token_name), null);
		String local_uri = settings.getString("local_server_uri", null);
		String relay_uri = settings.getString("relay_server_uri", null);
		String remote_uri = settings.getString("remote_server_uri", null);
		String music_name = settings.getString("music_library_name", null);
		String preferred_server = settings.getString("preferred_server", null);
		int music_id = settings.getInt("music_library_key", -1);

		if (token != null)
			((EditText)findViewById(R.id.token_field)).setText(token);

		if (local_uri != null)
			((EditText)findViewById(R.id.local_server_textbox)).setText(local_uri);

		if (relay_uri != null)
			((EditText)findViewById(R.id.relay_server_textbox)).setText(relay_uri);

		if (remote_uri != null)
			((EditText)findViewById(R.id.remote_server_textbox)).setText(remote_uri);

		if (music_name != null)
			((EditText)findViewById(R.id.music_library_name_textbox)).setText(music_name);

		if (music_id != -1)
			((EditText)findViewById(R.id.music_library_id_textbox)).setText(Integer.toString(music_id));

		if (preferred_server != null) {
			switch (preferred_server) {
				case "local":
					((CheckBox)findViewById(R.id.use_local_checkbox)).setChecked(true);
					break;
				case "relay":
					((CheckBox)findViewById(R.id.use_relay_checkbox)).setChecked(true);
					break;
				case "remote":
					((CheckBox)findViewById(R.id.use_remote_checkbox)).setChecked(true);
					break;
			}
		}
	}

	public void discoverPlexServers(View view) {
		EditText token_field = (EditText) findViewById(R.id.token_field);
		SharedPreferences settings = getSharedPreferences(this.getString(R.string.settings_name), MODE_PRIVATE);
		settings.edit().putString(
				this.getString(R.string.token_name), token_field.getText().toString()
		).apply();

		showProgressBar();

		PlexConnector plex = PlexConnector.getInstance(this);
		plex.discoverPlexConnections(new PlexCallback<PlexConnectionSet>() {
			@Override
			public void callback(PlexConnectionSet param) {
				hideProgressBar();
				EditText local_server_field = (EditText)findViewById(R.id.local_server_textbox);
				EditText remote_server_field = (EditText)findViewById(R.id.remote_server_textbox);
				EditText relay_server_field = (EditText)findViewById(R.id.relay_server_textbox);

				local_server_field.setText(param.getLocalUri());
				remote_server_field.setText(param.getRemoteUri());
				relay_server_field.setText(param.getRelayUri());
			}
		});
	}

	private void discoverMusicLibraryId(View view)
	{
		PlexConnector plex = PlexConnector.getInstance(this);
		SharedPreferences settings = getSharedPreferences(this.getString(R.string.settings_name), MODE_PRIVATE);
		String uri = plex.getPreferredUri();

		showProgressBar();

		plex.discoverMusicLibraryKey(uri, new PlexCallback<MusicLibrary>() {
			@Override
			public void callback(MusicLibrary param) {
				hideProgressBar();
				((EditText)findViewById(R.id.music_library_name_textbox)).setText(param.getName());
				((EditText)findViewById(R.id.music_library_id_textbox)).setText(Integer.toString(param.getId()));
			}
		});
	}

	public void setPreferredServer(View view) {
		Snackbar warn = Snackbar.make(view, R.string.relay_warning, 10000);
		SharedPreferences settings = getSharedPreferences(this.getString(R.string.settings_name), MODE_PRIVATE);
		String setting_name = "preferred_server";
		CheckBox local = (CheckBox) findViewById(R.id.use_local_checkbox);
		CheckBox relay = (CheckBox) findViewById(R.id.use_relay_checkbox);
		CheckBox remote = (CheckBox) findViewById(R.id.use_remote_checkbox);

		if (view.getId() == R.id.use_local_checkbox) {
			relay.setChecked(false);
			remote.setChecked(false);
			settings.edit().putString(setting_name, "local").apply();
		} else if (view.getId() == R.id.use_relay_checkbox) {
			warn.show();
			local.setChecked(false);
			remote.setChecked(false);
			settings.edit().putString(setting_name, "relay").apply();
		} else if (view.getId() == R.id.use_remote_checkbox) {
			local.setChecked(false);
			relay.setChecked(false);
			settings.edit().putString(setting_name, "remote").apply();
		}

		discoverMusicLibraryId(view);
	}

	public void fetchLibraryData(View view)
	{
		PlexConnector.getInstance(this).prefetchMenuItems();
	}

	private void disableForm()
	{
		((Button)findViewById(R.id.discover_server_button)).setEnabled(false);
		((EditText)findViewById(R.id.token_field)).setEnabled(false);
		((EditText)findViewById(R.id.local_server_textbox)).setEnabled(false);
		((EditText)findViewById(R.id.remote_server_textbox)).setEnabled(false);
		((EditText)findViewById(R.id.relay_server_textbox)).setEnabled(false);
		((CheckBox)findViewById(R.id.use_local_checkbox)).setEnabled(false);
		((CheckBox)findViewById(R.id.use_relay_checkbox)).setEnabled(false);
		((CheckBox)findViewById(R.id.use_remote_checkbox)).setEnabled(false);
		((EditText)findViewById(R.id.music_library_id_textbox)).setEnabled(false);
		((EditText)findViewById(R.id.music_library_name_textbox)).setEnabled(false);
	}

	private void enableForm()
	{
		((Button)findViewById(R.id.discover_server_button)).setEnabled(true);
		((EditText)findViewById(R.id.token_field)).setEnabled(true);
		((EditText)findViewById(R.id.local_server_textbox)).setEnabled(true);
		((EditText)findViewById(R.id.remote_server_textbox)).setEnabled(true);
		((EditText)findViewById(R.id.relay_server_textbox)).setEnabled(true);
		((CheckBox)findViewById(R.id.use_local_checkbox)).setEnabled(true);
		((CheckBox)findViewById(R.id.use_relay_checkbox)).setEnabled(true);
		((CheckBox)findViewById(R.id.use_remote_checkbox)).setEnabled(true);
		((EditText)findViewById(R.id.music_library_id_textbox)).setEnabled(true);
		((EditText)findViewById(R.id.music_library_name_textbox)).setEnabled(true);
	}

	private void showProgressBar()
	{
		disableForm();
		((ProgressBar)findViewById(R.id.progressBar)).setVisibility(View.VISIBLE);
	}

	private void hideProgressBar() {
		enableForm();
		((ProgressBar) findViewById(R.id.progressBar)).setVisibility(View.INVISIBLE);
	};
}
