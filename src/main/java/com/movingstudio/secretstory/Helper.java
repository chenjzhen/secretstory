package com.movingstudio.secretstory;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.text.TextUtils;
import android.util.Log;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class Helper  {

    static public boolean isSimpleChinese() {
        Locale l = Locale.getDefault();
        String language = l.getLanguage();
        String country = l.getCountry().toLowerCase();

        if ("zh".equals(language)) {
            if ("cn".equals(country)) {
                language = "zh-CN";
                return true;
            }
        }
        return false;
    }

    static public  void CheckValid(String strDeadLine)
    {
        Date today = new Date();
        SimpleDateFormat ft = new SimpleDateFormat ("yyyy-MM-dd");
        //String strDeadLine = "2019-3-15";
        //System.out.println("Current Date: " + ft.format(today));
        try {
            Date deadLine = ft.parse(strDeadLine);
            if(today.after(deadLine))
            {
                System.exit(1);
            }

        } catch (ParseException e) {
            //e.printStackTrace();
        }

    }

    static public int getChannelId(Context context)
    {
        String channelNumber = getAppMetaData(context, "MY_CHANNEL");
        //Toast.makeText(this, channelNumber, Toast.LENGTH_SHORT).show();
        if(channelNumber.compareTo("googleplay") == 0)
        {
            return 0;
        }
        else if(channelNumber.compareTo("migu") == 0)
        {
            return 1;
        }
        else if(channelNumber.compareTo("tianyi") == 0)
        {
            return 2;
        }
        else
        {
            return -1;
        }
    }

    static public  String getAppMetaData(Context context, String key) {
        if (context == null || TextUtils.isEmpty(key)) {
            return null;
        }
        String channelNumber = null;
        try {
            PackageManager packageManager = context.getPackageManager();
            if (packageManager != null) {
                ApplicationInfo applicationInfo = packageManager.getApplicationInfo(context.getPackageName(), PackageManager.GET_META_DATA);
                if (applicationInfo != null) {
                    if (applicationInfo.metaData != null) {
                        channelNumber = applicationInfo.metaData.getString(key);
                    }
                }
            }
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return channelNumber;
    }

}

