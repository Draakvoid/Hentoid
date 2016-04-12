package me.devsaki.hentoid.util;

import android.content.Context;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import me.devsaki.hentoid.R;

/**
 * Created by avluis on 04/11/2016.
 * Populate Drawer Menu Contents
 */
public class DrawerMenuContents {
    public static final String FIELD_TITLE = "title";
    public static final String FIELD_ICON = "icon";
    private static final String TAG = LogHelper.makeLogTag(DrawerMenuContents.class);
    private ArrayList<Map<String, ?>> items;
    private String[] mActivityList;
    private Class[] activities;

    public DrawerMenuContents(Context context) {
        mActivityList = context.getResources().getStringArray(R.array.nav_drawer_entries);
        populateActivities();
    }

    private void populateActivities() {
        activities = new Class[mActivityList.length];
        items = new ArrayList<>(mActivityList.length);

        String activity;
        String title;
        int resource;
        String resourcePath = "ic_menu_";
        Class<?> cls = null;
        for (int i = 0; i < mActivityList.length; i++) {
            activity = mActivityList[i];
            title = mActivityList[i].toUpperCase();
            resource = AndroidHelper.getId(resourcePath + mActivityList[i].toLowerCase(),
                    R.drawable.class);
            try {
                cls = Class.forName("me.devsaki.hentoid.activities." + activity + "Activity");
            } catch (ClassNotFoundException e) {
                // TODO: Log to Analytics
                LogHelper.e(TAG, "Class not found: ", e);
            }

            activities[i] = cls;
            items.add(populateDrawerItem(title, resource));
        }
    }

    public List<Map<String, ?>> getItems() {
        return items;
    }

    public Class getActivity(int position) {
        return activities[position];
    }

    public int getPosition(Class activityClass) {
        for (int i = 0; i < activities.length; i++) {
            if (activities[i].equals(activityClass)) {
                return i;
            }
        }
        return -1;
    }

    private Map<String, ?> populateDrawerItem(String title, int icon) {
        HashMap<String, Object> item = new HashMap<>();
        item.put(FIELD_TITLE, title);
        item.put(FIELD_ICON, icon);
        return item;
    }
}