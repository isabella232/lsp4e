/*******************************************************************************
 * Copyright (c) 2021 Red Hat Inc. and others.
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   Victor Rubezhny (Red Hat Inc.) - initial implementation
 *******************************************************************************/
package org.eclipse.lsp4e.test.linkedediting;

import static org.junit.Assert.assertTrue;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.link.LinkedPosition;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.lsp4e.test.AllCleanRule;
import org.eclipse.lsp4e.test.TestUtils;
import org.eclipse.lsp4e.tests.mock.MockLanguageServer;
import org.eclipse.lsp4j.LinkedEditingRanges;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.tests.harness.util.DisplayHelper;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class LinkedEditingTest {

	@Rule public AllCleanRule clear = new AllCleanRule();
	private IProject project;

	@Before
	public void setUp() throws CoreException {
		project = TestUtils.createProject("LinkedEditingTest" + System.currentTimeMillis());
	}

	@Test
	public void testLinkedEditing() throws CoreException, InvocationTargetException {
		List<Range> ranges = new ArrayList<>();
		ranges.add(new Range(new Position(1, 3), new Position(1, 7)));
		ranges.add(new Range(new Position(3, 4), new Position(3, 8)));
		
		LinkedEditingRanges linkkedEditingRanges = new LinkedEditingRanges(ranges);
		MockLanguageServer.INSTANCE.setLinkedEditingRanges(linkkedEditingRanges);

		IFile testFile = TestUtils.createUniqueTestFile(project, "<html>\n  <body>\n    a body text\n  </body>\n</html>");
		ITextViewer viewer = TestUtils.openTextViewer(testFile);

		viewer.getTextWidget().setCaretOffset(11); 

		if (!(viewer instanceof ISourceViewer)) {
			Assert.fail();
		}

		ISourceViewer sourceViewer = (ISourceViewer) viewer;

		viewer.getTextWidget().setSelection(11); // 10-14 <body|>
		
		Map<org.eclipse.jface.text.Position, Annotation> annotations = new HashMap<>();

		new DisplayHelper() {
			@Override
			protected boolean condition() {
				Iterator<Annotation> iterator = sourceViewer.getAnnotationModel().getAnnotationIterator();
				while (iterator.hasNext()) {
					Annotation annotation = iterator.next();
					if (annotation.getType().startsWith("org.eclipse.ui.internal.workbench.texteditor.link")) {
						return true;
					}
				}
				return false;
			}
		}.waitForCondition(Display.getCurrent(), 3000);

		IAnnotationModel model = sourceViewer.getAnnotationModel();
		final Iterator<Annotation> iterator = model.getAnnotationIterator();
		while (iterator.hasNext()) {
			Annotation annotation = iterator.next();
			annotations.put(model.getPosition(annotation), annotation);
		}

		Annotation annotation = annotations.get(new LinkedPosition(sourceViewer.getDocument(), 10, 4, 0));
		Assert.assertNotNull(annotation);
		assertTrue(annotation.getType().startsWith("org.eclipse.ui.internal.workbench.texteditor.link"));
	}
}
