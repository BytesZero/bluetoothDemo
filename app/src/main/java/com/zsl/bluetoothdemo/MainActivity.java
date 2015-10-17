package com.zsl.bluetoothdemo;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import com.zsl.bluetoothdemo.adapter.DevicesAdapter;
import com.zsl.bluetoothdemo.base.BaseActivity;
import com.zsl.bluetoothdemo.utils.ble.BleWrapper;
import com.zsl.bluetoothdemo.utils.ble.BleWrapperUiCallbacks;
import com.zsl.bluetoothdemo.utils.ble.oad.BluetoothLeService;

import java.util.ArrayList;
import java.util.List;


public class MainActivity extends BaseActivity {
    //扫描超时
    private static final long SCANNING_TIMEOUT = 10 * 1000; /* 5 seconds */
    //requestid请求打开蓝牙
    private static final int ENABLE_BT_REQUEST_ID = 1;

    //蓝牙设别的list
    private List<MyBluetoothDevice> bluetoothDevices;


    ListView lv_show;
    Button bt_state;
    private BluetoothLeService mBluetoothLeService = null;
    private DevicesAdapter devicesAdapter;
    int mSteps = 0;

    //    BleWrapper 对象
    private BleWrapper mBleWrapper = null;
    //    是否启动自动扫描
    private boolean mScanning = false;
    //    handler
    private Handler mHandler = new Handler();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mBluetoothLeService = BluetoothLeService.getInstance();
        // 创建一个BleWrapper然后返回扫描到的设备
        mBleWrapper = new BleWrapper(this, new BleWrapperUiCallbacks.Null() {
            @Override
            public void uiDeviceFound(final BluetoothDevice device, final int rssi, final byte[] record) {
                handleFoundDevice(device, rssi, record);
            }
        });

        if (mBleWrapper.checkBleHardwareAvailable() == false) {
            bleMissing();
        }

        initView();
    }


    private void initView() {
        bt_state = (Button) findViewById(R.id.main_bt_state);
        bt_state.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Uri uri = Uri.parse("market://details?id=" + getPackageName());

                Intent intent = new Intent(Intent.ACTION_VIEW, uri);

                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                startActivity(intent);
            }
        });
        lv_show = (ListView) findViewById(R.id.main_lv_show);
        lv_show.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                BluetoothDevice device = bluetoothDevices.get(position).getBluetoothDevice();

                Intent intent = new Intent(MainActivity.this, DeviceHomeActivity.class);
                intent.putExtra("BluetoothDevice", device);
                startActivity(intent);
                Toast.makeText(MainActivity.this, "连接成功", Toast.LENGTH_SHORT).show();

            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // 检查是否打开了蓝牙
        if (mBleWrapper.isBtEnabled() == false) {
            //如果没有打开通知用户打开蓝牙
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, ENABLE_BT_REQUEST_ID);
            // 在 onActivityResult 中检查用户是否打开了蓝牙功能
        }

        // 初始化 BleWrapper 对象
        mBleWrapper.initialize();

        bluetoothDevices = new ArrayList<MyBluetoothDevice>();
        devicesAdapter = new DevicesAdapter(this, bluetoothDevices);
        lv_show.setAdapter(devicesAdapter);

        // 是否启动自动烧卖哦
        mScanning = true;
        // 添加扫描超时
        addScanningTimeout();
        //启动扫描
        mBleWrapper.startScanning();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mScanning = false;

        mBleWrapper.stopScanning();
        bluetoothDevices.clear();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // 检查用户是否同意打开蓝牙
        if (requestCode == ENABLE_BT_REQUEST_ID) {
            if (resultCode == Activity.RESULT_CANCELED) {
                btDisabled();
                return;
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_settings) {
            mBleWrapper.startScanning();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }


    /**
     * 添加设备到列表
     *
     * @param device
     * @param rssi
     * @param scanRecord
     */
    private void handleFoundDevice(final BluetoothDevice device,
                                   final int rssi,
                                   final byte[] scanRecord) {


//        if (device.getName().equals("Misdk1")) {
        if (true) {
//            final ParsedAd parsedAd = ParsedAd.parseData(scanRecord);
//            Map<Integer, String> stringMap = BleUtils.ParseRecord(scanRecord);
//            Log.i("DEBUG", stringMap.toString());
//            Log.i("DEBUG", "======scanRecord:" + scanRecord.toString());
//            String msg = "msg======scanRecord:";
//            if (scanRecord != null && scanRecord.length > 0) {
//                final StringBuilder stringBuilder = new StringBuilder(scanRecord.length);
//                for (byte byteChar : scanRecord)
//                    stringBuilder.append(String.format("%08X ", byteChar));
//                msg += stringBuilder.toString();
//            }
//            Log.i("DEBUG", msg);
//            Log.i("DEBUG", "parsedAd===scanRecord:" + parsedAd.toString());

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (bluetoothDevices.contains(device) == false) {
                        MyBluetoothDevice myBluetoothDevice = new MyBluetoothDevice(device, null);
                        bluetoothDevices.add(myBluetoothDevice);
                        devicesAdapter.notifyDataSetChanged();
                    }
                }
            });
        }

    }


    /**
     * 添加扫描超时
     */
    private void addScanningTimeout() {
        Runnable timeout = new Runnable() {
            @Override
            public void run() {
                if (mBleWrapper == null) return;
                mScanning = false;
                mBleWrapper.stopScanning();
                invalidateOptionsMenu();
            }
        };
        mHandler.postDelayed(timeout, SCANNING_TIMEOUT);
    }

    /**
     * 用户没有打开蓝牙
     */
    private void btDisabled() {
        Toast.makeText(this, "对不起，只有打开蓝牙才可以运行软件", Toast.LENGTH_LONG).show();
        finish();
    }

    /**
     * 此设备不支持蓝牙功能
     */
    private void bleMissing() {
        Toast.makeText(this, "此设备不支持蓝牙功能", Toast.LENGTH_LONG).show();
        finish();
    }

}
