package io.github.jbytheway.octranspoalarm;

import android.content.Context;
import android.support.annotation.LayoutRes;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import com.orm.SugarRecord;

import java.util.List;

public class IndirectArrayAdapter<T> extends BaseAdapter {
    private static final String TAG = "IndirectArrayAdapter";

    interface ListGenerator<T> {
        List<T> makeList();
    }

    interface ViewGenerator<T> {
        void applyView(View v, T t);
    }

    IndirectArrayAdapter(Context context, @LayoutRes int resource, ListGenerator<T> lg, ViewGenerator<T> vg) {
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

    private Context mContext;
    private @LayoutRes int mResource;
    private ListGenerator<T> mListGenerator;
    private ViewGenerator<T> mViewGenerator;
    private List<T> mList;
}
