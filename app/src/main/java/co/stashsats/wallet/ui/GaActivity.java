package co.stashsats.wallet.ui;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.util.SparseArray;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;

import com.google.common.util.concurrent.SettableFuture;
import co.stashsats.sdk.data.NetworkData;
import co.stashsats.session.Session;
import co.stashsats.wallet.GreenAddressApplication;
import co.stashsats.wallet.ui.components.ProgressBarHandler;
import co.stashsats.wallet.ui.preferences.PrefKeys;

/**
 * Base class for activities within the application.
 *
 * Provides access to the main Application and Service objects along with
 * support code to handle service initialization, error handling etc.
 */
public abstract class GaActivity extends AppCompatActivity {

    public static final int HARDWARE_PIN_REQUEST = 59212;
    public static final int HARDWARE_PASSPHRASE_REQUEST = 21392;

    protected static final String TAG = GaActivity.class.getSimpleName();

    private ProgressBarHandler mProgressBarHandler;
    private final SparseArray<SettableFuture<String>> mHwFunctions = new SparseArray<>();

    protected GreenAddressApplication getGAApp() {
        return (GreenAddressApplication) getApplication();
    }

    public Bundle getMetadata() {
        Bundle metadata = null;
        try {
            metadata =
                getPackageManager().getActivityInfo(this.getComponentName(), PackageManager.GET_META_DATA).metaData;
        } catch (PackageManager.NameNotFoundException ignored) {}

        return metadata;
    }

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        Log.d(TAG, "onCreate -> " + this.getClass().getSimpleName());
        setTheme(ThemeUtils.getThemeFromNetworkId(getGAApp().getCurrentNetwork(), this,
                                                  getMetadata()));

        super.onCreate(savedInstanceState);
        final int viewId = getMainViewId();
        if (viewId != UI.INVALID_RESOURCE_ID)
            setContentView(viewId);
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mProgressBarHandler != null)
            mProgressBarHandler.stop();
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    /** Override to provide the main view id */
    protected int getMainViewId() { return UI.INVALID_RESOURCE_ID; }

    // Utility methods

    public void finishOnUiThread() {
        runOnUiThread(GaActivity.this::finish);
    }

    public void hideKeyboardFrom(final View v) {
        final View toHideFrom = v == null ? getCurrentFocus() : v;
        if (toHideFrom != null) {
            final InputMethodManager imm;
            imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(toHideFrom.getWindowToken(), 0);
        }
    }

    protected void setTitleWithNetwork(final int resource) {
        final NetworkData networkData = Session.getSession().getNetwork();
        if (networkData == null || getSupportActionBar() == null) {
            setTitle(resource);
            return;
        }

        final String netname = networkData.getName();
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setIcon(networkData.getIcon());
        if (!"Bitcoin".equals(netname))
            setTitle(String.format(" %s %s",
                                   netname, getString(resource)));
        else
            setTitle(resource);
    }

    public void setTitleBackTransparent() {
        setTitleBack();
        getSupportActionBar().setBackgroundDrawable(getResources().getDrawable(android.R.color.transparent));
    }

    public void setTitleBack() {
        getSupportActionBar().setElevation(0);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
    }

    public void startLoading() {
        startLoading("");
    }

    public void startLoading(final String label) {
        runOnUiThread(() -> {
            if (mProgressBarHandler == null)
                mProgressBarHandler = new ProgressBarHandler(GaActivity.this);
            mProgressBarHandler.start(label);
        });
    }

    public void stopLoading() {
        if (mProgressBarHandler == null)
            return;
        runOnUiThread(() -> mProgressBarHandler.stop());
    }

    public ProgressBarHandler getProgressBarHandler() {
        return mProgressBarHandler;
    }

    public boolean isLoading() {
        return mProgressBarHandler != null && mProgressBarHandler.isLoading();
    }

    @Override
    protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {

        if (requestCode == HARDWARE_PIN_REQUEST || requestCode == HARDWARE_PASSPHRASE_REQUEST) {
            Log.d(TAG,"onActivityResult " + requestCode);
            mHwFunctions.get(requestCode).set(resultCode ==
                                              RESULT_OK ? data.getStringExtra(String.valueOf(
                                                                                  requestCode)) : null);
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

//    public SharedPreferences cfg() {
//        return getSharedPreferences(network(), MODE_PRIVATE);
//    }

//    protected String network() {
//        return PreferenceManager.getDefaultSharedPreferences(this).getString(PrefKeys.NETWORK_ID_ACTIVE, "mainnet");
//    }

//    protected NetworkData getNetwork() {
//        return getGAApp().getCurrentNetworkData();
//    }
}
