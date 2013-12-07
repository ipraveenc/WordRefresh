package com.halfapp.wordrefresh;

import android.app.Dialog;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageButton;
import android.widget.TextView;

/*
    Changelog dialog fragment. Shows a summary of the changes in the new version.
    Change the changelog text in values/strings.xml
 */
public class ChangeLog extends DialogFragment
{
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        View view = inflater.inflate(R.layout.changelog, container, false);

        TextView textViewVersionName = (TextView)view.findViewById(R.id.textViewVersionName);
        TextView textViewChangelog = (TextView)view.findViewById(R.id.textViewChangelog);
        ImageButton buttonChangelogOk = (ImageButton)view.findViewById(R.id.buttonChangelogOk);

        buttonChangelogOk.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                dismiss();
            }
        });

        String versionName = "";
        try
        {
            versionName = getActivity().getPackageManager().getPackageInfo(getActivity().getPackageName(), 0).versionName;
        } catch (PackageManager.NameNotFoundException e)
        {
            e.printStackTrace();
        }

        textViewVersionName.setText("New in version " + versionName);
        textViewChangelog.setText(getActivity().getResources().getString(R.string.changelog));

        return view;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState)
    {
        Dialog d = super.onCreateDialog(savedInstanceState);
        d.getWindow().requestFeature(Window.FEATURE_NO_TITLE);

        return d;
    }
}
