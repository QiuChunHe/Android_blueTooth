package com.kedacom.bluetooth.model;

public class InitResInfo extends BaseInfo{
    private int result;
    private String message;

    public InitResInfo() {
        this.setRecordType(Recordtype.MSG_INIT_RES);
    }

    public InitResInfo(int result, String message) {
        this();
        this.result = result;
        this.message = message;
    }

    public int getResult() {
        return this.result;
    }

    public void setResult(int result) {
        this.result = result;
    }

    public String getMessage() {
        return this.message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
