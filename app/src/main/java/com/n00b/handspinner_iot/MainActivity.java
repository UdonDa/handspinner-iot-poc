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

import android.annotation.TargetApi;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.Intent;
import android.os.Build;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import cc.linkmob.bluetoothlowenergylibrary.BluetoothUtils;

import twitter4j.*;
import twitter4j.auth.AccessToken;
import twitter4j.auth.RequestToken;

public class MainActivity extends AppCompatActivity {

    /*Twitter*/
    Button tweetButton, authorizationTwitterButton;
    private Twitter mTwitter;
    private RequestToken mRequestToken;
    public Tweet mTweet;
    private String mCallbackURL;
    SharedPreferences preferences;
    Context act = this;
    String TIMES = "numberOfTweet";
  
    /*Bluetooth*/
    private BluetoothUtils mBluetoothUtil;
    private LeDeviceListAdapter mLeDeviceListAdapter;
    private TextView statusTV;
    private View gattView;

    private TextView dataValue;
    private Button btnSend;
    private EditText edtSend;


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
  
        Toolbar toolbar = (Toolbar)findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        initView();
        try {
            mBluetoothUtil = BluetoothUtils.getInstance(this);
        } catch (Exception e) {
            e.printStackTrace();
        }

        mBluetoothUtil.setOnBluetoothUtilStatusChangeLinsener(new BluetoothUtils.OnBluetoothUtilStatusChangeListener() {
            @Override
            public void onFindDevice(BluetoothDevice device) {

                mLeDeviceListAdapter.addDevice(device);
                mLeDeviceListAdapter.notifyDataSetChanged();
            }

            @Override
            public void onLeServiceInitFailed() {
            }

            @Override
            public void onFindGattServices(List<BluetoothGattService> supportedGattServices) {
            }

            @Override
            public void onFindData(String uuid, String data) {
                dataValue.append("接続：" + data + "\r\n");
            }
          
            @Override
            public void onConnected() {
                mListView.setVisibility(View.GONE);
                gattView.setVisibility(View.VISIBLE);
                isScanView = false;
                isConnected = true;
                statusTV.setText("status:connected");
                invalidateOptionsMenu();
            }

            @Override
            public void onDisconnected() {
                isConnected = false;
                statusTV.setText("status:disconnected");
                invalidateOptionsMenu();
            }

            @Override
            public void onFindGattService(BluetoothGattService supportedGattService) {
            }

            @Override
            public void onFindGattCharacteristic(BluetoothGattCharacteristic characteristic) {

                mNotifyCharacteristic = characteristic;
                mBluetoothUtil.setCharacteristicNotification(
                        characteristic, true);
            }

            @Override
            public void onSendData(String UUID, String data) {
                dataValue.append("送信：" + data + "\r\n");
            }
        });

        
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mBluetoothUtil.onDestroy(this);
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
      
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            showToast("成功");
        } else {
            showToast("失败");
        }
    }
      
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            showToast("成功");
        } else {
            showToast("失败");
        }
    }
     
    Toast mToast = null;

    private synchronized void showToast(String text) {
        if (mToast == null) {
            mToast = Toast.makeText(this, text, Toast.LENGTH_SHORT);
        } else {
            mToast.setText(text);
        }
        mToast.show();
    }
      
    private BluetoothGattCharacteristic mNotifyCharacteristic;
    private ListView mListView;
    UUID cId = UUID.fromString("0000ffe1-0000-1000-8000-00805f9b34fb");
    UUID sId = UUID.fromString("0000ffe0-0000-1000-8000-00805f9b34fb");
      
    private void initView() {
        mListView = (ListView) findViewById(R.id.listview);
        statusTV = (TextView) findViewById(R.id.tv_status);
        mLeDeviceListAdapter = new LeDeviceListAdapter();
        mListView.setAdapter(mLeDeviceListAdapter);
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                final BluetoothDevice device = mLeDeviceListAdapter.getDevice(position);
                if (device == null) return;
                mAddress = device.getAddress();
                mBluetoothUtil.connectDevice(MainActivity.this, mAddress, sId, cId);
                Toast.makeText(MainActivity.this, "item", Toast.LENGTH_SHORT).show();
            }
        });

        gattView = findViewById(R.id.gattView);
        dataValue = (TextView) findViewById(R.id.data_value);
        btnSend = (Button) findViewById(R.id.btnSend);
        edtSend = (EditText) findViewById(R.id.edtSend);

        btnSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String data = edtSend.getText().toString();
                try {

                    if (!mBluetoothUtil.sendData(mNotifyCharacteristic, data)) {
                        showToast("送信失败！");
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    showToast("送信失败！");
                }
                edtSend.setText("");
            }
        });
    }
      
    private String mAddress;
      
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_scan:
                if (mBluetoothUtil != null) {
                    mBluetoothUtil.scanLeDevice(this, true);
                }

                break;
            case R.id.menu_stop:
                if (mBluetoothUtil != null) {
                    mBluetoothUtil.scanLeDevice(this, false);
                }
                break;


            case R.id.menu_connect:
                if (mBluetoothUtil != null) {
                    mBluetoothUtil.connectDevice(this, mAddress, sId, cId);
                }
                break;
            case R.id.menu_disconnect:
                if (mBluetoothUtil != null) {
                    mBluetoothUtil.disconnecDevice();
                }
                break;

        }
        return true;
    }
      
    private boolean isScanView = true;
    private boolean isConnected = false;
      
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        if (isScanView) {

            if (!mBluetoothUtil.isScanning()) {
                menu.findItem(R.id.menu_stop).setVisible(false);
                menu.findItem(R.id.menu_connect).setVisible(false);
                menu.findItem(R.id.menu_disconnect).setVisible(false);
                menu.findItem(R.id.menu_scan).setVisible(true);
                menu.findItem(R.id.menu_refresh).setActionView(null);
            } else {
                menu.findItem(R.id.menu_stop).setVisible(true);
                menu.findItem(R.id.menu_scan).setVisible(false);
                menu.findItem(R.id.menu_connect).setVisible(false);
                menu.findItem(R.id.menu_disconnect).setVisible(false);
                menu.findItem(R.id.menu_refresh).setActionView(
                        R.layout.actionbar_indeterminate_progress);
            }
        } else {
            if (isConnected) {
                menu.findItem(R.id.menu_stop).setVisible(false);
                menu.findItem(R.id.menu_scan).setVisible(false);
                menu.findItem(R.id.menu_connect).setVisible(false);
                menu.findItem(R.id.menu_disconnect).setVisible(true);
            } else {
                menu.findItem(R.id.menu_stop).setVisible(false);
                menu.findItem(R.id.menu_scan).setVisible(false);
                menu.findItem(R.id.menu_connect).setVisible(true);
                menu.findItem(R.id.menu_disconnect).setVisible(false);
            }
        }
        return true;
    }

    @Override
    public void onBackPressed() {
        if (isScanView) {
            super.onBackPressed();

        } else {
            mBluetoothUtil.disconnecDevice();
            mListView.setVisibility(View.VISIBLE);
            gattView.setVisibility(View.GONE);
            dataValue.setText("");
            isScanView = true;
            invalidateOptionsMenu();
        }
    }

    // Adapter for holding devices found through scanning.
    private class LeDeviceListAdapter extends BaseAdapter {
        private ArrayList<BluetoothDevice> mLeDevices;
        private LayoutInflater mInflator;

        public LeDeviceListAdapter() {
            super();
            mLeDevices = new ArrayList<BluetoothDevice>();
            mInflator = MainActivity.this.getLayoutInflater();
            LayoutInflater.from(MainActivity.this);
        }

        public void addDevice(BluetoothDevice device) {
            if (!mLeDevices.contains(device)) {
                mLeDevices.add(device);
            }
        }

        public BluetoothDevice getDevice(int position) {
            return mLeDevices.get(position);
        }


        @Override
        public int getCount() {
            return mLeDevices.size();
        }

        @Override
        public Object getItem(int i) {
            return mLeDevices.get(i);
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            ViewHolder viewHolder;
            // General ListView optimization code.
            if (view == null) {
                view = mInflator.inflate(R.layout.listitem_device, null);
                viewHolder = new ViewHolder();
                viewHolder.deviceAddress = (TextView) view.findViewById(R.id.device_address);
                viewHolder.deviceName = (TextView) view.findViewById(R.id.device_name);
                view.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) view.getTag();
            }

            BluetoothDevice device = mLeDevices.get(i);
            final String deviceName = device.getName();
            if (deviceName != null && deviceName.length() > 0)
                viewHolder.deviceName.setText(deviceName);
            else
                viewHolder.deviceName.setText(R.string.unknown_device);
            viewHolder.deviceAddress.setText(device.getAddress());

            return view;
        }
    }

    static class ViewHolder {
        TextView deviceName;
        TextView deviceAddress;
    }
}