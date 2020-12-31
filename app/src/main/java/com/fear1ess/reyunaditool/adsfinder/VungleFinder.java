package com.fear1ess.reyunaditool.adsfinder;

import android.util.Log;

import com.fear1ess.reyunaditool.HttpParser;
import com.fear1ess.reyunaditool.IDoCommandService;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

public class VungleFinder extends Finder{
    public VungleFinder(String adsName, String adsClassName, ClassLoader cl, IDoCommandService service) {
        super(adsName, adsClassName, cl, service);
    }

    @Override
    public void hookAdsApi() {
        registerSSLHook(new SSLOutputStreamHookedCallback() {
            @Override
            public void onSSLOutputStreamHooked(HttpParser hp) {
                if(!hp.getMethod().equals("POST") || !hp.getPath().contains("api/v5/ads")) return;
                JSONObject jo = hp.getJsonBody();
                try {
                    JSONObject app = jo.getJSONObject("app");
                    String id = app.getString("id");
                    JSONObject request = jo.getJSONObject("request");
                    JSONArray pArr = request.getJSONArray("placements");
                    String placement = null;
                    for(int i = 0;i < pArr.length(); ++i){
                        placement = pArr.getString(i);
                    }
                    Map<String, String> map = new HashMap<>();
                    map.put("id", id);
                    map.put("placement", placement);
                    uploadAdsData(map);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });
        /*
        try {
            XposedBridge.hookAllMethods(mAppClassLoader.loadClass("com.vungle.warren.network.VungleApiImpl"), "createNewPostCall",
                    new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                            super.beforeHookedMethod(param);
                            Log.d(TAG, "vungle ads ua: " + param.args[0]);
                            Log.d(TAG, "vungle ads path: " + param.args[1]);
                            Log.d(TAG, "vungle ads body: " + XposedHelpers.callMethod(param.args[2],"toString"));
                        }
                    });


        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }*/
    }
}
