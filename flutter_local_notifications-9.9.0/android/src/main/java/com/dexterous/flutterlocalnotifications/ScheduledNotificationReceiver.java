package com.dexterous.flutterlocalnotifications;

import android.app.Notification;
import android.content.BroadcastReceiver;
import android.content.Context;

import android.content.Intent;

import androidx.annotation.Keep;
import androidx.core.app.NotificationManagerCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.dexterous.flutterlocalnotifications.models.NotificationDetails;
import com.dexterous.flutterlocalnotifications.utils.StringUtils;
import com.dexterous.flutterlocalnotifications.utils.Util;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.content.pm.PackageManager;
import android.net.Network;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;

/** Created by michaelbui on 24/3/18. */
@Keep
public class ScheduledNotificationReceiver extends BroadcastReceiver {

  private static Intent getLaunchIntent(Context context) {
    String packageName = context.getPackageName();
    PackageManager packageManager = context.getPackageManager();
    return packageManager.getLaunchIntentForPackage(packageName);
  }
  @Override
  public void onReceive(final Context context, Intent intent) {
    String notificationDetailsJson =
        intent.getStringExtra(FlutterLocalNotificationsPlugin.NOTIFICATION_DETAILS);
    if (StringUtils.isNullOrEmpty(notificationDetailsJson)) {
      // This logic is needed for apps that used the plugin prior to 0.3.4
      Notification notification = intent.getParcelableExtra("notification");
      notification.when = System.currentTimeMillis();
      int notificationId = intent.getIntExtra("notification_id", 0);
      NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
      notificationManager.notify(notificationId, notification);
      boolean repeat = intent.getBooleanExtra("repeat", false);
      if (!repeat) {
        FlutterLocalNotificationsPlugin.removeNotificationFromCache(context, notificationId);
      }
    } else {
      Gson gson = FlutterLocalNotificationsPlugin.buildGson();
      Type type = new TypeToken<NotificationDetails>() {}.getType();
      NotificationDetails notificationDetails = gson.fromJson(notificationDetailsJson, type);

        String appState = Util.getAppState();
        SharedPreferences sharedPreferences = context.getSharedPreferences("FlutterSharedPreferences",Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("app_state",appState);
        editor.commit();
        if (Util.foregrounded()){
          Intent newIntent = new Intent("SELECT_NOTIFICATION");
          newIntent.setAction("SELECT_NOTIFICATION");
          newIntent.putExtra("payload", notificationDetails.payload);
          LocalBroadcastManager.getInstance(context).sendBroadcast(newIntent);
        }
        else{FlutterLocalNotificationsPlugin.showNotification(context, notificationDetails);}

      if (notificationDetails.scheduledNotificationRepeatFrequency != null) {
        FlutterLocalNotificationsPlugin.zonedScheduleNextNotification(context, notificationDetails);
      } else if (notificationDetails.matchDateTimeComponents != null) {
        FlutterLocalNotificationsPlugin.zonedScheduleNextNotificationMatchingDateComponents(
            context, notificationDetails);
      } else if (notificationDetails.repeatInterval != null) {
        FlutterLocalNotificationsPlugin.scheduleNextRepeatingNotification(
            context, notificationDetails);
      } else {
        FlutterLocalNotificationsPlugin.removeNotificationFromCache(
            context, notificationDetails.id);
      }
    }
  }


}
