package com.example.bt_test;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import android.Manifest;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.bluetooth.le.BluetoothLeScanner;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {
    private final static String TAG = MainActivity.class.getSimpleName();
    BluetoothDevice mbtDevice;
    BluetoothAdapter mBluetoothAdapter;
    BluetoothLeScanner bluetoothlescanner;
    //private final Map<String, BluetoothDevice> devices = new HashMap<>();

    public Button searchBtn;//搜尋藍芽裝置
    public Button sendWifiInfoBtn;//傳送wifi 資訊
    public Button setSSIDDefaultBtn;//設定SSID為開機預設
    public Button setHotspotDefaultBtn;//設定SSID為開機預設
    public ToggleButton openBtn;//啟動藍芽裝置

    List<BluetoothDevice> searchedDevices= new ArrayList<BluetoothDevice>();
    private static final UUID MY_UUID = UUID.fromString("00000001-0000-1000-8000-00805F9B34FB");

    ConnectThread connectThread;

    //private ListAdapter mAdapter;
    private ArrayAdapter mAdapter;
    private List<String> devices_info;
    EditText ssidEdittext;
    EditText pwdEdittext;
    private final static int REQUEST_ENABLE_BT = 1;
    private static final int PERMISSION_REQUEST_COARSE_LOCATION = 1;
    private static final int PERMISSION_REQUEST_FINE_LOCATION = 10000;
    private static final int PERMISSION_REQUEST_BACKGROUND_LOCATION = 100;
    private static final int PERMISSION_REQUEST_ADVERTISE = 1000;
    private static final int PERMISSION_REQUEST_CONNECT = 1001;
    private InputStream is = null;
    private OutputStream os = null;
    TextView Status_tv;
    private static final int Status_Update = 1;
    private static final int DeviceInfo_Update = 2;
    private static final int Toast_Show = 3;

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        request_permission();

        ssidEdittext = (EditText)findViewById(R.id.SSIDeditTextText);
        pwdEdittext = (EditText)findViewById(R.id.PWDeditText);

        devices_info = new ArrayList<>();

        mAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, android.R.id.text1, devices_info);
        ((ListView) findViewById(R.id.search_lv)).setAdapter(mAdapter);
        ((ListView) findViewById(R.id.search_lv)).setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Log.i(TAG,"listview clicked");
                Log.i(TAG,"test : " + mAdapter.getItem(position));
                for(int j = 0; j < searchedDevices.size() ; j ++) {
                    Log.i(TAG, "searchDevice :" + searchedDevices.get(j).getName());
                    Log.i(TAG, mAdapter.getItem(position).toString());

                    if(devices_info.get(position).contains(searchedDevices.get(j).getName())){
                        Log.i(TAG, "find match device");
                        if(devices_info.get(position).contains("disconnected")) {
                            if (connectThread == null) {
                                connectThread = new ConnectThread(searchedDevices.get(j));
                                connectThread.start();
                            } else {
                                if (connectThread.isAlive() == false){
                                    connectThread.cancel();
                                    connectThread = null;
                                    connectThread = new ConnectThread(searchedDevices.get(j));
                                    connectThread.start();
                                }
                            }
                        }else {
                            if (connectThread != null) {
                                connectThread.cancel();
                                connectThread = null;
                                Log.i(TAG,"connectThread :" + connectThread);
                            }
                        }
                    }
                }
            }
        });


        ((ListView) findViewById(R.id.search_lv)).setHorizontalScrollBarEnabled(true);

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

        Set<BluetoothDevice> pairedDevices = BluetoothAdapter.getDefaultAdapter().getBondedDevices();
        if (pairedDevices.size() > 0) {
            for (BluetoothDevice d: pairedDevices) {
                String deviceName = d.getName();
                String macAddress = d.getAddress();
                Log.i(TAG, "paired device: " + deviceName + " at " + macAddress);
                // do what you need/want this these list items
                searchedDevices.add(d);
                devices_info.add("Device name:" + d.getName() + "\n Status:disconnected");

            }
        }

        IntentFilter intent = new IntentFilter();
        intent.addAction(BluetoothDevice.ACTION_FOUND);// 用BroadcastReceiver來取得搜尋結果
        intent.addAction(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED);
        intent.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
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

                                             mBluetoothAdapter.startDiscovery();
                                             enable_discovery();
                                         }
                                     });
        sendWifiInfoBtn = (Button)findViewById(R.id.btn_sendwifiinfo);
        sendWifiInfoBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // TODO Auto-generated method stub
                String tmp_ssid = ssidEdittext.getText().toString().replace("\\s", "");
                String tmp_pwd = pwdEdittext.getText().toString().replace("\\s", "");
                String senddata = "SSID:" + ssidEdittext.getText().toString() + "," + "PWD:" + pwdEdittext.getText().toString();
                Log.i(TAG,"senddata : " + senddata);
                send(senddata);
            }
        });
        sendWifiInfoBtn.setEnabled(false);

        setSSIDDefaultBtn = (Button)findViewById(R.id.btn_SSID_default);
        setSSIDDefaultBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // TODO Auto-generated method stub
                String senddata = "SetDefault:" + ssidEdittext.getText().toString();
                Log.i(TAG,"senddata : " + senddata);
                send(senddata);
            }
        });
        setSSIDDefaultBtn.setEnabled(false);

        setHotspotDefaultBtn = (Button)findViewById(R.id.btn_Hotspot_Default);
        setHotspotDefaultBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // TODO Auto-generated method stub
                String senddata = "SetDefault:" + "Hotspot";
                Log.i(TAG,"senddata : " + senddata);
                send(senddata);
            }
        });
        setHotspotDefaultBtn.setEnabled(false);

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


        Status_tv = (TextView) findViewById(R.id.status_textView);
        Status_tv.setText("Status: Disconnect");
        //devices_info.set(3, "TEST") ;
        //mAdapter.notifyDataSetChanged();
    }

    private void enable_discovery() {
        Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);

        //The second parameter can be set from 0 to 3600 seconds, which can be found in this time interval (window period)
        //Any value not in this range will be automatically set to 120 seconds.
        discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 3600);

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

    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case Status_Update:
                    String status_info = msg.obj.toString();
                    Status_tv.setText("Status: " + status_info);

                    //mAdapter.getItem(2).replace("disconnected", "connected");
                    //mAdapter.notifyDataSetChanged();
                    if (status_info.contains("disconnected")) {
                        sendWifiInfoBtn.setEnabled(false);
                        setSSIDDefaultBtn.setEnabled(false);
                        setHotspotDefaultBtn.setEnabled(false);
                    }else{
                        sendWifiInfoBtn.setEnabled(true);
                        setSSIDDefaultBtn.setEnabled(true);
                        setHotspotDefaultBtn.setEnabled(true);
                    }
                    break;
                case DeviceInfo_Update:
                    String obj_info = msg.obj.toString();
                    Log.i(TAG,"DeviceInfo_Update");
                    Log.i(TAG,"info :" + obj_info);

                    String d_name = obj_info.split(",")[0].split(":")[1].trim();
                    String d_status = obj_info.split(",")[1].split(":")[1].trim();
                    Log.i(TAG,"d_name : " + d_name);
                    Log.i(TAG,"d_name : " + d_name.length());
                    Log.i(TAG,"devices_info.size() : " + devices_info.size());
                    for (int i = 0; i < devices_info.size() ; i ++){
                        Log.i(TAG,"devices_info.get(i) : " + devices_info.get(i));
                        if (devices_info.get(i).contains(d_name)){
                            Log.i(TAG,"bingo i = " + i );
                            devices_info.set(i, "Device name:" + d_name + "\n Status:" + d_status);
                            mAdapter.notifyDataSetChanged();
                        }
                    }
                    break;
                case Toast_Show:
                    String data = (String)msg.obj;
                    Toast toast = Toast.makeText(getApplicationContext(),data,Toast.LENGTH_LONG);
                    toast.setGravity(Gravity.TOP,0,0);
                    toast.show();
                    break;
                default:
                    break;
            }
        }
    };

    // Create a BroadcastReceiver for ACTION_FOUND.
    private final BroadcastReceiver searchReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
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


                for (int i = 0; i < searchedDevices.size() ; i ++){
                    if (searchedDevices.get(i).getName().contains(device.getName())){
                        Log.i(TAG,"device name already listed");
                        return;
                    }
                }
                if (device != null) {
                    //Adapter added to ListView.
                    searchedDevices.add(device);
                    //mAdapter.add("Device name:" + device.getName() + "\n Status:disconnected");
                    devices_info.add("Device name:" + device.getName() + "\n Status:disconnected");
                    mAdapter.notifyDataSetChanged();
                    for(int i = 0; i < mAdapter.getCount(); i++){
                        Log.i(TAG,"mAdapter content : " + mAdapter.getItem(i));
                    }
                    for(int j = 0; j < searchedDevices.size() ; j ++) {
                        Log.i(TAG, "searchDevice :" + searchedDevices.get(j).getName());
                    }
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
            BluetoothSocket socket = null;
            try {
                // Get a BluetoothSocket to connect with the given BluetoothDevice.
                // MY_UUID is the app's UUID string, also used in the server code.

                socket = device.createRfcommSocketToServiceRecord(MY_UUID);
                //tmp = device.createRfcommSocketToServiceRecord(MY_UUID);

            } catch (IOException e) {
                Log.e(TAG, "Socket's create() method failed", e);
            }
            mmSocket = socket;
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
                    // update Status
                    Message msg = new Message();
                    msg.what = Status_Update;
                    msg.obj = mmDevice.getName() + " BT comm disconnected";
                    mHandler.sendMessage(msg);

                    Message msg1 = new Message();
                    msg1.what = DeviceInfo_Update;

                    msg1.obj = "Device:" + mmDevice.getName() + ",Status:disconnected";
                    mHandler.sendMessage(msg1);
                } catch (IOException closeException) {
                    Log.e(TAG, "Could not close the client socket", closeException);
                }
                return;
            }
            // update Status
            Message msg = new Message();
            msg.what = Status_Update;
            msg.obj = mmDevice.getName() + " BT comm connected";
            mHandler.sendMessage(msg);

            Message msg1 = new Message();
            msg1.what = DeviceInfo_Update;

            msg1.obj = "Device:" + mmDevice.getName() + ",Status:connected";
            mHandler.sendMessage(msg1);


            Log.i(TAG,"CONNECT OK!");

            ReadThread readThread = new ReadThread(mmSocket);
            readThread.start();

            try {
                is = mmSocket.getInputStream();
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                os = mmSocket.getOutputStream();
            } catch (IOException e) {
                e.printStackTrace();
            }
            /*String tmp_ssid = ssidEdittext.getText().toString().replace("\\s", "");
            String tmp_pwd = pwdEdittext.getText().toString().replace("\\s", "");
            String senddata = "SSID:" + ssidEdittext.getText().toString() + "," + "PWD:" + pwdEdittext.getText().toString();
            Log.i(TAG,"senddata : " + senddata);
            send(senddata);*/

            // The connection attempt succeeded. Perform work associated with
            // the connection in a separate thread.
            //manageMyConnectedSocket(mmSocket);
        }

        // Closes the client socket and causes the thread to finish.
        public void cancel() {
            try {
                mmSocket.close();
                // update Status
                Message msg = new Message();
                msg.what = Status_Update;
                msg.obj = mmDevice.getName() + " BT comm disconnected";
                mHandler.sendMessage(msg);

                Message msg1 = new Message();
                msg1.what = DeviceInfo_Update;

                msg1.obj = "Device:" + mmDevice.getName() + ",Status:disconnected";
                mHandler.sendMessage(msg1);
                is = null;
                os = null;
            } catch (IOException e) {
                Log.e(TAG, "Could not close the client socket", e);
            }
        }
    }

    private void pairDevice(BluetoothDevice device) {

        try {

            Method method = device.getClass().getMethod("createBond", (Class[]) null);

            method.invoke(device, (Object[]) null);

        } catch (Exception e) {

            e.printStackTrace();

        }

    }

    public void send(final String tmp){
        new Thread(new Runnable() {
            @Override
            public void run() {
                if (os!=null) {
                    try {
                        os.write(tmp.getBytes("utf-8"));
                        os.flush();

                    } catch (IOException e) {
                        e.printStackTrace();
                        Log.d(TAG,"send IOE");
                    }
                    Log.i(TAG,"send ok!");
                }else{
                    Log.i(TAG," os = null");
                }

            }
        }).start();
    }

    private void handle_read_message(String read_data){
        if (read_data.contains("Fail")){
            Message msg = new Message();
            msg.what = Toast_Show;
            msg.obj = read_data;
            mHandler.sendMessage(msg);
        }else {
            Message msg = new Message();
            msg.what = Toast_Show;
            msg.obj = read_data;
            mHandler.sendMessage(msg);
        }
    }

    public class ReadThread extends Thread{

        public BluetoothSocket socket;
        public ReadThread(BluetoothSocket socket){
            this.socket = socket;
        }

        @Override
        public void run() {
            while (socket.isConnected()) {
                byte[] buffer = new byte[128];
                int count = 0;
                if (socket != null) {
                    try {
                        String tmp;
                        count = is.read(buffer);
                        tmp = new String(buffer, 0, count, "utf-8");
                        Log.i(TAG,"read : " + tmp);
                        handle_read_message(tmp);

                    } catch (IOException e) {
                        e.printStackTrace();
                        Log.d(TAG, "ReadThread IOE");
                        break;
                    }
                }
            }
            try {
                if(connectThread != null) {
                    // update Status
                    Message msg = new Message();
                    msg.what = Status_Update;
                    msg.obj = connectThread.mmDevice.getName() + " BT comm disconnected";
                    mHandler.sendMessage(msg);

                    Message msg1 = new Message();
                    msg1.what = DeviceInfo_Update;

                    msg1.obj = "Device:" + connectThread.mmDevice.getName() + ",Status:disconnected";
                    mHandler.sendMessage(msg1);
                }
            } catch (Exception e){
                e.printStackTrace();
                Log.d(TAG, "ReadThread IOE");

            }
        }
    }

    @Override
    protected void onResume() {
        Log.i(TAG,"onResume");
        //mBluetoothAdapter.startDiscovery();
        //mAdapter.clear();
        //searchedDevices.clear();
        super.onResume();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // Don't forget to unregister the ACTION_FOUND receiver.
        unregisterReceiver(searchReceiver);
    }

    private void request_permission() {
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
        }//ok to mark
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
        }//ok to mark
        if (this.checkSelfPermission(Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            final AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("This app needs ACCESS_BACKGROUND_LOCATION access");
            builder.setMessage("Please grant ACCESS_BACKGROUND_LOCATION access so this app can detect peripherals.");
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
            builder.setTitle("This app needs BLUETOOTH_ADVERTISE access");
            builder.setMessage("Please grant BLUETOOTH_ADVERTISE access so this app can detect peripherals.");
            builder.setPositiveButton(android.R.string.ok, null);
            builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                @Override
                public void onDismiss(DialogInterface dialog) {
                    requestPermissions(new String[]{Manifest.permission.BLUETOOTH_ADVERTISE}, PERMISSION_REQUEST_ADVERTISE);
                }
            });
            builder.show();
        }

        if (this.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            final AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("This app needs BLUETOOTH_CONNECT access");
            builder.setMessage("Please grant BLUETOOTH_CONNECT access so this app can detect peripherals.");
            builder.setPositiveButton(android.R.string.ok, null);
            builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                @Override
                public void onDismiss(DialogInterface dialog) {
                    requestPermissions(new String[]{Manifest.permission.BLUETOOTH_CONNECT}, PERMISSION_REQUEST_CONNECT);
                }
            });
            builder.show();
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (this.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, PERMISSION_REQUEST_COARSE_LOCATION);
            }
        }
    }

}