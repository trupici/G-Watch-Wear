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

package sk.trupici.gwatch.wear.util;

public class BgUtils {

    public static final Double GLUCOSE_CONV_FACTOR = 18.018018;
    public static final String GLUCOSE_UNITS_MGDL = "mg/dl";
    public static final String GLUCOSE_UNITS_MMOLL = "mmol/l";

    public static final int MMOL_L_BOUNDARY_VALUE = 36;

    public static Double convertGlucoseToMmolL(double glucoseValue) {
        return Math.round(glucoseValue / GLUCOSE_CONV_FACTOR * 10d) / 10d;
    }

    public static Double convertGlucoseToMmolL2(double glucoseValue) {
        return Math.round(glucoseValue / GLUCOSE_CONV_FACTOR * 100d) / 100d;
    }

    public static Double convertGlucoseToMgDl(double glucoseValue) {
        return (double) Math.round(glucoseValue * GLUCOSE_CONV_FACTOR);
    }

}
