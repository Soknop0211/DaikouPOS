package com.mylibrary;

import android.util.Log;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import org.jetbrains.annotations.NotNull;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CustomCallback implements Callback<JsonElement> {
    private final CustomResponseListener customResponseListener;
    public static String TAG = "kesspaymentdebug";

    public CustomCallback(CustomResponseListener listener) {
        customResponseListener = listener;
    }

    @Override
    public void onResponse(@NotNull Call<JsonElement> call, Response<JsonElement> response) {
        Log.d(TAG, response.toString());
        if (response.isSuccessful()) {
            JsonElement jsonElement = response.body();
            if (jsonElement != null && !jsonElement.isJsonNull() && jsonElement instanceof JsonObject) {
                Log.d("logcallbacksucceess", "c: "+ jsonElement.getAsJsonObject());
                try {
                    customResponseListener.onSuccess(jsonElement.getAsJsonObject());
                } catch (Exception exception){
                    customResponseListener.onError(exception.getMessage(), response.code());
                }
            } else {
                customResponseListener.onError("Wrong Json Type!",response.code());
            }
        } else {
            if (response.errorBody() != null) {
                ResponseBody errorString = response.errorBody();
                Log.d("logcallbackerorr",response.errorBody().toString());
                String message = "";
                try {
                    String json = errorString.string();
                    Log.d("dfkj",errorString.toString());
                    Log.d("error", json);
                    JSONObject jsonOb = new JSONObject(json);
                    if (response.code() == 401) {
                        if(jsonOb.has("message")){
                            message = jsonOb.getString("message");
                            customResponseListener.onError(message,response.code());
                        } else if (jsonOb.has("error") && !jsonOb.getString("error").equalsIgnoreCase("Unauthenticated access")) {
                            message = jsonOb.getString("error");
                            customResponseListener.onError(message,response.code());
                        } else if (jsonOb.has("msg")){
                            customResponseListener.onError(message,response.code());
                        }
                    } else if(response.code() == 405){
                        if (jsonOb.has("msg")){
                            message = jsonOb.getString("msg");
                            customResponseListener.onError(message,response.code());
                        }
                    }
                    else if (jsonOb.has("msg")) {
                        message = jsonOb.getString("msg");
                        customResponseListener.onError(message,response.code());
                    } else if (jsonOb.has("error")) {
                        message = jsonOb.getString("error");
                        customResponseListener.onError(message,response.code());
                    }
                    else if (jsonOb.has("error_des")){
                        message = jsonOb.getString("error_des");
                        customResponseListener.onError(message,response.code());
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                    Log.e("jfgdf"+e.getClass().getName(), e.getMessage() + "");
                    message = e.getMessage();
                    customResponseListener.onError(message,response.code());
                } catch (IOException e2) {
                    Log.e(e2.getClass().getName(), e2.getMessage() + "");
                    message = e2.getMessage();
                    customResponseListener.onError(message,response.code());
                }
            }
        }
    }

    @Override
    public void onFailure(@NotNull Call<JsonElement> call, @NotNull Throwable t ) {
        customResponseListener.onError(t.getMessage(), 500);
    }

}
