// "Make 'IterablePipeline' abstract" "true"
// ERROR: 'pipe' overrides nothing
// ERROR: Type argument expected

// Actually this test is about getting rid of assertion happenning while creating quick fixes
// See KT-10409
interface Pipeline<TPipeline> {
    fun pipe(block: Pipeline<TPipeline, String>)
}

<caret>class IterablePipeline<T> : Pipeline<T> {
    override fun pipe(block: Pipeline<T>) {
    }
}
