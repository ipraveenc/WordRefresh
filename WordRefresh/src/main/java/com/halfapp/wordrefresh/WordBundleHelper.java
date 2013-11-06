package com.halfapp.wordrefresh;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;

import java.util.Set;

/**
 * This class handles the saving and loading of word bundles to shared preferences.
 */
public class WordBundleHelper
{
    private Context context;
    private SharedPreferences.Editor editor;
    private SharedPreferences sharedPreferences;
    private String KEY_SET = "Key set";

    public enum WordParts
    {
        word,
        wordPronunciation,
        wordPartOfSpeech,
        wordDefinition,
        wordExample,
        wordExampleSource
    }

    public WordBundleHelper(Context ctx)
    {
        context = ctx;

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        editor = sharedPreferences.edit();
    }

    public void saveWordBundleToDefaultSharedPreferences(Bundle b)
    {
        Set<String> keySet = b.keySet();

        for (int i = 0; i < b.size(); i++)
        {
            editor.putString((String)keySet.toArray()[i], b.getString((String)keySet.toArray()[i]));
        }

        editor.putStringSet(KEY_SET, keySet);
        editor.commit();
    }

    public Bundle loadWordBundleFromDefaultSharedPreferences()
    {
        Bundle b = new Bundle();
        Set<String> keySet = PreferenceManager.getDefaultSharedPreferences(context).getStringSet(KEY_SET, null);

        if (keySet != null)
        {
            for (int i = 0; i < keySet.size(); i++)
            {
                b.putString((String)keySet.toArray()[i], sharedPreferences.getString((String)keySet.toArray()[i], ""));
            }
        }

        return b;
    }
}