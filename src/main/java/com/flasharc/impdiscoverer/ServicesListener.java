package com.flasharc.impdiscoverer;

import java.util.List;
import java.util.Map;

import javax.lang.model.element.Element;

public interface ServicesListener {

	void processPluggableServicesMap(Map<Element, List<Element>> pluggableServices, AnnotationProcessor processor);
}
