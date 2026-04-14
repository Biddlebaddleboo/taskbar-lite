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

public class Constants {

    private Constants() {}

    // Intent actions

    public static final String ACTION_CONTEXT_MENU_APPEARING = "com.farmerbb.taskbar.CONTEXT_MENU_APPEARING";
    public static final String ACTION_CONTEXT_MENU_DISAPPEARING = "com.farmerbb.taskbar.CONTEXT_MENU_DISAPPEARING";
    public static final String ACTION_ENTER_ICON_ARRANGE_MODE = "com.farmerbb.taskbar.ENTER_ICON_ARRANGE_MODE";
    public static final String ACTION_FINISH_DIM_SCREEN_ACTIVITY = "com.farmerbb.taskbar.FINISH_DIM_SCREEN_ACTIVITY";
    public static final String ACTION_FORCE_TASKBAR_RESTART = "com.farmerbb.taskbar.FORCE_TASKBAR_RESTART";
    public static final String ACTION_HIDE_CONTEXT_MENU = "com.farmerbb.taskbar.HIDE_CONTEXT_MENU";
    public static final String ACTION_HIDE_START_MENU = "com.farmerbb.taskbar.HIDE_START_MENU";
    public static final String ACTION_HIDE_START_MENU_NO_RESET = "com.farmerbb.taskbar.HIDE_START_MENU_NO_RESET";
    public static final String ACTION_HIDE_START_MENU_SPACE = "com.farmerbb.taskbar.HIDE_START_MENU_SPACE";
    public static final String ACTION_KILL_HOME_ACTIVITY = "com.farmerbb.taskbar.KILL_HOME_ACTIVITY";
    public static final String ACTION_REFRESH_DESKTOP_ICONS = "com.farmerbb.taskbar.REFRESH_DESKTOP_ICONS";
    public static final String ACTION_RESET_START_MENU = "com.farmerbb.taskbar.RESET_START_MENU";
    public static final String ACTION_RESTART = "com.farmerbb.taskbar.RESTART";
    public static final String ACTION_SHOW_START_MENU_SPACE = "com.farmerbb.taskbar.SHOW_START_MENU_SPACE";
    public static final String ACTION_SHOW_TASKBAR = "com.farmerbb.taskbar.SHOW_TASKBAR";
    public static final String ACTION_SORT_DESKTOP_ICONS = "com.farmerbb.taskbar.SORT_DESKTOP_ICONS";
    public static final String ACTION_START = "com.farmerbb.taskbar.START";
    public static final String ACTION_START_MENU_APPEARING = "com.farmerbb.taskbar.START_MENU_APPEARING";
    public static final String ACTION_START_MENU_DISAPPEARING = "com.farmerbb.taskbar.START_MENU_DISAPPEARING";
    public static final String ACTION_TEMP_SHOW_TASKBAR = "com.farmerbb.taskbar.TEMP_SHOW_TASKBAR";
    public static final String ACTION_TOGGLE_START_MENU = "com.farmerbb.taskbar.TOGGLE_START_MENU";
    public static final String ACTION_UNDIM_SCREEN = "com.farmerbb.taskbar.ACTION_UNDIM_SCREEN";
    public static final String ACTION_UPDATE_HOME_SCREEN_MARGINS = "com.farmerbb.taskbar.UPDATE_HOME_SCREEN_MARGINS";

    // SharedPreference keys

    public static final String PREF_ADD_ICON_TO_DESKTOP = "add_icon_to_desktop";
    public static final String PREF_ADD_SHORTCUT = "add_shortcut";
    public static final String PREF_APP_INFO = "app_info";
    public static final String PREF_APP_SHORTCUTS = "app_shortcuts";
    public static final String PREF_ARRANGE_ICONS = "arrange_icons";
    public static final String PREF_AUTO_HIDE_NAVBAR = "auto_hide_navbar";
    public static final String PREF_AUTO_HIDE_NAVBAR_CATEGORY = "auto_hide_navbar_category";
    public static final String PREF_CENTERED_ICONS = "centered_icons";
    public static final String PREF_CHROME_OS_CONTEXT_MENU_FIX = "chrome_os_context_menu_fix";
    public static final String PREF_DEFAULT_NULL = "null";
    public static final String PREF_DESKTOP_ICONS = "desktop_icons";
    public static final String PREF_DIM_SCREEN = "dim_screen";
    public static final String PREF_DISABLE_ANIMATIONS = "disable_animations";
    public static final String PREF_DONT_SHOW_UNINSTALL_DIALOG = "dont_show_uninstall_dialog";
    public static final String PREF_FIRST_RUN = "first_run";
    public static final String PREF_HAS_CAPTION = "has_caption";
    public static final String PREF_HEADER = "header";
    public static final String PREF_HIDE_ICON_LABELS = "hide_icon_labels";
    public static final String PREF_HSL_ID = "hsl_id";
    public static final String PREF_HSL_NAME = "hsl_name";
    public static final String PREF_IS_RESTARTING = "is_restarting";
    public static final String PREF_REMOVE_DESKTOP_ICON = "remove_desktop_icon";
    public static final String PREF_RESET_COLORS = "reset_colors";
    public static final String PREF_SAMSUNG_DIALOG_SHOWN = "samsung_dialog_shown";
    public static final String PREF_SHORTCUT_1 = "shortcut_1";
    public static final String PREF_SHORTCUT_2 = "shortcut_2";
    public static final String PREF_SHORTCUT_3 = "shortcut_3";
    public static final String PREF_SHORTCUT_4 = "shortcut_4";
    public static final String PREF_SHORTCUT_5 = "shortcut_5";
    public static final String PREF_SHOW_WINDOW_SIZES = "show_window_sizes";
    public static final String PREF_SKIP_AUTO_HIDE_NAVBAR = "skip_auto_hide_navbar";
    public static final String PREF_SKIP_DISABLE_FREEFORM_RECEIVER = "skip_disable_freeform_receiver";
    public static final String PREF_SORT_BY_NAME = "sort_by_name";
    public static final String PREF_TASKBAR_ACTIVE = "taskbar_active";
    public static final String PREF_TIME_OF_SERVICE_START = "time_of_service_start";
    public static final String PREF_TRANSPARENT_START_MENU = "transparent_start_menu";
    public static final String PREF_UNINSTALL = "uninstall";
    public static final String PREF_UNINSTALL_DIALOG_SHOWN = "uninstall_dialog_shown";
    public static final String PREF_WINDOW_SIZE_FULLSCREEN = "window_size_fullscreen";
    public static final String PREF_WINDOW_SIZE_HALF_LEFT = "window_size_half_left";
    public static final String PREF_WINDOW_SIZE_HALF_RIGHT = "window_size_half_right";
    public static final String PREF_WINDOW_SIZE_LARGE = "window_size_large";
    public static final String PREF_WINDOW_SIZE_PHONE_SIZE = "window_size_phone_size";
    public static final String PREF_WINDOW_SIZE_STANDARD = "window_size_standard";

    public static final String PREF_ADDED_SUFFIX = "added";
    public static final String PREF_COMPONENT_NAME_SUFFIX = "component_name";
    public static final String PREF_ICON_THRESHOLD_SUFFIX = "icon_threshold";
    public static final String PREF_LABEL_SUFFIX = "label";
    public static final String PREF_PACKAGE_NAME_SUFFIX = "package_name";
    public static final String PREF_USER_ID_SUFFIX = "user_id";
    public static final String PREF_WINDOW_SIZE_SUFFIX = "window_size";

    // Intent extra keys

    public static final String EXTRA_ACTION = "action";
    public static final String EXTRA_APPWIDGET_ID = "appWidgetId";
    public static final String EXTRA_CELL_ID = "cellId";
    public static final String EXTRA_COMPONENT_NAME = "component_name";
    public static final String EXTRA_CONTEXT_MENU_FIX = "context_menu_fix";
    public static final String EXTRA_COUNT = "count";
    public static final String EXTRA_PACKAGE_NAME = "package_name";
    public static final String EXTRA_START_SERVICES = "start_services";
    public static final String EXTRA_USER_ID = "user_id";
    public static final String EXTRA_WINDOW_SIZE = "window_size";
    public static final String EXTRA_SHOW_PERMISSION_DIALOG = "show_permission_dialog";
}
