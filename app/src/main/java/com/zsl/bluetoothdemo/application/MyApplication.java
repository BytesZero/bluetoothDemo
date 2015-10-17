package com.zsl.bluetoothdemo.application;

import android.app.Application;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;

import com.bugtags.library.Bugtags;
import com.zsl.bluetoothdemo.utils.ble.oad.BluetoothLeService;

/**
 * Created by zsl on 15/9/22.
 */
public class MyApplication extends Application {
    private BluetoothLeService mBluetoothLeService = null;

    @Override
    public void onCreate() {
        startBluetoothLeService();
        super.onCreate();
        //BugTags
        Bugtags.start("ec53c69dfe79cdb66a978138716eb32e", this, Bugtags.BTGInvocationEventBubble);


    }
    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mBluetoothLeService = ((BluetoothLeService.LocalBinder) service)
                    .getService();
            if (!mBluetoothLeService.initialize()) {
                //Toast.makeText(context, "Unable to initialize BluetoothLeService", Toast.LENGTH_SHORT).show();
                //finish();
                return;
            }
            final int n = mBluetoothLeService.numConnectedDevices();
            if (n > 0) {
                /*
                runOnUiThread(new Runnable() {
                    public void run() {
                        mThis.setError("Multiple connections!");
                    }
                });
                */
            } else {
                //startScan();
                // Log.i(TAG, "BluetoothLeService connected");
            }
        }

        public void onServiceDisconnected(ComponentName componentName) {
            mBluetoothLeService = null;
            // Log.i(TAG, "BluetoothLeService disconnected");
        }
    };

    private void startBluetoothLeService() {
        Intent bindIntent = new Intent(this, BluetoothLeService.class);
        startService(bindIntent);
        bindService(bindIntent, mServiceConnection, Context.BIND_AUTO_CREATE);

    }
}
