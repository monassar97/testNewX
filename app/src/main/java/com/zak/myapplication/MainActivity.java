package com.zak.myapplication;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.zak.soundlibrary.Callback;
import com.zak.soundlibrary.Delegator;

public class MainActivity extends AppCompatActivity implements Callback, View.OnClickListener {
    Delegator delegator;
    Button speakBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setXmlReferences();
        setListener();
    }

    private void setListener() {
        speakBtn.setOnClickListener(this);
    }

    private void setXmlReferences() {
        speakBtn = findViewById(R.id.speakBtn);
    }

    @Override
    public void onDone(String text) {
        Toast.makeText(this, text, Toast.LENGTH_LONG).show();
        if (text.equalsIgnoreCase("منتجات")) {
            Intent intent = new Intent(this, Main2Activity.class);
            startActivity(intent);
        }
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {

            case R.id.speakBtn:
                delegator = new Delegator(this, "ar", this);
                delegator.startListen();
                break;

        }
    }
}
