package com.n00b.handspinner_iot;

/*
    参考：http://masatoshitada.hatenadiary.jp/entry/2015/10/18/143834
*/

import android.content.Context;
import android.os.AsyncTask;
import android.widget.Toast;

import twitter4j.*;

public class Twitteror {
    private Twitter mTwitter;
    private Context context;


    public Twitteror(Context c, Twitter t) {
        this.context = c;
        this.mTwitter = t;
    }

    public void tweet() {
        AsyncTask<String, Void, Boolean> task = new AsyncTask<String, Void, Boolean>() {
            @Override
            protected Boolean doInBackground(String... strings) {
                try {
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
    }

    private void showToast(String text) {
        Toast.makeText(context, text, Toast.LENGTH_SHORT).show();

        //アクセストークンをstringに書いたけん、その後が続き！だよーんwwww
        //ハラヘッタ
    }

}
