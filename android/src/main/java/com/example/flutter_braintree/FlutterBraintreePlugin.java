package com.example.flutter_braintree;

import android.app.Activity;
import android.content.Intent;
import android.util.Log;

import com.braintreepayments.api.PayPalVaultRequest;

import java.util.Map;

import androidx.annotation.NonNull;

import io.flutter.embedding.engine.plugins.FlutterPlugin;
import io.flutter.embedding.engine.plugins.activity.ActivityAware;
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;
import io.flutter.plugin.common.PluginRegistry.ActivityResultListener;

public class FlutterBraintreePlugin implements FlutterPlugin, ActivityAware, MethodCallHandler, ActivityResultListener {
    private static final int CUSTOM_ACTIVITY_REQUEST_CODE = 0x420;

    private Activity activity;
    private Result activeResult;
    private MethodChannel channel;

    @Override
    public void onAttachedToEngine(@NonNull FlutterPluginBinding binding) {
        channel = new MethodChannel(binding.getBinaryMessenger(), "flutter_braintree.custom");
        channel.setMethodCallHandler(this);
    }

    @Override
    public void onDetachedFromEngine(@NonNull FlutterPluginBinding binding) {
        if (channel != null) {
            channel.setMethodCallHandler(null);
            channel = null;
        }
    }

    @Override
    public void onAttachedToActivity(@NonNull ActivityPluginBinding binding) {
        activity = binding.getActivity();
        binding.addActivityResultListener(this);
    }

    @Override
    public void onDetachedFromActivityForConfigChanges() {
        activity = null;
    }

    @Override
    public void onReattachedToActivityForConfigChanges(@NonNull ActivityPluginBinding binding) {
        activity = binding.getActivity();
        binding.addActivityResultListener(this);
    }

    @Override
    public void onDetachedFromActivity() {
        activity = null;
    }

    @Override
    public void onMethodCall(MethodCall call, Result result) {
        if (activeResult != null) {
            result.error("already_running", "Cannot launch another custom activity while one is already running.", null);
            return;
        }
        activeResult = result;

        if (activity == null) {
            result.error("no_activity", "Plugin not attached to an activity.", null);
            activeResult = null;
            return;
        }

        switch (call.method) {
            case "tokenizeCreditCard": {
                Intent intent = new Intent(activity, FlutterBraintreeCustom.class);
                intent.putExtra("type", "tokenizeCreditCard");
                intent.putExtra("authorization", (String) call.argument("authorization"));
                Map request = (Map) call.argument("request");
                intent.putExtra("cardNumber", (String) request.get("cardNumber"));
                intent.putExtra("expirationMonth", (String) request.get("expirationMonth"));
                intent.putExtra("expirationYear", (String) request.get("expirationYear"));
                intent.putExtra("cvv", (String) request.get("cvv"));
                activity.startActivityForResult(intent, CUSTOM_ACTIVITY_REQUEST_CODE);
                break;
            }
            case "requestPaypalNonce": {
                Intent intent = new Intent(activity, FlutterBraintreeCustom.class);
                intent.putExtra("type", "requestPaypalNonce");
                intent.putExtra("authorization", (String) call.argument("authorization"));
                Map request = (Map) call.argument("request");
                intent.putExtra("amount", (String) request.get("amount"));
                intent.putExtra("currencyCode", (String) request.get("currencyCode"));
                intent.putExtra("displayName", (String) request.get("displayName"));
                intent.putExtra("billingAgreementDescription", (String) request.get("billingAgreementDescription"));
                activity.startActivityForResult(intent, CUSTOM_ACTIVITY_REQUEST_CODE);
                break;
            }
            case "isApplePayAvailable":
                result.success(false); // iOS only
                activeResult = null;
                break;

            case "isGooglePayAvailable": {
                Intent intent = new Intent(activity, FlutterBraintreeCustom.class);
                intent.putExtra("type", "isGooglePayAvailable");
                intent.putExtra("authorization", (String) call.argument("authorization"));
                activity.startActivityForResult(intent, CUSTOM_ACTIVITY_REQUEST_CODE);
                break;
            }
            case "collectDeviceData": {
                Intent intent = new Intent(activity, FlutterBraintreeCustom.class);
                intent.putExtra("type", "collectDeviceData");
                intent.putExtra("authorization", (String) call.argument("authorization"));
                activity.startActivityForResult(intent, CUSTOM_ACTIVITY_REQUEST_CODE);
                break;
            }
            case "payWithGooglePay": {
                Intent intent = new Intent(activity, FlutterBraintreeCustom.class);
                intent.putExtra("type", "payWithGooglePay");
                intent.putExtra("authorization", (String) call.argument("authorization"));
                intent.putExtra("testing", (boolean) call.argument("testing"));
                intent.putExtra("label", (String) call.argument("label"));
                intent.putExtra("currencyCode", (String) call.argument("currencyCode"));
                intent.putExtra("total", (String) call.argument("total"));
                activity.startActivityForResult(intent, CUSTOM_ACTIVITY_REQUEST_CODE);
                break;
            }
            default:
                result.notImplemented();
                activeResult = null;
                break;
        }
    }

    @Override
    public boolean onActivityResult(int requestCode, int resultCode, Intent data) {
        if (activeResult == null) return false;

        if (requestCode == CUSTOM_ACTIVITY_REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                String type = data.getStringExtra("type");
                switch (type) {
                    case "paymentMethodNonce":
                        activeResult.success(data.getSerializableExtra("paymentMethodNonce"));
                        break;
                    case "isGooglePayAvailable":
                        activeResult.success(data.getBooleanExtra("result", false));
                        break;
                    case "collectDeviceData":
                        activeResult.success(data.getStringExtra("result"));
                        break;
                    default:
                        activeResult.error("error", "Invalid activity result type.", null);
                        break;
                }
            } else if (resultCode == Activity.RESULT_CANCELED) {
                activeResult.success(null);
            } else {
                Exception error = (Exception) data.getSerializableExtra("error");
                activeResult.error("error", error.getMessage(), null);
            }
            activeResult = null;
            return true;
        }

        return false;
    }
}
