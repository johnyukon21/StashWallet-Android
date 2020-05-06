package co.stashsats.wallet.ui.preferences;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.view.MenuItem;

import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;

import co.stashsats.sdk.data.NetworkData;
import co.stashsats.wallet.GreenAddressApplication;
import co.stashsats.wallet.ui.LoggedActivity;

import static android.content.Context.MODE_PRIVATE;

public class GAPreferenceFragment extends PreferenceFragmentCompat {
    private static final String TAG = GAPreferenceFragment.class.getSimpleName();

    @Override
    public void onAttach(final Context context) {
        super.onAttach(context);
    }

    @Override
    public void onCreatePreferences(final Bundle savedInstanceState, final String rootKey) {}

    private static final Preference.OnPreferenceChangeListener onPreferenceChanged =
        new Preference.OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(final Preference preference, final Object value) {
            preference.setSummary(value.toString());
            return true;
        }
    };

    protected < T > T find(final String preferenceName) {
        return (T) findPreference(preferenceName);
    }

    protected boolean openURI(final String uri) {
        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(uri)));
        return false;
    }

//    public SharedPreferences cfg() {
//        return getContext().getSharedPreferences(network(), MODE_PRIVATE);
//    }

//    public String network() {
//        return PreferenceManager.getDefaultSharedPreferences(getContext()).getString(PrefKeys.NETWORK_ID_ACTIVE,
//                                                                                     "mainnet");
//    }
//    public NetworkData getNetwork() {
//        return mApp.getCurrentNetworkData();
//    }


    public boolean warnIfOffline(final Activity activity) {
        /*if (getConnectionManager().isOffline()) {
            UI.toast(activity, R.string.id_connection_failed, Toast.LENGTH_LONG);
            return true;
           }*/
        return false;
    }

    // Returns true if we are being restored without an activity or service
    protected boolean isZombie() {
        return getActivity() == null;
    }
}
