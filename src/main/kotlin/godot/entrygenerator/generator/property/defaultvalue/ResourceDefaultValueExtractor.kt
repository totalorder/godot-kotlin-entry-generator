package godot.entrygenerator.generator.property.defaultvalue

import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe

class ResourceDefaultValueExtractor(propertyDescriptor: PropertyDescriptor, bindingContext: BindingContext) : DefaultValueExtractor(propertyDescriptor, bindingContext) {

    override fun getDefaultValue(): Pair<String, Array<out Any>> {
        if (!propertyDescriptor.isLateInit && isVisibleInEditor()) {
            throw IllegalStateException("You initialized the property \"${propertyDescriptor.fqNameSafe}\". Properties of type Resource which are registered using the @RegisterProperty annotation and are visible in the editor are not allowed to have a default value. Use lateinit.")
        }
        return super.getDefaultValue()
    }
}