package com.lumiandrey.mediabuttonrecieverservicebinder;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.widget.TextView;

import com.lumiandrey.mediabuttonrecieverservicebinder.fragment.BlankConnectionFragment;
import com.lumiandrey.mediabuttonrecieverservicebinder.fragment.BlankNoConnectionFragment;

public class MainActivity extends AppCompatActivity {


    private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener
            = new BottomNavigationView.OnNavigationItemSelectedListener() {

        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            switch (item.getItemId()) {
                case R.id.navigation_home:

                    getSupportFragmentManager()
                            .beginTransaction()
                            .replace(R.id.frame_fragment_container, BlankNoConnectionFragment.newInstance("HOME ", "No connection"))
                            .commit();

                    return true;
                case R.id.navigation_dashboard:


                    getSupportFragmentManager()
                            .beginTransaction()
                            .replace(R.id.frame_fragment_container, BlankConnectionFragment.newInstance("Navigation ", "Connection"))
                            .commit();

                    return true;
                case R.id.navigation_notifications:

                    getSupportFragmentManager()
                            .beginTransaction()
                            .replace(R.id.frame_fragment_container, BlankNoConnectionFragment.newInstance("Notification ", "No connection"))
                            .commit();

                    return true;
            }

            return false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        BottomNavigationView navigation = findViewById(R.id.navigation);
        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);
    }

}
