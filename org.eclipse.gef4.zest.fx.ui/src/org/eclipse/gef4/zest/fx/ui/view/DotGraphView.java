/*******************************************************************************
 * Copyright (c) 2014 Fabian Steeg, hbz.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Fabian Steeg, hbz - initial API & implementation
 *
 *******************************************************************************/
package org.eclipse.gef4.zest.fx.ui.view;

import org.eclipse.gef4.dot.DotImport;
import org.eclipse.gef4.zest.fx.ZestFxModule;
import org.eclipse.gef4.zest.fx.ui.ZestFxUiModule;

import com.google.inject.Guice;
import com.google.inject.util.Modules;

public class DotGraphView extends ZestFxUiView {

	public DotGraphView() {
		super(Guice.createInjector(Modules.override(new ZestFxModule())//
				.with(new ZestFxUiModule())));
		setGraph(new DotImport("digraph{1->2;2->3;2->4}").newGraphInstance());
	}

}
