package co.stashsats.wallet.ui.preferences;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.text.TextUtils;
import android.util.Log;
import android.util.Pair;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.SwitchPreference;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

import com.afollestad.materialdialogs.MaterialDialog;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.BooleanNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import co.stashsats.sdk.data.BalanceData;
import co.stashsats.sdk.data.PricingData;
import co.stashsats.sdk.data.SettingsData;
import co.stashsats.sdk.data.TwoFactorConfigData;
import co.stashsats.session.Conversion;
import co.stashsats.session.Session;
import co.stashsats.wallet.ui.BuildConfig;
import co.stashsats.wallet.ui.R;
import co.stashsats.wallet.ui.TabbedMainActivity;
import co.stashsats.wallet.ui.UI;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class GeneralPreferenceFragment extends GAPreferenceFragment {
    private static final String TAG = GeneralPreferenceFragment.class.getSimpleName();

    private static final ObjectMapper mObjectMapper = new ObjectMapper();

    private Preference mPinPref;
    private ListPreference mUnitPref;
    private ListPreference mPriceSourcePref;
    private ListPreference mTxPriorityPref;
    private Preference mCustomRatePref;

    private ListPreference mTimeoutPref;
    private Disposable mSetupDisposable, mUpdateDisposable;

    @Override
    public void onCreatePreferences(final Bundle savedInstanceState, final String rootKey) {
        super.onCreatePreferences(savedInstanceState, rootKey);
        addPreferencesFromResource(R.xml.preference_general);
        setHasOptionsMenu(true);

//        // Bitcoin denomination
//        mUnitPref = find(PrefKeys.UNIT);
//        mUnitPref.setEntries(UI.UNITS);
//        mUnitPref.setEntryValues(UI.UNITS);
//        mUnitPref.setOnPreferenceChangeListener((preference, newValue) -> {
//            final SettingsData settings = Session.getSession().getSettings();
//            if (!newValue.equals(settings.getUnit())) {
//                settings.setUnit(newValue.toString());
//                ((TabbedMainActivity) getActivity()).recreate();
//                return true;
//            }
//            return false;
//        });
//
//        // Reference exchange rate
//        mPriceSourcePref = find(PrefKeys.PRICING);
//        mPriceSourcePref.setSingleLineTitle(false);
//        mPriceSourcePref.setOnPreferenceChangeListener((preference, o) -> {
//            if (warnIfOffline(getActivity())) {
//                return false;
//            }
//            final String[] split = o.toString().split(" ");
//            final String currency = split[0];
//            final String exchange = split[1];
//            final SettingsData settings = Session.getSession().getSettings();
//
//            settings.getPricing().setCurrency(currency);
//            settings.getPricing().setExchange(exchange);
//            setPricingSummary(null);
//            return true;
//        });
//
//        // Transaction priority, i.e. default fees
//        mTxPriorityPref = find(PrefKeys.REQUIRED_NUM_BLOCKS);
//        mTxPriorityPref.setSingleLineTitle(false);
//        final String[] priorityValues = getResources().getStringArray(R.array.fee_target_values);
//        mTxPriorityPref.setOnPreferenceChangeListener((preference, newValue) -> {
//            if (warnIfOffline(getActivity())) {
//                return false;
//            }
//            final int index = mTxPriorityPref.findIndexOfValue(newValue.toString());
//            final SettingsData settings = Session.getSession().getSettings();
//            settings.setRequiredNumBlocks(Integer.parseInt(priorityValues[index]));
//            setRequiredNumBlocksSummary(null);
//            updateSettings(settings);
//            return true;
//        });
//
//        // Default custom feerate
//        mCustomRatePref = find(PrefKeys.DEFAULT_FEERATE_SATBYTE);
//        setFeeRateSummary();
//        mCustomRatePref.setOnPreferenceClickListener(this::onFeeRatePreferenceClicked);

        // Terms of service
        final Preference termsOfUse = find(PrefKeys.TERMS_OF_USE);
        termsOfUse.setOnPreferenceClickListener(preference -> openURI("https://blockstream.com/green/terms/"));

        // Privacy policy
        final Preference privacyPolicy = find(PrefKeys.PRIVACY_POLICY);
        privacyPolicy.setOnPreferenceClickListener(preference -> openURI("https://blockstream.com/green/privacy/"));

        // Version
        final Preference version = find(PrefKeys.VERSION);
        version.setSummary(String.format("%s %s",
                                         getString(R.string.app_name),
                                         getString(R.string.id_version_1s_2s,
                                                   BuildConfig.VERSION_NAME,
                                                   BuildConfig.BUILD_TYPE)));
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mSetupDisposable != null)
            mSetupDisposable.dispose();
        if (mUpdateDisposable != null)
            mUpdateDisposable.dispose();
    }

//    private void initSummaries() {
//
//        try {
//            final Map<String, Object> availableCurrencies = Session.getSession().getAvailableCurrencies();
//            setPricingEntries(availableCurrencies);
//        } catch (final Exception e) {
//            e.printStackTrace();
//        }
//
//
//
//        if (Session.getSession().getSettings() != null) {
//            setPricingSummary(Session.getSession().getSettings().getPricing());
//            mUnitPref.setSummary(Conversion.getUnit());
//            setRequiredNumBlocksSummary(Session.getSession().getSettings().getRequiredNumBlocks());
//        }
//    }

//    private void updateSettings(final SettingsData settings) {
//        mUpdateDisposable = Observable.just(Session.getSession())
//                            .observeOn(Schedulers.computation())
//                            .map((session) -> {
//            session.changeSettings(settings.toObjectNode());
//            session.refreshSettings();
//            return session;
//        })
//                            .observeOn(AndroidSchedulers.mainThread())
//                            .subscribe((res) -> {
//            UI.toast(getActivity(), R.string.id_setting_updated, Toast.LENGTH_LONG);
//        }, (e) -> {
//            e.printStackTrace();
//            UI.toast(getActivity(), e.getMessage(), Toast.LENGTH_LONG);
//        });
//    }

//    private String getDefaultFeeRate() {
//        Long minFeeRateKB = 1000L;
//        try {
//            minFeeRateKB = Session.getSession().getFees().get(0);
//        } catch (final Exception e) {
//            e.printStackTrace();
//        }
//        final String minFeeRateText = String.valueOf(minFeeRateKB / 1000.0);
//        return cfg().getString( PrefKeys.DEFAULT_FEERATE_SATBYTE, minFeeRateText);
//    }
//
//    private boolean onFeeRatePreferenceClicked(final Preference preference) {
//        if (warnIfOffline(getActivity())) {
//            return false;
//        }
//        final View v = UI.inflateDialog(getActivity(), R.layout.dialog_set_custom_feerate);
//        final EditText rateEdit = UI.find(v, R.id.set_custom_feerate_amount);
//        UI.localeDecimalInput(rateEdit);
//
//        final Double aDouble = Double.valueOf(getDefaultFeeRate());
//
//        rateEdit.setText(Conversion.getNumberFormat(2).format(aDouble));
//        rateEdit.selectAll();
//
//        final MaterialDialog dialog;
//        dialog = UI.popup(getActivity(), R.string.id_set_custom_fee_rate)
//                 .customView(v, true)
//                 .backgroundColor(getResources().getColor(R.color.buttonJungleGreen))
//                 .onPositive((dlg, which) -> {
//            try {
//                final Long minFeeRateKB = Session.getSession().getFees().get(0);
//                final String enteredFeeRate = UI.getText(rateEdit);
//                final Number parsed = Conversion.getNumberFormat(2).parse(enteredFeeRate);
//                final Double enteredFeeRateKB = parsed.doubleValue();
//
//                if (enteredFeeRateKB * 1000 < minFeeRateKB) {
//                    UI.toast(getActivity(), getString(R.string.id_fee_rate_must_be_at_least_s,
//                                                      String.format("%.2f",(minFeeRateKB/1000.0) )), Toast.LENGTH_LONG);
//                } else {
//                    cfg().edit().putString(PrefKeys.DEFAULT_FEERATE_SATBYTE, String.valueOf(enteredFeeRateKB)).apply();
//                    setFeeRateSummary();
//                }
//            } catch (final Exception e) {
//                UI.toast(getActivity(), "Error setting Fee Rate", Toast.LENGTH_LONG);
//            }
//        }).build();
//        UI.showDialog(dialog);
//        return false;
//    }

//    private void setRequiredNumBlocksSummary(final Integer currentPriority) {
//        if (currentPriority == null)
//            mTxPriorityPref.setSummary("");
//        else {
//            final String[] prioritySummaries = {prioritySummary(3), prioritySummary(12), prioritySummary(24)};
//            final String[] priorityValues = getResources().getStringArray(R.array.fee_target_values);
//            for (int index = 0; index < priorityValues.length; index++)
//                if (currentPriority.equals(Integer.valueOf(priorityValues[index])))
//                    mTxPriorityPref.setSummary(prioritySummaries[index]);
//        }
//    }
//
//    private String prioritySummary(final int blocks) {
//        final int blocksPerHour = 6;
//        final int n = blocks % blocksPerHour == 0 ? blocks / blocksPerHour : blocks * (60 / blocksPerHour);
//        final String confirmationInBlocks = getResources().getString(R.string.id_confirmation_in_d_blocks, blocks);
//        final int idTime = blocks % blocksPerHour ==
//                           0 ? (blocks == blocksPerHour ? R.string.id_hour : R.string.id_hours) : R.string.id_minutes;
//        return String.format("%s, %d %s %s", confirmationInBlocks, n, getResources().getString(idTime),
//                             getResources().getString(R.string.id_on_average));
//    }

//    private void setTimeoutSummary(final Integer altimeout) {
//        if (altimeout == null)
//            mTimeoutPref.setSummary("");
//        else {
//            final String minutesText = altimeout == 1 ?
//                                       "1 " + getString(R.string.id_minute) :
//                                       getString(R.string.id_1d_minutes, altimeout);
//            mTimeoutPref.setSummary(minutesText);
//        }
//    }
//
//    private void setTimeoutValues(final ListPreference preference) {
//        final CharSequence[] entries = preference.getEntryValues();
//        final int length = entries.length;
//        final String[] entryValues = new String[length];
//        for (int i = 0; i < length; i++) {
//            final int currentMinutes = Integer.valueOf(entries[i].toString());
//            final String minutesText = currentMinutes == 1 ?
//                                       "1 " + getString(R.string.id_minute) :
//                                       getString(R.string.id_1d_minutes, currentMinutes);
//            entryValues[i] = minutesText;
//        }
//        preference.setEntries(entryValues);
//    }

//    private void setPricingEntries(final Map<String, Object> currencies) {
//        final List<String> values = getAvailableCurrenciesAsList(currencies);
//        final List<String> formatted =
//            getAvailableCurrenciesAsFormattedList(currencies, getString(R.string.id_s_from_s));
//        final String[] valuesArr = values.toArray(new String[0]);
//        final String[] formattedArr = formatted.toArray(new String[0]);
//        mPriceSourcePref.setEntries(formattedArr);
//        mPriceSourcePref.setEntryValues(valuesArr);
//    }
//
//    public List<String> getAvailableCurrenciesAsFormattedList(final Map<String, Object> currencies,
//                                                              final String format) {
//        final List<String> list = new ArrayList<>();
//        for (Pair<String,String> pair : getAvailableCurrenciesAsPairs(currencies)) {
//            list.add(String.format(format, pair.first, pair.second));
//        }
//        return list;
//    }
//
//    public List<String> getAvailableCurrenciesAsList(final Map<String, Object> currencies) {
//        if (getAvailableCurrenciesAsPairs(currencies) == null)
//            return null;
//        final List<String> list = new ArrayList<>();
//        for (Pair<String,String> pair : getAvailableCurrenciesAsPairs(currencies)) {
//            list.add(String.format("%s %s", pair.first, pair.second));
//        }
//        return list;
//    }
//
//    private List<Pair<String, String>> getAvailableCurrenciesAsPairs(final Map<String, Object> currencies) {
//        final List<Pair<String, String>> ret = new LinkedList<>();
//        final Map<String, ArrayList<String>> perExchange = (Map) currencies.get("per_exchange");
//
//        for (final String exchange : perExchange.keySet())
//            for (final String currency : perExchange.get(exchange))
//                ret.add(new Pair<>(currency, exchange));
//
//        Collections.sort(ret, (lhs, rhs) -> lhs.first.compareTo(rhs.first));
//        return ret;
//    }
//
//    private void setPricingSummary(final PricingData pricing) {
//        final String summary = pricing == null ? "" : String.format(getString(
//                                                                        R.string.id_s_from_s),
//                                                                    pricing.getCurrency(), pricing.getExchange());
//        mPriceSourcePref.setSummary(summary);
//    }



//    private void setFeeRateSummary() {
//        final Double aDouble = Double.valueOf(getDefaultFeeRate());
//        final String feeRateString = UI.getFeeRateString(Double.valueOf(aDouble * 1000).longValue());
//        mCustomRatePref.setSummary(feeRateString);
//    }

    @Override
    public void onResume() {
        super.onResume();
        if (isZombie())
            return;
//        initSummaries();
        updatesVisibilities();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (isZombie())
            return;
    }

    public void updatesVisibilities() {
//        mPinPref.setVisible(true);
//        mCustomRatePref.setVisible(true);
//        mTxPriorityPref.setVisible(true);
//        mPriceSourcePref.setVisible(true);
    }






}

