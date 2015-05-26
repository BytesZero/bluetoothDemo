package com.zsl.bluetoothdemo;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;

import com.orhanobut.logger.Logger;
import com.zsl.bluetoothdemo.adapter.DevicesAdapter;
import com.zsl.bluetoothdemo.ble.UniversalBluetoothLE;
import java.util.List;
import java.util.UUID;


public class MainActivity extends AppCompatActivity {

    private static final UUID UUID_MILI_SERVICE = UUID
            .fromString("0000fee0-0000-1000-8000-00805f9b34fb");

    private static final UUID UUID_CHAR_REALTIME_STEPS = UUID
            .fromString("0000ff06-0000-1000-8000-00805f9b34fb");

    private static final UUID UUID_CHAR_pair = UUID
            .fromString("0000ff0f-0000-1000-8000-00805f9b34fb");


    Handler mHandler;

    BluetoothGatt mBluetoothGatt;


    private static final int STATE_DISCONNECTED = 0;
    private static final int STATE_CONNECTING = 1;
    private static final int STATE_CONNECTED = 2;

    private int mConnectionState = STATE_DISCONNECTED;

    //蓝牙设别的list
    private List<BluetoothDevice> bluetoothDevices;


    ListView lv_show;
    TextView tv_state;
    private DevicesAdapter devicesAdapter;


    int mSteps = 0;

    UniversalBluetoothLE universalBluetoothLE;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        //初始化UniversalBluetoothLE
        universalBluetoothLE = UniversalBluetoothLE.inistance(MainActivity.this);
        //打开蓝牙
        universalBluetoothLE.openBbletooth();


        initView();

        mHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                if (msg.what == 2) {
                    tv_state.setText("连接成功");
                } else if (msg.what == 5) {
                    Logger.e("devicesAdapter.notifyDataSetChanged();");
                    bluetoothDevices= (List<BluetoothDevice>) msg.obj;
                    Logger.e("bluetoothDevices:"+bluetoothDevices);
                    if (devicesAdapter == null) {
                        //蓝牙的Adapter
                        devicesAdapter = new DevicesAdapter(MainActivity.this, bluetoothDevices);
                        //设置Adapter
                        lv_show.setAdapter(devicesAdapter);
                    } else {
                        devicesAdapter.notifyDataSetChanged();
                    }

                } else if (msg.what == 0) {
                    tv_state.setText(mSteps + "步");
                }
            }
        };


    }

    private void request(UUID what) {
        mBluetoothGatt.readCharacteristic(getMiliService().getCharacteristic(what));
    }

    private BluetoothGattService getMiliService() {
        return mBluetoothGatt.getService(UUID_MILI_SERVICE);
    }

    private void initView() {
        tv_state = (TextView) findViewById(R.id.main_tv_state);
        lv_show = (ListView) findViewById(R.id.main_lv_show);
        lv_show.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                BluetoothDevice device = bluetoothDevices.get(position);
                Logger.e("name:" + device.getName() + "address:" + device.getAddress() + "bluetoothcalss:" + device.getBluetoothClass() + "bondstate:" + device.getBondState() + "type:" + device
                        .getType() + "uuid:" + device.getUuids());

                mBluetoothGatt=universalBluetoothLE.getConnectGatt(device,true,mGattCallback);
                mBluetoothGatt.connect();
            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        mBluetoothGatt.disconnect();
        mBluetoothGatt.close();
        mBluetoothGatt=null;
    }

    private void pair() {

        BluetoothGattCharacteristic chrt = getMiliService().getCharacteristic(
                UUID_CHAR_pair);

        chrt.setValue(new byte[]{2});

        mBluetoothGatt.writeCharacteristic(chrt);

        Logger.e("pair sent");
    }

    private final BluetoothGattCallback mGattCallback =
            new BluetoothGattCallback() {

                @Override
                public void onConnectionStateChange(BluetoothGatt gatt, int status,
                                                    int newState) {
                    if (newState == BluetoothProfile.STATE_CONNECTED) {
                        mConnectionState = STATE_CONNECTED;
                        Logger.e("Connected to GATT server：" + newState);

                        mHandler.sendEmptyMessage(newState);
                        gatt.discoverServices();


                    } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                        mConnectionState = STATE_DISCONNECTED;
                        Logger.e("Disconnected from GATT server.");
                    }
                }

                @Override
                // New services discovered
                public void onServicesDiscovered(BluetoothGatt gatt, int status) {
                    if (status == BluetoothGatt.GATT_SUCCESS) {
                        pair();
                        Logger.e("onServicesDiscovered: " + status);
                    } else {
                        Logger.e("onServicesDiscovered received: " + status);
                    }
                }

                @Override
                // Result of a characteristic read operation
                public void onCharacteristicRead(BluetoothGatt gatt,
                                                 BluetoothGattCharacteristic characteristic,
                                                 int status) {
                    Logger.e("onCharacteristicRead: " + status);
                    if (status == BluetoothGatt.GATT_SUCCESS) {

                        byte[] b = characteristic.getValue();
                        Logger.e(characteristic.getUuid() + "bbb:" + b);

                        if (characteristic.getUuid().equals(UUID_CHAR_REALTIME_STEPS)) {
                            mSteps = (0xff & b[0] | (0xff & b[1]) << 8);
                            Logger.e("mSteps:" + mSteps);
                            mHandler.sendEmptyMessage(0);
                        }
                    }
                    request(UUID_CHAR_REALTIME_STEPS);
                }

                @Override
                public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
                    Logger.e("onCharacteristicWrite: " + status);
                    request(UUID_CHAR_REALTIME_STEPS);
                }
            };


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_settings) {
            Logger.e("scanLeDevice(true)");
            if (devicesAdapter!=null) {
                bluetoothDevices.clear();
                devicesAdapter.notifyDataSetChanged();
            }
            //扫描设备
            universalBluetoothLE.startScanLeDevice(new UniversalBluetoothLE.LeScanListenter() {

                @Override
                public void leScanCallBack(List<BluetoothDevice> bluetoothDeviceList) {
                    Logger.e(bluetoothDeviceList.get(0).getName());
//                    bluetoothDevices = bluetoothDeviceList;
                    Logger.e("bluetoothDeviceList:" + bluetoothDeviceList + "");
                    Message message = new Message();
                    message.what = 5;
                    message.obj = bluetoothDeviceList;
                    mHandler.sendMessage(message);
                }
            });
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

}
