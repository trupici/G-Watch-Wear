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

package sk.trupici.gwatch.wear.dispatch;

import sk.trupici.gwatch.wear.data.Packet;

public interface Dispatcher {

    /** send given packet if possible, enqueue packet if not connected */
    boolean dispatch(Packet packet);

    /** send given packet if possible, regardless of waiting packets */
    boolean dispatchNow(Packet packet);

    /** send given packet if possible. If failed, enqueue packet and force reconnect */
    boolean sync(Packet packet);

    /** resend last glucose packet if available */
    boolean repeatLastGlucosePacket();

    boolean isConnected();
    void connectionChangedCallback(boolean isConnected);
}
