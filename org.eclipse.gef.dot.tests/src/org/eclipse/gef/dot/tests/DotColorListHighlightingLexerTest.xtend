/*******************************************************************************
 * Copyright (c) 2018 itemis AG and others.
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Tamas Miklossy (itemis AG) - initial API and implementation
 * 
 *******************************************************************************/
package org.eclipse.gef.dot.tests

import com.google.inject.Inject
import com.google.inject.name.Named
import org.eclipse.gef.dot.internal.language.DotColorListUiInjectorProvider
import org.eclipse.xtext.junit4.InjectWith
import org.eclipse.xtext.junit4.XtextRunner
import org.eclipse.xtext.parser.antlr.Lexer
import org.eclipse.xtext.ui.LexerUIBindings
import org.junit.runner.RunWith

@RunWith(XtextRunner)
@InjectWith(DotColorListUiInjectorProvider)
class DotColorListHighlightingLexerTest extends AbstractDotColorListLexerTest {

	@Inject @Named(LexerUIBindings.HIGHLIGHTING) Lexer lexer

	override lexer() {
		lexer
	}

}