/* Copyright 2020 Braden Farmer
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.farmerbb.taskbar.util;

import android.content.Context;

import static com.farmerbb.taskbar.util.Constants.*;

public class TaskbarPosition {

    private TaskbarPosition() {}

    public static boolean isVertical(String position) {
        return false;
    }

    public static boolean isVertical(Context context) {
        return false;
    }

    public static boolean isLeft(String position) {
        return true;
    }

    public static boolean isLeft(Context context) {
        return true;
    }

    public static boolean isRight(String position) {
        return false;
    }

    public static boolean isRight(Context context) {
        return false;
    }

    public static boolean isBottom(String position) {
        return true;
    }

    public static boolean isBottom(Context context) {
        return true;
    }

    public static boolean isVerticalLeft(String position) {
        return false;
    }

    public static boolean isVerticalLeft(Context context) {
        return false;
    }

    public static boolean isVerticalRight(String position) {
        return false;
    }

    public static boolean isVerticalRight(Context context) {
        return false;
    }

    public static String getTaskbarPosition(Context context) {
        return POSITION_BOTTOM_LEFT;
    }
}
