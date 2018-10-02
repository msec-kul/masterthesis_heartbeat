package be.kuleuven.iot;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.wearable.activity.WearableActivity;
import android.util.Log;
import android.widget.TextView;

public class MainActivity extends WearableActivity {

    private final String TAG = "WearMainActivity";
    private TextView mTextView;
    private final int MY_PERMISSIONS_REQUEST_BODY_SENSORS = 0;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mTextView = (TextView) findViewById(R.id.text);

        // Enables Always-on
        setAmbientEnabled();

        // Check if app has permission to access body sensors
        int permissionCheck = ContextCompat.checkSelfPermission(this, Manifest.permission.BODY_SENSORS);
        if(permissionCheck == PackageManager.PERMISSION_DENIED) {
            // Request permission
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.BODY_SENSORS}, MY_PERMISSIONS_REQUEST_BODY_SENSORS);
        } else {
            // Permission granted
            permissionGranted();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch(requestCode) {
            case MY_PERMISSIONS_REQUEST_BODY_SENSORS:
                if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Permission granted
                    permissionGranted();
                } else {
                    // Permission denied
                    permissionDenied();
                }
                return;
        }
    }

    private void permissionGranted() {
        // Init sensors
        Log.d(TAG, "Permission granted");
        mTextView.setText("Permission granted");
    }

    private void permissionDenied() {
        // No permission given
        Log.d(TAG, "Permission denied");
        mTextView.setText("Permission denied");
    }
}
