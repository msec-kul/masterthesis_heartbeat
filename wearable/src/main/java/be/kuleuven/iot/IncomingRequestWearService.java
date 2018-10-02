package be.kuleuven.iot;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.AsyncTask;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.WearableListenerService;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutionException;

import be.kuleuven.msec.iot.iotframework.implementations.androidwear.communication.AndroidWearClient;

public class IncomingRequestWearService extends WearableListenerService {
    private static final String TAG = "IncomingRequestService";
    private SensorManager sensorManager;
    // Maps unique sensor ids (sensortype + sensorname) to sensor
    // https://developer.android.com/reference/android/hardware/Sensor.html#getId() -> id is not unique
    private HashMap<String, Sensor> sensors = new HashMap<>();
    // Maps unique sensor ids to amount of registered nodes, used to remove listener from sensors when node disconnects
    //private HashMap<String, ArrayList<String>> sensorNodesMap = new HashMap<>();

    @Override
    public void onCreate() {
        super.onCreate();
        // Check permission for body sensors
        checkSensorPermissions();

        //Wearable.getMessageClient(this).addListener(this);

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        List<Sensor> sl = sensorManager.getSensorList(Sensor.TYPE_ALL);
        String sensorId;
        for(Sensor s : sl) {
            sensorId = s.getStringType() + "_" + s.getName().replace(' ', '_');
            //Log.d(TAG, sensorId);
            sensors.put(sensorId, s);
            //sensorNodesMap.put(sensorId, new ArrayList<String>());
        }
    }

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        Log.d(TAG, "Message received: " + messageEvent.getPath());
        String sourceId = messageEvent.getSourceNodeId();
        switch(messageEvent.getPath()) {
            case AndroidWearClient.REQUEST_SENSOR_MONITORING:
                String sensorId = new String(messageEvent.getData(), StandardCharsets.UTF_8);
                // Check if sensorId is known
                if(sensors.keySet().contains(sensorId)) {
                    Intent intent = new Intent(this, SensorListenerService.class);
                    intent.putExtra(SensorListenerService.INTENT_ACTION, SensorListenerService.INTENT_ACTION_REGISTER);
                    intent.putExtra(SensorListenerService.INTENT_NODEID, sourceId);
                    intent.putExtra(SensorListenerService.INTENT_SENSORID, sensorId);
                    startService(intent);
                }
                break;
            case AndroidWearClient.REQUEST_SENSOR_VALUE:
                sensorId = new String(messageEvent.getData(), StandardCharsets.UTF_8);
                if(sensors.keySet().contains(sensorId)) {
                    // Request sensor value once

                }
                break;
            case AndroidWearClient.REQUEST_SENSORS:
                new RespondSensorListTask().execute(messageEvent.getSourceNodeId());
                break;
            case AndroidWearClient.REQUEST_SENSOR_VALUE_EXCEEDED:
                String message = new String(messageEvent.getData(), StandardCharsets.UTF_8);
                sensorId = message.substring(0, message.indexOf("|"));
                String valuesString = message.substring(message.indexOf("|") + 1);
                Log.d(TAG, message);
                Log.d(TAG, sensorId);
                Log.d(TAG, valuesString);
                if(sensors.keySet().contains(sensorId)) {
                    Intent intent = new Intent(this, SensorListenerService.class);
                    intent.putExtra(SensorListenerService.INTENT_ACTION, SensorListenerService.INTENT_ACTION_EXCEED);
                    intent.putExtra(SensorListenerService.INTENT_NODEID, sourceId);
                    intent.putExtra(SensorListenerService.INTENT_SENSORID, sensorId);
                    float[] exceedValues = AndroidWearClient.stringToArray(valuesString);
                    intent.putExtra(SensorListenerService.INTENT_EXCEEDVALUE, exceedValues);
                    startService(intent);
                }
                break;
            case AndroidWearClient.UNMONITOR_SENSOR:
                sensorId = new String(messageEvent.getData(), StandardCharsets.UTF_8);
                // Check if sensorId is known
                if(sensors.keySet().contains(sensorId)) {
                    Intent intent = new Intent(this, SensorListenerService.class);
                    intent.putExtra(SensorListenerService.INTENT_ACTION, SensorListenerService.INTENT_ACTION_UNREGISTER);
                    intent.putExtra(SensorListenerService.INTENT_NODEID, sourceId);
                    intent.putExtra(SensorListenerService.INTENT_SENSORID, sensorId);
                    startService(intent);
                }
                break;
            case AndroidWearClient.DISCONNECT:
                Intent intent = new Intent(this, SensorListenerService.class);
                intent.putExtra(SensorListenerService.INTENT_ACTION, SensorListenerService.INTENT_ACTION_DISCONNECT);
                intent.putExtra(SensorListenerService.INTENT_NODEID, sourceId);
                startService(intent);
                break;
        }
    }

    private void checkSensorPermissions() {
        int permissionCheck = ContextCompat.checkSelfPermission(this, Manifest.permission.BODY_SENSORS);
        if(permissionCheck == PackageManager.PERMISSION_DENIED) {
            // Launch activity to ask for permission
            Intent startIntent = new Intent(this, MainActivity.class);
            // If set, this activity will become the start of a new task on this history stack.
            startIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(startIntent);
        }
    }

    private class RespondSensorListTask extends AsyncTask<String, Void, Void> {
        @Override
        protected Void doInBackground(String... strings) {
            if(strings.length > 0) {
                String nodeId = strings[0];
                StringBuilder sb = new StringBuilder();
                for(String s : sensors.keySet()) {
                    sb.append(s + "-");
                }
                Task<Integer> task = Wearable.getMessageClient(getApplicationContext()).sendMessage(nodeId, AndroidWearClient.RESPOND_SENSORS, sb.toString().getBytes(StandardCharsets.UTF_8));
                try {
                    Tasks.await(task);
                } catch (ExecutionException e) {
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            return null;
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }
}
