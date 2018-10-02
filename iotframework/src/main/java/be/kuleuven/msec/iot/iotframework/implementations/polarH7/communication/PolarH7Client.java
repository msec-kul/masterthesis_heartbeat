package be.kuleuven.msec.iot.iotframework.implementations.polarH7.communication;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Context;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.UUID;

import be.kuleuven.msec.iot.iotframework.callbackinterfaces.OnEventOccurred;
import be.kuleuven.msec.iot.iotframework.callbackinterfaces.OnRequestCompleted;

/**
 * Created by Thomas on 19/03/2018.
 */

public class PolarH7Client {
    private final static String TAG = "PolarH7Client";
    private Context context;
    private String mac;
    private boolean isReachable = false;
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothDevice bluetoothDevice;
    private BluetoothGatt gatt;
    private boolean isConnected = false;

    private final UUID HR_MEASUREMENT = UUID.fromString("00002a37-0000-1000-8000-00805f9b34fb");
    private final UUID HR_SERVICE = UUID.fromString("0000180D-0000-1000-8000-00805f9b34fb");
    private static final UUID DESCRIPTOR_CCC = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");

    private OnEventOccurred<Integer> monitor;
    private LinkedList<OnRequestCompleted<Boolean>> reachabilityRequests = new LinkedList<>();
    private HashMap<Integer, OnEventOccurred<Integer>> exceedEvents = new HashMap();
    private LinkedList<OnRequestCompleted<Boolean>> connectRequests = new LinkedList<>();

    public PolarH7Client(Context context, String macAddress) {
        this.mac = macAddress;
        this.context = context;
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        bluetoothDevice = bluetoothAdapter.getRemoteDevice(mac);
    }

    public void monitorSensor(OnEventOccurred oeo) {
        monitor = oeo;
        if(!isConnected)
            bluetoothDevice.connectGatt(context, false, bluetoothGattCallback, BluetoothDevice.TRANSPORT_LE);
    }

    public void unmonitor() {
        monitor = null;
        if(gatt != null) {
            gatt.close();
            gatt.disconnect();
            isConnected = false;
        }
    }

    public void connect(OnRequestCompleted<Boolean> orc) {
        //connectRequests.add(orc);
        //bluetoothAdapter.getBluetoothLeScanner().startScan(scanCallback);
    }

    public void disconnect() {
        if(gatt != null) {
            gatt.close();
            gatt.disconnect();
        }
    }

    public void exceeds(int value, OnEventOccurred<Integer> oeo) {
        exceedEvents.put(value, oeo);
        if(!isConnected)
            bluetoothDevice.connectGatt(context, false, bluetoothGattCallback, BluetoothDevice.TRANSPORT_LE);
    }

    public void isReachable(OnRequestCompleted<Boolean> orc) {
        reachabilityRequests.add(orc);
        bluetoothAdapter.getBluetoothLeScanner().startScan(scanCallback);
    }

    private final ScanCallback scanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, final ScanResult result) {
            if(result.getDevice().getAddress() != null) {
                if(result.getDevice().getAddress().equals(mac)) {
                    bluetoothAdapter.getBluetoothLeScanner().stopScan(scanCallback);
                    isReachable = true;

                    OnRequestCompleted<Boolean> orc = reachabilityRequests.poll();
                    if(orc != null)
                        orc.onSuccess(isReachable);
/*                    orc = connectRequests.poll();
                    if(orc != null)
                        orc.onSuccess(isReachable);*/
                }
            }
        }

        @Override
        public void onScanFailed(int errorCode) {}
    };

    private final BluetoothGattCallback bluetoothGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);
            if(newState == BluetoothGatt.STATE_CONNECTED && status == BluetoothGatt.GATT_SUCCESS) {
                PolarH7Client.this.isConnected = true;
                PolarH7Client.this.gatt = gatt;
                gatt.discoverServices();
            }
            if(newState == BluetoothGatt.STATE_DISCONNECTED) {
                PolarH7Client.this.isConnected = false;
                PolarH7Client.this.gatt = null;
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            super.onServicesDiscovered(gatt, status);
            for(BluetoothGattService gattService : gatt.getServices()) {
                if(gattService.getUuid().equals(HR_SERVICE)) {
                    for(BluetoothGattCharacteristic characteristic : gattService.getCharacteristics()) {
                        if(characteristic.getUuid().equals(HR_MEASUREMENT)) {
                            BluetoothGattDescriptor descriptor = characteristic.getDescriptor(DESCRIPTOR_CCC);
                            gatt.setCharacteristicNotification(characteristic, true); // Register for changes
                            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                            gatt.writeDescriptor(descriptor);
                        }
                    }
                }
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicChanged(gatt, characteristic);
            if(characteristic.getUuid().equals(HR_MEASUREMENT)) {
                int flag = characteristic.getProperties();
                int format;
                if((flag & 0x01) != 0) {
                    format = BluetoothGattCharacteristic.FORMAT_UINT16;
                } else {
                    format = BluetoothGattCharacteristic.FORMAT_UINT8;
                }
                final int heartRate = characteristic.getIntValue(format, 1);
                // Update monitors
                if(monitor != null) {
                    monitor.onUpdate(heartRate);
                }
                // Update exceeds
                for(int i : exceedEvents.keySet()) {
                    if(heartRate > i)
                        exceedEvents.get(i).onUpdate(heartRate);
                }
            }
        }
    };
}
