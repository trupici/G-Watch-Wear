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
package sk.trupici.gwatch.wear.components;

import sk.trupici.gwatch.wear.data.GlucosePacket;

public class BgPanel {

    // TODO more sets are available in standard unicode font
    public static final char[] TREND_SET_1 = {' ', '⇈', '↑', '↗', '→', '↘', '↓', '⇊'}; // standard arrows
    public static final char[] TREND_SET_2 = {' ', '⮅', '⭡', '⭧', '⭢', '⭨', '⭣', '⮇'}; // triangle arrows (unknown chars on watch)

    public static GlucosePacket.Trend calcTrend(int glucoseDelta, int sampleTimeDelta) {
        if (glucoseDelta < -2 * sampleTimeDelta) {
            return GlucosePacket.Trend.DOWN;
        } else if (glucoseDelta < -sampleTimeDelta) {
            return GlucosePacket.Trend.DOWN_SLOW;
        } else if (glucoseDelta < sampleTimeDelta) {
            return GlucosePacket.Trend.FLAT;
        } else if (glucoseDelta < 2 * sampleTimeDelta) {
            return GlucosePacket.Trend.UP_SLOW;
        } else {
            return GlucosePacket.Trend.UP;
        }
    }

}
