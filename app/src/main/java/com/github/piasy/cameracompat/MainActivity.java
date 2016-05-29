package com.github.piasy.cameracompat;

import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import com.github.piasy.cameracompat.v16.PreviewFragmentV16;
import com.github.piasy.cameracompat.v21.PreviewFragmentV21;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        findViewById(R.id.mBtnStart).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    transaction.add(android.R.id.content, new PreviewFragmentV21());
                } else {
                    transaction.add(android.R.id.content, new PreviewFragmentV16());
                }
                transaction.commit();
            }
        });
    }
}
