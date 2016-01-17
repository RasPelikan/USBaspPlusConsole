package com.raspelikan.usbaspconsole;

import org.usb4java.LibUsb;
import org.usb4java.LibUsbException;

/**
 * This is the event handling thread. libusb doesn't start threads by its
 * own so it is our own responsibility to give libusb time to handle the
 * events in our own thread.
 * 
 * @author RasPelikan
 */
public class UsbEventHandlingThread extends Thread {
	
    /** If thread should abort. */
    private volatile boolean abort;

    /**
     * Aborts the event handling thread.
     */
    public void abort() {
    	
        this.abort = true;
        
    }

    @Override
    public void run() {
    	
        while (!this.abort) {
        	
            // Let libusb handle pending events. This blocks until events
            // have been handled, a hotplug callback has been deregistered
            // or the specified time of 1 second (Specified in
            // Microseconds) has passed.
            int result = LibUsb.handleEventsTimeout(null, 1000000);
            if (result != LibUsb.SUCCESS) {
                throw new LibUsbException("Unable to handle events", result);
            }
            
        }
        
    }
    
}
