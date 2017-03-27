# imp-discoverer
This library uses Annotation Processing to find the implementations of a Type annotated with @PluggableService annotation.
The implementing class does not have to have any annotations or some other kind of mark up, just extending from the annotated Type would be enough.

#### How is this different from other similar libraries
Other libraries all work by searching for Service implementations to have some kind of annotation. This would mean an reliance on all the service providers to include the library and create the annotations themselves. But this library requires the annotation to only be present in the Service interface itself. And the service providers just have to implement the interface as they would do normally.
