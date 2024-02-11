package com.cracklesparkle.smartnotify;

import android.app.Notification;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.util.Log;

public class NotificationListener extends NotificationListenerService {
	
	@Override
	public void onNotificationPosted(StatusBarNotification sbn) {
		super.onNotificationPosted(sbn);
		
		// Get the notification flags
		int flags = sbn.getNotification().flags;
		
		// Check if the notification is dismissible
		if ((flags & Notification.FLAG_NO_CLEAR) == 0 && (flags & Notification.FLAG_ONGOING_EVENT) == 0) {
			// Get the notification text
			CharSequence notificationText = sbn.getNotification().extras.getCharSequence(Notification.EXTRA_TEXT).toString();
			if (notificationText != null) {
				Log.d("NotificationText", notificationText.toString());
				
				// Get the instance of MainActivity
				MainActivity mainActivity = MainActivity.getInstance();
				
				// Update MainActivity UI with the notification text
				if (mainActivity != null) {
					mainActivity.updateNotificationText(notificationText.toString());
				}
			}
		}
	}
}