package me.kuehle.carreport;

import me.kuehle.carreport.db.Helper;

public class Application extends android.app.Application {
	@Override
	public void onCreate() {
		super.onCreate();
		Helper.init(this);
	}
}
