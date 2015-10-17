/**************************************************************************************************
  Filename:       BluetoothLeService.java
  Revised:        $Date: Wed Apr 22 13:01:34 2015 +0200$
  Revision:       $Revision: 599e5650a33a4a142d060c959561f9e9b0d88146$

  Copyright (c) 2013 - 2014 Texas Instruments Incorporated

  All rights reserved not granted herein.
  Limited License. 

  Texas Instruments Incorporated grants a world-wide, royalty-free,
  non-exclusive license under copyrights and patents it now or hereafter
  owns or controls to make, have made, use, import, offer to sell and sell ("Utilize")
  this software subject to the terms herein.  With respect to the foregoing patent
  license, such license is granted  solely to the extent that any such patent is necessary
  to Utilize the software alone.  The patent license shall not apply to any combinations which
  include this software, other than combinations with devices manufactured by or for TI ('TI Devices').
  No hardware patent is licensed hereunder.

  Redistributions must preserve existing copyright notices and reproduce this license (including the
  above copyright notice and the disclaimer and (if applicable) source code license limitations below)
  in the documentation and/or other materials provided with the distribution

  Redistribution and use in binary form, without modification, are permitted provided that the following
  conditions are met:

    * No reverse engineering, decompilation, or disassembly of this software is permitted with respect to any
      software provided in binary form.
    * any redistribution and use are licensed by TI for use only with TI Devices.
    * Nothing shall obligate TI to provide you with source code for the software licensed and provided to you in object code.

  If software source code is provided to you, modification and redistribution of the source code are permitted
  provided that the following conditions are met:

    * any redistribution and use of the source code, including any resulting derivative works, are licensed by
      TI for use only with TI Devices.
    * any redistribution and use of any object code compiled from the source code and any resulting derivative
      works, are licensed by TI for use only with TI Devices.

  Neither the name of Texas Instruments Incorporated nor the names of its suppliers may be used to endorse or
  promote products derived from this software without specific prior written permission.

  DISCLAIMER.

  THIS SOFTWARE IS PROVIDED BY TI AND TI'S LICENSORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING,
  BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
  IN NO EVENT SHALL TI AND TI'S LICENSORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
  CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA,
  OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
  OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
  POSSIBILITY OF SUCH DAMAGE.


 **************************************************************************************************/
package com.zsl.bluetoothdemo.utils.ble.oad;

import android.app.Service;
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
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import java.lang.reflect.Method;
import java.util.LinkedList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

// import android.util.Log;

/**
 * Service for managing connection and data communication with a GATT server
 * hosted on a given Bluetooth LE device.
 */
public class BluetoothLeService extends Service {
	static final String TAG = "BluetoothLeService";
    public static final UUID CLIENT_CHARACTERISTIC_CONFIG = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");
	public final static String ACTION_GATT_CONNECTED = "com.zsl.bluetoothdemo.utils.ble.oad.ACTION_GATT_CONNECTED";
	public final static String ACTION_GATT_DISCONNECTED = "com.zsl.bluetoothdemo.utils.ble.oad.ACTION_GATT_DISCONNECTED";
	public final static String ACTION_GATT_SERVICES_DISCOVERED = "com.zsl.bluetoothdemo.utils.ble.oad.ACTION_GATT_SERVICES_DISCOVERED";
	public final static String ACTION_DATA_READ = "com.zsl.bluetoothdemo.utils.ble.oad.ACTION_DATA_READ";
	public final static String ACTION_DATA_NOTIFY = "com.zsl.bluetoothdemo.utils.ble.oad.ACTION_DATA_NOTIFY";
	public final static String ACTION_DATA_WRITE = "com.zsl.bluetoothdemo.utils.ble.oad.ACTION_DATA_WRITE";
	public final static String EXTRA_DATA = "com.zsl.bluetoothdemo.utils.ble.oad.EXTRA_DATA";
	public final static String EXTRA_UUID = "com.zsl.bluetoothdemo.utils.ble.oad.EXTRA_UUID";
	public final static String EXTRA_STATUS = "com.zsl.bluetoothdemo.utils.ble.oad.EXTRA_STATUS";
	public final static String EXTRA_ADDRESS = "com.zsl.bluetoothdemo.utils.ble.oad.EXTRA_ADDRESS";
    public final static int GATT_TIMEOUT = 150;

	// BLE
	private BluetoothManager mBluetoothManager = null;
	private BluetoothAdapter mBtAdapter = null;
	private BluetoothGatt mBluetoothGatt = null;
	private static BluetoothLeService mThis = null;
	private String mBluetoothDeviceAddress;

	public Timer disconnectionTimer;
    private final Lock lock = new ReentrantLock();

    private volatile boolean blocking = false;
    private volatile int lastGattStatus = 0; //Success

    private volatile bleRequest curBleRequest = null;

    public enum bleRequestOperation {
        wrBlocking,
        wr,
        rdBlocking,
        rd,
        nsBlocking,
    }

    public enum bleRequestStatus {
        not_queued,
        queued,
        processing,
        timeout,
        done,
        no_such_request,
        failed,
    }

    public class bleRequest {
        public int id;
        public BluetoothGattCharacteristic characteristic;
        public bleRequestOperation operation;
        public volatile bleRequestStatus status;
        public int timeout;
        public int curTimeout;
        public boolean notifyenable;
    }

    // Queuing for fast application response.
    private volatile LinkedList<bleRequest> procQueue;
    private volatile LinkedList<bleRequest> nonBlockQueue;

    //

	/**
	 * GATT client callbacks
	 */
	private BluetoothGattCallback mGattCallbacks = new BluetoothGattCallback() {

		@Override
		public void onConnectionStateChange(BluetoothGatt gatt, int status,
		    int newState) {
			if (mBluetoothGatt == null) {
				// Log.e(TAG, "mBluetoothGatt not created!");
				return;
			}

			BluetoothDevice device = gatt.getDevice();
			String address = device.getAddress();
			// Log.d(TAG, "onConnectionStateChange (" + address + ") " + newState +
			// " status: " + status);

			try {
				switch (newState) {
				case BluetoothProfile.STATE_CONNECTED:
                    //refreshDeviceCache(mBluetoothGatt);
					broadcastUpdate(ACTION_GATT_CONNECTED, address, status);
					break;
				case BluetoothProfile.STATE_DISCONNECTED:
					broadcastUpdate(ACTION_GATT_DISCONNECTED, address, status);
					break;
				default:
					// Log.e(TAG, "New state not processed: " + newState);
					break;
				}
			} catch (NullPointerException e) {
				e.printStackTrace();
			}
		}

		@Override
		public void onServicesDiscovered(BluetoothGatt gatt, int status) {
			BluetoothDevice device = gatt.getDevice();
			broadcastUpdate(ACTION_GATT_SERVICES_DISCOVERED, device.getAddress(),
			    status);
		}

		@Override
		public void onCharacteristicChanged(BluetoothGatt gatt,
		    BluetoothGattCharacteristic characteristic) {
			broadcastUpdate(ACTION_DATA_NOTIFY, characteristic,
			    BluetoothGatt.GATT_SUCCESS);
		}

		@Override
		public void onCharacteristicRead(BluetoothGatt gatt,
		    BluetoothGattCharacteristic characteristic, int status) {
            if (blocking)unlockBlockingThread(status);
            if (nonBlockQueue.size() > 0) {
                lock.lock();
                for (int ii = 0; ii < nonBlockQueue.size(); ii++) {
                    bleRequest req = nonBlockQueue.get(ii);
                    if (req.characteristic == characteristic) {
                        req.status = bleRequestStatus.done;
                        nonBlockQueue.remove(ii);
                        break;
                    }
                }
                lock.unlock();
            }
			broadcastUpdate(ACTION_DATA_READ, characteristic, status);
		}

		@Override
		public void onCharacteristicWrite(BluetoothGatt gatt,
		    BluetoothGattCharacteristic characteristic, int status) {
            if (blocking)unlockBlockingThread(status);
            if (nonBlockQueue.size() > 0) {
                lock.lock();
                for (int ii = 0; ii < nonBlockQueue.size(); ii++) {
                    bleRequest req = nonBlockQueue.get(ii);
                    if (req.characteristic == characteristic) {
                        req.status = bleRequestStatus.done;
                        nonBlockQueue.remove(ii);
                        break;
                    }
                }
                lock.unlock();
            }
			broadcastUpdate(ACTION_DATA_WRITE, characteristic, status);
		}

		@Override
		public void onDescriptorRead(BluetoothGatt gatt,
		    BluetoothGattDescriptor descriptor, int status) {
            if (blocking)unlockBlockingThread(status);
            unlockBlockingThread(status);
		}

		@Override
		public void onDescriptorWrite(BluetoothGatt gatt,
		    BluetoothGattDescriptor descriptor, int status) {
            if (blocking)unlockBlockingThread(status);
			// Log.i(TAG, "onDescriptorWrite: " + descriptor.getUuid().toString());
		}
	};

    private void unlockBlockingThread(int status) {
        this.lastGattStatus = status;
        this.blocking = false;
    }

	private void broadcastUpdate(final String action, final String address,
	    final int status) {
		final Intent intent = new Intent(action);
		intent.putExtra(EXTRA_ADDRESS, address);
		intent.putExtra(EXTRA_STATUS, status);
		sendBroadcast(intent);
	}

	private void broadcastUpdate(final String action,
	    final BluetoothGattCharacteristic characteristic, final int status) {
		final Intent intent = new Intent(action);
		intent.putExtra(EXTRA_UUID, characteristic.getUuid().toString());
		intent.putExtra(EXTRA_DATA, characteristic.getValue());
		intent.putExtra(EXTRA_STATUS, status);
		sendBroadcast(intent);
	}

	public boolean checkGatt() {
		if (mBtAdapter == null) {
			// Log.w(TAG, "BluetoothAdapter not initialized");
			return false;
		}
		if (mBluetoothGatt == null) {
			// Log.w(TAG, "BluetoothGatt not initialized");
			return false;
		}
        if (this.blocking) {
            Log.d(TAG, "Cannot start operation : Blocked");
            return false;
        }
		return true;

	}

	/**
	 * Manage the BLE service
	 */
	public class LocalBinder extends Binder {
		public BluetoothLeService getService() {
			return BluetoothLeService.this;
		}
	}

	@Override
	public IBinder onBind(Intent intent) {
		return binder;
	}

	@Override
	public boolean onUnbind(Intent intent) {
		// After using a given device, you should make sure that
		// BluetoothGatt.close() is called
		// such that resources are cleaned up properly. In this particular example,
		// close() is
		// invoked when the UI is disconnected from the Service.
		close();
		return super.onUnbind(intent);
	}

	private final IBinder binder = new LocalBinder();

	/**
	 * Initializes a reference to the local Bluetooth adapter.
	 * 
	 * @return Return true if the initialization is successful.
	 */
	public boolean initialize() {
		// For API level 18 and above, get a reference to BluetoothAdapter through
		// BluetoothManager.
		mThis = this;
		if (mBluetoothManager == null) {
			mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
			if (mBluetoothManager == null) {
				// Log.e(TAG, "Unable to initialize BluetoothManager.");
				return false;
			}
		}

		mBtAdapter = mBluetoothManager.getAdapter();
		if (mBtAdapter == null) {
			// Log.e(TAG, "Unable to obtain a BluetoothAdapter.");
			return false;
		}

        procQueue = new LinkedList<bleRequest>();
        nonBlockQueue = new LinkedList<bleRequest>();


        Thread queueThread = new Thread() {
            @Override
            public void run() {
                while (true) {
                    executeQueue();
                    try {
                        Thread.sleep(0, 100000);
                    }
                    catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        };

        queueThread.start();
		return true;
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		// Log.i(TAG, "Received start id " + startId + ": " + intent);
		// We want this service to continue running until it is explicitly
		// stopped, so return sticky.
        this.initialize();
		return START_STICKY;
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		if (mBluetoothGatt != null) {
			mBluetoothGatt.close();
			mBluetoothGatt = null;
		}
	}

	//
	// GATT API
	//

	/**
	 * Request a read on a given {@code BluetoothGattCharacteristic}. The read
	 * result is reported asynchronously through the
	 * {@code BluetoothGattCallback#onCharacteristicRead(android.bluetooth.BluetoothGatt, android.bluetooth.BluetoothGattCharacteristic, int)}
	 * callback.
	 * 
	 * @param characteristic
	 *          The characteristic to read from.
	 */
	public int readCharacteristic(BluetoothGattCharacteristic characteristic) {
        bleRequest req = new bleRequest();
        req.status = bleRequestStatus.not_queued;
        req.characteristic = characteristic;
        req.operation = bleRequestOperation.rdBlocking;
        addRequestToQueue(req);
        boolean finished = false;
        while (!finished) {
            bleRequestStatus stat = pollForStatusofRequest(req);
            if (stat == bleRequestStatus.done) {
                finished = true;
                return 0;
            }
            else if (stat == bleRequestStatus.timeout) {
                finished = true;
                return -3;
            }
        }
        return -2;
	}

	public int writeCharacteristic(
	    BluetoothGattCharacteristic characteristic, byte b) {


        byte[] val = new byte[1];
        val[0] = b;
        characteristic.setValue(val);

        bleRequest req = new bleRequest();
        req.status = bleRequestStatus.not_queued;
        req.characteristic = characteristic;
        req.operation = bleRequestOperation.wrBlocking;
        addRequestToQueue(req);
        boolean finished = false;
        while (!finished) {
            bleRequestStatus stat = pollForStatusofRequest(req);
            if (stat == bleRequestStatus.done) {
                finished = true;
                return 0;
            }
            else if (stat == bleRequestStatus.timeout) {
                finished = true;
                return -3;
            }
        }
        return -2;
	}
	public int writeCharacteristic(
		    BluetoothGattCharacteristic characteristic, byte[] b) {
        characteristic.setValue(b);
        bleRequest req = new bleRequest();
        req.status = bleRequestStatus.not_queued;
        req.characteristic = characteristic;
        req.operation = bleRequestOperation.wrBlocking;
        addRequestToQueue(req);
        boolean finished = false;
        while (!finished) {
            bleRequestStatus stat = pollForStatusofRequest(req);
            if (stat == bleRequestStatus.done) {
                finished = true;
                return 0;
            }
            else if (stat == bleRequestStatus.timeout) {
                finished = true;
                return -3;
            }
        }
        return -2;
		}
	public int writeCharacteristic(BluetoothGattCharacteristic characteristic) {
        bleRequest req = new bleRequest();
        req.status = bleRequestStatus.not_queued;
        req.characteristic = characteristic;
        req.operation = bleRequestOperation.wrBlocking;
        addRequestToQueue(req);
        boolean finished = false;
        while (!finished) {
            bleRequestStatus stat = pollForStatusofRequest(req);
            if (stat == bleRequestStatus.done) {
                finished = true;
                return 0;
            }
            else if (stat == bleRequestStatus.timeout) {
                finished = true;
                return -3;
            }
        }
        return -2;
	}

    public boolean writeCharacteristicNonBlock(BluetoothGattCharacteristic characteristic) {
        bleRequest req = new bleRequest();
        req.status = bleRequestStatus.not_queued;
        req.characteristic = characteristic;
        req.operation = bleRequestOperation.wr;
        addRequestToQueue(req);
        return true;
    }

	/**
	 * Retrieves the number of GATT services on the connected device. This should
	 * be invoked only after {@code BluetoothGatt#discoverServices()} completes
	 * successfully.
	 * 
	 * @return A {@code integer} number of supported services.
	 */
	public int getNumServices() {
		if (mBluetoothGatt == null)
			return 0;

		return mBluetoothGatt.getServices().size();
	}

	/**
	 * Retrieves a list of supported GATT services on the connected device. This
	 * should be invoked only after {@code BluetoothGatt#discoverServices()}
	 * completes successfully.
	 * 
	 * @return A {@code List} of supported services.
	 */
	public List<BluetoothGattService> getSupportedGattServices() {
		if (mBluetoothGatt == null)
			return null;

		return mBluetoothGatt.getServices();
	}

	/**
	 * Enables or disables notification on a give characteristic.
	 * 
	 * @param characteristic
	 *          Characteristic to act on.
	 * @param enable
	 *          If true, enable notification. False otherwise.
	 */
	public int setCharacteristicNotification(
	    BluetoothGattCharacteristic characteristic, boolean enable) {
        bleRequest req = new bleRequest();
        req.status = bleRequestStatus.not_queued;
        req.characteristic = characteristic;
        req.operation = bleRequestOperation.nsBlocking;
        req.notifyenable = enable;
        addRequestToQueue(req);
        boolean finished = false;
        while (!finished) {
            bleRequestStatus stat = pollForStatusofRequest(req);
            if (stat == bleRequestStatus.done) {
                finished = true;
                return 0;
            }
            else if (stat == bleRequestStatus.timeout) {
                finished = true;
                return -3;
            }
        }
        return -2;
	}

	public boolean isNotificationEnabled(
	    BluetoothGattCharacteristic characteristic) {
        if (characteristic == null) {
            return false;
        }
		if (!checkGatt())
			return false;

		BluetoothGattDescriptor clientConfig = characteristic
		    .getDescriptor(CLIENT_CHARACTERISTIC_CONFIG);
		if (clientConfig == null)
			return false;

		return clientConfig.getValue() == BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE;
	}

	/**
	 * Connects to the GATT server hosted on the Bluetooth LE device.
	 * 
	 * @param address
	 *          The device address of the destination device.
	 * 
	 * @return Return true if the connection is initiated successfully. The
	 *         connection result is reported asynchronously through the
	 *         {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
	 *         callback.
	 */
	public boolean connect(final String address) {
		if (mBtAdapter == null || address == null) {
			// Log.w(TAG, "BluetoothAdapter not initialized or unspecified address.");
			return false;
		}
		final BluetoothDevice device = mBtAdapter.getRemoteDevice(address);
		int connectionState = mBluetoothManager.getConnectionState(device,
		    BluetoothProfile.GATT);

		if (connectionState == BluetoothProfile.STATE_DISCONNECTED) {

			// Previously connected device. Try to reconnect.
			if (mBluetoothDeviceAddress != null
			    && address.equals(mBluetoothDeviceAddress) && mBluetoothGatt != null) {
				// Log.d(TAG, "Re-use GATT connection");
				if (mBluetoothGatt.connect()) {
					return true;
				} else {
					// Log.w(TAG, "GATT re-connect failed.");
					return false;
				}
			}

			if (device == null) {
				// Log.w(TAG, "Device not found.  Unable to connect.");
				return false;
			}
			// We want to directly connect to the device, so we are setting the
			// autoConnect parameter to false.
			// Log.d(TAG, "Create a new GATT connection.");
			mBluetoothGatt = device.connectGatt(this, false, mGattCallbacks);
			mBluetoothDeviceAddress = address;
		} else {
			// Log.w(TAG, "Attempt to connect in state: " + connectionState);
			return false;
		}
		return true;
	}

	/**
	 * Disconnects an existing connection or cancel a pending connection. The
	 * disconnection result is reported asynchronously through the
	 * {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
	 * callback.
	 */
	public void disconnect(String address) {
		if (mBtAdapter == null) {
			// Log.w(TAG, "disconnect: BluetoothAdapter not initialized");
			return;
		}
		final BluetoothDevice device = mBtAdapter.getRemoteDevice(address);
		int connectionState = mBluetoothManager.getConnectionState(device,
		    BluetoothProfile.GATT);

		if (mBluetoothGatt != null) {
			if (connectionState != BluetoothProfile.STATE_DISCONNECTED) {
				mBluetoothGatt.disconnect();
			} else {
				// Log.w(TAG, "Attempt to disconnect in state: " + connectionState);
			}
		}
	}

	/**
	 * After using a given BLE device, the app must call this method to ensure
	 * resources are released properly.
	 */
	public void close() {
		if (mBluetoothGatt != null) {
			// Log.i(TAG, "close");
			mBluetoothGatt.close();
			mBluetoothGatt = null;
		}
  }

	public int numConnectedDevices() {
		int n = 0;

		if (mBluetoothGatt != null) {
			List<BluetoothDevice> devList;
			devList = mBluetoothManager.getConnectedDevices(BluetoothProfile.GATT);
			n = devList.size();
		}
		return n;
	}

	//
	// Utility functions
	//
	public static BluetoothGatt getBtGatt() {
		return mThis.mBluetoothGatt;
	}

	public static BluetoothManager getBtManager() {
		return mThis.mBluetoothManager;
	}

	public static BluetoothLeService getInstance() {
		return mThis;
	}

    public void waitIdle(int timeout) {
        while (timeout-- > 0) {
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

	public boolean refreshDeviceCache(BluetoothGatt gatt){
	    try {
	        BluetoothGatt localBluetoothGatt = gatt;
	        Method localMethod = localBluetoothGatt.getClass().getMethod("refresh", new Class[0]);
	        if (localMethod != null) {
	           boolean bool = ((Boolean) localMethod.invoke(localBluetoothGatt, new Object[0])).booleanValue();
	            return bool;
	         }
	    } 
	    catch (Exception localException) {
	        Log.e(TAG, "An exception occured while refreshing device");
	    }
	    return false;
	}
	
	public void timedDisconnect() {
		disconnectTimerTask disconnectionTimerTask;
		this.disconnectionTimer = new Timer();
		disconnectionTimerTask = new disconnectTimerTask(this);
		this.disconnectionTimer.schedule(disconnectionTimerTask, 20000);
	}
	public void abortTimedDisconnect() {
		if (this.disconnectionTimer != null) {
			this.disconnectionTimer.cancel();
		}
	}
	class disconnectTimerTask extends TimerTask {
		BluetoothLeService param;

	     public disconnectTimerTask(final BluetoothLeService param) {
	    	 this.param = param;
	     }

	     @Override
	     public void run() {
	    	 this.param.disconnect(mBluetoothDeviceAddress);
	     }
	}

    public boolean requestConnectionPriority(int connectionPriority) {
        return this.mBluetoothGatt.requestConnectionPriority(connectionPriority);
    }

    public boolean addRequestToQueue(bleRequest req) {
        lock.lock();
        if (procQueue.peekLast() != null) {
            req.id = procQueue.peek().id++;
        }
        else {
            req.id = 0;
            procQueue.add(req);
        }
        lock.unlock();
        return true;
    }

    public bleRequestStatus pollForStatusofRequest(bleRequest req) {
        lock.lock();
        if (req == curBleRequest) {
            bleRequestStatus stat = curBleRequest.status;
            if (stat == bleRequestStatus.done) {
                curBleRequest = null;
            }
            if (stat == bleRequestStatus.timeout) {
                curBleRequest = null;
            }
            lock.unlock();
            return stat;
        }
        else {
            lock.unlock();
            return bleRequestStatus.no_such_request;
        }
    }
    private void executeQueue() {
        // Everything here is done on the queue
        lock.lock();
        if (curBleRequest != null) {
                Log.d(TAG, "executeQueue, curBleRequest running");
                try {
                    curBleRequest.curTimeout++;
                    if (curBleRequest.curTimeout > GATT_TIMEOUT) {
                        curBleRequest.status = bleRequestStatus.timeout;
                        curBleRequest = null;
                    }
                    Thread.sleep(10, 0);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            lock.unlock();
                return;
        }
        if (procQueue == null) {
            lock.unlock();
            return;
        }
        if (procQueue.size() == 0) {
            lock.unlock();
            return;
        }
        bleRequest procReq = procQueue.removeFirst();

        switch (procReq.operation) {
            case rd:
                //Read, do non blocking read
                break;
            case rdBlocking:
                //Normal (blocking) read
                if (procReq.timeout == 0) {
                    procReq.timeout = GATT_TIMEOUT;
                }
                procReq.curTimeout = 0;
                curBleRequest = procReq;
                int stat = sendBlockingReadRequest(procReq);
                if (stat == -2) {
                    Log.d(TAG, "executeQueue rdBlocking: error, BLE was busy or device disconnected");
                    lock.unlock();
                    return;
                }
                break;
            case wr:
                //Write, do non blocking write (Ex: OAD)
                nonBlockQueue.add(procReq);
                sendNonBlockingWriteRequest(procReq);
                break;
            case wrBlocking:
                //Normal (blocking) write
                if (procReq.timeout == 0) {
                    procReq.timeout = GATT_TIMEOUT;
                }
                curBleRequest = procReq;
                stat = sendBlockingWriteRequest(procReq);
                if (stat == -2) {
                    Log.d(TAG, "executeQueue wrBlocking: error, BLE was busy or device disconnected");
                    lock.unlock();
                    return;
                }
                break;
            case nsBlocking:
                if (procReq.timeout == 0) {
                    procReq.timeout = GATT_TIMEOUT;
                }
                curBleRequest = procReq;
                stat = sendBlockingNotifySetting(procReq);
                if (stat == -2) {
                    Log.d(TAG, "executeQueue nsBlocking: error, BLE was busy or device disconnected");
                    lock.unlock();
                    return;
                }
                break;
            default:
                break;

        }
        lock.unlock();
    }

    public int sendNonBlockingReadRequest(bleRequest request) {
        request.status = bleRequestStatus.processing;
        if (!checkGatt()) {
            request.status = bleRequestStatus.failed;
            return -2;
        }
        mBluetoothGatt.readCharacteristic(request.characteristic);
        return 0;
    }

    public int sendNonBlockingWriteRequest(bleRequest request) {
        request.status = bleRequestStatus.processing;
        if (!checkGatt()) {
            request.status = bleRequestStatus.failed;
            return -2;
        }
        mBluetoothGatt.writeCharacteristic(request.characteristic);
        return 0;
    }

    public int sendBlockingReadRequest(bleRequest request) {
        request.status = bleRequestStatus.processing;
        int timeout = 0;
        if (!checkGatt()) {
            request.status = bleRequestStatus.failed;
            return -2;
        }
        mBluetoothGatt.readCharacteristic(request.characteristic);
        this.blocking = true; // Set read to be blocking
        while (this.blocking) {
            timeout ++;
            waitIdle(1);
            if (timeout > GATT_TIMEOUT) {this.blocking = false; request.status = bleRequestStatus.timeout; return -1;}  //Read failed TODO: Fix this to follow connection interval !
        }
        request.status = bleRequestStatus.done;
        return lastGattStatus;
    }

    public int sendBlockingWriteRequest(bleRequest request) {
        request.status = bleRequestStatus.processing;
        int timeout = 0;
        if (!checkGatt()) {
            request.status = bleRequestStatus.failed;
            return -2;
        }
        mBluetoothGatt.writeCharacteristic(request.characteristic);
        this.blocking = true; // Set read to be blocking
        while (this.blocking) {
            timeout ++;
            waitIdle(1);
            if (timeout > GATT_TIMEOUT) {this.blocking = false; request.status = bleRequestStatus.timeout; return -1;}  //Read failed TODO: Fix this to follow connection interval !
        }
        request.status = bleRequestStatus.done;
        return lastGattStatus;
    }
    public int sendBlockingNotifySetting(bleRequest request) {
        request.status = bleRequestStatus.processing;
        int timeout = 0;
        if (request.characteristic == null) {
            return -1;
        }
        if (!checkGatt())
            return -2;

        if (mBluetoothGatt.setCharacteristicNotification(request.characteristic, request.notifyenable)) {

            BluetoothGattDescriptor clientConfig = request.characteristic
                    .getDescriptor(CLIENT_CHARACTERISTIC_CONFIG);
            if (clientConfig != null) {

                if (request.notifyenable) {
                    // Log.i(TAG, "Enable notification: " +
                    // characteristic.getUuid().toString());
                    clientConfig
                            .setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                } else {
                    // Log.i(TAG, "Disable notification: " +
                    // characteristic.getUuid().toString());
                    clientConfig
                            .setValue(BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE);
                }
                mBluetoothGatt.writeDescriptor(clientConfig);
                // Log.i(TAG, "writeDescriptor: " +
                // characteristic.getUuid().toString());
                this.blocking = true; // Set read to be blocking
                while (this.blocking) {
                    timeout ++;
                    waitIdle(1);
                    if (timeout > GATT_TIMEOUT) {this.blocking = false; request.status = bleRequestStatus.timeout; return -1;}  //Read failed TODO: Fix this to follow connection interval !
                }
                request.status = bleRequestStatus.done;
                return lastGattStatus;
            }
        }
        return -3; // Set notification to android was wrong ...
    }
}
