package com.halfapp.wordrefresh;


import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceFragment;

/**
 * This fragment holds the Preference settings (used with preference_scenario_settings).
 */
public class Fragment_Settings extends PreferenceFragment implements SharedPreferences.OnSharedPreferenceChangeListener
{
    static final String KEY_PREFERENCE_DAILY_REFRESH = "preference_notification";
    static final String KEY_PREFERENCE_TRACKING ="preference_google_analytics";

    private OnFragmentChangedListener onFragmentChangedListener;

    @Override
    public void onAttach(Activity activity)
    {
        super.onAttach(activity);

        onFragmentChangedListener = (OnFragmentChangedListener)activity;
    }

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.preferences_scenario_settings);
    }

    @Override
    public void onResume()
    {
        super.onResume();

        getPreferenceScreen().getSharedPreferences()
                .registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onPause()
    {
        super.onPause();

        getPreferenceScreen().getSharedPreferences()
                .unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key)
    {
        if(key.equals(KEY_PREFERENCE_DAILY_REFRESH))
        {
            onFragmentChangedListener.OnDailyRefreshPreferenceChanged();
        }

        if(key.equals(KEY_PREFERENCE_TRACKING))
        {
            onFragmentChangedListener.OnGoogleTrackingPreferenceChanged();
        }
    }
}
