package com.zsl.bluetoothdemo;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import com.orhanobut.logger.Logger;
import com.zsl.bluetoothdemo.base.BaseActivity;
import com.zsl.bluetoothdemo.utils.ble.BleWrapper;
import com.zsl.bluetoothdemo.utils.ble.BleWrapperUiCallbacks;

import java.util.List;
import java.util.UUID;

/**
 * Created by zsl on 15/9/21.
 */
public class DeviceHomeActivity extends BaseActivity implements BleWrapperUiCallbacks{




    private static final UUID UUID_MILI_SERVICE = UUID
            .fromString("0000fee0-0000-1000-8000-00805f9b34fb");


    private static final UUID UUID_CHAR_REALTIME_STEPS = UUID
            .fromString("0000ff06-0000-1000-8000-00805f9b34fb");


    private static final UUID UUID_CHAR_pair = UUID
            .fromString("0000ff0f-0000-1000-8000-00805f9b34fb");


    TextView tv_connect_state;


    //记录步数
    int mSteps = 1;


    //btDevice
    BluetoothDevice bleDevice;

    //bleWrapper
    private BleWrapper mBleWrapper;
    //gatt
    BluetoothGatt bluetoothGatt;





    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_home);
        bleDevice=getIntent().getParcelableExtra("BluetoothDevice");
        tv_connect_state= (TextView) findViewById(R.id.device_home_tv_connect_state);

    }

    @Override
    protected void onResume() {
        super.onResume();
        if(mBleWrapper == null) mBleWrapper = new BleWrapper(this, this);

        if(mBleWrapper.initialize() == false) {
            finish();
        }
        Toast.makeText(this,bleDevice.getAddress()+":"+bleDevice.getName(),Toast.LENGTH_LONG).show();
        //启动自动连接设备
        tv_connect_state.setText(bleDevice.getAddress()+"\n"+bleDevice.getName()+"\n连接中 ...");
        mBleWrapper.connect(bleDevice.getAddress());
    }

    @Override
    protected void onPause() {
        super.onPause();
        mBleWrapper.stopMonitoringRssiValue();
        mBleWrapper.diconnect();
        mBleWrapper.close();
    }

    private void request(UUID what) {
        bluetoothGatt.readCharacteristic(getMiliService().getCharacteristic(what));
    }

    //get获取步数的Server
    private BluetoothGattService getMiliService() {
        return bluetoothGatt.getService(UUID_MILI_SERVICE);
    }

    //发功指令获取步数
    private void pair() {
        BluetoothGattCharacteristic chrt = getMiliService().getCharacteristic(
                UUID_CHAR_pair);
        chrt.setValue(new byte[]{2});
        bluetoothGatt.writeCharacteristic(chrt);
    }

    /**
     * 下面是 BleWrapper 实现接口
     */


    @Override
    public void uiDeviceFound(BluetoothDevice device, int rssi, byte[] record) {
        Logger.e("======uiDeviceFound");
    }

    @Override
    public void uiDeviceConnected(final BluetoothGatt gatt, BluetoothDevice device) {
        Logger.e("======uiDeviceConnected");
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                tv_connect_state.setText("连接成功");
                bluetoothGatt=gatt;
            }
        });
    }

    @Override
    public void uiDeviceDisconnected(BluetoothGatt gatt, BluetoothDevice device) {
        Logger.e("======uiDeviceDisconnected");
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                tv_connect_state.setText("断开连接");
            }
        });
    }

    //onServicesDiscovered
    @Override
    public void uiAvailableServices(BluetoothGatt gatt, BluetoothDevice device, List<BluetoothGattService> services) {
                pair();

    }

    @Override
    public void uiCharacteristicForService(BluetoothGatt gatt, BluetoothDevice device, BluetoothGattService service, List<BluetoothGattCharacteristic> chars) {
        Logger.e("======uiCharacteristicForService");
    }

    @Override
    public void uiCharacteristicsDetails(BluetoothGatt gatt, BluetoothDevice device, BluetoothGattService service, final BluetoothGattCharacteristic characteristic) {
        Logger.e("======uiCharacteristicsDetails");

    }

    //onCharacteristicRead
    @Override
    public void uiNewValueForCharacteristic(BluetoothGatt gatt, BluetoothDevice device, BluetoothGattService service, final BluetoothGattCharacteristic ch, String strValue, int intValue, byte[] rawValue, String timestamp) {
        Logger.e("======uiNewValueForCharacteristic");
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                byte[] b = ch.getValue();
                if (ch.getUuid().equals(UUID_CHAR_REALTIME_STEPS)) {
                    mSteps = (0xff & b[0] | (0xff & b[1]) << 8);
                    tv_connect_state.setText(mSteps+"步");
                }
                request(UUID_CHAR_REALTIME_STEPS);
            }
        });
    }

    //onCharacteristicChanged
    @Override
    public void uiGotNotification(BluetoothGatt gatt, BluetoothDevice device, BluetoothGattService service, BluetoothGattCharacteristic characteristic) {
        Logger.e("======uiGotNotification");
    }

    //onCharacteristicWrite successful
    @Override
    public void uiSuccessfulWrite(BluetoothGatt gatt, BluetoothDevice device, BluetoothGattService service, BluetoothGattCharacteristic ch, String description) {
        Logger.e("======uiSuccessfulWrite");
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                request(UUID_CHAR_REALTIME_STEPS);
            }
        });
    }

    //onCharacteristicWrite failed
    @Override
    public void uiFailedWrite(BluetoothGatt gatt, BluetoothDevice device, BluetoothGattService service, BluetoothGattCharacteristic ch, String description) {
        Logger.e("======uiFailedWrite");
    }

    //onReadRemoteRssi
    @Override
    public void uiNewRssiAvailable(BluetoothGatt gatt, BluetoothDevice device, int rssi) {
        Logger.e("======uiNewRssiAvailable");
    }
}
