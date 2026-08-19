package wallet.ui;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import androidx.core.app.NotificationCompat;
import wallet.R;

public class BrowserBackgroundService extends Service {
    private static final String CHANNEL_ID = "browser_playback_channel";
    private static final int NOTIFICATION_ID = 0x9876;

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        // ✅ Foreground Service — Android 16 BẮT BUỘC
        startForeground(NOTIFICATION_ID, buildNotification());
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "Trình duyệt phát nền",
                NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Giữ phát video/audio khi ra nền");
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }
    }

    private Notification buildNotification() {
        return new NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Đang phát")
            .setContentText("Tiếp tục phát trong nền")
            .setSmallIcon(android.R.drawable.ic_media_play) // ✅ Dùng icon hệ thống — KHÔNG BỊ LỖI
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setSilent(true)
            .setOngoing(true)
            .build();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY; // Giữ service chạy
    }

    @Override
    public IBinder onBind(Intent intent) { return null; }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopForeground(STOP_FOREGROUND_REMOVE);
    }
}
