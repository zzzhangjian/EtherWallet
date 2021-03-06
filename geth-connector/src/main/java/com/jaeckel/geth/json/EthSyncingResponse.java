package com.jaeckel.geth.json;

import com.jaeckel.geth.json.FalseToNullFactory.FalseToNull;

public class EthSyncingResponse {

    @FalseToNull
    public EthSyncingResult result;

    @Override
    public String toString() {
        return "EthSyncingResponse{" +
                "result=" + result +
                '}';
    }
}
