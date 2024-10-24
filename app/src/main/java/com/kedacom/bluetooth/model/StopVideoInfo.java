package com.kedacom.bluetooth.model;

public class StopVideoInfo extends BaseInfo{
    private String content;
    private String msgld;

    public StopVideoInfo() {
        this.setRecordType(Recordtype.MSG_STOPVIDEO_REQ);
    }

    public StopVideoInfo(String content, String msgld) {
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
