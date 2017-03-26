package com.flasharc.impdiscoverer;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.SimpleElementVisitor6;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic.Kind;

public class AnnotationProcessor extends AbstractProcessor {
	
	private Map<String, List<String>> serviceProviders = new HashMap<>();

	@Override
	public Set<String> getSupportedAnnotationTypes() {
		return Collections.singleton(PluggableService.class.getName());
	}
	
	@Override
	public SourceVersion getSupportedSourceVersion() {
		return SourceVersion.latestSupported();
	}

	@Override
	public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
		try {
			return processInternal(annotations, roundEnv);
		} catch (Exception e) {
			printError("Exception processing annotations", e);
			return true;
		}
	}
	
	private boolean processInternal(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
		if (roundEnv.processingOver()) {
			//TODO: Call the listeners.
		} else {
			processFile(annotations, roundEnv);
		}
		return true;
	}
	
	private void processFile(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
		Set<? extends Element> rootElements = roundEnv.getRootElements();
		
		for (Element element : rootElements) {
			printLog("Processing Type: " + element);
			AnnotationMirror annotationMirror = getAnnotationMirror(element, PluggableService.class);
			printLog(annotationMirror == null? "null" : annotationMirror.toString());
		}
	}
	
	private AnnotationMirror getAnnotationMirror(Element element, Class<? extends Annotation> annotationType) {
		String annotationClassName = annotationType.getCanonicalName();
		for (AnnotationMirror annotationMirror : element.getAnnotationMirrors()) {
			TypeElement annotationTypeElement = (TypeElement) annotationMirror.getAnnotationType().asElement();
			if (annotationTypeElement.getQualifiedName().contentEquals(annotationClassName)) {
				return annotationMirror;
			}
		}
		return null;
	}

	private void printLog(CharSequence message) {
		processingEnv.getMessager().printMessage(Kind.NOTE, message);
	}
	
	private void printError(CharSequence error) {
		processingEnv.getMessager().printMessage(Kind.ERROR, error);
	}
	
	private void printError(CharSequence error, Throwable throwable) {
		StringWriter writer = new StringWriter();
		throwable.printStackTrace(new PrintWriter(writer));
		// Leaving it with the default lineSeparator is not working on Windows commandline (atleast with Gradle).
		String stackTrace = writer.toString().replace(System.lineSeparator(), "\n");
		printError(error + stackTrace);
	}

}
