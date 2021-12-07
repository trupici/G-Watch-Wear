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

package sk.trupici.gwatch.wear.data;

import java.util.ArrayList;
import java.util.List;

public class Queue<T> {
    final private List<T> buffer;
    final private int limit;

    public Queue(int limit) {
        this.limit = limit;
        buffer = new ArrayList<>(limit);
    }

    public synchronized T peek() {
        if (buffer.isEmpty()) {
            return null;
        }
        return buffer.get(0);
    }

    public synchronized T poll() {
        if (buffer.isEmpty()) {
            return null;
        }
        return buffer.remove(0);
    }

    public synchronized void push(T data) {
        if (data != null) {
            if (buffer.size() >= limit) {
                buffer.remove(0);
            }
            buffer.add(data);
        }
    }

    public synchronized int size() {
        return buffer.size();
    }

    public synchronized boolean isEmpty() {
        return buffer.isEmpty();
    }
}
