import kotlin.properties.Delegates

val x: Int <caret>by Delegates.lazy {1}

// REF: (in kotlin.properties.ReadOnlyProperty).getValue(R,kotlin.PropertyMetadata)