package com.carapp;

import android.os.Handler;
import android.view.View;
import androidx.annotation.*;
import androidx.appcompat.app.AppCompatActivity;

public class ComActivity extends AppCompatActivity {

    public <T extends View> T findViewById(@IdRes int id) {
        return (T) super.findViewById(id);
    }

    public void postDelayed(Runnable r, int delayMillis) {
        new Handler().postDelayed( r, delayMillis);
    }

}
