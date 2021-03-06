package io.github.jbytheway.rideottawa.utils;

import android.content.Context;
import androidx.annotation.LayoutRes;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import java.util.List;

public class IndirectArrayAdapter<T> extends BaseAdapter {
    @SuppressWarnings("unused")
    private static final String TAG = "IndirectArrayAdapter";

    public interface ListGenerator<T> {
        List<T> makeList();
    }

    public interface ViewGenerator<T> {
        void applyView(View v, T t);
    }

    public IndirectArrayAdapter(Context context, @LayoutRes int resource, ListGenerator<T> lg, ViewGenerator<T> vg) {
        mContext = context;
        mResource = resource;
        mListGenerator = lg;
        mViewGenerator = vg;
        mList = mListGenerator.makeList();

        //Log.i(TAG, "List size is "+mList.size()+"; content:");
        //for (T t: mList) {
        //    Log.i(TAG, "  "+t);
        //}
    }

    @Override
    public void notifyDataSetChanged() {
        mList = mListGenerator.makeList();
        super.notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return mList.size();
    }

    @Override
    public T getItem(int position) {
        return mList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return -1;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(mResource, parent, false);
        }
        mViewGenerator.applyView(convertView, getItem(position));
        return convertView;
    }

    private final Context mContext;
    private final @LayoutRes int mResource;
    private final ListGenerator<T> mListGenerator;
    private final ViewGenerator<T> mViewGenerator;
    private List<T> mList;
}
