package me.devsaki.hentoid.activities;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Switch;

import me.devsaki.hentoid.R;
import me.devsaki.hentoid.fragments.NumpadFragment;
import me.devsaki.hentoid.util.Preferences;

public class RegisterPinActivity extends AppCompatActivity implements NumpadFragment.Parent {

    private final StringBuilder pinValue = new StringBuilder(4);

    private View pinLayout;

    private View placeholderImage1;

    private View placeholderImage2;

    private View placeholderImage3;

    private View placeholderImage4;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register_pin);

        Switch lockSwitch = findViewById(R.id.switch_lock);
        lockSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> onLockSwitchToggled(isChecked));

        pinLayout = findViewById(R.id.layout_pin);

        placeholderImage1 = findViewById(R.id.image_placeholder_1);

        placeholderImage2 = findViewById(R.id.image_placeholder_2);

        placeholderImage3 = findViewById(R.id.image_placeholder_3);

        placeholderImage4 = findViewById(R.id.image_placeholder_4);

        lockSwitch.setChecked(!Preferences.getAppLockPin().isEmpty());
    }

    @Override
    public void onKeyClick(String s) {
        if (pinValue.length() == 4) return;

        pinValue.append(s);
        switch (pinValue.length()) {
            case 4: placeholderImage4.setVisibility(View.VISIBLE);
                break;
            case 3: placeholderImage3.setVisibility(View.VISIBLE);
                break;
            case 2: placeholderImage2.setVisibility(View.VISIBLE);
                break;
            case 1: placeholderImage1.setVisibility(View.VISIBLE);
                break;
        }

        if (pinValue.length() == 4) {
            pinLayout.setVisibility(View.GONE);
            Preferences.setAppLockPin(pinValue.toString());
            finish();
        }
    }

    @Override
    public void onBackspaceClick() {
        if (pinValue.length() == 0) return;

        pinValue.setLength(pinValue.length() - 1);
        switch (pinValue.length()) {
            case 0: placeholderImage1.setVisibility(View.INVISIBLE);
                break;
            case 1: placeholderImage2.setVisibility(View.INVISIBLE);
                break;
            case 2: placeholderImage3.setVisibility(View.INVISIBLE);
                break;
            case 3: placeholderImage4.setVisibility(View.INVISIBLE);
                break;
        }
    }

    private void onLockSwitchToggled(boolean isOn) {
        if (isOn) {
            pinLayout.setVisibility(View.VISIBLE);
        } else {
            pinLayout.setVisibility(View.GONE);
            pinValue.setLength(0);
            placeholderImage1.setVisibility(View.INVISIBLE);
            placeholderImage2.setVisibility(View.INVISIBLE);
            placeholderImage3.setVisibility(View.INVISIBLE);
            placeholderImage4.setVisibility(View.INVISIBLE);
            Preferences.setAppLockPin("");
        }
    }
}
