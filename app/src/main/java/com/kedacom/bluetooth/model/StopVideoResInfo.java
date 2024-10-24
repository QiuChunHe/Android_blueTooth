package com.kedacom.bluetooth.model;

public class StopVideoResInfo extends BaseInfo{
    private String content;
    private String msgld;

    private int state;

    public StopVideoResInfo() {
        this.setRecordType(Recordtype.MSG_STOPVIDEO_RES);
    }

    public StopVideoResInfo(String content, String msgld) {
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
