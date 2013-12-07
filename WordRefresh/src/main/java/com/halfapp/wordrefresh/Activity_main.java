package com.halfapp.wordrefresh;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBarActivity;
import android.widget.Toast;

import com.google.analytics.tracking.android.EasyTracker;
import com.google.analytics.tracking.android.GoogleAnalytics;
import java.util.Calendar;

/*
    This activity handles all the fragment transactions as well as all of
    the communication between fragments.
 */
public class Activity_main extends ActionBarActivity implements OnFragmentChangedListener
{
    private boolean inFragmentSettings = false;

    //  The ID is used to overwrite an already existing alarm (if present in Alarm Manager)
    private int pendingIntentId = 1234;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setContentView(R.layout.activity_main);

        if (savedInstanceState == null)
        {
            getFragmentManager().beginTransaction()
                    .add(R.id.container, new Fragment_Word())
                    .addToBackStack(null)
                    .commit();
        }
        
        if (isNewVersion())
            showChangeLog();
    }

    private void showChangeLog()
    {
        ChangeLog changeLog = new ChangeLog();
        changeLog.show(getSupportFragmentManager().beginTransaction(), "Changelog");
    }

    private boolean isNewVersion()
    {
        int savedVersionNumber = PreferenceManager.getDefaultSharedPreferences(this).getInt("Version number", 0);
        int currentVersionNumber = 0;

        try
        {
            currentVersionNumber = getPackageManager().getPackageInfo(getPackageName(), 0).versionCode;
        } catch (PackageManager.NameNotFoundException e)
        {
            e.printStackTrace();
        }

        if (currentVersionNumber > savedVersionNumber)
        {
            //  Save current version, so the changelog only shows once
            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
            SharedPreferences.Editor editor = sharedPreferences.edit();

            editor.putInt("Version number", currentVersionNumber)
                .commit();

            return true;
        }

        return false;
    }

    @Override
    public void onStart()
    {
        super.onStart();

        //  Set up Google Analytics tracking
        EasyTracker.getInstance().activityStart(this);
        GoogleAnalytics ga = GoogleAnalytics.getInstance(this);
        ga.setAppOptOut(PreferenceManager.getDefaultSharedPreferences(this).getBoolean(Fragment_Settings.KEY_PREFERENCE_TRACKING, false));
    }

    @Override
    protected void onResume()
    {
        super.onResume();

        if (PreferenceManager.getDefaultSharedPreferences(this).getBoolean(Fragment_Settings.KEY_PREFERENCE_DAILY_REFRESH, true))
            setDailyWordRefresh();
    }

    @Override
    public void onStop()
    {
        super.onStop();

        EasyTracker.getInstance().activityStop(this);
    }

    private void setDailyWordRefresh()
    {
        Calendar cal = Calendar.getInstance();
        int refreshTime = Integer.parseInt(PreferenceManager.getDefaultSharedPreferences(this).getString(Fragment_Settings.KEY_PREFERENCE_DAILY_REFRESH_TIME, "8"));

        if (cal.get(Calendar.HOUR_OF_DAY) >= refreshTime)
        {
            cal.set(Calendar.DAY_OF_MONTH, cal.get(Calendar.DAY_OF_MONTH) + 1);
            cal.set(Calendar.HOUR_OF_DAY, refreshTime);
            cal.set(Calendar.MINUTE, 0);
        }

        else
        {
            cal.set(Calendar.DAY_OF_MONTH, cal.get(Calendar.DAY_OF_MONTH));
            cal.set(Calendar.HOUR_OF_DAY, refreshTime);
            cal.set(Calendar.MINUTE, 0);
        }

        setAlarmAtCalendarTime(cal);
    }

    private void setAlarmAtCalendarTime(Calendar cal)
    {
        Intent i = new Intent(this, Service_GetWord.class);
        //  This is used as a check for displaying a notification in Service_GetWord
        i.putExtra("Is daily refresh?", true);

        PendingIntent pi = PendingIntent.getService(this, pendingIntentId, i, 0);
        AlarmManager am = (AlarmManager)getSystemService(ALARM_SERVICE);
        am.setRepeating(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), AlarmManager.INTERVAL_DAY, pi);
    }

    public void cancelDailyWordRefresh()
    {
        Intent i = new Intent(this, Service_GetWord.class);
        PendingIntent pi = PendingIntent.getService(this, pendingIntentId, i, 0);
        AlarmManager am = (AlarmManager)getSystemService(ALARM_SERVICE);
        am.cancel(pi);
    }

    @Override
    public void OnSettingsButtonClicked()
    {
        inFragmentSettings = true;

        getFragmentManager().beginTransaction()
                .replace(R.id.container, new Fragment_Settings())
                .addToBackStack(null)
                .commit();
    }

    @Override
    public void OnDailyRefreshPreferenceChanged()
    {
        if(PreferenceManager.getDefaultSharedPreferences(this).getBoolean(Fragment_Settings.KEY_PREFERENCE_DAILY_REFRESH, true))
                 setDailyWordRefresh();
        
        else     cancelDailyWordRefresh();
    }

    @Override
    public void OnGoogleTrackingPreferenceChanged()
    {
        GoogleAnalytics ga = GoogleAnalytics.getInstance(this);
        ga.setAppOptOut(PreferenceManager.getDefaultSharedPreferences(this).getBoolean(Fragment_Settings.KEY_PREFERENCE_TRACKING, false));
    }

    @Override
    public void onBackPressed()
    {
        //  This is a hack because the normal add to back stack with the back
        //  button doesn't work :(
        if(inFragmentSettings)
        {
            getFragmentManager().popBackStack();
            inFragmentSettings = false;
        }
        else
        {
            super.onBackPressed();
        }
    }
}