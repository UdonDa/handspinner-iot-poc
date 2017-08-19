package com.n00b.handspinner_iot;

/*
    参考：http://masatoshitada.hatenadiary.jp/entry/2015/10/18/143834
*/

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.widget.Toast;

import twitter4j.*;
import twitter4j.auth.AccessToken;

public class Tweet {
    private Twitter mTwitter;
    private Context context;
    String TIMES = "numberOfTweet";
    SharedPreferences preferences;


    public Tweet(Context c, Twitter t) {
        this.context = c;
        this.mTwitter = t;
        preferences = c.getSharedPreferences(TIMES, Context.MODE_PRIVATE);

    }

    public void tweet() {
        AsyncTask<String, Void, Boolean> task = new AsyncTask<String, Void, Boolean>() {
            @Override
            protected Boolean doInBackground(String... strings) {
                try {
                    //ツイート
                    mTwitter.updateStatus("テストだよーんwww");
                    return true;
                } catch (TwitterException e) {
                    e.printStackTrace();
                    return false;
                }
            }

            @Override
            protected void onPostExecute(Boolean result) {
                if(result) {
                    showToast("ツイート成功");
                } else {
                    showToast("ツイート失敗");
                }
            }
        };
        task.execute("unchi");
    }

    private void showToast(String text) {
        Toast.makeText(context, text, Toast.LENGTH_SHORT).show();
    }

}
