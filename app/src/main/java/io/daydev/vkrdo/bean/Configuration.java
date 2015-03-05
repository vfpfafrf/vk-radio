package io.daydev.vkrdo.bean;

/**
 * Created by dmitry on 05.03.15.
 */
public class Configuration {

    private String vkApp;

    private String echoKey;

    private String lastFmKey;


    private Configuration() {
    }

    public String getVkApp() {
        return vkApp;
    }

    public String getEchoKey() {
        return echoKey;
    }

    public String getLastFmKey() {
        return lastFmKey;
    }
}
