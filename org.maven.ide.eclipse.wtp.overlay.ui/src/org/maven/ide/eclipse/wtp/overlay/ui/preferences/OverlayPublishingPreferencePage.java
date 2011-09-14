package org.maven.ide.eclipse.wtp.overlay.ui.preferences;

import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.maven.ide.eclipse.wtp.overlay.OverlayConstants;
import org.maven.ide.eclipse.wtp.overlay.ui.OverlayUIPluginActivator;

public class OverlayPublishingPreferencePage  extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

  public OverlayPublishingPreferencePage() {
    super(GRID);
    this.setPreferenceStore(OverlayUIPluginActivator.getDefault().getPreferenceStore());
  }
  
  @Override
  protected void createFieldEditors() {
    addField(new BooleanFieldEditor(OverlayConstants.P_REPUBLISH_ON_PROJECT_CHANGE, 
                                    "Automatically republish servers on overlay modifications",
                                    getFieldEditorParent()));
  }

  public void init(IWorkbench workbench) {
    // TODO Auto-generated method stub
    
  }


}
