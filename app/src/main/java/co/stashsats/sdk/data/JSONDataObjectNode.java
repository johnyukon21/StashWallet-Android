package co.stashsats.sdk.data;

import com.fasterxml.jackson.databind.node.ObjectNode;

public class JSONDataObjectNode extends JSONData {
    private ObjectNode mNode;

    public JSONDataObjectNode(ObjectNode mNode) {
        this.mNode = mNode;
    }

    @Override
    public String toString() {
        return mNode.toString();
    }
}
