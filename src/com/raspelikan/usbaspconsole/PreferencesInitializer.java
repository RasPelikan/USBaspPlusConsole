package com.raspelikan.usbaspconsole;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.jface.preference.IPreferenceStore;

/**
 * @author RasPelikan
 */
public class PreferencesInitializer extends AbstractPreferenceInitializer {

	@Override
	public void initializeDefaultPreferences() {
		
		// These settings will show up when the preference page
		// is shown for the first time.
		IPreferenceStore store = USBaspConsoleActivator.getDefault().getPreferenceStore();
		store.setDefault(USBaspConsoleActivator.ACTIVATED_PROPERTY,
				USBaspConsoleActivator.ACTIVATED_DEFAULT);
		store.setDefault(USBaspConsoleActivator.BAUDRATE_PROPERTY,
				USBaspConsoleActivator.BAUDRATE_DEFAULT);
		
	}

}
