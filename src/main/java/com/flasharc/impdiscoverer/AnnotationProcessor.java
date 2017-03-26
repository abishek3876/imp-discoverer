package com.flasharc.impdiscoverer;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic.Kind;

public class AnnotationProcessor extends AbstractProcessor {
	
	private final Map<Element, List<Element>> pluggableServices = new HashMap<>();
	private final List<ServicesListener> listeners;
	
	public AnnotationProcessor() {
		listeners = new ArrayList<>();
		listeners.add(new ServiceLoaderListener());
	}

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
			informListeners();
		} else {
			discoverServices(annotations, roundEnv);
		}
		return true;
	}
	
	private void informListeners() {
		printLog("Services & Implementations: " + pluggableServices.toString());
		for (ServicesListener listener : listeners) {
			listener.processPluggableServicesMap(pluggableServices);
		}
	}
	
	private void discoverServices(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
		Set<? extends Element> rootElements = roundEnv.getRootElements();
		
		for (Element element : rootElements) {
			printLog("Processing Type: " + element);
			AnnotationMirror annotationMirror = getAnnotationMirror(element, PluggableService.class);
			
			if (annotationMirror != null) {
				if (pluggableServices.put(element, new ArrayList<>()) != null) {
					printWarning("Duplicate Service Class", element);
				}
			}
		}
		
		Types typeUtils = processingEnv.getTypeUtils();
		for (Element element : rootElements) {
			Set<Modifier> modifiers = element.getModifiers();
			
			// If it's a public non-abstract class.
			if (ElementKind.CLASS.equals(element.getKind()) 
					&& modifiers.contains(Modifier.PUBLIC) 
					&& !modifiers.contains(Modifier.ABSTRACT)) {
				
				for (Entry<Element, List<Element>> serviceElementEntry : pluggableServices.entrySet()) {
					if (typeUtils.isSubtype(element.asType(), serviceElementEntry.getKey().asType())) {
						serviceElementEntry.getValue().add(element);
					}
				}
			}
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
	
	private void printWarning(CharSequence message, Element element) {
		processingEnv.getMessager().printMessage(Kind.WARNING, message, element);
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
