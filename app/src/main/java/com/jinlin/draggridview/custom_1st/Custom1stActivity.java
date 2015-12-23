package com.jinlin.draggridview.custom_1st;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.jinlin.draggridview.R;

import java.util.ArrayList;
import java.util.List;


public class Custom1stActivity extends AppCompatActivity {
    private DragGrid dragGrid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_1st);
        dragGrid = (DragGrid) findViewById(R.id.userGridView);

        List<String> list = new ArrayList<>();
        for (int i = 0; i < 25; i++) {
            list.add("测试 - " + i);
        }
        dragGrid.setAdapter(new DragAdapter(this, list));
    }


}
