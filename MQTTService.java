package com.homwee.gcpclient.mqtt;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import com.homwee.gcpclient.GCPClentApplication;
import com.homwee.gcpclient.JWTUtil;
import com.homwee.gcpclient.Logutil;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

public class MQTTService extends Service {

    public static final String BROKER_URL = "ssl://mqtt.googleapis.com:8883";
    public static final String CLIENT_ID = "projects/changhong-gcp-001/locations/europe-west1/registries/changhong-registry/devices/changhongTV";
    //订阅的主题
    public static final String TOPIC = "projects/changhong-gcp-001/topics/changhong-device-events/#";
    public static MqttClient mqttClient;
    //mqtt连接配置
    private MqttConnectOptions mqttOptions;
    private String username = "unused";

    public MQTTService() {

    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Logutil.i( "onStartCommand: begin");

        try {
            //第三个参数代表持久化客户端，如果为null，则不持久化
            mqttClient = new MqttClient(BROKER_URL, CLIENT_ID, new MemoryPersistence());
            //mqtt连接设置
            mqttOptions = new MqttConnectOptions();
            mqttOptions.setMqttVersion(MqttConnectOptions.MQTT_VERSION_3_1_1);
            mqttOptions.setUserName(username);

            mqttOptions.setPassword(JWTUtil.createJwtRsa(GCPClentApplication.PROJECT_ID).toCharArray());
            //超时连接，单位为秒
            mqttOptions.setConnectionTimeout(10);
            mqttOptions.setKeepAliveInterval(20);
            //false代表可以接受离线消息
            mqttOptions.setCleanSession(false);
            mqttOptions.setAutomaticReconnect(true);
            // 设置回调
            mqttClient.setCallback(new PushCallback(mqttClient));
            Logutil.i("onStartCommand: before connect");
            //客户端下线，其它客户端或者自己再次上线可以接收"遗嘱"消息. 需要注释以下两句，
            //否则会报“java.lang.IllegalArgumentException: The topic name MUST NOT contain any wildcard characters (#+)” 错误
            //  MqttTopic topic1 = mqttClient.getTopic(TOPIC);
            //  mqttOptions.setWill(topic1, "close".getBytes(), 2, true);
            mqttClient.connect(mqttOptions);
            Logutil.i("onStartCommand: after connect");
            Logutil.i("连接mqtt服务器成功");

            //mqtt客户端订阅主题
            //在mqtt中用QoS来标识服务质量
            //QoS=0时，报文最多发送一次，有可能丢失
            //QoS=1时，报文至少发送一次，有可能重复
            //QoS=2时，报文只发送一次，并且确保消息只到达一次。
            int[] qos = {0};
            String[] topic = {TOPIC};
            mqttClient.subscribe(topic, qos);

        } catch (MqttException e) {
            e.printStackTrace();
            Logutil.e(e.getMessage());
        }
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        if (mqttClient != null){
            try {
                mqttClient.disconnect();
            } catch (MqttException e) {
                e.printStackTrace();
            }
        }

    }

}
