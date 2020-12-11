package com.fear1ess.reyunaditool;

import android.app.Activity;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.RemoteException;
import android.util.Log;

import com.fear1ess.reyunaditool.adsfinder.AdmobFinder;
import com.fear1ess.reyunaditool.adsfinder.Finder;
import com.fear1ess.reyunaditool.adsfinder.FinderUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class HookEntry implements IXposedHookLoadPackage {
    public Context cxt = null;
    public ClassLoader cl = null;
    public static ArrayList<String> admobDataList = new ArrayList<>();
    public static IDoCommandService hdService = null;
    public static ServiceConnection conn = null;
    public static String processName = null;
    public static String TAG = "reyunadihookplugin_adsdkinfo";
    public static String serviceUrlBase = "http://127.0.0.1:2020?cmd=";

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        cl = lpparam.classLoader;

        XposedHelpers.findAndHookMethod("android.app.Application", cl, "attach",
                Context.class, new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        super.afterHookedMethod(param);
                        Log.d(TAG, "enter app attach....");
                        cxt = (Context) param.args[0];
                        processName = (String)Class.forName("android.app.ActivityThread").getDeclaredMethod("currentProcessName").invoke(null);
                        if(processName.equals("com.fear1ess.reyunaditool")) return;

                        Log.d(TAG, "start hook " + processName);

                        FinderUtils.doWork(cl,null);

                        bindAdiToolService();
                    }
                });

    }


    public void bindAdiToolService(){
        Intent intent = new Intent();
        intent.setComponent(new ComponentName("com.fear1ess.reyunaditool",
                "com.fear1ess.reyunaditool.DoCommandService"));

        conn = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                hdService = IDoCommandService.Stub.asInterface(service);

                FinderUtils.notifyServiceBound(hdService);

             //   new Thread(new UploadDataThread()).start();
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {

            }
        };

        cxt.bindService(intent, conn, Service.BIND_AUTO_CREATE);
    }

    public static class UploadDataThread implements Runnable {

        @Override
        public void run() {
          //  NetWorkUtils.Response res = NetWorkUtils.get(serviceUrlBase + OperateCmd.QUERY_CURRENT_PKGNAME,null);

            try {
                String curPkg = hdService.doCommand(OperateCmd.QUERY_CURRENT_PKGNAME, null);
                Log.d(TAG, "currentPkg: " + curPkg);
                Log.d(TAG, "processName: " + processName);
                if(!processName.equals(curPkg)) return;
            } catch (RemoteException e) {
                e.printStackTrace();
            }

            try {
                Thread.sleep(10*1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            synchronized (admobDataList){
                try {
                    JSONArray jArr = new JSONArray();
                    for(String str : admobDataList){
                        jArr.put(str);
                    }
                    JSONObject jo1 = new JSONObject();
                    jo1.put("adUnitId",jArr);
                    JSONObject jo2 = new JSONObject();
                    jo2.put("admob",jo1);
                    JSONObject jo3 = new JSONObject();
                    jo2.put("app_id", processName);
                    jo3.put("data",jo2);
                    String uploadData = jo3.toString();
                    Log.d(TAG, "uploadData: " + uploadData);

                 //   NetWorkUtils.post(serviceUrlBase + OperateCmd.UPLOAD_ADSDK_INFO,null,uploadData.getBytes());
                 //   NetWorkUtils.get(serviceUrlBase + OperateCmd.SHUTDOWN_APP,null);
                    hdService.doCommand(OperateCmd.UPLOAD_ADSDK_DATA, uploadData);
                //    hdService.doCommand(OperateCmd.SHUTDOWN_APP,null);

                } catch (JSONException | RemoteException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
