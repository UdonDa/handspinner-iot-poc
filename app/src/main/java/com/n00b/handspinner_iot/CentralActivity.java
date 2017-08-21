package com.n00b.handspinner_iot;

import android.annotation.TargetApi;
import android.app.Activity;
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
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResult;
import com.google.android.gms.location.LocationSettingsStatusCodes;

import java.util.UUID;
public class CentralActivity extends FragmentActivity implements IBleActivity{

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

    public void onGpsIsEnabled(){
        // 2016.03.07現在GPSを要求するのが6.0以降のみなのでOnになったら新しいAPIでScan開始.
        this.startScanByBleScanner();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_central);

        init_central();

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
