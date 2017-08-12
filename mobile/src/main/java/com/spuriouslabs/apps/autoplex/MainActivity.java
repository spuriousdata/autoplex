package com.spuriouslabs.apps.autoplex;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.EditText;

import com.spuriouslabs.apps.autoplex.plex.PlexConnector;
import com.spuriouslabs.apps.autoplex.plex.utils.PlexCallback;

public class MainActivity extends AppCompatActivity
{
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
	}

	public void onLogin(View view)
	{
		String username = ((EditText)findViewById(R.id.plex_username)).getText().toString();
		String password = ((EditText)findViewById(R.id.plex_password)).getText().toString();

		PlexConnector connector = PlexConnector.getInstance(this);

		final MainActivity m = this;

		connector.login(username, password, new PlexCallback<String>()
		{
			@Override
			public void callback(String param)
			{
				startActivity(new Intent(m, PlexSettingsActivity.class));
			}
		});
	}
}
