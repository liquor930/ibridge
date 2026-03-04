package com.brt.ibridge;

import java.util.ArrayList;

public class EventObjectUtils {
	public static int SAMPLE_EVENT_0 = 0;
	public static int SAMPLE_EVENT_1 = 1;
	public static int SAMPLE_EVENT_2 = 2;
	final int event;
	private static final EventObjectUtils SAMPLE_EVENT_OBJECT_0 = new EventObjectUtils(
			SAMPLE_EVENT_0);
	private static final EventObjectUtils SAMPLE_EVENT_OBJECT_1 = new EventObjectUtils(
			SAMPLE_EVENT_1);
	private static final EventObjectUtils SAMPLE_EVENT_OBJECT_2 = new EventObjectUtils(
			SAMPLE_EVENT_1);

	private static ArrayList<EventObjectUtils> list = new ArrayList<EventObjectUtils>();

	static {
		list.add(SAMPLE_EVENT_OBJECT_0);
		list.add(SAMPLE_EVENT_OBJECT_1);
		list.add(SAMPLE_EVENT_OBJECT_2);
	}

	private EventObjectUtils(int ev) {
		event = ev;
	}

	public static void waitEvent(int event, long millis) {
		EventObjectUtils obj = null;

		synchronized (list) {
			for (EventObjectUtils eo : list) {
				if (eo.event == event) {
					obj = eo;
					break;
				}
			}
		}

		if (obj != null) {
			synchronized (obj) {
				try {
					obj.wait(millis);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
	}

	public static void notifyEvent(int event) {
		EventObjectUtils obj = null;

		synchronized (list) {
			for (EventObjectUtils eo : list) {
				if (eo.event == event) {
					obj = eo;
					break;
				}
			}
		}

		if (obj != null) {
			synchronized (obj) {
				obj.notifyAll();
			}
		}
	}

	@Override
	public boolean equals(Object o) {
		if (o == null) {
			return false;
		}

		if (!(o instanceof EventObjectUtils)) {
			return false;
		}

		EventObjectUtils eo = (EventObjectUtils) o;

		return eo.event == this.event;
	}

	@Override
	public int hashCode() {
		return event;
	}

	@Override
	public String toString() {
		return "Event(" + event + ")";
	}
}
