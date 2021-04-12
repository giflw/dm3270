package com.bytezone.dm3270.application;

import java.util.concurrent.ThreadFactory;

class NamedThreadFactory implements ThreadFactory {
	public Thread newThread(Runnable r) {
		return new Thread(r, "dm3270");
	}
}