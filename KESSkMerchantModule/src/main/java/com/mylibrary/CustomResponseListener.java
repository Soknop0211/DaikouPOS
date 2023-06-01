package com.mylibrary;

import com.google.gson.JsonObject;

public interface CustomResponseListener {
    void onSuccess(JsonObject resObj);
    void onError(String error,int resCode);
}
