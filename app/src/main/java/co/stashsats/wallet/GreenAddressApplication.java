package co.stashsats.wallet;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.StrictMode;
import android.preference.PreferenceManager;
import android.util.Log;

import androidx.multidex.MultiDexApplication;

import com.blockstream.libwally.Wally;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import co.stashsats.sdk.data.NetworkData;
import co.stashsats.wallet.ui.BuildConfig;
import co.stashsats.wallet.ui.FailHardActivity;
import co.stashsats.wallet.ui.R;
import co.stashsats.wallet.ui.preferences.PrefKeys;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class GreenAddressApplication extends MultiDexApplication {

    private static AtomicBoolean isRunningTest;

    private void failHard(final String title, final String message) {
        final Intent fail = new Intent(this, FailHardActivity.class);
        fail.putExtra("errorTitle", title);
        final String supportMessage = "Please contact info@greenaddress.it for support.";
        fail.putExtra("errorContent", String.format("%s. %s", message, supportMessage));
        fail.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(fail);
    }

    @Override
    public void onCreate() {
        // Enable StrictMode if a debugger is connected
        if (BuildConfig.DEBUG) {
            StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
                    .detectDiskWrites()
                    .detectNetwork()
                    .penaltyLog()
                    .build());
            StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
                    .penaltyLog()
                    .penaltyDeath()
                    .build());
        }
        super.onCreate();

        if(isRunningTest())
            return;

        if (!Wally.isEnabled()) {
            failHard("Unsupported platform", "A suitable libwallycore.so was not found");
            return;
        }
    }

    public static synchronized boolean isRunningTest() {
        if (null == isRunningTest) {
            boolean istest;
            try {
                Class.forName("com.blockstream.libgreenaddress.GSDK");
                istest = true;
            } catch (ClassNotFoundException e) {
                istest = false;
            }
            isRunningTest = new AtomicBoolean(istest);
        }
        return isRunningTest.get();
    }


    // get Network function
//    public static NetworkData getNetworkData(final String network) {
//        final List<NetworkData> networks = Session.getSession().getNetworks();
//        for (final NetworkData n : networks) {
//            if (n.getNetwork().equals(network)) {
//                return n;
//            }
//        }
//        return null;
//    }

    public void setCurrentNetwork(final String networkId) {
        final boolean res = PreferenceManager.getDefaultSharedPreferences(this).edit().putString(PrefKeys.NETWORK_ID_ACTIVE, networkId).commit();
        if (res == false) {
            failHard(getString(R.string.id_error), getString(R.string.id_operation_failure));
        }
    }

    public String getCurrentNetwork() {
        return PreferenceManager.getDefaultSharedPreferences(this).getString(PrefKeys.NETWORK_ID_ACTIVE, "mainnet");
    }
//    public NetworkData getCurrentNetworkData() {
//        return getNetworkData(getCurrentNetwork());
//    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        Log.i("LoggedActivity","onLowMemory app");
    }
}
