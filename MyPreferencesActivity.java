package com.examples.user.or_gopro;

import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.util.Log;

/**
 * Created by User on 3/14/2016.
 */

public class MyPreferencesActivity extends PreferenceActivity{
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preference);

    }
}
