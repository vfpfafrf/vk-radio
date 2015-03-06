package io.daydev.vkrdo;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.util.Log;
import com.vk.sdk.*;
import com.vk.sdk.api.VKError;
import com.vk.sdk.dialogs.VKCaptchaDialog;
import io.daydev.vkrdo.bean.Configuration;

/**
 * Activity due vk.com recomendation
 */
public abstract class VKActivity extends Activity {


    private static final String[] sMyScope = new String[]{
            VKScope.AUDIO,
            VKScope.OFFLINE
    };

    @Override
    protected void onDestroy() {
        super.onStop();
        VKUIHelper.onDestroy(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        VKUIHelper.onResume(this);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        try {
            VKUIHelper.onActivityResult(this, requestCode, resultCode, data);
        } catch (Exception e) {
            Log.e("MainActivity", "VK error", e);
        }
    }

    protected void initSdk(Configuration configuration){
        VKSdk.initialize(vkSdkListener, configuration.getVkApp());
        if (!VKSdk.wakeUpSession()) {
            VKSdk.authorize(new String[]{VKScope.AUDIO, VKScope.OFFLINE}, true, false);
        }
    }

    protected final VKSdkListener vkSdkListener = new VKSdkListener() {
        @Override
        public void onCaptchaError(VKError captchaError) {
            new VKCaptchaDialog(captchaError).show();
        }

        @Override
        public void onTokenExpired(VKAccessToken expiredToken) {
            VKSdk.authorize(sMyScope);
        }

        @Override
        public void onAccessDenied(final VKError authorizationError) {
            new AlertDialog.Builder(VKUIHelper.getTopActivity())
                    .setMessage(authorizationError.toString())
                    .show();
        }

        @Override
        public void onReceiveNewToken(VKAccessToken newToken) {
        }

        @Override
        public void onAcceptUserToken(VKAccessToken token) {
        }
    };


}
