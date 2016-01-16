package io.github.jbytheway.octranspoalarm;

public class Stop {
    Stop(String id, String code, String name) {
        mId = id;
        mCode = code;
        mName = name;
    }

    public String getId() { return mId; }
    public String getCode() { return mCode; }
    public String getName() { return mName; }

    private String mId;
    private String mCode;
    private String mName;
}
