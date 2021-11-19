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

package sk.trupici.gwatch.wear.config.complications;

/**
 * Enumeration of unique IDs for each complication.
 * The settings activity that supports allowing users to select their complication data provider
 * requires numbers to be >= 0.
 */

public enum ComplicationId {
    LEFT_COMPLICATION_ID,
    RIGHT_COMPLICATION_ID,
    CENTER_COMPLICATION_ID, // not really central, a bit below the center
    BOTTOM_COMPLICATION_ID, // GLUCOSE ?
    TOP_COMPLICATION_ID, // GRAPH ?
    BACKGROUND_COMPLICATION_ID,
    CALENDAR_COMPLICATION_ID
}
