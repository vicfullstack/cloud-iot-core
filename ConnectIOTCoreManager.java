package com.homwee.gcpclient;

import android.util.Log;

import java.util.concurrent.TimeUnit;

import androidx.annotation.NonNull;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;

public class ConnectIOTCoreManager extends WebSocketListener {

    private static OkHttpClient client;

    public void init(/*String serverAddr*/) {
        String serverAddr = "https://cloudiotdevice.googleapis.com/v1/projects/changhong-gcp-001/locations/europe-west1/registries/changhong-registry/devices/changhongTV";
        Logutil.i("serverAddr: " + serverAddr);
        client = new OkHttpClient.Builder()
                .retryOnConnectionFailure(true)
                .pingInterval(20, TimeUnit.SECONDS)
                .readTimeout(0, TimeUnit.SECONDS)
                .build();
        String jwtRsa = JWTUtil.createJwtRsa(GCPClentApplication.PROJECT_ID);
        Request request = new Request.Builder()
                .url(serverAddr)
                .addHeader("authorization", " Bearer " + jwtRsa)
                .build();
        client.newWebSocket(request, this);

        // Trigger shutdown of the dispatcher's executor so this process can exit cleanly.
        client.dispatcher().executorService().shutdown();
    }

    public void stop() {
        client.dispatcher().cancelAll();
    }

    @Override
    public void onMessage(WebSocket webSocket, String text) {
        super.onMessage(webSocket, text);
        Logutil.i("onMessage");
    }

    @Override
    public void onOpen(WebSocket webSocket, Response response) {
        super.onOpen(webSocket, response);
        Logutil.i("onOpen");
    }

    @Override
    public void onFailure(WebSocket webSocket, Throwable t, Response response) {
        super.onFailure(webSocket, t, response);

        Logutil.i("onFailure code: " + response.code() +", message:"+ response.message() );

    }
}
