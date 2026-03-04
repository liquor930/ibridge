package com.brt.ibridge.common;

/**
 * Created by Administrator on 2017/1/5.
 */

public class Throughput {
    public static final int THROUGHPUT_STATUS_ONGOING = 1;
    public static final int THROUGHPUT_STATUS_FINSIHED = 2;
    public static final int THROUGHPUT_TYPE_TX = 1;
    public static final int THROUGHPUT_TYPE_RX = 2;

    public int type;
    public int status;
    public long startTime;
    public long finishTime;
    public int datalength;
    public String resultText;
}
