package com.spuriouslabs.apps.autoplex;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;

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

		showSpinner();
		connector.login(username, password, new PlexCallback<String>()
		{
			@Override
			public void callback(String param)
			{
				hideSpinner();
				startActivity(new Intent(m, PlexSettingsActivity.class));
			}
		});
	}

	private void showSpinner()
	{
		((ProgressBar)findViewById(R.id.login_progress_bar)).setVisibility(View.VISIBLE);
		enableForm(false);
	}

	private void hideSpinner()
	{
		((ProgressBar)findViewById(R.id.login_progress_bar)).setVisibility(View.INVISIBLE);
		enableForm(true);
	}

	private void enableForm(boolean enabled)
	{
		((EditText)findViewById(R.id.plex_username)).setEnabled(enabled);
		((EditText)findViewById(R.id.plex_password)).setEnabled(enabled);
	}
}
