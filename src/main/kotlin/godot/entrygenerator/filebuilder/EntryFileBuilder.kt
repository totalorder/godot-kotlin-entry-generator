package godot.entrygenerator.filebuilder

import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import godot.entrygenerator.model.ClassWithMembers
import org.jetbrains.kotlin.resolve.BindingContext
import java.io.File

abstract class EntryFileBuilder(val bindingContext: BindingContext) {
    protected val entryFileSpec = FileSpec
        .builder("godot", "Entry")
        .addComment("THIS FILE IS GENERATED! DO NOT EDIT IT MANUALLY! ALL CHANGES TO IT WILL BE OVERWRITTEN ON EACH BUILD")

    abstract fun registerClassesWithMembers(classesWithMembers: Set<ClassWithMembers>): EntryFileBuilder

    open fun build(outputPath: String) {
        entryFileSpec.build().writeTo(File(outputPath))
    }
}
