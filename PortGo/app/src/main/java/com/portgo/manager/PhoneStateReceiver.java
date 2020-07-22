package com.portgo.manager;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Binder;
import android.os.IBinder;
import android.telephony.TelephonyManager;

import com.portgo.BuildConfig;
import com.portgo.R;
import com.portgo.ui.PortActivityDialerSelector;
import com.portgo.ui.PortActivityLogin;

import java.lang.reflect.Method;

public class PhoneStateReceiver extends BroadcastReceiver {
    public static final String INTERNAL_CALL = "456c379";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        String phoneNumber = "";
        int callAction = TelephonyManager.CALL_STATE_IDLE;
        boolean internalcall = false;
        String resultData = this.getResultData();
        if (Intent.ACTION_NEW_OUTGOING_CALL.equals(intent.getAction())) {
            String type = intent.getType();

            phoneNumber = intent.getStringExtra(Intent.EXTRA_PHONE_NUMBER);

            if (phoneNumber != null && phoneNumber.endsWith(INTERNAL_CALL)) {
                internalcall = true;
            }
            boolean navitveDialer = ConfigurationManager.getInstance().getBooleanValue(context, ConfigurationManager.PRESENCE_NATIVE_DIALER, context.getResources().getBoolean(R.bool.prefrence_native_dialer));
            boolean defaultDialer = ConfigurationManager.getInstance().getBooleanValue(context, ConfigurationManager.PRESENCE_NATIVE_DIALER_ACTIVITY, context.getResources().getBoolean(R.bool.prefrence_native_dialer));
            if (navitveDialer && !internalcall) {
                endCall(context);
//                        Intent buttonUp = new Intent(Intent.ACTION_MEDIA_BUTTON);
//                        buttonUp.putExtra(Intent.EXTRA_KEY_EVENT, new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_HEADSETHOOK));
//                        context.sendOrderedBroadcast(buttonUp, "android.permission.CALL_PRIVILEGED");
                setResultData(null);
//                        this.abortBroadcast();
                if (defaultDialer) {
                    Uri uri = Uri.fromParts("tel", phoneNumber, null);
                    Intent appDialer = new Intent(context, PortActivityLogin.class);
                    appDialer.setAction(Intent.ACTION_CALL);
                    appDialer.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    appDialer.setData(uri);
                    context.startActivity(appDialer);
                } else {
                    Intent dialerSelector = new Intent(context, PortActivityDialerSelector.class);
                    dialerSelector.setAction(BuildConfig.PORT_ACTION_DIALERVIEW);
                    dialerSelector.putExtra(Intent.EXTRA_PHONE_NUMBER, phoneNumber);
                    dialerSelector.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    context.startActivity(dialerSelector);
                }
//                    }
            } else {
                if (internalcall) {
                    phoneNumber = phoneNumber.replaceFirst(INTERNAL_CALL, "");
                    setResultData(phoneNumber);
                }

            }
        } else {

            String state = intent.getStringExtra(TelephonyManager.EXTRA_STATE);
            //
            phoneNumber = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER);
            callAction = TelephonyManager.CALL_STATE_OFFHOOK;

            if (state.equalsIgnoreCase(TelephonyManager.EXTRA_STATE_RINGING)) {
                callAction = TelephonyManager.CALL_STATE_RINGING;
            } else if (state.equalsIgnoreCase(TelephonyManager.EXTRA_STATE_OFFHOOK)) {
                callAction = TelephonyManager.CALL_STATE_OFFHOOK;
            }
            if (state.equalsIgnoreCase(TelephonyManager.EXTRA_STATE_IDLE)) {
                callAction = TelephonyManager.CALL_STATE_IDLE;
            }

            if (AccountManager.getInstance().getLoginState() == UserAccount.STATE_ONLINE) {
                switch (callAction) {
                    case TelephonyManager.CALL_STATE_IDLE:      //
                        PortSipCall call = CallManager.getInstance().getDefaultCall();
                        if (call != null) {
                            if (SipManager.getSipManager().isConference()) {//
                                CallManager.getInstance().unholdAllcallsExcept(null);//
                            } else {
                                call.unHold();//
                            }
                        }
                        break;
                    case TelephonyManager.CALL_STATE_RINGING:   //
                    case TelephonyManager.CALL_STATE_OFFHOOK:   //
                        SipManager.getSipManager().holdAllExcept(null);
                        break;
                }
            }
        }
    }


    public static boolean endCall(Context context) {

        try {
            final Class<?> telephonyClass = Class.forName("com.android.internal.telephony.ITelephony");
            final Class<?> telephonyStubClass = telephonyClass.getClasses()[0];
            final Class<?> serviceManagerClass = Class.forName("android.os.ServiceManager");
            final Class<?> serviceManagerNativeClass = Class.forName("android.os.ServiceManagerNative");
            final Method getService = serviceManagerClass.getMethod("getService", String.class);
            final Method tempInterfaceMethod = serviceManagerNativeClass.getMethod("asInterface", IBinder.class);
            final Binder tmpBinder = new Binder();
            tmpBinder.attachInterface(null, "fake");
            final Object serviceManagerObject = tempInterfaceMethod.invoke(null, tmpBinder);
            final IBinder retbinder = (IBinder) getService.invoke(serviceManagerObject, "phone");
            final Method serviceMethod = telephonyStubClass.getMethod("asInterface", IBinder.class);
            final Object telephonyObject = serviceMethod.invoke(null, retbinder);
            final Method telephonyEndCall = telephonyClass.getMethod("endCall");
            telephonyEndCall.invoke(telephonyObject);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }
}