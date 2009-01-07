/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2008-2009, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.dna.maven;

import java.util.Locale;
import java.util.Set;
import org.jboss.dna.common.i18n.I18n;

/**
 * @author Randall Hauch
 */
public final class MavenI18n {

	public static I18n errorGettingUrlForMavenProject;
	public static I18n unsupportedMavenCoordinateFormat;
	public static I18n errorCreatingUrlForMavenId;
	public static I18n errorGettingPomFileForMavenIdAtUrl;
	public static I18n pomFileIsInvalid;
	public static I18n pomFileContainsUnexpectedId;
	public static I18n errorCreatingXpathStatementsToEvaluatePom;
	public static I18n errorCreatingXpathParserToEvaluatePom;
	public static I18n errorReadingXmlDocumentToEvaluatePom;
	public static I18n errorClosingUrlStreamToPom;

	public static I18n unableToOpenSessiontoRepositoryWhenCreatingNode;
	public static I18n unableToFindWorkspaceWhenCreatingNode;
	public static I18n errorCreatingNode;
	public static I18n unableToOpenSessiontoRepositoryWhenReadingNode;
	public static I18n unableToFindWorkspaceWhenReadingNode;
	public static I18n errorReadingNode;
	public static I18n unableToOpenSessiontoRepositoryWhenWritingNode;
	public static I18n unableToFindWorkspaceWhenWritingNode;
	public static I18n errorWritingNode;

	public static I18n unableToWriteToClosedStream;
	public static I18n errorClosingTempFileStreamAfterWritingContent;
	public static I18n errorDeletingTempFileStreamAfterWritingContent;

	static {
		try {
			I18n.initialize(MavenI18n.class);
		} catch (final Exception err) {
			System.err.println(err);
		}
	}

	public static Set<Locale> getLocalizationProblemLocales() {
		return I18n.getLocalizationProblemLocales(MavenI18n.class);
	}

	public static Set<String> getLocalizationProblems() {
		return I18n.getLocalizationProblems(MavenI18n.class);
	}

	public static Set<String> getLocalizationProblems( Locale locale ) {
		return I18n.getLocalizationProblems(MavenI18n.class, locale);
	}
}
