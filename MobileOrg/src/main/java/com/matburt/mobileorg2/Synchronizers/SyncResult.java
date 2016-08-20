package com.matburt.mobileorg.Synchronizers;

import java.util.HashSet;

/**
 * Created by bcoste on 18/06/16.
 */
public class SyncResult {
    HashSet<String> newFiles;
    HashSet<String> changedFiles;
    HashSet<String> deletedFiles;
    enum State {
        kSuccess,
        kFailed
    };

    State state;

    SyncResult(){
        newFiles = new HashSet<>();
        changedFiles = new HashSet<>();
        deletedFiles = new HashSet<>();
        state = State.kFailed;
    }

    void setState(State state){
        this.state = state;
    }
}
