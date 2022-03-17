package com.homwee.gcpclient;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Base64;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

import java.io.IOException;
import java.io.InputStream;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Date;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private static final int RC_SIGN_IN = 1001;
    private static final int RC_REQUEST_PERMISSION_SUCCESS_CONTINUE_FILE_CREATION = 1002;
    private static Context mContext;
    private GoogleSignInClient mGoogleSignInClient;
    private TextView tv_logstatus;
    private Button bt_signin, bt_signout, bt_createDevice, bt_getDeviceStatus, bt_connectIOTcore;
    private String idToken;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();
        mContext = this;

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                //.requestScopes(new Scope(Scopes.DRIVE_APPFOLDER))
                //.requestIdToken("913123775306-npeij2k0dclfgflhh7lak3r18njnbk2b.apps.googleusercontent.com")
                .requestIdToken("913123775306-3mt0etpov1bula2sa86lhrh9phj9bk1g.apps.googleusercontent.com")
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);
    }

    @Override
    protected void onStart() {
        super.onStart();
        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(this);
        if (account != null) {
            //TODO user had login
            tv_logstatus.setText("loged as: " + account.getDisplayName());
            Logutil.i("login state: " + account.getDisplayName());
            idToken = account.getIdToken();
//            if (!GoogleSignIn.hasPermissions(account, new Scope(Scopes.DRIVE_APPFOLDER))) {
//                GoogleSignIn.requestPermissions(this, RC_REQUEST_PERMISSION_SUCCESS_CONTINUE_FILE_CREATION, account, new Scope(Scopes.DRIVE_APPFOLDER));
//            } else {
//                Logutil.i("hasPermission: ");
//            }
        } else {
            tv_logstatus.setText("No account loged,please log in");
        }

        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(signInIntent);
        handleSignInResult(task);

    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Logutil.i("onActivityResult, " + "resultCode:" + resultCode + ", requestCode:" + requestCode);
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == RC_SIGN_IN) {
                // The Task returned from this call is always completed, no need to attach
                // a listener.
                Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
                handleSignInResult(task);
            } else if (requestCode == RC_REQUEST_PERMISSION_SUCCESS_CONTINUE_FILE_CREATION) {
                Logutil.i("get permission success!");
            }
        }
    }


    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.signin:
                signIn();
                break;
            case R.id.signout:
                signOut();
                break;
            case R.id.create_device:
                //createDevice();
                break;
            case R.id.authorize_device:
                authorizeDevice();
                break;
            case R.id.connect_iotcore:
                //connectIotCore();
                break;
        }
    }

    private void initView() {
        tv_logstatus = findViewById(R.id.google_logstatus);
        bt_signin = findViewById(R.id.signin);
        bt_signout = findViewById(R.id.signout);
        bt_createDevice = findViewById(R.id.create_device);
        bt_getDeviceStatus = findViewById(R.id.authorize_device);
        bt_connectIOTcore = findViewById(R.id.connect_iotcore);
        bt_signin.setOnClickListener(this);
        bt_signout.setOnClickListener(this);
        bt_createDevice.setOnClickListener(this);
        bt_getDeviceStatus.setOnClickListener(this);
        bt_connectIOTcore.setOnClickListener(this);
    }

    public void authorizeDevice() {
        OkHttpClient okHttpClient = new OkHttpClient.Builder()
                .retryOnConnectionFailure(true)
                .pingInterval(20, TimeUnit.SECONDS)
                .readTimeout(0, TimeUnit.SECONDS)
                .build();

        String jwtRsa = createJwtRsa("changhong-gcp-001");
        Logutil.i("JwtRsa: " + jwtRsa);
        Request request = new Request.Builder()
                .addHeader("authorization", " Bearer " + jwtRsa)
                .addHeader("idtoken", idToken)
                .addHeader("cache-control", "no-cache")
                .url("https://cloudiotdevice.googleapis.com/v1/projects/changhong-gcp-001/locations/europe-west1/registries/changhong-registry/devices/changhongTV/config?local_version=1")
                .build();

        okHttpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Logutil.i("fail: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                Logutil.i("response: " + response.body().string());
            }
        });
    }

    public void signIn() {
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    private void signOut() {
        mGoogleSignInClient.signOut()
                .addOnCompleteListener(this, new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        Logutil.i("signout success!");
                    }
                });
    }

    private void handleSignInResult(Task<GoogleSignInAccount> completedTask) {
        try {
            GoogleSignInAccount account = completedTask.getResult(ApiException.class);
            Logutil.i("SIGN GOOGLE success!");
            idToken = account.getIdToken();
            Logutil.i("idToken: " + idToken);
            // Signed in successfully, show authenticated UI.
            //updateUI(account);
        } catch (ApiException e) {
            // The ApiException status code indicates the detailed failure reason.
            // Please refer to the GoogleSignInStatusCodes class reference for more information.
            Logutil.i("signInResult:failed code=" + e.getStatusCode());
            Logutil.i("signInResult:failed code=" + e.getStatusMessage());
            //updateUI(null);
        }
    }

    /**
     * Create a Cloud IoT Core JWT for the given project id, signed with the given RSA key.
     */
    private static String createJwtRsa(String projectId) {

        Date date = new Date();
        // Create a JWT to authenticate this device. The device will be disconnected after the token
        // expires, and will have to reconnect with a new token. The audience field should always be set
        // to the GCP project id.
        JwtBuilder jwtBuilder = Jwts.builder()
                .setIssuedAt(date)
                .setExpiration(date)
                .setAudience(projectId);

        InputStream open = null;
        try {
            open = mContext.getAssets().open("rsa_private.pem");
            byte[] keyBytes = new byte[open.available()];
            open.read(keyBytes);

            String keyStr = new String(keyBytes, "UTF-8");
            Logutil.i("read key: " + keyStr);
            byte[] decode = Base64.decode(keyStr, Base64.NO_WRAP);

            PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(decode);
            KeyFactory kf = KeyFactory.getInstance("RSA");
            return jwtBuilder.signWith(SignatureAlgorithm.RS256, kf.generatePrivate(spec)).compact();
        } catch (IOException | NoSuchAlgorithmException | InvalidKeySpecException e) {
            e.printStackTrace();
        }
        return null;
    }

}
