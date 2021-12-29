/*
 * Copyright (C) 2019 Juraj Antal
 *
 * Originally created in G-Watch App
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

package sk.trupici.gwatch.wear.view;

import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.GestureDetector;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ScrollView;
import android.widget.TextView;

import java.lang.reflect.Method;

import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import sk.trupici.gwatch.wear.GWatchApplication;
import sk.trupici.gwatch.wear.R;
import sk.trupici.gwatch.wear.console.PacketConsole;
import sk.trupici.gwatch.wear.console.PacketConsoleView;

public class PacketViewActivity extends LocalizedActivityBase implements PacketConsoleView, HorizontalSwipeDetector.SwipeListener {

    protected Menu menu;

    private TextView packetView;
    protected GestureDetector gestureDetector;

    private PacketConsole consoleBuffer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.packet_console_layout);

        setupToolBar();

        this.consoleBuffer = GWatchApplication.getPacketConsole();

        gestureDetector = new GestureDetector(this, new HorizontalSwipeDetector(this));
        View.OnTouchListener gestureListener = (v, event) -> gestureDetector.onTouchEvent(event);

        findViewById(R.id.text_scroll).setOnTouchListener(gestureListener);
    }

    ///////////////////////////////////////////////////////////////////////////
    // SwipeListener implementation

    public boolean onLeftSwipe() {
        finish();
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_left);
        return true;
    }

    public boolean onRightSwipe() {
        // the last activity in (swipe) chain
        return false;
    }
    ///////////////////////////////////////////////////////////////////////////

    @Override
    protected void onResume() {
        super.onResume();
        restorePacketView();
        consoleBuffer.registerView(this);
   }

    @Override
    protected void onPause() {
        super.onPause();
    }

    private void setupToolBar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> onBackPressed());
        setSupportActionBar(toolbar);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(0, 0);
    }

    @Override
    public boolean onSupportNavigateUp() {
        super.onSupportNavigateUp();
        overridePendingTransition(0, 0);
        return false;
    }

    // Menu icons are inflated just as they were with actionbar
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        this.menu = menu;

        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_packet_view, menu);

//        setConnectionStatus(dispatcher.isConnected());
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        if (menu != null) {
            if (menu.getClass().getSimpleName().equals("MenuBuilder")) {
                try {
                    Method m = menu.getClass().getDeclaredMethod("setOptionalIconsVisible", Boolean.TYPE);
                    m.setAccessible(true);
                    m.invoke(menu, true);
                } catch (Exception e) {
                    Log.e(getClass().getSimpleName(), "onMenuOpened...unable to set icons for overflow menu", e);
                }
            }
        }
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_clear) {
            consoleBuffer.init();
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        consoleBuffer.unregisterView(this);
        super.onDestroy();
    }

    private void restorePacketView() {
        packetView = findViewById(R.id.packetView);

        setConnectionStatus(consoleBuffer.getIsWatchConnected());
        packetView.setText(GWatchApplication.getPacketConsole().getText());
        scrollToBottom();
    }

    private void scrollToBottom() {
        // scroll to the end (newest text)
        packetView.post(() -> ((ScrollView)findViewById(R.id.text_scroll)).fullScroll(View.FOCUS_DOWN));
    }

    ///////////////////////////////////////////////////////////////////////////
    // PacketConsoleView implementation

    @Override
    public void setConnectionStatus(final Boolean isConnected) {
//        if (menu == null) {
//            return;
//        }
//        int id;
//        if (isConnected == null) {
//            id = android.R.color.darker_gray;
//        } else {
//            id = isConnected ? R.color.status_indicator_connected : R.color.status_indicator_disconnected;
//        }
//
//        final int colorId = id;
//        Toolbar toolbar = findViewById(R.id.toolbar);
//        toolbar.postDelayed(() -> {
//            MenuItem item = menu.findItem(R.id.action_status);
//            if (item != null) {
//                Drawable icon = item.getIcon().mutate();
//                icon.setColorFilter(new PorterDuffColorFilter(ContextCompat.getColor(PacketViewActivity.this, colorId), PorterDuff.Mode.SRC_ATOP));
//                item.setIcon(icon);
//            }
//        }, 10);
    }

    @Override
    public void setText(final String text) {
        runOnUiThread(() -> {
            packetView.setText(text);
            scrollToBottom();
        });
    }
    ///////////////////////////////////////////////////////////////////////////
}
