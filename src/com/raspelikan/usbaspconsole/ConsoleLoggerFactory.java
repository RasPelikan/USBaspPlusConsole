package com.raspelikan.usbaspconsole;

import org.eclipse.ui.console.IConsoleFactory;

/**
 * Handling console opened events
 * 
 * @author RasPelikan
 */
public class ConsoleLoggerFactory implements IConsoleFactory {

	/**
	 * Called if the USBasp+ console is called
	 */
	@Override
	public void openConsole() {
		
		USBaspConsoleActivator.bringConsoleToForeground();
		
	}

}
