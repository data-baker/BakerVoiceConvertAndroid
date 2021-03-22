package com.databaker.bakervoiceconvertandroid;

import android.Manifest;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.databaker.voiceconvert.VoiceConvertManager;
import com.databaker.voiceconvert.callback.AuthCallback;
import com.databaker.voiceconvert.util.Utils;

import static android.content.pm.PackageManager.PERMISSION_GRANTED;

public class AuthActivity extends AppCompatActivity {

    public static final String SP_NAME = "dbvc";
    public static final String CLIENT_ID = "CLIENT_ID";
    public static final String CLIENT_SECRET = "CLIENT_SECRET";

    private static final int PERMISSION_REQ_CODE = 100;

    private String[] PERMISSION_ARRAY = new String[]{Manifest.permission.RECORD_AUDIO};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_auth);


        reqPermission();

        EditText etClientId = findViewById(R.id.etClientId);
        EditText etClientSecret = findViewById(R.id.etClientSecret);
        Button btnAuth = findViewById(R.id.btnAuthorization);
        btnAuth.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String clientId = etClientId.getText().toString();
                String clientSecret = etClientSecret.getText().toString();

                if (clientId.isEmpty() || clientSecret.isEmpty()) {
                    Toast.makeText(AuthActivity.this, "请填写完整", Toast.LENGTH_SHORT).show();
                    return;
                }

                btnAuth.setEnabled(false);
                btnAuth.setText("正在授权...");
                VoiceConvertManager.getInstance().auth(getApplicationContext(),
                        clientId,
                        clientSecret, new AuthCallback() {
                            @Override
                            public void onResult(boolean result) {
                                if (result) {
                                    getSharedPreferences(SP_NAME, MODE_PRIVATE).edit().putString(CLIENT_ID, clientId).apply();
                                    getSharedPreferences(SP_NAME, MODE_PRIVATE).edit().putString(CLIENT_SECRET, clientSecret).apply();
                                    startActivity(new Intent(AuthActivity.this, MainActivity.class));
                                    finish();
                                } else {
                                    Toast.makeText(AuthActivity.this, "鉴权失败", Toast.LENGTH_SHORT).show();
                                }
                            }
                        });
            }
        });


    }

    private void reqPermission() {
        if (Build.VERSION.SDK_INT > 23) {
            int resultCode = ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO);
            if (resultCode != PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, PERMISSION_ARRAY, PERMISSION_REQ_CODE);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERMISSION_REQ_CODE) {
            int result = grantResults[0];
            if (result == PERMISSION_GRANTED) {
                Utils.log("权限获取成功");
            } else {
                finish();
            }
        }
    }
}