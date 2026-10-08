package com.gmi.prospect.ui;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.annotation.IdRes;
import androidx.annotation.StringRes;

import com.gmi.prospect.R;
import com.gmi.prospect.base.BaseActivity;
import com.google.android.material.snackbar.Snackbar;

/**
 * Contact GMI: four cards that hand over to another app with an IMPLICIT Intent
 * (phone dialer, email app, browser, maps).
 * <p>
 * LIFECYCLE: tapping a card puts this screen in the background - Logcat shows
 * onPause → onStop (no onDestroy, it stays in the back stack). Coming back with
 * Back shows onRestart → onStart → onResume, and the screen is exactly as it was.
 */
public class ContactActivity extends BaseActivity {

    /** Delay between each card's entrance animation (staggered effect). */
    private static final long STAGGER_MS = 70;

    // Data for the Intents (what the user SEES is in strings.xml)
    private static final String PHONE_URI = "tel:+60389219000";
    private static final String EMAIL_URI = "mailto:marketing@gmi.edu.my";
    private static final String WEBSITE_URI = "https://www.gmi.edu.my";
    private static final String MAP_URI = "geo:0,0?q=German-Malaysian+Institute+Kajang";
    /** Used when no maps app is installed: the same search in the browser. */
    private static final String MAP_WEB_URI =
            "https://www.google.com/maps/search/?api=1&query=German-Malaysian+Institute+Kajang";

    // Builds the four cards. Nothing to save in onSaveInstanceState: the cards
    // never change, so onCreate rebuilds them the same way after rotation.
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contact);
        setupToolbar(R.string.title_contact);

        setupCard(R.id.cardCall, R.drawable.ic_call,
                R.string.contact_call_title, R.string.contact_phone,
                v -> openFirstAvailable(new Intent(Intent.ACTION_DIAL, Uri.parse(PHONE_URI))));
        setupCard(R.id.cardEmail, R.drawable.ic_mail,
                R.string.contact_email_title, R.string.contact_email,
                v -> openFirstAvailable(buildEmailIntent()));
        setupCard(R.id.cardWebsite, R.drawable.ic_language,
                R.string.contact_website_title, R.string.contact_website,
                v -> openFirstAvailable(new Intent(Intent.ACTION_VIEW, Uri.parse(WEBSITE_URI))));
        setupCard(R.id.cardLocation, R.drawable.ic_location,
                R.string.contact_location_title, R.string.contact_address,
                v -> openFirstAvailable(
                        new Intent(Intent.ACTION_VIEW, Uri.parse(MAP_URI)),
                        new Intent(Intent.ACTION_VIEW, Uri.parse(MAP_WEB_URI))));

        if (savedInstanceState == null) {
            playEntranceAnimation(STAGGER_MS,
                    findViewById(R.id.cardCall),
                    findViewById(R.id.cardEmail),
                    findViewById(R.id.cardWebsite),
                    findViewById(R.id.cardLocation));
        }
    }

    /** Fills one included item_contact card and sets its tap action. */
    private void setupCard(@IdRes int cardId, @DrawableRes int iconRes,
                           @StringRes int titleRes, @StringRes int valueRes,
                           View.OnClickListener onClick) {
        View card = findViewById(cardId);
        ((ImageView) card.findViewById(R.id.imageContactIcon)).setImageResource(iconRes);
        ((TextView) card.findViewById(R.id.textContactTitle)).setText(titleRes);
        ((TextView) card.findViewById(R.id.textContactValue)).setText(valueRes);
        card.setOnClickListener(onClick);
    }

    /** ACTION_SENDTO + mailto: only email apps can answer it (not chat apps). */
    private Intent buildEmailIntent() {
        Intent intent = new Intent(Intent.ACTION_SENDTO, Uri.parse(EMAIL_URI));
        intent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.contact_email_subject));
        return intent;
    }

    /**
     * Starts the first Intent that some installed app can handle. If none can
     * (e.g. no email app on the emulator), shows a Snackbar instead of crashing.
     */
    private void openFirstAvailable(Intent... intents) {
        for (Intent intent : intents) {
            try {
                startActivity(intent);
                return;
            } catch (ActivityNotFoundException e) {
                // No app for this one - try the next
            }
        }
        Snackbar.make(findViewById(android.R.id.content), R.string.contact_no_app, Snackbar.LENGTH_LONG)
                .show();
    }
}
