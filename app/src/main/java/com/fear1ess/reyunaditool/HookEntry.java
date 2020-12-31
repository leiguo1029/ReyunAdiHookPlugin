package com.fear1ess.reyunaditool;

import android.app.Activity;
import android.app.Application;
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
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class HookEntry implements IXposedHookLoadPackage {
    public Context cxt = null;
    public static ClassLoader cl = null;
    public static ArrayList<String> admobDataList = new ArrayList<>();
    public static IDoCommandService hdService = null;
    public static ServiceConnection conn = null;
    public static String processName = null;
    public static String TAG = "adihookplugin_log";
    public static String[] notNeedAppList = {"android", "system_server", "org.meowcat.edxposed.manager"};

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        processName = (String)Class.forName("android.app.ActivityThread").getDeclaredMethod("currentProcessName").invoke(null);

        Log.d(TAG, "handleLoadPackage processName :" + processName);

        if((lpparam.appInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0) return;


        for(String item : notNeedAppList) {
            if(item.contains(processName)) return;
        }

        cl = lpparam.classLoader;
        XposedHelpers.findAndHookMethod("android.app.Application", cl, "attach",
                Context.class, new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        super.beforeHookedMethod(param);
                        Log.d(TAG, "enter app attach....");
                        cxt = (Context) param.args[0];

                        if(processName.equals("com.topjohnwu.magisk")){
                            Log.d(TAG, "open app");
                            ExecuteCmdUtils.startApp(cxt, "com.fear1ess.reyunaditool");
                            return;
                        }

                        if(lpparam.processName.contains("com.fear1ess.reyunaditool")) return;

                        Log.d(TAG, "start hook " + processName);

                        Log.d(TAG, "start bindservice...");
                        bindAdiToolService();

                        FinderUtils.doWork(cl,null);
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

                new Thread(){
                    @Override
                    public void run() {
                        try {
                            String curPkgName = hdService.doCommand(OperateCmd.QUERY_CURRENT_PKGNAME, null);
                        //    if(!processName.equals(curPkgName)) return;
                        } catch (RemoteException e) {
                            e.printStackTrace();
                        }
                        FinderUtils.notifyServiceBound(hdService);
                    }
                }.start();
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {

            }
        };

        boolean res = cxt.bindService(intent, conn, Service.BIND_AUTO_CREATE);
        if(res == false) Log.d(TAG, "bindAdiToolService failed!");
        if(res == true) Log.d(TAG, "bindAdiToolService success!");
    }
}
