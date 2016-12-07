/*******************************************************************************
 * Copyright (c) 2016 itemis AG and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Alexander Nyßen (itemis AG) - initial API and implementation
 *     Tamas Miklossy  (itemis AG) - Add support for polygon-based node shapes (bug #441352)
 *                                 - Add support for all dot attributes (bug #461506)
 *
 *******************************************************************************/
package org.eclipse.gef.dot.internal;

import java.io.StringReader;
import java.util.Collections;
import java.util.List;

import org.eclipse.emf.common.util.BasicDiagnostic;
import org.eclipse.emf.common.util.Diagnostic;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.gef.dot.internal.language.DotArrowTypeStandaloneSetup;
import org.eclipse.gef.dot.internal.language.DotColorStandaloneSetup;
import org.eclipse.gef.dot.internal.language.DotPointStandaloneSetup;
import org.eclipse.gef.dot.internal.language.DotShapeStandaloneSetup;
import org.eclipse.gef.dot.internal.language.DotSplineTypeStandaloneSetup;
import org.eclipse.gef.dot.internal.language.DotStyleStandaloneSetup;
import org.eclipse.gef.dot.internal.language.clustermode.ClusterMode;
import org.eclipse.gef.dot.internal.language.dir.DirType;
import org.eclipse.gef.dot.internal.language.layout.Layout;
import org.eclipse.gef.dot.internal.language.outputmode.OutputMode;
import org.eclipse.gef.dot.internal.language.pagedir.Pagedir;
import org.eclipse.gef.dot.internal.language.parser.antlr.DotArrowTypeParser;
import org.eclipse.gef.dot.internal.language.parser.antlr.DotColorParser;
import org.eclipse.gef.dot.internal.language.parser.antlr.DotPointParser;
import org.eclipse.gef.dot.internal.language.parser.antlr.DotShapeParser;
import org.eclipse.gef.dot.internal.language.parser.antlr.DotSplineTypeParser;
import org.eclipse.gef.dot.internal.language.parser.antlr.DotStyleParser;
import org.eclipse.gef.dot.internal.language.rankdir.Rankdir;
import org.eclipse.gef.dot.internal.language.splines.Splines;
import org.eclipse.gef.dot.internal.language.validation.DotArrowTypeJavaValidator;
import org.eclipse.gef.dot.internal.language.validation.DotColorJavaValidator;
import org.eclipse.gef.dot.internal.language.validation.DotPointJavaValidator;
import org.eclipse.gef.dot.internal.language.validation.DotShapeJavaValidator;
import org.eclipse.gef.dot.internal.language.validation.DotSplineTypeJavaValidator;
import org.eclipse.gef.dot.internal.language.validation.DotStyleJavaValidator;
import org.eclipse.xtext.parser.IParseResult;
import org.eclipse.xtext.parser.IParser;
import org.eclipse.xtext.serializer.ISerializer;

import com.google.inject.Injector;

/**
 * Provides access to parsers, serializers, and validators of the respective Dot
 * language grammars.
 * 
 * @author nyssen
 *
 */
// TODO: Merge this class into DotAttributes, moving the validator fields to
// DotJavaValidator.
public class DotLanguageSupport {

	private static class PrimitiveValueParseResultImpl<T>
			implements IPrimitiveValueParser.IParseResult<T> {

		private T parsedValue;
		private List<Diagnostic> syntaxErrors;

		private PrimitiveValueParseResultImpl(T parsedValue) {
			this(parsedValue, Collections.<Diagnostic> emptyList());
		}

		private PrimitiveValueParseResultImpl(List<Diagnostic> syntaxErrors) {
			this(null, syntaxErrors);
		}

		private PrimitiveValueParseResultImpl(T parsedValue,
				List<Diagnostic> syntaxErrors) {
			this.parsedValue = parsedValue;
			this.syntaxErrors = syntaxErrors;
		}

		@Override
		public T getParsedValue() {
			return parsedValue;
		}

		@Override
		public List<Diagnostic> getSyntaxErrors() {
			return syntaxErrors;
		}

		@Override
		public boolean hasSyntaxErrors() {
			return !syntaxErrors.isEmpty();
		}
	}

	/**
	 * A parser to parse a DOT primitive value type.
	 * 
	 * @param <T>
	 *            The java equivalent of the parsed DOT value.
	 */
	public interface IPrimitiveValueParser<T> {

		/**
		 * The parse result of an {@link IPrimitiveValueParser}, which comprises
		 * a parsed value and/or syntax errors.
		 * 
		 * @param <T>
		 *            The java equivalent of the parsed DOT value.
		 */
		public interface IParseResult<T> {

			/**
			 * Returns the parsed (primitive) object value.
			 * 
			 * @return The parsed value, or <code>null</code> if it could not be
			 *         parsed.
			 */
			public T getParsedValue();

			/**
			 * Returns the syntax errors that occurred during the parse.
			 * 
			 * @return The list of syntax errors, if any.
			 */
			public List<Diagnostic> getSyntaxErrors();

			/**
			 * Indicates whether any syntax errors occurred during the parsing.
			 * 
			 * @return <code>true</code> in case syntax errors occurred,
			 *         <code>false</code> otherwise.
			 */
			public boolean hasSyntaxErrors();

		}

		/**
		 * Parses the given raw value as a DOT primitive value.
		 * 
		 * @param rawValue
		 *            The raw value to parse.
		 * @return An {@link IParseResult} indicating the parse result.
		 */
		IParseResult<T> parse(String rawValue);
	}

	/**
	 * A parser to parse a DOT primitive value type.
	 * 
	 * @param <T>
	 *            The java equivalent of the parsed DOT value.
	 */
	public interface IPrimitiveValueSerializer<T> {

		/**
		 * Serializes the given value.
		 * 
		 * @param value
		 *            The value to serialize.
		 * @return The string representations, to which the value was
		 *         serialized.
		 */
		String serialize(T value);
	}

	/**
	 * Parses the given value as a DOT dirType.
	 */
	public static IPrimitiveValueParser<DirType> DIRTYPE_PARSER = new IPrimitiveValueParser<DirType>() {
		@Override
		public IPrimitiveValueParser.IParseResult<DirType> parse(
				String rawValue) {
			if (rawValue == null) {
				return null;
			}
			for (DirType value : DirType.values()) {
				if (value.toString().equals(rawValue)) {
					return new PrimitiveValueParseResultImpl<>(value);
				}
			}
			return new PrimitiveValueParseResultImpl<>(
					Collections.<Diagnostic> singletonList(new BasicDiagnostic(
							Diagnostic.ERROR, rawValue, -1,
							"Value has to be one of "
									+ getFormattedValues(DirType.values()),
							new Object[] {})));
		}
	};

	/**
	 * A serializer for {@link DirType} values.
	 */
	public static IPrimitiveValueSerializer<DirType> DIRTYPE_SERIALIZER = new IPrimitiveValueSerializer<DirType>() {

		@Override
		public String serialize(DirType value) {
			return value.toString();
		}
	};

	/**
	 * Parses the given value as a DOT dirType.
	 */
	public static IPrimitiveValueParser<Layout> LAYOUT_PARSER = new IPrimitiveValueParser<Layout>() {
		@Override
		public IPrimitiveValueParser.IParseResult<Layout> parse(
				String rawValue) {
			if (rawValue == null) {
				return null;
			}
			for (Layout value : Layout.values()) {
				if (value.toString().equals(rawValue)) {
					return new PrimitiveValueParseResultImpl<>(value);
				}
			}
			return new PrimitiveValueParseResultImpl<>(
					Collections.<Diagnostic> singletonList(new BasicDiagnostic(
							Diagnostic.ERROR, rawValue, -1,
							"Value has to be one of "
									+ getFormattedValues(DirType.values()),
							new Object[] {})));
		}
	};

	/**
	 * A serializer for {@link DirType} values.
	 */
	public static IPrimitiveValueSerializer<Layout> LAYOUT_SERIALIZER = new IPrimitiveValueSerializer<Layout>() {

		@Override
		public String serialize(Layout value) {
			return value.toString();
		}
	};

	/**
	 * Parses the given value as a {@link ClusterMode}.
	 */
	public static IPrimitiveValueParser<ClusterMode> CLUSTERMODE_PARSER = new IPrimitiveValueParser<ClusterMode>() {
		@Override
		public IPrimitiveValueParser.IParseResult<ClusterMode> parse(
				String rawValue) {
			if (rawValue == null) {
				return null;
			}
			for (ClusterMode value : ClusterMode.values()) {
				if (value.toString().equals(rawValue)) {
					return new PrimitiveValueParseResultImpl<>(value);
				}
			}
			return new PrimitiveValueParseResultImpl<>(
					Collections.<Diagnostic> singletonList(new BasicDiagnostic(
							Diagnostic.ERROR, rawValue, -1,
							"Value has to be one of "
									+ getFormattedValues(ClusterMode.values()),
							new Object[] {})));
		}
	};

	/**
	 * Serializes the given {@link ClusterMode} value.
	 */
	public static IPrimitiveValueSerializer<ClusterMode> CLUSTERMODE_SERIALIZER = new IPrimitiveValueSerializer<ClusterMode>() {

		@Override
		public String serialize(ClusterMode value) {
			return value.toString();
		}
	};

	/**
	 * Parses the given value as a DOT outputMode.
	 */
	public static IPrimitiveValueParser<OutputMode> OUTPUTMODE_PARSER = new IPrimitiveValueParser<OutputMode>() {
		@Override
		public IPrimitiveValueParser.IParseResult<OutputMode> parse(
				String rawValue) {
			if (rawValue == null) {
				return null;
			}
			for (OutputMode value : OutputMode.values()) {
				if (value.toString().equals(rawValue)) {
					return new PrimitiveValueParseResultImpl<>(value);
				}
			}
			return new PrimitiveValueParseResultImpl<>(
					Collections.<Diagnostic> singletonList(new BasicDiagnostic(
							Diagnostic.ERROR, rawValue, -1,
							"Value has to be one of "
									+ getFormattedValues(OutputMode.values()),
							new Object[] {})));
		}
	};

	/**
	 * Serializes the given {@link OutputMode} value.
	 */
	public static IPrimitiveValueSerializer<OutputMode> OUTPUTMODE_SERIALIZER = new IPrimitiveValueSerializer<OutputMode>() {

		@Override
		public String serialize(OutputMode value) {
			return value.toString();
		}
	};

	/**
	 * Parses the given value as a DOT pagedir.
	 */
	public static IPrimitiveValueParser<Pagedir> PAGEDIR_PARSER = new IPrimitiveValueParser<Pagedir>() {
		@Override
		public IPrimitiveValueParser.IParseResult<Pagedir> parse(
				String rawValue) {
			if (rawValue == null) {
				return null;
			}
			for (Pagedir value : Pagedir.values()) {
				if (value.toString().equals(rawValue)) {
					return new PrimitiveValueParseResultImpl<>(value);
				}
			}
			return new PrimitiveValueParseResultImpl<>(
					Collections.<Diagnostic> singletonList(new BasicDiagnostic(
							Diagnostic.ERROR, rawValue, -1,
							"Value has to be one of "
									+ getFormattedValues(Pagedir.values()),
							new Object[] {})));
		}
	};

	/**
	 * Serializes the given {@link Pagedir} value.
	 */
	public static IPrimitiveValueSerializer<Pagedir> PAGEDIR_SERIALIZER = new IPrimitiveValueSerializer<Pagedir>() {

		@Override
		public String serialize(Pagedir value) {
			return value.toString();
		}
	};

	/**
	 * A parser used to parse DOT rankdir values.
	 */
	public static IPrimitiveValueParser<Rankdir> RANKDIR_PARSER = new IPrimitiveValueParser<Rankdir>() {
		@Override
		public IPrimitiveValueParser.IParseResult<Rankdir> parse(
				String rawValue) {
			if (rawValue == null) {
				return null;
			}
			for (Rankdir value : Rankdir.values()) {
				if (value.toString().equals(rawValue)) {
					return new PrimitiveValueParseResultImpl<>(value);
				}
			}
			return new PrimitiveValueParseResultImpl<>(
					Collections.<Diagnostic> singletonList(new BasicDiagnostic(
							Diagnostic.ERROR, rawValue, -1,
							"The given value '" + rawValue
									+ "' has to be one of "
									+ getFormattedValues(Rankdir.values()),
							new Object[] {})));
		}
	};

	/**
	 * Serializes the given {@link Rankdir} value.
	 */
	public static IPrimitiveValueSerializer<Rankdir> RANKDIR_SERIALIZER = new IPrimitiveValueSerializer<Rankdir>() {

		@Override
		public String serialize(Rankdir value) {
			return value.toString();
		}
	};

	/**
	 * A parser used to parse DOT {@link Splines} values.
	 */
	public static IPrimitiveValueParser<Splines> SPLINES_PARSER = new IPrimitiveValueParser<Splines>() {
		@Override
		public IPrimitiveValueParser.IParseResult<Splines> parse(
				String rawValue) {
			if (rawValue == null) {
				return null;
			}
			for (Splines value : Splines.values()) {
				if (value.toString().equals(rawValue)) {
					return new PrimitiveValueParseResultImpl<>(value);
				}
			}
			return new PrimitiveValueParseResultImpl<>(
					Collections.<Diagnostic> singletonList(new BasicDiagnostic(
							Diagnostic.ERROR, rawValue, -1,
							"The given value '" + rawValue
									+ "' has to be one of "
									+ getFormattedValues(Splines.values()),
							new Object[] {})));
		}
	};

	/**
	 * Serializes the given {@link Splines} value.
	 */
	public static IPrimitiveValueSerializer<Splines> SPLINES_SERIALIZER = new IPrimitiveValueSerializer<Splines>() {

		@Override
		public String serialize(Splines value) {
			return value.toString();
		}
	};

	private static String getFormattedValues(Object[] values) {
		StringBuilder sb = new StringBuilder();
		for (Object value : values) {
			if (sb.length() > 0) {
				sb.append(", ");
			}
			sb.append("'" + value.toString() + "'");
		}
		return sb.append(".").toString();
	}

	/**
	 * A parser for bool values.
	 */
	public static IPrimitiveValueParser<Boolean> BOOL_PARSER = new IPrimitiveValueParser<Boolean>() {

		@Override
		public IPrimitiveValueParser.IParseResult<Boolean> parse(
				String rawValue) {
			if (rawValue == null) {
				return null;
			}
			// case insensitive "true" or "yes"
			if (Boolean.TRUE.toString().equalsIgnoreCase(rawValue)
					|| "yes".equalsIgnoreCase(rawValue)) {
				return new PrimitiveValueParseResultImpl<>(Boolean.TRUE);
			}
			// case insensitive "false" or "no"
			if (Boolean.FALSE.toString().equalsIgnoreCase(rawValue)
					|| "no".equalsIgnoreCase(rawValue)) {
				return new PrimitiveValueParseResultImpl<>(Boolean.FALSE);
			}
			// an integer value
			try {
				int parsedValue = Integer.parseInt(rawValue);
				return new PrimitiveValueParseResultImpl<>(
						parsedValue > 0 ? Boolean.TRUE : Boolean.FALSE);
			} catch (NumberFormatException e) {
				return new PrimitiveValueParseResultImpl<>(Collections
						.<Diagnostic> singletonList(new BasicDiagnostic(
								Diagnostic.ERROR, rawValue, -1,
								"The given value '" + rawValue
										+ "' does not (case-insensitively) equal 'true', 'yes', 'false', or 'no' and is also not parsable as an integer value",
								new Object[] {})));
			}
		}
	};

	/**
	 * A serializer for bool values.
	 */
	public static IPrimitiveValueSerializer<Boolean> BOOL_SERIALIZER = new IPrimitiveValueSerializer<Boolean>() {

		@Override
		public String serialize(Boolean value) {
			return Boolean.toString(value);
		}
	};

	/**
	 * A parser for double values.
	 */
	public static IPrimitiveValueParser<Double> DOUBLE_PARSER = new IPrimitiveValueParser<Double>() {

		@Override
		public IPrimitiveValueParser.IParseResult<Double> parse(
				String rawValue) {
			if (rawValue == null) {
				return null;
			}
			try {
				// TODO: check that this resembles the DOT double interpretation
				double parsedValue = Double.parseDouble(rawValue);
				return new PrimitiveValueParseResultImpl<>(
						new Double(parsedValue));
			} catch (NumberFormatException exception) {
				return new PrimitiveValueParseResultImpl<>(Collections
						.<Diagnostic> singletonList(new BasicDiagnostic(
								Diagnostic.ERROR, rawValue, -1,
								exception.getMessage(), new Object[] {})));
			}
		}
	};

	/**
	 * A serializer for double values.
	 */
	public static IPrimitiveValueSerializer<Double> DOUBLE_SERIALIZER = new IPrimitiveValueSerializer<Double>() {

		@Override
		public String serialize(Double value) {
			return Double.toString(value);
		}
	};

	/**
	 * A parser used to parse DOT int values.
	 */
	public static IPrimitiveValueParser<Integer> INT_PARSER = new IPrimitiveValueParser<Integer>() {

		@Override
		public IPrimitiveValueParser.IParseResult<Integer> parse(
				String rawValue) {
			if (rawValue == null) {
				return null;
			}
			try {
				int parsedValue = Integer.parseInt(rawValue);
				return new PrimitiveValueParseResultImpl<>(
						new Integer(parsedValue));
			} catch (NumberFormatException exception) {
				return new PrimitiveValueParseResultImpl<>(Collections
						.<Diagnostic> singletonList(new BasicDiagnostic(
								Diagnostic.ERROR, rawValue, -1,
								exception.getMessage(), new Object[] {})));
			}
		}
	};

	/**
	 * A serializer for int values.
	 */
	public static IPrimitiveValueSerializer<Integer> INT_SERIALIZER = new IPrimitiveValueSerializer<Integer>() {

		@Override
		public String serialize(Integer value) {
			return Integer.toString(value);
		}
	};

	private static final Injector arrowTypeInjector = new DotArrowTypeStandaloneSetup()
			.createInjectorAndDoEMFRegistration();

	/**
	 * The validator for arrowtype attribute values.
	 */
	// TODO: move to DotJavaValidator
	public static final DotArrowTypeJavaValidator ARROWTYPE_VALIDATOR = arrowTypeInjector
			.getInstance(DotArrowTypeJavaValidator.class);

	/**
	 * The parser for arrowtype attribute values.
	 */
	public static final DotArrowTypeParser ARROWTYPE_PARSER = arrowTypeInjector
			.getInstance(DotArrowTypeParser.class);

	/**
	 * The serializer for arrowtype attribute values.
	 */
	public static final ISerializer ARROWTYPE_SERIALIZER = arrowTypeInjector
			.getInstance(ISerializer.class);

	private static final Injector colorInjector = new DotColorStandaloneSetup()
			.createInjectorAndDoEMFRegistration();

	/**
	 * The parser for color attribute values.
	 */
	public static final DotColorParser COLOR_PARSER = colorInjector
			.getInstance(DotColorParser.class);

	/**
	 * The serializer for color attribute values.
	 */
	public static final ISerializer COLOR_SERIALIZER = colorInjector
			.getInstance(ISerializer.class);

	private static final Injector pointInjector = new DotPointStandaloneSetup()
			.createInjectorAndDoEMFRegistration();

	/**
	 * The parser for point attribute values.
	 */
	public static final DotPointParser POINT_PARSER = pointInjector
			.getInstance(DotPointParser.class);

	/**
	 * The serializer for point attribute values.
	 */
	public static final ISerializer POINT_SERIALIZER = pointInjector
			.getInstance(ISerializer.class);

	private static final Injector shapeInjector = new DotShapeStandaloneSetup()
			.createInjectorAndDoEMFRegistration();

	/**
	 * The parser for shape attribute values.
	 */
	public static final DotShapeParser SHAPE_PARSER = shapeInjector
			.getInstance(DotShapeParser.class);

	/**
	 * The serializer for shape attribute values.
	 */
	public static final ISerializer SHAPE_SERIALIZER = shapeInjector
			.getInstance(ISerializer.class);

	private static final Injector splineTypeInjector = new DotSplineTypeStandaloneSetup()
			.createInjectorAndDoEMFRegistration();

	/**
	 * The parser for splinetype attribute values.
	 */
	public static final DotSplineTypeParser SPLINETYPE_PARSER = splineTypeInjector
			.getInstance(DotSplineTypeParser.class);

	/**
	 * The serializer for splinetype attribute values.
	 */
	public static final ISerializer SPLINETYPE_SERIALIZER = splineTypeInjector
			.getInstance(ISerializer.class);

	private static final Injector styleInjector = new DotStyleStandaloneSetup()
			.createInjectorAndDoEMFRegistration();

	/**
	 * The serializer for style attribute values.
	 */
	public static final ISerializer STYLE_SERIALIZER = styleInjector
			.getInstance(ISerializer.class);

	/**
	 * The parser for style attribute values.
	 */
	public static final DotStyleParser STYLE_PARSER = styleInjector
			.getInstance(DotStyleParser.class);

	/**
	 * Validator for Color types.
	 */
	public static final DotColorJavaValidator COLOR_VALIDATOR = colorInjector
			.getInstance(DotColorJavaValidator.class);

	/**
	 * Validator for SplineType types.
	 */
	public static final DotSplineTypeJavaValidator SPLINETYPE_VALIDATOR = splineTypeInjector
			.getInstance(DotSplineTypeJavaValidator.class);

	/**
	 * Validator for Point types.
	 */
	public static final DotPointJavaValidator POINT_VALIDATOR = pointInjector
			.getInstance(DotPointJavaValidator.class);

	/**
	 * Validator for Shape types.
	 */
	public static final DotShapeJavaValidator SHAPE_VALIDATOR = shapeInjector
			.getInstance(DotShapeJavaValidator.class);

	/**
	 * Validator for Style types.
	 */
	public static final DotStyleJavaValidator STYLE_VALIDATOR = styleInjector
			.getInstance(DotStyleJavaValidator.class);

	/**
	 * Serialize the given attribute value using the given serializer.
	 * 
	 * @param <T>
	 *            The object type of the to be serialized value.
	 * @param serializer
	 *            The {@link ISerializer} to use for serializing.
	 * @param attributeValue
	 *            The value to serialize.
	 * @return The serialized value.
	 */
	public static <T extends EObject> String serializeAttributeValue(
			ISerializer serializer, T attributeValue) {
		if (attributeValue == null) {
			return null;
		}
		return serializer.serialize(attributeValue);
	}

	/**
	 * Serialize the given attribute value using the given serializer.
	 * 
	 * @param <T>
	 *            The (primitive) object type of the to be serialized value.
	 * @param serializer
	 *            The {@link IPrimitiveValueSerializer} to use for serializing.
	 * @param attributeValue
	 *            The value to serialize.
	 * @return The serialized value.
	 */
	public static <T> String serializeAttributeValue(
			IPrimitiveValueSerializer<T> serializer, T attributeValue) {
		if (attributeValue == null) {
			return null;
		}
		return serializer.serialize(attributeValue);
	}

	/**
	 * Parses the given (unquoted) attribute, using the given
	 * {@link IPrimitiveValueParser}.
	 * 
	 * @param <T>
	 *            The (primitive) object type of the parsed value.
	 * @param parser
	 *            The parser to be used for parsing.
	 * @param attributeValue
	 *            The attribute value that is to be parsed.
	 * @return The parsed value, or <code>null</code> if the value could not be
	 *         parsed.
	 */
	public static <T> T parseAttributeValue(IPrimitiveValueParser<T> parser,
			String attributeValue) {
		if (attributeValue == null) {
			return null;
		}
		IPrimitiveValueParser.IParseResult<T> parsedAttributeValue = parser
				.parse(attributeValue);
		return parsedAttributeValue.getParsedValue();
	}

	/**
	 * Parses the given (unquoted) attribute, using the given {@link IParser}.
	 * 
	 * @param <T>
	 *            The type of the parsed value.
	 * @param parser
	 *            The parser to be used for parsing.
	 * @param attributeValue
	 *            The (unquoted) attribute value that is to be parsed.
	 * @return The parsed value, or <code>null</code> if the value could not be
	 *         parsed.
	 */
	@SuppressWarnings("unchecked")
	public static <T extends EObject> T parseAttributeValue(IParser parser,
			String attributeValue) {
		if (attributeValue == null) {
			return null;
		}
		IParseResult parsedAttributeValue = parser
				.parse(new StringReader(attributeValue));
		return (T) parsedAttributeValue.getRootASTElement();
	}
}
