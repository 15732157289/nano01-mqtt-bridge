package com.nano01.bridge;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            Log.i("Nano01Bridge", "开机自启，启动桥接服务");
            Intent serviceIntent = new Intent(context, MqttBridgeService.class);
            context.startService(serviceIntent);
        }
    }
}
