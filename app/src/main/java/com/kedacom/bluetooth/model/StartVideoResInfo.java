package com.kedacom.bluetooth.model;

public class StartVideoResInfo extends BaseInfo{
    private String content;
    private String msgld;

    private int state;

    public StartVideoResInfo() {
        this.setRecordType(Recordtype.MSG_STARTRVIDEO_RES);
    }

    public StartVideoResInfo(String content, String msgld) {
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

    public int getState() {
        return this.state;
    }

    public void setState(int state) {
        this.state = state;
    }
}
