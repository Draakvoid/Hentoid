package me.devsaki.hentoid.activities;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.Switch;

import me.devsaki.hentoid.R;
import me.devsaki.hentoid.fragments.NumpadFragment;
import me.devsaki.hentoid.util.Preferences;

// TODO: place pin registration in a descendant screen
// TODO: refer to https://material.io/design/platform-guidance/android-settings.html#label-secondary-text for illustration
// TODO: when app lock is "ON", show a "change pin" affordance
// TODO: pin registration screen appears when app lock is turned on or when "change pin" is selected
// TODO: implement viewmodel for this
// TODO: use fragment for pinLayout
public class RegisterPinActivity extends AppCompatActivity implements NumpadFragment.Parent {

    private final StringBuilder pinValue = new StringBuilder(4);

    private View offGroup;

    private View resetPinText;

    private View pinLayout;

    private View placeholderImage1;

    private View placeholderImage2;

    private View placeholderImage3;

    private View placeholderImage4;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register_pin);

        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        Switch lockSwitch = findViewById(R.id.switch_lock);
        lockSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> onLockSwitchToggled(isChecked));

        offGroup = findViewById(R.id.group_off);

        resetPinText = findViewById(R.id.text_reset_pin);
        resetPinText.setOnClickListener(v -> onResetPinClick());

        pinLayout = findViewById(R.id.layout_pin);

        placeholderImage1 = findViewById(R.id.image_placeholder_1);

        placeholderImage2 = findViewById(R.id.image_placeholder_2);

        placeholderImage3 = findViewById(R.id.image_placeholder_3);

        placeholderImage4 = findViewById(R.id.image_placeholder_4);

        lockSwitch.setChecked(!Preferences.getAppLockPin().isEmpty());
    }

    @Override
    protected void onStop() {
        super.onStop();
        clearPin();
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
            resetPinText.setVisibility(View.VISIBLE);
            offGroup.setVisibility(View.GONE);
        } else {
            resetPinText.setVisibility(View.GONE);
            offGroup.setVisibility(View.VISIBLE);
            clearPin();
            Preferences.setAppLockPin("");
        }
    }

    private void onResetPinClick() {
        pinLayout.setVisibility(View.VISIBLE);
        resetPinText.setVisibility(View.GONE);
    }

    private void clearPin() {
        pinValue.setLength(0);
        placeholderImage1.setVisibility(View.INVISIBLE);
        placeholderImage2.setVisibility(View.INVISIBLE);
        placeholderImage3.setVisibility(View.INVISIBLE);
        placeholderImage4.setVisibility(View.INVISIBLE);
    }
}
