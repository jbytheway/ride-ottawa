package io.github.jbytheway.rideottawa;

public class Stop {
    Stop(String id, String code, String name) {
        mId = id;
        mCode = code;
        mName = name;
    }

    public String getId() { return mId; }
    public String getCode() { return mCode; }
    public String getName() { return mName; }

    private final String mId;
    private final String mCode;
    private final String mName;
}
