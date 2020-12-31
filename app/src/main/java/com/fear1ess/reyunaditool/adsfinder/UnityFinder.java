package com.fear1ess.reyunaditool.adsfinder;

import android.util.Log;

import com.fear1ess.reyunaditool.HttpParser;
import com.fear1ess.reyunaditool.IDoCommandService;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

public class UnityFinder extends Finder {
    public UnityFinder(String adsName, String adsClassName, ClassLoader cl, IDoCommandService service) {
        super(adsName, adsClassName, cl, service);
    }

    @Override
    public void hookAdsApi() {
        registerSSLHook(new SSLOutputStreamHookedCallback() {
            @Override
            public void onSSLOutputStreamHooked(HttpParser hp) {
                if(!hp.getMethod().equals("POST") || !hp.getPath().contains("/v6/games")) return;
                Log.d(TAG, "onSSLOutputStreamHooked: unity0");
                String unityGameId = match(hp.getPath(), "v6/games/.*/requests").replace("v6/games/","")
                        .replace("/requests","");
                if(unityGameId == null) return;
                Log.d(TAG, "onSSLOutputStreamHooked: unity1");
                JSONObject jo = hp.getJsonBody();
                String gameSessionId = null;
                try {
                    gameSessionId = jo.getString("gameSessionId");
                    Log.d(TAG, "onSSLOutputStreamHooked: unity2");
                    String projectId = jo.getString("projectId");
                    String token = jo.getString("token");
                 //   if(gameSessionId == null || projectId == null || token == null) return;
                    Map<String, String> map = new HashMap<>();
                    map.put("gameSessionId", gameSessionId);
                    map.put("token", token);
                    map.put("projectId", projectId);
                    map.put("unityGameId", unityGameId);
                    uploadAdsData(map);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });

        /*
        try {
            XposedBridge.hookAllMethods(mAppClassLoader.loadClass("com.unity3d.ads.UnityAds"), "initialize",
                    new AdsApiHook("AD", "unityGameId", 1));

            XposedHelpers.findAndHookMethod("com.unity3d.services.core.request.WebRequest", mAppClassLoader,
                    "makeRequest", new UnityNetWorkHook("AD", "unityParam", 0));

            XposedHelpers.findAndHookMethod("java.net.SocketOutputStream", mAppClassLoader, "write",
                    byte[].class, int.class, int.class, new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                            super.beforeHookedMethod(param);
                            Log.d(TAG, "socketos hook success!!!!");
                         //   String data = new String((byte[])param.args[0]);
                         //   Log.d(TAG, "SocketOutput data: " + data);
                        }
                    });

            XposedHelpers.findAndHookMethod("sun.nio.cs.StreamEncoder", mAppClassLoader, "write",
                    String.class, int.class, int.class, new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                            super.beforeHookedMethod(param);
                            Log.d(TAG, "streamencoder hook success!!!!");
                            //   String data = new String((byte[])param.args[0]);
                                 Log.d(TAG, "streamencoder data: " + param.args[0]);
                        }
                    });
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }*/
    }

    public class UnityNetWorkHook extends AdsApiHook{

        public UnityNetWorkHook(String s, String dataName, int posInArgs) {
            super(s, dataName, posInArgs);
        }

        @Override
        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
            Object o = param.thisObject;
            URL url = (URL) XposedHelpers.getObjectField(o, "_url");
            String urlStr = url.toString();
            Log.d(TAG, "urlStr: " + urlStr);
            Pattern pattern = Pattern.compile("v6/games/.*/requests");
            Matcher matcher = pattern.matcher(urlStr);
            if(!matcher.find()) return;
            Log.d(TAG, "1");
            String body = (String) XposedHelpers.getObjectField(o, "_body");
            JSONObject jo = new JSONObject(body);
            Log.d(TAG, "2");
            if(!jo.has("gameSessionId") || !jo.has("token") || !jo.has("projectId")) return;
            Log.d(TAG, "3");
            Map<String,String> map = new HashMap<>();
            map.put("gameSessionId", jo.getString("gameSessionId"));
            map.put("token", jo.getString("token"));
            map.put("projectId", jo.getString("projectId"));
            String unityGameId = matcher.group(0).replace("v6/games/","").replace("/requests","");
            map.put("unityGameId", unityGameId);
            uploadAdsData(map);
        }
    }
}
