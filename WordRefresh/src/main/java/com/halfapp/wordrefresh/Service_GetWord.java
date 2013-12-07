package com.halfapp.wordrefresh;

import android.app.AlarmManager;
import android.app.IntentService;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.RingtoneManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import net.jeremybrooks.knicker.Knicker;
import net.jeremybrooks.knicker.KnickerException;
import net.jeremybrooks.knicker.WordApi;
import net.jeremybrooks.knicker.WordsApi;
import net.jeremybrooks.knicker.dto.Definition;
import net.jeremybrooks.knicker.dto.Example;
import net.jeremybrooks.knicker.dto.Pronunciation;
import net.jeremybrooks.knicker.dto.Word;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/*  This connects to the Wordnik API and gets a word with the proper definition.
    The result is then saved to the shared preferences of the application.
    This service relies on the use of the Knicker library for connecting to Wordnik.
 */
public class Service_GetWord extends IntentService
{
    static final String TAG = "Service_GetWord";
    public static final String SERVICE_SUCCESS = "Service success";
    public static final String SERVICE_FAIL = "Service fail";
    public static final String INTENT_RESULT = "Intent result";

    private int counter = 0;

    //  For every word Wordnik sends you a list, so you gotta work with lists.
    private List<Word> listRandomWords;
    private List<Definition> listWordDefinitions;
    private List<Pronunciation> listWordPronunciations;
    private List<Example> listWordExamples;

    public Service_GetWord()
    {
        super(TAG);
        System.setProperty("WORDNIK_API_KEY", DeveloperKeys.WORDNIK_API_KEY);
    }

    @Override
    protected void onHandleIntent(Intent intent)
    {
        if (isOnline())
        {
            listRandomWords = getRandomWordsFromWordnik();

            do
            {
                listWordDefinitions = getDefinitionsFromWordnik(listRandomWords.get(counter).getWord());

                if(isProperDefinition(listWordDefinitions))
                {
                    break;
                }

                counter++;

                //  If there's no proper definitions, get 10 new words and repeat
                if(counter == listRandomWords.size() && !isProperDefinition(listWordDefinitions))
                {
                    listRandomWords = getRandomWordsFromWordnik();
                    counter = 0;
                }
            }
            while(listWordDefinitions != null);

            listWordPronunciations = getPronunciationsFromWordnik(listRandomWords.get(counter).getWord());
            listWordExamples = getExamplesFromWordnik(listRandomWords.get(counter).getWord());

            Bundle wordBundle = createWordBundleFromLists();

            WordBundleHelper wbHelper = new WordBundleHelper(getApplicationContext());
            wbHelper.saveWordBundleToDefaultSharedPreferences(wordBundle);

            if(intent.getBooleanExtra("Is daily refresh?", false))
                showNotification(listRandomWords.get(counter).getWord());

            onServiceSuccess();
        }

        else
        {
            onServiceFailed();

            //TODO: Add repeating alarm, if the user is not online to recieve the new word of the day
        }
    }

    private void onServiceSuccess()
    {
        Intent i = new Intent(TAG);
        i.putExtra(INTENT_RESULT, SERVICE_SUCCESS);
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(i);

        //  When the user recieves first new word, they are no longer a first time user
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        if(sharedPreferences.getBoolean("First time user?", true))
            sharedPreferences.edit().putBoolean("First time user?", false).commit();

    }

    private void onServiceFailed()
    {
        Intent i = new Intent(TAG);
        i.putExtra(INTENT_RESULT, SERVICE_FAIL);
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(i);
    }

    public boolean isOnline()
    {
        ConnectivityManager cm = (ConnectivityManager)getSystemService(getApplicationContext().CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();

        if (netInfo != null && netInfo.isConnectedOrConnecting())
        {
            return true;
        }

        return false;
    }

    private List<Word> getRandomWordsFromWordnik()
    {
        Set<Knicker.PartOfSpeech> exclude = new HashSet<Knicker.PartOfSpeech>();
        exclude.add(Knicker.PartOfSpeech.proper_noun);

        List<Word> list = new ArrayList<Word>();

        try
        {
            return WordsApi.randomWords(true, null, exclude, 0, 0, 2, 0, 0, 0, null, null, 10);

        } catch (KnickerException e)
        {
            e.printStackTrace();
        }

        return list;
    }

    private List<Definition> getDefinitionsFromWordnik(String word)
    {
        Set<Knicker.SourceDictionary> ahdDictionary = new HashSet<Knicker.SourceDictionary>();
        ahdDictionary.add(Knicker.SourceDictionary.ahd);

        try
        {
            //  Changing the word to lowercase will remove all the proper names and last names
            //  This is a hack because Wordnik API doesn't work like it should :(
            return WordApi.definitions(word.toLowerCase(), ahdDictionary);
        } catch (KnickerException e)
        {
            e.printStackTrace();
        }

        return null;
    }

    private List<Pronunciation> getPronunciationsFromWordnik(String word)
    {
        try
        {
            return WordApi.pronunciations(word, false, null, Knicker.TypeFormat.ahd, 5);
        } catch (KnickerException e)
        {
            e.printStackTrace();
        }

        return null;
    }

    private List<Example> getExamplesFromWordnik(String word)
    {
        try
        {
            return WordApi.examples(word, false, null, true, 0, 1).getExamples();
        } catch (KnickerException e)
        {
            e.printStackTrace();
        }

        return null;
    }

    private Bundle createWordBundleFromLists()
    {
        Bundle bundle = new Bundle();

        bundle.putString(WordBundleHelper.WordParts.word.name(), listRandomWords.get(counter).getWord());

        String wordDefinitions = "";

        for (int i = 0; i < listWordDefinitions.size(); i++)
        {
            if (i == 0)     wordDefinitions = (i + 1) + ". " + listWordDefinitions.get(i).getText() + "\n";
            else            wordDefinitions = wordDefinitions + (i + 1) + ". " + listWordDefinitions.get(i).getText() + "\n";

            if (i != listWordDefinitions.size() -1)
                wordDefinitions = wordDefinitions + "\n";
        }

        bundle.putString(WordBundleHelper.WordParts.wordDefinition.name(), wordDefinitions);

        if (listWordPronunciations.size() > 0)                          bundle.putString(WordBundleHelper.WordParts.wordPronunciation.name(), listWordPronunciations.get(0).getRaw());
        if (!listWordDefinitions.get(0).getPartOfSpeech().equals(""))   bundle.putString(WordBundleHelper.WordParts.wordPartOfSpeech.name(), listWordDefinitions.get(0).getPartOfSpeech());

        if (listWordExamples.size() > 0)
        {
            bundle.putString(WordBundleHelper.WordParts.wordExample.name(), listWordExamples.get(0).getText());
            bundle.putString(WordBundleHelper.WordParts.wordExampleSource.name(), listWordExamples.get(0).getTitle());
        }

        return bundle;
    }

    private boolean isProperDefinition(List<Definition> list)
    {
        if (list == null || list.size() == 0)
        {
            return false;
        }

        return true;
    }

    public void showNotification(String word)
    {
        NotificationManager notificationManager = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);

        Intent intent = new Intent(this, Activity_main.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, 0);

        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(this)
                        .setContentTitle("WordRefresh!")
                        .setSmallIcon(R.drawable.ic_word_notification)
                        .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
                        .setContentText("Today's word is: " + word)
                        .setContentIntent(pendingIntent)
                        .setAutoCancel(true);

        notificationManager.notify(00, mBuilder.build());
    }
}