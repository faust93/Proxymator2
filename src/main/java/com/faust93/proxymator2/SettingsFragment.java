package com.faust93.proxymator2;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;

/**
 * Created by faust93 on 10.10.13.
 */

public class SettingsFragment extends Fragment implements Constants, CompoundButton.OnCheckedChangeListener {

    private CheckBox globalDisable;
    private CheckBox enableLogging;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View myView = inflater.inflate(R.layout.settings_fragment, container, false);
        globalDisable = (CheckBox) myView.findViewById(R.id.global_disable);
        enableLogging = (CheckBox) myView.findViewById(R.id.enable_logging);

        globalDisable.setChecked(MainFragment.getPreferences().getBoolean("globalDisable",false));
        enableLogging.setChecked(MainFragment.getPreferences().getBoolean("enableLogging", false)); 
        globalDisable.setOnCheckedChangeListener(this);
        enableLogging.setOnCheckedChangeListener(this);

        return myView;
    }

    @Override
    public void onCheckedChanged(CompoundButton cbBox, boolean isChecked){

        SharedPreferences.Editor editor = MainFragment.getPreferences().edit();

        switch (cbBox.getId()) {
            case R.id.global_disable:
                 editor.putBoolean("globalDisable", isChecked);
                 editor.commit();
                break;
            case R.id.enable_logging:
                MainFragment.setLogging(isChecked);
                editor.putBoolean("enableLogging", isChecked);
                editor.commit();
                break;
        }
    }


}
