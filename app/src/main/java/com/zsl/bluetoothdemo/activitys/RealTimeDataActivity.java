package com.zsl.bluetoothdemo.activitys;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.zsl.bluetoothdemo.R;
import com.zsl.bluetoothdemo.base.BaseActivity;
import com.zsl.bluetoothdemo.utils.ble.oad.BluetoothLeService;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * Created by zsl on 15/10/21.
 * 实时数据的传输
 */
public class RealTimeDataActivity extends BaseActivity {

    TextView tv_message;
    Button bt_read_data,bt_setconfig,bt_battery;


    private String logTag="RealTimeDataActivity";

    //实时数据的Service uuid
    public static final UUID UUID_HISTO_DATA_SERVICE = UUID
            .fromString("00001204-0000-1000-8000-00805f9b34fb");


    //BluetoothLeService
    private BluetoothLeService mLeService = null;
    //设备连接配置界面
    DeviceHomeActivity deviceHomeActivity;
    //所有的Service
    List<BluetoothGattService> serviceList;
    //实时数据的service
    BluetoothGattService realTimeDataService;
    //实时数据下service对应的所有BluetoothGattCharacteristic
    List<BluetoothGattCharacteristic> realTimeDataCharList;
    //配置的BluetoothGattCharacteristic
    BluetoothGattCharacteristic mCharConfig,mCharData,mCharBattery;

    //是否开启实时同步
    boolean isOpenUpdate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_realtimedata);

        initView();
        initEvent();
        initData();


    }

    private void initView() {
        tv_message= (TextView) findViewById(R.id.realtime_tv_message);
        bt_read_data= (Button) findViewById(R.id.realtime_bt_read_data);
        bt_setconfig= (Button) findViewById(R.id.realtime_bt_setconfig);
        bt_battery= (Button) findViewById(R.id.realtime_bt_read_battery);
    }

    private void initEvent() {
        //读取数据
        bt_read_data.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mLeService.readCharacteristic(mCharData);
            }
        });

        //开启或关闭数据
        bt_setconfig.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isOpenUpdata(!isOpenUpdate);

            }
        });

        //读取当前电量
        bt_battery.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mLeService.readCharacteristic(mCharBattery);
            }
        });
    }

    /**
     * 是否开启更新
     * @param isOpen
     */
    private void isOpenUpdata(boolean isOpen) {
        isOpenUpdate=isOpen;
        byte[] config;
        if (isOpen) {
            mLeService.setCharacteristicNotification(mCharData,true);
            config = new byte[]{(byte) 0xa0, 0x1f};
            bt_setconfig.setText("关闭数据同步");
        }else{
            mLeService.setCharacteristicNotification(mCharData,false);
            config = new byte[]{(byte) 0xc0, 0x1f};
            bt_setconfig.setText("开启数据同步");
        }
        mCharConfig.setValue(config);
        mLeService.writeCharacteristic(mCharConfig);
    }

    private void initData() {
        //注册广播
        registerReceiver(realtimeDataBroadcastReceiver, SettingIntentFilter());

        //获得到BluetoothLeService对象
        mLeService = BluetoothLeService.getInstance();
        deviceHomeActivity = DeviceHomeActivity.getmDeviceHomeActivity();
        //获得到所有的service
        serviceList = deviceHomeActivity.getServiceList();

        if (serviceList==null)
            return;
        for (BluetoothGattService service:serviceList) {
            Log.e(logTag, service.getUuid() + "");
            if (service.getUuid().equals(UUID_HISTO_DATA_SERVICE)){
                //历史数据的service
                realTimeDataService=service;
            }
        }

        //获得到Config，Data，Battery的Characteristic
        if (realTimeDataService!=null){
            realTimeDataCharList=realTimeDataService.getCharacteristics();
            Log.e(logTag,"realTimeDataCharList_count:"+realTimeDataCharList.size());
            mCharConfig=realTimeDataCharList.get(0);
            mCharData=realTimeDataCharList.get(1);
            mCharBattery=realTimeDataCharList.get(2);

            mLeService.setCharacteristicNotification(mCharData,true);
        }
    }

    @Override
    protected void onDestroy() {
        unregisterReceiver(realtimeDataBroadcastReceiver);
        super.onDestroy();
    }

    /**
     * 设置IntentFilter
     *
     * @return
     */
    private IntentFilter SettingIntentFilter() {
        final IntentFilter fi = new IntentFilter();
        fi.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        fi.addAction(BluetoothLeService.ACTION_DATA_NOTIFY);
        fi.addAction(BluetoothLeService.ACTION_DATA_WRITE);
        fi.addAction(BluetoothLeService.ACTION_DATA_READ);
        return fi;
    }

    /**
     * Ble的广播
     */
    private BroadcastReceiver realtimeDataBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) { //连接断开
                tv_message.setText("连接断开");
            } else if (BluetoothLeService.ACTION_DATA_NOTIFY.equals(action)) {
                byte[] value = intent.getByteArrayExtra(BluetoothLeService.EXTRA_DATA);
                String uuidStr = intent.getStringExtra(BluetoothLeService.EXTRA_UUID);
                if (uuidStr.equals(mCharData.getUuid().toString())){
                    tv_message.append("value_notify:" + Arrays.toString(value));
                }
            } else if (BluetoothLeService.ACTION_DATA_WRITE.equals(action)) {
                int status = intent.getIntExtra(BluetoothLeService.EXTRA_STATUS, BluetoothGatt.GATT_SUCCESS);
                byte[] value = intent.getByteArrayExtra(BluetoothLeService.EXTRA_DATA);
                if (status != BluetoothGatt.GATT_SUCCESS) {
                    showLongToast("GATT error: status=" + status);
                }
            }else if (BluetoothLeService.ACTION_DATA_READ.equals(action)) {
                byte[] value = intent.getByteArrayExtra(BluetoothLeService.EXTRA_DATA);
                String uuidStr = intent.getStringExtra(BluetoothLeService.EXTRA_UUID);
                Log.e(logTag, Arrays.toString(value));
                if (uuidStr.equals(mCharData.getUuid().toString())) {
                    tv_message.append("value:" + Arrays.toString(value));
                }else if(uuidStr.equals(mCharBattery.getUuid().toString())){
                    tv_message.append("当前电量："+Arrays.toString(value));
                }

            }
        }
    };
}
