package com.flasharc.impdiscoverer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.annotation.processing.Filer;
import javax.lang.model.element.Element;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.tools.FileObject;
import javax.tools.StandardLocation;

public class ServiceLoaderListener implements ServicesListener {
	private static final String SERVICES_PATH = "META-INF/services/";

	@Override
	public void processPluggableServicesMap(Map<Element, List<Element>> pluggableServices, AnnotationProcessor processor) {
		for (Entry<Element, List<Element>> serviceEntry : pluggableServices.entrySet()) {
			processService(serviceEntry.getKey(), serviceEntry.getValue(), processor);
		}
	}

	private void processService(Element service, List<Element> implementations, AnnotationProcessor processor) {
		try {
			Filer filer = processor.getProcessingEnvironment().getFiler();
			String resourceFile = SERVICES_PATH + getNameForElement(service);
			
			List<String> implementationNames = new ArrayList<String>();
			for (Element implementation : implementations) {
				implementationNames.add(getNameForElement(implementation));
			}
			
			Writer writer = null;
			try {
				FileObject existingResource = filer.getResource(StandardLocation.CLASS_OUTPUT, "", resourceFile);
				Set<String> existingServices = getServiceImpsFromResourceFile(existingResource);
				implementationNames.removeAll(existingServices);
				
				writer = existingResource.openWriter();
				for (String impName : implementationNames) {
					writer.append("\n" + impName);
				}
				writer.append("\n");
				return;
			} catch (IOException e) {
				// No existingResourceFile available.
			} finally {
				if (writer != null) {
					writer.close();
				}
			}
			
			// Coming here means there was no existing resource file. Create one.
			FileObject newResource = filer.createResource(StandardLocation.CLASS_OUTPUT, "", resourceFile);
			try {
				writer = newResource.openWriter();
				for (String impName : implementationNames) {
					writer.append(impName + "\n");
				}
			} finally {
				if (writer != null) {
					writer.close();
				}
			}
		} catch (Exception e) {
			processor.printError("Creating the services file failed for " + service, e);
		}
	}
	
	private Set<String> getServiceImpsFromResourceFile(FileObject existingResource) throws IOException {
		Reader reader = existingResource.openReader(true);
		Set<String> serviceImps = new HashSet<String>();
		try {
			BufferedReader bufReader = new BufferedReader(reader);
			String line;
			while ((line = bufReader.readLine()) != null) {
				int commentStart = line.indexOf('#');
				if (commentStart >= 0) {
					line = line.substring(0, commentStart);
				}
				line = line.trim();
				if (!line.isEmpty()) {
					serviceImps.add(line);
				}
			}
		} finally {
			if (reader != null) {
				reader.close();
			}
		}
		return serviceImps;
	}
	
	private String getNameForElement(Element element) {
		return getNameForElementRecursive(element, element.getSimpleName().toString());
	}

	private String getNameForElementRecursive(Element element, String className) {
		Element enclosingElement = element.getEnclosingElement();

		if (enclosingElement instanceof PackageElement) {
			PackageElement pkg = (PackageElement) enclosingElement;
			if (pkg.isUnnamed()) {
				return className;
			}
			return pkg.getQualifiedName() + "." + className;
		}

		TypeElement typeElement = (TypeElement) enclosingElement;
		return getNameForElementRecursive(typeElement, typeElement.getSimpleName() + "$" + className);
	}

}
