package com.schedule;

import java.util.HashMap;
import java.util.Queue;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

public class ScheduleMessageService {

	private int maxDelayLevel;
	
	private static final long FIRST_DELAY_TIME = 1000L;
	private static final long DELAY_FOR_A_PERIOD = 10000L;
	private static final long DELAY_FOR_A_WHILE = 100L;
	// ÿ��level��Ӧ����ʱʱ��
    private final ConcurrentHashMap<Integer /* level */, Long/* delay timeMillis */> delayLevelTable = new ConcurrentHashMap<Integer, Long>(32);
    // ��ʱ���㵽������
    private final ConcurrentHashMap<Integer /* level */, Long/* offset */> offsetTable = new ConcurrentHashMap<Integer, Long>(32);
    
    private final ConcurrentHashMap<Integer, Queue<MessageExt>> storeMessage = new ConcurrentHashMap<Integer, Queue<MessageExt>>();
    // ��ʱ��
    private final Timer timer = new Timer("ScheduleMessageTimerThread", true);
    
	public boolean parseDelayLevel() {
        HashMap<String, Long> timeUnitTable = new HashMap<String, Long>();
        timeUnitTable.put("s", 1000L);
        timeUnitTable.put("m", 1000L * 60);
        timeUnitTable.put("h", 1000L * 60 * 60);
        timeUnitTable.put("d", 1000L * 60 * 60 * 24);

        String levelString = "5s,10s,15s,30s,1m,2m,10m,15m,1h,2h,6h,15h";
        try {
            String[] levelArray = levelString.split(",");
            for (int i = 0; i < levelArray.length; i++) {
                String value = levelArray[i];
                String ch = value.substring(value.length() - 1);
                Long tu = timeUnitTable.get(ch);

                int level = i + 1;
                if (level > this.maxDelayLevel) {
                    this.maxDelayLevel = level;
                }
                long num = Long.parseLong(value.substring(0, value.length() - 1));
                long delayTimeMillis = tu * num;
                this.delayLevelTable.put(level, delayTimeMillis);
            }
        }
        catch (Exception e) {
            return false;
        }

        return true;
    }
	
	public void start() {
        // Ϊÿ����ʱ�������Ӷ�ʱ��
        for (Integer level : this.delayLevelTable.keySet()) {
            Long timeDelay = this.delayLevelTable.get(level);
            Long offset = this.offsetTable.get(level);
            if (null == offset) {
                offset = 0L;
            }

            if (timeDelay != null) {
                this.timer.schedule(new DeliverDelayedMessageTimerTask(level, offset), FIRST_DELAY_TIME);
            }
        }
    }
	
	class DeliverDelayedMessageTimerTask extends TimerTask {
        private final int delayLevel;
        private final long offset;


        public DeliverDelayedMessageTimerTask(int delayLevel, long offset) {
            this.delayLevel = delayLevel;
            this.offset = offset;
        }


        @Override
        public void run() {
            try {
                this.executeOnTimeup();
            }
            catch (Exception e) {
                // XXX: warn and notify me
                ScheduleMessageService.this.timer.schedule(new DeliverDelayedMessageTimerTask(
                    this.delayLevel, this.offset), DELAY_FOR_A_PERIOD);
            }
        }
        
        /**
         * �����´�Ͷ��ʱ�䣬���ʱ���ر�������Ϊ��ǰʱ��
         * 
         * @return
         */
        private long correctDeliverTimestamp(final long now, final long deliverTimestamp) {
            // ���Ϊ0���������Ͷ��
            long result = deliverTimestamp;
            // �������ֵ������Ϊ��ǰʱ��
            long maxTimestamp = now + ScheduleMessageService.this.delayLevelTable.get(this.delayLevel);
            if (deliverTimestamp > maxTimestamp) {
                result = now;
            }

            return result;
        }
        
        private void executeOnTimeup() {
        	Queue<MessageExt> queue = ScheduleMessageService.this.storeMessage.get(delayLevel);
        	for(MessageExt msg : queue) {
        		//��Ӧ����֪ͨʱ��
        		final long notifyTime = msg.getNotifyTime();
        		// ������洢��tagsCodeʵ����һ��ʱ���
                long now = System.currentTimeMillis();
                long deliverTimestamp = this.correctDeliverTimestamp(now, notifyTime);

                long countdown = deliverTimestamp - now;
                // ʱ�䵽�ˣ���Ͷ��
                if (countdown <= 0) {
                	System.out.println("Ͷ��=====>" + msg);
                	queue.remove(msg);
                	
                } else {
                	ScheduleMessageService.this.timer.schedule(
                            new DeliverDelayedMessageTimerTask(this.delayLevel, offset), countdown);
                	return;
                }
        	}
        	ScheduleMessageService.this.timer.schedule(new DeliverDelayedMessageTimerTask(this.delayLevel,
                    offset), DELAY_FOR_A_WHILE);
        }
	}
	
	public void add(MessageExt message) {
		Queue<MessageExt> queue = storeMessage.get(message.getLevel());
		if(queue == null) {
			queue = new LinkedBlockingQueue<MessageExt>();
			storeMessage.put(message.getLevel(), queue);
		}
		queue.add(message);
		
	}
}
