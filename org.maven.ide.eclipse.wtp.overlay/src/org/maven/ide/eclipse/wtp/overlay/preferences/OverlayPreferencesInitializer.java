package org.maven.ide.eclipse.wtp.overlay.preferences;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.core.runtime.preferences.DefaultScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.IScopeContext;
import org.maven.ide.eclipse.wtp.overlay.OverlayConstants;
import org.maven.ide.eclipse.wtp.overlay.OverlayPluginActivator;

public class OverlayPreferencesInitializer extends AbstractPreferenceInitializer {

  /**
   * @see org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer#initializeDefaultPreferences()
   */
  public void initializeDefaultPreferences() {
    IEclipsePreferences store = ((IScopeContext) new DefaultScope()).getNode(OverlayPluginActivator.PLUGIN_ID);
    store.putBoolean(OverlayConstants.P_REPUBLISH_ON_PROJECT_CHANGE, true);
  }
}