package io.daydev.vkrdo.fragment;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import io.daydev.vkrdo.R;

/**
 * display error fragment
 */
public class ErrorFragment  extends Fragment {

    public static final String ERROR_DESCRIPTION = "error";

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View root = inflater.inflate(R.layout.error, container, false);
        Bundle arguments = getArguments();
        if (arguments != null) {
            String errorDescription =  arguments.getString(ERROR_DESCRIPTION);
            if (errorDescription == null || errorDescription.isEmpty()){
                errorDescription = "Unknown error";
            }

            TextView textView = (TextView)root.findViewById(R.id.errorText);
            textView.setText(errorDescription);
        }
        return root;
    }

}

