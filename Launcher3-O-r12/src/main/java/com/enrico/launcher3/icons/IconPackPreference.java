package com.enrico.launcher3.icons;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.preference.Preference;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.TextView;

import com.enrico.launcher3.R;
import com.enrico.launcher3.settings.PreferencesState;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class IconPackPreference extends Preference {

    private final PackageManager pm;

    public IconPackPreference(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    private IconPackPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setLayoutResource(R.layout.preference_iconpack);
        pm = context.getPackageManager();
    }

    @Override
    protected View onCreateView(ViewGroup parent) {

        init();
        return super.onCreateView(parent);
    }

    private void init() {
        String currentPack = getPersistedString("");
        if (currentPack.isEmpty()) {
            setNone();
        } else {
            try {
                ApplicationInfo info = pm.getApplicationInfo(currentPack, 0);
                setIcon(info.loadIcon(pm));
                setSummary(info.loadLabel(pm));
            } catch (PackageManager.NameNotFoundException e) {
                setNone();
                persistString("");
            }
        }
    }

    private void setNone() {
        Resources res = getContext().getResources();
        setIcon(ContextCompat.getDrawable(getContext(), R.mipmap.ic_default_icon_pack));
        setSummary(res.getString(R.string.default_iconpack_title));
    }

    @Override
    protected void onClick() {
        super.onClick();
        showDialog();
    }

    private void showDialog() {

        final Map<String, IconPackInfo> packages = loadAvailableIconPacks();
        final IconAdapter adapter = new IconAdapter(getContext(), packages, getPersistedString(""));
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());

        builder.setTitle(R.string.icon_pack_summary);
        builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.dismiss();
            }
        });

        builder.setAdapter(adapter, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int position) {
                String item = adapter.getItem(position);
                persistString(item);

                if (!item.isEmpty()) {
                    IconPackInfo packInfo = packages.get(item);

                    setIcon(packInfo.icon);
                    setSummary(packInfo.label);
                } else {
                    setNone();
                }
            }
        });

        builder.show();
    }

    private Map<String, IconPackInfo> loadAvailableIconPacks() {
        Map<String, IconPackInfo> iconPacks = new HashMap<>();
        List<ResolveInfo> list;
        list = pm.queryIntentActivities(new Intent("com.novalauncher.THEME"), 0);
        list.addAll(pm.queryIntentActivities(new Intent("org.adw.launcher.THEMES"), 0));
        list.addAll(pm.queryIntentActivities(new Intent("com.dlto.atom.launcher.THEME"), 0));
        list.addAll(pm.queryIntentActivities(new Intent("com.gau.go.launcherex.theme"), 0));
        list.addAll(pm.queryIntentActivities(new Intent("android.intent.action.MAIN").addCategory("com.anddoes.launcher.THEME"), 0));
        list.addAll(pm.queryIntentActivities(new Intent("android.intent.action.MAIN").addCategory("com.teslacoilsw.launcher.THEME"), 0));
        list.addAll(pm.queryIntentActivities(new Intent("android.intent.action.MAIN").addCategory("com.fede.launcher.THEME_ICONPACK"), 0));
        for (ResolveInfo info : list) {
            iconPacks.put(info.activityInfo.packageName, new IconPackInfo(info, pm));
        }
        return iconPacks;
    }

    private static class IconPackInfo {
        String packageName;
        CharSequence label;
        Drawable icon;

        IconPackInfo(ResolveInfo r, PackageManager packageManager) {
            packageName = r.activityInfo.packageName;
            icon = r.loadIcon(packageManager);
            label = r.loadLabel(packageManager);
        }

        IconPackInfo(String label, Drawable icon, String packageName) {
            this.label = label;
            this.icon = icon;
            this.packageName = packageName;
        }
    }

    private static class IconAdapter extends BaseAdapter {
        ArrayList<IconPackInfo> mSupportedPackages;
        LayoutInflater mLayoutInflater;
        String mCurrentIconPack;

        IconAdapter(Context context, Map<String, IconPackInfo> supportedPackages, String currentPack) {

            int theme = PreferencesState.isDarkThemeEnabled(context)? R.style.DialogStyleDark : R.style.DialogStyle;
            ContextThemeWrapper contextThemeWrapper = new ContextThemeWrapper(context, theme);
            mLayoutInflater = LayoutInflater.from(contextThemeWrapper);
            mSupportedPackages = new ArrayList<>(supportedPackages.values());
            Collections.sort(mSupportedPackages, new Comparator<IconPackInfo>() {
                @Override
                public int compare(IconPackInfo lhs, IconPackInfo rhs) {
                    return lhs.label.toString().compareToIgnoreCase(rhs.label.toString());
                }
            });

            Resources res = context.getResources();
            String defaultLabel = res.getString(R.string.default_iconpack_title);
            Drawable icon = ContextCompat.getDrawable(context, R.mipmap.ic_default_icon_pack);
            mSupportedPackages.add(0, new IconPackInfo(defaultLabel, icon, ""));
            mCurrentIconPack = currentPack;
        }

        @Override
        public int getCount() {
            return mSupportedPackages.size();
        }

        @Override
        public String getItem(int position) {
            return mSupportedPackages.get(position).packageName;
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {

            if (convertView == null) {
                final ViewGroup nullParent = null;
                convertView = mLayoutInflater.inflate(R.layout.iconpack_dialog, nullParent);
            }

            IconPackInfo info = mSupportedPackages.get(position);
            TextView txtView = convertView.findViewById(R.id.title);
            txtView.setText(info.label);
            ImageView imgView = convertView.findViewById(R.id.icon);
            imgView.setImageDrawable(info.icon);
            RadioButton radioButton = convertView.findViewById(R.id.radio);
            radioButton.setChecked(info.packageName.equals(mCurrentIconPack));
            return convertView;
        }
    }

}

