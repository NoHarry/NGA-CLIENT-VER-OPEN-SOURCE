package sp.phone.common;

import android.support.annotation.NonNull;

/**
 * Created by Justwen on 2017/12/26.
 */

public class User {

    @NonNull
    private String mUserId;

    private String mNickName;

    private String mCid;

    private String mReplyString;

    private int mReplyCount;

    public User() {
    }

    public String getUserId() {
        return mUserId;
    }

    public void setUserId(String userId) {
        mUserId = userId;
    }

    public String getCid() {
        return mCid;
    }

    public void setCid(String cid) {
        mCid = cid;
    }

    public String getReplyString() {
        return mReplyString;
    }

    public void setReplyString(String replyString) {
        mReplyString = replyString;
    }

    public int getReplyCount() {
        return mReplyCount;
    }

    public void setReplyCount(int replyCount) {
        mReplyCount = replyCount;
    }

    public String getNickName() {
        return mNickName;
    }

    public void setNickName(String nickName) {
        mNickName = nickName;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof User && mUserId.equals(getUserId());
    }
}
