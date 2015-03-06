package io.daydev.vkrdo.external;

import android.os.AsyncTask;
import android.util.Log;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.daydev.vkrdo.bean.Configuration;
import io.daydev.vkrdo.util.Callback;
import io.daydev.vkrdo.util.ResultTuple;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * Download external configuration
 */
public class ConfigurationHolder {

    private static volatile Configuration instance;

    public static Configuration getInstance() {
        return instance;
    }

    public void load(String configUrl, final Callback<ResultTuple<Configuration>> configurationCallback){

        if (instance != null && configurationCallback!= null){
            configurationCallback.callback(ResultTuple.success(instance));
            return;
        }

        try {
            AsyncTask<String, String, ResultTuple<Configuration>> configLoader = new AsyncTask<String, String, ResultTuple<Configuration>>() {
                @Override
                protected ResultTuple<Configuration> doInBackground(String... params) {
                    HttpClient httpclient = new DefaultHttpClient();
                    HttpResponse response;
                    try {
                        response = httpclient.execute(new HttpGet(params[0]));
                        StatusLine statusLine = response.getStatusLine();
                        if(statusLine.getStatusCode() == HttpStatus.SC_OK){
                            try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                                response.getEntity().writeTo(out);
                                String responseString = out.toString();

                                Gson gson = new GsonBuilder().create();
                                return ResultTuple.success(gson.fromJson(responseString, Configuration.class));
                            }
                        } else{
                            response.getEntity().getContent().close();
                            throw new IOException(statusLine.getReasonPhrase());
                        }
                    } catch (Exception e) {
                        Log.e("ConfigurationTask", "while loading", e);
                        return ResultTuple.error(e.getMessage());
                    }
                }

                @Override
                protected void onPostExecute(ResultTuple<Configuration> result) {
                    if (configurationCallback != null) {
                        instance = result.getResult();
                        configurationCallback.callback(result);
                    }
                }
            };
            configLoader.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, configUrl);
        } catch (Exception e){
            Log.e("ConfigurationTask", "while loading", e);
            if (configurationCallback != null) {
                configurationCallback.callback(ResultTuple.<Configuration>error(e.getMessage()));
            }
        }
    }
}
