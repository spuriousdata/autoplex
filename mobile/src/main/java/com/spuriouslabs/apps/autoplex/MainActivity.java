package com.spuriouslabs.apps.autoplex;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.content.SharedPreferences;
import android.view.View;
import android.widget.EditText;

import com.spuriouslabs.apps.autoplex.plex.utils.PlexCallback;
import com.spuriouslabs.apps.autoplex.plex.PlexConnector;
import com.spuriouslabs.apps.autoplex.plex.utils.PlexConnectionSet;

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

		if (token != null)
			((EditText)findViewById(R.id.token_field)).setText(token);

		if (local_uri != null)
			((EditText)findViewById(R.id.local_server_textbox)).setText(local_uri);

		if (relay_uri != null)
			((EditText)findViewById(R.id.relay_server_textbox)).setText(relay_uri);

		if (remote_uri != null)
			((EditText)findViewById(R.id.remote_server_textbox)).setText(remote_uri);
	}

	public void discoverPlexServers(View view) {
		EditText token_field = (EditText) findViewById(R.id.token_field);
		SharedPreferences settings = getSharedPreferences(this.getString(R.string.settings_name), MODE_PRIVATE);
		settings.edit().putString(
				this.getString(R.string.token_name), token_field.getText().toString()
		).apply();

		PlexConnector plex = PlexConnector.getInstance(this);
		plex.discoverPlexConnections(new PlexCallback<PlexConnectionSet>() {
			@Override
			public void callback(PlexConnectionSet param) {
				EditText local_server_field = (EditText)findViewById(R.id.local_server_textbox);
				EditText remote_server_field = (EditText)findViewById(R.id.remote_server_textbox);
				EditText relay_server_field = (EditText)findViewById(R.id.relay_server_textbox);

				local_server_field.setText(param.getLocalUri());
				remote_server_field.setText(param.getRemoteUri());
				relay_server_field.setText(param.getRelayUri());
			}
		});
	}
}
