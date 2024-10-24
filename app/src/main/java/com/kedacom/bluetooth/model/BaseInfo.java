package com.kedacom.bluetooth.model;

import java.io.Serializable;

public class BaseInfo implements Serializable {
    private Recordtype recordType;

    public BaseInfo() {
    }

    public Recordtype getRecordType() {
        return this.recordType;
    }

    public void setRecordType(Recordtype recordType) {
        this.recordType = recordType;
    }
}
