package com.nano01.bridge;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        TextView tv = new TextView(this);
        tv.setText("纳米01 MQTT桥接服务\n\n" +
                "本地: tcp://127.0.0.1:1883\n" +
                "远程: ssl://f0c26efb.ala.asia-southeast1.emqxsl.com:8883\n" +
                "主题前缀: nano01/\n\n" +
                "服务已启动，开机自动运行。\n" +
                "可通过logcat查看日志: adb logcat -s Nano01Bridge");
        tv.setPadding(40, 40, 40, 40);
        tv.setTextSize(16);
        setContentView(tv);

        // 启动桥接服务
        Intent serviceIntent = new Intent(this, MqttBridgeService.class);
        startService(serviceIntent);
    }
}
