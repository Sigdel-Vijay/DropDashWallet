package com.devroid.dropdashwallet;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomappbar.BottomAppBar;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

public class MainActivity extends BaseActivity {
    FrameLayout container;
    ConstraintLayout bottomNavigationBar;
    BottomNavigationView bottomNavigationView;
    BottomAppBar bottomAppBar;
    FloatingActionButton fab;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        container = findViewById(R.id.container);


        bottomNavigationBar = findViewById(R.id.bottomNavigationBar);

        bottomNavigationView = findViewById(R.id.bottomNavigationView);
        bottomAppBar = findViewById(R.id.bottomAppBar);
        fab = findViewById(R.id.fab);


        fab.setOnClickListener(v -> {
            loadFragmentIfNeeded(new QrFragment(), QrFragment.class);
        });


        openHomeTab();


        bottomNavigationView.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {

                int id = item.getItemId();

                if (id==R.id.nav_home) {
                    openHomeTab();
                } else if (id==R.id.nav_statement) {
                    openStatementTab();
                } else if (id==R.id.nav_my_payment) {
                    openMyPaymentTab();
                } else if (id==R.id.nav_support){
                    openSupportTab();
                }


                return true;
            }
        });


    }

    public void openHomeTab() {
        loadFragmentIfNeeded(new HomeFragment(), HomeFragment.class);
    }

    public void openStatementTab() {
        loadFragmentIfNeeded(new StatementFragment(), StatementFragment.class);
    }

    public void openMyPaymentTab() {
        loadFragmentIfNeeded(new MyPaymentFragment(), MyPaymentFragment.class);
    }

    public void openSupportTab() {
        loadFragmentIfNeeded(new SupportFragment(), SupportFragment.class);
    }

    private void loadFragmentIfNeeded(Fragment fragment, Class<?> fragmentClass) {
        Fragment current = getSupportFragmentManager()
                .findFragmentById(R.id.container);

        if (current == null || !fragmentClass.isInstance(current)) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.container, fragment)
                    .commit();
        }
    }
}