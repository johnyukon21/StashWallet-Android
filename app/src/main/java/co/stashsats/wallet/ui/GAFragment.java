package co.stashsats.wallet.ui;

import android.content.Context;

import androidx.fragment.app.Fragment;

import co.stashsats.sdk.data.NetworkData;
import co.stashsats.wallet.GreenAddressApplication;
import co.stashsats.wallet.ui.preferences.PrefKeys;

public abstract class GAFragment extends Fragment {
    protected GreenAddressApplication mApp;

    @Override
    public void onAttach(final Context context) {
        super.onAttach(context);

        mApp = (GreenAddressApplication) getActivity().getApplication();
        final GaActivity activity = (GaActivity) getActivity();

        try {
            context.getTheme().applyStyle(ThemeUtils.getThemeFromNetworkId(mApp.getCurrentNetwork(), context,
                                                                           activity.getMetadata()),
                                          true);
        } catch (final Exception e) {
            // Some reports show NullPointer Exception in applying style
            // Applying theme is not mandatory, doing nothing here
        }
    }

    protected GaActivity getGaActivity() {
        return (GaActivity) getActivity();
    }


    // Returns true if we are being restored without an activity or service
    protected boolean isZombie() {
        return getActivity() == null;
    }

}
