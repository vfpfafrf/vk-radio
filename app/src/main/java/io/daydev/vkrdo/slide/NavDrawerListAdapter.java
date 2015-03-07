package io.daydev.vkrdo.slide;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import io.daydev.vkrdo.R;
import io.daydev.vkrdo.bean.RadioInfo;

import java.util.List;

public class NavDrawerListAdapter extends BaseAdapter {

    private Context context;
    private List<NavDrawerItem> navDrawerItems;

    public NavDrawerListAdapter(Context context, List<NavDrawerItem> navDrawerItems){
        this.context = context;
        this.navDrawerItems = navDrawerItems;
    }

    public void add(NavDrawerItem item){
        navDrawerItems.add(item);
        notifyDataSetChanged();
    }

    public boolean replace(String oldTitle, NavDrawerItem newItem){
        int i = 0;
        for(NavDrawerItem item : navDrawerItems){
            if (item.getTitle().equals(oldTitle)){
                if (!item.getTitle().equals(newItem.getTitle()) || item.getIcon() != newItem.getIcon() || !item.getCount().equals(newItem.getCount())) {
                    navDrawerItems.set(i, newItem);
                    notifyDataSetChanged();
                }
                return true;
            }
            i++;
        }
       return false;
    }

    public void removeItem(String title){
        int i=0;
        for(NavDrawerItem item : navDrawerItems) {
            if (item.getTitle().equals(title)) {
                navDrawerItems.remove(i);
                notifyDataSetChanged();
                return;
            }
            i++;
        }
    }

    public void setIcon(RadioInfo radioInfo, int iconSet, int iconUnset){
        if (radioInfo != null) {
            for (int i = 1; i < navDrawerItems.size(); i++) { // first is always "Add" button
                NavDrawerItem item = navDrawerItems.get(i);
                if (item.getTitle().equals(radioInfo.getTitle())) {
                    item.setIcon(iconSet);
                } else {
                    item.setIcon(iconUnset);
                }
            }
            notifyDataSetChanged();
        }
    }

    public void setIcon (RadioInfo radioInfo, int icon){
        if (radioInfo != null) {
            setIcon(radioInfo.getTitle(), icon);
        }
    }

    public void setIcon (String title, int icon){
        int i = 0;
        for(NavDrawerItem item : navDrawerItems) {
            if (item.getTitle().equals(title)) {
                setIcon(i, icon);
                return;
            }
            i++;
        }
    }

    public void setIcon (int position, int icon){
        NavDrawerItem item = navDrawerItems.get(position);
        if (item != null){
            item.setIcon(icon);
            notifyDataSetChanged();
        }
    }

    @Override
    public int getCount() {
        return navDrawerItems.size();
    }

    @Override
    public Object getItem(int position) {
        return navDrawerItems.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            LayoutInflater mInflater = (LayoutInflater)
                    context.getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
            convertView = mInflater.inflate(R.layout.drawer_list_item, null);
        }

        ImageView imgIcon = (ImageView) convertView.findViewById(R.id.icon);
        TextView txtTitle = (TextView) convertView.findViewById(R.id.title);
        TextView txtCount = (TextView) convertView.findViewById(R.id.counter);

        if (navDrawerItems.get(position).getIcon() > 0) {
            imgIcon.setImageResource(navDrawerItems.get(position).getIcon());
        } else {
            imgIcon.setImageResource(android.R.drawable.ic_media_pause);
        }
        txtTitle.setText(navDrawerItems.get(position).getTitle());

        // displaying count
        // check whether it set visible or not
        if(navDrawerItems.get(position).getCounterVisibility()){
            txtCount.setText(navDrawerItems.get(position).getCount());
        }else{
            // hide the counter view
            txtCount.setVisibility(View.GONE);
        }

        return convertView;
    }

}