package godot.entrygenerator.generator.clazz

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FunSpec
import godot.entrygenerator.EntryGenerationType
import godot.entrygenerator.generator.function.FunctionRegistrationGeneratorProvider
import godot.entrygenerator.generator.property.PropertyRegistrationGeneratorProvider
import godot.entrygenerator.generator.signal.KotlinNativeSignalRegistrationGenerator
import godot.entrygenerator.generator.signal.SignalRegistrationGeneratorProvider
import godot.entrygenerator.model.ClassWithMembers
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe

class KotlinNativeClassRegistrationGenerator : ClassRegistrationGenerator() {

    override fun provideRegisterClassControlFlow(classWithMembers: ClassWithMembers, classRegistryControlFlow: FunSpec.Builder, className: ClassName, superClass: String, isTool: Boolean): FunSpec.Builder {
        return classRegistryControlFlow.beginControlFlow(
            "registerClass(%S,·%S,·%L,·$isTool)·{",
            classWithMembers.classDescriptor.fqNameSafe.asString(),
            superClass,
            className.constructorReference()
        )
    }

    override fun registerFunctions(functions: List<FunctionDescriptor>, registerClassControlFlow: FunSpec.Builder, className: ClassName) {
        FunctionRegistrationGeneratorProvider
            .provide(EntryGenerationType.KOTLIN_NATIVE)
            .registerFunctions(functions, registerClassControlFlow, className)
    }

    override fun registerSignals(signals: List<PropertyDescriptor>, registerClassControlFlow: FunSpec.Builder) {
        SignalRegistrationGeneratorProvider
            .provide(EntryGenerationType.KOTLIN_NATIVE)
            .registerSignals(signals, registerClassControlFlow)
    }

    override fun registerProperties(properties: List<PropertyDescriptor>, registerClassControlFlow: FunSpec.Builder, className: ClassName, bindingContext: BindingContext) {
        PropertyRegistrationGeneratorProvider
            .provide(EntryGenerationType.KOTLIN_NATIVE)
            .registerProperties(properties, registerClassControlFlow, className, bindingContext)
    }
}
