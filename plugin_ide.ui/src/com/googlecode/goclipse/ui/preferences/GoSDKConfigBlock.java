/*******************************************************************************
 * Copyright (c) 2015, 2015 Bruno Medeiros and other Contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Bruno Medeiros - initial API and implementation
 *******************************************************************************/
package com.googlecode.goclipse.ui.preferences;

import static melnorme.utilbox.core.CoreUtil.array;

import java.io.File;
import java.util.List;

import melnorme.lang.ide.ui.preferences.common.AbstractComponentsPrefPage;
import melnorme.util.swt.SWTFactoryUtil;
import melnorme.util.swt.components.AbstractComponentExt;
import melnorme.util.swt.components.fields.ButtonTextField;
import melnorme.util.swt.components.fields.ComboBoxField;
import melnorme.util.swt.components.fields.DirectoryTextField;
import melnorme.util.swt.components.fields.FileTextField;
import melnorme.utilbox.fields.IFieldValueListener;
import melnorme.utilbox.misc.ArrayUtil;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;

import com.googlecode.goclipse.core.GoEnvironmentPrefs;
import com.googlecode.goclipse.core.GoEnvironmentUtils;
import com.googlecode.goclipse.tooling.GoSDKLocationValidator;
import com.googlecode.goclipse.tooling.env.GoArch;
import com.googlecode.goclipse.tooling.env.GoOs;

public class GoSDKConfigBlock extends AbstractComponentExt {
	
	protected final AbstractComponentsPrefPage prefPage;
	
	protected final DirectoryTextField goRootField = new DirectoryTextField("GO&ROOT:");
	protected final ComboBoxField goOSField = new ComboBoxField("G&OOS:",
		ArrayUtil.prepend("<default>", GoOs.GOOS_VALUES),
		ArrayUtil.prepend("", GoOs.GOOS_VALUES));
	protected final ComboBoxField goArchField = new ComboBoxField("GO&ARCH:", 
		array("<default>", GoArch.ARCH_AMD64, GoArch.ARCH_386,  GoArch.ARCH_ARM), 
		array(""         , GoArch.ARCH_AMD64, GoArch.ARCH_386, GoArch.ARCH_ARM));
	
	protected final FileTextField goToolPath = new FileTextField("go tool:");
	protected final FileTextField goFmtPath = new FileTextField("gofmt:");
	protected final FileTextField goDocPath = new FileTextField("godoc:");
	
	protected final ButtonTextField goPathField = new GoPathField("GO&PATH:", "Add Folder");
	
	public GoSDKConfigBlock(AbstractComponentsPrefPage preferencePage) {
		this.prefPage = preferencePage;
		
		goRootField.addValueChangedListener(new IFieldValueListener() {
			@Override
			public void fieldValueChanged() {
				handleGoRootChange();
			}
		});
	}
	
	protected void handleGoRootChange() {
		IPath gorootPath = new Path(goRootField.getFieldValue());
		File binPath = gorootPath.append("bin").toFile();
		
		if(prefPage.getFieldStatus(goRootField).isOkStatus()) {
			
			File compilerFile = findExistingFile(binPath.toPath(), 
				GoEnvironmentUtils.getSupportedCompilerNames());
			
			goOSField.setFieldStringValue(""); 
			goArchField.setFieldStringValue(""); 
			
			setValueIfFileExists(goToolPath, compilerFile);
			
			String goFmtName = GoEnvironmentUtils.getDefaultGofmtName();
			setValueIfFileExists(goFmtPath, gorootPath.append("bin").append(goFmtName).toFile());
			
			String goDocName = GoEnvironmentUtils.getDefaultGodocName();
			setValueIfFileExists(goDocPath, gorootPath.append("bin").append(goDocName).toFile());
			
		}
	}
	
	protected void setValueIfFileExists(FileTextField fileField, File filePath) {
		if (filePath != null && filePath.exists() && filePath.isFile()) {
			fileField.setFieldValue(filePath.getAbsolutePath());
		} else {
			fileField.setFieldValue("");
		}
	}
	
	protected static File findExistingFile(java.nio.file.Path binPath, List<String> paths) {
		for (String strPath : paths) {
			java.nio.file.Path path = binPath.resolve(strPath);
			File file = path.toFile();
			if (file.exists()) {
				return file;
			}
		}
		
		return null;
	}
	
	/* -----------------  ----------------- */
	
	@Override
	public int getPreferredLayoutColumns() {
		return 1;
	}
	
	@Override
	protected void createContents(Composite topControl) {
		Group goSDK = createPreferenceGroup(topControl, "Go SDK installation:");
		int numColumns = 3;
		goSDK.setLayout(glSwtDefaults().numColumns(numColumns).create());
		
		goRootField.createComponentInlined(goSDK);
		prefPage.connectStringField(GoEnvironmentPrefs.GO_ROOT.key, goRootField, new GoSDKLocationValidator());
		
		goOSField.createComponentInlined(goSDK);
		prefPage.addComboComponent(GoEnvironmentPrefs.GO_OS.key, goOSField);
		goArchField.createComponentInlined(goSDK);
		prefPage.addComboComponent(GoEnvironmentPrefs.GO_ARCH.key, goArchField);
		
		SWTFactoryUtil.createLabel(goSDK, 
			SWT.SEPARATOR | SWT.HORIZONTAL, "",
			gdFillDefaults().span(numColumns, 1).grab(true, false).indent(0, 5).create());
		
		goToolPath.createComponentInlined(goSDK);
		prefPage.connectFileField(GoEnvironmentPrefs.COMPILER_PATH.key, goToolPath, false, goToolPath.getLabelText());
		goFmtPath.createComponentInlined(goSDK);
		prefPage.connectFileField(GoEnvironmentPrefs.FORMATTER_PATH.key, goFmtPath, false, goFmtPath.getLabelText());
		goDocPath.createComponentInlined(goSDK);
		prefPage.connectFileField(GoEnvironmentPrefs.DOCUMENTOR_PATH.key, goDocPath, false, goDocPath.getLabelText());
		
		/* -----------------  ----------------- */
		
		Group goPathGroup = createPreferenceGroup(topControl, "Default environment:");
		goPathGroup.setLayout(glSwtDefaults().numColumns(numColumns).create());
		
		goPathField.createComponentInlined(goPathGroup);
		prefPage.addStringComponent(GoEnvironmentPrefs.GO_PATH.key, goPathField);
	}
	
	protected Group createPreferenceGroup(Composite parent, String groupName) {
		return SWTFactoryUtil.createGroup(parent, groupName,
			GridDataFactory.fillDefaults().grab(true, false).minSize(300, SWT.DEFAULT).create());
	}
	
	public static class GoPathField extends ButtonTextField {
		public GoPathField(String label, String buttonlabel) {
			super(label, buttonlabel);
		}
		
		@Override
		protected String getNewValueFromButtonSelection() {
			String newValue = DirectoryTextField.openDirectoryDialog("", text.getShell());
			
			return getFieldValue() + File.pathSeparator + newValue;
		}
	}
	
	@Override
	public void updateComponentFromInput() {
	}
	
}