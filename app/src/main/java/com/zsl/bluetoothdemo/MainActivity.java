package com.zsl.bluetoothdemo;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
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
    DevicesAdapter devicesAdapter;

    ListView lv_show;
    Button bt_state;
    //    是否启动自动扫描
    private boolean mScanning = false;
    //    handler
    private Handler mHandler = new Handler();


    //蓝牙管理
    private static BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBtAdapter = null;
    private BluetoothDevice mBluetoothDevice = null;
    private BluetoothLeService mBluetoothLeService = null;

    private boolean mBtAdapterEnabled = false;
    private boolean mBleSupported = true;
    private boolean mInitialised = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();
        initData();

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


    private void initData() {
        bluetoothDevices = new ArrayList<MyBluetoothDevice>();
        devicesAdapter = new DevicesAdapter(this, bluetoothDevices);
        lv_show.setAdapter(devicesAdapter);

    }



    @Override
    protected void onResume() {
        super.onResume();
        scanLeDevice(true);

    }

    @Override
    protected void onPause() {
        super.onPause();
        scanLeDevice(false);
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
            if (!mInitialised) {
                mBluetoothLeService = BluetoothLeService.getInstance();
                mBluetoothManager = mBluetoothLeService.getBtManager();
                mBtAdapter = mBluetoothManager.getAdapter();
                mBtAdapterEnabled = mBtAdapter.isEnabled();
                    Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivityForResult(enableIntent, ENABLE_BT_REQUEST_ID);
                }
                mInitialised = true;
            }
            if (!mBtAdapterEnabled) {
            scanLeDevice(true);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }



    /**
     * 添加扫描超时
     */
    private void addScanningTimeout() {
        Runnable timeout = new Runnable() {
            @Override
            public void run() {

            }
        };
        mHandler.postDelayed(timeout, SCANNING_TIMEOUT);
    }

    /**
     * 扫描蓝牙设备
     * @param enable
     * @return
     */
    private boolean scanLeDevice(boolean enable) {
        if (mBtAdapter==null){
            return false;
        }
        if (enable) {
            mScanning = mBtAdapter.startLeScan(mLeScanCallback);
        } else {
            mScanning = false;
            mBtAdapter.stopLeScan(mLeScanCallback);
        }
        return mScanning;
    }

    /**
     * 扫描回调
     */
    private BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback() {

        public void onLeScan(final BluetoothDevice device, final int rssi,
                             byte[] scanRecord) {
            runOnUiThread(new Runnable() {
                public void run() {
                    if (!deviceInfoExists(device.getAddress())) {
                        // 新设备
                        MyBluetoothDevice myBluetoothDevice = new MyBluetoothDevice(device, null, device.getAddress());
                        bluetoothDevices.add(myBluetoothDevice);
                        devicesAdapter.notifyDataSetChanged();
                    } else {
                        String address =device.getAddress();
                        for (MyBluetoothDevice bluetoothDevice : bluetoothDevices) {
                            if (bluetoothDevice.getAddress().equals(address)) {
                                bluetoothDevice.setAddress(address);
                                devicesAdapter.notifyDataSetChanged();
                            }
                        }
                    }
                }
//				}

            });
        }
    };

    /**
     * 列表中是否存在此设备
     * @param address
     * @return
     */
    private boolean deviceInfoExists(String address) {
        for (int i = 0; i < bluetoothDevices.size(); i++) {
            if (bluetoothDevices.get(i).getBluetoothDevice().getAddress()
                    .equals(address)) {
                return true;
            }
        }
        return false;
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
