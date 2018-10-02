package be.kuleuven.iot;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.IBinder;
import android.util.Log;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import be.kuleuven.msec.iot.iotframework.implementations.androidwear.communication.AndroidWearClient;

import static be.kuleuven.msec.iot.iotframework.implementations.androidwear.communication.AndroidWearClient.DISCONNECT;
import static be.kuleuven.msec.iot.iotframework.implementations.androidwear.communication.AndroidWearClient.SENSORVALUE_PATH;

public class SensorListenerService extends Service implements SensorEventListener {
    public static final String INTENT_NODEID = "NodeId";
    public static final String INTENT_SENSORID = "SensorId";
    public static final String INTENT_EXCEEDVALUE = "ExceedValue";
    public static final String INTENT_ACTION = "Action";
    public static final String INTENT_ACTION_REGISTER = "RegisterListener";
    public static final String INTENT_ACTION_UNREGISTER = "UnregisterListener";
    public static final String INTENT_ACTION_EXCEED = "RegisterExceedListener";
    public static final String INTENT_ACTION_DISCONNECT = "Disconnect";

    private SensorManager sensorManager;
    // Maps unique sensor ids (sensortype + sensorname) to sensor
    // https://developer.android.com/reference/android/hardware/Sensor.html#getId() -> id is not unique
    private HashMap<String, Sensor> sensors = new HashMap<>();
    // Maps unique sensor ids to amount of registered nodes, used to remove listener from sensors when node disconnects
    private HashMap<String, HashSet<String>> sensorNodesMap = new HashMap<>();
    // Maps unique sensor ids to exceed values and registered nodes
    private HashMap<String, HashMap<float[], String>> exceedMap = new HashMap<>();

    private String TAG = "SensorListenerService";

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "OnCreate");
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        List<Sensor> sl = sensorManager.getSensorList(Sensor.TYPE_ALL);
        String sensorId;
        for(Sensor s : sl) {
            sensorId = s.getStringType() + "_" + s.getName().replace(' ', '_');
            sensors.put(sensorId, s);
            sensorNodesMap.put(sensorId, new HashSet<String>());
            exceedMap.put(sensorId, new HashMap<float[], String>());
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent.getStringExtra(INTENT_ACTION);
        String nodeId = intent.getStringExtra(INTENT_NODEID);
        String sensorId = intent.getStringExtra(INTENT_SENSORID);
        Log.d(TAG, action);
        switch (action) {
            case INTENT_ACTION_REGISTER:
                HashSet<String> nodes = sensorNodesMap.get(sensorId);
                if(nodes != null && !nodes.contains(nodeId)) {
                    nodes.add(nodeId);
                    // Read this:
                    // https://developer.android.com/reference/android/hardware/SensorManager.html#registerListener(android.hardware.SensorEventListener, android.hardware.Sensor, int)
                    // Check ensures that the service is only registered once to a specific sensor
                    if(nodes.size() == 1)
                        sensorManager.registerListener(this, sensors.get(sensorId), SensorManager.SENSOR_DELAY_NORMAL);
                }
                break;
            case INTENT_ACTION_UNREGISTER:
                nodes = sensorNodesMap.get(sensorId);
                if(nodes != null && nodes.contains(nodeId)) {
                    nodes.remove(nodeId);
                    if(nodes.isEmpty()) {
                        // Unregister from sensor
                        sensorManager.unregisterListener(this, sensors.get(sensorId));
                    }
                }
                break;
            case INTENT_ACTION_EXCEED:
                nodes = sensorNodesMap.get(sensorId);
                float[] exceedValue = intent.getFloatArrayExtra(INTENT_EXCEEDVALUE);
                if(nodes != null && !nodes.contains(nodeId)) {
                    nodes.add(nodeId);
                    // Add entry to exceedMap
                    exceedMap.get(sensorId).put(exceedValue, nodeId);
                    // Check if listener has to be registered
                    if(nodes.size() == 1)
                        sensorManager.registerListener(this, sensors.get(sensorId), SensorManager.SENSOR_DELAY_NORMAL);
                }
                break;
            case DISCONNECT:
                for(String registeredSensor : sensorNodesMap.keySet()) {
                    sensorNodesMap.get(registeredSensor).remove(nodeId);
                    if(sensorNodesMap.get(registeredSensor).isEmpty())
                        sensorManager.unregisterListener(this, sensors.get(registeredSensor));
                }
                break;
        }
        return Service.START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        // Unregister listeners
        for(Sensor s : sensors.values()) {
            sensorManager.unregisterListener(this, s);
        }
        Log.d(TAG, "OnDestroy");
        super.onDestroy();
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        Sensor sensor = sensorEvent.sensor;
        String sensorId = sensor.getStringType() + "_" + sensor.getName().replace(' ', '_');
        HashSet<String> nodes = sensorNodesMap.get(sensorId);
        HashMap<float[], String> exceeds = exceedMap.get(sensorId);
        if(nodes != null) {
            if(!nodes.isEmpty()) {
                // Update datamap
                // Do I need to check if datamap has to be updated?
                // For situations when a event should be triggered when a value is exceeded
                PutDataMapRequest dataMap = PutDataMapRequest.create("/" + sensorId);
                dataMap.getDataMap().putFloatArray(SENSORVALUE_PATH, sensorEvent.values);
                //dataMap.getDataMap().putString(SENSORNAME_PATH, sensor.getName());
                //dataMap.getDataMap().putString(SENSORTYPE_PATH, sensor.getStringType());
                PutDataRequest request = dataMap.asPutDataRequest();
                request.setUrgent(); // Clients should only setUrgent() for DataItems which need to be delivered right away.
                Task<DataItem> dataItemTask = Wearable.getDataClient(this).putDataItem(request);
                dataItemTask.addOnSuccessListener(new OnSuccessListener<DataItem>() {
                    @Override
                    public void onSuccess(DataItem dataItem) {
                        //Log.d(TAG, "Datamap updated");
                    }
                });
            }

            if(!exceeds.isEmpty()) {
                // Check if value is exceeded
                for(float[] values : exceeds.keySet()) {
                    if(values.length == sensorEvent.values.length) {
                        boolean exceeded = true;
                        for(int i = 0; i < values.length; i++) {
                            if(values[i] > sensorEvent.values[i]) {
                                exceeded = false;
                            }
                        }
                        if(exceeded) {
                            String nodeId = exceeds.get(values);
                            Log.d(TAG, "Values exceeded: " + Arrays.toString(values));
                            Wearable.getMessageClient(this).sendMessage(nodeId, AndroidWearClient.RESPOND_VALUE_EXCEEDED, (sensorId + "|" + Arrays.toString(sensorEvent.values)).getBytes(StandardCharsets.UTF_8));
                        }
                    }
                }
            }

            if(nodes.isEmpty() && exceeds.isEmpty()) {
                // Unregister from sensor
                // Never called
                sensorManager.unregisterListener(this, sensors.get(sensorEvent.sensor));
                if (sensorNodesMap.values().isEmpty()) {
                    this.stopSelf();
                }
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {
        //Log.d(TAG, "Accuracy changed");
    }
}
