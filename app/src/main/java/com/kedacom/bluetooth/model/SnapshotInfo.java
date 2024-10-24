package com.kedacom.bluetooth.model;

public class SnapshotInfo extends BaseInfo{
    private String content;
    private String msgld;

    public SnapshotInfo() {
        this.setRecordType(Recordtype.MSG_SNAPSHOT_REQ);
    }

    public SnapshotInfo(String content, String msgld) {
        this();
        this.content = content;
        this.msgld = msgld;
    }

    public String getContent() {
        return this.content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getMsgld() {
        return this.msgld;
    }

    public void setMsgld(String msgld) {
        this.msgld = msgld;
    }
}
