package godot.entrygenerator.generator.property

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.MemberName.Companion.member
import com.squareup.kotlinpoet.asTypeName
import godot.entrygenerator.EntryGenerationType
import godot.entrygenerator.extension.getFirstRegistrableTypeAsFqNameStringOrNull
import godot.entrygenerator.generator.property.defaultvalue.DefaultValueExtractorProvider
import godot.entrygenerator.generator.property.hintstring.PropertyHintStringGeneratorProvider
import godot.entrygenerator.generator.property.typehint.PropertyTypeHintProvider
import godot.entrygenerator.mapper.RpcModeAnnotationMapper
import godot.entrygenerator.mapper.TypeToVariantAsClassNameMapper
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.types.typeUtil.isEnum

class KotlinNativePropertyRegistrationGenerator : PropertyRegistrationGenerator() {

    override fun registerEnumFlag(className: ClassName, propertyDescriptor: PropertyDescriptor, bindingContext: BindingContext, registerClassControlFlow: FunSpec.Builder) {
        val (defaultValueStringTemplate, defaultValueStringTemplateValues) = DefaultValueExtractorProvider
            .provide(propertyDescriptor, bindingContext)
            .getDefaultValue()

        registerClassControlFlow.addStatement(
            "enumFlagProperty(%S,·%L,·${defaultValueStringTemplate.replace(" ", "·")},·%L,·%T)",
            propertyDescriptor.name,
            className.member(propertyDescriptor.name.asString()).reference(),
            *defaultValueStringTemplateValues,
            shouldBeVisibleInEditor(propertyDescriptor),
            RpcModeAnnotationMapper.mapRpcModeAnnotationToClassName(getRpcModeEnum(propertyDescriptor))
        )
    }

    override fun registerEnumList(className: ClassName, propertyDescriptor: PropertyDescriptor, bindingContext: BindingContext, registerClassControlFlow: FunSpec.Builder) {
        val (defaultValueStringTemplate, defaultValueStringTemplateValues) = DefaultValueExtractorProvider
            .provide(propertyDescriptor, bindingContext)
            .getDefaultValue()

        registerClassControlFlow.addStatement(
            "enumListProperty(%S,·%L,·${defaultValueStringTemplate.replace(" ", "·")},·%L,·%T)",
            propertyDescriptor.name,
            className.member(propertyDescriptor.name.asString()).reference(),
            *defaultValueStringTemplateValues,
            shouldBeVisibleInEditor(propertyDescriptor),
            RpcModeAnnotationMapper.mapRpcModeAnnotationToClassName(getRpcModeEnum(propertyDescriptor))
        )
    }

    override fun registerEnum(className: ClassName, propertyDescriptor: PropertyDescriptor, bindingContext: BindingContext, registerClassControlFlow: FunSpec.Builder) {
        val (defaultValueStringTemplate, defaultValueStringTemplateValues) = DefaultValueExtractorProvider
            .provide(propertyDescriptor, bindingContext)
            .getDefaultValue()

        registerClassControlFlow
            .addStatement(
                "enumProperty(%S,·%L,·${defaultValueStringTemplate.replace(" ", "·")},·%L,·%T)",
                propertyDescriptor.name,
                className.member(propertyDescriptor.name.asString()).reference(),
                *defaultValueStringTemplateValues,
                shouldBeVisibleInEditor(propertyDescriptor),
                RpcModeAnnotationMapper.mapRpcModeAnnotationToClassName(getRpcModeEnum(propertyDescriptor))
            )
    }

    override fun registerProperty(className: ClassName, propertyDescriptor: PropertyDescriptor, bindingContext: BindingContext, registerClassControlFlow: FunSpec.Builder) {
        val (defaultValueStringTemplate, defaultValueStringTemplateValues) = DefaultValueExtractorProvider
            .provide(propertyDescriptor, bindingContext)
            .getDefaultValue()
        val (variantToTypeTemplate, variantToTypeTemplateValue) = getVariantToTypeConverter(propertyDescriptor)
        val (typeToVariantTemplate, typeToVariantTemplateValue) = getTypeToVariantConverter(propertyDescriptor)
        val hintString = PropertyHintStringGeneratorProvider.provide(propertyDescriptor, bindingContext).getHintString()

        registerClassControlFlow
            .addStatement(
                "property(%S,·%L,·$typeToVariantTemplate,·$variantToTypeTemplate,·%T,·${defaultValueStringTemplate.replace(" ", "·")},·%L,·%T,·%T,·%S)",
                propertyDescriptor.name,
                className.member(propertyDescriptor.name.asString()).reference(),
                typeToVariantTemplateValue,
                variantToTypeTemplateValue,
                TypeToVariantAsClassNameMapper.mapTypeToVariantAsClassName(
                    propertyDescriptor.type.toString(),
                    propertyDescriptor.type,
                    propertyDescriptor.type.isEnum()
                ), //property variant type
                *defaultValueStringTemplateValues,
                shouldBeVisibleInEditor(propertyDescriptor),
                RpcModeAnnotationMapper.mapRpcModeAnnotationToClassName(getRpcModeEnum(propertyDescriptor)),
                PropertyTypeHintProvider.provide(propertyDescriptor, EntryGenerationType.KOTLIN_NATIVE),
                hintString
            )
    }

    private fun getVariantToTypeConverter(propertyDescriptor: PropertyDescriptor): Pair<String, ClassName> {
        val firstRegistrableType = propertyDescriptor.type.getFirstRegistrableTypeAsFqNameStringOrNull()
            ?: throw IllegalArgumentException("Registered property \"${propertyDescriptor.fqNameSafe}\" is of unregistrable type: ${propertyDescriptor.type}. You can only register properties which are either primitive or derive from a Godot type")

        if (firstRegistrableType == "godot.core.ObjectArray") {
            throw IllegalArgumentException("Registered property \"${propertyDescriptor.fqNameSafe}\" is of type ObjectArray. ObjectArray cannot be registered directly. Use VariantArray instead and use the asObjectArray() function for conversion.")
        }

        val typeAsString = firstRegistrableType
            .replaceBeforeLast(".", "")
            .replace(".", "")

        val packageAsString = firstRegistrableType
            .replaceAfterLast(".", "")

        val argumentTemplateString = if (typeAsString == "GodotArray") {
            "getVariantToTypeConversionFunction<%T<*>>()"
        } else {
            "getVariantToTypeConversionFunction<%T>()"
        }

        return argumentTemplateString to ClassName(packageAsString, typeAsString)
    }

    private fun getTypeToVariantConverter(propertyDescriptor: PropertyDescriptor): Pair<String, ClassName> {
        val className = when {
            isOfType(propertyDescriptor.type, "godot.internal.type.CoreType") -> ClassName("godot.internal.type", "CoreType")
            isOfType(propertyDescriptor.type, "godot.Object") -> ClassName("godot", "Object")
            isOfType(propertyDescriptor.type, "godot.core.Variant") -> ClassName("godot.core", "Variant")
            KotlinBuiltIns.isInt(propertyDescriptor.type) -> Int::class.asTypeName()
            KotlinBuiltIns.isLongOrNullableLong(propertyDescriptor.type) -> Long::class.asTypeName()
            KotlinBuiltIns.isDoubleOrNullableDouble(propertyDescriptor.type) -> Double::class.asTypeName()
            KotlinBuiltIns.isFloatOrNullableFloat(propertyDescriptor.type) -> Float::class.asTypeName()
            KotlinBuiltIns.isBooleanOrNullableBoolean(propertyDescriptor.type) -> Boolean::class.asTypeName()
            KotlinBuiltIns.isStringOrNullableString(propertyDescriptor.type) -> String::class.asTypeName()
            else -> throw IllegalArgumentException("Registered property \"${propertyDescriptor.fqNameSafe}\" is of unregistrable type: ${propertyDescriptor.type}. You can only register properties which are either primitive or derive from a Godot type")
        }

        return "getTypeToVariantConversionFunction<%T>()" to className
    }
}
