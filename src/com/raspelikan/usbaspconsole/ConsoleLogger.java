package com.raspelikan.usbaspconsole;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Writer;

import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.Status;
import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IOConsole;
import org.eclipse.ui.console.IOConsoleOutputStream;

/**
 * The thread responsible for writing USART contents to Eclipse' console.
 * 
 * @author RasPelikan
 */
public class ConsoleLogger extends Thread {

	public static final String CONSOLE_NAME = "USBasp+ Console";
	
	private ILog log;
	
	private volatile boolean shutdown;
	private volatile boolean enabled;
	
	private USBaspDevice device;
	private IOConsole console;
	
	/**
	 * Constructor
	 */
	public ConsoleLogger(final ILog log, final USBaspDevice device, final boolean enabled) {
		
		this.log = log;
		this.device = device;
		this.enabled = enabled;
		
	}
	
	/**
	 * shutdown and wait for being entirely shut down
	 */
	public void shutdown() {
		
		synchronized (this) {

			this.shutdown = true;
			this.notifyAll();
			
		}

		try {
			
			this.join(10000);
			
		} catch (InterruptedException e) {
			// never mind
		}
		
	}
	
	/**
	 * the thread's main routine
	 */
	@Override
	public void run() {
		
		Writer consoleWriter = null;
		try {
			
			// initialize the console writer
			consoleWriter = initializeConsole();
			
			// repeat unless shutdown
			while (!shutdown) {

				// if console not disabled (no usb device connected)
				if (enabled) {
					
					try {
						
						// fetch all strings available and print to console
						String deviceOutput;
						while ((deviceOutput = device.getString()) != null) {
							
							try {
								consoleWriter.write(deviceOutput);
								consoleWriter.flush();
							} catch (IOException e) {
								// ignore at the moment
							}
							
						}
						
					} catch (Throwable e) {
						
						// errors will be ignored if occur during unplugging the device
						if (enabled) {
							
				        	this.log.log(new Status(Status.WARNING,
				        			USBaspConsoleActivator.PLUGIN_ID,
				        			"Error reading data from device", e));
			        	
						}
						
					}
					
					// show that console changed
					USBaspConsoleActivator.warnOfContentChange();
					
				}
                
				// no data any more? wait half a second
				synchronized (this) {
					
					try {
						this.wait(500);
					} catch (InterruptedException e) {
						// maybe shutdown called?
					}
					
				}
				
			}
			
		} finally {
		
			try {
				consoleWriter.close();
			} catch (IOException e) {
				// never mind any more
			}
			
			// free any resources acquired during console initialization
			shutdownConsole();
			
		}
		
	}
	
	/**
	 * initialize the console writer
	 * 
	 * @return the writer
	 */
	private Writer initializeConsole() {
		
		// shutdown any console previously initialized
		this.shutdownConsole();
		
		// build new console
		this.console = new IOConsole(CONSOLE_NAME,
				USBaspConsoleActivator.getImageDescriptor("icons/usbasp_console.png"));

		// build writer for new console
		final IOConsoleOutputStream stream = this.console.newOutputStream();
		final Writer consoleWriter = new PrintWriter(new OutputStreamWriter(
				stream), true);
		
		// add console to list of consoles
		final ConsolePlugin consolePlugin = ConsolePlugin.getDefault();
		consolePlugin.getConsoleManager().addConsoles(
				new IOConsole[] { this.console } );
		
		return consoleWriter;
		
	}
	
	/**
	 * free any resources acquired during console initialization
	 */
	private void shutdownConsole() {
		
		if (this.console != null) {

			// remove console from list of consoles available
			final ConsolePlugin consolePlugin = ConsolePlugin.getDefault();
			consolePlugin.getConsoleManager().removeConsoles(
					new IOConsole[] { this.console } );
			
			this.console = null;
			
		}
		
	}
	
	/**
	 * Called once a device is connected
	 */
	public void enable() {
		
		this.enabled = true;
		this.console.activate();
		
	}
	
	/**
	 * Called once a device is disconnected
	 */
	public void disable() {
		
		this.enabled = false;
		
	}
	
}
