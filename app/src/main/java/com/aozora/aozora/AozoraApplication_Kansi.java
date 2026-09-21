package com.aozora.aozora;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;

public class AozoraApplication_Kansi extends Application implements Application.ActivityLifecycleCallbacks {

    private int activityReferences = 0;
    private boolean isActivityChangingConfigurations = false;
    private boolean appWentToBackground = false;

    @Override
    public void onCreate() {
        super.onCreate();
        registerActivityLifecycleCallbacks(this);
    }

    @Override
    public void onActivityStarted(Activity activity) {
        if (++activityReferences == 1 && !isActivityChangingConfigurations) {
            // 本当にバックグラウンドから復帰した
        }
    }

    @Override
    public void onActivityStopped(Activity activity) {
        isActivityChangingConfigurations = activity.isChangingConfigurations();
        if (--activityReferences == 0 && !isActivityChangingConfigurations) {
            // アプリ全体が裏に回った（ホーム/他アプリ切替/画面ロック等）
            appWentToBackground = true;
        }
    }

    public boolean isAppWentToBackground() {
        return appWentToBackground;
    }

    public void clearAppWentToBackground() {
        appWentToBackground = false;
    }

    @Override public void onActivityCreated(Activity activity, Bundle savedInstanceState) {}
    @Override public void onActivityResumed(Activity activity) {}
    @Override public void onActivityPaused(Activity activity) {}
    @Override public void onActivitySaveInstanceState(Activity activity, Bundle outState) {}
    @Override public void onActivityDestroyed(Activity activity) {}
}