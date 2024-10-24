package com.kedacom.bluetooth.model;


public enum Recordtype {
    MSG_INIT_RES,
    MSG_STARTRVIDEO_REQ,
    MSG_STARTRVIDEO_RES,
    MSG_STOPVIDEO_REQ,
    MSG_STOPVIDEO_RES,
    MSG_SNAPSHOT_REQ,
    MSG_SNAPSHOT_RES,
    MSG_PTT_PRESSED,
    MSG_PTT_RELEASED;

    private Recordtype() {
    }
}
