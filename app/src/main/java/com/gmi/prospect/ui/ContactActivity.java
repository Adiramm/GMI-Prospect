package com.gmi.prospect.ui;

import android.os.Bundle;

import com.gmi.prospect.R;
import com.gmi.prospect.base.BaseActivity;

/**
 * Contact GMI (call, email, website, map).
 * <p>
 * PLACEHOLDER for now: shows only a toolbar with a back arrow. The real
 * screen (with its own activity_*.xml layout) is built in a later phase.
 */
public class ContactActivity extends BaseActivity {

    // Inflates the shared placeholder layout and sets the toolbar title.
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_placeholder);
        setupToolbar(R.string.title_contact);
    }
}
