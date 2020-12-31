package com.fear1ess.reyunaditool.adsfinder;

import android.graphics.Path;
import android.os.Handler;
import android.os.RemoteException;
import android.util.Log;

import com.fear1ess.reyunaditool.AdsSdkExistsFlag;
import com.fear1ess.reyunaditool.HookEntry;
import com.fear1ess.reyunaditool.HttpParser;
import com.fear1ess.reyunaditool.IDoCommandService;
import com.fear1ess.reyunaditool.OperateCmd;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;

public abstract class Finder {
    public static SSLOutputStreamHook sslHook = new SSLOutputStreamHook();
    public static Map<String, Map<String, String>>  adsMap = new HashMap<>();
    protected String mAdsClassName;
    protected String mAdsName;
    protected ClassLoader mAppClassLoader;
    protected IDoCommandService mDoCommandService;
    protected List<Map> mRemainAdsDataList = new ArrayList<>();
    protected volatile boolean isServiceBound = false;
    public static String TAG = "reyunadihookplugin_adsfinder";
    private static int adsSdkState = 0;
    private static int adsDataState = 0;

    public Finder(String adsName,String adsClassName,ClassLoader cl,IDoCommandService service){
        mAdsClassName = adsClassName;
        mAdsName = adsName;
        mAppClassLoader = cl;
        mDoCommandService = service;
    }

    static {
        hookSSLOutputStream(sslHook);
    }

    public boolean isAdsExisted(){
        Class<?> cls = null;
        try{
            Log.d(TAG, "start find " + mAdsClassName);
            cls = mAppClassLoader.loadClass(mAdsClassName);
        } catch (ClassNotFoundException e) {
            Log.d(TAG, "not find ads: " + mAdsName);
        } finally {
            if(cls == null) return false;
            Log.d(TAG, "find ads: " + mAdsName);
            updateAdsSdkState(mAdsName);
            return true;
        }
    }

    public static int getAdsSdkState(){
        return adsSdkState;
    }


    public static int getAdsDataState() {
        return adsDataState;
    }

    public static void updateAdsSdkState(String adsName){
        switch (adsName){
            case "admob":
                adsSdkState |= AdsSdkExistsFlag.ADMOB;
                break;
            case "unity":
                adsSdkState |= AdsSdkExistsFlag.UNITY;
                break;
            case "vungle":
                adsSdkState |= AdsSdkExistsFlag.VUNGLE;
                break;
            case "facebook":
                adsSdkState |= AdsSdkExistsFlag.FACEBOOK;
            default: break;
        }
    }

    public static void updateAdsDataState(String adsName){
        switch (adsName){
            case "admob":
                adsDataState |= AdsSdkExistsFlag.ADMOB;
                break;
            case "unity":
                adsDataState |= AdsSdkExistsFlag.UNITY;
                break;
            case "vungle":
                adsDataState |= AdsSdkExistsFlag.VUNGLE;
                break;
            case "facebook":
                adsDataState |= AdsSdkExistsFlag.FACEBOOK;
                break;
            default: break;
        }
    }

    public void startWork(){
        if(!isAdsExisted()) return;
        hookAdsApi();
    }

    public void registerSSLHook(SSLOutputStreamHookedCallback cb){
        sslHook.registerSSLOutputStreamCallback(cb);
    }

    public void hookAdsApi(){
        return;
    }


    public void uploadRemainAdsData(IDoCommandService service){
        isServiceBound = true;
        mDoCommandService = service;
        for(Map map : mRemainAdsDataList){
            uploadAdsData(map);
        }
    }

    public void uploadAdsData(Map<String,String> map){
        //ad map to global ads map
        adsMap.put(mAdsName, map);

        /*
        if(isServiceBound == false){
            mRemainAdsDataList.add(map);
            Log.d(TAG, "uploadAdsData failed, service is not bound" );
            return;
        }

        try {
            JSONObject adsDataJson = new JSONObject();
            for(Map.Entry<String,String> entry : map.entrySet()){
                adsDataJson.put(entry.getKey(),entry.getValue());
            }
            JSONObject jo2 = new JSONObject();
            jo2.put(mAdsName, adsDataJson);
            JSONObject jo3 = new JSONObject();
            jo2.put("package_name", HookEntry.processName);
            jo3.put("data",jo2);
            String uploadData = jo3.toString();
            Log.d(TAG, "uploadAdsData: " + uploadData);
            String res = mDoCommandService.doCommand(OperateCmd.UPLOAD_ADSDK_DATA, uploadData);

            updateAdsDataState(mAdsName);
            FinderUtils.uploadAdsSdkExistsState(mDoCommandService, adsSdkState, adsDataState);
            if(res.equals("success")){
                Log.d(TAG, "uploadAdsData success");
            }
        } catch (RemoteException | JSONException e) {
            e.printStackTrace();
        }*/
    }


    public static void hookSSLOutputStream(XC_MethodHook hook){
        XposedHelpers.findAndHookMethod("com.android.org.conscrypt.ConscryptFileDescriptorSocket$SSLOutputStream", HookEntry.cl, "write",
                byte[].class, int.class, int.class, hook);
    }

    public static class SSLOutputStreamHook extends XC_MethodHook {
        private List<SSLOutputStreamHookedCallback> mCallbacks = new ArrayList<>();

        public void registerSSLOutputStreamCallback(SSLOutputStreamHookedCallback cb){
            mCallbacks.add(cb);
        }

        @Override
        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
            super.beforeHookedMethod(param);
            byte[] data = (byte[]) param.args[0];
            int pos = (int) param.args[1];
            int len = (int) param.args[2];
            String payload = new String(data, pos, len);
            Log.d(TAG, "SSLOutputSteam data: " + payload);
            HttpParser hp = new HttpParser(payload);
            for(SSLOutputStreamHookedCallback cb : mCallbacks){
                cb.onSSLOutputStreamHooked(hp);
            }
        }
    }   JSONObject jo = new JSONObject(Finder.adsMap);

    public interface SSLOutputStreamHookedCallback {
        void onSSLOutputStreamHooked(HttpParser hp);
    }

    public class AdsApiHook extends XC_MethodHook {
        public String mAdType;
        public String mDataName;
        public int mPos;

        public AdsApiHook(String s, String dataName, int posInArgs) {
            super();
            mAdType = s;
            mDataName = dataName;
            mPos = posInArgs;
        }

        public Map buildAdsData(String data) {
            Map<String, String> map = new HashMap<>();
            map.put(mDataName, data);
            return map;
        }

        public void handleAdUnitId(String adUnitId) {
            if (adUnitId == null) return;
            Map map = buildAdsData(adUnitId);
            if (map == null) return;
            uploadAdsData(map);
        }


        @Override
        protected void beforeHookedMethod(XC_MethodHook.MethodHookParam param) throws Throwable {
            super.beforeHookedMethod(param);
            String data = (String) param.args[mPos];
            Log.d(TAG, "find " + mAdsName + " " + mAdType + ", " + mDataName + ": " + data);
            handleAdUnitId(data);
        }
    }

    public String matchQueryValue(String str, String key) {
        String patternStr = key + "=((.*&)|(.* ))";
        String matchStr = match(str, patternStr);
        if(matchStr == null) return null;
        String value = matchStr.replace(key + "=","").
                replace("&","").replace(" ","");
        Log.d(TAG, "matchQueryValue key: " + key + ", value: " + value);
        return value;
    }

    public String matchJsonBodyValue(String str, String key) {
        String patternStr = "\"" + key + "\":" + "((.*,)|(.*\\}))";
        String matchStr = match(str, patternStr);
        if(matchStr == null) return null;
        String value = matchStr.replace("\"" + key + "\":", "")
                .replace(",", "").replace("}","");
        Log.d(TAG, "matchJsonBodyValue key: " + key + ", value: " + value);
        return value;
    }

    public String match(String str, String patternStr) {
        Pattern pattern = Pattern.compile(patternStr);
        Matcher matcher = pattern.matcher(str);
        if(!matcher.find()) return null;
        String value = matcher.group(0);
        return value;
    }
}
