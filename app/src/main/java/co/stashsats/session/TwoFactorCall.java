package co.stashsats.session;

import android.util.Log;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import co.stashsats.sdk.data.TwoFactorStatusData;

public class TwoFactorCall {
    private Object mTwoFactorCall;
    private TwoFactorStatusData mStatus;
    private static ObjectMapper mObjectMapper = new ObjectMapper();

    TwoFactorCall(final Object twoFactorCall) {
        mTwoFactorCall = twoFactorCall;
        mObjectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }


    /**
     * This method must be called in a separate thread if requires GUI input, otherwise it blocks
     * @throws Exception
     */
    public ObjectNode resolve() throws Exception {
        return mStatus.getResult();
    }


}
