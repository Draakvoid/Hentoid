package me.devsaki.hentoid.activities;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Parcelable;
import android.os.Vibrator;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import me.devsaki.hentoid.R;
import me.devsaki.hentoid.abstracts.BaseActivity;
import me.devsaki.hentoid.fragments.NumpadFragment;
import me.devsaki.hentoid.util.Preferences;

/**
 * This activity asks for a 4 digit pin if it is set and transitions to another activity
 *
 * @see #wrapIntent(Context, Intent)
 */
public class UnlockActivity extends BaseActivity implements NumpadFragment.Parent {

    private static final String EXTRA_DEFAULT = "default";

    private static boolean isUnlocked = false;

    public static Intent wrapIntent(Context context, Intent destinationIntent) {
        Intent intent = new Intent(context, UnlockActivity.class);
        intent.putExtra(EXTRA_DEFAULT, destinationIntent);
        return intent;
    }

    private final StringBuilder pinValue = new StringBuilder(4);

    private TextView spielText;

    private View placeholderImage1;

    private View placeholderImage2;

    private View placeholderImage3;

    private View placeholderImage4;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (Preferences.getAppLockPin().isEmpty() || isUnlocked) {
            goToNextActivity();
            return;
        }

        setContentView(R.layout.activity_unlock);

        spielText = findViewById(R.id.textSpiel);

        placeholderImage1 = findViewById(R.id.image_placeholder_1);

        placeholderImage2 = findViewById(R.id.image_placeholder_2);

        placeholderImage3 = findViewById(R.id.image_placeholder_3);

        placeholderImage4 = findViewById(R.id.image_placeholder_4);
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

        if (pinValue.length() == 4) checkPin();
        else spielText.setText(R.string.app_lock_pin);
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

        spielText.setText(R.string.app_lock_pin);
    }

    private void checkPin() {
        invokeVibrate();

        if (Preferences.getAppLockPin().equals(pinValue.toString())) {
            isUnlocked = true;

            spielText.setText(R.string.pin_ok);

            ImageView lockImage1 = findViewById(R.id.image_lock_1);
            lockImage1.setImageResource(R.drawable.ic_lock_open);

            ImageView lockImage2 = findViewById(R.id.image_lock_2);
            lockImage2.setImageResource(R.drawable.ic_lock_open);

            goToNextActivity();
        } else {
            spielText.setText(R.string.pin_invalid);
            pinValue.setLength(0);
            placeholderImage1.setVisibility(View.INVISIBLE);
            placeholderImage2.setVisibility(View.INVISIBLE);
            placeholderImage3.setVisibility(View.INVISIBLE);
            placeholderImage4.setVisibility(View.INVISIBLE);
        }
    }

    private void invokeVibrate() {
        Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        if (vibrator != null) {
            vibrator.vibrate(100);
        }
    }

    private void goToNextActivity() {
        Parcelable parcelableExtra = getIntent().getParcelableExtra(EXTRA_DEFAULT);
        startActivity((Intent) parcelableExtra);
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
        finish();
    }
}
