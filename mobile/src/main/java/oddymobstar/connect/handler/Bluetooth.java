package oddymobstar.connect.handler;

import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.FragmentActivity;
import android.util.Log;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import oddymobstar.activity.DemoActivity;
import oddymobstar.connect.ConnectivityInterface;
import oddymobstar.connect.client.BluetoothClient;
import oddymobstar.connect.server.BluetoothServer;

/**
 * Created by root on 25/04/15.
 */
public class Bluetooth implements ConnectivityInterface {

    public static final int REQUEST_ENABLE_BT = 46;

    private FragmentActivity activity;
    private String uuid;
    private byte[] message;

    public static final int DISCOVERABLE_SECONDS = 120;
    private List<BluetoothDevice> devices = new ArrayList<>();

    private ProgressDialog progressDialog;

    private BluetoothServer server;
    private BluetoothClient client;

    private DemoActivity.DeviceDiscovery deviceDiscovery;

    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            Log.d("receiving ", "received somthing");

            if (BluetoothDevice.ACTION_FOUND.equals(intent.getAction())) {

                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                deviceDiscovery.addDevice(device);
                //kill off our progress...we can still add more to devices.
                progressDialog.dismiss();
                //and launch dd
                try {
                    if (!deviceDiscovery.getBluetoothManager().isRunning()) {
                        deviceDiscovery.onDiscover();
                    }
                } catch (Exception e) {
                    //cos android is a pile of bluetooth shite
                    deviceDiscovery.getBluetoothManager().setIsRunning(false);
                }
            }
        }
    };
    /*
      withmost stuff we need to enable then set up various crap..
     */
    private BluetoothAdapter bluetoothAdapter;

    public Bluetooth(FragmentActivity activity, String uuid) {
        this.activity = activity;
        this.uuid = uuid;

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    }

    public BroadcastReceiver getReceiver() {
        return receiver;
    }


    //may need to go into the interface assuming all things take time and need a progress...
    public ProgressDialog getProgress(DemoActivity.DeviceDiscovery deviceDiscovery) {

        this.deviceDiscovery = deviceDiscovery;
        deviceDiscovery.getBluetoothManager().init(devices, bluetoothAdapter.getName());

        progressDialog = new ProgressDialog(activity);

        progressDialog.setTitle("Bluetooth");
        progressDialog.setMessage("Discovering....");

        progressDialog.setIndeterminate(true);

        return progressDialog;
    }

    @Override
    public void enable() {


        Intent discoverableIntent = new
                Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, DISCOVERABLE_SECONDS);
        activity.startActivityForResult(discoverableIntent, REQUEST_ENABLE_BT);

    }


    @Override
    public void disable() {

        Log.d("calling disable ", "disable");

        //step 1: kill of the
        if (server != null) {
            server.cancel();
        }

        if (client != null) {
            client.cancel();
        }

        /* unpair

         */
        try {
            Class<?> btDeviceInstance = Class.forName(BluetoothDevice.class.getCanonicalName());

            Method removeBondMethod = btDeviceInstance.getMethod("removeBond");
            for (BluetoothDevice device : deviceDiscovery.getBluetoothManager().getSelectedDevices()) {

                Log.d("calling disable ", "unpair " + device.getName());

                removeBondMethod.invoke(device);
            }
        } catch (Exception e) {
            Log.d("i should have upaired", "but instead it errored " + e.toString());
        }


    }

    public void unpair(BluetoothDevice device) {

        Log.d("calling unpair ", "unpair " + device.getName());
        try {
            Class<?> btDeviceInstance = Class.forName(BluetoothDevice.class.getCanonicalName());

            Method removeBondMethod = btDeviceInstance.getMethod("removeBond");
            removeBondMethod.invoke(device);

        } catch (Exception e) {
            Log.d("i should have upaired", "but instead it errored " + e.toString());
        }


    }


    @Override
    public void setMessage(byte[] message) {
        this.message = message;
    }

    public void cancelDiscovery() {
        bluetoothAdapter.cancelDiscovery();
    }

    @Override
    public void handle(int requestCode, int resultCode, Intent data) {


        if (requestCode == REQUEST_ENABLE_BT) {


            if (resultCode == DISCOVERABLE_SECONDS) {


                Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();

                for (BluetoothDevice device : pairedDevices) {
                    devices.add(device);
                }

                if (bluetoothAdapter.isDiscovering()) {
                    bluetoothAdapter.cancelDiscovery();
                }

                bluetoothAdapter.startDiscovery();
            } else {
                //its a cancel so dont go discover.
            }


        }

    }

    public void createServer() {
        server = new BluetoothServer(deviceDiscovery, bluetoothAdapter, uuid, message);
        server.start();
    }

    public void createClient() {

        bluetoothAdapter.cancelDiscovery();

        client = new BluetoothClient(deviceDiscovery, uuid);
        client.start();

    }


}
