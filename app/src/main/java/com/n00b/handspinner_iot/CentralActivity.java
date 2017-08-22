package com.n00b.handspinner_iot;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.ProgressDialog;
import android.bluetooth.le.*;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.util.List;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.lang.reflect.Array;
import java.util.List;


import java.util.UUID;
public class CentralActivity extends FragmentActivity implements LocationListener, IBleActivity, View.OnClickListener{

    public class SendDataTimer extends TimerTask {
        @Override
        public void run() {
            if (isBleEnabled) {
                final String mSendValue = String.valueOf(random.nextInt(1000));
                // UIスレッドで生成した数をTextViewにセット.
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mSendValueView.setText(mSendValue);
                    }
                });

                // Characteristicにランダムな値をセットして、Writeリクエストを送信.
                mBleCharacteristic.setValue(mSendValue);
                mBleGatt.writeCharacteristic(mBleCharacteristic);
            }
        }
    }

    private BluetoothManager mBleManager;
    private BluetoothAdapter mBleAdapter;
    private BluetoothLeScanner mBleLeScanner;
    private boolean isBleEnabled = false;
    private BluetoothLeScanner mBleScanner;
    private BluetoothGatt mBleGatt;
    private BluetoothGattCharacteristic mBleCharacteristic;

    private TextView mReceivedValueView;
    private TextView mSendValueView;

    // 乱数送信用.
    private Random random = new Random();
    private Timer timer;
    private SendDataTimer sendDataTimer;

    //gps
    Button btnCatchGps;
    TextView txvLocation;
    LocationManager mLocationManager;
    ProgressDialog mProgressDialog;

    public void onGpsIsEnabled(){
        // 2016.03.07現在GPSを要求するのが6.0以降のみなのでOnになったら新しいAPIでScan開始.
        this.startScanByBleScanner();
    }


        void initGpsViews() {
            TextView txvLocation = (TextView)findViewById(R.id.txv_location);
            Button btnCatchGps = (Button)findViewById(R.id.button);
            btnCatchGps.setOnClickListener(this);

            mLocationManager = (LocationManager)getSystemService(LOCATION_SERVICE);
            mProgressDialog = new ProgressDialog(this);
            mProgressDialog.setMessage("Fetching location");
        }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_central);

        init_central();
/*
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION,}, REQUEST_PERMISSION);

            return;
        }
*/
        initGpsViews();

    }

    @Override
    public void onClick(View view) {
        if (view.getId()==R.id.button) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)!= PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this,Manifest.permission.ACCESS_COARSE_LOCATION)!=PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Please Grant Permission from settings", Toast.LENGTH_SHORT).show();
            }
            else {
                mLocationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER,5,1200, (android.location.LocationListener) this);
                mProgressDialog.show();
            }
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        double latitude= location.getLatitude();
        double longitude= location.getLongitude();

        txvLocation.setText("Location.."+latitude+" : "+longitude);

        mProgressDialog.dismiss();

        //locationmanager.removeUpdates(this);


        try {
            Geocoder geocoder= new Geocoder(this);
            List<Address> adrslist= geocoder.getFromLocation(latitude,longitude,2);
            if (adrslist!=null && adrslist.size()>0){
                Address address = adrslist.get(0);

                StringBuffer buffer=new StringBuffer();
                for (int i = 0; i < address.getMaxAddressLineIndex(); i++) {
                    buffer.append(address.getAddressLine(i)+"/n");
                }
                txvLocation.setText(buffer.toString());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(String provider) {

    }

    @Override
    public void onProviderDisabled(String provider) {

    }













    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // Intentでユーザーがボタンを押したら実行.
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case R.string.request_ble_on:
                if ((mBleAdapter != null)
                        || (mBleAdapter.isEnabled())) {
                    // BLEが使用可能ならスキャン開始.
                    this.scanNewDevice();
                }
                break;
            case R.string.request_enable_location:
                if(resultCode == RESULT_OK){
                    onGpsIsEnabled();
                }
                break;
        }
    }


    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback(){
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState){
            // 接続状況が変化したら実行.
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                // 接続に成功したらサービスを検索する.
                gatt.discoverServices();
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                // 接続が切れたらGATTを空にする.
                if (mBleGatt != null){
                    mBleGatt.close();
                    mBleGatt = null;
                }
                isBleEnabled = false;
            }
        }
        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status){
            // Serviceが見つかったら実行.
            if (status == BluetoothGatt.GATT_SUCCESS) {
                // UUIDが同じかどうかを確認する.
                BluetoothGattService bleService = gatt.getService(UUID.fromString(getString(R.string.uuid_service)));
                if (bleService != null){
                    // 指定したUUIDを持つCharacteristicを確認する.
                    mBleCharacteristic = bleService.getCharacteristic(UUID.fromString(getString(R.string.uuid_characteristic)));
                    if (mBleCharacteristic != null) {
                        // Service, CharacteristicのUUIDが同じならBluetoothGattを更新する.
                        mBleGatt = gatt;
                        // キャラクタリスティックが見つかったら、Notificationをリクエスト.
                        mBleGatt.setCharacteristicNotification(mBleCharacteristic, true);

                        // Characteristic の Notificationを有効化する.
                        BluetoothGattDescriptor bleDescriptor = mBleCharacteristic.getDescriptor(
                                UUID.fromString(getString(R.string.uuid_characteristic_config)));
                        bleDescriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                        mBleGatt.writeDescriptor(bleDescriptor);
                        // 接続が完了したらデータ送信を開始する.
                        isBleEnabled = true;
                    }
                }
            }
        }
        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, final BluetoothGattCharacteristic characteristic){
            // キャラクタリスティックのUUIDをチェック(getUuidの結果が全て小文字で帰ってくるのでUpperCaseに変換)
            if (getString(R.string.uuid_characteristic).equals(characteristic.getUuid().toString().toUpperCase())){
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mReceivedValueView.setText(characteristic.getStringValue(0));

                    }
                });
            }
        }
    };

    private void scanNewDevice(){
        // OS ver.6.0以上ならGPSがOnになっているかを確認する(GPSがOffだとScanに失敗するため).
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
        }
        // OS ver.5.0以上ならBluetoothLeScannerを使用する.
        else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP){
            this.startScanByBleScanner();
        }
        else{
            // デバイスの検出.
        }
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void startScanByBleScanner(){
        mBleScanner = mBleAdapter.getBluetoothLeScanner();

        // デバイスの検出.
        mBleScanner.startScan(new ScanCallback() {
            @Override
            public void onScanResult(int callbackType, ScanResult result) {
                super.onScanResult(callbackType, result);
                // スキャン中に見つかったデバイスに接続を試みる.第三引数には接続後に呼ばれるBluetoothGattCallbackを指定する.
                result.getDevice().connectGatt(getApplicationContext(), false, mGattCallback);
            }

            @Override
            public void onScanFailed(int intErrorCode) {
                super.onScanFailed(intErrorCode);
            }
        });
    }

    private void init_central() {
        isBleEnabled = false;

        // Writeリクエストで送信する値、Notificationで受け取った値をセットするTextView.
        mReceivedValueView = (TextView)findViewById(R.id.received_value_view);
        mSendValueView = (TextView)findViewById(R.id.send_value_view);

        //Bluetooth初期化
        mBleManager = (BluetoothManager)getSystemService(Context.BLUETOOTH_SERVICE);
        mBleAdapter = mBleManager.getAdapter();

        // Writeリクエスト用のタイマーをセット.
        timer = new Timer();
        sendDataTimer = new SendDataTimer();
        // 第二引数:最初の処理までのミリ秒 第三引数:以降の処理実行の間隔(ミリ秒).
        timer.schedule(sendDataTimer, 500, 1000);

        Button sendButton = (Button) findViewById(R.id.send_button);
        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(isBleEnabled){
                    mBleCharacteristic.setValue(((EditText) findViewById(R.id.input_area)).getText().toString());
                    mBleGatt.writeCharacteristic(mBleCharacteristic);
                }
            }
        });

        // BluetoothがOffならインテントを表示する.
        if ((mBleAdapter == null)
                || (! mBleAdapter.isEnabled())) {
            // Intentでボタンを押すとonActivityResultが実行されるので、第二引数の番号を元に処理を行う.
            startActivityForResult(new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE), R.string.request_ble_on);
        }
        else{
            // BLEが使用可能ならスキャン開始.
            this.scanNewDevice();
        }
    }

}
