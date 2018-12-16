package com.zeus.sdk.test;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;

import com.movingstudio.secretstory.R;
import com.zeus.sdk.AresAnalyticsAgent;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * Created by ben on 2018/2/10.
 */

public class AnalyticsActivity extends Activity {
    private Random mRandom;
    private String mLevel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_analytics);
        mRandom = new Random();
    }

    public void startLevel(View v) {
        mLevel = (mRandom.nextInt(10) + 1) * 10 + "";
        AresAnalyticsAgent.startLevel(mLevel);
    }

    public void failLevel(View v) {
        AresAnalyticsAgent.failLevel(mLevel);
    }

    public void finishLevel(View v) {
        AresAnalyticsAgent.finishLevel(mLevel);
    }

    public void coinBonus(View v) {
        AresAnalyticsAgent.bonus(100, 1);
    }

    public void productBonus(View v) {
        AresAnalyticsAgent.bonus("1", 2, 1000, 2);
    }

    public void buy(View v) {
        AresAnalyticsAgent.buy("2", 1, 500);
    }

    public void use(View v) {
        AresAnalyticsAgent.use("2", 1, 500);
    }

    public void onPlayerLevel(View v) {
        AresAnalyticsAgent.onPlayerLevel(mRandom.nextInt(10) * 10);
    }

    public void onAccountSignIn(View v) {
        AresAnalyticsAgent.onAccountSignIn("123456", "WX");
    }

    public void onAccountSignOff(View v) {
        AresAnalyticsAgent.onAccountSignOff();
    }

    public void onEvent(View v) {
        AresAnalyticsAgent.onEvent("show_banner");
    }

    public void onEventValue(View v) {
        int duration = 120;//播放时间
        Map<String, String> map = new HashMap();
        map.put("event_id", "1");//激励id
        AresAnalyticsAgent.onEventValue("show_video", map, duration);
    }

    public void pay1(View v) {
        AresAnalyticsAgent.pay(10, 100);
    }

    public void pay2(View v) {
        AresAnalyticsAgent.pay(100, "1", 1, 1000);
    }

    public void pay3(View v) {
        AresAnalyticsAgent.pay(1000, "RMB", 10000, "123456789");
    }
}
