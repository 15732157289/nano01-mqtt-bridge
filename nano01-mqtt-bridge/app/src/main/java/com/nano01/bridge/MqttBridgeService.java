package com.nano01.bridge;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import javax.net.ssl.SSLSocketFactory;

public class MqttBridgeService extends Service {

    private static final String TAG = "Nano01Bridge";

    // 车机本地MQTT
    private static final String LOCAL_HOST = "tcp://127.0.0.1:1883";
    private static final String LOCAL_CLIENT_ID = "nano01_local_bridge";

    // EMQX Cloud远程MQTT
    private static final String REMOTE_HOST = "ssl://f0c26efb.ala.asia-southeast1.emqxsl.com:8883";
    private static final String REMOTE_CLIENT_ID = "nano01_remote_bridge";
    private static final String REMOTE_USER = "dongfeng";
    private static final String REMOTE_PASS = "1qaz2wsx";
    private static final String TOPIC_PREFIX = "nano01";

    private MqttClient localClient;
    private MqttClient remoteClient;
    private boolean running = false;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG, "服务创建");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (!running) {
            running = true;
            new Thread(this::connectBoth).start();
        }
        return START_STICKY;
    }

    private void connectBoth() {
        // 连接远程EMQX Cloud
        while (!connectRemote()) {
            Log.e(TAG, "远程连接失败，3秒后重试...");
            try { Thread.sleep(3000); } catch (InterruptedException e) { return; }
        }

        // 连接本地车机MQTT
        while (!connectLocal()) {
            Log.e(TAG, "本地连接失败，3秒后重试...");
            try { Thread.sleep(3000); } catch (InterruptedException e) { return; }
        }

        Log.i(TAG, "桥接运行中！");
    }

    private boolean connectRemote() {
        try {
            remoteClient = new MqttClient(REMOTE_HOST, REMOTE_CLIENT_ID, null);
            MqttConnectOptions opts = new MqttConnectOptions();
            opts.setUserName(REMOTE_USER);
            opts.setPassword(REMOTE_PASS.toCharArray());
            opts.setAutomaticReconnect(true);
            opts.setCleanSession(true);
            opts.setSocketFactory(SSLSocketFactory.getDefault());
            opts.setConnectionTimeout(10);
            opts.setKeepAliveInterval(30);
            remoteClient.connect(opts);
            Log.i(TAG, "远程EMQX Cloud连接成功");
            return true;
        } catch (Exception e) {
            Log.e(TAG, "远程连接错误: " + e.getMessage());
            return false;
        }
    }

    private boolean connectLocal() {
        try {
            localClient = new MqttClient(LOCAL_HOST, LOCAL_CLIENT_ID, null);
            MqttConnectOptions opts = new MqttConnectOptions();
            opts.setAutomaticReconnect(true);
            opts.setCleanSession(true);
            opts.setConnectionTimeout(10);
            opts.setKeepAliveInterval(30);

            localClient.setCallback(new MqttCallback() {
                @Override
                public void connectionLost(Throwable cause) {
                    Log.w(TAG, "本地连接断开: " + cause.getMessage());
                }

                @Override
                public void messageArrived(String topic, MqttMessage message) {
                    forwardMessage(topic, message);
                }

                @Override
                public void deliveryComplete(IMqttDeliveryToken token) {
                }
            });

            localClient.connect(opts);
            localClient.subscribe("#", 0);
            Log.i(TAG, "本地车机MQTT连接成功，已订阅所有主题 #");
            return true;
        } catch (Exception e) {
            Log.e(TAG, "本地连接错误: " + e.getMessage());
            return false;
        }
    }

    private void forwardMessage(String topic, MqttMessage message) {
        try {
            if (remoteClient != null && remoteClient.isConnected()) {
                String remoteTopic = TOPIC_PREFIX + "/" + topic;
                MqttMessage msg = new MqttMessage(message.getPayload());
                msg.setQos(0);
                remoteClient.publish(remoteTopic, msg);
                Log.d(TAG, "转发: " + remoteTopic + " (" + message.getPayload().length + "字节)");
            }
        } catch (Exception e) {
            Log.e(TAG, "转发失败: " + e.getMessage());
        }
    }

    @Override
    public void onDestroy() {
        running = false;
        try { if (localClient != null) localClient.disconnect(); } catch (Exception e) {}
        try { if (remoteClient != null) remoteClient.disconnect(); } catch (Exception e) {}
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
