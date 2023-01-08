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

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Path;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.accessibility.AccessibilityEvent;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;

import java.io.File;

import ai.picovoice.porcupinemanager.PorcupineManager;
import ai.picovoice.porcupinemanager.PorcupineManagerException;

public class PorcupineService extends AccessibilityService{
    private static final String CHANNEL_ID = "PorcupineServiceChannel";
    public static int timmer;
    public static boolean flag;
    private PorcupineManager porcupineManager;

    private int numKeywordsDetected;
    public int x;
    public int y;

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel notificationChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "PorcupineServiceChannel",
                    NotificationManager.IMPORTANCE_HIGH);

            NotificationManager manager = getSystemService(NotificationManager.class);
            assert manager != null;
            manager.createNotificationChannel(notificationChannel);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        createNotificationChannel();
        x = intent.getIntExtra("x",0);
        y = intent.getIntExtra("y",0);
        Log.e("x","x in service "+ x);
        Log.e("x","y in service "+y);


        PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                0,
                new Intent(this, MainActivity.class),
                0);

        numKeywordsDetected = 0;

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Porcupine")
                .setContentText("num detected : " + numKeywordsDetected)
                .setSmallIcon(R.drawable.ic_launcher_background)
                .setContentIntent(pendingIntent)
                .build();

        startForeground(1234, notification);

        String modelFilePath = new File(this.getFilesDir(), "porcupine_params.pv").getAbsolutePath();

        String keywordFileName = intent.getStringExtra("keywordFileName");
        assert keywordFileName != null;
        String keywordFilePath = new File(this.getFilesDir(), keywordFileName).getAbsolutePath();

        try {
            porcupineManager = new PorcupineManager(
                    modelFilePath,
                    keywordFilePath,
                    0.5f,
                    (keywordIndex) -> {
                        numKeywordsDetected++;
                        dispatchGesture(createClick(x, y), callback, null);
                        CharSequence title = "Porcupine";
                        PendingIntent contentIntent = PendingIntent.getActivity(
                                this,
                                0,
                                new Intent(this, MainActivity.class),
                                0);

                        Notification n = new NotificationCompat.Builder(this, CHANNEL_ID)
                                .setContentTitle(title)
                                .setContentText("num detected : " + numKeywordsDetected)
                                .setSmallIcon(R.drawable.ic_launcher_background)
                                .setContentIntent(contentIntent)
                                .build();

                        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                        assert notificationManager != null;
                        notificationManager.notify(1234, n);
                    });
            porcupineManager.start();
        } catch (PorcupineManagerException e) {
            Log.e("PORCUPINE_SERVICE", e.toString());
        }

        return START_STICKY;
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent accessibilityEvent) {

    }

    @Override
    public void onInterrupt() {

    }

    @Override
    public void onDestroy() {
        try {
            porcupineManager.stop();
        } catch (PorcupineManagerException e) {
            Log.e("PORCUPINE_SERVICE", e.toString());
        }

        super.onDestroy();
    }
    // (x, y) in screen coordinates
    @RequiresApi(api = Build.VERSION_CODES.N)
    private static GestureDescription createClick(float x, float y) {
        // for a single tap a duration of 1 ms is enough
        final int DURATION = 1;

        Path clickPath = new Path();
        clickPath.moveTo(x, y);
        GestureDescription.StrokeDescription clickStroke =
                new GestureDescription.StrokeDescription(clickPath, 0, DURATION);
        GestureDescription.Builder clickBuilder = new GestureDescription.Builder();
        clickBuilder.addStroke(clickStroke);
        timmer=0;
        flag=true;
        while (flag){
            timmer+=1;
        }
        return clickBuilder.build();
    }
    @RequiresApi(api = Build.VERSION_CODES.N)
    // callback invoked either when the gesture has been completed or cancelled
            AccessibilityService.GestureResultCallback callback = new AccessibilityService.GestureResultCallback() {
        @Override
        public void onCompleted(GestureDescription gestureDescription) {
            super.onCompleted(gestureDescription);
//            Toast.makeText(FloatingViewService.this,"gesture completed ", Toast.LENGTH_LONG).show();
            flag=false;
            Toast.makeText(getApplicationContext(), "duration :"+timmer, Toast.LENGTH_LONG).show();
        }

        @Override
        public void onCancelled(GestureDescription gestureDescription) {
            super.onCancelled(gestureDescription);
            Log.d("gesture", "gesture cancelled");
        }
    };
}
