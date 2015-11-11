package com.schedule;

public class MessageExt {

	private int level;
	
	private Object obj;
	
	private long notifyTime;

	public MessageExt(int level, Object obj) {
		this.level = level;
		this.obj = obj;
		this.notifyTime = System.currentTimeMillis();
	}
	
	public int getLevel() {
		return level;
	}

	public void setLevel(int level) {
		this.level = level;
	}

	public Object getObj() {
		return obj;
	}

	public void setObj(Object obj) {
		this.obj = obj;
	}

	public long getNotifyTime() {
		return notifyTime;
	}

	public void setNotifyTime(long notifyTime) {
		this.notifyTime = notifyTime;
	}
	
	
}
