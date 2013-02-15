/*
 * Copyright 2012 Jan Kühle
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package me.kuehle.carreport.util.gui;

import java.util.ArrayList;

import me.kuehle.carreport.R;
import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

public class SectionListAdapter extends BaseAdapter {
	public abstract static class AbstractListItem implements
			Comparable<AbstractListItem> {
		protected String label;

		public String getLabel() {
			return label;
		}

		public void setLabel(String label) {
			this.label = label;
		}
	}

	public static class Item extends AbstractListItem {
		private String value;

		public Item(String label, String value) {
			this.label = label;
			this.value = value;
		}

		public String getValue() {
			return value;
		}

		public void setValue(String value) {
			this.value = value;
		}

		@Override
		public int compareTo(AbstractListItem another) {
			if (another instanceof Section) {
				return -1;
			} else {
				// return label.compareTo(another.getLabel());
				return 0;
			}
		}
	}

	public static class Section extends AbstractListItem {
		public static final int DONT_STICK = 0;
		public static final int STICK_TOP = Integer.MIN_VALUE;
		public static final int STICK_BOTTOM = Integer.MAX_VALUE;

		private int color;
		private int stick;
		private ArrayList<Item> items;

		public Section(String label, int color) {
			this(label, color, DONT_STICK);
		}

		public Section(String label, int color, int stick) {
			this.label = label;
			this.color = color;
			this.stick = stick;
			this.items = new ArrayList<Item>();
		}

		public void addItem(Item item) {
			items.add(item);
		}

		@Override
		public int compareTo(AbstractListItem another) {
			if (another instanceof Item) {
				return 1;
			} else {
				Section otherSection = (Section) another;
				if (stick != otherSection.getStick()) {
					return Integer.valueOf(stick).compareTo(
							otherSection.getStick());
				} else {
					return label.compareTo(another.getLabel());
				}
			}
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			Section other = (Section) obj;
			if (color != other.color)
				return false;
			if (label == null) {
				if (other.label != null)
					return false;
			} else if (!label.equals(other.label))
				return false;
			return true;
		}

		public int getColor() {
			return color;
		}

		public ArrayList<Item> getItems() {
			return items;
		}

		public int getStick() {
			return stick;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + color;
			result = prime * result + ((label == null) ? 0 : label.hashCode());
			return result;
		}

		public void removeItem(Item item) {
			items.remove(item);
		}

		public void setColor(int color) {
			this.color = color;
		}

		public void setStick(int stick) {
			this.stick = stick;
		}
	}

	private static final int ITEM_VIEW_TYPE_NORMAL = 0;
	private static final int ITEM_VIEW_TYPE_SEPARATOR = 1;
	private static final int ITEM_VIEW_TYPE_COUNT = 2;

	private Context context;
	private int itemViewID;
	private AbstractListItem[] items;
	private boolean colorSections;

	public SectionListAdapter(Context context, int itemViewID,
			ArrayList<? extends AbstractListItem> data, boolean colorSections) {
		this.context = context;
		this.itemViewID = itemViewID;
		ArrayList<AbstractListItem> items = new ArrayList<AbstractListItem>();
		for (AbstractListItem item : data) {
			items.add(item);
			if (item instanceof Section) {
				items.addAll(((Section) item).getItems());
			}
		}
		this.items = items.toArray(new AbstractListItem[items.size()]);
		this.colorSections = colorSections;
	}

	@Override
	public int getCount() {
		return items.length;
	}

	@Override
	public AbstractListItem getItem(int position) {
		return items[position];
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	@Override
	public int getItemViewType(int position) {
		return (items[position] instanceof Section) ? ITEM_VIEW_TYPE_SEPARATOR
				: ITEM_VIEW_TYPE_NORMAL;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		int type = getItemViewType(position);

		if (convertView == null) {
			convertView = LayoutInflater
					.from(context)
					.inflate(
							type == ITEM_VIEW_TYPE_SEPARATOR ? R.layout.separator_list_item
									: itemViewID, parent, false);
		}

		if (type == ITEM_VIEW_TYPE_SEPARATOR) {
			Section section = (Section) getItem(position);
			TextView text = (TextView) convertView;
			text.setText(section.getLabel());
			if (colorSections) {
				text.setTextColor(section.getColor());
				GradientDrawable drawableBottom = (GradientDrawable) text
						.getCompoundDrawables()[3];
				drawableBottom.setColor(section.getColor());
			}
		} else {
			Item item = (Item) getItem(position);
			((TextView) convertView.findViewById(android.R.id.text1))
					.setText(item.getLabel());
			((TextView) convertView.findViewById(android.R.id.text2))
					.setText(item.getValue());
		}

		return convertView;
	}

	@Override
	public int getViewTypeCount() {
		return ITEM_VIEW_TYPE_COUNT;
	}

	@Override
	public boolean isEnabled(int position) {
		return getItemViewType(position) != ITEM_VIEW_TYPE_SEPARATOR;
	}
}
