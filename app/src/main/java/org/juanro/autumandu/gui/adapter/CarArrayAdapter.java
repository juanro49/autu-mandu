package org.juanro.autumandu.gui.adapter;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.juanro.autumandu.model.entity.Car;

import java.util.List;

public class CarArrayAdapter extends ArrayAdapter<Car> {

    public CarArrayAdapter(@NonNull Context context, @NonNull List<Car> objects) {
        super(context, android.R.layout.simple_spinner_dropdown_item, objects);
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        TextView v = (TextView) super.getView(position, convertView, parent);
        Car item = getItem(position);
        if (item != null) {
            v.setText(item.getName());
        }
        return v;
    }

    @NonNull
    @Override
    public View getDropDownView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        TextView v = (TextView) super.getDropDownView(position, convertView, parent);
        Car item = getItem(position);
        if (item != null) {
            v.setText(item.getName());
        }
        return v;
    }

    @Override
    public long getItemId(int position) {
        Car item = getItem(position);
        return item != null ? item.getId() : -1;
    }
}
