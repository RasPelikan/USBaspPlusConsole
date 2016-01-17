package com.raspelikan.usbaspconsole;

import org.eclipse.core.runtime.Status;
import org.usb4java.Context;
import org.usb4java.Device;
import org.usb4java.DeviceDescriptor;
import org.usb4java.HotplugCallback;
import org.usb4java.LibUsb;
import org.usb4java.LibUsbException;

/**
 * @author RasPelikan
 */
public class UsbHotplugHandler implements HotplugCallback {
	
	private USBaspConsoleActivator plugin;
	
	public UsbHotplugHandler(final USBaspConsoleActivator plugin) {
		
		this.plugin = plugin;
		
	}
	
    @Override
    public int processEvent(
    		final Context context,
    		final Device device, 
    		final int event,
    		final Object userData) {
    	
        final DeviceDescriptor descriptor = new DeviceDescriptor();
        final int result = LibUsb.getDeviceDescriptor(device, descriptor);
        if (result != LibUsb.SUCCESS) {
        	throw new LibUsbException("Unable to read device descriptor", result);
        }
        
        final short idVendor = descriptor.idVendor();
        final short idProduct = descriptor.idProduct();
        
        if ((idVendor == USBaspConsoleActivator.USBasp_idVendor)
        		&& (idProduct == USBaspConsoleActivator.USBasp_idProduct)) {

            if (event == LibUsb.HOTPLUG_EVENT_DEVICE_ARRIVED) {
            	
            	plugin.enableLogger();

	        	plugin.getLog().log(new Status(Status.INFO,
	        			USBaspConsoleActivator.PLUGIN_ID,
	        			"USBasp plugged in. Enabled USBasp+ console at baud rate '"
	        			+ plugin.getBaudRateByPreferences() + "'!"));
            	
            }
            else if (event == LibUsb.HOTPLUG_EVENT_DEVICE_LEFT) {
            	
            	plugin.disableLogger();

	        	plugin.getLog().log(new Status(Status.INFO,
	        			USBaspConsoleActivator.PLUGIN_ID,
	        			"USBasp unplugged. Disabled USBasp+ console!"));
            	
            }
        	
        }

        return 0;
        
    }
    
}