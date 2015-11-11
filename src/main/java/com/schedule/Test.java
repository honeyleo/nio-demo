package com.schedule;

public class Test {

	public static void main(String[] args) {
		ScheduleMessageService scheduleMessageService = new ScheduleMessageService();
		
		scheduleMessageService.parseDelayLevel();
		
		scheduleMessageService.start();
		
		scheduleMessageService.add(new MessageExt(1, new Object()));

		try {
			Thread.sleep(1000000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

}
