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

package me.kuehle.carreport.reports;

import java.util.ArrayList;

import me.kuehle.carreport.gui.SectionListAdapter.AbstractListItem;
import me.kuehle.carreport.gui.SectionListAdapter.Item;
import me.kuehle.carreport.gui.SectionListAdapter.Section;

public class ReportData {
	private ArrayList<AbstractListItem> data = new ArrayList<AbstractListItem>();

	public void applyCalculation(double value1, int option) {
		for (AbstractListItem item : data) {
			if (item instanceof Section) {
				for (Item childItem : ((Section) item).getItems()) {
					if (childItem instanceof CalculatableItem) {
						((CalculatableItem) childItem).applyCalculation(value1,
								option);
					}
				}
			} else if (item instanceof CalculatableItem) {
				((CalculatableItem) item).applyCalculation(value1, option);
			}
		}
	}

	public ArrayList<AbstractListItem> getData() {
		return data;
	}

	public void resetCalculation() {
		for (AbstractListItem item : data) {
			if (item instanceof Section) {
				for (Item childItem : ((Section) item).getItems()) {
					if (childItem instanceof CalculatableItem) {
						((CalculatableItem) childItem).resetCalculation();
					}
				}
			} else if (item instanceof CalculatableItem) {
				((CalculatableItem) item).resetCalculation();
			}
		}
	}

	public abstract static class CalculatableItem extends Item {
		protected String origLabel;
		protected String origValue;

		public CalculatableItem(String label, String value) {
			super(label, value);
			origLabel = label;
			origValue = value;
		}

		public abstract void applyCalculation(double value1, int option);

		public void resetCalculation() {
			setLabel(origLabel);
			setValue(origValue);
		}
	}
}
