/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.wtp.preferences;

import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.maven.ide.eclipse.wtp.internal.MavenWtpPlugin;

public class MavenWtpPreferencePage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {
  
  private Composite parent;
  
  public MavenWtpPreferencePage() {
    super(GRID);
    setPreferenceStore(MavenWtpPlugin.getDefault().getPreferenceStore());
  }

  public void init(IWorkbench workbench) {
  }

  public void createFieldEditors() {
    parent = getFieldEditorParent();
    BooleanFieldEditor useSourcesField = new BooleanFieldEditor(MavenWtpPreferencesConstants.P_APPLICATION_XML_IN_BUILD_DIR, "Generate application.xml under the build directory", parent);
    addField(useSourcesField);
    
  }
}
