package com.fear1ess.reyunaditool.adsfinder;

import android.net.VpnManager;
import android.net.VpnService;
import android.os.RemoteException;
import android.util.Log;

import com.fear1ess.reyunaditool.HookEntry;
import com.fear1ess.reyunaditool.IDoCommandService;
import com.fear1ess.reyunaditool.OperateCmd;
import com.fear1ess.reyunaditool.RyTag;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FinderUtils {
    public static Map<String,String> adsInfoMap = new HashMap<>();
    public static List<Finder> finderArray = new ArrayList<>();

    static{
        adsInfoMap.put("admob", "com.google.android.gms.ads.MobileAds");
        adsInfoMap.put("unity", "com.unity3d.ads.UnityAds");
        adsInfoMap.put("vungle", "com.vungle.warren.Vungle");
        adsInfoMap.put("facebook", "com.facebook.ads.AudienceNetworkAds");
    }

    public static Finder createFinder(String adsName, String adsClsName, ClassLoader cl, IDoCommandService service){
        switch(adsName){
            case "admob":
                return new AdmobFinder(adsName, adsClsName, cl, service);
            case "unity":
                return new UnityFinder(adsName, adsClsName, cl, service);
            case "vungle":
                return new VungleFinder(adsName, adsClsName, cl, service);
            case "facebook":
                return new FacebookFinder(adsName, adsClsName, cl, service);
            default:
                return new DefaultFinder(adsName, adsClsName, cl, service);
        }
    }

    public static void uploadAdsSdkExistsState(IDoCommandService service, int sdkState, int dataState){
        if(service == null){
            Log.d(RyTag.TAG, "uploadAdsData failed, service is not bound" );
            return;
        }
        try {
            JSONObject jo = new JSONObject();
            jo.put("ads_sdk_state", sdkState);
            jo.put("ads_data_state", dataState);
            jo.put("package_name", HookEntry.processName);
            JSONObject jo2 = new JSONObject();
            jo2.put("data", jo);
            String uploadData = jo2.toString();
            Log.d(RyTag.TAG, "uploadAdsExistsStateData: " + uploadData);
            String res = service.doCommand(OperateCmd.UPLOAD_ADSDK_EXISTS_STATE, uploadData);
            if(res.equals("success")){
                Log.d(RyTag.TAG, "uploadAdsExistsStateData success");
            }
        } catch (JSONException | RemoteException e) {
            e.printStackTrace();
        }
    }

    public static void notifyServiceBound(IDoCommandService service){
        uploadAdsSdkExistsState(service, Finder.getAdsSdkState(), Finder.getAdsDataState());
        for(Finder finder : finderArray){
            finder.uploadRemainAdsData(service);
        }

        //do upload...
        ExecutorService es = Executors.newSingleThreadExecutor();
        es.execute(new UploadDataProceduce(service));
        es.shutdown();
    }

    public static void doWork(ClassLoader cl, IDoCommandService service){
        for(Map.Entry<String,String> entry : adsInfoMap.entrySet()){
            Finder finder = createFinder(entry.getKey(), entry.getValue(), cl, service);
            finderArray.add(finder);
            finder.startWork();
        }

    }

    public static class UploadDataProceduce implements Runnable {

        IDoCommandService hdService;

        public UploadDataProceduce(IDoCommandService service){
            hdService = service;
        }

        @Override
        public void run(){

            // wait for some time
            try {
                Thread.sleep(50*1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            String curPkg = null;
            try {
                curPkg = hdService.doCommand(OperateCmd.QUERY_CURRENT_PKGNAME, null);
                Log.d(RyTag.TAG, "currentPkg: " + curPkg);
                Log.d(RyTag.TAG, "processName: " + HookEntry.processName);
                //    if(!HookEntry.processName.equals(curPkg)) return;
            } catch (RemoteException e) {
                e.printStackTrace();
            }

            //upload ads data
            try {
                JSONObject jo = new JSONObject(Finder.adsMap);
                jo.put("app_id", HookEntry.processName);
                String data = jo.toString();
                Log.d(RyTag.TAG, "upload ads data: " + data);
                if(!HookEntry.processName.equals(curPkg)) return;
                hdService.doCommand(OperateCmd.UPLOAD_ADSDK_DATA, data);
              //  hdService.doCommand(OperateCmd.SHUTDOWN_APP, null);
            } catch (JSONException | RemoteException e) {
                e.printStackTrace();
            }
        }
    }
}
