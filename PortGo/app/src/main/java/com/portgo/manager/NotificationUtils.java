package com.portgo.manager;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;

import androidx.core.app.NotificationCompat;

import android.text.TextUtils;
import android.widget.RemoteViews;

import com.portgo.R;
import com.portgo.ui.PortActivityLogin;
import com.portgo.ui.PortIncallActivity;

import static com.portgo.BuildConfig.PORT_ACTION_ACCEPT;
import static com.portgo.BuildConfig.PORT_ACTION_REJECT;

/**
 * Contact class defining an entity from the native address book or XCAP server.
 */
public class NotificationUtils {
    static private final int NOTIVICATIONID = 492371;
    static private final int APP_NOTIFICATION = NOTIVICATIONID+1;
    static private final int AVCALL_NOTIFICATION = NOTIVICATIONID+2;
    static private final int MESSAGE_NOTIFICATION = NOTIVICATIONID+3;
    static private final int PENDINGCALL_NOTIFICATION = NOTIVICATIONID+4;
    static public final int MESSAGECOMING= 1;

	String name;//
	String serviceChannelid; //
    String appChannelid; //
    String callchannel="call channel";

	static private NotificationUtils instance;
	private NotificationManager mNotifManager;
	static  public NotificationUtils getInstance(Context context){
		if(instance==null){
			synchronized (NotificationUtils.class){
				if(instance==null){
					instance = new NotificationUtils(context);
				}
			}
		}
		return instance;
	}

	private NotificationUtils(Context context){
		mNotifManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			int importance = NotificationManager.IMPORTANCE_LOW;
            name = context.getString(R.string.app_name)+" Service";
            serviceChannelid = name+context.getPackageName();

            NotificationChannel mChannel = new NotificationChannel(serviceChannelid, name, importance);
            mChannel.setDescription(name);
            mChannel.enableVibration(false);
            mChannel.setSound(null,null);
            mChannel.enableLights(true); //
            mNotifManager.createNotificationChannel(mChannel);

            NotificationChannel defaul =new  NotificationChannel(callchannel, "dsfadsafa3", NotificationManager.IMPORTANCE_HIGH);
            mNotifManager.createNotificationChannel(defaul);
		}
	}



    public void updateNotifiChannel(Context context,Notification notification){
        Uri defaultSoundUri= RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        SharedPreferences preference = context.getSharedPreferences("setting", Context.MODE_PRIVATE);

        boolean enableRing = context.getResources().getBoolean(R.bool.prefrence_ring_default);
        boolean enableVibrate= context.getResources().getBoolean(R.bool.prefrence_vibrate_default);

        boolean needRing = preference.getBoolean(ConfigurationManager.PRESENCE_ENABLE_RING,enableRing);
        Uri defaultNotifiUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        String ringPath = preference.getString(ConfigurationManager.PRESENCE_IMRING,defaultNotifiUri==null?"":defaultNotifiUri.toString());
        boolean needVibrate = preference.getBoolean(ConfigurationManager.PRESENCE_ENABLE_VIBRATE,enableVibrate);
        long[]vibrate = null;
        Uri ring =null;
        if(needRing){
            ring = Uri.parse(ringPath);
        }
        if(needVibrate)
        {
            vibrate =  new long[]{0, 200, 0, 0};
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (!TextUtils.isEmpty(appChannelid)) {
                mNotifManager.deleteNotificationChannel(appChannelid);
            }

            name = context.getString(R.string.app_name)+ " Notification";
            appChannelid = name + context.getPackageName();
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel mChannel = new NotificationChannel(appChannelid, name, importance);
            if (ring != null) {
                mChannel.setSound(ring, null);
            }
            if (vibrate != null) {
                mChannel.setVibrationPattern(vibrate);
            }
            mNotifManager.createNotificationChannel(mChannel);
        }else {
            if(notification!=null) {
                if (ring != null) {
                    notification.sound = ring;
                }
                if (vibrate != null) {
                    notification.vibrate = vibrate;
                }
            }
        }
	    return;
    }

	public void showMessageNotification(Context context, int drawableId, String title,
										 String tickerText) {

		Intent intent = new Intent(context, PortActivityLogin.class);
		intent.putExtra("Entry",MESSAGECOMING);
		intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP|Intent.FLAG_ACTIVITY_NEW_TASK);

		PendingIntent contentIntent = PendingIntent.getActivity(context, MESSAGE_NOTIFICATION/*requestCode*/, intent, PendingIntent.FLAG_UPDATE_CURRENT);

		 Notification.Builder notificationBuilder= new Notification.Builder(context).
				setSmallIcon(drawableId).
				setContentTitle(title).
				setContentText(tickerText).
				setTicker(title)
                .setOngoing(false)
                .setAutoCancel(true)
				.setContentIntent(contentIntent);

		 if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            updateNotifiChannel(context,null);
			notificationBuilder.setChannelId(appChannelid);
		 }
		 final Notification notification =notificationBuilder.build();

         if (!(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)) {
            updateNotifiChannel(context, notification);
         }

		mNotifManager.notify(MESSAGE_NOTIFICATION, notification);
	}


    public void showSubNotification(Context context, int drawableId, String title,
                                               String tickerText) {
        Intent intent = new Intent(context, PortActivityLogin.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP|Intent.FLAG_ACTIVITY_NEW_TASK);

        PendingIntent contentIntent = PendingIntent.getActivity(context, MESSAGE_NOTIFICATION/*requestCode*/, intent, PendingIntent.FLAG_UPDATE_CURRENT);

		Notification.Builder notificationBuilder= new Notification.Builder(context).
                setSmallIcon(drawableId).
                setContentTitle(title).
                setContentText(tickerText).
                setTicker(title).
				setOngoing(false).
                setAutoCancel(true).
                setContentIntent(contentIntent);


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            updateNotifiChannel(context,null);
            notificationBuilder.setChannelId(appChannelid);
        }
        final Notification notification =notificationBuilder.build();

        if (!(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)) {
            updateNotifiChannel(context, notification);
        }

        mNotifManager.notify(MESSAGE_NOTIFICATION, notification);
    }

	//title =appname new long[]{0, 200,0,0}
	public void showAppNotification(Context context,int drawableId, String title,String tickerText,Intent intent)  {

		PendingIntent contentIntent = PendingIntent.getActivity(context, APP_NOTIFICATION/*requestCode*/, intent, PendingIntent.FLAG_UPDATE_CURRENT);
		Notification.Builder notificationBuilder= new Notification.Builder(context).
				setSmallIcon(drawableId).
				setContentTitle(title).
                setContentText(tickerText).
				setTicker(tickerText).
				setContentIntent(contentIntent);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationBuilder.setChannelId(serviceChannelid);
        }
        final Notification notification =notificationBuilder.build();

		notification.flags |= Notification.FLAG_ONGOING_EVENT;
		mNotifManager.notify(APP_NOTIFICATION, notification);
        ((Service)context).startForeground(APP_NOTIFICATION, notification);
		if(context instanceof Service) {
            ((Service)context).startForeground(APP_NOTIFICATION, notification);
        }
	}
    public void showPendingCallNotification(Context context,int callid,int drawableId, String title,String tickerText,Intent intent){
        PendingIntent contentIntent = PendingIntent.getActivity(context, 0, intent, 0);

        Intent answerAudioIntent = new Intent(context,PortIncallActivity.class);
        answerAudioIntent.setAction(PORT_ACTION_ACCEPT);
        answerAudioIntent.putExtra("video", false);
        answerAudioIntent.putExtra("CALLID", callid);
        PendingIntent answerAudioPendingIntent = PendingIntent.getActivity(context,0,answerAudioIntent,PendingIntent.FLAG_UPDATE_CURRENT);
        Intent answerVideoIntent = new Intent(context,PortIncallActivity.class);
        answerVideoIntent.setAction(PORT_ACTION_ACCEPT);
        answerAudioIntent.putExtra("video", true);
        answerAudioIntent.putExtra("CALLID", callid);
        PendingIntent answerVideoPendingIntent = PendingIntent.getActivity(context,0,answerAudioIntent,PendingIntent.FLAG_UPDATE_CURRENT);

        Intent rejectIntent = new Intent(context, PortSipService.class);
        rejectIntent.setAction(PORT_ACTION_REJECT);
        rejectIntent.putExtra("CALLID", callid);
        PendingIntent rejectPendingIntent = PendingIntent.getService(context,1,rejectIntent,PendingIntent.FLAG_UPDATE_CURRENT);

        RemoteViews headsUpView = new RemoteViews(context.getPackageName(),R.layout.headsup_notification_layout);
        headsUpView.setOnClickPendingIntent(R.id.iv_audio,answerAudioPendingIntent);
        headsUpView.setOnClickPendingIntent(R.id.iv_video,answerVideoPendingIntent);
        headsUpView.setOnClickPendingIntent(R.id.iv_hangup,rejectPendingIntent);
        headsUpView.setTextViewText(R.id.tv_remote,tickerText);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context,callchannel)
                .setSmallIcon(drawableId)

                .setContentTitle(title)
                .setContentText(tickerText)
                .setAutoCancel(true)
                .setShowWhen(true)
                .setContentIntent(contentIntent)
                .setFullScreenIntent(contentIntent, true)
                .setCustomContentView(headsUpView);
        mNotifManager.notify(PENDINGCALL_NOTIFICATION, builder.build());

    }


    public void updateAppNotification(Context context,int drawableId, String title
            ,String tickerText,Intent intent){

        PendingIntent contentIntent = PendingIntent.getActivity(context, 0/*requestCode*/, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(context,serviceChannelid);
        notificationBuilder.setSmallIcon(drawableId);
        notificationBuilder.setContentTitle(title);
        notificationBuilder.setTicker(tickerText);
        notificationBuilder.setContentText(tickerText);
        notificationBuilder.setAutoCancel(false);
        notificationBuilder.setOngoing(true);
        notificationBuilder.setPriority(NotificationCompat.PRIORITY_DEFAULT);
        notificationBuilder.setCategory(NotificationCompat.CATEGORY_CALL);
        notificationBuilder.setContentIntent(contentIntent);


        final Notification notification =notificationBuilder.build();
        mNotifManager.notify(APP_NOTIFICATION, notification);
    }

    public void cancelMessageNotification(Context context){
		mNotifManager.cancel(MESSAGE_NOTIFICATION);
	}

    public void cancelPendingCallNotification(Context context){
		mNotifManager.cancel(PENDINGCALL_NOTIFICATION);
	}

    public void cancelAppNotification(Context context){
		mNotifManager.cancel(APP_NOTIFICATION);
	}

    public void cancelAllNotification(Context context){
		mNotifManager.cancelAll();
	}
}
