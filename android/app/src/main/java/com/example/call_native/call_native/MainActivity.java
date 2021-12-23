package com.example.call_native.call_native;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.net.Uri;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.facebook.*;
import com.facebook.appevents.AppEventsLogger;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.inappmessaging.FirebaseInAppMessaging;
import com.google.firebase.inappmessaging.FirebaseInAppMessagingClickListener;
import com.google.firebase.inappmessaging.FirebaseInAppMessagingDisplay;
import com.google.firebase.inappmessaging.FirebaseInAppMessagingDisplayCallbacks;
import com.google.firebase.inappmessaging.model.Action;
import com.google.firebase.inappmessaging.model.CampaignMetadata;
import com.google.firebase.inappmessaging.model.InAppMessage;
import com.google.firebase.installations.FirebaseInstallations;
import io.flutter.embedding.android.FlutterActivity;
import io.flutter.embedding.engine.FlutterEngine;
import io.flutter.plugin.common.*;
import io.flutter.plugins.GeneratedPluginRegistrant;
import org.jetbrains.annotations.NotNull;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Map;

public class MainActivity extends FlutterActivity {

    private final String CHANNEL = "flutterplugins.javatpoint.com/browser";
    private final String FACEBOOK_LOGIN_CHANNEL = "call_native.example.com/facebook_login";
    private final String EVENT_CHANNEL = "call_native.example.com/listen_message";
    private final String BASIC_MESSAGE_CHANNEL = "call_native.example.com/listen_basic_message";
    private CallbackManager mCallbackManager;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        printKeyHash();

        AppEventsLogger.activateApp(getApplication());
        new MethodChannel(getFlutterEngine().getDartExecutor(), CHANNEL).setMethodCallHandler((call, result) -> {
            String url = call.argument("url");
            if (call.method.equals("call_native_open_url")) {
                openBrowser(call, result, url);
            } else {
                result.notImplemented();
            }
        });
        new MethodChannel(getFlutterEngine().getDartExecutor(), FACEBOOK_LOGIN_CHANNEL).setMethodCallHandler((call, result) -> {
            if (call.method.equals("open_facebook")) {
                openFacebook(call, result);
            } else {
                result.notImplemented();
            }
        });

        new EventChannel(getFlutterEngine().getDartExecutor(), EVENT_CHANNEL).setStreamHandler(new EventChannel.StreamHandler() {
            @Override
            public void onListen(Object arguments, EventChannel.EventSink events) {
                configFireBase(events);
            }

            @Override
            public void onCancel(Object arguments) {

            }
        });

        BasicMessageChannel<String> basicMessageChannel = new BasicMessageChannel<>(getFlutterEngine().getDartExecutor(), BASIC_MESSAGE_CHANNEL, StringCodec.INSTANCE);
        basicMessageChannel.setMessageHandler((message, reply) -> {
            Log.d("AAAAAAAA", "Message send by flutter: " + message);
            basicMessageChannel.send("Flutter was sent \"" + message + "\"");
        });
    }

    private void configFireBase(EventChannel.EventSink events) {
        FirebaseDatabase.getInstance().getReference("message").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull @NotNull DataSnapshot snapshot) {
                if(snapshot.getValue() != null){
                    Log.d("Message Firebase", "onDataChange: " + snapshot.getValue());
                    events.success(snapshot.getValue());
                }

            }

            @Override
            public void onCancelled(@NonNull @NotNull DatabaseError error) {

            }
        });


        FirebaseInstallations.getInstance().getId().addOnSuccessListener(new OnSuccessListener<String>() {
            @Override
            public void onSuccess(String s) {
                Log.d("FirebaseInstallations", "onSuccess: " + s);
            }
        });
        FirebaseInAppMessaging.getInstance().setMessageDisplayComponent(new FirebaseInAppMessagingDisplay() {
            @Override
            public void displayMessage(@NonNull @NotNull InAppMessage inAppMessage, @NonNull @NotNull FirebaseInAppMessagingDisplayCallbacks firebaseInAppMessagingDisplayCallbacks) {

            }
        });
        FirebaseInAppMessaging.getInstance().addClickListener(new FirebaseInAppMessagingClickListener() {
            @Override
            public void messageClicked(@NonNull @NotNull InAppMessage inAppMessage, @NonNull @NotNull Action action) {
                String url = action.getActionUrl();
                CampaignMetadata metadata = inAppMessage.getCampaignMetadata();
                Map map = inAppMessage.getData();
                Log.d(
                        "MyClickListener",
                        "url:" +  url + "metadata:" + metadata.getCampaignId() +  metadata.getCampaignName() + map
                );

            }
        });
    }

    private void openFacebook(MethodCall call, MethodChannel.Result result) {

        mCallbackManager = CallbackManager.Factory.create();

        LoginManager.getInstance().registerCallback(mCallbackManager,
                new FacebookCallback<LoginResult>() {
                    @Override
                    public void onSuccess(LoginResult loginResult) {
                        Log.e("AAAAAAAAAAAa", "Login access token: " + loginResult.getAccessToken().getToken());
                        result.success(loginResult.getAccessToken().getToken());
                    }

                    @Override
                    public void onCancel() {
                        Toast.makeText(MainActivity.this, "Login Cancel", Toast.LENGTH_LONG).show();
                    }

                    @Override
                    public void onError(FacebookException exception) {
                        Toast.makeText(MainActivity.this, exception.getMessage(), Toast.LENGTH_LONG).show();
                    }
                });

        LoginManager.getInstance().logIn(this, Arrays.asList("user_friends"));
        Log.d("Open facebook: ", "openFacebook: ");

    }

    private void openBrowser(MethodCall call, MethodChannel.Result result, String url) {
        Activity activity = this;
        if (activity == null) {
            result.error("UNAVAILABLE", "It cannot open the browser without foreground activity", null);
            return;
        }
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse(url));
        activity.startActivity(intent);
        result.success(true);

    }

    @Override
    public void configureFlutterEngine(@NonNull FlutterEngine flutterEngine) {
        GeneratedPluginRegistrant.registerWith(flutterEngine);
    }

    private void printKeyHash() {
        try {
            PackageInfo info = getPackageManager().getPackageInfo(getPackageName(), PackageManager.GET_SIGNATURES);
            for (Signature signature : info.signatures) {
                MessageDigest md = MessageDigest.getInstance("SHA1");
                md.update(signature.toByteArray());
                Log.d("KeyHash:", Base64.encodeToString(md.digest(), Base64.DEFAULT));
            }
        } catch (PackageManager.NameNotFoundException e) {
            Log.e("KeyHash:", e.toString());
        } catch (NoSuchAlgorithmException e) {
            Log.e("KeyHash:", e.toString());
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        mCallbackManager.onActivityResult(requestCode, resultCode, data);
    }

}
