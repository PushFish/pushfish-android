package fish.push.api.Async;

import android.annotation.TargetApi;
import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Date;

import androidx.core.app.NotificationCompat;
import fish.push.api.API.PushfishMessage;
import fish.push.api.API.PushfishService;
import fish.push.api.DatabaseHandler;
import fish.push.api.MiscUtil;
import fish.push.api.pushfish.PushFishActivity;
import fish.push.api.R;


public class PushFishIntentService extends IntentService{
    private static int NOTIFICATION_ID = 0;

    public PushFishIntentService() {
        super("PushFishIntentService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Bundle extras = intent.getExtras();
//         mqtt = .getInstance(this);
//        String messageType = gcm.getMessageType(intent);

        if (!extras.isEmpty()) {
            String rawJson = extras.getString("message");

            try {
                JSONObject Msg = new JSONObject(rawJson);
                JSONObject Srv = Msg.getJSONObject("service");
                PushfishService srv = new PushfishService(
                        Srv.getString("public"),
                        Srv.getString("name"),
                        new Date((long) Srv.getInt("created") * 1000)
                );
                srv.setIcon(Srv.getString("icon"));

                PushfishMessage msg = new PushfishMessage(
                        srv,
                        Msg.getString("message"),
                        Msg.getString("title"),
                        Msg.getInt("timestamp")
                );
                msg.setLevel(Msg.getInt("level"));
                msg.setLink(Msg.getString("link"));
                DatabaseHandler db = new DatabaseHandler(this);
                db.addMessage(msg);
                sendNotification(msg);
            } catch (JSONException ignore) {
                Log.e("PushfishJson", ignore.getMessage());
            }
        }
//        GcmBroadcastReceiver.completeWakefulIntent(intent);
//        sendBroadcast(new Intent("PushfishMessageRefresh"));
    }

    private void sendNotification(PushfishMessage msg) {
        NOTIFICATION_ID++;
        NotificationManager mNotificationManager = (NotificationManager)
                this.getSystemService(Context.NOTIFICATION_SERVICE);

        Intent intent = new Intent(this, PushFishActivity.class);
        if (msg.hasLink()) {
            try {
                intent = new Intent(Intent.ACTION_VIEW, Uri.parse(msg.getLink()));
            } catch (Exception ignore) {
            }
        }
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, intent, 0);

        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(this)
                        .setSmallIcon(R.drawable.ic_stat_notif)
                        .setContentTitle(msg.getTitleOrName())
                        .setStyle(new NotificationCompat.BigTextStyle()
                                .bigText(msg.getMessage()))
                        .setContentText(msg.getMessage())
                        .setAutoCancel(true);

        if (msg.getService().hasIcon()) {
            try {
                Bitmap icon = msg.getService().getIconBitmap(getApplicationContext());
                Resources res = getApplicationContext().getResources();

                int nHeight = (int) res.getDimension(android.R.dimen.notification_large_icon_height);
                int nWidth = (int) res.getDimension(android.R.dimen.notification_large_icon_width);

                mBuilder.setLargeIcon(MiscUtil.scaleBitmap(icon, nWidth, nHeight));
            } catch (IOException ignore) {
                ignore.printStackTrace();
            }
        }

        setPriority(mBuilder, msg);
        mBuilder.setDefaults(Notification.DEFAULT_ALL);

        mBuilder.setContentIntent(contentIntent);
        mNotificationManager.notify(NOTIFICATION_ID, mBuilder.build());
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    private void setPriority(NotificationCompat.Builder mBuilder, PushfishMessage msg) {
        int priority = msg.getLevel() - 3;
        if(Math.abs(priority) > 2) {
            priority = 0;
        }

        mBuilder.setPriority(priority);
    }

}
