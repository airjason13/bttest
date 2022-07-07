package com.example.bt_test;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import android.Manifest;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ToggleButton;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {
    private final static String TAG = MainActivity.class.getSimpleName();
    BluetoothDevice mbtDevice;
    BluetoothAdapter mBluetoothAdapter;
    BluetoothLeScanner bluetoothlescanner;
    private final Map<String, BluetoothDevice> devices = new HashMap<>();
    String deviceaddress;
    BluetoothGatt mBluetoothGatt;
    public Button searchBtn;//搜尋藍芽裝置
    public ToggleButton openBtn;//啟動藍芽裝置

    Set<BluetoothDevice> bondDevices;
    Set<BluetoothDevice> remoteDevices;
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private BluetoothSocket mmSocket;
    private BluetoothDevice mmDevice;

    private final static int REQUEST_ENABLE_BT = 1;
    private static final int PERMISSION_REQUEST_COARSE_LOCATION = 1;
    private static final int PERMISSION_REQUEST_FINE_LOCATION = 10000;
    private static final int PERMISSION_REQUEST_BACKGROUND_LOCATION = 100;
    private static final int PERMISSION_REQUEST_ADVERTISE = 1000;
    UUID uuid;
    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Make sure we have access coarse location enabled, if not, prompt the user to enable it
        if (this.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            final AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("This app needs location access");
            builder.setMessage("Please grant location access so this app can detect peripherals.");
            builder.setPositiveButton(android.R.string.ok, null);
            builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                @Override
                public void onDismiss(DialogInterface dialog) {
                    requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, PERMISSION_REQUEST_COARSE_LOCATION);
                }
            });
            builder.show();
        }
        if (this.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            final AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("This app needs location access");
            builder.setMessage("Please grant location access so this app can detect peripherals.");
            builder.setPositiveButton(android.R.string.ok, null);
            builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                @Override
                public void onDismiss(DialogInterface dialog) {
                    requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSION_REQUEST_FINE_LOCATION);
                }
            });
            builder.show();
        }
        if (this.checkSelfPermission(Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            final AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("This app needs location access");
            builder.setMessage("Please grant location access so this app can detect peripherals.");
            builder.setPositiveButton(android.R.string.ok, null);
            builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                @Override
                public void onDismiss(DialogInterface dialog) {
                    requestPermissions(new String[]{Manifest.permission.ACCESS_BACKGROUND_LOCATION}, PERMISSION_REQUEST_BACKGROUND_LOCATION);
                }
            });
            builder.show();
        }
        if (this.checkSelfPermission(Manifest.permission.BLUETOOTH_ADVERTISE) != PackageManager.PERMISSION_GRANTED) {
            final AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("This app needs location access");
            builder.setMessage("Please grant location access so this app can detect peripherals.");
            builder.setPositiveButton(android.R.string.ok, null);
            builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                @Override
                public void onDismiss(DialogInterface dialog) {
                    requestPermissions(new String[]{Manifest.permission.BLUETOOTH_ADVERTISE}, PERMISSION_REQUEST_ADVERTISE);
                }
            });
            builder.show();
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (this.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, PERMISSION_REQUEST_COARSE_LOCATION);
            }
        }

        BluetoothManager manager = (BluetoothManager) this.getSystemService(Context.BLUETOOTH_SERVICE);
        if (manager == null) {
            Log.i(TAG, "No bt manager");
        }else{
            Log.i(TAG, "get bt manager");
        }
        mBluetoothAdapter = manager.getAdapter();
        if (!mBluetoothAdapter.isEnabled()) {
            Log.d(TAG, "Ask the user to turn on Bluetooth");

            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            //Next, judge in the onActivityResult callback
        }
        Log.i(TAG, "mBluetoothAdapter name : " + mBluetoothAdapter.getName());
        IntentFilter intent = new IntentFilter();
        intent.addAction(BluetoothDevice.ACTION_FOUND);// 用BroadcastReceiver來取得搜尋結果
        intent.addAction(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED); //每當掃描模式變化的時候，應用程式可以為通過ACTION_SCAN_MODE_CHANGED值來監聽全域性的訊息通知。比如，當裝置停止被搜尋以後，該訊息可以被系統通知給應用程式。
        intent.addAction(BluetoothAdapter.ACTION_STATE_CHANGED); //每當藍芽模組被開啟或者關閉，應用程式可以為通過ACTION_STATE_CHANGED值來監聽全域性的訊息通知。
        intent.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        intent.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        registerReceiver(searchReceiver, intent);




        openBtn = (ToggleButton)findViewById(R.id.btenable_tbtn);
        openBtn.setEnabled(true);
        openBtn.setActivated(false);
        //openBtn.setChecked(false);
        searchBtn = (Button)findViewById(R.id.search_btn);
        searchBtn.setOnClickListener(new View.OnClickListener() {
                                         @Override
                                         public void onClick(View v) {
                                        // TODO Auto-generated method stub
                                             if (mBluetoothAdapter.isDiscovering()) {
                                                 mBluetoothAdapter.cancelDiscovery();
                                             }
                                             bondDevices = mBluetoothAdapter.getBondedDevices();

                                             if (bondDevices.size() > 0) {
                                                 // There are paired devices. Get the name and address of each paired device.
                                                 for (BluetoothDevice device : bondDevices) {
                                                     String deviceName = device.getName();
                                                     String deviceHardwareAddress = device.getAddress(); // MAC address
                                                     Log.i(TAG, "deviceName : " + deviceName);
                                                     if(deviceName.contains("raspberry")){
                                                         deviceaddress = deviceHardwareAddress;
                                                     }
                                                 }
                                             }else {
                                                 Log.i(TAG, "no Bond devices" );
                                             }


                                             //mBluetoothAdapter.startDiscovery();
                                             enable_discovery();
                                         }
                                     });

        openBtn.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    // The toggle is enabled

                    mBluetoothAdapter.enable();
                    if (!mBluetoothAdapter.isEnabled()) {
                        Log.d(TAG, "Ask the user to turn on Bluetooth");

                        Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                        startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
                        //Next, judge in the onActivityResult callback
                    }
                } else {
                    // The toggle is disabled
                    mBluetoothAdapter.disable();
                }
            }
        });

        //mBluetoothAdapter.startDiscovery();
        //enable_discovery();

    }

    private void enable_discovery() {
        Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);

        //The second parameter can be set from 0 to 3600 seconds, which can be found in this time interval (window period)
        //Any value not in this range will be automatically set to 120 seconds.
        discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 1200);

        startActivity(discoverableIntent);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.i(TAG,"onActivityResult");
        if (requestCode == REQUEST_ENABLE_BT) {
            if (resultCode == RESULT_OK) {
                Log.d(TAG, "Open Bluetooth successfully!");
            }

            if (resultCode == RESULT_CANCELED) {
                Log.d(TAG, "Give up Bluetooth!");
            }

        } else {
            Log.d(TAG, "Bluetooth is abnormal!");
        }
    }

    // Create a BroadcastReceiver for ACTION_FOUND.
    private final BroadcastReceiver searchReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.i(TAG, "get intent, action :" + action);
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // Discovery has found a device. Get the BluetoothDevice
                // object and its info from the Intent.
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                String deviceName = device.getName();
                String deviceHardwareAddress = device.getAddress(); // MAC address
                if(deviceName == null) {
                    Log.i(TAG, "deviceName is null" );
                    return;
                }
                Log.i(TAG, "deviceName :" + deviceName);
                if(deviceName.contains("rasp")){
                    //mbtDevice = device;
                    ConnectThread connectThread = new ConnectThread(device);
                    connectThread.start();
                }

            }else if(BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)){
                Log.i(TAG, "get intent ACTION_DISCOVERY_FINISHED");
            }else if(BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action)){
                Log.i(TAG, "get intent ACTION_DISCOVERY_STARTED");
            }else if(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE.equals(action)){
                Log.i(TAG, "get intent ACTION_REQUEST_DISCOVERABLE");
            }else if(BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED.equals(action)) {
                Log.i(TAG, "get intent ACTION_CONNECTION_STATE_CHANGED");
            }else if(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED.equals(action)){
                Log.i(TAG, "scanmode :" + mBluetoothAdapter.getScanMode());
            }
        }
    };

    private class ConnectThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;

        public ConnectThread(BluetoothDevice device) {
            // Use a temporary object that is later assigned to mmSocket
            // because mmSocket is final.
            BluetoothSocket tmp = null;
            mmDevice = device;

            try {
                // Get a BluetoothSocket to connect with the given BluetoothDevice.
                // MY_UUID is the app's UUID string, also used in the server code.
                tmp = device.createRfcommSocketToServiceRecord(MY_UUID);
            } catch (IOException e) {
                Log.e(TAG, "Socket's create() method failed", e);
            }
            mmSocket = tmp;
        }

        public void run() {
            // Cancel discovery because it otherwise slows down the connection.
            mBluetoothAdapter.cancelDiscovery();

            try {
                // Connect to the remote device through the socket. This call blocks
                // until it succeeds or throws an exception.
                mmSocket.connect();
            } catch (IOException connectException) {
                // Unable to connect; close the socket and return.
                Log.i(TAG,"connect fail");
                try {
                    mmSocket.close();
                } catch (IOException closeException) {
                    Log.e(TAG, "Could not close the client socket", closeException);
                }
                return;
            }

            // The connection attempt succeeded. Perform work associated with
            // the connection in a separate thread.
            //manageMyConnectedSocket(mmSocket);
        }

        // Closes the client socket and causes the thread to finish.
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "Could not close the client socket", e);
            }
        }
    }


    @Override
    protected void onResume() {
        Log.i(TAG,"onResume");
        mBluetoothAdapter.startDiscovery();

        super.onResume();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();


        // Don't forget to unregister the ACTION_FOUND receiver.
        unregisterReceiver(searchReceiver);
    }





}