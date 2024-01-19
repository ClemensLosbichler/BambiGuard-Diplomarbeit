package at.ac.szybbs.bambiguard.dji;

import android.app.AlertDialog;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.core.content.ContextCompat;
import at.ac.szybbs.bambiguard.R;
import dji.common.error.DJIError;
import dji.common.error.DJISDKError;
import dji.sdk.base.BaseComponent;
import dji.sdk.base.BaseProduct;
import dji.sdk.sdkmanager.DJISDKInitEvent;
import dji.sdk.sdkmanager.DJISDKManager;

public class DJIApplication extends Application {

    public static final String FLAG_CONNECTION_CHANGE = "dji_connection_change";

    private static BaseProduct product;
    private static DJIApplication singletonInstance;
    public Handler handler;
    private Application instance;
    private final Runnable updateRunnable = () -> {
        Intent intent = new Intent(FLAG_CONNECTION_CHANGE);
        getApplicationContext().sendBroadcast(intent);
    };

    public static DJIApplication getInstance() {
        return singletonInstance;
    }

    public static synchronized BaseProduct getProductInstance() {
        if (product == null)
            product = DJISDKManager.getInstance().getProduct();
        return product;
    }

    public void setContext(Application application) {
        instance = application;
        singletonInstance = this;
    }

    @Override
    public Context getApplicationContext() {
        return instance;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        handler = new Handler(Looper.getMainLooper());
    }

    public boolean registerApplication() {
        if (product != null)
            return true;

        Log.d("kek", "register Application");

        DJISDKManager.SDKManagerCallback djiSdkManagerCallback = new DJISDKManager.SDKManagerCallback() {
            @Override
            public void onRegister(DJIError djiError) {
                if (djiError == DJISDKError.REGISTRATION_SUCCESS) {
                    DJISDKManager.getInstance().startConnectionToProduct();
                } else {
                    showNoInternetConnectionDialog();
                    Log.d("kek", "err " + djiError.getDescription());
                }
            }

            @Override
            public void onProductDisconnect() {
                notifyStatusChange();
            }

            @Override
            public void onProductConnect(BaseProduct baseProduct) {
                notifyStatusChange();
            }

            @Override
            public void onProductChanged(BaseProduct baseProduct) {
                notifyStatusChange();
            }

            @Override
            public void onComponentChange(BaseProduct.ComponentKey componentKey, BaseComponent oldComponent, BaseComponent newComponent) {
                if (newComponent != null) {
                    newComponent.setComponentListener(isConnected -> notifyStatusChange());
                }
            }

            @Override
            public void onInitProcess(DJISDKInitEvent djisdkInitEvent, int i) {
            }

            @Override
            public void onDatabaseDownloadProgress(long l, long l1) {
            }
        };

        if (permissionsGranted()) {
            DJISDKManager.getInstance().registerApp(getApplicationContext(), djiSdkManagerCallback);
            return true;
        }

        Log.d("kek", "no permisisons WTF");

        return false;
    }

    private void showNoInternetConnectionDialog() {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);

        alertDialogBuilder.setTitle(R.string.error_when_connecting_with_server);
        alertDialogBuilder
                .setMessage(R.string.please_turn_on_wifi_to_connect_to_the_server)
                .setCancelable(true)
                .setPositiveButton(R.string.ok, (dialog, id) -> {
                });

        AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.show();
    }

    private boolean permissionsGranted() {
        int permissionCheck = ContextCompat.checkSelfPermission(getApplicationContext(), android.Manifest.permission.WRITE_EXTERNAL_STORAGE);
        int permissionCheck2 = ContextCompat.checkSelfPermission(getApplicationContext(), android.Manifest.permission.READ_PHONE_STATE);
        return (permissionCheck == 0 && permissionCheck2 == 0);
    }

    private void notifyStatusChange() {
        handler.removeCallbacks(updateRunnable);
        handler.postDelayed(updateRunnable, 500);
    }
}
