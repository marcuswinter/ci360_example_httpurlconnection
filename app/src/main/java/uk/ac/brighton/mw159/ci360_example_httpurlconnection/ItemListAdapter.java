package uk.ac.brighton.mw159.ci360_example_httpurlconnection;

import java.util.List;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

public class ItemListAdapter extends ArrayAdapter<Item>
{
    private Context mContext;
    private List<Item> mItemList;

    public ItemListAdapter(Context context, int resid, List<Item> list, boolean itemsPending)
    {
        super(context, resid, list);
        mContext = context;
        mItemList = list;
    }

    public int getCount()
    {
        return mItemList.size();
    }

    public Item getItem(int position)
    {
        return mItemList.get(position);
    }

    public View getView(int position, View itemView, ViewGroup parent)
    {
        Item item = mItemList.get(position);

        if (itemView == null)
        {
            LayoutInflater li = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            itemView = li.inflate(R.layout.list_item, parent, false);
        }

        ((TextView) itemView.findViewById(R.id.item_artist)).setText(item.artist);
        ((TextView) itemView.findViewById(R.id.item_date)).setText(item.date);
        ((ImageView) itemView.findViewById(R.id.item_thumb)).setImageBitmap(item.image_thumb);

        return itemView;
    }
}
