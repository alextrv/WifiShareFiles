package org.trv.alex.wifisharefiles;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;

public class ShareActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_share);

        FragmentManager fragmentManager = getSupportFragmentManager();
        Fragment fragment = fragmentManager.findFragmentById(R.id.share_fragment_container);

        if (fragment == null) {
            fragmentManager.beginTransaction()
                    .add(R.id.share_fragment_container, new PeersListFragment())
                    .commit();
        }

    }

}
