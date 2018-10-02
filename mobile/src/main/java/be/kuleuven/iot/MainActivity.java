package be.kuleuven.iot;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Arrays;

import be.kuleuven.msec.iot.iotframework.callbackinterfaces.OnEventOccurred;
import be.kuleuven.msec.iot.iotframework.callbackinterfaces.OnRequestCompleted;
import be.kuleuven.msec.iot.iotframework.generic.devicelayer.Accelerometer;
import be.kuleuven.msec.iot.iotframework.generic.devicelayer.Gyroscope;
import be.kuleuven.msec.iot.iotframework.generic.devicelayer.HeartRateSensor;
import be.kuleuven.msec.iot.iotframework.generic.devicelayer.VirtualIoTConnector;
import be.kuleuven.msec.iot.iotframework.implementations.androidwear.AndroidWearDevice;
import be.kuleuven.msec.iot.iotframework.implementations.polarH7.PolarH7Device;

public class MainActivity extends Activity {

    private final static String TAG = "Mobile - MainActivity";

    private TextView textViewWearableHeartRate, textViewH7HeartRate, textViewAcceleration, textViewGyroscope;
    private Switch switchDevices;

    // Change to VirtualIoTConnector, can be done by defining gateways and connected sensors in config file.
    // A parser parses the config file to a list of VirtualIoTConnectors and sensors
    private VirtualIoTConnector androidWearDevice, polarH7;

    private HeartRateSensor hrSensorWearable, hrSensorH7;
    private Accelerometer accelerometerWearable;
    private Gyroscope gyroscopeWearable;

    private final static String WEAR_ID = "2958ef1f"; // Node id
    private final static String POLARBEAT_ID = "A0:9E:1A:16:B2:3A"; // bluetooth MAC address

    private final static int REQUEST_COARSE_LOCATION = 1001;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        textViewWearableHeartRate = findViewById(R.id.textViewWearableHeartRate);
        textViewH7HeartRate = findViewById(R.id.textViewH7HeartRate);
        textViewAcceleration = findViewById(R.id.textViewAcceleration);
        textViewGyroscope = findViewById(R.id.textViewGyroscope);
        switchDevices = findViewById(R.id.switchDevices);

        //TODO: Ask permission for Bluetooth and coarse location
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // Permission is not granted
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                    REQUEST_COARSE_LOCATION);
        }

        androidWearDevice = new AndroidWearDevice(this, WEAR_ID);
        polarH7 = new PolarH7Device(this, POLARBEAT_ID);
    }

    @Override
    public void onResume() {
        if(androidWearDevice != null) androidWearDevice.connect(new OnRequestCompleted<Boolean>() {
            @Override
            public void onSuccess(Boolean response) {

            }
        });
        if(polarH7 != null) polarH7.connect(new OnRequestCompleted<Boolean>() {
            @Override
            public void onSuccess(Boolean response) {

            }
        });
        super.onResume();
    }

    @Override
    public void onPause() {
        if(androidWearDevice != null) androidWearDevice.disconnect(new OnRequestCompleted<Boolean>() {
            @Override
            public void onSuccess(Boolean response) {

            }
        });
        if(polarH7 != null) polarH7.disconnect(new OnRequestCompleted<Boolean>() {
            @Override
            public void onSuccess(Boolean response) {

            }
        });
        super.onPause();
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    public void clearButtonClicked(View view) {
        textViewGyroscope.setText("...");
        textViewAcceleration.setText("...");
        textViewWearableHeartRate.setText("...");
        textViewH7HeartRate.setText("...");
    }

    public void requestHRMonitorButtonClicked(View view) {
        // Monitor one
        if(switchDevices.isChecked()) {
            // Polar H7
            if(hrSensorWearable != null) hrSensorWearable.unmonitorHeartRate();
            if(hrSensorH7 != null) {
                hrSensorH7.monitorHeartRate(new OnEventOccurred<Integer>() {
                    @Override
                    public void onUpdate(Integer response) {
                        final StringBuilder sb = new StringBuilder();
                        sb.append("Polar H7 HR: ");
                        sb.append(response);
                        sb.append(" " + hrSensorH7.getUnit());
                        Log.d(TAG, sb.toString());
                        // Crashes here 'cause textView cannot be updated from this thread
                        // Only the original thread that created a view hierarchy can touch its views.
                        // Solution -> runOnUIThread
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    textViewH7HeartRate.setText(sb.toString());
                                } catch(Exception e) {
                                    Log.d(TAG, e.getMessage());
                                }
                            }
                        });
                    }
                });
            } else {
                Toast.makeText(this, "No polar H7 connected", Toast.LENGTH_LONG).show();
            }
        } else {
            // Android Wear
            if(hrSensorH7 != null) hrSensorH7.unmonitorHeartRate();
            if(hrSensorWearable != null) {
                hrSensorWearable.monitorHeartRate(new OnEventOccurred<Integer>() {
                        @Override
                        public void onUpdate(Integer response) {
                            StringBuilder sb = new StringBuilder();
                            sb.append("Android Wearable HR: ");
                            sb.append(response);
                            sb.append(" " + hrSensorWearable.getUnit());
                            textViewWearableHeartRate.setText(sb.toString());

                        }
                    });
/*                hrSensorWearable.exceeds(80, new OnEventOccurred<Integer>() {
                    @Override
                    public void onUpdate(Integer response) {
                        textViewWearableHeartRate.setText("HeartRate exceeded 80, " + response);
                    }
                });*/
            } else {
                Toast.makeText(this, "No Android Wear device connected", Toast.LENGTH_LONG).show();
            }
        }
    }

    public void requestAMMonitorButtonClicked(View view) {
        if(accelerometerWearable != null) {
            //final Accelerometer accelerometer = androidWearDevice.getAccelerometer();
            accelerometerWearable.monitorAcceleration(new OnEventOccurred<float[]>() {
                @Override
                public void onUpdate(float[] response) {
                    //sensorLogger.log("WearableAM", response);
                    Log.d(TAG, "WearableAM: " + System.currentTimeMillis() + " " + "1");
                    StringBuilder sb = new StringBuilder();
                    sb.append("Accelerometer: ");
                    sb.append(Arrays.toString(response));
                    sb.append(" " + accelerometerWearable.getUnit());
                    textViewAcceleration.setText(sb.toString());
                }
            });

            accelerometerWearable.detectFall(new OnEventOccurred<float[]>() {
                @Override
                public void onUpdate(float[] response) {
                    Toast.makeText(MainActivity.this, "Fall detected", Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            Toast.makeText(this, "No accelerometer connected", Toast.LENGTH_LONG).show();
        }
    }

    public void requestGyroscopeMonitorButtonClicked(View view) {
        if(gyroscopeWearable != null) {
            gyroscopeWearable.monitorRateOfRotation(new OnEventOccurred<float[]>() {
                @Override
                public void onUpdate(float[] response) {
                    //sensorLogger.log("WearableGyro", response);
                    Log.d(TAG, "WearableGyro: " + System.currentTimeMillis() + " " + "1");
                    StringBuilder sb = new StringBuilder();
                    sb.append("Gyroscope: ");
                    sb.append(Arrays.toString(response));
                    sb.append(" " + gyroscopeWearable.getUnit());
                    textViewGyroscope.setText(sb.toString());
                }
            });
        } else {
            Toast.makeText(this, "No gyroscope connected", Toast.LENGTH_LONG).show();
        }
    }

    public void monitorAllButtonClicked(View view) {
        // Monitor HR
        if(hrSensorWearable != null)
            hrSensorWearable.monitorHeartRate(new OnEventOccurred<Integer>() {
                @Override
                public void onUpdate(Integer response) {
                    Log.d(TAG, "WearableHeartRate: " + System.currentTimeMillis() + ", " + response);

                    StringBuilder sb = new StringBuilder();
                    sb.append("Android Wearable HR: ");
                    sb.append(response);
                    sb.append(" " + hrSensorWearable.getUnit());
                    textViewWearableHeartRate.setText(sb.toString());

                }
            });
        if(hrSensorH7 != null)
            hrSensorH7.monitorHeartRate(new OnEventOccurred<Integer>() {
                @Override
                public void onUpdate(Integer response) {
                    Log.d(TAG, "H7HeartRate: " + System.currentTimeMillis() + ", " + response);

                    final StringBuilder sb = new StringBuilder();
                    sb.append("Polar H7 HR: ");
                    sb.append(response);
                    sb.append(" " + hrSensorH7.getUnit());
                    // Crashes here 'cause textView cannot be updated from this thread
                    // Only the original thread that created a view hierarchy can touch its views.
                    // Solution -> runOnUIThread
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                textViewH7HeartRate.setText(sb.toString());
                            } catch(Exception e) {
                                Log.d(TAG, e.getMessage());
                            }
                        }
                    });
                }
            });
        // Monitor Accelerometer
        if(accelerometerWearable != null)
            accelerometerWearable.monitorAcceleration(new OnEventOccurred<float[]>() {
                @Override
                public void onUpdate(float[] response) {
                    Log.d(TAG, "WearableAM: " + System.currentTimeMillis() + " " + "1");
                    StringBuilder sb = new StringBuilder();
                    sb.append("Accelerometer: ");
                    sb.append(Arrays.toString(response));
                    sb.append(" " + accelerometerWearable.getUnit());
                    textViewAcceleration.setText(sb.toString());
                }
            });
        // Monitor Gyroscope
        if(gyroscopeWearable != null)
            gyroscopeWearable.monitorRateOfRotation(new OnEventOccurred<float[]>() {
                @Override
                public void onUpdate(float[] response) {
                    Log.d(TAG, "WearableGyro: " + System.currentTimeMillis() + " " + "1");
                    StringBuilder sb = new StringBuilder();
                    sb.append("Gyroscope: ");
                    sb.append(Arrays.toString(response));
                    sb.append(" " + gyroscopeWearable.getUnit());
                    textViewGyroscope.setText(sb.toString());
                }
            });
    }

    public void pairButtonClicked(View view) {
        androidWearDevice.isReachable(new OnRequestCompleted<Boolean>() {
            @Override
            public void onSuccess(Boolean response) {
                if(response) {
                    Toast.makeText(MainActivity.this, "Wear device is connected", Toast.LENGTH_SHORT).show();
                    androidWearDevice.updateConnectedDeviceList(new OnRequestCompleted<Boolean>() {
                        @Override
                        public void onSuccess(Boolean response) {
                            // register sensors
                            hrSensorWearable = ((AndroidWearDevice)androidWearDevice).getHeartRateSensor();
                            accelerometerWearable = ((AndroidWearDevice)androidWearDevice).getAccelerometer();
                            gyroscopeWearable = ((AndroidWearDevice)androidWearDevice).getGyroscope();
                            Toast.makeText(MainActivity.this, "Wear device sensors updated", Toast.LENGTH_SHORT).show();
                        }
                    });
                } else {
                    Toast.makeText(MainActivity.this, "Wear device is not connected", Toast.LENGTH_SHORT).show();
                }
            }
        });

        polarH7.isReachable(new OnRequestCompleted<Boolean>() {
            @Override
            public void onSuccess(Boolean response) {
                if(response) {
                    hrSensorH7 = ((PolarH7Device)polarH7).getHeartRateSensor();
                    Toast.makeText(MainActivity.this, "Polar H7 is connected", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(MainActivity.this, "Polar H7 is not connected", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    public void requestHRUnmonitorButtonClicked(View view) {
        if(hrSensorWearable != null) hrSensorWearable.unmonitorHeartRate();
        if(hrSensorH7 != null) hrSensorH7.unmonitorHeartRate();
    }

    public void requestAMUnmonitorButtonClicked(View view) {
        if(accelerometerWearable != null) accelerometerWearable.unmonitorAcceleration();
    }

    public void requestGyroscopeUnmonitorButtonClicked(View view) {
        if(gyroscopeWearable != null) gyroscopeWearable.unmonitorRateOfRotation();
    }
}