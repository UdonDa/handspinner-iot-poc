package com.n00b.handspinner_iot;

import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import twitter4j.*;
import twitter4j.auth.AccessToken;
import twitter4j.auth.RequestToken;

public class MainActivity extends AppCompatActivity {

    Button tweetButton, authorizationTwitterButton;
    private Twitter mTwitter;
    private RequestToken mRequestToken;
    public Tweet mTweet;
    private String mCallbackURL;
    SharedPreferences preferences;
    Context act = this;
    String TIMES = "numberOfTweet";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        tweetButton = (Button)findViewById(R.id.btn_tweet);
        authorizationTwitterButton = (Button)findViewById(R.id.btn_authorization_twitter);
        preferences = act.getSharedPreferences(TIMES, Context.MODE_PRIVATE);


        /*--- twitter ---*/
        mCallbackURL = getString(R.string.twitter_callback_url);
        mTwitter = TwitterUtils.getTwitterInstance(act);
        mTweet = new Tweet(this, mTwitter);

        authorizationTwitterButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                if(!TwitterUtils.hasAccessToken(act)) {
                    SharedPreferences.Editor editor = preferences.edit();
                    editor.putInt(TIMES, 1);
                    editor.apply();
                    startAuthorize();
                } else {
                    showToast("認証済やで");
                }
            }
        });

        tweetButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                SharedPreferences.Editor editor = preferences.edit();
                editor.putInt(TIMES, preferences.getInt(TIMES,0) + 1);
                editor.apply();
                mTweet.tweet();
            }
        });
        
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }


     /*--- Twitter ---*/
    public void startAuthorize() {
        AsyncTask<Void, Void, String> task = new AsyncTask<Void, Void, String>() {
            @Override
            protected String doInBackground(Void... voids) {
                try{
                    mRequestToken = mTwitter.getOAuthRequestToken(mCallbackURL);
                    return mRequestToken.getAuthorizationURL();
                } catch (TwitterException e) {
                    e.printStackTrace();
                }
                return null;
            }

            @Override
            protected void onPostExecute(String url) {
                if (url != null) {
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                    startActivity(intent);
                } else {

                }
            }
        };
        task.execute();
    }

    @Override
    public void onNewIntent(Intent intent) {
        if (intent == null
                || intent.getData() == null
                || !intent.getData().toString().startsWith(mCallbackURL)) {
            return;
        }
        String verifier = intent.getData().getQueryParameter("oauth_verifier");

        AsyncTask<String, Void, AccessToken> task = new AsyncTask<String, Void, AccessToken>() {
            @Override
            protected AccessToken doInBackground(String... params) {
                try {
                    return mTwitter.getOAuthAccessToken(mRequestToken, params[0]);
                } catch (TwitterException e) {
                    e.printStackTrace();
                }
                return null;
            }

            @Override
            protected void onPostExecute(AccessToken accessToken) {
                if (accessToken != null) {
                    // 認証成功！
                    showToast("認証成功！");
                    successOAuth(accessToken);
                } else {
                    // 認証失敗。。。
                    showToast("認証失敗。。。");
                }
            }
        };
        task.execute(verifier);
    }

    private void successOAuth(AccessToken accessToken) {
        TwitterUtils.storeAccessToken(this, accessToken);
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        //finish();
    }

    private void showToast(String text){
        Toast.makeText(this, text, Toast.LENGTH_SHORT).show();
    }
}
