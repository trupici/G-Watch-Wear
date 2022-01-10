/*
 * Copyright (C) 2021 Juraj Antal
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package sk.trupici.gwatch.wear.config;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.view.MotionEvent;
import android.view.View;
import android.widget.EditText;

import com.larswerkman.holocolorpicker.ColorPicker;
import com.larswerkman.holocolorpicker.OpacityBar;
import com.larswerkman.holocolorpicker.SVBar;

import java.util.Locale;

import androidx.core.view.InputDeviceCompat;
import androidx.core.view.MotionEventCompat;
import sk.trupici.gwatch.wear.R;

/**
 * Picker {@code Activity} to select a color
 */
public class ColorPickerActivity extends Activity implements ColorPicker.OnColorChangedListener {

    public static final String EXTRA_ITEM_ID = "itemId";
    public static final String EXTRA_ITEM_TYPE = "itemType";
    public static final String EXTRA_COLOR = "color";

    private ColorPicker picker;
    private SVBar svBar;
    private OpacityBar opacityBar;
    private EditText text;
    private Integer itemId;
    private Integer itemType;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        int startColor = R.color.bar_pointer_default_color;

        Intent data = getIntent();
        if (data != null) {
            itemId = data.getExtras().getInt(EXTRA_ITEM_ID);
            itemType = data.getExtras().getInt(EXTRA_ITEM_TYPE);
            startColor = data.getIntExtra(EXTRA_COLOR, startColor);
        }


        setContentView(R.layout.layout_color_picker);

        text = findViewById(R.id.text);

        picker = findViewById(R.id.picker);
        opacityBar = findViewById(R.id.opacitybar);
        svBar = findViewById(R.id.svbar);

        picker.addOpacityBar(opacityBar);
        picker.addSVBar(svBar);

        picker.setTouchAnywhereOnColorWheelEnabled(false);
        picker.setOnColorChangedListener(this);
        picker.setShowOldCenterColor(true);

        picker.setOldCenterColor(startColor);
        picker.setColor(startColor);

        picker.setOnGenericMotionListener(new View.OnGenericMotionListener() {
            @Override
            public boolean onGenericMotion(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_SCROLL && event.isFromSource(InputDeviceCompat.SOURCE_ROTARY_ENCODER)) {
                    float delta = -event.getAxisValue(MotionEventCompat.AXIS_SCROLL) * 6;
                    picker.turnColorWheel(delta);
                    return true;
                }
                return false;
            }
        });

        picker.setOnClickCenterListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                closeAndReturnResult();
            }
        });

        onColorChanged(picker.getColor());
        picker.requestFocus();

        text.setFilters(new InputFilter[]{new InputFilter.LengthFilter(8)});
        text.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                try {
                    int color = Integer.valueOf(s.toString().toLowerCase(Locale.ROOT), 16);
                    picker.setOnColorChangedListener(null);
                    picker.setColor(color);
                    picker.setOnColorChangedListener(ColorPickerActivity.this);
                } catch (Exception e) {
                    // swallow any exception
                }
            }
        });
    }

    @Override
    public void onColorChanged(int color) {
        text.setText(String.format("%1$08x", picker.getColor()));
        picker.requestFocus();
    }

    @Override
    public boolean onGenericMotionEvent(MotionEvent event) {
        picker.requestFocus();
        return super.onGenericMotionEvent(event);
    }

    private void closeAndReturnResult() {
        Intent data = new Intent();
        data.putExtra(EXTRA_COLOR, picker.getColor());
        data.putExtra(EXTRA_ITEM_ID, itemId);
        data.putExtra(EXTRA_ITEM_TYPE, itemType);
        setResult(RESULT_OK, data);
        finish();
    }
}
