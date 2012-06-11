package me.kuehle.carreport.db;

public abstract class AbstractItem {
	protected boolean deleted = false;

	protected int id;

	public int getId() {
		return id;
	}
	
	public boolean isDeleted() {
		return deleted;
	}
	
	public abstract void delete();
}
