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

public class HandsConfigData extends ConfigItemData {
    final private int hourHandId;
    final private int hourHandShadowId;
    final private int minuteHandId;
    final private int minuteHandShadowId;
    final private int secondHandId;
    final private int secondHandShadowId;

    public HandsConfigData(int id, String label, int resourceId,
                           int hourHandId, int hourHandShadowId,
                           int minuteHandId, int minuteHandShadowId,
                           int secondHandId, int secondHandShadowId) {
        super(id, label, resourceId);
        this.hourHandId = hourHandId;
        this.hourHandShadowId = hourHandShadowId;
        this.minuteHandId = minuteHandId;
        this.minuteHandShadowId = minuteHandShadowId;
        this.secondHandId = secondHandId;
        this.secondHandShadowId = secondHandShadowId;
    }

    public int getHourHandId() {
        return hourHandId;
    }

    public int getHourHandShadowId() {
        return hourHandShadowId;
    }

    public int getMinuteHandId() {
        return minuteHandId;
    }

    public int getMinuteHandShadowId() {
        return minuteHandShadowId;
    }

    public int getSecondHandId() {
        return secondHandId;
    }

    public int getSecondHandShadowId() {
        return secondHandShadowId;
    }
}
