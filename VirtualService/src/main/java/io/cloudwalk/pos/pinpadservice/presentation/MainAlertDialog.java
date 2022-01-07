package io.cloudwalk.pos.pinpadservice.presentation;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.view.View;
import android.widget.TextView;

import io.cloudwalk.loglibrary.Log;
import io.cloudwalk.pos.pinpadservice.R;
import io.cloudwalk.utilitieslibrary.Application;

public class MainAlertDialog extends AlertDialog {
    private static final String
            TAG = MainAlertDialog.class.getSimpleName();

    /**
     * Constructor.
     */
    private MainAlertDialog(Context context) {
        super(context);

        Log.d(TAG, "AboutAlertDialog");
    }

    /**
     * Constructor.
     */
    private MainAlertDialog(Context context, boolean cancelable, OnCancelListener cancelListener) {
        super(context, cancelable, cancelListener);

        Log.d(TAG, "AboutAlertDialog");
    }

    /**
     * Constructor.
     */
    private MainAlertDialog(Context context, int themeResId) {
        super(context, themeResId);

        Log.d(TAG, "AboutAlertDialog");
    }

    /**
     * Constructor.
     */
    protected MainAlertDialog(Activity activity) {
        super(activity);

        Log.d(TAG, "AboutAlertDialog");

        setIcon(R.mipmap.ic_launcher);

        setTitle(R.string.app_name);

        View view = getLayoutInflater().inflate(R.layout.alert_dialog_main, null);

        setView(view);

        Context context = Application.getPackageContext();

        String componentList = "\n";
        String versionName   = "";

        try {
            versionName = context.getPackageManager()
                    .getPackageInfo(context.getPackageName(), 0).versionName;
        } catch (Exception exception) {
            Log.e(TAG, Log.getStackTraceString(exception));
        }

        componentList += "\n\t\u2022 LogLibrary v"
                + io.cloudwalk.loglibrary.BuildConfig.VERSION_NAME;

        componentList += "\n\t\u2022 PinpadLibrary v"
                + io.cloudwalk.pos.pinpadlibrary.BuildConfig.VERSION_NAME;

        componentList += "\n\t\u2022 UtilitiesLibrary v"
                + io.cloudwalk.utilitieslibrary.BuildConfig.VERSION_NAME;

        String contentView = "Virtual Pinpad Service v" + versionName
                + componentList
                + "\n\n"
                + context.getString(R.string.content_about);

        ((TextView) view.findViewById(R.id.tv_about)).setText(contentView);

        setCanceledOnTouchOutside(false);
    }
}
