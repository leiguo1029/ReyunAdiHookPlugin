package com.fear1ess.reyunaditool.adsfinder;

import android.os.RemoteException;
import android.util.Log;

import com.fear1ess.reyunaditool.HookEntry;
import com.fear1ess.reyunaditool.IDoCommandService;
import com.fear1ess.reyunaditool.OperateCmd;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.robv.android.xposed.XC_MethodHook;

public abstract class Finder {
    protected String mAdsClassName;
    protected String mAdsName;
    protected ClassLoader mAppClassLoader;
    protected IDoCommandService mDoCommandService;
    protected List<Map> mRemainAdsDataList = new ArrayList<>();
    protected volatile boolean isServiceBound = false;
    public static String TAG = "reyunadihookplugin_adsfinder";

    public Finder(String adsName,String adsClassName,ClassLoader cl,IDoCommandService service){
        mAdsClassName = adsClassName;
        mAdsName = adsName;
        mAppClassLoader = cl;
        mDoCommandService = service;
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
            return true;
        }
    }

    public void startWork(){
        if(!isAdsExisted()) return;
        hookAdsApi();
    }

    public abstract void hookAdsApi();

    public void uploadRemainAdsData(IDoCommandService service){
        isServiceBound = true;
        mDoCommandService = service;
        for(Map map : mRemainAdsDataList){
            uploadAdsData(map);
        }
    }

    public void uploadAdsData(Map<String,String> map){
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
            jo2.put("app_id", HookEntry.processName);
            jo3.put("data",jo2);
            String uploadData = jo3.toString();
            Log.d(TAG, "uploadAdsData: " + uploadData);;
            String res = mDoCommandService.doCommand(OperateCmd.UPLOAD_ADSDK_DATA, uploadData);
            if(res.equals("success")){
                Log.d(TAG, "uploadAdsData success");
            }
        } catch (RemoteException | JSONException e) {
            e.printStackTrace();
        }
    }

    public class AdsApiHook extends XC_MethodHook {
        public String mAdType;
        public String mDataName;
        public int mPos;

        public AdsApiHook(String s, String dataName,  int posInArgs){
            super();
            mAdType = s;
            mDataName = dataName;
            mPos = posInArgs;
        }

        public Map buildAdsData(String data){
            Map<String,String> map = new HashMap<>();
            map.put(mDataName, data);
            return map;
        }

        public void handleAdUnitId(String adUnitId){
            if(adUnitId == null) return;
            Map map = buildAdsData(adUnitId);
            if(map == null) return;
            uploadAdsData(map);
        }

        @Override
        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
            super.beforeHookedMethod(param);
            String data = (String) param.args[mPos];
            Log.d(TAG, "find " + mAdsName + " " + mAdType + ", " + mDataName +": " + data);
            handleAdUnitId(data);
        }
    }
}
