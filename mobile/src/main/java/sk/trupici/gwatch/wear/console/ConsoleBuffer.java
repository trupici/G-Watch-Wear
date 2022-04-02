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

package sk.trupici.gwatch.wear.console;

import java.util.Collections;
import java.util.Date;
import java.util.LinkedList;

import sk.trupici.gwatch.wear.GWatchApplication;
import sk.trupici.gwatch.wear.R;
import sk.trupici.gwatch.wear.common.util.StringUtils;

public class ConsoleBuffer implements PacketConsole {

    private final static int MAX_CONSOLE_LINES = 300;

    final private Date creationDate;
    final private LinkedList<String> buffer;
    private boolean isWatchConnected;

    private PacketConsoleView view;

    ///////////////////////////////////////////////////////////////////////////
    // PacketView implementation

    public ConsoleBuffer(Date creationDate) {
        this.creationDate = creationDate;
        this.buffer = new LinkedList<>();
    }

    public void init() {
        buffer.clear();
        appendText(GWatchApplication.getAppContext().getString(R.string.created_at, StringUtils.formatDateTime(creationDate)));
        appendText(StringUtils.EMPTY_STRING);
        appendText(GWatchApplication.getAppContext().getString(R.string.waiting_for_packet));
    }

    @Override
    public synchronized void registerView(PacketConsoleView view) {
        this.view = view;
    }

    @Override
    public synchronized void unregisterView(PacketConsoleView view) {
        this.view = null;
    }

    @Override
    public synchronized void onWatchConnectionChanged(final boolean isConnected) {
        this.isWatchConnected = isConnected;
        if (view != null) {
            view.setConnectionStatus(isConnected);
        }
    }

    @Override
    public Boolean getIsWatchConnected() {
        return isWatchConnected;
    }

    @Override
    public synchronized String getText() {
        StringBuilder strBuffer = new StringBuilder();
        for (String s : buffer) {
            strBuffer.append(s).append("\n");
        }
        return strBuffer.toString();
    }

    @Override
    public void showText(final String text) {
        appendText(text);
    }

    private synchronized void appendText(String text) {
        if (text == null) {
            return;
        }

        // calculate the number of lines of text to add
        String[] textLines = text.split("\n");

        // recycle lines from the top of the content if necessary
        while (MAX_CONSOLE_LINES - buffer.size() < textLines.length + 1) {
            buffer.removeFirst();
        }

        Collections.addAll(buffer, textLines);

        if (view != null) {
            view.setText(getText());
        }
    }
    ///////////////////////////////////////////////////////////////////////////
}
