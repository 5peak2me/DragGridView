package com.jinlin.draggridview;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

import com.jinlin.draggridview.custom_1st.Custom1stActivity;
import com.jinlin.draggridview.custom_2nd.Custom2ndActivity;
import com.jinlin.draggridview.custom_3rd.Custom3rdActivity;
import com.jinlin.draggridview.custom_4th.Custom4thActivity;
import com.jinlin.draggridview.custom_5th.Custom5thActivity;
import com.jinlin.draggridview.custom_6th.Custom6thActivity;

public class MainActivity extends AppCompatActivity implements OnClickListener {
    private Button btn1;
    private Button btn2;
    private Button btn3;
    private Button btn4;
    private Button btn5;
    private Button btn6;

    private void assignViews() {
        btn1 = (Button) findViewById(R.id.btn1);
        btn1.setOnClickListener(this);

        btn2 = (Button) findViewById(R.id.btn2);
        btn2.setOnClickListener(this);

        btn3 = (Button) findViewById(R.id.btn3);
        btn3.setOnClickListener(this);

        btn4 = (Button) findViewById(R.id.btn4);
        btn4.setOnClickListener(this);

        btn5 = (Button) findViewById(R.id.btn5);
        btn5.setOnClickListener(this);

        btn6 = (Button) findViewById(R.id.btn6);
        btn6.setOnClickListener(this);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        assignViews();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn1:
                startActivity(new Intent(this, Custom1stActivity.class));
                break;
            case R.id.btn2:
                startActivity(new Intent(this, Custom2ndActivity.class));
                break;
            case R.id.btn3:
                startActivity(new Intent(this, Custom3rdActivity.class));
                break;
            case R.id.btn4:
                startActivity(new Intent(this, Custom4thActivity.class));
                break;
            case R.id.btn5:
                startActivity(new Intent(this, Custom5thActivity.class));
                break;
            case R.id.btn6:
                startActivity(new Intent(this, Custom6thActivity.class));
                break;
        }
    }
}
