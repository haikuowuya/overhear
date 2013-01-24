package com.afollestad.overhear.ui;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import com.afollestad.overhear.Queue;
import com.afollestad.overhear.R;
import com.afollestad.overhearapi.Song;
import twitter4j.*;

public class TweetNowPlaying extends Activity {

	private Twitter twitter;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.dialog_tweet_now_playing); 
		twitter = LoginHandler.getTwitterInstance(getApplicationContext(), true);
		loadInitialText();
		findViewById(R.id.tweetBtn).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				send();
			}
		});
		findViewById(R.id.cancel).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				finish();
			}
		});
	}

	private void send() {
		final TextView text = (TextView)findViewById(R.id.tweetText);
		final Button send = (Button)findViewById(R.id.tweetBtn);
		new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					twitter.updateStatus(new StatusUpdate(text.getText().toString().trim()));
					runOnUiThread(new Runnable() {
						@Override
						public void run() {
							Toast.makeText(getApplicationContext(), R.string.tweeted_str, Toast.LENGTH_SHORT).show();
							finish();
						}
					});
				} catch (final TwitterException e) {
					e.printStackTrace();
					runOnUiThread(new Runnable() {
						@Override
						public void run() {
							send.setEnabled(true);
							Toast.makeText(getApplicationContext(), getString(R.string.failed_tweet_str)
									.replace("{error}", e.getMessage()), Toast.LENGTH_SHORT).show();
						}
					});
				}
			}
		}).start();
	}

	private void loadInitialText() {
		final TextView text = (TextView)findViewById(R.id.tweetText);
		final Button send = (Button)findViewById(R.id.tweetBtn);
		final Song last = Queue.getFocused(this);
		
		text.setText(R.string.loading_str);
		text.setEnabled(false);
		send.setEnabled(false);
		
		new Thread(new Runnable() {
			@Override
			public void run() {
				String displayArtist = last.getArtist();
				try {
					ResponseList<User> possibleUsers = twitter.searchUsers(last.getArtist(), 0);
					if(possibleUsers.size() > 0) {
						for(int i = 0; i < possibleUsers.size(); i++) {
							if(possibleUsers.get(i).isVerified()) {
								displayArtist = "@" + possibleUsers.get(i).getScreenName();
								break;
							}
						}
					}
				} catch(Exception e) {
					e.printStackTrace();
				}
				final String fArtist = displayArtist;
				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						text.setText("");
                        text.append(getString(R.string.now_playing_tweet_content)
                                .replace("{title}", last.getTitle())
                                .replace("{artist}", fArtist));
						text.setEnabled(true);
						send.setEnabled(true);
					}
				});
			}
		}).start();
	}
}
