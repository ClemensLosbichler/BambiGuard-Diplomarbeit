package at.ac.szybbs.bambiguard.components;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import at.ac.szybbs.bambiguard.R;

public class ListViewAdapter extends ArrayAdapter<String> {
    public ListViewAdapter(@NonNull Context context, int resource) {
        super(context, resource);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null)
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.list_view_row, parent, false);

        String item = getItem(position);

        TextView textView = (TextView) convertView.findViewById(R.id.value);
        textView.setText(item);

        return convertView;
    }
}
