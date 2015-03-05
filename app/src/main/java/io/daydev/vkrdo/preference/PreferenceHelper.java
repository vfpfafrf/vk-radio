package io.daydev.vkrdo.preference;

import android.content.SharedPreferences;
import io.daydev.vkrdo.bean.RadioInfo;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by dmitry on 05.03.15.
 */
public class PreferenceHelper {

    public static <K,V> void  saveMap(SharedPreferences sharedPreferences, String key, Map<K,V> obj){
        Gson gson = new GsonBuilder().create();
        String hashMapString = gson.toJson(obj);

        sharedPreferences
                .edit()
                .putString(key, hashMapString)
                .apply();

    }

    public static void clean(SharedPreferences sharedPreferences, String key){
        sharedPreferences
                .edit()
                .remove(key)
                .apply();
    }

    public static  Map<String, RadioInfo> getMap(SharedPreferences sharedPreferences, String key){
        String value = sharedPreferences.getString(key, null);
        if (value == null || value.isEmpty()){
            return null;
        }

        Gson gson = new GsonBuilder().create();
        return gson.fromJson(value, new TypeToken<HashMap<String, RadioInfo>>(){}.getType());
    }



}
