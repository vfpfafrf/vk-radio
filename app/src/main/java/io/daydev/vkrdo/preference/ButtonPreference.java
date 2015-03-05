package io.daydev.vkrdo.preference;

import android.content.Context;
import android.preference.Preference;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import io.daydev.vkrdo.R;
import io.daydev.vkrdo.util.Callback;

/**
 * custom preference with 2 buttons
 */
public class ButtonPreference extends Preference {

    public static final String CMD_PLAY = "play";
    public static final String CMD_REMOVE = "remove";

    private Callback<String> buttonCallback;

    public ButtonPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public ButtonPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public ButtonPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ButtonPreference(Context context) {
        super(context);
    }

    public void setButtonCallback(Callback<String> buttonCallback) {
        this.buttonCallback = buttonCallback;
    }

    @Override
    protected View onCreateView(ViewGroup parent) {
        setLayoutResource(R.layout.preference_buttons);

        View view =  super.onCreateView(parent);

        Button button = (Button) view.findViewById(R.id.playButton);
        if (button != null){
            button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (buttonCallback != null){
                        buttonCallback.callback(CMD_PLAY);
                    }
                }
            });
        }

        ImageButton delete = (ImageButton) view.findViewById(R.id.delete_radio);
        if (delete != null){
            delete.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (buttonCallback != null){
                        buttonCallback.callback(CMD_REMOVE);
                    }
                }
            });
        }
        return view;
    }
}
