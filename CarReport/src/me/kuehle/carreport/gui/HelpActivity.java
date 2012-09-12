package me.kuehle.carreport.gui;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import me.kuehle.carreport.R;
import android.app.ActionBar;
import android.app.Fragment;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

public class HelpActivity extends PreferenceActivity {
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		ActionBar actionBar = getActionBar();
		actionBar.setDisplayHomeAsUpEnabled(true);
	}

	@Override
	public void onBuildHeaders(List<Header> target) {
		loadHeadersFromResource(R.xml.help_headers, target);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			finish();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	private static abstract class HelpFragment extends Fragment {
		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container,
				Bundle savedInstanceState) {
			TextView v = (TextView) inflater.inflate(
					R.layout.fragment_help_detail, container, false);

			v.setMovementMethod(LinkMovementMethod.getInstance());
			try {
				InputStream in = getActivity().getAssets().open(
						String.format("help-%d-%s.html", getHelpId(),
								getLocale()));
				byte[] buffer = new byte[in.available()];
				in.read(buffer);
				in.close();
				v.setText(Html.fromHtml(new String(buffer)));
			} catch (IOException e) {
			}

			return v;
		}

		protected abstract int getHelpId();

		private String getLocale() {
			String[] availableLocales = { "de", "en" };
			String locale = Locale.getDefault().getLanguage().substring(0, 2)
					.toLowerCase();
			if (Arrays.binarySearch(availableLocales, locale) < 0) {
				return availableLocales[0];
			} else {
				return locale;
			}
		}
	}

	public static class GettingStartedFragment extends HelpFragment {
		@Override
		protected int getHelpId() {
			return 0;
		}
	}

	public static class TipsFragment extends HelpFragment {
		@Override
		protected int getHelpId() {
			return 1;
		}
	}

	public static class CalculationsFragment extends HelpFragment {
		@Override
		protected int getHelpId() {
			return 2;
		}
	}
}
