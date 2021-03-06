package com.jaeckel.geth.json;

public class EthSyncingResult {

    @Hex
    public long currentBlock;
    @Hex
    public long highestBlock;
    @Hex
    public long knownStates;
    @Hex
    public long pulledStates;
    @Hex
    public long startingBlock;

    @Override
    public String toString() {
        return "EthSyncingResponse{" +
                "currentBlock=" + currentBlock +
                ", highestBlock=" + highestBlock +
                ", knownStates=" + knownStates +
                ", pulledStates=" + pulledStates +
                ", startingBlock=" + startingBlock +
                '}';
    }
}
