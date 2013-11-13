package com.halfapp.wordrefresh;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.Activity;
import android.app.Fragment;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.analytics.tracking.android.GoogleAnalytics;
import com.google.analytics.tracking.android.Tracker;

/**
 * This is the main fragment that shows the word and its descriptions. The whole process uses
 * a service to connect to the Wordnik API, get the juicy stuff and then show it to the user.
 */
public class Fragment_Word extends Fragment
{
    private TextView textViewWord, textViewWordPronunciationAndPartOfSpeech, textViewWordDefinition,
    textViewWordExample, textViewWordExampleSource;
    private ImageButton buttonRefreshWord, buttonSettings;
    private LinearLayout viewContainerWord;
    private ProgressBar progressBar;

    //  All of the Strings of the displayed word are stored in a bundle
    private Bundle wordBundle;

    private WordBundleHelper wordBundleHelper;
    private OnFragmentChangedListener activityCommunicator;

    @Override
    public void onAttach(Activity activity)
    {
        super.onAttach(activity);

        activityCommunicator = (OnFragmentChangedListener)activity;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState)
    {
        View rootView = inflater.inflate(R.layout.fragment_main, container, false);

        initializeViews(rootView);
        setClickListeners();

        if (wordBundleHelper == null)    wordBundleHelper = new WordBundleHelper(getActivity().getApplicationContext());
        wordBundle = wordBundleHelper.loadWordBundleFromDefaultSharedPreferences();

        if(!firstTimeUser())
                setTextWithAnimation(false);

        return rootView;
    }

    private boolean firstTimeUser()
    {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity());

        if(sharedPreferences.getBoolean("First time user?", true))
        {
            return true;
        }

        else
        {
            return false;
        }
    }

    private void setClickListeners()
    {
        buttonRefreshWord.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                Intent i = new Intent(getActivity(), Service_GetWord.class);
                getActivity().startService(i);

                //  When the user presses the refresh button, it hides it and shows a progress bar
                buttonRefreshWord.setEnabled(false);
                buttonRefreshWord.setVisibility(View.GONE);
                progressBar.setVisibility(View.VISIBLE);
                progressBar.setIndeterminate(true);

                Tracker tracker = GoogleAnalytics.getInstance(getActivity()).getTracker(DeveloperKeys.GOOGLE_ANALYTICS_KEY);
                tracker.sendEvent("User action", "Pressed button", "Refresh word", (long)1);
            }
        });

        buttonSettings.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                activityCommunicator.OnSettingsButtonClicked();
            }
        });
    }

    @Override
    public void onResume()
    {
        IntentFilter iff = new IntentFilter(Service_GetWord.TAG);
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(onNotice, iff);

        super.onResume();
    }

    private void initializeViews(View rootView)
    {
        viewContainerWord = (LinearLayout)rootView.findViewById(R.id. viewContainerWord);
        progressBar =(ProgressBar)rootView.findViewById(R.id.progressBar);
        textViewWord = (TextView)rootView.findViewById(R.id.textViewWord);
        textViewWordPronunciationAndPartOfSpeech = (TextView)rootView.findViewById(R.id.textViewWordPronunciationAndPartOfSpeech);
        textViewWordDefinition = (TextView)rootView.findViewById(R.id.textViewWordDefinition);
        textViewWordExample = (TextView)rootView.findViewById(R.id.textViewWordExample);
        textViewWordExampleSource = (TextView)rootView.findViewById(R.id.textViewWordExampleSource);
        buttonRefreshWord = (ImageButton)rootView.findViewById(R.id.buttonRefreshWord);
        buttonSettings = (ImageButton)rootView.findViewById(R.id.buttonSettings);
    }

    private void setTextWithAnimation(boolean b)
    {
        final long animationDuration = 250;

        if (b)
        {
            viewContainerWord.animate()
                    .alpha(0f)
                    .setDuration(animationDuration)
                    .setListener(new AnimatorListenerAdapter()
                    {
                        @Override
                        public void onAnimationEnd(Animator animation)
                        {
                            super.onAnimationEnd(animation);
                            setTextViews();

                            viewContainerWord.animate()
                                    .alpha(1f)
                                    .setDuration(animationDuration)
                                    .setListener(null);
                        }
                    });
        }

        else    setTextViews();
    }

    private void setTextViews()
    {
        textViewWord.setText(wordBundle.getString(WordBundleHelper.WordParts.word.name()));
        textViewWordDefinition.setText(wordBundle.getString(WordBundleHelper.WordParts.wordDefinition.name()));

        textViewWordPronunciationAndPartOfSpeech.setText(wordBundle.getString(WordBundleHelper.WordParts.wordPronunciation.name()));

        if(wordBundle.getString(WordBundleHelper.WordParts.wordPronunciation.name()) != null)
        {
            textViewWordPronunciationAndPartOfSpeech.setText(wordBundle.getString(WordBundleHelper.WordParts.wordPronunciation.name()));

            if(wordBundle.getString(WordBundleHelper.WordParts.wordPartOfSpeech.name()) != null)   textViewWordPronunciationAndPartOfSpeech.
                    append(", " + wordBundle.getString(WordBundleHelper.WordParts.wordPartOfSpeech.name()));
        }

        else
        {
            if(wordBundle.getString(WordBundleHelper.WordParts.wordPartOfSpeech.name()) != null)   textViewWordPronunciationAndPartOfSpeech.
                    setText(wordBundle.getString(WordBundleHelper.WordParts.wordPartOfSpeech.name()));
        }

        textViewWordExample.setText(wordBundle.getString(WordBundleHelper.WordParts.wordExample.name()));
        textViewWordExampleSource.setText(wordBundle.getString(WordBundleHelper.WordParts.wordExampleSource.name()));
    }

    private BroadcastReceiver onNotice = new BroadcastReceiver()
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            if (intent.getStringExtra(Service_GetWord.INTENT_RESULT).equals(Service_GetWord.SERVICE_SUCCESS))
            {
                wordBundle = wordBundleHelper.loadWordBundleFromDefaultSharedPreferences();
                setTextWithAnimation(true);
            }

            else
            {
                Toast.makeText(getActivity(), "Not able to connect, please try again.", Toast.LENGTH_SHORT).show();
            }

            //  Hide the progress bar and show the refresh button again
            buttonRefreshWord.setEnabled(true);
            buttonRefreshWord.setVisibility(View.VISIBLE);
            progressBar.setVisibility(View.GONE);
            progressBar.setIndeterminate(true);
        }
    };
}

