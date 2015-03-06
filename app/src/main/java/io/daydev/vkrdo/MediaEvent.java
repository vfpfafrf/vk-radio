package io.daydev.vkrdo;

/**
 * Broadcast intet consts
 */
public interface MediaEvent {

    public static final String EVENT = "media-event";

    public static final String TYPE = "event-type";

    public static final String GLOBAL_ERROR = "global-error";

    public static final String SIMPLE_RADIO = "simple-radio";
    public static final String REAL_RADIO = "real-radio";
    public static final String RADIO_TITLE = "radio-title";
    public static final String RADIO_REMOVE = "radio-remove";

    public static final String MEDIAPLAYER_COMMAND = "media-command";

    /**
     * "magic" home screen radio const
     */
    public static final String MAGIC_HOME = "~~~home123";

    public static final String DATA_MESSAGE_CODE = "message";
    public static final String DATA_SERIALIZEBLE = "data";
    public static final String DATA_PARCEABLE = "data-parcelabe";
    public static final String DATA_RADIO = "radio-extra";

    /**
     * default home radio artist - if nothings is selected, but play button pushed
     */
    public static final String DEFAULT_HOME_RADIO = "Taylor Swift";
}
