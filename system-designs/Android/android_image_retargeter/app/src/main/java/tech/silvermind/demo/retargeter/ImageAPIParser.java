package tech.silvermind.demo.retargeter;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by edward on 7/13/16.
 */
public class ImageAPIParser {
    private static abstract class APIParser {
        protected abstract Intent parse(Intent intent);
    }

    private static String TITLE = "Share";
    private static final String FACEBOOK = "com.facebook";
    private static final String TUMBLR = "com.tumblr";
    private static final String FLICKR = "com.yahoo.mobile.client.android.flickr";
    private static final String INSTAGRAM = "com.instagram.android";
    //private static final String GOOGLE_PHOTO = "com.google.android.apps.photos";
    private static final String GOOGLE = "com.google";
    private static final String GMAIL = "com.google.android.gm";
    private static HashMap<String, APIParser> APIDataSheet = new HashMap<String, APIParser>(){{
        put(GMAIL, new APIParser() {
            @Override
            protected Intent parse(Intent intent) {
                //intent.putExtra(Intent.EXTRA_SUBJECT, "Photo from Triangle");
                intent.setType("message/rfc822");
                return intent;
            }
        });
        put(FACEBOOK, new APIParser() {
            @Override
            protected Intent parse(Intent intent) {
                return intent;
            }
        });
        put(TUMBLR, null);
        put(FLICKR, null);
        put(INSTAGRAM, null);
        put(GOOGLE, null);
    }};

    public static Intent parse(final Activity activity, PackageManager manager, final Uri cacheUri){

        Intent share = new Intent(Intent.ACTION_SEND) {{
            setType("image/j*");
            putExtra(Intent.EXTRA_STREAM, cacheUri);
            putExtra(Intent.EXTRA_TEXT, "Photo from Image Retargeter");
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        }};

        // filter correct share intent
        List<ResolveInfo> activityList = manager.queryIntentActivities(share, 0);
        List<Intent> filteredActivities = new ArrayList<>();
        for (final ResolveInfo app : activityList) {
            //Log.d(TAG, "Sharing app: " + app.activityInfo.name);
            boolean contains = false;
            String key = null;
            for(String allow: APIDataSheet.keySet()){
                contains = app.activityInfo.name.contains(allow)?true:false;
                if(contains){
                    key = allow;
                    break;
                }
            }
            if (contains) {
                final ActivityInfo info = app.activityInfo;
                //final ComponentName name = new ComponentName(activity.applicationInfo.packageName, activity.name);
                //share.addCategory(Intent.CATEGORY_LAUNCHER);
                //share.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
                //share.setComponent(name);

                Intent shareExtras = (Intent) share.clone();
                shareExtras.setPackage(info.packageName);
                shareExtras.setClassName(info.packageName, info.name);
                activity.grantUriPermission(info.packageName, cacheUri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                if(APIDataSheet.get(key) != null) shareExtras = APIDataSheet.get(key).parse(shareExtras);
                filteredActivities.add(shareExtras);
            }
        }
        if(filteredActivities.isEmpty()) return null;

        // share intent is wrapped by new intent to prevent a bug in android
        //Intent chooser = Intent.createChooser(new Intent(share), "Share");
        Intent chooser = Intent.createChooser(filteredActivities.remove(0), TITLE);
        chooser.putExtra(Intent.EXTRA_INITIAL_INTENTS, filteredActivities.toArray(new Parcelable[]{}));
        return chooser;
    }
}
