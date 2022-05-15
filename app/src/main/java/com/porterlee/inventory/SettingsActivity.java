package com.porterlee.inventory;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;

public class SettingsActivity extends Activity {

    private Switch duplicateSwitch;
    private Switch externalSwitch;
    private Switch saveEmptySwitch;

    private DataManager dataManager;
    private SharedPreferences preferences;

    private TextView versionText;

    private static final String TAG = MainActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        //dataManager = getParent().getLastNonConfigurationInstance().getDataManager();

        initialize();
        setSwitchValues();
    }

    private void initialize() {
        versionText = findViewById(R.id.versionView);
        String version = getResources().getString(R.string.app_header);
        versionText.setText(version);
        duplicateSwitch = (Switch)  findViewById(R.id.switch1);
        duplicateSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                changePreference(isChecked, 1);
            }
        });

        externalSwitch = (Switch)  findViewById(R.id.switch2);
        externalSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                changePreference(isChecked, 2);
            }
        });

        saveEmptySwitch = (Switch)  findViewById(R.id.switch3);
        saveEmptySwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                changePreference(isChecked, 3);
            }
        });
    }

    private void setSwitchValues() {
        preferences = getApplicationContext().getSharedPreferences("Inventory_preference", Context.MODE_PRIVATE);
        String one = preferences.getString("allowDuplicates", "");
        String two = preferences.getString("allowExternal", "");
        String three = preferences.getString("allowEmptyLocation", "");
        Log.v(TAG, ":"+one+two+":");

        if (BuildConfig.FLAVOR.equals("LAM")) {
            setState(duplicateSwitch, "false");
            changePreference(false, 1);
            duplicateSwitch.setClickable(false);
        }
        else{
            setState(duplicateSwitch, one);
        }
        setState(externalSwitch, two);
        setState(saveEmptySwitch, three);
    }

    private void setState(final Switch mSwitch, final String value) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (value.equals("true")) mSwitch.setChecked(true);
                else if(value.equals("false")) mSwitch.setChecked(false);
            }
        });
    }

    private void changePreference(boolean isChecked, int switchNumber) {
        SharedPreferences.Editor editor = preferences.edit();
        Log.v(TAG, "pref");
        if (switchNumber == 1) {
            asyncRun(editor, "allowDuplicates", isChecked);
            //editor.putString("allowDuplicates", String.valueOf(isChecked));
            Log.v(TAG, "pref1");
        }
        else if (switchNumber == 2) {
            asyncRun(editor, "allowExternal", isChecked);
            //editor.putString("allowExternal", String.valueOf(isChecked));
            Log.v(TAG, "pref2");
        }
        else if (switchNumber == 3) {
            asyncRun(editor, "allowEmptyLocation", isChecked);
            //editor.putString("allowEmptyLocation", String.valueOf(isChecked));
        }
        editor.commit();
    }
    private void asyncRun(final SharedPreferences.Editor editor, final String value, final Boolean isChecked) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                editor.putString(value, String.valueOf(isChecked));
            }
        });
    }


}