/*
    Copyright 2018 Picovoice Inc.

    You may not use this file except in compliance with the license. A copy of the license is
    located in the "LICENSE" file accompanying this source.

    Unless required by applicable law or agreed to in writing, software distributed under the
    License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
    express or implied. See the License for the specific language governing permissions and
    limitations under the License.
*/

package ai.picovoice.porcupinedemoservice;

import android.Manifest;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import ai.picovoice.porcupine.Porcupine;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class MainActivity extends AppCompatActivity {
    private ToggleButton startButton;
    private TextView textView;
    public int x;
    public int y;
    public Intent serviceIntent;
    private boolean hasRecordPermission() {
        return ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestRecordPermission() {
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, 0);
    }

    final GestureDetector gestureDetector = new GestureDetector(new GestureDetector.SimpleOnGestureListener() {
        public void onLongPress(MotionEvent e) {
            Log.e("check", "Longpress detected");

            x = (int)e.getX();
            y = (int)e.getY();
            Toast.makeText(getApplicationContext(),"Pressure point set at ("+x+","+y+")",Toast.LENGTH_LONG).show();
            Log.e("x","x in main "+x);
            Log.e("x","y in main "+y);
        }
    });

    public boolean onTouchEvent(MotionEvent event) {
        return gestureDetector.onTouchEvent(event);
    };


    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            @NonNull String[] permissions,
            @NonNull int[] grantResults) {
        if (grantResults.length == 0 || grantResults[0] == PackageManager.PERMISSION_DENIED) {
            startButton.toggle();
        } else {
            startService();
        }
    }

    private void copyResourceFile(int resourceID, String filename) throws IOException {
        Resources resources = getResources();
        try (InputStream is = new BufferedInputStream(resources.openRawResource(resourceID), 256); OutputStream os = new BufferedOutputStream(openFileOutput(filename, Context.MODE_PRIVATE), 256)) {
            int r;
            while ((r = is.read()) != -1) {
                os.write(r);
            }
            os.flush();
        }
    }

    private void startService() {
        Intent serviceIntent = new Intent(this, PorcupineService.class);
        serviceIntent.putExtra("keywordFileName", "porcupine_android.ppn");
        serviceIntent.putExtra("x", x);
        serviceIntent.putExtra("y", y);
        ContextCompat.startForegroundService(this, serviceIntent);
    }

    private void stopService() {
        Intent serviceIntent = new Intent(this, PorcupineService.class);
        stopService(serviceIntent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        textView=(TextView)findViewById(R.id.textView);

        try {
            copyResourceFile(R.raw.porcupine_params, "porcupine_params.pv");
            copyResourceFile(R.raw.porcupine_android, "porcupine_android.ppn");
        } catch (IOException e) {
            Toast.makeText(this, "Failed to copy resource files.", Toast.LENGTH_SHORT).show();
        }

        startButton = findViewById(R.id.startButton);

        startButton.setOnClickListener(v -> {
            if (startButton.isChecked()) {
                if (hasRecordPermission()) {
                    startService();
                    textView.setVisibility(View.VISIBLE);
                } else {
                    requestRecordPermission();
                }
            } else {
                stopService();
            }
        });
    }
}
