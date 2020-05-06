package co.stashsats.session;

import android.app.Activity;
import android.util.Log;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.LongNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import co.stashsats.sdk.data.BalanceData;
import co.stashsats.sdk.data.EstimatesData;
import co.stashsats.sdk.data.JSONData;
import co.stashsats.sdk.data.NetworkData;
import co.stashsats.sdk.data.SettingsData;
import co.stashsats.sdk.data.SubaccountData;
import co.stashsats.sdk.data.TransactionData;

import co.stashsats.wallet.ui.BuildConfig;
import co.stashsats.wallet.ui.GaActivity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;


public class Session {

    // Fine to have a static objectMapper according to docs if using always same configuration
    private static final ObjectMapper mObjectMapper = new ObjectMapper();
    private static Session instance = new Session();

    private SettingsData mSettings;
    private String mNetwork;

    static {
        mObjectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    private Session() { }

    /*
     * YOU SHOULD NEVER KEEP A REFERENCE TO THE INSTANCE RETURNED HERE. IT COULD BE DESTROYED
     * AND LEAD TO ITS USAGE AFTER ITS DESTRUCTOR HAS BEEN CALLED
     */
    public static Session getSession() {
        return instance;
    }



    public JsonNode findTransactionRaw(final ArrayNode txListObject, final String txhash) throws Exception {
        for (JsonNode node : txListObject) {
            if (node.get("txhash").asText().equals(txhash))
                return node;
        }

        return null;
    }
    public ObjectNode convert(final ObjectNode amount) throws Exception {
//        return (ObjectNode) GDK.convert_amount(mNativeSession, amount);
        return new ObjectNode(new JsonNodeFactory(true));
    }
    public ObjectNode convertSatoshi(final long satoshi) throws Exception {
        final ObjectNode amount = mObjectMapper.createObjectNode();
        amount.set("satoshi", new LongNode(satoshi));
        return convert(amount);
    }

    public ObjectNode createTransactionFromUri(final Activity parent, final String uri, final int subaccount) throws Exception {
        final ObjectNode tx = mObjectMapper.createObjectNode();
        tx.put("subaccount", subaccount);
        final ObjectNode address = mObjectMapper.createObjectNode();
        address.put("address", uri);
        final ArrayNode addressees = mObjectMapper.createArrayNode();
        addressees.add(address);
        tx.set("addressees", addressees);
        return createTransactionRaw(parent, tx).resolve();
    }

    public Map<String, Object> getAvailableCurrencies() throws Exception {
//        final ObjectNode availableCurrencies = (ObjectNode) GDK.get_available_currencies(mNativeSession);
        final ObjectNode availableCurrencies = new ObjectNode(new JsonNodeFactory(true));
        return mObjectMapper.treeToValue(availableCurrencies, Map.class);
    }

//    public static List<NetworkData> getNetworks() {
//        final List<NetworkData> networksMap = new LinkedList<>();
//        final ObjectNode networks = (ObjectNode) GDK.get_networks();
//        final ArrayNode nodes = (ArrayNode) networks.get("all_networks");
//        final boolean isProduction = !BuildConfig.DEBUG;
//
//        for (final JsonNode node : nodes) {
//            final String networkName = node.asText();
//            try {
//                final NetworkData data = mObjectMapper.treeToValue(networks.get(networkName), NetworkData.class);
//                if (!(isProduction && data.getDevelopment())) {
//                    networksMap.add(data);
//                }
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//        }
//        Collections.sort( networksMap);
//        return networksMap;
//    }
    public ObjectNode getGDKSettings() throws Exception  {
//        return (ObjectNode) GDK.get_settings(mNativeSession);
        return new ObjectNode(new JsonNodeFactory(true));
    }

    public ObjectNode getReceiveAddress(final int subAccount) throws Exception {
        final ObjectNode details = mObjectMapper.createObjectNode();
        details.put("subaccount", subAccount);
//        return new TwoFactorCall(GDK.get_receive_address(mNativeSession, details)).resolve();
        return new ObjectNode(new JsonNodeFactory(true));
    }
    public List<Long> getFeeEstimates() throws Exception {
//        final ObjectNode feeEstimates = (ObjectNode) GDK.get_fee_estimates(mNativeSession);
//        return mObjectMapper.treeToValue(feeEstimates, EstimatesData.class).getFees();
        return mObjectMapper.treeToValue(new ObjectNode(new JsonNodeFactory(true)), EstimatesData.class).getFees();
    }










    public TwoFactorCall getTransactionsRaw(final int subAccount, final int first, final int count) throws Exception {
        final ObjectNode details = mObjectMapper.createObjectNode();
        details.put("subaccount", subAccount);
        details.put("first", first);
        details.put("count", count);
        details.put("num_confs", 0);
//        return new TwoFactorCall(GDK.get_transactions(mNativeSession, details));
        return new TwoFactorCall(new Object());
    }


    public TwoFactorCall getBalance(final Integer subAccount, final long confirmations) {
        final ObjectNode details = mObjectMapper.createObjectNode();
        details.put("subaccount", subAccount);
        details.put("num_confs", confirmations);
//        return new TwoFactorCall( GDK.get_balance(mNativeSession, details));
        return new TwoFactorCall(new Object());
    }


    public TwoFactorCall createTransactionRaw(final Activity parent, final JSONData createTransactionData) throws Exception {
//        return new TwoFactorCall(GDK.create_transaction(mNativeSession, createTransactionData));
        return new TwoFactorCall(new Object());
    }

    public TwoFactorCall createTransactionRaw(final Activity parent, final ObjectNode tx) throws Exception {
//        return new TwoFactorCall(GDK.create_transaction(mNativeSession, tx)).resolve();
        return new TwoFactorCall(new Object());
    }
    public TwoFactorCall signTransactionRaw(final ObjectNode createTransactionData) throws Exception {
//        return new TwoFactorCall(GDK.sign_transaction(mNativeSession, createTransactionData));
        return new TwoFactorCall(new Object());
    }

    public TwoFactorCall sendTransactionRaw(final Activity parent, final ObjectNode txDetails) throws Exception {
//        final Object twoFactorCall = GDK.send_transaction(mNativeSession, txDetails);
//        final TwoFactorCall gdkTwoFactorCall = new TwoFactorCall(twoFactorCall);
//        return gdkTwoFactorCall;
        return new TwoFactorCall(new Object());
    }




    public List<TransactionData> parseTransactions(final ArrayNode txListObject) throws Exception {
        //final ArrayNode txListObject = getTransactionsRaw(subAccount, first, count);
        final List<TransactionData> transactionDataPagedData =
                mObjectMapper.readValue(mObjectMapper.treeAsTokens(txListObject),
                        new TypeReference<List<TransactionData>>() {});
        return transactionDataPagedData;
    }

    public BalanceData convertBalance(final long satoshi) throws Exception {
        final ObjectNode convertedBalanceData = (ObjectNode) convertSatoshi(satoshi);
        final BalanceData balanceData = BalanceData.from(mObjectMapper, convertedBalanceData);
        return balanceData;
    }




    public static boolean isEnabled() {
//        return GDK.isEnabled();
        return true;
    }


    public Boolean changeMemo(final String txHashHex, final String memo) throws Exception {
//        GDK.set_transaction_memo(mNativeSession, txHashHex, memo, GDK.GA_MEMO_USER);
        return true;
    }


    public SettingsData refreshSettings() {
        try {
            final ObjectNode settings = getGDKSettings();
            final SettingsData settingsData = mObjectMapper.convertValue(settings, SettingsData.class);
            mSettings = settingsData;
            return settingsData;
        } catch (final Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public SettingsData getSettings() {
        if (mSettings != null)
            return mSettings;
        return refreshSettings();
    }

    public SubaccountData getSubAccount(final GaActivity activity, final long subaccount) throws Exception {
//        final TwoFactorCall call = getSubAccount(subaccount);
//        final ObjectNode account = call.resolve();
//        final SubaccountData subAccount = mObjectMapper.readValue(account.toString(), SubaccountData.class);
        final SubaccountData subAccount = new SubaccountData();
        Map<String, Long> x = new HashMap<>();
        x.put("btc",10000L);
        subAccount.setSatoshi(x);
        return subAccount;
    }


    public List<TransactionData> getTransactions(final GaActivity activity, final int subaccount, final int first, final int size) throws Exception {
        final TwoFactorCall call =
                getTransactionsRaw(subaccount, first, size);
        final ObjectNode txListObject = call.resolve();
        final List<TransactionData> transactions =
                parseTransactions((ArrayNode) txListObject.get("transactions"));
        return transactions;
    }

    public List<Long> getFees() {
//        if (!getNotificationModel().getFees().isEmpty())
//            return getNotificationModel().getFees();
        try {
            return getFeeEstimates();
        } catch (final Exception e) {
            return new ArrayList<Long>(0);
        }
    }

//    public NetworkData getNetworkData() {
//        final List<NetworkData> networks = getNetworks();
//        for (final NetworkData n : networks) {
//            if (n.getNetwork().equals(mNetwork)) {
//                return n;
//            }
//        }
//        return null;
//    }

    public void setSettings(final SettingsData settings) {
        mSettings = settings;
    }

    public int getBlockHeight(){
        return 600000;
    }

    public NetworkData getNetwork(){
        NetworkData x = new NetworkData();
        x.setName("Bitcoin");
        x.setNetwork("mainnet");

        return x;
    }
}
