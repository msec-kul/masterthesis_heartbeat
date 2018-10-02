package be.kuleuven.msec.iot.iotframework.implementations.polarH7.communication;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;

import java.util.UUID;

/**
 * Created by Thomas on 20/03/2018.
 */

public class BluetoothLeService extends Service {
    private final static String TAG = "BluetoothLeService";

    private BluetoothAdapter bluetoothAdapter;
    private BluetoothDevice bluetoothDevice;
    private BluetoothGatt gatt;

    private final UUID HR_MEASUREMENT = UUID.fromString("00002a37-0000-1000-8000-00805f9b34fb");
    private final UUID HR_SERVICE = UUID.fromString("0000180D-0000-1000-8000-00805f9b34fb");
    private static final UUID DESCRIPTOR_CCC = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");

    public final static String HEART_RATE_DATA = "HeartRate";
    public final static String ACTION_HEART_RATE_UPDATE = "be.kuleuven.msec.iot.iotframework.implementations.polarbeat.communication.ACTION_HEART_RATE_UPDATE";

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private final BluetoothGattCallback bluetoothGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);
            if(newState == BluetoothGatt.STATE_CONNECTED && status == BluetoothGatt.GATT_SUCCESS) {
                BluetoothLeService.this.gatt = gatt;
                gatt.discoverServices();
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
                // Send broadcast
                broadcastUpdate(ACTION_HEART_RATE_UPDATE, heartRate);
            }
        }
    };

    private void broadcastUpdate(final String action, final int heartRate) {
        Intent intent = new Intent(action);
        intent.putExtra(HEART_RATE_DATA, heartRate);
        sendBroadcast(intent);
    }
}
